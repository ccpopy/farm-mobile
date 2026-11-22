package com.qqfarm.mobile.data.model

data class ServerVersionInfo(
    val clientVersion: String = "",
    val wxClientVersion: String = "",
    val botVersion: String = "",
)

data class LoginResponse(
    val ok: Boolean,
    val token: String = "",
    val username: String = "",
    val mustChangePassword: Boolean = false,
    val error: String = "",
    val errorType: String = "",
    val remainingMs: Long = 0L,
)

data class AccountItem(
    val id: String,
    val name: String,
    val nick: String = "",
    val uin: Long = 0L,
    val avatar: String = "",
    val platform: String = "qq",
    val running: Boolean = false,
) {
    val displayTitle: String
        get() = when {
            nick.isNotBlank() && name.isNotBlank() && nick != name -> "$nick ($name)"
            name.isNotBlank() -> name
            nick.isNotBlank() -> nick
            else -> "账号 $id"
        }

    val platformLabel: String
        get() = when (platform.lowercase()) {
            "wx", "wechat" -> "微信"
            else -> "QQ"
        }
}

data class LevelProgress(
    val current: Long = 0L,
    val needed: Long = 0L,
) {
    val fraction: Float
        get() = if (needed > 0L) (current.toFloat() / needed.toFloat()).coerceIn(0f, 1f) else 0f
}

data class StatusSnapshot(
    val accountId: String = "",
    val name: String = "",
    val level: Int = 0,
    val exp: Long = 0L,
    val gold: Long = 0L,
    val coupon: Long = 0L,
    val goldBean: Long = 0L,
    val avatarUrl: String = "",
    val connected: Boolean = false,
    val uptimeSec: Long = 0L,
    val sessionElapsedSec: Long = 0L,
    val sessionExpGained: Long = 0L,
    val sessionGoldGained: Long = 0L,
    val sessionCouponGained: Long = 0L,
    val levelProgress: LevelProgress = LevelProgress(),
    val levelForecastText: String = "",
    val nextFarmRunAtMs: Long? = null,
    val nextFriendRunAtMs: Long? = null,
    val farmQuiet: Boolean = false,
    val friendQuiet: Boolean = false,
    val serverTimeMs: Long = 0L,
    val receivedAtElapsedMs: Long = 0L,
    val operations: Map<String, Int> = emptyMap(),
    val ranchOperations: Map<String, Int>? = null,
)

data class RunLogEntry(
    val ts: Long = 0L,
    val time: String = "",
    val tag: String = "",
    val msg: String = "",
    val module: String = "",
    val event: String = "",
    val result: String = "",
    val isWarn: Boolean = false,
    val accountId: String = "",
    val friendName: String = "",
    val actions: List<String> = emptyList(),
    val searchText: String = "",
)

enum class ActivityNotifyGroup(
    val key: String,
    val label: String,
    val notificationId: Int,
) {
    FARM("farm", "农场动态", 2101),
    FRIEND("friend", "好友互动", 2102),
    RANCH("ranch", "牧场动态", 2103),
    ECONOMY("economy", "收益与日常", 2104),
}

enum class ActivityNotifyType(
    val key: String,
    val group: ActivityNotifyGroup,
    val label: String,
    val description: String,
    val defaultEnabled: Boolean,
) {
    FARM_HARVEST("farm_harvest", ActivityNotifyGroup.FARM, "农场收获", "自家作物成熟并收获完成", true),
    FARM_PLANT("farm_plant", ActivityNotifyGroup.FARM, "自动种植", "空地完成播种或背包优先补种", false),
    FARM_CARE("farm_care", ActivityNotifyGroup.FARM, "务农与施肥", "除草、除虫、浇水、铲除与施肥", false),
    FARM_LAND("farm_land", ActivityNotifyGroup.FARM, "土地解锁与升级", "新土地解锁或土壤升级成功", false),

    FRIEND_STEAL("friend_steal", ActivityNotifyGroup.FRIEND, "偷取好友", "偷取好友农场作物或牧场产物", true),
    FRIEND_HELP("friend_help", ActivityNotifyGroup.FRIEND, "帮好友务农", "帮好友浇水除草除虫或牧场抚摸清理", false),
    FRIEND_BAD("friend_bad", ActivityNotifyGroup.FRIEND, "好友捣乱", "对好友放草、放虫或放蚊子", false),

    RANCH_HARVEST("ranch_harvest", ActivityNotifyGroup.RANCH, "牧场收获", "牧场动物产物或货架收获完成", true),
    RANCH_PRODUCE("ranch_produce", ActivityNotifyGroup.RANCH, "动物生产", "牧场动物开始生产", false),
    RANCH_CARE("ranch_care", ActivityNotifyGroup.RANCH, "喂养与清理", "罐头喂养、清理牧场与补充牧草", false),
    RANCH_TRADE("ranch_trade", ActivityNotifyGroup.RANCH, "购置与升级", "购买幼崽、放养、兑换、出售与棚升级", false),

    WAREHOUSE_SELL("warehouse_sell", ActivityNotifyGroup.ECONOMY, "仓库出售", "果实自动出售并结算金币", true),
    TASK_REWARD("task_reward", ActivityNotifyGroup.ECONOMY, "任务与礼包", "任务、活跃、图鉴、邮箱与每日礼包领取", false);

    companion object {
        val defaultKeys: Set<String> = entries.filter { it.defaultEnabled }.map { it.key }.toSet()
        val allKeys: Set<String> = entries.map { it.key }.toSet()
    }
}

data class DailyGiftItem(
    val key: String,
    val label: String,
    val enabled: Boolean = false,
    val doneToday: Boolean = false,
    val result: String = "",
    val lastAt: Long = 0L,
    val completedCount: Int = 0,
    val totalCount: Int = 0,
    val mode: String = "",
    val checkStatus: String = "",
    val isVip: Boolean? = null,
    val hasGift: Boolean? = null,
    val hasCard: Boolean? = null,
    val scheduledHour: Int? = null,
)

data class DailyGiftsSummary(
    val date: String = "",
    val timeZone: String = "",
    val gifts: List<DailyGiftItem> = emptyList(),
)

data class LandSummary(
    val harvestable: Int = 0,
    val growing: Int = 0,
    val empty: Int = 0,
    val dead: Int = 0,
    val needWater: Int = 0,
    val needWeed: Int = 0,
    val needBug: Int = 0,
)

data class LandLevelBonus(
    val level: Int = 0,
    val name: String = "",
    val yieldBonus: Double = 0.0,
    val growthReduction: Double = 0.0,
    val expBonus: Double = 0.0,
    val mutantMultiplier: Double = 0.0,
    val resonanceBonus: Double = 0.0,
)

data class LandUpgradeCost(
    val id: String,
    val name: String,
    val image: String = "",
    val count: String = "0",
)

data class LandUpgradeInfo(
    val mode: String = "",
    val current: LandLevelBonus? = null,
    val next: LandLevelBonus? = null,
    val serverAllowed: Boolean = false,
    val needLevel: Int? = null,
    val prerequisiteLandId: Int? = null,
    val prerequisiteMet: Boolean = false,
    val precedingName: String = "",
    val precedingRequired: Int = 0,
    val precedingOwned: Int? = null,
    val costs: List<LandUpgradeCost> = emptyList(),
)

data class LandItem(
    val id: Int,
    val level: Int = 1,
    val unlocked: Boolean = true,
    val status: String = "empty",
    val plantName: String = "",
    val phaseName: String = "",
    val seedImage: String = "",
    val matureInSec: Long = 0L,
    val totalGrowTime: Long = 0L,
    val currentSeason: Int = 1,
    val totalSeason: Int = 1,
    val needWater: Boolean = false,
    val needWeed: Boolean = false,
    val needBug: Boolean = false,
    val needInteractionCleanup: Boolean = false,
    val couldUnlock: Boolean = false,
    val couldUpgrade: Boolean = false,
    val landInfo: LandUpgradeInfo? = null,
    val occupiedByMaster: Boolean = false,
    val masterLandId: Int = 0,
    val plantSize: Int = 1,
    val leftInorcFertTimes: Int? = null,
    val mutantNames: List<String> = emptyList(),
    val fetchedAtElapsedMs: Long = 0L,
)

data class LandsPayload(
    val lands: List<LandItem> = emptyList(),
    val summary: LandSummary = LandSummary(),
    val cleanableSocialEventNames: List<String> = emptyList(),
)

data class FriendLandsPayload(
    val lands: List<LandItem>,
    val stealableCount: Int,
    val dryCount: Int,
    val weedCount: Int,
    val insectCount: Int,
    val canDisrupt: Boolean,
) {
    val needsCare: Boolean get() = dryCount > 0 || weedCount > 0 || insectCount > 0
}

data class BagSystemItem(
    val id: Int,
    val count: Long,
    val name: String,
    val interactionType: String = "",
    val hoursText: String = "",
)

private val LOCKABLE_BAG_ITEM_TYPES = setOf(5, 6, 17)
private val USABLE_BAG_ITEM_TYPES = setOf(11, 14, 33)
private const val FERTILIZER_CAPACITY_SECONDS = 990L * 3600L

data class BagItem(
    val key: String,
    val id: Int,
    val uid: Long,
    val name: String,
    val image: String,
    val count: Long,
    val category: String,
    val itemType: Int,
    val sellable: Boolean = false,
    val price: Long = 0L,
    val priceUnit: String = "金",
    val locked: Boolean = false,
    val description: String = "",
    val interactionType: String = "",
    val hoursText: String = "",
) {
    val canChangeLock: Boolean get() = uid > 0L && itemType in LOCKABLE_BAG_ITEM_TYPES
    val isFertilizer: Boolean get() = interactionType.equals("fertilizer", ignoreCase = true) ||
        interactionType.equals("fertilizerpro", ignoreCase = true)
    val supportsUse: Boolean get() = uid > 0L && (itemType in USABLE_BAG_ITEM_TYPES || isFertilizer)
}

data class BagPayload(
    val items: List<BagItem> = emptyList(),
    val systemItems: List<BagSystemItem> = emptyList(),
) {
    fun maxUseCount(item: BagItem): Int {
        if (!item.supportsUse || item.locked) return 0
        val stock = item.count.coerceIn(0L, Int.MAX_VALUE.toLong())
        if (!item.isFertilizer) return stock.toInt()

        val containerId = if (item.interactionType.equals("fertilizerpro", ignoreCase = true)) 1012 else 1011
        val usedSeconds = systemItems.firstOrNull { it.id == containerId }?.count ?: 0L
        val remainingSeconds = (FERTILIZER_CAPACITY_SECONDS - usedSeconds).coerceAtLeast(0L)
        val hoursPerItem = when (item.id) {
            80002, 80012 -> 4L
            80003, 80013 -> 8L
            80004, 80014 -> 12L
            else -> 1L
        }
        return minOf(stock, remainingSeconds / (hoursPerItem * 3600L)).toInt()
    }
}

data class RanchBucket(
    val id: Int,
    val name: String,
    val seconds: Long,
)

data class RanchAnimal(
    val id: Long,
    val ranchId: String,
    val slot: Int,
    val name: String,
    val image: String,
    val stageName: String,
    val canHarvest: Boolean = false,
    val canProduce: Boolean = false,
    val canFeed: Boolean = false,
    val limitedCanUsed: Boolean = false,
    val canRelease: Boolean = false,
    val grassBlocked: Boolean = false,
    val produceCount: Int = 0,
    val maxProduceCount: Int = 0,
    val isGiant: Boolean = false,
    val weightKg: String = "",
    val nextAtMs: Long = 0L,
    val deliveringUntilMs: Long = 0L,
    val canStroke: Boolean = false,
    val stroked: Boolean = false,
    val stage: Int = 0,
)

data class RanchPenGroup(
    val moduleType: Int,
    val name: String,
    val count: Int,
    val capacity: Int,
    val animals: List<RanchAnimal>,
)

data class RanchCareer(val harvest: Long, val mature: Long, val steal: Long) {
    val ratioText: String get() = if (harvest > 0) {
        String.format(java.util.Locale.ROOT, "%.1f", steal.toDouble() / harvest * 100).removeSuffix(".0") + "%"
    } else "—"
}

data class RanchPayload(
    val unlocked: Boolean = false,
    val level: Int = 0,
    val grass: Long = 0L,
    val buckets: List<RanchBucket> = emptyList(),
    val pens: List<RanchPenGroup> = emptyList(),
    val hazardsCount: Int = 0,
    val shelvesCollectableCount: Int = 0,
    val serverNowMs: Long = 0L,
    val fetchedAtElapsedMs: Long = 0L,
    val currentExp: Long = 0L,
    val neededExp: Long = 0L,
    val currencies: List<RanchCurrency> = emptyList(),
    val facilities: List<RanchFacility> = emptyList(),
    val notice: String = "",
    val reason: String = "",
    val dailyShareClaimed: Boolean = false,
    val dailyBenefitClaimed: Boolean = false,
    val hourglassSeconds: Long? = null,
    val career: RanchCareer? = null,
) {
    fun canFeed(animals: List<RanchAnimal>, organic: Boolean): Boolean {
        val bucketId = if (organic) 7004 else 7003
        return buckets.any { it.id == bucketId && it.seconds > 0 } &&
            animals.any { it.canFeed && (organic || !it.limitedCanUsed) }
    }
}

data class PetDog(
    val id: Int,
    val name: String,
    val image: String,
    val rarityLabel: String,
    val level: Int,
    val owned: Boolean,
    val active: Boolean,
    val skillDescription: String,
    val rarity: Int = 0,
)

data class DogFood(
    val id: Int,
    val name: String,
    val image: String,
    val durationSec: Long,
    val count: Long,
)

data class PetSnapshot(
    val dogs: List<PetDog> = emptyList(),
    val foods: List<DogFood> = emptyList(),
    val protectDurationSec: Long = 0L,
    val maxProtectDurationSec: Long = 0L,
    val remainingDurationSec: Long = 0L,
    val pendingGiftCount: Int = 0,
    val activeDogId: Int = 0,
) {
    fun maxFeedCount(food: DogFood): Int {
        if (food.durationSec <= 0L) return 0
        val remaining = (maxProtectDurationSec - protectDurationSec).coerceAtLeast(0L)
        return minOf(food.count, remaining / food.durationSec).coerceIn(0L, Int.MAX_VALUE.toLong()).toInt()
    }
}

data class FriendItem(
    val gid: Long,
    val name: String,
    val avatarUrl: String = "",
    val level: Int = 0,
    val stealNum: Int = 0,
    val dryNum: Int = 0,
    val weedNum: Int = 0,
    val insectNum: Int = 0,
) {
    val needsCareCount: Int
        get() = dryNum + weedNum + insectNum
}

data class FriendBlacklistItem(
    val gid: Long,
    val name: String,
    val avatarUrl: String = "",
)

data class InteractRecord(
    val id: String,
    val friendName: String,
    val friendAvatar: String = "",
    val actionText: String,
    val timeText: String,
    val serverTimeMs: Long = 0L,
)

data class FriendsPayload(
    val friends: List<FriendItem> = emptyList(),
    val state: String = "ready",
    val error: String = "",
)

data class SchedulerSummary(
    val workerRunning: Boolean = false,
    val workerTaskCount: Int = 0,
    val runningTasks: List<String> = emptyList(),
    val workerError: String = "",
)

data class RanchAnimalOption(
    val value: Int,
    val label: String,
    val kind: String,
)

data class SeedOption(
    val seedId: Long,
    val name: String,
    val requiredLevel: Int = 0,
    val requiresUnlockCard: Boolean = false,
)

data class AccountSettingsModel(
    val plantingStrategy: String = "max_exp",
    val preferredSeedId: Long = 0L,
    val bagSeedPriority: List<Int> = emptyList(),
    val ranchCoopCubPriority: List<Int> = emptyList(),
    val ranchShedCubPriority: List<Int> = emptyList(),
    val farmMinSec: Int = 20,
    val farmMaxSec: Int = 25,
    val friendMinSec: Int = 20,
    val friendMaxSec: Int = 25,
    val quietHoursEnabled: Boolean = false,
    val quietHoursStart: String = "01:00",
    val quietHoursEnd: String = "07:30",
    val quietContinueFarm: Boolean = true,
    val stealDelaySeconds: Int = 1,
    val plantOrderRandom: Boolean = true,
    val plantDelaySeconds: Int = 2,
    val fertilizerMode: String = "smart",
    val fertilizerSmartSeconds: Int = 300,
    val fertilizerLandTypes: List<String> = listOf("purple-gold", "gold", "black", "red", "normal"),
    val fertilizerBuyOrganicCount: Int = 1,
    val fertilizerBuyOrganicThresholdHours: Int = 10,
    val fertilizerBuyNormalCount: Int = 1,
    val fertilizerBuyNormalThresholdHours: Int = 10,
    val fertilizerBuyCheckIntervalMinutes: Int = 60,
    val emailClaimHour: Int? = null,
    val autoAcceptFriendMinLevel: Int = 0,
    val autoAcceptRequireOwnLevel: Boolean = false,
    val autoAcceptHarvestStealEnabled: Boolean = true,
    val autoAcceptHarvestStealHarvest: Int = 8,
    val autoAcceptHarvestStealSteal: Int = 1,
    val ranchStrategy: String = "level",
    val ranchBagFallback: String = "level",
    val ranchCoopAnimal: Int = 0,
    val ranchShedAnimal: Int = 0,
    val ranchGrassThresholdPercent: Int = 20,
    val ranchCheckMinutes: Int = 5,
    val ranchFeedNormalStage: String? = null,
    val ranchFeedOrganicStage: String? = null,
    val automationBooleans: Map<String, Boolean> = emptyMap(),
    val rawIntervalsJson: String = "{}",
    val rawAutomationJson: String = "{}",
)

data class LoginConfigModel(
    val wechatQrLogin: Boolean = true,
    val qqQrLogin: Boolean = false,
)

data class WxQrTask(
    val taskId: String,
    val status: String,
    val qrUrl: String,
    val expiresAt: Long,
)

data class QqQrTask(
    val taskId: String,
    val status: String,
    val qrImage: String,
    val expiresAt: Long,
)

data class AccountLogEvent(
    val time: String,
    val action: String,
    val msg: String,
    val accountId: String,
    val accountName: String,
) {
    val eventKey: String
        get() = "$time|$accountId|$action|$msg"
}
