package com.qingban.app.ui.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import com.qingban.app.domain.Quadrant
import com.qingban.app.domain.RepeatCalculator
import com.qingban.app.domain.RepeatType
import com.qingban.app.domain.TaskDraft
import com.qingban.app.domain.TaskUi
import com.qingban.app.ui.formatFullDate
import com.qingban.app.ui.formatTime
import com.qingban.app.ui.isWeekdaySelected
import com.qingban.app.ui.normalizeWeekdays
import com.qingban.app.ui.quadrantTitle
import com.qingban.app.ui.repeatLabel
import com.qingban.app.ui.theme.quadrantColors
import com.qingban.app.ui.weekdayMaskForChip
import com.qingban.app.ui.weekdayShortNames
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

data class TaskEditorRequest(
    val task: TaskUi? = null,
    val initialDate: LocalDate,
    val initialQuadrant: Quadrant? = null,
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TaskEditorSheet(
    request: TaskEditorRequest,
    onDismiss: () -> Unit,
    onSave: (TaskDraft) -> Unit,
    onDelete: (TaskUi) -> Unit,
    onSkip: (TaskUi) -> Unit,
    onEndRepeat: (TaskUi) -> Unit,
) {
    val existing = request.task
    val isEditing = existing != null
    val initialQuadrant = request.initialQuadrant
    val focusRequester = remember { FocusRequester() }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var title by rememberSaveable(existing?.seriesId) { mutableStateOf(existing?.title.orEmpty()) }
    var notes by rememberSaveable(existing?.seriesId) { mutableStateOf(existing?.notes.orEmpty()) }
    var dueDate by remember(existing?.occurrenceId) {
        mutableStateOf(existing?.dueDate ?: request.initialDate)
    }
    var timeMinutes by remember(existing?.occurrenceId) { mutableStateOf(existing?.timeMinutes) }
    var important by remember(existing?.seriesId) {
        mutableStateOf(existing?.important ?: initialQuadrant?.important ?: true)
    }
    var urgent by remember(existing?.seriesId) {
        mutableStateOf(existing?.urgent ?: initialQuadrant?.urgent ?: false)
    }
    var repeatType by remember(existing?.seriesId) {
        mutableStateOf(existing?.repeatType ?: RepeatType.NONE)
    }
    var weekdaysMask by remember(existing?.seriesId) {
        mutableIntStateOf(
            existing?.weekdaysMask?.takeIf { it != 0 }
                ?: RepeatCalculator.dayMask(existing?.dueDate ?: request.initialDate),
        )
    }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showEndRepeatConfirm by remember { mutableStateOf(false) }

    val quadrant = Quadrant.of(important, urgent)
    val quadrantColor = quadrantColors(quadrant).accent

    LaunchedEffect(Unit) {
        if (!isEditing) {
            focusRequester.requestFocus()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background,
        dragHandle = {
            Box(
                Modifier
                    .padding(top = 10.dp, bottom = 6.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .background(
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
                        CircleShape,
                    ),
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.94f)
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 20.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = if (isEditing) "编辑待办" else "新建待办",
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Text(
                        text = when {
                            existing?.isRecurring == true -> "修改会应用到这一次及以后"
                            isEditing -> "修改这次待办"
                            else -> "先写下来，细节可以慢慢补"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Outlined.Close, contentDescription = "关闭")
                }
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(top = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    placeholder = { Text("想做什么？") },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                    shape = RoundedCornerShape(15.dp),
                    singleLine = false,
                    maxLines = 3,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionLabel("时间")
                    EditorField(
                        icon = Icons.Outlined.CalendarMonth,
                        label = "日期",
                        value = dueDate.formatFullDate(),
                        onClick = { showDatePicker = true },
                    )
                    EditorField(
                        icon = Icons.Outlined.AccessTime,
                        label = "时间",
                        value = formatTime(timeMinutes) ?: "不设置具体时间",
                        onClick = { showTimePicker = true },
                        onClear = if (timeMinutes != null) {
                            { timeMinutes = null }
                        } else {
                            null
                        },
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    SectionLabel("重复")
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        RepeatType.entries.forEach { type ->
                            FilterChip(
                                selected = repeatType == type,
                                onClick = {
                                    repeatType = type
                                    if (type == RepeatType.WEEKLY && weekdaysMask == 0) {
                                        weekdaysMask = RepeatCalculator.dayMask(dueDate)
                                    }
                                },
                                label = { Text(repeatTypeLabel(type), fontSize = 12.sp) },
                                shape = RoundedCornerShape(10.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                ),
                            )
                        }
                    }

                    if (repeatType == RepeatType.WEEKLY) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            weekdayShortNames().forEachIndexed { index, name ->
                                val selected = isWeekdaySelected(weekdaysMask, index)
                                Surface(
                                    onClick = {
                                        val bit = weekdayMaskForChip(index)
                                        val next = if (selected) {
                                            weekdaysMask and bit.inv()
                                        } else {
                                            weekdaysMask or bit
                                        }
                                        if (next != 0) weekdaysMask = next
                                    },
                                    shape = CircleShape,
                                    color = if (selected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant
                                    },
                                ) {
                                    Box(
                                        modifier = Modifier.size(36.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            text = name,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = if (selected) {
                                                MaterialTheme.colorScheme.onPrimary
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            },
                                        )
                                    }
                                }
                            }
                        }
                        Text(
                            text = "至少选择一天",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    if (repeatType != RepeatType.NONE) {
                        Text(
                            text = "下一次会在完成后自动出现",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    SectionLabel("轻重缓急")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = important,
                            onClick = { important = !important },
                            label = { Text("重要") },
                            shape = RoundedCornerShape(10.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            ),
                        )
                        FilterChip(
                            selected = urgent,
                            onClick = { urgent = !urgent },
                            label = { Text("紧急") },
                            shape = RoundedCornerShape(10.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onTertiaryContainer,
                            ),
                        )
                        Row(
                            modifier = Modifier
                                .height(32.dp)
                                .padding(start = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                Modifier
                                    .size(7.dp)
                                    .background(quadrantColor, CircleShape),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = quadrantTitle(quadrant),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("备注（可选）") },
                    shape = RoundedCornerShape(15.dp),
                    minLines = 2,
                    maxLines = 4,
                )

                if (existing != null) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        if (existing.isRecurring) {
                            TextButton(
                                onClick = { onSkip(existing); onDismiss() },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("跳过本次")
                            }
                            TextButton(
                                onClick = { showEndRepeatConfirm = true },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("结束重复")
                            }
                        }
                        TextButton(
                            onClick = { showDeleteConfirm = true },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(
                                Icons.Outlined.DeleteOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(17.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "删除这个待办",
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }
            }

            Button(
                onClick = {
                    if (title.isBlank()) return@Button
                    val mask = if (repeatType == RepeatType.WEEKLY) {
                        normalizeWeekdays(weekdaysMask, dueDate)
                    } else {
                        0
                    }
                    onSave(
                        TaskDraft(
                            seriesId = existing?.seriesId,
                            occurrenceId = existing?.occurrenceId,
                            title = title.trim(),
                            notes = notes.trim(),
                            dueDate = dueDate,
                            timeMinutes = timeMinutes,
                            important = important,
                            urgent = urgent,
                            repeatType = repeatType,
                            weekdaysMask = mask,
                        ),
                    )
                    onDismiss()
                },
                enabled = title.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                Text(
                    text = if (isEditing) "保存修改" else "添加待办",
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            Spacer(Modifier.height(12.dp))
        }
    }

    if (showDatePicker) {
        val initialMillis = dueDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            dueDate = Instant.ofEpochMilli(millis)
                                .atZone(ZoneOffset.UTC)
                                .toLocalDate()
                        }
                        showDatePicker = false
                    },
                ) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("取消")
                }
            },
        ) {
            DatePicker(
                state = datePickerState,
                showModeToggle = false,
            )
        }
    }

    if (showTimePicker) {
        val initialHour = (timeMinutes ?: 9 * 60) / 60
        val initialMinute = (timeMinutes ?: 9 * 60) % 60
        val timePickerState = rememberTimePickerState(
            initialHour = initialHour,
            initialMinute = initialMinute,
            is24Hour = true,
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("设置时间") },
            text = {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    TimePicker(state = timePickerState)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        timeMinutes = timePickerState.hour * 60 + timePickerState.minute
                        showTimePicker = false
                    },
                ) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("取消")
                }
            },
        )
    }

    if (showDeleteConfirm && existing != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("删除这个待办？") },
            text = {
                Text(
                    if (existing.isRecurring) {
                        "重复任务的所有历史记录也会一起删除，且无法恢复。"
                    } else {
                        "删除后无法恢复。"
                    },
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete(existing)
                        onDismiss()
                    },
                ) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("取消")
                }
            },
        )
    }

    if (showEndRepeatConfirm && existing != null) {
        AlertDialog(
            onDismissRequest = { showEndRepeatConfirm = false },
            title = { Text("结束重复？") },
            text = { Text("这一次待办会保留，之后不再生成新的重复任务。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showEndRepeatConfirm = false
                        onEndRepeat(existing)
                        onDismiss()
                    },
                ) {
                    Text("结束重复")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndRepeatConfirm = false }) {
                    Text("取消")
                }
            },
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onBackground,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
private fun EditorField(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    onClick: () -> Unit,
    onClear: (() -> Unit)? = null,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.58f),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 13.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(19.dp),
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            if (onClear != null) {
                IconButton(
                    onClick = onClear,
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "清除时间",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}

private fun repeatTypeLabel(type: RepeatType): String = when (type) {
    RepeatType.NONE -> "不重复"
    RepeatType.DAILY -> "每天"
    RepeatType.WEEKDAYS -> "工作日"
    RepeatType.WEEKLY -> "每周"
    RepeatType.MONTHLY -> "每月"
    RepeatType.YEARLY -> "每年"
}
