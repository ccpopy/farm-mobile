@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.qqfarm.mobile.platform

import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

// ---------------------------------------------------------------------------
// 跨平台时间 / UUID / 编码 / 大整数 / IO 异常垫片
// 目标：让 Android 端代码尽量原样进入 commonMain，iOS 端用等价实现补齐
// ---------------------------------------------------------------------------

/** 当前墙钟毫秒（对齐 System.currentTimeMillis） */
expect fun wallClockMillis(): Long

/** 随机 UUID 字符串（对齐 UUID.randomUUID().toString()） */
expect fun randomUuid(): String

/** java.io.IOException 的跨平台别名 */
expect class PlatformIOException : Exception

/**
 * Long 型实现：价格乘法场景下数值有限，
 * 溢出时饱和到 Long.MAX/MIN，保证比较语义仍成立。
 */
class KBigInt internal constructor(private val value: Long) {
    fun add(other: KBigInt): KBigInt {
        val r = value + other.value
        // 溢出检测：同号相加得异号
        if (((value xor r) and (other.value xor r)) < 0) {
            return KBigInt(if (value > 0) Long.MAX_VALUE else Long.MIN_VALUE)
        }
        return KBigInt(r)
    }

    fun multiply(other: KBigInt): KBigInt {
        if (value == 0L || other.value == 0L) return KBigInt(0)
        val r = value * other.value
        if (r / other.value != value || (value == Long.MIN_VALUE && other.value == -1L)) {
            val negative = (value < 0) xor (other.value < 0)
            return KBigInt(if (negative) Long.MIN_VALUE else Long.MAX_VALUE)
        }
        return KBigInt(r)
    }

    override fun equals(other: Any?): Boolean = other is KBigInt && other.value == value
    override fun hashCode(): Int = value.hashCode()
    override fun toString(): String = value.toString()
}

fun kBigIntValueOf(v: Long): KBigInt = KBigInt(v)
val kBigIntZero: KBigInt = KBigInt(0)

operator fun KBigInt.plus(other: KBigInt): KBigInt = add(other)
operator fun KBigInt.times(other: KBigInt): KBigInt = multiply(other)

// ---- URL 编解码（RFC 3986 form-safe，对齐 URLEncoder.encode(v, "UTF-8")） ----

fun percentEncode(value: String): String {
    val sb = StringBuilder()
    val bytes = value.encodeToByteArray()
    for (b in bytes) {
        val c = b.toInt() and 0xFF
        if (c in '0'.code..'9'.code || c in 'A'.code..'Z'.code || c in 'a'.code..'z'.code ||
            c == '-'.code || c == '_'.code || c == '.'.code || c == '*'.code
        ) {
            sb.append(c.toChar())
        } else if (c == ' '.code) {
            sb.append('+')
        } else {
            sb.append('%')
            sb.append("0123456789ABCDEF"[c shr 4])
            sb.append("0123456789ABCDEF"[c and 0x0F])
        }
    }
    return sb.toString()
}

fun percentDecode(value: String): String {
    val out = StringBuilder()
    var i = 0
    val bytes = ArrayList<Byte>()
    while (i < value.length) {
        val c = value[i]
        when {
            c == '%' && i + 2 < value.length -> {
                val hex = value.substring(i + 1, i + 3)
                val v = hex.toIntOrNull(16)
                if (v != null) {
                    bytes.add(v.toByte())
                    i += 3
                } else {
                    flushBytes(out, bytes)
                    out.append(c)
                    i += 1
                }
            }
            c == '+' -> { flushBytes(out, bytes); out.append(' '); i += 1 }
            else -> { flushBytes(out, bytes); out.append(c); i += 1 }
        }
    }
    flushBytes(out, bytes)
    return out.toString()
}

private fun flushBytes(out: StringBuilder, bytes: MutableList<Byte>) {
    if (bytes.isNotEmpty()) {
        out.append(bytes.toByteArray().decodeToString())
        bytes.clear()
    }
}

// ---------------------------------------------------------------------------
// 轻量日期格式化（替代 SimpleDateFormat/DateTimeFormatter）
// 支持 pattern 中的 yyyy / M / MM / dd / dd / HH / mm / ss 与任意字面文本
// ---------------------------------------------------------------------------

/** 统一使用东八区（对齐安卓端固定 Locale.CHINA + Asia/Shanghai 的展示习惯） */
private val CN_TZ = TimeZone.of("Asia/Shanghai")

private data class DateTimeParts(
    val year: Int, val month: Int, val day: Int,
    val hour: Int, val minute: Int, val second: Int,
)

private fun partsOf(epochMs: Long, tz: TimeZone = CN_TZ): DateTimeParts {
    val ldt = Instant.fromEpochMilliseconds(epochMs).toLocalDateTime(tz)
    return DateTimeParts(ldt.year, ldt.monthNumber, ldt.dayOfMonth, ldt.hour, ldt.minute, ldt.second)
}

private fun two(n: Int): String = if (n < 10) "0$n" else n.toString()

/**
 * 按模式格式化 epoch 毫秒。
 * 支持的占位符：yyyy、M、MM、d、dd、H、HH、m、mm、s、ss；其余字符按字面输出。
 */
fun formatEpoch(pattern: String, epochMs: Long, tz: TimeZone = CN_TZ): String {
    val p = partsOf(epochMs, tz)
    val sb = StringBuilder()
    var i = 0
    while (i < pattern.length) {
        val c = pattern[i]
        when {
            c == 'y' && pattern.startsWith("yyyy", i) -> { sb.append(p.year); i += 4 }
            c == 'M' && pattern.startsWith("MM", i) -> { sb.append(two(p.month)); i += 2 }
            c == 'M' -> { sb.append(p.month); i += 1 }
            c == 'd' && pattern.startsWith("dd", i) -> { sb.append(two(p.day)); i += 2 }
            c == 'd' -> { sb.append(p.day); i += 1 }
            c == 'H' && pattern.startsWith("HH", i) -> { sb.append(two(p.hour)); i += 2 }
            c == 'H' -> { sb.append(p.hour); i += 1 }
            c == 'm' && pattern.startsWith("mm", i) -> { sb.append(two(p.minute)); i += 2 }
            c == 'm' -> { sb.append(p.minute); i += 1 }
            c == 's' && pattern.startsWith("ss", i) -> { sb.append(two(p.second)); i += 2 }
            c == 's' -> { sb.append(p.second); i += 1 }
            else -> { sb.append(c); i += 1 }
        }
    }
    return sb.toString()
}

/** 本地时区格式化（对齐 ZoneId.systemDefault() 用法） */
fun formatEpochLocal(pattern: String, epochMs: Long): String =
    formatEpoch(pattern, epochMs, TimeZone.currentSystemDefault())

/** epoch 毫秒 → 本地 LocalDateTime（对齐 Instant.atZone(ZoneId.systemDefault())） */
fun localDateTimeOf(epochMs: Long): kotlinx.datetime.LocalDateTime =
    Instant.fromEpochMilliseconds(epochMs).toLocalDateTime(TimeZone.currentSystemDefault())


// ---- 通用格式化辅助（替代 NumberFormat / String.format(Locale)） ----

internal fun two0(n: Long): String = if (n < 10) "0$n" else n.toString()

/** 千分位分组（对齐 NumberFormat.getIntegerInstance(Locale.CHINA)） */
fun groupDigits(value: Long): String {
    val s = value.toString()
    val neg = s.startsWith("-")
    val digits = if (neg) s.substring(1) else s
    val sb = StringBuilder()
    for ((i, c) in digits.withIndex()) {
        if (i > 0 && (digits.length - i) % 3 == 0) sb.append(',')
        sb.append(c)
    }
    return (if (neg) "-" else "") + sb.toString()
}

/** 小数格式化（对齐 String.format("%.1f") 等，避免 Locale 依赖） */
fun fmtDec(value: Double, decimals: Int): String {
    val d = decimals.coerceIn(0, 6)
    val factor = 10.0.pow(d)
    val scaled = (value * factor).roundToInt().toLong()
    val neg = scaled < 0
    val abs = abs(scaled)
    val whole = abs / factor.toLong()
    val frac = abs % factor.toLong()
    return buildString {
        if (neg) append('-')
        append(whole)
        if (d > 0) {
            append('.')
            append(frac.toString().padStart(d, '0'))
        }
    }
}

/** 定长小数段拼接（"%d天 %02d:%02d:%02d" 类格式） */
fun fmtParts(parts: List<String>): String = parts.joinToString(":")


/** Base64 解码（封装 ExperimentalEncodingApi） */
@OptIn(kotlin.io.encoding.ExperimentalEncodingApi::class)
fun decodeBase64(value: String): ByteArray = kotlin.io.encoding.Base64.decode(value)

/** 当前应用版本号（Android 读 BuildConfig.VERSION_NAME；iOS 由构建注入） */
expect fun appVersionName(): String
