import Foundation
import SwiftUI

struct FarmWorkspaceView: View {
    @Environment(\.palette) private var p
    @EnvironmentObject private var store: AppStore
    @State private var lands: LandsPayload?
    @State private var loading = false
    @State private var error = ""
    @State private var busy = false
    @State private var selectedLand: LandItem?
    @State private var showBag = false
    @State private var showPet = false
    @State private var amountPrompt: AmountPrompt?
    @State private var showUpgradeConfirm = false

    var body: some View {
        ScrollView {
            VStack(spacing: 14) {
                TopBar()
                if store.status == nil && store.overviewLoading {
                    Card { LoadingState(text: "正在读取账号状态") }
                        .padding(.horizontal, 16)
                } else if !store.isAccountOnline {
                    Card {
                        EmptyStateView(icon: "wifi.slash", title: "账号未在线",
                                       subtitle: "请先在总览启动当前账号，农场操作需要在线连接")
                    }
                    .padding(.horizontal, 16)
                } else {
                    if !error.isEmpty {
                        ErrorBanner(message: error).padding(.horizontal, 16)
                    }
                    summaryCard.padding(.horizontal, 16)
                    actionBar.padding(.horizontal, 16)
                    landGrid.padding(.horizontal, 16)
                }
                Color.clear.frame(height: 20)
            }
            .padding(.top, 4)
        }
        .background(p.background.ignoresSafeArea())
        .navigationBarHidden(true)
        .refreshable { await load() }
        .task(id: store.currentAccountId) { await load() }
        .task(id: store.status?.connected) {
            if store.isAccountOnline { await load() }
        }
        .sheet(item: $selectedLand) { land in
            LandDetailSheet(land: land, onAction: { await refreshAfterAction() })
                .environmentObject(store)
        }
        .sheet(isPresented: $showBag) {
            FarmBagSheet().environmentObject(store)
        }
        .sheet(isPresented: $showPet) {
            PetSheet().environmentObject(store)
        }
        .sheet(item: $amountPrompt) { prompt in
            AmountPromptView(prompt: prompt)
        }
        .alert("批量解锁与升级土地", isPresented: $showUpgradeConfirm) {
            Button("确认升级") { operate("upgrade") }
            Button("取消", role: .cancel) {}
        } message: {
            Text("将自动解锁或升级农场中所有满足条件的土地，会消耗金币或道具。")
        }
    }

    private var summaryCard: some View {
        let summary = lands?.summary ?? LandSummary()
        return Card {
            VStack(spacing: 12) {
                HStack {
                    VStack(alignment: .leading, spacing: 2) {
                        Text("农场工作台").font(.system(size: 20, weight: .bold)).foregroundColor(p.text)
                        Text(store.status?.connected == true ? "实时同步土地状态" : "账号未在线，操作可能不可用")
                            .font(.system(size: 11)).foregroundColor(p.secondary)
                    }
                    Spacer()
                    Button { Task { await load() } } label: {
                        Image(systemName: "arrow.clockwise").foregroundColor(p.emerald)
                    }
                }
                let columns = [GridItem(.flexible()), GridItem(.flexible()), GridItem(.flexible())]
                LazyVGrid(columns: columns, spacing: 8) {
                    summaryCell("可收获", summary.harvestable, p.green)
                    summaryCell("生长中", summary.growing, p.emerald)
                    summaryCell("空地", summary.empty, p.secondary)
                    summaryCell("待浇水", summary.needWater, p.blue)
                    summaryCell("待除草", summary.needWeed, p.orange)
                    summaryCell("待除虫", summary.needBug, p.red)
                }
            }
        }
    }

    private func summaryCell(_ title: String, _ value: Int, _ color: Color) -> some View {
        VStack(spacing: 2) {
            Text("\(value)").font(.system(size: 18, weight: .bold)).foregroundColor(value > 0 ? color : p.secondary)
            Text(title).font(.system(size: 10)).foregroundColor(p.secondary)
        }
        .frame(maxWidth: .infinity).padding(.vertical, 6)
        .background(p.elevated)
        .clipShape(RoundedRectangle(cornerRadius: 9, style: .continuous))
    }

    private var actionBar: some View {
        let summary = lands?.summary ?? LandSummary()
        let list = lands?.lands ?? []
        let care = summary.needWater + summary.needWeed + summary.needBug > 0
        let canUpgrade = list.contains { $0.couldUnlock || $0.couldUpgrade }
        let online = store.isAccountOnline
        return FlowLayout(spacing: 8) {
            ActionPill(title: "一键全自动", icon: "wand.and.stars", enabled: online && !busy) {
                operate("all")
            }
            ActionPill(title: "一键收获", icon: "basket.fill", enabled: online && !busy && summary.harvestable > 0) {
                operate("harvest")
            }
            ActionPill(title: "一键护理", icon: "drop.fill", tint: p.blue, enabled: online && !busy && care) {
                operate("clear")
            }
            ActionPill(title: "一键种植", icon: "leaf.fill", enabled: online && !busy && (summary.empty > 0 || summary.dead > 0)) {
                operate("plant")
            }
            ActionPill(title: "普通肥", icon: "drop.circle.fill", tint: p.green, enabled: online && !busy) {
                fertilizeAll("normal")
            }
            ActionPill(title: "有机肥", icon: "sparkles", tint: p.green, enabled: online && !busy) {
                fertilizeAll("organic")
            }
            ActionPill(title: "升级土地", icon: "arrow.up.circle.fill", tint: p.indigo, enabled: online && !busy && canUpgrade) {
                showUpgradeConfirm = true
            }
            ActionPill(title: "背包", icon: "bag.fill", tint: p.orange, enabled: true) { showBag = true }
            ActionPill(title: "看门狗", icon: "pawprint.fill", tint: p.indigo, enabled: true) { showPet = true }
        }
    }

    @ViewBuilder
    private var landGrid: some View {
        if loading && lands == nil {
            Card { LoadingState(text: "正在读取土地") }
        } else if let payload = lands, payload.lands.isEmpty {
            Card { EmptyStateView(icon: "square.grid.3x3", title: "暂无土地数据", subtitle: "启动账号或刷新后重试", actionTitle: "刷新", action: { Task { await load() } }) }
        } else if let payload = lands {
            let columns = Array(repeating: GridItem(.flexible(), spacing: 8), count: p.landColumns)
            VStack(spacing: 10) {
                SectionTitle(title: "土地宫格", trailing: "共 \(payload.lands.count) 块 · \(p.landColumns) 列")
                LazyVGrid(columns: columns, spacing: 8) {
                    ForEach(payload.lands) { land in
                        Button { selectedLand = land } label: { landTile(land) }
                            .buttonStyle(.plain)
                    }
                }
            }
        }
    }

    private func landTile(_ land: LandItem) -> some View {
        let soil = p.soil(land.level)
        let colors = p.landColors(level: land.level)
        let isHarvest = ["harvestable", "ripe", "mature"].contains(land.status)
        let isStealable = land.status == "stealable"
        let isEmpty = land.status == "empty" || land.plantName.isEmpty
        let remainSec = max(0, land.matureAt?.timeIntervalSinceNow ?? 0)
        let fraction: Double = {
            if isHarvest || isStealable { return 1 }
            if land.totalGrowTime > 0 && remainSec > 0 {
                return min(1, max(0.05, 1 - remainSec / Double(land.totalGrowTime)))
            }
            return land.status == "growing" ? 1 : 0
        }()
        return ZStack(alignment: .topLeading) {
            RoundedRectangle(cornerRadius: p.tileRadius, style: .continuous)
                .fill(land.unlocked ? colors.background : p.track.opacity(0.55))
            VStack(alignment: .leading, spacing: 4) {
                HStack(spacing: 3) {
                    Text("#\(land.id)").font(.system(size: 10, weight: .bold)).foregroundColor(colors.badgeText)
                    Text("Lv\(land.level)")
                        .font(.system(size: 9)).foregroundColor(colors.badgeText)
                        .padding(.horizontal, 4).padding(.vertical, 1)
                        .background(colors.badgeBackground)
                        .clipShape(Capsule())
                    Spacer(minLength: 0)
                    if land.needsCare {
                        Image(systemName: "exclamationmark.circle.fill")
                            .font(.system(size: 9)).foregroundColor(p.orange)
                    }
                }
                if land.unlocked {
                    HStack(spacing: 6) {
                        Group {
                            if isHarvest {
                                Image(systemName: "basket.fill").font(.system(size: 18)).foregroundColor(p.green)
                            } else if isEmpty {
                                Image(systemName: "leaf.fill").font(.system(size: 18)).foregroundColor(p.secondary.opacity(0.45))
                            } else {
                                RemoteIcon(url: AssetURL.resolve(land.seedImage, base: store.serverURL),
                                           fallback: "leaf.fill", size: 28)
                            }
                        }
                        .frame(width: 28, height: 28)
                        VStack(alignment: .leading, spacing: 1) {
                            Text(land.plantName.isEmpty ? land.statusLabel : land.plantName)
                                .font(.system(size: 12, weight: .semibold)).foregroundColor(p.text)
                                .lineLimit(1).minimumScaleFactor(0.55).truncationMode(.middle)
                            if let matureAt = land.matureAt, !isHarvest {
                                TimelineView(.periodic(from: .now, by: 1)) { context in
                                    let ready = matureAt <= context.date
                                    Text(ready ? "可收获" : Fmt.countdown(to: matureAt))
                                        .font(.system(size: 9, design: .monospaced))
                                        .foregroundColor(ready ? p.green : p.secondary)
                                }
                            } else {
                                Text(isHarvest ? "可收获" : (isStealable ? "可偷取" : land.statusLabel))
                                    .font(.system(size: 9))
                                    .foregroundColor(isHarvest ? p.green : (isStealable ? p.orange : p.secondary))
                            }
                        }
                        Spacer(minLength: 0)
                    }
                    if !isEmpty {
                        ProgressLine(value: fraction, color: isHarvest ? p.green : soil, height: 3)
                    }
                    HStack(spacing: 5) {
                        if land.needWater { Image(systemName: "drop.fill").font(.system(size: 9)).foregroundColor(p.blue) }
                        if land.needWeed { Image(systemName: "leaf.fill").font(.system(size: 9)).foregroundColor(p.orange) }
                        if land.needBug { Image(systemName: "ant.fill").font(.system(size: 9)).foregroundColor(p.red) }
                        if !land.mutantNames.isEmpty { Image(systemName: "star.fill").font(.system(size: 9)).foregroundColor(p.indigo) }
                        if land.occupiedByMaster { Text("合").font(.system(size: 8, weight: .bold)).foregroundColor(p.indigo) }
                        if let n = land.leftInorcFertTimes {
                            Text("肥\(n)").font(.system(size: 8, weight: .semibold)).foregroundColor(p.green)
                        }
                    }
                    .frame(height: 10)
                } else {
                    Spacer(minLength: 0)
                    HStack(spacing: 6) {
                        Image(systemName: land.couldUnlock ? "lock.open.fill" : "lock.fill")
                            .font(.system(size: 15)).foregroundColor(p.secondary)
                        Text(land.couldUnlock ? "可解锁" : "未解锁")
                            .font(.system(size: 11, weight: .medium)).foregroundColor(p.secondary)
                    }
                }
            }
            .padding(9)
        }
        .frame(height: p.landColumns == 4 ? 108 : 116)
        .overlay(RoundedRectangle(cornerRadius: p.tileRadius, style: .continuous)
            .stroke(isHarvest ? p.green.opacity(0.7)
                    : (isStealable ? p.orange.opacity(0.7) : colors.border), lineWidth: 1))
    }

    private func load() async {
        let accountId = store.currentAccountId
        guard !accountId.isEmpty, store.isAccountOnline else { return }
        loading = true
        error = ""
        do {
            let payload = try await store.api.fetchLands(accountId: accountId)
            if store.currentAccountId == accountId, store.isAccountOnline {
                lands = payload
            }
        } catch let e as APIError {
            if store.currentAccountId == accountId { error = e.message }
        } catch let err {
            if store.currentAccountId == accountId { error = err.localizedDescription }
        }
        loading = false
    }

    private func refreshAfterAction() async {
        await load()
        await store.refreshBag()
        await store.refreshStatusOnly()
    }

    private func operate(_ op: String) {
        guard !busy, store.isAccountOnline else { return }
        busy = true
        Task {
            do {
                let message = try await store.api.operateFarm(accountId: store.currentAccountId, op: op)
                store.showToast(message)
                await refreshAfterAction()
            } catch let e as APIError { store.showToast(e.message) }
            catch { store.showToast(error.localizedDescription) }
            busy = false
        }
    }

    private func fertilizeAll(_ type: String) {
        guard !busy, store.isAccountOnline else { return }
        busy = true
        Task {
            do {
                let count = try await store.api.fertilizeAll(accountId: store.currentAccountId, type: type)
                store.showToast(count > 0 ? "已施肥 \(count) 块土地" : "当前没有可施肥土地")
                await refreshAfterAction()
            } catch let e as APIError { store.showToast(e.message) }
            catch { store.showToast(error.localizedDescription) }
            busy = false
        }
    }
}

struct LandDetailSheet: View {
    @Environment(\.palette) private var p
    @Environment(\.dismiss) private var dismiss
    @EnvironmentObject private var store: AppStore
    var land: LandItem
    var onAction: () async -> Void
    @State private var busy = false
    @State private var error = ""
    @State private var amountPrompt: AmountPrompt?

    private var isHarvest: Bool { ["harvestable", "ripe", "mature"].contains(land.status) }

    var body: some View {
        NavigationView {
            ScrollView {
                VStack(alignment: .leading, spacing: 14) {
                    HStack {
                        VStack(alignment: .leading, spacing: 4) {
                            Text("土地 #\(land.id)").font(.system(size: 24, weight: .bold)).foregroundColor(p.text)
                            HStack(spacing: 6) {
                                Pill(text: "Lv.\(land.level)", color: p.soil(land.level))
                                Pill(text: land.statusLabel, color: isHarvest ? p.green : p.secondary)
                                if land.occupiedByMaster { Pill(text: "与 #\(land.masterLandId) 合种", color: p.indigo) }
                            }
                        }
                        Spacer()
                        RemoteIcon(url: AssetURL.resolve(land.seedImage, base: store.serverURL), fallback: "leaf.fill", size: 54)
                    }
                    if !land.plantName.isEmpty {
                        Card {
                            VStack(alignment: .leading, spacing: 8) {
                                infoRow("作物", land.plantName)
                                infoRow("阶段", land.phaseName.isEmpty ? land.statusLabel : land.phaseName)
                                if land.totalSeason > 1 { infoRow("季数", "\(land.currentSeason)/\(land.totalSeason)") }
                                if land.totalGrowTime > 0 { infoRow("总生长", Fmt.duration(land.totalGrowTime)) }
                                if let mature = land.matureAt, !isHarvest {
                                    HStack {
                                        Text("成熟倒计时").font(.system(size: 13)).foregroundColor(p.secondary)
                                        Spacer()
                                        CountdownLabel(date: mature, expiredText: "可收获")
                                            .font(.system(size: 14, weight: .semibold, design: .monospaced)).foregroundColor(p.text)
                                    }
                                }
                                if !land.mutantNames.isEmpty { infoRow("变异", land.mutantNames.joined(separator: "、")) }
                                if let n = land.leftInorcFertTimes { infoRow("化肥次数", "\(n)") }
                            }
                        }
                    }
                    Card {
                        VStack(alignment: .leading, spacing: 8) {
                            Text("土地状态").font(.system(size: 14, weight: .semibold)).foregroundColor(p.text)
                            HStack(spacing: 6) {
                                Pill(text: land.needWater ? "需浇水" : "水分正常", color: land.needWater ? p.blue : p.green)
                                Pill(text: land.needWeed ? "有杂草" : "无杂草", color: land.needWeed ? p.orange : p.green)
                                Pill(text: land.needBug ? "有害虫" : "无害虫", color: land.needBug ? p.red : p.green)
                            }
                        }
                    }
                    if let info = land.landInfo, land.couldUpgrade {
                        Card {
                            VStack(alignment: .leading, spacing: 8) {
                                Text("土地升级").font(.system(size: 14, weight: .semibold)).foregroundColor(p.text)
                                if info.nextLevel > 0 {
                                    Text("Lv.\(info.currentLevel) → Lv.\(info.nextLevel) \(info.nextName)")
                                        .font(.system(size: 13)).foregroundColor(p.text)
                                }
                                if !info.costs.isEmpty {
                                    Text("消耗：" + info.costs.map { "\($0.name) \($0.count)" }.joined(separator: " · "))
                                        .font(.system(size: 12)).foregroundColor(p.secondary)
                                }
                                if let need = info.needLevel, need > 0 {
                                    Text("需要农场等级 Lv.\(need)").font(.system(size: 12)).foregroundColor(p.orange)
                                }
                            }
                        }
                    }
                    if !error.isEmpty { ErrorBanner(message: error) }
                    VStack(spacing: 8) {
                        if isHarvest {
                            PrimaryButton(title: "收获全部成熟作物", icon: "basket.fill", enabled: !busy) {
                                runFarm("harvest", landId: nil)
                            }
                        }
                        if land.unlocked && (land.status == "empty" || land.status == "dead") {
                            PrimaryButton(title: "一键种植", icon: "leaf.fill", enabled: !busy) {
                                runFarm("plant", landId: nil)
                            }
                        }
                        if land.needsCare || land.needInteractionCleanup {
                            PrimaryButton(title: "除草 · 除虫 · 浇水", icon: "drop.fill", tint: p.blue, enabled: !busy) {
                                runFarm("clear", landId: land.id)
                            }
                        }
                        HStack(spacing: 8) {
                            Button { fertilize("normal") } label: {
                                Text("施普通肥").frame(maxWidth: .infinity)
                            }.buttonStyle(.bordered).tint(p.green).disabled(busy)
                            Button { fertilize("organic") } label: {
                                Text("施有机肥").frame(maxWidth: .infinity)
                            }.buttonStyle(.bordered).tint(p.emerald).disabled(busy)
                        }
                        if land.couldUpgrade {
                            PrimaryButton(title: "升级土地", icon: "arrow.up.circle.fill", tint: p.indigo, enabled: !busy) {
                                runFarm("upgrade", landId: nil)
                            }
                        }
                        if land.occupiedByMaster && land.masterLandId > 0 && land.masterLandId != land.id {
                            Button("护理请操作主地块 #\(land.masterLandId)") { dismiss() }
                                .font(.system(size: 12)).foregroundColor(p.secondary)
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

    private func infoRow(_ label: String, _ value: String) -> some View {
        HStack(alignment: .top) {
            Text(label).font(.system(size: 12)).foregroundColor(p.secondary)
            Spacer()
            Text(value).font(.system(size: 13, weight: .medium)).foregroundColor(p.text)
                .multilineTextAlignment(.trailing)
        }
    }

    private func runFarm(_ op: String, landId: Int?) {
        guard store.isAccountOnline else { error = "账号未在线，请先启动账号"; return }
        busy = true
        Task {
            do {
                let message = try await store.api.operateFarm(accountId: store.currentAccountId, op: op, landId: landId)
                store.showToast(message)
                await onAction()
                dismiss()
            } catch let e as APIError { error = e.message }
            catch let err { error = err.localizedDescription }
            busy = false
        }
    }

    private func fertilize(_ type: String) {
        guard store.isAccountOnline else { error = "账号未在线，请先启动账号"; return }
        busy = true
        Task {
            do {
                try await store.api.fertilizeLand(accountId: store.currentAccountId, landId: land.id, type: type)
                store.showToast("施肥完成")
                await onAction()
                dismiss()
            } catch let e as APIError { error = e.message }
            catch let err { error = err.localizedDescription }
            busy = false
        }
    }
}

struct FarmBagSheet: View {
    @Environment(\.palette) private var p
    @Environment(\.dismiss) private var dismiss
    @EnvironmentObject private var store: AppStore
    @State private var bag = BagPayload()
    @State private var loading = true
    @State private var busy = false
    @State private var amountPrompt: AmountPrompt?
    @State private var error = ""

    var body: some View {
        NavigationView {
            ScrollView {
                VStack(spacing: 12) {
                    HStack(spacing: 8) {
                        ForEach(bag.systemItems.filter { [1011, 1012, 3001, 3002].contains($0.id) }) { item in
                            VStack(spacing: 2) {
                                Text(item.hoursText.isEmpty ? Fmt.compact(item.count) : item.hoursText)
                                    .font(.system(size: 14, weight: .bold)).foregroundColor(p.text)
                                Text(item.name).font(.system(size: 9)).foregroundColor(p.secondary).lineLimit(1)
                            }
                            .frame(maxWidth: .infinity).padding(.vertical, 8)
                            .background(p.card).clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
                        }
                    }
                    if !error.isEmpty { ErrorBanner(message: error) }
                    if loading && bag.items.isEmpty {
                        LoadingState(text: "正在读取背包")
                    } else if bag.items.isEmpty {
                        Card { EmptyStateView(icon: "bag", title: "背包为空", subtitle: "收获或购买后会显示在这里") }
                    } else {
                        ForEach(bag.items, id: \.key) { item in
                            Card(padding: 12) {
                                HStack(spacing: 10) {
                                    RemoteIcon(url: AssetURL.resolve(item.image, base: store.serverURL), fallback: "shippingbox.fill", size: 40)
                                    VStack(alignment: .leading, spacing: 3) {
                                        HStack(spacing: 5) {
                                            Text(item.name).font(.system(size: 14, weight: .semibold)).foregroundColor(p.text).lineLimit(1)
                                            if item.locked { Image(systemName: "lock.fill").font(.system(size: 10)).foregroundColor(p.orange) }
                                        }
                                        Text("×\(Fmt.compact(item.count))" + (item.hoursText.isEmpty ? "" : " · \(item.hoursText)"))
                                            .font(.system(size: 11)).foregroundColor(p.secondary)
                                        if !item.description.isEmpty {
                                            Text(item.description).font(.system(size: 10)).foregroundColor(p.secondary).lineLimit(2)
                                        }
                                    }
                                    Spacer(minLength: 0)
                                    VStack(spacing: 5) {
                                        if item.supportsUse && !item.locked {
                                            Button("使用") { use(item) }.font(.system(size: 12, weight: .semibold)).foregroundColor(p.emerald)
                                        }
                                        if item.sellable {
                                            Button("出售") { sell(item) }.font(.system(size: 12)).foregroundColor(p.orange)
                                        }
                                        if item.canChangeLock {
                                            Button(item.locked ? "解锁" : "锁定") { toggleLock(item) }
                                                .font(.system(size: 11)).foregroundColor(p.secondary)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                .padding(16)
            }
            .background(p.background.ignoresSafeArea())
            .navigationTitle("农场背包")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button("关闭") { dismiss() } }
                ToolbarItem(placement: .confirmationAction) {
                    Button { Task { await load() } } label: { Image(systemName: "arrow.clockwise") }
                }
            }
            .task { await load() }
            .sheet(item: $amountPrompt) { AmountPromptView(prompt: $0) }
        }
    }

    private func load() async {
        loading = true
        do { bag = try await store.api.fetchBag(accountId: store.currentAccountId) }
        catch let e as APIError { error = e.message }
        catch let err { error = err.localizedDescription }
        loading = false
    }

    private func use(_ item: BagItem) {
        let maxCount = bag.maxUseCount(item)
        guard maxCount > 0 else { return }
        amountPrompt = AmountPrompt(title: "使用 \(item.name)", caption: item.hoursText, maxCount: maxCount,
                                    confirmLabel: "使用", unit: "个") { count in
            Task {
                do {
                    let message = try await store.api.useBagItem(accountId: store.currentAccountId, item: item, count: count)
                    store.showToast(message); await load(); await store.refreshBag()
                } catch let e as APIError { error = e.message }
                catch let err { error = err.localizedDescription }
            }
        }
    }

    private func sell(_ item: BagItem) {
        amountPrompt = AmountPrompt(title: "出售 \(item.name)",
                                    caption: "单价 \(item.price) \(item.priceUnit) · 库存 \(Fmt.compact(item.count))",
                                    maxCount: Int(min(item.count, 9999)),
                                    confirmLabel: "出售", unit: "个") { count in
            Task {
                do {
                    try await store.api.sellBagItem(accountId: store.currentAccountId, item: item, count: Int64(count))
                    store.showToast("出售成功"); await load(); await store.refreshBag()
                } catch let e as APIError { error = e.message }
                catch let err { error = err.localizedDescription }
            }
        }
    }

    private func toggleLock(_ item: BagItem) {
        Task {
            do {
                try await store.api.lockBagItems(accountId: store.currentAccountId, uids: [item.uid], locked: !item.locked)
                await load()
            } catch let e as APIError { error = e.message }
            catch let err { error = err.localizedDescription }
        }
    }
}

struct PetSheet: View {
    @Environment(\.palette) private var p
    @Environment(\.dismiss) private var dismiss
    @EnvironmentObject private var store: AppStore
    @State private var pets = PetSnapshot()
    @State private var loading = true
    @State private var error = ""

    var body: some View {
        NavigationView {
            ScrollView {
                VStack(spacing: 12) {
                    if !error.isEmpty { ErrorBanner(message: error) }
                    Card {
                        VStack(alignment: .leading, spacing: 8) {
                            HStack {
                                Text("看门狗守护").font(.system(size: 16, weight: .semibold)).foregroundColor(p.text)
                                Spacer()
                                if pets.pendingGiftCount > 0 {
                                    Button("领取礼物 (\(pets.pendingGiftCount))") { claim() }
                                        .font(.system(size: 12, weight: .semibold)).foregroundColor(p.emerald)
                                }
                            }
                            ProgressLine(value: pets.maxProtectDurationSec > 0 ? Double(pets.protectDurationSec) / Double(pets.maxProtectDurationSec) : 0)
                            Text("守护 \(Fmt.duration(pets.protectDurationSec)) / 上限 \(Fmt.duration(pets.maxProtectDurationSec)) · 剩余 \(Fmt.duration(pets.remainingDurationSec))")
                                .font(.system(size: 11)).foregroundColor(p.secondary)
                        }
                    }
                    if loading {
                        LoadingState(text: "正在读取看门狗")
                    } else {
                        ForEach(pets.dogs) { dog in
                            Card(padding: 12) {
                                HStack(spacing: 10) {
                                    RemoteIcon(url: AssetURL.resolve(dog.image, base: store.serverURL), fallback: "dog.fill", size: 44)
                                    VStack(alignment: .leading, spacing: 3) {
                                        HStack(spacing: 5) {
                                            Text(dog.name).font(.system(size: 14, weight: .semibold)).foregroundColor(p.text)
                                            Pill(text: "Lv.\(dog.level)", color: p.indigo)
                                            if dog.active { Pill(text: "出战中", color: p.green, filled: true) }
                                        }
                                        Text(dog.rarityLabel + " · " + dog.skillDescription)
                                            .font(.system(size: 10)).foregroundColor(p.secondary).lineLimit(2)
                                    }
                                    Spacer()
                                    if dog.active {
                                        Button("收回") { withdraw() }.font(.system(size: 12)).foregroundColor(p.orange)
                                    } else if dog.owned {
                                        Button("出战") { deploy(dog.id) }.font(.system(size: 12, weight: .semibold)).foregroundColor(p.emerald)
                                    }
                                }
                            }
                        }
                        if !pets.foods.isEmpty {
                            Card {
                                VStack(alignment: .leading, spacing: 8) {
                                    Text("狗粮").font(.system(size: 14, weight: .semibold)).foregroundColor(p.text)
                                    ForEach(pets.foods) { food in
                                        HStack {
                                            RemoteIcon(url: AssetURL.resolve(food.image, base: store.serverURL), fallback: "takeoutbag.and.cup.and.straw.fill", size: 28)
                                            VStack(alignment: .leading, spacing: 1) {
                                                Text(food.name).font(.system(size: 13)).foregroundColor(p.text)
                                                Text("时长 \(Fmt.duration(food.durationSec)) · 库存 \(Fmt.compact(food.count))")
                                                    .font(.system(size: 10)).foregroundColor(p.secondary)
                                            }
                                            Spacer()
                                            let maxFeed = pets.maxFeedCount(food)
                                            Button("喂养") { feed(food) }
                                                .font(.system(size: 12, weight: .semibold))
                                                .foregroundColor(maxFeed > 0 ? p.emerald : p.secondary)
                                                .disabled(maxFeed <= 0)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                .padding(16)
            }
            .background(p.background.ignoresSafeArea())
            .navigationTitle("看门狗")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button("关闭") { dismiss() } }
            }
            .task { await load() }
        }
    }

    private func load() async {
        loading = true
        do { pets = try await store.api.fetchPets(accountId: store.currentAccountId) }
        catch let e as APIError { error = e.message }
        catch let err { error = err.localizedDescription }
        loading = false
    }

    private func deploy(_ id: Int) {
        Task {
            do { pets = try await store.api.deployDog(accountId: store.currentAccountId, dogId: id); store.showToast("看门狗已出战") }
            catch let e as APIError { error = e.message }
            catch let err { error = err.localizedDescription }
        }
    }

    private func withdraw() {
        Task {
            do { pets = try await store.api.withdrawDog(accountId: store.currentAccountId); store.showToast("看门狗已收回") }
            catch let e as APIError { error = e.message }
            catch let err { error = err.localizedDescription }
        }
    }

    private func feed(_ food: DogFood) {
        let maxCount = pets.maxFeedCount(food)
        guard maxCount > 0 else { return }
        Task {
            do { pets = try await store.api.useDogFood(accountId: store.currentAccountId, itemId: food.id, count: maxCount); store.showToast("已喂养 \(maxCount) 个 \(food.name)") }
            catch let e as APIError { error = e.message }
            catch let err { error = err.localizedDescription }
        }
    }

    private func claim() {
        Task {
            do {
                let pending = try await store.api.claimDogGifts(accountId: store.currentAccountId)
                store.showToast("已领取，剩余待领取 \(pending)")
                await load()
            } catch let e as APIError { error = e.message }
            catch let err { error = err.localizedDescription }
        }
    }
}
