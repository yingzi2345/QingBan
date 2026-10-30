package com.qingban.app.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DragIndicator
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.qingban.app.domain.Quadrant
import com.qingban.app.domain.TaskUi
import com.qingban.app.ui.formatMonthDay
import com.qingban.app.ui.formatTime
import com.qingban.app.ui.quadrantSubtitle
import com.qingban.app.ui.quadrantTitle
import com.qingban.app.ui.repeatLabel
import com.qingban.app.ui.theme.quadrantColors
import java.time.LocalDate
import kotlin.math.roundToInt

private data class DragState(
    val task: TaskUi,
    val startQuadrant: Quadrant,
    val position: Offset,
    val itemSize: androidx.compose.ui.unit.IntSize,
    val targetQuadrant: Quadrant,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuadrantScreen(
    today: LocalDate,
    tasks: List<TaskUi>,
    onAddTask: (Quadrant) -> Unit,
    onEditTask: (TaskUi) -> Unit,
    onCompleteTask: (TaskUi) -> Unit,
    onMoveTask: (TaskUi, Quadrant) -> Unit,
    modifier: Modifier = Modifier,
) {
    val byQuadrant = remember(tasks) {
        Quadrant.entries.associateWith { quadrant ->
            tasks.filter { it.quadrant == quadrant }
                .sortedWith(compareBy({ it.dueDate }, { it.timeMinutes ?: Int.MAX_VALUE }, { it.title }))
        }
    }
    val quadrantBounds = remember { arrayOfNulls<Rect>(4) }
    var dragState by remember { mutableStateOf<DragState?>(null) }
    var sheetQuadrant by remember { mutableStateOf<Quadrant?>(null) }
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current

    fun dragTarget(position: Offset, size: androidx.compose.ui.unit.IntSize): Quadrant {
        val center = position + Offset(size.width / 2f, size.height / 2f)
        return Quadrant.entries.firstOrNull { quadrant ->
            quadrantBounds[quadrant.ordinal]?.contains(center) == true
        } ?: dragState?.startQuadrant ?: Quadrant.PLAN
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 14.dp),
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "四象限",
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "长按任务拖动，换一个轻重缓急",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = "${tasks.size} 件待办",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(18.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    QuadrantCard(
                        quadrant = Quadrant.DO_NOW,
                        tasks = byQuadrant.getValue(Quadrant.DO_NOW),
                        today = today,
                        isDropTarget = dragState?.targetQuadrant == Quadrant.DO_NOW && dragState != null,
                        draggingTaskId = dragState?.task?.occurrenceId,
                        onAdd = { onAddTask(Quadrant.DO_NOW) },
                        onEdit = onEditTask,
                        onComplete = onCompleteTask,
                        onShowAll = { sheetQuadrant = Quadrant.DO_NOW },
                        onDragStart = { task, cardBounds ->
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            dragState = DragState(
                                task = task,
                                startQuadrant = Quadrant.DO_NOW,
                                position = cardBounds.topLeft,
                                itemSize = androidx.compose.ui.unit.IntSize(
                                    cardBounds.width.roundToInt(),
                                    cardBounds.height.roundToInt(),
                                ),
                                targetQuadrant = Quadrant.DO_NOW,
                            )
                        },
                        onDrag = { delta ->
                            val current = dragState
                            if (current != null) {
                                val newPosition = current.position + delta
                                dragState = current.copy(
                                    position = newPosition,
                                    targetQuadrant = dragTarget(newPosition, current.itemSize),
                                )
                            }
                        },
                        onDragEnd = {
                            val current = dragState
                            if (current != null && current.targetQuadrant != current.startQuadrant) {
                                onMoveTask(current.task, current.targetQuadrant)
                            }
                            dragState = null
                        },
                        onDragCancel = { dragState = null },
                        onBoundsChanged = { quadrantBounds[Quadrant.DO_NOW.ordinal] = it },
                        modifier = Modifier.weight(1f),
                    )
                    QuadrantCard(
                        quadrant = Quadrant.PLAN,
                        tasks = byQuadrant.getValue(Quadrant.PLAN),
                        today = today,
                        isDropTarget = dragState?.targetQuadrant == Quadrant.PLAN && dragState != null,
                        draggingTaskId = dragState?.task?.occurrenceId,
                        onAdd = { onAddTask(Quadrant.PLAN) },
                        onEdit = onEditTask,
                        onComplete = onCompleteTask,
                        onShowAll = { sheetQuadrant = Quadrant.PLAN },
                        onDragStart = { task, cardBounds ->
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            dragState = DragState(
                                task = task,
                                startQuadrant = Quadrant.PLAN,
                                position = cardBounds.topLeft,
                                itemSize = androidx.compose.ui.unit.IntSize(
                                    cardBounds.width.roundToInt(),
                                    cardBounds.height.roundToInt(),
                                ),
                                targetQuadrant = Quadrant.PLAN,
                            )
                        },
                        onDrag = { delta ->
                            val current = dragState
                            if (current != null) {
                                val newPosition = current.position + delta
                                dragState = current.copy(
                                    position = newPosition,
                                    targetQuadrant = dragTarget(newPosition, current.itemSize),
                                )
                            }
                        },
                        onDragEnd = {
                            val current = dragState
                            if (current != null && current.targetQuadrant != current.startQuadrant) {
                                onMoveTask(current.task, current.targetQuadrant)
                            }
                            dragState = null
                        },
                        onDragCancel = { dragState = null },
                        onBoundsChanged = { quadrantBounds[Quadrant.PLAN.ordinal] = it },
                        modifier = Modifier.weight(1f),
                    )
                }
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    QuadrantCard(
                        quadrant = Quadrant.QUICK,
                        tasks = byQuadrant.getValue(Quadrant.QUICK),
                        today = today,
                        isDropTarget = dragState?.targetQuadrant == Quadrant.QUICK && dragState != null,
                        draggingTaskId = dragState?.task?.occurrenceId,
                        onAdd = { onAddTask(Quadrant.QUICK) },
                        onEdit = onEditTask,
                        onComplete = onCompleteTask,
                        onShowAll = { sheetQuadrant = Quadrant.QUICK },
                        onDragStart = { task, cardBounds ->
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            dragState = DragState(
                                task = task,
                                startQuadrant = Quadrant.QUICK,
                                position = cardBounds.topLeft,
                                itemSize = androidx.compose.ui.unit.IntSize(
                                    cardBounds.width.roundToInt(),
                                    cardBounds.height.roundToInt(),
                                ),
                                targetQuadrant = Quadrant.QUICK,
                            )
                        },
                        onDrag = { delta ->
                            val current = dragState
                            if (current != null) {
                                val newPosition = current.position + delta
                                dragState = current.copy(
                                    position = newPosition,
                                    targetQuadrant = dragTarget(newPosition, current.itemSize),
                                )
                            }
                        },
                        onDragEnd = {
                            val current = dragState
                            if (current != null && current.targetQuadrant != current.startQuadrant) {
                                onMoveTask(current.task, current.targetQuadrant)
                            }
                            dragState = null
                        },
                        onDragCancel = { dragState = null },
                        onBoundsChanged = { quadrantBounds[Quadrant.QUICK.ordinal] = it },
                        modifier = Modifier.weight(1f),
                    )
                    QuadrantCard(
                        quadrant = Quadrant.LATER,
                        tasks = byQuadrant.getValue(Quadrant.LATER),
                        today = today,
                        isDropTarget = dragState?.targetQuadrant == Quadrant.LATER && dragState != null,
                        draggingTaskId = dragState?.task?.occurrenceId,
                        onAdd = { onAddTask(Quadrant.LATER) },
                        onEdit = onEditTask,
                        onComplete = onCompleteTask,
                        onShowAll = { sheetQuadrant = Quadrant.LATER },
                        onDragStart = { task, cardBounds ->
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            dragState = DragState(
                                task = task,
                                startQuadrant = Quadrant.LATER,
                                position = cardBounds.topLeft,
                                itemSize = androidx.compose.ui.unit.IntSize(
                                    cardBounds.width.roundToInt(),
                                    cardBounds.height.roundToInt(),
                                ),
                                targetQuadrant = Quadrant.LATER,
                            )
                        },
                        onDrag = { delta ->
                            val current = dragState
                            if (current != null) {
                                val newPosition = current.position + delta
                                dragState = current.copy(
                                    position = newPosition,
                                    targetQuadrant = dragTarget(newPosition, current.itemSize),
                                )
                            }
                        },
                        onDragEnd = {
                            val current = dragState
                            if (current != null && current.targetQuadrant != current.startQuadrant) {
                                onMoveTask(current.task, current.targetQuadrant)
                            }
                            dragState = null
                        },
                        onDragCancel = { dragState = null },
                        onBoundsChanged = { quadrantBounds[Quadrant.LATER.ordinal] = it },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        dragState?.let { state ->
            val widthDp = with(density) { state.itemSize.width.toDp() }
            Popup(
                alignment = Alignment.TopStart,
                offset = IntOffset(state.position.x.roundToInt(), state.position.y.roundToInt()),
                properties = PopupProperties(focusable = false),
            ) {
                Surface(
                    modifier = Modifier
                        .width(widthDp)
                        .graphicsLayer {
                            scaleX = 1.02f
                            scaleY = 1.02f
                            shadowElevation = 18.dp.toPx()
                        },
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DragIndicator,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = state.task.title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }

    sheetQuadrant?.let { quadrant ->
        val sheetTasks = byQuadrant.getValue(quadrant)
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { sheetQuadrant = null },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.background,
            dragHandle = {
                Box(
                    Modifier
                        .padding(top = 10.dp, bottom = 8.dp)
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
                    .fillMaxHeight(0.78f)
                    .padding(horizontal = 20.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(10.dp)
                            .background(quadrantColors(quadrant).accent, CircleShape),
                    )
                    Spacer(Modifier.width(9.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = quadrantTitle(quadrant),
                            style = MaterialTheme.typography.titleLarge,
                        )
                        Text(
                            text = "${quadrantSubtitle(quadrant)} · ${sheetTasks.size} 件",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = { onAddTask(quadrant); sheetQuadrant = null }) {
                        Icon(Icons.Rounded.Add, contentDescription = "添加待办")
                    }
                }
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                if (sheetTasks.isEmpty()) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "这个象限暂时没有待办",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(sheetTasks, key = { it.occurrenceId }) { task ->
                            com.qingban.app.ui.components.TaskRow(
                                task = task,
                                today = today,
                                onToggle = { onCompleteTask(task) },
                                onClick = {
                                    sheetQuadrant = null
                                    onEditTask(task)
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuadrantCard(
    quadrant: Quadrant,
    tasks: List<TaskUi>,
    today: LocalDate,
    isDropTarget: Boolean,
    draggingTaskId: Long?,
    onAdd: () -> Unit,
    onEdit: (TaskUi) -> Unit,
    onComplete: (TaskUi) -> Unit,
    onShowAll: () -> Unit,
    onDragStart: (TaskUi, Rect) -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit,
    onBoundsChanged: (Rect) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = quadrantColors(quadrant)
    val container by animateColorAsState(
        targetValue = if (isDropTarget) {
            colors.container.copy(alpha = 0.98f)
        } else {
            colors.container.copy(alpha = 0.78f)
        },
        label = "quadrantContainer",
    )
    val border = if (isDropTarget) {
        BorderStroke(2.dp, colors.accent)
    } else {
        BorderStroke(1.dp, colors.accent.copy(alpha = 0.16f))
    }
    Surface(
        modifier = modifier
            .fillMaxHeight()
            .onGloballyPositioned { onBoundsChanged(it.boundsInWindow()) },
        shape = RoundedCornerShape(22.dp),
        color = container,
        border = border,
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val maxItems = when {
                maxHeight < 220.dp -> 1
                maxHeight < 270.dp -> 2
                else -> 3
            }
            val shown = tasks.take(maxItems)
            val hiddenCount = (tasks.size - shown.size).coerceAtLeast(0)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 11.dp, vertical = 11.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(width = 4.dp, height = 26.dp)
                        .background(colors.accent, RoundedCornerShape(50)),
                )
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = quadrantTitle(quadrant),
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.onContainer,
                        fontSize = 15.sp,
                        maxLines = 1,
                    )
                    Text(
                        text = "${quadrantSubtitle(quadrant)}  ${tasks.size}",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onContainer.copy(alpha = 0.66f),
                        maxLines = 1,
                    )
                }
                IconButton(
                    onClick = onAdd,
                    modifier = Modifier.size(30.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = "在${quadrantTitle(quadrant)}中添加",
                        tint = colors.onContainer.copy(alpha = 0.8f),
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            Spacer(Modifier.height(9.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (shown.isEmpty()) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "暂无任务",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.onContainer.copy(alpha = 0.5f),
                        )
                    }
                } else {
                    shown.forEach { task ->
                        QuadrantTaskItem(
                            task = task,
                            today = today,
                            accent = colors.accent,
                            onContainer = colors.onContainer,
                            isDragging = draggingTaskId == task.occurrenceId,
                            onClick = { onEdit(task) },
                            onToggle = { onComplete(task) },
                            onDragStart = { bounds -> onDragStart(task, bounds) },
                            onDrag = onDrag,
                            onDragEnd = onDragEnd,
                            onDragCancel = onDragCancel,
                        )
                    }
                }
            }

            if (hiddenCount > 0) {
                TextButton(
                    onClick = onShowAll,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(30.dp),
                ) {
                    Text(
                        text = "查看全部 ${tasks.size} 件",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onContainer.copy(alpha = 0.8f),
                    )
                }
            }
            }
        }
    }
}

@Composable
private fun QuadrantTaskItem(
    task: TaskUi,
    today: LocalDate,
    accent: androidx.compose.ui.graphics.Color,
    onContainer: androidx.compose.ui.graphics.Color,
    isDragging: Boolean,
    onClick: () -> Unit,
    onToggle: () -> Unit,
    onDragStart: (Rect) -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit,
) {
    var bounds by remember { mutableStateOf<Rect?>(null) }
    val repeat = repeatLabel(task.repeatType, task.weekdaysMask, task.anchorDate)
    val due = quadrantDueLabel(task, today)
    val meta = listOfNotNull(due, repeat).joinToString(" · ")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
            .onGloballyPositioned { bounds = it.boundsInWindow() }
            .pointerInput(task.occurrenceId) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { bounds?.let(onDragStart) },
                    onDrag = { change, amount ->
                        change.consume()
                        onDrag(amount)
                    },
                    onDragEnd = onDragEnd,
                    onDragCancel = onDragCancel,
                )
            }
            .clickable(onClick = onClick)
            .alpha(if (isDragging) 0.24f else 1f)
            .padding(start = 7.dp, end = 7.dp, top = 7.dp, bottom = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(21.dp)
                .clip(CircleShape)
                .border(1.5.dp, accent.copy(alpha = 0.8f), CircleShape)
                .clickable(onClick = onToggle),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = "完成${task.title}",
                tint = accent.copy(alpha = 0.35f),
                modifier = Modifier.size(13.dp),
            )
        }
        Spacer(Modifier.width(7.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = task.title,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 16.sp,
            )
            if (meta.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (repeat != null) {
                        Icon(
                            imageVector = Icons.Rounded.Repeat,
                            contentDescription = null,
                            tint = onContainer.copy(alpha = 0.55f),
                            modifier = Modifier
                                .size(10.dp)
                                .padding(end = 2.dp),
                        )
                    }
                    Text(
                        text = meta,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (task.dueDate.isBefore(today)) {
                            MaterialTheme.colorScheme.error
                        } else {
                            onContainer.copy(alpha = 0.6f)
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 10.sp,
                    )
                }
            }
        }
    }
}

private fun quadrantDueLabel(task: TaskUi, today: LocalDate): String? {
    val time = formatTime(task.timeMinutes)
    return when {
        task.dueDate.isBefore(today) -> "逾期"
        task.dueDate.isEqual(today) -> time ?: "今天"
        else -> listOfNotNull(task.dueDate.formatMonthDay(), time).joinToString(" ")
    }
}
