import Foundation
import SwiftUI

struct ServerSetupView: View {
    @Environment(\.palette) private var p
    @Environment(\.dismiss) private var dismiss
    @EnvironmentObject private var store: AppStore
    @State private var server = ""
    @State private var username = ""
    @State private var password = ""
    @State private var token = ""
    @State private var mode = 0
    @State private var busy = false
    @State private var error = ""
    @State private var versionText = ""
    @State private var showPassword = false

    // 当前这版 IPA 面向 43.142.187.232 的部署，首屏预填地址与管理员用户名，
    // 避免用户手动输入时漏掉 http:// 或输错 IP。密码仍需用户自己填写。
    private let presetServer = "http://43.142.187.232"
    private let presetUsername = "admin"

    var body: some View {
        ZStack {
            p.background.ignoresSafeArea()
            ScrollView {
                VStack(spacing: 18) {
                    header
                    connectionCard
                    savedServers
                    footer
                }
                .padding(.horizontal, 20)
                .padding(.top, 28)
                .padding(.bottom, 40)
            }
        }
        .navigationBarHidden(true)
        .onAppear {
            if server.isEmpty, let active = store.activeProfile { server = active.serverURL }
            if server.isEmpty { server = presetServer }
            if username.isEmpty { username = presetUsername }
        }
    }

    private var header: some View {
        VStack(spacing: 12) {
            ZStack {
                RoundedRectangle(cornerRadius: 22, style: .continuous)
                    .fill(LinearGradient(colors: [p.emerald, p.green], startPoint: .topLeading, endPoint: .bottomTrailing))
                    .frame(width: 82, height: 82)
                Image(systemName: "leaf.fill")
                    .font(.system(size: 38, weight: .bold))
                    .foregroundColor(.white)
            }
            .shadow(color: p.emerald.opacity(0.25), radius: 18, y: 8)
            Text("农场助手").font(.system(size: 30, weight: .bold)).foregroundColor(p.text)
            Text("连接你的自托管 qq-farm-bot")
                .font(.system(size: 14)).foregroundColor(p.secondary)
        }
        .padding(.bottom, 4)
    }

    private var connectionCard: some View {
        Card {
            VStack(alignment: .leading, spacing: 14) {
                FieldLabel(text: "远程服务器地址")
                HStack(spacing: 8) {
                    TextField("https://farm.example.com", text: $server)
                        .keyboardType(.URL)
                        .textInputAutocapitalization(.never)
                        .autocorrectionDisabled()
                        .font(.system(size: 15))
                        .foregroundColor(p.text)
                    Button {
                        Task { await testConnection() }
                    } label: {
                        Image(systemName: "bolt.horizontal.circle.fill")
                            .font(.system(size: 22))
                            .foregroundColor(p.emerald)
                    }
                    .disabled(server.isEmpty || busy)
                }
                .padding(12)
                .background(p.elevated)
                .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))

                if !versionText.isEmpty {
                    Label(versionText, systemImage: "checkmark.seal.fill")
                        .font(.system(size: 12, weight: .medium)).foregroundColor(p.green)
                }

                Picker("授权方式", selection: $mode) {
                    Text("账号密码").tag(0)
                    Text("x-admin-token").tag(1)
                }
                .pickerStyle(.segmented)

                if mode == 0 {
                    VStack(spacing: 10) {
                        TextField("管理员用户名", text: $username)
                            .textInputAutocapitalization(.never)
                            .autocorrectionDisabled()
                            .padding(12).background(p.elevated)
                            .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                        HStack {
                            Group {
                                if showPassword {
                                    TextField("管理员密码", text: $password)
                                } else {
                                    SecureField("管理员密码", text: $password)
                                }
                            }
                            .textInputAutocapitalization(.never)
                            .autocorrectionDisabled()
                            Button { showPassword.toggle() } label: {
                                Image(systemName: showPassword ? "eye.slash" : "eye")
                                    .foregroundColor(p.secondary)
                            }
                        }
                        .padding(12).background(p.elevated)
                        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                    }
                } else {
                    SecureField("粘贴 x-admin-token", text: $token)
                        .textInputAutocapitalization(.never)
                        .autocorrectionDisabled()
                        .padding(12).background(p.elevated)
                        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                    Text("可在 Web 管理面板登录后，从浏览器开发者工具中复制 x-admin-token。")
                        .font(.system(size: 11)).foregroundColor(p.secondary)
                }

                if !error.isEmpty { ErrorBanner(message: error) }

                PrimaryButton(title: busy ? "正在连接…" : "连接服务器", icon: "arrow.right.circle.fill", enabled: !busy && canSubmit) {
                    Task { await submit() }
                }
            }
        }
    }

    private var canSubmit: Bool {
        guard !server.trimmingCharacters(in: .whitespaces).isEmpty else { return false }
        return mode == 0 ? (!username.isEmpty && !password.isEmpty) : !token.isEmpty
    }

    @ViewBuilder
    private var savedServers: some View {
        if !store.profiles.isEmpty {
            VStack(alignment: .leading, spacing: 10) {
                SectionTitle(title: "已保存的服务器")
                ForEach(store.profiles) { profile in
                    Button {
                        server = profile.serverURL
                        if profile.isAuthenticated {
                            Task {
                                await store.selectProfile(profile.id)
                                dismiss()
                            }
                        } else {
                            username = profile.username
                        }
                    } label: {
                        HStack(spacing: 12) {
                            Image(systemName: "server.rack")
                                .foregroundColor(p.emerald)
                                .frame(width: 24)
                            VStack(alignment: .leading, spacing: 2) {
                                Text(profile.displayName).font(.system(size: 14, weight: .semibold)).foregroundColor(p.text)
                                Text(profile.serverURL).font(.system(size: 11)).foregroundColor(p.secondary).lineLimit(1)
                            }
                            Spacer()
                            Image(systemName: "chevron.right").font(.system(size: 12)).foregroundColor(p.secondary)
                        }
                        .padding(14)
                        .background(p.card)
                        .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
                    }
                    .buttonStyle(.plain)
                }
            }
        }
    }

    private var footer: some View {
        VStack(spacing: 6) {
            Text("支持 Android 端同款 qq-farm-bot 服务端")
                .font(.system(size: 11)).foregroundColor(p.secondary)
            Text("iOS 16+ · SwiftUI 原生实现")
                .font(.system(size: 10)).foregroundColor(p.secondary.opacity(0.8))
        }
        .padding(.top, 8)
    }

    private func testConnection() async {
        guard let normalized = FarmAPI.normalizeBaseURL(server) else {
            error = "请输入有效的服务器地址"
            return
        }
        busy = true
        error = ""
        versionText = ""
        do {
            let info = try await store.api.fetchGameVersion(baseOverride: normalized)
            versionText = "已连接 · Bot \(info.botVersion) · 客户端 \(info.clientVersion)"
        } catch let e as APIError {
            error = e.message
        } catch {
            self.error = error.localizedDescription
        }
        busy = false
    }

    private func submit() async {
        busy = true
        error = ""
        let message: String?
        if mode == 0 {
            message = await store.login(server: server, username: username, password: password)
        } else {
            message = await store.loginWithToken(server: server, token: token)
        }
        busy = false
        if let message {
            error = message
        } else {
            dismiss()
        }
    }
}
