import Foundation
import SwiftUI

struct AccountManagerSheet: View {
    @Environment(\.palette) private var p
    @Environment(\.dismiss) private var dismiss
    @EnvironmentObject private var store: AppStore
    @State private var showAdd = false
    @State private var pendingDelete: AccountItem?
    @State private var editing: AccountItem?

    var body: some View {
        NavigationView {
            List {
                if store.accountsLoading && store.accounts.isEmpty {
                    HStack { Spacer(); ProgressView(); Spacer() }
                        .listRowBackground(Color.clear)
                }
                ForEach(store.accounts) { account in
                    Button {
                        Task {
                            await store.selectAccount(account.id)
                            dismiss()
                        }
                    } label: {
                        HStack(spacing: 12) {
                            AvatarView(url: account.avatarURL(base: store.serverURL),
                                       name: account.displayTitle, size: 44)
                            VStack(alignment: .leading, spacing: 4) {
                                HStack(spacing: 6) {
                                    Text(account.displayTitle)
                                        .font(.system(size: 15, weight: .semibold))
                                        .foregroundColor(p.text)
                                        .lineLimit(1)
                                    if account.id == store.currentAccountId {
                                        Image(systemName: "checkmark.circle.fill").foregroundColor(p.emerald)
                                    }
                                }
                                HStack(spacing: 6) {
                                    Pill(text: account.running ? "运行中" : "已停止",
                                         color: account.running ? p.green : p.secondary, dot: true)
                                    Pill(text: account.platformLabel,
                                         color: account.platformLabel == "微信" ? p.green : p.blue)
                                    Text("GID \(account.id)")
                                        .font(.system(size: 10)).foregroundColor(p.secondary)
                                }
                            }
                        }
                        .padding(.vertical, 3)
                    }
                    .buttonStyle(.plain)
                    .swipeActions(edge: .trailing, allowsFullSwipe: false) {
                        Button(role: .destructive) { pendingDelete = account } label: {
                            Label("删除", systemImage: "trash")
                        }
                        Button { editing = account } label: {
                            Label("编辑", systemImage: "pencil")
                        }.tint(p.blue)
                        if account.running {
                            Button { Task { await store.stopAccount(account.id) } } label: {
                                Label("停止", systemImage: "stop.fill")
                            }.tint(p.orange)
                        } else {
                            Button { Task { await store.startAccount(account.id) } } label: {
                                Label("启动", systemImage: "play.fill")
                            }.tint(p.green)
                        }
                    }
                }
                Section {
                    Button {
                        showAdd = true
                    } label: {
                        Label("添加农场账号", systemImage: "plus.circle.fill")
                            .foregroundColor(p.emerald)
                    }
                }
            }
            .scrollContentBackground(.hidden)
            .background(p.background)
            .navigationTitle("农场账号")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("完成") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button {
                        Task { await store.refreshAccounts() }
                    } label: { Image(systemName: "arrow.clockwise") }
                }
            }
            .sheet(isPresented: $showAdd) {
                AddAccountSheet().environmentObject(store)
            }
            .sheet(item: $editing) { account in
                EditAccountSheet(account: account).environmentObject(store)
            }
            .alert("删除账号", isPresented: Binding(
                get: { pendingDelete != nil },
                set: { if !$0 { pendingDelete = nil } }
            )) {
                Button("删除", role: .destructive) {
                    if let account = pendingDelete {
                        Task { await store.deleteAccount(account.id) }
                    }
                    pendingDelete = nil
                }
                Button("取消", role: .cancel) { pendingDelete = nil }
            } message: {
                Text("删除后该账号的登录信息会从服务器移除，确认继续？")
            }
        }
    }
}

struct AddAccountSheet: View {
    @Environment(\.palette) private var p
    @Environment(\.dismiss) private var dismiss
    @EnvironmentObject private var store: AppStore
    @State private var name = ""
    @State private var platform = "qq"
    @State private var code = ""
    @State private var mode = 0
    @State private var config = LoginConfigModel()
    @State private var busy = false
    @State private var error = ""
    @State private var wxTaskId = ""
    @State private var wxQr = ""
    @State private var wxStatus = ""
    @State private var qqTaskId = ""
    @State private var qqQr = ""
    @State private var qqStatus = ""

    var body: some View {
        NavigationView {
            Form {
                Section("账号") {
                    TextField("备注名称", text: $name)
                    if mode == 0 {
                        Picker("平台", selection: $platform) {
                            Text("QQ").tag("qq")
                            Text("微信").tag("wx")
                        }
                    }
                    Picker("登录方式", selection: $mode) {
                        Text("手动凭证").tag(0)
                        if config.wechatQrLogin { Text("微信扫码").tag(1) }
                        if config.qqQrLogin { Text("QQ 扫码").tag(2) }
                    }
                    .pickerStyle(.segmented)
                }

                if mode == 0 {
                    Section {
                        TextEditor(text: $code)
                            .frame(minHeight: 140)
                            .font(.system(size: 13, design: .monospaced))
                    } header: {
                        Text(platform == "qq" ? "QQ 登录凭证 / Cookie" : "微信登录凭证")
                    } footer: {
                        Text("凭证仅提交到你的自托管服务器保存，不会上传到第三方。")
                    }
                } else if mode == 1 {
                    Section("微信扫码") {
                        HStack {
                            Spacer()
                            QRCodeView(content: wxQr)
                            Spacer()
                        }
                        .listRowBackground(Color.clear)
                        Text(wxStatus.isEmpty ? "请使用微信扫一扫授权登录" : wxStatus)
                            .font(.system(size: 13)).foregroundColor(p.secondary)
                        Button("刷新二维码") { Task { await startWx() } }
                            .font(.system(size: 13, weight: .semibold)).foregroundColor(p.emerald)
                        Text("扫码确认后需要填写账号备注，再自动保存到服务器。")
                            .font(.system(size: 11)).foregroundColor(p.secondary)
                    }
                } else {
                    Section("QQ 扫码") {
                        HStack {
                            Spacer()
                            QRCodeView(content: qqQr)
                            Spacer()
                        }
                        .listRowBackground(Color.clear)
                        Text(qqStatus.isEmpty ? "请使用手机 QQ 扫码登录" : qqStatus)
                            .font(.system(size: 13)).foregroundColor(p.secondary)
                        Button("刷新二维码") { Task { await startQq() } }
                            .font(.system(size: 13, weight: .semibold)).foregroundColor(p.emerald)
                        Text("扫码确认后会获取授权 Code 并自动保存账号。")
                            .font(.system(size: 11)).foregroundColor(p.secondary)
                    }
                }

                if !error.isEmpty {
                    Section { ErrorBanner(message: error) }
                }
                if busy {
                    Section { HStack { Spacer(); ProgressView(); Spacer() } }
                }
            }
            .scrollContentBackground(.hidden)
            .background(p.background)
            .navigationTitle("添加账号")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button("取消") { dismiss() } }
                if mode == 0 {
                    ToolbarItem(placement: .confirmationAction) {
                        Button("保存") { saveManual() }
                            .disabled(busy || name.trimmingCharacters(in: .whitespaces).isEmpty || code.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                    }
                }
            }
            .task { await loadConfig() }
            .task(id: mode) { await switchMode() }
            .task(id: wxTaskId) { await pollWx(wxTaskId) }
            .task(id: qqTaskId) { await pollQq(qqTaskId) }
            .onDisappear { Task { await cancelAll() } }
        }
    }

    @MainActor
    private func loadConfig() async {
        if let cfg = try? await store.api.fetchLoginConfig() {
            config = cfg
            if mode == 1 && !cfg.wechatQrLogin { mode = 0 }
            if mode == 2 && !cfg.qqQrLogin { mode = 0 }
        }
    }

    @MainActor
    private func switchMode() async {
        error = ""
        if mode != 1 {
            let id = wxTaskId
            wxTaskId = ""; wxQr = ""; wxStatus = ""
            if !id.isEmpty { await store.api.cancelWxQrTask(id) }
        }
        if mode != 2 {
            let id = qqTaskId
            qqTaskId = ""; qqQr = ""; qqStatus = ""
            if !id.isEmpty { await store.api.cancelQqQrTask(id) }
        }
        if mode == 1 { await startWx() }
        if mode == 2 { await startQq() }
    }

    @MainActor
    private func startWx() async {
        let old = wxTaskId
        wxTaskId = ""; wxQr = ""; wxStatus = "正在获取微信二维码…"; error = ""
        if !old.isEmpty { await store.api.cancelWxQrTask(old) }
        do {
            let task = try await store.api.createWxQrTask()
            guard !task.taskId.isEmpty else { throw APIError(message: "服务端未返回任务 ID") }
            wxTaskId = task.taskId
            wxQr = task.qrUrl
            wxStatus = "请使用微信扫一扫授权登录"
        } catch let e as APIError {
            error = e.message; wxStatus = ""
        } catch {
            self.error = error.localizedDescription; wxStatus = ""
        }
    }

    @MainActor
    private func startQq() async {
        let old = qqTaskId
        qqTaskId = ""; qqQr = ""; qqStatus = "正在获取 QQ 登录二维码…"; error = ""
        if !old.isEmpty { await store.api.cancelQqQrTask(old) }
        do {
            let task = try await store.api.createQqQrTask()
            guard !task.taskId.isEmpty else { throw APIError(message: "服务端未返回任务 ID") }
            qqTaskId = task.taskId
            qqQr = task.qrImage
            qqStatus = "请使用手机 QQ 扫码登录"
        } catch let e as APIError {
            error = e.message; qqStatus = ""
        } catch {
            self.error = error.localizedDescription; qqStatus = ""
        }
    }

    @MainActor
    private func pollWx(_ taskId: String) async {
        guard !taskId.isEmpty else { return }
        while !Task.isCancelled && wxTaskId == taskId {
            try? await Task.sleep(nanoseconds: 1_400_000_000)
            if Task.isCancelled || wxTaskId != taskId { return }
            do {
                let task = try await store.api.pollWxQrStatus(taskId)
                guard wxTaskId == taskId else { return }
                switch task.status {
                case "waiting": wxStatus = "等待微信扫码…"
                case "scanned": wxStatus = "已扫码，请在手机微信上确认"
                case "authorized":
                    let cleanName = name.trimmingCharacters(in: .whitespacesAndNewlines)
                    guard !cleanName.isEmpty else {
                        wxStatus = "已确认授权，请先填写账号备注"
                        continue
                    }
                    wxStatus = "正在建立会话…"
                    try await store.api.confirmWxQr(taskId)
                    let claimed = try await store.api.claimWxQrCode(taskId)
                    let saved = await store.addAccount(name: cleanName, platform: "wx", code: claimed,
                                                       loginType: "wechat_qr", wxLoginTaskId: taskId)
                    wxTaskId = ""
                    if saved {
                        dismiss()
                    } else {
                        wxStatus = "账号保存失败，请查看提示后刷新二维码重试"
                    }
                    return
                case "cancelled", "expired", "failed":
                    wxStatus = "二维码已失效，请点击刷新重试"
                    return
                default: break
                }
            } catch let e as APIError {
                error = e.message
                return
            } catch {
                if !Task.isCancelled { self.error = error.localizedDescription }
                return
            }
        }
    }

    @MainActor
    private func pollQq(_ taskId: String) async {
        guard !taskId.isEmpty else { return }
        while !Task.isCancelled && qqTaskId == taskId {
            try? await Task.sleep(nanoseconds: 1_400_000_000)
            if Task.isCancelled || qqTaskId != taskId { return }
            do {
                let task = try await store.api.pollQqQrStatus(taskId)
                guard qqTaskId == taskId else { return }
                if !task.qrImage.isEmpty { qqQr = task.qrImage }
                switch task.status {
                case "waiting_scan": qqStatus = "等待 QQ 扫码…"
                case "scanned": qqStatus = "已扫码，请在手机 QQ 上确认"
                case "confirmed":
                    let cleanName = name.trimmingCharacters(in: .whitespacesAndNewlines)
                    guard !cleanName.isEmpty else {
                        qqStatus = "已确认授权，请先填写账号备注"
                        continue
                    }
                    qqStatus = "正在获取授权 Code…"
                    let claimed = try await store.api.claimQqQrCode(taskId)
                    guard !claimed.isEmpty else { throw APIError(message: "服务端未返回 QQ 登录 Code") }
                    let saved = await store.addAccount(name: cleanName, platform: "qq", code: claimed)
                    qqTaskId = ""
                    if saved {
                        dismiss()
                    } else {
                        qqStatus = "账号保存失败，请查看提示后刷新二维码重试"
                    }
                    return
                case "cancelled", "expired", "failed":
                    qqStatus = "二维码已失效，请点击刷新重试"
                    return
                default: break
                }
            } catch let e as APIError {
                error = e.message
                return
            } catch {
                if !Task.isCancelled { self.error = error.localizedDescription }
                return
            }
        }
    }

    @MainActor
    private func cancelAll() async {
        let wx = wxTaskId
        let qq = qqTaskId
        wxTaskId = ""; qqTaskId = ""
        if !wx.isEmpty { await store.api.cancelWxQrTask(wx) }
        if !qq.isEmpty { await store.api.cancelQqQrTask(qq) }
    }

    private func saveManual() {
        let cleanName = name.trimmingCharacters(in: .whitespacesAndNewlines)
        let cleanCode = code.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !cleanName.isEmpty, !cleanCode.isEmpty else { return }
        busy = true
        Task {
            let saved = await store.addAccount(name: cleanName, platform: platform, code: cleanCode)
            busy = false
            if saved { dismiss() }
        }
    }
}


struct EditAccountSheet: View {
    @Environment(\.palette) private var p
    @Environment(\.dismiss) private var dismiss
    @EnvironmentObject private var store: AppStore
    var account: AccountItem
    @State private var name = ""
    @State private var code = ""
    @State private var busy = false

    var body: some View {
        NavigationView {
            Form {
                Section("账号备注") {
                    TextField("备注名称", text: $name)
                }
                Section {
                    TextEditor(text: $code)
                        .frame(minHeight: 130)
                        .font(.system(size: 13, design: .monospaced))
                } header: {
                    Text("更换登录凭证（可留空）")
                } footer: {
                    Text("留空时只更新备注；填写新凭证后服务端会重启该账号并尝试登录。")
                }
                if busy {
                    Section { HStack { Spacer(); ProgressView(); Spacer() } }
                }
            }
            .scrollContentBackground(.hidden)
            .background(p.background)
            .navigationTitle("编辑账号")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button("取消") { dismiss() } }
                ToolbarItem(placement: .confirmationAction) {
                    Button("保存") {
                        let clean = name.trimmingCharacters(in: .whitespacesAndNewlines)
                        guard !clean.isEmpty else { return }
                        busy = true
                        Task {
                            let saved = await store.updateAccount(id: account.id, name: clean,
                                                                  code: code.trimmingCharacters(in: .whitespacesAndNewlines))
                            busy = false
                            if saved { dismiss() }
                        }
                    }
                    .disabled(busy || name.trimmingCharacters(in: .whitespaces).isEmpty)
                }
            }
            .onAppear { name = account.name.isEmpty ? account.nick : account.name }
        }
    }
}

struct ServerListView: View {
    @Environment(\.palette) private var p
    @Environment(\.dismiss) private var dismiss
    @EnvironmentObject private var store: AppStore
    @State private var showAdd = false
    @State private var renaming: ServerProfile?
    @State private var renameText = ""

    var body: some View {
        NavigationView {
            List {
                ForEach(store.profiles) { profile in
                    Button {
                        Task {
                            await store.selectProfile(profile.id)
                            dismiss()
                        }
                    } label: {
                        HStack(spacing: 12) {
                            Image(systemName: "server.rack")
                                .foregroundColor(profile.id == store.activeProfileId ? p.emerald : p.secondary)
                                .frame(width: 28)
                            VStack(alignment: .leading, spacing: 3) {
                                HStack(spacing: 6) {
                                    Text(profile.displayName)
                                        .font(.system(size: 15, weight: .semibold))
                                        .foregroundColor(p.text)
                                    if profile.id == store.activeProfileId {
                                        Pill(text: "当前", color: p.emerald, filled: true)
                                    }
                                }
                                Text(profile.serverURL)
                                    .font(.system(size: 11)).foregroundColor(p.secondary).lineLimit(1)
                                Text(profile.isAuthenticated ? "已授权 · \(profile.username.isEmpty ? "Token" : profile.username)" : "未授权")
                                    .font(.system(size: 10)).foregroundColor(profile.isAuthenticated ? p.green : p.orange)
                            }
                            Spacer()
                        }
                    }
                    .buttonStyle(.plain)
                    .swipeActions {
                        Button(role: .destructive) {
                            Task { await store.deleteProfile(profile.id) }
                        } label: { Label("删除", systemImage: "trash") }
                        Button {
                            renaming = profile
                            renameText = profile.name
                        } label: { Label("重命名", systemImage: "pencil") }
                        .tint(p.blue)
                    }
                }
                Section {
                    Button { showAdd = true } label: {
                        Label("添加服务器", systemImage: "plus.circle.fill").foregroundColor(p.emerald)
                    }
                }
            }
            .scrollContentBackground(.hidden)
            .background(p.background)
            .navigationTitle("远程服务器")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button("完成") { dismiss() } }
            }
            .sheet(isPresented: $showAdd) {
                ServerSetupView().environmentObject(store)
            }
            .alert("重命名服务器", isPresented: Binding(
                get: { renaming != nil },
                set: { if !$0 { renaming = nil } }
            )) {
                TextField("名称", text: $renameText)
                Button("保存") {
                    if let profile = renaming { store.renameProfile(profile.id, name: renameText) }
                    renaming = nil
                }
                Button("取消", role: .cancel) { renaming = nil }
            }
        }
    }
}
