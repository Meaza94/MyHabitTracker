package com.example.myhabittracker.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import java.util.Calendar
// Imports for data and components
import com.example.myhabittracker.data.Checklist
import com.example.myhabittracker.data.HabitSentiment
import com.example.myhabittracker.data.formatTimestamp
import com.example.myhabittracker.ui.components.ActiveHabitItem
import com.example.myhabittracker.ui.components.CompletedHabitItem

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

    // Categorize completed habits
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
                // Initialize calendar
                val calendar = remember {
                    Calendar.getInstance().apply { selectedTimestamp?.let { timeInMillis = it } }
                }

                LaunchedEffect(selectedTimestamp) {
                    selectedTimestamp?.let { calendar.timeInMillis = it }
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
                                        Text("+", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = if (selectedSentiment == HabitSentiment.POSITIVE) Color(0xFF4CAF50) else Color.Gray)
                                    },
                                    modifier = Modifier.weight(1f)
                                )

                                FilterChip(
                                    selected = selectedSentiment == HabitSentiment.NEGATIVE,
                                    onClick = { selectedSentiment = HabitSentiment.NEGATIVE },
                                    label = { Text("Bad Habit") },
                                    leadingIcon = {
                                        Text("-", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = if (selectedSentiment == HabitSentiment.NEGATIVE) Color(0xFFF44336) else Color.Gray)
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            selectedSentiment?.let { sentiment ->
                                Text(
                                    text = if (sentiment == HabitSentiment.POSITIVE) "✓ Positive habit I did (e.g., Exercise)" else "✓ Negative habit I did (e.g., Smoking)",
                                    modifier = Modifier.padding(top = 4.dp),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(onClick = { datePickerDialog.show() }, modifier = Modifier.weight(1f)) { Text("Set Date") }
                                Button(onClick = { timePickerDialog.show() }, modifier = Modifier.weight(1f)) { Text("Set Time") }
                            }
                            selectedTimestamp?.let {
                                Text(text = "Due: ${formatTimestamp(it)}", modifier = Modifier.padding(top = 8.dp))
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
                        ) { Text("Add") }
                    },
                    dismissButton = {
                        Button(onClick = {
                            showHabitDialog = false
                            checklistName = ""
                            selectedTimestamp = null
                            selectedSentiment = null
                        }) { Text("Cancel") }
                    }
                )
            }

            // List of Habits
            LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                // Active Habits
                if (checklists.isNotEmpty()) {
                    item(key = "active_habits_header") {
                        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Active Habits", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("(${checklists.size})", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    items(items = checklists, key = { "active_${it.id}" }) { checklist ->
                        ActiveHabitItem(
                            checklist = checklist,
                            onCheckedChange = { onChecklistComplete(checklist) },
                            onNotesClick = { navController.navigate("notes/${it.id}") }
                        )
                    }
                }

                // Good Habits
                if (goodHabits.isNotEmpty()) {
                    item(key = "good_habits_header") {
                        Row(modifier = Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("Good Habits", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("(${goodHabits.size})", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    items(items = goodHabits, key = { "good_${it.id}" }) { checklist ->
                        AnimatedVisibility(visible = true, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                            CompletedHabitItem(
                                checklist = checklist,
                                onCheckedChange = { onChecklistUncomplete(checklist) },
                                onNotesClick = { navController.navigate("notes/${it.id}") },
                                accentColor = Color(0xFF4CAF50)
                            )
                        }
                    }
                }

                // Bad Habits
                if (badHabits.isNotEmpty()) {
                    item(key = "bad_habits_header") {
                        Row(modifier = Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("Bad Habits", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF44336))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("(${badHabits.size})", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    items(items = badHabits, key = { "bad_${it.id}" }) { checklist ->
                        AnimatedVisibility(visible = true, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                            CompletedHabitItem(
                                checklist = checklist,
                                onCheckedChange = { onChecklistUncomplete(checklist) },
                                onNotesClick = { navController.navigate("notes/${it.id}") },
                                accentColor = Color(0xFFF44336)
                            )
                        }
                    }
                }

                // Empty state
                if (checklists.isEmpty() && completedChecklists.isEmpty()) {
                    item(key = "empty_state") {
                        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("No habits yet", fontSize = 18.sp, fontWeight = FontWeight.Medium)
                            Text("Tap + to create your first habit", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                item(key = "bottom_spacer") { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }
}