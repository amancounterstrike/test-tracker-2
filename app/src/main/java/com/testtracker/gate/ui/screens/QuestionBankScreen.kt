package com.testtracker.gate.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.testtracker.gate.model.ERROR_CATEGORIES
import com.testtracker.gate.model.TestRecord
import com.testtracker.gate.ui.components.EmptyStateCard
import com.testtracker.gate.ui.components.ErrorCodeBadge
import com.testtracker.gate.ui.components.SubjectPill
import com.testtracker.gate.ui.theme.StatusRed
import com.testtracker.gate.viewmodel.TrackerUiState

@Composable
fun QuestionBankScreen(
    state: TrackerUiState,
    onOpenTest: (String) -> Unit,
    onAddQuestionClick: () -> Unit,
    onDeleteQuestion: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCode by remember { mutableStateOf("All") }
    var selectedSubject by remember { mutableStateOf("All") }

    val testMap = state.testMap
    val repeatedSet = state.repeatedQuestionNumbers

    val filtered = remember(state.questions, selectedCode, selectedSubject, testMap) {
        state.questions.filter { q ->
            val t = testMap[q.testId]
            val matchesCode = selectedCode == "All" || q.errorCode.equals(selectedCode, ignoreCase = true)
            val matchesSubj = selectedSubject == "All" || t?.subject == selectedSubject
            matchesCode && matchesSubj
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Error code legend card
        item(key = "legend") {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Error Classification Legend",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Button(
                            onClick = onAddQuestionClick,
                            enabled = state.tests.isNotEmpty(),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Log Mistake", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ERROR_CATEGORIES.forEach { cat ->
                            ErrorCodeBadge(code = cat.code, showLabel = true)
                        }
                    }
                }
            }
        }

        // Repeated mistake alert banner
        if (repeatedSet.isNotEmpty()) {
            item(key = "repeated_banner") {
                Surface(
                    color = StatusRed.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, StatusRed.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Warning,
                            contentDescription = null,
                            tint = StatusRed,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Repeated Question Numbers across tests: ${repeatedSet.sorted().joinToString(", ")}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = StatusRed
                        )
                    }
                }
            }
        }

        // Error Code Filter Chips
        item(key = "code_filters") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = selectedCode == "All",
                    onClick = { selectedCode = "All" },
                    label = { Text("All (${state.questions.size})") }
                )
                ERROR_CATEGORIES.forEach { cat ->
                    val count = state.questions.count { it.errorCode.equals(cat.code, ignoreCase = true) }
                    FilterChip(
                        selected = selectedCode == cat.code,
                        onClick = { selectedCode = cat.code },
                        label = { Text("${cat.code} ($count)") }
                    )
                }
            }
        }

        // Subject Filter Chips
        if (state.distinctSubjects.isNotEmpty()) {
            item(key = "q_subj_filters") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = selectedSubject == "All",
                        onClick = { selectedSubject = "All" },
                        label = { Text("All Subjects") }
                    )
                    state.distinctSubjects.forEach { subj ->
                        FilterChip(
                            selected = selectedSubject == subj,
                            onClick = { selectedSubject = subj },
                            label = { Text(subj, maxLines = 1) }
                        )
                    }
                }
            }
        }

        if (filtered.isEmpty()) {
            item(key = "q_empty") {
                EmptyStateCard(
                    title = "No mistakes logged",
                    subtitle = if (state.tests.isEmpty()) "Add a test first, then log mistakes with C/F/A/E/T codes."
                    else "Tap 'Log Mistake' above to add single or bulk questions (e.g., Q3: C, Q8: F).",
                    actionLabel = if (state.tests.isNotEmpty()) "Log Mistake" else null,
                    onAction = if (state.tests.isNotEmpty()) onAddQuestionClick else null
                )
            }
        } else {
            items(filtered, key = { it.id }) { q ->
                val test = testMap[q.testId]
                val isRepeated = q.questionNumber.trim().uppercase() in repeatedSet
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { test?.let { onOpenTest(it.id) } },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(
                        1.dp,
                        if (isRepeated) StatusRed.copy(alpha = 0.55f)
                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = q.questionNumber,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                ErrorCodeBadge(code = q.errorCode, showLabel = true)
                                if (isRepeated) {
                                    Surface(
                                        color = StatusRed.copy(alpha = 0.14f),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = "Repeated",
                                            color = StatusRed,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            IconButton(
                                onClick = { onDeleteQuestion(q.id) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Delete,
                                    contentDescription = "Delete",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        if (test != null) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = test.testNumber,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                SubjectPill(subject = test.subject)
                            }
                        }

                        if (q.topicTag.isNotBlank()) {
                            Text(
                                text = "Topic: ${q.topicTag}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        if (q.notes.isNotBlank()) {
                            Text(
                                text = q.notes,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        item(key = "q_spacer") { Spacer(modifier = Modifier.height(72.dp)) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddQuestionBottomSheet(
    tests: List<TestRecord>,
    preselectedTestId: String?,
    onDismiss: () -> Unit,
    onAddSingle: (testId: String, qNo: String, code: String, topic: String, notes: String) -> Unit,
    onAddBulk: (testId: String, bulkText: String, defaultTopic: String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedTestId by remember { mutableStateOf(preselectedTestId ?: tests.firstOrNull()?.id.orEmpty()) }
    var isBulkMode by remember { mutableStateOf(false) }
    var qNumber by remember { mutableStateOf("") }
    var errorCode by remember { mutableStateOf("C") }
    var topicTag by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var bulkText by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Log Mistake / Question",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = !isBulkMode,
                    onClick = { isBulkMode = false },
                    label = { Text("Single Entry") }
                )
                FilterChip(
                    selected = isBulkMode,
                    onClick = { isBulkMode = true },
                    label = { Text("Bulk Entry (Q3: C, Q8: F)") }
                )
            }

            // Select Test chips
            Text(
                text = "Select Test",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                tests.forEach { t ->
                    FilterChip(
                        selected = selectedTestId == t.id,
                        onClick = { selectedTestId = t.id },
                        label = { Text(t.testNumber) }
                    )
                }
            }

            if (!isBulkMode) {
                OutlinedTextField(
                    value = qNumber,
                    onValueChange = { qNumber = it },
                    label = { Text("Question Number *") },
                    placeholder = { Text("e.g., Q14") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Error Type",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ERROR_CATEGORIES.forEach { cat ->
                        FilterChip(
                            selected = errorCode == cat.code,
                            onClick = { errorCode = cat.code },
                            label = { Text("${cat.code} • ${cat.label}") }
                        )
                    }
                }

                OutlinedTextField(
                    value = topicTag,
                    onValueChange = { topicTag = it },
                    label = { Text("Sub-topic (optional)") },
                    placeholder = { Text("e.g., Terzaghi Bearing Capacity") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Mistake Notes") },
                    placeholder = { Text("Why did it go wrong? How to avoid next time?") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                OutlinedTextField(
                    value = bulkText,
                    onValueChange = { bulkText = it },
                    label = { Text("Bulk Questions (one per line or comma-separated)") },
                    placeholder = { Text("Q3: C\nQ8: F\nQ15: E Unit slip") },
                    minLines = 4,
                    maxLines = 8,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = topicTag,
                    onValueChange = { topicTag = it },
                    label = { Text("Default Topic Tag (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Cancel")
                }
                Button(
                    onClick = {
                        if (selectedTestId.isBlank()) return@Button
                        if (!isBulkMode) {
                            onAddSingle(
                                selectedTestId,
                                qNumber.ifBlank { "Q1" },
                                errorCode,
                                topicTag,
                                notes
                            )
                        } else {
                            onAddBulk(selectedTestId, bulkText, topicTag)
                        }
                        onDismiss()
                    },
                    modifier = Modifier.weight(1.3f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(if (isBulkMode) "Add All" else "Save Question")
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
