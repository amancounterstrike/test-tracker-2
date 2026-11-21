package com.testtracker.gate.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Assessment
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.testtracker.gate.model.ERROR_CATEGORIES
import com.testtracker.gate.model.parseLocalDate
import com.testtracker.gate.model.subjectColorFor
import com.testtracker.gate.ui.components.EmptyStateCard
import com.testtracker.gate.ui.components.MetricCard
import com.testtracker.gate.ui.theme.BrandOrange
import com.testtracker.gate.ui.theme.StatusAmber
import com.testtracker.gate.ui.theme.StatusBlue
import com.testtracker.gate.ui.theme.StatusGreen
import com.testtracker.gate.ui.theme.StatusRed
import com.testtracker.gate.viewmodel.TrackerUiState
import java.time.LocalDate
import kotlin.math.roundToInt

@Composable
fun AnalyticsScreen(
    state: TrackerUiState,
    modifier: Modifier = Modifier
) {
    val chronologicalTests = remember(state.tests) { state.tests.sortedBy { it.testDate } }
    val scorePoints = remember(chronologicalTests) {
        chronologicalTests.mapNotNull { t -> t.scorePercent?.let { t.testNumber to it.toFloat() } }
    }
    val accuracyPoints = remember(chronologicalTests) {
        chronologicalTests.mapNotNull { t -> t.accuracyPercent?.let { t.testNumber to it.toFloat() } }
    }
    val subjectAvgScores = remember(state.tests) {
        state.tests.groupBy { it.subject }
            .mapNotNull { (subj, list) ->
                val scores = list.mapNotNull { it.scorePercent }
                if (scores.isEmpty()) null else Triple(subj, scores.average().roundToInt(), subjectColorFor(subj))
            }
            .sortedByDescending { it.second }
    }
    val errorDistribution = remember(state.questions) {
        ERROR_CATEGORIES.map { cat ->
            Triple(cat.label, state.questions.count { it.errorCode.equals(cat.code, ignoreCase = true) }, cat.color)
        }
    }
    val weeklyActivity = remember(state.tests, state.revisions) {
        val today = LocalDate.now()
        (6 downTo 0).map { daysBack ->
            val d = today.minusDays(daysBack.toLong())
            val iso = d.toString()
            val label = d.dayOfWeek.name.take(3)
            val testsOnDay = state.tests.count { it.testDate == iso }
            val revsDoneOnDay = state.revisions.count { it.isCompleted && it.completedDate == iso }
            label to (testsOnDay + revsDoneOnDay).toFloat()
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 4 KPI Cards
        item(key = "kpi_row_1") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricCard(
                    title = "Avg Score",
                    value = state.averageScorePercent?.let { "$it%" } ?: "—",
                    subtitle = "${scorePoints.size} scored tests",
                    accentColor = BrandOrange,
                    icon = Icons.Rounded.TrendingUp,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Avg Accuracy",
                    value = state.averageAccuracyPercent?.let { "$it%" } ?: "—",
                    subtitle = "${accuracyPoints.size} tracked tests",
                    accentColor = StatusGreen,
                    icon = Icons.Rounded.Assessment,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item(key = "kpi_row_2") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricCard(
                    title = "Revision Rate",
                    value = "${state.revisionCompletionRate}%",
                    subtitle = "${state.completedRevisions.size} of ${state.trackableRevisions.size} done",
                    accentColor = StatusBlue,
                    icon = Icons.Rounded.CheckCircle,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Logged Errors",
                    value = state.questions.size.toString(),
                    subtitle = "${state.repeatedQuestionNumbers.size} repeated Qs",
                    accentColor = StatusRed,
                    icon = Icons.Rounded.BugReport,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        if (state.tests.isEmpty()) {
            item(key = "analytics_empty") {
                EmptyStateCard(
                    title = "No analytics data yet",
                    subtitle = "Add tests or load sample GATE CE tests from Settings to view all 6 native performance charts."
                )
            }
        } else {
            // Chart 1: Score Trend (%)
            item(key = "chart_score_trend") {
                ChartCard(title = "1. Score Trend (%)", subtitle = "Chronological percentage score across tests") {
                    NativeLineChart(points = scorePoints, lineColor = BrandOrange, maxValue = 100f)
                }
            }

            // Chart 2: Accuracy Trend (%)
            item(key = "chart_acc_trend") {
                ChartCard(title = "2. Accuracy Trend (%)", subtitle = "Correct / (Correct + Wrong + Skipped)") {
                    NativeLineChart(points = accuracyPoints, lineColor = StatusGreen, maxValue = 100f)
                }
            }

            // Chart 3: Subject-wise Average Score
            item(key = "chart_subject_scores") {
                ChartCard(title = "3. Subject-wise Average Score", subtitle = "Average percentage score by GATE subject") {
                    if (subjectAvgScores.isEmpty()) {
                        NoChartDataText()
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            subjectAvgScores.forEach { (subj, avg, color) ->
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = subj,
                                            style = MaterialTheme.typography.labelMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            text = "$avg%",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = color
                                        )
                                    }
                                    Canvas(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(10.dp)
                                    ) {
                                        drawRoundRect(
                                            color = color.copy(alpha = 0.16f),
                                            cornerRadius = CornerRadius(12f, 12f),
                                            size = size
                                        )
                                        drawRoundRect(
                                            color = color,
                                            cornerRadius = CornerRadius(12f, 12f),
                                            size = Size(size.width * (avg.coerceIn(0, 100) / 100f), size.height)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Chart 4: Error Type Distribution (Donut Chart)
            item(key = "chart_errors") {
                ChartCard(title = "4. Error Type Distribution", subtitle = "Breakdown across C, F, A, E, and T mistake codes") {
                    val total = errorDistribution.sumOf { it.second }
                    if (total == 0) {
                        NoChartDataText("Log questions in Question Bank to see error breakdown.")
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Canvas(modifier = Modifier.size(120.dp)) {
                                var startAngle = -90f
                                val strokeWidth = 22.dp.toPx()
                                errorDistribution.forEach { (_, count, color) ->
                                    if (count > 0) {
                                        val sweep = (count.toFloat() / total.toFloat()) * 360f
                                        drawArc(
                                            color = color,
                                            startAngle = startAngle,
                                            sweepAngle = sweep,
                                            useCenter = false,
                                            style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                                        )
                                        startAngle += sweep
                                    }
                                }
                            }
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                errorDistribution.forEach { (label, count, color) ->
                                    val pct = if (total > 0) ((count * 100f) / total).roundToInt() else 0
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(9.dp)
                                                    .clip(CircleShape)
                                                    .background(color)
                                            )
                                            Text(
                                                text = label,
                                                style = MaterialTheme.typography.labelSmall,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Text(
                                            text = "$count ($pct%)",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Chart 5: Revision Status Breakdown (Ring + Counts)
            item(key = "chart_revisions") {
                ChartCard(title = "5. Revision Status Breakdown", subtitle = "Completed, Today, Overdue, and Upcoming R1–R7 stages") {
                    val slices = listOf(
                        Triple("Completed", state.completedRevisions.size, StatusGreen),
                        Triple("Due Today", state.dueTodayRevisions.size, StatusAmber),
                        Triple("Overdue", state.overdueRevisions.size, StatusRed),
                        Triple("Upcoming", state.upcomingRevisions.size, StatusBlue)
                    )
                    val total = slices.sumOf { it.second }.coerceAtLeast(1)
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(16.dp)
                                .clip(RoundedCornerShape(50))
                        ) {
                            var x = 0f
                            slices.forEach { (_, count, color) ->
                                val w = size.width * (count.toFloat() / total.toFloat())
                                if (w > 0f) {
                                    drawRect(color = color, topLeft = Offset(x, 0f), size = Size(w, size.height))
                                    x += w
                                }
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            slices.forEach { (label, count, color) ->
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = count.toString(),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = color
                                    )
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Chart 6: 7-Day Study Activity Bar Chart
            item(key = "chart_weekly") {
                ChartCard(title = "6. 7-Day Activity", subtitle = "Tests taken & revisions completed over the last 7 days") {
                    val maxVal = weeklyActivity.maxOfOrNull { it.second }?.coerceAtLeast(1f) ?: 1f
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        weeklyActivity.forEach { (dayLabel, value) ->
                            val fraction = (value / maxVal).coerceIn(0f, 1f)
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Bottom,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = value.roundToInt().toString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Box(
                                    modifier = Modifier
                                        .height((80f * fraction).coerceAtLeast(6f).dp)
                                        .fillMaxWidth(0.5f)
                                        .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                                        .background(if (value > 0f) BrandOrange else MaterialTheme.colorScheme.surfaceVariant)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = dayLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }

        item(key = "analytics_spacer") { Spacer(modifier = Modifier.height(72.dp)) }
    }
}

@Composable
private fun ChartCard(
    title: String,
    subtitle: String,
    content: @Composable () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            content()
        }
    }
}

@Composable
private fun NativeLineChart(
    points: List<Pair<String, Float>>,
    lineColor: Color,
    maxValue: Float
) {
    if (points.isEmpty()) {
        NoChartDataText()
        return
    }
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(145.dp)
        ) {
            val padX = 18f
            val padY = 14f
            val w = size.width - padX * 2
            val h = size.height - padY * 2

            // Horizontal guide lines
            listOf(0f, 0.5f, 1f).forEach { frac ->
                val y = padY + h * (1f - frac)
                drawLine(
                    color = gridColor,
                    start = Offset(padX, y),
                    end = Offset(size.width - padX, y),
                    strokeWidth = 1.5f
                )
            }

            val coords = points.mapIndexed { idx, (_, v) ->
                val x = if (points.size == 1) padX + w / 2f else padX + (w * idx / (points.size - 1))
                val y = padY + h * (1f - (v / maxValue).coerceIn(0f, 1f))
                Offset(x, y)
            }

            if (coords.size > 1) {
                val path = Path().apply {
                    moveTo(coords.first().x, coords.first().y)
                    coords.drop(1).forEach { lineTo(it.x, it.y) }
                }
                drawPath(
                    path = path,
                    color = lineColor,
                    style = Stroke(width = 6f, cap = StrokeCap.Round)
                )
            }

            coords.forEach { pt ->
                drawCircle(color = Color.White, radius = 9f, center = pt)
                drawCircle(color = lineColor, radius = 6f, center = pt)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            points.take(6).forEach { (label, v) ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${v.roundToInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = lineColor
                    )
                    Text(
                        text = label.take(8),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun NoChartDataText(msg: String = "Add scored tests to populate this chart.") {
    Text(
        text = msg,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}
