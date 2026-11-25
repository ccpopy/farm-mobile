package com.qqfarm.mobile.data.network

import com.qqfarm.mobile.data.model.AccountItem
import com.qqfarm.mobile.data.model.AccountSettingsModel
import com.qqfarm.mobile.data.model.BagItem
import com.qqfarm.mobile.data.model.BagPayload
import com.qqfarm.mobile.data.model.BagSystemItem
import com.qqfarm.mobile.data.model.DailyGiftItem
import com.qqfarm.mobile.data.model.DailyGiftsSummary
import com.qqfarm.mobile.data.model.DogFood
import com.qqfarm.mobile.data.model.FriendBlacklistItem
import com.qqfarm.mobile.data.model.FriendItem
import com.qqfarm.mobile.data.model.FriendLandsPayload
import com.qqfarm.mobile.data.model.FriendOperationResult
import com.qqfarm.mobile.data.model.FriendsPayload
import com.qqfarm.mobile.data.model.InteractRecord
import com.qqfarm.mobile.data.model.LandItem
import com.qqfarm.mobile.data.model.LandLevelBonus
import com.qqfarm.mobile.data.model.LandSummary
import com.qqfarm.mobile.data.model.LandUpgradeCost
import com.qqfarm.mobile.data.model.LandUpgradeInfo
import com.qqfarm.mobile.data.model.LandsPayload
import com.qqfarm.mobile.data.model.LevelProgress
import com.qqfarm.mobile.data.model.LoginConfigModel
import com.qqfarm.mobile.data.model.MobileOfflineEvent
import com.qqfarm.mobile.data.model.PetDog
import com.qqfarm.mobile.data.model.PetSnapshot
import com.qqfarm.mobile.data.model.PluginAccountDetail
import com.qqfarm.mobile.data.model.PluginDefinition
import com.qqfarm.mobile.data.model.PluginJob
import com.qqfarm.mobile.data.model.PluginListPayload
import com.qqfarm.mobile.data.model.PluginLogEntry
import com.qqfarm.mobile.data.model.PluginOpCost
import com.qqfarm.mobile.data.model.PluginPendingOp
import com.qqfarm.mobile.data.model.PluginQuietWindow
import com.qqfarm.mobile.data.model.PluginSpendingPolicy
import com.qqfarm.mobile.data.model.PluginSpendingRule
import com.qqfarm.mobile.data.model.PluginSpendingUsage
import com.qqfarm.mobile.data.model.PluginSummary
import com.qqfarm.mobile.data.model.RanchAnimalOption
import com.qqfarm.mobile.data.model.SeedOption
import com.qqfarm.mobile.data.model.QqQrTask
import com.qqfarm.mobile.data.model.RanchAnimal
import com.qqfarm.mobile.data.model.RanchBucket
import com.qqfarm.mobile.data.model.RanchCurrency
import com.qqfarm.mobile.data.model.RanchFacility
import com.qqfarm.mobile.data.model.RanchFacilityNext
import com.qqfarm.mobile.data.model.RanchCareer
import com.qqfarm.mobile.data.model.RanchPayload
import com.qqfarm.mobile.data.model.RanchPenGroup
import com.qqfarm.mobile.data.model.RunLogEntry
import com.qqfarm.mobile.data.model.ServerVersionInfo
import com.qqfarm.mobile.data.model.StatusSnapshot
import com.qqfarm.mobile.data.model.WxQrTask
import com.qqfarm.mobile.data.network.json.JSONArray
import com.qqfarm.mobile.platform.formatEpoch
import com.qqfarm.mobile.platform.wallClockMillis
import com.qqfarm.mobile.data.network.json.JSONObject

internal fun JSONObject.optStringClean(key: String, fallback: String = ""): String {
    if (!has(key) || isNull(key)) return fallback
    val raw = opt(key)?.toString()?.trim().orEmpty()
    return if (raw == "null") fallback else raw
}

internal fun JSONObject.optLongFlex(key: String, fallback: Long = 0L): Long {
    if (!has(key) || isNull(key)) return fallback
    return when (val v = opt(key)) {
        is Number -> v.toLong()
        is String -> v.trim().toDoubleOrNull()?.toLong() ?: fallback
        else -> fallback
    }
}

internal fun JSONObject.optIntFlex(key: String, fallback: Int = 0): Int =
    optLongFlex(key, fallback.toLong()).toInt()

internal fun JSONObject.optBoolFlex(key: String, fallback: Boolean = false): Boolean {
    if (!has(key) || isNull(key)) return fallback
    return when (val v = opt(key)) {
        is Boolean -> v
        is Number -> v.toInt() != 0
        is String -> v.equals("true", ignoreCase = true) || v == "1"
        else -> fallback
    }
}

internal fun JSONObject.optBoolNullable(key: String): Boolean? {
    if (!has(key) || isNull(key)) return null
    return optBoolFlex(key)
}

object JsonParsers {

    fun parseServerVersion(json: JSONObject): ServerVersionInfo = ServerVersionInfo(
        clientVersion = json.optStringClean("clientVersion"),
        wxClientVersion = json.optStringClean("wxClientVersion"),
        botVersion = json.optStringClean("botVersion"),
    )

    fun parseAccounts(dataObj: JSONObject): List<AccountItem> {
        val arr = dataObj.optJSONArray("accounts") ?: JSONArray()
        return buildList(arr.length()) {
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                add(
                    AccountItem(
                        id = obj.optStringClean("id"),
                        name = obj.optStringClean("name"),
                        nick = obj.optStringClean("nick"),
                        uin = obj.optLongFlex("uin"),
                        avatar = obj.optStringClean("avatar"),
                        platform = obj.optStringClean("platform", "qq"),
                        running = obj.optBoolFlex("running"),
                    )
                )
            }
        }
    }

    fun parseStatusSnapshot(
        accountId: String,
        dataObj: JSONObject,
        serverTimeHintMs: Long = 0L,
    ): StatusSnapshot {
        val statusObj = dataObj.optJSONObject("status") ?: JSONObject()
        val connObj = dataObj.optJSONObject("connection") ?: JSONObject()
        val progressObj = dataObj.optJSONObject("levelProgress")
        val nextChecks = dataObj.optJSONObject("nextChecks") ?: JSONObject()
        val opsObj = dataObj.optJSONObject("operations") ?: JSONObject()
        val opsMap = buildMap {
            for (key in opsObj.keys()) {
                if (key != "upgrade" && key != "levelUp") {
                    put(key, opsObj.optIntFlex(key))
                }
            }
        }
        val forecastObj = dataObj.optJSONObject("levelForecast")
        val forecastText = buildForecastText(forecastObj)
        val snapshotTimeMs = dataObj.optLongFlex("serverTime").takeIf { it > 0L }
        val serverNowMs = serverTimeHintMs.takeIf { it > 0L } ?: snapshotTimeMs ?: wallClockMillis()

        fun nextRunAtMs(deadlineKey: String, vararg remainingKeys: String): Long? {
            val deadline = dataObj.optLongFlex(deadlineKey, nextChecks.optLongFlex(deadlineKey, -1L))
            if (deadline >= 0L) return deadline

            // 上游服务端也会只返回快照时的剩余秒数，0 表示等待下次排程。
            val remaining = remainingKeys.firstNotNullOfOrNull { key ->
                nextChecks.optLongFlex(key, -1L).takeIf { it >= 0L }
            } ?: return null
            return if (remaining == 0L) 0L else (snapshotTimeMs ?: serverNowMs) + remaining * 1000L
        }

        return StatusSnapshot(
            accountId = accountId,
            name = statusObj.optStringClean("name"),
            level = statusObj.optIntFlex("level"),
            exp = statusObj.optLongFlex("exp"),
            gold = statusObj.optLongFlex("gold"),
            coupon = statusObj.optLongFlex("coupon"),
            goldBean = statusObj.optLongFlex("goldBean"),
            avatarUrl = statusObj.optStringClean("avatarUrl"),
            connected = connObj.optBoolFlex("connected"),
            uptimeSec = dataObj.optLongFlex("uptime"),
            sessionElapsedSec = dataObj.optLongFlex("sessionElapsedSeconds"),
            sessionExpGained = dataObj.optLongFlex("sessionExpGained"),
            sessionGoldGained = dataObj.optLongFlex("sessionGoldGained"),
            sessionCouponGained = dataObj.optLongFlex("sessionCouponGained"),
            levelProgress = LevelProgress(
                current = progressObj?.optLongFlex("current") ?: 0L,
                needed = progressObj?.optLongFlex("needed") ?: 0L,
            ),
            levelForecastText = forecastText,
            nextFarmRunAtMs = nextRunAtMs("nextFarmRunAt", "farmRemainSec"),
            nextFriendRunAtMs = nextRunAtMs("nextFriendRunAt", "friendRemainSec", "helpRemainSec", "stealRemainSec"),
            farmQuiet = nextChecks.optBoolFlex("farmQuiet"),
            friendQuiet = nextChecks.optBoolFlex("friendQuiet")
                || nextChecks.optBoolFlex("helpQuiet")
                || nextChecks.optBoolFlex("stealQuiet"),
            serverTimeMs = serverNowMs,
            receivedAtElapsedMs = com.qqfarm.mobile.platform.monotonicMillis(),
            operations = opsMap,
            ranchOperations = dataObj.optJSONObject("ranchOperations")?.let { ops ->
                listOf("harvest", "produce", "helpFarming", "steal")
                    .filter { !ops.isNull(it) }.associateWith { ops.optIntFlex(it) }
            },
        )
    }

    private fun buildForecastText(forecastObj: JSONObject?): String {
        if (forecastObj == null) return ""
        val source = forecastObj.optStringClean("source")
        if (source == "ready") return "当前可升下一级"
        val estimatedAt = forecastObj.optLongFlex("estimatedAt")
        if (estimatedAt <= 0L || source == "unavailable") return ""
        val deltaSec = ((estimatedAt - wallClockMillis()) / 1000L).coerceAtLeast(0L)
        return when {
            deltaSec < 60L -> "预计 1 分钟内升级"
            deltaSec < 3600L -> "预计 ${deltaSec / 60L} 分钟后升级"
            deltaSec < 86400L -> {
                val hours = deltaSec / 3600L
                val mins = (deltaSec % 3600L) / 60L
                if (mins > 0L) "预计 ${hours}小时${mins}分后升级" else "预计 ${hours}小时后升级"
            }
            else -> {
                "预计 ${formatEpoch("MM-dd HH:mm", estimatedAt)} 升级"
            }
        }
    }

    fun parseRunLogEntry(obj: JSONObject): RunLogEntry {
        val meta = obj.optJSONObject("meta")
        val ts = obj.optLongFlex("ts").let { if (it > 0L) it else wallClockMillis() }
        val time = obj.optStringClean("time").ifBlank {
            formatEpoch("HH:mm:ss", ts)
        }
        val actionsArr = meta?.optJSONArray("actions")
        val actionsList = buildList {
            if (actionsArr != null) {
                for (i in 0 until actionsArr.length()) {
                    val action = actionsArr.optString(i).trim()
                    if (action.isNotBlank()) add(action)
                }
            }
        }
        return RunLogEntry(
            ts = ts,
            time = time,
            tag = obj.optStringClean("tag"),
            msg = obj.optStringClean("msg"),
            module = meta?.optStringClean("module").orEmpty().ifBlank { obj.optStringClean("module") },
            event = meta?.optStringClean("event").orEmpty().ifBlank { obj.optStringClean("event") },
            result = meta?.optStringClean("result") ?: "",
            isWarn = obj.optBoolFlex("isWarn"),
            accountId = obj.optStringClean("accountId"),
            friendName = meta?.optStringClean("friendName").orEmpty(),
            actions = actionsList,
            searchText = obj.optStringClean("_searchText").ifBlank { meta?.toString().orEmpty() },
        )
    }

    fun parseRunLogs(arr: JSONArray?): List<RunLogEntry> {
        if (arr == null) return emptyList()
        return buildList(arr.length()) {
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                add(parseRunLogEntry(obj))
            }
        }
    }

    fun parseDailyGifts(obj: JSONObject): DailyGiftsSummary {
        val arr = obj.optJSONArray("gifts") ?: JSONArray()
        val gifts = buildList(arr.length()) {
            for (i in 0 until arr.length()) {
                val g = arr.optJSONObject(i) ?: continue
                add(
                    DailyGiftItem(
                        key = g.optStringClean("key"),
                        label = g.optStringClean("label"),
                        enabled = g.optBoolFlex("enabled"),
                        doneToday = g.optBoolFlex("doneToday"),
                        result = g.optStringClean("result"),
                        lastAt = g.optLongFlex("lastAt"),
                        completedCount = g.optIntFlex("completedCount"),
                        totalCount = g.optIntFlex("totalCount"),
                        mode = g.optStringClean("mode"),
                        checkStatus = g.optStringClean("checkStatus"),
                        isVip = g.optBoolNullable("isVip"),
                        hasGift = g.optBoolNullable("hasGift"),
                        hasCard = g.optBoolNullable("hasCard"),
                        scheduledHour = if (g.has("scheduledHour") && !g.isNull("scheduledHour")) {
                            g.optIntFlex("scheduledHour")
                        } else null,
                    )
                )
            }
        }
        return DailyGiftsSummary(
            date = obj.optStringClean("date"),
            timeZone = obj.optStringClean("timeZone"),
            gifts = gifts,
        )
    }

    fun parseLandsPayload(obj: JSONObject): LandsPayload {
        val fetchedAt = com.qqfarm.mobile.platform.monotonicMillis()
        val landsArr = obj.optJSONArray("lands") ?: JSONArray()
        val lands = buildList(landsArr.length()) {
            for (i in 0 until landsArr.length()) {
                val item = landsArr.optJSONObject(i) ?: continue
                val mutantArr = item.optJSONArray("mutantEffects")
                val mutantNames = buildList {
                    if (mutantArr != null) {
                        for (m in 0 until mutantArr.length()) {
                            val mObj = mutantArr.optJSONObject(m) ?: continue
                            val name = mObj.optStringClean("name").ifBlank { mObj.optStringClean("effect_name") }
                            if (name.isNotBlank()) add(name)
                        }
                    }
                }
                val fertTimes = if (item.has("leftInorcFertTimes") && !item.isNull("leftInorcFertTimes")) {
                    item.optIntFlex("leftInorcFertTimes")
                } else null
                val status = item.optStringClean("status", "empty")
                val landInfo = item.optJSONObject("landInfo")?.let { parseLandUpgradeInfo(it) }

                add(
                    LandItem(
                        id = item.optIntFlex("id"),
                        level = item.optIntFlex("level", 1),
                        unlocked = item.optBoolFlex("unlocked", true),
                        status = if (status == "stealable" && item.optBoolNullable("stealable") == false) "unstealable" else status,
                        plantName = item.optStringClean("plantName"),
                        phaseName = item.optStringClean("phaseName"),
                        seedImage = item.optStringClean("seedImage"),
                        matureInSec = item.optLongFlex("matureInSec"),
                        totalGrowTime = item.optLongFlex("totalGrowTime"),
                        currentSeason = item.optIntFlex("currentSeason", 1),
                        totalSeason = item.optIntFlex("totalSeason", 1),
                        needWater = item.optBoolFlex("needWater"),
                        needWeed = item.optBoolFlex("needWeed"),
                        needBug = item.optBoolFlex("needBug"),
                        needInteractionCleanup = item.optBoolFlex("needInteractionCleanup"),
                        couldUnlock = item.optBoolFlex("couldUnlock"),
                        couldUpgrade = item.optBoolFlex("couldUpgrade"),
                        landInfo = landInfo,
                        occupiedByMaster = item.optBoolFlex("occupiedByMaster"),
                        masterLandId = item.optIntFlex("masterLandId"),
                        plantSize = item.optIntFlex("plantSize", 1),
                        leftInorcFertTimes = fertTimes,
                        mutantNames = mutantNames,
                        fetchedAtElapsedMs = fetchedAt,
                    )
                )
            }
        }
        val summaryObj = obj.optJSONObject("summary") ?: JSONObject()
        val summary = LandSummary(
            harvestable = summaryObj.optIntFlex("harvestable"),
            growing = summaryObj.optIntFlex("growing"),
            empty = summaryObj.optIntFlex("empty"),
            dead = summaryObj.optIntFlex("dead"),
            needWater = summaryObj.optIntFlex("needWater"),
            needWeed = summaryObj.optIntFlex("needWeed"),
            needBug = summaryObj.optIntFlex("needBug"),
        )
        val socialArr = obj.optJSONArray("socialEvents") ?: JSONArray()
        val cleanableSocialEventNames = buildList {
            for (i in 0 until socialArr.length()) {
                val ev = socialArr.optJSONObject(i) ?: continue
                if (!ev.optBoolFlex("cleanable")) continue
                val label = ev.optStringClean("itemName").ifBlank { ev.optStringClean("name") }
                if (label.isNotBlank() && label !in this) add(label)
            }
        }
        return LandsPayload(
            lands = lands,
            summary = summary,
            cleanableSocialEventNames = cleanableSocialEventNames,
        )
    }

    fun parseFriendOperation(response: JSONObject): FriendOperationResult {
        val data = response.optJSONObject("data") ?: throw ApiException("服务器未返回好友操作结果")
        val message = data.optStringClean("message")
        val reason = data.optStringClean("reason")
        if (!data.optBoolFlex("ok")) {
            if (reason == "invalid_removed") {
                return FriendOperationResult(
                    message = message.ifBlank { "好友关系已失效，已从扫描名单移除" },
                    count = 0,
                    reason = reason,
                )
            }
            throw ApiException(
                message = message.ifBlank { "服务器未确认好友操作结果" },
                errorCode = data.optStringClean("errorCode").ifBlank { data.optStringClean("code") },
            )
        }
        val count = data.optIntFlex("count", -1).takeIf { it >= 0 }
        val display = message.ifBlank { count?.let { "本次操作 $it 块土地" } ?: "服务器已确认操作" }
        return FriendOperationResult(display, count, reason = reason)
    }

    fun parseFriendLandsPayload(obj: JSONObject): FriendLandsPayload {
        val actions = obj.optJSONObject("summary")
        fun ids(key: String) = FeatureJsonParsers.parseIntList(actions?.optJSONArray(key)).filter { it > 0 }.toSet()
        val stealable = ids("stealable")
        val dry = ids("needWater")
        val weed = ids("needWeed")
        val insect = ids("needBug")
        val disrupt = ids("canPutWeed") + ids("canPutBug")
        val lands = parseLandsPayload(obj).lands.map { land ->
            val eligible = land.unlocked && !land.occupiedByMaster
            val status = when {
                land.status == "stealable" -> if (eligible && land.id in stealable) "stealable" else "unstealable"
                land.status == "harvested" || land.status == "harvestable" -> "unstealable"
                else -> land.status
            }
            land.copy(
                status = status,
                needWater = eligible && land.id in dry,
                needWeed = eligible && land.id in weed,
                needBug = eligible && land.id in insect,
            )
        }
        return FriendLandsPayload(
            lands = lands,
            dryCount = lands.count { it.needWater },
            weedCount = lands.count { it.needWeed },
            insectCount = lands.count { it.needBug },
            canDisrupt = lands.any { it.unlocked && !it.occupiedByMaster && it.id in disrupt },
        )
    }

    private fun parseLandLevelBonus(obj: JSONObject): LandLevelBonus = LandLevelBonus(
        level = obj.optIntFlex("level"),
        name = obj.optStringClean("name"),
        yieldBonus = obj.optDouble("yieldBonus", 0.0),
        growthReduction = obj.optDouble("growthReduction", 0.0),
        expBonus = obj.optDouble("expBonus", 0.0),
        mutantMultiplier = obj.optDouble("mutantMultiplier", 0.0),
        resonanceBonus = obj.optDouble("resonanceBonus", 0.0),
    )

    private fun parseLandUpgradeInfo(obj: JSONObject): LandUpgradeInfo {
        val pre = obj.optJSONObject("prerequisite")
        val prec = obj.optJSONObject("precedingLands")
        val costsArr = obj.optJSONArray("costs") ?: JSONArray()
        val costs = buildList(costsArr.length()) {
            for (i in 0 until costsArr.length()) {
                val c = costsArr.optJSONObject(i) ?: continue
                add(
                    LandUpgradeCost(
                        id = c.optStringClean("id"),
                        name = c.optStringClean("name"),
                        image = c.optStringClean("image"),
                        count = c.optStringClean("count", "0"),
                    )
                )
            }
        }
        return LandUpgradeInfo(
            mode = obj.optStringClean("mode"),
            current = obj.optJSONObject("current")?.let { parseLandLevelBonus(it) },
            next = obj.optJSONObject("next")?.let { parseLandLevelBonus(it) },
            serverAllowed = obj.optBoolFlex("serverAllowed"),
            needLevel = if (obj.has("needLevel") && !obj.isNull("needLevel")) obj.optIntFlex("needLevel") else null,
            prerequisiteLandId = pre?.optIntFlex("landId")?.takeIf { it > 0 },
            prerequisiteMet = pre?.optBoolFlex("met") ?: false,
            precedingName = prec?.optStringClean("name").orEmpty(),
            precedingRequired = prec?.optIntFlex("required") ?: 0,
            precedingOwned = if (prec != null && prec.has("owned") && !prec.isNull("owned")) prec.optIntFlex("owned") else null,
            costs = costs,
        )
    }

    fun parseBagPayload(obj: JSONObject): BagPayload {
        val itemsArr = obj.optJSONArray("items") ?: JSONArray()
        val items = buildList(itemsArr.length()) {
            for (i in 0 until itemsArr.length()) {
                val it = itemsArr.optJSONObject(i) ?: continue
                add(
                    BagItem(
                        key = it.optStringClean("key").ifBlank { "${it.optIntFlex("id")}:${it.optLongFlex("uid")}" },
                        id = it.optIntFlex("id"),
                        uid = it.optLongFlex("uid"),
                        name = it.optStringClean("name"),
                        image = it.optStringClean("image"),
                        count = it.optLongFlex("count"),
                        category = it.optStringClean("category", "item"),
                        itemType = it.optIntFlex("itemType"),
                        sellable = it.optBoolFlex("sellable"),
                        price = it.optLongFlex("price"),
                        priceUnit = it.optStringClean("priceUnit", "金"),
                        locked = it.optBoolFlex("locked"),
                        description = it.optStringClean("description"),
                        interactionType = it.optStringClean("interactionType"),
                        hoursText = it.optStringClean("hoursText"),
                    )
                )
            }
        }
        val sysArr = obj.optJSONArray("systemItems") ?: JSONArray()
        val systemItems = buildList(sysArr.length()) {
            for (i in 0 until sysArr.length()) {
                val it = sysArr.optJSONObject(i) ?: continue
                add(
                    BagSystemItem(
                        id = it.optIntFlex("id"),
                        count = it.optLongFlex("count"),
                        name = it.optStringClean("name"),
                        interactionType = it.optStringClean("interactionType"),
                        hoursText = it.optStringClean("hoursText"),
                    )
                )
            }
        }
        return BagPayload(items = items, systemItems = systemItems)
    }

    internal fun parseRanchCareer(obj: JSONObject?): RanchCareer? = obj?.let {
        RanchCareer(it.optLongFlex("harvest"), it.optLongFlex("mature"), it.optLongFlex("steal"))
    }

    fun parseRanchPayload(obj: JSONObject): RanchPayload {
        val unlocked = obj.optBoolFlex("unlocked", true)
        val serverNow = normalizeMs(obj.optLongFlex("serverTime", wallClockMillis()))
        val fetchedAt = com.qqfarm.mobile.platform.monotonicMillis()

        val bucketsArr = obj.optJSONArray("buckets") ?: JSONArray()
        val buckets = buildList(bucketsArr.length()) {
            for (i in 0 until bucketsArr.length()) {
                val b = bucketsArr.optJSONObject(i) ?: continue
                val id = b.optIntFlex("id")
                val name = if (id == 7004) "有机罐头" else "普通罐头"
                add(RanchBucket(id = id, name = name, seconds = b.optLongFlex("seconds")))
            }
        }

        val currArr = obj.optJSONArray("currencies") ?: JSONArray()
        val currencies = buildList(currArr.length()) {
            for (i in 0 until currArr.length()) {
                val c = currArr.optJSONObject(i) ?: continue
                add(
                    RanchCurrency(
                        id = c.optIntFlex("id"),
                        name = c.optStringClean("name"),
                        image = c.optStringClean("image"),
                        count = c.optLongFlex("count"),
                    )
                )
            }
        }

        val facilitiesArr = obj.optJSONArray("facilities") ?: JSONArray()
        val facilitiesList = mutableListOf<RanchFacility>()
        val grouped = linkedMapOf<Int, MutableList<RanchAnimal>>()
        val groupNames = linkedMapOf<Int, String>()
        val groupCounts = linkedMapOf<Int, Int>()
        val groupCaps = linkedMapOf<Int, Int>()
        var hazardsTotal = 0
        var collectableShelves = 0

        for (i in 0 until facilitiesArr.length()) {
            val f = facilitiesArr.optJSONObject(i) ?: continue
            val moduleType = f.optIntFlex("moduleType")
            val ranch = f.optJSONObject("ranch")
            val feed = f.optJSONObject("feed")
            val nextObj = f.optJSONObject("next")

            val nextModel = if (nextObj != null) {
                RanchFacilityNext(
                    level = nextObj.optIntFlex("level"),
                    capacity = nextObj.optIntFlex("capacity"),
                    durationSec = nextObj.optLongFlex("durationSec"),
                    requiredLevel = nextObj.optIntFlex("requiredLevel"),
                    available = nextObj.optBoolFlex("available"),
                    costs = FeatureJsonParsers.parseCommerceItems(nextObj.optJSONArray("costs")),
                )
            } else null

            facilitiesList.add(
                RanchFacility(
                    id = f.optStringClean("id"),
                    moduleType = moduleType,
                    name = f.optStringClean("name", "设施"),
                    level = f.optIntFlex("level"),
                    state = f.optIntFlex("state"),
                    active = f.optBoolFlex("active"),
                    canActivate = f.optBoolFlex("canActivate"),
                    capacity = f.optIntFlex("capacity"),
                    upgradeState = f.optIntFlex("upgradeState"),
                    upgradeEndTimeMs = normalizeMs(f.optLongFlex("upgradeEndTime")),
                    ranchCount = ranch?.optIntFlex("count") ?: 0,
                    ranchCapacity = ranch?.optIntFlex("capacity") ?: 0,
                    hasRanch = ranch != null,
                    feedCount = feed?.optIntFlex("count") ?: 0,
                    feedRemainingSec = feed?.optLongFlex("remainingSec", -1L) ?: -1L,
                    hasFeed = feed != null,
                    next = nextModel,
                )
            )

            if (ranch == null) continue
            if (!groupNames.containsKey(moduleType)) groupNames[moduleType] = f.optStringClean("name", "畜舍")
            groupCounts[moduleType] = (groupCounts[moduleType] ?: 0) + ranch.optIntFlex("count")
            groupCaps[moduleType] = (groupCaps[moduleType] ?: 0) + ranch.optIntFlex("capacity")

            val hazardsArr = ranch.optJSONArray("hazards")
            hazardsTotal += hazardsArr?.length() ?: 0

            val shelvesArr = ranch.optJSONArray("shelves")
            if (shelvesArr != null) {
                for (s in 0 until shelvesArr.length()) {
                    val shelf = shelvesArr.optJSONObject(s) ?: continue
                    if (shelf.optBoolFlex("canCollect")) collectableShelves++
                }
            }

            val animalsArr = ranch.optJSONArray("animals") ?: JSONArray()
            val targetList = grouped.getOrPut(moduleType) { mutableListOf() }
            for (a in 0 until animalsArr.length()) {
                val animal = animalsArr.optJSONObject(a) ?: continue
                targetList.add(parseRanchAnimal(animal))
            }
        }

        val sortedFacilities = facilitiesList.sortedWith(
            compareBy<RanchFacility> {
                when (it.moduleType) {
                    6 -> 0
                    7 -> 1
                    8 -> 2
                    else -> it.moduleType
                }
            }.thenBy { it.id.toLongOrNull() ?: 0L }.thenBy { it.id }
        )

        val pens = grouped.entries
            .sortedBy { (type, _) ->
                when (type) {
                    6 -> 0
                    7 -> 1
                    else -> type
                }
            }
            .map { (type, list) ->
                RanchPenGroup(
                    moduleType = type,
                    name = groupNames[type] ?: "畜舍",
                    count = groupCounts[type] ?: list.size,
                    capacity = groupCaps[type] ?: list.size,
                    animals = list.sortedWith(compareBy<RanchAnimal> { it.slot }.thenBy { it.id }),
                )
            }

        return RanchPayload(
            unlocked = unlocked,
            level = obj.optIntFlex("level"),
            grass = obj.optLongFlex("grass"),
            hourglassSeconds = if (obj.isNull("hourglassSeconds")) null else obj.optLongFlex("hourglassSeconds"),
            career = parseRanchCareer(obj.optJSONObject("career")),
            buckets = buckets,
            pens = pens,
            hazardsCount = hazardsTotal,
            shelvesCollectableCount = collectableShelves,
            serverNowMs = serverNow,
            fetchedAtElapsedMs = fetchedAt,
            currentExp = obj.optLongFlex("currentExp"),
            neededExp = obj.optLongFlex("neededExp"),
            currencies = currencies,
            facilities = sortedFacilities,
            notice = obj.optStringClean("notice"),
            reason = obj.optStringClean("reason"),
            dailyShareClaimed = obj.optJSONObject("dailyShare")?.optBoolFlex("claimed") ?: false,
            dailyBenefitClaimed = obj.optJSONObject("dailyBenefit")?.optBoolFlex("claimed") ?: false,
        )
    }

    internal fun parseRanchAnimal(animal: JSONObject): RanchAnimal {
        val mutation = animal.optJSONObject("mutation")
        val isGiant = mutation != null && mutation.optIntFlex("type") > 0
        val weightX10 = mutation?.optIntFlex("weightX10") ?: 0
        val weightKg = if (weightX10 > 0) {
            val v = weightX10 / 100.0
            com.qqfarm.mobile.platform.fmtDec(v, 2).trimEnd('0').trimEnd('.') + "kg"
        } else ""
        return RanchAnimal(
            id = animal.optLongFlex("id"),
            ranchId = animal.optStringClean("ranchId"),
            slot = animal.optIntFlex("slot"),
            name = animal.optStringClean("name"),
            image = animal.optStringClean("image"),
            stageName = animal.optStringClean("stageName"),
            stage = animal.optIntFlex("stage"),
            canHarvest = animal.optBoolFlex("canHarvest"),
            canProduce = animal.optBoolFlex("canProduce"),
            canFeed = animal.optBoolFlex("canFeed"),
            limitedCanUsed = animal.optBoolFlex("limitedCanUsed"),
            canRelease = animal.optBoolFlex("canRelease"),
            grassBlocked = animal.optBoolFlex("grassBlocked"),
            produceCount = animal.optIntFlex("produceCount"),
            maxProduceCount = animal.optIntFlex("maxProduceCount"),
            isGiant = isGiant,
            weightKg = weightKg,
            nextAtMs = normalizeMs(animal.optLongFlex("nextAtMs")),
            deliveringUntilMs = normalizeMs(animal.optLongFlex("deliveringUntilMs")),
            canStroke = animal.optBoolFlex("canStroke"),
            stroked = animal.optBoolFlex("stroked"),
        )
    }

    private fun normalizeMs(raw: Long): Long =
        if (raw in 1L..99_999_999_999L) raw * 1000L else raw

    fun parsePetSnapshot(obj: JSONObject): PetSnapshot {
        val dogsArr = obj.optJSONArray("dogs") ?: JSONArray()
        val dogs = buildList(dogsArr.length()) {
            for (i in 0 until dogsArr.length()) {
                val d = dogsArr.optJSONObject(i) ?: continue
                add(
                    PetDog(
                        id = d.optIntFlex("id"),
                        name = d.optStringClean("name"),
                        image = d.optStringClean("image"),
                        rarityLabel = d.optStringClean("rarityLabel"),
                        rarity = d.optIntFlex("rarity"),
                        level = d.optIntFlex("level"),
                        owned = d.optBoolFlex("owned"),
                        active = d.optBoolFlex("active"),
                        skillDescription = d.optStringClean("skillDescription"),
                    )
                )
            }
        }
        val foodsArr = obj.optJSONArray("foods") ?: JSONArray()
        val foods = buildList(foodsArr.length()) {
            for (i in 0 until foodsArr.length()) {
                val f = foodsArr.optJSONObject(i) ?: continue
                add(
                    DogFood(
                        id = f.optIntFlex("id"),
                        name = f.optStringClean("name"),
                        image = f.optStringClean("image"),
                        durationSec = f.optLongFlex("duration"),
                        count = f.optLongFlex("count"),
                    )
                )
            }
        }
        return PetSnapshot(
            dogs = dogs,
            foods = foods,
            protectDurationSec = obj.optLongFlex("protectDuration"),
            maxProtectDurationSec = obj.optLongFlex("maxProtectDuration"),
            remainingDurationSec = obj.optLongFlex("remainingDuration"),
            pendingGiftCount = obj.optIntFlex("pendingGiftCount"),
            activeDogId = obj.optIntFlex("activeDogId"),
        )
    }

    fun parseFriendsPayload(rootObj: JSONObject): FriendsPayload {
        val arr = rootObj.optJSONArray("data") ?: JSONArray()
        val friends = buildList(arr.length()) {
            for (i in 0 until arr.length()) {
                val f = arr.optJSONObject(i) ?: continue
                val plant = f.optJSONObject("plant")
                add(
                    FriendItem(
                        gid = f.optLongFlex("gid"),
                        name = f.optStringClean("name").ifBlank { f.optStringClean("nick") },
                        avatarUrl = f.optStringClean("avatarUrl"),
                        level = f.optIntFlex("level"),
                        stealNum = plant?.optIntFlex("stealNum") ?: 0,
                        dryNum = plant?.optIntFlex("dryNum") ?: 0,
                        weedNum = plant?.optIntFlex("weedNum") ?: 0,
                        insectNum = plant?.optIntFlex("insectNum") ?: 0,
                    )
                )
            }
        }
        val status = rootObj.optJSONObject("status")
        return FriendsPayload(
            friends = friends,
            state = status?.optStringClean("state", "ready") ?: "ready",
            error = status?.optStringClean("error").orEmpty(),
        )
    }

    fun parseBlacklist(arr: JSONArray?): List<FriendBlacklistItem> {
        if (arr == null) return emptyList()
        return buildList(arr.length()) {
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                add(
                    FriendBlacklistItem(
                        gid = obj.optLongFlex("gid"),
                        name = obj.optStringClean("name").ifBlank { "GID:${obj.optLongFlex("gid")}" },
                        avatarUrl = obj.optStringClean("avatarUrl"),
                    )
                )
            }
        }
    }

    fun parseInteractRecords(arr: JSONArray?): List<InteractRecord> {
        if (arr == null) return emptyList()
        return buildList(arr.length()) {
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val ms = obj.optLongFlex("serverTimeMs")
                val timeText = if (ms > 0L) formatEpoch("MM-dd HH:mm", ms) else ""
                add(
                    InteractRecord(
                        id = obj.optStringClean("key").ifBlank { "$i:$ms" },
                        friendName = obj.optStringClean("nick").ifBlank { "好友" },
                        friendAvatar = obj.optStringClean("avatarUrl"),
                        actionText = obj.optStringClean("actionDetail").ifBlank { obj.optStringClean("actionLabel", "互动") },
                        timeText = timeText,
                        serverTimeMs = ms,
                    )
                )
            }
        }
    }

    fun parsePluginList(obj: JSONObject): PluginListPayload {
        val runtime = obj.optJSONObject("runtime")
        val arr = obj.optJSONArray("plugins") ?: JSONArray()
        val list = buildList(arr.length()) {
            for (i in 0 until arr.length()) {
                val plugin = arr.optJSONObject(i) ?: continue
                add(parsePluginSummary(plugin))
            }
        }
        return PluginListPayload(
            hostApiVersion = obj.optStringClean("hostApiVersion"),
            runtimeAvailable = runtime?.optBoolFlex("available", true) ?: true,
            runtimeReason = runtime?.optStringClean("reason").orEmpty(),
            plugins = list,
        )
    }

    fun parsePluginSummary(plugin: JSONObject): PluginSummary {
        val execution = plugin.optJSONObject("executionStatus")
        val management = plugin.optJSONObject("management")
        return PluginSummary(
            id = plugin.optStringClean("id"),
            name = plugin.optStringClean("name"),
            version = plugin.optStringClean("version"),
            description = plugin.optStringClean("description"),
            author = plugin.optStringClean("author"),
            origin = plugin.optStringClean("origin", "extension"),
            badge = plugin.optStringClean("badge"),
            enabled = plugin.optBoolFlex("enabled"),
            accountsBound = plugin.optIntFlex("accountsBound"),
            accountsEnabled = plugin.optIntFlex("accountsEnabled"),
            pausedAccounts = plugin.optIntFlex("pausedAccounts"),
            pendingConfirmations = plugin.optIntFlex("pendingConfirmations"),
            executionState = execution?.optStringClean("state").orEmpty(),
            executionLabel = execution?.optStringClean("label").orEmpty(),
            executionMessage = execution?.optStringClean("message").orEmpty(),
            lastFinishedAtMs = plugin.optLongFlex("lastFinishedAt"),
            nextRunAtMs = plugin.optLongFlex("nextRunAt"),
            canDisable = management?.optBoolFlex("disable", true) ?: true,
            canPause = management?.optBoolFlex("pause", false) ?: false,
        )
    }

    fun parseRanchAnimalOptions(arr: JSONArray?): List<RanchAnimalOption> {
        if (arr == null) return emptyList()
        return buildList(arr.length()) {
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                val value = o.optIntFlex("value")
                val label = o.optStringClean("label")
                val kind = o.optStringClean("kind")
                if (value > 0 && label.isNotBlank()) {
                    add(RanchAnimalOption(value = value, label = label, kind = kind))
                }
            }
        }
    }

    fun parseSeedCatalog(arr: JSONArray?): List<SeedOption> {
        if (arr == null) return emptyList()
        return buildList(arr.length()) {
            for (i in 0 until arr.length()) {
                val s = arr.optJSONObject(i) ?: continue
                val seedId = s.optLongFlex("seedId", s.optLongFlex("id"))
                val name = s.optStringClean("name")
                if (seedId > 0L && name.isNotBlank()) {
                    add(
                        SeedOption(
                            seedId = seedId,
                            name = name,
                            requiredLevel = s.optIntFlex("requiredLevel", s.optIntFlex("level")),
                            requiresUnlockCard = s.optBoolFlex("requiresUnlockCard"),
                        )
                    )
                }
            }
        }
    }

    private fun parseStringMap(obj: JSONObject?): Map<String, String> {
        if (obj == null) return emptyMap()
        return buildMap {
            for (key in obj.keys()) {
                val v = obj.optStringClean(key)
                if (v.isNotBlank()) put(key, v)
            }
        }
    }

    fun parsePluginDefinition(obj: JSONObject): PluginDefinition = PluginDefinition(
        id = obj.optStringClean("id"),
        name = obj.optStringClean("name"),
        version = obj.optStringClean("version"),
        description = obj.optStringClean("description"),
        author = obj.optStringClean("author"),
        origin = obj.optStringClean("origin", "extension"),
        badge = obj.optStringClean("badge"),
        animalOptions = parseRanchAnimalOptions(obj.optJSONArray("animalOptions")),
        settingsSchemaJson = obj.optJSONObject("settingsSchema")?.toString() ?: "{}",
        uiSchemaJson = obj.optJSONObject("uiSchema")?.toString() ?: "{}",
        resourceNames = parseStringMap(obj.optJSONObject("resourceNames")),
        operationNames = parseStringMap(obj.optJSONObject("operationNames")),
    )

    private fun parseOpCosts(arr: JSONArray?): List<PluginOpCost> {
        if (arr == null) return emptyList()
        return buildList(arr.length()) {
            for (i in 0 until arr.length()) {
                val c = arr.optJSONObject(i) ?: continue
                val key = c.optStringClean("resourceKey")
                val amount = c.optStringClean("amount", c.optLongFlex("amount").toString())
                if (key.isNotBlank()) {
                    add(PluginOpCost(resourceKey = key, amount = amount, name = c.optStringClean("name")))
                }
            }
        }
    }

    private fun parseQuietWindows(arr: JSONArray?): List<PluginQuietWindow> {
        if (arr == null) return emptyList()
        return buildList(arr.length()) {
            for (i in 0 until arr.length()) {
                val w = arr.optJSONObject(i) ?: continue
                val start = w.optStringClean("start", "00:00")
                val end = w.optStringClean("end", "08:00")
                add(PluginQuietWindow(start = start, end = end))
            }
        }
    }

    private fun parseSpendingPolicy(obj: JSONObject?): PluginSpendingPolicy {
        if (obj == null) return PluginSpendingPolicy()
        val rulesArr = obj.optJSONArray("rules") ?: JSONArray()
        val rules = buildList(rulesArr.length()) {
            for (i in 0 until rulesArr.length()) {
                val r = rulesArr.optJSONObject(i) ?: continue
                val key = r.optStringClean("resourceKey")
                add(
                    PluginSpendingRule(
                        resourceKey = key,
                        perOperationMax = r.optStringClean("perOperationMax", "-1"),
                        dailyMax = r.optStringClean("dailyMax", "-1"),
                        minReserve = r.optStringClean("minReserve", "0"),
                    )
                )
            }
        }
        return PluginSpendingPolicy(autoSpend = obj.optBoolFlex("autoSpend"), rules = rules)
    }

    fun parseBuiltinPluginPaused(obj: JSONObject?): Boolean =
        obj?.optJSONObject("status")?.opt("paused") as? Boolean
            ?: throw ApiException("服务器未返回当前账号的模块状态")

    fun parsePluginAccountDetail(obj: JSONObject, isBuiltin: Boolean): PluginAccountDetail {
        val statusObj = obj.optJSONObject("status")
        val paused = isBuiltin && parseBuiltinPluginPaused(obj)
        val enabledFeaturesArr = statusObj?.optJSONArray("enabledFeatures") ?: JSONArray()
        val enabledFeatures = buildList(enabledFeaturesArr.length()) {
            for (i in 0 until enabledFeaturesArr.length()) {
                val f = enabledFeaturesArr.optString(i).trim()
                if (f.isNotEmpty()) add(f)
            }
        }

        val jobsArr = obj.optJSONArray("jobs") ?: JSONArray()
        val jobs = buildList(jobsArr.length()) {
            for (i in 0 until jobsArr.length()) {
                val j = jobsArr.optJSONObject(i) ?: continue
                val trigger = j.optJSONObject("trigger")
                val exec = j.optJSONObject("executionStatus")
                val stateObj = j.optJSONObject("state")
                val errObj = exec?.optJSONObject("error")
                add(
                    PluginJob(
                        id = j.optStringClean("id"),
                        title = j.optStringClean("title"),
                        triggerText = describeTrigger(trigger),
                        stateLabel = exec?.optStringClean("label").orEmpty(),
                        stateKey = exec?.optStringClean("state").orEmpty(),
                        message = exec?.optStringClean("message").orEmpty(),
                        nextRunAtMs = j.optLongFlex("nextRunAt"),
                        lastRunAtMs = stateObj?.optLongFlex("lastRunAt") ?: 0L,
                        errorCode = errObj?.optStringClean("code").orEmpty(),
                        errorMessage = errObj?.optStringClean("message").orEmpty(),
                        blockedReason = stateObj?.optStringClean("blockedReason").orEmpty(),
                    )
                )
            }
        }

        val ops = mutableListOf<PluginPendingOp>()
        val nativeArr = obj.optJSONArray("nativeOperations")
        if (nativeArr != null) {
            for (i in 0 until nativeArr.length()) {
                val op = nativeArr.optJSONObject(i) ?: continue
                ops.add(
                    PluginPendingOp(
                        operationId = op.optStringClean("operationId"),
                        kind = op.optStringClean("label", "待核对操作"),
                        status = "unknown",
                        message = op.optStringClean("message"),
                        createdAtMs = op.optLongFlex("createdAt"),
                        isNative = true,
                        costs = parseOpCosts(op.optJSONArray("costs")),
                    )
                )
            }
        }
        val extArr = obj.optJSONArray("operations")
        if (extArr != null) {
            for (i in 0 until extArr.length()) {
                val op = extArr.optJSONObject(i) ?: continue
                ops.add(
                    PluginPendingOp(
                        operationId = op.optStringClean("operationId"),
                        kind = op.optStringClean("kind", "操作记录"),
                        status = op.optStringClean("status", "confirmed"),
                        message = op.optStringClean("message"),
                        createdAtMs = op.optLongFlex("createdAtMs"),
                        isNative = false,
                        costs = parseOpCosts(op.optJSONArray("costs")),
                    )
                )
            }
        }

        val bindingObj = obj.optJSONObject("binding")
        val decisionObj = obj.optJSONObject("decision")
        val execStatusObj = obj.optJSONObject("executionStatus")
        val conflictsArr = obj.optJSONArray("conflicts") ?: JSONArray()
        val conflicts = buildList(conflictsArr.length()) {
            for (i in 0 until conflictsArr.length()) {
                val text = conflictsArr.optString(i).trim()
                if (text.isNotEmpty()) add(text)
            }
        }
        val usageArr = obj.optJSONObject("spendingSummary")?.optJSONArray("usage") ?: JSONArray()
        val usageList = buildList(usageArr.length()) {
            for (i in 0 until usageArr.length()) {
                val u = usageArr.optJSONObject(i) ?: continue
                val key = u.optStringClean("resourceKey")
                if (key.isNotBlank()) {
                    add(PluginSpendingUsage(resourceKey = key, usedToday = u.optStringClean("usedToday", "0")))
                }
            }
        }

        return PluginAccountDetail(
            isBuiltin = isBuiltin,
            accountId = obj.optStringClean("accountId"),
            accountName = obj.optStringClean("accountName"),
            timeZone = obj.optStringClean("timeZone"),
            paused = paused,
            running = statusObj?.optBoolFlex("running") ?: false,
            connected = statusObj?.optBoolFlex("connected") ?: false,
            enabledFeatures = enabledFeatures,
            settingsRevision = obj.optStringClean("settingsRevision"),
            builtinSettings = if (isBuiltin) {
                parseAccountSettings(obj.optJSONObject("settings") ?: JSONObject())
            } else {
                AccountSettingsModel()
            },
            nativeOperationsError = obj.optStringClean("nativeOperationsError"),
            bindingExists = obj.optBoolFlex("exists"),
            bindingEnabled = bindingObj?.optBoolFlex("enabled") ?: false,
            bindingRevision = bindingObj?.optIntFlex("revision") ?: 0,
            bindingSettingsJson = bindingObj?.optJSONObject("settings")?.toString() ?: "{}",
            quietWindows = parseQuietWindows(bindingObj?.optJSONArray("quietWindows")),
            accountQuietWindows = parseQuietWindows(obj.optJSONArray("accountQuietWindows")),
            spendingPolicy = parseSpendingPolicy(bindingObj?.optJSONObject("spending")),
            spendingUsage = usageList,
            conflicts = conflicts,
            decisionAllowed = decisionObj?.optBoolFlex("allowed", true) ?: true,
            decisionReason = decisionObj?.optStringClean("reason").orEmpty(),
            nextAllowedAtMs = decisionObj?.optLongFlex("nextAllowedAtMs") ?: 0L,
            executionState = execStatusObj?.optStringClean("state").orEmpty(),
            executionLabel = execStatusObj?.optStringClean("label").orEmpty(),
            executionMessage = execStatusObj?.optStringClean("message").orEmpty(),
            jobs = jobs,
            operations = ops,
        )
    }

    private fun describeTrigger(trigger: JSONObject?): String {
        if (trigger == null) return "手动"
        return when (trigger.optStringClean("type")) {
            "interval" -> {
                val mins = (trigger.optLongFlex("everyMs") / 60000L).coerceAtLeast(1L)
                if (mins >= 60L && mins % 60L == 0L) "每 ${mins / 60L} 小时" else "每 $mins 分钟"
            }
            "daily" -> "每天 ${trigger.optStringClean("at")}"
            "event" -> "事件 ${trigger.optStringClean("event")}"
            else -> "手动"
        }
    }

    fun parsePluginLogs(arr: JSONArray?): List<PluginLogEntry> {
        if (arr == null) return emptyList()
        return buildList(arr.length()) {
            for (i in 0 until arr.length()) {
                val l = arr.optJSONObject(i) ?: continue
                add(
                    PluginLogEntry(
                        ts = l.optLongFlex("ts"),
                        level = l.optStringClean("level", "info"),
                        accountName = l.optStringClean("accountName"),
                        jobId = l.optStringClean("jobId"),
                        message = l.optStringClean("message"),
                    )
                )
            }
        }
    }

    fun parseAccountSettings(obj: JSONObject): AccountSettingsModel {
        val intervals = obj.optJSONObject("intervals") ?: JSONObject()
        val quiet = obj.optJSONObject("friendQuietHours") ?: JSONObject()
        val automation = obj.optJSONObject("automation") ?: JSONObject()
        val boolMap = buildMap {
            for (key in automation.keys()) {
                val value = automation.opt(key)
                if (value is Boolean) {
                    put(key, value)
                }
            }
        }
        val farmMin = intervals.optIntFlex("farmMin", intervals.optIntFlex("farm", 20)).coerceAtLeast(1)
        val farmMax = intervals.optIntFlex("farmMax", farmMin).coerceAtLeast(farmMin)
        val friendMin = intervals.optIntFlex("friendMin", intervals.optIntFlex("friend", 20)).coerceAtLeast(1)
        val friendMax = intervals.optIntFlex("friendMax", friendMin).coerceAtLeast(friendMin)

        val defaultLandTypes = listOf("blue-crystal", "purple-gold", "gold", "black", "red", "normal")
        val landTypesArr = automation.optJSONArray("fertilizer_land_types")
        val landTypes = if (landTypesArr != null) {
            buildList(landTypesArr.length()) {
                for (i in 0 until landTypesArr.length()) {
                    val v = landTypesArr.optString(i).trim().lowercase()
                    if (v.isNotEmpty() && v !in this) add(v)
                }
            }
        } else {
            defaultLandTypes
        }

        return AccountSettingsModel(
            plantingStrategy = obj.optStringClean("strategy", obj.optStringClean("plantingStrategy", "max_exp")),
            preferredSeedId = obj.optLongFlex("preferredSeed", obj.optLongFlex("preferredSeedId")),
            bagSeedFallbackStrategy = obj.optStringClean("bagSeedFallbackStrategy", "level"),
            bagSeedMultiLandReservationEnabled = obj.optBoolFlex("bagSeedMultiLandReservationEnabled", false),
            farmMinSec = farmMin,
            farmMaxSec = farmMax,
            friendMinSec = friendMin,
            friendMaxSec = friendMax,
            quietHoursEnabled = quiet.optBoolFlex("enabled", false),
            quietHoursStart = quiet.optStringClean("start", "23:00"),
            quietHoursEnd = quiet.optStringClean("end", "07:00"),
            quietContinueFarm = quiet.optBoolFlex("continueFarm", true),
            stealDelaySeconds = obj.optIntFlex("stealDelaySeconds", 0).coerceIn(0, 300),
            plantOrderRandom = obj.optBoolFlex("plantOrderRandom", false),
            plantDelaySeconds = obj.optIntFlex("plantDelaySeconds", 0).coerceIn(0, 60),
            fertilizerMode = automation.optStringClean("fertilizer", "none"),
            fertilizerSmartSeconds = automation.optIntFlex("fertilizer_smart_seconds", 300).coerceIn(30, 3600),
            fertilizerLandTypes = landTypes,
            fertilizerBuyOrganicCount = obj.optIntFlex("fertilizerBuyOrganicCount", 0).coerceIn(0, 10000),
            fertilizerBuyOrganicThresholdHours = obj.optIntFlex("fertilizerBuyOrganicThresholdHours", 10).coerceIn(0, 990),
            fertilizerBuyNormalCount = obj.optIntFlex("fertilizerBuyNormalCount", 0).coerceIn(0, 10000),
            fertilizerBuyNormalThresholdHours = obj.optIntFlex("fertilizerBuyNormalThresholdHours", 10).coerceIn(0, 990),
            fertilizerBuyCheckIntervalMinutes = obj.optIntFlex("fertilizerBuyCheckIntervalMinutes", 30).coerceIn(1, 1440),
            emailClaimHour = obj.optIntFlex("emailClaimHour", 10).coerceIn(0, 23),
            autoAcceptFriendMinLevel = obj.optIntFlex("autoAcceptFriendMinLevel", 0).coerceIn(0, 200),
            autoAcceptRequireOwnLevel = obj.optBoolFlex("autoAcceptRequireOwnLevel", false),
            autoAcceptHarvestStealEnabled = obj.optBoolFlex("autoAcceptHarvestStealEnabled", true),
            autoAcceptHarvestStealHarvest = obj.optIntFlex("autoAcceptHarvestStealHarvest", 8).coerceIn(0, 9999),
            autoAcceptHarvestStealSteal = obj.optIntFlex("autoAcceptHarvestStealSteal", 1).coerceIn(1, 9999),
            ranchStrategy = automation.optStringClean("ranch_strategy", "level"),
            ranchBagFallback = automation.optStringClean("ranch_bag_fallback", "level"),
            bagSeedPriority = FeatureJsonParsers.parseIntList(obj.optJSONArray("bagSeedPriority")),
            ranchCoopCubPriority = FeatureJsonParsers.parseIntList(automation.optJSONArray("ranch_coop_cub_priority")),
            ranchShedCubPriority = FeatureJsonParsers.parseIntList(automation.optJSONArray("ranch_shed_cub_priority")),
            ranchCoopAnimal = automation.optIntFlex("ranch_coop_animal", 0),
            ranchShedAnimal = automation.optIntFlex("ranch_shed_animal", 0),
            ranchGrassThresholdPercent = automation.optIntFlex("ranch_grass_threshold_percent", 20).coerceIn(1, 100),
            ranchCheckMinutes = automation.optIntFlex("ranch_check_minutes", 5).coerceIn(1, 60),
            ranchFeedNormalStage = readRanchCanStage(automation.optStringClean("ranch_feed_normal_stage")),
            ranchFeedOrganicStage = readRanchCanStage(automation.optStringClean("ranch_feed_organic_stage")),
            automationBooleans = boolMap,
            rawIntervalsJson = intervals.toString(),
            rawAutomationJson = automation.toString(),
        )
    }

    private fun readRanchCanStage(raw: String): String? = when (raw) {
        "any", "longest", "to_production", "near_harvest" -> raw
        else -> null
    }

    fun parseLoginConfig(obj: JSONObject): LoginConfigModel = LoginConfigModel(
        wechatQrLogin = obj.optBoolFlex("wechatQrLogin", true),
        qqQrLogin = obj.optBoolFlex("qqQrLogin", false),
        phoneCaptureLogin = obj.optBoolFlex("phoneCaptureLogin", false),
        mobileAssistant = obj.optBoolFlex("mobileAssistant", false),
    )

    fun parseWxQrTask(obj: JSONObject): WxQrTask = WxQrTask(
        taskId = obj.optStringClean("task_id"),
        status = obj.optStringClean("status"),
        qrUrl = obj.optStringClean("qr_url"),
        expiresAt = obj.optLongFlex("expires_at"),
    )

    fun parseQqQrTask(obj: JSONObject): QqQrTask = QqQrTask(
        taskId = obj.optStringClean("task_id"),
        status = obj.optStringClean("status"),
        qrImage = obj.optStringClean("qr_image"),
        expiresAt = obj.optLongFlex("expires_at"),
    )

    fun parseOfflineEvents(obj: JSONObject): Triple<String, Boolean, List<MobileOfflineEvent>> {
        val cursor = obj.optStringClean("cursor")
        val reset = obj.optBoolFlex("reset")
        val arr = obj.optJSONArray("events") ?: JSONArray()
        val events = buildList(arr.length()) {
            for (i in 0 until arr.length()) {
                val e = arr.optJSONObject(i) ?: continue
                add(
                    MobileOfflineEvent(
                        eventId = e.optStringClean("eventId"),
                        accountId = e.optStringClean("accountId"),
                        accountName = e.optStringClean("accountName"),
                        platform = e.optStringClean("platform", "wx"),
                        createdAt = e.optLongFlex("createdAt"),
                    )
                )
            }
        }
        return Triple(cursor, reset, events)
    }
}
