import Foundation
import SwiftUI

struct DiscoverView: View {
    @Environment(\.palette) private var p
    @EnvironmentObject private var store: AppStore
    @State private var tab = 0

    var body: some View {
        VStack(spacing: 8) {
            TopBar()
            HStack(spacing: 4) {
                ForEach(Array(["活动", "商城", "节令"].indices), id: \.self) { index in
                    let title = ["活动", "商城", "节令"][index]
                    Button {
                        withAnimation(.easeOut(duration: 0.15)) { tab = index }
                    } label: {
                        Text(title)
                            .font(.system(size: 14, weight: .semibold))
                            .foregroundColor(tab == index ? .white : p.secondary)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 8)
                            .background(tab == index ? (store.space == .farm ? p.emerald : p.orange) : Color.clear)
                            .clipShape(Capsule())
                    }
                    .buttonStyle(.plain)
                }
            }
            .padding(3)
            .background(p.card)
            .clipShape(Capsule())
            .padding(.horizontal, 16)

            Group {
                switch tab {
                case 0: ActivityCenterView(showsTopBar: false)
                case 1: MallView(showsTopBar: false)
                default: SolarCalendarView(showsTopBar: false)
                }
            }
        }
        .padding(.top, 4)
        .background(p.background.ignoresSafeArea())
        .navigationBarHidden(true)
    }
}

private struct ActivityLoadContext: Equatable {
    var accountId: String
    var space: GameSpace
    var isOnline: Bool
}

struct ActivityCenterView: View {
    @Environment(\.palette) private var p
    @EnvironmentObject private var store: AppStore
    var showsTopBar = true
    @State private var items: [ActivityDirectoryItem] = []
    @State private var loading = false
    @State private var error = ""
    @State private var now = Date()
    @State private var showEnded = false
    @State private var loadGeneration = 0

    private struct Groups {
        var timed: [ActivityDirectoryItem] = []
        var standing: [ActivityDirectoryItem] = []
        var ended: [ActivityDirectoryItem] = []
    }

    private struct EndedMonth: Identifiable {
        var id: String
        var items: [ActivityDirectoryItem]
    }

    var body: some View {
        ScrollView {
            VStack(spacing: 12) {
                if showsTopBar { TopBar() }
                if store.status == nil && store.overviewLoading {
                    Card { LoadingState(text: "正在读取账号状态") }.padding(.horizontal, 16)
                } else if !store.isAccountOnline {
                    Card {
                        EmptyStateView(icon: "wifi.slash", title: "账号未在线",
                                       subtitle: "请先在总览启动当前账号，活动目录与领奖需要在线连接")
                    }.padding(.horizontal, 16)
                } else if loading && items.isEmpty {
                    Card { LoadingState(text: "正在同步活动目录") }.padding(.horizontal, 16)
                } else if items.isEmpty {
                    emptyState
                } else {
                    if !error.isEmpty { ErrorBanner(message: error).padding(.horizontal, 16) }
                    let groups = classify(items)
                    if !groups.timed.isEmpty {
                        SectionTitle(title: store.space == .ranch ? "牧场活动 (\(groups.timed.count))" : "限时活动 (\(groups.timed.count))",
                                     trailing: "刷新") { Task { await load() } }
                            .padding(.horizontal, 16)
                        ForEach(groups.timed) { item in
                            activityRow(item).padding(.horizontal, 16)
                        }
                    }
                    if !groups.standing.isEmpty {
                        SectionTitle(title: "常驻活动 (\(groups.standing.count))")
                            .padding(.horizontal, 16)
                        ForEach(groups.standing) { item in
                            activityRow(item).padding(.horizontal, 16)
                        }
                    }
                    if !groups.ended.isEmpty {
                        endedToggle(count: groups.ended.count)
                            .padding(.horizontal, 16)
                        if showEnded {
                            ForEach(endedMonths(groups.ended)) { month in
                                Text(month.id)
                                    .font(.system(size: 11, weight: .semibold))
                                    .foregroundColor(p.secondary)
                                    .frame(maxWidth: .infinity, alignment: .leading)
                                    .padding(.horizontal, 20)
                                    .padding(.top, 2)
                                ForEach(month.items) { item in
                                    activityRow(item).padding(.horizontal, 16)
                                }
                            }
                        }
                    }
                    if groups.timed.isEmpty && groups.standing.isEmpty && groups.ended.isEmpty {
                        emptyState
                    }
                }
                Color.clear.frame(height: 20)
            }
            .padding(.top, 4)
        }
        .background(p.background.ignoresSafeArea())
        .refreshable { await load() }
        .onChange(of: store.space) { _ in
            items = []
            showEnded = false
            error = ""
        }
        .task(id: ActivityLoadContext(accountId: store.currentAccountId, space: store.space, isOnline: store.isAccountOnline)) {
            await load()
        }
        .task {
            while !Task.isCancelled {
                try? await Task.sleep(nanoseconds: 60_000_000_000)
                if Task.isCancelled { return }
                now = Date()
            }
        }
    }

    private var emptyState: some View {
        Card {
            EmptyStateView(icon: "party.popper", title: store.space == .ranch ? "暂无牧场活动" : "暂无进行中活动",
                           subtitle: "下拉刷新同步服务器最新活动目录",
                           actionTitle: "立即刷新", action: { Task { await load() } })
        }
        .padding(.horizontal, 16)
    }

    private func endedToggle(count: Int) -> some View {
        Button {
            withAnimation(.easeOut(duration: 0.2)) { showEnded.toggle() }
        } label: {
            HStack {
                Text("往期活动 (\(count))")
                    .font(.system(size: 15, weight: .semibold)).foregroundColor(p.text)
                Spacer()
                Image(systemName: showEnded ? "chevron.up" : "chevron.down")
                    .font(.system(size: 11, weight: .bold)).foregroundColor(p.secondary)
            }
            .padding(.horizontal, 16).padding(.vertical, 12)
            .background(p.card)
            .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
        }
        .buttonStyle(.plain)
    }

    private var nowMs: Int64 { Int64(now.timeIntervalSince1970 * 1000) }

    private func isNewbie(_ item: ActivityDirectoryItem) -> Bool {
        let ids = Set([item.id] + item.activityIds)
        return ids.contains("2026092200") || ids.contains("2026092201") || item.name.contains("新田园")
    }

    private func isEnded(_ item: ActivityDirectoryItem) -> Bool {
        guard item.hasEndTime else { return false }
        return nowMs >= item.endTimeMs
    }

    private func isUpcoming(_ item: ActivityDirectoryItem) -> Bool {
        guard item.hasStartTime else { return false }
        return item.startTimeMs > nowMs
    }

    private func canOpen(_ item: ActivityDirectoryItem) -> Bool {
        item.typeKey != "generic" && !isEnded(item) && !isUpcoming(item)
    }

    private func classify(_ raw: [ActivityDirectoryItem]) -> Groups {
        var timed: [(item: ActivityDirectoryItem, order: Int)] = []
        var standing: [ActivityDirectoryItem] = []
        var ended: [ActivityDirectoryItem] = []
        for item in raw {
            let supported = item.typeKey != "generic"
            let duration = (item.hasStartTime && item.hasEndTime) ? item.endTimeMs - item.startTimeMs : 0
            if isEnded(item) {
                ended.append(item)
            } else if isNewbie(item) || (!supported && duration > 365 * 86_400_000) {
                standing.append(item)
            } else {
                let order = isUpcoming(item) ? 2 : (supported ? 0 : 1)
                timed.append((item, order))
            }
        }
        let sortedTimed = timed.sorted {
            if $0.order != $1.order { return $0.order < $1.order }
            return ($0.item.endTimeMs == 0 ? Int64.max : $0.item.endTimeMs)
                < ($1.item.endTimeMs == 0 ? Int64.max : $1.item.endTimeMs)
        }.map { $0.item }
        return Groups(timed: sortedTimed, standing: standing, ended: ended)
    }

    private func endedMonths(_ list: [ActivityDirectoryItem]) -> [EndedMonth] {
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy 年 M 月结束"
        formatter.timeZone = TimeZone(identifier: "Asia/Shanghai")
        var seen = Set<String>()
        var order: [String] = []
        var map: [String: [ActivityDirectoryItem]] = [:]
        for item in list.sorted(by: { $0.endTimeMs > $1.endTimeMs }) {
            let key = "\(item.name)|\(item.startTimeMs)|\(item.endTimeMs)"
            if seen.contains(key) { continue }
            seen.insert(key)
            let label: String
            if item.hasEndTime, item.endTimeMs >= 946_684_800_000, let date = dateFromMs(item.endTimeMs) {
                label = formatter.string(from: date)
            } else {
                label = "历史活动"
            }
            if map[label] == nil { order.append(label) }
            map[label, default: []].append(item)
        }
        return order.map { EndedMonth(id: $0, items: map[$0] ?? []) }
    }

    @ViewBuilder
    private func activityRow(_ item: ActivityDirectoryItem) -> some View {
        if canOpen(item) {
            NavigationLink { ActivityDetailView(item: item) } label: {
                activityCard(item)
            }
            .buttonStyle(.plain)
        } else {
            Button {
                if isEnded(item) {
                    store.showToast("「\(item.name)」已结束")
                } else if isUpcoming(item) {
                    store.showToast("「\(item.name)」尚未开始")
                } else {
                    store.showToast("「\(item.name)」暂未适配移动端玩法")
                }
            } label: {
                activityCard(item)
            }
            .buttonStyle(.plain)
        }
    }

    private func activityCard(_ item: ActivityDirectoryItem) -> some View {
        let supported = item.typeKey != "generic"
        let ended = isEnded(item)
        let upcoming = isUpcoming(item)
        let open = canOpen(item)
        let statusText = ended ? "已结束" : (upcoming ? "未开始" : (supported ? "进行中" : "暂未适配"))
        let statusColor = ended ? p.secondary : (upcoming ? p.orange : (supported ? p.green : p.secondary))
        let accent = store.space == .farm ? p.emerald : p.orange
        let scheduleText: String = {
            if ended {
                if let end = item.endAt { return "已于 " + Fmt.dateTime(end) + " 结束" }
                return "活动已结束"
            }
            if upcoming, let start = item.startAt {
                return "距开启 " + Fmt.duration(max(1, Int64(start.timeIntervalSince(now))))
            }
            if let end = item.endAt, end > now {
                return "剩余 " + Fmt.duration(Int64(end.timeIntervalSince(now)))
            }
            if let start = item.startAt, let end = item.endAt {
                return Fmt.dateTime(start) + " ~ " + Fmt.dateTime(end)
            }
            return "长期开放"
        }()
        return Card {
            HStack(spacing: 12) {
                ZStack {
                    RoundedRectangle(cornerRadius: 12, style: .continuous)
                        .fill((open ? accent : p.secondary).opacity(0.12))
                        .frame(width: 46, height: 46)
                    Image(systemName: icon(for: item))
                        .font(.system(size: 20, weight: .semibold))
                        .foregroundColor(open ? accent : p.secondary)
                }
                VStack(alignment: .leading, spacing: 5) {
                    HStack(spacing: 6) {
                        Text(item.name)
                            .font(.system(size: 16, weight: .semibold)).foregroundColor(p.text)
                            .lineLimit(1)
                        Pill(text: statusText, color: statusColor, dot: !ended)
                        Spacer(minLength: 0)
                    }
                    HStack(spacing: 5) {
                        Image(systemName: "clock").font(.system(size: 10)).foregroundColor(p.secondary)
                        Text(scheduleText)
                            .font(.system(size: 11, design: .monospaced)).foregroundColor(p.secondary)
                            .lineLimit(1)
                    }
                }
                if open {
                    Image(systemName: "chevron.right")
                        .font(.system(size: 12, weight: .semibold)).foregroundColor(p.secondary)
                } else if !supported && !ended {
                    Image(systemName: "sparkles").font(.system(size: 12)).foregroundColor(p.secondary)
                }
            }
        }
        .opacity(ended ? 0.66 : 1)
    }

    private func icon(for item: ActivityDirectoryItem) -> String {
        switch item.typeKey {
        case "muge": return "music.note"
        case "milktea": return "cup.and.saucer.fill"
        case "share": return "heart.fill"
        case "autumn": return "leaf.fill"
        case "stellar": return "star.fill"
        case "qixi": return "sparkles"
        case "qingmei": return "wineglass.fill"
        case "charity": return "hand.raised.fill"
        case "weather": return "cloud.rain.fill"
        default: return "sparkles"
        }
    }

    private func load() async {
        loadGeneration += 1
        let generation = loadGeneration
        let accountId = store.currentAccountId
        let space = store.space
        guard !accountId.isEmpty, store.isAccountOnline else {
            items = []
            showEnded = false
            error = ""
            loading = false
            return
        }
        loading = true
        error = ""
        do {
            let result = try await store.api.fetchActivityDirectory(accountId: accountId, isRanch: space == .ranch)
            guard generation == loadGeneration, accountId == store.currentAccountId,
                  space == store.space, store.isAccountOnline else { return }
            items = result
        } catch let e as APIError {
            guard generation == loadGeneration, accountId == store.currentAccountId, space == store.space else { return }
            error = e.message
        } catch let err {
            guard generation == loadGeneration, accountId == store.currentAccountId, space == store.space else { return }
            error = err.localizedDescription
        }
        loading = false
    }
}

struct ActivityDetailView: View {
    @EnvironmentObject private var store: AppStore
    var item: ActivityDirectoryItem

    var body: some View {
        Group {
            switch item.typeKey {
            case "muge": MugeDetailView(item: item)
            case "share": ShareRewardDetailView(item: item)
            case "autumn": AutumnWishDetailView(item: item)
            case "stellar": StellarDetailView(item: item)
            case "qixi": QixiDetailView(item: item)
            case "qingmei": QingMeiDetailView(item: item)
            case "charity": CharityDetailView(item: item)
            case "weather": WeatherDetailView(item: item)
            default: GenericActivityView(item: item)
            }
        }
        .navigationTitle(item.name)
        .navigationBarTitleDisplayMode(.inline)
    }
}

private struct ActivityHeader: View {
    @Environment(\.palette) private var p
    var title: String
    var subtitle: String
    var endAt: Date?
    var body: some View {
        Card {
            VStack(alignment: .leading, spacing: 6) {
                Text(title).font(.system(size: 22, weight: .bold)).foregroundColor(p.text)
                Text(subtitle).font(.system(size: 12)).foregroundColor(p.secondary)
                if let endAt {
                    TimelineView(.periodic(from: .now, by: 60)) { context in
                        let ended = endAt <= context.date
                        Text(ended ? "活动已结束" : "剩余 " + Fmt.countdown(to: endAt))
                            .font(.system(size: 11))
                            .foregroundColor(ended ? p.secondary : p.orange)
                    }
                }
            }
        }
    }
}

struct MugeDetailView: View {
    @Environment(\.palette) private var p
    @EnvironmentObject private var store: AppStore
    var item: ActivityDirectoryItem
    @State private var state: MugeState?
    @State private var answers: [Int: Int] = [:]
    @State private var loading = true
    @State private var error = ""
    @State private var busy = false

    var body: some View {
        ScrollView {
            VStack(spacing: 12) {
                ActivityHeader(title: state?.title ?? item.name,
                               subtitle: "牧场庆典与人格手册", endAt: state?.endAt)
                if let state {
                    Card {
                        HStack(spacing: 10) {
                            metric("庆典值", Fmt.compact(state.celebrationTotal), p.orange)
                            metric("好友", "\(state.friendsCount)", p.green)
                            metric("手册积分", "\(state.handbookPoints)", p.indigo)
                        }
                    }
                    tierSection("庆典奖励", state.celebrationTiers) { operate("claimCelebration") }
                    tierSection("好友助力", state.friendsTiers) { operate("claimFriends") }
                    tierSection("手册等级", state.handbookTiers) { operate("claimHandbook") }
                    if !state.handbookTasks.isEmpty {
                        Card {
                            VStack(alignment: .leading, spacing: 8) {
                                HStack {
                                    Text("手册任务").font(.system(size: 15, weight: .semibold)).foregroundColor(p.text)
                                    Spacer()
                                    ActionPill(title: "领取任务", tint: p.orange) { operate("claimTask") }
                                }
                                ForEach(state.handbookTasks) { task in
                                    HStack {
                                        VStack(alignment: .leading, spacing: 2) {
                                            Text(task.description).font(.system(size: 12)).foregroundColor(p.text)
                                            Text("\(task.progress)/\(task.target) · +\(task.points) 积分")
                                                .font(.system(size: 10)).foregroundColor(p.secondary)
                                        }
                                        Spacer()
                                        Pill(text: task.status == 2 ? "已领取" : (task.status == 1 ? "可领取" : "进行中"),
                                             color: task.status == 2 ? p.secondary : (task.status == 1 ? p.green : p.orange))
                                    }
                                }
                            }
                        }
                    }
                    if !state.questions.isEmpty {
                        Card {
                            VStack(alignment: .leading, spacing: 10) {
                                Text("人格问答").font(.system(size: 15, weight: .semibold)).foregroundColor(p.text)
                                if state.personalityId > 0 {
                                    Text("已生成人格 #\(state.personalityId)").font(.system(size: 12)).foregroundColor(p.green)
                                }
                                ForEach(state.questions) { q in
                                    VStack(alignment: .leading, spacing: 5) {
                                        Text(q.title).font(.system(size: 12, weight: .medium)).foregroundColor(p.text)
                                        ForEach(q.options) { option in
                                            Button {
                                                answers[q.id] = option.id
                                            } label: {
                                                HStack(spacing: 5) {
                                                    Image(systemName: answers[q.id] == option.id ? "largecircle.fill.circle" : "circle")
                                                        .font(.system(size: 12)).foregroundColor(p.emerald)
                                                    Text(option.label).font(.system(size: 11)).foregroundColor(p.text)
                                                    Spacer()
                                                }
                                            }
                                            .buttonStyle(.plain)
                                        }
                                    }
                                }
                                HStack(spacing: 8) {
                                    ActionPill(title: "保存答案", icon: "checkmark.circle", tint: p.blue) {
                                        let dict = Dictionary(uniqueKeysWithValues: answers.map { ("\($0.key)", $0.value) })
                                        operate("saveAnswers", ["answers": dict, "clear": false])
                                    }
                                    ActionPill(title: "生成人格", icon: "sparkles", tint: p.orange) { operate("settle") }
                                    ActionPill(title: "分享奖励", icon: "square.and.arrow.up", tint: p.green) { operate("claimShare") }
                                }
                            }
                        }
                    }
                }
                if !error.isEmpty { ErrorBanner(message: error) }
                if loading && state == nil { LoadingState(text: "正在读取牧歌初启") }
            }
            .padding(16)
        }
        .background(p.background.ignoresSafeArea())
        .task { await load() }
    }

    private func metric(_ title: String, _ value: String, _ color: Color) -> some View {
        VStack(spacing: 2) {
            Text(value).font(.system(size: 17, weight: .bold)).foregroundColor(color)
            Text(title).font(.system(size: 10)).foregroundColor(p.secondary)
        }.frame(maxWidth: .infinity).padding(.vertical, 6)
        .background(p.elevated).clipShape(RoundedRectangle(cornerRadius: 9, style: .continuous))
    }

    private func tierSection(_ title: String, _ tiers: [MugeTier], claim: @escaping () -> Void) -> some View {
        Card {
            VStack(alignment: .leading, spacing: 8) {
                HStack {
                    Text(title).font(.system(size: 15, weight: .semibold)).foregroundColor(p.text)
                    Spacer()
                    if tiers.contains(where: { $0.status == 1 }) {
                        ActionPill(title: "一键领取", tint: p.orange, action: claim)
                    }
                }
                if tiers.isEmpty {
                    Text("暂无阶段数据").font(.system(size: 11)).foregroundColor(p.secondary)
                } else {
                    ForEach(tiers) { tier in
                        HStack {
                            Text("\(Fmt.compact(tier.threshold))").font(.system(size: 12, weight: .semibold)).foregroundColor(p.text)
                            Text(tier.rewards.map(\.name).joined(separator: "、"))
                                .font(.system(size: 10)).foregroundColor(p.secondary).lineLimit(1)
                            Spacer()
                            Pill(text: tier.stateText, color: tier.status == 2 ? p.secondary : (tier.status == 1 ? p.green : p.orange))
                        }
                    }
                }
            }
        }
    }

    private func load() async {
        guard store.isAccountOnline else {
            error = "账号未在线，请先启动当前账号"
            loading = false
            return
        }
        loading = true
        do {
            state = try await store.api.fetchMuge(accountId: store.currentAccountId)
            answers = state?.answers ?? [:]
        } catch let e as APIError { error = e.message }
        catch let err { error = err.localizedDescription }
        loading = false
    }

    private func operate(_ action: String, _ body: [String: Any] = [:]) {
        guard store.isAccountOnline else { store.showToast("账号未在线，请先启动账号"); return }
        guard !busy else { return }
        busy = true
        Task {
            do {
                let (newState, message) = try await store.api.operateMuge(accountId: store.currentAccountId, action: action, body: body)
                if let newState { state = newState }
                store.showToast(message)
                await load()
            } catch let e as APIError { store.showToast(e.message) }
            catch { store.showToast(error.localizedDescription) }
            busy = false
        }
    }
}

struct ShareRewardDetailView: View {
    @Environment(\.palette) private var p
    @EnvironmentObject private var store: AppStore
    var item: ActivityDirectoryItem
    @State private var state: ShareRewardState?
    @State private var loading = true
    @State private var error = ""
    @State private var busy = false

    private var activityId: String { item.instanceId ?? item.id }

    var body: some View {
        ScrollView {
            VStack(spacing: 12) {
                ActivityHeader(title: state?.title ?? item.name, subtitle: "分享与快乐值奖励", endAt: state?.endAt)
                if let state {
                    Card {
                        HStack(spacing: 10) {
                            VStack(spacing: 2) {
                                Text(Fmt.compact(state.score)).font(.system(size: 24, weight: .bold)).foregroundColor(p.orange)
                                Text("快乐值").font(.system(size: 10)).foregroundColor(p.secondary)
                            }.frame(maxWidth: .infinity)
                            VStack(spacing: 2) {
                                Text("\(state.claimedCount)/\(state.claimLimit)").font(.system(size: 18, weight: .bold)).foregroundColor(p.green)
                                Text("已领档位").font(.system(size: 10)).foregroundColor(p.secondary)
                            }.frame(maxWidth: .infinity)
                        }
                    }
                    HStack(spacing: 8) {
                        PrimaryButton(title: state.canShare ? "分享活动" : "今日已分享", icon: "square.and.arrow.up",
                                      tint: p.orange, enabled: !busy && state.canShare) {
                            operate("share")
                        }
                        PrimaryButton(title: "领取里程碑", icon: "gift.fill",
                                      tint: p.green, enabled: !busy && state.canClaimMilestones) {
                            operate("claim")
                        }
                    }
                    Card {
                        VStack(alignment: .leading, spacing: 8) {
                            Text("里程碑").font(.system(size: 15, weight: .semibold)).foregroundColor(p.text)
                            ForEach(state.milestones) { m in
                                HStack {
                                    Text("\(Fmt.compact(m.threshold)) 快乐值").font(.system(size: 12, weight: .medium)).foregroundColor(p.text)
                                    Spacer()
                                    Text(m.rewards.map(\.name).joined(separator: "、")).font(.system(size: 10)).foregroundColor(p.secondary).lineLimit(1)
                                    Pill(text: m.state == 2 ? "已领取" : (m.state == 1 ? "可领取" : "未达成"),
                                         color: m.state == 2 ? p.secondary : (m.state == 1 ? p.green : p.orange))
                                }
                            }
                        }
                    }
                }
                if !error.isEmpty { ErrorBanner(message: error) }
                if loading && state == nil { LoadingState() }
            }
            .padding(16)
        }
        .background(p.background.ignoresSafeArea())
        .task { await load() }
    }

    private func load() async {
        guard store.isAccountOnline else {
            error = "账号未在线，请先启动当前账号"
            loading = false
            return
        }
        loading = true
        do { state = try await store.api.fetchShareReward(accountId: store.currentAccountId, activityId: activityId) }
        catch let e as APIError { error = e.message }
        catch let err { error = err.localizedDescription }
        loading = false
    }

    private func operate(_ action: String) {
        guard store.isAccountOnline else { store.showToast("账号未在线，请先启动账号"); return }
        guard !busy else { return }
        busy = true
        Task {
            do {
                let (newState, message) = try await store.api.operateShareReward(accountId: store.currentAccountId, activityId: activityId, action: action)
                if let newState { state = newState }
                store.showToast(message)
                await load()
            } catch let e as APIError { store.showToast(e.message) }
            catch { store.showToast(error.localizedDescription) }
            busy = false
        }
    }
}

struct AutumnWishDetailView: View {
    @Environment(\.palette) private var p
    @EnvironmentObject private var store: AppStore
    var item: ActivityDirectoryItem
    @State private var state: AutumnWishState?
    @State private var loading = true
    @State private var busy = false
    @State private var error = ""

    var body: some View {
        ScrollView {
            VStack(spacing: 12) {
                ActivityHeader(title: state?.title ?? item.name, subtitle: "每日祈愿与签文奖励", endAt: state?.endAt)
                if let state {
                    Card {
                        HStack(spacing: 10) {
                            VStack(spacing: 2) {
                                Text("第 \(state.day) 天").font(.system(size: 20, weight: .bold)).foregroundColor(p.orange)
                                Text("剩余 \(state.remaining) 次").font(.system(size: 10)).foregroundColor(p.secondary)
                            }.frame(maxWidth: .infinity)
                            VStack(spacing: 2) {
                                Text("\(state.rewardDays.count)").font(.system(size: 20, weight: .bold)).foregroundColor(p.green)
                                Text("奖励日程").font(.system(size: 10)).foregroundColor(p.secondary)
                            }.frame(maxWidth: .infinity)
                        }
                    }
                    if !state.pendingText.isEmpty {
                        Card {
                            VStack(alignment: .leading, spacing: 8) {
                                Text("签文").font(.system(size: 14, weight: .semibold)).foregroundColor(p.text)
                                Text(state.pendingText).font(.system(size: 13)).foregroundColor(p.text)
                                if !state.pendingRewards.isEmpty {
                                    Text(state.pendingRewards.map(\.text).joined(separator: "、"))
                                        .font(.system(size: 11)).foregroundColor(p.green)
                                }
                                PrimaryButton(title: "领取奖励", tint: p.orange, enabled: !busy && state.canClaim) {
                                    operate("claim")
                                }
                            }
                        }
                    }
                    if state.canDraw || state.choices.isEmpty == false {
                        Card {
                            VStack(alignment: .leading, spacing: 8) {
                                Text("选择今日祈愿").font(.system(size: 14, weight: .semibold)).foregroundColor(p.text)
                                ForEach(state.choices) { choice in
                                    Button { operate("draw", ["chooseId": choice.id]) } label: {
                                        HStack {
                                            Text(choice.name).font(.system(size: 13)).foregroundColor(p.text)
                                            Spacer()
                                            Image(systemName: "chevron.right").font(.system(size: 11)).foregroundColor(p.secondary)
                                        }
                                        .padding(10).background(p.elevated)
                                        .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
                                    }
                                    .buttonStyle(.plain)
                                    .disabled(!state.canDraw || busy)
                                }
                            }
                        }
                    }
                    if !state.rewardDays.isEmpty {
                        Card {
                            VStack(alignment: .leading, spacing: 8) {
                                Text("祈愿奖励").font(.system(size: 14, weight: .semibold)).foregroundColor(p.text)
                                LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 8) {
                                    ForEach(state.rewardDays) { entry in
                                        HStack {
                                            Text("D\(entry.day)").font(.system(size: 10, weight: .bold)).foregroundColor(p.orange)
                                            Text(entry.reward.name).font(.system(size: 11)).foregroundColor(p.text).lineLimit(1)
                                            Spacer()
                                            Text("×\(entry.reward.count)").font(.system(size: 10)).foregroundColor(p.secondary)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                if !error.isEmpty { ErrorBanner(message: error) }
                if loading && state == nil { LoadingState() }
            }
            .padding(16)
        }
        .background(p.background.ignoresSafeArea())
        .task { await load() }
    }

    private func load() async {
        guard store.isAccountOnline else {
            error = "账号未在线，请先启动当前账号"
            loading = false
            return
        }
        loading = true
        do { state = try await store.api.fetchAutumnWish(accountId: store.currentAccountId) }
        catch let e as APIError { error = e.message }
        catch let err { error = err.localizedDescription }
        loading = false
    }

    private func operate(_ action: String, _ body: [String: Any] = [:]) {
        guard store.isAccountOnline else { store.showToast("账号未在线，请先启动账号"); return }
        guard !busy else { return }
        busy = true
        Task {
            do {
                let (newState, message) = try await store.api.operateAutumnWish(accountId: store.currentAccountId, action: action, body: body)
                if let newState { state = newState }
                store.showToast(message)
                await load()
            } catch let e as APIError { store.showToast(e.message) }
            catch { store.showToast(error.localizedDescription) }
            busy = false
        }
    }
}

struct StellarDetailView: View {
    @Environment(\.palette) private var p
    @EnvironmentObject private var store: AppStore
    var item: ActivityDirectoryItem
    @State private var state: StellarSnapshot?
    @State private var loading = true
    @State private var busy = false
    @State private var error = ""
    @State private var amountPrompt: AmountPrompt?

    var body: some View {
        ScrollView {
            VStack(spacing: 12) {
                ActivityHeader(title: state?.title ?? item.name, subtitle: "千星游记 · 通行证与星砂商店", endAt: state?.endAt)
                if let state {
                    Card {
                        VStack(alignment: .leading, spacing: 8) {
                            HStack {
                                Text("通行证 Lv.\(state.passLevel)").font(.system(size: 16, weight: .semibold)).foregroundColor(p.text)
                                Spacer()
                                ActionPill(title: "领取通行证", tint: p.indigo, enabled: state.canClaimPass && !busy) {
                                    run { try await store.api.claimStellarPass(accountId: store.currentAccountId) }
                                }
                            }
                            ProgressLine(value: state.passProgressMax > 0 ? Double(state.passProgress) / Double(state.passProgressMax) : 0, color: p.indigo)
                            Text("\(state.passProgress)/\(state.passProgressMax)").font(.system(size: 10)).foregroundColor(p.secondary)
                            ForEach(state.passNodes) { node in
                                HStack {
                                    Text("Lv.\(node.level)").font(.system(size: 11)).foregroundColor(p.text)
                                    Spacer()
                                    Pill(text: node.claimed ? "已领" : (node.claimable ? "可领" : (node.locked ? "未解锁" : "未达成")),
                                         color: node.claimed ? p.secondary : (node.claimable ? p.green : p.orange))
                                }
                            }
                        }
                    }
                    Card {
                        VStack(alignment: .leading, spacing: 8) {
                            HStack {
                                Text("星座").font(.system(size: 15, weight: .semibold)).foregroundColor(p.text)
                                Spacer()
                                ActionPill(title: "点亮星座", tint: p.indigo, enabled: state.canLightConstellation && !busy) {
                                    run { try await store.api.lightStellarConstellation(accountId: store.currentAccountId) }
                                }
                            }
                            ForEach(state.constellationGroups) { g in
                                HStack {
                                    Text(g.name).font(.system(size: 12)).foregroundColor(p.text)
                                    Text(g.visualState).font(.system(size: 10)).foregroundColor(p.secondary)
                                    Spacer()
                                    Text(g.rewards.map(\.name).joined(separator: "、")).font(.system(size: 9)).foregroundColor(p.secondary).lineLimit(1)
                                }
                            }
                        }
                    }
                    if !state.solarTerms.isEmpty {
                        Card {
                            VStack(alignment: .leading, spacing: 8) {
                                Text("节令").font(.system(size: 15, weight: .semibold)).foregroundColor(p.text)
                                ForEach(state.solarTerms) { term in
                                    HStack {
                                        VStack(alignment: .leading, spacing: 1) {
                                            Text(term.name).font(.system(size: 12, weight: .medium)).foregroundColor(p.text)
                                            Text(term.description).font(.system(size: 10)).foregroundColor(p.secondary).lineLimit(1)
                                        }
                                        Spacer()
                                        Button(term.claimed ? "已领取" : "领取") {
                                            run { try await store.api.claimStellarTerm(accountId: store.currentAccountId, termId: term.id) }
                                        }
                                        .font(.system(size: 11, weight: .semibold)).foregroundColor(p.emerald)
                                        .disabled(term.claimed || !term.claimable || busy)
                                    }
                                }
                            }
                        }
                    }
                    Card {
                        VStack(alignment: .leading, spacing: 8) {
                            HStack {
                                Text("星砂商店").font(.system(size: 15, weight: .semibold)).foregroundColor(p.text)
                                Spacer()
                                Text("\(state.shopCurrencyName) \(state.shopBalance)").font(.system(size: 11)).foregroundColor(p.secondary)
                            }
                            ForEach(state.shopGoods) { good in
                                HStack {
                                    VStack(alignment: .leading, spacing: 2) {
                                        Text(good.name).font(.system(size: 12, weight: .medium)).foregroundColor(p.text)
                                        Text("\(good.item.name) ×\(good.item.count) · \(good.cost.name) \(good.cost.count)")
                                            .font(.system(size: 10)).foregroundColor(p.secondary)
                                    }
                                    Spacer()
                                    Button(good.soldOut ? "售罄" : "兑换") {
                                        let maxCount = max(1, min(good.maxExchangeCount, 99))
                                        amountPrompt = AmountPrompt(title: "兑换 \(good.name)", caption: "消耗 \(good.cost.name)",
                                                                    maxCount: maxCount, confirmLabel: "兑换", unit: "个") { count in
                                            run { try await store.api.exchangeStarSand(accountId: store.currentAccountId, goodsId: good.id, count: count) }
                                        }
                                    }
                                    .font(.system(size: 11, weight: .semibold)).foregroundColor(p.emerald)
                                    .disabled(good.soldOut || !good.exchangeable || busy)
                                }
                            }
                        }
                    }
                }
                if !error.isEmpty { ErrorBanner(message: error) }
                if loading && state == nil { LoadingState() }
            }
            .padding(16)
        }
        .background(p.background.ignoresSafeArea())
        .task { await load() }
        .sheet(item: $amountPrompt) { AmountPromptView(prompt: $0) }
    }

    private func load() async {
        guard store.isAccountOnline else {
            error = "账号未在线，请先启动当前账号"
            loading = false
            return
        }
        loading = true
        do { state = try await store.api.fetchStellar(accountId: store.currentAccountId) }
        catch let e as APIError { error = e.message }
        catch let err { error = err.localizedDescription }
        loading = false
    }

    private func run(_ block: @escaping () async throws -> String) {
        guard store.isAccountOnline else { store.showToast("账号未在线，请先启动账号"); return }
        guard !busy else { return }
        busy = true
        Task {
            do {
                let message = try await block()
                store.showToast(message)
                await load()
            } catch let e as APIError { store.showToast(e.message) }
            catch { store.showToast(error.localizedDescription) }
            busy = false
        }
    }
}

struct QixiDetailView: View {
    @Environment(\.palette) private var p
    @EnvironmentObject private var store: AppStore
    var item: ActivityDirectoryItem
    @State private var state: QixiState?
    @State private var loading = true
    @State private var busy = false
    @State private var error = ""
    @State private var showGift = false

    var body: some View {
        ScrollView {
            VStack(spacing: 12) {
                ActivityHeader(title: state?.title ?? item.name, subtitle: "鹊桥寄情 · 羽毛与香囊", endAt: state?.endAt)
                if let state {
                    Card {
                        HStack(spacing: 10) {
                            VStack(spacing: 2) {
                                Text(state.feather).font(.system(size: 22, weight: .bold)).foregroundColor(p.indigo)
                                Text("鹊羽").font(.system(size: 10)).foregroundColor(p.secondary)
                            }.frame(maxWidth: .infinity)
                            VStack(spacing: 2) {
                                Text(state.sachet).font(.system(size: 22, weight: .bold)).foregroundColor(p.orange)
                                Text("香囊").font(.system(size: 10)).foregroundColor(p.secondary)
                            }.frame(maxWidth: .infinity)
                        }
                    }
                    HStack(spacing: 8) {
                        PrimaryButton(title: "领取鹊桥奖励", icon: "sparkles", tint: p.indigo,
                                      enabled: state.canClaimBridge && !busy) {
                            run { try await store.api.claimQixiBridge(accountId: store.currentAccountId) }
                        }
                        PrimaryButton(title: "赠送香囊", icon: "gift.fill", tint: p.orange, enabled: !busy) {
                            showGift = true
                        }
                    }
                    if !state.stages.isEmpty {
                        Card {
                            VStack(alignment: .leading, spacing: 8) {
                                Text("鹊桥阶段").font(.system(size: 15, weight: .semibold)).foregroundColor(p.text)
                                ForEach(state.stages) { stage in
                                    HStack {
                                        Text(stage.label).font(.system(size: 12)).foregroundColor(p.text)
                                        Spacer()
                                        Text(stage.rewards.map(\.name).joined(separator: "、"))
                                            .font(.system(size: 9)).foregroundColor(p.secondary).lineLimit(1)
                                        Pill(text: stage.statusText, color: stage.claimed ? p.secondary : (stage.claimable ? p.green : p.orange))
                                    }
                                }
                            }
                        }
                    }
                }
                if !error.isEmpty { ErrorBanner(message: error) }
                if loading && state == nil { LoadingState() }
            }
            .padding(16)
        }
        .background(p.background.ignoresSafeArea())
        .task { await load() }
        .sheet(isPresented: $showGift) {
            QixiGiftSheet().environmentObject(store)
        }
    }

    private func load() async {
        guard store.isAccountOnline else {
            error = "账号未在线，请先启动当前账号"
            loading = false
            return
        }
        loading = true
        do { state = try await store.api.fetchQixi(accountId: store.currentAccountId) }
        catch let e as APIError { error = e.message }
        catch let err { error = err.localizedDescription }
        loading = false
    }

    private func run(_ block: @escaping () async throws -> String) {
        guard store.isAccountOnline else { store.showToast("账号未在线，请先启动账号"); return }
        guard !busy else { return }
        busy = true
        Task {
            do {
                let message = try await block()
                store.showToast(message)
                await load()
            } catch let e as APIError { store.showToast(e.message) }
            catch { store.showToast(error.localizedDescription) }
            busy = false
        }
    }
}

struct QixiGiftSheet: View {
    @Environment(\.palette) private var p
    @Environment(\.dismiss) private var dismiss
    @EnvironmentObject private var store: AppStore
    @State private var friends: [FriendItem] = []
    @State private var loading = true
    @State private var busy = false

    var body: some View {
        NavigationView {
            List {
                if loading && friends.isEmpty {
                    HStack { Spacer(); ProgressView(); Spacer() }
                }
                ForEach(friends) { friend in
                    HStack(spacing: 10) {
                        AvatarView(url: AssetURL.resolve(friend.avatarUrl, base: store.serverURL), name: friend.name, size: 34)
                        Text(friend.name).font(.system(size: 14)).foregroundColor(p.text)
                        Spacer()
                        Button("赠送") { gift(friend) }
                            .font(.system(size: 12, weight: .semibold)).foregroundColor(p.orange)
                            .disabled(busy)
                    }
                }
            }
            .scrollContentBackground(.hidden)
            .background(p.background)
            .navigationTitle("赠送鹊羽香囊")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button("关闭") { dismiss() } }
            }
            .task {
                do { friends = try await store.api.fetchFriends(accountId: store.currentAccountId) }
                catch { friends = [] }
                loading = false
            }
        }
    }

    private func gift(_ friend: FriendItem) {
        guard store.isAccountOnline else { store.showToast("账号未在线，请先启动账号"); return }
        busy = true
        Task {
            do {
                let message = try await store.api.giftQixiSachet(accountId: store.currentAccountId, friendGid: "\(friend.gid)")
                store.showToast(message)
                busy = false
                dismiss()
            } catch let e as APIError { store.showToast(e.message); busy = false }
            catch { store.showToast(error.localizedDescription); busy = false }
        }
    }
}

struct GenericActivityView: View {
    @Environment(\.palette) private var p
    @EnvironmentObject private var store: AppStore
    var item: ActivityDirectoryItem
    @State private var raw: JSONValue?
    @State private var loading = true
    @State private var error = ""

    private var path: String {
        switch item.typeKey {
        case "milktea": return "/api/activities/milk-tea?page=shop"
        case "qingmei": return "/api/activity-center/qingmei"
        case "charity": return "/api/activity-center/charity-red-flower"
        case "weather": return "/api/activity-center/weather"
        default: return ""
        }
    }

    var body: some View {
        ScrollView {
            VStack(spacing: 12) {
                ActivityHeader(title: item.name, subtitle: "活动数据与实时进度", endAt: item.endAt)
                if path.isEmpty {
                    Card {
                        EmptyStateView(icon: "questionmark.circle", title: "该活动暂未提供独立操作面板",
                                       subtitle: "服务端没有返回可识别的活动接口。你仍可在 Web 管理面板中操作。")
                    }
                } else if let raw {
                    Card {
                        VStack(alignment: .leading, spacing: 8) {
                            HStack {
                                Text("服务端数据").font(.system(size: 15, weight: .semibold)).foregroundColor(p.text)
                                Spacer()
                                Button { Task { await load() } } label: {
                                    Image(systemName: "arrow.clockwise").foregroundColor(p.emerald)
                                }
                            }
                            Text(raw.pretty)
                                .font(.system(size: 10, design: .monospaced))
                                .foregroundColor(p.secondary)
                                .textSelection(.enabled)
                        }
                    }
                }
                if !error.isEmpty { ErrorBanner(message: error) }
                if loading && raw == nil { LoadingState() }
            }
            .padding(16)
        }
        .background(p.background.ignoresSafeArea())
        .task { await load() }
    }

    private func load() async {
        guard store.isAccountOnline else {
            error = "账号未在线，请先启动当前账号"
            loading = false
            return
        }
        guard !path.isEmpty else { loading = false; return }
        loading = true
        do { raw = try await store.api.fetchRaw(accountId: store.currentAccountId, path: path) }
        catch let e as APIError { error = e.message }
        catch let err { error = err.localizedDescription }
        loading = false
    }
}

struct QingMeiDetailView: View {
    @Environment(\.palette) private var p
    @EnvironmentObject private var store: AppStore
    var item: ActivityDirectoryItem
    @State private var state: QingMeiState?
    @State private var loading = true
    @State private var busy = false
    @State private var error = ""
    @State private var amountPrompt: AmountPrompt?

    var body: some View {
        ScrollView {
            VStack(spacing: 12) {
                ActivityHeader(title: state?.title ?? item.name, subtitle: "领取青梅种子，发酵三轮后结算金币", endAt: item.endAt)
                if let state {
                    Card {
                        HStack {
                            VStack(alignment: .leading, spacing: 2) {
                                Text("可用青梅").font(.system(size: 11)).foregroundColor(p.secondary)
                                Text(state.balance).font(.system(size: 25, weight: .bold)).foregroundColor(p.emerald)
                            }
                            Spacer()
                            VStack(alignment: .trailing, spacing: 2) {
                                Text("轮次").font(.system(size: 11)).foregroundColor(p.secondary)
                                Text("\(state.currentRound)/\(state.maxRounds)").font(.system(size: 20, weight: .bold)).foregroundColor(p.text)
                            }
                        }
                        PrimaryButton(title: state.dailySeedClaimed ? "今日青梅种子已领取" : "领取今日青梅种子",
                                      icon: "leaf.fill", tint: p.emerald,
                                      enabled: !busy && !state.dailySeedClaimed) {
                            op("daily-seed/claim")
                        }
                        .padding(.top, 8)
                    }
                    Card {
                        VStack(alignment: .leading, spacing: 10) {
                            Text(state.started ? "当前酿造估值：\(state.currentQuoteGold) 金币" : "尚未开启本轮酿造")
                                .font(.system(size: 15, weight: .semibold))
                                .foregroundColor(state.started ? p.orange : p.secondary)
                            if state.started {
                                HStack(spacing: 8) {
                                    PrimaryButton(title: state.finished ? "本轮已结束" : "继续发酵",
                                                  icon: "arrow.triangle.2.circlepath", tint: p.emerald,
                                                  enabled: !busy && state.canContinue && !state.finished) {
                                        op("brew/continue")
                                    }
                                    PrimaryButton(title: "立即结算售出", icon: "yensign.circle.fill",
                                                  tint: p.orange, enabled: !busy && state.canSettle) {
                                        op("brew/settle")
                                    }
                                }
                            } else {
                                PrimaryButton(title: "投入青梅开始酿造", icon: "wineglass.fill",
                                              tint: p.orange,
                                              enabled: !busy && state.canStart && !state.firstIngredientUid.isEmpty && state.firstIngredientCount > 0) {
                                    let maxCount = Int(min(state.firstIngredientCount, 9999))
                                    amountPrompt = AmountPrompt(title: "投入青梅", caption: "本组可投入 \(state.firstIngredientCount) 个",
                                                                maxCount: maxCount, confirmLabel: "开始酿造", unit: "个") { count in
                                        let ingredient: [String: Any] = ["uid": state.firstIngredientUid, "count": "\(count)"]
                                        op("brew/start", ["ingredients": [ingredient]])
                                    }
                                }
                            }
                        }
                    }
                }
                if !error.isEmpty { ErrorBanner(message: error) }
                if loading && state == nil { LoadingState(text: "正在读取青酿换万金") }
            }
            .padding(16)
        }
        .background(p.background.ignoresSafeArea())
        .task { await load() }
        .sheet(item: $amountPrompt) { AmountPromptView(prompt: $0) }
    }

    private func load() async {
        guard store.isAccountOnline else {
            error = "账号未在线，请先启动当前账号"
            loading = false
            return
        }
        loading = true
        do { state = try await store.api.fetchQingMei(accountId: store.currentAccountId) }
        catch let e as APIError { error = e.message }
        catch let err { error = err.localizedDescription }
        loading = false
    }

    private func op(_ path: String, _ body: [String: Any] = [:]) {
        guard store.isAccountOnline else { store.showToast("账号未在线，请先启动账号"); return }
        guard !busy else { return }
        busy = true
        Task {
            do {
                let message = try await store.api.operateSeasonal(accountId: store.currentAccountId,
                                                                  path: "/api/activity-center/qingmei/\(path)", body: body)
                store.showToast(message)
                await load()
            } catch let e as APIError { store.showToast(e.message) }
            catch { store.showToast(error.localizedDescription) }
            busy = false
        }
    }
}

struct CharityDetailView: View {
    @Environment(\.palette) private var p
    @EnvironmentObject private var store: AppStore
    var item: ActivityDirectoryItem
    @State private var state: CharityState?
    @State private var loading = true
    @State private var busy = false
    @State private var error = ""
    @State private var confirmDonate = false

    var body: some View {
        ScrollView {
            VStack(spacing: 12) {
                ActivityHeader(title: state?.title ?? item.name, subtitle: "公益小红花 · 爱心捐赠与里程碑", endAt: state?.endAt)
                if let state {
                    Card {
                        HStack {
                            VStack(alignment: .leading, spacing: 2) {
                                Text("持有爱心").font(.system(size: 11)).foregroundColor(p.secondary)
                                Text(state.loveBalance).font(.system(size: 25, weight: .bold)).foregroundColor(p.red)
                            }
                            Spacer()
                            VStack(alignment: .trailing, spacing: 2) {
                                Text("已捐出爱心").font(.system(size: 11)).foregroundColor(p.secondary)
                                Text(state.donatedLove).font(.system(size: 25, weight: .bold)).foregroundColor(p.emerald)
                            }
                        }
                        PrimaryButton(title: "领取今日种子", icon: "leaf.fill", tint: p.emerald,
                                      enabled: !busy && state.canClaimSeeds) {
                            op("seeds/claim")
                        }
                        .padding(.top, 8)
                        HStack(spacing: 8) {
                            PrimaryButton(title: "捐出爱心", icon: "heart.fill", tint: p.red,
                                          enabled: !busy && state.canDonateLove) {
                                confirmDonate = true
                            }
                            PrimaryButton(title: "领每日礼包", icon: "gift.fill", tint: p.orange,
                                          enabled: !busy && state.canClaimDailyGift) {
                                op("daily-gift/claim")
                            }
                        }
                    }
                    if !state.stages.isEmpty {
                        Card {
                            VStack(alignment: .leading, spacing: 8) {
                                Text("捐赠里程碑").font(.system(size: 15, weight: .semibold)).foregroundColor(p.text)
                                ForEach(state.stages) { stage in
                                    HStack {
                                        VStack(alignment: .leading, spacing: 2) {
                                            Text(stage.label).font(.system(size: 12, weight: .medium)).foregroundColor(p.text)
                                            Text(stage.rewards.map(\.text).joined(separator: "、"))
                                                .font(.system(size: 10)).foregroundColor(p.secondary).lineLimit(1)
                                        }
                                        Spacer()
                                        if stage.claimable {
                                            Button("领取") { op("progress/claim", ["target": stage.id]) }
                                                .font(.system(size: 11, weight: .semibold)).foregroundColor(p.emerald)
                                                .disabled(busy)
                                        } else {
                                            Pill(text: stage.statusText,
                                                 color: stage.claimed ? p.secondary : p.orange)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                if !error.isEmpty { ErrorBanner(message: error) }
                if loading && state == nil { LoadingState(text: "正在读取公益小红花") }
            }
            .padding(16)
        }
        .background(p.background.ignoresSafeArea())
        .task { await load() }
        .alert("捐出爱心", isPresented: $confirmDonate) {
            Button("确认捐出", role: .destructive) { op("love/donate") }
            Button("取消", role: .cancel) {}
        } message: {
            Text("当前持有爱心：\(state?.loveBalance ?? "0")，确认捐出以推进公益进度？")
        }
    }

    private func load() async {
        guard store.isAccountOnline else {
            error = "账号未在线，请先启动当前账号"
            loading = false
            return
        }
        loading = true
        do { state = try await store.api.fetchCharity(accountId: store.currentAccountId) }
        catch let e as APIError { error = e.message }
        catch let err { error = err.localizedDescription }
        loading = false
    }

    private func op(_ path: String, _ body: [String: Any] = [:]) {
        guard store.isAccountOnline else { store.showToast("账号未在线，请先启动账号"); return }
        guard !busy else { return }
        busy = true
        Task {
            do {
                let message = try await store.api.operateSeasonal(accountId: store.currentAccountId,
                                                                  path: "/api/activity-center/charity-red-flower/\(path)", body: body)
                store.showToast(message)
                await load()
            } catch let e as APIError { store.showToast(e.message) }
            catch { store.showToast(error.localizedDescription) }
            busy = false
        }
    }
}

struct WeatherDetailView: View {
    @Environment(\.palette) private var p
    @EnvironmentObject private var store: AppStore
    var item: ActivityDirectoryItem
    @State private var state: WeatherState?
    @State private var loading = true
    @State private var busy = false
    @State private var error = ""

    var body: some View {
        ScrollView {
            VStack(spacing: 12) {
                ActivityHeader(title: state?.title ?? item.name, subtitle: "召唤天气、采集瓶子与科研节点", endAt: state?.endAt)
                if let state {
                    Card {
                        VStack(alignment: .leading, spacing: 8) {
                            Text("当前天气：\(state.activeWeatherName.isEmpty ? "晴朗" : state.activeWeatherName)")
                                .font(.system(size: 17, weight: .semibold)).foregroundColor(p.cyan)
                            HStack {
                                weatherMetric("徽章", state.badgeBalance)
                                weatherMetric("采集瓶", state.collectionBottleCount)
                                weatherMetric("召唤瓶", state.rainBottleCount)
                            }
                        }
                    }
                    Card {
                        VStack(spacing: 8) {
                            PrimaryButton(title: state.canSummonRain ? "召唤雷雨" : "暂不可召唤雷雨",
                                          icon: "cloud.bolt.rain.fill", tint: p.cyan,
                                          enabled: !busy && state.canSummonRain) {
                                op("summon")
                            }
                            PrimaryButton(title: "兑换天气采集瓶", icon: "drop.fill", tint: p.blue,
                                          enabled: !busy && state.canExchangeCollector) {
                                op("shop/exchange")
                            }
                            if !state.nextResearchNodeId.isEmpty {
                                PrimaryButton(title: "点亮科研节点 #\(state.nextResearchNodeId)",
                                              icon: "lightbulb.fill", tint: p.indigo,
                                              enabled: !busy && state.canAdvanceResearch) {
                                    op("research/\(state.nextResearchNodeId)/advance")
                                }
                            }
                        }
                    }
                }
                if !error.isEmpty { ErrorBanner(message: error) }
                if loading && state == nil { LoadingState(text: "正在读取雨落成诗") }
            }
            .padding(16)
        }
        .background(p.background.ignoresSafeArea())
        .task { await load() }
    }

    private func weatherMetric(_ title: String, _ value: String) -> some View {
        VStack(spacing: 2) {
            Text(value).font(.system(size: 16, weight: .bold)).foregroundColor(p.text)
            Text(title).font(.system(size: 10)).foregroundColor(p.secondary)
        }
        .frame(maxWidth: .infinity).padding(.vertical, 6)
        .background(p.elevated).clipShape(RoundedRectangle(cornerRadius: 9, style: .continuous))
    }

    private func load() async {
        guard store.isAccountOnline else {
            error = "账号未在线，请先启动当前账号"
            loading = false
            return
        }
        loading = true
        do { state = try await store.api.fetchWeather(accountId: store.currentAccountId) }
        catch let e as APIError { error = e.message }
        catch let err { error = err.localizedDescription }
        loading = false
    }

    private func op(_ path: String) {
        guard store.isAccountOnline else { store.showToast("账号未在线，请先启动账号"); return }
        guard !busy else { return }
        busy = true
        Task {
            do {
                let message = try await store.api.operateSeasonal(accountId: store.currentAccountId,
                                                                  path: "/api/activity-center/weather/\(path)")
                store.showToast(message)
                await load()
            } catch let e as APIError { store.showToast(e.message) }
            catch { store.showToast(error.localizedDescription) }
            busy = false
        }
    }
}
