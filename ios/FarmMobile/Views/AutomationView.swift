import Foundation
import SwiftUI

struct AutomationView: View {
    @Environment(\.palette) private var p
    @Environment(\.dismiss) private var dismiss
    @EnvironmentObject private var store: AppStore
    @State private var settings = AccountSettings()
    @State private var bagSeeds: [BagPriorityItem] = []
    @State private var seedCatalog: [SeedOption] = []
    @State private var ranchOptions: [RanchAnimalOption] = []
    @State private var scheduler: SchedulerSummary?
    @State private var loading = true
    @State private var saving = false
    @State private var error = ""
    @State private var showBagPriority = false

    private let strategies = [
        ("max_exp", "最大经验/时"), ("max_fert_exp", "最大普通肥经验/时"),
        ("max_profit", "最大净利润/时"), ("max_fert_profit", "最大普通肥净利润/时"),
        ("level", "最高等级作物"), ("preferred", "指定优先种子"), ("bag_priority", "背包种子优先"),
    ]
    private let bagFallbackStrategies = [
        ("level", "最高等级作物"), ("max_exp", "最大经验/时"), ("max_fert_exp", "最大普通肥经验/时"),
        ("max_profit", "最大净利润/时"), ("max_fert_profit", "最大普通肥净利润/时"), ("preferred", "指定优先种子"),
    ]
    private let fertilizers = [("both", "普通 + 有机"), ("smart", "普通 + 智能有机"), ("normal", "仅普通"), ("organic", "仅有机"), ("none", "不自动施肥")]
    private let landTypes = [("blue-crystal", "蓝晶土地"), ("purple-gold", "紫金土地"), ("gold", "金土地"),
                             ("black", "黑土地"), ("red", "红土地"), ("normal", "普通土地")]
    private let ranchStrategies = [("level", "按已解锁等级"), ("preferred", "指定动物"), ("bag_priority", "优先背包幼崽")]
    private let canStages = [
        ("", "未设置"), ("any", "可喂养时就用"), ("longest", "仅最长阶段"),
        ("to_production", "成长期 · 加速到生产期"), ("near_harvest", "成熟期 · 距收获 10 分钟内"),
    ]
    private let farmSwitches = [
        ("farm", "自动种植收获"), ("farm_harvest_tiangong", "自动收获天工作物"), ("farm_push", "推送触发巡田"),
        ("skip_own_weed_bug", "巡田时跳过一键务农"), ("land_upgrade", "自动升级土地"), ("task", "自动做任务"),
        ("sell", "自动卖果实"), ("fertilizer_gift", "自动填充化肥"),
    ]
    private let ranchSwitches = [
        ("ranch_harvest", "自动收获"), ("ranch_produce", "自动生产"), ("ranch_buy", "自动补栏（金券）"),
        ("ranch_upgrade", "自动升级建筑（金券）"), ("ranch_sell", "自动出售产物"), ("ranch_clean", "自动清理"),
        ("ranch_grass", "自动补充牧草"), ("ranch_grass_buy", "牧草不足时购买（金券）"),
        ("ranch_task", "自动领取任务奖励"),
        ("ranch_share", "每日分享礼包"), ("ranch_daily_benefit", "每日商城福利"),
    ]
    private let friendSwitches = [
        ("friend", "自动好友互动"), ("friend_steal", "自动偷菜"), ("friend_help", "自动帮忙"),
        ("friend_bad", "自动捣乱"), ("friend_help_exp_limit", "经验满不帮忙"),
        ("friend_help_protect_dog_ignore_exp_limit", "护主犬经验满仍帮忙"),
    ]
    private let ranchFriendSwitches = [
        ("ranch_friend_steal", "自动偷取好友产物"), ("ranch_friend_clean", "自动帮好友清理"),
        ("ranch_stroke", "自动抚摸动物"), ("ranch_friend_mosquito", "自动放蚊子"),
    ]
    private let mysterySwitches = [
        ("mystery_shop_allow_gold", "金币"), ("mystery_shop_allow_coupon", "点券"),
        ("mystery_shop_allow_gold_bean", "金豆豆"), ("mystery_shop_allow_diamond", "钻石"),
    ]

    var body: some View {
        List {
            if loading {
                Section { HStack { Spacer(); ProgressView(); Spacer() } }
            }
            if !error.isEmpty {
                Section { ErrorBanner(message: error) }
            }
            strategySection
            intervalSection
            fertilizerSection
            friendSection
            ranchSection
            canFeedSection
            quietSection
            schedulerSection
            saveSection
        }
        .listStyle(.insetGrouped)
        .scrollContentBackground(.hidden)
        .background(p.background)
        .navigationTitle("自动化设置")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .confirmationAction) {
                Button(saving ? "保存中…" : "保存") { Task { await save() } }
                    .disabled(saving || loading || settings.plantingStrategy.isEmpty)
            }
        }
        .task { await load() }
        .sheet(isPresented: $showBagPriority) {
            BagPriorityEditor(values: $settings.bagSeedPriority, pool: priorityEntryPool, catalog: seedCatalog)
        }
    }

    private var strategySection: some View {
        Section("种植策略") {
            Picker("种植策略", selection: $settings.plantingStrategy) {
                ForEach(strategies.indices, id: \.self) { i in Text(strategies[i].1).tag(strategies[i].0) }
            }
            if settings.plantingStrategy == "bag_priority" {
                Picker("第二优先策略", selection: $settings.bagSeedFallbackStrategy) {
                    ForEach(bagFallbackStrategies.indices, id: \.self) { i in
                        Text(bagFallbackStrategies[i].1).tag(bagFallbackStrategies[i].0)
                    }
                }
                Toggle("为高优先级多格种子预留土地", isOn: $settings.bagSeedMultiLandReservationEnabled)
                    .tint(p.emerald)
                Button {
                    showBagPriority = true
                } label: {
                    HStack(spacing: 8) {
                        VStack(alignment: .leading, spacing: 3) {
                            Text("背包种子优先顺序").foregroundColor(p.text)
                            Text(settings.bagSeedPriority.isEmpty
                                 ? "默认按背包顺序补种"
                                 : "已排 \(settings.bagSeedPriority.count) 项，点击调整")
                                .font(.system(size: 11)).foregroundColor(p.secondary)
                        }
                        Spacer()
                        Image(systemName: "chevron.right")
                            .font(.system(size: 12, weight: .semibold)).foregroundColor(p.secondary)
                    }
                }
                .buttonStyle(.plain)
            }
            if settings.plantingStrategy == "preferred"
                || (settings.plantingStrategy == "bag_priority" && settings.bagSeedFallbackStrategy == "preferred") {
                Picker("优先种子", selection: $settings.preferredSeedId) {
                    Text("未指定").tag(Int64(0))
                    ForEach(seedCatalog) { seed in
                        Text("\(seed.name)\(seed.requiredLevel > 0 ? " (Lv.\(seed.requiredLevel))" : "")")
                            .tag(seed.seedId)
                    }
                }
            }
            Toggle("种植顺序随机", isOn: $settings.plantOrderRandom).tint(p.emerald)
            NumberField(title: "种植延迟（秒）", value: $settings.plantDelaySeconds, range: 0...60)
            NumberField(title: "偷菜延迟（秒）", value: $settings.stealDelaySeconds, range: 0...300)
        }
    }

    private var priorityEntryPool: [AutomationPriorityEntry] {
        var byId: [Int: AutomationPriorityEntry] = [:]
        for seed in seedCatalog {
            byId[Int(seed.seedId)] = AutomationPriorityEntry(id: Int(seed.seedId), name: seed.name, count: 0)
        }
        for seed in bagSeeds {
            let fallbackName = byId[seed.id]?.name ?? "种子 #\(seed.id)"
            byId[seed.id] = AutomationPriorityEntry(
                id: seed.id,
                name: seed.name.isEmpty ? fallbackName : seed.name,
                count: seed.count
            )
        }
        return Array(byId.values)
    }

    private var intervalSection: some View {
        Section("巡查间隔") {
            NumberField(title: "农场最短（秒）", value: $settings.farmMinSec, range: 1...3600)
            NumberField(title: "农场最长（秒）", value: $settings.farmMaxSec, range: 1...3600)
            NumberField(title: "好友最短（秒）", value: $settings.friendMinSec, range: 1...3600)
            NumberField(title: "好友最长（秒）", value: $settings.friendMaxSec, range: 1...3600)
        }
    }

    private var fertilizerSection: some View {
        Section("施肥策略") {
            Picker("施肥方式", selection: $settings.fertilizerMode) {
                ForEach(fertilizers.indices, id: \.self) { i in Text(fertilizers[i].1).tag(fertilizers[i].0) }
            }
            if settings.fertilizerMode == "smart" {
                NumberField(title: "智能施肥窗口（秒）", value: $settings.fertilizerSmartSeconds, range: 30...3600)
            }
            ForEach(landTypes.indices, id: \.self) { i in
                let key = landTypes[i].0
                let label = landTypes[i].1
                Toggle(label, isOn: Binding(
                    get: { settings.fertilizerLandTypes.contains(key) },
                    set: { on in
                        if on { if !settings.fertilizerLandTypes.contains(key) { settings.fertilizerLandTypes.append(key) } }
                        else { settings.fertilizerLandTypes.removeAll { $0 == key } }
                    }
                )).tint(p.emerald)
            }
            NumberField(title: "补普通肥数量", value: $settings.fertilizerBuyNormalCount, range: 0...10000)
            NumberField(title: "普通肥阈值（小时）", value: $settings.fertilizerBuyNormalThresholdHours, range: 0...990)
            NumberField(title: "补有机肥数量", value: $settings.fertilizerBuyOrganicCount, range: 0...10000)
            NumberField(title: "有机肥阈值（小时）", value: $settings.fertilizerBuyOrganicThresholdHours, range: 0...990)
            NumberField(title: "补肥检查间隔（分）", value: $settings.fertilizerBuyCheckIntervalMinutes, range: 1...1440)
        }
    }

    private var friendSection: some View {
        Section("好友互动") {
            NumberField(title: "自动接受好友最低等级", value: $settings.autoAcceptFriendMinLevel, range: 0...200)
            Toggle("要求对方等级不低于自己", isOn: $settings.autoAcceptRequireOwnLevel).tint(p.emerald)
            Toggle("按收获/偷取条件自动接受", isOn: $settings.autoAcceptHarvestStealEnabled).tint(p.emerald)
            if settings.autoAcceptHarvestStealEnabled {
                NumberField(title: "最低收获数量", value: $settings.autoAcceptHarvestStealHarvest, range: 0...9999)
                NumberField(title: "最低偷取数量", value: $settings.autoAcceptHarvestStealSteal, range: 1...9999)
            }
            ForEach(friendSwitches.indices, id: \.self) { i in
                Toggle(friendSwitches[i].1, isOn: boolBinding(friendSwitches[i].0)).tint(p.emerald)
            }
        }
    }

    private var ranchSection: some View {
        Section("养殖策略") {
            Picker("牧场策略", selection: $settings.ranchStrategy) {
                ForEach(ranchStrategies.indices, id: \.self) { i in Text(ranchStrategies[i].1).tag(ranchStrategies[i].0) }
            }
            Picker("背包幼崽兜底", selection: $settings.ranchBagFallback) {
                Text("按已解锁等级").tag("level")
                Text("指定动物").tag("preferred")
                Text("优先背包幼崽").tag("bag_priority")
            }
            let coopOptions = ranchOptions.filter { $0.kind != "shed" }
            let shedOptions = ranchOptions.filter { $0.kind != "coop" }
            Picker("指定窝动物", selection: $settings.ranchCoopAnimal) {
                Text("不指定").tag(0)
                ForEach(coopOptions) { option in Text(option.label).tag(option.value) }
            }
            Picker("指定棚动物", selection: $settings.ranchShedAnimal) {
                Text("不指定").tag(0)
                ForEach(shedOptions) { option in Text(option.label).tag(option.value) }
            }
            NumberField(title: "牧草低于百分比补充", value: $settings.ranchGrassThresholdPercent, range: 1...100)
            NumberField(title: "牧场检查间隔（分）", value: $settings.ranchCheckMinutes, range: 1...60)
            ForEach(ranchSwitches.indices, id: \.self) { i in
                Toggle(ranchSwitches[i].1, isOn: boolBinding(ranchSwitches[i].0)).tint(p.orange)
            }
            ForEach(ranchFriendSwitches.indices, id: \.self) { i in
                Toggle(ranchFriendSwitches[i].1, isOn: boolBinding(ranchFriendSwitches[i].0)).tint(p.orange)
            }
            Text("神秘商人购买货币").font(.system(size: 11)).foregroundColor(p.secondary)
            ForEach(mysterySwitches.indices, id: \.self) { i in
                Toggle(mysterySwitches[i].1, isOn: boolBinding(mysterySwitches[i].0)).tint(p.orange)
            }
        }
    }

    private var canFeedSection: some View {
        Section("罐头喂养策略") {
            Toggle("自动填充库存罐头", isOn: boolBinding("ranch_fill_cans")).tint(p.orange)
            Toggle("普通罐头喂养", isOn: boolBinding("ranch_feed_normal")).tint(p.orange)
            if settings.automationBooleans["ranch_feed_normal"] ?? true {
                Picker("普通罐头使用时机", selection: Binding(
                    get: { settings.ranchFeedNormalStage ?? "" },
                    set: { settings.ranchFeedNormalStage = $0.isEmpty ? nil : $0 }
                )) {
                    ForEach(canStages.indices, id: \.self) { i in
                        Text(canStages[i].1).tag(canStages[i].0)
                    }
                }
            }
            Toggle("有机罐头喂养", isOn: boolBinding("ranch_feed_organic")).tint(p.orange)
            if settings.automationBooleans["ranch_feed_organic"] ?? true {
                Picker("有机罐头使用时机", selection: Binding(
                    get: { settings.ranchFeedOrganicStage ?? "" },
                    set: { settings.ranchFeedOrganicStage = $0.isEmpty ? nil : $0 }
                )) {
                    ForEach(canStages.indices, id: \.self) { i in
                        Text(canStages[i].1).tag(canStages[i].0)
                    }
                }
            }
            Text("普通罐头会在动物标记 limitedCanUsed 后跳过；有机罐头不受该限制。")
                .font(.system(size: 10)).foregroundColor(p.secondary)
        }
    }

    private var quietSection: some View {
        Section("静默时段") {
            Toggle("启用好友静默时段", isOn: $settings.quietHoursEnabled).tint(p.emerald)
            if settings.quietHoursEnabled {
                HStack {
                    Text("开始").foregroundColor(p.text)
                    Spacer()
                    TextField("01:00", text: $settings.quietHoursStart).multilineTextAlignment(.trailing)
                }
                HStack {
                    Text("结束").foregroundColor(p.text)
                    Spacer()
                    TextField("07:30", text: $settings.quietHoursEnd).multilineTextAlignment(.trailing)
                }
                Toggle("静默期间继续农场", isOn: $settings.quietContinueFarm).tint(p.emerald)
            }
            if settings.emailClaimHour != nil {
                NumberField(title: "邮件领取整点", value: Binding(
                    get: { settings.emailClaimHour ?? 0 },
                    set: { settings.emailClaimHour = $0 }
                ), range: 0...23)
            }
        }
    }

    @ViewBuilder
    private var schedulerSection: some View {
        if let scheduler {
            Section("定时任务") {
                HStack {
                    Text("Worker").foregroundColor(p.text)
                    Spacer()
                    Pill(text: scheduler.workerRunning ? "运行中" : "未运行",
                         color: scheduler.workerRunning ? p.green : p.orange, dot: true)
                }
                Text("任务数 \(scheduler.workerTaskCount) · 运行中 \(scheduler.runningTasks.count)")
                    .font(.system(size: 11)).foregroundColor(p.secondary)
                if !scheduler.runningTasks.isEmpty {
                    Text(scheduler.runningTasks.joined(separator: "、"))
                        .font(.system(size: 11)).foregroundColor(p.text)
                }
                if !scheduler.workerError.isEmpty { ErrorBanner(message: scheduler.workerError) }
            }
        }
    }

    private var saveSection: some View {
        Section {
            PrimaryButton(title: saving ? "正在保存…" : "保存自动化设置", icon: "checkmark.circle.fill", enabled: !saving && !loading) {
                Task { await save() }
            }
            .listRowBackground(Color.clear)
        }
    }

    private let boolDefaults: [String: Bool] = [
        "ranch_grass": true,
        "ranch_fill_cans": true,
        "ranch_feed_normal": true,
        "ranch_feed_organic": true,
        "show_manual_fertilizer": true,
    ]

    private func boolBinding(_ key: String) -> Binding<Bool> {
        Binding(get: { settings.automationBooleans[key] ?? boolDefaults[key] ?? false },
                set: { settings.automationBooleans[key] = $0 })
    }

    private func load() async {
        loading = true
        error = ""
        do {
            async let cfg = store.api.fetchSettings(accountId: store.currentAccountId)
            async let seeds = store.api.fetchBagSeeds(accountId: store.currentAccountId)
            async let catalog = store.api.fetchSeedCatalog(accountId: store.currentAccountId)
            async let options = store.api.fetchRanchOptions()
            async let sched = store.api.fetchScheduler(accountId: store.currentAccountId)
            settings = try await cfg
            bagSeeds = (try? await seeds) ?? []
            seedCatalog = (try? await catalog) ?? []
            ranchOptions = (try? await options) ?? []
            scheduler = try? await sched
            if settings.bagSeedPriority.isEmpty { settings.bagSeedPriority = bagSeeds.map(\.id) }
        } catch let e as APIError { error = e.message }
        catch let err { error = err.localizedDescription }
        loading = false
    }

    private func save() async {
        saving = true
        let (ok, message) = await store.api.saveSettings(accountId: store.currentAccountId, model: settings)
        saving = false
        store.showToast(message)
        if ok { dismiss() }
    }
}

private struct AutomationPriorityEntry: Identifiable {
    var id: Int
    var name: String
    var count: Int64
}

private struct BagPriorityEditor: View {
    @Environment(\.palette) private var p
    @Environment(\.dismiss) private var dismiss
    @Binding var values: [Int]
    var pool: [AutomationPriorityEntry]
    var catalog: [SeedOption]

    private var rows: [AutomationPriorityEntry] {
        values.map { id in
            pool.first(where: { $0.id == id })
                ?? AutomationPriorityEntry(id: id, name: "种子 #\(id)", count: 0)
        }
    }

    var body: some View {
        NavigationView {
            List {
                prioritySection
                addSection
            }
            .scrollContentBackground(.hidden)
            .background(p.background)
            .navigationTitle("种子优先顺序")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("完成") { dismiss() }
                }
            }
        }
    }

    private var prioritySection: some View {
        Section {
            if rows.isEmpty {
                Text("暂无排序").foregroundColor(p.secondary)
            }
            ForEach(rows.indices, id: \.self) { index in
                priorityRow(rows[index], index: index)
            }
        } header: {
            Text("背包种子优先顺序")
        } footer: {
            Text("越靠前越优先。多格种子会按该顺序占用连续土地；背包种子用完后按第二优先策略补种。")
                .font(.system(size: 10))
        }
    }

    private func priorityRow(_ row: AutomationPriorityEntry, index: Int) -> some View {
        HStack(spacing: 8) {
            Text("\(index + 1). \(row.name)")
                .font(.system(size: 14)).foregroundColor(p.text)
                .lineLimit(1)
            Spacer()
            Text("库存 \(Fmt.compact(row.count))")
                .font(.system(size: 10)).foregroundColor(p.secondary)
            Button {
                move(index, -1)
            } label: {
                Image(systemName: "chevron.up")
            }
            .disabled(index == 0)
            Button {
                move(index, 1)
            } label: {
                Image(systemName: "chevron.down")
            }
            .disabled(index >= rows.count - 1)
            Button(role: .destructive) {
                values.removeAll { $0 == row.id }
            } label: {
                Image(systemName: "minus.circle.fill")
            }
        }
    }

    private var addSection: some View {
        Section {
            if catalog.isEmpty {
                Text("暂无可添加种子").foregroundColor(p.secondary)
            } else {
                Menu {
                    ForEach(catalog.filter { !values.contains(Int($0.seedId)) }) { seed in
                        Button("\(seed.name)\(seed.requiredLevel > 0 ? " (Lv.\(seed.requiredLevel))" : "")") {
                            values.append(Int(seed.seedId))
                        }
                    }
                } label: {
                    Label("添加种子", systemImage: "plus.circle.fill")
                        .foregroundColor(p.emerald)
                }
            }
        }
    }

    private func move(_ index: Int, _ delta: Int) {
        let target = index + delta
        guard values.indices.contains(index), values.indices.contains(target) else { return }
        values.swapAt(index, target)
    }
}
