package com.qqfarm.mobile.data.network.json

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject

/**
 * org.json 的跨平台兼容实现（基于 kotlinx-serialization-json）。
 * API 语义对齐 org.json：缺失键返回 fallback、isNull 覆盖缺失键、put 返回 this 等。
 * 仅实现本项目实际使用的调用面。
 */
class JSONException(message: String) : Exception(message)

private val compatJson = Json { ignoreUnknownKeys = true; isLenient = true }

private fun JsonPrimitive.flexLong(fallback: Long): Long = when {
    isString -> content.toLongOrNull() ?: content.toDoubleOrNull()?.toLong() ?: fallback
    else -> content.toLongOrNull() ?: content.toDoubleOrNull()?.toLong() ?: fallback
}

private fun JsonPrimitive.flexInt(fallback: Int): Int {
    val l = flexLong(fallback.toLong())
    return if (l in Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong()) l.toInt() else fallback
}

private fun JsonPrimitive.flexDouble(fallback: Double): Double = when {
    isString -> content.toDoubleOrNull() ?: fallback
    else -> content.toDoubleOrNull() ?: fallback
}

private fun JsonPrimitive.flexBool(fallback: Boolean): Boolean = when {
    isString -> when (content.lowercase()) {
        "true", "1", "yes" -> true
        "false", "0", "no" -> false
        else -> fallback
    }
    else -> content == "true" || content == "1"
}

/** JSON_NULL 语义：序列化为 null 的占位值 */
val JSON_NULL: JsonNull = JsonNull

class JSONObject {
    internal var obj: JsonObject

    constructor() { obj = JsonObject(emptyMap()) }
    constructor(json: String) {
        val element = try {
            compatJson.parseToJsonElement(json)
        } catch (e: Exception) {
            throw JSONException("无效的 JSON 对象: ${e.message}")
        }
        obj = element as? JsonObject
            ?: throw JSONException("JSON 内容不是对象")
    }
    internal constructor(element: JsonObject) { obj = element }

    fun has(key: String): Boolean = obj.containsKey(key)
    fun isNull(key: String): Boolean = obj[key] is JsonNull || !obj.containsKey(key)
    fun length(): Int = obj.size
    fun keys(): Iterator<String> = obj.keys.iterator()
    fun keySet(): Set<String> = obj.keys

    /** org.json opt 语义：缺失返回 NULL 哨兵；基础类型自动解包为 String/Long/Double/Boolean */
    fun opt(key: String): Any? {
        val v = obj[key] ?: return JSON_NULL
        return unwrapTyped(v)
    }

    fun optString(key: String, fallback: String = ""): String {
        val v = obj[key] ?: return fallback
        return when (v) {
            is JsonNull -> fallback
            is JsonPrimitive -> v.content
            else -> v.toString()
        }
    }

    fun optInt(key: String, fallback: Int = 0): Int =
        (obj[key] as? JsonPrimitive)?.flexInt(fallback) ?: fallback

    fun optLong(key: String, fallback: Long = 0L): Long =
        (obj[key] as? JsonPrimitive)?.flexLong(fallback) ?: fallback

    fun optDouble(key: String, fallback: Double = Double.NaN): Double =
        (obj[key] as? JsonPrimitive)?.flexDouble(fallback) ?: fallback

    fun optBoolean(key: String, fallback: Boolean = false): Boolean =
        (obj[key] as? JsonPrimitive)?.flexBool(fallback) ?: fallback

    fun optJSONObject(key: String): JSONObject? {
        val v = obj[key] ?: return null
        return (v as? JsonObject)?.let(::JSONObject)
    }

    fun optJSONArray(key: String): JSONArray? {
        val v = obj[key] ?: return null
        return (v as? JsonArray)?.let(::JSONArray)
    }

    fun getString(key: String): String {
        if (!has(key) || obj[key] is JsonNull) throw JSONException("缺少字段: $key")
        return optString(key)
    }

    fun getInt(key: String): Int = optInt(key, 0).also {
        if (!has(key)) throw JSONException("缺少字段: $key")
    }

    fun getLong(key: String): Long = optLong(key, 0L).also {
        if (!has(key)) throw JSONException("缺少字段: $key")
    }

    fun getBoolean(key: String): Boolean = optBoolean(key, false).also {
        if (!has(key)) throw JSONException("缺少字段: $key")
    }

    fun getJSONObject(key: String): JSONObject =
        optJSONObject(key) ?: throw JSONException("缺少对象字段: $key")

    fun getJSONArray(key: String): JSONArray =
        optJSONArray(key) ?: throw JSONException("缺少数组字段: $key")

    /** put 原地修改并返回 this（对齐 org.json），value 可为 String/Int/Long/Boolean/Double/JSONObject/JSONArray/null */
    fun put(key: String, value: Any?): JSONObject {
        val newMap = LinkedHashMap(obj)
        newMap[key] = toJsonElement(value)
        obj = JsonObject(newMap)
        return this
    }

    fun remove(key: String): Any? {
        val newMap = LinkedHashMap(obj)
        val removed = newMap.remove(key)
        obj = JsonObject(newMap)
        return removed?.let { unwrap(it) }
    }

    override fun toString(): String = compatJson.encodeToString(JsonElement.serializer(), obj)

    override fun equals(other: Any?): Boolean = other is JSONObject && other.obj == obj
    override fun hashCode(): Int = obj.hashCode()
}

class JSONArray {
    internal var arr: JsonArray

    constructor() { arr = JsonArray(emptyList()) }
    constructor(json: String) {
        val element = try {
            compatJson.parseToJsonElement(json)
        } catch (e: Exception) {
            throw JSONException("无效的 JSON 数组: ${e.message}")
        }
        arr = element as? JsonArray ?: throw JSONException("JSON 内容不是数组")
    }
    internal constructor(element: JsonArray) { arr = element }
    constructor(values: List<Any?>) {
        arr = JsonArray(values.map { toJsonElement(it) })
    }

    fun length(): Int = arr.size

    /** org.json opt 语义：越界返回 NULL 哨兵；基础类型自动解包 */
    fun opt(index: Int): Any? {
        val v = arr.getOrNull(index) ?: return JSON_NULL
        return unwrapTyped(v)
    }

    fun optJSONObject(index: Int): JSONObject? =
        (arr.getOrNull(index) as? JsonObject)?.let(::JSONObject)

    fun optJSONArray(index: Int): JSONArray? =
        (arr.getOrNull(index) as? JsonArray)?.let(::JSONArray)

    fun optString(index: Int, fallback: String = ""): String {
        val v = arr.getOrNull(index) ?: return fallback
        return when (v) {
            is JsonNull -> fallback
            is JsonPrimitive -> v.content
            else -> v.toString()
        }
    }

    fun optInt(index: Int, fallback: Int = 0): Int =
        (arr.getOrNull(index) as? JsonPrimitive)?.flexInt(fallback) ?: fallback

    fun optLong(index: Int, fallback: Long = 0L): Long =
        (arr.getOrNull(index) as? JsonPrimitive)?.flexLong(fallback) ?: fallback

    fun optDouble(index: Int, fallback: Double = Double.NaN): Double =
        (arr.getOrNull(index) as? JsonPrimitive)?.flexDouble(fallback) ?: fallback

    fun optBoolean(index: Int, fallback: Boolean = false): Boolean =
        (arr.getOrNull(index) as? JsonPrimitive)?.flexBool(fallback) ?: fallback

    fun isNull(index: Int): Boolean = arr.getOrNull(index) is JsonNull || index >= arr.size

    fun getJSONObject(index: Int): JSONObject =
        optJSONObject(index) ?: throw JSONException("缺少下标对象: $index")

    fun getJSONArray(index: Int): JSONArray =
        optJSONArray(index) ?: throw JSONException("缺少下标数组: $index")

    fun getString(index: Int): String {
        if (index >= arr.size || arr[index] is JsonNull) throw JSONException("缺少下标值: $index")
        return optString(index)
    }

    fun getInt(index: Int): Int = optInt(index).also { if (index >= arr.size) throw JSONException("缺少下标值: $index") }
    fun getLong(index: Int): Long = optLong(index).also { if (index >= arr.size) throw JSONException("缺少下标值: $index") }
    fun getBoolean(index: Int): Boolean = optBoolean(index).also { if (index >= arr.size) throw JSONException("缺少下标值: $index") }

    fun put(value: Any?): JSONArray {
        val newList = ArrayList(arr)
        newList.add(toJsonElement(value))
        arr = JsonArray(newList)
        return this
    }

    fun put(index: Int, value: Any?): JSONArray {
        val newList = ArrayList(arr)
        while (newList.size <= index) newList.add(JsonNull)
        newList[index] = toJsonElement(value)
        arr = JsonArray(newList)
        return this
    }

    override fun toString(): String = compatJson.encodeToString(JsonElement.serializer(), arr)

    override fun equals(other: Any?): Boolean = other is JSONArray && other.arr == arr
    override fun hashCode(): Int = arr.hashCode()
}

// ---- 值 ↔ JsonElement 转换 ----

internal fun toJsonElement(value: Any?): JsonElement = when (value) {
    null, JSON_NULL -> JsonNull
    is String -> JsonPrimitive(value)
    is Char -> JsonPrimitive(value.toString())
    is Int -> JsonPrimitive(value)
    is Long -> JsonPrimitive(value)
    is Short -> JsonPrimitive(value.toInt())
    is Byte -> JsonPrimitive(value.toInt())
    is Boolean -> JsonPrimitive(value)
    is Float -> JsonPrimitive(value.toDouble())
    is Double -> JsonPrimitive(value)
    is JSONObject -> value.obj
    is JSONArray -> value.arr
    is JsonObject -> value
    is JsonArray -> value
    is Iterable<*> -> buildJsonArray { value.forEach { add(toJsonElement(it)) } }
    is Map<*, *> -> buildJsonObject { value.forEach { (k, v) -> put(k.toString(), toJsonElement(v)) } }
    else -> throw JSONException("不支持的 JSON 值类型: ${value::class.simpleName}")
}

internal fun unwrap(element: JsonElement): Any? = when (element) {
    is JsonNull -> null
    is JsonObject -> JSONObject(element)
    is JsonArray -> JSONArray(element)
    is JsonPrimitive -> if (element.isString) element.content else element.content
}

/** 类型化解包：布尔/数字还原为 Boolean/Long/Double，缺失返回 NULL 哨兵（对齐 org.json） */
internal fun unwrapTyped(element: JsonElement): Any? = when (element) {
    is JsonNull -> JSON_NULL
    is JsonObject -> JSONObject(element)
    is JsonArray -> JSONArray(element)
    is JsonPrimitive -> when {
        element.isString -> element.content
        element.content == "true" -> true
        element.content == "false" -> false
        else -> element.content.toLongOrNull()
            ?: element.content.toDoubleOrNull()
            ?: element.content
    }
}
