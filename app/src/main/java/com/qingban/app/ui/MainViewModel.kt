package com.qingban.app.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.qingban.app.QingBanApplication
import com.qingban.app.data.BackupManager
import com.qingban.app.data.ThemeMode
import com.qingban.app.domain.CompletionResult
import com.qingban.app.domain.CountdownDraft
import com.qingban.app.domain.CountdownUi
import com.qingban.app.domain.EventDraft
import com.qingban.app.domain.EventUi
import com.qingban.app.domain.Quadrant
import com.qingban.app.domain.TaskDraft
import com.qingban.app.domain.TaskUi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.YearMonth

sealed interface UiEvent {
    data class TaskCompleted(
        val taskTitle: String,
        val result: CompletionResult,
    ) : UiEvent

    data class Message(val text: String) : UiEvent
}

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as QingBanApplication
    private val repository = app.repository
    private val agendaRepository = app.agendaRepository
    private val backupManager: BackupManager = app.backupManager
    private val reminderScheduler = app.reminderScheduler

    val settings = app.settings

    private val _today = MutableStateFlow(LocalDate.now())
    val today = _today.asStateFlow()

    private val _calendarMonth = MutableStateFlow(YearMonth.now())
    val calendarMonth = _calendarMonth.asStateFlow()

    private val _calendarDate = MutableStateFlow(LocalDate.now())
    val calendarDate = _calendarDate.asStateFlow()

    val pendingTasks = repository.pendingTasks.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    val completedToday = _today.flatMapLatest(repository::observeCompletedOn).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    val calendarTasks = _calendarMonth.flatMapLatest { month ->
        repository.observeTasksBetween(month.atDay(1), month.atEndOfMonth())
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    val calendarEvents = _calendarMonth.flatMapLatest { month ->
        agendaRepository.observeEventsBetween(month.atDay(1), month.atEndOfMonth())
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    val countdowns = agendaRepository.countdowns.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    private val _events = MutableSharedFlow<UiEvent>(extraBufferCapacity = 16)
    val events = _events.asSharedFlow()

    private val operationsInFlight = mutableSetOf<Long>()

    init {
        // Rebuild local notifications whenever the visible data changes.
        viewModelScope.launch {
            combine(
                repository.pendingTasks,
                agendaRepository.allEvents,
                agendaRepository.countdowns,
            ) { _, _, _ -> Unit }
                .collectLatest {
                    runCatching { reminderScheduler.refresh() }
                }
        }
    }

    fun refreshToday() {
        val current = LocalDate.now()
        if (current != _today.value) {
            val previous = _today.value
            _today.value = current
            if (_calendarDate.value == previous) {
                _calendarDate.value = current
                _calendarMonth.value = YearMonth.from(current)
            }
        }
    }

    fun refreshReminders() {
        viewModelScope.launch {
            runCatching { reminderScheduler.refresh() }
        }
    }

    fun completeTask(task: TaskUi) {
        if (!operationsInFlight.add(task.occurrenceId)) return
        viewModelScope.launch {
            runCatching { repository.completeTask(task) }
                .onSuccess { result ->
                    _events.emit(UiEvent.TaskCompleted(task.title, result))
                }
                .onFailure { _events.emit(UiEvent.Message("完成失败，请重试")) }
            operationsInFlight.remove(task.occurrenceId)
        }
    }

    fun undoComplete(result: CompletionResult) {
        viewModelScope.launch {
            runCatching { repository.undoComplete(result) }
                .onFailure { _events.emit(UiEvent.Message("撤销失败，请重试")) }
        }
    }

    fun skipTask(task: TaskUi) {
        viewModelScope.launch {
            runCatching { repository.skipTask(task) }
                .onSuccess { _events.emit(UiEvent.Message("已跳过本次，下次会照常出现")) }
                .onFailure { _events.emit(UiEvent.Message("跳过失败，请重试")) }
        }
    }

    fun moveToToday(task: TaskUi) {
        viewModelScope.launch {
            runCatching { repository.moveToToday(task) }
                .onSuccess { _events.emit(UiEvent.Message("已移到今天")) }
                .onFailure { _events.emit(UiEvent.Message("移动失败，请重试")) }
        }
    }

    fun moveToQuadrant(task: TaskUi, quadrant: Quadrant) {
        viewModelScope.launch {
            runCatching { repository.moveToQuadrant(task, quadrant) }
                .onFailure { _events.emit(UiEvent.Message("调整象限失败，请重试")) }
        }
    }

    fun saveTask(draft: TaskDraft) {
        viewModelScope.launch {
            runCatching {
                if (draft.seriesId == null) {
                    repository.createTask(draft)
                } else {
                    repository.updateTask(draft)
                }
            }.onSuccess {
                _calendarMonth.value = YearMonth.from(draft.dueDate)
                _calendarDate.value = draft.dueDate
            }.onFailure {
                _events.emit(UiEvent.Message("保存失败，请重试"))
            }
        }
    }

    fun skipFromEditor(task: TaskUi) {
        skipTask(task)
    }

    fun endRepeat(task: TaskUi) {
        viewModelScope.launch {
            runCatching { repository.endRepeat(task) }
                .onSuccess { _events.emit(UiEvent.Message("已结束重复，本次待办会保留")) }
                .onFailure { _events.emit(UiEvent.Message("操作失败，请重试")) }
        }
    }

    fun deleteTask(task: TaskUi) {
        viewModelScope.launch {
            runCatching { repository.deleteTask(task) }
                .onSuccess { _events.emit(UiEvent.Message("已删除待办")) }
                .onFailure { _events.emit(UiEvent.Message("删除失败，请重试")) }
        }
    }

    fun selectCalendarDate(date: LocalDate) {
        _calendarDate.value = date
    }

    fun previousMonth() = shiftMonth(-1)

    fun nextMonth() = shiftMonth(1)

    fun goToToday() {
        val now = LocalDate.now()
        _today.value = now
        _calendarMonth.value = YearMonth.from(now)
        _calendarDate.value = now
    }

    private fun shiftMonth(delta: Long) {
        val month = _calendarMonth.value.plusMonths(delta)
        val day = _calendarDate.value.dayOfMonth.coerceAtMost(month.lengthOfMonth())
        _calendarMonth.value = month
        _calendarDate.value = month.atDay(day)
    }

    fun saveEvent(draft: EventDraft) {
        viewModelScope.launch {
            runCatching {
                if (draft.id == null) {
                    agendaRepository.createEvent(draft)
                } else {
                    agendaRepository.updateEvent(draft)
                }
            }.onSuccess {
                _calendarMonth.value = YearMonth.from(draft.date)
                _calendarDate.value = draft.date
                _events.emit(UiEvent.Message(if (draft.id == null) "已添加日程" else "已保存日程"))
            }.onFailure {
                _events.emit(UiEvent.Message("日程保存失败，请重试"))
            }
        }
    }

    fun deleteEvent(event: EventUi) {
        viewModelScope.launch {
            runCatching { agendaRepository.deleteEvent(event.id) }
                .onSuccess { _events.emit(UiEvent.Message("已删除日程")) }
                .onFailure { _events.emit(UiEvent.Message("删除失败，请重试")) }
        }
    }

    fun saveCountdown(draft: CountdownDraft) {
        viewModelScope.launch {
            runCatching {
                if (draft.id == null) {
                    agendaRepository.createCountdown(draft)
                } else {
                    agendaRepository.updateCountdown(draft)
                }
            }.onSuccess {
                _events.emit(UiEvent.Message(if (draft.id == null) "已添加倒数日" else "已保存倒数日"))
            }.onFailure {
                _events.emit(UiEvent.Message("倒数日保存失败，请重试"))
            }
        }
    }

    fun deleteCountdown(countdown: CountdownUi) {
        viewModelScope.launch {
            runCatching { agendaRepository.deleteCountdown(countdown.id) }
                .onSuccess { _events.emit(UiEvent.Message("已删除倒数日")) }
                .onFailure { _events.emit(UiEvent.Message("删除失败，请重试")) }
        }
    }

    fun toggleCountdownPin(countdown: CountdownUi) {
        viewModelScope.launch {
            runCatching { agendaRepository.setCountdownPinned(countdown.id, !countdown.pinned) }
                .onSuccess {
                    _events.emit(UiEvent.Message(if (countdown.pinned) "已取消置顶" else "已置顶"))
                }
                .onFailure { _events.emit(UiEvent.Message("操作失败，请重试")) }
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        settings.setThemeMode(mode)
    }

    fun exportBackup(uri: Uri) {
        viewModelScope.launch {
            runCatching {
                val json = backupManager.exportJson()
                withContext(Dispatchers.IO) {
                    val stream = app.contentResolver.openOutputStream(uri)
                        ?: error("无法写入文件")
                    stream.use { it.write(json.toByteArray(Charsets.UTF_8)) }
                }
            }.onSuccess {
                _events.emit(UiEvent.Message("备份已导出"))
            }.onFailure {
                _events.emit(UiEvent.Message("导出失败：${it.message ?: "请重试"}"))
            }
        }
    }

    fun importBackup(uri: Uri) {
        viewModelScope.launch {
            runCatching {
                val raw = withContext(Dispatchers.IO) {
                    val stream = app.contentResolver.openInputStream(uri)
                        ?: error("无法读取文件")
                    stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                }
                backupManager.importJson(raw)
            }.onSuccess { summary ->
                _events.emit(
                    UiEvent.Message(
                        "已导入：${summary.series} 个待办、${summary.events} 个日程、" +
                            "${summary.countdowns} 个倒数日",
                    ),
                )
            }.onFailure {
                _events.emit(UiEvent.Message("导入失败：${it.message ?: "文件格式不正确"}"))
            }
        }
    }
}
