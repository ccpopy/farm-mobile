import fs from "node:fs";
import path from "node:path";
import express from "express";
import { io } from "socket.io-client";
import apn from "@parse/node-apn";

const PORT = Number(process.env.PORT || 8787);
const FARM_BASE_URL = (process.env.FARM_BASE_URL || "").replace(/\/+$/, "");
const FARM_ADMIN_TOKEN = process.env.FARM_ADMIN_TOKEN || "";
const RELAY_SECRET = process.env.RELAY_SECRET || "";
const APNS_KEY_PATH = process.env.APNS_KEY_PATH || "";
const APNS_KEY_ID = process.env.APNS_KEY_ID || "";
const APNS_TEAM_ID = process.env.APNS_TEAM_ID || "";
const APNS_TOPIC = process.env.APNS_TOPIC || "io.github.ccpopy.farmmobile";
const APNS_PRODUCTION = String(process.env.APNS_PRODUCTION || "false") === "true";
const DATA_FILE = process.env.DATA_FILE || path.join(process.cwd(), "data", "devices.json");

fs.mkdirSync(path.dirname(DATA_FILE), { recursive: true });

function loadDevices() {
  try {
    const raw = JSON.parse(fs.readFileSync(DATA_FILE, "utf8"));
    return new Map((Array.isArray(raw) ? raw : []).map((d) => [d.token, d]));
  } catch { return new Map(); }
}
let devices = loadDevices();
function saveDevices() {
  const tmp = DATA_FILE + ".tmp";
  fs.writeFileSync(tmp, JSON.stringify([...devices.values()], null, 2));
  fs.renameSync(tmp, DATA_FILE);
}

const app = express();
app.use(express.json({ limit: "64kb" }));

function requireSecret(req, res, next) {
  if (!RELAY_SECRET) return next();
  if (req.get("x-relay-secret") !== RELAY_SECRET) return res.status(401).json({ ok: false, error: "中继密钥不正确" });
  next();
}

let provider = null;
if (APNS_KEY_PATH && APNS_KEY_ID && APNS_TEAM_ID && fs.existsSync(APNS_KEY_PATH)) {
  provider = new apn.Provider({ token: { key: fs.readFileSync(APNS_KEY_PATH), keyId: APNS_KEY_ID, teamId: APNS_TEAM_ID }, production: APNS_PRODUCTION });
  console.log("[apns] provider ready", APNS_PRODUCTION ? "production" : "sandbox");
} else {
  console.warn("[apns] key not configured; running in dry-run mode");
}

function sanitizeToken(value) { return String(value || "").trim().toLowerCase(); }
function devicesForAccount(accountId) {
  return [...devices.values()].filter((d) => !d.accountId || d.accountId === "all" || d.accountId === accountId);
}

async function sendPush({ title, body, accountId = "", event = "", tokens }) {
  const targets = (tokens || devicesForAccount(accountId)).map((d) => (typeof d === "string" ? d : d.token)).filter(Boolean);
  if (!targets.length) return { sent: 0, failed: 0 };
  if (!provider) {
    console.log("[dry-run]", title, body, "->", targets.length, "device(s)");
    return { sent: targets.length, failed: 0 };
  }
  const note = new apn.Notification();
  note.expiry = Math.floor(Date.now() / 1000) + 3600;
  note.sound = "default";
  note.alert = { title, body };
  note.topic = APNS_TOPIC;
  note.pushType = "alert";
  note.contentAvailable = true;
  note.payload = { accountId, event, source: "farm-mobile-relay" };
  try {
    const result = await provider.send(note, targets);
    let removed = 0;
    for (const failure of result.failed || []) {
      const reason = failure?.response?.reason || failure?.error?.message || "";
      if (["BadDeviceToken", "Unregistered", "DeviceTokenNotForTopic"].includes(reason)) {
        if (devices.delete(failure.device || "")) removed++;
      }
      console.warn("[apns] failed", failure.device, reason);
    }
    if (removed) saveDevices();
    return { sent: (result.sent || []).length, failed: (result.failed || []).length };
  } catch (error) {
    console.error("[apns] send error", error);
    return { sent: 0, failed: targets.length };
  }
}

app.get("/api/health", (_req, res) => {
  res.json({ ok: true, service: "farm-mobile-push-relay", devices: devices.size, apns: Boolean(provider), socketConnected: socket?.connected === true, farmBaseUrl: FARM_BASE_URL || null, time: new Date().toISOString() });
});

app.post("/api/devices/register", requireSecret, (req, res) => {
  const token = sanitizeToken(req.body?.token);
  if (!/^[a-f0-9]{32,200}$/.test(token)) return res.status(400).json({ ok: false, error: "APNs device token 格式无效" });
  const entry = {
    token, deviceId: String(req.body?.deviceId || ""), platform: String(req.body?.platform || "ios"),
    accountId: String(req.body?.accountId || ""), farmServerUrl: String(req.body?.farmServerUrl || ""),
    bundleId: String(req.body?.bundleId || ""), updatedAt: new Date().toISOString()
  };
  devices.set(token, entry);
  saveDevices();
  console.log("[register]", token.slice(0, 12) + "...", "account", entry.accountId || "all");
  res.json({ ok: true, message: "远程推送已注册" });
});

app.post("/api/devices/unregister", requireSecret, (req, res) => {
  const token = sanitizeToken(req.body?.token);
  const removed = devices.delete(token);
  if (removed) saveDevices();
  res.json({ ok: true, removed, message: removed ? "设备已注销" : "设备不存在" });
});

app.post("/api/test", requireSecret, async (req, res) => {
  const token = sanitizeToken(req.body?.token);
  const tokens = token ? [token] : undefined;
  const result = await sendPush({ title: "农场助手", body: "这是一条来自 APNs 推送中继的测试通知。", event: "relay_test", tokens });
  res.json({ ok: true, ...result, message: "测试推送已处理（成功 " + result.sent + "，失败 " + result.failed + "）" });
});

app.use((_req, res) => res.status(404).json({ ok: false, error: "not found" }));

let socket = null;
let retryDelay = 2000;
const lastStatus = new Map();
const lastPushAt = new Map();

function connectFarmSocket() {
  if (!FARM_BASE_URL || !FARM_ADMIN_TOKEN) {
    console.warn("[socket] FARM_BASE_URL / FARM_ADMIN_TOKEN not configured");
    return;
  }
  const base = new URL(FARM_BASE_URL);
  const path = (base.pathname.replace(/\/+$/, "") || "") + "/socket.io";
  socket = io(base.origin, {
    path, transports: ["websocket"], reconnection: true, reconnectionDelay: 2000, reconnectionDelayMax: 15000,
    auth: { token: FARM_ADMIN_TOKEN, accountId: "all" }, extraHeaders: { "x-admin-token": FARM_ADMIN_TOKEN }
  });
  socket.on("connect", () => {
    retryDelay = 2000;
    console.log("[socket] connected", base.origin + path);
    socket.emit("subscribe", { accountId: "all" });
  });
  socket.on("disconnect", (reason) => {
    console.warn("[socket] disconnected", reason);
    setTimeout(() => socket?.connect(), retryDelay);
    retryDelay = Math.min(retryDelay * 1.6, 15000);
  });
  socket.on("connect_error", (error) => console.warn("[socket] connect error", error.message));
  socket.on("log:new", (payload) => {
    if (!payload || typeof payload !== "object") return;
    const accountId = String(payload.accountId || "all");
    const now = Date.now();
    if (now - (lastPushAt.get(accountId) || 0) < 900) return;
    lastPushAt.set(accountId, now);
    const title = String(payload.tag || "农牧动态");
    const body = String(payload.msg || "").trim();
    if (!body) return;
    const event = String(payload.event || payload?.meta?.event || "");
    sendPush({ title, body, accountId, event }).catch((e) => console.error("[push]", e));
  });
  socket.on("status:update", (payload) => {
    if (!payload || typeof payload !== "object") return;
    const accountId = String(payload.accountId || "");
    const connected = Boolean(payload?.status?.connection?.connected);
    const previous = lastStatus.get(accountId);
    lastStatus.set(accountId, connected);
    if (previous === true && connected === false) {
      sendPush({ title: "账号掉线提醒", body: "账号 " + (payload?.status?.status?.name || accountId) + " 已断开连接", accountId, event: "offline" }).catch((e) => console.error("[push]", e));
    }
  });
}

connectFarmSocket();
app.listen(PORT, () => console.log("[relay] listening on :" + PORT));
process.on("SIGTERM", () => {
  console.log("[relay] shutting down");
  socket?.disconnect();
  provider?.shutdown();
  process.exit(0);
});
