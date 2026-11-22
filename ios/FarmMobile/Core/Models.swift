import Foundation

func normalizeMs(_ raw: Int64) -> Int64 {
    if raw >= 1 && raw <= 99_999_999_999 { return raw * 1000 }
    return raw
}

func normalizeSec(_ raw: Int64) -> Int64 {
    if raw >= 100_000_000_000 { return raw / 1000 }
    return raw
}

// Activity directory timestamps may be seconds or milliseconds depending on
// the server version. Android normalizes both before comparing to Date().
func normalizeDirectoryTimeMs(_ raw: Int64) -> Int64 {
    if raw <= 0 { return 0 }
    return raw < 1_000_000_000_000 ? raw * 1_000 : raw
}

func dateFromMs(_ raw: Int64) -> Date? {
    let ms = normalizeMs(raw)
    guard ms > 0 else { return nil }
    return Date(timeIntervalSince1970: Double(ms) / 1000)
}

enum AssetURL {
    static func resolve(_ raw: String, base: String) -> String {
        let t = raw.trimmingCharacters(in: .whitespacesAndNewlines)
        if t.isEmpty { return "" }
        let lower = t.lowercased()
        if lower.hasPrefix("http://") || lower.hasPrefix("https://") || lower.hasPrefix("data:image/") { return t }
        if t.hasPrefix("/") { return base + t }
        return base + "/" + t
    }
}

struct CommerceItem: Identifiable, Equatable {
    var id: Int = 0
    var count: Int64 = 0
    var name: String = ""
    var image: String = ""
    var rarity: Int = 0
    var description: String = ""

    init() {}
    init(_ j: JSONValue) {
        id = j.i("id"); count = j.l("count"); name = j.str("name")
        image = j.str("image"); rarity = j.i("rarity"); description = j.str("description")
    }
    static func list(_ a: [JSONValue]) -> [CommerceItem] { a.map { CommerceItem($0) } }
    var text: String { count > 0 ? "\(name) ×\(Fmt.compact(count))" : name }
}

struct ServerVersionInfo {
    var clientVersion = ""
    var wxClientVersion = ""
    var botVersion = ""
    init(_ j: JSONValue) {
        clientVersion = j.str("clientVersion")
        wxClientVersion = j.str("wxClientVersion")
        botVersion = j.str("botVersion")
    }
}

struct AccountItem: Identifiable, Equatable {
    var id: String
    var name: String
    var nick: String = ""
    var uin: Int64 = 0
    var avatar: String = ""
    var platform: String = "qq"
    var running: Bool = false

    init(_ j: JSONValue) {
        id = j.str("id"); name = j.str("name"); nick = j.str("nick"); uin = j.l("uin")
        avatar = j.str("avatar"); platform = j.str("platform", "qq"); running = j.flag("running")
    }
    var displayTitle: String {
        if !nick.isEmpty && !name.isEmpty && nick != name { return "\(nick) (\(name))" }
        if !name.isEmpty { return name }
        if !nick.isEmpty { return nick }
        return "账号 \(id)"
    }
    var platformLabel: String { ["wx", "wechat"].contains(platform.lowercased()) ? "微信" : "QQ" }
    func avatarURL(base: String) -> String { AssetURL.resolve(avatar, base: base) }
}

struct LevelProgress {
    var current: Int64 = 0
    var needed: Int64 = 0
    var fraction: Double { needed > 0 ? min(1, max(0, Double(current) / Double(needed))) : 0 }
}

struct StatusSnapshot {
    var accountId = ""
    var name = ""
    var level = 0
    var exp: Int64 = 0
    var gold: Int64 = 0
    var coupon: Int64 = 0
    var goldBean: Int64 = 0
    var avatarUrl = ""
    var connected = false
    var uptimeSec: Int64 = 0
    var sessionElapsedSec: Int64 = 0
    var sessionExpGained: Int64 = 0
    var sessionGoldGained: Int64 = 0
    var sessionCouponGained: Int64 = 0
    var progress = LevelProgress()
    var levelForecastText = ""
    var nextFarmRunAt: Date?
    var nextFriendRunAt: Date?
    var farmQuiet = false
    var friendQuiet = false
    var serverTimeMs: Int64 = 0
    var operations: [String: Int] = [:]
    var ranchOperations: [String: Int] = [:]

    init() {}

    init(accountId: String, data: JSONValue, serverTimeHint: Int64 = 0) {
        self.accountId = accountId
        let status = data.obj("status") ?? .null
        let conn = data.obj("connection") ?? .null
        let progressObj = data.obj("levelProgress")
        let next = data.obj("nextChecks") ?? .null
        let ops = data.obj("operations")
        let ranchOps = data.obj("ranchOperations")

        name = status.str("name"); level = status.i("level"); exp = status.l("exp")
        gold = status.l("gold"); coupon = status.l("coupon"); goldBean = status.l("goldBean")
        avatarUrl = status.str("avatarUrl"); connected = conn.flag("connected")
        uptimeSec = data.l("uptime"); sessionElapsedSec = data.l("sessionElapsedSeconds")
        sessionExpGained = data.l("sessionExpGained"); sessionGoldGained = data.l("sessionGoldGained")
        sessionCouponGained = data.l("sessionCouponGained")
        progress = LevelProgress(current: progressObj?.l("current") ?? 0, needed: progressObj?.l("needed") ?? 0)
        levelForecastText = Self.forecast(data.obj("levelForecast"))
        let snapshotTime = data.l("serverTime")
        let serverNow = serverTimeHint > 0 ? serverTimeHint : (snapshotTime > 0 ? snapshotTime : Int64(Date().timeIntervalSince1970 * 1000))
        serverTimeMs = normalizeMs(serverNow)

        func nextRun(_ deadlineKey: String, _ remainingKeys: [String]) -> Date? {
            let deadline = data.l(deadlineKey, next.l(deadlineKey, -1))
            if deadline >= 0 { return dateFromMs(deadline) }
            for key in remainingKeys {
                let remaining = next.l(key, -1)
                if remaining >= 0 {
                    if remaining == 0 { return Date(timeIntervalSince1970: 0) }
                    let base = snapshotTime > 0 ? snapshotTime : serverNow
                    return dateFromMs(base + remaining * 1000)
                }
            }
            return nil
        }
        nextFarmRunAt = nextRun("nextFarmRunAt", ["farmRemainSec"])
        nextFriendRunAt = nextRun("nextFriendRunAt", ["friendRemainSec", "helpRemainSec", "stealRemainSec"])
        farmQuiet = next.flag("farmQuiet")
        friendQuiet = next.flag("friendQuiet") || next.flag("helpQuiet") || next.flag("stealQuiet")

        if let ops {
            for (k, v) in ops.objectValue ?? [:] where k != "upgrade" && k != "levelUp" {
                if let n = v.intValue { operations[k] = n }
            }
        }
        if let ranchOps {
            for key in ["harvest", "produce", "helpFarming", "steal"] where !ranchOps.isNull(key) {
                ranchOperations[key] = ranchOps.i(key)
            }
        }
    }

    private static func forecast(_ j: JSONValue?) -> String {
        guard let j else { return "" }
        let source = j.str("source")
        if source == "ready" { return "当前可升下一级" }
        let estimatedAt = j.l("estimatedAt")
        if estimatedAt <= 0 || source == "unavailable" { return "" }
        let now = Int64(Date().timeIntervalSince1970)
        let estimatedSec = normalizeMs(estimatedAt) / 1000
        let delta = max(0, estimatedSec - now)
        if delta < 60 { return "预计 1 分钟内升级" }
        if delta < 3600 { return "预计 \(delta / 60) 分钟后升级" }
        if delta < 86400 {
            let hours = delta / 3600
            let mins = (delta % 3600) / 60
            return mins > 0 ? "预计 \(hours)小时\(mins)分后升级" : "预计 \(hours)小时后升级"
        }
        if let d = dateFromMs(estimatedAt) { return "预计 \(Fmt.dateTime(d)) 升级" }
        return ""
    }
}

struct RunLogEntry: Identifiable, Equatable {
    var ts: Int64 = 0
    var time = ""
    var tag = ""
    var msg = ""
    var module = ""
    var event = ""
    var result = ""
    var isWarn = false
    var accountId = ""
    var friendName = ""

    // Keep the row identity stable across HTTP refreshes. A fresh UUID on every
    // poll made SwiftUI replace the whole log list and reset the scroll offset.
    var id: String { "\(accountId)|\(ts)|\(tag)|\(module)|\(event)|\(msg)" }

    init() {}
    init(_ j: JSONValue) {
        let meta = j.obj("meta")
        ts = j.l("ts")
        if ts <= 0 { ts = Int64(Date().timeIntervalSince1970 * 1000) }
        time = j.str("time")
        if time.isEmpty, let d = dateFromMs(ts) { time = Fmt.time(d) }
        tag = j.str("tag"); msg = j.str("msg")
        module = meta?.str("module") ?? j.str("module")
        event = meta?.str("event") ?? j.str("event")
        result = meta?.str("result") ?? ""
        isWarn = j.flag("isWarn"); accountId = j.str("accountId"); friendName = meta?.str("friendName") ?? ""
    }
}

enum RunLogScope {
    case farm, ranch, system
}

private let farmLogModules: Set<String> = ["farm", "friend", "warehouse", "task", "mall", "shop", "dog"]
private let farmLogTags: Set<String> = ["农场", "好友", "仓库", "任务", "活跃", "商店", "购买"]

extension GameSpace {
    var runLogScope: RunLogScope { self == .ranch ? .ranch : .farm }
}

extension RunLogEntry {
    var scope: RunLogScope {
        let normalizedTag = tag.trimmingCharacters(in: .whitespacesAndNewlines)
        if module == "ranch" || normalizedTag.hasPrefix("牧场") || event.hasPrefix("ranch_") {
            return .ranch
        }
        if farmLogModules.contains(module) || (module.isEmpty && farmLogTags.contains(normalizedTag)) {
            return .farm
        }
        return .system
    }

    func isVisible(since cutoffMs: Int64) -> Bool {
        ts >= cutoffMs && event != "runtime_diagnostics" && msg != "账号运行诊断"
    }
}

struct DailyGiftItem: Identifiable {
    var key: String
    var label: String
    var enabled = false
    var doneToday = false
    var result = ""
    var id: String { key }
    init(_ j: JSONValue) {
        key = j.str("key"); label = j.str("label"); enabled = j.flag("enabled")
        doneToday = j.flag("doneToday"); result = j.str("result")
    }
}

struct DailyGiftsSummary {
    var date = ""
    var timeZone = ""
    var gifts: [DailyGiftItem] = []
    init() {}
    init(_ j: JSONValue) {
        date = j.str("date"); timeZone = j.str("timeZone"); gifts = j.arr("gifts").map(DailyGiftItem.init)
    }
}

struct LandSummary {
    var harvestable = 0; var growing = 0; var empty = 0; var dead = 0
    var needWater = 0; var needWeed = 0; var needBug = 0
    init() {}
    init(_ j: JSONValue) {
        harvestable = j.i("harvestable"); growing = j.i("growing"); empty = j.i("empty"); dead = j.i("dead")
        needWater = j.i("needWater"); needWeed = j.i("needWeed"); needBug = j.i("needBug")
    }
}

struct LandUpgradeCost: Identifiable {
    var id: String; var name: String; var image: String; var count: String
    init(_ j: JSONValue) {
        id = j.str("id"); name = j.str("name"); image = j.str("image"); count = j.str("count", "0")
    }
}

struct LandUpgradeInfo {
    var mode = ""
    var currentLevel = 0; var currentName = ""
    var nextLevel = 0; var nextName = ""
    var serverAllowed = false
    var needLevel: Int?
    var prerequisiteLandId: Int?
    var prerequisiteMet = false
    var precedingName = ""; var precedingRequired = 0; var precedingOwned: Int?
    var costs: [LandUpgradeCost] = []
    init() {}
    init(_ j: JSONValue) {
        mode = j.str("mode")
        if let c = j.obj("current") { currentLevel = c.i("level"); currentName = c.str("name") }
        if let n = j.obj("next") { nextLevel = n.i("level"); nextName = n.str("name") }
        serverAllowed = j.flag("serverAllowed")
        if !j.isNull("needLevel") { needLevel = j.i("needLevel") }
        if let pre = j.obj("prerequisite") {
            if pre.i("landId") > 0 { prerequisiteLandId = pre.i("landId") }
            prerequisiteMet = pre.flag("met")
        }
        if let p = j.obj("precedingLands") {
            precedingName = p.str("name"); precedingRequired = p.i("required")
            if !p.isNull("owned") { precedingOwned = p.i("owned") }
        }
        costs = j.arr("costs").map(LandUpgradeCost.init)
    }
}

struct LandItem: Identifiable, Equatable {
    var id: Int
    var level = 1
    var unlocked = true
    var status = "empty"
    var plantName = ""
    var phaseName = ""
    var seedImage = ""
    var matureInSec: Int64 = 0
    var matureAt: Date?
    var totalGrowTime: Int64 = 0
    var currentSeason = 1
    var totalSeason = 1
    var needWater = false
    var needWeed = false
    var needBug = false
    var needInteractionCleanup = false
    var couldUnlock = false
    var couldUpgrade = false
    var landInfo: LandUpgradeInfo?
    var occupiedByMaster = false
    var masterLandId = 0
    var leftInorcFertTimes: Int?
    var mutantNames: [String] = []

    init(id: Int) { self.id = id }
    init(_ j: JSONValue) {
        id = j.i("id"); level = j.i("level", 1); unlocked = j.flag("unlocked", true)
        status = j.str("status", "empty"); plantName = j.str("plantName"); phaseName = j.str("phaseName")
        seedImage = j.str("seedImage"); matureInSec = j.l("matureInSec")
        matureAt = matureInSec > 0 ? Date().addingTimeInterval(TimeInterval(matureInSec)) : nil
        totalGrowTime = j.l("totalGrowTime"); currentSeason = j.i("currentSeason", 1); totalSeason = j.i("totalSeason", 1)
        needWater = j.flag("needWater"); needWeed = j.flag("needWeed"); needBug = j.flag("needBug")
        needInteractionCleanup = j.flag("needInteractionCleanup")
        couldUnlock = j.flag("couldUnlock"); couldUpgrade = j.flag("couldUpgrade")
        if let info = j.obj("landInfo") { landInfo = LandUpgradeInfo(info) }
        occupiedByMaster = j.flag("occupiedByMaster"); masterLandId = j.i("masterLandId")
        if !j.isNull("leftInorcFertTimes") { leftInorcFertTimes = j.i("leftInorcFertTimes") }
        mutantNames = j.arr("mutantEffects").map { m in
            let n = m.str("name"); return n.isEmpty ? m.str("effect_name") : n
        }.filter { !$0.isEmpty }
    }

    static func == (lhs: LandItem, rhs: LandItem) -> Bool {
        lhs.id == rhs.id && lhs.status == rhs.status && lhs.plantName == rhs.plantName &&
        lhs.matureAt == rhs.matureAt && lhs.leftInorcFertTimes == rhs.leftInorcFertTimes
    }

    var needsCare: Bool { needWater || needWeed || needBug }
    var statusLabel: String {
        if !unlocked { return couldUnlock ? "可解锁" : "未解锁" }
        switch status {
        case "harvestable", "ripe", "mature": return "可收获"
        case "growing": return phaseName.isEmpty ? "生长中" : phaseName
        case "harvested": return "已成熟"
        case "dead": return "已枯萎"
        case "stealable": return "可偷取"
        case "unstealable": return "不可偷"
        default: return plantName.isEmpty ? "空地" : plantName
        }
    }
}

struct LandsPayload {
    var lands: [LandItem] = []
    var summary = LandSummary()
    var cleanableSocialEventNames: [String] = []
    init() {}
    init(_ j: JSONValue) {
        lands = j.arr("lands").map { LandItem($0) }
        summary = LandSummary(j.obj("summary") ?? .null)
        cleanableSocialEventNames = j.arr("socialEvents").compactMap { ev in
            guard ev.flag("cleanable") else { return nil }
            let label = ev.str("itemName").isEmpty ? ev.str("name") : ev.str("itemName")
            return label.isEmpty ? nil : label
        }
    }
}

struct FriendLandsPayload {
    var payload = LandsPayload()
    var stealableCount = 0; var dryCount = 0; var weedCount = 0; var insectCount = 0
    var canDisrupt = false
    init() {}
    init(_ j: JSONValue) {
        payload = LandsPayload(j)
        let summary = j.obj("summary") ?? .null
        func ids(_ key: String) -> Set<Int> {
            Set(summary.arr(key).compactMap(\.intValue).filter { $0 > 0 })
        }
        let stealable = ids("stealable")
        let dry = ids("needWater")
        let weed = ids("needWeed")
        let insect = ids("needBug")
        let disrupt = ids("canPutWeed").union(ids("canPutBug"))
        payload.lands = payload.lands.map { land in
            var copy = land
            let eligible = land.unlocked && !land.occupiedByMaster
            if eligible && stealable.contains(land.id) {
                copy.status = "stealable"
            } else if land.status == "stealable" || land.status == "harvested" {
                copy.status = "unstealable"
            }
            copy.needWater = eligible && dry.contains(land.id)
            copy.needWeed = eligible && weed.contains(land.id)
            copy.needBug = eligible && insect.contains(land.id)
            return copy
        }
        stealableCount = payload.lands.filter { $0.status == "stealable" }.count
        dryCount = payload.lands.filter(\.needWater).count
        weedCount = payload.lands.filter(\.needWeed).count
        insectCount = payload.lands.filter(\.needBug).count
        canDisrupt = payload.lands.contains { $0.unlocked && !$0.occupiedByMaster && disrupt.contains($0.id) }
    }
    var needsCare: Bool { dryCount > 0 || weedCount > 0 || insectCount > 0 }
}

struct BagSystemItem: Identifiable {
    var id: Int; var count: Int64; var name: String
    var interactionType = ""; var hoursText = ""
    init(_ j: JSONValue) {
        id = j.i("id"); count = j.l("count"); name = j.str("name")
        interactionType = j.str("interactionType"); hoursText = j.str("hoursText")
    }
}

struct BagItem: Equatable {
    var key: String; var id: Int; var uid: Int64; var name: String; var image: String
    var count: Int64; var category: String; var itemType: Int
    var sellable = false; var price: Int64 = 0; var priceUnit = "金"; var locked = false
    var description = ""; var interactionType = ""; var hoursText = ""

    init(_ j: JSONValue) {
        id = j.i("id"); uid = j.l("uid"); key = j.str("key")
        if key.isEmpty { key = "\(id):\(uid)" }
        name = j.str("name"); image = j.str("image"); count = j.l("count")
        category = j.str("category", "item"); itemType = j.i("itemType")
        sellable = j.flag("sellable"); price = j.l("price"); priceUnit = j.str("priceUnit", "金")
        locked = j.flag("locked"); description = j.str("description")
        interactionType = j.str("interactionType"); hoursText = j.str("hoursText")
    }
    var canChangeLock: Bool { uid > 0 && [5, 6, 17].contains(itemType) }
    var isFertilizer: Bool {
        let t = interactionType.lowercased()
        return t == "fertilizer" || t == "fertilizerpro"
    }
    var supportsUse: Bool { uid > 0 && ([11, 14, 33].contains(itemType) || isFertilizer) }
}

struct BagPayload {
    var items: [BagItem] = []
    var systemItems: [BagSystemItem] = []
    init() {}
    init(_ j: JSONValue) {
        items = j.arr("items").map(BagItem.init)
        systemItems = j.arr("systemItems").map(BagSystemItem.init)
    }
    func maxUseCount(_ item: BagItem) -> Int {
        if !item.supportsUse || item.locked { return 0 }
        let stock = Int(min(item.count, 9_999))
        if !item.isFertilizer { return stock }
        let containerId = item.interactionType.lowercased() == "fertilizerpro" ? 1012 : 1011
        let used = systemItems.first(where: { $0.id == containerId })?.count ?? 0
        let remaining = max(0, 990 * 3600 - used)
        let hours: Int64 = [80002, 80012].contains(item.id) ? 4 : ([80003, 80013].contains(item.id) ? 8 : ([80004, 80014].contains(item.id) ? 12 : 1))
        return Int(min(Int64(stock), remaining / (hours * 3600)))
    }
}

struct RanchBucket: Identifiable {
    var id: Int; var name: String; var seconds: Int64
}

struct RanchAnimal: Identifiable, Equatable {
    var id: Int64; var ranchId: String; var slot: Int; var name: String; var image: String
    var stageName: String
    var canHarvest = false; var canProduce = false; var canFeed = false; var canRelease = false
    var limitedCanUsed = false
    var grassBlocked = false; var produceCount = 0; var maxProduceCount = 0
    var isGiant = false; var weightKg = ""; var nextAt: Date?
    var canStroke = false; var stroked = false; var stage = 0

    init(_ j: JSONValue) {
        id = j.l("id"); ranchId = j.str("ranchId"); slot = j.i("slot"); name = j.str("name")
        image = j.str("image"); stageName = j.str("stageName"); stage = j.i("stage")
        canHarvest = j.flag("canHarvest"); canProduce = j.flag("canProduce")
        canFeed = j.flag("canFeed"); canRelease = j.flag("canRelease"); grassBlocked = j.flag("grassBlocked")
        limitedCanUsed = j.flag("limitedCanUsed")
        produceCount = j.i("produceCount"); maxProduceCount = j.i("maxProduceCount")
        let mutation = j.obj("mutation")
        isGiant = (mutation?.i("type") ?? 0) > 0
        if let wx = mutation?.i("weightX10"), wx > 0 {
            var s = String(format: "%.2f", Double(wx) / 100)
            while s.hasSuffix("0") { s.removeLast() }
            if s.hasSuffix(".") { s.removeLast() }
            weightKg = s + "kg"
        }
        nextAt = dateFromMs(j.l("nextAtMs"))
        canStroke = j.flag("canStroke"); stroked = j.flag("stroked")
    }
}

struct RanchPenGroup: Identifiable {
    var moduleType: Int; var name: String; var count: Int; var capacity: Int; var animals: [RanchAnimal]
    var id: Int { moduleType }
}

struct RanchCareer {
    var harvest: Int64; var mature: Int64; var steal: Int64
    init(_ j: JSONValue) { harvest = j.l("harvest"); mature = j.l("mature"); steal = j.l("steal") }
    var ratioText: String {
        if harvest > 0 { return String(format: "%.1f", Double(steal) / Double(harvest) * 100).replacingOccurrences(of: ".0", with: "") + "%" }
        return "—"
    }
}

struct RanchCurrency: Identifiable {
    var id: Int; var name: String; var image: String; var count: Int64
}

struct RanchFacilityNext {
    var level = 0; var capacity = 0; var durationSec: Int64 = 0
    var requiredLevel = 0; var available = false; var costs: [CommerceItem] = []
    init() {}
    init(_ j: JSONValue) {
        level = j.i("level"); capacity = j.i("capacity"); durationSec = j.l("durationSec")
        requiredLevel = j.i("requiredLevel"); available = j.flag("available")
        costs = CommerceItem.list(j.arr("costs"))
    }
}

struct RanchFacility: Identifiable {
    var id: String; var moduleType: Int; var name: String
    var level = 0; var state = 0; var active = false; var canActivate = false
    var capacity = 0; var upgradeState = 0; var upgradeEndAt: Date?
    var ranchCount = 0; var ranchCapacity = 0; var hasRanch = false
    var feedCount = 0; var feedRemainingSec: Int64 = -1; var hasFeed = false
    var next: RanchFacilityNext?
    init() { id = UUID().uuidString; moduleType = 0; name = "" }
    init(_ j: JSONValue) {
        id = j.str("id"); moduleType = j.i("moduleType"); name = j.str("name", "设施")
        level = j.i("level"); state = j.i("state"); active = j.flag("active"); canActivate = j.flag("canActivate")
        capacity = j.i("capacity"); upgradeState = j.i("upgradeState"); upgradeEndAt = dateFromMs(j.l("upgradeEndTime"))
        if let ranch = j.obj("ranch") { hasRanch = true; ranchCount = ranch.i("count"); ranchCapacity = ranch.i("capacity") }
        if let feed = j.obj("feed") { hasFeed = true; feedCount = feed.i("count"); feedRemainingSec = feed.l("remainingSec", -1) }
        if let n = j.obj("next") { next = RanchFacilityNext(n) }
    }
}

struct RanchPayload {
    var unlocked = false; var level = 0; var grass: Int64 = 0
    var buckets: [RanchBucket] = []; var pens: [RanchPenGroup] = []
    var hazardsCount = 0; var shelvesCollectableCount = 0
    var currentExp: Int64 = 0; var neededExp: Int64 = 0
    var currencies: [RanchCurrency] = []; var facilities: [RanchFacility] = []
    var notice = ""; var reason = ""; var career: RanchCareer?
    var hourglassSeconds: Int64?

    init() {}
    init(_ j: JSONValue) {
        unlocked = j.flag("unlocked", true)
        level = j.i("level"); grass = j.l("grass")
        buckets = j.arr("buckets").map { b in
            let id = b.i("id")
            return RanchBucket(id: id, name: id == 7004 ? "有机罐头" : "普通罐头", seconds: b.l("seconds"))
        }
        currencies = j.arr("currencies").map { c in
            RanchCurrency(id: c.i("id"), name: c.str("name"), image: c.str("image"), count: c.l("count"))
        }
        facilities = j.arr("facilities").map { RanchFacility($0) }.sorted { a, b in
            let order: (Int) -> Int = { [6: 0, 7: 1, 8: 2][$0] ?? $0 }
            if order(a.moduleType) != order(b.moduleType) { return order(a.moduleType) < order(b.moduleType) }
            return (Int64(a.id) ?? 0) < (Int64(b.id) ?? 0)
        }
        var grouped: [Int: [RanchAnimal]] = [:]
        var names: [Int: String] = [:]
        var counts: [Int: Int] = [:]
        var caps: [Int: Int] = [:]
        var hazards = 0; var shelves = 0
        for f in j.arr("facilities") {
            let type = f.i("moduleType")
            guard let ranch = f.obj("ranch") else { continue }
            names[type] = names[type] ?? f.str("name", "畜舍")
            counts[type, default: 0] += ranch.i("count")
            caps[type, default: 0] += ranch.i("capacity")
            hazards += ranch.arr("hazards").count
            shelves += ranch.arr("shelves").filter { $0.flag("canCollect") }.count
            grouped[type, default: []] += ranch.arr("animals").map(RanchAnimal.init)
        }
        pens = grouped.map { type, animals in
            RanchPenGroup(moduleType: type, name: names[type] ?? "畜舍", count: counts[type] ?? animals.count,
                          capacity: caps[type] ?? animals.count, animals: animals.sorted { $0.slot < $1.slot })
        }.sorted { a, b in
            let order: (Int) -> Int = { [6: 0, 7: 1][$0] ?? $0 }
            return order(a.moduleType) < order(b.moduleType)
        }
        hazardsCount = hazards; shelvesCollectableCount = shelves
        currentExp = j.l("currentExp"); neededExp = j.l("neededExp")
        notice = j.str("notice"); reason = j.str("reason")
        if let c = j.obj("career") { career = RanchCareer(c) }
        if !j.isNull("hourglassSeconds") { hourglassSeconds = j.l("hourglassSeconds") }
    }

    func canFeed(animals: [RanchAnimal], organic: Bool) -> Bool {
        let bucketId = organic ? 7004 : 7003
        guard buckets.contains(where: { $0.id == bucketId && $0.seconds > 0 }) else { return false }
        return animals.contains { $0.canFeed && (organic || !$0.limitedCanUsed) }
    }
}

struct RanchBagItem {
    var uid: String; var id: Int; var name: String; var image: String; var count: Int64
    var type = 0; var locked = false; var lockable = false
    var animalId = 0; var husbandryType = ""; var giantCub = false; var canSell = false
    var isCan = false; var canSpeedUpSec: Int64 = 0; var isProduct = false; var weightX10 = 0
    var sellPrices: [CommerceItem] = []
    var key: String { uid.isEmpty ? "\(id)" : uid }
    init() { uid = ""; id = 0; name = ""; image = ""; count = 0 }
    init(_ j: JSONValue) {
        uid = j.str("uid"); id = j.i("id"); name = j.str("name"); image = j.str("image"); count = j.l("count")
        type = j.i("type"); locked = j.flag("locked"); lockable = j.flag("lockable")
        animalId = j.i("animalId"); husbandryType = j.str("husbandryType"); giantCub = j.flag("giantCub")
        canSell = j.flag("canSell")
        if let can = j.obj("can") { isCan = true; canSpeedUpSec = can.l("speed_up_secs") }
        isProduct = j.obj("product") != nil || type == 106 || type == 107
        weightX10 = j.obj("product")?.i("weightX10") ?? 0
        sellPrices = CommerceItem.list(j.arr("sellPrices"))
    }
}

struct RanchTaskItem: Identifiable {
    var id: String; var category: String; var desc: String
    var progress = 0; var total = 0; var unlocked = false; var canClaim = false; var claimed = false
    init(_ j: JSONValue) {
        id = j.str("id"); category = j.str("category", "daily"); desc = j.str("desc")
        progress = j.i("progress"); total = j.i("total")
        unlocked = j.flag("unlocked"); canClaim = j.flag("canClaim"); claimed = j.flag("claimed")
    }
}

struct RanchShopGood: Identifiable {
    var id: String; var name: String; var reward: CommerceItem; var rewards: [CommerceItem]
    var priceId = 0; var priceName = ""; var priceImage = ""; var priceCount: Int64 = 0
    var priceBalance: Int64?; var available = false; var free = false; var maxCount = 1
    var remaining: Int?; var requiredLevel = 0; var shelfId = 0; var husbandryType = ""
    init(_ j: JSONValue) {
        id = j.str("id")
        reward = CommerceItem(j.obj("reward") ?? .null)
        name = j.str("name").isEmpty ? reward.name : j.str("name")
        rewards = CommerceItem.list(j.arr("rewards"))
        let price = j.obj("price") ?? .null
        priceId = price.i("id"); priceName = price.str("name"); priceImage = price.str("image"); priceCount = price.l("count")
        if !price.isNull("balance") { priceBalance = price.l("balance") }
        available = j.flag("available"); free = j.flag("free"); maxCount = max(1, j.i("maxCount", 999))
        if !j.isNull("remaining") { remaining = j.i("remaining") }
        requiredLevel = j.i("requiredLevel"); shelfId = j.i("shelfId")
        husbandryType = j.obj("animal")?.str("husbandry_type") ?? j.str("husbandryType")
    }
}

struct RanchMarketShelf: Identifiable {
    var id: Int; var name: String; var sort: Int
    init(_ j: JSONValue) { id = j.i("id"); name = j.str("name"); sort = j.i("sort") }
}

struct RanchFriendSummary: Identifiable {
    var gid: String; var name: String; var avatarUrl = ""; var canHelp = false; var helpCount = 0
    var id: String { gid }
    init(_ j: JSONValue) {
        gid = j.str("gid"); name = j.str("name").isEmpty ? j.str("gid") : j.str("name")
        avatarUrl = j.str("avatarUrl"); canHelp = j.flag("canHelp"); helpCount = j.i("helpCount")
    }
}

struct RanchFriendShelf {
    var penId: String; var slot: Int; var canSteal: Bool; var products: [CommerceItem]
    init(_ j: JSONValue, penId: String) {
        self.penId = penId; slot = j.i("slot"); canSteal = j.flag("canSteal"); products = CommerceItem.list(j.arr("products"))
    }
}

struct RanchFriendPen: Identifiable {
    var id: String; var name: String; var mosquitoes = 0; var mosquitoLimit = 0; var mosquitoRoom = 0
    var animals: [RanchAnimal] = []; var shelves: [RanchFriendShelf] = []
    init(_ j: JSONValue) {
        id = j.str("id"); name = j.str("name")
        mosquitoes = j.i("mosquitoes"); mosquitoLimit = j.i("mosquitoLimit"); mosquitoRoom = j.i("mosquitoRoom")
        animals = j.arr("animals").map(RanchAnimal.init)
        shelves = j.arr("shelves").map { RanchFriendShelf($0, penId: id) }
    }
}

struct RanchFriendDetail {
    var gid = ""; var level = 0; var canSteal = false; var stealableShelves = 0
    var strokeRemaining = 0; var mosquitoRemaining = 0; var cleanableHazards = 0
    var career: RanchCareer?; var notice = ""; var sceneRestored = true
    var ranches: [RanchFriendPen] = []
    init() {}
    init(_ j: JSONValue) {
        gid = j.str("gid"); level = j.i("level")
        sceneRestored = j.flag("sceneRestored", true)
        let serverCanSteal = j.flag("canSteal")
        let serverStealable = j.i("stealableShelves")
        strokeRemaining = j.i("strokeRemaining")
        mosquitoRemaining = j.i("mosquitoRemaining"); cleanableHazards = j.i("cleanableHazards")
        if let c = j.obj("career") { career = RanchCareer(c) }
        notice = j.str("notice")
        let allowShelfSteal = sceneRestored && serverCanSteal && serverStealable > 0
        ranches = j.arr("ranches").map { raw in
            var pen = RanchFriendPen(raw)
            if !allowShelfSteal {
                pen.shelves = pen.shelves.map { shelf in
                    var copy = shelf
                    copy.canSteal = false
                    return copy
                }
            }
            return pen
        }
        stealableShelves = ranches.reduce(0) { $0 + $1.shelves.filter(\.canSteal).count }
        canSteal = stealableShelves > 0
    }
}

struct RanchViewSnapshot {
    var base = RanchPayload()
    var bag: [RanchBagItem] = []
    var tasks: [RanchTaskItem] = []
    var shopGoods: [RanchShopGood] = []
    var marketShelves: [RanchMarketShelf] = []
    var marketGoods: [RanchShopGood] = []
    var canShopGoods: [RanchShopGood] = []
    var mallGoods: [RanchShopGood] = []
    var friends: [RanchFriendSummary] = []
    var friendRanch: RanchFriendDetail?
    init() {}
    init(_ j: JSONValue) {
        base = RanchPayload(j)
        bag = j.arr("bag").map { RanchBagItem($0) }
        tasks = j.arr("tasks").map(RanchTaskItem.init)
        shopGoods = Self.goods(j.obj("shop")?.arr("goods") ?? [])
        if let market = j.obj("market") {
            marketShelves = market.arr("shelves").map(RanchMarketShelf.init).sorted { $0.sort < $1.sort }
            marketGoods = Self.goods(market.arr("goods"))
        }
        canShopGoods = Self.goods(j.obj("canShop")?.arr("goods") ?? [])
        mallGoods = Self.goods(j.obj("mall")?.arr("goods") ?? [])
        friends = j.arr("friends").map(RanchFriendSummary.init)
        if let fr = j.obj("friendRanch") { friendRanch = RanchFriendDetail(fr) }
    }
    static func goods(_ a: [JSONValue]) -> [RanchShopGood] { a.map(RanchShopGood.init) }
}

struct RanchQuote {
    var action = ""; var label = ""; var count = 0; var skipped = false; var signature = ""
    var costItems: [CommerceItem] = []; var rewards: [CommerceItem] = []; var message = ""
    init() {}
    init(_ j: JSONValue) {
        action = j.str("action"); label = j.str("label"); count = j.i("count"); skipped = j.flag("skipped")
        signature = j.obj("expected")?.str("signature") ?? j.str("signature")
        costItems = CommerceItem.list(j.arr("costItems"))
        rewards = CommerceItem.list(j.arr("rewards")); message = j.str("message")
    }
}

struct RanchOperateResult {
    var status = "confirmed"; var notice = ""; var refreshRequired = false
    init() {}
    init(_ j: JSONValue) {
        status = j.str("status", "confirmed"); notice = j.str("notice"); refreshRequired = j.flag("refreshRequired")
    }
}

struct PetDog: Identifiable {
    var id: Int; var name: String; var image: String; var rarityLabel: String
    var level: Int; var owned: Bool; var active: Bool; var skillDescription: String
    init(_ j: JSONValue) {
        id = j.i("id"); name = j.str("name"); image = j.str("image"); rarityLabel = j.str("rarityLabel")
        level = j.i("level"); owned = j.flag("owned"); active = j.flag("active"); skillDescription = j.str("skillDescription")
    }
}

struct DogFood: Identifiable {
    var id: Int; var name: String; var image: String; var durationSec: Int64; var count: Int64
    init(_ j: JSONValue) {
        id = j.i("id"); name = j.str("name"); image = j.str("image")
        durationSec = j.l("duration"); count = j.l("count")
    }
}

struct PetSnapshot {
    var dogs: [PetDog] = []; var foods: [DogFood] = []
    var protectDurationSec: Int64 = 0; var maxProtectDurationSec: Int64 = 0
    var remainingDurationSec: Int64 = 0; var pendingGiftCount = 0; var activeDogId = 0
    init() {}
    init(_ j: JSONValue) {
        dogs = j.arr("dogs").map(PetDog.init); foods = j.arr("foods").map(DogFood.init)
        protectDurationSec = j.l("protectDuration"); maxProtectDurationSec = j.l("maxProtectDuration")
        remainingDurationSec = j.l("remainingDuration"); pendingGiftCount = j.i("pendingGiftCount")
        activeDogId = j.i("activeDogId")
    }
    func maxFeedCount(_ food: DogFood) -> Int {
        if food.durationSec <= 0 { return 0 }
        let remain = max(0, maxProtectDurationSec - protectDurationSec)
        return Int(min(food.count, remain / food.durationSec))
    }
}

struct FriendItem: Identifiable {
    var gid: Int64; var name: String; var avatarUrl: String; var level: Int
    var stealNum: Int; var dryNum: Int; var weedNum: Int; var insectNum: Int
    var id: Int64 { gid }
    init(_ j: JSONValue) {
        gid = j.l("gid"); name = j.str("name").isEmpty ? j.str("nick") : j.str("name")
        avatarUrl = j.str("avatarUrl"); level = j.i("level")
        let plant = j.obj("plant") ?? .null
        stealNum = plant.i("stealNum"); dryNum = plant.i("dryNum"); weedNum = plant.i("weedNum"); insectNum = plant.i("insectNum")
    }
    var needsCareCount: Int { dryNum + weedNum + insectNum }
}

struct FriendBlacklistItem: Identifiable {
    var gid: Int64; var name: String; var avatarUrl: String
    var id: Int64 { gid }
    init(_ j: JSONValue) {
        gid = j.l("gid"); name = j.str("name").isEmpty ? "GID:\(j.l("gid"))" : j.str("name")
        avatarUrl = j.str("avatarUrl")
    }
}

struct InteractRecord: Identifiable {
    var id: String; var friendName: String; var friendAvatar: String; var actionText: String; var timeText: String
    init(_ j: JSONValue, index: Int) {
        id = j.str("key").isEmpty ? "\(index):\(j.l("serverTimeMs"))" : j.str("key")
        friendName = j.str("nick").isEmpty ? "好友" : j.str("nick")
        friendAvatar = j.str("avatarUrl")
        actionText = j.str("actionDetail").isEmpty ? j.str("actionLabel", "互动") : j.str("actionDetail")
        if let d = dateFromMs(j.l("serverTimeMs")) { timeText = Fmt.dateTime(d) } else { timeText = "" }
    }
}

struct SchedulerSummary {
    var workerRunning = false; var workerTaskCount = 0; var runningTasks: [String] = []; var workerError = ""
    init() {}
    init(_ j: JSONValue) {
        workerError = j.str("workerError")
        guard let worker = j.obj("worker") else { return }
        workerRunning = true
        for s in worker.arr("schedulers") {
            let tasks = s.arr("tasks"); workerTaskCount += tasks.count
            for t in tasks where t.flag("running") {
                let n = t.str("name"); if !n.isEmpty { runningTasks.append(n) }
            }
        }
    }
}

struct RanchAnimalOption: Identifiable {
    var value: Int; var label: String; var kind: String
    var id: Int { value }
    init(_ j: JSONValue) { value = j.i("value"); label = j.str("label"); kind = j.str("kind") }
}

struct SeedOption: Identifiable {
    var seedId: Int64; var name: String; var requiredLevel = 0; var requiresUnlockCard = false
    var id: Int64 { seedId }
    init(_ j: JSONValue) {
        seedId = j.l("seedId", j.l("id")); name = j.str("name")
        requiredLevel = j.i("requiredLevel"); requiresUnlockCard = j.flag("requiresUnlockCard")
    }
}

struct BagPriorityItem: Identifiable {
    var id: Int; var name: String; var count: Int64; var image: String
    init(_ j: JSONValue) { id = j.i("seedId"); name = j.str("name"); count = j.l("count"); image = j.str("image") }
}

struct AccountSettings {
    var plantingStrategy = "max_exp"
    var preferredSeedId: Int64 = 0
    var bagSeedPriority: [Int] = []
    var bagSeedFallbackStrategy = "level"
    var bagSeedMultiLandReservationEnabled = false
    var ranchCoopCubPriority: [Int] = []
    var ranchShedCubPriority: [Int] = []
    var farmMinSec = 20; var farmMaxSec = 25; var friendMinSec = 20; var friendMaxSec = 25
    var quietHoursEnabled = false; var quietHoursStart = "01:00"; var quietHoursEnd = "07:30"
    var quietContinueFarm = true
    var stealDelaySeconds = 1; var plantOrderRandom = true; var plantDelaySeconds = 2
    var fertilizerMode = "smart"; var fertilizerSmartSeconds = 300
    var fertilizerLandTypes = ["purple-gold", "gold", "black", "red", "normal"]
    var fertilizerBuyOrganicCount = 1; var fertilizerBuyOrganicThresholdHours = 10
    var fertilizerBuyNormalCount = 1; var fertilizerBuyNormalThresholdHours = 10
    var fertilizerBuyCheckIntervalMinutes = 60
    var emailClaimHour: Int?
    var autoAcceptFriendMinLevel = 0; var autoAcceptRequireOwnLevel = false
    var autoAcceptHarvestStealEnabled = true; var autoAcceptHarvestStealHarvest = 8; var autoAcceptHarvestStealSteal = 1
    var ranchStrategy = "level"; var ranchBagFallback = "level"
    var ranchCoopAnimal = 0; var ranchShedAnimal = 0
    var ranchGrassThresholdPercent = 20; var ranchCheckMinutes = 5
    var ranchFeedNormalStage: String?; var ranchFeedOrganicStage: String?
    var automationBooleans: [String: Bool] = [:]
    var rawIntervalsJson = "{}"; var rawAutomationJson = "{}"

    init() {}
    init(_ j: JSONValue) {
        let intervals = j.obj("intervals") ?? .null
        let quiet = j.obj("friendQuietHours") ?? .null
        let automation = j.obj("automation") ?? .null
        plantingStrategy = j.str("strategy", "max_exp"); preferredSeedId = j.l("preferredSeed")
        farmMinSec = max(1, intervals.i("farmMin", intervals.i("farm", 20)))
        farmMaxSec = max(farmMinSec, intervals.i("farmMax", farmMinSec))
        friendMinSec = max(1, intervals.i("friendMin", intervals.i("friend", 20)))
        friendMaxSec = max(friendMinSec, intervals.i("friendMax", friendMinSec))
        quietHoursEnabled = quiet.flag("enabled"); quietHoursStart = quiet.str("start", "01:00")
        quietHoursEnd = quiet.str("end", "07:30"); quietContinueFarm = quiet.flag("continueFarm", true)
        stealDelaySeconds = min(300, max(0, j.i("stealDelaySeconds", 1)))
        plantOrderRandom = j.flag("plantOrderRandom", true)
        plantDelaySeconds = min(60, max(0, j.i("plantDelaySeconds", 2)))
        fertilizerMode = automation.str("fertilizer", "smart")
        fertilizerSmartSeconds = min(3600, max(30, automation.i("fertilizer_smart_seconds", 300)))
        let landTypes = automation.arr("fertilizer_land_types").compactMap(\.stringValue).filter { !$0.isEmpty }
        if automation.has("fertilizer_land_types") && !automation.isNull("fertilizer_land_types") {
            fertilizerLandTypes = landTypes
        }
        fertilizerBuyOrganicCount = min(10000, max(0, j.i("fertilizerBuyOrganicCount", 1)))
        fertilizerBuyOrganicThresholdHours = min(990, max(0, j.i("fertilizerBuyOrganicThresholdHours", 10)))
        fertilizerBuyNormalCount = min(10000, max(0, j.i("fertilizerBuyNormalCount", 1)))
        fertilizerBuyNormalThresholdHours = min(990, max(0, j.i("fertilizerBuyNormalThresholdHours", 10)))
        fertilizerBuyCheckIntervalMinutes = min(1440, max(1, j.i("fertilizerBuyCheckIntervalMinutes", 60)))
        if !j.isNull("emailClaimHour") { emailClaimHour = min(23, max(0, j.i("emailClaimHour", 10))) }
        autoAcceptFriendMinLevel = min(200, max(0, j.i("autoAcceptFriendMinLevel", 0)))
        autoAcceptRequireOwnLevel = j.flag("autoAcceptRequireOwnLevel")
        autoAcceptHarvestStealEnabled = j.flag("autoAcceptHarvestStealEnabled", true)
        autoAcceptHarvestStealHarvest = min(9999, max(0, j.i("autoAcceptHarvestStealHarvest", 8)))
        autoAcceptHarvestStealSteal = min(9999, max(1, j.i("autoAcceptHarvestStealSteal", 1)))
        ranchStrategy = automation.str("ranch_strategy", "level")
        ranchBagFallback = automation.str("ranch_bag_fallback", "level")
        bagSeedPriority = j.arr("bagSeedPriority").compactMap(\.intValue)
        bagSeedFallbackStrategy = j.str("bagSeedFallbackStrategy", "level")
        if !["level", "max_exp", "max_fert_exp", "max_profit", "max_fert_profit", "preferred"].contains(bagSeedFallbackStrategy) {
            bagSeedFallbackStrategy = "level"
        }
        bagSeedMultiLandReservationEnabled = j.flag("bagSeedMultiLandReservationEnabled")
        ranchCoopCubPriority = automation.arr("ranch_coop_cub_priority").compactMap(\.intValue)
        ranchShedCubPriority = automation.arr("ranch_shed_cub_priority").compactMap(\.intValue)
        ranchCoopAnimal = automation.i("ranch_coop_animal", 0)
        ranchShedAnimal = automation.i("ranch_shed_animal", 0)
        ranchGrassThresholdPercent = min(100, max(1, automation.i("ranch_grass_threshold_percent", 20)))
        ranchCheckMinutes = min(60, max(1, automation.i("ranch_check_minutes", 5)))
        let normalStage = automation.str("ranch_feed_normal_stage")
        ranchFeedNormalStage = ["any", "longest", "to_production", "near_harvest"].contains(normalStage) ? normalStage : nil
        let organicStage = automation.str("ranch_feed_organic_stage")
        ranchFeedOrganicStage = ["any", "longest", "to_production", "near_harvest"].contains(organicStage) ? organicStage : nil
        for (k, v) in automation.objectValue ?? [:] {
            if let b = v.boolValue { automationBooleans[k] = b }
        }
        rawIntervalsJson = intervals.pretty; rawAutomationJson = automation.pretty
    }
}

struct MallCurrency: Identifiable {
    var id: Int; var name: String; var image: String; var count: Int64; var balanceKnown: Bool
}

struct MallLimit {
    var type = 0; var bought = 0; var max = 0; var remaining: Int?
}

struct MallMembership {
    var isSvip = false; var remainingDays = 0
}

struct MallGoods: Identifiable {
    var id: Int; var name: String; var type = 0; var rewards: [CommerceItem] = []
    var priceId = 0; var priceName = ""; var priceImage = ""; var priceCount: Int64 = 0
    var priceBalance: Int64?; var originalPrice: Int64?; var isFree = false
    var limit: MallLimit?; var isLimited = false; var discountText = ""; var isDiscounted = false
    var available = false; var purchasable = false; var purchaseStatus = "available"; var unavailableReason = ""
    init(_ j: JSONValue) {
        id = j.i("id"); name = j.str("name"); type = j.i("type")
        rewards = CommerceItem.list(j.arr("rewards"))
        let price = j.obj("price") ?? .null
        priceId = price.i("id"); priceName = price.str("name"); priceImage = price.str("image"); priceCount = price.l("count")
        if !price.isNull("balance") { priceBalance = price.l("balance") }
        if !j.isNull("originalPrice") { originalPrice = j.l("originalPrice") }
        isFree = j.flag("isFree")
        if let l = j.obj("limit") {
            var m = MallLimit(); m.type = l.i("type"); m.bought = l.i("bought"); m.max = l.i("max")
            if !l.isNull("remaining") { m.remaining = l.i("remaining") }
            limit = m
        }
        isLimited = j.flag("isLimited"); discountText = j.str("discountText"); isDiscounted = j.flag("isDiscounted")
        available = j.flag("available"); purchasable = j.flag("purchasable")
        purchaseStatus = j.str("purchaseStatus", "available"); unavailableReason = j.str("unavailableReason")
    }
    var canBuy: Bool { purchasable || available || isFree }
}

struct MallCatalog {
    var shopId = 0; var slotType = 1; var subSlotType = 0; var refreshCountdownSec: Int64 = 0
    var membership: MallMembership?; var currencies: [MallCurrency] = []; var goods: [MallGoods] = []
    init() {}
    init(_ j: JSONValue) {
        shopId = j.i("shopId"); slotType = j.i("slotType", 1); subSlotType = j.i("subSlotType")
        refreshCountdownSec = j.l("refreshCountdown")
        if let m = j.obj("membership") {
            var v = MallMembership(); v.isSvip = m.flag("isSvip"); v.remainingDays = m.i("remainingDays"); membership = v
        }
        currencies = j.arr("currencies").map { c in
            MallCurrency(id: c.i("id"), name: c.str("name"), image: c.str("image"), count: c.l("count"), balanceKnown: c.flag("balanceKnown"))
        }
        goods = j.arr("goods").map(MallGoods.init)
    }
}

struct MallPurchaseResult {
    var rewards: [CommerceItem] = []; var catalog: MallCatalog?; var refreshRequired = false
    init() {}
    init(_ j: JSONValue) {
        rewards = CommerceItem.list(j.obj("purchase")?.arr("rewards") ?? [])
        if let c = j.obj("catalog") { catalog = MallCatalog(c) }
        refreshRequired = j.flag("refreshRequired")
    }
}

struct MysteryNpcOffer {
    var id = 0; var reward = CommerceItem(); var priceId = 0; var priceName = ""; var priceImage = ""
    var priceCount: Int64 = 0; var priceBalance: Int64?; var originalPrice: Int64 = 0; var discountPercent = 0
    init(_ j: JSONValue) {
        id = j.i("id"); reward = CommerceItem(j.obj("reward") ?? .null)
        let p = j.obj("price") ?? .null
        priceId = p.i("id"); priceName = p.str("name"); priceImage = p.str("image"); priceCount = p.l("count")
        if !p.isNull("balance") { priceBalance = p.l("balance") }
        originalPrice = j.l("originalPrice"); discountPercent = j.i("discountPercent")
    }
}

struct MysteryShopState {
    var active = false; var expireAt: Date?; var npc: MysteryNpcOffer?; var raw: JSONValue = .null
    init() {}
    init(_ j: JSONValue) {
        active = j.flag("active"); expireAt = dateFromMs(j.l("expireTime")); raw = j
        if let n = j.obj("npc") { npc = MysteryNpcOffer(n) }
    }
}

struct SolarSeason: Identifiable {
    var id: Int; var name: String; var litCount = 0; var totalCount = 0
    var claimed = false; var canClaim = false; var rewards: [CommerceItem] = []
    var eventIds: [Int] = []
    init(_ j: JSONValue) {
        id = j.i("id"); name = j.str("name"); litCount = j.i("litCount"); totalCount = j.i("totalCount")
        claimed = j.flag("claimed"); canClaim = j.flag("canClaim"); rewards = CommerceItem.list(j.arr("rewards"))
        eventIds = j.arr("eventIds").compactMap(\.intValue)
    }
}

struct SolarEvent: Identifiable {
    var id: Int; var name: String; var description = ""
    var startSec: Int64 = 0; var endSec: Int64 = 0; var status = 0
    var directClaimed = false; var quizClaimed = false; var quizWrongToday = false
    var question = ""; var options: [String] = []; var errorTips = ""
    var rewards: [CommerceItem] = []; var questionRewards: [CommerceItem] = []
    var canClaim = false; var canAnswer = false
    init(_ j: JSONValue) {
        id = j.i("id"); name = j.str("name"); description = j.str("description")
        startSec = normalizeSec(j.l("startTime")); endSec = normalizeSec(j.l("endTime")); status = j.i("status")
        directClaimed = j.flag("directClaimed"); quizClaimed = j.flag("quizClaimed"); quizWrongToday = j.flag("quizWrongToday")
        question = j.str("question"); options = j.arr("options").compactMap(\.stringValue); errorTips = j.str("errorTips")
        rewards = CommerceItem.list(j.arr("rewards")); questionRewards = CommerceItem.list(j.arr("questionRewards"))
        canClaim = j.flag("canClaim"); canAnswer = j.flag("canAnswer")
    }
}

struct SolarCalendarState {
    var serverTimeSec: Int64 = 0; var defaultEventId = 0
    var seasons: [SolarSeason] = []; var events: [SolarEvent] = []
    init() {}
    init(_ j: JSONValue) {
        serverTimeSec = normalizeSec(j.l("serverTime")); defaultEventId = j.i("defaultEventId")
        seasons = j.arr("seasons").map(SolarSeason.init); events = j.arr("events").map(SolarEvent.init)
    }
}

struct SolarActionResult {
    var rewards: [CommerceItem] = []; var correct: Bool?; var calendar: SolarCalendarState?; var refreshRequired = false
    init() {}
    init(_ j: JSONValue) {
        rewards = CommerceItem.list(j.arr("rewards"))
        if !j.isNull("correct") { correct = j.flag("correct") }
        if let c = j.obj("calendar") { calendar = SolarCalendarState(c) }
        refreshRequired = j.flag("refreshRequired")
    }
}

struct ActivityDirectoryItem: Identifiable {
    var id: String; var activityIds: [String]; var name: String
    var startAt: Date?; var endAt: Date?; var instanceId: String?
    var gameplayKey: String?; var detailTarget: String?; var gameplayTargets: [String] = []
    // Raw millisecond values are kept so malformed/zero timestamps can be
    // classified the same way as the Android client and the web console.
    var startTimeMs: Int64 = 0
    var endTimeMs: Int64 = 0
    var hasStartTime = false
    var hasEndTime = false

    init(_ j: JSONValue) {
        id = j.str("id")
        let ids = j.arr("activityIds").compactMap(\.stringValue)
        activityIds = ids.isEmpty ? [id] : ids
        name = j.str("name")
        hasStartTime = j.has("startTime") && !j.isNull("startTime")
        hasEndTime = j.has("endTime") && !j.isNull("endTime")
        startTimeMs = normalizeDirectoryTimeMs(j.l("startTime"))
        endTimeMs = normalizeDirectoryTimeMs(j.l("endTime"))
        if hasStartTime, startTimeMs > 0 { startAt = dateFromMs(startTimeMs) }
        if hasEndTime, endTimeMs > 0 { endAt = dateFromMs(endTimeMs) }
        instanceId = j.str("instanceId").isEmpty ? nil : j.str("instanceId")
        gameplayKey = j.str("gameplayKey").isEmpty ? nil : j.str("gameplayKey")
        detailTarget = j.str("detailTarget").isEmpty ? nil : j.str("detailTarget")
        gameplayTargets = j.arr("gameplayTargets").compactMap(\.stringValue)
    }
    var typeKey: String {
        let targets = [detailTarget, gameplayKey].compactMap { $0 } + gameplayTargets
        for raw in targets {
            switch raw.trimmingCharacters(in: .whitespacesAndNewlines).lowercased() {
            case "muge": return "muge"
            case "milk", "milk-tea": return "milktea"
            case "happy", "share-reward": return "share"
            case "wish", "autumn-wish": return "autumn"
            case "stellar": return "stellar"
            case "qixi": return "qixi"
            case "qingmei": return "qingmei"
            case "charity", "charity-red-flower": return "charity"
            case "weather": return "weather"
            default: break
            }
        }
        let ids = [id] + activityIds
        for raw in ids {
            switch raw.trimmingCharacters(in: .whitespacesAndNewlines) {
            case "2026111700", "2026111701", "2026111702", "2026111703", "2026111704", "2026111705", "2026111706": return "muge"
            case "2026110100", "2026110101", "2026110102", "2026110103": return "milktea"
            case "2026092500", "2026092501", "2026110104": return "share"
            case "2026092400", "2026092401": return "autumn"
            case "2026072700", "2026072701": return "stellar"
            case "2026081800", "2026081801", "2026081802": return "qixi"
            case "2026081200", "2026081201", "2026081202": return "qingmei"
            case "2026090900", "2026090901": return "charity"
            case "2026070300", "2026070301", "2026070302", "2026070303", "2026070304", "2026070305": return "weather"
            default: break
            }
        }
        let joined = ([id] + activityIds + targets).joined(separator: " ").lowercased()
        if joined.contains("muge") { return "muge" }
        if joined.contains("milk") { return "milktea" }
        if joined.contains("share") { return "share" }
        if joined.contains("autumn") || joined.contains("wish") { return "autumn" }
        if joined.contains("stellar") { return "stellar" }
        if joined.contains("qixi") { return "qixi" }
        if joined.contains("qingmei") { return "qingmei" }
        if joined.contains("charity") { return "charity" }
        if joined.contains("weather") { return "weather" }
        return "generic"
    }
}

extension Optional where Wrapped == String {
    var orEmpty: String { self ?? "" }
}

struct MugeTier: Identifiable {
    var id: String; var threshold: Int64; var status: Int; var rewards: [CommerceItem]
    init(_ j: JSONValue) {
        id = j.str("id"); threshold = j.l("threshold"); status = j.i("status"); rewards = CommerceItem.list(j.arr("rewards"))
    }
    var stateText: String { status == 2 ? "已领取" : (status == 1 ? "可领取" : "未达成") }
}

struct MugeTask: Identifiable {
    var id: String; var description: String; var progress: Int; var target: Int
    var status: Int; var points: Int; var rewards: [CommerceItem]
    init(_ j: JSONValue) {
        id = j.str("id"); description = j.str("description"); progress = j.i("progress"); target = j.i("target")
        status = j.i("status"); points = j.i("points"); rewards = CommerceItem.list(j.arr("rewards"))
    }
}

struct MugeOption: Identifiable {
    var id: Int; var label: String
    init(_ j: JSONValue) {
        id = j.i("value")
        label = j.str("label").isEmpty ? j.str("text") : j.str("label")
    }
}

struct MugeQuestion: Identifiable {
    var id: Int; var title: String; var options: [MugeOption]
    init(_ j: JSONValue) {
        id = j.i("id"); title = j.str("title")
        options = j.arr("options").map(MugeOption.init)
    }
}

struct MugeState {
    var title = "牧歌初启"; var active = false; var endAt: Date?
    var celebrationTotal: Int64 = 0; var celebrationTiers: [MugeTier] = []
    var friendsCount: Int64 = 0; var friendsTiers: [MugeTier] = []
    var handbookPoints = 0; var handbookTiers: [MugeTier] = []; var handbookTasks: [MugeTask] = []
    var answers: [Int: Int] = [:]; var personalityId = 0; var questions: [MugeQuestion] = []; var rules: [String] = []
    init() {}
    init(_ j: JSONValue) {
        title = j.str("title", "牧歌初启"); active = j.flag("active"); endAt = dateFromMs(j.l("endTime"))
        let cel = j.obj("celebration") ?? .null
        celebrationTotal = cel.l("total")
        celebrationTiers = cel.arr("tiers").map(MugeTier.init)
        if let fr = j.obj("friends") { friendsCount = fr.l("count"); friendsTiers = fr.arr("tiers").map(MugeTier.init) }
        if let hb = j.obj("handbook") {
            handbookPoints = hb.i("points"); handbookTiers = hb.arr("tiers").map(MugeTier.init)
            handbookTasks = hb.arr("tasks").map(MugeTask.init)
        }
        if let p = j.obj("personality") {
            personalityId = p.i("personalityId")
            for (k, v) in p.obj("answers")?.objectValue ?? [:] {
                if let q = Int(k), let a = v.intValue { answers[q] = a }
            }
        }
        questions = j.arr("questions").map(MugeQuestion.init)
        rules = j.arr("rules").compactMap(\.stringValue)
    }
}

struct ShareMilestone: Identifiable {
    var id: String; var threshold: Int64; var state: Int; var rewards: [CommerceItem]
    init(_ j: JSONValue) {
        id = j.str("id"); threshold = j.l("threshold"); state = j.i("state"); rewards = CommerceItem.list(j.arr("rewards"))
    }
}

struct ShareRewardState {
    var id = ""; var title = "快乐不独享"; var active = false; var endAt: Date?
    var score: Int64 = 0; var canShare = false; var canClaimDaily = false; var canClaimMilestones = false
    var dailyReward = 0; var firstShareReward = 0; var firstShareAwarded = false
    var claimedCount = 0; var claimLimit = 0; var milestones: [ShareMilestone] = []; var rules: [String] = []
    init() {}
    init(_ j: JSONValue) {
        id = j.str("id"); title = j.str("title", "快乐不独享"); active = j.flag("active")
        endAt = dateFromMs(j.l("endTime")); score = j.l("score")
        canShare = j.flag("canShare"); canClaimDaily = j.flag("canClaimDaily"); canClaimMilestones = j.flag("canClaimMilestones")
        dailyReward = j.i("dailyReward"); firstShareReward = j.i("firstShareReward"); firstShareAwarded = j.flag("firstShareAwarded")
        claimedCount = j.i("claimedCount"); claimLimit = j.i("claimLimit")
        milestones = j.arr("milestones").map(ShareMilestone.init); rules = j.arr("rules").compactMap(\.stringValue)
    }
}

struct AutumnChoice: Identifiable {
    var id: Int; var name: String
    init(_ j: JSONValue) { id = j.i("id"); name = j.str("name") }
}

struct AutumnRewardDay: Identifiable {
    var day: Int; var reward: CommerceItem
    var id: Int { day }
    init(_ j: JSONValue) { day = j.i("day"); reward = CommerceItem(j.obj("reward") ?? .null) }
}

struct AutumnWishState {
    var title = "秋祈良愿"; var active = false; var endAt: Date?; var day = 0; var remaining = 0
    var canDraw = false; var canClaim = false; var choices: [AutumnChoice] = []
    var pendingChooseId = 0; var pendingText = ""; var pendingRewards: [CommerceItem] = []
    var rewardDays: [AutumnRewardDay] = []; var rules: [String] = []
    init() {}
    init(_ j: JSONValue) {
        title = j.str("title", "秋祈良愿"); active = j.flag("active"); endAt = dateFromMs(j.l("endTime"))
        day = j.i("day"); remaining = j.i("remaining"); canDraw = j.flag("canDraw"); canClaim = j.flag("canClaim")
        choices = j.arr("choices").map(AutumnChoice.init)
        if let p = j.obj("pending") {
            pendingChooseId = p.i("chooseId"); pendingText = p.str("text")
            pendingRewards = CommerceItem.list(p.arr("rewards"))
        }
        rewardDays = j.arr("rewardDays").map(AutumnRewardDay.init)
        rules = j.arr("rules").compactMap(\.stringValue)
    }
}

struct StellarPassNode: Identifiable {
    var id: String; var level: String; var claimed: Bool; var claimable: Bool; var locked: Bool; var rewards: [CommerceItem]
    init(_ j: JSONValue) {
        id = j.str("id"); level = j.str("level"); claimed = j.flag("claimed"); claimable = j.flag("claimable")
        locked = j.flag("locked"); rewards = CommerceItem.list(j.arr("rewards"))
    }
}

struct StellarConstellation: Identifiable {
    var id: String; var name: String; var explain: String; var visualState: String; var rewards: [CommerceItem]
    init(_ j: JSONValue) {
        id = j.str("id"); name = j.str("name"); explain = j.str("explain")
        visualState = j.str("visualState"); rewards = CommerceItem.list(j.arr("rewards"))
    }
}

struct StellarShopGood: Identifiable {
    var id: String; var name: String; var item: CommerceItem; var cost: CommerceItem
    var exchangeable: Bool; var soldOut: Bool; var maxExchangeCount: Int
    init(_ j: JSONValue) {
        id = j.str("id"); name = j.str("name"); item = CommerceItem(j.obj("item") ?? .null)
        cost = CommerceItem(j.obj("cost") ?? .null)
        exchangeable = j.flag("exchangeable"); soldOut = j.flag("soldOut"); maxExchangeCount = j.i("maxExchangeCount")
    }
}

struct StellarTerm: Identifiable {
    var id: String; var name: String; var description: String
    var claimed: Bool; var claimable: Bool; var locked: Bool; var rewards: [CommerceItem]
    init(_ j: JSONValue) {
        id = j.str("id"); name = j.str("name"); description = j.str("description")
        let status = j.str("statusCode")
        claimed = status == "3" || j.flag("claimed")
        claimable = j.flag("canClaim") || j.flag("claimable")
        locked = status == "1" || j.flag("locked")
        rewards = CommerceItem.list(j.arr("rewards"))
    }
}

struct StellarSnapshot {
    var title = "千星游记"; var endAt: Date?; var passLevel = "0"; var passProgress = 0; var passProgressMax = 0
    var canClaimPass = false; var passNodes: [StellarPassNode] = []
    var canLightConstellation = false; var constellationGroups: [StellarConstellation] = []
    var shopCurrencyName = "星砂"; var shopBalance = "--"; var shopGoods: [StellarShopGood] = []; var solarTerms: [StellarTerm] = []
    init() {}
    init(_ j: JSONValue) {
        let season = j.obj("season") ?? .null
        let pass = season.obj("pass") ?? .null
        let constellation = j.obj("constellation") ?? .null
        let shop = j.obj("shop") ?? .null
        let solar = j.obj("solarTerms") ?? .null
        let actions = j.obj("actions") ?? .null
        title = season.str("title", "千星游记"); endAt = dateFromMs(season.l("endTime"))
        passLevel = pass.str("level", "0"); passProgress = pass.i("progress"); passProgressMax = pass.i("progressMax")
        passNodes = pass.arr("nodes").map { StellarPassNode($0) }
        canClaimPass = actions.obj("claimPass")?.flag("available") ?? passNodes.contains { $0.claimable }
        constellationGroups = constellation.arr("groups").map { StellarConstellation($0) }
        canLightConstellation = actions.obj("lightConstellation")?.flag("attemptable")
            ?? constellationGroups.contains { $0.visualState == "lightable" }
        let currencies = shop.arr("currencies")
        let first = currencies.first
        shopCurrencyName = first?.str("name", "星砂") ?? "星砂"
        shopBalance = first?.str("balance", "--") ?? "--"
        shopGoods = shop.arr("goods").map { StellarShopGood($0) }
        solarTerms = solar.arr("terms").map { StellarTerm($0) }
    }
}

struct SeasonalStage: Identifiable {
    var id: String; var label: String; var statusText: String
    var claimable: Bool; var claimed: Bool; var rewards: [CommerceItem]
    init(id: String, label: String, statusText: String, claimable: Bool, claimed: Bool, rewards: [CommerceItem]) {
        self.id = id; self.label = label; self.statusText = statusText
        self.claimable = claimable; self.claimed = claimed; self.rewards = rewards
    }
    init(_ j: JSONValue, index: Int) {
        id = j.str("id"); label = "第 \(j.i("stage", index + 1)) 阶段"
        claimed = j.flag("claimed"); claimable = j.flag("claimable")
        statusText = claimed ? "已领取" : (claimable ? "可领取" : "未达成")
        rewards = CommerceItem.list(j.arr("rewards"))
    }
}

struct QixiState {
    var title = "鹊桥寄情"; var active = false; var endAt: Date?
    var feather = "--"; var sachet = "--"; var canClaimBridge = false; var stages: [SeasonalStage] = []
    init() {}
    init(_ j: JSONValue) {
        title = j.str("title", "鹊桥寄情"); active = j.flag("active"); endAt = dateFromMs(j.l("endTime"))
        if let b = j.obj("balances") { feather = b.str("feather", "--"); sachet = b.str("sachet", "--") }
        canClaimBridge = j.obj("actions")?.obj("bridge")?.flag("available") ?? (j.obj("bridge")?.flag("claimable") ?? false)
        stages = j.obj("bridge")?.arr("stages").enumerated().map { SeasonalStage($0.element, index: $0.offset) } ?? []
    }
}


struct QingMeiState {
    var title = "青酿换万金"
    var balance = "--"
    var dailySeedClaimed = false
    var started = false
    var currentRound = 0
    var maxRounds = 3
    var finished = false
    var currentQuoteGold = "0"
    var canStart = false
    var canContinue = false
    var canSettle = false
    var ingredientsCount = 0
    var firstIngredientUid = ""
    var firstIngredientCount: Int64 = 0
    init() {}
    init(_ j: JSONValue) {
        let actions = j.obj("actions") ?? .null
        title = j.str("name", "青酿换万金")
        balance = j.str("balance", "--").isEmpty ? "--" : j.str("balance", "--")
        dailySeedClaimed = j.obj("dailySeed")?.flag("claimed") ?? false
        started = j.flag("started"); currentRound = j.i("currentRound")
        maxRounds = j.i("maxRounds", 3); finished = j.flag("finished")
        currentQuoteGold = j.arr("quoteTotals").last?.stringValue ?? j.str("baseGold", "0")
        canStart = actions.obj("start")?.flag("available") ?? false
        canContinue = actions.obj("continue")?.flag("available") ?? false
        canSettle = actions.obj("settle")?.flag("available") ?? false
        let ingredients = j.arr("ingredients")
        ingredientsCount = ingredients.count
        if let first = ingredients.first {
            firstIngredientUid = first.str("uid"); firstIngredientCount = first.l("count")
        }
    }
}

struct CharityState {
    var title = "公益小红花"
    var active = false
    var endAt: Date?
    var loveBalance = "0"
    var donatedLove = "0"
    var agreementAccepted = false
    var canClaimSeeds = false
    var canShare = false
    var canDonateLove = false
    var canClaimDailyGift = false
    var stages: [SeasonalStage] = []
    init() {}
    init(_ j: JSONValue) {
        let actions = j.obj("actions") ?? .null
        title = j.str("title", "公益小红花"); active = j.flag("active")
        endAt = dateFromMs(j.l("endTime"))
        loveBalance = j.str("loveBalance", "0"); donatedLove = j.str("donatedLove", "0")
        agreementAccepted = j.str("agreementStatus") == "1"
        canClaimSeeds = actions.obj("claimSeeds")?.flag("available") ?? false
        canShare = actions.obj("share")?.flag("available") ?? false
        canDonateLove = actions.obj("donateLove")?.flag("available") ?? false
        canClaimDailyGift = actions.obj("claimDailyGift")?.flag("attemptable") ?? false
        stages = j.arr("progressRewards").map { p in
            let claimed = p.flag("claimed"); let claimable = p.flag("claimable")
            let target = p.str("target")
            return SeasonalStage(id: target, label: target + " 爱心",
                                 statusText: claimed ? "已领取" : (claimable ? "可领取" : "未达成"),
                                 claimable: claimable, claimed: claimed,
                                 rewards: p.obj("reward").map { [CommerceItem($0)] } ?? [])
        }
    }
}

struct WeatherState {
    var title = "雨落成诗"
    var active = false
    var endAt: Date?
    var badgeBalance = "--"
    var collectionBottleCount = "--"
    var rainBottleCount = "--"
    var activeWeatherName = ""
    var canExchangeCollector = false
    var canSummonRain = false
    var nextResearchNodeId = ""
    var canAdvanceResearch = false
    init() {}
    init(_ j: JSONValue) {
        let activity = j.obj("activity") ?? .null
        let actions = j.obj("actions") ?? .null
        let own = j.obj("ownWeather") ?? .null
        title = activity.str("name", "雨落成诗"); active = j.flag("active")
        endAt = dateFromMs(activity.l("endTime"))
        var inventory: [Int: Int64] = [:]
        for item in CommerceItem.list(j.arr("inventory")) { inventory[item.id] = item.count }
        badgeBalance = inventory[1027].map { String($0) } ?? "--"
        collectionBottleCount = inventory[5001].map { String($0) } ?? "--"
        rainBottleCount = inventory[5002].map { String($0) } ?? "--"
        if own.flag("active") {
            activeWeatherName = own.flag("isThunderstorm") ? "雷雨天气中" : (own.str("typeName").isEmpty ? "特殊天气中" : own.str("typeName"))
        } else {
            activeWeatherName = "晴朗"
        }
        canExchangeCollector = actions.obj("exchangeCollector")?.flag("enabled") ?? false
        canSummonRain = actions.obj("summonThunderstorm")?.flag("enabled") ?? false
        if let research = actions.obj("advanceResearch") {
            nextResearchNodeId = research.str("nodeId")
            canAdvanceResearch = research.flag("enabled")
        }
    }
}


struct LoginConfigModel {
    var wechatQrLogin = true
    var qqQrLogin = false
    init() {}
    init(_ j: JSONValue) {
        wechatQrLogin = j.flag("wechatQrLogin", true)
        qqQrLogin = j.flag("qqQrLogin", false)
    }
}

struct WxQrTask {
    var taskId = ""
    var status = ""
    var qrUrl = ""
    var expiresAt: Date?
    init() {}
    init(_ j: JSONValue) {
        taskId = j.str("task_id"); status = j.str("status"); qrUrl = j.str("qr_url")
        expiresAt = dateFromMs(j.l("expires_at"))
    }
}

struct QqQrTask {
    var taskId = ""
    var status = ""
    var qrImage = ""
    var expiresAt: Date?
    init() {}
    init(_ j: JSONValue) {
        taskId = j.str("task_id"); status = j.str("status"); qrImage = j.str("qr_image")
        expiresAt = dateFromMs(j.l("expires_at"))
    }
}
struct IllustratedShopSeed {
    var seedId = 0; var goodsId = 0; var name = ""; var price: Int64 = 0
    var locked = false; var soldOut = false; var itemCount = 1
    init(_ j: JSONValue) {
        seedId = j.i("seedId"); goodsId = j.i("goodsId"); name = j.str("name"); price = j.l("price")
        locked = j.flag("locked"); soldOut = j.flag("soldOut"); itemCount = max(1, j.i("itemCount", 1))
    }
}

struct IllustratedItem: Identifiable {
    var seedId: Int; var name: String; var image: String; var unlocked: Bool; var progress: Int
    var shopSeed: IllustratedShopSeed?
    var id: Int { seedId }
    init(_ j: JSONValue) {
        seedId = j.i("seedId"); name = j.str("name"); image = j.str("image")
        unlocked = j.flag("unlocked"); progress = j.i("progress")
        if let s = j.obj("shopSeed") { shopSeed = IllustratedShopSeed(s) }
    }
}

struct IllustratedLevel: Identifiable {
    var level: Int; var progress: Int; var claimed: Bool; var canClaim: Bool; var rewards: [CommerceItem]
    var id: Int { level }
    init(_ j: JSONValue) {
        level = j.i("level"); progress = j.i("progress"); claimed = j.flag("claimed"); canClaim = j.flag("canClaim")
        rewards = CommerceItem.list(j.arr("rewards"))
    }
}

struct IllustratedBook {
    var type = 1; var level = 0; var progress = 0; var nextLevelProgress = 0; var claimable = false
    var levels: [IllustratedLevel] = []; var items: [IllustratedItem] = []
    init() {}
    init(_ j: JSONValue, type: Int) {
        self.type = j.i("type", type); level = j.i("level"); progress = j.i("progress")
        nextLevelProgress = j.i("nextLevelProgress"); claimable = j.flag("claimable")
        levels = j.arr("levels").map(IllustratedLevel.init); items = j.arr("items").map(IllustratedItem.init)
    }
}

struct IllustratedSnapshot {
    var crop = IllustratedBook(); var mutant = IllustratedBook()
    init() {}
    init(_ j: JSONValue) {
        if let c = j.obj("crop") { crop = IllustratedBook(c, type: 1) }
        if let m = j.obj("mutant") { mutant = IllustratedBook(m, type: 2) }
    }
}

struct CollectionMilestone: Identifiable {
    var level: Int; var score: Int64; var claimed: Bool; var reached: Bool; var rewards: [CommerceItem]
    var id: Int { level }
    init(_ j: JSONValue) {
        level = j.i("level"); score = j.l("score"); claimed = j.flag("claimed"); reached = j.flag("reached")
        rewards = CommerceItem.list(j.arr("rewards"))
    }
}

struct CollectionItem: Identifiable {
    var id: Int; var name: String; var description: String; var image: String
    var starLevel: Int; var maxStarLevel: Int; var ownCount: Int64
    var activated: Bool; var displayed: Bool; var canUpgrade: Bool; var nextStarCosts: [CommerceItem]
    init(_ j: JSONValue) {
        id = j.i("id"); name = j.str("name"); description = j.str("description"); image = j.str("image")
        starLevel = j.i("starLevel"); maxStarLevel = j.i("maxStarLevel", 5); ownCount = j.l("ownCount")
        activated = j.flag("activated"); displayed = j.flag("displayed"); canUpgrade = j.flag("canUpgrade")
        nextStarCosts = CommerceItem.list(j.arr("nextStarCosts"))
    }
}

struct CollectionSeries: Identifiable {
    var id: Int; var name: String; var rarityName: String; var description: String
    var items: [CollectionItem]; var activatedCount: Int; var totalCount: Int
    init(_ j: JSONValue) {
        id = j.i("id"); name = j.str("name"); rarityName = j.str("rarityName"); description = j.str("description")
        items = j.arr("items").map(CollectionItem.init)
        activatedCount = j.i("activatedCount"); totalCount = j.i("totalCount")
    }
}

struct CollectionSnapshot {
    var score: Int64 = 0; var activatedCount = 0; var totalCount = 0
    var milestones: [CollectionMilestone] = []; var series: [CollectionSeries] = []; var displayItemId = "0"
    init() {}
    init(_ j: JSONValue) {
        score = j.l("score"); activatedCount = j.i("activatedCount"); totalCount = j.i("totalCount")
        milestones = j.arr("milestones").map(CollectionMilestone.init)
        series = j.arr("series").map(CollectionSeries.init)
        displayItemId = j.obj("display")?.str("itemId", "0") ?? "0"
    }
}

struct CollectionOperateResult {
    var snapshot = CollectionSnapshot(); var rewards: [CommerceItem] = []; var message = ""
    init() {}
    init(_ j: JSONValue, fallback: String) {
        if let s = j.obj("snapshot") { snapshot = CollectionSnapshot(s) }
        rewards = CommerceItem.list(j.arr("rewards"))
        let rewardsText = rewards.map(\.text).joined(separator: "、")
        message = rewardsText.isEmpty ? fallback : rewardsText
    }
}
