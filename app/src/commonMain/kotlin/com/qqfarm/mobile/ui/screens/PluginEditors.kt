package com.qqfarm.mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Agriculture
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.qqfarm.mobile.data.model.AccountSettingsModel
import com.qqfarm.mobile.data.model.PluginAccountDetail
import com.qqfarm.mobile.data.model.PluginDefinition
import com.qqfarm.mobile.data.model.PluginQuietWindow
import com.qqfarm.mobile.data.model.PluginSpendingPolicy
import com.qqfarm.mobile.data.model.PluginSpendingRule
import com.qqfarm.mobile.data.model.PluginSpendingUsage
import com.qqfarm.mobile.data.model.RanchAnimalOption
import com.qqfarm.mobile.data.model.SeedOption
import com.qqfarm.mobile.ui.components.AppleAlertDialog
import com.qqfarm.mobile.ui.components.AppleFilterChip
import com.qqfarm.mobile.ui.components.AppleGroupedCard
import com.qqfarm.mobile.ui.components.AppleHairlineDivider
import com.qqfarm.mobile.ui.components.AppleSearchField
import com.qqfarm.mobile.ui.components.AppleSectionHeader
import com.qqfarm.mobile.ui.components.AppleSettingRow
import com.qqfarm.mobile.ui.components.AppleStatusPill
import com.qqfarm.mobile.ui.components.AppleSwitch
import com.qqfarm.mobile.ui.components.AppleSwitchRow
import com.qqfarm.mobile.ui.components.formatTimestamp
import com.qqfarm.mobile.ui.components.retainSheetHeight
import com.qqfarm.mobile.ui.theme.LocalAppleColors
import com.qqfarm.mobile.data.network.json.JSONArray
import com.qqfarm.mobile.data.network.json.JSONObject
import com.qqfarm.mobile.platform.groupDigits
import com.qqfarm.mobile.platform.kBigIntValueOf

private val BUILTIN_PLANTING_STRATEGIES = listOf(
    "preferred" to "优先种植种子",
    "level" to "最高等级作物",
    "max_exp" to "最大经验/时",
    "max_fert_exp" to "最大普通肥经验/时",
    "max_profit" to "最大净利润/时",
    "max_fert_profit" to "最大普通肥净利润/时",
    "bag_priority" to "背包种子优先",
)

private val BUILTIN_BAG_FALLBACK_STRATEGIES = BUILTIN_PLANTING_STRATEGIES.filter { it.first != "bag_priority" }

private val BUILTIN_RANCH_STRATEGIES = listOf(
    "level" to "按已解锁等级",
    "preferred" to "指定动物",
    "bag_priority" to "优先背包幼崽",
)

private val BUILTIN_FERTILIZER_OPTIONS = listOf(
    "both" to "普通 + 有机",
    "smart" to "普通 + 快成熟有机",
    "normal" to "仅普通化肥",
    "organic" to "仅有机化肥",
    "none" to "不施肥",
)

private val BUILTIN_FERTILIZER_LAND_TYPES = listOf(
    "blue-crystal" to "蓝晶土地",
    "purple-gold" to "紫金土地",
    "gold" to "金土地",
    "black" to "黑土地",
    "red" to "红土地",
    "normal" to "普通土地",
)

private val BUILTIN_FARM_SWITCHES = listOf(
    "farm" to "自动种植收获",
    "farm_harvest_tiangong" to "自动收获天工作物",
    "farm_push" to "推送触发巡田",
    "skip_own_weed_bug" to "巡田时跳过一键务农",
    "land_upgrade" to "自动升级土地",
    "task" to "自动做任务",
    "email" to "自动领取邮箱奖励",
    "sell" to "自动卖果实",
    "fertilizer_gift" to "自动填充化肥",
)

private val BUILTIN_RANCH_SWITCHES = listOf(
    "ranch_harvest" to "自动收获",
    "ranch_produce" to "自动生产",
    "ranch_buy" to "自动补栏（金券）",
    "ranch_activate_shed" to "自动解锁一级棚（免费）",
    "ranch_upgrade" to "自动升级建筑（金券）",
    "ranch_market_organic_poop" to "便便兑换有机化肥",
    "ranch_market_organic_pinecone" to "松果兑换有机化肥",
    "ranch_sell" to "自动出售产物",
    "ranch_clean" to "自动清理",
    "ranch_grass" to "自动补充牧草",
    "ranch_grass_buy" to "牧草不足时购买（金券）",
    "ranch_task" to "自动领取任务奖励",
    "ranch_illustrated" to "自动领取图鉴奖励",
    "ranch_share" to "每日分享礼包",
    "ranch_daily_benefit" to "每日商城福利",
)

private val RANCH_CAN_STAGE_OPTIONS = listOf(
    Triple("any", "可喂养时就用", "动物允许喂罐头时立即使用"),
    Triple("longest", "仅最长阶段", "只在动物生长周期最长的阶段使用"),
    Triple("to_production", "成长期 · 加速到生产期", "仅在成长期使用"),
    Triple("near_harvest", "成熟期 · 距收获 10 分钟内", "仅在接近收获时使用，减少浪费"),
)

private fun ranchCanStageLabel(value: String): String =
    RANCH_CAN_STAGE_OPTIONS.firstOrNull { it.first == value }?.second ?: value

private val BUILTIN_RANCH_FRIEND_SWITCHES = listOf(
    Triple("ranch_friend_steal", "自动偷取产物", true),
    Triple("ranch_friend_produce", "自动帮忙生产", false),
    Triple("ranch_friend_clean", "自动清理", true),
    Triple("ranch_stroke", "自动抚摸动物", true),
    Triple("ranch_friend_mosquito", "自动放蚊子", true),
)

private val BUILTIN_FRIEND_SWITCHES = listOf(
    "friend" to "自动好友互动",
    "friend_steal" to "自动偷菜",
    "friend_help" to "自动帮忙",
    "friend_bad" to "自动捣乱",
    "friend_help_exp_limit" to "经验满不帮忙",
    "friend_help_protect_dog_ignore_exp_limit" to "护主犬经验满仍帮忙",
)

private val BUILTIN_MYSTERY_CURRENCIES = listOf(
    "mystery_shop_allow_gold" to "金币",
    "mystery_shop_allow_coupon" to "点券",
    "mystery_shop_allow_gold_bean" to "金豆豆",
    "mystery_shop_allow_diamond" to "钻石",
)

private val KNOWN_RESOURCE_NAMES = linkedMapOf(
    "item:1001" to "金币",
    "item:1002" to "点券",
    "item:1004" to "钻石",
    "item:1005" to "金豆豆",
    "item:1041" to "奶茶券",
    "item:1042" to "宣传喇叭",
    "item:90004" to "1 天狗粮",
    "item:90005" to "3 天狗粮",
    "item:90006" to "5 天狗粮",
)

private val KNOWN_OPERATION_NAMES = mapOf(
    "mall.purchase" to "商城购买",
    "pets.feed" to "宠物喂食",
    "milkTea.claim" to "领取收益",
    "milkTea.interact" to "选择工作事项",
    "milkTea.claimFund" to "领取成长基金",
    "milkTea.upgrade" to "升级店铺",
    "milkTea.craft" to "制作奶茶",
    "milkTea.boost" to "宣传加速",
    "milkTea.buySeeds" to "购买种子",
    "milkTea.hire" to "雇佣员工",
    "shareReward.daily" to "领取每日快乐值",
    "shareReward.share" to "每日分享",
    "shareReward.milestones" to "领取档位奖励",
)

private val OP_STATUS_LABELS = mapOf(
    "confirmed" to "已生效",
    "skipped" to "已跳过",
    "rejected" to "未执行",
    "unknown" to "结果未知",
    "sent" to "已发送",
    "prepared" to "已准备",
)

private val QUIET_TIME_REGEX = Regex("^([01]\\d|2[0-3]):([0-5]\\d)$")
private val RESOURCE_KEY_REGEX = Regex("^item:[1-9]\\d{0,9}$")

internal fun pluginResourceName(resourceKey: String, customNames: Map<String, String> = emptyMap()): String {
    customNames[resourceKey]?.takeIf { it.isNotBlank() }?.let { return it }
    KNOWN_RESOURCE_NAMES[resourceKey]?.let { return it }
    val id = resourceKey.removePrefix("item:")
    return if (id != resourceKey && id.all(Char::isDigit)) "物品 $id" else resourceKey
}

internal fun pluginOperationName(kind: String, customNames: Map<String, String> = emptyMap()): String {
    customNames[kind]?.takeIf { it.isNotBlank() }?.let { return it }
    return KNOWN_OPERATION_NAMES[kind] ?: kind
}

internal fun formatPluginQuantity(value: String): String =
    runCatching { groupDigits(value.trim().toLongOrNull() ?: 0L) }
        .getOrDefault(value)

internal data class SchemaFieldOption(
    val label: String,
    val value: Any?,
)

internal data class SchemaFieldSpec(
    val name: String,
    val title: String,
    val type: String,
    val widget: String,
    val unit: String = "",
    val help: String = "",
    val inlineHelp: Boolean = false,
    val placeholder: String = "",
    val allowUnlimited: Boolean = false,
    val minimum: Double? = null,
    val maximum: Double? = null,
    val minLength: Int? = null,
    val maxLength: Int? = null,
    val maxItems: Int? = null,
    val pattern: String = "",
    val options: List<SchemaFieldOption> = emptyList(),
    val visibleIfField: String = "",
    val visibleIfEquals: Any? = null,
)

internal data class SchemaSectionSpec(
    val title: String,
    val fields: List<SchemaFieldSpec>,
)

internal fun parseSchemaSections(definition: PluginDefinition): List<SchemaSectionSpec> {
    val schema = runCatching { JSONObject(definition.settingsSchemaJson) }.getOrNull() ?: return emptyList()
    val props = schema.optJSONObject("properties") ?: return emptyList()
    val ui = runCatching { JSONObject(definition.uiSchemaJson) }.getOrDefault(JSONObject())
    val uiFields = ui.optJSONObject("fields") ?: JSONObject()

    val propNames = buildList {
        for (k in props.keys()) add(k)
    }
    if (propNames.isEmpty()) return emptyList()

    val orderArr = ui.optJSONArray("order")
    val preferred = buildList {
        if (orderArr != null) {
            for (i in 0 until orderArr.length()) {
                val n = orderArr.optString(i).trim()
                if (n in propNames && n !in this) add(n)
            }
        }
    }
    val ordered = preferred + propNames.filter { it !in preferred }

    fun toSpec(name: String): SchemaFieldSpec? {
        val prop = props.optJSONObject(name) ?: return null
        val fieldUi = uiFields.optJSONObject(name) ?: JSONObject()
        val type = prop.optString("type").ifBlank { "string" }
        val configuredWidget = fieldUi.optString("widget")
        val enumArr = if (type == "array") {
            prop.optJSONObject("items")?.optJSONArray("enum") ?: prop.optJSONArray("enum")
        } else {
            prop.optJSONArray("enum")
        }
        val enumNamesArr = if (type == "array") {
            prop.optJSONObject("items")?.optJSONArray("enumNames") ?: prop.optJSONArray("enumNames")
        } else {
            prop.optJSONArray("enumNames")
        }
        val options = buildList {
            if (enumArr != null) {
                for (i in 0 until enumArr.length()) {
                    val v = enumArr.opt(i)
                    val label = enumNamesArr?.optString(i)?.takeIf { it.isNotBlank() && it != "null" }
                        ?: v?.toString().orEmpty()
                    add(SchemaFieldOption(label = label, value = v))
                }
            }
        }
        val widget = when {
            configuredWidget.isNotBlank() -> configuredWidget
            type == "boolean" -> "switch"
            type == "array" -> "checkbox-group"
            enumArr != null -> "select"
            type == "integer" || type == "number" -> "number"
            else -> "input"
        }
        val unit = fieldUi.optString("unit")
        val rawTitle = prop.optString("title").ifBlank { name }
        val title = if (unit.isNotBlank()) "$rawTitle（$unit）" else rawTitle
        val help = fieldUi.optString("help").ifBlank { prop.optString("description") }
        val inlineHelp = fieldUi.optString("helpDisplay") == "inline"
        val visObj = fieldUi.optJSONObject("visibleIf")
        val visField = visObj?.optString("field").orEmpty()
        val visEquals = visObj?.opt("equals")
        val minVal = if (prop.has("minimum") && !prop.isNull("minimum")) prop.optDouble("minimum") else null
        val maxVal = if (prop.has("maximum") && !prop.isNull("maximum")) prop.optDouble("maximum") else null
        val maxLen = if (prop.has("maxLength") && !prop.isNull("maxLength")) prop.optInt("maxLength") else null
        return SchemaFieldSpec(
            name = name,
            title = title,
            type = type,
            widget = widget,
            unit = unit,
            help = help,
            inlineHelp = inlineHelp,
            placeholder = fieldUi.optString("placeholder"),
            allowUnlimited = prop.optBoolean("allowUnlimited", false),
            minimum = minVal,
            maximum = maxVal,
            minLength = prop.optInt("minLength").takeIf { it > 0 },
            maxLength = maxLen,
            maxItems = prop.optInt("maxItems").takeIf { it > 0 },
            pattern = prop.optString("pattern"),
            options = options,
            visibleIfField = visField,
            visibleIfEquals = visEquals,
        )
    }

    val groupsArr = ui.optJSONArray("groups")
    if (groupsArr == null || groupsArr.length() == 0) {
        return listOf(SchemaSectionSpec(title = "", fields = ordered.mapNotNull(::toSpec)))
    }
    val groupedNames = mutableSetOf<String>()
    val sections = mutableListOf<SchemaSectionSpec>()
    for (i in 0 until groupsArr.length()) {
        val g = groupsArr.optJSONObject(i) ?: continue
        val fieldsArr = g.optJSONArray("fields") ?: continue
        val namesInGroup = buildList {
            for (j in 0 until fieldsArr.length()) {
                val n = fieldsArr.optString(j).trim()
                if (n.isNotEmpty()) add(n)
            }
        }
        groupedNames.addAll(namesInGroup)
        val specs = ordered.filter { it in namesInGroup }.mapNotNull(::toSpec)
        if (specs.isNotEmpty()) {
            sections.add(SchemaSectionSpec(title = g.optString("title"), fields = specs))
        }
    }
    val rest = ordered.filter { it !in groupedNames }.mapNotNull(::toSpec)
    if (rest.isNotEmpty()) sections.add(SchemaSectionSpec(title = "其他", fields = rest))
    return sections
}

private fun isFieldVisible(spec: SchemaFieldSpec, values: JSONObject): Boolean {
    if (spec.visibleIfField.isBlank()) return true
    val actual = values.opt(spec.visibleIfField)
    val expected = spec.visibleIfEquals
    return jsonValuesEqual(actual, expected)
}

private fun jsonValuesEqual(a: Any?, b: Any?): Boolean {
    if (a === b) return true
    if (a == null || a == com.qqfarm.mobile.data.network.json.JSON_NULL) return b == null || b == com.qqfarm.mobile.data.network.json.JSON_NULL
    if (b == null || b == com.qqfarm.mobile.data.network.json.JSON_NULL) return false
    if (a is Number && b is Number) {
        return a.toDouble() == b.toDouble()
    }
    if (a is Boolean && b is Boolean) return a == b
    return a.toString() == b.toString()
}

internal fun schemaFieldError(spec: SchemaFieldSpec, values: JSONObject): String {
    if (!isFieldVisible(spec, values)) return ""
    val v = values.opt(spec.name)
    if (spec.type == "integer" || spec.type == "number") {
        if (v == null || v == com.qqfarm.mobile.data.network.json.JSON_NULL || v.toString().isBlank()) return "请输入数字"
        val num = (v as? Number)?.toDouble()?.takeIf { it.isFinite() } ?: return if (spec.type == "integer") "请输入整数" else "请输入有效数字"
        if (spec.type == "integer" && num % 1.0 != 0.0) return "请输入整数"
        if (spec.allowUnlimited && num == -1.0) return ""
        val min = spec.minimum
        val max = spec.maximum
        if (min != null && num < min) {
            val minText = if (min % 1.0 == 0.0) min.toLong().toString() else min.toString()
            return "不能小于 $minText"
        }
        if (max != null && num > max) {
            val maxText = if (max % 1.0 == 0.0) max.toLong().toString() else max.toString()
            return "不能大于 $maxText"
        }
    } else if (spec.type == "array" && v is JSONArray && spec.maxItems != null && v.length() > spec.maxItems) {
        return "最多选 ${spec.maxItems} 项"
    } else if (spec.type == "string" && v is String) {
        if (spec.minLength != null && v.length < spec.minLength) return "至少 ${spec.minLength} 字"
        val maxLen = spec.maxLength
        if (maxLen != null && v.length > maxLen) return "最多 $maxLen 字"
        if (spec.pattern.isNotBlank()) {
            val valid = runCatching { Regex(spec.pattern).containsMatchIn(v) }.getOrDefault(true)
            if (!valid) return "格式不正确"
        }
    }
    return ""
}

internal fun hasExtensionSchemaErrors(definition: PluginDefinition, settingsJson: String): Boolean {
    val values = runCatching { JSONObject(settingsJson) }.getOrDefault(JSONObject())
    val sections = parseSchemaSections(definition)
    return sections.any { section ->
        section.fields.any { spec -> schemaFieldError(spec, values).isNotBlank() }
    }
}

internal fun isValidQuietWindowTime(value: String): Boolean = QUIET_TIME_REGEX.matches(value.trim())

internal fun isValidResourceKey(key: String): Boolean = RESOURCE_KEY_REGEX.matches(key.trim())

// =========================================================================
// Shared UI Primitives for Plugin Editors
// =========================================================================

@Composable
private fun PluginStepperRow(
    title: String,
    valueText: String,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    canDecrease: Boolean = true,
    canIncrease: Boolean = true,
) {
    val apple = LocalAppleColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = apple.primaryText,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onDecrease, enabled = canDecrease) {
            Icon(Icons.Rounded.Remove, contentDescription = "减少")
        }
        Text(
            text = valueText,
            style = MaterialTheme.typography.titleSmall.copy(fontFeatureSettings = "tnum"),
            color = apple.primaryText,
        )
        IconButton(onClick = onIncrease, enabled = canIncrease) {
            Icon(Icons.Rounded.Add, contentDescription = "增加")
        }
    }
}

@Composable
private fun PausedNoticeBanner() {
    val apple = LocalAppleColors.current
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = apple.orange.copy(alpha = 0.14f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = "当前账号已暂停该模块，修改保存后将在恢复运行时生效",
            style = MaterialTheme.typography.bodySmall,
            color = apple.orange,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
        )
    }
}

// =========================================================================
// Official Built-in Module Editors
// =========================================================================

@Composable
internal fun BuiltinDailyFarmSettingsTab(
    model: AccountSettingsModel,
    paused: Boolean,
    onChange: (AccountSettingsModel) -> Unit,
) {
    val apple = LocalAppleColors.current
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (paused) PausedNoticeBanner()

        AppleSectionHeader(title = "日常农场开关")
        AppleGroupedCard {
            BUILTIN_FARM_SWITCHES.forEachIndexed { idx, (key, label) ->
                if (idx > 0) AppleHairlineDivider(startIndent = 16.dp)
                AppleSwitchRow(
                    icon = Icons.Rounded.Agriculture,
                    iconTint = apple.green,
                    title = label,
                    checked = model.automationBooleans[key] ?: false,
                    onCheckedChange = { checked ->
                        onChange(model.copy(automationBooleans = model.automationBooleans + (key to checked)))
                    },
                )
            }
            AppleHairlineDivider(startIndent = 16.dp)
            PluginStepperRow(
                title = "自动邮箱领取时间",
                valueText = "${model.emailClaimHour} 时",
                onDecrease = {
                    val prev = if (model.emailClaimHour <= 0) 23 else model.emailClaimHour - 1
                    onChange(model.copy(emailClaimHour = prev))
                },
                onIncrease = {
                    val next = if (model.emailClaimHour >= 23) 0 else model.emailClaimHour + 1
                    onChange(model.copy(emailClaimHour = next))
                },
            )
        }

        AppleSectionHeader(title = "巡田间隔")
        AppleGroupedCard {
            PluginStepperRow(
                title = "农场巡查最小 (秒)",
                valueText = "${model.farmMinSec}s",
                canIncrease = model.farmMinSec < 86_400,
                canDecrease = model.farmMinSec > 1,
                onDecrease = {
                    val nextMin = (model.farmMinSec - 1).coerceAtLeast(1)
                    onChange(model.copy(farmMinSec = nextMin, farmMaxSec = model.farmMaxSec.coerceAtLeast(nextMin)))
                },
                onIncrease = {
                    val nextMin = model.farmMinSec + 1
                    onChange(model.copy(farmMinSec = nextMin, farmMaxSec = model.farmMaxSec.coerceAtLeast(nextMin)))
                },
            )
            AppleHairlineDivider(startIndent = 16.dp)
            PluginStepperRow(
                title = "农场巡查最大 (秒)",
                valueText = "${model.farmMaxSec}s",
                canIncrease = model.farmMaxSec < 86_400,
                canDecrease = model.farmMaxSec > model.farmMinSec,
                onDecrease = {
                    onChange(model.copy(farmMaxSec = (model.farmMaxSec - 1).coerceAtLeast(model.farmMinSec)))
                },
                onIncrease = {
                    onChange(model.copy(farmMaxSec = model.farmMaxSec + 1))
                },
            )
        }
    }
}

@Composable
internal fun BuiltinDailyFarmStrategyTab(
    model: AccountSettingsModel,
    onEditBagPriority: () -> Unit,
    seeds: List<SeedOption>,
    paused: Boolean,
    onChange: (AccountSettingsModel) -> Unit,
    onPickSeed: () -> Unit,
) {
    val apple = LocalAppleColors.current
    var showFallbackPicker by remember { mutableStateOf(false) }
    val needsPreferred = model.plantingStrategy == "preferred" ||
        (model.plantingStrategy == "bag_priority" && model.bagSeedFallbackStrategy == "preferred")

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (paused) PausedNoticeBanner()

        AppleSectionHeader(title = "种植策略")
        AppleGroupedCard {
            BUILTIN_PLANTING_STRATEGIES.forEachIndexed { idx, (code, label) ->
                if (idx > 0) AppleHairlineDivider(startIndent = 16.dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onChange(model.copy(plantingStrategy = code)) }
                        .padding(horizontal = 16.dp, vertical = 13.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(label, style = MaterialTheme.typography.bodyLarge, color = apple.primaryText)
                    if (model.plantingStrategy == code) {
                        Icon(Icons.Rounded.Check, contentDescription = "已选", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        if (model.plantingStrategy == "bag_priority") {
            val fallbackLabel = BUILTIN_BAG_FALLBACK_STRATEGIES.firstOrNull {
                it.first == model.bagSeedFallbackStrategy
            }?.second ?: model.bagSeedFallbackStrategy
            AppleSectionHeader(title = "背包种子策略")
            AppleGroupedCard {
                AppleSettingRow(
                    icon = Icons.Rounded.Eco,
                    iconTint = apple.green,
                    title = "背包种子优先顺序",
                    trailingText = if (model.bagSeedPriority.isNotEmpty()) "已排 ${model.bagSeedPriority.size} 项" else "默认顺序",
                    showChevron = true,
                    onClick = onEditBagPriority,
                )
                AppleHairlineDivider(startIndent = 16.dp)
                AppleSettingRow(
                    icon = Icons.Rounded.Tune,
                    iconTint = apple.blue,
                    title = "库存不足时回退策略",
                    trailingText = fallbackLabel,
                    showChevron = true,
                    onClick = { showFallbackPicker = true },
                )
                AppleHairlineDivider(startIndent = 16.dp)
                AppleSwitchRow(
                    icon = Icons.Rounded.Eco,
                    iconTint = apple.emerald,
                    title = "多季预留高等级/活动地块",
                    checked = model.bagSeedMultiLandReservationEnabled,
                    onCheckedChange = { onChange(model.copy(bagSeedMultiLandReservationEnabled = it)) },
                )
            }
        }

        if (needsPreferred) {
            val matched = seeds.firstOrNull { it.seedId == model.preferredSeedId }
            AppleGroupedCard {
                AppleSettingRow(
                    icon = Icons.Rounded.Eco,
                    iconTint = apple.green,
                    title = "指定优先种子",
                    subtitle = matched?.name ?: if (model.preferredSeedId > 0L) "种子 ID ${model.preferredSeedId}" else "请选择优先种植的种子",
                    showChevron = true,
                    onClick = onPickSeed,
                )
            }
        }

        AppleSectionHeader(title = "种植节奏")
        AppleGroupedCard {
            AppleSwitchRow(
                icon = Icons.Rounded.Eco,
                iconTint = apple.emerald,
                title = "随机打乱种植顺序",
                checked = model.plantOrderRandom,
                onCheckedChange = { onChange(model.copy(plantOrderRandom = it)) },
            )
            AppleHairlineDivider(startIndent = 16.dp)
            PluginStepperRow(
                title = "地块种植延迟 (秒)",
                valueText = "${model.plantDelaySeconds}s",
                canDecrease = model.plantDelaySeconds > 0,
                canIncrease = model.plantDelaySeconds < 60,
                onDecrease = { onChange(model.copy(plantDelaySeconds = (model.plantDelaySeconds - 1).coerceAtLeast(0))) },
                onIncrease = { onChange(model.copy(plantDelaySeconds = (model.plantDelaySeconds + 1).coerceAtMost(60))) },
            )
        }
    }

    if (showFallbackPicker) {
        AppleAlertDialog(
            onDismissRequest = { showFallbackPicker = false },
            title = { Text("库存不足时回退策略") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    BUILTIN_BAG_FALLBACK_STRATEGIES.forEach { (code, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    onChange(model.copy(bagSeedFallbackStrategy = code))
                                    showFallbackPicker = false
                                }
                                .padding(horizontal = 10.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(label, style = MaterialTheme.typography.bodyLarge, color = apple.primaryText)
                            if (model.bagSeedFallbackStrategy == code) {
                                Icon(Icons.Rounded.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showFallbackPicker = false }) { Text("关闭") }
            },
        )
    }
}

@Composable
internal fun BuiltinDailyRanchSettingsTab(
    model: AccountSettingsModel,
    paused: Boolean,
    onChange: (AccountSettingsModel) -> Unit,
) {
    val apple = LocalAppleColors.current
    val grassEnabled = model.automationBooleans["ranch_grass"] ?: true
    val normalCanEnabled = model.automationBooleans["ranch_feed_normal"] ?: true
    val organicCanEnabled = model.automationBooleans["ranch_feed_organic"] ?: true
    var pickingCanStageTarget by remember { mutableStateOf<String?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (paused) PausedNoticeBanner()

        AppleSectionHeader(title = "日常牧场开关与阈值")
        AppleGroupedCard {
            BUILTIN_RANCH_SWITCHES.forEachIndexed { idx, (key, label) ->
                if (idx > 0) AppleHairlineDivider(startIndent = 16.dp)
                AppleSwitchRow(
                    icon = Icons.Rounded.Pets,
                    iconTint = apple.orange,
                    title = label,
                    checked = model.automationBooleans[key] ?: true,
                    enabled = if (key == "ranch_grass_buy") grassEnabled else true,
                    onCheckedChange = { checked ->
                        onChange(model.copy(automationBooleans = model.automationBooleans + (key to checked)))
                    },
                )
            }
            AppleHairlineDivider(startIndent = 16.dp)
            PluginStepperRow(
                title = "牧草余量低于时补满",
                valueText = "${model.ranchGrassThresholdPercent}%",
                canDecrease = model.ranchGrassThresholdPercent > 1,
                canIncrease = model.ranchGrassThresholdPercent < 100,
                onDecrease = {
                    val step = if (model.ranchGrassThresholdPercent > 5) 5 else 1
                    onChange(model.copy(ranchGrassThresholdPercent = (model.ranchGrassThresholdPercent - step).coerceAtLeast(1)))
                },
                onIncrease = {
                    onChange(model.copy(ranchGrassThresholdPercent = (model.ranchGrassThresholdPercent + 5).coerceAtMost(100)))
                },
            )
            AppleHairlineDivider(startIndent = 16.dp)
            PluginStepperRow(
                title = "牧场巡查间隔",
                valueText = "${model.ranchCheckMinutes} 分钟",
                canDecrease = model.ranchCheckMinutes > 1,
                canIncrease = model.ranchCheckMinutes < 60,
                onDecrease = {
                    onChange(model.copy(ranchCheckMinutes = (model.ranchCheckMinutes - 1).coerceAtLeast(1)))
                },
                onIncrease = {
                    onChange(model.copy(ranchCheckMinutes = (model.ranchCheckMinutes + 1).coerceAtMost(60)))
                },
            )
        }

        AppleSectionHeader(title = "罐头喂养策略")
        AppleGroupedCard {
            AppleSwitchRow(
                icon = Icons.Rounded.Science,
                iconTint = apple.orange,
                title = "自动填充库存罐头",
                checked = model.automationBooleans["ranch_fill_cans"] ?: true,
                onCheckedChange = { checked ->
                    onChange(model.copy(automationBooleans = model.automationBooleans + ("ranch_fill_cans" to checked)))
                },
            )
            AppleHairlineDivider(startIndent = 16.dp)
            AppleSwitchRow(
                icon = Icons.Rounded.Science,
                iconTint = apple.blue,
                title = "普通罐头喂养",
                checked = normalCanEnabled,
                onCheckedChange = { checked ->
                    onChange(model.copy(automationBooleans = model.automationBooleans + ("ranch_feed_normal" to checked)))
                },
            )
            model.ranchFeedNormalStage?.let { stage ->
                AppleSettingRow(
                    icon = Icons.Rounded.Schedule,
                    iconTint = apple.blue,
                    title = "普通罐头使用时机",
                    trailingText = ranchCanStageLabel(stage),
                    showChevron = true,
                    modifier = Modifier.alpha(if (normalCanEnabled) 1f else 0.45f),
                    onClick = if (normalCanEnabled) {
                        { pickingCanStageTarget = "normal" }
                    } else null,
                )
            }
            AppleHairlineDivider(startIndent = 16.dp)
            AppleSwitchRow(
                icon = Icons.Rounded.Science,
                iconTint = apple.emerald,
                title = "有机罐头喂养",
                checked = organicCanEnabled,
                onCheckedChange = { checked ->
                    onChange(model.copy(automationBooleans = model.automationBooleans + ("ranch_feed_organic" to checked)))
                },
            )
            model.ranchFeedOrganicStage?.let { stage ->
                AppleSettingRow(
                    icon = Icons.Rounded.Schedule,
                    iconTint = apple.emerald,
                    title = "有机罐头使用时机",
                    trailingText = ranchCanStageLabel(stage),
                    showChevron = true,
                    modifier = Modifier.alpha(if (organicCanEnabled) 1f else 0.45f),
                    onClick = if (organicCanEnabled) {
                        { pickingCanStageTarget = "organic" }
                    } else null,
                )
            }
        }

        AppleSectionHeader(title = "牧场好友互动")
        AppleGroupedCard {
            BUILTIN_RANCH_FRIEND_SWITCHES.forEachIndexed { idx, (key, label, defaultEnabled) ->
                if (idx > 0) AppleHairlineDivider(startIndent = 16.dp)
                AppleSwitchRow(
                    icon = Icons.Rounded.People,
                    iconTint = apple.blue,
                    title = label,
                    checked = model.automationBooleans[key] ?: defaultEnabled,
                    onCheckedChange = { checked ->
                        onChange(model.copy(automationBooleans = model.automationBooleans + (key to checked)))
                    },
                )
            }
        }
    }

    pickingCanStageTarget?.let { target ->
        val isOrganic = target == "organic"
        val currentValue = if (isOrganic) model.ranchFeedOrganicStage else model.ranchFeedNormalStage
        val stageOptions = if (model.ranchFeedOrganicStage != null) RANCH_CAN_STAGE_OPTIONS else {
            RANCH_CAN_STAGE_OPTIONS.filter { it.first == "any" || it.first == "longest" }
        }
        AppleAlertDialog(
            onDismissRequest = { pickingCanStageTarget = null },
            title = { Text(if (isOrganic) "有机罐头使用时机" else "普通罐头使用时机") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    stageOptions.forEachIndexed { idx, (value, label, desc) ->
                        if (idx > 0) AppleHairlineDivider()
                        val selected = currentValue == value
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    onChange(
                                        if (isOrganic) {
                                            model.copy(ranchFeedOrganicStage = value)
                                        } else {
                                            model.copy(ranchFeedNormalStage = value)
                                        }
                                    )
                                    pickingCanStageTarget = null
                                }
                                .padding(horizontal = 8.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = if (selected) MaterialTheme.colorScheme.primary else apple.primaryText,
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = desc,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = apple.secondaryText,
                                )
                            }
                            if (selected) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = "已选",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { pickingCanStageTarget = null }) { Text("关闭") }
            },
        )
    }
}

@Composable
internal fun BuiltinDailyRanchStrategyTab(
    model: AccountSettingsModel,
    onEditBagPriority: () -> Unit,
    animalOptions: List<RanchAnimalOption>,
    paused: Boolean,
    onChange: (AccountSettingsModel) -> Unit,
    onPickAnimal: (String) -> Unit,
) {
    val apple = LocalAppleColors.current
    val coopName = animalOptions.firstOrNull { it.value == model.ranchCoopAnimal }?.label
        ?: if (model.ranchCoopAnimal == 0) "不购买" else "动物 #${model.ranchCoopAnimal}"
    val shedName = animalOptions.firstOrNull { it.value == model.ranchShedAnimal }?.label
        ?: if (model.ranchShedAnimal == 0) "不购买" else "动物 #${model.ranchShedAnimal}"
    val needsPreferred = model.ranchStrategy == "preferred" ||
        (model.ranchStrategy == "bag_priority" && model.ranchBagFallback == "preferred")

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (paused) PausedNoticeBanner()

        AppleSectionHeader(title = "养殖策略")
        AppleGroupedCard {
            BUILTIN_RANCH_STRATEGIES.forEachIndexed { idx, (code, label) ->
                if (idx > 0) AppleHairlineDivider(startIndent = 16.dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onChange(model.copy(ranchStrategy = code)) }
                        .padding(horizontal = 16.dp, vertical = 13.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(label, style = MaterialTheme.typography.bodyLarge, color = apple.primaryText)
                    if (model.ranchStrategy == code) {
                        Icon(Icons.Rounded.Check, contentDescription = "已选", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            if (model.ranchStrategy == "bag_priority") {
                AppleHairlineDivider(startIndent = 16.dp)
                val priorityCount = model.ranchCoopCubPriority.size + model.ranchShedCubPriority.size
                AppleSettingRow(
                    icon = Icons.Rounded.Pets,
                    iconTint = apple.orange,
                    title = "背包幼崽优先顺序",
                    trailingText = if (priorityCount > 0) "已排 ${priorityCount} 项" else "默认顺序",
                    showChevron = true,
                    onClick = onEditBagPriority,
                )
                AppleHairlineDivider(startIndent = 16.dp)
                val fallbackLabel = if (model.ranchBagFallback == "preferred") "指定动物" else "按已解锁等级"
                AppleSettingRow(
                    icon = Icons.Rounded.Pets,
                    iconTint = apple.orange,
                    title = "同类幼崽用完后",
                    trailingText = fallbackLabel,
                    showChevron = true,
                    onClick = {
                        val next = if (model.ranchBagFallback == "preferred") "level" else "preferred"
                        onChange(model.copy(ranchBagFallback = next))
                    },
                )
            }
            if (needsPreferred) {
                AppleHairlineDivider(startIndent = 16.dp)
                AppleSettingRow(
                    icon = Icons.Rounded.Pets,
                    iconTint = apple.orange,
                    title = "窝 · 指定动物",
                    trailingText = coopName,
                    showChevron = true,
                    onClick = { onPickAnimal("窝") },
                )
                AppleHairlineDivider(startIndent = 16.dp)
                AppleSettingRow(
                    icon = Icons.Rounded.Pets,
                    iconTint = apple.orange,
                    title = "棚 · 指定动物",
                    trailingText = shedName,
                    showChevron = true,
                    onClick = { onPickAnimal("棚") },
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun BuiltinFertilizerSettingsTab(
    model: AccountSettingsModel,
    paused: Boolean,
    onChange: (AccountSettingsModel) -> Unit,
) {
    val apple = LocalAppleColors.current
    val buyOrganic = model.automationBooleans["fertilizer_buy_organic"] ?: false
    val buyNormal = model.automationBooleans["fertilizer_buy_normal"] ?: false

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (paused) PausedNoticeBanner()

        AppleSectionHeader(title = "施肥策略")
        AppleGroupedCard {
            BUILTIN_FERTILIZER_OPTIONS.forEachIndexed { idx, (code, label) ->
                if (idx > 0) AppleHairlineDivider(startIndent = 16.dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onChange(model.copy(fertilizerMode = code)) }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(label, style = MaterialTheme.typography.bodyLarge, color = apple.primaryText)
                    if (model.fertilizerMode == code) {
                        Icon(Icons.Rounded.Check, contentDescription = "已选", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            if (model.fertilizerMode == "smart") {
                AppleHairlineDivider(startIndent = 16.dp)
                PluginStepperRow(
                    title = "快成熟判定 (秒)",
                    valueText = "${model.fertilizerSmartSeconds}s",
                    canDecrease = model.fertilizerSmartSeconds > 30,
                    canIncrease = model.fertilizerSmartSeconds < 3600,
                    onDecrease = {
                        onChange(model.copy(fertilizerSmartSeconds = (model.fertilizerSmartSeconds - 30).coerceAtLeast(30)))
                    },
                    onIncrease = {
                        onChange(model.copy(fertilizerSmartSeconds = (model.fertilizerSmartSeconds + 30).coerceAtMost(3600)))
                    },
                )
            }
            AppleHairlineDivider(startIndent = 16.dp)
            AppleSwitchRow(
                icon = Icons.Rounded.Science,
                iconTint = apple.emerald,
                title = "多季补肥",
                checked = model.automationBooleans["fertilizer_multi_season"] ?: false,
                onCheckedChange = { checked ->
                    onChange(model.copy(automationBooleans = model.automationBooleans + ("fertilizer_multi_season" to checked)))
                },
            )
            AppleHairlineDivider(startIndent = 16.dp)
            AppleSwitchRow(
                icon = Icons.Rounded.Science,
                iconTint = apple.blue,
                title = "显示手动施肥按钮",
                checked = model.automationBooleans["show_manual_fertilizer"] ?: true,
                onCheckedChange = { checked ->
                    onChange(model.copy(automationBooleans = model.automationBooleans + ("show_manual_fertilizer" to checked)))
                },
            )
        }

        AppleSectionHeader(title = "施肥范围（适用土地）")
        AppleGroupedCard(contentPadding = PaddingValues(14.dp)) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                BUILTIN_FERTILIZER_LAND_TYPES.forEach { (key, label) ->
                    val selected = key in model.fertilizerLandTypes
                    AppleFilterChip(
                        selected = selected,
                        onClick = {
                            val next = if (selected) {
                                model.fertilizerLandTypes.filterNot { it == key }
                            } else {
                                model.fertilizerLandTypes + key
                            }
                            onChange(model.copy(fertilizerLandTypes = next))
                        },
                        label = { Text(label) },
                    )
                }
            }
        }

        AppleSectionHeader(title = "自动购买化肥")
        AppleGroupedCard {
            AppleSwitchRow(
                icon = Icons.Rounded.Science,
                iconTint = apple.green,
                title = "自动购买有机化肥",
                checked = buyOrganic,
                onCheckedChange = { checked ->
                    onChange(model.copy(automationBooleans = model.automationBooleans + ("fertilizer_buy_organic" to checked)))
                },
            )
            if (buyOrganic) {
                AppleHairlineDivider(startIndent = 16.dp)
                PluginStepperRow(
                    title = "有机肥单次购买数量",
                    valueText = "${model.fertilizerBuyOrganicCount} 个",
                    canDecrease = model.fertilizerBuyOrganicCount > 0,
                    canIncrease = model.fertilizerBuyOrganicCount < 10000,
                    onDecrease = {
                        onChange(model.copy(fertilizerBuyOrganicCount = (model.fertilizerBuyOrganicCount - 1).coerceAtLeast(0)))
                    },
                    onIncrease = {
                        onChange(model.copy(fertilizerBuyOrganicCount = (model.fertilizerBuyOrganicCount + 1).coerceAtMost(10000)))
                    },
                )
                AppleHairlineDivider(startIndent = 16.dp)
                PluginStepperRow(
                    title = "有机肥触发阈值 (小时)",
                    valueText = "${model.fertilizerBuyOrganicThresholdHours}h",
                    canDecrease = model.fertilizerBuyOrganicThresholdHours > 0,
                    canIncrease = model.fertilizerBuyOrganicThresholdHours < 990,
                    onDecrease = {
                        onChange(model.copy(fertilizerBuyOrganicThresholdHours = (model.fertilizerBuyOrganicThresholdHours - 1).coerceAtLeast(0)))
                    },
                    onIncrease = {
                        onChange(model.copy(fertilizerBuyOrganicThresholdHours = (model.fertilizerBuyOrganicThresholdHours + 1).coerceAtMost(990)))
                    },
                )
            }
            AppleHairlineDivider(startIndent = 16.dp)
            AppleSwitchRow(
                icon = Icons.Rounded.Science,
                iconTint = apple.emerald,
                title = "自动购买普通化肥",
                checked = buyNormal,
                onCheckedChange = { checked ->
                    onChange(model.copy(automationBooleans = model.automationBooleans + ("fertilizer_buy_normal" to checked)))
                },
            )
            if (buyNormal) {
                AppleHairlineDivider(startIndent = 16.dp)
                PluginStepperRow(
                    title = "普通肥单次购买数量",
                    valueText = "${model.fertilizerBuyNormalCount} 个",
                    canDecrease = model.fertilizerBuyNormalCount > 0,
                    canIncrease = model.fertilizerBuyNormalCount < 10000,
                    onDecrease = {
                        onChange(model.copy(fertilizerBuyNormalCount = (model.fertilizerBuyNormalCount - 1).coerceAtLeast(0)))
                    },
                    onIncrease = {
                        onChange(model.copy(fertilizerBuyNormalCount = (model.fertilizerBuyNormalCount + 1).coerceAtMost(10000)))
                    },
                )
                AppleHairlineDivider(startIndent = 16.dp)
                PluginStepperRow(
                    title = "普通肥触发阈值 (小时)",
                    valueText = "${model.fertilizerBuyNormalThresholdHours}h",
                    canDecrease = model.fertilizerBuyNormalThresholdHours > 0,
                    canIncrease = model.fertilizerBuyNormalThresholdHours < 990,
                    onDecrease = {
                        onChange(model.copy(fertilizerBuyNormalThresholdHours = (model.fertilizerBuyNormalThresholdHours - 1).coerceAtLeast(0)))
                    },
                    onIncrease = {
                        onChange(model.copy(fertilizerBuyNormalThresholdHours = (model.fertilizerBuyNormalThresholdHours + 1).coerceAtMost(990)))
                    },
                )
            }
            if (buyOrganic || buyNormal) {
                AppleHairlineDivider(startIndent = 16.dp)
                PluginStepperRow(
                    title = "检测间隔 (分钟)",
                    valueText = "${model.fertilizerBuyCheckIntervalMinutes} 分钟",
                    canDecrease = model.fertilizerBuyCheckIntervalMinutes > 1,
                    canIncrease = model.fertilizerBuyCheckIntervalMinutes < 1440,
                    onDecrease = {
                        val step = if (model.fertilizerBuyCheckIntervalMinutes > 5) 5 else 1
                        onChange(model.copy(fertilizerBuyCheckIntervalMinutes = (model.fertilizerBuyCheckIntervalMinutes - step).coerceAtLeast(1)))
                    },
                    onIncrease = {
                        onChange(model.copy(fertilizerBuyCheckIntervalMinutes = (model.fertilizerBuyCheckIntervalMinutes + 5).coerceAtMost(1440)))
                    },
                )
            }
        }
    }
}

@Composable
internal fun BuiltinFriendsSettingsTab(
    model: AccountSettingsModel,
    paused: Boolean,
    onChange: (AccountSettingsModel) -> Unit,
) {
    val apple = LocalAppleColors.current
    val autoAccept = model.automationBooleans["friend_auto_accept"] ?: false

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (paused) PausedNoticeBanner()

        AppleSectionHeader(title = "互动行为")
        AppleGroupedCard {
            BUILTIN_FRIEND_SWITCHES.forEachIndexed { idx, (key, label) ->
                if (idx > 0) AppleHairlineDivider(startIndent = 16.dp)
                AppleSwitchRow(
                    icon = Icons.Rounded.People,
                    iconTint = apple.blue,
                    title = label,
                    checked = model.automationBooleans[key] ?: false,
                    onCheckedChange = { checked ->
                        onChange(model.copy(automationBooleans = model.automationBooleans + (key to checked)))
                    },
                )
            }
        }

        AppleSectionHeader(title = "巡查间隔与蹲守延迟")
        AppleGroupedCard {
            PluginStepperRow(
                title = "好友摘要刷新最小 (秒)",
                valueText = "${model.friendMinSec}s",
                canIncrease = model.friendMinSec < 86_400,
                canDecrease = model.friendMinSec > 1,
                onDecrease = {
                    val nextMin = (model.friendMinSec - 1).coerceAtLeast(1)
                    onChange(model.copy(friendMinSec = nextMin, friendMaxSec = model.friendMaxSec.coerceAtLeast(nextMin)))
                },
                onIncrease = {
                    val nextMin = model.friendMinSec + 1
                    onChange(model.copy(friendMinSec = nextMin, friendMaxSec = model.friendMaxSec.coerceAtLeast(nextMin)))
                },
            )
            AppleHairlineDivider(startIndent = 16.dp)
            PluginStepperRow(
                title = "好友摘要刷新最大 (秒)",
                valueText = "${model.friendMaxSec}s",
                canIncrease = model.friendMaxSec < 86_400,
                canDecrease = model.friendMaxSec > model.friendMinSec,
                onDecrease = {
                    onChange(model.copy(friendMaxSec = (model.friendMaxSec - 1).coerceAtLeast(model.friendMinSec)))
                },
                onIncrease = {
                    onChange(model.copy(friendMaxSec = model.friendMaxSec + 1))
                },
            )
            AppleHairlineDivider(startIndent = 16.dp)
            PluginStepperRow(
                title = "偷菜延迟 (秒)",
                valueText = "${model.stealDelaySeconds}s",
                canDecrease = model.stealDelaySeconds > 0,
                canIncrease = model.stealDelaySeconds < 300,
                onDecrease = { onChange(model.copy(stealDelaySeconds = (model.stealDelaySeconds - 1).coerceAtLeast(0))) },
                onIncrease = { onChange(model.copy(stealDelaySeconds = (model.stealDelaySeconds + 1).coerceAtMost(300))) },
            )
        }

        AppleSectionHeader(title = "好友静默时段")
        AppleGroupedCard {
            AppleSwitchRow(
                icon = Icons.Rounded.Schedule,
                iconTint = apple.indigo,
                title = "启用静默时段",
                checked = model.quietHoursEnabled,
                onCheckedChange = { onChange(model.copy(quietHoursEnabled = it)) },
            )
            if (model.quietHoursEnabled) {
                AppleHairlineDivider(startIndent = 16.dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    OutlinedTextField(
                        value = model.quietHoursStart,
                        onValueChange = { onChange(model.copy(quietHoursStart = it)) },
                        label = { Text("开始 HH:mm") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = model.quietHoursEnd,
                        onValueChange = { onChange(model.copy(quietHoursEnd = it)) },
                        label = { Text("结束 HH:mm") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f),
                    )
                }
                AppleHairlineDivider(startIndent = 16.dp)
                AppleSwitchRow(
                    icon = Icons.Rounded.Eco,
                    iconTint = apple.green,
                    title = "静默时段继续农场巡查",
                    checked = model.quietContinueFarm,
                    onCheckedChange = { onChange(model.copy(quietContinueFarm = it)) },
                )
            }
        }

        AppleSectionHeader(title = "好友申请")
        AppleGroupedCard {
            AppleSwitchRow(
                icon = Icons.Rounded.People,
                iconTint = apple.emerald,
                title = "自动通过好友申请",
                checked = autoAccept,
                onCheckedChange = { checked ->
                    onChange(model.copy(automationBooleans = model.automationBooleans + ("friend_auto_accept" to checked)))
                },
            )
            if (autoAccept) {
                AppleHairlineDivider(startIndent = 16.dp)
                PluginStepperRow(
                    title = "最低等级 (0 为不限)",
                    valueText = "Lv.${model.autoAcceptFriendMinLevel}",
                    canDecrease = model.autoAcceptFriendMinLevel > 0,
                    canIncrease = model.autoAcceptFriendMinLevel < 200,
                    onDecrease = {
                        onChange(model.copy(autoAcceptFriendMinLevel = (model.autoAcceptFriendMinLevel - 1).coerceAtLeast(0)))
                    },
                    onIncrease = {
                        onChange(model.copy(autoAcceptFriendMinLevel = (model.autoAcceptFriendMinLevel + 1).coerceAtMost(200)))
                    },
                )
                AppleHairlineDivider(startIndent = 16.dp)
                AppleSwitchRow(
                    icon = Icons.Rounded.People,
                    iconTint = apple.orange,
                    title = "不低于自己等级",
                    checked = model.autoAcceptRequireOwnLevel,
                    onCheckedChange = { onChange(model.copy(autoAcceptRequireOwnLevel = it)) },
                )
                AppleHairlineDivider(startIndent = 16.dp)
                AppleSwitchRow(
                    icon = Icons.Rounded.People,
                    iconTint = apple.red,
                    title = "按收偷比过滤 (${model.autoAcceptHarvestStealHarvest}:${model.autoAcceptHarvestStealSteal})",
                    subtitle = "不满足的申请直接拒绝",
                    checked = model.autoAcceptHarvestStealEnabled,
                    onCheckedChange = { onChange(model.copy(autoAcceptHarvestStealEnabled = it)) },
                )
                if (model.autoAcceptHarvestStealEnabled) {
                    AppleHairlineDivider(startIndent = 16.dp)
                    PluginStepperRow(
                        title = "收获比例",
                        valueText = model.autoAcceptHarvestStealHarvest.toString(),
                        canDecrease = model.autoAcceptHarvestStealHarvest > 0,
                        canIncrease = model.autoAcceptHarvestStealHarvest < 9999,
                        onDecrease = {
                            onChange(model.copy(autoAcceptHarvestStealHarvest = (model.autoAcceptHarvestStealHarvest - 1).coerceAtLeast(0)))
                        },
                        onIncrease = {
                            onChange(model.copy(autoAcceptHarvestStealHarvest = (model.autoAcceptHarvestStealHarvest + 1).coerceAtMost(9999)))
                        },
                    )
                    AppleHairlineDivider(startIndent = 16.dp)
                    PluginStepperRow(
                        title = "偷菜比例",
                        valueText = model.autoAcceptHarvestStealSteal.toString(),
                        canDecrease = model.autoAcceptHarvestStealSteal > 1,
                        canIncrease = model.autoAcceptHarvestStealSteal < 9999,
                        onDecrease = {
                            onChange(model.copy(autoAcceptHarvestStealSteal = (model.autoAcceptHarvestStealSteal - 1).coerceAtLeast(1)))
                        },
                        onIncrease = {
                            onChange(model.copy(autoAcceptHarvestStealSteal = (model.autoAcceptHarvestStealSteal + 1).coerceAtMost(9999)))
                        },
                    )
                }
            }
        }
    }
}

@Composable
internal fun BuiltinMysteryShopSettingsTab(
    model: AccountSettingsModel,
    paused: Boolean,
    onChange: (AccountSettingsModel) -> Unit,
) {
    val apple = LocalAppleColors.current
    val autoBuy = model.automationBooleans["mystery_shop_auto_buy"] ?: false

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (paused) PausedNoticeBanner()

        AppleSectionHeader(title = "神秘商人开关")
        AppleGroupedCard {
            AppleSwitchRow(
                icon = Icons.Rounded.Storefront,
                iconTint = apple.indigo,
                title = "自动购买",
                checked = autoBuy,
                onCheckedChange = { checked ->
                    onChange(model.copy(automationBooleans = model.automationBooleans + ("mystery_shop_auto_buy" to checked)))
                },
            )
            AppleHairlineDivider(startIndent = 16.dp)
            AppleSwitchRow(
                icon = Icons.Rounded.Storefront,
                iconTint = apple.blue,
                title = "到货提醒",
                checked = model.automationBooleans["mystery_shop_arrival_notify"] ?: false,
                onCheckedChange = { checked ->
                    onChange(model.copy(automationBooleans = model.automationBooleans + ("mystery_shop_arrival_notify" to checked)))
                },
            )
            AppleHairlineDivider(startIndent = 16.dp)
            AppleSwitchRow(
                icon = Icons.Rounded.Storefront,
                iconTint = apple.emerald,
                title = "购买提醒",
                checked = model.automationBooleans["mystery_shop_purchase_notify"] ?: false,
                enabled = autoBuy,
                onCheckedChange = { checked ->
                    onChange(model.copy(automationBooleans = model.automationBooleans + ("mystery_shop_purchase_notify" to checked)))
                },
            )
        }

        if (autoBuy) {
            AppleSectionHeader(title = "允许使用的货币")
            AppleGroupedCard {
                BUILTIN_MYSTERY_CURRENCIES.forEachIndexed { idx, (key, label) ->
                    if (idx > 0) AppleHairlineDivider(startIndent = 16.dp)
                    AppleSwitchRow(
                        icon = Icons.Rounded.Storefront,
                        iconTint = apple.orange,
                        title = label,
                        checked = model.automationBooleans[key] ?: false,
                        onCheckedChange = { checked ->
                            onChange(model.copy(automationBooleans = model.automationBooleans + (key to checked)))
                        },
                    )
                }
            }
            val anySelected = BUILTIN_MYSTERY_CURRENCIES.any { (key, _) -> model.automationBooleans[key] == true }
            if (!anySelected) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = apple.orange.copy(alpha = 0.14f),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = "尚未允许任何货币，自动购买将跳过全部商品。",
                        style = MaterialTheme.typography.bodySmall,
                        color = apple.orange,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun BuiltinStatusTab(
    detail: PluginAccountDetail,
    busy: Boolean,
    onTogglePause: (Boolean) -> Unit,
    onRefresh: () -> Unit,
    onResolveOp: (String, String, Boolean) -> Unit,
) {
    val apple = LocalAppleColors.current
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        AppleGroupedCard(contentPadding = PaddingValues(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        detail.paused -> apple.orange
                                        detail.connected -> apple.green
                                        else -> apple.secondaryText
                                    }
                                ),
                        )
                        Text(
                            text = when {
                                detail.paused -> "已暂停"
                                detail.connected -> "运行中"
                                else -> "等待账号上线"
                            },
                            style = MaterialTheme.typography.titleSmall,
                            color = apple.primaryText,
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (detail.running) (if (detail.connected) "账号在线" else "账号连接中") else "账号未运行",
                        style = MaterialTheme.typography.bodySmall,
                        color = apple.secondaryText,
                    )
                }
                AppleSwitch(
                    checked = !detail.paused,
                    onCheckedChange = { run -> onTogglePause(!run) },
                    enabled = !busy,
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            if (detail.enabledFeatures.isEmpty()) {
                Text("未开启任何功能", style = MaterialTheme.typography.bodySmall, color = apple.secondaryText)
            } else {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    detail.enabledFeatures.forEach { feature ->
                        AppleStatusPill(text = feature, color = MaterialTheme.colorScheme.primary, showDot = false)
                    }
                }
            }
        }

        AppleSectionHeader(
            title = if (detail.operations.isNotEmpty()) "待核对交易（${detail.operations.size}）" else "待核对交易",
            actionLabel = "刷新",
            onAction = onRefresh,
        )
        if (detail.nativeOperationsError.isNotBlank()) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = apple.red.copy(alpha = 0.12f),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = detail.nativeOperationsError,
                    style = MaterialTheme.typography.bodySmall,
                    color = apple.red,
                    modifier = Modifier.padding(12.dp),
                )
            }
        } else if (detail.operations.isEmpty()) {
            AppleGroupedCard(contentPadding = PaddingValues(14.dp)) {
                Text("暂无待核对交易", style = MaterialTheme.typography.bodySmall, color = apple.secondaryText)
            }
        } else {
            detail.operations.forEach { op ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = apple.orange.copy(alpha = 0.10f),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Rounded.WarningAmber, contentDescription = null, tint = apple.orange, modifier = Modifier.size(18.dp))
                            Text(op.kind, style = MaterialTheme.typography.titleSmall, color = apple.primaryText)
                        }
                        if (op.costs.isNotEmpty()) {
                            Text(
                                text = op.costs.joinToString("、") { c ->
                                    "${c.name.ifBlank { pluginResourceName(c.resourceKey) }} ×${formatPluginQuantity(c.amount)}"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = apple.secondaryText,
                            )
                        }
                        if (op.message.isNotBlank()) {
                            Text(op.message, style = MaterialTheme.typography.bodySmall, color = apple.secondaryText)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { onResolveOp(op.operationId, "confirmed", op.isNative) },
                                enabled = !busy,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f),
                            ) { Text("已生效") }

                            OutlinedButton(
                                onClick = { onResolveOp(op.operationId, "rejected", op.isNative) },
                                enabled = !busy,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f),
                            ) { Text("未生效") }
                        }
                    }
                }
            }
        }
    }
}

// =========================================================================
// Extension Plugin Editors (Config SchemaForm / Rules / Status & Logs)
// =========================================================================

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ExtensionConfigTab(
    definition: PluginDefinition,
    enabled: Boolean,
    settingsJson: String,
    conflicts: List<String>,
    disabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    onSettingsJsonChange: (String) -> Unit,
) {
    val apple = LocalAppleColors.current
    val values = remember(settingsJson) {
        runCatching { JSONObject(settingsJson) }.getOrDefault(JSONObject())
    }
    val sections = remember(definition.settingsSchemaJson, definition.uiSchemaJson) {
        parseSchemaSections(definition)
    }
    var helpDialog by remember { mutableStateOf<Pair<String, String>?>(null) }
    var selectDialogSpec by remember { mutableStateOf<SchemaFieldSpec?>(null) }

    fun updateValue(fieldName: String, nextVal: Any?) {
        val nextObj = runCatching { JSONObject(settingsJson) }.getOrDefault(JSONObject())
        if (nextVal == null) nextObj.put(fieldName, com.qqfarm.mobile.data.network.json.JSON_NULL) else nextObj.put(fieldName, nextVal)
        onSettingsJsonChange(nextObj.toString())
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        AppleGroupedCard {
            AppleSwitchRow(
                icon = Icons.Rounded.Tune,
                iconTint = MaterialTheme.colorScheme.primary,
                title = "启用",
                subtitle = if (enabled) "当前账号已绑定启用此脚本" else "关闭时不调度本脚本任务",
                checked = enabled,
                enabled = !disabled,
                onCheckedChange = onEnabledChange,
            )
        }

        if (conflicts.isNotEmpty()) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = apple.red.copy(alpha = 0.12f),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    conflicts.forEach { line ->
                        Text(line, style = MaterialTheme.typography.bodySmall, color = apple.red)
                    }
                }
            }
        }

        if (sections.isEmpty()) {
            AppleGroupedCard(contentPadding = PaddingValues(18.dp)) {
                Text("无可配置项", style = MaterialTheme.typography.bodyMedium, color = apple.secondaryText)
            }
        } else {
            sections.forEach { section ->
                val visibleFields = section.fields.filter { isFieldVisible(it, values) }
                if (visibleFields.isNotEmpty()) {
                    if (section.title.isNotBlank()) {
                        AppleSectionHeader(title = section.title)
                    }
                    AppleGroupedCard(contentPadding = PaddingValues(14.dp)) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            visibleFields.forEachIndexed { idx, spec ->
                                if (idx > 0) AppleHairlineDivider(startIndent = 0.dp)
                                val err = schemaFieldError(spec, values)
                                SchemaFieldItem(
                                    spec = spec,
                                    values = values,
                                    error = err,
                                    disabled = disabled,
                                    onHelpClick = { helpDialog = spec.title to spec.help },
                                    onSelectClick = { selectDialogSpec = spec },
                                    onValueChange = { updateValue(spec.name, it) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    helpDialog?.let { (title, content) ->
        AppleAlertDialog(
            onDismissRequest = { helpDialog = null },
            title = { Text(title) },
            text = { Text(content, modifier = Modifier.verticalScroll(rememberScrollState())) },
            confirmButton = {
                TextButton(onClick = { helpDialog = null }) { Text("知道了") }
            },
        )
    }

    selectDialogSpec?.let { spec ->
        val currentVal = values.opt(spec.name)
        AppleAlertDialog(
            onDismissRequest = { selectDialogSpec = null },
            title = { Text(spec.title) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    spec.options.forEach { opt ->
                        val selected = jsonValuesEqual(currentVal, opt.value)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    updateValue(spec.name, opt.value)
                                    selectDialogSpec = null
                                }
                                .padding(horizontal = 10.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(opt.label, style = MaterialTheme.typography.bodyLarge, color = apple.primaryText)
                            if (selected) {
                                Icon(Icons.Rounded.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectDialogSpec = null }) { Text("取消") }
            },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SchemaFieldItem(
    spec: SchemaFieldSpec,
    values: JSONObject,
    error: String,
    disabled: Boolean,
    onHelpClick: () -> Unit,
    onSelectClick: () -> Unit,
    onValueChange: (Any?) -> Unit,
) {
    val apple = LocalAppleColors.current
    val rawVal = values.opt(spec.name)

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (spec.widget != "switch") {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = spec.title,
                    style = MaterialTheme.typography.labelLarge,
                    color = apple.primaryText,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (spec.help.isNotBlank() && !spec.inlineHelp) {
                    IconButton(
                        onClick = onHelpClick,
                        modifier = Modifier.size(22.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Info,
                            contentDescription = "说明",
                            tint = apple.secondaryText,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }
        }

        when (spec.widget) {
            "switch" -> {
                val checked = rawVal as? Boolean ?: (rawVal?.toString()?.toBooleanStrictOrNull() ?: false)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !disabled) { onValueChange(!checked) },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = spec.title,
                            style = MaterialTheme.typography.bodyLarge,
                            color = apple.primaryText,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        if (spec.help.isNotBlank() && !spec.inlineHelp) {
                            IconButton(onClick = onHelpClick, modifier = Modifier.size(22.dp)) {
                                Icon(
                                    imageVector = Icons.Rounded.Info,
                                    contentDescription = "说明",
                                    tint = apple.secondaryText,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    AppleSwitch(checked = checked, onCheckedChange = null, enabled = !disabled)
                }
            }

            "number" -> {
                fun formatNum(v: Any?): String = when {
                    v == null || v == com.qqfarm.mobile.data.network.json.JSON_NULL -> ""
                    v is Double && v % 1.0 == 0.0 -> v.toLong().toString()
                    else -> v.toString()
                }
                var numText by remember(spec.name) { mutableStateOf(formatNum(rawVal)) }
                LaunchedEffect(rawVal) {
                    val incomingDouble = (rawVal as? Number)?.toDouble() ?: rawVal?.toString()?.toDoubleOrNull()
                    val currentDouble = numText.trim().toDoubleOrNull()
                    val incomingStr = formatNum(rawVal)
                    if (incomingStr != numText && (incomingDouble == null || currentDouble == null || incomingDouble != currentDouble)) {
                        numText = incomingStr
                    }
                }
                val isUnlimited = spec.allowUnlimited && numText.toDoubleOrNull() == -1.0
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value = numText,
                        onValueChange = { input ->
                            numText = input
                            val trimmed = input.trim()
                            if (trimmed.isEmpty()) {
                                onValueChange("")
                            } else if (spec.type == "integer") {
                                trimmed.toLongOrNull()?.let(onValueChange) ?: onValueChange(trimmed)
                            } else {
                                trimmed.toDoubleOrNull()?.takeIf { it.isFinite() }?.let(onValueChange) ?: onValueChange(trimmed)
                            }
                        },
                        enabled = !disabled,
                        singleLine = true,
                        isError = error.isNotBlank(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = if (spec.type == "integer") KeyboardType.Number else KeyboardType.Decimal,
                        ),
                        trailingIcon = {
                            if (!spec.allowUnlimited) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = {
                                            val cur = numText.toDoubleOrNull()?.takeIf { it.isFinite() } ?: (spec.minimum ?: 0.0)
                                            val next = cur - 1.0
                                            val clamped = spec.minimum?.let { kotlin.math.max(next, it) } ?: next
                                            val out: Any = if (spec.type == "integer") clamped.toLong() else clamped
                                            numText = out.toString()
                                            onValueChange(out)
                                        },
                                        enabled = !disabled && (spec.minimum == null || (numText.toDoubleOrNull() ?: spec.minimum) > spec.minimum),
                                    ) {
                                        Icon(Icons.Rounded.Remove, contentDescription = "减少")
                                    }
                                    IconButton(
                                        onClick = {
                                            val cur = numText.toDoubleOrNull()?.takeIf { it.isFinite() } ?: (spec.minimum ?: 0.0)
                                            val next = cur + 1.0
                                            val clamped = spec.maximum?.let { kotlin.math.min(next, it) } ?: next
                                            val out: Any = if (spec.type == "integer") clamped.toLong() else clamped
                                            numText = out.toString()
                                            onValueChange(out)
                                        },
                                        enabled = !disabled && (spec.maximum == null || (numText.toDoubleOrNull() ?: 0.0) < spec.maximum),
                                    ) {
                                        Icon(Icons.Rounded.Add, contentDescription = "增加")
                                    }
                                }
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f),
                    )
                    if (spec.allowUnlimited) {
                        AppleFilterChip(
                            selected = isUnlimited,
                            onClick = { onValueChange(-1L) },
                            enabled = !disabled,
                            label = { Text(if (isUnlimited) "不限" else "设为不限") },
                        )
                    }
                }
            }

            "select" -> {
                val chosenLabel = spec.options.firstOrNull { jsonValuesEqual(rawVal, it.value) }?.label
                    ?: rawVal?.takeIf { it != com.qqfarm.mobile.data.network.json.JSON_NULL }?.toString()
                    ?: "请选择"
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(enabled = !disabled, onClick = onSelectClick),
                    shape = RoundedCornerShape(12.dp),
                    color = apple.track,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(chosenLabel, style = MaterialTheme.typography.bodyMedium, color = apple.primaryText)
                        Icon(
                            imageVector = Icons.Rounded.Tune,
                            contentDescription = "选择",
                            tint = apple.secondaryText,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }

            "radio" -> {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    spec.options.forEach { opt ->
                        val selected = jsonValuesEqual(rawVal, opt.value)
                        AppleFilterChip(
                            selected = selected,
                            onClick = { if (!disabled) onValueChange(opt.value) },
                            label = { Text(opt.label) },
                        )
                    }
                }
            }

            "checkbox-group" -> {
                val arr = rawVal as? JSONArray ?: JSONArray()
                val currentItems = buildList {
                    for (i in 0 until arr.length()) add(arr.opt(i))
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    spec.options.forEach { opt ->
                        val checked = currentItems.any { jsonValuesEqual(it, opt.value) }
                        AppleFilterChip(
                            selected = checked,
                            onClick = {
                                if (!disabled) {
                                    val next = if (checked) {
                                        currentItems.filterNot { jsonValuesEqual(it, opt.value) }
                                    } else {
                                        currentItems + opt.value
                                    }
                                    onValueChange(JSONArray(next))
                                }
                            },
                            label = { Text(opt.label) },
                        )
                    }
                }
            }

            "textarea" -> {
                val str = if (rawVal == null || rawVal == com.qqfarm.mobile.data.network.json.JSON_NULL) "" else rawVal.toString()
                OutlinedTextField(
                    value = str,
                    onValueChange = { onValueChange(it) },
                    placeholder = if (spec.placeholder.isNotBlank()) ({ Text(spec.placeholder) }) else null,
                    enabled = !disabled,
                    minLines = 2,
                    maxLines = 6,
                    isError = error.isNotBlank(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            else -> {
                val str = if (rawVal == null || rawVal == com.qqfarm.mobile.data.network.json.JSON_NULL) "" else rawVal.toString()
                OutlinedTextField(
                    value = str,
                    onValueChange = { onValueChange(it) },
                    placeholder = if (spec.placeholder.isNotBlank()) ({ Text(spec.placeholder) }) else null,
                    enabled = !disabled,
                    singleLine = true,
                    isError = error.isNotBlank(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        if (error.isNotBlank()) {
            Text(error, style = MaterialTheme.typography.bodySmall, color = apple.red)
        } else if (spec.inlineHelp && spec.help.isNotBlank()) {
            Text(spec.help, style = MaterialTheme.typography.bodySmall, color = apple.secondaryText)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ExtensionRulesTab(
    quietWindows: List<PluginQuietWindow>,
    accountQuietWindows: List<PluginQuietWindow>,
    spendingPolicy: PluginSpendingPolicy,
    spendingUsage: List<PluginSpendingUsage>,
    resourceNames: Map<String, String>,
    disabled: Boolean,
    onQuietChange: (List<PluginQuietWindow>) -> Unit,
    onAccountQuietChange: (List<PluginQuietWindow>) -> Unit,
    onSpendingChange: (PluginSpendingPolicy) -> Unit,
) {
    val apple = LocalAppleColors.current
    var pickingResourceIdx by remember { mutableStateOf<Int?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        QuietWindowsSection(
            label = "静默时段",
            description = "仅限制当前脚本在该时段不触发运行（HH:mm，可跨午夜）",
            windows = quietWindows,
            disabled = disabled,
            onChange = onQuietChange,
        )

        QuietWindowsSection(
            label = "账号静默时段（所有脚本）",
            description = "该账号下全部扩展脚本共享的静默时段",
            windows = accountQuietWindows,
            disabled = disabled,
            onChange = onAccountQuietChange,
        )

        AppleSectionHeader(
            title = "自动消费与资源限额",
            actionLabel = if (spendingPolicy.rules.size < 16 && !disabled) "添加资源" else null,
            onAction = if (spendingPolicy.rules.size < 16 && !disabled) {
                {
                    onSpendingChange(
                        spendingPolicy.copy(rules = spendingPolicy.rules + PluginSpendingRule(resourceKey = ""))
                    )
                }
            } else null,
        )

        AppleGroupedCard {
            AppleSwitchRow(
                icon = Icons.Rounded.Storefront,
                iconTint = apple.orange,
                title = "自动消费",
                subtitle = if (spendingPolicy.autoSpend) {
                    if (spendingPolicy.rules.isEmpty()) "已开启 · 所需资源不限" else "已开启 · ${spendingPolicy.rules.size} 项单独限制"
                } else {
                    "已关闭 · 不会自动消耗任何资源"
                },
                checked = spendingPolicy.autoSpend,
                enabled = !disabled,
                onCheckedChange = { onSpendingChange(spendingPolicy.copy(autoSpend = it)) },
            )
        }

        if (spendingPolicy.rules.isEmpty()) {
            AppleGroupedCard(contentPadding = PaddingValues(14.dp)) {
                Text(
                    text = if (spendingPolicy.autoSpend) "未设置单独限制，插件按需使用资源。需要控制某种资源时点上方「添加资源」。" else "开启自动消费后，可为特定资源添加单次、每日上限及最低保留数量。",
                    style = MaterialTheme.typography.bodySmall,
                    color = apple.secondaryText,
                )
            }
        } else {
            spendingPolicy.rules.forEachIndexed { idx, rule ->
                val resTitle = if (rule.resourceKey.isBlank()) "点击选择资源" else pluginResourceName(rule.resourceKey, resourceNames)
                val keyValid = rule.resourceKey.isBlank() || isValidResourceKey(rule.resourceKey)
                AppleGroupedCard(contentPadding = PaddingValues(14.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable(enabled = !disabled) { pickingResourceIdx = idx },
                                shape = RoundedCornerShape(10.dp),
                                color = apple.track,
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = resTitle,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = if (rule.resourceKey.isBlank()) apple.secondaryText else apple.primaryText,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        if (rule.resourceKey.isNotBlank()) {
                                            Text(rule.resourceKey, style = MaterialTheme.typography.labelSmall, color = apple.secondaryText)
                                        }
                                    }
                                    Icon(Icons.Rounded.Tune, contentDescription = "选择资源", tint = apple.secondaryText, modifier = Modifier.size(16.dp))
                                }
                            }
                            IconButton(
                                onClick = {
                                    onSpendingChange(
                                        spendingPolicy.copy(rules = spendingPolicy.rules.filterIndexed { i, _ -> i != idx })
                                    )
                                },
                                enabled = !disabled,
                            ) {
                                Icon(Icons.Rounded.Delete, contentDescription = "删除规则", tint = apple.red)
                            }
                        }

                        if (!keyValid) {
                            Text("资源格式需为 item:物品ID（例如 item:1002）", style = MaterialTheme.typography.bodySmall, color = apple.red)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            OutlinedTextField(
                                value = if (rule.perOperationMax == "-1") "" else rule.perOperationMax,
                                onValueChange = { text ->
                                    val digits = text.filter(Char::isDigit)
                                    val nextVal = if (digits.isEmpty()) "-1" else digits
                                    onSpendingChange(
                                        spendingPolicy.copy(
                                            rules = spendingPolicy.rules.mapIndexed { i, r ->
                                                if (i == idx) r.copy(perOperationMax = nextVal) else r
                                            }
                                        )
                                    )
                                },
                                label = { Text("单次上限") },
                                placeholder = { Text("不限") },
                                singleLine = true,
                                enabled = !disabled,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f),
                            )
                            OutlinedTextField(
                                value = if (rule.dailyMax == "-1") "" else rule.dailyMax,
                                onValueChange = { text ->
                                    val digits = text.filter(Char::isDigit)
                                    val nextVal = if (digits.isEmpty()) "-1" else digits
                                    onSpendingChange(
                                        spendingPolicy.copy(
                                            rules = spendingPolicy.rules.mapIndexed { i, r ->
                                                if (i == idx) r.copy(dailyMax = nextVal) else r
                                            }
                                        )
                                    )
                                },
                                label = { Text("每日上限") },
                                placeholder = { Text("不限") },
                                singleLine = true,
                                enabled = !disabled,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f),
                            )
                            OutlinedTextField(
                                value = if (rule.minReserve == "0") "" else rule.minReserve,
                                onValueChange = { text ->
                                    val digits = text.filter(Char::isDigit)
                                    val nextVal = if (digits.isEmpty()) "0" else digits
                                    onSpendingChange(
                                        spendingPolicy.copy(
                                            rules = spendingPolicy.rules.mapIndexed { i, r ->
                                                if (i == idx) r.copy(minReserve = nextVal) else r
                                            }
                                        )
                                    )
                                },
                                label = { Text("最低保留") },
                                placeholder = { Text("不保留") },
                                singleLine = true,
                                enabled = !disabled,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f),
                            )
                        }
                        Text("上限留空表示不限，填 0 表示禁止消费。", style = MaterialTheme.typography.labelSmall, color = apple.secondaryText)
                    }
                }
            }
        }

        val activeUsage = spendingUsage.filter { it.usedToday != "0" }
        if (activeUsage.isNotEmpty()) {
            AppleSectionHeader(title = "今日已消耗")
            AppleGroupedCard(contentPadding = PaddingValues(14.dp)) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    activeUsage.forEach { u ->
                        AppleStatusPill(
                            text = "${pluginResourceName(u.resourceKey, resourceNames)} ${formatPluginQuantity(u.usedToday)}",
                            color = apple.blue,
                            showDot = false,
                        )
                    }
                }
            }
        }
    }

    pickingResourceIdx?.let { targetIdx ->
        val usedKeys = spendingPolicy.rules
            .mapIndexedNotNull { i, r -> if (i != targetIdx && r.resourceKey.isNotBlank()) r.resourceKey else null }
            .toSet()
        val allKeys = (KNOWN_RESOURCE_NAMES.keys + resourceNames.keys + spendingUsage.map { it.resourceKey })
            .distinct()
        val currentRule = spendingPolicy.rules.getOrNull(targetIdx)
        var customInput by remember(targetIdx) { mutableStateOf(currentRule?.resourceKey.orEmpty()) }

        AppleAlertDialog(
            onDismissRequest = { pickingResourceIdx = null },
            title = { Text("选择资源") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = customInput,
                        onValueChange = { customInput = it.trim() },
                        label = { Text("资源标识 (如 item:1002)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 260.dp),
                    ) {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            items(allKeys) { key ->
                                val alreadyUsed = key in usedKeys
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable(enabled = !alreadyUsed) {
                                            onSpendingChange(
                                                spendingPolicy.copy(
                                                    rules = spendingPolicy.rules.mapIndexed { i, r ->
                                                        if (i == targetIdx) r.copy(resourceKey = key) else r
                                                    }
                                                )
                                            )
                                            pickingResourceIdx = null
                                        }
                                        .padding(horizontal = 8.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = "${pluginResourceName(key, resourceNames)} ($key)",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (alreadyUsed) apple.secondaryText else apple.primaryText,
                                    )
                                    if (key == currentRule?.resourceKey) {
                                        Icon(Icons.Rounded.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSpendingChange(
                            spendingPolicy.copy(
                                rules = spendingPolicy.rules.mapIndexed { i, r ->
                                    if (i == targetIdx) r.copy(resourceKey = customInput) else r
                                }
                            )
                        )
                        pickingResourceIdx = null
                    },
                    enabled = isValidResourceKey(customInput) && customInput !in usedKeys,
                ) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { pickingResourceIdx = null }) { Text("取消") }
            },
        )
    }
}

@Composable
private fun QuietWindowsSection(
    label: String,
    description: String,
    windows: List<PluginQuietWindow>,
    disabled: Boolean,
    onChange: (List<PluginQuietWindow>) -> Unit,
) {
    val apple = LocalAppleColors.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AppleSectionHeader(
            title = label,
            actionLabel = if (windows.size < 8 && !disabled) "添加时段" else null,
            onAction = if (windows.size < 8 && !disabled) {
                { onChange(windows + PluginQuietWindow(start = "00:00", end = "08:00")) }
            } else null,
        )
        Text(description, style = MaterialTheme.typography.bodySmall, color = apple.secondaryText)
        if (windows.isEmpty()) {
            AppleGroupedCard(contentPadding = PaddingValues(14.dp)) {
                Text("未设置静默时段", style = MaterialTheme.typography.bodySmall, color = apple.secondaryText)
            }
        } else {
            AppleGroupedCard(contentPadding = PaddingValues(12.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    windows.forEachIndexed { idx, w ->
                        if (idx > 0) AppleHairlineDivider(startIndent = 0.dp)
                        val validTimes = isValidQuietWindowTime(w.start) && isValidQuietWindowTime(w.end)
                        val sameStartEnd = w.start.trim() == w.end.trim()
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                OutlinedTextField(
                                    value = w.start,
                                    onValueChange = { s ->
                                        onChange(windows.mapIndexed { i, item -> if (i == idx) item.copy(start = s) else item })
                                    },
                                    label = { Text("开始") },
                                    singleLine = true,
                                    enabled = !disabled,
                                    isError = !validTimes || sameStartEnd,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f),
                                )
                                Text("至", style = MaterialTheme.typography.bodySmall, color = apple.secondaryText)
                                OutlinedTextField(
                                    value = w.end,
                                    onValueChange = { e ->
                                        onChange(windows.mapIndexed { i, item -> if (i == idx) item.copy(end = e) else item })
                                    },
                                    label = { Text("结束") },
                                    singleLine = true,
                                    enabled = !disabled,
                                    isError = !validTimes || sameStartEnd,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f),
                                )
                                IconButton(
                                    onClick = { onChange(windows.filterIndexed { i, _ -> i != idx }) },
                                    enabled = !disabled,
                                ) {
                                    Icon(Icons.Rounded.Delete, contentDescription = "删除", tint = apple.red)
                                }
                            }
                            if (!validTimes) {
                                Text("格式需为 HH:mm（例如 23:00）", style = MaterialTheme.typography.labelSmall, color = apple.red)
                            } else if (sameStartEnd) {
                                Text("开始与结束时间不能相同", style = MaterialTheme.typography.labelSmall, color = apple.red)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun ExtensionStatusLogsTab(
    detail: PluginAccountDetail,
    definition: PluginDefinition,
    dirty: Boolean,
    runningJobId: String,
    busy: Boolean,
    onRefresh: () -> Unit,
    onRunJob: (String) -> Unit,
    onResolveOp: (String, String, Boolean) -> Unit,
) {
    val apple = LocalAppleColors.current
    val currentAccName = detail.accountName

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        AppleGroupedCard(contentPadding = PaddingValues(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    val stateColor = when (detail.executionState) {
                        "running" -> apple.green
                        "failed" -> apple.red
                        else -> apple.secondaryText
                    }
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(stateColor),
                    )
                    Text(
                        text = detail.executionLabel.ifBlank { "等待运行" },
                        style = MaterialTheme.typography.titleSmall,
                        color = apple.primaryText,
                    )
                }
                IconButton(onClick = onRefresh, enabled = !busy) {
                    Icon(Icons.Rounded.Refresh, contentDescription = "刷新", tint = apple.blue)
                }
            }
            if (!detail.decisionAllowed && detail.decisionReason.isNotBlank()) {
                Text(detail.decisionReason, style = MaterialTheme.typography.bodySmall, color = apple.orange)
            }
            if (!detail.decisionAllowed && detail.nextAllowedAtMs > 0L) {
                Text("预计 ${formatTimestamp(detail.nextAllowedAtMs)} 恢复", style = MaterialTheme.typography.labelSmall, color = apple.secondaryText)
            }
            if (detail.executionState == "failed" && detail.executionMessage.isNotBlank()) {
                Text(detail.executionMessage, style = MaterialTheme.typography.bodySmall, color = apple.red)
            }
        }

        if (detail.jobs.isNotEmpty()) {
            AppleSectionHeader(title = "调度任务")
            AppleGroupedCard {
                detail.jobs.forEachIndexed { idx, job ->
                    if (idx > 0) AppleHairlineDivider(startIndent = 14.dp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Text(job.title, style = MaterialTheme.typography.titleSmall, color = apple.primaryText)
                                AppleStatusPill(text = job.triggerText, color = apple.blue, showDot = false)
                            }
                            Text(
                                text = buildString {
                                    append("上次 ${if (job.lastRunAtMs > 0L) formatTimestamp(job.lastRunAtMs) else "—"}")
                                    if (job.nextRunAtMs > 0L && job.stateKey != "running") {
                                        append(" · 下次 ${formatTimestamp(job.nextRunAtMs)}")
                                    }
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = apple.secondaryText,
                            )
                            if (job.stateLabel.isNotBlank() && job.stateKey != "waiting") {
                                Text(
                                    text = buildString {
                                        append(job.stateLabel)
                                        if (job.message.isNotBlank()) append(" · ${job.message}")
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (job.stateKey == "failed") apple.red else apple.secondaryText,
                                )
                            }
                            if (job.errorMessage.isNotBlank()) {
                                Text("${job.errorCode}：${job.errorMessage}", style = MaterialTheme.typography.bodySmall, color = apple.red)
                            }
                            if (!detail.decisionAllowed && job.blockedReason.isNotBlank()) {
                                Text("已停止重试：${job.blockedReason}", style = MaterialTheme.typography.bodySmall, color = apple.orange)
                            }
                        }
                        OutlinedButton(
                            onClick = { onRunJob(job.id) },
                            enabled = !busy && runningJobId.isBlank() && detail.bindingEnabled && detail.decisionAllowed &&
                                detail.executionState != "running" && !dirty,
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        ) {
                            Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(if (runningJobId == job.id) "运行中" else "立即运行")
                        }
                    }
                }
            }
        }

        if (detail.operations.isNotEmpty()) {
            AppleSectionHeader(title = "消费与领奖记录")
            AppleGroupedCard {
                detail.operations.forEachIndexed { idx, op ->
                    if (idx > 0) AppleHairlineDivider(startIndent = 14.dp)
                    val isUnknown = op.status == "unknown"
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (isUnknown) apple.orange.copy(alpha = 0.08f) else Color.Transparent)
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = pluginOperationName(op.kind, definition.operationNames),
                                style = MaterialTheme.typography.titleSmall,
                                color = apple.primaryText,
                                modifier = Modifier.weight(1f),
                            )
                            val stColor = when (op.status) {
                                "confirmed" -> apple.green
                                "rejected" -> apple.red
                                "unknown" -> apple.orange
                                else -> apple.secondaryText
                            }
                            AppleStatusPill(
                                text = OP_STATUS_LABELS[op.status] ?: op.status,
                                color = stColor,
                                showDot = false,
                            )
                        }
                        if (op.costs.isNotEmpty()) {
                            Text(
                                text = op.costs.joinToString("、") { c ->
                                    "${pluginResourceName(c.resourceKey, definition.resourceNames)} -${formatPluginQuantity(c.amount)}"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = apple.orange,
                            )
                        }
                        if (op.message.isNotBlank() && op.message != OP_STATUS_LABELS[op.status]) {
                            Text(op.message, style = MaterialTheme.typography.bodySmall, color = apple.secondaryText)
                        }
                        if (op.createdAtMs > 0L) {
                            Text(formatTimestamp(op.createdAtMs), style = MaterialTheme.typography.labelSmall, color = apple.secondaryText)
                        }
                        if (isUnknown) {
                            Text("结果未知，请在游戏中核对后标记：", style = MaterialTheme.typography.bodySmall, color = apple.orange)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { onResolveOp(op.operationId, "confirmed", op.isNative) },
                                    enabled = !busy,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f),
                                ) { Text("已生效") }
                                OutlinedButton(
                                    onClick = { onResolveOp(op.operationId, "rejected", op.isNative) },
                                    enabled = !busy,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f),
                                ) { Text("未生效") }
                            }
                        }
                    }
                }
            }
        }

        AppleSectionHeader(title = "运行日志")
        AppleGroupedCard {
            if (detail.logs.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(18.dp), contentAlignment = Alignment.Center) {
                    Text("暂无日志", style = MaterialTheme.typography.bodySmall, color = apple.secondaryText)
                }
            } else {
                val slice = detail.logs.take(50)
                slice.forEachIndexed { idx, l ->
                    if (idx > 0) AppleHairlineDivider(startIndent = 14.dp)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            val accPrefix = if (l.accountName.isNotBlank() && l.accountName != currentAccName) {
                                "[${l.accountName}] "
                            } else ""
                            Text(
                                text = "$accPrefix${l.jobId.ifBlank { l.level.uppercase() }}",
                                style = MaterialTheme.typography.labelSmall,
                                color = when (l.level) {
                                    "error" -> apple.red
                                    "warn" -> apple.orange
                                    else -> apple.blue
                                },
                            )
                            Text(
                                text = formatTimestamp(l.ts),
                                style = MaterialTheme.typography.labelSmall,
                                color = apple.secondaryText,
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(l.message, style = MaterialTheme.typography.bodySmall, color = apple.primaryText)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PluginSeedPickerSheet(
    seeds: List<SeedOption>,
    selectedSeedId: Long,
    onSelect: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    val apple = LocalAppleColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var search by remember { mutableStateOf("") }
    var manualSeedId by remember(selectedSeedId) {
        mutableStateOf(if (selectedSeedId > 0L) selectedSeedId.toString() else "")
    }
    val filtered = remember(seeds, search) {
        val kw = search.trim()
        if (kw.isEmpty()) seeds else seeds.filter {
            it.name.contains(kw, ignoreCase = true) || it.seedId.toString().contains(kw)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = apple.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 600.dp)
                .navigationBarsPadding()
                .imePadding()
                .retainSheetHeight()
                .padding(horizontal = 16.dp)
                .padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("选择优先种子", style = MaterialTheme.typography.titleLarge, color = apple.primaryText)
            if (seeds.isEmpty()) {
                OutlinedTextField(
                    value = manualSeedId,
                    onValueChange = { manualSeedId = it.filter(Char::isDigit) },
                    label = { Text("种子 ID") },
                    supportingText = { Text("账号上线后可拉取商店完整种子列表，也可直接输入种子 ID") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                Button(
                    onClick = { manualSeedId.toLongOrNull()?.let(onSelect) },
                    enabled = manualSeedId.toLongOrNull()?.let { it > 0L } == true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                ) { Text("确定") }
            } else {
                AppleSearchField(
                    value = search,
                    onValueChange = { search = it },
                    placeholder = "搜索作物名或种子 ID",
                    modifier = Modifier.fillMaxWidth(),
                )
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(filtered, key = { it.seedId }) { seed ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onSelect(seed.seedId) },
                            shape = RoundedCornerShape(12.dp),
                            color = apple.card,
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(seed.name, style = MaterialTheme.typography.bodyLarge, color = apple.primaryText)
                                    Text(
                                        text = "ID ${seed.seedId}" + if (seed.requiredLevel > 0) " · Lv.${seed.requiredLevel}" else "",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = apple.secondaryText,
                                    )
                                }
                                if (seed.seedId == selectedSeedId) {
                                    Icon(Icons.Rounded.Check, contentDescription = "已选", tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PluginRanchAnimalPickerSheet(
    kind: String,
    options: List<RanchAnimalOption>,
    selectedValue: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val apple = LocalAppleColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = apple.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 580.dp)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("$kind · 指定动物", style = MaterialTheme.typography.titleLarge, color = apple.primaryText)
            val rows = listOf(RanchAnimalOption(0, "不购买", kind)) + options
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(rows, key = { it.value }) { opt ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onSelect(opt.value) },
                        shape = RoundedCornerShape(12.dp),
                        color = apple.card,
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(opt.label, style = MaterialTheme.typography.bodyLarge, color = apple.primaryText)
                            if (opt.value == selectedValue) {
                                Icon(Icons.Rounded.Check, contentDescription = "已选", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
    }
}
