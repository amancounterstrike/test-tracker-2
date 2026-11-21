package com.testtracker.gate

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Assignment
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Quiz
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.testtracker.gate.model.RevisionWithTest
import com.testtracker.gate.model.TestRecord
import com.testtracker.gate.ui.components.CompleteRevisionDialog
import com.testtracker.gate.ui.screens.AddEditTestBottomSheet
import com.testtracker.gate.ui.screens.AddQuestionBottomSheet
import com.testtracker.gate.ui.screens.AnalyticsScreen
import com.testtracker.gate.ui.screens.CalendarScreen
import com.testtracker.gate.ui.screens.DashboardScreen
import com.testtracker.gate.ui.screens.GlobalSearchDialog
import com.testtracker.gate.ui.screens.QuestionBankScreen
import com.testtracker.gate.ui.screens.RevisionsScreen
import com.testtracker.gate.ui.screens.SettingsScreen
import com.testtracker.gate.ui.screens.SubjectsScreen
import com.testtracker.gate.ui.screens.TestDetailScreen
import com.testtracker.gate.ui.screens.TestsScreen
import com.testtracker.gate.ui.theme.TestTrackerTheme
import com.testtracker.gate.viewmodel.TrackerViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        configure120HzDisplay()

        setContent {
            val vm: TrackerViewModel = viewModel()
            TestTrackerRoot(vm = vm)
        }
    }

    override fun onResume() {
        super.onResume()
        configure120HzDisplay()
    }

    /**
     * Locks the window to the highest available display refresh rate (120Hz+)
     * on Android 15 (API 35) and earlier by setting both preferredDisplayModeId
     * and preferredRefreshRate so Adaptive Refresh Rate stays at peak FPS.
     */
    private fun configure120HzDisplay() {
        try {
            val currentDisplay = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                display
            } else {
                @Suppress("DEPRECATION")
                windowManager.defaultDisplay
            } ?: return

            val currentMode = currentDisplay.mode
            val highestMode = currentDisplay.supportedModes
                .filter {
                    it.physicalWidth == currentMode.physicalWidth &&
                        it.physicalHeight == currentMode.physicalHeight
                }
                .maxByOrNull { it.refreshRate }

            if (highestMode != null) {
                val params = window.attributes
                params.preferredDisplayModeId = highestMode.modeId
                params.preferredRefreshRate = highestMode.refreshRate
                window.attributes = params
            }
        } catch (_: Exception) {
            // Safely fall back on devices without multi-mode display support
        }
    }
}

private enum class AppDestination(val label: String, val icon: ImageVector) {
    DASHBOARD("Home", Icons.Rounded.Dashboard),
    TESTS("Tests", Icons.Rounded.Assignment),
    REVISIONS("Revisions", Icons.Rounded.Repeat),
    QUESTIONS("Mistakes", Icons.Rounded.Quiz),
    SUBJECTS("Subjects", Icons.Rounded.MenuBook),
    ANALYTICS("Analytics", Icons.Rounded.BarChart),
    CALENDAR("Calendar", Icons.Rounded.CalendarMonth),
    SETTINGS("Settings", Icons.Rounded.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TestTrackerRoot(vm: TrackerViewModel) {
    val state by vm.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var currentDestination by remember { mutableStateOf(AppDestination.DASHBOARD) }
    var selectedTestId by remember { mutableStateOf<String?>(null) }
    var showAddEditTestSheet by remember { mutableStateOf(false) }
    var editingTest by remember { mutableStateOf<TestRecord?>(null) }
    var showAddQuestionSheet by remember { mutableStateOf(false) }
    var preselectedQuestionTestId by remember { mutableStateOf<String?>(null) }
    var completingRevision by remember { mutableStateOf<RevisionWithTest?>(null) }
    var showMoreSheet by remember { mutableStateOf(false) }
    var showGlobalSearch by remember { mutableStateOf(false) }

    LaunchedEffect(state.snackbarMessage) {
        val msg = state.snackbarMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            vm.clearSnackbar()
        }
    }

    TestTrackerTheme(darkTheme = state.settings.darkMode) {
        val detailTest = selectedTestId?.let { state.testMap[it] }

        if (detailTest != null) {
            val detailRevisions = remember(state.revisions, detailTest.id) {
                state.revisions.filter { it.testId == detailTest.id }
            }
            val detailQuestions = remember(state.questions, detailTest.id) {
                state.questions.filter { it.testId == detailTest.id }
            }
            Scaffold(
                snackbarHost = { SnackbarHost(snackbarHostState) }
            ) { innerPadding ->
                Box(modifier = Modifier.padding(innerPadding)) {
                    TestDetailScreen(
                        test = detailTest,
                        revisions = detailRevisions,
                        questions = detailQuestions,
                        onBack = { selectedTestId = null },
                        onEditTest = {
                            editingTest = detailTest
                            showAddEditTestSheet = true
                        },
                        onDeleteTest = {
                            vm.deleteTest(detailTest)
                            selectedTestId = null
                        },
                        onCompleteRevision = { completingRevision = it },
                        onQuickToggleRevision = { vm.quickCompleteRevision(it) },
                        onAddQuestionClick = {
                            preselectedQuestionTestId = detailTest.id
                            showAddQuestionSheet = true
                        },
                        onDeleteQuestion = { vm.deleteQuestion(it) }
                    )
                }
            }
        } else {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.app_logo),
                                    contentDescription = "Test Tracker",
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                )
                                Text(
                                    text = if (currentDestination == AppDestination.DASHBOARD) "Test Tracker" else currentDestination.label,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        },
                        actions = {
                            IconButton(onClick = { showGlobalSearch = true }) {
                                Icon(Icons.Rounded.Search, contentDescription = "Global Search")
                            }
                            IconButton(onClick = { vm.toggleDarkMode() }) {
                                Icon(
                                    imageVector = if (state.settings.darkMode) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
                                    contentDescription = "Toggle Theme"
                                )
                            }
                        }
                    )
                },
                floatingActionButton = {
                    if (currentDestination != AppDestination.SETTINGS) {
                        ExtendedFloatingActionButton(
                            onClick = {
                                editingTest = null
                                showAddEditTestSheet = true
                            },
                            icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                            text = { Text("Add Test") },
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                bottomBar = {
                    NavigationBar {
                        val primaryTabs = remember {
                            listOf(
                                AppDestination.DASHBOARD,
                                AppDestination.TESTS,
                                AppDestination.REVISIONS,
                                AppDestination.QUESTIONS
                            )
                        }
                        primaryTabs.forEach { dest ->
                            NavigationBarItem(
                                selected = currentDestination == dest,
                                onClick = { currentDestination = dest },
                                icon = { Icon(dest.icon, contentDescription = dest.label) },
                                label = { Text(dest.label) }
                            )
                        }
                        val isMoreSelected = currentDestination == AppDestination.SUBJECTS ||
                            currentDestination == AppDestination.ANALYTICS ||
                            currentDestination == AppDestination.CALENDAR ||
                            currentDestination == AppDestination.SETTINGS
                        NavigationBarItem(
                            selected = isMoreSelected,
                            onClick = { showMoreSheet = true },
                            icon = { Icon(Icons.Rounded.GridView, contentDescription = "More") },
                            label = { Text("More") }
                        )
                    }
                },
                snackbarHost = { SnackbarHost(snackbarHostState) }
            ) { innerPadding ->
                Crossfade(
                    targetState = currentDestination,
                    animationSpec = tween(durationMillis = 120),
                    label = "screen_transition",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) { dest ->
                    when (dest) {
                        AppDestination.DASHBOARD -> DashboardScreen(
                            state = state,
                            onOpenTest = { selectedTestId = it },
                            onQuickComplete = { vm.quickCompleteRevision(it.revision) },
                            onDetailedComplete = { completingRevision = it },
                            onNavigateToRevisions = { currentDestination = AppDestination.REVISIONS },
                            onNavigateToTests = { currentDestination = AppDestination.TESTS },
                            onLoadSampleData = { vm.loadSampleGateData() }
                        )
                        AppDestination.TESTS -> TestsScreen(
                            state = state,
                            onOpenTest = { selectedTestId = it },
                            onAddTestClick = {
                                editingTest = null
                                showAddEditTestSheet = true
                            }
                        )
                        AppDestination.REVISIONS -> RevisionsScreen(
                            state = state,
                            onOpenTest = { selectedTestId = it },
                            onQuickComplete = { vm.quickCompleteRevision(it.revision) },
                            onDetailedComplete = { completingRevision = it }
                        )
                        AppDestination.QUESTIONS -> QuestionBankScreen(
                            state = state,
                            onOpenTest = { selectedTestId = it },
                            onAddQuestionClick = {
                                preselectedQuestionTestId = state.tests.firstOrNull()?.id
                                showAddQuestionSheet = true
                            },
                            onDeleteQuestion = { vm.deleteQuestion(it) }
                        )
                        AppDestination.SUBJECTS -> SubjectsScreen(
                            state = state,
                            onOpenTest = { selectedTestId = it },
                            onQuickComplete = { vm.quickCompleteRevision(it.revision) },
                            onDetailedComplete = { completingRevision = it }
                        )
                        AppDestination.ANALYTICS -> AnalyticsScreen(state = state)
                        AppDestination.CALENDAR -> CalendarScreen(
                            state = state,
                            onOpenTest = { selectedTestId = it },
                            onQuickComplete = { vm.quickCompleteRevision(it.revision) },
                            onDetailedComplete = { completingRevision = it }
                        )
                        AppDestination.SETTINGS -> SettingsScreen(
                            state = state,
                            onToggleDarkMode = { vm.toggleDarkMode() },
                            onExportJson = { vm.exportJsonString() },
                            onExportCsv = { vm.exportCsvString() },
                            onImportJson = { vm.importJsonString(it) },
                            onLoadSampleData = { vm.loadSampleGateData() },
                            onClearAllData = { vm.clearAllData() },
                            onMessage = { vm.showMessage(it) }
                        )
                    }
                }
            }
        }

        if (showMoreSheet) {
            ModalBottomSheet(onDismissRequest = { showMoreSheet = false }) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Workspace Views",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold
                    )
                    val moreItems = remember {
                        listOf(
                            Triple(AppDestination.SUBJECTS, "Subjects", "Spreadsheet & chronological schedule by subject"),
                            Triple(AppDestination.ANALYTICS, "Analytics", "6 performance charts & error breakdown"),
                            Triple(AppDestination.CALENDAR, "Calendar", "Monthly schedule with test & revision dots"),
                            Triple(AppDestination.SETTINGS, "Settings & Backup", "JSON/CSV export, import, sample data & theme")
                        )
                    }
                    moreItems.forEach { (dest, title, desc) ->
                        Surface(
                            onClick = {
                                currentDestination = dest
                                showMoreSheet = false
                            },
                            shape = RoundedCornerShape(14.dp),
                            color = if (currentDestination == dest) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Icon(
                                    imageVector = dest.icon,
                                    contentDescription = title,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Column {
                                    Text(
                                        text = title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = desc,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }

        if (showAddEditTestSheet) {
            AddEditTestBottomSheet(
                existingTest = editingTest,
                existingSubjects = state.distinctSubjects,
                onDismiss = {
                    showAddEditTestSheet = false
                    editingTest = null
                },
                onSave = { saved ->
                    vm.saveTest(
                        test = saved,
                        isEditing = editingTest != null,
                        previousDate = editingTest?.testDate
                    )
                    showAddEditTestSheet = false
                    editingTest = null
                }
            )
        }

        if (showAddQuestionSheet && state.tests.isNotEmpty()) {
            AddQuestionBottomSheet(
                tests = state.tests,
                preselectedTestId = preselectedQuestionTestId,
                onDismiss = { showAddQuestionSheet = false },
                onAddSingle = { testId, qNo, code, topic, notes ->
                    vm.addSingleQuestion(testId, qNo, code, topic, notes)
                },
                onAddBulk = { testId, bulkText, defaultTopic ->
                    vm.addBulkQuestions(testId, bulkText, defaultTopic)
                }
            )
        }

        completingRevision?.let { revItem ->
            CompleteRevisionDialog(
                item = revItem,
                onDismiss = { completingRevision = null },
                onConfirm = { date, difficulty, notes ->
                    vm.completeRevisionDetailed(revItem.revision, date, difficulty, notes)
                    completingRevision = null
                }
            )
        }

        if (showGlobalSearch) {
            GlobalSearchDialog(
                state = state,
                onDismiss = { showGlobalSearch = false },
                onOpenTest = { selectedTestId = it }
            )
        }
    }
}
