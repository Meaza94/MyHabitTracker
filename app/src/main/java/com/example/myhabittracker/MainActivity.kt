package com.example.myhabittracker

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.example.myhabittracker.ui.theme.MyHabitTrackerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            var useDarkTheme by remember {
                mutableStateOf((context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES)
            }
            MyHabitTrackerTheme(darkTheme = useDarkTheme) {
                MyHabitTrackerApp(
                    darkTheme = useDarkTheme,
                    onThemeToggle = { useDarkTheme = !useDarkTheme }
                )
            }
        }
    }
}

data class Checklist(val id: Long, val name: String, var notes: String = "")

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

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Habit Tracker") },
                actions = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(if (darkTheme) "Dark Mode" else "Light Mode")
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
                AlertDialog(
                    onDismissRequest = { showHabitDialog = false },
                    title = { Text("New Habit") },
                    text = {
                        TextField(
                            value = checklistName,
                            onValueChange = { checklistName = it },
                            label = { Text("Habit Name") }
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (checklistName.isNotBlank()) {
                                    onAddChecklist(Checklist(System.currentTimeMillis(), checklistName))
                                    checklistName = ""
                                    showHabitDialog = false
                                }
                            }
                        ) {
                            Text("Add")
                        }
                    },
                    dismissButton = {
                        Button(onClick = { showHabitDialog = false }) {
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
        Text(
            text = checklist.name,
            modifier = Modifier.padding(start = 8.dp),
            textDecoration = if (isChecked) TextDecoration.LineThrough else null
        )
        Spacer(modifier = Modifier.weight(1f))
        IconButton(onClick = { onNotesClick(checklist) }) {
            Icon(Icons.AutoMirrored.Filled.Notes, contentDescription = "Notes")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(navController: NavController, checklist: Checklist) {
    var notes by remember { mutableStateOf(checklist.notes) }

    Scaffold(modifier = Modifier.fillMaxSize()) { it ->
        Column(
            modifier = Modifier
                .padding(it)
                .padding(16.dp)
        ) {
            Text("Notes for ${checklist.name}", modifier = Modifier.padding(bottom = 16.dp))
            TextField(
                value = notes,
                onValueChange = { notes = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Notes") }
            )
            Spacer(modifier = Modifier.weight(1f))
            Button(
                onClick = {
                    checklist.notes = notes
                    navController.popBackStack()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save")
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
fun MyHabitTrackerAppPreview() {
    MyHabitTrackerTheme {
        MyHabitTrackerApp(darkTheme = false, onThemeToggle = {})
    }
}
