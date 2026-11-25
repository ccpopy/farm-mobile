@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.qqfarm.mobile.platform

actual fun wallClockMillis(): Long =
    kotlin.time.Clock.System.now().toEpochMilliseconds()

actual fun randomUuid(): String {
    // RFC 4122 v4 随机 UUID
    val bytes = ByteArray(16) { (0..255).random().toByte() }
    bytes[6] = ((bytes[6].toInt() and 0x0F) or 0x40).toByte()
    bytes[8] = ((bytes[8].toInt() and 0x3F) or 0x80).toByte()
    fun hex(b: Byte) = (b.toInt() and 0xFF).toString(16).padStart(2, '0')
    val s = bytes.joinToString("") { hex(it) }
    return "${s.substring(0, 8)}-${s.substring(8, 12)}-${s.substring(12, 16)}-${s.substring(16, 20)}-${s.substring(20)}"
}

actual class PlatformIOException : Exception {
    constructor() : super()
    constructor(message: String?) : super(message)
    constructor(message: String?, cause: Throwable?) : super(message, cause)
    constructor(cause: Throwable?) : super(cause)
}

/**
 * Long 型 KBigInt（iOS actual）：价格乘法场景下数值有限，
 * 溢出时饱和到 Long.MAX/MIN，保证比较语义仍成立。
 */
actual fun appVersionName(): String = "0.1.5"
