import Foundation
import SwiftUI

struct OverviewView: View {
    @Environment(\.palette) private var p
    @EnvironmentObject private var store: AppStore
    @State private var exactStats: Set<String> = []
    @State private var ranchSnapshot: RanchViewSnapshot?
    @State private var showClearLogs = false
    @State private var showMall = false
    @State private var mallInitialTab = 0

    private var status: StatusSnapshot? { store.status }

    var body: some View {
        ScrollView {
            VStack(spacing: 14) {
                TopBar(showAccount: true)
                if !store.connectionWarning.isEmpty {
                    HStack(spacing: 8) {
                        ErrorBanner(message: store.connectionWarning)
                        Button {
                            Task { await store.connectActive() }
                        } label: {
                            Text("重连")
                                .font(.system(size: 12, weight: .semibold))
                                .foregroundColor(p.emerald)
                                .padding(.horizontal, 10)
                                .padding(.vertical, 7)
                                .background(p.card)
                                .clipShape(Capsule())
                        }
                        .buttonStyle(.plain)
                    }
                    .padding(.horizontal, 16)
                }
                accountCard.padding(.horizontal, 16)
                quickActions.padding(.horizontal, 16)
                assetsSection.padding(.horizontal, 16)
                stockSection.padding(.horizontal, 16)
                countdownSection.padding(.horizontal, 16)
                if store.space == .ranch {
                    ranchSection.padding(.horizontal, 16)
                }
                dailyGiftsSection.padding(.horizontal, 16)
                operationsSection.padding(.horizontal, 16)
                logsSection.padding(.horizontal, 16)
                Color.clear.frame(height: 20)
            }
            .padding(.top, 4)
        }
        .background(p.background.ignoresSafeArea())
        .navigationBarHidden(true)
        .refreshable {
            await store.refreshOverview()
            await loadRanch()
        }
        .task(id: store.currentAccountId) {
            if store.status == nil { await store.refreshOverview() }
            if store.space == .ranch { await loadRanch() }
        }
        .task(id: store.space) {
            if store.space == .ranch { await loadRanch() }
        }
        .navigationDestination(isPresented: $showMall) {
            MallView(initialTab: mallInitialTab)
        }
        .confirmationDialog("清空运行日志？", isPresented: $showClearLogs) {
            Button("清空当前账号日志", role: .destructive) {
                Task { await store.clearLogs() }
            }
            Button("取消", role: .cancel) {}
        }
    }

    private var accountCard: some View {
        Card {
            VStack(alignment: .leading, spacing: 12) {
                if let account = store.currentAccount {
                    HStack(spacing: 12) {
                        ZStack(alignment: .bottomTrailing) {
                            AvatarView(url: status?.avatarUrl.isEmpty == false
                                       ? AssetURL.resolve(status?.avatarUrl ?? "", base: store.serverURL)
                                       : account.avatarURL(base: store.serverURL),
                                       name: account.displayTitle, size: 62)
                            Circle()
                                .fill(status?.connected == true ? p.green : p.secondary)
                                .frame(width: 13, height: 13)
                                .overlay(Circle().stroke(p.card, lineWidth: 2))
                        }
                        VStack(alignment: .leading, spacing: 6) {
                            HStack(spacing: 6) {
                                Text(status?.name.isEmpty == false ? status!.name : account.displayTitle)
                                    .font(.system(size: 22, weight: .bold))
                                    .foregroundColor(p.text)
                                    .lineLimit(1)
                                if let level = status?.level, level > 0 {
                                    Pill(text: "Lv.\(level)", color: p.emerald)
                                }
                            }
                            HStack(spacing: 6) {
                                Pill(text: status?.connected == true ? "在线" : "离线",
                                     color: status?.connected == true ? p.green : p.secondary, dot: true)
                                Pill(text: account.platformLabel,
                                     color: account.platformLabel == "微信" ? p.green : p.blue)
                                if let sec = status?.uptimeSec, sec > 0 {
                                    Pill(text: "已运行 \(Fmt.duration(sec))", color: p.secondary)
                                }
                            }
                        }
                    }
                    if let snapshot = status {
                        VStack(spacing: 6) {
                            HStack {
                                Text("升级经验").font(.system(size: 12)).foregroundColor(p.secondary)
                                Spacer()
                                Text("\(Fmt.full(snapshot.progress.current)) / \(Fmt.full(snapshot.progress.needed)) (\(Int(snapshot.progress.fraction * 100))%)")
                                    .font(.system(size: 12, weight: .medium)).foregroundColor(p.text)
                            }
                            ProgressLine(value: snapshot.progress.fraction)
                            HStack {
                                Text("经验速率 \(Fmt.compact(snapshot.sessionExpGained))/时")
                                    .font(.system(size: 12)).foregroundColor(p.secondary)
                                Spacer()
                                if !snapshot.levelForecastText.isEmpty {
                                    Text(snapshot.levelForecastText).font(.system(size: 12, weight: .semibold)).foregroundColor(p.emerald)
                                }
                            }
                        }
                        .padding(.top, 2)
                    }
                } else if store.accountsLoading {
                    LoadingState(text: "正在读取账号")
                } else {
                    EmptyStateView(icon: "person.crop.circle.badge.questionmark",
                                   title: "还没有可用账号",
                                   subtitle: "请在右上角账号管理中启动或添加账号")
                }
            }
        }
    }

    private var quickActions: some View {
        Card(padding: 10) {
            HStack(spacing: 4) {
                NavigationLink { ActivityCenterView() } label: {
                    quickTile("活动中心", icon: "party.popper.fill", color: p.orange)
                }
                .buttonStyle(.plain)
                NavigationLink { MallView() } label: {
                    quickTile("游戏商城", icon: "storefront.fill", color: p.blue)
                }
                .buttonStyle(.plain)
                NavigationLink { SolarCalendarView() } label: {
                    quickTile("节令日历", icon: "calendar", color: p.red)
                }
                .buttonStyle(.plain)
                NavigationLink { AutomationView() } label: {
                    quickTile("自动化设置", icon: "slider.horizontal.3", color: p.indigo)
                }
                .buttonStyle(.plain)
            }
        }
    }

    private func quickTile(_ title: String, icon: String, color: Color) -> some View {
        VStack(spacing: 7) {
            Image(systemName: icon)
                .font(.system(size: 20, weight: .semibold))
                .foregroundColor(.white)
                .frame(width: 44, height: 44)
                .background(color)
                .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
            Text(title).font(.system(size: 11, weight: .medium)).foregroundColor(p.text)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 4)
    }

    private var assetsSection: some View {
        VStack(spacing: 10) {
            SectionTitle(title: "资产概览")
            LazyVGrid(columns: [GridItem(.flexible(), spacing: 10), GridItem(.flexible(), spacing: 10)], spacing: 10) {
                StatTile(icon: "yensign.circle.fill", tint: p.orange, label: "金币",
                         value: exactStats.contains("gold") ? Fmt.full(status?.gold ?? 0) : Fmt.compact(status?.gold ?? 0),
                         subtitle: gainText(status?.sessionGoldGained),
                         onTap: { toggleExact("gold") })
                StatTile(icon: "ticket.fill", tint: p.green, label: "点券",
                         value: exactStats.contains("coupon") ? Fmt.full(status?.coupon ?? 0) : Fmt.compact(status?.coupon ?? 0),
                         subtitle: gainText(status?.sessionCouponGained),
                         onTap: { toggleExact("coupon") })
                StatTile(icon: "leaf.fill", tint: p.amber, label: "金豆豆",
                         value: exactStats.contains("bean") ? Fmt.full(status?.goldBean ?? 0) : Fmt.compact(status?.goldBean ?? 0),
                         onTap: { toggleExact("bean") })
                StatTile(icon: "diamond.fill", tint: p.cyan, label: "钻石",
                         value: exactStats.contains("diamond") ? Fmt.full(store.diamond) : Fmt.compact(store.diamond),
                         onTap: { toggleExact("diamond") })
            }
        }
    }

    private func gainText(_ value: Int64?) -> String? {
        guard let value, value != 0 else { return nil }
        let sign = value > 0 ? "+" : "-"
        return "本次 \(sign)\(Fmt.compact(abs(value)))"
    }

    private func toggleExact(_ key: String) {
        if exactStats.contains(key) { exactStats.remove(key) } else { exactStats.insert(key) }
    }

    private var stockSection: some View {
        VStack(spacing: 10) {
            SectionTitle(title: "库存与化肥", trailing: "补充化肥") {
                mallInitialTab = 2
                showMall = true
            }
            LazyVGrid(columns: [GridItem(.flexible(), spacing: 10), GridItem(.flexible(), spacing: 10)], spacing: 10) {
                stockCard(id: 1011, title: "普通化肥", icon: "drop.fill", tint: p.green)
                stockCard(id: 1012, title: "有机化肥", icon: "sparkles", tint: p.green)
                stockCard(id: 3001, title: "普通收藏点", icon: "star.fill", tint: p.indigo)
                stockCard(id: 3002, title: "典藏收藏点", icon: "star.circle.fill", tint: p.orange)
            }
        }
    }

    private func stockCard(id: Int, title: String, icon: String, tint: Color) -> some View {
        let item = store.bag.systemItems.first(where: { $0.id == id })
        return StatTile(icon: icon, tint: tint, label: title,
                        value: item?.hoursText.isEmpty == false ? item!.hoursText : Fmt.compact(item?.count ?? 0))
    }

    private var countdownSection: some View {
        VStack(spacing: 10) {
            SectionTitle(title: "下次巡查倒计时")
            Card {
                VStack(spacing: 10) {
                    HStack {
                        Label("农场巡查", systemImage: "leaf.circle.fill").foregroundColor(p.emerald)
                            .font(.system(size: 14, weight: .medium))
                        Spacer()
                        inspectionLabel(status?.nextFarmRunAt, quiet: status?.farmQuiet == true)
                            .font(.system(size: 14, weight: .semibold, design: .rounded))
                            .foregroundColor(p.text)
                    }
                    Divider().overlay(p.separator)
                    HStack {
                        Label("好友巡查", systemImage: "person.2.circle.fill").foregroundColor(p.blue)
                            .font(.system(size: 14, weight: .medium))
                        Spacer()
                        inspectionLabel(status?.nextFriendRunAt, quiet: status?.friendQuiet == true)
                            .font(.system(size: 14, weight: .semibold, design: .rounded))
                            .foregroundColor(p.text)
                    }
                }
            }
        }
    }

    @ViewBuilder
    private func inspectionLabel(_ date: Date?, quiet: Bool) -> some View {
        if status?.connected != true {
            Text("账号未登录")
        } else if quiet {
            Text("静默时段")
        } else {
            CountdownLabel(date: date, expiredText: "等待下次巡查", emptyText: "等待下次巡查")
        }
    }

    private var ranchSection: some View {
        VStack(spacing: 10) {
            SectionTitle(title: "牧场概览", trailing: "牧场商城") {
                mallInitialTab = 0
                showMall = true
            }
            if let snap = ranchSnapshot {
                Card {
                    VStack(alignment: .leading, spacing: 10) {
                        HStack {
                            Text("Lv.\(snap.base.level)").font(.system(size: 20, weight: .bold)).foregroundColor(p.text)
                            Pill(text: "牧草 \(Fmt.compact(snap.base.grass))", color: p.green, dot: true)
                            Spacer()
                            if let hourglass = snap.base.hourglassSeconds {
                                Pill(text: "沙漏 \(Fmt.duration(hourglass))", color: p.indigo)
                            }
                        }
                        if snap.base.neededExp > 0 {
                            ProgressLine(value: Double(snap.base.currentExp) / Double(snap.base.neededExp), color: p.orange)
                            Text("经验 \(Fmt.full(snap.base.currentExp)) / \(Fmt.full(snap.base.neededExp))")
                                .font(.system(size: 11)).foregroundColor(p.secondary)
                        }
                        HStack(spacing: 10) {
                            ranchMetric("待处理", "\(snap.base.hazardsCount)")
                            ranchMetric("可收", "\(snap.base.shelvesCollectableCount)")
                            ranchMetric("动物", "\(snap.base.pens.reduce(0) { $0 + $1.animals.count })")
                        }
                        if let career = snap.base.career {
                            Divider().overlay(p.separator)
                            HStack {
                                Text("牧场生涯").font(.system(size: 13, weight: .semibold)).foregroundColor(p.text)
                                Spacer()
                                Text("收获 \(Fmt.compact(career.harvest)) · 成熟 \(Fmt.compact(career.mature)) · 收偷比 \(career.ratioText)")
                                    .font(.system(size: 11)).foregroundColor(p.secondary)
                            }
                        }
                        if !snap.base.notice.isEmpty {
                            Text(snap.base.notice).font(.system(size: 11)).foregroundColor(p.orange)
                        }
                    }
                }
            } else {
                Card { LoadingState(text: "正在读取牧场") }
            }
        }
    }

    private func ranchMetric(_ label: String, _ value: String) -> some View {
        VStack(spacing: 2) {
            Text(value).font(.system(size: 18, weight: .bold)).foregroundColor(p.text)
            Text(label).font(.system(size: 10)).foregroundColor(p.secondary)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 6)
        .background(p.elevated)
        .clipShape(RoundedRectangle(cornerRadius: 9, style: .continuous))
    }

    @ViewBuilder
    private var dailyGiftsSection: some View {
        if let gifts = store.dailyGifts, !gifts.gifts.isEmpty {
            VStack(spacing: 10) {
                SectionTitle(title: "今日礼包")
                Card {
                    let columns = [GridItem(.flexible(), spacing: 8), GridItem(.flexible(), spacing: 8)]
                    LazyVGrid(columns: columns, spacing: 8) {
                        ForEach(gifts.gifts.prefix(8)) { gift in
                            HStack(spacing: 5) {
                                Circle().fill(gift.doneToday ? p.green : (gift.enabled ? p.orange : p.secondary))
                                    .frame(width: 6, height: 6)
                                Text(gift.label).font(.system(size: 11)).foregroundColor(p.text).lineLimit(1)
                                Spacer(minLength: 0)
                                Text(gift.doneToday ? "已完成" : (gift.enabled ? "待执行" : "未开启"))
                                    .font(.system(size: 9)).foregroundColor(p.secondary)
                            }
                        }
                    }
                }
            }
        }
    }

    @ViewBuilder
    private var operationsSection: some View {
        let ops = status?.operations ?? [:]
        if !ops.isEmpty || !(status?.ranchOperations.isEmpty ?? true) {
            VStack(spacing: 10) {
                SectionTitle(title: "今日收益统计")
                Card {
                    let columns = [GridItem(.flexible()), GridItem(.flexible()), GridItem(.flexible())]
                    LazyVGrid(columns: columns, spacing: 12) {
                        ForEach(ops.keys.sorted(), id: \.self) { key in
                            let value = ops[key] ?? 0
                            VStack(spacing: 2) {
                                Text("\(value)").font(.system(size: 18, weight: .bold)).foregroundColor(p.text)
                                Text(opLabel(key)).font(.system(size: 10)).foregroundColor(p.secondary)
                            }
                        }
                        if let ranch = status?.ranchOperations {
                            ForEach(ranch.keys.sorted(), id: \.self) { key in
                                let value = ranch[key] ?? 0
                                VStack(spacing: 2) {
                                    Text("\(value)").font(.system(size: 18, weight: .bold)).foregroundColor(p.orange)
                                    Text(ranchOpLabel(key)).font(.system(size: 10)).foregroundColor(p.secondary)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private func opLabel(_ key: String) -> String {
        let map = [
            "harvest": "收获", "plant": "种植", "fertilize": "施肥", "water": "浇水",
            "weed": "除草", "bug": "除虫", "clear": "清理", "help": "帮忙",
            "steal": "偷取", "sell": "出售", "upgrade": "升级", "friend": "好友",
            "task": "任务", "mail": "邮件", "gift": "礼包",
        ]
        return map[key] ?? key
    }

    private func ranchOpLabel(_ key: String) -> String {
        ["harvest": "牧场收获", "produce": "生产", "helpFarming": "帮忙清理", "steal": "偷取"][key] ?? key
    }

    private var visibleLogs: [RunLogEntry] {
        let scope = store.space.runLogScope
        return store.logs.filter { $0.scope == scope || $0.scope == .system }
    }

    private var logsSection: some View {
        let visible = visibleLogs
        return VStack(spacing: 10) {
            SectionTitle(title: "运行日志", trailing: "清空") { showClearLogs = true }
            Card {
                if visible.isEmpty {
                    EmptyStateView(icon: "text.alignleft", title: "暂无日志", subtitle: "账号运行后这里会实时刷新")
                } else {
                    VStack(spacing: 0) {
                        ForEach(visible.suffix(30).reversed()) { log in
                            HStack(alignment: .top, spacing: 8) {
                                Text(log.time).font(.system(size: 10, design: .monospaced))
                                    .foregroundColor(p.secondary).frame(width: 52, alignment: .leading)
                                Text(log.tag.isEmpty ? "日志" : log.tag)
                                    .font(.system(size: 9, weight: .semibold))
                                    .foregroundColor(log.isWarn ? p.orange : p.emerald)
                                    .padding(.horizontal, 5).padding(.vertical, 2)
                                    .background((log.isWarn ? p.orange : p.emerald).opacity(0.1))
                                    .clipShape(Capsule())
                                Text(log.msg).font(.system(size: 12)).foregroundColor(p.text)
                                    .multilineTextAlignment(.leading)
                                Spacer(minLength: 0)
                            }
                            .padding(.vertical, 6)
                            Divider().overlay(p.separator)
                        }
                    }
                }
            }
        }
    }

    private func loadRanch() async {
        guard store.space == .ranch, !store.currentAccountId.isEmpty else { return }
        ranchSnapshot = try? await store.api.fetchRanchView(accountId: store.currentAccountId, view: "animals")
    }
}
