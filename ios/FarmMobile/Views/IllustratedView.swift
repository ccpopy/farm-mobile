import Foundation
import SwiftUI

struct IllustratedView: View {
    @Environment(\.palette) private var p
    @EnvironmentObject private var store: AppStore
    @State private var tab = 0
    @State private var illustrated = IllustratedSnapshot()
    @State private var collections = CollectionSnapshot()
    @State private var loading = false
    @State private var error = ""
    @State private var busy = false
    @State private var amountPrompt: AmountPrompt?
    @State private var selectedItem: CollectionItem?

    var body: some View {
        ScrollView {
            VStack(spacing: 12) {
                HStack(spacing: 4) {
                    ForEach(Array(["作物图鉴", "超变图鉴", "蓝晶藏品"].indices), id: \.self) { index in
                        Button {
                            tab = index
                        } label: {
                            Text(["作物图鉴", "超变图鉴", "蓝晶藏品"][index])
                                .font(.system(size: 13, weight: .semibold))
                                .foregroundColor(tab == index ? .white : p.secondary)
                                .frame(maxWidth: .infinity).padding(.vertical, 8)
                                .background(tab == index ? p.emerald : Color.clear)
                                .clipShape(Capsule())
                        }
                        .buttonStyle(.plain)
                    }
                }
                .padding(3).background(p.card).clipShape(Capsule())
                if !error.isEmpty { ErrorBanner(message: error) }
                if store.status == nil && store.overviewLoading {
                    Card { LoadingState(text: "正在读取账号状态") }
                } else if !store.isAccountOnline {
                    Card {
                        EmptyStateView(icon: "wifi.slash", title: "账号未在线",
                                       subtitle: "请先在总览启动当前账号，图鉴与藏品进度需要在线读取")
                    }
                } else if loading && illustrated.crop.items.isEmpty && collections.series.isEmpty {
                    Card { LoadingState(text: "正在读取图鉴") }
                } else {
                    switch tab {
                    case 0: bookView(illustrated.crop)
                    case 1: bookView(illustrated.mutant)
                    default: collectionsView
                    }
                }
            }
            .padding(16)
        }
        .background(p.background.ignoresSafeArea())
        .navigationTitle("图鉴")
        .navigationBarTitleDisplayMode(.inline)
        .task(id: store.currentAccountId) { await load() }
        .task(id: store.status?.connected) {
            if store.isAccountOnline { await load() }
        }
        .sheet(item: $amountPrompt) { AmountPromptView(prompt: $0) }
        .sheet(item: $selectedItem) { item in
            CollectionItemSheet(item: item, snapshot: collections) { action, body, label in
                await operateCollection(action, body: body, label: label)
            }
            .environmentObject(store)
        }
    }

    private func bookView(_ book: IllustratedBook) -> some View {
        VStack(spacing: 10) {
            Card {
                VStack(alignment: .leading, spacing: 8) {
                    HStack {
                        Text(book.type == 1 ? "作物图鉴" : "超变图鉴")
                            .font(.system(size: 18, weight: .bold)).foregroundColor(p.text)
                        Pill(text: "Lv.\(book.level)", color: p.emerald)
                        Spacer()
                        if book.claimable {
                            Button("领取奖励") { claim(book.type) }
                                .font(.system(size: 12, weight: .semibold)).foregroundColor(p.emerald)
                                .disabled(busy)
                        }
                    }
                    ProgressLine(value: book.nextLevelProgress > 0 ? Double(book.progress) / Double(book.nextLevelProgress) : 0)
                    Text("收集进度 \(book.progress) / \(book.nextLevelProgress)")
                        .font(.system(size: 11)).foregroundColor(p.secondary)
                }
            }
            LazyVGrid(columns: [GridItem(.flexible(), spacing: 8), GridItem(.flexible(), spacing: 8), GridItem(.flexible(), spacing: 8)], spacing: 8) {
                ForEach(book.items) { item in
                    Card(padding: 9) {
                        VStack(alignment: .leading, spacing: 5) {
                            RemoteIcon(url: AssetURL.resolve(item.image, base: store.serverURL), fallback: "leaf.fill", size: 38)
                            Text(item.name).font(.system(size: 11, weight: .semibold)).foregroundColor(p.text).lineLimit(1)
                            Text(item.unlocked ? "已解锁" : "未解锁")
                                .font(.system(size: 9)).foregroundColor(item.unlocked ? p.green : p.secondary)
                            Text("\(item.progress)").font(.system(size: 9)).foregroundColor(p.secondary)
                            if let seed = item.shopSeed, !seed.soldOut && !seed.locked {
                                Button("买种子") { buySeed(seed) }
                                    .font(.system(size: 10, weight: .semibold)).foregroundColor(p.emerald)
                                    .disabled(busy)
                            }
                        }
                        .frame(maxWidth: .infinity, alignment: .leading)
                    }
                }
            }
        }
    }

    private var collectionsView: some View {
        VStack(spacing: 10) {
            Card {
                VStack(alignment: .leading, spacing: 8) {
                    HStack {
                        Text("蓝晶藏品").font(.system(size: 18, weight: .bold)).foregroundColor(p.text)
                        Pill(text: "\(collections.score) 分", color: p.indigo)
                        Spacer()
                        Button("领取评分奖励") {
                            Task { await operateCollection("claim", body: [:], label: "领取评分奖励") }
                        }
                        .font(.system(size: 12, weight: .semibold)).foregroundColor(p.indigo)
                        .disabled(busy || !collections.milestones.contains(where: { $0.reached && !$0.claimed }))
                    }
                    Text("已激活 \(collections.activatedCount)/\(collections.totalCount)")
                        .font(.system(size: 11)).foregroundColor(p.secondary)
                    ForEach(collections.milestones) { m in
                        HStack {
                            Text("\(m.score) 分").font(.system(size: 11, weight: .medium)).foregroundColor(p.text)
                            Text(m.rewards.map(\.name).joined(separator: "、")).font(.system(size: 9)).foregroundColor(p.secondary).lineLimit(1)
                            Spacer()
                            Pill(text: m.claimed ? "已领取" : (m.reached ? "可领取" : "未达成"),
                                 color: m.claimed ? p.secondary : (m.reached ? p.green : p.orange))
                        }
                    }
                }
            }
            ForEach(collections.series) { series in
                Card {
                    VStack(alignment: .leading, spacing: 8) {
                        HStack {
                            Text(series.name).font(.system(size: 15, weight: .semibold)).foregroundColor(p.text)
                            Pill(text: series.rarityName, color: p.indigo)
                            Spacer()
                            Text("\(series.activatedCount)/\(series.totalCount)")
                                .font(.system(size: 11)).foregroundColor(p.secondary)
                        }
                        ForEach(series.items) { item in
                            Button { selectedItem = item } label: {
                                HStack(spacing: 8) {
                                    RemoteIcon(url: AssetURL.resolve(item.image, base: store.serverURL), fallback: "sparkles", size: 34)
                                    VStack(alignment: .leading, spacing: 1) {
                                        Text(item.name).font(.system(size: 12, weight: .medium)).foregroundColor(p.text)
                                        Text(String(repeating: "★", count: max(0, item.starLevel)) + String(repeating: "☆", count: max(0, item.maxStarLevel - item.starLevel)))
                                            .font(.system(size: 9)).foregroundColor(p.amber)
                                    }
                                    Spacer()
                                    if item.displayed { Pill(text: "陈列中", color: p.indigo) }
                                    if item.canUpgrade { Pill(text: "可升星", color: p.green) }
                                    Image(systemName: "chevron.right").font(.system(size: 10)).foregroundColor(p.secondary)
                                }
                                .padding(.vertical, 3)
                            }
                            .buttonStyle(.plain)
                        }
                    }
                }
            }
        }
    }

    private func claim(_ type: Int) {
        guard store.isAccountOnline else { store.showToast("账号未在线，请先启动账号"); return }
        guard !busy else { return }
        busy = true
        Task {
            do {
                let (snapshot, message) = try await store.api.claimIllustrated(accountId: store.currentAccountId, type: type)
                if let snapshot { illustrated = snapshot }
                store.showToast(message)
                await load()
            } catch let e as APIError { store.showToast(e.message) }
            catch { store.showToast(error.localizedDescription) }
            busy = false
        }
    }

    private func buySeed(_ seed: IllustratedShopSeed) {
        guard store.isAccountOnline else { store.showToast("账号未在线，请先启动账号"); return }
        amountPrompt = AmountPrompt(title: "购买 \(seed.name)",
                                    caption: "价格 \(seed.price) · 每份 \(seed.itemCount) 个",
                                    maxCount: 99, confirmLabel: "购买", unit: "份") { count in
            busy = true
            Task {
                do {
                    let received = try await store.api.purchaseIllustratedSeed(accountId: store.currentAccountId,
                                                                                goodsId: seed.goodsId, seedId: seed.seedId, count: count)
                    store.showToast("已获得 \(received) 个 \(seed.name)")
                    await load()
                } catch let e as APIError { store.showToast(e.message) }
                catch { store.showToast(error.localizedDescription) }
                busy = false
            }
        }
    }

    private func operateCollection(_ action: String, body: [String: Any], label: String) async {
        guard store.isAccountOnline else { store.showToast("账号未在线，请先启动账号"); return }
        guard !busy else { return }
        busy = true
        do {
            let result = try await store.api.operateCollections(accountId: store.currentAccountId, action: action, body: body, label: label)
            collections = result.snapshot
            store.showToast(result.message)
        } catch let e as APIError { store.showToast(e.message) }
        catch { store.showToast(error.localizedDescription) }
        busy = false
    }

    private func load() async {
        let accountId = store.currentAccountId
        guard !accountId.isEmpty, store.isAccountOnline else { return }
        loading = true
        error = ""
        do {
            async let ill = store.api.fetchIllustrated(accountId: accountId)
            async let col = store.api.fetchCollections(accountId: accountId)
            let newIllustrated = try await ill
            let newCollections = try await col
            guard store.currentAccountId == accountId, store.isAccountOnline else { return }
            illustrated = newIllustrated
            collections = newCollections
        } catch let e as APIError {
            if store.currentAccountId == accountId { error = e.message }
        } catch let err {
            if store.currentAccountId == accountId { error = err.localizedDescription }
        }
        loading = false
    }
}

struct CollectionItemSheet: View {
    @Environment(\.palette) private var p
    @Environment(\.dismiss) private var dismiss
    @EnvironmentObject private var store: AppStore
    var item: CollectionItem
    var snapshot: CollectionSnapshot
    var onOperate: (String, [String: Any], String) async -> Void
    @State private var busy = false

    var body: some View {
        NavigationView {
            ScrollView {
                VStack(alignment: .leading, spacing: 14) {
                    HStack(spacing: 12) {
                        RemoteIcon(url: AssetURL.resolve(item.image, base: store.serverURL), fallback: "sparkles", size: 64)
                        VStack(alignment: .leading, spacing: 3) {
                            Text(item.name).font(.system(size: 22, weight: .bold)).foregroundColor(p.text)
                            Text(String(repeating: "★", count: max(0, item.starLevel)) + String(repeating: "☆", count: max(0, item.maxStarLevel - item.starLevel)))
                                .font(.system(size: 13)).foregroundColor(p.amber)
                            Text("持有 \(Fmt.compact(item.ownCount))").font(.system(size: 11)).foregroundColor(p.secondary)
                        }
                    }
                    Text(item.description).font(.system(size: 13)).foregroundColor(p.text)
                    if !item.nextStarCosts.isEmpty {
                        Card {
                            VStack(alignment: .leading, spacing: 6) {
                                Text("下一星消耗").font(.system(size: 13, weight: .semibold)).foregroundColor(p.text)
                                ForEach(item.nextStarCosts) { cost in
                                    HStack {
                                        Text(cost.name).font(.system(size: 12)).foregroundColor(p.text)
                                        Spacer()
                                        Text("×\(Fmt.compact(cost.count))").font(.system(size: 12)).foregroundColor(p.orange)
                                    }
                                }
                            }
                        }
                    }
                    PrimaryButton(title: item.canUpgrade ? "升星 / 激活" : "暂不可升星",
                                  icon: "arrow.up.circle.fill", tint: p.indigo, enabled: !busy && item.canUpgrade) {
                        busy = true
                        Task {
                            await onOperate("upgrade", ["collectionId": item.id], "升星成功")
                            busy = false
                            dismiss()
                        }
                    }
                    PrimaryButton(title: item.displayed ? "取消陈列" : "设为陈列",
                                  icon: "sparkles", tint: p.emerald, enabled: !busy && item.activated) {
                        busy = true
                        let body: [String: Any] = [
                            "collectionId": item.id,
                            "displayed": !item.displayed,
                            "expectedDisplayItemId": snapshot.displayItemId,
                        ]
                        Task {
                            await onOperate("display", body, item.displayed ? "已取消陈列" : "已设为陈列")
                            busy = false
                            dismiss()
                        }
                    }
                }
                .padding(20)
            }
            .background(p.background.ignoresSafeArea())
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button("关闭") { dismiss() } }
            }
        }
    }
}
