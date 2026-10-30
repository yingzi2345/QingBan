package com.qingban.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qingban.app.domain.CountdownCalculator
import com.qingban.app.domain.CountdownUi
import com.qingban.app.ui.components.EmptyState
import com.qingban.app.ui.formatCompactDate
import kotlin.math.absoluteValue

@Composable
fun CountdownScreen(
    today: java.time.LocalDate,
    countdowns: List<CountdownUi>,
    onAddCountdown: () -> Unit,
    onEditCountdown: (CountdownUi) -> Unit,
    onTogglePin: (CountdownUi) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sorted = remember(countdowns, today) {
        countdowns.sortedWith(
            compareByDescending<CountdownUi> { it.pinned }
                .thenBy {
                    val days = CountdownCalculator.daysFromToday(it.targetDate, it.yearly, today)
                    if (days < 0) Long.MAX_VALUE else days
                }
                .thenBy { it.title },
        )
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 112.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "倒数日",
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        text = "把期待放在看得见的地方",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (countdowns.isNotEmpty()) {
                    Text(
                        text = "${countdowns.size} 个",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onOpenSettings) {
                    Icon(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = "设置",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        if (sorted.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Outlined.HourglassEmpty,
                    title = "还没有倒数日",
                    message = "生日、纪念日、旅行出发日，都值得被记住。",
                    actionLabel = "添加一个倒数日",
                    onAction = onAddCountdown,
                    modifier = Modifier.padding(top = 48.dp),
                )
            }
        } else {
            items(sorted, key = { it.id }) { countdown ->
                CountdownCard(
                    countdown = countdown,
                    today = today,
                    onClick = { onEditCountdown(countdown) },
                    onTogglePin = { onTogglePin(countdown) },
                )
            }
            item {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "置顶的倒数日会一直排在最前面",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun CountdownCard(
    countdown: CountdownUi,
    today: java.time.LocalDate,
    onClick: () -> Unit,
    onTogglePin: () -> Unit,
) {
    val days = CountdownCalculator.daysFromToday(countdown.targetDate, countdown.yearly, today)
    val nextDate = CountdownCalculator.nextOccurrence(countdown.targetDate, countdown.yearly, today)
    val isPast = days < 0

    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = if (countdown.pinned) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.52f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        border = BorderStroke(
            width = 1.dp,
            color = if (countdown.pinned) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
            } else {
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.72f)
            },
        ),
    ) {
        Row(
            modifier = Modifier.padding(start = 18.dp, end = 8.dp, top = 15.dp, bottom = 15.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.width(74.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (days == 0L) {
                    Text(
                        text = "今天",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                } else {
                    Text(
                        text = days.absoluteValue.toString(),
                        style = MaterialTheme.typography.displaySmall,
                        color = if (isPast) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                        maxLines = 1,
                    )
                    Text(
                        text = if (isPast) "天前" else "天后",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.width(10.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = countdown.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = if (countdown.yearly) {
                        "每年 · 下次 ${nextDate.formatCompactDate()}"
                    } else {
                        nextDate.formatCompactDate()
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (countdown.notes.isNotBlank()) {
                    Text(
                        text = countdown.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (countdown.pinned) {
                        Badge(text = "置顶")
                    }
                    if (countdown.yearly) {
                        Badge(text = "每年")
                    }
                    if (days == 0L) {
                        Text(
                            text = "就是这一天",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary,
                        )
                    }
                }
            }

            IconButton(onClick = onTogglePin) {
                Icon(
                    imageVector = if (countdown.pinned) {
                        Icons.Filled.PushPin
                    } else {
                        Icons.Outlined.PushPin
                    },
                    contentDescription = if (countdown.pinned) "取消置顶" else "置顶",
                    tint = if (countdown.pinned) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outline
                    },
                    modifier = Modifier.size(19.dp),
                )
            }
        }
    }
}

@Composable
private fun Badge(text: String) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
        )
    }
}
