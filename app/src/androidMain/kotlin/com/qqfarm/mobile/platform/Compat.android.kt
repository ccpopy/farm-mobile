package com.qqfarm.mobile.platform

actual fun wallClockMillis(): Long = System.currentTimeMillis()

actual fun randomUuid(): String = java.util.UUID.randomUUID().toString()

actual typealias PlatformIOException = java.io.IOException



actual fun appVersionName(): String = com.qqfarm.mobile.BuildConfig.VERSION_NAME
