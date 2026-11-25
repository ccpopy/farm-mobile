package com.qqfarm.mobile.platform

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig

/** 创建平台 HTTP 客户端：Android 用 OkHttp engine（禁重定向），iOS 用 Darwin engine */
expect fun createPlatformHttpClient(config: HttpClientConfig<*>.() -> Unit = {}): HttpClient
