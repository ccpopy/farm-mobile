import UIKit
import Foundation
import SwiftUI

struct RootView: View {
    @EnvironmentObject private var store: AppStore
    @Environment(\.colorScheme) private var systemScheme
    @Environment(\.scenePhase) private var scenePhase

    private var effectiveScheme: ColorScheme {
        switch store.effectiveThemeMode {
        case .light: return .light
        case .dark: return .dark
        case .system, .custom: return systemScheme
        }
    }

    var body: some View {
        Group {
            if !store.ready {
                SplashView()
            } else if store.isAuthenticated {
                MainShell()
            } else {
                ServerSetupView()
            }
        }
        .environment(\.palette, Palette(effectiveScheme, appearance: store.customAppearance,
                                         isCustom: store.themeMode == .custom))
        .preferredColorScheme(store.preferredScheme)
        .overlay(alignment: .top) {
            if let toast = store.toast {
                ToastView(message: toast)
                    .transition(.move(edge: .top).combined(with: .opacity))
                    .zIndex(10)
            }
        }
        .animation(.easeOut(duration: 0.2), value: store.toast)
        .onChange(of: scenePhase) { phase in
            if phase == .active {
                Task { await store.handleForeground() }
            }
        }
    }
}

struct SplashView: View {
    @Environment(\.palette) private var p
    var body: some View {
        ZStack {
            p.background.ignoresSafeArea()
            VStack(spacing: 16) {
                ZStack {
                    RoundedRectangle(cornerRadius: 22, style: .continuous)
                        .fill(LinearGradient(colors: [p.emerald, p.green], startPoint: .topLeading, endPoint: .bottomTrailing))
                        .frame(width: 84, height: 84)
                    Image(systemName: "leaf.fill").font(.system(size: 38, weight: .bold)).foregroundColor(.white)
                }
                Text("农场助手").font(.system(size: 24, weight: .bold)).foregroundColor(p.text)
                ProgressView().tint(p.emerald)
            }
        }
    }
}

struct MainShell: View {
    @Environment(\.palette) private var p
    @EnvironmentObject private var store: AppStore
    @State private var tab = 0

    var body: some View {
        VStack(spacing: 0) {
            Group {
                switch tab {
                case 0: NavigationStack { OverviewView() }
                case 1:
                    NavigationStack {
                        if store.space == .farm { FarmWorkspaceView() } else { RanchWorkspaceView() }
                    }
                case 2: NavigationStack { DiscoverView() }
                case 3: NavigationStack { FriendsView() }
                default: NavigationStack { SettingsView() }
                }
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)

            bottomBar
        }
        .background(p.background.ignoresSafeArea())
    }

    private var bottomBar: some View {
        VStack(spacing: 0) {
            Rectangle().fill(p.separator).frame(height: 0.5)
            HStack(spacing: 0) {
                tabItem(0, "总览", "square.grid.2x2", "square.grid.2x2.fill", accent: p.spaceAccent(store.space))
                tabItem(1, store.space.label, store.space == .farm ? "leaf" : "pawprint",
                        store.space == .farm ? "leaf.fill" : "pawprint.fill", accent: p.spaceAccent(store.space))
                tabItem(2, "发现", "safari", "safari.fill", accent: p.spaceAccent(store.space))
                tabItem(3, "好友", "person.2", "person.2.fill", accent: p.spaceAccent(store.space))
                tabItem(4, "我的", "person.crop.circle", "person.crop.circle.fill", accent: p.emerald)
            }
            .padding(.horizontal, 6)
            .padding(.top, 7)
            .padding(.bottom, 3)
        }
        .background(p.card)
    }

    private func tabItem(_ index: Int, _ title: String, _ icon: String, _ selectedIcon: String, accent: Color) -> some View {
        let selected = tab == index
        return Button {
            if tab != index {
                let generator = UIImpactFeedbackGenerator(style: .light)
                generator.impactOccurred()
                tab = index
            }
        } label: {
            VStack(spacing: 3) {
                ZStack {
                    Capsule().fill(accent.opacity(selected ? 0.14 : 0))
                        .frame(width: 48, height: 27)
                    Image(systemName: selected ? selectedIcon : icon)
                        .font(.system(size: 20, weight: .semibold))
                        .foregroundColor(selected ? accent : p.secondary)
                }
                Text(title)
                    .font(.system(size: 10, weight: selected ? .semibold : .medium))
                    .foregroundColor(selected ? accent : p.secondary)
            }
            .frame(maxWidth: .infinity)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }
}
