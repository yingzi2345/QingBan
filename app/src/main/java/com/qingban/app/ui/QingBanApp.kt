package com.qingban.app.ui

import android.Manifest
import android.net.Uri
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.HourglassBottom
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.qingban.app.domain.CountdownUi
import com.qingban.app.domain.EventUi
import com.qingban.app.domain.Quadrant
import com.qingban.app.domain.TaskUi
import com.qingban.app.notifications.NotificationHelper
import com.qingban.app.ui.screens.CalendarScreen
import com.qingban.app.ui.screens.CountdownScreen
import com.qingban.app.ui.screens.QuadrantScreen
import com.qingban.app.ui.screens.SettingsScreen
import com.qingban.app.ui.screens.TodayScreen
import com.qingban.app.ui.sheets.CountdownEditorRequest
import com.qingban.app.ui.sheets.CountdownEditorSheet
import com.qingban.app.ui.sheets.EventEditorRequest
import com.qingban.app.ui.sheets.EventEditorSheet
import com.qingban.app.ui.sheets.TaskEditorRequest
import com.qingban.app.ui.sheets.TaskEditorSheet
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private data class MainTab(
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
)

private val tabs = listOf(
    MainTab("今日", Icons.Outlined.Today, Icons.Filled.Today),
    MainTab("日历", Icons.Outlined.CalendarMonth, Icons.Filled.CalendarMonth),
    MainTab("四象限", Icons.Outlined.GridView, Icons.Filled.GridView),
    MainTab("倒数日", Icons.Outlined.HourglassBottom, Icons.Filled.HourglassBottom),
)

@Composable
fun QingBanApp(
    viewModel: MainViewModel = viewModel(),
) {
    val context = LocalContext.current
    val today by viewModel.today.collectAsStateWithLifecycle()
    val pendingTasks by viewModel.pendingTasks.collectAsStateWithLifecycle()
    val completedToday by viewModel.completedToday.collectAsStateWithLifecycle()
    val calendarMonth by viewModel.calendarMonth.collectAsStateWithLifecycle()
    val calendarDate by viewModel.calendarDate.collectAsStateWithLifecycle()
    val calendarTasks by viewModel.calendarTasks.collectAsStateWithLifecycle()
    val calendarEvents by viewModel.calendarEvents.collectAsStateWithLifecycle()
    val countdowns by viewModel.countdowns.collectAsStateWithLifecycle()
    val themeMode by viewModel.settings.themeMode.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var editorRequest by remember { mutableStateOf<TaskEditorRequest?>(null) }
    var eventEditorRequest by remember { mutableStateOf<EventEditorRequest?>(null) }
    var countdownEditorRequest by remember { mutableStateOf<CountdownEditorRequest?>(null) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var pendingImportUri by remember { mutableStateOf<Uri?>(null) }
    var notificationsGranted by remember {
        mutableStateOf(NotificationHelper.hasPermission(context))
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        notificationsGranted = granted
        if (granted) viewModel.refreshReminders()
    }

    val requestNotifications: () -> Unit = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri != null) viewModel.exportBackup(uri)
    }

    val backupFileName = remember {
        "qingban-backup-${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmm"))}.json"
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) pendingImportUri = uri
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refreshToday()
        notificationsGranted = NotificationHelper.hasPermission(context)
        viewModel.refreshReminders()
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is UiEvent.TaskCompleted -> {
                    val result = snackbarHostState.showSnackbar(
                        message = "已完成「${event.taskTitle}」",
                        actionLabel = "撤销",
                        withDismissAction = false,
                        duration = androidx.compose.material3.SnackbarDuration.Short,
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        viewModel.undoComplete(event.result)
                    }
                }

                is UiEvent.Message -> {
                    snackbarHostState.showSnackbar(
                        message = event.text,
                        duration = androidx.compose.material3.SnackbarDuration.Short,
                    )
                }
            }
        }
    }

    BackHandler(enabled = showSettings) { showSettings = false }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing.only(
            WindowInsetsSides.Top + WindowInsetsSides.Horizontal,
        ),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (!showSettings) {
                Box {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f),
                    )
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 0.dp,
                    ) {
                        tabs.forEachIndexed { index, tab ->
                            val selected = selectedTab == index
                            NavigationBarItem(
                                selected = selected,
                                onClick = { selectedTab = index },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.icon,
                                        contentDescription = tab.label,
                                    )
                                },
                                label = {
                                    Text(
                                        text = tab.label,
                                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    selectedTextColor = MaterialTheme.colorScheme.onSurface,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                ),
                            )
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            if (!showSettings) {
                when (selectedTab) {
                    0 -> ExtendedFloatingActionButton(
                        onClick = { editorRequest = TaskEditorRequest(initialDate = today) },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        shape = RoundedCornerShape(18.dp),
                        icon = { Icon(Icons.Outlined.Add, contentDescription = null) },
                        text = { Text("新待办", fontWeight = FontWeight.SemiBold) },
                    )

                    1 -> ExtendedFloatingActionButton(
                        onClick = {
                            eventEditorRequest = EventEditorRequest(initialDate = calendarDate)
                        },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        shape = RoundedCornerShape(18.dp),
                        icon = { Icon(Icons.Outlined.Add, contentDescription = null) },
                        text = { Text("新日程", fontWeight = FontWeight.SemiBold) },
                    )

                    3 -> ExtendedFloatingActionButton(
                        onClick = {
                            countdownEditorRequest = CountdownEditorRequest(initialDate = today)
                        },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        shape = RoundedCornerShape(18.dp),
                        icon = { Icon(Icons.Outlined.Add, contentDescription = null) },
                        text = { Text("新倒数日", fontWeight = FontWeight.SemiBold) },
                    )
                }
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            if (showSettings) {
                SettingsScreen(
                    themeMode = themeMode,
                    notificationsGranted = notificationsGranted,
                    onThemeModeChange = viewModel::setThemeMode,
                    onRequestNotifications = requestNotifications,
                    onExport = { exportLauncher.launch(backupFileName) },
                    onImport = {
                        importLauncher.launch(
                            arrayOf("application/json", "text/plain", "application/octet-stream"),
                        )
                    },
                    onBack = { showSettings = false },
                )
            } else {
                when (selectedTab) {
                    0 -> TodayScreen(
                        today = today,
                        pendingTasks = pendingTasks,
                        completedToday = completedToday,
                        onAddTask = {
                            editorRequest = TaskEditorRequest(initialDate = today)
                        },
                        onEditTask = { task ->
                            editorRequest = TaskEditorRequest(task = task, initialDate = today)
                        },
                        onCompleteTask = viewModel::completeTask,
                        onMoveToToday = viewModel::moveToToday,
                        onOpenSettings = { showSettings = true },
                    )

                    1 -> CalendarScreen(
                        today = today,
                        month = calendarMonth,
                        selectedDate = calendarDate,
                        tasks = calendarTasks,
                        events = calendarEvents,
                        onPreviousMonth = viewModel::previousMonth,
                        onNextMonth = viewModel::nextMonth,
                        onGoToToday = viewModel::goToToday,
                        onSelectDate = viewModel::selectCalendarDate,
                        onAddTask = { date ->
                            editorRequest = TaskEditorRequest(initialDate = date)
                        },
                        onEditTask = { task ->
                            editorRequest = TaskEditorRequest(task = task, initialDate = calendarDate)
                        },
                        onCompleteTask = viewModel::completeTask,
                        onMoveToToday = viewModel::moveToToday,
                        onAddEvent = { date ->
                            eventEditorRequest = EventEditorRequest(initialDate = date)
                        },
                        onEditEvent = { event ->
                            eventEditorRequest = EventEditorRequest(
                                event = event,
                                initialDate = event.date,
                            )
                        },
                        onOpenSettings = { showSettings = true },
                    )

                    2 -> QuadrantScreen(
                        today = today,
                        tasks = pendingTasks,
                        onAddTask = { quadrant: Quadrant ->
                            editorRequest = TaskEditorRequest(
                                initialDate = today,
                                initialQuadrant = quadrant,
                            )
                        },
                        onEditTask = { task: TaskUi ->
                            editorRequest = TaskEditorRequest(task = task, initialDate = today)
                        },
                        onCompleteTask = viewModel::completeTask,
                        onMoveTask = viewModel::moveToQuadrant,
                    )

                    3 -> CountdownScreen(
                        today = today,
                        countdowns = countdowns,
                        onAddCountdown = {
                            countdownEditorRequest = CountdownEditorRequest(initialDate = today)
                        },
                        onEditCountdown = { countdown: CountdownUi ->
                            countdownEditorRequest = CountdownEditorRequest(
                                countdown = countdown,
                                initialDate = countdown.targetDate,
                            )
                        },
                        onTogglePin = viewModel::toggleCountdownPin,
                        onOpenSettings = { showSettings = true },
                    )
                }
            }
        }
    }

    editorRequest?.let { request ->
        TaskEditorSheet(
            request = request,
            onDismiss = { editorRequest = null },
            onSave = { draft ->
                viewModel.saveTask(draft)
                if (draft.timeMinutes != null && !NotificationHelper.hasPermission(context)) {
                    requestNotifications()
                }
            },
            onDelete = viewModel::deleteTask,
            onSkip = viewModel::skipFromEditor,
            onEndRepeat = viewModel::endRepeat,
        )
    }

    eventEditorRequest?.let { request ->
        EventEditorSheet(
            request = request,
            onDismiss = { eventEditorRequest = null },
            onSave = { draft ->
                viewModel.saveEvent(draft)
                if (draft.reminderMinutes != null && !NotificationHelper.hasPermission(context)) {
                    requestNotifications()
                }
            },
            onDelete = { event: EventUi -> viewModel.deleteEvent(event) },
        )
    }

    countdownEditorRequest?.let { request ->
        CountdownEditorSheet(
            request = request,
            onDismiss = { countdownEditorRequest = null },
            onSave = { draft ->
                viewModel.saveCountdown(draft)
                if (!NotificationHelper.hasPermission(context)) {
                    requestNotifications()
                }
            },
            onDelete = { countdown: CountdownUi -> viewModel.deleteCountdown(countdown) },
        )
    }

    pendingImportUri?.let { uri ->
        AlertDialog(
            onDismissRequest = { pendingImportUri = null },
            title = { Text("导入这份备份？") },
            text = {
                Text(
                    "导入会覆盖当前全部待办、日程和倒数日，且无法撤销。建议先导出一份当前数据。",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingImportUri = null
                        viewModel.importBackup(uri)
                    },
                ) {
                    Text("导入", color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingImportUri = null }) {
                    Text("取消")
                }
            },
        )
    }
}
