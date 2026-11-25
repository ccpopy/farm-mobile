package com.qqfarm.mobile.data.network

import com.qqfarm.mobile.platform.wallClockMillis
import com.qqfarm.mobile.data.model.ActivityDirectoryItem
import com.qqfarm.mobile.data.model.ActivityDirectoryResponse
import com.qqfarm.mobile.data.model.AutumnWishState
import com.qqfarm.mobile.data.model.CharityState
import com.qqfarm.mobile.data.model.CollectionBuff
import com.qqfarm.mobile.data.model.CollectionItemEntry
import com.qqfarm.mobile.data.model.CollectionMilestone
import com.qqfarm.mobile.data.model.CollectionMutationRule
import com.qqfarm.mobile.data.model.CollectionOperateResult
import com.qqfarm.mobile.data.model.CollectionSeries
import com.qqfarm.mobile.data.model.CollectionSnapshot
import com.qqfarm.mobile.data.model.CommerceItem
import com.qqfarm.mobile.data.model.IllustratedBook
import com.qqfarm.mobile.data.model.IllustratedBookItem
import com.qqfarm.mobile.data.model.IllustratedBuff
import com.qqfarm.mobile.data.model.IllustratedClaimResult
import com.qqfarm.mobile.data.model.IllustratedLevel
import com.qqfarm.mobile.data.model.IllustratedShopSeed
import com.qqfarm.mobile.data.model.IllustratedSnapshot
import com.qqfarm.mobile.data.model.MallCatalog
import com.qqfarm.mobile.data.model.MallCurrency
import com.qqfarm.mobile.data.model.MallGoods
import com.qqfarm.mobile.data.model.MallLimit
import com.qqfarm.mobile.data.model.MallMembership
import com.qqfarm.mobile.data.model.MallPurchaseResult
import com.qqfarm.mobile.data.model.MapleMarketComparison
import com.qqfarm.mobile.data.model.MapleMarketFriendQuoteRow
import com.qqfarm.mobile.data.model.MapleMarketGoods
import com.qqfarm.mobile.data.model.MapleMarketGoodsItem
import com.qqfarm.mobile.data.model.MapleMarketPurchaseRecord
import com.qqfarm.mobile.data.model.MapleMarketQuote
import com.qqfarm.mobile.data.model.MapleMarketState
import com.qqfarm.mobile.data.model.MilkTeaCandidate
import com.qqfarm.mobile.data.model.MilkTeaFruit
import com.qqfarm.mobile.data.model.MilkTeaFundTier
import com.qqfarm.mobile.data.model.MilkTeaGoods
import com.qqfarm.mobile.data.model.MilkTeaInteraction
import com.qqfarm.mobile.data.model.MilkTeaLevel
import com.qqfarm.mobile.data.model.MilkTeaRecipe
import com.qqfarm.mobile.data.model.MilkTeaSlip
import com.qqfarm.mobile.data.model.MilkTeaSlot
import com.qqfarm.mobile.data.model.MilkTeaState
import com.qqfarm.mobile.data.model.MilkTeaWorkState
import com.qqfarm.mobile.data.model.MugePersonality
import com.qqfarm.mobile.data.model.MugeQuestion
import com.qqfarm.mobile.data.model.MugeQuestionOption
import com.qqfarm.mobile.data.model.MugeState
import com.qqfarm.mobile.data.model.MugeTask
import com.qqfarm.mobile.data.model.MugeTier
import com.qqfarm.mobile.data.model.MutantCatalogEffect
import com.qqfarm.mobile.data.model.MutantProbability
import com.qqfarm.mobile.data.model.MutationCatalogCollectionSeries
import com.qqfarm.mobile.data.model.MutationCatalogSnapshot
import com.qqfarm.mobile.data.model.MysteryNpcOffer
import com.qqfarm.mobile.data.model.MysteryPurchaseResult
import com.qqfarm.mobile.data.model.MysteryShopState
import com.qqfarm.mobile.data.model.OfflineNotificationTypes
import com.qqfarm.mobile.data.model.OfflineReminderConfig
import com.qqfarm.mobile.data.model.PushChannelEntry
import com.qqfarm.mobile.data.model.findPushChannelSpec
import com.qqfarm.mobile.data.model.normalizeDingTalkEndpoint
import com.qqfarm.mobile.data.model.QingMeiState
import com.qqfarm.mobile.data.model.QixiState
import com.qqfarm.mobile.data.model.RanchBagItem
import com.qqfarm.mobile.data.model.RanchBookAnimal
import com.qqfarm.mobile.data.model.RanchBookData
import com.qqfarm.mobile.data.model.RanchBookGiantLevel
import com.qqfarm.mobile.data.model.RanchBookMutationRule
import com.qqfarm.mobile.data.model.RanchFriendDetail
import com.qqfarm.mobile.data.model.RanchFriendFeedMachine
import com.qqfarm.mobile.data.model.RanchFriendPen
import com.qqfarm.mobile.data.model.RanchFriendShelf
import com.qqfarm.mobile.data.model.RanchFriendSummary
import com.qqfarm.mobile.data.model.RanchMarketShelf
import com.qqfarm.mobile.data.model.RanchOperateResult
import com.qqfarm.mobile.data.model.RanchQuote
import com.qqfarm.mobile.data.model.RanchShopGood
import com.qqfarm.mobile.data.model.RanchTaskItem
import com.qqfarm.mobile.data.model.RanchViewSnapshot
import com.qqfarm.mobile.data.model.SeasonalSimpleStage
import com.qqfarm.mobile.data.model.ShareRewardMilestone
import com.qqfarm.mobile.data.model.ShareRewardState
import com.qqfarm.mobile.data.model.SolarActionResult
import com.qqfarm.mobile.data.model.SolarCalendarState
import com.qqfarm.mobile.data.model.SolarEvent
import com.qqfarm.mobile.data.model.SolarSeason
import com.qqfarm.mobile.data.model.StellarConstellationGroup
import com.qqfarm.mobile.data.model.StellarPassNode
import com.qqfarm.mobile.data.model.StellarShopGoods
import com.qqfarm.mobile.data.model.StellarSnapshot
import com.qqfarm.mobile.data.model.StellarSolarTerm
import com.qqfarm.mobile.data.model.SystemReleaseBackup
import com.qqfarm.mobile.data.model.SystemReleaseCleanupResult
import com.qqfarm.mobile.data.model.SystemReleaseCleanupSummary
import com.qqfarm.mobile.data.model.SystemUpdateCommit
import com.qqfarm.mobile.data.model.SystemUpdateJob
import com.qqfarm.mobile.data.model.SystemUpdatePullResult
import com.qqfarm.mobile.data.model.SystemUpdateStatus
import com.qqfarm.mobile.data.model.WeatherState
import com.qqfarm.mobile.data.network.json.JSONArray
import com.qqfarm.mobile.data.network.json.JSONObject
import com.qqfarm.mobile.platform.monotonicMillis

object FeatureJsonParsers {

    private const val WEATHER_COLLECTOR_BOTTLE_ID = 5001
    private const val WEATHER_SUMMON_BOTTLE_ID = 5002
    private const val WEATHER_BADGE_ID = 1027

    fun normalizeTimestampMs(raw: Long): Long =
        if (raw in 1L..99_999_999_999L) raw * 1000L else raw

    private fun directoryTimeMs(raw: Long): Long = when {
        raw <= 0L -> 0L
        raw < 1_000_000_000_000L -> raw * 1000L
        else -> raw
    }

    private fun normalizeTimestampSec(raw: Long): Long =
        if (raw > 99_999_999_999L) raw / 1000L else raw

    private fun cleanHtml(raw: String): String =
        raw.replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
            .replace(Regex("<[^>]+>"), "")
            .trim()

    fun parseCommerceItem(obj: JSONObject?): CommerceItem {
        if (obj == null) return CommerceItem()
        return CommerceItem(
            id = obj.optIntFlex("id"),
            count = obj.optLongFlex("count"),
            name = obj.optStringClean("name"),
            image = obj.optStringClean("image"),
            rarity = obj.optIntFlex("rarity"),
            description = obj.optStringClean("description"),
        )
    }

    fun parseCommerceItems(arr: JSONArray?): List<CommerceItem> {
        if (arr == null) return emptyList()
        return buildList(arr.length()) {
            for (i in 0 until arr.length()) {
                val item = arr.optJSONObject(i) ?: continue
                add(parseCommerceItem(item))
            }
        }
    }

    private fun parseStringList(arr: JSONArray?): List<String> {
        if (arr == null) return emptyList()
        return buildList(arr.length()) {
            for (i in 0 until arr.length()) {
                val text = arr.optString(i).trim()
                if (text.isNotEmpty() && text != "null") add(text)
            }
        }
    }

    internal fun parseIntList(arr: JSONArray?): List<Int> {
        if (arr == null) return emptyList()
        return buildList(arr.length()) {
            for (i in 0 until arr.length()) {
                val raw = arr.opt(i)?.toString()?.trim()?.toDoubleOrNull()?.toInt()
                if (raw != null) add(raw)
            }
        }
    }

    // ---- Solar Calendar ----

    fun parseSolarCalendar(obj: JSONObject): SolarCalendarState {
        val fetchedAt = com.qqfarm.mobile.platform.monotonicMillis()
        val seasonsArr = obj.optJSONArray("seasons") ?: JSONArray()
        val seasons = buildList(seasonsArr.length()) {
            for (i in 0 until seasonsArr.length()) {
                val s = seasonsArr.optJSONObject(i) ?: continue
                add(
                    SolarSeason(
                        id = s.optIntFlex("id"),
                        name = s.optStringClean("name"),
                        year = s.optStringClean("year"),
                        description = s.optStringClean("description"),
                        eventIds = parseIntList(s.optJSONArray("eventIds")),
                        litCount = s.optIntFlex("litCount"),
                        totalCount = s.optIntFlex("totalCount"),
                        claimed = s.optBoolFlex("claimed"),
                        canClaim = s.optBoolFlex("canClaim"),
                        rewards = parseCommerceItems(s.optJSONArray("rewards")),
                    )
                )
            }
        }

        val eventsArr = obj.optJSONArray("events") ?: JSONArray()
        val events = buildList(eventsArr.length()) {
            for (i in 0 until eventsArr.length()) {
                val e = eventsArr.optJSONObject(i) ?: continue
                val ansIdx = if (e.has("answerIndex") && !e.isNull("answerIndex")) e.optIntFlex("answerIndex") else null
                add(
                    SolarEvent(
                        id = e.optIntFlex("id"),
                        name = e.optStringClean("name"),
                        description = e.optStringClean("description"),
                        startTimeSec = normalizeTimestampSec(e.optLongFlex("startTime")),
                        endTimeSec = normalizeTimestampSec(e.optLongFlex("endTime")),
                        status = e.optIntFlex("status"),
                        directClaimed = e.optBoolFlex("directClaimed"),
                        quizClaimed = e.optBoolFlex("quizClaimed"),
                        quizWrongToday = e.optBoolFlex("quizWrongToday"),
                        wrongOptions = parseIntList(e.optJSONArray("wrongOptions")),
                        question = e.optStringClean("question"),
                        options = parseStringList(e.optJSONArray("options")),
                        errorTips = e.optStringClean("errorTips"),
                        answerIndex = ansIdx,
                        rewards = parseCommerceItems(e.optJSONArray("rewards")),
                        questionRewards = parseCommerceItems(e.optJSONArray("questionRewards")),
                        canClaim = e.optBoolFlex("canClaim"),
                        canAnswer = e.optBoolFlex("canAnswer"),
                    )
                )
            }
        }

        return SolarCalendarState(
            serverTimeSec = normalizeTimestampSec(obj.optLongFlex("serverTime", wallClockMillis() / 1000L)),
            receivedAtElapsedMs = fetchedAt,
            defaultEventId = obj.optIntFlex("defaultEventId"),
            seasons = seasons,
            events = events,
        )
    }

    fun parseSolarActionResult(obj: JSONObject): SolarActionResult {
        val calObj = obj.optJSONObject("calendar")
        val correct = if (obj.has("correct") && !obj.isNull("correct")) obj.optBoolFlex("correct") else null
        return SolarActionResult(
            rewards = parseCommerceItems(obj.optJSONArray("rewards")),
            correct = correct,
            calendar = if (calObj != null) parseSolarCalendar(calObj) else null,
            refreshRequired = obj.optBoolFlex("refreshRequired"),
        )
    }

    // ---- Mall / Props Shop / Mystery Shop ----

    fun parseMallCatalog(obj: JSONObject): MallCatalog {
        val fetchedAt = com.qqfarm.mobile.platform.monotonicMillis()
        val memObj = obj.optJSONObject("membership")
        val membership = if (memObj != null) {
            MallMembership(
                isSvip = memObj.optBoolFlex("isSvip"),
                remainingDays = memObj.optIntFlex("remainingDays"),
            )
        } else null

        val currArr = obj.optJSONArray("currencies") ?: JSONArray()
        val currencies = buildList(currArr.length()) {
            for (i in 0 until currArr.length()) {
                val c = currArr.optJSONObject(i) ?: continue
                add(
                    MallCurrency(
                        id = c.optIntFlex("id"),
                        name = c.optStringClean("name"),
                        image = c.optStringClean("image"),
                        count = c.optLongFlex("count"),
                        balanceKnown = c.optBoolFlex("balanceKnown"),
                    )
                )
            }
        }

        val goodsArr = obj.optJSONArray("goods") ?: JSONArray()
        val goods = buildList(goodsArr.length()) {
            for (i in 0 until goodsArr.length()) {
                val g = goodsArr.optJSONObject(i) ?: continue
                val priceObj = g.optJSONObject("price") ?: JSONObject()
                val priceBalance = if (priceObj.has("balance") && !priceObj.isNull("balance")) {
                    priceObj.optLongFlex("balance")
                } else null
                val origPrice = if (g.has("originalPrice") && !g.isNull("originalPrice")) {
                    g.optLongFlex("originalPrice")
                } else null
                val limitObj = g.optJSONObject("limit")
                val limit = if (limitObj != null) {
                    val rem = if (limitObj.has("remaining") && !limitObj.isNull("remaining")) {
                        limitObj.optIntFlex("remaining")
                    } else null
                    MallLimit(
                        type = limitObj.optIntFlex("type"),
                        bought = limitObj.optIntFlex("bought"),
                        max = limitObj.optIntFlex("max"),
                        remaining = rem,
                    )
                } else null

                add(
                    MallGoods(
                        id = g.optIntFlex("id"),
                        name = g.optStringClean("name"),
                        type = g.optIntFlex("type"),
                        rewards = parseCommerceItems(g.optJSONArray("rewards")),
                        priceId = priceObj.optIntFlex("id"),
                        priceName = priceObj.optStringClean("name"),
                        priceImage = priceObj.optStringClean("image"),
                        priceCount = priceObj.optLongFlex("count"),
                        priceBalance = priceBalance,
                        originalPrice = origPrice,
                        isFree = g.optBoolFlex("isFree"),
                        limit = limit,
                        isLimited = g.optBoolFlex("isLimited"),
                        discountText = g.optStringClean("discountText"),
                        isDiscounted = g.optBoolFlex("isDiscounted"),
                        discountEndTimeMs = normalizeTimestampMs(g.optLongFlex("discountEndTime")),
                        available = g.optBoolFlex("available"),
                        purchasable = g.optBoolFlex("purchasable"),
                        purchaseStatus = g.optStringClean("purchaseStatus", "available"),
                        unavailableReason = g.optStringClean("unavailableReason"),
                    )
                )
            }
        }

        return MallCatalog(
            shopId = obj.optIntFlex("shopId"),
            slotType = obj.optIntFlex("slotType", 1),
            subSlotType = obj.optIntFlex("subSlotType"),
            serverTimeMs = normalizeTimestampMs(obj.optLongFlex("serverTime", wallClockMillis())),
            receivedAtElapsedMs = fetchedAt,
            refreshCountdownSec = obj.optLongFlex("refreshCountdown"),
            membership = membership,
            currencies = currencies,
            goods = goods,
        )
    }

    fun parseMallPurchaseResult(obj: JSONObject): MallPurchaseResult {
        val purchase = obj.optJSONObject("purchase")
        val catalogObj = obj.optJSONObject("catalog")
        return MallPurchaseResult(
            rewards = parseCommerceItems(purchase?.optJSONArray("rewards")),
            catalog = if (catalogObj != null) parseMallCatalog(catalogObj) else null,
            refreshRequired = obj.optBoolFlex("refreshRequired"),
        )
    }

    fun parseMysteryShop(obj: JSONObject): MysteryShopState {
        val fetchedAt = com.qqfarm.mobile.platform.monotonicMillis()
        val npcObj = obj.optJSONObject("npc")
        val npc = if (npcObj != null) {
            val priceObj = npcObj.optJSONObject("price") ?: JSONObject()
            val bal = if (priceObj.has("balance") && !priceObj.isNull("balance")) {
                priceObj.optLongFlex("balance")
            } else null
            MysteryNpcOffer(
                id = npcObj.optIntFlex("id"),
                reward = parseCommerceItem(npcObj.optJSONObject("reward")),
                priceId = priceObj.optIntFlex("id"),
                priceName = priceObj.optStringClean("name"),
                priceImage = priceObj.optStringClean("image"),
                priceCount = priceObj.optLongFlex("count"),
                priceBalance = bal,
                originalPrice = npcObj.optLongFlex("originalPrice"),
                unitPrice = npcObj.optLongFlex("unitPrice"),
                unitOriginalPrice = npcObj.optLongFlex("unitOriginalPrice"),
                discountPercent = npcObj.optIntFlex("discountPercent"),
            )
        } else null

        return MysteryShopState(
            active = obj.optBoolFlex("active"),
            purchaseState = obj.optStringClean("purchaseState"),
            serverTimeMs = normalizeTimestampMs(obj.optLongFlex("serverTime", wallClockMillis())),
            receivedAtElapsedMs = fetchedAt,
            activeTimeMs = normalizeTimestampMs(obj.optLongFlex("activeTime")),
            expireTimeMs = normalizeTimestampMs(obj.optLongFlex("expireTime")),
            npc = npc,
            rawJson = obj.toString(),
        )
    }

    fun parseMysteryPurchaseResult(obj: JSONObject): MysteryPurchaseResult {
        val shopObj = obj.optJSONObject("shop")
        val purchase = obj.optJSONObject("purchase")
        return MysteryPurchaseResult(
            shop = if (shopObj != null) parseMysteryShop(shopObj) else null,
            reward = if (purchase?.has("reward") == true) parseCommerceItem(purchase.optJSONObject("reward")) else null,
            price = if (purchase?.has("price") == true) parseCommerceItem(purchase.optJSONObject("price")) else null,
        )
    }

    // ---- Extended Ranch Views ----

    fun parseRanchViewSnapshot(obj: JSONObject): RanchViewSnapshot {
        val base = JsonParsers.parseRanchPayload(obj)
        val bagArr = obj.optJSONArray("bag") ?: JSONArray()
        val bag = buildList(bagArr.length()) {
            for (i in 0 until bagArr.length()) {
                val b = bagArr.optJSONObject(i) ?: continue
                val canObj = b.optJSONObject("can")
                val prodObj = b.optJSONObject("product")
                val type = b.optIntFlex("type")
                add(
                    RanchBagItem(
                        uid = b.optStringClean("uid"),
                        id = b.optIntFlex("id"),
                        name = b.optStringClean("name"),
                        image = b.optStringClean("image"),
                        count = b.optLongFlex("count"),
                        type = type,
                        locked = b.optBoolFlex("locked"),
                        lockable = b.optBoolFlex("lockable"),
                        animalId = b.optIntFlex("animalId"),
                        husbandryType = b.optStringClean("husbandryType"),
                        giantCub = b.optBoolFlex("giantCub"),
                        canSell = b.optBoolFlex("canSell"),
                        isCan = canObj != null,
                        canSpeedUpSec = canObj?.optLongFlex("speed_up_secs") ?: 0L,
                        isProduct = prodObj != null || type == 106 || type == 107,
                        weightX10 = prodObj?.optIntFlex("weightX10") ?: 0,
                        sellPrices = parseCommerceItems(b.optJSONArray("sellPrices")),
                    )
                )
            }
        }

        val tasksArr = obj.optJSONArray("tasks") ?: JSONArray()
        val tasks = buildList(tasksArr.length()) {
            for (i in 0 until tasksArr.length()) {
                val t = tasksArr.optJSONObject(i) ?: continue
                add(
                    RanchTaskItem(
                        id = t.optStringClean("id"),
                        category = t.optStringClean("category", "daily"),
                        desc = t.optStringClean("desc"),
                        progress = t.optIntFlex("progress"),
                        total = t.optIntFlex("total"),
                        unlocked = t.optBoolFlex("unlocked"),
                        canClaim = t.optBoolFlex("canClaim"),
                        claimed = t.optBoolFlex("claimed"),
                    )
                )
            }
        }

        val bookObj = obj.optJSONObject("book")
        val book = if (bookObj != null) {
            val normalObj = bookObj.optJSONObject("normal") ?: JSONObject()
            val giantObj = bookObj.optJSONObject("giant") ?: JSONObject()
            val levelsArr = giantObj.optJSONArray("levels") ?: JSONArray()
            val giantLevels = buildList(levelsArr.length()) {
                for (i in 0 until levelsArr.length()) {
                    val lv = levelsArr.optJSONObject(i) ?: continue
                    add(
                        RanchBookGiantLevel(
                            level = lv.optIntFlex("id", i + 1),
                            score = lv.optLongFlex("score"),
                            claimed = lv.optBoolFlex("claimed"),
                            canClaim = lv.optBoolFlex("canClaim"),
                            rewards = parseCommerceItems(lv.optJSONArray("rewards")),
                        )
                    )
                }
            }
            val animalsArr = bookObj.optJSONArray("animals") ?: JSONArray()
            val animals = buildList(animalsArr.length()) {
                for (i in 0 until animalsArr.length()) {
                    val a = animalsArr.optJSONObject(i) ?: continue
                    val gItem = a.optJSONObject("giantItem")
                    val g = a.optJSONObject("giant")
                    val weightNum = g?.optDouble("weightKg", 0.0) ?: 0.0
                    val weightStr = if (weightNum > 0.0) {
                        com.qqfarm.mobile.platform.fmtDec(weightNum, 2).trimEnd('0').trimEnd('.') + " kg"
                    } else ""
                    add(
                        RanchBookAnimal(
                            id = a.optIntFlex("id"),
                            name = a.optStringClean("name"),
                            image = a.optStringClean("image"),
                            unlocked = a.optBoolFlex("unlocked"),
                            hasGiant = gItem != null,
                            giantName = gItem?.optStringClean("name").orEmpty(),
                            giantImage = gItem?.optStringClean("image").orEmpty(),
                            giantUnlocked = g?.optBoolFlex("unlocked") ?: false,
                            giantIsNew = g?.optBoolFlex("isNew") ?: false,
                            giantTierName = g?.optStringClean("tierName").orEmpty(),
                            giantWeightKg = weightStr,
                            giantHarvestCount = g?.optStringClean("harvestCount").orEmpty(),
                        )
                    )
                }
            }
            val mutArr = bookObj.optJSONArray("mutations") ?: JSONArray()
            val mutations = buildList(mutArr.length()) {
                for (i in 0 until mutArr.length()) {
                    val m = mutArr.optJSONObject(i) ?: continue
                    add(
                        RanchBookMutationRule(
                            type = m.optIntFlex("type"),
                            name = m.optStringClean("name"),
                            description = m.optStringClean("description"),
                            baseProbabilityPercent = m.optIntFlex("baseProbabilityPercent"),
                        )
                    )
                }
            }
            RanchBookData(
                normalLevel = normalObj.optIntFlex("level"),
                normalProgress = normalObj.optIntFlex("progress"),
                normalNextProgress = normalObj.optIntFlex("nextProgress"),
                normalCanClaim = normalObj.optBoolFlex("canClaim"),
                normalRewards = parseCommerceItems(normalObj.optJSONArray("rewards")),
                giantScore = giantObj.optLongFlex("score"),
                giantLevels = giantLevels,
                animals = animals,
                mutations = mutations,
            )
        } else null

        val shopGoods = parseRanchGoodsArray(obj.optJSONObject("shop")?.optJSONArray("goods"))
        val marketObj = obj.optJSONObject("market")
        val shelvesArr = marketObj?.optJSONArray("shelves") ?: JSONArray()
        val marketShelves = buildList(shelvesArr.length()) {
            for (i in 0 until shelvesArr.length()) {
                val s = shelvesArr.optJSONObject(i) ?: continue
                add(
                    RanchMarketShelf(
                        id = s.optIntFlex("id"),
                        name = s.optStringClean("name"),
                        sort = s.optIntFlex("sort"),
                    )
                )
            }
        }.sortedBy { it.sort }
        val marketGoods = parseRanchGoodsArray(marketObj?.optJSONArray("goods"))
        val canShopGoods = parseRanchGoodsArray(obj.optJSONObject("canShop")?.optJSONArray("goods"))

        val friendsArr = obj.optJSONArray("friends") ?: JSONArray()
        val friends = buildList(friendsArr.length()) {
            for (i in 0 until friendsArr.length()) {
                val f = friendsArr.optJSONObject(i) ?: continue
                add(
                    RanchFriendSummary(
                        gid = f.optStringClean("gid"),
                        name = f.optStringClean("name").ifBlank { f.optStringClean("gid") },
                        avatarUrl = f.optStringClean("avatarUrl"),
                        canSteal = f.optBoolFlex("canSteal"),
                        stealLotCount = f.optIntFlex("stealLotCount"),
                        canMidwife = f.optBoolFlex("canMidwife"),
                        midwifeCount = f.optIntFlex("midwifeCount"),
                        canHelp = f.optBoolFlex("canHelp"),
                        helpCount = f.optIntFlex("helpCount"),
                    )
                )
            }
        }

        val frObj = obj.optJSONObject("friendRanch")
        val friendRanch = if (frObj != null) {
            val sceneRestored = frObj.optBoolFlex("sceneRestored", true)
            val canSteal = sceneRestored && frObj.optBoolFlex("canSteal") && frObj.optIntFlex("stealableShelves") > 0
            val ranchesArr = frObj.optJSONArray("ranches") ?: JSONArray()
            val ranches = buildList(ranchesArr.length()) {
                for (i in 0 until ranchesArr.length()) {
                    val p = ranchesArr.optJSONObject(i) ?: continue
                    val penId = p.optStringClean("id")
                    val animalsArr = p.optJSONArray("animals") ?: JSONArray()
                    val animals = buildList(animalsArr.length()) {
                        for (a in 0 until animalsArr.length()) {
                            val an = animalsArr.optJSONObject(a) ?: continue
                            add(JsonParsers.parseRanchAnimal(an))
                        }
                    }
                    val shelvesFr = p.optJSONArray("shelves") ?: JSONArray()
                    val shelves = buildList(shelvesFr.length()) {
                        for (s in 0 until shelvesFr.length()) {
                            val sh = shelvesFr.optJSONObject(s) ?: continue
                            add(
                                RanchFriendShelf(
                                    penId = penId,
                                    slot = sh.optIntFlex("slot"),
                                    canSteal = canSteal && sh.optBoolFlex("canSteal"),
                                    products = parseCommerceItems(sh.optJSONArray("products")),
                                )
                            )
                        }
                    }
                    add(
                        RanchFriendPen(
                            id = penId,
                            name = p.optStringClean("name"),
                            mosquitoes = p.optIntFlex("mosquitoes"),
                            mosquitoLimit = p.optIntFlex("mosquitoLimit"),
                            mosquitoRoom = p.optIntFlex("mosquitoRoom"),
                            animals = animals,
                            shelves = shelves,
                        )
                    )
                }
            }
            val stealableShelves = ranches.sumOf { pen -> pen.shelves.count { it.canSteal } }
            val feedMachine = frObj.optJSONObject("feedMachine")?.let { machine ->
                val id = machine.optStringClean("id")
                val count = machine.optStringClean("count").toIntOrNull()
                val capacity = machine.optStringClean("capacity").toIntOrNull()
                if ((id.toULongOrNull() ?: 0uL) > 0uL && count != null && count >= 0 && capacity != null && capacity > 0) {
                    RanchFriendFeedMachine(id, count, capacity)
                } else null
            }
            val grassAvailable = frObj.optStringClean("grassAvailable").toLongOrNull()?.coerceAtLeast(0L)
            RanchFriendDetail(
                gid = frObj.optStringClean("gid"),
                level = frObj.optIntFlex("level"),
                canSteal = stealableShelves > 0,
                stealableShelves = stealableShelves,
                strokeRemaining = frObj.optIntFlex("strokeRemaining"),
                mosquitoRemaining = frObj.optIntFlex("mosquitoRemaining"),
                cleanableHazards = frObj.optIntFlex("cleanableHazards"),
                feedMachine = feedMachine,
                grassAvailable = grassAvailable,
                career = JsonParsers.parseRanchCareer(frObj.optJSONObject("career")),
                notice = frObj.optStringClean("notice"),
                sceneRestored = sceneRestored,
                ranches = ranches,
            )
        } else null

        return RanchViewSnapshot(
            base = base,
            bag = bag,
            tasks = tasks,
            book = book,
            shopGoods = shopGoods,
            marketPresent = marketObj?.optBoolFlex("present") ?: false,
            marketShelves = marketShelves,
            marketGoods = marketGoods,
            canShopGoods = canShopGoods,
            mallGoods = obj.optJSONObject("mall")?.let { parseRanchGoodsArray(it.optJSONArray("goods")) },
            friends = friends,
            friendRanch = friendRanch,
        )
    }

    private fun parseRanchGoodsArray(arr: JSONArray?): List<RanchShopGood> {
        if (arr == null) return emptyList()
        return buildList(arr.length()) {
            for (i in 0 until arr.length()) {
                val g = arr.optJSONObject(i) ?: continue
                val reward = parseCommerceItem(g.optJSONObject("reward"))
                val rewardsList = parseCommerceItems(g.optJSONArray("rewards")).ifEmpty { listOf(reward) }
                val priceObj = g.optJSONObject("price") ?: JSONObject()
                val bal = if (priceObj.has("balance") && !priceObj.isNull("balance")) {
                    priceObj.optLongFlex("balance")
                } else null
                val rem = if (g.has("remaining") && !g.isNull("remaining")) g.optIntFlex("remaining") else null
                add(
                    RanchShopGood(
                        id = g.optStringClean("id"),
                        name = g.optStringClean("name").ifBlank { reward.name },
                        reward = reward,
                        rewards = rewardsList,
                        priceId = priceObj.optIntFlex("id"),
                        priceName = priceObj.optStringClean("name"),
                        priceImage = priceObj.optStringClean("image"),
                        priceCount = priceObj.optLongFlex("count"),
                        priceBalance = bal,
                        available = g.optBoolFlex("available"),
                        free = g.optBoolFlex("free"),
                        maxCount = g.optIntFlex("maxCount", 999).coerceAtLeast(1),
                        remaining = rem,
                        requiredLevel = g.optIntFlex("requiredLevel"),
                        shelfId = g.optIntFlex("shelfId"),
                        husbandryType = g.optJSONObject("animal")?.optStringClean("husbandry_type").orEmpty(),
                    )
                )
            }
        }
    }

    fun parseRanchQuote(obj: JSONObject): RanchQuote {
        val exp = obj.optJSONObject("expected")
        return RanchQuote(
            action = obj.optStringClean("action"),
            label = obj.optStringClean("label"),
            count = obj.optIntFlex("count"),
            skipped = obj.optBoolFlex("skipped"),
            signature = exp?.optStringClean("signature").orEmpty(),
            costItems = parseCommerceItems(obj.optJSONArray("costItems")),
            rewards = parseCommerceItems(obj.optJSONArray("rewards")),
            message = obj.optStringClean("message"),
        )
    }

    fun parseRanchOperateResult(obj: JSONObject): RanchOperateResult {
        val status = obj.optStringClean("status")
        if (status != "confirmed" && status != "skipped") {
            throw ApiException("牧场操作结果未确认，请刷新核对", errorCode = "RANCH_RESULT_UNKNOWN")
        }
        return RanchOperateResult(
            status = status,
            notice = obj.optStringClean("notice"),
            refreshRequired = obj.optBoolFlex("refreshRequired"),
            sceneRestored = obj.optBoolFlex("sceneRestored", true),
        )
    }

    // ---- Activity Center ----

    fun parseActivityDirectory(obj: JSONObject): ActivityDirectoryResponse {
        val fetchedAt = com.qqfarm.mobile.platform.monotonicMillis()
        val arr = obj.optJSONArray("activities") ?: JSONArray()
        val items = buildList(arr.length()) {
            for (i in 0 until arr.length()) {
                val e = arr.optJSONObject(i) ?: continue
                val id = e.optStringClean("id")
                if (id.isEmpty()) continue
                val rawIds = parseStringList(e.optJSONArray("activityIds"))
                val startRaw = if (e.has("startTime") && !e.isNull("startTime")) e.optLongFlex("startTime") else null
                val endRaw = if (e.has("endTime") && !e.isNull("endTime")) e.optLongFlex("endTime") else null
                add(
                    ActivityDirectoryItem(
                        id = id,
                        activityIds = if (rawIds.isNotEmpty()) rawIds else listOf(id),
                        name = e.optStringClean("name"),
                        startTimeMs = startRaw?.let { directoryTimeMs(it) },
                        endTimeMs = endRaw?.let { directoryTimeMs(it) },
                        instanceId = e.optStringClean("instanceId").ifBlank { null },
                        gameplayKey = e.optStringClean("gameplayKey").ifBlank { null },
                        gameplayTargets = parseStringList(e.optJSONArray("gameplayTargets")),
                        detailTarget = e.optStringClean("detailTarget").ifBlank { null },
                    )
                )
            }
        }
        return ActivityDirectoryResponse(
            serverTimeMs = normalizeTimestampMs(obj.optLongFlex("serverTime", wallClockMillis())),
            receivedAtElapsedMs = fetchedAt,
            activities = items,
        )
    }

    private fun parseMugeTiers(arr: JSONArray?): List<MugeTier> {
        if (arr == null) return emptyList()
        return buildList(arr.length()) {
            for (i in 0 until arr.length()) {
                val t = arr.optJSONObject(i) ?: continue
                add(
                    MugeTier(
                        id = t.optStringClean("id"),
                        threshold = t.optLongFlex("threshold"),
                        status = t.optIntFlex("status"),
                        rewards = parseCommerceItems(t.optJSONArray("rewards")),
                    )
                )
            }
        }
    }

    fun parseMugeState(obj: JSONObject): MugeState {
        val fetchedAt = com.qqfarm.mobile.platform.monotonicMillis()
        val rulesArr = obj.optJSONArray("rules") ?: JSONArray()
        val rules = buildList {
            for (i in 0 until rulesArr.length()) {
                val raw = rulesArr.optString(i).trim()
                if (raw.isEmpty() || raw == "null") continue
                val parsed = runCatching { JSONObject(raw) }.getOrNull()
                val txtArr = parsed?.optJSONObject("tips")?.optJSONArray("txt")
                if (txtArr != null) {
                    for (j in 0 until txtArr.length()) {
                        val line = cleanHtml(txtArr.optString(j))
                        if (line.isNotEmpty()) add(line)
                    }
                } else {
                    val line = cleanHtml(raw)
                    if (line.isNotEmpty()) add(line)
                }
            }
        }

        val celObj = obj.optJSONObject("celebration")
        val frObj = obj.optJSONObject("friends")
        val hbObj = obj.optJSONObject("handbook")
        val tasksArr = hbObj?.optJSONArray("tasks") ?: JSONArray()
        val hbTasks = buildList(tasksArr.length()) {
            for (i in 0 until tasksArr.length()) {
                val t = tasksArr.optJSONObject(i) ?: continue
                add(
                    MugeTask(
                        id = t.optStringClean("id"),
                        description = t.optStringClean("description"),
                        progress = t.optIntFlex("progress"),
                        target = t.optIntFlex("target"),
                        status = t.optIntFlex("status"),
                        unlockDay = t.optIntFlex("unlockDay"),
                        points = t.optIntFlex("points"),
                        rewards = parseCommerceItems(t.optJSONArray("rewards")),
                    )
                )
            }
        }

        val pObj = obj.optJSONObject("personality")
        val personality = if (pObj != null) {
            val ansObj = pObj.optJSONObject("answers") ?: JSONObject()
            val ansMap = mutableMapOf<Int, Int>()
            val keys = ansObj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                val qId = k.toIntOrNull() ?: continue
                ansMap[qId] = ansObj.optIntFlex(k)
            }
            MugePersonality(
                answers = ansMap,
                personalityId = pObj.optIntFlex("personalityId"),
                settleCount = pObj.optIntFlex("settleCount"),
                image = pObj.optStringClean("image"),
                shareRewardClaimed = pObj.optBoolFlex("shareRewardClaimed"),
            )
        } else null

        val qArr = obj.optJSONArray("questions") ?: JSONArray()
        val questions = buildList(qArr.length()) {
            for (i in 0 until qArr.length()) {
                val q = qArr.optJSONObject(i) ?: continue
                val optArr = q.optJSONArray("options") ?: JSONArray()
                val options = buildList(optArr.length()) {
                    for (j in 0 until optArr.length()) {
                        val o = optArr.optJSONObject(j) ?: continue
                        add(
                            MugeQuestionOption(
                                value = o.optIntFlex("value"),
                                label = o.optStringClean("label"),
                                text = o.optStringClean("text"),
                            )
                        )
                    }
                }
                add(
                    MugeQuestion(
                        id = q.optIntFlex("id"),
                        title = q.optStringClean("title"),
                        options = options,
                    )
                )
            }
        }

        return MugeState(
            activityId = obj.optStringClean("activityId"),
            title = obj.optStringClean("title", "牧歌初启"),
            serverTimeMs = normalizeTimestampMs(obj.optLongFlex("serverTime", wallClockMillis())),
            receivedAtElapsedMs = fetchedAt,
            startTimeMs = normalizeTimestampMs(obj.optLongFlex("startTime")),
            endTimeMs = normalizeTimestampMs(obj.optLongFlex("endTime")),
            active = obj.optBoolFlex("active"),
            rules = rules,
            celebrationTotal = celObj?.optLongFlex("total") ?: 0L,
            celebrationTiers = parseMugeTiers(celObj?.optJSONArray("tiers")),
            friendsCount = frObj?.optLongFlex("count") ?: 0L,
            friendsTiers = parseMugeTiers(frObj?.optJSONArray("tiers")),
            handbookAvailable = hbObj != null,
            handbookPoints = hbObj?.optIntFlex("points") ?: 0,
            handbookTiers = parseMugeTiers(hbObj?.optJSONArray("tiers")),
            handbookTasks = hbTasks,
            personality = personality,
            questions = questions,
        )
    }

    fun parseMilkTeaState(obj: JSONObject): MilkTeaState {
        val fetchedAt = com.qqfarm.mobile.platform.monotonicMillis()
        val summary = obj.optJSONObject("summary") ?: JSONObject()
        val shop = obj.optJSONObject("shop")
        val balancesObj = obj.optJSONObject("balances") ?: JSONObject()
        val balances = mutableMapOf<Int, Long>()
        val bKeys = balancesObj.keys()
        while (bKeys.hasNext()) {
            val k = bKeys.next()
            val id = k.toIntOrNull() ?: continue
            balances[id] = balancesObj.optLongFlex(k)
        }

        val boostCoinObj = shop?.optJSONObject("boost_coin_price") ?: JSONObject()
        val boostCoins = mutableMapOf<String, Long>()
        val bcKeys = boostCoinObj.keys()
        while (bcKeys.hasNext()) {
            val k = bcKeys.next()
            boostCoins[k] = boostCoinObj.optLongFlex(k)
        }

        val drinkCupsObj = shop?.optJSONObject("drink_cups") ?: JSONObject()
        val recipesArr = obj.optJSONArray("recipes") ?: JSONArray()
        val recipes = buildList(recipesArr.length()) {
            for (i in 0 until recipesArr.length()) {
                val r = recipesArr.optJSONObject(i) ?: continue
                val rid = r.optStringClean("recipe_id")
                add(
                    MilkTeaRecipe(
                        recipeId = rid,
                        name = r.optStringClean("recipe_name"),
                        image = r.optStringClean("image"),
                        description = r.optStringClean("description"),
                        unlocked = r.optBoolFlex("unlocked"),
                        isAdvanced = r.optBoolFlex("is_advanced"),
                        tokenPrice = r.optLongFlex("token_price"),
                        coinPrice = r.optLongFlex("coin_price"),
                        boostTokenPrice = r.optLongFlex("boostTokenPrice"),
                        boostCoinPrice = boostCoins[rid] ?: 0L,
                        advancedStockCups = drinkCupsObj.optLongFlex(rid),
                        fruitId = r.optIntFlex("fruitId"),
                    )
                )
            }
        }

        val pendingCupsObj = shop?.optJSONObject("pending_cups_by_recipe") ?: JSONObject()
        val pendingCups = mutableListOf<Pair<String, Int>>()
        val pcKeys = pendingCupsObj.keys()
        while (pcKeys.hasNext()) {
            val rid = pcKeys.next()
            val count = pendingCupsObj.optIntFlex(rid)
            if (count > 0) {
                val rName = recipes.firstOrNull { it.recipeId == rid }?.name ?: "奶茶 #$rid"
                pendingCups.add(rName to count)
            }
        }

        val slotsArr = shop?.optJSONArray("slots") ?: JSONArray()
        val slots = buildList(slotsArr.length()) {
            for (i in 0 until slotsArr.length()) {
                val s = slotsArr.optJSONObject(i) ?: continue
                add(
                    MilkTeaSlot(
                        slotIndex = s.optIntFlex("slot_index"),
                        employeeName = s.optJSONObject("employee")?.optStringClean("name").orEmpty().ifBlank { "员工" },
                        talentName = s.optStringClean("talent_name"),
                        endAtSec = normalizeTimestampSec(s.optLongFlex("end_at")),
                    )
                )
            }
        }

        val fruitsArr = obj.optJSONArray("fruits") ?: JSONArray()
        val fruits = buildList(fruitsArr.length()) {
            for (i in 0 until fruitsArr.length()) {
                val f = fruitsArr.optJSONObject(i) ?: continue
                add(
                    MilkTeaFruit(
                        uid = f.optStringClean("uid"),
                        id = f.optIntFlex("id"),
                        name = f.optStringClean("name"),
                        image = f.optStringClean("image"),
                        count = f.optLongFlex("count"),
                        locked = f.optBoolFlex("locked"),
                        mutations = parseStringList(f.optJSONArray("mutations")),
                    )
                )
            }
        }

        val goodsArr = obj.optJSONArray("goods") ?: JSONArray()
        val goods = buildList(goodsArr.length()) {
            for (i in 0 until goodsArr.length()) {
                val g = goodsArr.optJSONObject(i) ?: continue
                val conds = g.optJSONArray("conditions") ?: JSONArray()
                var unmet = ""
                for (cIdx in 0 until conds.length()) {
                    val cond = conds.optJSONObject(cIdx) ?: continue
                    if (!cond.optBoolFlex("satisfied")) {
                        unmet = "需店铺等级 ${cond.optIntFlex("target")}（当前 ${cond.optIntFlex("current")}）"
                        break
                    }
                }
                val rem = if (g.has("remaining") && !g.isNull("remaining")) g.optIntFlex("remaining") else null
                add(
                    MilkTeaGoods(
                        id = g.optStringClean("id"),
                        name = g.optStringClean("name"),
                        rewards = parseCommerceItems(g.optJSONArray("rewards")),
                        costs = parseCommerceItems(g.optJSONArray("costs")),
                        available = g.optBoolFlex("available"),
                        remaining = rem,
                        purchaseLimit = g.optIntFlex("purchaseLimit"),
                        limitPeriod = g.optStringClean("limitPeriod"),
                        unmetConditionText = unmet,
                    )
                )
            }
        }

        val fundObj = obj.optJSONObject("fund")
        val fundTiersArr = fundObj?.optJSONArray("tiers") ?: JSONArray()
        val fundTiers = buildList(fundTiersArr.length()) {
            for (i in 0 until fundTiersArr.length()) {
                val ft = fundTiersArr.optJSONObject(i) ?: continue
                val cond0 = ft.optJSONArray("conditions")?.optJSONObject(0)
                add(
                    MilkTeaFundTier(
                        id = ft.optStringClean("id"),
                        targetLevel = cond0?.optIntFlex("target") ?: 0,
                        state = ft.optIntFlex("state"),
                        rewards = parseCommerceItems(ft.optJSONArray("rewards")),
                    )
                )
            }
        }

        val levelsArr = obj.optJSONArray("levels") ?: JSONArray()
        val levels = buildList(levelsArr.length()) {
            for (i in 0 until levelsArr.length()) {
                val lv = levelsArr.optJSONObject(i) ?: continue
                add(
                    MilkTeaLevel(
                        level = lv.optIntFlex("level"),
                        revenueRequired = lv.optLongFlex("revenue_required"),
                        basePopularity = lv.optIntFlex("base_popularity"),
                        employeeSlots = lv.optIntFlex("employee_slot_count"),
                        rewards = parseCommerceItems(lv.optJSONArray("rewards")),
                        recipes = parseStringList(lv.optJSONArray("recipes")),
                    )
                )
            }
        }

        val hireObj = obj.optJSONObject("hire")
        val durationsArr = obj.optJSONArray("durations") ?: JSONArray()
        val durations = buildList(durationsArr.length()) {
            for (i in 0 until durationsArr.length()) {
                val d = durationsArr.optJSONObject(i) ?: continue
                add(d.optIntFlex("duration_id") to (d.optIntFlex("duration_sec") / 3600).coerceAtLeast(1))
            }
        }
        val candidates = mutableListOf<MilkTeaCandidate>()
        fun readCandidateArray(arr: JSONArray?, isRobot: Boolean) {
            if (arr == null) return
            for (i in 0 until arr.length()) {
                val c = arr.optJSONObject(i) ?: continue
                val fr = c.optJSONObject("friend")
                candidates.add(
                    MilkTeaCandidate(
                        gid = fr?.optStringClean("gid").orEmpty(),
                        name = fr?.optStringClean("name").orEmpty(),
                        talentName = c.optStringClean("talent_name"),
                        talentId = c.optIntFlex("talent_id"),
                        isRobot = isRobot || c.optBoolFlex("is_robot"),
                        state = c.optIntFlex("state"),
                        stateUntilSec = normalizeTimestampSec(c.optLongFlex("state_until_at")),
                        rehireAtSec = normalizeTimestampSec(c.optLongFlex("rehire_available_at")),
                    )
                )
            }
        }
        readCandidateArray(hireObj?.optJSONArray("friends"), false)
        readCandidateArray(hireObj?.optJSONArray("robots"), true)

        val workObj = obj.optJSONObject("work")
        val work = if (workObj != null) {
            val shift = workObj.optJSONObject("my_shift")
            val slipsArr = workObj.optJSONArray("slips") ?: JSONArray()
            val slips = buildList(slipsArr.length()) {
                for (i in 0 until slipsArr.length()) {
                    val s = slipsArr.optJSONObject(i) ?: continue
                    add(
                        MilkTeaSlip(
                            shiftId = s.optStringClean("shift_id"),
                            shopName = s.optStringClean("shop_name"),
                            employerName = s.optJSONObject("employer")?.optStringClean("name").orEmpty(),
                            beginAtSec = normalizeTimestampSec(s.optLongFlex("begin_at")),
                            endAtSec = normalizeTimestampSec(s.optLongFlex("end_at")),
                            finalWage = s.optJSONObject("wages")?.optLongFlex("final_wage") ?: 0L,
                            wageStatus = s.optIntFlex("wage_status"),
                        )
                    )
                }
            }
            val interArr = obj.optJSONArray("interactions") ?: JSONArray()
            val interactions = buildList(interArr.length()) {
                for (i in 0 until interArr.length()) {
                    val it = interArr.optJSONObject(i) ?: continue
                    add(
                        MilkTeaInteraction(
                            id = it.optIntFlex("interaction_id"),
                            name = it.optStringClean("name"),
                            description = cleanHtml(it.optStringClean("description")),
                            image = it.optStringClean("image"),
                            ready = it.optBoolFlex("ready"),
                        )
                    )
                }
            }
            MilkTeaWorkState(
                talentName = workObj.optStringClean("talent_name"),
                talentDesc = cleanHtml(workObj.optStringClean("talentDescription")),
                hasShift = shift != null,
                shiftId = shift?.optStringClean("shift_id").orEmpty(),
                shopName = shift?.optStringClean("shop_name").orEmpty(),
                employerName = shift?.optJSONObject("employer")?.optStringClean("name").orEmpty(),
                employerAvatar = shift?.optJSONObject("employer")?.optStringClean("avatar").orEmpty(),
                beginAtSec = normalizeTimestampSec(shift?.optLongFlex("begin_at") ?: 0L),
                endAtSec = normalizeTimestampSec(shift?.optLongFlex("end_at") ?: 0L),
                baseWage = if (shift != null && shift.has("baseWage") && !shift.isNull("baseWage")) shift.optLongFlex("baseWage") else null,
                interactLimit = shift?.optIntFlex("interact_limit") ?: 0,
                interactCount = shift?.optIntFlex("interact_count") ?: 0,
                interactDone = workObj.optBoolFlex("interact_done"),
                slips = slips,
                interactions = interactions,
            )
        } else null

        val actionAvail = obj.optJSONObject("actionAvailability")
        return MilkTeaState(
            id = obj.optStringClean("id"),
            active = obj.optBoolFlex("active"),
            settling = obj.optBoolFlex("settling"),
            serverTimeSec = normalizeTimestampSec(obj.optLongFlex("serverTime", wallClockMillis() / 1000L)),
            receivedAtElapsedMs = fetchedAt,
            startTimeSec = normalizeTimestampSec(obj.optLongFlex("startTime")),
            endTimeSec = normalizeTimestampSec(obj.optLongFlex("endTime")),
            opened = summary.optLongFlex("shop_open_at") > 0L,
            shopName = summary.optStringClean("shop_name"),
            level = summary.optIntFlex("level"),
            totalRevenue = summary.optLongFlex("total_revenue"),
            nextLevelRevenue = shop?.optLongFlex("next_level_revenue") ?: 0L,
            canUpgrade = summary.optBoolFlex("can_upgrade"),
            hireDisabled = summary.optBoolFlex("hire_disabled"),
            renameCdUntilSec = normalizeTimestampSec(summary.optLongFlex("rename_cd_until_at")),
            effectiveCupsPerHour = shop?.optIntFlex("effective_cups_per_hour") ?: 0,
            cashierElapsedSec = shop?.optLongFlex("cashier_elapsed_sec") ?: 0L,
            cashierCapacitySec = shop?.optLongFlex("cashier_capacity_sec") ?: 0L,
            salesItemId = shop?.optIntFlex("sales_item_id", 1042) ?: 1042,
            boostCupsPerUse = shop?.optIntFlex("boost_cups_per_use") ?: 0,
            boostCoinPrices = boostCoins,
            employeeSlotCount = obj.optIntFlex("employeeSlotCount"),
            balances = balances,
            recommendedNames = parseStringList(obj.optJSONArray("recommendedNames")),
            pendingRewards = parseCommerceItems(shop?.optJSONArray("pending_rewards")),
            pendingCups = pendingCups,
            slots = slots,
            recipes = recipes,
            fruits = fruits,
            goods = goods,
            canCraft = actionAvail?.optBoolFlex("craft", true) ?: true,
            canExchange = actionAvail?.optBoolFlex("exchange", true) ?: true,
            fundUnlocked = fundObj?.optBoolFlex("unlocked") ?: false,
            fundCosts = parseCommerceItems(fundObj?.optJSONArray("costs")),
            fundTiers = fundTiers,
            levels = levels,
            hireWageTotal = hireObj?.optLongFlex("wage_total") ?: 0L,
            hireDurations = durations,
            candidates = candidates,
            work = work,
            rules = parseStringList(obj.optJSONArray("rules")).map { cleanHtml(it) }.filter { it.isNotEmpty() },
        )
    }

    fun parseShareRewardState(obj: JSONObject): ShareRewardState {
        val fetchedAt = com.qqfarm.mobile.platform.monotonicMillis()
        val mArr = obj.optJSONArray("milestones") ?: JSONArray()
        val milestones = buildList(mArr.length()) {
            for (i in 0 until mArr.length()) {
                val m = mArr.optJSONObject(i) ?: continue
                add(
                    ShareRewardMilestone(
                        id = m.optStringClean("id"),
                        threshold = m.optLongFlex("threshold"),
                        state = m.optIntFlex("state"),
                        rewards = parseCommerceItems(m.optJSONArray("rewards")),
                    )
                )
            }
        }
        return ShareRewardState(
            id = obj.optStringClean("id"),
            title = obj.optStringClean("title", "快乐不独享"),
            active = obj.optBoolFlex("active"),
            startTimeMs = normalizeTimestampMs(obj.optLongFlex("startTime")),
            endTimeMs = normalizeTimestampMs(obj.optLongFlex("endTime")),
            serverTimeMs = normalizeTimestampMs(obj.optLongFlex("serverTime", wallClockMillis())),
            receivedAtElapsedMs = fetchedAt,
            score = obj.optLongFlex("score"),
            dailyReward = obj.optIntFlex("dailyReward"),
            firstShareReward = obj.optIntFlex("firstShareReward"),
            firstShareAwarded = obj.optBoolFlex("firstShareAwarded"),
            canShare = obj.optBoolFlex("canShare"),
            canClaimDaily = obj.optBoolFlex("canClaimDaily"),
            canClaimMilestones = obj.optBoolFlex("canClaimMilestones"),
            claimedCount = obj.optIntFlex("claimedCount"),
            claimLimit = obj.optIntFlex("claimLimit"),
            poolClaimedCount = obj.optIntFlex("poolClaimedCount"),
            poolClaimLimit = obj.optIntFlex("poolClaimLimit"),
            milestones = milestones,
            rules = parseStringList(obj.optJSONArray("rules")),
        )
    }

    fun parseAutumnWishState(obj: JSONObject): AutumnWishState {
        val fetchedAt = com.qqfarm.mobile.platform.monotonicMillis()
        val choicesArr = obj.optJSONArray("choices") ?: JSONArray()
        val choices = buildList(choicesArr.length()) {
            for (i in 0 until choicesArr.length()) {
                val c = choicesArr.optJSONObject(i) ?: continue
                add(c.optIntFlex("id") to c.optStringClean("name"))
            }
        }
        val daysArr = obj.optJSONArray("rewardDays") ?: JSONArray()
        val rewardDays = buildList(daysArr.length()) {
            for (i in 0 until daysArr.length()) {
                val d = daysArr.optJSONObject(i) ?: continue
                add(d.optIntFlex("day") to parseCommerceItem(d.optJSONObject("reward")))
            }
        }
        val pending = obj.optJSONObject("pending")
        return AutumnWishState(
            title = obj.optStringClean("title", "秋祈良愿"),
            active = obj.optBoolFlex("active"),
            startTimeMs = normalizeTimestampMs(obj.optLongFlex("startTime")),
            endTimeMs = normalizeTimestampMs(obj.optLongFlex("endTime")),
            serverTimeMs = normalizeTimestampMs(obj.optLongFlex("serverTime", wallClockMillis())),
            receivedAtElapsedMs = fetchedAt,
            day = obj.optIntFlex("day"),
            remaining = obj.optIntFlex("remaining"),
            canDraw = obj.optBoolFlex("canDraw"),
            canClaim = obj.optBoolFlex("canClaim"),
            choices = choices,
            pendingChooseId = pending?.optIntFlex("chooseId") ?: 0,
            pendingText = pending?.optStringClean("text").orEmpty(),
            pendingRewards = parseCommerceItems(pending?.optJSONArray("rewards")),
            rewardDays = rewardDays,
            rules = parseStringList(obj.optJSONArray("rules")),
        )
    }

    fun parseStellarSnapshot(obj: JSONObject): StellarSnapshot {
        val fetchedAt = com.qqfarm.mobile.platform.monotonicMillis()
        val season = obj.optJSONObject("season")
        val pass = season?.optJSONObject("pass")
        val constellation = obj.optJSONObject("constellation")
        val shop = obj.optJSONObject("shop")
        val solar = obj.optJSONObject("solarTerms")
        val actions = obj.optJSONObject("actions")

        val passNodesArr = pass?.optJSONArray("nodes") ?: JSONArray()
        val passNodes = buildList(passNodesArr.length()) {
            for (i in 0 until passNodesArr.length()) {
                val n = passNodesArr.optJSONObject(i) ?: continue
                add(
                    StellarPassNode(
                        id = n.optStringClean("id"),
                        level = n.optStringClean("level"),
                        claimed = n.optBoolFlex("claimed"),
                        claimable = n.optBoolFlex("claimable"),
                        locked = n.optBoolFlex("locked"),
                        rewards = parseCommerceItems(n.optJSONArray("rewards")),
                    )
                )
            }
        }

        val groupsArr = constellation?.optJSONArray("groups") ?: JSONArray()
        val groups = buildList(groupsArr.length()) {
            for (i in 0 until groupsArr.length()) {
                val g = groupsArr.optJSONObject(i) ?: continue
                add(
                    StellarConstellationGroup(
                        id = g.optStringClean("id"),
                        name = g.optStringClean("name"),
                        explain = g.optStringClean("explain"),
                        visualState = g.optStringClean("visualState"),
                        rewards = parseCommerceItems(g.optJSONArray("rewards")),
                    )
                )
            }
        }

        val shopCurrencies = shop?.optJSONArray("currencies")
        val c0 = shopCurrencies?.optJSONObject(0)
        val goodsArr = shop?.optJSONArray("goods") ?: JSONArray()
        val shopGoods = buildList(goodsArr.length()) {
            for (i in 0 until goodsArr.length()) {
                val g = goodsArr.optJSONObject(i) ?: continue
                add(
                    StellarShopGoods(
                        id = g.optStringClean("id"),
                        name = g.optStringClean("name"),
                        item = parseCommerceItem(g.optJSONObject("item")),
                        cost = parseCommerceItem(g.optJSONObject("cost")),
                        exchangeable = g.optBoolFlex("exchangeable"),
                        soldOut = g.optBoolFlex("soldOut"),
                        maxExchangeCount = g.optIntFlex("maxExchangeCount"),
                    )
                )
            }
        }

        val termsArr = solar?.optJSONArray("terms") ?: JSONArray()
        val terms = buildList(termsArr.length()) {
            for (i in 0 until termsArr.length()) {
                val t = termsArr.optJSONObject(i) ?: continue
                val status = t.optStringClean("statusCode")
                add(
                    StellarSolarTerm(
                        id = t.optStringClean("id"),
                        name = t.optStringClean("name"),
                        description = t.optStringClean("description"),
                        claimed = status == "3" || t.optBoolFlex("claimed"),
                        claimable = t.optBoolFlex("canClaim") || t.optBoolFlex("claimable"),
                        locked = status == "1" || t.optBoolFlex("locked"),
                        rewards = parseCommerceItems(t.optJSONArray("rewards")),
                    )
                )
            }
        }

        return StellarSnapshot(
            title = season?.optStringClean("title", "千星游记").orEmpty().ifBlank { "千星游记" },
            serverTimeMs = normalizeTimestampMs(obj.optLongFlex("serverTime", wallClockMillis())),
            receivedAtElapsedMs = fetchedAt,
            endTimeMs = normalizeTimestampMs(season?.optLongFlex("endTime") ?: 0L),
            passLevel = pass?.optStringClean("level", "0").orEmpty(),
            passProgress = pass?.optIntFlex("progress") ?: 0,
            passProgressMax = pass?.optIntFlex("progressMax") ?: 0,
            canClaimPass = actions?.optJSONObject("claimPass")?.optBoolFlex("available")
                ?: passNodes.any { it.claimable },
            passNodes = passNodes,
            canLightConstellation = actions?.optJSONObject("lightConstellation")?.optBoolFlex("attemptable")
                ?: groups.any { it.visualState == "lightable" },
            constellationGroups = groups,
            shopCurrencyName = c0?.optStringClean("name", "星砂").orEmpty().ifBlank { "星砂" },
            shopBalance = c0?.optStringClean("balance", "--").orEmpty().ifBlank { "--" },
            shopBalanceKnown = c0?.optBoolFlex("balanceKnown") ?: (shop?.optBoolFlex("balanceKnown") ?: false),
            shopGoods = shopGoods,
            solarTerms = terms,
        )
    }

    fun parseQixiState(obj: JSONObject): QixiState {
        val bal = obj.optJSONObject("balances")
        val bridge = obj.optJSONObject("bridge")
        val stagesArr = bridge?.optJSONArray("stages") ?: JSONArray()
        val stages = buildList(stagesArr.length()) {
            for (i in 0 until stagesArr.length()) {
                val s = stagesArr.optJSONObject(i) ?: continue
                val claimed = s.optBoolFlex("claimed")
                val claimable = s.optBoolFlex("claimable")
                add(
                    SeasonalSimpleStage(
                        id = s.optStringClean("id"),
                        label = "第 ${s.optIntFlex("stage", i + 1)} 阶段",
                        statusText = when {
                            claimed -> "已领取"
                            claimable -> "可领取"
                            else -> "未达成"
                        },
                        claimable = claimable,
                        claimed = claimed,
                        rewards = parseCommerceItems(s.optJSONArray("rewards")),
                    )
                )
            }
        }
        return QixiState(
            title = obj.optStringClean("title", "鹊桥寄情"),
            active = obj.optBoolFlex("active"),
            endTimeMs = normalizeTimestampMs(obj.optLongFlex("endTime")),
            featherBalance = bal?.optStringClean("feather", "--").orEmpty().ifBlank { "--" },
            sachetBalance = bal?.optStringClean("sachet", "--").orEmpty().ifBlank { "--" },
            canClaimBridge = obj.optJSONObject("actions")?.optJSONObject("bridge")?.optBoolFlex("available")
                ?: (bridge?.optBoolFlex("claimable") ?: false),
            stages = stages,
        )
    }

    fun parseQingMeiState(obj: JSONObject): QingMeiState {
        val actions = obj.optJSONObject("actions")
        val dailySeed = obj.optJSONObject("dailySeed")
        val ingArr = obj.optJSONArray("ingredients")
        val firstIng = ingArr?.optJSONObject(0)
        val totals = obj.optJSONArray("quoteTotals")
        val latestGold = if (totals != null && totals.length() > 0) {
            totals.optString(totals.length() - 1)
        } else obj.optStringClean("baseGold", "0")
        return QingMeiState(
            title = obj.optStringClean("name", "青酿换万金"),
            balance = obj.optStringClean("balance", "--").ifBlank { "--" },
            dailySeedClaimed = dailySeed?.optBoolFlex("claimed") ?: false,
            started = obj.optBoolFlex("started"),
            currentRound = obj.optIntFlex("currentRound"),
            maxRounds = obj.optIntFlex("maxRounds", 3),
            finished = obj.optBoolFlex("finished"),
            currentQuoteGold = latestGold,
            canStart = actions?.optJSONObject("start")?.optBoolFlex("available") ?: false,
            canContinue = actions?.optJSONObject("continue")?.optBoolFlex("available") ?: false,
            canSettle = actions?.optJSONObject("settle")?.optBoolFlex("available") ?: false,
            ingredientsCount = ingArr?.length() ?: 0,
            firstIngredientUid = firstIng?.optStringClean("uid").orEmpty(),
            firstIngredientCount = firstIng?.optLongFlex("count") ?: 0L,
        )
    }

    fun parseCharityState(obj: JSONObject): CharityState {
        val actions = obj.optJSONObject("actions")
        val pArr = obj.optJSONArray("progressRewards") ?: JSONArray()
        val stages = buildList(pArr.length()) {
            for (i in 0 until pArr.length()) {
                val p = pArr.optJSONObject(i) ?: continue
                val claimed = p.optBoolFlex("claimed")
                val claimable = p.optBoolFlex("claimable")
                val target = p.optStringClean("target")
                add(
                    SeasonalSimpleStage(
                        id = target,
                        label = "$target 爱心",
                        statusText = when {
                            claimed -> "已领取"
                            claimable -> "可领取"
                            else -> "未达成"
                        },
                        claimable = claimable,
                        claimed = claimed,
                        rewards = listOfNotNull(p.optJSONObject("reward")?.let { parseCommerceItem(it) }),
                    )
                )
            }
        }
        return CharityState(
            title = obj.optStringClean("title", "公益小红花"),
            active = obj.optBoolFlex("active"),
            endTimeMs = normalizeTimestampMs(obj.optLongFlex("endTime")),
            loveBalance = obj.optStringClean("loveBalance", "0"),
            donatedLove = obj.optStringClean("donatedLove", "0"),
            agreementAccepted = obj.optStringClean("agreementStatus") == "1",
            canClaimSeeds = actions?.optJSONObject("claimSeeds")?.optBoolFlex("available") ?: false,
            canShare = actions?.optJSONObject("share")?.optBoolFlex("available") ?: false,
            canDonateLove = actions?.optJSONObject("donateLove")?.optBoolFlex("available") ?: false,
            canClaimDailyGift = actions?.optJSONObject("claimDailyGift")?.optBoolFlex("attemptable") ?: false,
            stages = stages,
        )
    }

    fun parseWeatherState(obj: JSONObject): WeatherState {
        val inventory = parseCommerceItems(obj.optJSONArray("inventory")).associateBy { it.id }
        val activity = obj.optJSONObject("activity")
        val actions = obj.optJSONObject("actions")
        val own = obj.optJSONObject("ownWeather")
        val activeName = if (own?.optBoolFlex("active") == true) {
            if (own.optBoolFlex("isThunderstorm")) "雷雨天气中" else own.optStringClean("typeName").ifBlank { "特殊天气中" }
        } else "晴朗"
        return WeatherState(
            title = activity?.optStringClean("name", "雨落成诗") ?: "雨落成诗",
            active = obj.optBoolFlex("active"),
            endTimeMs = normalizeTimestampMs(activity?.optLongFlex("endTime") ?: 0L),
            badgeBalance = inventory[WEATHER_BADGE_ID]?.count?.toString() ?: "--",
            collectionBottleCount = inventory[WEATHER_COLLECTOR_BOTTLE_ID]?.count?.toString() ?: "--",
            rainBottleCount = inventory[WEATHER_SUMMON_BOTTLE_ID]?.count?.toString() ?: "--",
            activeWeatherName = activeName,
            canExchangeCollector = actions?.optJSONObject("exchangeCollector")?.optBoolFlex("enabled") ?: false,
            canSummonRain = actions?.optJSONObject("summonThunderstorm")?.optBoolFlex("enabled") ?: false,
            nextResearchNodeId = actions?.optJSONObject("advanceResearch")?.optStringClean("nodeId").orEmpty(),
            canAdvanceResearch = actions?.optJSONObject("advanceResearch")?.optBoolFlex("enabled") ?: false,
        )
    }

    // ---- Illustrated Book & Collections ----

    private fun JSONObject.optDoubleFlex(key: String, fallback: Double = 0.0): Double {
        if (!has(key) || isNull(key)) return fallback
        return when (val v = opt(key)) {
            is Number -> v.toDouble()
            is String -> v.trim().toDoubleOrNull() ?: fallback
            else -> fallback
        }
    }

    private fun parseBookCommerceItems(arr: JSONArray?): List<CommerceItem> {
        if (arr == null) return emptyList()
        return buildList(arr.length()) {
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val id = obj.optIntFlex("itemId").let { if (it != 0) it else obj.optIntFlex("id") }
                add(
                    CommerceItem(
                        id = id,
                        count = obj.optLongFlex("count"),
                        name = obj.optStringClean("name"),
                        image = obj.optStringClean("image"),
                        rarity = obj.optIntFlex("rarity"),
                        description = obj.optStringClean("description"),
                    )
                )
            }
        }
    }

    private fun parseIllustratedBuffs(arr: JSONArray?): List<IllustratedBuff> {
        if (arr == null) return emptyList()
        return buildList(arr.length()) {
            for (i in 0 until arr.length()) {
                val b = arr.optJSONObject(i) ?: continue
                add(
                    IllustratedBuff(
                        id = b.optIntFlex("id"),
                        level = b.optIntFlex("level"),
                        name = b.optStringClean("name"),
                        value = b.optIntFlex("value"),
                        valueType = b.optStringClean("valueType"),
                    )
                )
            }
        }
    }

    private fun parseIllustratedBook(obj: JSONObject, defaultType: Int): IllustratedBook {
        val levelsArr = obj.optJSONArray("levels") ?: JSONArray()
        val levels = buildList(levelsArr.length()) {
            for (i in 0 until levelsArr.length()) {
                val l = levelsArr.optJSONObject(i) ?: continue
                add(
                    IllustratedLevel(
                        level = l.optIntFlex("level"),
                        progress = l.optIntFlex("progress"),
                        claimed = l.optBoolFlex("claimed"),
                        canClaim = l.optBoolFlex("canClaim"),
                        rewards = parseBookCommerceItems(l.optJSONArray("rewards")),
                    )
                )
            }
        }
        val itemsArr = obj.optJSONArray("items") ?: JSONArray()
        val items = buildList(itemsArr.length()) {
            for (i in 0 until itemsArr.length()) {
                val it = itemsArr.optJSONObject(i) ?: continue
                val seedObj = it.optJSONObject("shopSeed")
                val shopSeed = if (seedObj != null) {
                    IllustratedShopSeed(
                        seedId = seedObj.optIntFlex("seedId"),
                        goodsId = seedObj.optIntFlex("goodsId"),
                        name = seedObj.optStringClean("name"),
                        price = seedObj.optLongFlex("price"),
                        requiredLevel = seedObj.optIntFlex("requiredLevel"),
                        locked = seedObj.optBoolFlex("locked"),
                        soldOut = seedObj.optBoolFlex("soldOut"),
                        itemCount = seedObj.optIntFlex("itemCount", 1).coerceAtLeast(1),
                        remainingPurchaseCount = if (seedObj.isNull("remainingPurchaseCount")) {
                            null
                        } else {
                            seedObj.optIntFlex("remainingPurchaseCount")
                        },
                        unknownMeta = seedObj.optBoolFlex("unknownMeta"),
                        requiresUnlockCard = seedObj.optBoolFlex("requiresUnlockCard"),
                    )
                } else null
                add(
                    IllustratedBookItem(
                        seedId = it.optIntFlex("seedId"),
                        name = it.optStringClean("name"),
                        image = it.optStringClean("image"),
                        rewardCategory = it.optIntFlex("rewardCategory"),
                        group = it.optStringClean("group"),
                        sort = it.optIntFlex("sort"),
                        cropCategory = it.optStringClean("cropCategory"),
                        unlocked = it.optBoolFlex("unlocked"),
                        progress = it.optIntFlex("progress"),
                        isNew = it.optBoolFlex("isNew"),
                        shopSeed = shopSeed,
                    )
                )
            }
        }
        return IllustratedBook(
            type = obj.optIntFlex("type", defaultType),
            level = obj.optIntFlex("level"),
            progress = obj.optIntFlex("progress"),
            nextLevelProgress = obj.optIntFlex("nextLevelProgress"),
            claimable = obj.optBoolFlex("claimable"),
            levels = levels,
            buffs = parseIllustratedBuffs(obj.optJSONArray("buffs")),
            currentBuffs = parseIllustratedBuffs(obj.optJSONArray("currentBuffs")),
            items = items,
        )
    }

    fun parseIllustratedSnapshot(obj: JSONObject): IllustratedSnapshot {
        val crop = obj.optJSONObject("crop") ?: throw ApiException("服务端未返回作物图鉴")
        val mutant = obj.optJSONObject("mutant") ?: throw ApiException("服务端未返回超变图鉴")
        return IllustratedSnapshot(
            crop = parseIllustratedBook(crop, defaultType = 1),
            mutant = parseIllustratedBook(mutant, defaultType = 2),
            updatedAt = obj.optLongFlex("updatedAt"),
        )
    }

    // Protobuf Long 在线程间传递后，会以 low/high 对象出现在购买回执里。
    private fun JSONObject.optSeedReceiptLong(key: String): Long {
        val words = optJSONObject(key) ?: return optLongFlex(key)
        val low = words.opt("low") as? Int ?: return 0L
        val high = words.opt("high") as? Int ?: return 0L
        return (high.toLong() shl 32) or (low.toLong() and 0xffff_ffffL)
    }

    fun parseIllustratedSeedReceipt(obj: JSONObject?, seedId: Int): Long {
        val items = obj?.optJSONArray("get_items")
        val received = if (items == null) 0L else (0 until items.length()).sumOf { index ->
            val item = items.optJSONObject(index)
            if (item?.optSeedReceiptLong("id") == seedId.toLong()) {
                item.optSeedReceiptLong("count").coerceAtLeast(0L)
            } else 0L
        }
        if (received <= 0L) throw ApiException("种子购买结果未确认，请刷新背包后核对")
        return received
    }

    fun parseSeedShopItems(arr: JSONArray?): List<IllustratedBookItem> {
        if (arr == null) return emptyList()
        return buildList(arr.length()) {
            for (i in 0 until arr.length()) {
                val s = arr.optJSONObject(i) ?: continue
                val seedId = s.optIntFlex("seedId", s.optIntFlex("id"))
                val goodsId = s.optIntFlex("goodsId")
                val name = s.optStringClean("name")
                if (seedId <= 0 || goodsId <= 0 || name.isBlank() || s.optBoolFlex("unknownMeta")) continue
                val remaining = if (s.has("remainingPurchaseCount") && !s.isNull("remainingPurchaseCount")) {
                    s.optIntFlex("remainingPurchaseCount")
                } else null
                val shopSeed = IllustratedShopSeed(
                    seedId = seedId,
                    goodsId = goodsId,
                    name = name,
                    price = s.optLongFlex("price"),
                    requiredLevel = s.optIntFlex("requiredLevel", s.optIntFlex("level")),
                    locked = s.optBoolFlex("locked"),
                    soldOut = s.optBoolFlex("soldOut"),
                    itemCount = s.optIntFlex("itemCount", 1).coerceAtLeast(1),
                    remainingPurchaseCount = remaining,
                    requiresUnlockCard = s.optBoolFlex("requiresUnlockCard"),
                )
                add(
                    IllustratedBookItem(
                        seedId = seedId,
                        name = name,
                        image = s.optStringClean("image").ifBlank { "/game-config/seed_images_named/seed_images/$seedId.png" },
                        unlocked = !shopSeed.locked,
                        shopSeed = shopSeed,
                    )
                )
            }
        }
    }

    fun parseIllustratedClaimResult(obj: JSONObject): IllustratedClaimResult {
        val claimedCount = obj.optJSONArray("claimedLevels")?.length() ?: 0
        if (claimedCount == 0) throw ApiException("图鉴领取结果未确认，请刷新后核对")
        val rewards = parseBookCommerceItems(obj.optJSONArray("rewards"))
        val snapshot = obj.optJSONObject("snapshot")?.let(::parseIllustratedSnapshot)
        val refreshRequired = obj.optBoolFlex("refreshRequired")
        val rewardSummary = rewards.joinToString("、") { "${it.name} ×${it.count}" }.ifBlank { "已领取 $claimedCount 档图鉴奖励" }
        val message = if (refreshRequired) "$rewardSummary（请刷新确认最新进度）" else rewardSummary
        return IllustratedClaimResult(
            rewards = rewards,
            snapshot = snapshot,
            refreshRequired = refreshRequired,
            message = message,
        )
    }

    private fun parseCollectionBuffs(arr: JSONArray?): List<CollectionBuff> {
        if (arr == null) return emptyList()
        return buildList(arr.length()) {
            for (i in 0 until arr.length()) {
                val b = arr.optJSONObject(i) ?: continue
                add(
                    CollectionBuff(
                        id = b.optIntFlex("id"),
                        name = b.optStringClean("name"),
                        value = b.optDoubleFlex("value"),
                        description = b.optStringClean("description"),
                    )
                )
            }
        }
    }

    private fun parseMutationRules(arr: JSONArray?): List<CollectionMutationRule> {
        if (arr == null) return emptyList()
        return buildList(arr.length()) {
            for (i in 0 until arr.length()) {
                val r = arr.optJSONObject(i) ?: continue
                add(
                    CollectionMutationRule(
                        name = r.optStringClean("name"),
                        probability = r.optDoubleFlex("probability"),
                        guarantee = r.optIntFlex("guarantee"),
                    )
                )
            }
        }
    }

    fun parseCollectionSnapshot(obj: JSONObject): CollectionSnapshot {
        val milestonesArr = obj.optJSONArray("milestones") ?: JSONArray()
        val milestones = buildList(milestonesArr.length()) {
            for (i in 0 until milestonesArr.length()) {
                val m = milestonesArr.optJSONObject(i) ?: continue
                add(
                    CollectionMilestone(
                        level = m.optIntFlex("level"),
                        score = m.optLongFlex("score"),
                        claimed = m.optBoolFlex("claimed"),
                        reached = m.optBoolFlex("reached"),
                        rewards = parseBookCommerceItems(m.optJSONArray("rewards")),
                        buffs = parseCollectionBuffs(m.optJSONArray("buffs")),
                    )
                )
            }
        }
        val seriesArr = obj.optJSONArray("series") ?: throw ApiException("服务端未返回藏品系列")
        val series = buildList(seriesArr.length()) {
            for (i in 0 until seriesArr.length()) {
                val s = seriesArr.optJSONObject(i) ?: continue
                val itemsArr = s.optJSONArray("items") ?: JSONArray()
                val items = buildList(itemsArr.length()) {
                    for (j in 0 until itemsArr.length()) {
                        val it = itemsArr.optJSONObject(j) ?: continue
                        add(
                            CollectionItemEntry(
                                id = it.optIntFlex("id"),
                                name = it.optStringClean("name"),
                                description = it.optStringClean("description"),
                                image = it.optStringClean("image"),
                                starLevel = it.optIntFlex("starLevel"),
                                maxStarLevel = it.optIntFlex("maxStarLevel", 5).coerceAtLeast(1),
                                ownCount = it.optLongFlex("ownCount"),
                                activated = it.optBoolFlex("activated"),
                                isNew = it.optBoolFlex("isNew"),
                                score = it.optLongFlex("score"),
                                displayed = it.optBoolFlex("displayed"),
                                wished = it.optBoolFlex("wished"),
                                requirements = parseStringList(it.optJSONArray("requirements")),
                                nextStarCosts = parseBookCommerceItems(it.optJSONArray("nextStarCosts")),
                                unlocked = it.optBoolFlex("unlocked"),
                                canUpgrade = it.optBoolFlex("canUpgrade"),
                            )
                        )
                    }
                }
                add(
                    CollectionSeries(
                        id = s.optIntFlex("id"),
                        name = s.optStringClean("name"),
                        rarity = s.optIntFlex("rarity", 1),
                        rarityName = s.optStringClean("rarityName"),
                        description = s.optStringClean("description"),
                        requirements = parseStringList(s.optJSONArray("requirements")),
                        mutationRules = parseMutationRules(s.optJSONArray("mutationRules")),
                        items = items,
                        activatedCount = s.optIntFlex("activatedCount"),
                        totalCount = s.optIntFlex("totalCount"),
                    )
                )
            }
        }
        return CollectionSnapshot(
            score = obj.optLongFlex("score"),
            claimedScore = obj.optLongFlex("claimedScore"),
            activatedCount = obj.optIntFlex("activatedCount"),
            totalCount = obj.optIntFlex("totalCount"),
            nextRewardScore = if (obj.isNull("nextRewardScore")) null else obj.optLongFlex("nextRewardScore"),
            currentBuffs = parseCollectionBuffs(obj.optJSONArray("currentBuffs")),
            milestones = milestones,
            series = series,
            displayItemId = obj.optJSONObject("display")?.optStringClean("itemId", "0").orEmpty().ifBlank { "0" },
        )
    }

    fun parseCollectionOperateResult(obj: JSONObject, defaultActionLabel: String): CollectionOperateResult {
        val skipped = obj.optBoolFlex("skipped")
        val snapshot = obj.optJSONObject("snapshot")?.let(::parseCollectionSnapshot)
            ?: throw ApiException("藏品操作结果未确认，请刷新后核对")
        val rewards = parseBookCommerceItems(obj.optJSONArray("rewards"))
        val message = when {
            rewards.isNotEmpty() -> rewards.joinToString("、") { "${it.name} ×${it.count}" }
            skipped -> "当前状态已更新，无需重复操作"
            else -> defaultActionLabel
        }
        return CollectionOperateResult(
            skipped = skipped,
            snapshot = snapshot,
            rewards = rewards,
            message = message,
        )
    }

    private fun parseMutantEffects(arr: JSONArray?): List<MutantCatalogEffect> {
        if (arr == null) return emptyList()
        return buildList(arr.length()) {
            for (i in 0 until arr.length()) {
                val e = arr.optJSONObject(i) ?: continue
                val probsArr = e.optJSONArray("probabilities") ?: JSONArray()
                val probs = buildList(probsArr.length()) {
                    for (j in 0 until probsArr.length()) {
                        val p = probsArr.optJSONObject(j) ?: continue
                        add(
                            MutantProbability(
                                rarity = p.optIntFlex("rarity"),
                                percent = p.optDoubleFlex("percent"),
                            )
                        )
                    }
                }
                add(
                    MutantCatalogEffect(
                        id = e.optIntFlex("id"),
                        name = e.optStringClean("name"),
                        image = e.optStringClean("image"),
                        effect = e.optStringClean("effect"),
                        description = e.optStringClean("description"),
                        probabilities = probs,
                    )
                )
            }
        }
    }

    fun parseMutationCatalog(obj: JSONObject): MutationCatalogSnapshot {
        val colsArr = obj.optJSONArray("collections") ?: JSONArray()
        val collections = buildList(colsArr.length()) {
            for (i in 0 until colsArr.length()) {
                val c = colsArr.optJSONObject(i) ?: continue
                add(
                    MutationCatalogCollectionSeries(
                        id = c.optIntFlex("id"),
                        name = c.optStringClean("name"),
                        rarityName = c.optStringClean("rarityName"),
                        description = c.optStringClean("description"),
                        mutationRules = parseMutationRules(c.optJSONArray("mutationRules")),
                        items = parseBookCommerceItems(c.optJSONArray("items")),
                    )
                )
            }
        }
        return MutationCatalogSnapshot(
            normal = parseMutantEffects(obj.optJSONArray("normal")),
            decoration = parseMutantEffects(obj.optJSONArray("decoration")),
            collections = collections,
        )
    }

    fun parseSystemUpdateStatus(obj: JSONObject): SystemUpdateStatus {
        val dirtyArr = obj.optJSONArray("dirtyFiles") ?: JSONArray()
        val dirtyFiles = buildList(dirtyArr.length()) {
            for (i in 0 until dirtyArr.length()) {
                val item = dirtyArr.opt(i)?.toString()?.trim().orEmpty()
                if (item.isNotEmpty() && item != "null") add(item)
            }
        }
        val commitsArr = obj.optJSONArray("incomingCommits") ?: JSONArray()
        val incomingCommits = buildList(commitsArr.length()) {
            for (i in 0 until commitsArr.length()) {
                val c = commitsArr.optJSONObject(i) ?: continue
                add(
                    SystemUpdateCommit(
                        hash = c.optStringClean("hash"),
                        shortHash = c.optStringClean("shortHash"),
                        subject = c.optStringClean("subject"),
                        author = c.optStringClean("author"),
                        timestampMs = c.optLongFlex("timestampMs"),
                    )
                )
            }
        }
        val releases = obj.optJSONArray("releases")?.let { rows ->
            buildList(rows.length()) {
                for (i in 0 until rows.length()) {
                    val row = rows.optJSONObject(i) ?: continue
                    val id = row.optStringClean("releaseId")
                    if (id.isBlank()) continue
                    val compat = row.optJSONObject("compat")
                    val compatible = compat?.optBoolFlex("compatible") == true
                    val active = row.optBoolFlex("isActive")
                    add(SystemReleaseBackup(
                        releaseId = id,
                        shortCommit = row.optStringClean("shortCommit"),
                        subject = row.optStringClean("commitSubject"),
                        createdAt = row.optLongFlex("createdAt"),
                        pairedWebReleaseId = row.optStringClean("pairedWebReleaseId"),
                        isActive = active,
                        isPrevious = row.optBoolFlex("isPrevious"),
                        compatible = compatible,
                        compatibilityReasons = parseStringList(compat?.optJSONArray("reasons")),
                        canRollback = !active && compatible && row.optBoolFlex("canRollback"),
                        canDelete = !active && row.optBoolFlex("canDelete"),
                        isOutdated = compat?.optBoolFlex("outdated") == true,
                        sizeBytes = row.optLongFlex("sizeBytes"),
                    ))
                }
            }
        }
        val jobObj = obj.optJSONObject("job")
        val job = if (jobObj != null) {
            val linesArr = jobObj.optJSONArray("recentLines") ?: JSONArray()
            val recentLines = buildList(linesArr.length()) {
                for (i in 0 until linesArr.length()) {
                    val line = linesArr.opt(i)?.toString()?.trimEnd().orEmpty()
                    if (line.isNotEmpty() && line != "null") add(line)
                }
            }
            SystemUpdateJob(
                id = jobObj.optStringClean("id"),
                status = jobObj.optStringClean("status", "running"),
                phase = jobObj.optStringClean("phase"),
                detail = jobObj.optStringClean("detail"),
                startedAt = jobObj.optLongFlex("startedAt"),
                updatedAt = jobObj.optLongFlex("updatedAt"),
                finishedAt = jobObj.optLongFlex("finishedAt"),
                error = jobObj.optStringClean("error"),
                fromCommit = jobObj.optStringClean("fromCommit"),
                targetCommit = jobObj.optStringClean("targetCommit"),
                recentLines = recentLines,
            )
        } else null

        val lastCheckedAt = if (!obj.has("lastCheckedAt") || obj.isNull("lastCheckedAt")) {
            null
        } else {
            obj.optLongFlex("lastCheckedAt").takeIf { it > 0L }
        }
        val lastPulledAt = if (!obj.has("lastPulledAt") || obj.isNull("lastPulledAt")) {
            null
        } else {
            obj.optLongFlex("lastPulledAt").takeIf { it > 0L }
        }

        return SystemUpdateStatus(
            mode = obj.optStringClean("mode", "unknown"),
            gitSupported = obj.optBoolFlex("gitSupported"),
            githubTokenConfigured = obj.optBoolNullable("githubTokenConfigured"),
            unsupportedReason = obj.optStringClean("unsupportedReason"),
            applySupported = obj.optBoolFlex("applySupported"),
            applyUnsupportedReason = obj.optStringClean("applyUnsupportedReason"),
            branch = obj.optStringClean("branch"),
            detached = obj.optBoolFlex("detached"),
            commit = obj.optStringClean("commit"),
            shortCommit = obj.optStringClean("shortCommit"),
            commitSubject = obj.optStringClean("commitSubject"),
            startupCommit = obj.optStringClean("startupCommit"),
            needsApply = obj.optBoolFlex("needsApply"),
            upstream = obj.optStringClean("upstream"),
            remote = obj.optStringClean("remote"),
            remoteBranch = obj.optStringClean("remoteBranch"),
            behindCount = obj.optIntFlex("behindCount"),
            aheadCount = obj.optIntFlex("aheadCount"),
            dirtyFiles = dirtyFiles,
            incomingCommits = incomingCommits,
            canCheck = obj.optBoolFlex("canCheck"),
            canPull = obj.optBoolFlex("canPull"),
            canApply = obj.optBoolFlex("canApply"),
            deployBusy = obj.optBoolFlex("deployBusy"),
            lastCheckedAt = lastCheckedAt,
            lastCheckError = obj.optStringClean("lastCheckError"),
            lastPulledAt = lastPulledAt,
            job = job,
            releases = releases,
            releaseCleanup = obj.optJSONObject("releasesSummary")?.let { summary ->
                SystemReleaseCleanupSummary(
                    orphanHostCount = summary.optIntFlex("orphanHostCount"),
                    orphanWebCount = summary.optIntFlex("orphanWebCount"),
                    orphanSizeBytes = summary.optLongFlex("orphanSizeBytes"),
                )
            },
            keepBackups = obj.optBoolNullable("keepBackups"),
        )
    }

    fun parseSystemReleaseCleanupResult(obj: JSONObject): SystemReleaseCleanupResult {
        val deletedReleases = obj.optJSONArray("deletedReleaseIds")
        val deletedWeb = obj.optJSONArray("deletedWebIds")
        val deletedHosts = obj.optJSONArray("deletedHostIds")
        val freed = obj.optLongFlex("freedBytes", fallback = -1L)
        val status = obj.optJSONObject("status")
        if (deletedReleases == null || deletedWeb == null || deletedHosts == null ||
            freed < 0 || status?.optJSONArray("releases") == null
        ) {
            throw ApiException("服务端未确认清理结果，请刷新备份列表核对")
        }
        return SystemReleaseCleanupResult(
            deletedReleaseIds = parseStringList(deletedReleases),
            deletedWebIds = parseStringList(deletedWeb),
            deletedHostIds = parseStringList(deletedHosts),
            freedBytes = freed,
            status = parseSystemUpdateStatus(status),
        )
    }

    fun parseSystemUpdatePullResult(obj: JSONObject): SystemUpdatePullResult {
        val statusObj = obj.optJSONObject("status")
            ?: throw ApiException("服务端未返回更新状态")
        return SystemUpdatePullResult(
            pulled = obj.optBoolFlex("pulled"),
            fromCommit = obj.optStringClean("fromCommit"),
            toCommit = obj.optStringClean("toCommit"),
            status = parseSystemUpdateStatus(statusObj),
        )
    }

    private fun normalizePushEntry(
        rawChannel: String,
        enabled: Boolean,
        endpoint: String,
        token: String,
        secret: String,
    ): PushChannelEntry? {
        val ch = rawChannel.trim().lowercase()
        val spec = findPushChannelSpec(ch) ?: return null
        val ep = if (spec.isDingTalk) normalizeDingTalkEndpoint(endpoint, token) else endpoint.trim()
        val tk = if (spec.isDingTalk) "" else token.trim()
        val entry = PushChannelEntry(
            channel = ch,
            enabled = enabled,
            endpoint = ep,
            token = tk,
            secret = secret.trim(),
        )
        return entry.takeIf { it.hasCredentials }?.copy(enabled = entry.isEffectivelyEnabled)
    }

    fun parseOfflineReminder(obj: JSONObject): OfflineReminderConfig {
        val channelsArr = obj.optJSONArray("channels")
        val typesObj = obj.optJSONObject("notificationTypes")
        val notificationTypes = if (typesObj != null) {
            OfflineNotificationTypes(
                offline = typesObj.optBoolFlex("offline", true),
                mysteryArrival = typesObj.optBoolFlex("mysteryArrival", false),
                mysteryPurchase = typesObj.optBoolFlex("mysteryPurchase", false),
            )
        } else {
            OfflineNotificationTypes()
        }

        val channels = if (channelsArr != null) {
            val map = LinkedHashMap<String, PushChannelEntry>()
            for (i in 0 until channelsArr.length()) {
                val item = channelsArr.optJSONObject(i) ?: continue
                val normalized = normalizePushEntry(
                    rawChannel = item.optStringClean("channel"),
                    enabled = item.optBoolFlex("enabled", false),
                    endpoint = item.optStringClean("endpoint"),
                    token = item.optStringClean("token"),
                    secret = item.optStringClean("secret"),
                ) ?: continue
                map.remove(normalized.normalizedChannel)
                map[normalized.normalizedChannel] = normalized
            }
            map.values.toList()
        } else {
            val endpoint = obj.optStringClean("endpoint")
            val channel = obj.optStringClean("channel")
            val migrated = if (channel.isBlank()) findPushChannelSpec(endpoint) else null
            val legacy = normalizePushEntry(
                rawChannel = channel.ifBlank { migrated?.value ?: "webhook" },
                enabled = true,
                endpoint = if (migrated == null) endpoint else "",
                token = obj.optStringClean("token"),
                secret = obj.optStringClean("secret"),
            )
            if (legacy != null) listOf(legacy) else emptyList()
        }

        return OfflineReminderConfig(
            supportsMultiChannel = channelsArr != null,
            hasNotificationTypes = typesObj != null,
            channels = channels,
            title = obj.optStringClean("title", "账号下线提醒").ifBlank { "账号下线提醒" },
            msg = obj.optStringClean("msg", "账号下线").ifBlank { "账号下线" },
            offlineDeleteSec = obj.optLongFlex("offlineDeleteSec", 0).coerceAtLeast(0),
            notificationTypes = notificationTypes,
        )
    }

    fun parseMapleMarketQuote(obj: JSONObject?): MapleMarketQuote? {
        if (obj == null) return null
        return MapleMarketQuote(
            basePrice = obj.optIntFlex("basePrice"),
            boost = obj.optIntFlex("boost"),
            unitPrice = obj.optIntFlex("unitPrice"),
            complete = obj.optBoolFlex("complete"),
            tier = obj.optStringClean("tier"),
            canSell = obj.optBoolFlex("canSell"),
            remainingQuota = obj.optIntFlex("remainingQuota"),
            ownerName = obj.optStringClean("ownerName"),
        )
    }

    fun parseMapleMarketState(obj: JSONObject): MapleMarketState {
        val fetchedAt = monotonicMillis()
        val goodsArr = obj.optJSONArray("goods") ?: JSONArray()
        val goods = buildList(goodsArr.length()) {
            for (i in 0 until goodsArr.length()) {
                val g = goodsArr.optJSONObject(i) ?: continue
                val itemsArr = g.optJSONArray("items") ?: JSONArray()
                val items = buildList(itemsArr.length()) {
                    for (j in 0 until itemsArr.length()) {
                        val it = itemsArr.optJSONObject(j) ?: continue
                        add(
                            MapleMarketGoodsItem(
                                id = it.optIntFlex("id"),
                                count = it.optIntFlex("count"),
                                name = it.optStringClean("name"),
                                image = it.optStringClean("image"),
                            )
                        )
                    }
                }
                val limitTimes = if (g.has("limitTimes") && !g.isNull("limitTimes")) g.optIntFlex("limitTimes") else null
                val remaining = if (g.has("remaining") && !g.isNull("remaining")) g.optIntFlex("remaining") else null
                add(
                    MapleMarketGoods(
                        id = g.optIntFlex("id"),
                        name = g.optStringClean("name"),
                        items = items,
                        price = g.optIntFlex("price"),
                        limitType = g.optIntFlex("limitType"),
                        limitTimes = limitTimes,
                        remaining = remaining,
                        exchangeable = g.optBoolFlex("exchangeable"),
                    )
                )
            }
        }
        return MapleMarketState(
            activityId = obj.optStringClean("activityId", "2026111707"),
            serverTimeMs = normalizeTimestampMs(obj.optLongFlex("serverTime", wallClockMillis())),
            receivedAtElapsedMs = fetchedAt,
            startTimeMs = normalizeTimestampMs(obj.optLongFlex("startTime")),
            endTimeMs = normalizeTimestampMs(obj.optLongFlex("endTime")),
            phase = obj.optStringClean("phase", "unavailable"),
            seedClaimed = obj.optBoolFlex("seedClaimed"),
            canClaimSeed = obj.optBoolFlex("canClaimSeed"),
            seedItemId = obj.optIntFlex("seedItemId", 20307),
            cropItemId = obj.optIntFlex("cropItemId", 1032),
            coinItemId = obj.optIntFlex("coinItemId", 1033),
            shared = obj.optBoolFlex("shared"),
            coinBalance = obj.optLongFlex("coinBalance"),
            pendingCommissionCoin = obj.optLongFlex("pendingCommissionCoin"),
            pendingCommissionCount = obj.optIntFlex("pendingCommissionCount"),
            canClaimCommission = obj.optBoolFlex("canClaimCommission"),
            purchaseLogUnread = obj.optBoolFlex("purchaseLogUnread"),
            rules = parseStringList(obj.optJSONArray("rules")),
            quote = parseMapleMarketQuote(obj.optJSONObject("quote")),
            goods = goods,
            seedStock = obj.optLongFlex("seedStock"),
            cropStock = obj.optLongFlex("cropStock"),
            sellableCropStock = obj.optLongFlex("sellableCropStock"),
        )
    }

    fun parseMapleMarketPurchaseRecords(arr: JSONArray?): List<MapleMarketPurchaseRecord> {
        if (arr == null) return emptyList()
        return buildList(arr.length()) {
            for (i in 0 until arr.length()) {
                val r = arr.optJSONObject(i) ?: continue
                add(
                    MapleMarketPurchaseRecord(
                        timestampMs = normalizeTimestampMs(r.optLongFlex("timestamp")),
                        sellerGid = r.optLongFlex("sellerGid"),
                        sellerName = r.optStringClean("sellerName"),
                        sellerAvatar = r.optStringClean("sellerAvatar"),
                        sellerLevel = r.optIntFlex("sellerLevel"),
                        soldCount = r.optIntFlex("soldCount"),
                        income = r.optLongFlex("income"),
                        commission = r.optLongFlex("commission"),
                    )
                )
            }
        }
    }

    fun parseMapleMarketComparison(obj: JSONObject): MapleMarketComparison {
        val rowsArr = obj.optJSONArray("rows") ?: JSONArray()
        val rows = buildList(rowsArr.length()) {
            for (i in 0 until rowsArr.length()) {
                val r = rowsArr.optJSONObject(i) ?: continue
                add(
                    MapleMarketFriendQuoteRow(
                        gid = r.optLongFlex("gid"),
                        name = r.optStringClean("name"),
                        quote = parseMapleMarketQuote(r.optJSONObject("quote")),
                        failed = r.optBoolFlex("failed"),
                        reused = r.optBoolFlex("reused"),
                    )
                )
            }
        }
        return MapleMarketComparison(
            status = obj.optStringClean("status", "idle"),
            dayKey = obj.optStringClean("dayKey"),
            total = obj.optIntFlex("total"),
            checked = obj.optIntFlex("checked"),
            reused = obj.optIntFlex("reused"),
            queried = obj.optIntFlex("queried"),
            partial = obj.optBoolFlex("partial"),
            updatedAtMs = normalizeTimestampMs(obj.optLongFlex("updatedAt")),
            rows = rows,
            error = obj.optStringClean("error"),
        )
    }
}
