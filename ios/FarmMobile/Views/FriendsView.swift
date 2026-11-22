import Foundation
import SwiftUI

private let friendsOfflineMessage = "账号未在线，请先启动当前账号"

private struct FriendsLoadContext: Equatable {
    var accountId: String
    var space: GameSpace
    var isOnline: Bool
}

struct FriendsView: View {
    @Environment(\.palette) private var p
    @EnvironmentObject private var store: AppStore
    @State private var friends: [FriendItem] = []
    @State private var ranchFriends: [RanchFriendSummary] = []
    @State private var loading = false
    @State private var error = ""
    @State private var filter = 0
    @State private var selectedFriend: FriendItem?
    @State private var selectedRanchFriend: RanchFriendSummary?
    @State private var showInteractions = false
    @State private var showBlacklist = false
    @State private var busy = false
    @State private var loadGeneration = 0

    private var filteredFriends: [FriendItem] {
        switch filter {
        case 1: return friends.filter { $0.stealNum > 0 }
        case 2: return friends.filter { $0.needsCareCount > 0 }
        default: return friends
        }
    }

    var body: some View {
        ScrollView {
            VStack(spacing: 14) {
                TopBar()
                if store.status == nil && store.overviewLoading {
                    Card { LoadingState(text: "正在读取账号状态") }.padding(.horizontal, 16)
                } else if store.isAccountOnline {
                    header.padding(.horizontal, 16)
                    if !error.isEmpty { ErrorBanner(message: error).padding(.horizontal, 16) }
                    if store.space == .farm { farmFilters.padding(.horizontal, 16) }
                    content.padding(.horizontal, 16)
                } else {
                    Card {
                        EmptyStateView(icon: "person.crop.circle.badge.exclamationmark", title: friendsOfflineMessage, subtitle: "账号上线后将自动同步好友")
                    }
                    .padding(.horizontal, 16)
                }
                Color.clear.frame(height: 20)
            }
            .padding(.top, 4)
        }
        .background(p.background.ignoresSafeArea())
        .navigationBarHidden(true)
        .refreshable { await loadFriends() }
        .onChange(of: store.space) { _ in filter = 0 }
        .task(id: FriendsLoadContext(accountId: store.currentAccountId, space: store.space, isOnline: store.isAccountOnline)) {
            await loadFriends()
        }
        .sheet(item: $selectedFriend) { friend in
            FriendLandsSheet(friend: friend).environmentObject(store)
        }
        .sheet(item: $selectedRanchFriend) { friend in
            RanchFriendSheet(friend: friend).environmentObject(store)
        }
        .sheet(isPresented: $showInteractions) {
            InteractionsSheet().environmentObject(store)
        }
        .sheet(isPresented: $showBlacklist) {
            BlacklistSheet().environmentObject(store)
        }
    }

    private var header: some View {
        Card {
            HStack {
                VStack(alignment: .leading, spacing: 2) {
                    Text(store.space == .farm ? "农场好友" : "牧场好友")
                        .font(.system(size: 20, weight: .bold)).foregroundColor(p.text)
                    Text(store.space == .farm ? "偷菜、帮忙、查看土地与访客记录" : "偷取产物、清理、放蚊子与抚摸动物")
                        .font(.system(size: 11)).foregroundColor(p.secondary)
                }
                Spacer()
                Menu {
                    Button { showInteractions = true } label: { Label("访客记录", systemImage: "clock.arrow.circlepath") }
                        .disabled(!store.isAccountOnline)
                    if store.space == .farm {
                        Button { showBlacklist = true } label: { Label("黑名单", systemImage: "person.crop.circle.badge.xmark") }
                            .disabled(!store.isAccountOnline)
                    }
                } label: {
                    Image(systemName: "ellipsis.circle.fill").font(.system(size: 22)).foregroundColor(p.secondary)
                }
            }
        }
    }

    private var farmFilters: some View {
        HStack(spacing: 8) {
            filterPill(0, "全部", friends.count)
            filterPill(1, "可偷", friends.filter { $0.stealNum > 0 }.count)
            filterPill(2, "可帮", friends.filter { $0.needsCareCount > 0 }.count)
            Spacer()
        }
    }

    private func filterPill(_ index: Int, _ title: String, _ count: Int) -> some View {
        Button {
            filter = index
        } label: {
            HStack(spacing: 4) {
                Text(title)
                Text("\(count)").font(.system(size: 10, weight: .bold))
                    .foregroundColor(filter == index ? p.green : p.secondary)
                    .padding(.horizontal, 5).padding(.vertical, 1)
                    .background((filter == index ? p.green : p.secondary).opacity(0.12))
                    .clipShape(Capsule())
            }
            .font(.system(size: 13, weight: .semibold))
            .foregroundColor(filter == index ? .white : p.text)
            .padding(.horizontal, 12).padding(.vertical, 7)
            .background(filter == index ? (store.space == .farm ? p.emerald : p.orange) : p.card)
            .clipShape(Capsule())
        }
        .buttonStyle(.plain)
    }

    @ViewBuilder
    private var content: some View {
        if loading && (friends.isEmpty && ranchFriends.isEmpty) {
            Card { LoadingState(text: "正在读取好友") }
        } else if store.space == .farm {
            if filteredFriends.isEmpty {
                Card { EmptyStateView(icon: "person.2.slash", title: "没有匹配的好友", subtitle: "启动账号后好友列表会自动同步") }
            } else {
                LazyVStack(spacing: 8) {
                    ForEach(filteredFriends) { friend in
                        farmFriendRow(friend)
                    }
                }
            }
        } else {
            if ranchFriends.isEmpty {
                Card { EmptyStateView(icon: "pawprint", title: "暂无牧场好友", subtitle: "启动账号后同步好友牧场数据") }
            } else {
                LazyVStack(spacing: 8) {
                    ForEach(ranchFriends) { friend in
                        ranchFriendRow(friend)
                    }
                }
            }
        }
    }

    private func farmFriendRow(_ friend: FriendItem) -> some View {
        Card(padding: 12) {
            HStack(spacing: 10) {
                Button {
                    selectedFriend = friend
                } label: {
                    HStack(spacing: 10) {
                        AvatarView(url: AssetURL.resolve(friend.avatarUrl, base: store.serverURL), name: friend.name, size: 44)
                        VStack(alignment: .leading, spacing: 4) {
                            HStack(spacing: 5) {
                                Text(friend.name).font(.system(size: 14, weight: .semibold)).foregroundColor(p.text)
                            .lineLimit(1).minimumScaleFactor(0.75)
                                Pill(text: "Lv.\(friend.level)", color: p.secondary)
                            }
                            HStack(spacing: 4) {
                                if friend.stealNum > 0 { Pill(text: "可偷 \(friend.stealNum)", color: p.green) }
                                if friend.dryNum > 0 { Pill(text: "旱 \(friend.dryNum)", color: p.blue) }
                                if friend.weedNum > 0 { Pill(text: "草 \(friend.weedNum)", color: p.orange) }
                                if friend.insectNum > 0 { Pill(text: "虫 \(friend.insectNum)", color: p.red) }
                            }
                        }
                    }
                }
                .buttonStyle(.plain)
                Spacer(minLength: 4)
                Menu {
                    Button("偷取") { operateFriend(friend, "steal") }.disabled(friend.stealNum <= 0)
                    Button("帮务农") { operateFriend(friend, "farming") }.disabled(friend.needsCareCount <= 0)
                    Button("捣乱", role: .destructive) { operateFriend(friend, "bad") }
                    Button("查看土地") { selectedFriend = friend }
                    Button("移入/移出黑名单") { toggleBlacklist(friend) }
                } label: {
                    Image(systemName: "ellipsis.circle").font(.system(size: 20)).foregroundColor(p.secondary)
                }
                .disabled(!store.isAccountOnline)
            }
        }
    }

    private func ranchFriendRow(_ friend: RanchFriendSummary) -> some View {
        Card(padding: 12) {
            Button { selectedRanchFriend = friend } label: {
                HStack(spacing: 10) {
                    AvatarView(url: AssetURL.resolve(friend.avatarUrl, base: store.serverURL), name: friend.name, size: 44)
                    VStack(alignment: .leading, spacing: 4) {
                        Text(friend.name).font(.system(size: 14, weight: .semibold)).foregroundColor(p.text)
                        HStack(spacing: 4) {
                            if friend.canHelp { Pill(text: "可帮忙 \(friend.helpCount)", color: p.green) }
                            else { Pill(text: "暂无可帮", color: p.secondary) }
                        }
                    }
                    Spacer()
                    Image(systemName: "chevron.right").font(.system(size: 12)).foregroundColor(p.secondary)
                }
            }
            .buttonStyle(.plain)
        }
    }

    private func loadFriends() async {
        loadGeneration += 1
        let generation = loadGeneration
        let accountId = store.currentAccountId
        let space = store.space
        guard store.isAccountOnline else {
            friends = []
            ranchFriends = []
            error = ""
            loading = false
            return
        }
        loading = true
        error = ""
        do {
            if space == .farm {
                let result = try await store.api.fetchFriends(accountId: accountId)
                guard generation == loadGeneration, accountId == store.currentAccountId, space == store.space, store.isAccountOnline else { return }
                friends = result
                ranchFriends = []
            } else {
                let snap = try await store.api.fetchRanchView(accountId: accountId, view: "friends")
                guard generation == loadGeneration, accountId == store.currentAccountId, space == store.space, store.isAccountOnline else { return }
                ranchFriends = snap.friends
                friends = []
            }
        } catch let e as APIError {
            guard generation == loadGeneration, accountId == store.currentAccountId, space == store.space else { return }
            error = e.message
        } catch let err {
            guard generation == loadGeneration, accountId == store.currentAccountId, space == store.space else { return }
            error = err.localizedDescription
        }
        guard generation == loadGeneration, accountId == store.currentAccountId, space == store.space else { return }
        loading = false
    }

    private func operateFriend(_ friend: FriendItem, _ op: String) {
        guard store.isAccountOnline else { return }
        guard !busy else { return }
        busy = true
        Task {
            do {
                let message = try await store.api.operateFriend(accountId: store.currentAccountId, gid: friend.gid, op: op)
                store.showToast(message)
                await loadFriends()
            } catch let e as APIError { store.showToast(e.message) }
            catch { store.showToast(error.localizedDescription) }
            busy = false
        }
    }

    private func toggleBlacklist(_ friend: FriendItem) {
        guard store.isAccountOnline else { return }
        Task {
            do {
                let list = try await store.api.toggleBlacklist(accountId: store.currentAccountId, gid: friend.gid)
                store.showToast(list.contains(where: { $0.gid == friend.gid }) ? "已加入黑名单" : "已移出黑名单")
            } catch let e as APIError { store.showToast(e.message) }
            catch { store.showToast(error.localizedDescription) }
        }
    }
}

struct FriendLandsSheet: View {
    @Environment(\.palette) private var p
    @Environment(\.dismiss) private var dismiss
    @EnvironmentObject private var store: AppStore
    var friend: FriendItem
    @State private var data = FriendLandsPayload()
    @State private var loading = true
    @State private var busy = false
    @State private var error = ""

    var body: some View {
        NavigationView {
            ScrollView {
                VStack(spacing: 12) {
                    HStack(spacing: 10) {
                        AvatarView(url: AssetURL.resolve(friend.avatarUrl, base: store.serverURL), name: friend.name, size: 50)
                        VStack(alignment: .leading, spacing: 3) {
                            Text(friend.name).font(.system(size: 20, weight: .bold)).foregroundColor(p.text)
                            HStack(spacing: 5) {
                                Pill(text: "Lv.\(friend.level)", color: p.secondary)
                                Pill(text: "可偷 \(data.stealableCount)", color: p.green)
                                Pill(text: "待护理 \(data.dryCount + data.weedCount + data.insectCount)", color: p.orange)
                            }
                        }
                        Spacer()
                    }
                    HStack(spacing: 8) {
                        PrimaryButton(title: "偷取", icon: "hand.raised.fill", tint: p.green, enabled: store.isAccountOnline && !busy && data.stealableCount > 0) {
                            operate("steal")
                        }
                        PrimaryButton(title: "帮务农", icon: "drop.fill", tint: p.blue, enabled: store.isAccountOnline && !busy && data.needsCare) {
                            operate("farming")
                        }
                    }
                    if data.canDisrupt {
                        PrimaryButton(title: "捣乱", icon: "exclamationmark.bubble.fill", tint: p.red,
                                      enabled: store.isAccountOnline && !busy) {
                            operate("bad")
                        }
                    }
                    if store.isAccountOnline && !error.isEmpty { ErrorBanner(message: error) }
                    if !store.isAccountOnline {
                        Card { EmptyStateView(icon: "person.crop.circle.badge.exclamationmark", title: friendsOfflineMessage, subtitle: "请启动账号后重新加载好友土地") }
                    } else if loading && data.payload.lands.isEmpty {
                        LoadingState(text: "正在读取好友土地")
                    } else {
                        LazyVGrid(columns: [GridItem(.flexible(), spacing: 8), GridItem(.flexible(), spacing: 8), GridItem(.flexible(), spacing: 8)], spacing: 8) {
                            ForEach(data.payload.lands) { land in
                                VStack(alignment: .leading, spacing: 4) {
                                    Text("#\(land.id)").font(.system(size: 10, weight: .bold)).foregroundColor(p.soil(land.level))
                                    Spacer(minLength: 0)
                                    Text(land.plantName.isEmpty ? land.statusLabel : land.plantName)
                                        .font(.system(size: 11, weight: .semibold)).foregroundColor(p.text).lineLimit(1)
                                    Text(land.status == "stealable" ? "可偷取" : (["harvestable", "ripe", "mature"].contains(land.status) ? "已成熟" : land.phaseName))
                                        .font(.system(size: 9)).foregroundColor(p.secondary).lineLimit(1)
                                    HStack(spacing: 3) {
                                        if land.needWater { Text("水").font(.system(size: 8)).foregroundColor(p.blue) }
                                        if land.needWeed { Text("草").font(.system(size: 8)).foregroundColor(p.orange) }
                                        if land.needBug { Text("虫").font(.system(size: 8)).foregroundColor(p.red) }
                                    }
                                }
                                .padding(8)
                                .frame(height: 88)
                                .background(p.card)
                                .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                            }
                        }
                    }
                }
                .padding(16)
            }
            .background(p.background.ignoresSafeArea())
            .navigationTitle("好友土地")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button("关闭") { dismiss() } }
                ToolbarItem(placement: .confirmationAction) {
                    Button { Task { await load() } } label: { Image(systemName: "arrow.clockwise") }
                }
            }
            .task(id: FriendsLoadContext(accountId: store.currentAccountId, space: store.space, isOnline: store.isAccountOnline)) { await load() }
        }
    }

    private func load() async {
        guard store.isAccountOnline else {
            data = FriendLandsPayload()
            error = friendsOfflineMessage
            loading = false
            return
        }
        loading = true
        error = ""
        do { data = try await store.api.fetchFriendLands(accountId: store.currentAccountId, gid: friend.gid) }
        catch let e as APIError { error = e.message }
        catch let err { error = err.localizedDescription }
        loading = false
    }

    private func operate(_ op: String) {
        guard store.isAccountOnline else { return }
        busy = true
        Task {
            do {
                let message = try await store.api.operateFriend(accountId: store.currentAccountId, gid: friend.gid, op: op)
                store.showToast(message)
                await load()
            } catch let e as APIError { error = e.message }
            catch let err { error = err.localizedDescription }
            busy = false
        }
    }
}

private struct FriendPending: Identifiable {
    let id = UUID()
    var accountId: String
    var action: String
    var input: [String: Any]
    var quote: RanchQuote
}

struct RanchFriendSheet: View {
    @Environment(\.palette) private var p
    @Environment(\.dismiss) private var dismiss
    @EnvironmentObject private var store: AppStore
    var friend: RanchFriendSummary
    @State private var snapshot: RanchViewSnapshot?
    @State private var loading = true
    @State private var busy = false
    @State private var error = ""
    @State private var pending: FriendPending?

    var detail: RanchFriendDetail? { snapshot?.friendRanch }

    var body: some View {
        NavigationView {
            ScrollView {
                VStack(spacing: 12) {
                    HStack(spacing: 10) {
                        AvatarView(url: AssetURL.resolve(friend.avatarUrl, base: store.serverURL), name: friend.name, size: 50)
                        VStack(alignment: .leading, spacing: 3) {
                            Text(friend.name).font(.system(size: 20, weight: .bold)).foregroundColor(p.text)
                            HStack(spacing: 5) {
                                if let d = detail {
                                    Pill(text: "Lv.\(d.level)", color: p.secondary)
                                    Pill(text: "可偷 \(d.stealableShelves)", color: p.green)
                                    Pill(text: "可清理 \(d.cleanableHazards)", color: p.orange)
                                    if !d.sceneRestored { Pill(text: "场景恢复中", color: p.orange) }
                                }
                            }
                        }
                        Spacer()
                    }
                    if let d = detail {
                        HStack(spacing: 8) {
                            PrimaryButton(title: "清理 (\(d.cleanableHazards))", icon: "sparkles", tint: p.blue, enabled: store.isAccountOnline && !busy && d.cleanableHazards > 0) {
                                quote("cleanFriend", ["hostGid": friend.gid])
                            }
                            PrimaryButton(title: d.canSteal ? "偷取 (\(d.stealableShelves))" : "暂无可偷",
                                          icon: "hand.raised.fill", tint: p.orange, enabled: store.isAccountOnline && !busy && d.canSteal) {
                                quote("steal", ["hostGid": friend.gid])
                            }
                        }
                        if !d.notice.isEmpty { ErrorBanner(message: d.notice) }
                        if !d.sceneRestored { ErrorBanner(message: "好友牧场场景恢复中，偷取暂不可用，请稍后刷新") }
                        if let career = d.career {
                            Card {
                                HStack {
                                    Text("牧场生涯").font(.system(size: 13, weight: .semibold)).foregroundColor(p.text)
                                    Spacer()
                                    Text("收获 \(Fmt.compact(career.harvest)) · 成熟 \(Fmt.compact(career.mature)) · 收偷比 \(career.ratioText)")
                                        .font(.system(size: 11)).foregroundColor(p.secondary)
                                }
                            }
                        }
                        ForEach(d.ranches) { pen in
                            Card(padding: 12) {
                                VStack(alignment: .leading, spacing: 8) {
                                    HStack {
                                        Text(pen.name).font(.system(size: 14, weight: .semibold)).foregroundColor(p.text)
                                        Pill(text: "蚊子 \(pen.mosquitoes)/\(pen.mosquitoLimit)", color: pen.mosquitoes > 0 ? p.indigo : p.secondary)
                                        Spacer()
                                        Button("放蚊子") {
                                            quote("putMosquito", ["hostGid": friend.gid, "ranchId": pen.id])
                                        }
                                        .font(.system(size: 11, weight: .semibold)).foregroundColor(p.indigo)
                                        .disabled(!store.isAccountOnline || busy || pen.mosquitoRoom <= 0 || d.mosquitoRemaining <= 0)
                                    }
                                    ForEach(pen.animals) { animal in
                                        HStack(spacing: 8) {
                                            RemoteIcon(url: AssetURL.resolve(animal.image, base: store.serverURL), fallback: "pawprint.fill", size: 32)
                                            VStack(alignment: .leading, spacing: 1) {
                                                Text(animal.name).font(.system(size: 12, weight: .medium)).foregroundColor(p.text)
                                                Text([animal.stageName, animal.weightKg].filter { !$0.isEmpty }.joined(separator: " · "))
                                                    .font(.system(size: 9)).foregroundColor(p.secondary)
                                            }
                                            Spacer()
                                            if animal.canStroke {
                                                Button("抚摸") {
                                                    quote("stroke", ["hostGid": friend.gid, "ranchId": pen.id, "animalId": animal.id])
                                                }
                                                .font(.system(size: 11, weight: .semibold)).foregroundColor(p.orange)
                                                .disabled(!store.isAccountOnline || busy || d.strokeRemaining <= 0)
                                            } else if animal.stroked {
                                                Pill(text: "已抚摸", color: p.green)
                                            }
                                        }
                                    }
                                    let stealable = pen.shelves.filter { $0.canSteal }
                                    if !stealable.isEmpty {
                                        Text("货架可偷 \(stealable.count) 个").font(.system(size: 10)).foregroundColor(p.green)
                                    }
                                }
                            }
                        }
                    }
                    if !store.isAccountOnline {
                        Card { EmptyStateView(icon: "person.crop.circle.badge.exclamationmark", title: friendsOfflineMessage, subtitle: "请启动账号后重新加载好友牧场") }
                    }
                    if store.isAccountOnline && !error.isEmpty { ErrorBanner(message: error) }
                    if loading && snapshot == nil { LoadingState(text: "正在读取好友牧场") }
                }
                .padding(16)
            }
            .background(p.background.ignoresSafeArea())
            .navigationTitle("好友牧场")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button("关闭") { dismiss() } }
                ToolbarItem(placement: .confirmationAction) {
                    Button { Task { await load() } } label: { Image(systemName: "arrow.clockwise") }
                }
            }
            .task(id: FriendsLoadContext(accountId: store.currentAccountId, space: store.space, isOnline: store.isAccountOnline)) { await load() }
            .onChange(of: store.isAccountOnline) { online in
                if !online { pending = nil }
            }
            .sheet(item: $pending) { item in
                QuotePromptView(prompt: QuotePrompt(quote: item.quote) {
                    execute(item)
                })
            }
        }
    }

    private func load() async {
        guard store.isAccountOnline else {
            snapshot = nil
            error = friendsOfflineMessage
            loading = false
            return
        }
        loading = true
        error = ""
        do {
            snapshot = try await store.api.fetchRanchView(accountId: store.currentAccountId, view: "friends", hostGid: friend.gid)
        } catch let e as APIError { error = e.message }
        catch let err { error = err.localizedDescription }
        loading = false
    }

    private func quote(_ action: String, _ input: [String: Any]) {
        guard store.isAccountOnline, !busy, !loading, error.isEmpty else { return }
        let accountId = store.currentAccountId
        busy = true
        Task {
            do {
                let q = try await store.api.quoteRanch(accountId: accountId, action: action, input: input)
                guard accountId == store.currentAccountId, store.isAccountOnline else { busy = false; return }
                if q.skipped { store.showToast(q.message.isEmpty ? "当前没有可操作项目" : q.message) }
                else { pending = FriendPending(accountId: accountId, action: action, input: input, quote: q) }
            } catch let e as APIError { store.showToast(e.message) }
            catch { store.showToast(error.localizedDescription) }
            busy = false
        }
    }

    private func execute(_ item: FriendPending) {
        pending = nil
        guard store.isAccountOnline, item.accountId == store.currentAccountId else { return }
        busy = true
        Task {
            do {
                let result = try await store.api.executeRanch(accountId: item.accountId,
                                                               action: item.action, input: item.input,
                                                               signature: item.quote.signature)
                store.showToast(result.notice.isEmpty ? "操作完成" : result.notice)
                await load()
            } catch let e as APIError { store.showToast(e.message) }
            catch { store.showToast(error.localizedDescription) }
            busy = false
        }
    }
}

struct InteractionsSheet: View {
    @Environment(\.palette) private var p
    @Environment(\.dismiss) private var dismiss
    @EnvironmentObject private var store: AppStore
    @State private var records: [InteractRecord] = []
    @State private var loading = true
    @State private var error = ""

    var body: some View {
        NavigationView {
            List {
                if !store.isAccountOnline {
                    Text(friendsOfflineMessage).foregroundColor(p.secondary)
                } else if loading && records.isEmpty {
                    HStack { Spacer(); ProgressView(); Spacer() }
                } else if records.isEmpty {
                    Text(error.isEmpty ? "暂无访客记录" : error).foregroundColor(p.secondary)
                } else {
                    ForEach(records) { record in
                        HStack(spacing: 10) {
                            AvatarView(url: AssetURL.resolve(record.friendAvatar, base: store.serverURL), name: record.friendName, size: 34)
                            VStack(alignment: .leading, spacing: 2) {
                                Text(record.friendName).font(.system(size: 14, weight: .semibold)).foregroundColor(p.text)
                                Text(record.actionText).font(.system(size: 11)).foregroundColor(p.secondary)
                            }
                            Spacer()
                            Text(record.timeText).font(.system(size: 10)).foregroundColor(p.secondary)
                        }
                    }
                }
            }
            .scrollContentBackground(.hidden)
            .background(p.background)
            .navigationTitle("访客记录")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button("关闭") { dismiss() } }
            }
            .task(id: FriendsLoadContext(accountId: store.currentAccountId, space: store.space, isOnline: store.isAccountOnline)) { await load() }
        }
    }

    private func load() async {
        guard store.isAccountOnline else {
            records = []
            error = friendsOfflineMessage
            loading = false
            return
        }
        loading = true
        error = ""
        do { records = try await store.api.fetchInteractRecords(accountId: store.currentAccountId) }
        catch let e as APIError { error = e.message }
        catch let err { error = err.localizedDescription }
        loading = false
    }
}

struct BlacklistSheet: View {
    @Environment(\.palette) private var p
    @Environment(\.dismiss) private var dismiss
    @EnvironmentObject private var store: AppStore
    @State private var items: [FriendBlacklistItem] = []
    @State private var loading = true
    @State private var error = ""

    var body: some View {
        NavigationView {
            List {
                if !store.isAccountOnline {
                    Text(friendsOfflineMessage).foregroundColor(p.secondary)
                } else if loading && items.isEmpty {
                    HStack { Spacer(); ProgressView(); Spacer() }
                } else if items.isEmpty {
                    Text(error.isEmpty ? "黑名单为空" : error).foregroundColor(p.secondary)
                } else {
                    ForEach(items) { item in
                        HStack(spacing: 10) {
                            AvatarView(url: AssetURL.resolve(item.avatarUrl, base: store.serverURL), name: item.name, size: 34)
                            VStack(alignment: .leading, spacing: 2) {
                                Text(item.name).font(.system(size: 14, weight: .semibold)).foregroundColor(p.text)
                                Text("GID \(item.gid)").font(.system(size: 10)).foregroundColor(p.secondary)
                            }
                            Spacer()
                            Button("移出") { toggle(item.gid) }
                                .font(.system(size: 12, weight: .semibold)).foregroundColor(p.emerald)
                                .disabled(!store.isAccountOnline)
                        }
                    }
                }
            }
            .scrollContentBackground(.hidden)
            .background(p.background)
            .navigationTitle("好友黑名单")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button("关闭") { dismiss() } }
            }
            .task(id: FriendsLoadContext(accountId: store.currentAccountId, space: store.space, isOnline: store.isAccountOnline)) { await load() }
        }
    }

    private func load() async {
        guard store.isAccountOnline else {
            items = []
            error = friendsOfflineMessage
            loading = false
            return
        }
        loading = true
        error = ""
        do { items = try await store.api.fetchBlacklist(accountId: store.currentAccountId) }
        catch let e as APIError { error = e.message }
        catch let err { error = err.localizedDescription }
        loading = false
    }

    private func toggle(_ gid: Int64) {
        guard store.isAccountOnline else { return }
        Task {
            do {
                items = try await store.api.toggleBlacklist(accountId: store.currentAccountId, gid: gid)
            } catch let e as APIError { error = e.message }
            catch let err { error = err.localizedDescription }
        }
    }
}
