package com.qingban.app.ui

import com.qingban.app.domain.Quadrant
import com.qingban.app.domain.RepeatCalculator
import com.qingban.app.domain.RepeatType
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private val monthDayFormatter = DateTimeFormatter.ofPattern("M月d日", Locale.CHINA)
private val monthDayWeekFormatter = DateTimeFormatter.ofPattern("M月d日 EEE", Locale.CHINA)
private val fullDateFormatter = DateTimeFormatter.ofPattern("yyyy年M月d日 EEE", Locale.CHINA)
private val monthFormatter = DateTimeFormatter.ofPattern("yyyy年M月", Locale.CHINA)
private val compactDateFormatter = DateTimeFormatter.ofPattern("yyyy年M月d日", Locale.CHINA)
private val weekdayFormatter = DateTimeFormatter.ofPattern("EEEE", Locale.CHINA)

fun LocalDate.formatMonthDay(): String = format(monthDayFormatter)

fun LocalDate.formatMonthDayWeek(): String = format(monthDayWeekFormatter)

fun LocalDate.formatFullDate(): String = format(fullDateFormatter)

fun YearMonth.formatMonthTitle(): String = format(monthFormatter)

fun LocalDate.formatCompactDate(): String = format(compactDateFormatter)

fun LocalDate.formatWeekday(): String = format(weekdayFormatter)

fun formatTime(minutes: Int?): String? {
    if (minutes == null) return null
    val hour = minutes / 60
    val minute = minutes % 60
    return "%02d:%02d".format(Locale.CHINA, hour, minute)
}

fun repeatLabel(type: RepeatType, weekdaysMask: Int, anchor: LocalDate): String? = when (type) {
    RepeatType.NONE -> null
    RepeatType.DAILY -> "每天"
    RepeatType.WEEKDAYS -> "工作日"
    RepeatType.WEEKLY -> weeklyLabel(weekdaysMask, anchor)
    RepeatType.MONTHLY -> "每月${anchor.dayOfMonth}日"
    RepeatType.YEARLY -> "每年${anchor.monthValue}月${anchor.dayOfMonth}日"
}

private fun weeklyLabel(mask: Int, anchor: LocalDate): String {
    val names = listOf("一", "二", "三", "四", "五", "六", "日")
    val selected = names.filterIndexed { index, _ -> mask and (1 shl index) != 0 }
    val effective = if (selected.isEmpty()) listOf(names[anchor.dayOfWeek.value - 1]) else selected
    return "每周${effective.joinToString("、")}"
}

fun quadrantTitle(quadrant: Quadrant): String = when (quadrant) {
    Quadrant.DO_NOW -> "马上做"
    Quadrant.PLAN -> "计划做"
    Quadrant.QUICK -> "尽快处理"
    Quadrant.LATER -> "有空再说"
}

fun quadrantSubtitle(quadrant: Quadrant): String = when (quadrant) {
    Quadrant.DO_NOW -> "重要且紧急"
    Quadrant.PLAN -> "重要不紧急"
    Quadrant.QUICK -> "紧急不重要"
    Quadrant.LATER -> "不重要不紧急"
}

fun weekdayShortNames(): List<String> = listOf("一", "二", "三", "四", "五", "六", "日")

fun weekdayMaskForChip(index: Int): Int = 1 shl index

fun isWeekdaySelected(mask: Int, index: Int): Boolean = mask and (1 shl index) != 0

fun normalizeWeekdays(mask: Int, dueDate: LocalDate): Int =
    RepeatCalculator.normalizeWeekdayMask(mask, dueDate)
