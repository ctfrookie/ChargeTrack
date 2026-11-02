package com.example

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tracker.ChargingTracker
import com.example.ui.components.ExportDialog
import com.example.ui.components.ManualSimulationSheet
import com.example.ui.components.SessionFinishedDialog
import com.example.ui.components.SettingsDialog
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.LiveMonitorScreen
import com.example.ui.screens.StatsScreen
import com.example.ui.theme.ElectricEmerald
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.ChargingViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                ChargeTimerApp()
            }
        }
    }
}

@Composable
fun ChargeTimerApp(
    viewModel: ChargingViewModel = viewModel()
) {
    val context = LocalContext.current
    val tracker = remember { ChargingTracker.getInstance(context) }

    var currentTab by remember { mutableIntStateOf(0) }
    var showSimulationSheet by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showVivoKeepAliveDialog by remember { mutableStateOf(false) }

    val isCharging by viewModel.isCharging.collectAsState()
    val activeSession by viewModel.activeSession.collectAsState()
    val batteryLevel by viewModel.batteryLevel.collectAsState()
    val batteryStatus by viewModel.batteryStatus.collectAsState()
    val plugType by viewModel.plugType.collectAsState()
    val voltageMv by viewModel.voltageMv.collectAsState()
    val currentMa by viewModel.currentMa.collectAsState()
    val powerWatts by viewModel.powerWatts.collectAsState()
    val temperatureC by viewModel.temperatureC.collectAsState()
    val health by viewModel.health.collectAsState()
    val allSessions by viewModel.allSessions.collectAsState()
    val lastCompletedSession by viewModel.lastCompletedSession.collectAsState()
    val settings by viewModel.settings.collectAsState()

    // Handle back button when on sub-screens
    BackHandler(enabled = currentTab != 0) {
        currentTab = 0
    }

    // Request notification permission on Android 13+
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    // Register dynamic battery changed receiver while UI is alive
    DisposableEffect(context) {
        val batteryReceiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context, intent: Intent) {
                if (intent.action == Intent.ACTION_BATTERY_CHANGED) {
                    tracker.handleBatteryChangedIntent(intent)
                }
            }
        }
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        context.registerReceiver(batteryReceiver, filter)

        onDispose {
            try {
                context.unregisterReceiver(batteryReceiver)
            } catch (_: Exception) {}
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .testTag("bottom_nav_bar")
                    .windowInsetsPadding(WindowInsets.navigationBars),
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                NavigationBarItem(
                    selected = currentTab == 0,
                    onClick = { currentTab = 0 },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == 0) Icons.Filled.Bolt else Icons.Outlined.Bolt,
                            contentDescription = stringResource(R.string.tab_monitor)
                        )
                    },
                    label = { Text(stringResource(R.string.tab_monitor)) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = ElectricEmerald,
                        selectedTextColor = ElectricEmerald,
                        indicatorColor = ElectricEmerald.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier.testTag("nav_item_monitor")
                )
                NavigationBarItem(
                    selected = currentTab == 1,
                    onClick = { currentTab = 1 },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == 1) Icons.Filled.History else Icons.Outlined.History,
                            contentDescription = stringResource(R.string.tab_history)
                        )
                    },
                    label = { Text(stringResource(R.string.tab_history)) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = ElectricEmerald,
                        selectedTextColor = ElectricEmerald,
                        indicatorColor = ElectricEmerald.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier.testTag("nav_item_history")
                )
                NavigationBarItem(
                    selected = currentTab == 2,
                    onClick = { currentTab = 2 },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == 2) Icons.Filled.BarChart else Icons.Outlined.BarChart,
                            contentDescription = stringResource(R.string.tab_stats)
                        )
                    },
                    label = { Text(stringResource(R.string.tab_stats)) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = ElectricEmerald,
                        selectedTextColor = ElectricEmerald,
                        indicatorColor = ElectricEmerald.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier.testTag("nav_item_stats")
                )
            }
        }
    ) { innerPadding ->
        Crossfade(
            targetState = currentTab,
            label = "tab_crossfade",
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) { tabIndex ->
            when (tabIndex) {
                0 -> LiveMonitorScreen(
                    isCharging = isCharging,
                    activeSession = activeSession,
                    batteryLevel = batteryLevel,
                    batteryStatus = batteryStatus,
                    plugType = plugType,
                    voltageMv = voltageMv,
                    currentMa = currentMa,
                    powerWatts = powerWatts,
                    temperatureC = temperatureC,
                    health = health,
                    settings = settings,
                    onOpenSettings = { showSettingsDialog = true },
                    onOpenSimulation = { showSimulationSheet = true },
                    onStopCharging = { viewModel.simulateDisconnect() },
                    onOpenKeepAliveGuide = { showVivoKeepAliveDialog = true }
                )
                1 -> HistoryScreen(
                    sessions = allSessions,
                    onDeleteSession = { viewModel.deleteSession(it) },
                    onUpdateNote = { id, note -> viewModel.updateSessionNote(id, note) },
                    onClearAll = { viewModel.deleteAllSessions() },
                    onOpenExport = { showExportDialog = true },
                    onSeedSampleData = { viewModel.seedSampleData() }
                )
                2 -> StatsScreen(
                    sessions = allSessions,
                    onOpenExport = { showExportDialog = true }
                )
            }
        }
    }

    // Settings Modal Sheet
    if (showSettingsDialog) {
        SettingsDialog(
            settings = settings,
            onDismiss = { showSettingsDialog = false },
            onUpdateTargetLevel = { viewModel.setTargetBatteryLevel(it) },
            onUpdateStopTimerAtTarget = { viewModel.setStopTimerAtTarget(it) },
            onUpdateWebhookUrl = { viewModel.setWebhookUrl(it) },
            onUpdateWebhookEnabled = { viewModel.setWebhookEnabled(it) },
            onUpdateNotifyOnStart = { viewModel.setNotifyOnStart(it) },
            onUpdateNotifyOnTarget = { viewModel.setNotifyOnTarget(it) },
            onUpdateNotifyOnFinish = { viewModel.setNotifyOnFinish(it) },
            onUpdateVibrateOnTarget = { viewModel.setVibrateOnTarget(it) },
            onTestWebhook = { url, callback ->
                viewModel.testWebhook(url, callback)
            },
            onOpenVivoKeepAliveGuide = {
                showSettingsDialog = false
                showVivoKeepAliveDialog = true
            }
        )
    }

    // Vivo Keep Alive Guide Modal Sheet
    if (showVivoKeepAliveDialog) {
        com.example.ui.components.VivoKeepAliveDialog(
            onDismiss = { showVivoKeepAliveDialog = false }
        )
    }

    // Export Modal Sheet
    if (showExportDialog) {
        ExportDialog(
            recordCount = allSessions.size,
            onDismiss = { showExportDialog = false },
            onExportCsv = { viewModel.exportCsv(context) },
            onExportJson = { viewModel.exportJson(context) },
            onCopySummary = { viewModel.copySummaryReport(context) }
        )
    }

    // Simulation Modal Sheet
    if (showSimulationSheet) {
        ManualSimulationSheet(
            isCharging = isCharging,
            currentBatteryLevel = batteryLevel,
            onDismiss = { showSimulationSheet = false },
            onSimulateConnect = { startPercent, plug ->
                viewModel.simulateConnect(startPercent, plug)
            },
            onSimulateAdvanceBattery = { newPercent ->
                viewModel.simulateAdvanceBattery(newPercent)
            },
            onSimulateDisconnect = { endPercent ->
                viewModel.simulateDisconnect(endPercent)
            },
            onSeedSampleData = {
                viewModel.seedSampleData()
            }
        )
    }

    // Celebration & Summary Dialog when charging completes
    lastCompletedSession?.let { completed ->
        SessionFinishedDialog(
            session = completed,
            onDismiss = { viewModel.dismissLastCompleted() },
            onSaveNote = { note ->
                viewModel.updateSessionNote(completed.id, note)
            }
        )
    }
}
