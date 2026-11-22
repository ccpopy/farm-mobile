import Foundation
import UIKit
import SwiftUI
import UserNotifications

@MainActor
final class AppStore: ObservableObject {
    @Published var ready = false
    @Published var profiles: [ServerProfile] = []
    @Published var activeProfileId = ""
    @Published var prefs = AppPrefs()
    @Published var accounts: [AccountItem] = []
    @Published var currentAccountId = ""
    @Published var status: StatusSnapshot?
    @Published var diamond: Int64 = 0
    @Published var bag = BagPayload()
    @Published var dailyGifts: DailyGiftsSummary?
    @Published var logs: [RunLogEntry] = []
    @Published var realtimeConnected = false
    @Published var accountsLoading = false
    @Published var overviewLoading = false
    @Published var connectionWarning = ""
    @Published var busy = false
    @Published var toast: String?
    @Published var space: GameSpace = .farm
    @Published var pushToken = ""
    @Published var remotePushEnabled = false
    @Published var pushRelayURL = ""
    @Published var pushRelaySecret = ""
    @Published var pushStatus = "未开启"

    let api = FarmAPI()
    let socket = SocketClient()
    private var pollTask: Task<Void, Never>?
    private var toastTask: Task<Void, Never>?
    private var reconnectWarningTask: Task<Void, Never>?
    private var logRefreshCounter = 0
    private var sessionGeneration = 0

    init() {
        socket.onConnectionChanged = { [weak self] connected in
            guard let self else { return }
            self.realtimeConnected = connected
            self.reconnectWarningTask?.cancel()
            if connected {
                self.connectionWarning = ""
            } else if self.isAuthenticated {
                self.reconnectWarningTask = Task { [weak self] in
                    try? await Task.sleep(nanoseconds: 6_000_000_000)
                    guard let self, !Task.isCancelled, !self.realtimeConnected else { return }
                    self.connectionWarning = "实时连接已断开，正在自动重连（数据仍会定时刷新）"
                }
            }
        }
        socket.onStatus = { [weak self] accountId, snapshot in
            guard let self, accountId == self.currentAccountId else { return }
            let wasConnected = self.status?.connected == true
            self.status = snapshot
            self.connectionWarning = ""
            if wasConnected && !snapshot.connected && self.prefs.offlineAlertsEnabled {
                NotificationManager.shared.post(title: "账号掉线提醒", body: "账号 \(snapshot.name.isEmpty ? accountId : snapshot.name) 已断开连接，请检查登录状态。")
            }
        }
        socket.onLog = { [weak self] entry in
            guard let self, self.shouldAcceptLog(entry) else { return }
            self.mergeLog(entry)
            self.dispatchNotification(for: entry)
        }
        socket.onLogsSnapshot = { [weak self] accountId, logs in
            guard let self, accountId == self.currentAccountId || (accountId == "all" && self.currentAccountId.isEmpty) else { return }
            self.mergeLogs(logs.filter { self.shouldAcceptLog($0) })
        }
        api.onUnauthorized = { [weak self] rejectedToken in
            Task { @MainActor in
                guard let self,
                      !rejectedToken.isEmpty,
                      rejectedToken == self.activeProfile?.token else { return }
                await self.handleUnauthorized()
            }
        }
        NotificationCenter.default.addObserver(forName: .pushTokenReceived, object: nil, queue: .main) { [weak self] note in
            guard let token = note.object as? String else { return }
            Task { @MainActor in
                guard let self else { return }
                self.pushToken = token
                self.pushStatus = self.remotePushEnabled ? "设备令牌已更新，正在注册…" : "已获取设备令牌"
                await self.syncPushRegistration()
            }
        }
        NotificationCenter.default.addObserver(forName: .pushRegistrationFailed, object: nil, queue: .main) { [weak self] note in
            Task { @MainActor in
                guard let self else { return }
                self.pushStatus = "APNs 注册失败：\(note.object as? String ?? "未知错误")"
            }
        }
        NotificationCenter.default.addObserver(forName: .silentPushReceived, object: nil, queue: .main) { [weak self] _ in
            Task { @MainActor in
                guard let self else { return }
                await self.handleForeground()
                BackgroundRefresh.schedule()
            }
        }
    }

    var activeProfile: ServerProfile? { profiles.first(where: { $0.id == activeProfileId }) }
    var isAuthenticated: Bool { activeProfile?.isAuthenticated == true }
    var isAccountOnline: Bool { !currentAccountId.isEmpty && status?.connected == true }
    var serverURL: String { activeProfile?.serverURL ?? "" }
    var accountId: String { currentAccountId }
    var currentAccount: AccountItem? { accounts.first(where: { $0.id == currentAccountId }) }
    var themeMode: AppThemeMode { AppThemeMode(rawValue: prefs.themeMode) ?? .system }
    var customAppearance: CustomAppearanceConfig { prefs.customAppearance }
    var effectiveThemeMode: AppThemeMode {
        guard themeMode == .custom else { return themeMode }
        return AppThemeMode(rawValue: prefs.customAppearance.baseThemeMode) ?? .system
    }
    var preferredScheme: ColorScheme? {
        switch effectiveThemeMode {
        case .light: return .light
        case .dark: return .dark
        case .system, .custom: return nil
        }
    }

    func bootstrap() async {
#if DEBUG
        if ProcessInfo.processInfo.arguments.contains("--farm-selftest") {
            await runSelfTest()
            return
        }
#endif
        let state = LocalStore.load()
        profiles = state.profiles
        prefs = state.prefs
        if prefs.activeProfileId.isEmpty || !profiles.contains(where: { $0.id == prefs.activeProfileId }) {
            prefs.activeProfileId = profiles.first?.id ?? ""
        }
        activeProfileId = prefs.activeProfileId
        let remote = RemotePushStore.settings
        remotePushEnabled = remote.enabled
        pushRelayURL = remote.relayURL
        pushRelaySecret = remote.secret
        pushStatus = remote.enabled ? "等待 APNs 设备令牌" : "未开启"
        ready = true
        BackgroundRefresh.register()
        BackgroundRefresh.schedule()
        if remote.enabled {
            UIApplication.shared.registerForRemoteNotifications()
        }
        if activeProfile?.isAuthenticated == true {
            await connectActive()
        }
    }

    private func persist() {
        var list = profiles
        if let idx = list.firstIndex(where: { $0.id == activeProfileId }) {
            list[idx].currentAccountId = currentAccountId
        }
        profiles = list
        prefs.activeProfileId = activeProfileId
        LocalStore.save(profiles: list, prefs: prefs)
    }

    func setTheme(_ mode: AppThemeMode) {
        prefs.themeMode = mode.rawValue
        persist()
    }

    func setCustomAppearance(_ value: CustomAppearanceConfig) {
        prefs.customAppearance = value
        prefs.themeMode = AppThemeMode.custom.rawValue
        persist()
    }

    func showToast(_ text: String) {
        toast = text
        toastTask?.cancel()
        toastTask = Task { [weak self] in
            try? await Task.sleep(nanoseconds: 2_800_000_000)
            guard let self, !Task.isCancelled else { return }
            if self.toast == text { self.toast = nil }
        }
    }

    private func updateActiveProfile(_ mutate: (inout ServerProfile) -> Void) {
        guard let idx = profiles.firstIndex(where: { $0.id == activeProfileId }) else { return }
        mutate(&profiles[idx])
        persist()
    }

    func connectActive() async {
        guard let profile = activeProfile, profile.isAuthenticated else { return }
        sessionGeneration += 1
        api.configure(serverURL: profile.serverURL, token: profile.token, accountId: profile.currentAccountId)
        currentAccountId = profile.currentAccountId
        status = nil
        diamond = 0
        bag = BagPayload()
        dailyGifts = nil
        logs = []
        accounts = []
        connectionWarning = ""
        socket.connect(baseURL: profile.serverURL, token: profile.token, accountId: profile.currentAccountId)
        await refreshAccounts()
        startPolling()
    }

    func selectProfile(_ id: String) async {
        guard let target = profiles.first(where: { $0.id == id }) else { return }
        pollTask?.cancel()
        socket.disconnect()
        activeProfileId = id
        currentAccountId = target.currentAccountId
        persist()
        await connectActive()
    }

    func renameProfile(_ id: String, name: String) {
        guard let idx = profiles.firstIndex(where: { $0.id == id }) else { return }
        profiles[idx].name = name.trimmingCharacters(in: .whitespacesAndNewlines)
        persist()
    }

    func deleteProfile(_ id: String) async {
        guard id != activeProfileId else {
            showToast("当前服务器无法删除，请先切换")
            return
        }
        profiles.removeAll { $0.id == id }
        KeychainStore.delete(id)
        persist()
    }

    func logout() async {
        sessionGeneration += 1
        await api.logout()
        if remotePushEnabled && !pushToken.isEmpty {
            try? await PushRelayClient.unregister(settings: RemotePushStore.settings, token: pushToken)
            pushStatus = "已退出登录，重新登录后会再次注册"
        }
        updateActiveProfile { $0.token = "" }
        pollTask?.cancel()
        socket.disconnect()
        accounts = []
        currentAccountId = ""
        status = nil
        diamond = 0
        bag = BagPayload()
        dailyGifts = nil
        logs = []
        connectionWarning = ""
        showToast("已退出当前授权")
    }

#if DEBUG
    private func launchArgument(_ name: String) -> String? {
        for argument in ProcessInfo.processInfo.arguments where argument.hasPrefix(name + "=") {
            return String(argument.dropFirst(name.count + 1))
        }
        return nil
    }

    private func runSelfTest() async {
        let server = launchArgument("--farm-server") ?? "http://43.142.187.232"
        let user = launchArgument("--farm-user") ?? "admin"
        let pass = launchArgument("--farm-pass") ?? ""
        var result: [String: Any] = ["server": server, "user": user, "build": 17]
        let documents = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first
        func progress(_ text: String) {
            NSLog("%@", "FARM_SELFTEST_STEP " + text)
            if let documents {
                try? text.write(to: documents.appendingPathComponent("selftest-progress.txt"),
                                atomically: true, encoding: .utf8)
            }
        }
        progress("start")

        let probe = FarmAPI()
        do {
            let version = try await probe.fetchGameVersion(baseOverride: server)
            result["farmVersion"] = version.botVersion
            result["farmClientVersion"] = version.clientVersion
            progress("farm-version-ok")
        } catch {
            result["farmVersionError"] = "\(error)"
            progress("farm-version-error: \(error)")
        }

        if let url = URL(string: server + "/api/game-version") {
            var request = URLRequest(url: url)
            request.timeoutInterval = 15
            do {
                let (data, response) = try await URLSession.shared.data(for: request)
                result["urlSessionHttpStatus"] = (response as? HTTPURLResponse)?.statusCode ?? -1
                result["urlSessionHttpBytes"] = data.count
                progress("urlsession-http-ok")
            } catch {
                result["urlSessionHttpError"] = "\(error)"
                progress("urlsession-http-error: \(error)")
            }
        }

        progress("login-start")
        let login = await probe.login(base: server, username: user, password: pass)
        progress("login-finished")
        result["loginOk"] = login.ok
        result["loginError"] = login.error
        result["mustChangePassword"] = login.mustChangePassword
        result["tokenLength"] = login.token.count

        if login.ok, !login.token.isEmpty {
            result["tokenValid"] = await probe.validateToken(baseOverride: server, tokenOverride: login.token)
            probe.configure(serverURL: server, token: login.token, accountId: "")
            do {
                let accounts = try await probe.fetchAccounts()
                result["accountCount"] = accounts.count
            } catch {
                result["accountsError"] = "\(error)"
            }

            progress("ws-direct-start")
            if let wsURL = URL(string: server.replacingOccurrences(of: "http://", with: "ws://") + "/socket.io/?EIO=4&transport=websocket") {
                let wsConfig = URLSessionConfiguration.ephemeral
                wsConfig.timeoutIntervalForRequest = 12
                let wsSession = URLSession(configuration: wsConfig)
                let wsTask = wsSession.webSocketTask(with: wsURL)
                wsTask.resume()
                do {
                    let first = try await wsTask.receive()
                    result["wsDirectFirst"] = String(String(describing: first).prefix(160))
                    if let authData = try? JSONSerialization.data(withJSONObject: ["token": login.token, "accountId": "all"]),
                       let authText = String(data: authData, encoding: .utf8) {
                        try await wsTask.send(.string("40" + authText))
                        let second = try await wsTask.receive()
                        result["wsDirectSecond"] = String(String(describing: second).prefix(160))
                    }
                } catch {
                    result["wsDirectError"] = "\(error)"
                }
                wsTask.cancel(with: .goingAway, reason: nil)
            }
            progress("ws-direct-finished")

            progress("socket-start")
            let socketProbe = SocketClient()
            var connected = false
            socketProbe.onConnectionChanged = { connected = $0 }
            socketProbe.connect(baseURL: server, token: login.token, accountId: "all")
            try? await Task.sleep(nanoseconds: 8_000_000_000)
            result["socketConnected"] = connected
            socketProbe.disconnect()
            progress("socket-finished: \(connected)")
        }

        let json: String
        if let data = try? JSONSerialization.data(withJSONObject: result, options: [.sortedKeys]),
           let text = String(data: data, encoding: .utf8) {
            json = text
        } else {
            json = "{\"error\":\"serialize failed\"}"
        }
        NSLog("%@", "FARM_SELFTEST_RESULT " + json)
        if let documents {
            try? Data(json.utf8).write(to: documents.appendingPathComponent("selftest.json"))
            progress("done")
        }
    }
#endif

    private func handleUnauthorized() async {
        guard !activeProfileId.isEmpty else { return }
        sessionGeneration += 1
        updateActiveProfile { $0.token = "" }
        pollTask?.cancel()
        socket.disconnect()
        accounts = []
        currentAccountId = ""
        status = nil
        diamond = 0
        bag = BagPayload()
        dailyGifts = nil
        logs = []
        connectionWarning = ""
        showToast("登录凭证已失效，请重新授权")
    }

    func login(server: String, username: String, password: String, displayName: String = "") async -> String? {
        guard let normalized = FarmAPI.normalizeBaseURL(server) else { return "请输入有效的服务器地址" }
        do {
            _ = try await api.fetchGameVersion(baseOverride: normalized)
        } catch let e as APIError {
            return e.message
        } catch {
            return error.localizedDescription
        }
        let result = await api.login(base: normalized, username: username, password: password)
        guard result.ok, !result.token.isEmpty else {
            let message = result.error.isEmpty ? "登录失败" : result.error
            switch result.errorType {
            case "locked":
                let mins = max(1, (result.remainingMs + 59_999) / 60_000)
                return "\(message)（约 \(mins) 分钟后解锁）"
            case "rate_limit":
                let secs = max(1, (result.remainingMs + 999) / 1_000)
                return "\(message)（\(secs) 秒后可重试）"
            default:
                return message
            }
        }
        saveProfile(server: normalized, token: result.token, username: result.username.isEmpty ? username : result.username,
                    authMode: "password", name: displayName)
        if result.mustChangePassword {
            showToast("登录成功，建议在网页端修改默认密码")
        }
        await connectActive()
        return nil
    }

    func loginWithToken(server: String, token: String, displayName: String = "") async -> String? {
        guard let normalized = FarmAPI.normalizeBaseURL(server) else { return "请输入有效的服务器地址" }
        let clean = token.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !clean.isEmpty else { return "请输入 x-admin-token" }
        let valid = await api.validateToken(baseOverride: normalized, tokenOverride: clean)
        guard valid else { return "Token 无效或已过期" }
        let username = await api.fetchUserMe(baseOverride: normalized, tokenOverride: clean)
        saveProfile(server: normalized, token: clean, username: username, authMode: "token", name: displayName)
        await connectActive()
        return nil
    }

    private func saveProfile(server: String, token: String, username: String, authMode: String, name: String) {
        if let idx = profiles.firstIndex(where: { $0.serverURL == server }) {
            profiles[idx].token = token
            profiles[idx].username = username
            profiles[idx].authMode = authMode
            if !name.isEmpty { profiles[idx].name = name }
            activeProfileId = profiles[idx].id
        } else {
            let profile = ServerProfile(name: name, serverURL: server, authMode: authMode, username: username)
            profiles.append(profile)
            activeProfileId = profile.id
        }
        if let idx = profiles.firstIndex(where: { $0.id == activeProfileId }) {
            profiles[idx].token = token
        }
        prefs.activeProfileId = activeProfileId
        currentAccountId = activeProfile?.currentAccountId ?? ""
        persist()
    }

    func refreshAccounts() async {
        guard isAuthenticated, !accountsLoading else { return }
        let generation = sessionGeneration
        accountsLoading = true
        defer { accountsLoading = false }
        do {
            let list = try await api.fetchAccounts()
            guard generation == sessionGeneration, isAuthenticated else { return }
            accounts = list
            let previousAccount = currentAccountId
            if !list.contains(where: { $0.id == currentAccountId }) {
                currentAccountId = list.first?.id ?? ""
            }
            if previousAccount != currentAccountId {
                status = nil
                diamond = 0
                bag = BagPayload()
                dailyGifts = nil
                logs = []
            }
            api.setAccount(currentAccountId)
            socket.subscribe(accountId: currentAccountId)
            updateActiveProfile { $0.currentAccountId = self.currentAccountId }
            if !currentAccountId.isEmpty {
                await refreshOverview()
            }
            await syncPushRegistration()
        } catch let e as APIError {
            if generation == sessionGeneration, !e.message.isEmpty { connectionWarning = e.message }
        } catch {
            if generation == sessionGeneration { connectionWarning = error.localizedDescription }
        }
    }

    func selectAccount(_ id: String) async {
        currentAccountId = id
        api.setAccount(id)
        updateActiveProfile { $0.currentAccountId = id }
        socket.subscribe(accountId: id)
        status = nil
        diamond = 0
        bag = BagPayload()
        dailyGifts = nil
        logs = []
        await refreshOverview()
        await syncPushRegistration()
    }

    func refreshOverview() async {
        let id = currentAccountId
        let generation = sessionGeneration
        guard !id.isEmpty else { return }
        overviewLoading = true
        async let statusTask = api.fetchStatus(accountId: id)
        async let diamondTask = api.fetchDiamond(accountId: id)
        async let bagTask = api.fetchBag(accountId: id)
        async let giftsTask = api.fetchDailyGifts(accountId: id)
        async let logsTask = api.fetchLogs(accountId: id, limit: 120)
        let s = try? await statusTask
        let d = try? await diamondTask
        let b = try? await bagTask
        let g = try? await giftsTask
        let l = try? await logsTask
        if generation == sessionGeneration, currentAccountId == id {
            if let s { status = s }
            if let d { diamond = d }
            if let b { bag = b }
            if let g { dailyGifts = g }
            if let l { mergeLogs(l) }
        }
        overviewLoading = false
    }

    func refreshStatusOnly() async {
        let id = currentAccountId
        let generation = sessionGeneration
        guard !id.isEmpty else { return }
        if let snapshot = try? await api.fetchStatus(accountId: id),
           generation == sessionGeneration, currentAccountId == id {
            status = snapshot
        }
    }

    func refreshLogs() async {
        let id = currentAccountId
        let generation = sessionGeneration
        guard !id.isEmpty else { return }
        if let list = try? await api.fetchLogs(accountId: id, limit: 120),
           generation == sessionGeneration, currentAccountId == id {
            mergeLogs(list)
        }
    }

    private func startPolling() {
        pollTask?.cancel()
        pollTask = Task { [weak self] in
            var tick = 0
            while !Task.isCancelled {
                // Socket.IO already streams status/log changes. Keep a slower
                // HTTP fallback while it is connected, and poll faster only
                // when the realtime channel is down.
                let interval: UInt64 = (self?.realtimeConnected ?? false) ? 15_000_000_000 : 5_000_000_000
                try? await Task.sleep(nanoseconds: interval)
                guard let self, !Task.isCancelled else { return }
                tick += 1
                if self.isAuthenticated && self.accounts.isEmpty {
                    // A transient failure while restoring a saved session used
                    // to leave the account list permanently empty. Retry it
                    // instead of requiring the user to re-enter authorization.
                    await self.refreshAccounts()
                } else {
                    await self.refreshStatusOnly()
                    if tick % 2 == 0 { await self.refreshLogs() }
                }
            }
        }
    }

    private func shouldAcceptLog(_ entry: RunLogEntry) -> Bool {
        let active = currentAccountId
        return active.isEmpty || entry.accountId.isEmpty || entry.accountId == active
    }

    private func retainedLogs(_ candidates: [RunLogEntry]) -> [RunLogEntry] {
        let cutoff = Int64(Date().timeIntervalSince1970 * 1000) - 3 * 86_400_000
        var map: [String: RunLogEntry] = [:]
        for item in candidates where shouldAcceptLog(item) && item.isVisible(since: cutoff) {
            map[item.id] = item
        }
        return map.values.sorted { $0.ts < $1.ts }.suffix(1_000).map { $0 }
    }

    private func mergeLogs(_ incoming: [RunLogEntry]) {
        logs = retainedLogs(logs + incoming)
    }

    private func mergeLog(_ entry: RunLogEntry) {
        guard shouldAcceptLog(entry) else { return }
        logs = retainedLogs(logs + [entry])
    }

    private func dispatchNotification(for entry: RunLogEntry) {
        guard prefs.activityPushEnabled else { return }
        guard let key = notificationKey(entry), prefs.enabledNotifyTypes.contains(key) else { return }
        let title = entry.tag.isEmpty ? "农牧动态" : entry.tag
        NotificationManager.shared.post(title: title, body: entry.msg)
    }

    private func notificationKey(_ entry: RunLogEntry) -> String? {
        let raw = [entry.event, entry.module, entry.tag, entry.msg].joined(separator: " ").lowercased()
        if raw.contains("steal") || raw.contains("偷") {
            if raw.contains("ranch") || raw.contains("牧场") { return "ranch_harvest" }
            return "friend_steal"
        }
        if raw.contains("ranch") || raw.contains("牧场") || raw.contains("动物") {
            if raw.contains("produce") || raw.contains("生产") { return "ranch_produce" }
            if raw.contains("feed") || raw.contains("喂养") || raw.contains("清理") || raw.contains("牧草") { return "ranch_care" }
            if raw.contains("buy") || raw.contains("place") || raw.contains("sell") || raw.contains("升级") { return "ranch_trade" }
            return "ranch_harvest"
        }
        if raw.contains("friend") || raw.contains("好友") || raw.contains("帮忙") {
            if raw.contains("bad") || raw.contains("捣乱") || raw.contains("蚊子") { return "friend_bad" }
            return "friend_help"
        }
        if raw.contains("plant") || raw.contains("种植") { return "farm_plant" }
        if raw.contains("water") || raw.contains("weed") || raw.contains("bug") || raw.contains("施肥") || raw.contains("除草") || raw.contains("除虫") { return "farm_care" }
        if raw.contains("upgrade") || raw.contains("升级") { return "farm_land" }
        if raw.contains("sell") || raw.contains("出售") { return "warehouse_sell" }
        if raw.contains("task") || raw.contains("gift") || raw.contains("任务") || raw.contains("礼包") { return "task_reward" }
        if raw.contains("harvest") || raw.contains("收获") { return "farm_harvest" }
        return nil
    }

    func setPushEnabled(_ enabled: Bool) async {
        let previous = prefs.activityPushEnabled
        prefs.activityPushEnabled = enabled
        persist()
        if enabled {
            let granted = await NotificationManager.shared.requestAuthorization()
            if !granted {
                prefs.activityPushEnabled = previous
                persist()
                showToast("系统通知权限未授予，请前往设置开启")
            }
        }
    }

    func setOfflineAlerts(_ enabled: Bool) {
        prefs.offlineAlertsEnabled = enabled
        persist()
    }

    func setNotifyTypes(_ keys: [String]) {
        prefs.enabledNotifyTypes = keys
        persist()
    }

    func sendTestNotification() async {
        let granted = await NotificationManager.shared.requestAuthorization()
        if granted {
            NotificationManager.shared.post(title: "农场助手", body: "这是一条测试通知，农牧动态推送已开启。", id: "test-\(UUID().uuidString)")
            showToast("测试通知已发送")
        } else {
            showToast("系统通知权限未授予")
        }
    }

    func startAccount(_ id: String) async {
        do {
            try await api.startAccount(id)
            showToast("账号启动指令已发送")
            await refreshAccounts()
        } catch let e as APIError { showToast(e.message) }
        catch { showToast(error.localizedDescription) }
    }

    func stopAccount(_ id: String) async {
        do {
            try await api.stopAccount(id)
            showToast("账号已停止")
            await refreshAccounts()
        } catch let e as APIError { showToast(e.message) }
        catch { showToast(error.localizedDescription) }
    }

    func deleteAccount(_ id: String) async {
        do {
            try await api.deleteAccount(id)
            showToast("账号已删除")
            await refreshAccounts()
        } catch let e as APIError { showToast(e.message) }
        catch { showToast(error.localizedDescription) }
    }

    @discardableResult
    func updateAccount(id: String, name: String, code: String) async -> Bool {
        do {
            let startError = try await api.updateAccount(id: id, name: name, code: code)
            if let startError { showToast("账号已保存，但启动失败：\(startError)") }
            else { showToast("账号信息已更新") }
            await refreshAccounts()
            return true
        } catch let e as APIError {
            showToast(e.message)
            return false
        } catch {
            showToast(error.localizedDescription)
            return false
        }
    }

    @discardableResult
    func addAccount(name: String, platform: String, code: String, loginType: String = "manual", wxLoginTaskId: String? = nil) async -> Bool {
        do {
            try await api.saveAccount(name: name, platform: platform, code: code, loginType: loginType, wxLoginTaskId: wxLoginTaskId)
            showToast("账号已保存")
            await refreshAccounts()
            return true
        } catch let e as APIError {
            showToast(e.message)
            return false
        } catch {
            showToast(error.localizedDescription)
            return false
        }
    }

    func clearLogs() async {
        guard !currentAccountId.isEmpty else { return }
        do {
            try await api.clearLogs(accountId: currentAccountId)
            logs = []
            showToast("已清空当前账号运行日志")
        } catch let e as APIError { showToast(e.message) }
        catch { showToast(error.localizedDescription) }
    }

    func refreshBag() async {
        guard !currentAccountId.isEmpty else { return }
        if let b = try? await api.fetchBag(accountId: currentAccountId) { bag = b }
    }

    func refreshAfterAction() async {
        await refreshOverview()
    }

    func handleForeground() async {
        guard let profile = activeProfile, profile.isAuthenticated else { return }
        if !realtimeConnected {
            socket.connect(baseURL: profile.serverURL, token: profile.token, accountId: currentAccountId)
        }
        if accounts.isEmpty || currentAccountId.isEmpty {
            await refreshAccounts()
        } else {
            await refreshOverview()
        }
        await syncPushRegistration()
    }

    func configureRemotePush(enabled: Bool, relayURL: String, secret: String) async -> String? {
        let normalized = enabled ? PushRelayClient.normalize(relayURL) : (relayURL.isEmpty ? "" : PushRelayClient.normalize(relayURL))
        if enabled && normalized == nil {
            return "请填写有效的推送中继地址，例如 http://192.168.1.10:8787"
        }
        let settings = RemotePushSettings(enabled: enabled, relayURL: normalized ?? "", secret: secret.trimmingCharacters(in: .whitespacesAndNewlines))
        RemotePushStore.save(settings)
        remotePushEnabled = enabled
        pushRelayURL = settings.relayURL
        pushRelaySecret = settings.secret
        if enabled {
            let granted = await NotificationManager.shared.requestAuthorization()
            if !granted {
                let disabled = RemotePushSettings(enabled: false, relayURL: settings.relayURL, secret: settings.secret)
                RemotePushStore.save(disabled)
                remotePushEnabled = false
                pushStatus = "未开启：系统通知权限被拒绝"
                return "系统通知权限未授予，请在设置中开启"
            }
            UIApplication.shared.registerForRemoteNotifications()
            pushStatus = pushToken.isEmpty ? "已开启，等待 APNs 设备令牌" : "正在注册到推送中继…"
            await syncPushRegistration()
        } else {
            if !pushToken.isEmpty {
                try? await PushRelayClient.unregister(settings: settings, token: pushToken)
            }
            pushStatus = "已关闭"
        }
        return nil
    }

    func syncPushRegistration() async {
        guard remotePushEnabled else { return }
        guard let profile = activeProfile, profile.isAuthenticated else {
            pushStatus = "当前没有有效服务器授权"
            return
        }
        guard !pushRelayURL.isEmpty else {
            pushStatus = "未配置推送中继地址"
            return
        }
        guard !pushToken.isEmpty else {
            pushStatus = "等待 APNs 设备令牌"
            return
        }
        let settings = RemotePushSettings(enabled: true, relayURL: pushRelayURL, secret: pushRelaySecret)
        do {
            let message = try await PushRelayClient.register(
                settings: settings,
                token: pushToken,
                accountId: currentAccountId,
                farmServerURL: profile.serverURL,
                bundleId: Bundle.main.bundleIdentifier ?? "io.github.ccpopy.farmmobile"
            )
            pushStatus = message
        } catch let e as APIError {
            pushStatus = "注册失败：\(e.message)"
        } catch {
            pushStatus = "注册失败：\(error.localizedDescription)"
        }
    }

    func sendRelayTest() async -> String {
        guard remotePushEnabled else { return "请先开启远程推送" }
        guard !pushToken.isEmpty else { return "尚未获取 APNs 设备令牌，请使用真机并确认 Push 能力" }
        guard !pushRelayURL.isEmpty else { return "请先配置推送中继地址" }
        do {
            let message = try await PushRelayClient.test(
                settings: RemotePushSettings(enabled: true, relayURL: pushRelayURL, secret: pushRelaySecret),
                token: pushToken
            )
            return message
        } catch let e as APIError {
            return "测试失败：\(e.message)"
        } catch {
            return "测试失败：\(error.localizedDescription)"
        }
    }
}
