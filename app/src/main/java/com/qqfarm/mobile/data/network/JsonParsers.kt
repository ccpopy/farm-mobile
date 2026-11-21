package com.qqfarm.mobile.data.network

import android.os.SystemClock
import com.qqfarm.mobile.data.model.AccountItem
import com.qqfarm.mobile.data.model.AccountLogEvent
import com.qqfarm.mobile.data.model.AccountSettingsModel
import com.qqfarm.mobile.data.model.BagItem
import com.qqfarm.mobile.data.model.BagPayload
import com.qqfarm.mobile.data.model.BagSystemItem
import com.qqfarm.mobile.data.model.DailyGiftItem
import com.qqfarm.mobile.data.model.DailyGiftsSummary
import com.qqfarm.mobile.data.model.DogFood
import com.qqfarm.mobile.data.model.FriendBlacklistItem
import com.qqfarm.mobile.data.model.FriendItem
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
import com.qqfarm.mobile.data.model.PetDog
import com.qqfarm.mobile.data.model.PetSnapshot
import com.qqfarm.mobile.data.model.QqQrTask
import com.qqfarm.mobile.data.model.RanchAnimal
import com.qqfarm.mobile.data.model.RanchAnimalOption
import com.qqfarm.mobile.data.model.RanchBucket
import com.qqfarm.mobile.data.model.RanchCurrency
import com.qqfarm.mobile.data.model.RanchFacility
import com.qqfarm.mobile.data.model.RanchFacilityNext
import com.qqfarm.mobile.data.model.RanchPayload
import com.qqfarm.mobile.data.model.RanchPenGroup
import com.qqfarm.mobile.data.model.RunLogEntry
import com.qqfarm.mobile.data.model.SchedulerSummary
import com.qqfarm.mobile.data.model.SeedOption
import com.qqfarm.mobile.data.model.ServerVersionInfo
import com.qqfarm.mobile.data.model.StatusSnapshot
import com.qqfarm.mobile.data.model.WxQrTask
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
        val serverTime = dataObj.optLongFlex("serverTime", serverTimeHintMs)
            .let { if (it > 0L) it else System.currentTimeMillis() }

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
            nextFarmRunAtMs = dataObj.optLongFlex(
                "nextFarmRunAt",
                nextChecks.optLongFlex("nextFarmRunAt"),
            ),
            nextFriendRunAtMs = dataObj.optLongFlex(
                "nextFriendRunAt",
                nextChecks.optLongFlex("nextFriendRunAt"),
            ),
            farmQuiet = nextChecks.optBoolFlex("farmQuiet"),
            friendQuiet = nextChecks.optBoolFlex("friendQuiet")
                || nextChecks.optBoolFlex("helpQuiet")
                || nextChecks.optBoolFlex("stealQuiet"),
            serverTimeMs = serverTime,
            receivedAtElapsedMs = SystemClock.elapsedRealtime(),
            operations = opsMap,
        )
    }

    private fun buildForecastText(forecastObj: JSONObject?): String {
        if (forecastObj == null) return ""
        val source = forecastObj.optStringClean("source")
        if (source == "ready") return "当前可升下一级"
        val estimatedAt = forecastObj.optLongFlex("estimatedAt")
        if (estimatedAt <= 0L || source == "unavailable") return ""
        val deltaSec = ((estimatedAt - System.currentTimeMillis()) / 1000L).coerceAtLeast(0L)
        return when {
            deltaSec < 60L -> "预计 1 分钟内升级"
            deltaSec < 3600L -> "预计 ${deltaSec / 60L} 分钟后升级"
            deltaSec < 86400L -> {
                val hours = deltaSec / 3600L
                val mins = (deltaSec % 3600L) / 60L
                if (mins > 0L) "预计 ${hours}小时${mins}分后升级" else "预计 ${hours}小时后升级"
            }
            else -> {
                val sdf = SimpleDateFormat("MM-dd HH:mm", Locale.CHINA)
                "预计 ${sdf.format(Date(estimatedAt))} 升级"
            }
        }
    }

    fun parseRunLogEntry(obj: JSONObject): RunLogEntry {
        val meta = obj.optJSONObject("meta")
        val ts = obj.optLongFlex("ts").let { if (it > 0L) it else System.currentTimeMillis() }
        val time = obj.optStringClean("time").ifBlank {
            SimpleDateFormat("HH:mm:ss", Locale.CHINA).format(Date(ts))
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
            module = meta?.optStringClean("module") ?: obj.optStringClean("module"),
            event = meta?.optStringClean("event") ?: obj.optStringClean("event"),
            result = meta?.optStringClean("result") ?: "",
            isWarn = obj.optBoolFlex("isWarn"),
            accountId = obj.optStringClean("accountId"),
            friendName = meta?.optStringClean("friendName").orEmpty(),
            actions = actionsList,
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
        val fetchedAt = SystemClock.elapsedRealtime()
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
                val landInfo = item.optJSONObject("landInfo")?.let { parseLandUpgradeInfo(it) }

                add(
                    LandItem(
                        id = item.optIntFlex("id"),
                        level = item.optIntFlex("level", 1),
                        unlocked = item.optBoolFlex("unlocked", true),
                        status = item.optStringClean("status", "empty"),
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

    fun parseRanchPayload(obj: JSONObject): RanchPayload {
        val unlocked = obj.optBoolFlex("unlocked", true)
        val serverNow = normalizeMs(obj.optLongFlex("serverTime", System.currentTimeMillis()))
        val fetchedAt = SystemClock.elapsedRealtime()

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
            groupNames.putIfAbsent(moduleType, f.optStringClean("name", "畜舍"))
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
            String.format(Locale.US, "%.2f", v).trimEnd('0').trimEnd('.') + "kg"
        } else ""
        return RanchAnimal(
            id = animal.optLongFlex("id"),
            ranchId = animal.optStringClean("ranchId"),
            slot = animal.optIntFlex("slot"),
            name = animal.optStringClean("name"),
            image = animal.optStringClean("image"),
            stageName = animal.optStringClean("stageName"),
            canHarvest = animal.optBoolFlex("canHarvest"),
            canProduce = animal.optBoolFlex("canProduce"),
            canFeed = animal.optBoolFlex("canFeed"),
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
        val sdf = SimpleDateFormat("MM-dd HH:mm", Locale.CHINA)
        return buildList(arr.length()) {
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val ms = obj.optLongFlex("serverTimeMs")
                val timeText = if (ms > 0L) sdf.format(Date(ms)) else ""
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

    fun parseSchedulerSummary(obj: JSONObject): SchedulerSummary {
        val workerError = obj.optStringClean("workerError")
        val worker = obj.optJSONObject("worker")
        val schedulersArr = worker?.optJSONArray("schedulers") ?: JSONArray()
        val taskNames = mutableListOf<String>()
        var taskCount = 0
        for (i in 0 until schedulersArr.length()) {
            val s = schedulersArr.optJSONObject(i) ?: continue
            val tasks = s.optJSONArray("tasks") ?: continue
            taskCount += tasks.length()
            for (j in 0 until tasks.length()) {
                val t = tasks.optJSONObject(j) ?: continue
                if (t.optBoolFlex("running")) {
                    val name = t.optStringClean("name")
                    if (name.isNotBlank()) taskNames.add(name)
                }
            }
        }
        return SchedulerSummary(
            workerRunning = worker != null,
            workerTaskCount = taskCount,
            runningTasks = taskNames,
            workerError = workerError,
        )
    }

    fun parseRanchAutomationOptions(arr: JSONArray?): List<RanchAnimalOption> {
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

        val defaultLandTypes = listOf("purple-gold", "gold", "black", "red", "normal")
        val landTypesArr = automation.optJSONArray("fertilizer_land_types")
        val landTypes = if (landTypesArr != null) {
            buildList(landTypesArr.length()) {
                for (i in 0 until landTypesArr.length()) {
                    val v = landTypesArr.optString(i).trim()
                    if (v.isNotEmpty() && v !in this) add(v)
                }
            }
        } else {
            defaultLandTypes
        }

        return AccountSettingsModel(
            plantingStrategy = obj.optStringClean("strategy", "max_exp"),
            preferredSeedId = obj.optLongFlex("preferredSeed"),
            farmMinSec = farmMin,
            farmMaxSec = farmMax,
            friendMinSec = friendMin,
            friendMaxSec = friendMax,
            quietHoursEnabled = quiet.optBoolFlex("enabled"),
            quietHoursStart = quiet.optStringClean("start", "01:00"),
            quietHoursEnd = quiet.optStringClean("end", "07:30"),
            quietContinueFarm = quiet.optBoolFlex("continueFarm", true),
            stealDelaySeconds = obj.optIntFlex("stealDelaySeconds", 1).coerceIn(0, 300),
            plantOrderRandom = obj.optBoolFlex("plantOrderRandom", true),
            plantDelaySeconds = obj.optIntFlex("plantDelaySeconds", 2).coerceIn(0, 60),
            fertilizerMode = automation.optStringClean("fertilizer", "smart"),
            fertilizerSmartSeconds = automation.optIntFlex("fertilizer_smart_seconds", 300).coerceIn(30, 3600),
            fertilizerLandTypes = landTypes,
            fertilizerBuyOrganicCount = obj.optIntFlex("fertilizerBuyOrganicCount", 1).coerceIn(0, 10000),
            fertilizerBuyOrganicThresholdHours = obj.optIntFlex("fertilizerBuyOrganicThresholdHours", 10).coerceIn(0, 990),
            fertilizerBuyNormalCount = obj.optIntFlex("fertilizerBuyNormalCount", 1).coerceIn(0, 10000),
            fertilizerBuyNormalThresholdHours = obj.optIntFlex("fertilizerBuyNormalThresholdHours", 10).coerceIn(0, 990),
            fertilizerBuyCheckIntervalMinutes = obj.optIntFlex("fertilizerBuyCheckIntervalMinutes", 60).coerceIn(1, 1440),
            emailClaimHour = if (obj.has("emailClaimHour") && !obj.isNull("emailClaimHour")) {
                obj.optIntFlex("emailClaimHour", 10).coerceIn(0, 23)
            } else null,
            autoAcceptFriendMinLevel = obj.optIntFlex("autoAcceptFriendMinLevel", 0).coerceIn(0, 200),
            autoAcceptRequireOwnLevel = obj.optBoolFlex("autoAcceptRequireOwnLevel", false),
            autoAcceptHarvestStealEnabled = obj.optBoolFlex("autoAcceptHarvestStealEnabled", true),
            autoAcceptHarvestStealHarvest = obj.optIntFlex("autoAcceptHarvestStealHarvest", 8).coerceIn(0, 9999),
            autoAcceptHarvestStealSteal = obj.optIntFlex("autoAcceptHarvestStealSteal", 1).coerceIn(1, 9999),
            ranchStrategy = automation.optStringClean("ranch_strategy", "level"),
            ranchBagFallback = automation.optStringClean("ranch_bag_fallback", "level"),
            ranchCoopAnimal = obj.optIntFlex("ranch_coop_animal", 0),
            ranchShedAnimal = obj.optIntFlex("ranch_shed_animal", 0),
            ranchGrassThresholdPercent = obj.optIntFlex("ranch_grass_threshold_percent", 20).coerceIn(1, 100),
            ranchCheckMinutes = obj.optIntFlex("ranch_check_minutes", 5).coerceIn(1, 60),
            automationBooleans = boolMap,
            rawIntervalsJson = intervals.toString(),
            rawAutomationJson = automation.toString(),
        )
    }

    fun parseLoginConfig(obj: JSONObject): LoginConfigModel = LoginConfigModel(
        wechatQrLogin = obj.optBoolFlex("wechatQrLogin", true),
        qqQrLogin = obj.optBoolFlex("qqQrLogin", false),
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

    fun parseAccountLogEvent(obj: JSONObject): AccountLogEvent = AccountLogEvent(
        time = obj.optStringClean("time"),
        action = obj.optStringClean("action"),
        msg = obj.optStringClean("msg"),
        accountId = obj.optStringClean("accountId"),
        accountName = obj.optStringClean("accountName"),
    )

    fun parseAccountLogs(arr: JSONArray?): List<AccountLogEvent> {
        if (arr == null) return emptyList()
        return buildList(arr.length()) {
            for (i in 0 until arr.length()) {
                val e = arr.optJSONObject(i) ?: continue
                add(parseAccountLogEvent(e))
            }
        }
    }
}
