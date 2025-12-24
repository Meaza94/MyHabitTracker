package com.example.myhabittracker.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.StickyNote2
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
// Imports for data models
import com.example.myhabittracker.data.Checklist
import com.example.myhabittracker.data.HabitSentiment
import com.example.myhabittracker.data.formatTimestamp

@Composable
fun ActiveHabitItem(
    checklist: Checklist,
    onCheckedChange: () -> Unit,
    onNotesClick: (Checklist) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = false, onCheckedChange = { onCheckedChange() })
            Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
                Text(text = checklist.name, fontWeight = FontWeight.Medium, fontSize = 16.sp)
                Text(
                    text = if (checklist.sentiment == HabitSentiment.POSITIVE) "Good habit" else "Bad habit",
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
                Icon(Icons.AutoMirrored.Filled.StickyNote2, contentDescription = "Add notes", tint = MaterialTheme.colorScheme.onSurfaceVariant)
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
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = accentColor.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = true,
                onCheckedChange = { onCheckedChange() },
                colors = CheckboxDefaults.colors(checkedColor = accentColor)
            )
            Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
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
                Icon(Icons.AutoMirrored.Filled.StickyNote2, contentDescription = "View notes", tint = MaterialTheme.colorScheme.onSurfaceVariant)
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
    // Deprecated component kept for compatibility
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = isChecked, onCheckedChange = { onCheckedChange() })
        Column(modifier = Modifier.padding(start = 8.dp)) {
            Text(text = checklist.name, textDecoration = if (isChecked) TextDecoration.LineThrough else null)
            checklist.dueTimestamp?.let {
                Text(text = "Due: ${formatTimestamp(it)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        IconButton(onClick = { onNotesClick(checklist) }) {
            Icon(Icons.AutoMirrored.Filled.StickyNote2, contentDescription = "Notes")
        }
    }
}