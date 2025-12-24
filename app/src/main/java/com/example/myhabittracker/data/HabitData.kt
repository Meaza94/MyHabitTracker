package com.example.myhabittracker.data

import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

// Enum to define the nature of the habit
enum class HabitSentiment {
    POSITIVE, NEGATIVE, NEUTRAL
}

// Core Data Model for a Habit/Checklist item
data class Checklist(
    val id: Long,
    val name: String,
    var notes: String = "",
    var dueTimestamp: Long? = null,
    var sentiment: HabitSentiment = HabitSentiment.NEUTRAL
)

// --- Helper Functions ---

fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy, hh:mm a", Locale.getDefault())
    return sdf.format(timestamp)
}

fun formatDate(date: LocalDate): String {
    val monthName = date.month.name.lowercase().replaceFirstChar { it.uppercase() }
    return "$monthName ${date.dayOfMonth}, ${date.year}"
}

fun isSameDay(timestamp: Long, date: LocalDate): Boolean {
    val timestampDate = Instant.ofEpochMilli(timestamp)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
    return timestampDate == date
}