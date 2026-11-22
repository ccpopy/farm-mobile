import UIKit
import UserNotifications

extension Notification.Name {
    static let pushTokenReceived = Notification.Name("farm.push.token.received")
    static let pushRegistrationFailed = Notification.Name("farm.push.registration.failed")
    static let silentPushReceived = Notification.Name("farm.push.silent.received")
}

@MainActor
final class AppDelegate: NSObject, UIApplicationDelegate {
    func application(_ application: UIApplication,
                     didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil) -> Bool {
        _ = NotificationManager.shared
        BackgroundRefresh.register()
        UIApplication.shared.setMinimumBackgroundFetchInterval(15 * 60)
        return true
    }

    func application(_ application: UIApplication,
                     didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data) {
        let token = deviceToken.map { String(format: "%02x", $0) }.joined()
        NotificationCenter.default.post(name: .pushTokenReceived, object: token)
    }

    func application(_ application: UIApplication,
                     didFailToRegisterForRemoteNotificationsWithError error: Error) {
        NotificationCenter.default.post(name: .pushRegistrationFailed, object: error.localizedDescription)
    }

    func application(_ application: UIApplication,
                     didReceiveRemoteNotification userInfo: [AnyHashable: Any],
                     fetchCompletionHandler completionHandler: @escaping (UIBackgroundFetchResult) -> Void) {
        NotificationCenter.default.post(name: .silentPushReceived, object: userInfo)
        completionHandler(.newData)
    }

    func application(_ application: UIApplication,
                     performFetchWithCompletionHandler completionHandler: @escaping (UIBackgroundFetchResult) -> Void) {
        Task {
            let success = await BackgroundRefresh.performFetch()
            completionHandler(success ? .newData : .failed)
        }
    }

    func applicationDidEnterBackground(_ application: UIApplication) {
        BackgroundRefresh.schedule()
    }

    func applicationWillEnterForeground(_ application: UIApplication) {
        BackgroundRefresh.schedule()
    }
}
