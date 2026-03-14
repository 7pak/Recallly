package com.at.recallly.domain.repository

interface ReminderScheduler {
    fun schedule(title: String, description: String, triggerAtMillis: Long, reminderId: Int)
    fun cancel(reminderId: Int)
}
