package com.example.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Healing
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Healing
import androidx.compose.material.icons.outlined.Radar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.ui.screens.AuditDetailScreen
import com.example.ui.screens.BuildArchitectScreen
import com.example.ui.screens.ConsoleHomeScreen
import com.example.ui.screens.PatchPlaybookScreen
import com.example.ui.screens.UserGuideScreen
import com.example.ui.theme.CyanMuted
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldPass
import com.example.ui.theme.GunmetalSurface
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.SteelElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TitanApp(viewModel: TitanViewModel) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        val activeTab by viewModel.activeTab.collectAsStateWithLifecycle()
        val telemetry by viewModel.hostTelemetry.collectAsStateWithLifecycle()
        val scanState by viewModel.scanState.collectAsStateWithLifecycle()
        val history by viewModel.scanHistory.collectAsStateWithLifecycle()
        val selectedReport by viewModel.selectedReport.collectAsStateWithLifecycle()
        val architectInput by viewModel.architectInput.collectAsStateWithLifecycle()
        val architectOutput by viewModel.architectOutput.collectAsStateWithLifecycle()
        val logInput by viewModel.logInput.collectAsStateWithLifecycle()
        val selectedCategory by viewModel.selectedPatchCategory.collectAsStateWithLifecycle()
        val diagnosedRules by viewModel.diagnosedRules.collectAsStateWithLifecycle()
        val filteredRules by viewModel.filteredPlaybookRules.collectAsStateWithLifecycle()

        val snackbarHostState = remember { SnackbarHostState() }
        val scope = rememberCoroutineScope()
        val notify: (String) -> Unit = { msg ->
            scope.launch {
                snackbarHostState.currentSnackbarData?.dismiss()
                snackbarHostState.showSnackbar(msg)
            }
        }

        val openArchiveLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocument(),
            onResult = { uri ->
                if (uri != null) {
                    viewModel.inspectArchiveUri(uri)
                }
            },
        )

        val report = selectedReport
        if (report != null) {
            AuditDetailScreen(
                report = report,
                onBack = viewModel::closeReport,
                onSendToArchitect = {
                    viewModel.applyArchitectPreset("FROM_SCAN")
                    viewModel.closeReport()
                    viewModel.selectTab(ConsoleTab.ARCHITECT)
                    notify("اطلاعات ${report.fileName} به معمار بیلد منتقل شد")
                },
                onNotify = notify,
            )
            return@CompositionLocalProvider
        }

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(ObsidianBg),
        ) {
            val useRail = maxWidth >= 680.dp

            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = ObsidianBg,
                contentWindowInsets = WindowInsets.safeDrawing,
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                topBar = {
                    Column {
                        TopAppBar(
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = ObsidianBg,
                                titleContentColor = TextPrimary,
                            ),
                            title = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    Image(
                                        painter = painterResource(id = R.drawable.ic_titan_emblem_clean),
                                        contentDescription = "نشان تیتان‌دروید آرشیتکت",
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(9.dp))
                                            .border(1.dp, ElectricCyan.copy(alpha = 0.4f), RoundedCornerShape(9.dp)),
                                    )
                                    Column {
                                        Text(
                                            text = "تیتان‌دروید آرشیتکت",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = TextPrimary,
                                        )
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .clip(CircleShape)
                                                    .background(EmeraldPass),
                                            )
                                            Text(
                                                text = "کنسول مهندسی بیلد و تحلیل آرشیو",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = ElectricCyan,
                                            )
                                        }
                                    }
                                }
                            },
                            actions = {
                                if (activeTab != ConsoleTab.GUIDE) {
                                    Surface(
                                        onClick = { viewModel.selectTab(ConsoleTab.GUIDE) },
                                        shape = RoundedCornerShape(8.dp),
                                        color = SteelElevated,
                                        border = BorderStroke(1.dp, HairlineBorder),
                                        modifier = Modifier
                                            .padding(end = 8.dp)
                                            .testTag("topbar_guide_button"),
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                                contentDescription = "دفترچه راهنما",
                                                tint = ElectricCyan,
                                                modifier = Modifier.size(15.dp),
                                            )
                                            Text(
                                                text = "راهنما",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = TextPrimary,
                                            )
                                        }
                                    }
                                }
                            },
                        )
                        HorizontalDivider(color = HairlineBorder, thickness = 1.dp)
                    }
                },
                bottomBar = {
                    if (!useRail) {
                        Column {
                            HorizontalDivider(color = HairlineBorder, thickness = 1.dp)
                            NavigationBar(
                                containerColor = GunmetalSurface,
                                tonalElevation = 0.dp,
                            ) {
                                ConsoleTab.entries.forEach { tab ->
                                    val selected = activeTab == tab
                                    NavigationBarItem(
                                        selected = selected,
                                        onClick = { viewModel.selectTab(tab) },
                                        icon = {
                                            Icon(
                                                imageVector = tabIcon(tab, selected),
                                                contentDescription = tab.label,
                                            )
                                        },
                                        label = {
                                            Text(
                                                text = tab.label,
                                                style = MaterialTheme.typography.labelSmall,
                                                maxLines = 1,
                                            )
                                        },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = ElectricCyan,
                                            selectedTextColor = ElectricCyan,
                                            indicatorColor = CyanMuted,
                                            unselectedIconColor = TextMuted,
                                            unselectedTextColor = TextSecondary,
                                        ),
                                        modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}"),
                                    )
                                }
                            }
                        }
                    }
                },
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                ) {
                    if (useRail) {
                        NavigationRail(
                            containerColor = GunmetalSurface,
                            modifier = Modifier.fillMaxHeight(),
                        ) {
                            Spacer(modifier = Modifier.size(12.dp))
                            ConsoleTab.entries.forEach { tab ->
                                val selected = activeTab == tab
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = { viewModel.selectTab(tab) },
                                    icon = {
                                        Icon(
                                            imageVector = tabIcon(tab, selected),
                                            contentDescription = tab.label,
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = tab.label,
                                            style = MaterialTheme.typography.labelSmall,
                                        )
                                    },
                                    colors = NavigationRailItemDefaults.colors(
                                        selectedIconColor = ElectricCyan,
                                        selectedTextColor = ElectricCyan,
                                        indicatorColor = CyanMuted,
                                        unselectedIconColor = TextMuted,
                                        unselectedTextColor = TextSecondary,
                                    ),
                                    modifier = Modifier.testTag("rail_tab_${tab.name.lowercase()}"),
                                )
                            }
                        }
                        VerticalDivider(color = HairlineBorder, thickness = 1.dp)
                    }

                    Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                        Crossfade(targetState = activeTab, label = "ConsoleTabCrossfade") { tab ->
                            when (tab) {
                                ConsoleTab.INSPECTOR -> {
                                    ConsoleHomeScreen(
                                        telemetry = telemetry,
                                        scanState = scanState,
                                        history = history,
                                        onPickArchive = {
                                            openArchiveLauncher.launch(
                                                arrayOf(
                                                    "application/zip",
                                                    "application/vnd.android.package-archive",
                                                    "application/octet-stream",
                                                    "*/*",
                                                )
                                            )
                                        },
                                        onInspectSelfApk = viewModel::inspectSelfApk,
                                        onRefreshTelemetry = viewModel::refreshTelemetry,
                                        onDismissScanError = viewModel::dismissScanState,
                                        onOpenReport = viewModel::openReport,
                                        onDeleteReport = viewModel::deleteReport,
                                        onClearHistory = viewModel::clearHistory,
                                        onOpenGuide = { viewModel.selectTab(ConsoleTab.GUIDE) },
                                    )
                                }

                                ConsoleTab.ARCHITECT -> {
                                    BuildArchitectScreen(
                                        input = architectInput,
                                        output = architectOutput,
                                        hasLastScan = history.isNotEmpty(),
                                        onUpdateInput = viewModel::updateArchitectInput,
                                        onApplyPreset = viewModel::applyArchitectPreset,
                                        onCopied = notify,
                                    )
                                }

                                ConsoleTab.PLAYBOOK -> {
                                    PatchPlaybookScreen(
                                        logInput = logInput,
                                        selectedCategory = selectedCategory,
                                        diagnosedRules = diagnosedRules,
                                        rules = filteredRules,
                                        onLogInputChange = viewModel::updateLogInput,
                                        onSelectCategory = viewModel::selectPatchCategory,
                                        onCopied = notify,
                                    )
                                }

                                ConsoleTab.GUIDE -> {
                                    UserGuideScreen(onCopied = notify)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun tabIcon(tab: ConsoleTab, selected: Boolean) = when (tab) {
    ConsoleTab.INSPECTOR -> if (selected) Icons.Filled.Radar else Icons.Outlined.Radar
    ConsoleTab.ARCHITECT -> if (selected) Icons.Filled.Build else Icons.Outlined.Build
    ConsoleTab.PLAYBOOK -> if (selected) Icons.Filled.Healing else Icons.Outlined.Healing
    ConsoleTab.GUIDE -> if (selected) Icons.AutoMirrored.Filled.MenuBook else Icons.AutoMirrored.Outlined.MenuBook
}
