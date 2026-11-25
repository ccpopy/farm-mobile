package com.qqfarm.mobile.data.network

// App 更新检查的公共常量（AppPreferences 等通用代码引用）
internal const val DEFAULT_UPDATE_ACCELERATOR = "https://ghfast.top"
internal fun isValidGithubToken(token: String): Boolean = token.isNotEmpty() && token.all { it in '!'..'~' }

internal const val PROJECT_GITHUB_URL = "https://github.com/ccpopy/farm-mobile"

internal fun normalizeUpdateAccelerator(value: String): String? {
    val trimmed = value.trim()
    if (trimmed.isEmpty()) return null
    val candidate = if ("://" in trimmed) trimmed else "https://$trimmed"
    val scheme = candidate.substringBefore("://").lowercase()
    if (scheme != "https") return null
    val rest = candidate.substringAfter("://")
    if (rest.contains('?') || rest.contains('#') || rest.contains('@')) return null
    val authority = rest.substringBefore('/')
    if (authority.isEmpty()) return null
    return candidate.trimEnd('/')
}

internal fun normalizeUpdateProxy(value: String): String? {
    val trimmed = value.trim()
    if (trimmed.isEmpty()) return null
    val regex = Regex("^(https?|socks5?)://[^/@:?#\\s]+(:\\d{1,5})?$", RegexOption.IGNORE_CASE)
    if (!regex.matches(trimmed)) return null
    return trimmed
}
