package com.example.myhabittracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.myhabittracker.ui.theme.MyHabitTrackerTheme
import com.example.myhabittracker.data.Checklist
// Imports for the screens located in the other files
import com.example.myhabittracker.ui.screens.HabitListScreen
import com.example.myhabittracker.ui.screens.CalendarScreen
import com.example.myhabittracker.ui.screens.NotesScreen

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

@Composable
fun MyHabitTrackerApp(darkTheme: Boolean, onThemeToggle: () -> Unit) {
    val navController = rememberNavController()
    // State is hoisted here to persist across navigation
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