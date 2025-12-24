package com.example.myhabittracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.StickyNote2
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.kizitonwose.calendar.compose.HorizontalCalendar
import com.kizitonwose.calendar.compose.rememberCalendarState
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.firstDayOfWeekFromLocale
import java.time.LocalDate
import java.time.YearMonth
// Imports for data
import com.example.myhabittracker.data.Checklist
import com.example.myhabittracker.data.HabitSentiment
import com.example.myhabittracker.data.formatTimestamp
import com.example.myhabittracker.data.formatDate
import com.example.myhabittracker.data.isSameDay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(navController: NavController, checklists: List<Checklist>) {
    val currentMonth = remember { YearMonth.now() }
    val startMonth = remember { currentMonth.minusMonths(100) }
    val endMonth = remember { currentMonth.plusMonths(100) }
    val firstDayOfWeek = remember { firstDayOfWeekFromLocale() }
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }

    val state = rememberCalendarState(
        startMonth = startMonth,
        endMonth = endMonth,
        firstVisibleMonth = currentMonth,
        firstDayOfWeek = firstDayOfWeek
    )

    val visibleMonth = state.firstVisibleMonth
    val monthYearText = remember(visibleMonth) {
        "${visibleMonth.yearMonth.month.name.lowercase().replaceFirstChar { it.uppercase() }} ${visibleMonth.yearMonth.year}"
    }

    val tasksForSelectedDate = remember(selectedDate, checklists) {
        selectedDate?.let { date ->
            checklists.filter { it.dueTimestamp != null && isSameDay(it.dueTimestamp!!, date) }
        } ?: emptyList()
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(monthYearText) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            HorizontalCalendar(
                state = state,
                dayContent = { day ->
                    val hasTasks = checklists.any { it.dueTimestamp != null && isSameDay(it.dueTimestamp!!, day.date) }
                    DayContent(day = day, isSelected = selectedDate == day.date, hasTasks = hasTasks, onClick = { selectedDate = it.date })
                }
            )

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()

            if (selectedDate != null) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Event, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Tasks for ${formatDate(selectedDate!!)}", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "(${tasksForSelectedDate.size})", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                if (tasksForSelectedDate.isEmpty()) {
                    Column(modifier = Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.EventBusy, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "No tasks scheduled", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 16.sp)
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                        items(tasksForSelectedDate) { task ->
                            CalendarTaskItem(task = task, onTaskClick = { navController.navigate("notes/${it.id}") })
                        }
                        item { Spacer(modifier = Modifier.height(16.dp)) }
                    }
                }
            } else {
                Column(modifier = Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.TouchApp, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "Select a date to view tasks", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
fun DayContent(day: CalendarDay, isSelected: Boolean, hasTasks: Boolean, onClick: (CalendarDay) -> Unit) {
    Box(
        modifier = Modifier.aspectRatio(1f).padding(4.dp).clip(CircleShape)
            .background(color = when {
                isSelected -> MaterialTheme.colorScheme.primary
                hasTasks && day.position == DayPosition.MonthDate -> Color(0xFFBDBDBD)
                else -> Color.Transparent
            })
            .clickable(enabled = day.position == DayPosition.MonthDate) { onClick(day) },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = day.date.dayOfMonth.toString(),
                color = when {
                    isSelected -> MaterialTheme.colorScheme.onPrimary
                    hasTasks && day.position == DayPosition.MonthDate -> Color.White
                    day.position == DayPosition.MonthDate -> MaterialTheme.colorScheme.onSurface
                    else -> Color.Gray
                },
                fontWeight = if (hasTasks) FontWeight.Bold else FontWeight.Normal
            )
            if (hasTasks && !isSelected) {
                Box(modifier = Modifier.padding(top = 2.dp).size(4.dp).clip(CircleShape).background(Color.White))
            }
        }
    }
}

@Composable
fun CalendarTaskItem(task: Checklist, onTaskClick: (Checklist) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onTaskClick(task) },
        colors = CardDefaults.cardColors(
            containerColor = when (task.sentiment) {
                HabitSentiment.POSITIVE -> Color(0xFF4CAF50).copy(alpha = 0.1f)
                HabitSentiment.NEGATIVE -> Color(0xFFF44336).copy(alpha = 0.1f)
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(8.dp).clip(CircleShape).background(
                    when (task.sentiment) {
                        HabitSentiment.POSITIVE -> Color(0xFF4CAF50)
                        HabitSentiment.NEGATIVE -> Color(0xFFF44336)
                        else -> Color.Gray
                    }
                )
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = task.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(
                    text = when (task.sentiment) {
                        HabitSentiment.POSITIVE -> "Good habit"
                        HabitSentiment.NEGATIVE -> "Bad habit"
                        else -> "Habit"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
                task.dueTimestamp?.let {
                    Text(text = "Due: ${formatTimestamp(it)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp))
                }
                if (task.notes.isNotBlank()) {
                    Row(modifier = Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.AutoMirrored.Filled.StickyNote2, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Has notes", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "View details", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(navController: NavController, checklist: Checklist) {
    var notes by remember { mutableStateOf(checklist.notes) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notes: ${checklist.name}") },
                navigationIcon = {
                    IconButton(onClick = {
                        checklist.notes = notes
                        navController.popBackStack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp)) {
            TextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes") },
                modifier = Modifier.fillMaxWidth().weight(1f),
                maxLines = Int.MAX_VALUE
            )
        }
    }
}