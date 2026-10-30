package com.qingban.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.qingban.app.domain.RepeatType
import com.qingban.app.domain.TaskUi
import com.qingban.app.ui.formatMonthDay
import com.qingban.app.ui.formatTime
import com.qingban.app.ui.repeatLabel
import com.qingban.app.ui.theme.quadrantColors
import java.time.LocalDate

@Composable
fun TaskRow(
    task: TaskUi,
    today: LocalDate,
    onToggle: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onMoveToToday: (() -> Unit)? = null,
    showCompletionTime: Boolean = false,
) {
    val completed = task.isCompleted
    val overdue = task.isOverdue(today)
    val quadrantColor = quadrantColors(task.quadrant).accent
    val repeat = repeatLabel(task.repeatType, task.weekdaysMask, task.anchorDate)
    val time = formatTime(task.timeMinutes)

    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = if (completed) {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.72f),
        ),
    ) {
        Row(
            modifier = Modifier.padding(start = 4.dp, end = 14.dp, top = 7.dp, bottom = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = completed,
                onCheckedChange = { onToggle() },
                enabled = !completed,
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.primary,
                    uncheckedColor = quadrantColor.copy(alpha = 0.8f),
                    checkmarkColor = MaterialTheme.colorScheme.onPrimary,
                    disabledCheckedColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
                    disabledUncheckedColor = MaterialTheme.colorScheme.outline,
                ),
            )

            Spacer(Modifier.width(2.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = if (completed) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    textDecoration = if (completed) TextDecoration.LineThrough else null,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Box(
                        Modifier
                            .size(6.dp)
                            .background(
                                color = if (completed) quadrantColor.copy(alpha = 0.35f) else quadrantColor,
                                shape = CircleShape,
                            ),
                    )
                    val labels = buildList {
                        if (time != null) add(time)
                        if (repeat != null) add(repeat)
                        if (overdue) add("逾期 ${task.dueDate.formatMonthDay()}")
                        if (showCompletionTime && completed) {
                            task.completedAt?.let { add("已完成") }
                        }
                    }
                    if (labels.isNotEmpty()) {
                        Text(
                            text = labels.joinToString("  ·  "),
                            style = MaterialTheme.typography.labelMedium,
                            color = if (overdue) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (repeat != null) {
                        Icon(
                            imageVector = Icons.Rounded.Repeat,
                            contentDescription = null,
                            tint = if (overdue) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.size(13.dp),
                        )
                    }
                }

            }

            if (overdue && onMoveToToday != null) {
                Surface(
                    onClick = onMoveToToday,
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                ) {
                    Text(
                        text = "移到今天",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                    )
                }
            }
        }
    }
}
