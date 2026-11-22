# Farm Mobile Push Relay

为 iOS 客户端提供 APNs 远程推送的独立中继。它连接现有 qq-farm-bot 的 Socket.IO，把农牧动态和掉线事件转发为 APNs 通知，不需要修改上游服务端。

## 为什么需要它

iOS 在 App 进入后台后会挂起 WebSocket。APNs 是唯一能在后台或 App 被系统回收后仍稳定送达通知的机制。中继常驻服务器，代替 iOS 客户端保持 Socket 连接。

## 准备

1. 在 Apple Developer 后台创建 APNs Auth Key（.p8），记录 Key ID、Team ID。
2. 确认 App Bundle ID 为 io.github.ccpopy.farmmobile，或修改 APNS_TOPIC。
3. 在 iOS 工程开启 Push Notifications 能力，并使用真机运行。

## 配置与启动

    cp .env.example .env
    # 填写 FARM_BASE_URL、FARM_ADMIN_TOKEN、RELAY_SECRET、APNs 参数
    npm install
    npm start

默认监听 :8787。没有配置 APNs 密钥时会以 dry-run 模式打印推送内容，便于先验证 Socket 事件。

### Docker

    docker build -t farm-push-relay .
    docker run -d --name farm-push-relay \
      --env-file .env \
      -p 8787:8787 \
      -v "$PWD/data:/app/data" \
      -v "$PWD/certs:/app/certs:ro" \
      farm-push-relay

## 接口

- POST /api/devices/register：iOS 客户端注册 device token。
- POST /api/devices/unregister：注销 token。
- POST /api/test：发送测试推送。
- GET /api/health：健康检查。

如果设置了 RELAY_SECRET，请求必须携带 x-relay-secret 请求头。iOS 客户端在「我的 → 消息推送 → 远程推送 (APNs)」填写相同密钥。

## iOS 端配置

1. 真机运行 App。
2. 我的 → 消息推送与通知 → 远程推送 (APNs)。
3. 填写中继地址，例如 http://192.168.1.10:8787，以及 RELAY_SECRET。
4. 开启并保存，确认页面显示 APNs 令牌与「远程推送已注册」。
5. 点击「发送测试推送」验证。

## 生产注意事项

- Debug 安装使用 APNs sandbox，APNS_PRODUCTION=false；TestFlight / App Store 使用 production，APNS_PRODUCTION=true。两种安装需要分别设置或部署两套中继。
- 建议把中继放在 HTTPS 反向代理后，限制来源 IP，并务必设置 RELAY_SECRET。
- 中继只保存 device token、accountId 与中继配置，不保存农场 Token；农场 Token 仍由 iOS Keychain 保存。
- 通知频率受中继的 900ms 去抖限制；如需更细策略可在 src/index.js 调整。
- 没有 APNs 时，iOS 端仍会使用前台 Socket + BGAppRefresh 兜底，但后台实时性无法与 APNs 相比。
