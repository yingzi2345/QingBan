package com.qingban.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qingban.app.domain.EventUi
import com.qingban.app.domain.TaskUi
import com.qingban.app.ui.components.EventRow
import com.qingban.app.ui.components.SectionHeader
import com.qingban.app.ui.components.TaskRow
import com.qingban.app.ui.formatMonthDayWeek
import com.qingban.app.ui.formatMonthTitle
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun CalendarScreen(
    today: LocalDate,
    month: YearMonth,
    selectedDate: LocalDate,
    tasks: List<TaskUi>,
    events: List<EventUi>,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onGoToToday: () -> Unit,
    onSelectDate: (LocalDate) -> Unit,
    onAddTask: (LocalDate) -> Unit,
    onEditTask: (TaskUi) -> Unit,
    onCompleteTask: (TaskUi) -> Unit,
    onMoveToToday: (TaskUi) -> Unit,
    onAddEvent: (LocalDate) -> Unit,
    onEditEvent: (EventUi) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tasksByDate = remember(tasks) { tasks.groupBy { it.dueDate } }
    val eventsByDate = remember(events) { events.groupBy { it.date } }
    val selectedTasks = tasksByDate[selectedDate].orEmpty()
        .sortedWith(compareBy({ it.timeMinutes ?: Int.MAX_VALUE }, { it.isCompleted }, { it.title }))
    val selectedEvents = eventsByDate[selectedDate].orEmpty()
        .sortedWith(compareBy({ it.startMinutes ?: Int.MAX_VALUE }, { it.title }))

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 8.dp, top = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = "日历",
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = "把日子摊开看，心里会更稳",
                    style = MaterialTheme.typography.bodyMedium,
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

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onPreviousMonth) {
                Icon(Icons.Outlined.ChevronLeft, contentDescription = "上个月")
            }
            Text(
                text = month.formatMonthTitle(),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onGoToToday) {
                Text("今天")
            }
            IconButton(onClick = onNextMonth) {
                Icon(Icons.Outlined.ChevronRight, contentDescription = "下个月")
            }
        }

        WeekdayHeader()

        MonthGrid(
            month = month,
            selectedDate = selectedDate,
            today = today,
            tasksByDate = tasksByDate,
            eventsByDate = eventsByDate,
            onSelectDate = onSelectDate,
        )

        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f),
            modifier = Modifier.padding(top = 4.dp),
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Text(
                    text = selectedDate.formatMonthDayWeek(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 2.dp),
                )
            }

            item {
                SectionHeader(
                    title = "日程",
                    count = selectedEvents.size,
                    trailing = {
                        TextButton(
                            onClick = { onAddEvent(selectedDate) },
                            contentPadding = PaddingValues(horizontal = 8.dp),
                        ) {
                            Text("添加", style = MaterialTheme.typography.labelMedium)
                        }
                    },
                )
            }
            if (selectedEvents.isEmpty()) {
                item {
                    AgendaEmptyHint("这一天没有日程")
                }
            } else {
                items(selectedEvents, key = { "event-${it.id}" }) { event ->
                    EventRow(event = event, onClick = { onEditEvent(event) })
                }
            }

            item {
                Spacer(Modifier.height(8.dp))
                SectionHeader(
                    title = "待办",
                    count = selectedTasks.size,
                    trailing = {
                        TextButton(
                            onClick = { onAddTask(selectedDate) },
                            contentPadding = PaddingValues(horizontal = 8.dp),
                        ) {
                            Text("添加", style = MaterialTheme.typography.labelMedium)
                        }
                    },
                )
            }
            if (selectedTasks.isEmpty()) {
                item {
                    AgendaEmptyHint("这一天没有待办")
                }
            } else {
                items(selectedTasks, key = { "task-${it.occurrenceId}" }) { task ->
                    TaskRow(
                        task = task,
                        today = today,
                        onToggle = { if (!task.isCompleted) onCompleteTask(task) },
                        onClick = { if (!task.isCompleted) onEditTask(task) },
                        onMoveToToday = if (!task.isCompleted) {
                            { onMoveToToday(task) }
                        } else {
                            null
                        },
                        showCompletionTime = true,
                    )
                }
            }
        }
    }
}

@Composable
private fun WeekdayHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp),
    ) {
        listOf("一", "二", "三", "四", "五", "六", "日").forEach { label ->
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun MonthGrid(
    month: YearMonth,
    selectedDate: LocalDate,
    today: LocalDate,
    tasksByDate: Map<LocalDate, List<TaskUi>>,
    eventsByDate: Map<LocalDate, List<EventUi>>,
    onSelectDate: (LocalDate) -> Unit,
) {
    val firstDate = month.atDay(1)
    val leadingBlanks = firstDate.dayOfWeek.value - 1
    val daysInMonth = month.lengthOfMonth()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp),
    ) {
        repeat(6) { rowIndex ->
            Row(modifier = Modifier.fillMaxWidth()) {
                repeat(7) { columnIndex ->
                    val day = rowIndex * 7 + columnIndex - leadingBlanks + 1
                    if (day in 1..daysInMonth) {
                        val date = month.atDay(day)
                        DayCell(
                            date = date,
                            selected = date == selectedDate,
                            isToday = date == today,
                            hasTask = tasksByDate[date].orEmpty().any { !it.isCompleted },
                            hasCompletedTask = tasksByDate[date].orEmpty().any { it.isCompleted },
                            hasEvent = eventsByDate[date].orEmpty().isNotEmpty(),
                            onClick = { onSelectDate(date) },
                            modifier = Modifier.weight(1f),
                        )
                    } else {
                        Spacer(
                            Modifier
                                .weight(1f)
                                .height(42.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    selected: Boolean,
    isToday: Boolean,
    hasTask: Boolean,
    hasCompletedTask: Boolean,
    hasEvent: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedBackground = MaterialTheme.colorScheme.primaryContainer
    val todayBorder = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
    val weekend = date.dayOfWeek.value >= 6
    val dayColor = when {
        selected -> MaterialTheme.colorScheme.onPrimaryContainer
        isToday -> MaterialTheme.colorScheme.primary
        weekend -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.onSurface
    }

    Box(
        modifier = modifier
            .height(42.dp)
            .padding(2.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(12.dp))
                .background(if (selected) selectedBackground else Color.Transparent)
                .then(
                    if (isToday && !selected) {
                        Modifier.border(1.dp, todayBorder, RoundedCornerShape(12.dp))
                    } else {
                        Modifier
                    },
                )
                .clickable(onClick = onClick),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontSize = 14.sp,
                color = dayColor,
                fontWeight = if (selected || isToday) FontWeight.SemiBold else FontWeight.Normal,
            )
            Spacer(Modifier.height(3.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (hasTask) {
                    Box(
                        Modifier
                            .size(4.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape),
                    )
                }
                if (hasCompletedTask && !hasTask) {
                    Box(
                        Modifier
                            .size(4.dp)
                            .background(MaterialTheme.colorScheme.outline, CircleShape),
                    )
                }
                if (hasEvent) {
                    Box(
                        Modifier
                            .size(4.dp)
                            .background(MaterialTheme.colorScheme.tertiary, CircleShape),
                    )
                }
                if (!hasTask && !hasCompletedTask && !hasEvent) {
                    Spacer(Modifier.size(4.dp))
                }
            }
        }
    }
}

@Composable
private fun AgendaEmptyHint(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
    )
}
