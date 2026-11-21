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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.testtracker.gate.model.RevisionStatus
import com.testtracker.gate.model.RevisionWithTest
import com.testtracker.gate.model.SPACING_SCHEDULE
import com.testtracker.gate.model.formatDisplayDate
import com.testtracker.gate.model.formatShortDate
import com.testtracker.gate.model.revisionStatusFor
import com.testtracker.gate.model.subjectColorFor
import com.testtracker.gate.model.todayIso
import com.testtracker.gate.ui.components.EmptyStateCard
import com.testtracker.gate.ui.components.RevisionRowCard
import com.testtracker.gate.ui.theme.StatusAmber
import com.testtracker.gate.ui.theme.StatusBlue
import com.testtracker.gate.ui.theme.StatusGreen
import com.testtracker.gate.ui.theme.StatusRed
import com.testtracker.gate.viewmodel.TrackerUiState

@Composable
fun SubjectsScreen(
    state: TrackerUiState,
    onOpenTest: (String) -> Unit,
    onQuickComplete: (RevisionWithTest) -> Unit,
    onDetailedComplete: (RevisionWithTest) -> Unit,
    modifier: Modifier = Modifier
) {
    var mode by remember { mutableStateOf("Spreadsheet") }
    var selectedSubject by remember { mutableStateOf("All") }
    val today = remember { todayIso() }

    val revisionsByTest = remember(state.revisions) {
        state.revisions.groupBy { it.testId }
    }
    val testsBySubject = remember(state.tests, selectedSubject) {
        state.tests
            .filter { selectedSubject == "All" || it.subject == selectedSubject }
            .groupBy { it.subject }
            .toSortedMap()
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "mode_toggle") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = mode == "Spreadsheet",
                    onClick = { mode = "Spreadsheet" },
                    label = { Text("Spreadsheet View") }
                )
                FilterChip(
                    selected = mode == "Schedule",
                    onClick = { mode = "Schedule" },
                    label = { Text("Chronological Schedule") }
                )
            }
        }

        item(key = "subject_chips") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
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

        if (state.tests.isEmpty()) {
            item(key = "empty_subjects") {
                EmptyStateCard(
                    title = "No subjects yet",
                    subtitle = "Add tests to unlock subject-wise spreadsheets and revision schedules."
                )
            }
        } else if (mode == "Spreadsheet") {
            testsBySubject.forEach { (subject, subjectTests) ->
                item(key = "sheet_$subject") {
                    val accent = subjectColorFor(subject)
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.65f))
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            // Distinct colored subject header bar
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(accent)
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = subject,
                                    color = Color.White,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "${subjectTests.size} test${if (subjectTests.size == 1) "" else "s"}",
                                    color = Color.White.copy(alpha = 0.9f),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }

                            // Horizontally scrollable spreadsheet table
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                            ) {
                                // Header Row
                                Row(
                                    modifier = Modifier
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                                        .padding(vertical = 8.dp, horizontal = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    SheetCell("Test", 96, bold = true)
                                    SheetCell("Score", 72, bold = true)
                                    SheetCell("Acc", 52, bold = true)
                                    SPACING_SCHEDULE.forEach { st ->
                                        SheetCell(st.code, 68, bold = true, center = true)
                                    }
                                }

                                subjectTests.forEach { test ->
                                    val revMap = revisionsByTest[test.id].orEmpty().associateBy { it.stageCode }
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                                    Row(
                                        modifier = Modifier
                                            .clickable { onOpenTest(test.id) }
                                            .padding(vertical = 9.dp, horizontal = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        SheetCell(test.testNumber, 96, bold = true)
                                        SheetCell(
                                            if (test.marksScored != null && test.totalMarks != null) {
                                                "${test.marksScored}/${test.totalMarks}"
                                            } else "—",
                                            72
                                        )
                                        SheetCell(test.accuracyPercent?.let { "$it%" } ?: "—", 52)
                                        SPACING_SCHEDULE.forEach { st ->
                                            val rev = revMap[st.code]
                                            val status = rev?.let { revisionStatusFor(it, today) } ?: RevisionStatus.UPCOMING
                                            val pillColor = when (status) {
                                                RevisionStatus.COMPLETED -> StatusGreen
                                                RevisionStatus.OVERDUE -> StatusRed
                                                RevisionStatus.TODAY -> StatusAmber
                                                RevisionStatus.UPCOMING -> StatusBlue
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .width(68.dp)
                                                    .padding(horizontal = 2.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Surface(
                                                    color = pillColor.copy(alpha = 0.14f),
                                                    shape = RoundedCornerShape(6.dp)
                                                ) {
                                                    Text(
                                                        text = rev?.let { formatShortDate(it.scheduledDate) } ?: "—",
                                                        color = pillColor,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        textAlign = TextAlign.Center,
                                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 3.dp)
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
        } else {
            // Chronological Repetition Schedule filtered by subject
            val chrono = state.trackableRevisions
                .filter { selectedSubject == "All" || it.test.subject == selectedSubject }
                .sortedBy { it.revision.scheduledDate }
            items(chrono, key = { "subj_rev_${it.revision.id}" }) { item ->
                RevisionRowCard(
                    item = item,
                    onQuickDone = { onQuickComplete(item) },
                    onDetailedComplete = { onDetailedComplete(item) },
                    onOpenTest = { onOpenTest(item.test.id) }
                )
            }
        }

        item(key = "subj_spacer") { Spacer(modifier = Modifier.height(72.dp)) }
    }
}

@Composable
private fun SheetCell(text: String, widthDp: Int, bold: Boolean = false, center: Boolean = false) {
    Text(
        text = text,
        modifier = Modifier
            .width(widthDp.dp)
            .padding(horizontal = 4.dp),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
        textAlign = if (center) TextAlign.Center else TextAlign.Start,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}
