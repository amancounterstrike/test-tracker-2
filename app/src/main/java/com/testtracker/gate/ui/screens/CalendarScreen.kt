package com.testtracker.gate.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.testtracker.gate.model.RevisionStatus
import com.testtracker.gate.model.RevisionWithTest
import com.testtracker.gate.model.formatDisplayDate
import com.testtracker.gate.model.todayIso
import com.testtracker.gate.ui.components.RevisionRowCard
import com.testtracker.gate.ui.components.SubjectPill
import com.testtracker.gate.ui.theme.BrandOrange
import com.testtracker.gate.ui.theme.StatusAmber
import com.testtracker.gate.ui.theme.StatusBlue
import com.testtracker.gate.ui.theme.StatusGreen
import com.testtracker.gate.ui.theme.StatusRed
import com.testtracker.gate.viewmodel.TrackerUiState
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun CalendarScreen(
    state: TrackerUiState,
    onOpenTest: (String) -> Unit,
    onQuickComplete: (RevisionWithTest) -> Unit,
    onDetailedComplete: (RevisionWithTest) -> Unit,
    modifier: Modifier = Modifier
) {
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    var selectedIso by remember { mutableStateOf(todayIso()) }
    val today = remember { todayIso() }

    val testsByDate = remember(state.tests) { state.tests.groupBy { it.testDate } }
    val revisionsByDate = remember(state.trackableRevisions) {
        state.trackableRevisions.groupBy { it.revision.scheduledDate }
    }

    val selectedTests = testsByDate[selectedIso].orEmpty()
    val selectedRevs = revisionsByDate[selectedIso].orEmpty()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "calendar_card") {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Month navigation
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { currentMonth = currentMonth.minusMonths(1) }) {
                            Icon(Icons.Rounded.ChevronLeft, contentDescription = "Previous month")
                        }
                        Text(
                            text = "${currentMonth.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH)} ${currentMonth.year}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold
                        )
                        IconButton(onClick = { currentMonth = currentMonth.plusMonths(1) }) {
                            Icon(Icons.Rounded.ChevronRight, contentDescription = "Next month")
                        }
                    }

                    // Weekday headers
                    Row(modifier = Modifier.fillMaxWidth()) {
                        listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat").forEach { wd ->
                            Text(
                                text = wd,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Days Grid
                    val firstOfMonth = currentMonth.atDay(1)
                    val leadingEmpty = firstOfMonth.dayOfWeek.value % 7 // Sunday = 0
                    val daysInMonth = currentMonth.lengthOfMonth()
                    val totalCells = leadingEmpty + daysInMonth
                    val rows = (totalCells + 6) / 7

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        for (r in 0 until rows) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                for (c in 0 until 7) {
                                    val cellIndex = r * 7 + c
                                    val dayNum = cellIndex - leadingEmpty + 1
                                    if (dayNum in 1..daysInMonth) {
                                        val date = LocalDate.of(currentMonth.year, currentMonth.month, dayNum)
                                        val iso = date.toString()
                                        val isSelected = iso == selectedIso
                                        val isToday = iso == today
                                        val hasTests = testsByDate.containsKey(iso)
                                        val revs = revisionsByDate[iso].orEmpty()
                                        val hasOverdue = revs.any { it.status == RevisionStatus.OVERDUE }
                                        val hasPending = revs.any { it.status == RevisionStatus.TODAY || it.status == RevisionStatus.UPCOMING }
                                        val hasDone = revs.any { it.status == RevisionStatus.COMPLETED }

                                        Surface(
                                            onClick = { selectedIso = iso },
                                            modifier = Modifier
                                                .weight(1f)
                                                .aspectRatio(1f),
                                            shape = RoundedCornerShape(10.dp),
                                            color = when {
                                                isSelected -> MaterialTheme.colorScheme.primary
                                                isToday -> MaterialTheme.colorScheme.primaryContainer
                                                else -> Color.Transparent
                                            }
                                        ) {
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center
                                            ) {
                                                Text(
                                                    text = dayNum.toString(),
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = if (isSelected || isToday) FontWeight.ExtraBold else FontWeight.Medium,
                                                    color = when {
                                                        isSelected -> MaterialTheme.colorScheme.onPrimary
                                                        isToday -> MaterialTheme.colorScheme.onPrimaryContainer
                                                        else -> MaterialTheme.colorScheme.onSurface
                                                    }
                                                )
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                                                    modifier = Modifier.padding(top = 2.dp)
                                                ) {
                                                    if (hasTests) Dot(if (isSelected) Color.White else BrandOrange)
                                                    if (hasOverdue) Dot(if (isSelected) Color.White else StatusRed)
                                                    if (hasPending) Dot(if (isSelected) Color.White else StatusBlue)
                                                    if (hasDone) Dot(if (isSelected) Color.White else StatusGreen)
                                                }
                                            }
                                        }
                                    } else {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }

                    // Legend
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        LegendDot("Test", BrandOrange)
                        LegendDot("Overdue", StatusRed)
                        LegendDot("Due", StatusBlue)
                        LegendDot("Done", StatusGreen)
                    }
                }
            }
        }

        // Day Details Header
        item(key = "day_header") {
            Text(
                text = "Schedule for ${formatDisplayDate(selectedIso)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        if (selectedTests.isEmpty() && selectedRevs.isEmpty()) {
            item(key = "day_empty") {
                Text(
                    text = "No tests or revisions scheduled on this date.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(selectedTests, key = { "cal_test_${it.id}" }) { t ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenTest(t.id) },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, BrandOrange.copy(alpha = 0.45f))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Test Attempt (R0) • ${t.testNumber}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        SubjectPill(subject = t.subject)
                        if (t.topic.isNotBlank()) {
                            Text(
                                text = t.topic,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            items(selectedRevs, key = { "cal_rev_${it.revision.id}" }) { item ->
                RevisionRowCard(
                    item = item,
                    onQuickDone = { onQuickComplete(item) },
                    onDetailedComplete = { onDetailedComplete(item) },
                    onOpenTest = { onOpenTest(item.test.id) }
                )
            }
        }

        item(key = "cal_spacer") { Spacer(modifier = Modifier.height(72.dp)) }
    }
}

@Composable
private fun Dot(color: Color) {
    Box(
        modifier = Modifier
            .size(5.dp)
            .clip(CircleShape)
            .background(color)
    )
}

@Composable
private fun LegendDot(label: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
