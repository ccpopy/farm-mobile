import Foundation
import UIKit
import SwiftUI

struct SettingsView: View {
    @Environment(\.palette) private var p
    @EnvironmentObject private var store: AppStore
    @State private var showServers = false
    @State private var showAccounts = false
    @State private var showLogout = false
    @State private var showNotifyTypes = false
    @State private var showClearLogs = false

    var body: some View {
        List {
            Section {
                NavigationLink { AutomationView() } label: {
                    settingsRow("自动化设置", subtitle: "种植与养殖策略、日常农牧、施肥、好友与神秘商人",
                                icon: "slider.horizontal.3", color: p.indigo)
                }
                NavigationLink { IllustratedView() } label: {
                    settingsRow("图鉴", subtitle: "作物、超变与蓝晶藏品进度与奖励",
                                icon: "book.closed.fill", color: p.orange)
                }
            } header: { Text("扩展与自动化") }

            Section {
                Button { showServers = true } label: {
                    settingsRow("远程服务器", subtitle: store.activeProfile.map { "\($0.displayName) · \($0.serverURL)" } ?? "未配置",
                                icon: "server.rack", color: p.blue)
                }
                HStack {
                    settingsRow("当前登录凭证",
                                subtitle: store.activeProfile?.authMode == "password"
                                    ? "管理员账号 \(store.activeProfile?.username ?? "")"
                                    : "x-admin-token 会话",
                                icon: "key.fill", color: p.emerald)
                    Spacer()
                    Pill(text: store.isAuthenticated ? "已授权" : "未授权", color: store.isAuthenticated ? p.green : p.orange, dot: true)
                }
                Button { showAccounts = true } label: {
                    settingsRow("农场账号管理", subtitle: store.currentAccount?.displayTitle ?? "管理、切换账号",
                                icon: "person.2.fill", color: p.orange)
                }
                Button(role: .destructive) { showLogout = true } label: {
                    settingsRow("退出当前授权", subtitle: "清除本地 Token，保留服务器地址",
                                icon: "rectangle.portrait.and.arrow.right", color: p.red)
                }
            } header: { Text("服务器与授权") }

            Section {
                Toggle(isOn: Binding(get: { store.prefs.activityPushEnabled },
                                     set: { value in Task { await store.setPushEnabled(value) } })) {
                    settingsRow("农牧动态推送", subtitle: "种植、收获、偷取、牧场与出售等动作通知",
                                icon: "bell.badge.fill", color: p.red)
                }
                Toggle(isOn: Binding(get: { store.prefs.offlineAlertsEnabled },
                                     set: { store.setOfflineAlerts($0) })) {
                    settingsRow("账号掉线重登提醒", subtitle: "监听登录失效与异常断连（默认关闭）",
                                icon: "wifi.exclamationmark", color: p.orange)
                }
                NavigationLink { RemotePushSettingsView() } label: {
                    settingsRow("远程推送 (APNs)", subtitle: store.pushStatus,
                                icon: "antenna.radiowaves.left.and.right", color: p.indigo)
                }
                Button { showNotifyTypes = true } label: {
                    settingsRow("通知分类", subtitle: "已订阅 \(store.prefs.enabledNotifyTypes.count) 类动作",
                                icon: "list.bullet.rectangle", color: p.indigo)
                }
                Button { Task { await store.sendTestNotification() } } label: {
                    settingsRow("发送测试通知", subtitle: "验证系统通知权限与展示效果",
                                icon: "paperplane.fill", color: p.blue)
                }
            } header: { Text("消息推送与通知") }

            Section {
                HStack {
                    settingsRow("外观主题", subtitle: "浅色 / 深色 / 跟随系统 / 自定义", icon: "circle.lefthalf.filled", color: p.purple)
                    Spacer()
                }
                Picker("外观主题", selection: Binding(get: { store.themeMode },
                                                      set: { store.setTheme($0) })) {
                    ForEach(AppThemeMode.allCases) { mode in
                        Text(mode.label).tag(mode)
                    }
                }
                .pickerStyle(.segmented)
                .listRowBackground(Color.clear)
                .listRowInsets(EdgeInsets(top: 0, leading: 16, bottom: 8, trailing: 16))
                NavigationLink { CustomAppearanceView() } label: {
                    settingsRow("自定义外观", subtitle: "配色、土地、牧场与卡片样式", icon: "paintpalette.fill", color: p.purple)
                }
                NavigationLink { AppUpdateView() } label: {
                    settingsRow("检查更新", subtitle: "从 GitHub Releases 获取最新版本与更新说明",
                                icon: "arrow.down.circle.fill", color: p.blue)
                }
            } header: { Text("外观与通用") }

            Section {
                Button(role: .destructive) { showClearLogs = true } label: {
                    settingsRow("清理运行日志", subtitle: "删除当前账号在服务端的日志记录",
                                icon: "trash.fill", color: p.red)
                }
                HStack {
                    settingsRow("关于", subtitle: "Farm Mobile iOS · SwiftUI 原生客户端",
                                icon: "info.circle.fill", color: p.secondary)
                    Spacer()
                    Text(appVersion).font(.system(size: 11)).foregroundColor(p.secondary)
                }
            } footer: {
                Text("与原 Android 端共用 liyangpengs/qq-farm-bot 服务端协议。请遵守 CC BY-NC-SA 4.0，禁止商业倒卖。")
                    .font(.system(size: 10))
            }
        }
        .listStyle(.insetGrouped)
        .scrollContentBackground(.hidden)
        .background(p.background)
        .navigationTitle("我的")
        .navigationBarTitleDisplayMode(.large)
        .sheet(isPresented: $showServers) { ServerListView().environmentObject(store) }
        .sheet(isPresented: $showAccounts) { AccountManagerSheet().environmentObject(store) }
        .sheet(isPresented: $showNotifyTypes) { NotifyTypesSheet().environmentObject(store) }
        .alert("退出授权", isPresented: $showLogout) {
            Button("退出登录", role: .destructive) { Task { await store.logout() } }
            Button("取消", role: .cancel) {}
        } message: {
            Text("确定清除本机保存的 Token 并返回服务器连接页面吗？")
        }
        .confirmationDialog("清空运行日志？", isPresented: $showClearLogs) {
            Button("清空", role: .destructive) { Task { await store.clearLogs() } }
            Button("取消", role: .cancel) {}
        }
    }

    private func settingsRow(_ title: String, subtitle: String, icon: String, color: Color) -> some View {
        HStack(spacing: 11) {
            Image(systemName: icon)
                .font(.system(size: 13, weight: .semibold))
                .foregroundColor(.white)
                .frame(width: 28, height: 28)
                .background(color)
                .clipShape(RoundedRectangle(cornerRadius: 7, style: .continuous))
            VStack(alignment: .leading, spacing: 2) {
                Text(title).font(.system(size: 14, weight: .medium)).foregroundColor(p.text)
                Text(subtitle).font(.system(size: 10)).foregroundColor(p.secondary).lineLimit(2)
            }
        }
        .padding(.vertical, 2)
    }

    private var appVersion: String {
        let v = Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "1.0"
        let b = Bundle.main.infoDictionary?["CFBundleVersion"] as? String ?? "1"
        return "\(v) (\(b))"
    }
}

struct NotifyTypesSheet: View {
    @Environment(\.palette) private var p
    @Environment(\.dismiss) private var dismiss
    @EnvironmentObject private var store: AppStore
    private let groups: [(String, [(String, String)])] = [
        ("农场动态", [("farm_harvest", "农场收获"), ("farm_plant", "自动种植"), ("farm_care", "务农与施肥"), ("farm_land", "土地解锁与升级")]),
        ("好友互动", [("friend_steal", "偷取好友"), ("friend_help", "帮好友务农"), ("friend_bad", "好友捣乱")]),
        ("牧场动态", [("ranch_harvest", "牧场收获"), ("ranch_produce", "动物生产"), ("ranch_care", "喂养与清理"), ("ranch_trade", "购置与升级")]),
        ("收益与日常", [("warehouse_sell", "仓库出售"), ("task_reward", "任务与礼包")]),
    ]

    var body: some View {
        NavigationView {
            List {
                ForEach(groups.indices, id: \.self) { gi in
                    let group = groups[gi]
                    Section(group.0) {
                        ForEach(group.1.indices, id: \.self) { ii in
                            let key = group.1[ii].0
                            let label = group.1[ii].1
                            Toggle(label, isOn: Binding(
                                get: { store.prefs.enabledNotifyTypes.contains(key) },
                                set: { enabled in
                                    var keys = store.prefs.enabledNotifyTypes
                                    if enabled { if !keys.contains(key) { keys.append(key) } }
                                    else { keys.removeAll { $0 == key } }
                                    store.setNotifyTypes(keys)
                                }
                            )).tint(p.emerald)
                        }
                    }
                }
            }
            .scrollContentBackground(.hidden)
            .background(p.background)
            .navigationTitle("通知分类")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button("完成") { dismiss() } }
            }
        }
    }
}

private struct GitHubRelease: Decodable {
    var tagName: String
    var name: String
    var body: String
    var htmlURL: String
    var publishedAt: String

    enum CodingKeys: String, CodingKey {
        case tagName = "tag_name"
        case name
        case body
        case htmlURL = "html_url"
        case publishedAt = "published_at"
    }
}

struct AppUpdateView: View {
    @Environment(\.palette) private var p
    @State private var release: GitHubRelease?
    @State private var loading = false
    @State private var error = ""
    @State private var checked = false

    private var currentVersion: String {
        Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "0.1.0"
    }

    private var latestVersion: String {
        release?.tagName.trimmingCharacters(in: CharacterSet(charactersIn: "vV")) ?? ""
    }

    private var hasUpdate: Bool {
        guard !latestVersion.isEmpty else { return false }
        return compareVersion(latestVersion, currentVersion) > 0
    }

    var body: some View {
        ScrollView {
            VStack(spacing: 12) {
                Card {
                    HStack(spacing: 12) {
                        Image(systemName: "app.badge.checkmark")
                            .font(.system(size: 26, weight: .semibold))
                            .foregroundColor(.white)
                            .frame(width: 52, height: 52)
                            .background(p.blue)
                            .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
                        VStack(alignment: .leading, spacing: 3) {
                            Text("Farm Mobile iOS").font(.system(size: 18, weight: .bold)).foregroundColor(p.text)
                            Text("当前版本 \(currentVersion)").font(.system(size: 12)).foregroundColor(p.secondary)
                            if let release {
                                Text("最新版本 \(release.tagName)").font(.system(size: 12, weight: .semibold))
                                    .foregroundColor(hasUpdate ? p.orange : p.green)
                            }
                        }
                        Spacer()
                    }
                }
                if loading {
                    Card { LoadingState(text: "正在查询 GitHub Releases") }
                } else if let release {
                    Card {
                        VStack(alignment: .leading, spacing: 8) {
                            Text(release.name.isEmpty ? release.tagName : release.name)
                                .font(.system(size: 17, weight: .semibold)).foregroundColor(p.text)
                            Text(hasUpdate ? "发现新版本，可前往 GitHub 查看更新说明" : "当前已是最新版本")
                                .font(.system(size: 12)).foregroundColor(hasUpdate ? p.orange : p.green)
                            if !release.body.isEmpty {
                                Text(release.body)
                                    .font(.system(size: 11, design: .monospaced))
                                    .foregroundColor(p.secondary)
                                    .lineLimit(20)
                                    .textSelection(.enabled)
                            }
                            if let url = URL(string: release.htmlURL) {
                                Link(destination: url) {
                                    HStack {
                                        Image(systemName: "arrow.up.right.square.fill")
                                        Text("打开 GitHub 发布页")
                                    }
                                    .font(.system(size: 14, weight: .semibold))
                                    .foregroundColor(.white)
                                    .frame(maxWidth: .infinity)
                                    .padding(.vertical, 11)
                                    .background(p.blue)
                                    .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                                }
                            }
                        }
                    }
                }
                if !error.isEmpty { ErrorBanner(message: error) }
                if checked {
                    Button { Task { await check() } } label: {
                        HStack(spacing: 5) {
                            Image(systemName: "arrow.clockwise")
                            Text("重新检查")
                        }
                        .font(.system(size: 13, weight: .semibold)).foregroundColor(p.emerald)
                    }
                }
                Text("iOS 版本需要通过 Xcode 或 TestFlight 安装；GitHub Releases 主要用于查看版本与更新说明。")
                    .font(.system(size: 10)).foregroundColor(p.secondary)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 20)
            }
            .padding(16)
        }
        .background(p.background.ignoresSafeArea())
        .navigationTitle("应用更新")
        .navigationBarTitleDisplayMode(.inline)
        .task { if !checked { await check() } }
    }

    private func check() async {
        loading = true; error = ""; checked = true
        guard let url = URL(string: "https://api.github.com/repos/ccpopy/farm-mobile/releases/latest") else {
            error = "更新地址无效"; loading = false; return
        }
        var request = URLRequest(url: url)
        request.setValue("application/vnd.github+json", forHTTPHeaderField: "Accept")
        request.setValue("FarmMobile-iOS", forHTTPHeaderField: "User-Agent")
        do {
            let (data, response) = try await URLSession.shared.data(for: request)
            guard let http = response as? HTTPURLResponse, http.statusCode == 200 else {
                throw APIError(message: "GitHub 返回异常 (\((response as? HTTPURLResponse)?.statusCode ?? 0))")
            }
            release = try JSONDecoder().decode(GitHubRelease.self, from: data)
        } catch let e as APIError {
            error = e.message
        } catch {
            self.error = "检查更新失败：\(error.localizedDescription)"
        }
        loading = false
    }

    private func compareVersion(_ lhs: String, _ rhs: String) -> Int {
        let a = lhs.split(separator: ".").map { Int($0) ?? 0 }
        let b = rhs.split(separator: ".").map { Int($0) ?? 0 }
        let count = max(a.count, b.count)
        for i in 0..<count {
            let x = i < a.count ? a[i] : 0
            let y = i < b.count ? b[i] : 0
            if x != y { return x > y ? 1 : -1 }
        }
        return 0
    }
}

struct RemotePushSettingsView: View {
    @Environment(\.palette) private var p
    @Environment(\.dismiss) private var dismiss
    @EnvironmentObject private var store: AppStore
    @State private var enabled = false
    @State private var relayURL = ""
    @State private var secret = ""
    @State private var saving = false
    @State private var error = ""

    var body: some View {
        ScrollView {
            VStack(spacing: 12) {
                Card {
                    VStack(alignment: .leading, spacing: 8) {
                        Label("iOS 后台实时方案", systemImage: "antenna.radiowaves.left.and.right")
                            .font(.system(size: 16, weight: .semibold)).foregroundColor(p.indigo)
                        Text("iOS 会在 App 进入后台后挂起 Socket。APNs 是唯一能在后台或被系统回收后仍稳定送达通知的方案；本页配置一个推送中继，由中继连接 qq-farm-bot 并转发通知。")
                            .font(.system(size: 12)).foregroundColor(p.secondary)
                        Text("另已启用 BGAppRefresh 兜底：系统可能在约 15 分钟后唤醒一次拉取日志并发送本地通知，但时间与次数由系统决定，不能替代 APNs。")
                            .font(.system(size: 11)).foregroundColor(p.secondary)
                    }
                }

                Card {
                    VStack(alignment: .leading, spacing: 12) {
                        Toggle("启用 APNs 远程推送", isOn: $enabled).tint(p.indigo)
                        VStack(alignment: .leading, spacing: 4) {
                            FieldLabel(text: "推送中继地址")
                            TextField("http://192.168.1.10:8787", text: $relayURL)
                                .keyboardType(.URL)
                                .textInputAutocapitalization(.never)
                                .autocorrectionDisabled()
                                .padding(11).background(p.elevated)
                                .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
                        }
                        VStack(alignment: .leading, spacing: 4) {
                            FieldLabel(text: "中继密钥（可选）")
                            SecureField("RELAY_SECRET", text: $secret)
                                .textInputAutocapitalization(.never)
                                .autocorrectionDisabled()
                                .padding(11).background(p.elevated)
                                .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
                        }
                        PrimaryButton(title: saving ? "正在保存…" : "保存并注册", icon: "checkmark.circle.fill", tint: p.indigo, enabled: !saving) {
                            save()
                        }
                        if !error.isEmpty { ErrorBanner(message: error) }
                    }
                }

                Card {
                    VStack(alignment: .leading, spacing: 8) {
                        Text("设备与注册状态").font(.system(size: 15, weight: .semibold)).foregroundColor(p.text)
                        HStack {
                            Text("APNs 令牌").font(.system(size: 12)).foregroundColor(p.secondary)
                            Spacer()
                            Text(store.pushToken.isEmpty ? "等待真机下发" : String(store.pushToken.prefix(16)) + "…")
                                .font(.system(size: 11, design: .monospaced)).foregroundColor(p.text)
                        }
                        Text(store.pushStatus).font(.system(size: 12)).foregroundColor(store.pushStatus.contains("失败") ? p.red : p.secondary)
                        HStack(spacing: 8) {
                            PrimaryButton(title: "发送测试推送", icon: "paperplane.fill", tint: p.emerald,
                                          enabled: store.remotePushEnabled && !store.pushToken.isEmpty) {
                                Task { store.showToast(await store.sendRelayTest()) }
                            }
                            if !store.pushToken.isEmpty {
                                Button {
                                    UIPasteboard.general.string = store.pushToken
                                    store.showToast("设备令牌已复制")
                                } label: {
                                    Text("复制令牌").font(.system(size: 12, weight: .semibold)).foregroundColor(p.blue)
                                }
                            }
                        }
                    }
                }

                Text("提示：APNs 需要真机、付费 Apple Developer 账号与开启 Push Notifications 能力。模拟器只能通过 Xcode 的模拟推送调试。")
                    .font(.system(size: 10)).foregroundColor(p.secondary)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 16)
            }
            .padding(16)
        }
        .background(p.background.ignoresSafeArea())
        .navigationTitle("远程推送")
        .navigationBarTitleDisplayMode(.inline)
        .onAppear {
            enabled = store.remotePushEnabled
            relayURL = store.pushRelayURL
            secret = store.pushRelaySecret
        }
    }

    private func save() {
        saving = true
        error = ""
        Task {
            let message = await store.configureRemotePush(enabled: enabled, relayURL: relayURL, secret: secret)
            saving = false
            if let message {
                error = message
            } else {
                store.showToast(enabled ? "远程推送配置已保存" : "远程推送已关闭")
            }
        }
    }
}

struct CustomAppearanceView: View {
    @Environment(\.palette) private var p
    @EnvironmentObject private var store: AppStore

    private var config: Binding<CustomAppearanceConfig> {
        Binding(get: { store.prefs.customAppearance },
                set: { store.setCustomAppearance($0) })
    }

    private let accentPresets: [(String, String?)] = [
        ("农牧默认", nil), ("晴空蓝", "#007AFF"), ("薰衣草", "#6E56CF"),
        ("樱花粉", "#E5487B"), ("琥珀金", "#D97706"), ("青松青", "#0D9488"),
        ("珊瑚红", "#EF4444"),
    ]
    private let ranchPresets: [(String, String?)] = [
        ("经典橙", nil), ("暖琥珀", "#D97706"), ("珊瑚橘", "#F97316"),
        ("焦糖棕", "#B45309"), ("玫瑰粉", "#E11D48"), ("青松绿", "#0D9488"),
    ]

    var body: some View {
        List {
            quickPresetSection
            Section("基础") {
                Picker("基础主题", selection: config.baseThemeMode) {
                    Text("跟随系统").tag("system")
                    Text("浅色").tag("light")
                    Text("深色").tag("dark")
                }
                Picker("背景色调", selection: config.backgroundTone) {
                    ForEach(BackgroundTone.allCases) { tone in
                        Text(tone.label).tag(tone)
                    }
                }
                Text(config.wrappedValue.backgroundTone.detail)
                    .font(.system(size: 10)).foregroundColor(p.secondary)
            }

            accentSection
            landSection
            ranchSection
            layoutSection

            Section {
                Button(role: .destructive) {
                    store.setCustomAppearance(CustomAppearanceConfig())
                } label: {
                    Label("恢复默认外观", systemImage: "arrow.counterclockwise")
                }
            } footer: {
                Text("自定义外观会立即生效；恢复默认后仍停留在自定义主题，可重新选择跟随系统或浅/深色。")
                    .font(.system(size: 10))
            }
        }
        .listStyle(.insetGrouped)
        .scrollContentBackground(.hidden)
        .background(p.background)
        .navigationTitle("自定义外观")
        .navigationBarTitleDisplayMode(.inline)
    }

    private var quickPresetSection: some View {
        Section("快捷方案") {
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 10) {
                    ForEach(appearanceQuickPresets) { preset in
                        let selected = config.wrappedValue == preset.config
                        Button {
                            store.setCustomAppearance(preset.config)
                        } label: {
                            VStack(alignment: .leading, spacing: 8) {
                                HStack(spacing: 5) {
                                    ForEach(preset.preview.indices, id: \.self) { index in
                                        Circle().fill(preset.preview[index])
                                            .frame(width: 18, height: 18)
                                            .overlay(Circle().stroke(p.separator, lineWidth: 0.5))
                                    }
                                }
                                Text(preset.name).font(.system(size: 13, weight: .semibold))
                                    .foregroundColor(p.text)
                                Text(preset.subtitle).font(.system(size: 10))
                                    .foregroundColor(p.secondary).lineLimit(2)
                            }
                            .padding(12)
                            .frame(width: 142, alignment: .leading)
                            .background(selected ? p.emerald.opacity(0.10) : p.card)
                            .clipShape(RoundedRectangle(cornerRadius: p.controlRadius, style: .continuous))
                            .overlay(RoundedRectangle(cornerRadius: p.controlRadius, style: .continuous)
                                .stroke(selected ? p.emerald : p.separator, lineWidth: selected ? 1.4 : 0.7))
                        }
                        .buttonStyle(.plain)
                    }
                }
                .padding(.vertical, 2)
            }
        }
    }

    private var accentSection: some View {
        Section("主色") {
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 10) {
                    ForEach(Array(accentPresets.enumerated()), id: \.offset) { pair in
                        let item = pair.element
                        let hex = item.1
                        let selected = config.wrappedValue.accentHex == hex
                        Button {
                            config.wrappedValue.accentHex = hex
                        } label: {
                            VStack(spacing: 4) {
                                Circle()
                                    .fill(hex.flatMap { Color(hexString: $0) } ?? p.emerald)
                                    .frame(width: 30, height: 30)
                                    .overlay(Circle().stroke(selected ? p.text : p.separator, lineWidth: selected ? 2 : 0.7))
                                Text(item.0).font(.system(size: 9)).foregroundColor(p.secondary)
                            }
                        }
                        .buttonStyle(.plain)
                    }
                }
                .padding(.vertical, 2)
            }
            ColorPicker("自定义主色", selection: Binding(
                get: { Color(hexString: config.wrappedValue.accentHex ?? "#1B8A5A") ?? p.emerald },
                set: { config.wrappedValue.accentHex = $0.hexString }
            ))
            Button("使用农牧默认主色") { config.wrappedValue.accentHex = nil }
                .font(.system(size: 12)).foregroundColor(p.emerald)
        }
    }

    private var landSection: some View {
        Section("土地卡片") {
            Picker("土地配色", selection: config.landPreset) {
                ForEach(LandColorPreset.allCases) { preset in
                    Text(preset.label).tag(preset)
                }
            }
            Text(config.wrappedValue.landPreset.detail)
                .font(.system(size: 10)).foregroundColor(p.secondary)
            Toggle("土地详情使用等级色", isOn: config.landDetailTinted)
                .tint(p.emerald)
            if config.wrappedValue.landPreset == .custom {
                ForEach(1...6, id: \.self) { level in
                    ColorPicker("Lv\(level) 土地", selection: landColorBinding(level))
                }
                Text("未选色的等级会自动使用网页淡彩。")
                    .font(.system(size: 10)).foregroundColor(p.secondary)
            }
        }
    }

    private var ranchSection: some View {
        Section("牧场动物") {
            Picker("动物卡片", selection: config.ranchAnimalPreset) {
                ForEach(RanchAnimalColorPreset.allCases) { preset in
                    Text(preset.label).tag(preset)
                }
            }
            Text(config.wrappedValue.ranchAnimalPreset.detail)
                .font(.system(size: 10)).foregroundColor(p.secondary)
            Toggle("牧场主色跟随主色", isOn: config.ranchAccentFollowPrimary)
                .tint(p.emerald)
            if !config.wrappedValue.ranchAccentFollowPrimary {
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 10) {
                        ForEach(Array(ranchPresets.enumerated()), id: \.offset) { pair in
                            let item = pair.element
                            let hex = item.1
                            let selected = config.wrappedValue.ranchAccentHex == hex
                            Button {
                                config.wrappedValue.ranchAccentHex = hex
                            } label: {
                                VStack(spacing: 4) {
                                    Circle()
                                        .fill(hex.flatMap { Color(hexString: $0) } ?? p.ranchAccent)
                                        .frame(width: 30, height: 30)
                                        .overlay(Circle().stroke(selected ? p.text : p.separator, lineWidth: selected ? 2 : 0.7))
                                    Text(item.0).font(.system(size: 9)).foregroundColor(p.secondary)
                                }
                            }
                            .buttonStyle(.plain)
                        }
                    }
                    .padding(.vertical, 2)
                }
                ColorPicker("自定义牧场主色", selection: Binding(
                    get: { Color(hexString: config.wrappedValue.ranchAccentHex ?? "#FF9500") ?? p.ranchAccent },
                    set: { config.wrappedValue.ranchAccentHex = $0.hexString }
                ))
                Button("使用经典橙") { config.wrappedValue.ranchAccentHex = nil }
                    .font(.system(size: 12)).foregroundColor(p.ranchAccent)
            }
            if config.wrappedValue.ranchAnimalPreset == .custom {
                ColorPicker("窝 · 动物", selection: ranchColorBinding("coop", defaultHex: "#FDF7EE"))
                ColorPicker("棚 · 动物", selection: ranchColorBinding("shed", defaultHex: "#EFF8FD"))
                ColorPicker("巨大动物", selection: ranchColorBinding("giant", defaultHex: "#FFF8E1"))
            }
        }
    }

    private var layoutSection: some View {
        Section("布局与质感") {
            Picker("农场列数", selection: config.landGridColumns) {
                Text("3 列").tag(3)
                Text("4 列").tag(4)
            }
            Picker("牧场列数", selection: config.ranchAnimalColumns) {
                Text("2 列").tag(2)
                Text("3 列").tag(3)
            }
            Picker("进度条", selection: config.progressBarStyle) {
                ForEach(ProgressBarStyle.allCases) { style in
                    Text(style.label).tag(style)
                }
            }
            Picker("圆角", selection: config.cornerStyle) {
                ForEach(CornerStyle.allCases) { style in
                    Text(style.label).tag(style)
                }
            }
            Picker("卡片表面", selection: config.cardSurfaceStyle) {
                ForEach(CardSurfaceStyle.allCases) { style in
                    Text(style.label).tag(style)
                }
            }
        }
    }

    private func landColorBinding(_ level: Int) -> Binding<Color> {
        Binding(get: {
            let hex = config.wrappedValue.customLandColors["\(level)"] ?? p.soil(level).hexString
            return Color(hexString: hex) ?? p.soil(level)
        }, set: {
            config.wrappedValue.customLandColors["\(level)"] = $0.hexString
        })
    }

    private func ranchColorBinding(_ key: String, defaultHex: String) -> Binding<Color> {
        Binding(get: {
            let hex = config.wrappedValue.customRanchColors[key] ?? defaultHex
            return Color(hexString: hex) ?? p.card
        }, set: {
            config.wrappedValue.customRanchColors[key] = $0.hexString
        })
    }
}
