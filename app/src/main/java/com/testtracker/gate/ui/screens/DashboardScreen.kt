package com.testtracker.gate.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Assignment
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.EventAvailable
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Percent
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.testtracker.gate.model.RevisionWithTest
import com.testtracker.gate.model.TestRecord
import com.testtracker.gate.model.formatDisplayDate
import com.testtracker.gate.ui.components.EmptyStateCard
import com.testtracker.gate.ui.components.HeroBannerCard
import com.testtracker.gate.ui.components.MetricCard
import com.testtracker.gate.ui.components.RevisionRowCard
import com.testtracker.gate.ui.components.SectionHeader
import com.testtracker.gate.ui.components.SubjectPill
import com.testtracker.gate.ui.theme.BrandOrange
import com.testtracker.gate.ui.theme.StatusAmber
import com.testtracker.gate.ui.theme.StatusBlue
import com.testtracker.gate.ui.theme.StatusGreen
import com.testtracker.gate.ui.theme.StatusRed
import com.testtracker.gate.viewmodel.TrackerUiState

@Composable
fun DashboardScreen(
    state: TrackerUiState,
    onOpenTest: (String) -> Unit,
    onQuickComplete: (RevisionWithTest) -> Unit,
    onDetailedComplete: (RevisionWithTest) -> Unit,
    onNavigateToRevisions: () -> Unit,
    onNavigateToTests: () -> Unit,
    onLoadSampleData: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dueToday = state.dueTodayRevisions
    val overdue = state.overdueRevisions
    val upcoming = state.upcomingRevisions.take(5)
    val recentTests = state.tests.take(4)

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item(key = "hero") {
            HeroBannerCard(
                branchSubtitle = state.settings.branchSubtitle,
                examTitle = state.settings.examTitle
            )
        }

        // 6 KPI metric cards in 3 compact rows of 2
        item(key = "metrics_row_1") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricCard(
                    title = "Total Tests",
                    value = state.tests.size.toString(),
                    subtitle = "${state.distinctSubjects.size} subjects tracked",
                    accentColor = BrandOrange,
                    icon = Icons.Rounded.Assignment,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Due Today",
                    value = dueToday.size.toString(),
                    subtitle = "R1–R7 scheduled today",
                    accentColor = StatusAmber,
                    icon = Icons.Rounded.EventAvailable,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item(key = "metrics_row_2") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricCard(
                    title = "Overdue",
                    value = overdue.size.toString(),
                    subtitle = if (overdue.isEmpty()) "All caught up!" else "Needs immediate review",
                    accentColor = StatusRed,
                    icon = Icons.Rounded.ErrorOutline,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Avg Score",
                    value = state.averageScorePercent?.let { "$it%" } ?: "—",
                    subtitle = "Across scored tests",
                    accentColor = StatusBlue,
                    icon = Icons.Rounded.Insights,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item(key = "metrics_row_3") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricCard(
                    title = "Avg Accuracy",
                    value = state.averageAccuracyPercent?.let { "$it%" } ?: "—",
                    subtitle = "${state.questions.size} mistakes logged",
                    accentColor = StatusGreen,
                    icon = Icons.Rounded.Percent,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Revisions Done",
                    value = "${state.revisionCompletionRate}%",
                    subtitle = "${state.completedRevisions.size}/${state.trackableRevisions.size} completed",
                    accentColor = StatusGreen,
                    icon = Icons.Rounded.CheckCircle,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Revision Completion Progress Bar
        item(key = "progress_bar") {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Spaced Repetition Progress (R1–R7)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${state.revisionCompletionRate}%",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    LinearProgressIndicator(
                        progress = { state.revisionCompletionRate / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(50)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Text(
                        text = "${state.completedRevisions.size} completed • ${dueToday.size} today • ${overdue.size} overdue • ${state.upcomingRevisions.size} upcoming",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (state.tests.isEmpty()) {
            item(key = "empty_workspace") {
                EmptyStateCard(
                    title = "Welcome to Test Tracker",
                    subtitle = "Tap + below to add your first GATE test, or load 6 sample GATE Civil Engineering tests to explore.",
                    actionLabel = "Load Sample CE Tests",
                    onAction = onLoadSampleData
                )
            }
        } else {
            // Today's Revisions
            item(key = "hdr_today") {
                SectionHeader(
                    title = "Due Today",
                    badgeCount = dueToday.size,
                    badgeColor = StatusAmber,
                    actionLabel = "View All",
                    onAction = onNavigateToRevisions
                )
            }
            if (dueToday.isEmpty()) {
                item(key = "no_today") {
                    Text(
                        text = "No revisions due today. Great momentum!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            } else {
                items(dueToday, key = { "today_${it.revision.id}" }) { item ->
                    RevisionRowCard(
                        item = item,
                        onQuickDone = { onQuickComplete(item) },
                        onDetailedComplete = { onDetailedComplete(item) },
                        onOpenTest = { onOpenTest(item.test.id) }
                    )
                }
            }

            // Overdue Revisions
            if (overdue.isNotEmpty()) {
                item(key = "hdr_overdue") {
                    SectionHeader(
                        title = "Overdue Revisions",
                        badgeCount = overdue.size,
                        badgeColor = StatusRed,
                        actionLabel = "Clear All",
                        onAction = onNavigateToRevisions
                    )
                }
                items(overdue.take(5), key = { "overdue_${it.revision.id}" }) { item ->
                    RevisionRowCard(
                        item = item,
                        onQuickDone = { onQuickComplete(item) },
                        onDetailedComplete = { onDetailedComplete(item) },
                        onOpenTest = { onOpenTest(item.test.id) }
                    )
                }
            }

            // Upcoming Revisions
            if (upcoming.isNotEmpty()) {
                item(key = "hdr_upcoming") {
                    SectionHeader(
                        title = "Upcoming Revisions",
                        badgeCount = state.upcomingRevisions.size,
                        badgeColor = StatusBlue,
                        actionLabel = "Schedule",
                        onAction = onNavigateToRevisions
                    )
                }
                items(upcoming, key = { "upcoming_${it.revision.id}" }) { item ->
                    RevisionRowCard(
                        item = item,
                        onQuickDone = { onQuickComplete(item) },
                        onDetailedComplete = { onDetailedComplete(item) },
                        onOpenTest = { onOpenTest(item.test.id) }
                    )
                }
            }

            // Recent Tests
            item(key = "hdr_recent") {
                SectionHeader(
                    title = "Recent Tests",
                    badgeCount = state.tests.size,
                    badgeColor = BrandOrange,
                    actionLabel = "My Tests",
                    onAction = onNavigateToTests
                )
            }
            items(recentTests, key = { "recent_${it.id}" }) { test ->
                RecentTestMiniCard(test = test, onClick = { onOpenTest(test.id) })
            }
        }

        item(key = "bottom_spacer") {
            Spacer(modifier = Modifier.height(72.dp))
        }
    }
}

@Composable
private fun RecentTestMiniCard(
    test: TestRecord,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
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
                    fontWeight = FontWeight.Bold
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
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (test.marksScored != null && test.totalMarks != null) {
                    Text(
                        text = "Score: ${test.marksScored}/${test.totalMarks} (${test.scorePercent ?: 0}%)",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                test.accuracyPercent?.let { acc ->
                    Text(
                        text = "Accuracy: $acc%",
                        style = MaterialTheme.typography.labelMedium,
                        color = StatusGreen
                    )
                }
            }
        }
    }
}
