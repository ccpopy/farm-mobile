import Foundation
import SwiftUI

private struct PlaceTarget: Identifiable {
    let id = UUID()
    let item: RanchBagItem
}

private struct PendingRanch: Identifiable {
    let id = UUID()
    var accountId: String
    var action: String
    var input: [String: Any]
    var quote: RanchQuote
}

private struct RanchLoadContext: Equatable {
    var accountId: String
    var isOnline: Bool
    var viewTab: Int
}

struct RanchWorkspaceView: View {
    @Environment(\.palette) private var p
    @EnvironmentObject private var store: AppStore
    @State private var viewTab = 0
    @State private var snapshot: RanchViewSnapshot?
    @State private var loading = false
    @State private var error = ""
    @State private var busy = false
    @State private var pending: PendingRanch?
    @State private var amountPrompt: AmountPrompt?
    @State private var placeTarget: PlaceTarget?
    @State private var showFeed = false
    @State private var feedAnimal: RanchAnimal?
    @State private var feedBucket = 0
    @State private var produceAfterFeed = true
    @State private var loadTask: Task<Void, Never>?

    private var viewName: String { ["animals", "facilities", "bag", "tasks"][viewTab] }
    private var loadContext: RanchLoadContext {
        RanchLoadContext(accountId: store.currentAccountId, isOnline: store.isAccountOnline, viewTab: viewTab)
    }
    private var canOperate: Bool { store.isAccountOnline && !busy }
    private var allAnimals: [RanchAnimal] { snapshot?.base.pens.flatMap(\.animals) ?? [] }

    var body: some View {
        ScrollView {
            VStack(spacing: 14) {
                TopBar()
                if store.status == nil && store.overviewLoading {
                    Card { LoadingState(text: "正在读取账号状态") }.padding(.horizontal, 16)
                } else if store.isAccountOnline {
                    if !error.isEmpty {
                        ErrorBanner(message: error).padding(.horizontal, 16)
                    }
                    statsCard.padding(.horizontal, 16)
                    subTabs.padding(.horizontal, 16)
                    content.padding(.horizontal, 16)
                } else {
                    Card {
                        EmptyStateView(icon: "wifi.slash", title: "账号未在线，请先启动当前账号")
                    }
                    .padding(.horizontal, 16)
                }
                Color.clear.frame(height: 20)
            }
            .padding(.top, 4)
        }
        .background(p.background.ignoresSafeArea())
        .navigationBarHidden(true)
        .refreshable { await load() }
        .onChange(of: viewTab) { _ in
            snapshot = nil
            error = ""
        }
        .task(id: loadContext) {
            loadTask?.cancel()
            loadTask = Task { await load() }
            await loadTask?.value
        }
        .onDisappear {
            loadTask?.cancel()
            loadTask = nil
        }
        .sheet(item: $pending) { item in
            QuotePromptView(prompt: QuotePrompt(quote: item.quote) {
                execute(item)
            })
        }
        .sheet(item: $amountPrompt) { AmountPromptView(prompt: $0) }
        .sheet(item: $placeTarget) { target in
            RanchPlaceSheet(item: target.item, facilities: snapshot?.base.facilities ?? []) { pen, count in
                place(target.item, pen: pen, count: count)
            }
            .environmentObject(store)
        }
        .sheet(isPresented: $showFeed) {
            RanchFeedSheet(buckets: snapshot?.base.buckets ?? [], animals: allAnimals, animal: feedAnimal,
                           initialBucket: feedBucket) { animal, bucket, produce in
                Task { await feed(animal: animal, bucket: bucket, produce: produce) }
            }
        }
    }

    private var statsCard: some View {
        let base = snapshot?.base ?? RanchPayload()
        return Card {
            VStack(alignment: .leading, spacing: 10) {
                HStack {
                    Text("牧场工作台").font(.system(size: 20, weight: .bold)).foregroundColor(p.text)
                    Pill(text: "Lv.\(base.level)", color: p.ranchAccent)
                    Spacer()
                    Button { Task { await load() } } label: {
                        Image(systemName: "arrow.clockwise").foregroundColor(p.ranchAccent)
                    }
                }
                if !base.unlocked {
                    Text(base.reason.isEmpty ? "牧场暂未解锁" : base.reason)
                        .font(.system(size: 12)).foregroundColor(p.ranchAccent)
                } else {
                    HStack(spacing: 10) {
                        ranchMetric("牧草", Fmt.compact(base.grass), p.green)
                        ranchMetric("待处理", "\(base.hazardsCount)", base.hazardsCount > 0 ? p.ranchAccent : p.secondary)
                        ranchMetric("可收", "\(base.shelvesCollectableCount)", p.emerald)
                        ranchMetric("动物", "\(base.pens.reduce(0) { $0 + $1.animals.count })", p.ranchAccent)
                    }
                    if base.neededExp > 0 {
                        ProgressLine(value: Double(base.currentExp) / Double(base.neededExp), color: p.ranchAccent, height: 6)
                        Text("经验 \(Fmt.full(base.currentExp)) / \(Fmt.full(base.neededExp))")
                            .font(.system(size: 10)).foregroundColor(p.secondary)
                    }
                    let currencies = base.currencies.filter { $0.count != 0 }.prefix(4)
                    if !currencies.isEmpty {
                        HStack(spacing: 8) {
                            ForEach(Array(currencies)) { currency in
                                HStack(spacing: 3) {
                                    Text(currency.name).font(.system(size: 10)).foregroundColor(p.secondary)
                                    Text(Fmt.compact(currency.count)).font(.system(size: 11, weight: .semibold)).foregroundColor(p.text)
                                }
                                .padding(.horizontal, 7).padding(.vertical, 4)
                                .background(p.elevated).clipShape(Capsule())
                            }
                        }
                    }
                    if !base.notice.isEmpty {
                        Text(base.notice).font(.system(size: 11)).foregroundColor(p.ranchAccent)
                    }
                }
            }
        }
    }

    private func ranchMetric(_ title: String, _ value: String, _ color: Color) -> some View {
        VStack(spacing: 2) {
            Text(value).font(.system(size: 16, weight: .bold)).foregroundColor(color)
            Text(title).font(.system(size: 10)).foregroundColor(p.secondary)
        }
        .frame(maxWidth: .infinity).padding(.vertical, 6)
        .background(p.elevated).clipShape(RoundedRectangle(cornerRadius: 9, style: .continuous))
    }

    private var subTabs: some View {
        HStack(spacing: 4) {
            ForEach(Array(["动物", "设施", "背包", "任务"].indices), id: \.self) { index in
                let title = ["动物", "设施", "背包", "任务"][index]
                Button {
                    withAnimation(.easeOut(duration: 0.15)) { viewTab = index }
                } label: {
                    Text(title)
                        .font(.system(size: 13, weight: .semibold))
                        .foregroundColor(viewTab == index ? .white : p.secondary)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 8)
                        .background(viewTab == index ? p.ranchAccent : Color.clear)
                        .clipShape(Capsule())
                }
                .buttonStyle(.plain)
            }
        }
        .padding(3)
        .background(p.card)
        .clipShape(Capsule())
    }

    @ViewBuilder
    private var content: some View {
        if loading && snapshot == nil {
            Card { LoadingState(text: "正在读取牧场") }
        } else if let snapshot, !snapshot.base.unlocked {
            Card { EmptyStateView(icon: "pawprint", title: "牧场暂未解锁", subtitle: snapshot.base.reason.isEmpty ? "达到解锁等级后可在此管理牧场" : snapshot.base.reason) }
        } else {
            switch viewTab {
            case 0: animalsTab
            case 1: facilitiesTab
            case 2: bagTab
            default: tasksTab
            }
        }
    }

    private var animalsTab: some View {
        VStack(spacing: 10) {
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 8) {
                    ActionPill(title: "一键收获", icon: "basket.fill", tint: p.ranchAccent, enabled: canOperate) {
                        direct("harvest", ["all": true])
                    }
                    ActionPill(title: "一键生产", icon: "arrow.triangle.2.circlepath", tint: p.ranchAccent, enabled: canOperate) {
                        direct("produce", ["all": true])
                    }
                    ActionPill(title: "清理牧场", icon: "sparkles", tint: p.blue, enabled: canOperate) {
                        direct("clean", [:])
                    }
                    ActionPill(title: "一键喂养", icon: "leaf.fill", tint: p.green,
                               enabled: canOperate && ((snapshot?.base.canFeed(animals: allAnimals, organic: false) ?? false)
                                   || (snapshot?.base.canFeed(animals: allAnimals, organic: true) ?? false))) {
                        feedAnimal = nil
                        showFeed = true
                    }
                }
            }
            if let pens = snapshot?.base.pens, !pens.isEmpty {
                ForEach(pens) { pen in
                    Card(padding: 14) {
                        VStack(spacing: 8) {
                            HStack {
                                Text(pen.name).font(.system(size: 15, weight: .semibold)).foregroundColor(p.text)
                                Pill(text: "\(pen.count)/\(pen.capacity)", color: p.ranchAccent)
                                Spacer()
                            }
                            let columns = Array(repeating: GridItem(.flexible(), spacing: 8), count: p.ranchColumns)
                            LazyVGrid(columns: columns, spacing: 8) {
                                ForEach(pen.animals) { animal in
                                    animalCard(animal, isShed: pen.moduleType == 7)
                                }
                            }
                        }
                    }
                }
            } else {
                Card { EmptyStateView(icon: "pawprint", title: "牧场里还没有动物", subtitle: "可在牧场背包放养幼崽，或从牧场商城购买") }
            }
        }
    }

    private func animalCard(_ animal: RanchAnimal, isShed: Bool) -> some View {
        let colors = p.ranchColors(isShed: isShed, isGiant: animal.isGiant,
                                   canHarvest: animal.canHarvest, canProduce: animal.canProduce,
                                   grassBlocked: animal.grassBlocked)
        let base = snapshot?.base ?? RanchPayload()
        let feedAvailable = base.canFeed(animals: [animal], organic: false)
            || base.canFeed(animals: [animal], organic: true)
        let fraction = animal.maxProduceCount > 0
            ? min(1, Double(animal.produceCount) / Double(animal.maxProduceCount)) : 0
        return VStack(alignment: .leading, spacing: 6) {
            HStack(spacing: 5) {
                RemoteIcon(url: AssetURL.resolve(animal.image, base: store.serverURL), fallback: "pawprint.fill", size: 34)
                VStack(alignment: .leading, spacing: 1) {
                    Text(animal.name).font(.system(size: 12, weight: .semibold)).foregroundColor(p.text)
                        .lineLimit(1).minimumScaleFactor(0.7)
                    Text(animal.stageName).font(.system(size: 9)).foregroundColor(p.secondary).lineLimit(1)
                }
                Spacer(minLength: 0)
                if animal.isGiant {
                    Image(systemName: "star.circle.fill").font(.system(size: 11)).foregroundColor(p.purple)
                }
            }
            if !animal.weightKg.isEmpty {
                Text(animal.weightKg).font(.system(size: 9)).foregroundColor(p.purple)
            }
            if animal.maxProduceCount > 0 {
                Text("产物 \(animal.produceCount)/\(animal.maxProduceCount)")
                    .font(.system(size: 9)).foregroundColor(p.secondary)
                ProgressLine(value: fraction, color: colors.border, height: 3)
            }
            if let next = animal.nextAt, !animal.canHarvest {
                TimelineView(.periodic(from: .now, by: 1)) { context in
                    Text(next <= context.date ? "可收获/待刷新" : "下次 \(Fmt.countdown(to: next))")
                        .font(.system(size: 9, design: .monospaced)).foregroundColor(p.ranchAccent)
                }
            }
            HStack(spacing: 7) {
                if animal.canHarvest {
                    animalAction("basket.fill", color: p.ranchAccent, enabled: canOperate) {
                        direct("harvest", ["ranchId": animal.ranchId, "animalId": animal.id])
                    }
                }
                if animal.canProduce {
                    animalAction("arrow.triangle.2.circlepath", color: p.emerald, enabled: canOperate) {
                        direct("produce", ["ranchId": animal.ranchId, "animalId": animal.id])
                    }
                }
                if animal.canFeed {
                    animalAction("leaf.fill", color: p.green, enabled: canOperate && feedAvailable) {
                        feedAnimal = animal
                        showFeed = true
                    }
                }
                Spacer(minLength: 0)
                Menu {
                    Button("放生", role: .destructive) {
                        quote("release", ["ranchId": animal.ranchId, "animalId": animal.id])
                    }
                } label: {
                    Image(systemName: "ellipsis.circle").font(.system(size: 14)).foregroundColor(p.secondary)
                }
                .disabled(!canOperate)
            }
        }
        .padding(9)
        .background(colors.background)
        .clipShape(RoundedRectangle(cornerRadius: p.tileRadius, style: .continuous))
        .overlay(RoundedRectangle(cornerRadius: p.tileRadius, style: .continuous)
            .stroke(colors.border, lineWidth: colors.highlightBorder ? 1.4 : 0.8))
    }

    private func animalAction(_ icon: String, color: Color, enabled: Bool = true,
                              action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Image(systemName: icon)
                .font(.system(size: 12, weight: .semibold))
                .foregroundColor(enabled ? color : color.opacity(0.35))
                .frame(width: 23, height: 23)
                .background((enabled ? color : color.opacity(0.35)).opacity(0.12))
                .clipShape(Circle())
        }
        .buttonStyle(.plain)
        .disabled(!enabled)
    }

    private var facilitiesTab: some View {
        VStack(spacing: 10) {
            if let facilities = snapshot?.base.facilities, !facilities.isEmpty {
                ForEach(facilities) { fac in
                    let upgradeDone = fac.upgradeState == 2 ||
                        (fac.upgradeState == 1 && (fac.upgradeEndAt.map { $0 <= Date() } ?? false))
                    Card(padding: 14) {
                        VStack(alignment: .leading, spacing: 8) {
                            HStack {
                                Text(fac.name).font(.system(size: 15, weight: .semibold)).foregroundColor(p.text)
                                Pill(text: "Lv.\(fac.level)", color: p.ranchAccent)
                                if fac.upgradeState != 0 {
                                    Pill(text: upgradeDone ? "可完成" : "升级中", color: p.indigo, dot: true)
                                }
                                Spacer()
                            }
                            if fac.hasRanch {
                                Text("畜舍容量 \(fac.ranchCount)/\(fac.ranchCapacity)")
                                    .font(.system(size: 11)).foregroundColor(p.secondary)
                            }
                            if fac.hasFeed {
                                HStack(spacing: 6) {
                                    Text("牧草 \(fac.feedCount)/\(fac.capacity)").font(.system(size: 11)).foregroundColor(p.secondary)
                                    Text(fac.feedRemainingSec >= 0 ? "剩余 \(Fmt.duration(fac.feedRemainingSec))" : "")
                                        .font(.system(size: 11)).foregroundColor(p.secondary)
                                }
                            }
                            if let end = fac.upgradeEndAt, fac.upgradeState == 1 {
                                TimelineView(.periodic(from: .now, by: 1)) { context in
                                    if end <= context.date {
                                        Text("升级已完成，可完成升级")
                                    } else {
                                        CountdownLabel(date: end, prefix: "升级剩余 ")
                                    }
                                }
                                .font(.system(size: 11)).foregroundColor(p.indigo)
                            }
                            ScrollView(.horizontal, showsIndicators: false) {
                                TimelineView(.periodic(from: .now, by: 1)) { context in
                                    let upgradeDone = fac.upgradeState == 2 ||
                                        (fac.upgradeState == 1 && (fac.upgradeEndAt.map { $0 <= context.date } ?? false))
                                    HStack(spacing: 7) {
                                    if fac.hasRanch {
                                        ActionPill(title: "补牧草", icon: "leaf.fill", tint: p.green, enabled: canOperate) {
                                            addGrass(fac)
                                        }
                                    }
                                    if fac.next?.available == true {
                                        ActionPill(title: "升级", icon: "arrow.up.circle.fill", tint: p.indigo, enabled: canOperate && fac.upgradeState == 0) {
                                            quote("upgrade", ["facilityId": fac.id])
                                        }
                                    }
                                    if fac.upgradeState != 0 {
                                        ActionPill(title: "完成升级", icon: "checkmark.circle.fill", tint: p.emerald, enabled: canOperate && upgradeDone) {
                                            quote("completeUpgrade", ["facilityId": fac.id])
                                        }
                                    }
                                    if fac.upgradeState == 1 && !upgradeDone {
                                        ActionPill(title: "沙漏加速", icon: "hourglass", tint: p.ranchAccent, enabled: canOperate) {
                                            speedUp(fac)
                                        }
                                    }
                                    if fac.canActivate {
                                        ActionPill(title: "激活棚", icon: "power", tint: p.ranchAccent, enabled: canOperate) {
                                            quote("activateShed", ["facilityId": fac.id])
                                        }
                                    }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                Card { EmptyStateView(icon: "building.2", title: "暂无设施数据") }
            }
        }
    }

    private var bagTab: some View {
        VStack(spacing: 10) {
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 8) {
                    ActionPill(title: "出售全部产物", icon: "yensign.circle.fill", tint: p.ranchAccent, enabled: canOperate) {
                        quote("sell", ["all": true])
                    }
                    ActionPill(title: "全部锁定", icon: "lock.fill", tint: p.indigo, enabled: canOperate) {
                        let uids = (snapshot?.bag ?? []).filter { $0.lockable && !$0.locked }.map(\.uid)
                        if !uids.isEmpty { quote("setItemLock", ["itemUids": uids, "locked": true]) }
                    }
                    ActionPill(title: "全部解锁", icon: "lock.open.fill", tint: p.indigo, enabled: canOperate) {
                        let uids = (snapshot?.bag ?? []).filter { $0.lockable && $0.locked }.map(\.uid)
                        if !uids.isEmpty { quote("setItemLock", ["itemUids": uids, "locked": false]) }
                    }
                }
            }
            if let bag = snapshot?.bag, !bag.isEmpty {
                ForEach(bag, id: \.key) { item in
                    Card(padding: 12) {
                        HStack(spacing: 10) {
                            RemoteIcon(url: AssetURL.resolve(item.image, base: store.serverURL), fallback: "shippingbox.fill", size: 40)
                            VStack(alignment: .leading, spacing: 3) {
                                HStack(spacing: 4) {
                                    Text(item.name).font(.system(size: 14, weight: .semibold)).foregroundColor(p.text)
                                    if item.locked { Image(systemName: "lock.fill").font(.system(size: 10)).foregroundColor(p.ranchAccent) }
                                    if item.giantCub { Pill(text: "巨型幼崽", color: p.purple) }
                                }
                                Text("×\(Fmt.compact(item.count))" + (item.husbandryType.isEmpty ? "" : " · \(item.husbandryType)"))
                                    .font(.system(size: 11)).foregroundColor(p.secondary)
                                if item.isCan { Text("每个加速 \(Fmt.duration(item.canSpeedUpSec))").font(.system(size: 10)).foregroundColor(p.secondary) }
                                if !item.sellPrices.isEmpty {
                                    Text("售价 " + item.sellPrices.map { "\(Fmt.compact($0.count)) \($0.name)" }.joined(separator: " + "))
                                        .font(.system(size: 10)).foregroundColor(p.secondary)
                                }
                            }
                            Spacer(minLength: 0)
                            VStack(spacing: 4) {
                                if item.husbandryType.isEmpty == false && !item.isProduct && !item.isCan {
                                    Button("放养") { placeTarget = PlaceTarget(item: item) }.font(.system(size: 12, weight: .semibold)).foregroundColor(p.ranchAccent)
                                        .disabled(!canOperate)
                                }
                                if item.isCan {
                                    Button("加罐头") { useCan(item) }.font(.system(size: 12, weight: .semibold)).foregroundColor(p.green)
                                        .disabled(!canOperate)
                                }
                                if item.canSell {
                                    Button("出售") { sell(item) }.font(.system(size: 12)).foregroundColor(p.ranchAccent)
                                        .disabled(!canOperate)
                                }
                                if item.lockable {
                                    Button(item.locked ? "解锁" : "锁定") {
                                        quote("setItemLock", ["itemUids": [item.uid], "locked": !item.locked])
                                    }
                                    .font(.system(size: 11)).foregroundColor(p.secondary)
                                    .disabled(!canOperate)
                                }
                            }
                        }
                    }
                }
            } else {
                Card { EmptyStateView(icon: "bag", title: "牧场背包为空") }
            }
        }
    }

    private var tasksTab: some View {
        VStack(spacing: 10) {
            if let tasks = snapshot?.tasks, !tasks.isEmpty {
                HStack {
                    SectionTitle(title: "牧场任务")
                    Spacer()
                    ActionPill(title: "一键领取", icon: "checkmark.seal.fill", tint: p.ranchAccent, enabled: canOperate) {
                        quote("claimTasks", ["all": true])
                    }
                }
                ForEach(tasks) { task in
                    Card(padding: 12) {
                        HStack {
                            VStack(alignment: .leading, spacing: 3) {
                                Text(task.desc).font(.system(size: 13, weight: .medium)).foregroundColor(p.text)
                                Text("\(task.progress)/\(task.total)").font(.system(size: 11)).foregroundColor(p.secondary)
                            }
                            Spacer()
                            Pill(text: task.claimed ? "已领取" : (task.canClaim ? "可领取" : "进行中"),
                                 color: task.claimed ? p.secondary : (task.canClaim ? p.green : p.ranchAccent), dot: true)
                        }
                    }
                }
            } else {
                Card { EmptyStateView(icon: "checklist", title: "暂无牧场任务") }
            }
        }
    }

    private func load() async {
        let accountId = store.currentAccountId
        let view = viewName
        let tab = viewTab
        guard !accountId.isEmpty, store.isAccountOnline else {
            loading = false
            return
        }
        loading = true
        error = ""
        do {
            let next = try await store.api.fetchRanchView(accountId: accountId, view: view)
            guard accountId == store.currentAccountId, tab == viewTab, store.isAccountOnline else { return }
            snapshot = next
            feedBucket = next.base.buckets.first?.id ?? 0
        } catch let e as APIError {
            guard accountId == store.currentAccountId, tab == viewTab else { return }
            error = e.message
        } catch let err {
            guard accountId == store.currentAccountId, tab == viewTab else { return }
            error = err.localizedDescription
        }
        if accountId == store.currentAccountId && tab == viewTab {
            loading = false
        }
    }

    private func direct(_ action: String, _ params: [String: Any]) {
        guard store.isAccountOnline else { store.showToast("账号未在线，请先启动账号"); return }
        guard !busy else { return }
        busy = true
        Task {
            do {
                let message = try await store.api.ranchDirect(accountId: store.currentAccountId, action: action, params: params)
                store.showToast(message)
                await load()
            } catch let e as APIError { store.showToast(e.message) }
            catch { store.showToast(error.localizedDescription) }
            busy = false
        }
    }

    private func feed(animal: RanchAnimal?, bucket: Int, produce: Bool) async {
        guard store.isAccountOnline else { store.showToast("账号未在线，请先启动账号"); return }
        var params: [String: Any] = ["bucketType": bucket, "produceAfterFeed": produce]
        if let animal {
            params["ranchId"] = animal.ranchId
            params["animalId"] = animal.id
        } else {
            params["all"] = true
        }
        busy = true
        do {
            let message = try await store.api.ranchDirect(accountId: store.currentAccountId, action: "feed", params: params)
            store.showToast(message)
            await load()
        } catch let e as APIError { store.showToast(e.message) }
        catch { store.showToast(error.localizedDescription) }
        busy = false
    }

    private func quote(_ action: String, _ input: [String: Any]) {
        guard store.isAccountOnline else { store.showToast("账号未在线，请先启动账号"); return }
        guard !busy else { return }
        let accountId = store.currentAccountId
        busy = true
        Task {
            do {
                let q = try await store.api.quoteRanch(accountId: accountId, action: action, input: input)
                guard accountId == store.currentAccountId, store.isAccountOnline else { busy = false; return }
                if q.skipped {
                    store.showToast(q.message.isEmpty ? "当前没有可操作项目" : q.message)
                } else {
                    pending = PendingRanch(accountId: accountId, action: action, input: input, quote: q)
                }
            } catch let e as APIError { store.showToast(e.message) }
            catch { store.showToast(error.localizedDescription) }
            busy = false
        }
    }

    private func execute(_ item: PendingRanch) {
        pending = nil
        guard store.isAccountOnline, item.accountId == store.currentAccountId else { return }
        busy = true
        Task {
            do {
                let result = try await store.api.executeRanch(accountId: item.accountId,
                                                               action: item.action, input: item.input,
                                                               signature: item.quote.signature)
                let message = result.notice.isEmpty
                    ? (result.status == "skipped" ? "当前没有可操作项目" : "\(item.quote.label.isEmpty ? "操作" : item.quote.label)完成")
                    : result.notice
                store.showToast(message)
                await load()
                await store.refreshStatusOnly()
            } catch let e as APIError { store.showToast(e.message) }
            catch { store.showToast(error.localizedDescription) }
            busy = false
        }
    }

    private func addGrass(_ fac: RanchFacility) {
        let room = max(0, fac.capacity - fac.feedCount)
        let maxAdd = min(Int(min(snapshot?.base.grass ?? 0, 9999)), room)
        guard maxAdd > 0 else {
            store.showToast(room <= 0 ? "牧草机已满" : "牧草库存不足")
            return
        }
        amountPrompt = AmountPrompt(title: "为\(fac.name)补充牧草",
                                    caption: "库存 \(Fmt.compact(snapshot?.base.grass ?? 0)) · 还可放入 \(room)",
                                    maxCount: maxAdd, confirmLabel: "补充牧草", unit: "株") { count in
            quote("addGrass", ["facilityId": fac.id, "count": count])
        }
    }

    private func speedUp(_ fac: RanchFacility) {
        let maxSeconds = Int(min(max(60, fac.upgradeEndAt?.timeIntervalSinceNow ?? 3600), 86400))
        amountPrompt = AmountPrompt(title: "沙漏加速",
                                    caption: "升级结束时间 \(fac.upgradeEndAt.map { Fmt.dateTime($0) } ?? "未知")",
                                    maxCount: maxSeconds, confirmLabel: "加速", unit: "秒") { seconds in
            quote("speedUpUpgrade", ["facilityId": fac.id, "seconds": seconds])
        }
    }

    private func place(_ item: RanchBagItem, pen: RanchFacility, count: Int) {
        quote("place", ["uid": item.uid, "itemId": item.id, "ranchId": pen.id, "count": count])
    }

    private func useCan(_ item: RanchBagItem) {
        let maxCount = Int(min(item.count, 9999))
        guard maxCount > 0 else { store.showToast("罐头库存不足"); return }
        amountPrompt = AmountPrompt(title: "补充罐头",
                                    caption: "每个加速 \(Fmt.duration(item.canSpeedUpSec)) · 库存 \(Fmt.compact(item.count))",
                                    maxCount: maxCount, confirmLabel: "添加罐头", unit: "个") { count in
            quote("useCan", ["uid": item.uid, "itemId": item.id, "count": count])
        }
    }

    private func sell(_ item: RanchBagItem) {
        let maxCount = Int(min(item.count, 9999))
        guard maxCount > 0 else { store.showToast("库存不足"); return }
        let price = item.sellPrices.map { "\(Fmt.compact($0.count)) \($0.name)" }.joined(separator: " + ")
        amountPrompt = AmountPrompt(title: "出售 \(item.name)",
                                    caption: price.isEmpty ? "库存 \(Fmt.compact(item.count))" : "单价 \(price) · 库存 \(Fmt.compact(item.count))",
                                    maxCount: maxCount, confirmLabel: "下一步", unit: "个") { count in
            quote("sell", ["uid": item.uid, "itemId": item.id, "count": count])
        }
    }
}

struct RanchPlaceSheet: View {
    @Environment(\.palette) private var p
    @Environment(\.dismiss) private var dismiss
    @EnvironmentObject private var store: AppStore
    var item: RanchBagItem
    var facilities: [RanchFacility]
    var onConfirm: (RanchFacility, Int) -> Void
    @State private var selectedId = ""
    @State private var text = "1"

    private var pens: [RanchFacility] {
        let expected: Int? = item.husbandryType == "棚" ? 7 : (item.husbandryType.isEmpty ? nil : 6)
        return facilities.filter { fac in
            fac.hasRanch && fac.state != 2 && (expected == nil || fac.moduleType == expected)
        }
    }
    private var selected: RanchFacility? { pens.first(where: { $0.id == selectedId }) ?? pens.first }
    private var maxCount: Int {
        let room = selected.map { max(0, $0.ranchCapacity - $0.ranchCount) } ?? 0
        return max(0, min(Int(min(item.count, 9999)), room))
    }

    var body: some View {
        NavigationView {
            ScrollView {
                VStack(alignment: .leading, spacing: 14) {
                    Text("放养 \(item.name)").font(.system(size: 22, weight: .bold)).foregroundColor(p.text)
                    Text("库存 \(Fmt.compact(item.count))").font(.system(size: 13)).foregroundColor(p.secondary)
                    if pens.isEmpty {
                        EmptyStateView(icon: "pawprint", title: "没有可用畜舍", subtitle: "请先升级设施或清理空位")
                    } else if maxCount == 0 {
                        EmptyStateView(icon: "pawprint", title: "当前畜舍已满", subtitle: "请先收获动物或升级设施后再放养")
                    } else {
                        Text("选择畜舍").font(.system(size: 13, weight: .semibold)).foregroundColor(p.secondary)
                        ForEach(pens) { fac in
                            Button {
                                selectedId = fac.id
                            } label: {
                                HStack {
                                    Text(fac.name).foregroundColor(p.text)
                                    Text("Lv.\(fac.level)").font(.system(size: 11)).foregroundColor(p.secondary)
                                    Spacer()
                                    Text("\(fac.ranchCount)/\(fac.ranchCapacity)").font(.system(size: 12)).foregroundColor(p.ranchAccent)
                                    if selected?.id == fac.id {
                                        Image(systemName: "checkmark.circle.fill").foregroundColor(p.ranchAccent)
                                    }
                                }
                                .padding(12)
                                .background(selected?.id == fac.id ? p.ranchAccent.opacity(0.12) : p.elevated)
                                .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                            }
                            .buttonStyle(.plain)
                        }
                        TextField("数量", text: $text)
                            .keyboardType(.numberPad)
                            .font(.system(size: 22, weight: .semibold, design: .rounded))
                            .padding(12).background(p.elevated)
                            .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                        Text("可放养 1 – \(maxCount)").font(.system(size: 12)).foregroundColor(p.secondary)
                        PrimaryButton(title: "下一步", tint: p.ranchAccent) {
                            guard let pen = selected else { return }
                            let count = min(maxCount, max(1, Int(text) ?? 1))
                            dismiss()
                            onConfirm(pen, count)
                        }
                    }
                }
                .padding(20)
            }
            .background(p.background.ignoresSafeArea())
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button("取消") { dismiss() } }
            }
            .onAppear {
                selectedId = pens.first?.id ?? ""
                text = "1"
            }
        }
    }
}

struct RanchFeedSheet: View {
    @Environment(\.palette) private var p
    @Environment(\.dismiss) private var dismiss
    var buckets: [RanchBucket]
    var animals: [RanchAnimal] = []
    var animal: RanchAnimal?
    var initialBucket: Int
    var onConfirm: (RanchAnimal?, Int, Bool) -> Void
    @State private var bucketId = 0
    @State private var produce = true

    private func isOrganic(_ bucket: RanchBucket) -> Bool {
        bucket.id == 7004 || bucket.name.contains("有机")
    }

    private func usable(_ bucket: RanchBucket) -> Bool {
        guard bucket.seconds > 0 else { return false }
        let organic = isOrganic(bucket)
        let pool = animal.map { [$0] } ?? animals
        return pool.contains { $0.canFeed && (organic || !$0.limitedCanUsed) }
    }

    private func unavailableReason(_ bucket: RanchBucket) -> String {
        if bucket.seconds <= 0 { return "库存为空" }
        if !isOrganic(bucket), animal?.limitedCanUsed == true { return "该动物已用完普通罐头" }
        return "当前没有可喂养动物"
    }

    private var selectedUsable: Bool {
        buckets.first(where: { $0.id == bucketId }).map { usable($0) } ?? false
    }

    var body: some View {
        NavigationView {
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    Text(animal == nil ? "一键喂养" : "喂养 \(animal!.name)")
                        .font(.system(size: 22, weight: .bold)).foregroundColor(p.text)
                    if buckets.isEmpty {
                        EmptyStateView(icon: "leaf", title: "没有可用罐头", subtitle: "请先在牧场背包补充罐头")
                    } else {
                        Text("选择罐头").font(.system(size: 13, weight: .semibold)).foregroundColor(p.secondary)
                        ForEach(buckets) { bucket in
                            let available = usable(bucket)
                            Button {
                                if available { bucketId = bucket.id }
                            } label: {
                                HStack {
                                    VStack(alignment: .leading, spacing: 2) {
                                        Text(bucket.name).foregroundColor(p.text)
                                        if !available {
                                            Text(unavailableReason(bucket))
                                                .font(.system(size: 10)).foregroundColor(p.ranchAccent)
                                        }
                                    }
                                    Spacer()
                                    Text("+\(Fmt.duration(bucket.seconds))")
                                        .font(.system(size: 12)).foregroundColor(available ? p.green : p.secondary)
                                    if bucketId == bucket.id && available {
                                        Image(systemName: "checkmark.circle.fill").foregroundColor(p.green)
                                    }
                                }
                                .padding(12)
                                .background(bucketId == bucket.id && available ? p.green.opacity(0.12) : p.elevated)
                                .clipShape(RoundedRectangle(cornerRadius: p.controlRadius, style: .continuous))
                                .opacity(available ? 1 : 0.55)
                            }
                            .buttonStyle(.plain)
                            .disabled(!available)
                        }
                        Toggle("喂养后立即生产", isOn: $produce).tint(p.green)
                        if animal?.limitedCanUsed == true {
                            Text("该动物已用完普通罐头，仅有机罐头可继续使用")
                                .font(.system(size: 10)).foregroundColor(p.secondary)
                        }
                        PrimaryButton(title: "开始喂养", icon: "leaf.fill", tint: p.ranchAccent) {
                            // The server protocol uses 1 = organic, 2 = normal.
                            // bucketId here is the inventory bucket id (7004/7003).
                            let selected = buckets.first(where: { $0.id == bucketId })
                            let bucketType = (selected?.id == 7004 || selected?.name.contains("有机") == true) ? 1 : 2
                            dismiss()
                            onConfirm(animal, bucketType, produce)
                        }
                        .disabled(!selectedUsable)
                    }
                }
                .padding(20)
            }
            .background(p.background.ignoresSafeArea())
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button("取消") { dismiss() } }
            }
            .onAppear {
                if let initial = buckets.first(where: { $0.id == initialBucket }), usable(initial) {
                    bucketId = initial.id
                } else if let first = buckets.first(where: { usable($0) }) {
                    bucketId = first.id
                } else {
                    bucketId = buckets.first?.id ?? 0
                }
            }
        }
    }
}
