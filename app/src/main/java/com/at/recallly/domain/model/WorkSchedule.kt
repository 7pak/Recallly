package com.at.recallly.domain.model

import java.time.DayOfWeek
import java.time.LocalTime

data class WorkSchedule(
    val workDays: Set<DayOfWeek> = DEFAULT_WORK_DAYS,
    val startTime: LocalTime = DEFAULT_START_TIME,
    val endTime: LocalTime = DEFAULT_END_TIME
) {
    companion object {
        val DEFAULT_WORK_DAYS = setOf(
            DayOfWeek.MONDAY,
            DayOfWeek.TUESDAY,
            DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY
        )
        val DEFAULT_START_TIME: LocalTime = LocalTime.of(9, 0)
        val DEFAULT_END_TIME: LocalTime = LocalTime.of(17, 0)
    }
}
