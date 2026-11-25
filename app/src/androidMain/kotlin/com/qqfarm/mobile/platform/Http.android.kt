package com.qqfarm.mobile.platform

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.okhttp.OkHttp
import java.util.concurrent.TimeUnit

actual fun createPlatformHttpClient(config: HttpClientConfig<*>.() -> Unit): HttpClient =
    HttpClient(OkHttp) {
        engine {
            config {
                followRedirects(false)
                connectTimeout(12, TimeUnit.SECONDS)
                readTimeout(90, TimeUnit.SECONDS)
                writeTimeout(90, TimeUnit.SECONDS)
            }
        }
        config()
    }
