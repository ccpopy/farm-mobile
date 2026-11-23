package com.qqfarm.mobile.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qqfarm.mobile.data.model.SystemReleaseBackup
import com.qqfarm.mobile.data.model.SystemUpdateJob
import com.qqfarm.mobile.data.model.SystemUpdateStatus
import com.qqfarm.mobile.ui.theme.LocalAppleColors

@Composable
internal fun ServerUpdateTaskCard(job: SystemUpdateJob) {
    val apple = LocalAppleColors.current
    val (label, color) = when (job.status) {
        "running" -> "执行中" to apple.blue
        "succeeded" -> "已完成" to apple.green
        "failed" -> "失败" to apple.red
        else -> job.status to apple.secondaryText
    }
    AppleGroupedCard(contentPadding = PaddingValues(14.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (job.isRollback) "回滚任务进度" else "更新任务进度",
                    style = MaterialTheme.typography.titleSmall,
                    color = apple.primaryText,
                    modifier = Modifier.weight(1f),
                )
                AppleStatusPill(text = label, color = color, showDot = job.status == "running")
            }
            if (job.phase.isNotBlank()) {
                Text(job.phase, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, color = apple.primaryText)
            }
            val commits = listOf(job.fromCommit, job.targetCommit).filter { it.isNotBlank() }
            if (commits.isNotEmpty()) {
                Text(commits.joinToString(" → "), style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace, color = apple.secondaryText)
            }
            if (job.detail.isNotBlank()) Text(job.detail, style = MaterialTheme.typography.bodySmall, color = apple.secondaryText)
            if (job.error.isNotBlank()) Text(job.error, style = MaterialTheme.typography.bodySmall, color = apple.red)
            if (job.recentLines.isNotEmpty()) {
                val scroll = rememberScrollState(initial = Int.MAX_VALUE)
                var previousMax by remember { mutableIntStateOf(Int.MAX_VALUE) }
                val max = scroll.maxValue
                LaunchedEffect(max) {
                    // 只有原先停在末尾时才跟随新日志，保留向上翻阅的位置。
                    if (max != Int.MAX_VALUE && scroll.value >= previousMax && !scroll.isScrollInProgress) {
                        scroll.scrollTo(max)
                    }
                    previousMax = max
                }
                Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), color = apple.elevated) {
                    Text(
                        text = job.recentLines.joinToString("\n"),
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp, lineHeight = 15.sp),
                        color = apple.primaryText,
                        softWrap = false,
                        modifier = Modifier
                            .heightIn(max = 144.dp)
                            .verticalScroll(scroll)
                            .horizontalScroll(rememberScrollState())
                            .padding(10.dp),
                    )
                }
            }
        }
    }
}

@Composable
internal fun ServerRollbackOption(
    backup: SystemReleaseBackup,
    selected: Boolean,
    enabled: Boolean,
    onSelect: () -> Unit,
) {
    val apple = LocalAppleColors.current
    AppleGroupedCard {
        Row(
            modifier = Modifier.fillMaxWidth()
                .selectable(selected = selected, enabled = enabled, role = Role.RadioButton, onClick = onSelect)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            RadioButton(selected = selected, enabled = enabled, onClick = null)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(backup.label, style = MaterialTheme.typography.titleSmall, color = apple.primaryText)
                if (backup.subject.isNotBlank()) {
                    Text(backup.subject, style = MaterialTheme.typography.bodySmall, color = apple.primaryText, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                if (backup.createdAt > 0) {
                    Text(formatServerTimestamp(backup.createdAt), style = MaterialTheme.typography.labelSmall, color = apple.secondaryText)
                }
                if (!backup.compatible && backup.compatibilityReasons.isNotEmpty()) {
                    Text(backup.compatibilityReasons.joinToString("；"), style = MaterialTheme.typography.labelSmall, color = apple.secondaryText)
                }
            }
            when {
                backup.isActive -> AppleStatusPill("当前运行", apple.green, showDot = false)
                !backup.compatible -> AppleStatusPill("不兼容", apple.orange, showDot = false)
                backup.isPrevious -> AppleStatusPill("上一版本", apple.blue, showDot = false)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ServerUpdateActions(
    status: SystemUpdateStatus,
    enabled: Boolean,
    busyAction: String?,
    onCheck: () -> Unit,
    onPull: () -> Unit,
    onApply: (Boolean) -> Unit,
) {
    val shape = RoundedCornerShape(12.dp)
    val padding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
    FlowRow(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (status.canCheck) {
            OutlinedButton(onClick = onCheck, enabled = enabled, shape = shape, contentPadding = padding) {
                ServerActionLabel("检查更新", busyAction == "check")
            }
        }
        if (status.canPull) {
            OutlinedButton(onClick = onPull, enabled = enabled, shape = shape, contentPadding = padding) {
                ServerActionLabel("仅拉取代码", busyAction == "pull")
            }
        }
        val pullFirst = status.behindCount > 0
        if (status.applySupported && (pullFirst || status.needsApply)) {
            Button(onClick = { onApply(pullFirst) }, enabled = enabled && status.canApply, shape = shape, contentPadding = padding) {
                ServerActionLabel(if (pullFirst) "拉取并更新" else "应用更新", busyAction == "apply")
            }
        }
    }
}

@Composable
private fun ServerActionLabel(label: String, busy: Boolean) {
    if (busy) {
        CircularProgressIndicator(modifier = Modifier.size(15.dp), strokeWidth = 2.dp, color = LocalContentColor.current)
        Spacer(Modifier.width(6.dp))
    }
    Text(label)
}
