import Foundation
import SwiftUI

private struct MallLoadContext: Equatable {
    var space: GameSpace
    var tab: Int
    var accountId: String
    var shopId: Int
    var slotType: Int
    var connected: Bool
}

private struct MallPending: Identifiable {
    let id = UUID()
    var accountId: String
    var action: String
    var input: [String: Any]
    var quote: RanchQuote
}

struct MallView: View {
    @Environment(\.palette) private var p
    @EnvironmentObject private var store: AppStore
    var showsTopBar = true
    @State private var tab = 0
    @State private var shopId = 5
    @State private var mallSlot = 6
    @State private var selectedShelf = -1
    @State private var autoPlace = false
    @State private var catalog: MallCatalog?
    @State private var mystery: MysteryShopState?
    @State private var ranch: RanchViewSnapshot?
    @State private var loading = false
    @State private var error = ""
    @State private var busy = false
    @State private var amountPrompt: AmountPrompt?
    @State private var pending: MallPending?
    @State private var loadGeneration = 0

    init(showsTopBar: Bool = true, initialTab: Int = 0) {
        self.showsTopBar = showsTopBar
        _tab = State(initialValue: initialTab)
    }

    private var isRanch: Bool { store.space == .ranch }
    private var loadContext: MallLoadContext {
        MallLoadContext(space: store.space, tab: tab, accountId: store.currentAccountId,
                        shopId: shopId, slotType: mallSlot, connected: store.status?.connected == true)
    }

    var body: some View {
        ScrollView {
            VStack(spacing: 12) {
                if showsTopBar { TopBar() }
                tabsRow.padding(.horizontal, 16)
                if !error.isEmpty { ErrorBanner(message: error).padding(.horizontal, 16) }
                if store.status == nil && store.overviewLoading {
                    Card { LoadingState(text: "正在读取账号状态") }.padding(.horizontal, 16)
                } else if store.status?.connected != true {
                    Card { EmptyStateView(icon: "wifi.slash", title: "账号未在线", subtitle: "请先启动当前账号后查看商店") }
                        .padding(.horizontal, 16)
                } else if loading && catalog == nil && ranch == nil && mystery == nil {
                    Card { LoadingState(text: "正在读取商城") }.padding(.horizontal, 16)
                } else if isRanch {
                    ranchContent.padding(.horizontal, 16)
                } else if tab == 4 {
                    mysteryContent.padding(.horizontal, 16)
                } else {
                    farmContent.padding(.horizontal, 16)
                }
                Color.clear.frame(height: 20)
            }
            .padding(.top, 4)
        }
        .background(p.background.ignoresSafeArea())
        .navigationBarTitleDisplayMode(.inline)
        .refreshable { await load() }
        .onChange(of: store.space) { _ in
            tab = 0; catalog = nil; ranch = nil; mystery = nil
            shopId = 5; mallSlot = 6; selectedShelf = -1; autoPlace = false
            amountPrompt = nil; pending = nil
        }
        .onChange(of: tab) { _ in
            // Do not show the previous tab's catalog while the new one loads.
            if isRanch {
                ranch = nil
            } else if tab == 4 {
                mystery = nil
            } else {
                catalog = nil
            }
            error = ""
        }
        .onChange(of: shopId) { _ in
            ranch = nil
            selectedShelf = -1
            error = ""
        }
        .onChange(of: mallSlot) { _ in
            ranch = nil
            error = ""
        }
        .task(id: loadContext) { await load() }
        .sheet(item: $amountPrompt) { AmountPromptView(prompt: $0) }
        .sheet(item: $pending) { item in
            QuotePromptView(prompt: QuotePrompt(quote: item.quote) {
                execute(item)
            })
        }
    }

    private var tabsRow: some View {
        let titles = isRanch ? ["牧场商店", "仓鼠集市", "礼包商城"] : ["游戏商城", "限时", "道具", "SVIP", "神秘"]
        return ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 6) {
                ForEach(titles.indices, id: \.self) { index in
                    let title = titles[index]
                    Button {
                        tab = index
                    } label: {
                        Text(title)
                            .font(.system(size: 12, weight: .semibold))
                            .foregroundColor(tab == index ? .white : p.secondary)
                            .padding(.horizontal, 12).padding(.vertical, 7)
                            .background(tab == index ? (isRanch ? p.orange : p.emerald) : p.card)
                            .clipShape(Capsule())
                    }
                    .buttonStyle(.plain)
                }
            }
        }
    }

    private var farmContent: some View {
        VStack(spacing: 10) {
            if let catalog {
                currenciesRow(catalog.currencies, tint: p.emerald)
                if catalog.goods.isEmpty {
                    Card { EmptyStateView(icon: "storefront", title: "当前商店暂无货品", actionTitle: "刷新", action: { Task { await load() } }) }
                } else {
                    ForEach(catalog.goods) { good in
                        farmGoodsRow(good)
                    }
                }
            } else {
                Card { EmptyStateView(icon: "storefront", title: "商城读取失败", subtitle: error, actionTitle: "重试", action: { Task { await load() } }) }
            }
        }
    }

    private func currenciesRow(_ currencies: [MallCurrency], tint: Color) -> some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 7) {
                ForEach(currencies) { c in
                    HStack(spacing: 3) {
                        Text(c.name).font(.system(size: 10)).foregroundColor(p.secondary)
                        Text(c.balanceKnown ? Fmt.compact(c.count) : "--").font(.system(size: 11, weight: .semibold)).foregroundColor(tint)
                    }
                    .padding(.horizontal, 8).padding(.vertical, 5)
                    .background(p.card).clipShape(Capsule())
                }
            }
        }
    }

    private func farmGoodsRow(_ good: MallGoods) -> some View {
        Card(padding: 12) {
            HStack(spacing: 10) {
                RemoteIcon(url: AssetURL.resolve(good.rewards.first?.image ?? good.priceImage, base: store.serverURL),
                           fallback: "shippingbox.fill", size: 42)
                VStack(alignment: .leading, spacing: 3) {
                    HStack(spacing: 5) {
                        Text(good.name).font(.system(size: 14, weight: .semibold)).foregroundColor(p.text)
                            .lineLimit(2).minimumScaleFactor(0.75).layoutPriority(1)
                        if good.isDiscounted { Pill(text: good.discountText.isEmpty ? "折扣" : good.discountText, color: p.red) }
                        if good.isLimited, let remaining = good.limit?.remaining {
                            Pill(text: "剩余 \(remaining)", color: p.orange)
                        }
                    }
                    Text(good.rewards.map { "\($0.name) ×\(Fmt.compact($0.count))" }.joined(separator: "、"))
                        .font(.system(size: 10)).foregroundColor(p.secondary).lineLimit(2)
                    HStack(spacing: 4) {
                        if good.isFree {
                            Text("免费").font(.system(size: 12, weight: .semibold)).foregroundColor(p.green)
                        } else {
                            Text(good.priceName).font(.system(size: 10)).foregroundColor(p.secondary)
                            Text(Fmt.compact(good.priceCount)).font(.system(size: 12, weight: .semibold)).foregroundColor(p.orange)
                            if let bal = good.priceBalance {
                                Text("/ \(Fmt.compact(bal))").font(.system(size: 9)).foregroundColor(p.secondary)
                            }
                        }
                    }
                }
                Spacer(minLength: 0)
                VStack(alignment: .trailing, spacing: 2) {
                    let reason = farmBuyReason(good)
                    if !reason.isEmpty {
                        Text(reason)
                            .font(.system(size: 9))
                            .foregroundColor(farmCanBuy(good) ? p.secondary : p.orange)
                            .lineLimit(2)
                            .multilineTextAlignment(.trailing)
                    }
                    Button(good.isFree ? "领取" : "购买") {
                        buyFarm(good)
                    }
                    .font(.system(size: 12, weight: .semibold)).foregroundColor(p.emerald)
                    .disabled(!farmCanBuy(good) || busy)
                }
            }
        }
    }

    private var mysteryContent: some View {
        VStack(spacing: 10) {
            if let m = mystery, m.active, let npc = m.npc {
                Card {
                    VStack(alignment: .leading, spacing: 10) {
                        HStack {
                            Text("神秘商人").font(.system(size: 17, weight: .semibold)).foregroundColor(p.text)
                            Pill(text: "限时", color: p.purple, filled: true)
                            Spacer()
                            if let expire = m.expireAt {
                                TimelineView(.periodic(from: .now, by: 1)) { context in
                                    let ended = expire <= context.date
                                    Text(ended ? "已结束" : Fmt.countdown(to: expire))
                                        .font(.system(size: 11))
                                        .foregroundColor(ended ? p.secondary : p.orange)
                                }
                            }
                        }
                        HStack(spacing: 10) {
                            RemoteIcon(url: AssetURL.resolve(npc.reward.image, base: store.serverURL), fallback: "gift.fill", size: 46)
                            VStack(alignment: .leading, spacing: 3) {
                                Text(npc.reward.name).font(.system(size: 14, weight: .semibold)).foregroundColor(p.text)
                                Text("×\(Fmt.compact(npc.reward.count))").font(.system(size: 11)).foregroundColor(p.secondary)
                                Text("\(npc.priceName) \(Fmt.compact(npc.priceCount))" + (npc.discountPercent > 0 ? " · \(npc.discountPercent)% 折扣" : ""))
                                    .font(.system(size: 11)).foregroundColor(p.orange)
                            }
                            Spacer()
                            Button("购买") { buyMystery(m, npc: npc) }
                                .font(.system(size: 12, weight: .semibold)).foregroundColor(p.emerald)
                                .disabled(busy || (m.expireAt.map { $0 <= Date() } ?? false))
                        }
                    }
                }
            } else {
                Card {
                    EmptyStateView(icon: "person.crop.circle.badge.questionmark", title: "神秘商人暂未来访",
                                   subtitle: "商人到访时会在这里显示限时折扣货品", actionTitle: "刷新", action: { Task { await load() } })
                }
            }
        }
    }

    private var ranchContent: some View {
        VStack(spacing: 10) {
            if tab == 0 || tab == 2 {
                Card {
                    VStack(spacing: 10) {
                        if tab == 0 {
                            Picker("牧场商店分类", selection: $shopId) {
                                Text("幼崽").tag(5)
                                Text("饲料").tag(6)
                            }
                            .pickerStyle(.segmented)
                            .disabled(busy)
                            if shopId == 5 {
                                Toggle("购买后自动放养", isOn: $autoPlace)
                                    .font(.system(size: 14)).foregroundColor(p.text)
                                    .tint(p.orange).disabled(busy)
                            }
                        } else {
                            Picker("礼包商城分类", selection: $mallSlot) {
                                Text("礼包商城").tag(6)
                                Text("特惠商城").tag(7)
                            }
                            .pickerStyle(.segmented)
                            .disabled(busy)
                        }
                    }
                }
            }
            if let ranch {
                if !ranch.base.unlocked {
                    Card { EmptyStateView(icon: "pawprint", title: "牧场暂未解锁", subtitle: ranch.base.reason) }
                } else {
                    if tab == 1 && !ranch.marketShelves.isEmpty {
                        ScrollView(.horizontal, showsIndicators: false) {
                            HStack(spacing: 6) {
                                marketShelfChip("全部", selected: selectedShelf < 0) { selectedShelf = -1 }
                                ForEach(ranch.marketShelves) { shelf in
                                    marketShelfChip(shelf.name, selected: selectedShelf == shelf.id) { selectedShelf = shelf.id }
                                }
                            }
                        }
                    }
                    currenciesRow(ranch.base.currencies.map { MallCurrency(id: $0.id, name: $0.name, image: $0.image, count: $0.count, balanceKnown: true) }, tint: p.orange)
                    let goods: [RanchShopGood] = {
                        switch tab {
                        case 1:
                            return selectedShelf < 0 ? ranch.marketGoods : ranch.marketGoods.filter { $0.shelfId == selectedShelf }
                        case 2: return ranch.canShopGoods
                        default: return ranch.shopGoods
                        }
                    }()
                    if goods.isEmpty {
                        Card { EmptyStateView(icon: "storefront", title: "当前商店暂无货品", actionTitle: "刷新", action: { Task { await load() } }) }
                    } else {
                        ForEach(goods) { good in
                            ranchGoodsRow(good, ranch: ranch)
                        }
                    }
                }
            } else {
                Card { EmptyStateView(icon: "storefront", title: "牧场商店读取失败", subtitle: error, actionTitle: "重试", action: { Task { await load() } }) }
            }
        }
    }

    private func marketShelfChip(_ title: String, selected: Bool, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Text(title)
                .font(.system(size: 12, weight: .semibold))
                .foregroundColor(selected ? .white : p.secondary)
                .padding(.horizontal, 12).padding(.vertical, 7)
                .background(selected ? p.emerald : p.card)
                .clipShape(Capsule())
        }
        .buttonStyle(.plain)
    }

    private func ranchGoodsRow(_ good: RanchShopGood, ranch: RanchViewSnapshot) -> some View {
        let balance = good.priceBalance ?? ranch.base.currencies.first(where: { $0.id == good.priceId })?.count
        let maxCount = ranchPurchaseLimit(good, balance: balance)
        return Card(padding: 12) {
            HStack(spacing: 10) {
                RemoteIcon(url: AssetURL.resolve(good.reward.image, base: store.serverURL), fallback: "shippingbox.fill", size: 42)
                VStack(alignment: .leading, spacing: 3) {
                    HStack(spacing: 5) {
                        Text(good.name).font(.system(size: 14, weight: .semibold)).foregroundColor(p.text)
                            .lineLimit(2).minimumScaleFactor(0.8).layoutPriority(1)
                        if good.free { Pill(text: "免费", color: p.green) }
                        if let remaining = good.remaining { Pill(text: "剩余 \(remaining)", color: p.orange) }
                    }
                    Text((good.rewards.isEmpty ? [good.reward] : good.rewards).map { "\($0.name) ×\(Fmt.compact($0.count))" }.joined(separator: "、"))
                        .font(.system(size: 10)).foregroundColor(p.secondary).lineLimit(2)
                    HStack(spacing: 4) {
                        Text(good.priceName).font(.system(size: 10)).foregroundColor(p.secondary)
                        Text(Fmt.compact(good.priceCount)).font(.system(size: 12, weight: .semibold)).foregroundColor(p.orange)
                        if let balance {
                            Text("/ \(Fmt.compact(balance))").font(.system(size: 9)).foregroundColor(p.secondary)
                        }
                    }
                    if !good.husbandryType.isEmpty {
                        Text("适合：\(good.husbandryType)").font(.system(size: 9)).foregroundColor(p.secondary)
                    }
                    if good.available && maxCount == 0 {
                        Text(good.free ? "暂不可领取" : (balance == nil ? "余额未确认" : (good.priceCount <= 0 ? "价格无效" : "余额不足")))
                            .font(.system(size: 9)).foregroundColor(p.orange)
                    }
                }
                Spacer(minLength: 0)
                Button(good.free ? "领取" : (tab == 1 ? "兑换" : "购买")) {
                    buyRanch(good, ranch: ranch)
                }
                .font(.system(size: 12, weight: .semibold)).foregroundColor(p.orange)
                .disabled(busy || loading || !good.available || maxCount == 0)
            }
        }
    }

    private func load() async {
        loadGeneration += 1
        let generation = loadGeneration
        let context = loadContext
        guard !context.accountId.isEmpty, context.connected else {
            loading = false
            return
        }
        loading = true
        error = ""
        do {
            if context.space == .ranch {
                let view = context.tab == 1 ? "market" : (context.tab == 2 ? "cans" : "shop")
                let shopId: Int? = context.tab == 0 ? context.shopId : (context.tab == 2 ? context.slotType : nil)
                let snapshot = try await store.api.fetchRanchView(accountId: context.accountId, view: view, shopId: shopId)
                guard generation == loadGeneration, context == loadContext else { return }
                ranch = snapshot
                catalog = nil; mystery = nil
            } else if context.tab == 4 {
                let state = try await store.api.fetchMysteryShop(accountId: context.accountId)
                guard generation == loadGeneration, context == loadContext else { return }
                mystery = state
                catalog = nil; ranch = nil
            } else {
                let slots = [1, 5, 0, 4]
                let slot = slots[min(context.tab, slots.count - 1)]
                let props = context.tab == 2
                let result = try await store.api.fetchMallCatalog(accountId: context.accountId, slotType: slot, propsShop: props)
                guard generation == loadGeneration, context == loadContext else { return }
                catalog = result
                ranch = nil; mystery = nil
            }
        } catch let e as APIError {
            guard generation == loadGeneration, context == loadContext else { return }
            error = e.message
        } catch let err {
            guard generation == loadGeneration, context == loadContext else { return }
            error = err.localizedDescription
        }
        loading = false
    }

    private func farmCanBuy(_ good: MallGoods) -> Bool {
        if good.purchaseStatus == "owned" { return false }
        if let remaining = good.limit?.remaining, remaining <= 0 { return false }
        if !good.purchasable { return false }
        if good.isFree { return true }
        guard good.priceId > 0, good.priceCount > 0, let balance = good.priceBalance else { return false }
        return balance >= good.priceCount
    }

    private func farmBuyReason(_ good: MallGoods) -> String {
        if good.purchaseStatus == "owned" { return "已拥有" }
        if let remaining = good.limit?.remaining, remaining <= 0 {
            return good.isFree ? "已领取" : "已售罄"
        }
        if !good.purchasable {
            return good.unavailableReason.isEmpty ? "暂不可购" : good.unavailableReason
        }
        if good.isFree { return "" }
        if good.priceId <= 0 || good.priceCount <= 0 { return "价格未确认" }
        guard let balance = good.priceBalance else { return "余额未确认" }
        if balance < good.priceCount { return "\(good.priceName)余额不足" }
        return ""
    }

    private func buyFarm(_ good: MallGoods) {
        let slots = [1, 5, 0, 4]
        let slot = slots[min(tab, slots.count - 1)]
        let props = tab == 2
        let limit = good.limit?.remaining ?? good.limit?.max
        let quota = max(0, min(limit ?? 99, 99))
        let affordable: Int
        if good.isFree {
            affordable = 99
        } else if good.priceCount > 0, let balance = good.priceBalance {
            affordable = max(0, Int(min(Int64(quota), balance / good.priceCount)))
        } else {
            affordable = 0
        }
        let maxCount = min(quota, affordable)
        guard maxCount > 0 else {
            store.showToast(farmBuyReason(good).isEmpty ? "当前不可购买" : farmBuyReason(good))
            return
        }
        amountPrompt = AmountPrompt(title: good.isFree ? "领取 \(good.name)" : "购买 \(good.name)",
                                    caption: "\(good.priceName) \(Fmt.compact(good.priceCount))",
                                    maxCount: maxCount, confirmLabel: "确认", unit: "个") { count in
            busy = true
            Task {
                do {
                    let result = try await store.api.purchaseMallGoods(accountId: store.currentAccountId, goods: good,
                                                                        slotType: slot, propsShop: props, count: count)
                    if let newCatalog = result.catalog { catalog = newCatalog }
                    store.showToast(result.rewards.isEmpty ? "操作成功" : result.rewards.map(\.text).joined(separator: "、"))
                    await load()
                } catch let e as APIError { store.showToast(e.message) }
                catch { store.showToast(error.localizedDescription) }
                busy = false
            }
        }
    }

    private func buyMystery(_ m: MysteryShopState, npc: MysteryNpcOffer) {
        busy = true
        Task {
            do {
                let reward = try await store.api.purchaseMystery(accountId: store.currentAccountId, npcId: npc.id, expectedOffer: m.raw)
                store.showToast(reward == nil ? "购买成功" : "已获得 \(reward!.name) ×\(reward!.count)")
                await load()
            } catch let e as APIError { store.showToast(e.message) }
            catch { store.showToast(error.localizedDescription) }
            busy = false
        }
    }

    private func buyRanch(_ good: RanchShopGood, ranch: RanchViewSnapshot) {
        let balance = good.priceBalance ?? ranch.base.currencies.first(where: { $0.id == good.priceId })?.count
        let maxCount = ranchPurchaseLimit(good, balance: balance)
        guard maxCount > 0 else {
            store.showToast(good.free ? "当前没有可领取数量" : (balance == nil ? "余额未确认，请刷新后重试" : (good.priceCount <= 0 ? "商品价格无效" : "余额不足")))
            return
        }
        let accountId = store.currentAccountId
        let context = loadContext
        amountPrompt = AmountPrompt(title: (good.free ? "领取 " : "购买 ") + good.name,
                                    caption: "\(good.priceName) \(Fmt.compact(good.priceCount))",
                                    maxCount: maxCount, confirmLabel: "下一步", unit: "个") { count in
            guard !busy, context == loadContext else { return }
            let action = context.tab == 1 ? "exchange" : (context.tab == 2 ? "buyCan" : "buy")
            var input: [String: Any] = ["goodsId": good.id, "count": count]
            if context.tab == 0 {
                input["shopId"] = context.shopId
                if context.shopId == 5 && !good.husbandryType.isEmpty {
                    input["autoPlace"] = autoPlace
                }
            } else if context.tab == 2 {
                input["slotType"] = context.slotType
            }
            busy = true
            Task {
                do {
                    let quote = try await store.api.quoteRanch(accountId: accountId, action: action, input: input)
                    guard accountId == store.currentAccountId, context == loadContext else {
                        busy = false
                        return
                    }
                    if quote.skipped { store.showToast(quote.message.isEmpty ? "当前不可购买" : quote.message) }
                    else { pending = MallPending(accountId: accountId, action: action, input: input, quote: quote) }
                } catch let e as APIError { store.showToast(e.message) }
                catch { store.showToast(error.localizedDescription) }
                busy = false
            }
        }
    }

    private func execute(_ item: MallPending) {
        guard item.accountId == store.currentAccountId else {
            pending = nil
            return
        }
        pending = nil
        busy = true
        Task {
            do {
                let result = try await store.api.executeRanch(accountId: item.accountId, action: item.action,
                                                               input: item.input, signature: item.quote.signature)
                let fallback = result.status == "skipped" ? "当前没有可操作项目" : "购买成功"
                store.showToast(result.notice.isEmpty ? fallback : result.notice)
                await load()
            } catch let e as APIError { store.showToast(e.message.isEmpty ? "购买失败" : e.message) }
            catch { store.showToast(error.localizedDescription) }
            busy = false
        }
    }

    private func ranchPurchaseLimit(_ good: RanchShopGood, balance: Int64?) -> Int {
        let quota = max(0, min(good.maxCount, good.remaining ?? good.maxCount))
        if good.free { return quota }
        guard let balance, good.priceCount > 0 else { return 0 }
        return max(0, Int(min(Int64(quota), balance / good.priceCount)))
    }
}
