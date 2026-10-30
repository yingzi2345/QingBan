package com.qingban.app.ui.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qingban.app.domain.CountdownCalculator
import com.qingban.app.domain.CountdownDraft
import com.qingban.app.domain.CountdownUi
import com.qingban.app.ui.formatFullDate
import java.time.LocalDate

data class CountdownEditorRequest(
    val countdown: CountdownUi? = null,
    val initialDate: LocalDate,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CountdownEditorSheet(
    request: CountdownEditorRequest,
    onDismiss: () -> Unit,
    onSave: (CountdownDraft) -> Unit,
    onDelete: (CountdownUi) -> Unit,
) {
    val existing = request.countdown
    val isEditing = existing != null
    val focusRequester = remember { FocusRequester() }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var title by rememberSaveable(existing?.id) { mutableStateOf(existing?.title.orEmpty()) }
    var notes by rememberSaveable(existing?.id) { mutableStateOf(existing?.notes.orEmpty()) }
    var targetDate by remember(existing?.id) {
        mutableStateOf(existing?.targetDate ?: request.initialDate)
    }
    var yearly by remember(existing?.id) { mutableStateOf(existing?.yearly ?: false) }
    var pinned by remember(existing?.id) { mutableStateOf(existing?.pinned ?: false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!isEditing) focusRequester.requestFocus()
    }

    val today = LocalDate.now()
    val nextDate = CountdownCalculator.nextOccurrence(targetDate, yearly, today)
    val days = CountdownCalculator.daysFromToday(targetDate, yearly, today)
    val preview = when {
        days > 0 -> "还有 $days 天"
        days == 0L -> "就是今天"
        else -> "已过去 ${-days} 天"
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
                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.45f), CircleShape),
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
                        text = if (isEditing) "编辑倒数日" else "新建倒数日",
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Text(
                        text = "记住那些值得等待的日子",
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
                    placeholder = { Text("生日、纪念日，或一个重要节点") },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                    shape = RoundedCornerShape(15.dp),
                    maxLines = 3,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                )

                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                ) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 16.dp),
                    ) {
                        Text(
                            text = preview,
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            text = if (yearly) {
                                "下次 ${nextDate.formatFullDate()}"
                            } else {
                                targetDate.formatFullDate()
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    EditorSectionLabel("目标日期")
                    EditorValueField(
                        icon = Icons.Outlined.CalendarMonth,
                        label = "日期",
                        value = targetDate.formatFullDate(),
                        onClick = { showDatePicker = true },
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    ToggleRow(
                        title = "每年重复",
                        subtitle = "到期后自动指向下一年的同一天，2 月 29 日按 2 月 28 日算",
                        checked = yearly,
                        onCheckedChange = { yearly = it },
                    )
                    ToggleRow(
                        title = "置顶显示",
                        subtitle = "把这一天固定在倒数日列表最前面",
                        checked = pinned,
                        onCheckedChange = { pinned = it },
                    )
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
                        Text("删除这个倒数日", color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            Button(
                onClick = {
                    if (title.isBlank()) return@Button
                    onSave(
                        CountdownDraft(
                            id = existing?.id,
                            title = title.trim(),
                            notes = notes.trim(),
                            targetDate = targetDate,
                            pinned = pinned,
                            yearly = yearly,
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
                    text = if (isEditing) "保存倒数日" else "添加倒数日",
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            Spacer(Modifier.height(12.dp))
        }
    }

    if (showDatePicker) {
        EditorDatePickerDialog(
            initialDate = targetDate,
            onDismiss = { showDatePicker = false },
            onConfirm = { targetDate = it },
        )
    }

    if (showDeleteConfirm && existing != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("删除这个倒数日？") },
            text = { Text("删除后无法恢复。") },
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
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
