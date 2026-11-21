package com.qqfarm.mobile.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.qqfarm.mobile.ui.components.AppleEmptyState
import com.qqfarm.mobile.ui.components.AppleGroupedCard
import com.qqfarm.mobile.ui.components.AppleRadius
import com.qqfarm.mobile.ui.components.AppleSectionHeader
import com.qqfarm.mobile.ui.components.AppleSegmentedControl
import com.qqfarm.mobile.ui.components.RemoteAssetImage
import com.qqfarm.mobile.ui.components.formatCompactCount
import com.qqfarm.mobile.ui.theme.LocalAppleColors

internal data class BagLockEntry<T : Any>(
    val uid: T, val name: String, val image: String, val count: Long, val locked: Boolean,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun <T : Any> BagLockSheet(
    entries: List<BagLockEntry<T>>,
    busy: Boolean,
    error: String? = null,
    onSubmit: (List<T>, Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    val apple = LocalAppleColors.current
    var lock by remember { mutableStateOf(entries.any { !it.locked }) }
    val candidates = remember(entries, lock) { entries.filter { it.locked != lock } }
    var selection by remember(lock) { mutableStateOf(emptySet<T>()) }
    val selected = candidates.map { it.uid }.filter { it in selection }
    val allSelected = candidates.isNotEmpty() && selected.size == candidates.size
    ModalBottomSheet(
        onDismissRequest = { if (!busy) onDismiss() },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = apple.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.78f)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "批量管理物品锁",
                style = MaterialTheme.typography.titleLarge,
                color = apple.primaryText,
            )
            AppleSegmentedControl(
                options = listOf("上锁", "解锁"),
                selectedIndex = if (lock) 0 else 1,
                onSelect = { if (!busy) lock = it == 0 },
            )
            AppleSectionHeader(
                title = "已选 ${selected.size} / ${candidates.size}",
                actionLabel = if (candidates.isNotEmpty()) {
                    if (allSelected) "取消全选" else "全选"
                } else null,
                onAction = if (candidates.isNotEmpty()) {
                    {
                        if (!busy) {
                            selection = if (allSelected) emptySet() else candidates.map { it.uid }.toSet()
                        }
                    }
                } else null,
            )
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (candidates.isEmpty()) {
                    item {
                        AppleGroupedCard {
                            AppleEmptyState(
                                icon = if (lock) Icons.Rounded.Lock else Icons.Rounded.LockOpen,
                                title = if (lock) "没有可上锁的物品" else "没有可解锁的物品",
                                subtitle = if (lock) "当前支持加锁的物品均已上锁" else "当前没有处于锁定状态的物品",
                            )
                        }
                    }
                }
                items(candidates, key = { it.uid }) { item ->
                    val checked = item.uid in selection
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(AppleRadius.tile)),
                        shape = RoundedCornerShape(AppleRadius.tile),
                        color = if (checked) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else apple.card,
                        border = BorderStroke(
                            0.5.dp,
                            if (checked) MaterialTheme.colorScheme.primary.copy(alpha = 0.45f) else apple.separator,
                        ),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .toggleable(value = checked, enabled = !busy, role = Role.Checkbox) { select ->
                                    selection = if (select) selection + item.uid else selection - item.uid
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Checkbox(checked = checked, onCheckedChange = null, enabled = !busy)
                            RemoteAssetImage(
                                url = item.image,
                                contentDescription = null,
                                fallbackIcon = Icons.Rounded.Inventory2,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(AppleRadius.control))
                                    .background(apple.elevated)
                                    .padding(4.dp),
                            )
                            Text(
                                text = item.name,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                color = apple.primaryText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                text = "×${formatCompactCount(item.count)}",
                                style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"),
                                color = apple.secondaryText,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
            if (!error.isNullOrBlank()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(AppleRadius.control),
                    color = apple.red.copy(alpha = 0.12f),
                ) {
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodySmall,
                        color = apple.red,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                }
            }
            Button(
                onClick = { onSubmit(selected, lock) },
                enabled = selected.isNotEmpty() && !busy,
                shape = RoundedCornerShape(AppleRadius.tile),
                modifier = Modifier.fillMaxWidth().height(46.dp),
            ) { Text("${if (lock) "上锁" else "解锁"}所选（${selected.size}）") }
        }
    }
}
