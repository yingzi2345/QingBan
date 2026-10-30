package com.qingban.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qingban.app.domain.TaskUi
import com.qingban.app.ui.components.EmptyState
import com.qingban.app.ui.components.ProgressRing
import com.qingban.app.ui.components.SectionHeader
import com.qingban.app.ui.components.TaskRow
import com.qingban.app.ui.formatMonthDayWeek
import com.qingban.app.ui.formatTime
import java.time.LocalDate

@Composable
fun TodayScreen(
    today: LocalDate,
    pendingTasks: List<TaskUi>,
    completedToday: List<TaskUi>,
    onAddTask: () -> Unit,
    onEditTask: (TaskUi) -> Unit,
    onCompleteTask: (TaskUi) -> Unit,
    onMoveToToday: (TaskUi) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val todayPending = pendingTasks
        .filter { it.dueDate == today }
        .sortedWith(compareBy({ it.timeMinutes ?: Int.MAX_VALUE }, { it.title }))
    val overdueTasks = pendingTasks
        .filter { it.dueDate.isBefore(today) }
        .sortedWith(compareBy({ it.dueDate }, { it.timeMinutes ?: Int.MAX_VALUE }))
    var completedExpanded by rememberSaveable(today) { mutableStateOf(false) }

    val total = todayPending.size + completedToday.size
    val remaining = todayPending.size
    val message = when {
        total == 0 && overdueTasks.isEmpty() -> "从一件小事开始，今天会很轻。"
        remaining == 0 && total > 0 -> "今天的待办都完成了，留点时间给自己。"
        remaining == 0 && overdueTasks.isNotEmpty() -> "有 ${overdueTasks.size} 件逾期，先挑一件开始。"
        remaining == 1 -> "还有 1 件待办，慢慢来。"
        else -> "还有 $remaining 件待办，慢慢来。"
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 112.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            TodayHeader(
                today = today,
                completed = completedToday.size,
                total = total,
                message = message,
                onOpenSettings = onOpenSettings,
            )
        }

        if (overdueTasks.isNotEmpty()) {
            item {
                Spacer(Modifier.height(10.dp))
                SectionHeader(
                    title = "逾期",
                    count = overdueTasks.size,
                    trailing = {
                        Text(
                            text = "别急，一件件来",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                        )
                    },
                )
            }
            items(overdueTasks, key = { "overdue-${it.occurrenceId}" }) { task ->
                TaskRow(
                    task = task,
                    today = today,
                    onToggle = { onCompleteTask(task) },
                    onClick = { onEditTask(task) },
                    onMoveToToday = { onMoveToToday(task) },
                )
            }
        }

        item {
            Spacer(Modifier.height(if (overdueTasks.isNotEmpty()) 14.dp else 10.dp))
            SectionHeader(
                title = "待办",
                count = todayPending.size,
                trailing = {
                    if (todayPending.isNotEmpty()) {
                        Text(
                            text = "轻一点，也能做完",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
            )
        }

        if (todayPending.isEmpty()) {
            item {
                SurfaceEmptyToday(
                    hasOverdue = overdueTasks.isNotEmpty(),
                    hasCompleted = completedToday.isNotEmpty(),
                    onAddTask = onAddTask,
                )
            }
        } else {
            items(todayPending, key = { "today-${it.occurrenceId}" }) { task ->
                TaskRow(
                    task = task,
                    today = today,
                    onToggle = { onCompleteTask(task) },
                    onClick = { onEditTask(task) },
                )
            }
        }

        if (completedToday.isNotEmpty()) {
            item {
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SectionHeader(
                        title = "已完成",
                        count = completedToday.size,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { completedExpanded = !completedExpanded }) {
                        Icon(
                            imageVector = if (completedExpanded) {
                                Icons.Outlined.KeyboardArrowUp
                            } else {
                                Icons.Outlined.KeyboardArrowDown
                            },
                            contentDescription = if (completedExpanded) "收起已完成" else "展开已完成",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            item {
                AnimatedVisibility(
                    visible = completedExpanded,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically(),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        completedToday.forEach { task ->
                            TaskRow(
                                task = task,
                                today = today,
                                onToggle = {},
                                onClick = {},
                                showCompletionTime = true,
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Spa,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(end = 5.dp),
                )
                Text(
                    text = "把今天过好，就已经很好",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        }
    }
}

@Composable
private fun TodayHeader(
    today: LocalDate,
    completed: Int,
    total: Int,
    message: String,
    onOpenSettings: () -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "轻办",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = today.formatMonthDayWeek(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier.padding(start = 2.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = "设置",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        Spacer(Modifier.height(18.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = "今天",
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            ProgressRing(completed = completed, total = total)
        }
    }
}

@Composable
private fun SurfaceEmptyToday(
    hasOverdue: Boolean,
    hasCompleted: Boolean,
    onAddTask: () -> Unit,
) {
    val title = when {
        hasOverdue -> "今天的待办已清空"
        hasCompleted -> "今天的待办都完成了"
        else -> "今天还没有待办"
    }
    val message = when {
        hasOverdue -> "先把逾期的事情处理掉，也可以留到明天。"
        hasCompleted -> "做得不错，剩下的时间留给自己。"
        else -> "写下一件小事，三秒钟就能开始。"
    }
    EmptyState(
        icon = Icons.Outlined.CheckCircle,
        title = title,
        message = message,
        actionLabel = "添加一件待办",
        onAction = onAddTask,
    )
}
