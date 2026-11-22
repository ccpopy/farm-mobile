import Foundation
import SwiftUI

struct SolarCalendarView: View {
    @Environment(\.palette) private var p
    @EnvironmentObject private var store: AppStore
    var showsTopBar = true
    @State private var calendar: SolarCalendarState?
    @State private var selectedSeasonId = 0
    @State private var loading = false
    @State private var error = ""
    @State private var busy = false
    @State private var quizEvent: SolarEvent?
    @State private var fetchedAt = Date()

    private var nowSec: Int64 {
        guard let server = calendar?.serverTimeSec, server > 0 else {
            return Int64(Date().timeIntervalSince1970)
        }
        return server + Int64(Date().timeIntervalSince(fetchedAt))
    }

    private var selectedSeason: SolarSeason? {
        calendar?.seasons.first(where: { $0.id == selectedSeasonId }) ?? calendar?.seasons.last
    }

    private var events: [SolarEvent] {
        guard let calendar else { return [] }
        guard let season = selectedSeason, !season.eventIds.isEmpty else { return calendar.events }
        return calendar.events.filter { season.eventIds.contains($0.id) }
    }

    var body: some View {
        ScrollView {
            VStack(spacing: 12) {
                if showsTopBar { TopBar() }
                if !error.isEmpty { ErrorBanner(message: error).padding(.horizontal, 16) }
                if store.status == nil && store.overviewLoading {
                    Card { LoadingState(text: "正在读取账号状态") }.padding(.horizontal, 16)
                } else if !store.isAccountOnline {
                    Card {
                        EmptyStateView(icon: "wifi.slash", title: "账号未在线",
                                       subtitle: "请先在总览启动当前账号，节令进度与领奖需要在线连接")
                    }
                    .padding(.horizontal, 16)
                } else if loading && calendar == nil {
                    Card { LoadingState(text: "正在读取节令日历") }.padding(.horizontal, 16)
                } else if let calendar {
                    seasonChips(calendar).padding(.horizontal, 16)
                    if let season = selectedSeason {
                        seasonCard(season).padding(.horizontal, 16)
                    }
                    if events.isEmpty {
                        Card { EmptyStateView(icon: "calendar", title: "暂无节令事件", actionTitle: "刷新", action: { Task { await load() } }) }
                            .padding(.horizontal, 16)
                    } else {
                        ForEach(events) { event in
                            eventCard(event).padding(.horizontal, 16)
                        }
                    }
                }
                Color.clear.frame(height: 20)
            }
            .padding(.top, 4)
        }
        .background(p.background.ignoresSafeArea())
        .refreshable { await load() }
        .task(id: store.currentAccountId) { await load() }
        .task(id: store.status?.connected) {
            if store.isAccountOnline { await load() }
        }
        .sheet(item: $quizEvent) { event in
            SolarQuizSheet(event: event) { optionIndex in
                submitAnswer(event, optionIndex: optionIndex)
            }
            .environmentObject(store)
        }
    }

    private func seasonChips(_ cal: SolarCalendarState) -> some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 6) {
                ForEach(cal.seasons) { season in
                    Button {
                        selectedSeasonId = season.id
                    } label: {
                        Text(season.name)
                            .font(.system(size: 12, weight: .semibold))
                            .foregroundColor(selectedSeasonId == season.id ? .white : p.secondary)
                            .padding(.horizontal, 12).padding(.vertical, 7)
                            .background(selectedSeasonId == season.id ? p.red : p.card)
                            .clipShape(Capsule())
                    }
                    .buttonStyle(.plain)
                }
            }
        }
    }

    private func seasonCard(_ season: SolarSeason) -> some View {
        Card {
            VStack(alignment: .leading, spacing: 8) {
                HStack {
                    Text(season.name).font(.system(size: 18, weight: .bold)).foregroundColor(p.text)
                    Spacer()
                    if season.rewards.isEmpty == false {
                        Button(season.claimed ? "已领取" : (season.canClaim ? "领集齐礼" : "未集齐")) {
                            perform("season", ["id": season.id])
                        }
                        .font(.system(size: 12, weight: .semibold)).foregroundColor(p.red)
                        .disabled(!season.canClaim || busy)
                    }
                }
                Text("已点亮 \(season.litCount)/\(season.totalCount)")
                    .font(.system(size: 12)).foregroundColor(p.secondary)
                HStack(spacing: 4) {
                    ForEach(Array(0..<max(1, season.totalCount)), id: \.self) { index in
                        Capsule()
                            .fill(index < season.litCount ? p.red : p.track)
                            .frame(height: 6)
                    }
                }
                if !season.rewards.isEmpty {
                    Text(season.rewards.map(\.text).joined(separator: "、"))
                        .font(.system(size: 11)).foregroundColor(p.secondary)
                }
            }
        }
    }

    private func eventCard(_ event: SolarEvent) -> some View {
        let done = event.directClaimed && (event.question.isEmpty || event.quizClaimed)
        let expired = !done && (event.status == 3 || (event.endSec > 0 && nowSec > event.endSec))
        let upcoming = !done && !expired && event.startSec > 0 && nowSec < event.startSec
        return Card {
            VStack(alignment: .leading, spacing: 8) {
                HStack {
                    Text(event.name).font(.system(size: 16, weight: .semibold)).foregroundColor(p.text)
                    Spacer()
                    Pill(text: done ? "已完成" : (expired ? "已过期" : (upcoming ? "未开始" : "进行中")),
                         color: done ? p.secondary : (expired ? p.secondary : (upcoming ? p.orange : p.green)), dot: true)
                }
                Text(event.description).font(.system(size: 11)).foregroundColor(p.secondary).lineLimit(3)
                if !event.rewards.isEmpty {
                    Text("节令奖励：" + event.rewards.map(\.text).joined(separator: "、"))
                        .font(.system(size: 10)).foregroundColor(p.secondary)
                }
                HStack(spacing: 8) {
                    Button(event.directClaimed ? "已领取" : "领取节令奖励") {
                        perform("claim", ["id": event.id])
                    }
                    .font(.system(size: 12, weight: .semibold))
                    .foregroundColor(event.canClaim && !expired && !upcoming ? p.green : p.secondary)
                    .disabled(!event.canClaim || expired || upcoming || busy)
                    if !event.question.isEmpty {
                        Button(event.quizClaimed ? "答题已完成" : (event.quizWrongToday ? "明日再来" : "节令问答")) {
                            quizEvent = event
                        }
                        .font(.system(size: 12, weight: .semibold))
                        .foregroundColor(event.canAnswer && !expired && !upcoming ? p.red : p.secondary)
                        .disabled(expired || upcoming || busy || event.quizWrongToday || event.quizClaimed)
                    }
                    Spacer()
                }
                if event.quizWrongToday && !event.errorTips.isEmpty {
                    Text(event.errorTips).font(.system(size: 10)).foregroundColor(p.orange)
                }
            }
        }
    }

    private func load() async {
        let accountId = store.currentAccountId
        guard !accountId.isEmpty, store.isAccountOnline else { return }
        loading = true
        error = ""
        do {
            let cal = try await store.api.fetchSolarCalendar(accountId: accountId)
            guard store.currentAccountId == accountId, store.isAccountOnline else { return }
            calendar = cal
            fetchedAt = Date()
            if !cal.seasons.contains(where: { $0.id == selectedSeasonId }) {
                // Match the Android client: the server tells us which event is
                // current; select the season that contains it instead of always
                // jumping to the last season in the list.
                let target = cal.defaultEventId
                selectedSeasonId = cal.seasons.first(where: { $0.eventIds.contains(target) })?.id
                    ?? cal.seasons.last?.id ?? 0
            }
        } catch let e as APIError {
            if store.currentAccountId == accountId { error = e.message }
        } catch let err {
            if store.currentAccountId == accountId { error = err.localizedDescription }
        }
        loading = false
    }

    private func perform(_ action: String, _ body: [String: Any]) {
        guard !busy, store.isAccountOnline else { return }
        busy = true
        Task {
            do {
                let result = try await store.api.operateSolar(accountId: store.currentAccountId, action: action, body: body)
                if let cal = result.calendar { calendar = cal }
                let text = result.rewards.map(\.text).joined(separator: "、")
                store.showToast(text.isEmpty ? (result.correct == false ? "回答未中，明日再来" : "领取成功") : text)
                await load()
                await store.refreshOverview()
            } catch let e as APIError { store.showToast(e.message) }
            catch { store.showToast(error.localizedDescription) }
            busy = false
        }
    }

    private func submitAnswer(_ event: SolarEvent, optionIndex: Int) {
        quizEvent = nil
        perform("answer", ["id": event.id, "optionIndex": optionIndex, "expectedQuestion": event.question])
    }
}

struct SolarQuizSheet: View {
    @Environment(\.palette) private var p
    @Environment(\.dismiss) private var dismiss
    var event: SolarEvent
    var onSubmit: (Int) -> Void
    @State private var selected: Int?
    @State private var busy = false

    var body: some View {
        NavigationView {
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    Text(event.name + " · 节令问答").font(.system(size: 22, weight: .bold)).foregroundColor(p.text)
                    Text(event.question).font(.system(size: 15)).foregroundColor(p.text)
                    ForEach(event.options.indices, id: \.self) { index in
                        let option = event.options[index]
                        Button {
                            selected = index
                        } label: {
                            HStack(spacing: 8) {
                                Image(systemName: selected == index ? "largecircle.fill.circle" : "circle")
                                    .foregroundColor(p.red)
                                Text(option).font(.system(size: 14)).foregroundColor(p.text)
                                Spacer()
                            }
                            .padding(12)
                            .background(selected == index ? p.red.opacity(0.10) : p.elevated)
                            .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                        }
                        .buttonStyle(.plain)
                    }
                    PrimaryButton(title: "提交答案", tint: p.red, enabled: selected != nil && !busy) {
                        if let selected {
                            busy = true
                            dismiss()
                            onSubmit(selected)
                        }
                    }
                    .padding(.top, 4)
                }
                .padding(20)
            }
            .background(p.background.ignoresSafeArea())
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button("取消") { dismiss() } }
            }
        }
    }
}
