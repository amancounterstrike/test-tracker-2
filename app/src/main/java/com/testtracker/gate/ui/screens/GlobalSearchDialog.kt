package com.testtracker.gate.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.testtracker.gate.ui.components.ErrorCodeBadge
import com.testtracker.gate.ui.components.SubjectPill
import com.testtracker.gate.viewmodel.TrackerUiState

@Composable
fun GlobalSearchDialog(
    state: TrackerUiState,
    onDismiss: () -> Unit,
    onOpenTest: (String) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val q = query.trim().lowercase()

    val matchedTests = remember(state.tests, q) {
        if (q.isEmpty()) emptyList()
        else state.tests.filter {
            it.testNumber.lowercase().contains(q) ||
                it.subject.lowercase().contains(q) ||
                it.topic.lowercase().contains(q) ||
                it.notes.lowercase().contains(q)
        }
    }

    val matchedQuestions = remember(state.questions, q) {
        if (q.isEmpty()) emptyList()
        else state.questions.filter {
            it.questionNumber.lowercase().contains(q) ||
                it.topicTag.lowercase().contains(q) ||
                it.notes.lowercase().contains(q) ||
                it.errorCode.lowercase() == q
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .heightIn(max = 560.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Global Search",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close")
                    }
                }

                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Search tests, subjects, topics, Q14, C/F/A/E/T...") },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (q.isEmpty()) {
                        item {
                            Text(
                                text = "Type anything to search across all ${state.tests.size} tests and ${state.questions.size} logged questions.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else if (matchedTests.isEmpty() && matchedQuestions.isEmpty()) {
                        item {
                            Text(
                                text = "No matches found for \"$query\".",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        if (matchedTests.isNotEmpty()) {
                            item {
                                Text(
                                    text = "Tests (${matchedTests.size})",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            items(matchedTests, key = { "gs_t_${it.id}" }) { t ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onDismiss()
                                            onOpenTest(t.id)
                                        },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(t.testNumber, fontWeight = FontWeight.Bold)
                                        SubjectPill(subject = t.subject)
                                        if (t.topic.isNotBlank()) {
                                            Text(t.topic, style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                }
                            }
                        }

                        if (matchedQuestions.isNotEmpty()) {
                            item {
                                Text(
                                    text = "Questions (${matchedQuestions.size})",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(top = 6.dp)
                                )
                            }
                            items(matchedQuestions, key = { "gs_q_${it.id}" }) { mq ->
                                val parent = state.testMap[mq.testId]
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onDismiss()
                                            parent?.let { onOpenTest(it.id) }
                                        },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(mq.questionNumber, fontWeight = FontWeight.Bold)
                                        ErrorCodeBadge(code = mq.errorCode)
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = parent?.testNumber ?: "Test",
                                                style = MaterialTheme.typography.labelMedium
                                            )
                                            if (mq.notes.isNotBlank()) {
                                                Text(
                                                    text = mq.notes,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
