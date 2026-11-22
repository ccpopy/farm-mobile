import Foundation
import BackgroundTasks
import UserNotifications

enum BackgroundRefresh {
    static let taskIdentifier = "com.qqfarm.mobile.refresh"
    private static let lastLogKeyPrefix = "farm_ios_bg_last_log_"
    private static let lastStatusKeyPrefix = "farm_ios_bg_last_status_"
    private static var registered = false

    static func register() {
        guard !registered else { return }
        registered = true
        BGTaskScheduler.shared.register(forTaskWithIdentifier: taskIdentifier, using: nil) { task in
            guard let refreshTask = task as? BGAppRefreshTask else {
                task.setTaskCompleted(success: false)
                return
            }
            handle(refreshTask)
        }
    }

    static func schedule() {
        let request = BGAppRefreshTaskRequest(identifier: taskIdentifier)
        request.earliestBeginDate = Date(timeIntervalSinceNow: 15 * 60)
        try? BGTaskScheduler.shared.submit(request)
    }

    private static func handle(_ task: BGAppRefreshTask) {
        schedule()
        let work = Task<Bool, Never> {
            await refreshInBackground()
        }
        task.expirationHandler = { work.cancel() }
        Task {
            let success = await work.value
            task.setTaskCompleted(success: success)
        }
    }

    static func performFetch() async -> Bool {
        await refreshInBackground()
    }

    private static func refreshInBackground() async -> Bool {
        let state = LocalStore.load()
        guard let profile = state.profiles.first(where: { $0.id == state.prefs.activeProfileId }),
              profile.isAuthenticated, !profile.currentAccountId.isEmpty else { return false }
        let api = FarmAPI()
        api.configure(serverURL: profile.serverURL, token: profile.token, accountId: profile.currentAccountId)
        do {
            let logs = try await api.fetchLogs(accountId: profile.currentAccountId, limit: 30)
            guard !logs.isEmpty else { return true }
            let lastKey = lastLogKeyPrefix + profile.currentAccountId
            let previous = UserDefaults.standard.double(forKey: lastKey)
            let newLogs = logs.filter { Double($0.ts) > previous }.sorted { $0.ts < $1.ts }
            if previous > 0, !newLogs.isEmpty, state.prefs.activityPushEnabled {
                let latest = newLogs.suffix(3).map { "\($0.tag.isEmpty ? "动态" : $0.tag)：\($0.msg)" }.joined(separator: "\n")
                post(title: "农场助手 · \(newLogs.count) 条新动态", body: latest, identifier: "bg-\(profile.currentAccountId)")
            }
            if let newest = logs.map(\.ts).max() {
                UserDefaults.standard.set(Double(newest), forKey: lastKey)
            }
            if state.prefs.offlineAlertsEnabled {
                let status = try await api.fetchStatus(accountId: profile.currentAccountId)
                let statusKey = lastStatusKeyPrefix + profile.currentAccountId
                let previousStatus = UserDefaults.standard.object(forKey: statusKey) as? Bool
                if previousStatus == true, !status.connected {
                    post(title: "账号掉线提醒",
                         body: "账号 \(status.name.isEmpty ? profile.currentAccountId : status.name) 已断开连接",
                         identifier: "offline-\(profile.currentAccountId)")
                }
                UserDefaults.standard.set(status.connected, forKey: statusKey)
            }
            return true
        } catch {
            return false
        }
    }

    private static func post(title: String, body: String, identifier: String) {
        let content = UNMutableNotificationContent()
        content.title = title
        content.body = body
        content.sound = .default
        let request = UNNotificationRequest(identifier: identifier + "-\(Int(Date().timeIntervalSince1970))",
                                            content: content, trigger: nil)
        UNUserNotificationCenter.current().add(request)
    }
}
