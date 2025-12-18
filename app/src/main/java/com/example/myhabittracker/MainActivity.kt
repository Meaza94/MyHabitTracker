package com.example.myhabittracker

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.kizitonwose.calendar.compose.HorizontalCalendar
import com.kizitonwose.calendar.compose.rememberCalendarState
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.firstDayOfWeekFromLocale
import java.text.SimpleDateFormat
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Calendar
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val configuration = LocalConfiguration.current
            var useDarkTheme by remember {
                mutableStateOf((configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES)
            }

            MaterialTheme(
                colorScheme = if (useDarkTheme) darkColorScheme() else lightColorScheme()
            ) {
                MyHabitTrackerApp(
                    darkTheme = useDarkTheme,
                    onThemeToggle = { useDarkTheme = !useDarkTheme }
                )
            }
        }
    }
}

data class Checklist(val id: Long, val name: String, var notes: String = "", var dueTimestamp: Long? = null)

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
            CalendarScreen(navController = navController)
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

    val context = LocalContext.current

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
                            onCheckedChange = { onThemeToggle() },
                            modifier = Modifier.padding(start = 8.dp, end = 8.dp)
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showHabitDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Add Checklist")
            }
        },
        floatingActionButtonPosition = FabPosition.Center
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            if (showHabitDialog) {
                val calendar = Calendar.getInstance()
                selectedTimestamp?.let { calendar.timeInMillis = it }

                val datePickerDialog = DatePickerDialog(
                    context,
                    { _, year, month, dayOfMonth ->
                        calendar.set(year, month, dayOfMonth)
                        selectedTimestamp = calendar.timeInMillis
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
                )

                val timePickerDialog = TimePickerDialog(
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

                AlertDialog(
                    onDismissRequest = {
                        showHabitDialog = false
                        selectedTimestamp = null
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
                                if (checklistName.isNotBlank()) {
                                    onAddChecklist(Checklist(System.currentTimeMillis(), checklistName, dueTimestamp = selectedTimestamp))
                                    checklistName = ""
                                    selectedTimestamp = null
                                    showHabitDialog = false
                                }
                            }
                        ) {
                            Text("Add")
                        }
                    },
                    dismissButton = {
                        Button(
                            onClick = {
                                showHabitDialog = false
                                selectedTimestamp = null
                            }
                        ) {
                            Text("Cancel")
                        }
                    }
                )
            }

            LazyColumn(modifier = Modifier.padding(16.dp)) {
                items(checklists, key = { it.id }) { checklist ->
                    ChecklistItem(
                        checklist = checklist,
                        isChecked = false,
                        onCheckedChange = { onChecklistComplete(checklist) },
                        onNotesClick = { navController.navigate("notes/${it.id}") }
                    )
                }
            }

            if (completedChecklists.isNotEmpty()) {
                Text("Completed Habits", modifier = Modifier.padding(16.dp))

                LazyColumn(modifier = Modifier.padding(16.dp)) {
                    items(completedChecklists, key = { it.id }) { checklist ->
                        ChecklistItem(
                            checklist = checklist,
                            isChecked = true,
                            onCheckedChange = { onChecklistUncomplete(checklist) },
                            onNotesClick = { navController.navigate("notes/${it.id}") }
                        )
                    }
                }
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = isChecked,
            onCheckedChange = { onCheckedChange() }
        )
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
            Icon(Icons.Filled.Notes, contentDescription = "Notes")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(navController: NavController, checklist: Checklist) {
    var notes by remember { mutableStateOf(checklist.notes) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(checklist.name) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            checklist.notes = notes
                            navController.popBackStack()
                        }
                    ) {
                        Text("Save")
                    }
                }
            )
        }
    ) { innerPadding ->
        TextField(
            value = notes,
            onValueChange = { notes = it },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            placeholder = { Text("Take a note...") },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(navController: NavController) {
    val currentMonth = remember { YearMonth.now() }
    val startMonth = remember { currentMonth.minusMonths(100) }
    val endMonth = remember { currentMonth.plusMonths(100) }
    val firstDayOfWeek = remember { firstDayOfWeekFromLocale() }

    val state = rememberCalendarState(
        startMonth = startMonth,
        endMonth = endMonth,
        firstVisibleMonth = currentMonth,
        firstDayOfWeek = firstDayOfWeek
    )

    val visibleMonth = state.firstVisibleMonth.yearMonth
    val title = remember(visibleMonth) {
        val month = visibleMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())
        "$month ${visibleMonth.year}"
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        HorizontalCalendar(
            modifier = Modifier.padding(innerPadding),
            state = state,
            dayContent = { DayContent(day = it) }
        )
    }
}

@Composable
fun DayContent(day: CalendarDay) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(4.dp)
            .clip(CircleShape)
            .background(color = if (day.position == DayPosition.MonthDate) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent),
        contentAlignment = Alignment.Center
    ) {
        Text(text = day.date.dayOfMonth.toString())
    }
}

fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy, hh:mm a", Locale.getDefault())
    return sdf.format(timestamp)
}


@Preview(showBackground = true)
@Composable
fun MyHabitTrackerAppPreview() {
    MaterialTheme {
        MyHabitTrackerApp(darkTheme = false, onThemeToggle = {})
    }
}