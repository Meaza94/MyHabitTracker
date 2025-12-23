package com.example.myhabittracker

import android.app.DatePickerDialog
import android.app.TimePickerDialog

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.StickyNote2
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip

import androidx.compose.ui.graphics.Color

import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight

import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.myhabittracker.ui.theme.MyHabitTrackerTheme
import com.kizitonwose.calendar.compose.HorizontalCalendar
import com.kizitonwose.calendar.compose.rememberCalendarState
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.firstDayOfWeekFromLocale
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.util.Calendar
import java.util.Locale


enum class HabitSentiment {
    POSITIVE, NEGATIVE, NEUTRAL
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val systemIsDark = isSystemInDarkTheme()
            var useDarkTheme by remember { mutableStateOf(systemIsDark) }

            MyHabitTrackerTheme(
                darkTheme = useDarkTheme,
                dynamicColor = false
            ) {
                MyHabitTrackerApp(
                    darkTheme = useDarkTheme,
                    onThemeToggle = { useDarkTheme = !useDarkTheme }
                )
            }
        }
    }
}

data class Checklist(val id: Long, val name: String, var notes: String = "", var dueTimestamp: Long? = null, var sentiment: HabitSentiment = HabitSentiment.NEUTRAL)

@Composable
fun MyHabitTrackerApp(darkTheme: Boolean, onThemeToggle: () -> Unit) {
    val navController = rememberNavController()
    val checklists = remember { mutableStateListOf<Checklist>() }
    val completedChecklists = remember { mutableStateListOf<Checklist>() }

    NavHost(navController = navController, startDestination = "habitList") {
        composable("habitList") {
            HabitListScreen(
                navController = navController,
                checklists = checklists,
                completedChecklists = completedChecklists,
                onAddChecklist = { checklists.add(it) },
                onChecklistComplete = { checklist ->
                    checklists.remove(checklist)
                    completedChecklists.add(checklist)
                },
                onChecklistUncomplete = { checklist ->
                    completedChecklists.remove(checklist)
                    checklists.add(checklist)
                },
                darkTheme = darkTheme,
                onThemeToggle = onThemeToggle
            )
        }
        composable(
            "notes/{checklistId}",
            arguments = listOf(navArgument("checklistId") { type = NavType.LongType })
        ) { backStackEntry ->
            val checklistId = backStackEntry.arguments?.getLong("checklistId")
            val checklist = (checklists + completedChecklists).find { it.id == checklistId }
            if (checklist != null) {
                NotesScreen(navController = navController, checklist = checklist)
            }
        }
        composable("calendar") {
            CalendarScreen(navController = navController, checklists = checklists.toList() + completedChecklists.toList())
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitListScreen(
    navController: NavController,
    checklists: MutableList<Checklist>,
    completedChecklists: MutableList<Checklist>,
    onAddChecklist: (Checklist) -> Unit,
    onChecklistComplete: (Checklist) -> Unit,
    onChecklistUncomplete: (Checklist) -> Unit,
    darkTheme: Boolean,
    onThemeToggle: () -> Unit
) {
    var showHabitDialog by remember { mutableStateOf(false) }
    var checklistName by remember { mutableStateOf("") }
    var selectedTimestamp by remember { mutableStateOf<Long?>(null) }
    var selectedSentiment by remember { mutableStateOf<HabitSentiment?>(null) }

    val context = LocalContext.current

    // Categorize completed habits - remove remember to allow immediate updates
    val goodHabits = completedChecklists.filter { it.sentiment == HabitSentiment.POSITIVE }
    val badHabits = completedChecklists.filter { it.sentiment == HabitSentiment.NEGATIVE }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Daily Habits") },
                actions = {
                    IconButton(onClick = { navController.navigate("calendar") }) {
                        Icon(Icons.Filled.DateRange, contentDescription = "Calendar")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(if (darkTheme) "Dark" else "Light")
                        Switch(
                            checked = darkTheme,
                            onCheckedChange = { _ -> onThemeToggle() },
                            modifier = Modifier.padding(start = 8.dp, end = 8.dp)
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showHabitDialog = true }) {
                @Suppress("UNUSED_EXPRESSION")
                Icon(Icons.Filled.Add, contentDescription = "Add Checklist")
            }
        },
        floatingActionButtonPosition = FabPosition.Center
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            if (showHabitDialog) {
                // Initialize calendar with current time or selected timestamp
                val calendar = remember {
                    Calendar.getInstance().apply {
                        selectedTimestamp?.let { timeInMillis = it }
                    }
                }

                // Update calendar when selectedTimestamp changes
                LaunchedEffect(selectedTimestamp) {
                    selectedTimestamp?.let {
                        calendar.timeInMillis = it
                    }
                }

                val datePickerDialog = remember {
                    DatePickerDialog(
                        context,
                        { _, year, month, dayOfMonth ->
                            calendar.set(Calendar.YEAR, year)
                            calendar.set(Calendar.MONTH, month)
                            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                            selectedTimestamp = calendar.timeInMillis
                        },
                        calendar.get(Calendar.YEAR),
                        calendar.get(Calendar.MONTH),
                        calendar.get(Calendar.DAY_OF_MONTH)
                    )
                }

                val timePickerDialog = remember {
                    TimePickerDialog(
                        context,
                        { _, hourOfDay, minute ->
                            calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                            calendar.set(Calendar.MINUTE, minute)
                            selectedTimestamp = calendar.timeInMillis
                        },
                        calendar.get(Calendar.HOUR_OF_DAY),
                        calendar.get(Calendar.MINUTE),
                        false
                    )
                }

                AlertDialog(
                    onDismissRequest = {
                        showHabitDialog = false
                        checklistName = ""
                        selectedTimestamp = null
                        selectedSentiment = null
                    },
                    title = { Text("New Habit") },
                    text = {
                        Column {
                            TextField(
                                value = checklistName,
                                onValueChange = { checklistName = it },
                                label = { Text("Habit Name") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            Text(
                                text = "What type of habit is this?",
                                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
                                style = MaterialTheme.typography.bodyMedium
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = selectedSentiment == HabitSentiment.POSITIVE,
                                    onClick = { selectedSentiment = HabitSentiment.POSITIVE },
                                    label = { Text("Good Habit") },
                                    leadingIcon = {
                                        Text(
                                            text = "+",
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (selectedSentiment == HabitSentiment.POSITIVE)
                                                Color(0xFF4CAF50) else Color.Gray
                                        )
                                    },
                                    modifier = Modifier.weight(1f)
                                )

                                FilterChip(
                                    selected = selectedSentiment == HabitSentiment.NEGATIVE,
                                    onClick = { selectedSentiment = HabitSentiment.NEGATIVE },
                                    label = { Text("Bad Habit") },
                                    leadingIcon = {
                                        Text(
                                            text = "-",
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (selectedSentiment == HabitSentiment.NEGATIVE)
                                                Color(0xFFF44336) else Color.Gray
                                        )
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            selectedSentiment?.let { sentiment ->
                                Text(
                                    text = if (sentiment == HabitSentiment.POSITIVE)
                                        "✓ Positive habit I did (e.g., Exercise)"
                                    else
                                        "✓ Negative habit I did (e.g., Smoking)",
                                    modifier = Modifier.padding(top = 4.dp),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { datePickerDialog.show() },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Set Date")
                                }
                                Button(
                                    onClick = { timePickerDialog.show() },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Set Time")
                                }
                            }
                            selectedTimestamp?.let {
                                Text(
                                    text = "Due: ${formatTimestamp(it)}",
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (checklistName.isNotBlank() && selectedSentiment != null) {
                                    onAddChecklist(
                                        Checklist(
                                            System.currentTimeMillis(),
                                            checklistName,
                                            dueTimestamp = selectedTimestamp,
                                            sentiment = selectedSentiment!!
                                        )
                                    )
                                    checklistName = ""
                                    selectedTimestamp = null
                                    selectedSentiment = null
                                    showHabitDialog = false
                                }
                            },
                            enabled = checklistName.isNotBlank() && selectedSentiment != null
                        ) {
                            Text("Add")
                        }
                    },
                    dismissButton = {
                        Button(onClick = {
                            showHabitDialog = false
                            checklistName = ""
                            selectedTimestamp = null
                            selectedSentiment = null
                        }) {
                            Text("Cancel")
                        }
                    }
                )
            }

            // Single LazyColumn with all sections
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Active Habits Section
                if (checklists.isNotEmpty()) {
                    item(key = "active_habits_header") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Active Habits",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "(${checklists.size})",
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    items(
                        items = checklists,
                        key = { "active_${it.id}" }
                    ) { checklist ->
                        ActiveHabitItem(
                            checklist = checklist,
                            onCheckedChange = {
                                onChecklistComplete(checklist)
                            },
                            onNotesClick = { navController.navigate("notes/${it.id}") }
                        )
                    }
                }

                // Good Habits Section
                if (goodHabits.isNotEmpty()) {
                    item(key = "good_habits_header") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 24.dp, bottom = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Good Habits",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4CAF50)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "(${goodHabits.size})",
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    items(
                        items = goodHabits,
                        key = { "good_${it.id}" }
                    ) { checklist ->
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            CompletedHabitItem(
                                checklist = checklist,
                                onCheckedChange = {
                                    onChecklistUncomplete(checklist)
                                },
                                onNotesClick = { navController.navigate("notes/${it.id}") },
                                accentColor = Color(0xFF4CAF50)
                            )
                        }
                    }
                }

                // Bad Habits Section
                if (badHabits.isNotEmpty()) {
                    item(key = "bad_habits_header") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 24.dp, bottom = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Bad Habits",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFF44336)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "(${badHabits.size})",
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    items(
                        items = badHabits,
                        key = { "bad_${it.id}" }
                    ) { checklist ->
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            CompletedHabitItem(
                                checklist = checklist,
                                onCheckedChange = {
                                    onChecklistUncomplete(checklist)
                                },
                                onNotesClick = { navController.navigate("notes/${it.id}") },
                                accentColor = Color(0xFFF44336)
                            )
                        }
                    }
                }

                // Empty state
                if (checklists.isEmpty() && completedChecklists.isEmpty()) {
                    item(key = "empty_state") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Filled.Add,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "No habits yet",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                "Tap + to create your first habit",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Add bottom spacing for FAB
                item(key = "bottom_spacer") {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }
}

@Composable
fun ActiveHabitItem(
    checklist: Checklist,
    onCheckedChange: () -> Unit,
    onNotesClick: (Checklist) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = false,
                onCheckedChange = { onCheckedChange() }
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp)
            ) {
                Text(
                    text = checklist.name,
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp
                )
                Text(
                    text = if (checklist.sentiment == HabitSentiment.POSITIVE)
                        "Good habit"
                    else
                        "Bad habit",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
                checklist.dueTimestamp?.let {
                    Text(
                        text = "Due: ${formatTimestamp(it)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
            IconButton(onClick = { onNotesClick(checklist) }) {
                Icon(
                    Icons.AutoMirrored.Filled.StickyNote2,
                    contentDescription = "Add notes",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun CompletedHabitItem(
    checklist: Checklist,
    onCheckedChange: () -> Unit,
    onNotesClick: (Checklist) -> Unit,
    accentColor: Color
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = accentColor.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = true,
                onCheckedChange = { onCheckedChange() },
                colors = CheckboxDefaults.colors(
                    checkedColor = accentColor
                )
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp)
            ) {
                Text(
                    text = checklist.name,
                    textDecoration = TextDecoration.LineThrough,
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                checklist.dueTimestamp?.let {
                    Text(
                        text = "Completed: ${formatTimestamp(System.currentTimeMillis())}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
            IconButton(onClick = { onNotesClick(checklist) }) {
                Icon(
                    Icons.AutoMirrored.Filled.StickyNote2,
                    contentDescription = "View notes",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ChecklistItem(
    checklist: Checklist,
    isChecked: Boolean,
    onCheckedChange: () -> Unit,
    onNotesClick: (Checklist) -> Unit
) {
    // This is now deprecated - keeping for compatibility but use new components instead
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = isChecked, onCheckedChange = { onCheckedChange() })
        Column(modifier = Modifier.padding(start = 8.dp)) {
            Text(
                text = checklist.name,
                textDecoration = if (isChecked) TextDecoration.LineThrough else null
            )
            checklist.dueTimestamp?.let {
                Text(
                    text = "Due: ${formatTimestamp(it)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        IconButton(onClick = { onNotesClick(checklist) }) {
            Icon(Icons.AutoMirrored.Filled.StickyNote2, contentDescription = "Notes")
        }
    }
}

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

    // Get the currently visible month
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
                    val hasTasks = checklists.any {
                        it.dueTimestamp != null && isSameDay(it.dueTimestamp!!, day.date)
                    }

                    DayContent(
                        day = day,
                        isSelected = selectedDate == day.date,
                        hasTasks = hasTasks,
                        onClick = { selectedDate = it.date }
                    )
                }
            )

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()

            if (selectedDate != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.Event,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Tasks for ${formatDate(selectedDate!!)}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "(${tasksForSelectedDate.size})",
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (tasksForSelectedDate.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Filled.EventBusy,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No tasks scheduled",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 16.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp)
                    ) {
                        items(tasksForSelectedDate) { task ->
                            CalendarTaskItem(
                                task = task,
                                onTaskClick = { navController.navigate("notes/${it.id}") }
                            )
                        }
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Filled.TouchApp,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Select a date to view tasks",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
fun DayContent(
    day: CalendarDay,
    isSelected: Boolean,
    hasTasks: Boolean,
    onClick: (CalendarDay) -> Unit
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(4.dp)
            .clip(CircleShape)
            .background(
                color = when {
                    isSelected -> MaterialTheme.colorScheme.primary
                    hasTasks && day.position == DayPosition.MonthDate -> Color(0xFFBDBDBD)
                    else -> Color.Transparent
                }
            )
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
                Box(
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                )
            }
        }
    }
}

@Composable
fun CalendarTaskItem(
    task: Checklist,
    onTaskClick: (Checklist) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onTaskClick(task) },
        colors = CardDefaults.cardColors(
            containerColor = when (task.sentiment) {
                HabitSentiment.POSITIVE -> Color(0xFF4CAF50).copy(alpha = 0.1f)
                HabitSentiment.NEGATIVE -> Color(0xFFF44336).copy(alpha = 0.1f)
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(
                        when (task.sentiment) {
                            HabitSentiment.POSITIVE -> Color(0xFF4CAF50)
                            HabitSentiment.NEGATIVE -> Color(0xFFF44336)
                            else -> Color.Gray
                        }
                    )
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
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
                task.dueTimestamp?.let { timestamp ->
                    Text(
                        text = "Due: ${formatTimestamp(timestamp)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                if (task.notes.isNotBlank()) {
                    Row(
                        modifier = Modifier.padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.StickyNote2,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Has notes",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "View details",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            TextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes") },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                maxLines = Int.MAX_VALUE
            )
        }
    }
}

// Helper functions
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

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MyHabitTrackerTheme {
        // Preview content
    }
}
