package com.qqfarm.mobile.data.model

import java.util.concurrent.TimeUnit

internal const val RUN_LOG_RETENTION_DAYS = 3L
internal const val RUN_LOG_LIMIT = 1000
private val retentionMs = TimeUnit.DAYS.toMillis(RUN_LOG_RETENTION_DAYS)

internal enum class RunLogScope(val label: String) {
    FARM("农场日志"),
    RANCH("牧场日志"),
    SYSTEM("系统日志"),
}

internal val GameSpace.runLogScope: RunLogScope
    get() = if (this == GameSpace.RANCH) RunLogScope.RANCH else RunLogScope.FARM

private val farmModules = setOf("farm", "friend", "warehouse", "task", "mall", "shop", "dog")
private val farmTags = setOf("农场", "好友", "仓库", "任务", "活跃", "商店", "购买")

internal val RunLogEntry.scope: RunLogScope
    get() = when {
        module == "ranch" || tag.startsWith("牧场") || event.startsWith("ranch_") -> RunLogScope.RANCH
        module in farmModules || module.isBlank() && tag in farmTags -> RunLogScope.FARM
        else -> RunLogScope.SYSTEM
    }

internal fun runLogCutoff(nowMs: Long = System.currentTimeMillis()): Long = nowMs - retentionMs

internal fun RunLogEntry.isVisibleSince(cutoffMs: Long): Boolean =
    ts >= cutoffMs && event != "runtime_diagnostics" && msg != "账号运行诊断"

internal fun List<RunLogEntry>.retainedRunLogs(nowMs: Long = System.currentTimeMillis()): List<RunLogEntry> {
    val cutoffMs = runLogCutoff(nowMs)
    return filter { it.isVisibleSince(cutoffMs) }.takeLast(RUN_LOG_LIMIT)
}
