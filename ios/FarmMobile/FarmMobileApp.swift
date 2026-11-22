import Foundation
import UIKit
import SwiftUI

@main
struct FarmMobileApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) private var appDelegate
    @StateObject private var store = AppStore()

    var body: some Scene {
        WindowGroup {
            RootView()
                .environmentObject(store)
                .task {
                    if !store.ready { await store.bootstrap() }
                }
        }
    }
}
