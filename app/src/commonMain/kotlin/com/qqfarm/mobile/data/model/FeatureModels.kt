package com.qqfarm.mobile.data.model


enum class GameSpace(
    val key: String,
    val label: String,
) {
    FARM("farm", "农场"),
    RANCH("ranch", "牧场"),
}

data class CommerceItem(
    val id: Int = 0,
    val count: Long = 0L,
    val name: String = "",
    val image: String = "",
    val rarity: Int = 0,
    val description: String = "",
)

// ---- Solar Calendar ----

data class SolarSeason(
    val id: Int,
    val name: String,
    val year: String = "",
    val description: String = "",
    val eventIds: List<Int> = emptyList(),
    val litCount: Int = 0,
    val totalCount: Int = 0,
    val claimed: Boolean = false,
    val canClaim: Boolean = false,
    val rewards: List<CommerceItem> = emptyList(),
)

data class SolarEvent(
    val id: Int,
    val name: String,
    val description: String = "",
    val startTimeSec: Long = 0L,
    val endTimeSec: Long = 0L,
    val status: Int = 0,
    val directClaimed: Boolean = false,
    val quizClaimed: Boolean = false,
    val quizWrongToday: Boolean = false,
    val wrongOptions: List<Int> = emptyList(),
    val question: String = "",
    val options: List<String> = emptyList(),
    val errorTips: String = "",
    val answerIndex: Int? = null,
    val rewards: List<CommerceItem> = emptyList(),
    val questionRewards: List<CommerceItem> = emptyList(),
    val canClaim: Boolean = false,
    val canAnswer: Boolean = false,
)

data class SolarCalendarState(
    val serverTimeSec: Long = 0L,
    val receivedAtElapsedMs: Long = 0L,
    val defaultEventId: Int = 0,
    val seasons: List<SolarSeason> = emptyList(),
    val events: List<SolarEvent> = emptyList(),
)

data class SolarActionResult(
    val rewards: List<CommerceItem> = emptyList(),
    val correct: Boolean? = null,
    val calendar: SolarCalendarState? = null,
    val refreshRequired: Boolean = false,
)

// ---- Mall / Props Shop / Mystery Shop ----

data class MallCurrency(
    val id: Int,
    val name: String,
    val image: String,
    val count: Long,
    val balanceKnown: Boolean,
)

data class MallLimit(
    val type: Int,
    val bought: Int,
    val max: Int,
    val remaining: Int?,
)

data class MallMembership(
    val isSvip: Boolean = false,
    val remainingDays: Int = 0,
)

data class MallGoods(
    val id: Int,
    val name: String,
    val type: Int = 0,
    val rewards: List<CommerceItem> = emptyList(),
    val priceId: Int = 0,
    val priceName: String = "",
    val priceImage: String = "",
    val priceCount: Long = 0L,
    val priceBalance: Long? = null,
    val originalPrice: Long? = null,
    val isFree: Boolean = false,
    val limit: MallLimit? = null,
    val isLimited: Boolean = false,
    val discountText: String = "",
    val isDiscounted: Boolean = false,
    val discountEndTimeMs: Long = 0L,
    val available: Boolean = false,
    val purchasable: Boolean = false,
    val purchaseStatus: String = "available",
    val unavailableReason: String = "",
)

data class MallCatalog(
    val shopId: Int = 0,
    val slotType: Int = 1,
    val subSlotType: Int = 0,
    val serverTimeMs: Long = 0L,
    val receivedAtElapsedMs: Long = 0L,
    val refreshCountdownSec: Long = 0L,
    val membership: MallMembership? = null,
    val currencies: List<MallCurrency> = emptyList(),
    val goods: List<MallGoods> = emptyList(),
)

data class MallPurchaseResult(
    val rewards: List<CommerceItem> = emptyList(),
    val catalog: MallCatalog? = null,
    val refreshRequired: Boolean = false,
)

data class MysteryNpcOffer(
    val id: Int,
    val reward: CommerceItem,
    val priceId: Int = 0,
    val priceName: String = "",
    val priceImage: String = "",
    val priceCount: Long = 0L,
    val priceBalance: Long? = null,
    val originalPrice: Long = 0L,
    val unitPrice: Long = 0L,
    val unitOriginalPrice: Long = 0L,
    val discountPercent: Int = 0,
)

data class MysteryShopState(
    val active: Boolean = false,
    val purchaseState: String = "",
    val serverTimeMs: Long = 0L,
    val receivedAtElapsedMs: Long = 0L,
    val activeTimeMs: Long = 0L,
    val expireTimeMs: Long = 0L,
    val npc: MysteryNpcOffer? = null,
    val rawJson: String = "{}",
)

data class MysteryPurchaseResult(
    val shop: MysteryShopState? = null,
    val reward: CommerceItem? = null,
    val price: CommerceItem? = null,
)

// ---- Extended Ranch Models ----

data class RanchCurrency(
    val id: Int,
    val name: String,
    val image: String,
    val count: Long,
)

data class RanchFacilityNext(
    val level: Int,
    val capacity: Int,
    val durationSec: Long,
    val requiredLevel: Int,
    val available: Boolean,
    val costs: List<CommerceItem> = emptyList(),
)

data class RanchFacility(
    val id: String,
    val moduleType: Int,
    val name: String,
    val level: Int,
    val state: Int,
    val active: Boolean,
    val canActivate: Boolean,
    val capacity: Int,
    val upgradeState: Int,
    val upgradeEndTimeMs: Long,
    val ranchCount: Int,
    val ranchCapacity: Int,
    val hasRanch: Boolean,
    val feedCount: Int,
    val feedRemainingSec: Long,
    val hasFeed: Boolean,
    val next: RanchFacilityNext? = null,
)

data class BagPriorityItem(
    val id: Int,
    val name: String,
    val count: Long,
    val image: String,
    val housing: String = "",
)

data class RanchBagItem(
    val uid: String,
    val id: Int,
    val name: String,
    val image: String,
    val count: Long,
    val type: Int = 0,
    val locked: Boolean = false,
    val lockable: Boolean = false,
    val animalId: Int = 0,
    val husbandryType: String = "",
    val giantCub: Boolean = false,
    val canSell: Boolean = false,
    val isCan: Boolean = false,
    val canSpeedUpSec: Long = 0L,
    val isProduct: Boolean = false,
    val weightX10: Int = 0,
    val sellPrices: List<CommerceItem> = emptyList(),
)

data class RanchTaskItem(
    val id: String,
    val category: String,
    val desc: String,
    val progress: Int,
    val total: Int,
    val unlocked: Boolean,
    val canClaim: Boolean,
    val claimed: Boolean,
)

data class RanchBookAnimal(
    val id: Int,
    val name: String,
    val image: String,
    val unlocked: Boolean = false,
    val hasGiant: Boolean = false,
    val giantName: String = "",
    val giantImage: String = "",
    val giantUnlocked: Boolean = false,
    val giantIsNew: Boolean = false,
    val giantTierName: String = "",
    val giantWeightKg: String = "",
    val giantHarvestCount: String = "",
)

data class RanchBookGiantLevel(
    val level: Int,
    val score: Long,
    val claimed: Boolean,
    val canClaim: Boolean,
    val rewards: List<CommerceItem> = emptyList(),
)

data class RanchBookMutationRule(
    val type: Int,
    val name: String,
    val description: String,
    val baseProbabilityPercent: Int,
)

data class RanchBookData(
    val normalLevel: Int = 0,
    val normalProgress: Int = 0,
    val normalNextProgress: Int = 0,
    val normalCanClaim: Boolean = false,
    val normalRewards: List<CommerceItem> = emptyList(),
    val giantScore: Long = 0L,
    val giantLevels: List<RanchBookGiantLevel> = emptyList(),
    val animals: List<RanchBookAnimal> = emptyList(),
    val mutations: List<RanchBookMutationRule> = emptyList(),
)

data class RanchMarketShelf(
    val id: Int,
    val name: String,
    val sort: Int,
)

data class RanchShopGood(
    val id: String,
    val name: String,
    val reward: CommerceItem,
    val rewards: List<CommerceItem> = emptyList(),
    val priceId: Int = 0,
    val priceName: String = "",
    val priceImage: String = "",
    val priceCount: Long = 0L,
    val priceBalance: Long? = null,
    val available: Boolean = false,
    val free: Boolean = false,
    val maxCount: Int = 1,
    val remaining: Int? = null,
    val requiredLevel: Int = 0,
    val shelfId: Int = 0,
    val husbandryType: String = "",
)

data class RanchFriendSummary(
    val gid: String,
    val name: String,
    val avatarUrl: String = "",
    val canSteal: Boolean = false,
    val stealLotCount: Int = 0,
    val canMidwife: Boolean = false,
    val midwifeCount: Int = 0,
    val canHelp: Boolean = false,
    val helpCount: Int = 0,
) {
    val hasActionableTag: Boolean
        get() = canSteal || canMidwife || canHelp
}

data class RanchFriendShelf(
    val penId: String,
    val slot: Int,
    val canSteal: Boolean,
    val products: List<CommerceItem> = emptyList(),
)

data class RanchFriendPen(
    val id: String,
    val name: String,
    val mosquitoes: Int,
    val mosquitoLimit: Int,
    val mosquitoRoom: Int,
    val animals: List<RanchAnimal> = emptyList(),
    val shelves: List<RanchFriendShelf> = emptyList(),
)

data class RanchFriendFeedMachine(
    val id: String,
    val count: Int,
    val capacity: Int,
) {
    val remainingCapacity: Int get() = (capacity - count).coerceAtLeast(0)
}

data class RanchFriendDetail(
    val gid: String,
    val level: Int = 0,
    val canSteal: Boolean = false,
    val stealableShelves: Int = 0,
    val strokeRemaining: Int = 0,
    val mosquitoRemaining: Int = 0,
    val cleanableHazards: Int = 0,
    val feedMachine: RanchFriendFeedMachine? = null,
    val grassAvailable: Long? = null,
    val career: RanchCareer? = null,
    val notice: String = "",
    val sceneRestored: Boolean = true,
    val ranches: List<RanchFriendPen> = emptyList(),
) {
    val grassGiftLimit: Int
        get() = minOf(grassAvailable ?: 0L, (feedMachine?.remainingCapacity ?: 0).toLong()).toInt()
}

data class RanchViewSnapshot(
    val base: RanchPayload,
    val bag: List<RanchBagItem> = emptyList(),
    val tasks: List<RanchTaskItem> = emptyList(),
    val book: RanchBookData? = null,
    val shopGoods: List<RanchShopGood> = emptyList(),
    val marketPresent: Boolean = false,
    val marketShelves: List<RanchMarketShelf> = emptyList(),
    val marketGoods: List<RanchShopGood> = emptyList(),
    val canShopGoods: List<RanchShopGood> = emptyList(),
    val mallGoods: List<RanchShopGood>? = null,
    val friends: List<RanchFriendSummary> = emptyList(),
    val friendRanch: RanchFriendDetail? = null,
)

data class RanchQuote(
    val action: String,
    val label: String,
    val count: Int = 0,
    val skipped: Boolean = false,
    val signature: String = "",
    val costItems: List<CommerceItem> = emptyList(),
    val rewards: List<CommerceItem> = emptyList(),
    val message: String = "",
)

data class RanchOperateResult(
    val status: String = "confirmed",
    val notice: String = "",
    val refreshRequired: Boolean = false,
    val sceneRestored: Boolean = true,
)

// ---- Activity Center Models ----

data class ActivityDirectoryItem(
    val id: String,
    val activityIds: List<String> = emptyList(),
    val name: String,
    val startTimeMs: Long? = null,
    val endTimeMs: Long? = null,
    val instanceId: String? = null,
    val gameplayKey: String? = null,
    val gameplayTargets: List<String> = emptyList(),
    val detailTarget: String? = null,
)

data class ActivityDirectoryResponse(
    val serverTimeMs: Long = 0L,
    val receivedAtElapsedMs: Long = 0L,
    val activities: List<ActivityDirectoryItem> = emptyList(),
)

data class MugeTier(
    val id: String,
    val threshold: Long,
    val status: Int,
    val rewards: List<CommerceItem> = emptyList(),
)

data class MugeTask(
    val id: String,
    val description: String,
    val progress: Int,
    val target: Int,
    val status: Int,
    val unlockDay: Int,
    val points: Int,
    val rewards: List<CommerceItem> = emptyList(),
)

data class MugeQuestionOption(
    val value: Int,
    val label: String,
    val text: String,
)

data class MugeQuestion(
    val id: Int,
    val title: String,
    val options: List<MugeQuestionOption> = emptyList(),
)

data class MugePersonality(
    val answers: Map<Int, Int> = emptyMap(),
    val personalityId: Int = 0,
    val settleCount: Int = 0,
    val image: String = "",
    val shareRewardClaimed: Boolean = false,
)

data class MugeState(
    val activityId: String = "",
    val title: String = "牧歌初启",
    val serverTimeMs: Long = 0L,
    val receivedAtElapsedMs: Long = 0L,
    val startTimeMs: Long = 0L,
    val endTimeMs: Long = 0L,
    val active: Boolean = false,
    val rules: List<String> = emptyList(),
    val celebrationTotal: Long = 0L,
    val celebrationTiers: List<MugeTier> = emptyList(),
    val friendsCount: Long = 0L,
    val friendsTiers: List<MugeTier> = emptyList(),
    val handbookAvailable: Boolean = false,
    val handbookPoints: Int = 0,
    val handbookTiers: List<MugeTier> = emptyList(),
    val handbookTasks: List<MugeTask> = emptyList(),
    val personality: MugePersonality? = null,
    val questions: List<MugeQuestion> = emptyList(),
)

data class MilkTeaRecipe(
    val recipeId: String,
    val name: String,
    val image: String,
    val description: String,
    val unlocked: Boolean,
    val isAdvanced: Boolean,
    val tokenPrice: Long,
    val coinPrice: Long,
    val boostTokenPrice: Long,
    val boostCoinPrice: Long,
    val advancedStockCups: Long,
    val fruitId: Int,
)

data class MilkTeaFruit(
    val uid: String,
    val id: Int,
    val name: String,
    val image: String,
    val count: Long,
    val locked: Boolean,
    val mutations: List<String> = emptyList(),
)

data class MilkTeaSlot(
    val slotIndex: Int,
    val employeeName: String,
    val talentName: String,
    val endAtSec: Long,
)

data class MilkTeaCandidate(
    val gid: String,
    val name: String,
    val talentName: String,
    val talentId: Int,
    val isRobot: Boolean,
    val state: Int,
    val stateUntilSec: Long,
    val rehireAtSec: Long,
)

data class MilkTeaGoods(
    val id: String,
    val name: String,
    val rewards: List<CommerceItem> = emptyList(),
    val costs: List<CommerceItem> = emptyList(),
    val available: Boolean = false,
    val remaining: Int? = null,
    val purchaseLimit: Int = 0,
    val limitPeriod: String = "",
    val unmetConditionText: String = "",
)

data class MilkTeaFundTier(
    val id: String,
    val targetLevel: Int,
    val state: Int,
    val rewards: List<CommerceItem> = emptyList(),
)

data class MilkTeaLevel(
    val level: Int,
    val revenueRequired: Long,
    val basePopularity: Int,
    val employeeSlots: Int,
    val rewards: List<CommerceItem> = emptyList(),
    val recipes: List<String> = emptyList(),
)

data class MilkTeaInteraction(
    val id: Int,
    val name: String,
    val description: String,
    val image: String,
    val ready: Boolean,
)

data class MilkTeaSlip(
    val shiftId: String,
    val shopName: String,
    val employerName: String,
    val beginAtSec: Long,
    val endAtSec: Long,
    val finalWage: Long,
    val wageStatus: Int,
)

data class MilkTeaWorkState(
    val talentName: String = "",
    val talentDesc: String = "",
    val hasShift: Boolean = false,
    val shiftId: String = "",
    val shopName: String = "",
    val employerName: String = "",
    val employerAvatar: String = "",
    val beginAtSec: Long = 0L,
    val endAtSec: Long = 0L,
    val baseWage: Long? = null,
    val interactLimit: Int = 0,
    val interactCount: Int = 0,
    val interactDone: Boolean = false,
    val slips: List<MilkTeaSlip> = emptyList(),
    val interactions: List<MilkTeaInteraction> = emptyList(),
)

data class MilkTeaState(
    val id: String = "",
    val active: Boolean = false,
    val settling: Boolean = false,
    val serverTimeSec: Long = 0L,
    val receivedAtElapsedMs: Long = 0L,
    val startTimeSec: Long = 0L,
    val endTimeSec: Long = 0L,
    val opened: Boolean = false,
    val shopName: String = "",
    val level: Int = 0,
    val totalRevenue: Long = 0L,
    val nextLevelRevenue: Long = 0L,
    val canUpgrade: Boolean = false,
    val hireDisabled: Boolean = false,
    val renameCdUntilSec: Long = 0L,
    val effectiveCupsPerHour: Int = 0,
    val cashierElapsedSec: Long = 0L,
    val cashierCapacitySec: Long = 0L,
    val salesItemId: Int = 1042,
    val boostCupsPerUse: Int = 0,
    val boostCoinPrices: Map<String, Long> = emptyMap(),
    val employeeSlotCount: Int = 0,
    val balances: Map<Int, Long> = emptyMap(),
    val recommendedNames: List<String> = emptyList(),
    val pendingRewards: List<CommerceItem> = emptyList(),
    val pendingCups: List<Pair<String, Int>> = emptyList(),
    val slots: List<MilkTeaSlot> = emptyList(),
    val recipes: List<MilkTeaRecipe> = emptyList(),
    val fruits: List<MilkTeaFruit> = emptyList(),
    val goods: List<MilkTeaGoods> = emptyList(),
    val canCraft: Boolean = false,
    val canExchange: Boolean = false,
    val fundUnlocked: Boolean = false,
    val fundCosts: List<CommerceItem> = emptyList(),
    val fundTiers: List<MilkTeaFundTier> = emptyList(),
    val levels: List<MilkTeaLevel> = emptyList(),
    val hireWageTotal: Long = 0L,
    val hireDurations: List<Pair<Int, Int>> = emptyList(),
    val candidates: List<MilkTeaCandidate> = emptyList(),
    val work: MilkTeaWorkState? = null,
    val rules: List<String> = emptyList(),
)

data class ShareRewardMilestone(
    val id: String,
    val threshold: Long,
    val state: Int,
    val rewards: List<CommerceItem> = emptyList(),
)

data class ShareRewardState(
    val id: String = "",
    val title: String = "快乐不独享",
    val active: Boolean = false,
    val startTimeMs: Long = 0L,
    val endTimeMs: Long = 0L,
    val serverTimeMs: Long = 0L,
    val receivedAtElapsedMs: Long = 0L,
    val score: Long = 0L,
    val dailyReward: Int = 0,
    val firstShareReward: Int = 0,
    val firstShareAwarded: Boolean = false,
    val canShare: Boolean = false,
    val canClaimDaily: Boolean = false,
    val canClaimMilestones: Boolean = false,
    val claimedCount: Int = 0,
    val claimLimit: Int = 0,
    val poolClaimedCount: Int = 0,
    val poolClaimLimit: Int = 0,
    val milestones: List<ShareRewardMilestone> = emptyList(),
    val rules: List<String> = emptyList(),
)

data class AutumnWishState(
    val title: String = "秋祈良愿",
    val active: Boolean = false,
    val startTimeMs: Long = 0L,
    val endTimeMs: Long = 0L,
    val serverTimeMs: Long = 0L,
    val receivedAtElapsedMs: Long = 0L,
    val day: Int = 0,
    val remaining: Int = 0,
    val canDraw: Boolean = false,
    val canClaim: Boolean = false,
    val choices: List<Pair<Int, String>> = emptyList(),
    val pendingChooseId: Int = 0,
    val pendingText: String = "",
    val pendingRewards: List<CommerceItem> = emptyList(),
    val rewardDays: List<Pair<Int, CommerceItem>> = emptyList(),
    val rules: List<String> = emptyList(),
)

data class StellarPassNode(
    val id: String,
    val level: String,
    val claimed: Boolean,
    val claimable: Boolean,
    val locked: Boolean,
    val rewards: List<CommerceItem> = emptyList(),
)

data class StellarConstellationGroup(
    val id: String,
    val name: String,
    val explain: String,
    val visualState: String,
    val rewards: List<CommerceItem> = emptyList(),
)

data class StellarShopGoods(
    val id: String,
    val name: String,
    val item: CommerceItem,
    val cost: CommerceItem,
    val exchangeable: Boolean,
    val soldOut: Boolean,
    val maxExchangeCount: Int,
)

data class StellarSolarTerm(
    val id: String,
    val name: String,
    val description: String,
    val claimed: Boolean,
    val claimable: Boolean,
    val locked: Boolean,
    val rewards: List<CommerceItem> = emptyList(),
)

data class StellarSnapshot(
    val title: String = "千星游记",
    val serverTimeMs: Long = 0L,
    val receivedAtElapsedMs: Long = 0L,
    val endTimeMs: Long = 0L,
    val passLevel: String = "0",
    val passProgress: Int = 0,
    val passProgressMax: Int = 0,
    val canClaimPass: Boolean = false,
    val passNodes: List<StellarPassNode> = emptyList(),
    val canLightConstellation: Boolean = false,
    val constellationGroups: List<StellarConstellationGroup> = emptyList(),
    val shopCurrencyName: String = "星砂",
    val shopBalance: String = "--",
    val shopBalanceKnown: Boolean = false,
    val shopGoods: List<StellarShopGoods> = emptyList(),
    val solarTerms: List<StellarSolarTerm> = emptyList(),
)

data class SeasonalSimpleStage(
    val id: String,
    val label: String,
    val statusText: String,
    val claimable: Boolean = false,
    val claimed: Boolean = false,
    val rewards: List<CommerceItem> = emptyList(),
)

data class QixiState(
    val title: String = "鹊桥寄情",
    val active: Boolean = false,
    val endTimeMs: Long = 0L,
    val featherBalance: String = "--",
    val sachetBalance: String = "--",
    val canClaimBridge: Boolean = false,
    val stages: List<SeasonalSimpleStage> = emptyList(),
)

data class QingMeiState(
    val title: String = "青酿换万金",
    val balance: String = "--",
    val dailySeedClaimed: Boolean = false,
    val started: Boolean = false,
    val currentRound: Int = 0,
    val maxRounds: Int = 3,
    val finished: Boolean = false,
    val currentQuoteGold: String = "0",
    val canStart: Boolean = false,
    val canContinue: Boolean = false,
    val canSettle: Boolean = false,
    val ingredientsCount: Int = 0,
    val firstIngredientUid: String = "",
    val firstIngredientCount: Long = 0L,
)

data class CharityState(
    val title: String = "公益小红花",
    val active: Boolean = false,
    val endTimeMs: Long = 0L,
    val loveBalance: String = "0",
    val donatedLove: String = "0",
    val agreementAccepted: Boolean = false,
    val canClaimSeeds: Boolean = false,
    val canShare: Boolean = false,
    val canDonateLove: Boolean = false,
    val canClaimDailyGift: Boolean = false,
    val stages: List<SeasonalSimpleStage> = emptyList(),
)

data class WeatherState(
    val title: String = "雨落成诗",
    val active: Boolean = false,
    val endTimeMs: Long = 0L,
    val badgeBalance: String = "--",
    val collectionBottleCount: String = "--",
    val rainBottleCount: String = "--",
    val activeWeatherName: String = "",
    val canExchangeCollector: Boolean = false,
    val canSummonRain: Boolean = false,
    val nextResearchNodeId: String = "",
    val canAdvanceResearch: Boolean = false,
)

// ---- Illustrated Book & Collections ----

data class IllustratedShopSeed(
    val seedId: Int = 0,
    val goodsId: Int = 0,
    val name: String = "",
    val price: Long = 0L,
    val requiredLevel: Int = 0,
    val locked: Boolean = false,
    val soldOut: Boolean = false,
    val itemCount: Int = 1,
    val remainingPurchaseCount: Int? = null,
    val unknownMeta: Boolean = false,
    val requiresUnlockCard: Boolean = false,
)

data class IllustratedBookItem(
    val seedId: Int = 0,
    val name: String = "",
    val image: String = "",
    val rewardCategory: Int = 0,
    val group: String = "",
    val sort: Int = 0,
    val cropCategory: String = "",
    val unlocked: Boolean = false,
    val progress: Int = 0,
    val isNew: Boolean = false,
    val shopSeed: IllustratedShopSeed? = null,
)

data class IllustratedLevel(
    val level: Int = 0,
    val progress: Int = 0,
    val claimed: Boolean = false,
    val canClaim: Boolean = false,
    val rewards: List<CommerceItem> = emptyList(),
)

data class IllustratedBuff(
    val id: Int = 0,
    val level: Int = 0,
    val name: String = "",
    val value: Int = 0,
    val valueType: String = "",
) {
    val formattedEffect: String
        get() = if (valueType == "probability") {
            val pct = value / 10.0
            val text = if (pct % 1.0 == 0.0) pct.toInt().toString() else pct.toString()
            "触发概率 $text%"
        } else {
            "数量 +$value"
        }
}

data class IllustratedBook(
    val type: Int = 1,
    val level: Int = 0,
    val progress: Int = 0,
    val nextLevelProgress: Int = 0,
    val claimable: Boolean = false,
    val levels: List<IllustratedLevel> = emptyList(),
    val buffs: List<IllustratedBuff> = emptyList(),
    val currentBuffs: List<IllustratedBuff> = emptyList(),
    val items: List<IllustratedBookItem> = emptyList(),
)

data class IllustratedSnapshot(
    val crop: IllustratedBook = IllustratedBook(type = 1),
    val mutant: IllustratedBook = IllustratedBook(type = 2),
    val updatedAt: Long = 0L,
)

data class IllustratedClaimResult(
    val rewards: List<CommerceItem> = emptyList(),
    val snapshot: IllustratedSnapshot? = null,
    val refreshRequired: Boolean = false,
    val message: String = "",
)

data class CollectionBuff(
    val id: Int = 0,
    val name: String = "",
    val value: Double = 0.0,
    val description: String = "",
)

data class CollectionMilestone(
    val level: Int = 0,
    val score: Long = 0L,
    val claimed: Boolean = false,
    val reached: Boolean = false,
    val rewards: List<CommerceItem> = emptyList(),
    val buffs: List<CollectionBuff> = emptyList(),
)

data class CollectionMutationRule(
    val name: String = "",
    val probability: Double = 0.0,
    val guarantee: Int = 0,
)

data class CollectionItemEntry(
    val id: Int = 0,
    val name: String = "",
    val description: String = "",
    val image: String = "",
    val starLevel: Int = 0,
    val maxStarLevel: Int = 5,
    val ownCount: Long = 0L,
    val activated: Boolean = false,
    val isNew: Boolean = false,
    val score: Long = 0L,
    val displayed: Boolean = false,
    val wished: Boolean = false,
    val requirements: List<String> = emptyList(),
    val nextStarCosts: List<CommerceItem> = emptyList(),
    val unlocked: Boolean = false,
    val canUpgrade: Boolean = false,
)

data class CollectionSeries(
    val id: Int = 0,
    val name: String = "",
    val rarity: Int = 1,
    val rarityName: String = "",
    val description: String = "",
    val requirements: List<String> = emptyList(),
    val mutationRules: List<CollectionMutationRule> = emptyList(),
    val items: List<CollectionItemEntry> = emptyList(),
    val activatedCount: Int = 0,
    val totalCount: Int = 0,
)

data class CollectionSnapshot(
    val score: Long = 0L,
    val claimedScore: Long = 0L,
    val activatedCount: Int = 0,
    val totalCount: Int = 0,
    val nextRewardScore: Long? = null,
    val currentBuffs: List<CollectionBuff> = emptyList(),
    val milestones: List<CollectionMilestone> = emptyList(),
    val series: List<CollectionSeries> = emptyList(),
    val displayItemId: String = "0",
)

data class CollectionOperateResult(
    val skipped: Boolean = false,
    val snapshot: CollectionSnapshot,
    val rewards: List<CommerceItem> = emptyList(),
    val message: String = "",
)

data class MutantProbability(
    val rarity: Int = 0,
    val percent: Double = 0.0,
)

data class MutantCatalogEffect(
    val id: Int = 0,
    val name: String = "",
    val image: String = "",
    val effect: String = "",
    val description: String = "",
    val probabilities: List<MutantProbability> = emptyList(),
)

data class MutationCatalogCollectionSeries(
    val id: Int = 0,
    val name: String = "",
    val rarityName: String = "",
    val description: String = "",
    val mutationRules: List<CollectionMutationRule> = emptyList(),
    val items: List<CommerceItem> = emptyList(),
)

data class MutationCatalogSnapshot(
    val normal: List<MutantCatalogEffect> = emptyList(),
    val decoration: List<MutantCatalogEffect> = emptyList(),
    val collections: List<MutationCatalogCollectionSeries> = emptyList(),
)

data class SystemUpdateCommit(
    val hash: String,
    val shortHash: String,
    val subject: String,
    val author: String = "",
    val timestampMs: Long = 0L,
)

data class SystemUpdateJob(
    val id: String,
    val status: String,
    val phase: String = "",
    val detail: String = "",
    val startedAt: Long = 0L,
    val updatedAt: Long = 0L,
    val finishedAt: Long = 0L,
    val error: String = "",
    val fromCommit: String = "",
    val targetCommit: String = "",
    val recentLines: List<String> = emptyList(),
) {
    val isRollback: Boolean get() = id.startsWith("rbk_")
}

data class SystemReleaseBackup(
    val releaseId: String,
    val shortCommit: String,
    val subject: String,
    val createdAt: Long,
    val pairedWebReleaseId: String,
    val isActive: Boolean,
    val isPrevious: Boolean,
    val compatible: Boolean,
    val compatibilityReasons: List<String>,
    val canRollback: Boolean,
    val canDelete: Boolean,
    val isOutdated: Boolean,
    val sizeBytes: Long,
) {
    val label: String get() = shortCommit.ifBlank { releaseId }
}

data class SystemReleaseCleanupSummary(
    val orphanHostCount: Int,
    val orphanWebCount: Int,
    val orphanSizeBytes: Long,
) {
    val hasOrphans: Boolean get() = orphanHostCount > 0 || orphanWebCount > 0
}

data class SystemUpdateStatus(
    val mode: String = "unknown",
    val gitSupported: Boolean = false,
    val githubTokenConfigured: Boolean? = null,
    val unsupportedReason: String = "",
    val applySupported: Boolean = false,
    val applyUnsupportedReason: String = "",
    val branch: String = "",
    val detached: Boolean = false,
    val commit: String = "",
    val shortCommit: String = "",
    val commitSubject: String = "",
    val startupCommit: String = "",
    val needsApply: Boolean = false,
    val upstream: String = "",
    val remote: String = "",
    val remoteBranch: String = "",
    val behindCount: Int = 0,
    val aheadCount: Int = 0,
    val dirtyFiles: List<String> = emptyList(),
    val incomingCommits: List<SystemUpdateCommit> = emptyList(),
    val canCheck: Boolean = false,
    val canPull: Boolean = false,
    val canApply: Boolean = false,
    val deployBusy: Boolean = false,
    val lastCheckedAt: Long? = null,
    val lastCheckError: String = "",
    val lastPulledAt: Long? = null,
    val job: SystemUpdateJob? = null,
    val releases: List<SystemReleaseBackup>? = null,
    val releaseCleanup: SystemReleaseCleanupSummary? = null,
    val keepBackups: Boolean? = null,
)

data class SystemReleaseCleanupResult(
    val deletedReleaseIds: List<String>,
    val deletedWebIds: List<String>,
    val deletedHostIds: List<String>,
    val freedBytes: Long,
    val status: SystemUpdateStatus,
)

data class SystemUpdatePullResult(
    val pulled: Boolean = false,
    val fromCommit: String = "",
    val toCommit: String = "",
    val status: SystemUpdateStatus,
)

// ---- Multi-Channel Push Notifications ----

data class PushChannelSpec(
    val value: String,
    val label: String,
    val docUrl: String = "",
    val usesEndpoint: Boolean = false,
    val isDingTalk: Boolean = false,
    val isMeow: Boolean = false,
)

val PUSH_CHANNEL_SPECS: List<PushChannelSpec> = listOf(
    PushChannelSpec(value = "webhook", label = "Webhook(自定义接口)", usesEndpoint = true),
    PushChannelSpec(value = "qmsg", label = "Qmsg 酱", docUrl = "https://qmsg.zendee.cn/"),
    PushChannelSpec(value = "serverchan", label = "Server 酱", docUrl = "https://sct.ftqq.com/"),
    PushChannelSpec(value = "pushplus", label = "Push Plus", docUrl = "https://www.pushplus.plus/"),
    PushChannelSpec(value = "pushplushxtrip", label = "Push Plus Hxtrip", docUrl = "https://pushplus.hxtrip.com/"),
    PushChannelSpec(
        value = "dingtalk",
        label = "钉钉",
        docUrl = "https://open.dingtalk.com/document/orgapp/customize-robot-security-settings",
        usesEndpoint = true,
        isDingTalk = true,
    ),
    PushChannelSpec(value = "wecom", label = "企业微信", docUrl = "https://guole.fun/posts/626/"),
    PushChannelSpec(value = "bark", label = "Bark", docUrl = "https://github.com/Finb/Bark"),
    PushChannelSpec(value = "gocqhttp", label = "Go-cqhttp", docUrl = "https://docs.go-cqhttp.org/api/"),
    PushChannelSpec(value = "onebot", label = "OneBot", docUrl = "https://docs.go-cqhttp.org/api/"),
    PushChannelSpec(value = "atri", label = "Atri", docUrl = "https://blog.tianli0.top/"),
    PushChannelSpec(value = "pushdeer", label = "PushDeer", docUrl = "https://www.pushdeer.com/"),
    PushChannelSpec(value = "igot", label = "iGot", docUrl = "https://push.hellyw.com/"),
    PushChannelSpec(value = "telegram", label = "Telegram", docUrl = "https://core.telegram.org/bots"),
    PushChannelSpec(value = "feishu", label = "飞书", docUrl = "https://www.feishu.cn/hc/zh-CN/articles/360024984973"),
    PushChannelSpec(value = "ifttt", label = "IFTTT", docUrl = "https://ifttt.com/maker_webhooks"),
    PushChannelSpec(value = "wecombot", label = "企业微信群机器人", docUrl = "https://developer.work.weixin.qq.com/document/path/91770"),
    PushChannelSpec(value = "discord", label = "Discord", docUrl = "https://discord.com/developers/docs/resources/webhook#execute-webhook"),
    PushChannelSpec(value = "wxpusher", label = "WxPusher", docUrl = "https://wxpusher.zjiecode.com/docs/#/"),
    PushChannelSpec(value = "meow", label = "MeoW", docUrl = "https://www.chuckfang.com/MeoW/api_doc.html", isMeow = true),
)

private val PUSH_CHANNEL_SPEC_MAP: Map<String, PushChannelSpec> =
    PUSH_CHANNEL_SPECS.associateBy { it.value }

fun findPushChannelSpec(channel: String): PushChannelSpec? =
    PUSH_CHANNEL_SPEC_MAP[channel.trim().lowercase()]

fun normalizeDingTalkEndpoint(endpoint: String, token: String): String {
    val credential = endpoint.trim().ifBlank { token.trim() }
    if (credential.isEmpty()) return ""
    if (credential.startsWith("http://", ignoreCase = true) || credential.startsWith("https://", ignoreCase = true)) {
        return credential
    }
    return "https://oapi.dingtalk.com/robot/send?access_token=" +
        com.qqfarm.mobile.platform.percentEncode(credential)
}


/** 轻量 HTTP(S) URL 校验（替代 okhttp HttpUrl，双端通用） */
fun isValidHttpUrl(raw: String): Boolean {
    val url = raw.trim()
    val schemeEnd = url.indexOf("://")
    if (schemeEnd <= 0) return false
    when (url.substring(0, schemeEnd).lowercase()) {
        "http", "https" -> {}
        else -> return false
    }
    val authority = url.substring(schemeEnd + 3).substringBefore('/').substringBefore('?').substringBefore('#')
    if (authority.isBlank()) return false
    val hostPart = authority.substringAfterLast('@', missingDelimiterValue = authority)
    if (hostPart.contains('@')) return false
    val portIdx = hostPart.lastIndexOf(':')
    if (portIdx >= 0) {
        val port = hostPart.substring(portIdx + 1)
        if (port.isEmpty() || port.any { !it.isDigit() } || port.toInt() !in 1..65535) return false
    }
    val host = if (portIdx >= 0) hostPart.substring(0, portIdx) else hostPart
    if (host.isBlank()) return false
    if (host.startsWith('[')) return host.endsWith(']') && host.length > 2 // IPv6
    return host.none { it.isWhitespace() }
}

fun validateDingTalkWebhookUrl(endpoint: String): String {
    val url = endpoint.trim()
    val ok = url.startsWith("https://oapi.dingtalk.com/robot/send", ignoreCase = true) &&
        url.substringAfter("https://oapi.dingtalk.com/robot/send", missingDelimiterValue = "")
            .let { rest ->
                // path 必须精确为 /robot/send，其后只允许 query/fragment
                rest.isEmpty() || rest.startsWith("?") || rest.startsWith("#")
            } && url.containsQueryToken("access_token")
    if (ok) return ""
    return "钉钉 Webhook 地址格式不正确，请从群机器人设置页重新复制"
}

private fun String.containsQueryToken(name: String): Boolean {
    val query = substringAfter("?", missingDelimiterValue = "").substringBefore("#")
    if (query.isBlank()) return false
    return query.split("&").any { pair ->
        val k = pair.substringBefore("=")
        k == name && pair.substringAfter("=", missingDelimiterValue = "").isNotBlank()
    }
}

fun parseOfflineDeleteSeconds(input: String): Long? =
    input.trim().toLongOrNull()?.takeIf { it in 0..9_007_199_254_740_991L }

data class PushChannelEntry(
    val channel: String,
    val enabled: Boolean = false,
    val endpoint: String = "",
    val token: String = "",
    val secret: String = "",
) {
    val normalizedChannel: String get() = channel.trim().lowercase()
    val hasCredentials: Boolean get() = endpoint.isNotBlank() || token.isNotBlank() || secret.isNotBlank()

    val isConfigured: Boolean
        get() {
            val spec = findPushChannelSpec(normalizedChannel) ?: return false
            return when {
                spec.isDingTalk -> endpoint.trim().isNotEmpty() || token.trim().isNotEmpty()
                spec.usesEndpoint -> endpoint.trim().isNotEmpty()
                else -> token.trim().isNotEmpty()
            }
        }

    val isEffectivelyEnabled: Boolean get() = enabled && isConfigured
}

data class OfflineNotificationTypes(
    val offline: Boolean = true,
    val mysteryArrival: Boolean = false,
    val mysteryPurchase: Boolean = false,
)

data class OfflineReminderConfig(
    val supportsMultiChannel: Boolean = false,
    val hasNotificationTypes: Boolean = false,
    val channels: List<PushChannelEntry> = emptyList(),
    val title: String = "账号下线提醒",
    val msg: String = "账号下线",
    val offlineDeleteSec: Long = 0,
    val notificationTypes: OfflineNotificationTypes = OfflineNotificationTypes(),
) {
    val configuredChannels: List<PushChannelEntry> get() = channels.filter { it.isConfigured }
    val activeChannels: List<PushChannelEntry> get() = channels.filter { it.isEffectivelyEnabled }
    val hasActiveChannel: Boolean get() = activeChannels.isNotEmpty()

    val summaryText: String
        get() = when {
            activeChannels.size > 1 -> "已启用 ${activeChannels.size} 个"
            activeChannels.size == 1 -> {
                val active = activeChannels.first()
                findPushChannelSpec(active.normalizedChannel)?.label ?: active.channel
            }
            configuredChannels.isNotEmpty() -> "已停用 (${configuredChannels.size})"
            else -> "未配置"
        }

    fun entryFor(channelId: String): PushChannelEntry {
        val key = channelId.trim().lowercase()
        return channels.firstOrNull { it.normalizedChannel == key } ?: PushChannelEntry(channel = key)
    }
}
