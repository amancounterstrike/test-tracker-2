package com.testtracker.gate.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.testtracker.gate.model.QuestionRecord
import com.testtracker.gate.model.RevisionRecord
import com.testtracker.gate.model.RevisionStatus
import com.testtracker.gate.model.RevisionWithTest
import com.testtracker.gate.model.SPACING_SCHEDULE
import com.testtracker.gate.model.SUGGESTED_SUBJECTS
import com.testtracker.gate.model.TestRecord
import com.testtracker.gate.model.formatDisplayDate
import com.testtracker.gate.model.revisionStatusFor
import com.testtracker.gate.model.subjectColorFor
import com.testtracker.gate.model.todayIso
import com.testtracker.gate.ui.components.DatePickerField
import com.testtracker.gate.ui.components.EmptyStateCard
import com.testtracker.gate.ui.components.ErrorCodeBadge
import com.testtracker.gate.ui.components.SubjectPill
import com.testtracker.gate.ui.theme.StatusAmber
import com.testtracker.gate.ui.theme.StatusBlue
import com.testtracker.gate.ui.theme.StatusGreen
import com.testtracker.gate.ui.theme.StatusRed
import com.testtracker.gate.viewmodel.TrackerUiState
import java.util.UUID

@Composable
fun TestsScreen(
    state: TrackerUiState,
    onOpenTest: (String) -> Unit,
    onAddTestClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedSubject by remember { mutableStateOf("All") }
    var sortBy by remember { mutableStateOf("Newest") }

    val filtered = remember(state.tests, searchQuery, selectedSubject, sortBy) {
        val base = state.tests.filter { t ->
            val matchesSubject = selectedSubject == "All" || t.subject == selectedSubject
            val q = searchQuery.trim().lowercase()
            val matchesQuery = q.isEmpty() ||
                t.testNumber.lowercase().contains(q) ||
                t.subject.lowercase().contains(q) ||
                t.topic.lowercase().contains(q)
            matchesSubject && matchesQuery
        }
        when (sortBy) {
            "Oldest" -> base.sortedBy { it.testDate }
            "Score" -> base.sortedByDescending { it.scorePercent ?: -1 }
            "Accuracy" -> base.sortedByDescending { it.accuracyPercent ?: -1 }
            else -> base.sortedByDescending { it.testDate }
        }
    }

    val revisionsByTest = remember(state.revisions) {
        state.revisions.filter { it.stageCode != "R0" }.groupBy { it.testId }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item(key = "search_bar") {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search test number, subject, or topic") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )
        }

        item(key = "subject_filters") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedSubject == "All",
                    onClick = { selectedSubject = "All" },
                    label = { Text("All (${state.tests.size})") }
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

        item(key = "sort_chips") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Sort:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                listOf("Newest", "Oldest", "Score", "Accuracy").forEach { option ->
                    FilterChip(
                        selected = sortBy == option,
                        onClick = { sortBy = option },
                        label = { Text(option) }
                    )
                }
            }
        }

        if (filtered.isEmpty()) {
            item(key = "empty_tests") {
                EmptyStateCard(
                    title = "No matching tests",
                    subtitle = "Add a new test or clear your filters to view your test library.",
                    actionLabel = "Add Test",
                    onAction = onAddTestClick
                )
            }
        } else {
            items(filtered, key = { it.id }) { test ->
                val testRevs = revisionsByTest[test.id].orEmpty()
                val doneCount = testRevs.count { it.isCompleted }
                TestLibraryCard(
                    test = test,
                    doneRevisions = doneCount,
                    totalRevisions = testRevs.size,
                    onClick = { onOpenTest(test.id) }
                )
            }
        }

        item(key = "spacer") { Spacer(modifier = Modifier.height(72.dp)) }
    }
}

@Composable
private fun TestLibraryCard(
    test: TestRecord,
    doneRevisions: Int,
    totalRevisions: Int,
    onClick: () -> Unit
) {
    val accent = subjectColorFor(test.subject)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.65f))
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .height(116.dp)
                    .background(accent)
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = test.testNumber,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = formatDisplayDate(test.testDate),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                SubjectPill(subject = test.subject)
                if (test.topic.isNotBlank()) {
                    Text(
                        text = test.topic,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = if (test.marksScored != null && test.totalMarks != null) {
                                "${test.marksScored}/${test.totalMarks}"
                            } else "Score: —",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = test.accuracyPercent?.let { "Acc: $it%" } ?: "Acc: —",
                            style = MaterialTheme.typography.labelMedium,
                            color = StatusGreen
                        )
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            text = "Rev $doneRevisions/$totalRevisions",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTestBottomSheet(
    existingTest: TestRecord?,
    existingSubjects: List<String>,
    onDismiss: () -> Unit,
    onSave: (TestRecord) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var testNumber by remember { mutableStateOf(existingTest?.testNumber ?: "") }
    var subject by remember { mutableStateOf(existingTest?.subject ?: "") }
    var topic by remember { mutableStateOf(existingTest?.topic ?: "") }
    var testDate by remember { mutableStateOf(existingTest?.testDate ?: todayIso()) }
    var showOptionalMetrics by remember {
        mutableStateOf(
            existingTest?.totalMarks != null ||
                existingTest?.marksScored != null ||
                existingTest?.correctCount != null
        )
    }
    var totalMarks by remember { mutableStateOf(existingTest?.totalMarks?.toString() ?: "") }
    var marksScored by remember { mutableStateOf(existingTest?.marksScored?.toString() ?: "") }
    var correct by remember { mutableStateOf(existingTest?.correctCount?.toString() ?: "") }
    var incorrect by remember { mutableStateOf(existingTest?.incorrectCount?.toString() ?: "") }
    var skipped by remember { mutableStateOf(existingTest?.skippedCount?.toString() ?: "") }
    var timeTaken by remember { mutableStateOf(existingTest?.timeTakenMinutes?.toString() ?: "") }
    var notes by remember { mutableStateOf(existingTest?.notes ?: "") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val allSuggestions = remember(existingSubjects) {
        (existingSubjects + SUGGESTED_SUBJECTS).distinct()
    }

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
                text = if (existingTest == null) "Quick Add Test" else "Edit ${existingTest.testNumber}",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold
            )

            OutlinedTextField(
                value = testNumber,
                onValueChange = { testNumber = it; errorMsg = null },
                label = { Text("Test Name / Number *") },
                placeholder = { Text("e.g., CE-Mock-07") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = subject,
                onValueChange = { subject = it; errorMsg = null },
                label = { Text("Subject *") },
                placeholder = { Text("Type or tap a GATE CE subject below") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Quick 1-tap subject chips for streamlined mobile entry
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                allSuggestions.forEach { s ->
                    FilterChip(
                        selected = subject.equals(s, ignoreCase = true),
                        onClick = { subject = s; errorMsg = null },
                        label = { Text(s, maxLines = 1) }
                    )
                }
            }

            OutlinedTextField(
                value = topic,
                onValueChange = { topic = it },
                label = { Text("Topic / Syllabus Covered") },
                placeholder = { Text("e.g., Consolidation & Shear Strength") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            DatePickerField(
                label = "Test Date (R0 Anchor)",
                isoDate = testDate,
                onDateSelected = { testDate = it }
            )

            // Collapsible performance metrics for fewer taps
            Surface(
                onClick = { showOptionalMetrics = !showOptionalMetrics },
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Score, Accuracy & Time (Optional)",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Icon(
                        imageVector = if (showOptionalMetrics) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                        contentDescription = null
                    )
                }
            }

            if (showOptionalMetrics) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = totalMarks,
                        onValueChange = { totalMarks = it },
                        label = { Text("Total Marks") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = marksScored,
                        onValueChange = { marksScored = it },
                        label = { Text("Marks Scored") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = correct,
                        onValueChange = { correct = it },
                        label = { Text("Correct") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = incorrect,
                        onValueChange = { incorrect = it },
                        label = { Text("Wrong") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = skipped,
                        onValueChange = { skipped = it },
                        label = { Text("Skip") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = timeTaken,
                    onValueChange = { timeTaken = it },
                    label = { Text("Time Taken (minutes)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes / Key Takeaways") },
                minLines = 2,
                maxLines = 4,
                modifier = Modifier.fillMaxWidth()
            )

            errorMsg?.let {
                Text(text = it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
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
                        if (testNumber.isBlank() || subject.isBlank()) {
                            errorMsg = "Please enter both Test Name and Subject."
                            return@Button
                        }
                        val record = TestRecord(
                            id = existingTest?.id ?: UUID.randomUUID().toString(),
                            testNumber = testNumber.trim(),
                            subject = subject.trim(),
                            topic = topic.trim(),
                            testDate = testDate,
                            totalMarks = totalMarks.trim().toDoubleOrNull(),
                            marksScored = marksScored.trim().toDoubleOrNull(),
                            correctCount = correct.trim().toIntOrNull(),
                            incorrectCount = incorrect.trim().toIntOrNull(),
                            skippedCount = skipped.trim().toIntOrNull(),
                            timeTakenMinutes = timeTaken.trim().toIntOrNull(),
                            notes = notes.trim()
                        )
                        onSave(record)
                    },
                    modifier = Modifier.weight(1.4f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(if (existingTest == null) "Save & Generate R0–R7" else "Save Changes")
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestDetailScreen(
    test: TestRecord,
    revisions: List<RevisionRecord>,
    questions: List<QuestionRecord>,
    onBack: () -> Unit,
    onEditTest: () -> Unit,
    onDeleteTest: () -> Unit,
    onCompleteRevision: (RevisionWithTest) -> Unit,
    onQuickToggleRevision: (RevisionRecord) -> Unit,
    onAddQuestionClick: () -> Unit,
    onDeleteQuestion: (String) -> Unit
) {
    var confirmDelete by remember { mutableStateOf(false) }
    val today = remember { todayIso() }
    val revMap = remember(revisions) { revisions.associateBy { it.stageCode } }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Text(
                    text = test.testNumber,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Bold
                )
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                IconButton(onClick = onEditTest) {
                    Icon(Icons.Rounded.Edit, contentDescription = "Edit Test")
                }
                IconButton(onClick = { confirmDelete = true }) {
                    Icon(Icons.Rounded.Delete, contentDescription = "Delete Test", tint = StatusRed)
                }
            }
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item(key = "overview") {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SubjectPill(subject = test.subject)
                        if (test.topic.isNotBlank()) {
                            Text(
                                text = test.topic,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "Test Date: ${formatDisplayDate(test.testDate)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            StatMini(
                                label = "Score",
                                value = if (test.marksScored != null && test.totalMarks != null) {
                                    "${test.marksScored}/${test.totalMarks}"
                                } else "—"
                            )
                            StatMini(
                                label = "Accuracy",
                                value = test.accuracyPercent?.let { "$it%" } ?: "—"
                            )
                            StatMini(
                                label = "C / W / S",
                                value = if (test.correctCount != null) {
                                    "${test.correctCount}/${test.incorrectCount ?: 0}/${test.skippedCount ?: 0}"
                                } else "—"
                            )
                            StatMini(
                                label = "Time",
                                value = test.timeTakenMinutes?.let { "${it}m" } ?: "—"
                            )
                        }
                        if (test.notes.isNotBlank()) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            Text(
                                text = test.notes,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Strict R0–R7 Calendar Repetition Chart (ONLY R0..R7 + dd MMM yyyy date, no Day/D-offset labels)
            item(key = "repetition_chart") {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Spaced Repetition Calendar (R0–R7)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Tap any stage R1–R7 to mark complete or update notes.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        // 2 rows of 4 cards (R0..R3 and R4..R7)
                        SPACING_SCHEDULE.chunked(4).forEach { rowStages ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowStages.forEach { stage ->
                                    val rev = revMap[stage.code]
                                    val status = rev?.let { revisionStatusFor(it, today) } ?: RevisionStatus.UPCOMING
                                    val cellColor = when (status) {
                                        RevisionStatus.COMPLETED -> StatusGreen
                                        RevisionStatus.OVERDUE -> StatusRed
                                        RevisionStatus.TODAY -> StatusAmber
                                        RevisionStatus.UPCOMING -> StatusBlue
                                    }
                                    Surface(
                                        onClick = {
                                            if (rev != null && stage.code != "R0") {
                                                if (rev.isCompleted) {
                                                    onQuickToggleRevision(rev)
                                                } else {
                                                    onCompleteRevision(RevisionWithTest(rev, test, status))
                                                }
                                            }
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        color = cellColor.copy(alpha = 0.13f),
                                        border = BorderStroke(1.dp, cellColor.copy(alpha = 0.45f)),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                                            ) {
                                                Text(
                                                    text = stage.code,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = cellColor,
                                                    fontSize = 14.sp
                                                )
                                                if (rev?.isCompleted == true) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.CheckCircle,
                                                        contentDescription = "Done",
                                                        tint = StatusGreen,
                                                        modifier = Modifier.size(13.dp)
                                                    )
                                                }
                                            }
                                            Text(
                                                text = rev?.let { formatDisplayDate(it.scheduledDate) } ?: "—",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontSize = 10.sp,
                                                textAlign = TextAlign.Center,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 2
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Linked Questions
            item(key = "questions_hdr") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Logged Questions (${questions.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Button(
                        onClick = onAddQuestionClick,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Log Mistake", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            if (questions.isEmpty()) {
                item(key = "no_questions") {
                    Text(
                        text = "No mistakes logged for this test yet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(questions, key = { it.id }) { q ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = q.questionNumber,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold
                            )
                            ErrorCodeBadge(code = q.errorCode, showLabel = true)
                            Column(modifier = Modifier.weight(1f)) {
                                if (q.topicTag.isNotBlank()) {
                                    Text(
                                        text = q.topicTag,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold
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
                            IconButton(onClick = { onDeleteQuestion(q.id) }) {
                                Icon(
                                    imageVector = Icons.Rounded.Delete,
                                    contentDescription = "Remove question",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            item(key = "detail_spacer") { Spacer(modifier = Modifier.height(40.dp)) }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete ${test.testNumber}?") },
            text = { Text("This will permanently remove the test, all R0–R7 revisions, and linked mistakes.") },
            confirmButton = {
                Button(
                    onClick = {
                        confirmDelete = false
                        onDeleteTest()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusRed)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun StatMini(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}
