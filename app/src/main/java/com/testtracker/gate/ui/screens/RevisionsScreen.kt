package com.testtracker.gate.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.testtracker.gate.model.RevisionStatus
import com.testtracker.gate.model.RevisionWithTest
import com.testtracker.gate.ui.components.EmptyStateCard
import com.testtracker.gate.ui.components.RevisionRowCard
import com.testtracker.gate.viewmodel.TrackerUiState

@Composable
fun RevisionsScreen(
    state: TrackerUiState,
    onOpenTest: (String) -> Unit,
    onQuickComplete: (RevisionWithTest) -> Unit,
    onDetailedComplete: (RevisionWithTest) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedFilter by remember { mutableStateOf("All") }
    var searchQuery by remember { mutableStateOf("") }

    val filtered = remember(state.trackableRevisions, selectedFilter, searchQuery) {
        val byStatus = when (selectedFilter) {
            "Today" -> state.trackableRevisions.filter { it.status == RevisionStatus.TODAY }
            "Overdue" -> state.trackableRevisions.filter { it.status == RevisionStatus.OVERDUE }
            "Upcoming" -> state.trackableRevisions.filter { it.status == RevisionStatus.UPCOMING }
            "Completed" -> state.trackableRevisions.filter { it.status == RevisionStatus.COMPLETED }
            else -> state.trackableRevisions
        }
        val q = searchQuery.trim().lowercase()
        if (q.isEmpty()) byStatus
        else byStatus.filter {
            it.test.testNumber.lowercase().contains(q) ||
                it.test.subject.lowercase().contains(q) ||
                it.test.topic.lowercase().contains(q) ||
                it.revision.stageCode.lowercase().contains(q)
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item(key = "rev_search") {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search by test, subject, or stage (R1–R7)") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )
        }

        item(key = "rev_filters") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val counts = listOf(
                    "All" to state.trackableRevisions.size,
                    "Today" to state.dueTodayRevisions.size,
                    "Overdue" to state.overdueRevisions.size,
                    "Upcoming" to state.upcomingRevisions.size,
                    "Completed" to state.completedRevisions.size
                )
                counts.forEach { (label, count) ->
                    FilterChip(
                        selected = selectedFilter == label,
                        onClick = { selectedFilter = label },
                        label = { Text("$label ($count)") }
                    )
                }
            }
        }

        if (filtered.isEmpty()) {
            item(key = "rev_empty") {
                EmptyStateCard(
                    title = "No revisions in this view",
                    subtitle = "Try another filter or add a new test to generate its R1–R7 revision schedule."
                )
            }
        } else {
            items(filtered, key = { it.revision.id }) { item ->
                RevisionRowCard(
                    item = item,
                    onQuickDone = { onQuickComplete(item) },
                    onDetailedComplete = { onDetailedComplete(item) },
                    onOpenTest = { onOpenTest(item.test.id) }
                )
            }
        }

        item(key = "rev_spacer") { Spacer(modifier = Modifier.height(72.dp)) }
    }
}
