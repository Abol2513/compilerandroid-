package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.ScanReportRepository
import com.example.data.local.TitanDatabase
import com.example.domain.engine.HostTelemetryProvider
import com.example.domain.engine.PatchPlaybookEngine
import com.example.domain.engine.ResourceCalculator
import com.example.domain.engine.StreamingArchiveAnalyzer
import com.example.domain.model.BuildArchitectInput
import com.example.domain.model.BuildArchitectOutput
import com.example.domain.model.EcosystemStack
import com.example.domain.model.HostTelemetry
import com.example.domain.model.PatchCategory
import com.example.domain.model.PatchRule
import com.example.domain.model.ScanProgressState
import com.example.domain.model.ScanReport
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class ConsoleTab(val label: String) {
    INSPECTOR("تحلیلگر"),
    ARCHITECT("معمار بیلد"),
    PLAYBOOK("پچ‌یاب"),
    GUIDE("دفترچه راهنما"),
}

class TitanViewModel(
    application: Application,
    private val repository: ScanReportRepository,
    private val telemetryProvider: HostTelemetryProvider,
    private val archiveAnalyzer: StreamingArchiveAnalyzer,
) : AndroidViewModel(application) {

    private val _activeTab = MutableStateFlow(ConsoleTab.INSPECTOR)
    val activeTab: StateFlow<ConsoleTab> = _activeTab.asStateFlow()

    private val _hostTelemetry = MutableStateFlow(telemetryProvider.collect())
    val hostTelemetry: StateFlow<HostTelemetry> = _hostTelemetry.asStateFlow()

    private val _scanState = MutableStateFlow<ScanProgressState>(ScanProgressState.Idle)
    val scanState: StateFlow<ScanProgressState> = _scanState.asStateFlow()

    val scanHistory: StateFlow<List<ScanReport>> = repository.scanHistory
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList(),
        )

    private val _selectedReport = MutableStateFlow<ScanReport?>(null)
    val selectedReport: StateFlow<ScanReport?> = _selectedReport.asStateFlow()

    private val _architectInput = MutableStateFlow(
        BuildArchitectInput(
            archiveSizeGb = 2.5,
            uncompressedSizeGb = 6.0,
            hostRamGb = (_hostTelemetry.value.totalRamMb / 1024.0).coerceAtLeast(4.0),
            cpuCores = _hostTelemetry.value.cpuCores,
            freeDiskGb = _hostTelemetry.value.freeDiskGb.coerceAtLeast(1.0),
            agpMajorMinor = "9.1",
            usesNdkCpp = true,
            hasMultiGbAssets = false,
        )
    )
    val architectInput: StateFlow<BuildArchitectInput> = _architectInput.asStateFlow()

    val architectOutput: StateFlow<BuildArchitectOutput> = _architectInput
        .map(ResourceCalculator::calculate)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ResourceCalculator.calculate(_architectInput.value),
        )

    private val _logInput = MutableStateFlow("")
    val logInput: StateFlow<String> = _logInput.asStateFlow()

    private val _selectedPatchCategory = MutableStateFlow(PatchCategory.ALL)
    val selectedPatchCategory: StateFlow<PatchCategory> = _selectedPatchCategory.asStateFlow()

    val diagnosedRules: StateFlow<List<PatchRule>> = _logInput
        .map(PatchPlaybookEngine::diagnoseLog)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList(),
        )

    val filteredPlaybookRules: StateFlow<List<PatchRule>> = _selectedPatchCategory
        .combine(_logInput) { category, query ->
            val byCategory = if (category == PatchCategory.ALL) {
                PatchPlaybookEngine.allRules
            } else {
                PatchPlaybookEngine.allRules.filter { it.category == category }
            }
            if (query.isBlank()) {
                byCategory
            } else {
                val matchedIds = PatchPlaybookEngine.diagnoseLog(query).map { it.id }.toSet()
                if (matchedIds.isNotEmpty()) {
                    byCategory.sortedByDescending { it.id in matchedIds }
                } else {
                    byCategory
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = PatchPlaybookEngine.allRules,
        )

    fun selectTab(tab: ConsoleTab) {
        _activeTab.value = tab
    }

    fun refreshTelemetry() {
        _hostTelemetry.value = telemetryProvider.collect()
    }

    fun inspectArchiveUri(uri: Uri) {
        if (_scanState.value is ScanProgressState.Scanning) return
        viewModelScope.launch {
            _scanState.value = ScanProgressState.Scanning(
                fileName = uri.lastPathSegment?.substringAfterLast('/') ?: "آرشیو انتخاب‌شده",
                phaseLabel = "باز کردن جریان امن فایل (SAF)",
                entriesProcessed = 0,
                bytesProcessed = 0L,
                totalBytesHint = 0L,
            )
            val result = archiveAnalyzer.analyzeUri(uri) { phase, entries, bytes, totalHint ->
                _scanState.value = ScanProgressState.Scanning(
                    fileName = uri.lastPathSegment?.substringAfterLast('/') ?: "آرشیو انتخاب‌شده",
                    phaseLabel = phase,
                    entriesProcessed = entries,
                    bytesProcessed = bytes,
                    totalBytesHint = totalHint,
                )
            }
            handleScanResult(result)
        }
    }

    fun inspectSelfApk() {
        if (_scanState.value is ScanProgressState.Scanning) return
        viewModelScope.launch {
            _scanState.value = ScanProgressState.Scanning(
                fileName = "فایل نصبی تیتان‌دروید (Self-APK)",
                phaseLabel = "خواندن جریان باینری بسته",
                entriesProcessed = 0,
                bytesProcessed = 0L,
                totalBytesHint = 0L,
            )
            val result = archiveAnalyzer.analyzeSelfApk { phase, entries, bytes, totalHint ->
                _scanState.value = ScanProgressState.Scanning(
                    fileName = "فایل نصبی تیتان‌دروید (Self-APK)",
                    phaseLabel = phase,
                    entriesProcessed = entries,
                    bytesProcessed = bytes,
                    totalBytesHint = totalHint,
                )
            }
            handleScanResult(result)
        }
    }

    private suspend fun handleScanResult(result: Result<ScanReport>) {
        result.fold(
            onSuccess = { report ->
                val saved = repository.saveReport(report)
                _scanState.value = ScanProgressState.Success(saved)
                _selectedReport.value = saved
            },
            onFailure = { error ->
                _scanState.value = ScanProgressState.Error(
                    error.message ?: "بازرسی فایل انتخاب‌شده با خطا متوقف شد."
                )
            },
        )
    }

    fun dismissScanState() {
        _scanState.value = ScanProgressState.Idle
    }

    fun openReport(report: ScanReport) {
        _selectedReport.value = report
    }

    fun closeReport() {
        _selectedReport.value = null
        if (_scanState.value is ScanProgressState.Success) {
            _scanState.value = ScanProgressState.Idle
        }
    }

    fun deleteReport(id: Long) {
        viewModelScope.launch {
            if (_selectedReport.value?.id == id) {
                _selectedReport.value = null
            }
            repository.deleteReport(id)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            _selectedReport.value = null
            repository.clearHistory()
        }
    }

    fun updateArchitectInput(transform: (BuildArchitectInput) -> BuildArchitectInput) {
        _architectInput.value = transform(_architectInput.value)
    }

    fun applyArchitectPreset(preset: String) {
        val tel = _hostTelemetry.value
        when (preset) {
            "DEVICE" -> {
                _architectInput.value = BuildArchitectInput(
                    archiveSizeGb = 1.5,
                    uncompressedSizeGb = 3.2,
                    hostRamGb = (tel.totalRamMb / 1024.0).coerceAtLeast(2.0),
                    cpuCores = tel.cpuCores,
                    freeDiskGb = tel.freeDiskGb.coerceAtLeast(1.0),
                    agpMajorMinor = "9.1",
                    usesNdkCpp = true,
                    hasMultiGbAssets = false,
                )
            }
            "CI_16GB" -> {
                _architectInput.value = BuildArchitectInput(
                    archiveSizeGb = 2.5,
                    uncompressedSizeGb = 6.0,
                    hostRamGb = 16.0,
                    cpuCores = 8,
                    freeDiskGb = 64.0,
                    agpMajorMinor = "8.7",
                    usesNdkCpp = true,
                    hasMultiGbAssets = false,
                )
            }
            "MEGA_10GB" -> {
                _architectInput.value = BuildArchitectInput(
                    archiveSizeGb = 10.0,
                    uncompressedSizeGb = 22.0,
                    hostRamGb = 32.0,
                    cpuCores = 16,
                    freeDiskGb = 80.0,
                    agpMajorMinor = "9.1",
                    usesNdkCpp = true,
                    hasMultiGbAssets = true,
                )
            }
            "FROM_SCAN" -> {
                val report = _selectedReport.value ?: scanHistory.value.firstOrNull() ?: return
                val bytesPerGb = 1024.0 * 1024.0 * 1024.0
                val compGb = (report.compressedBytes / bytesPerGb).coerceAtLeast(0.1)
                val uncompGb = (report.uncompressedBytes / bytesPerGb).coerceAtLeast(compGb)
                _architectInput.value = _architectInput.value.copy(
                    archiveSizeGb = compGb,
                    uncompressedSizeGb = uncompGb,
                    agpMajorMinor = report.toolchain.agpVersion ?: _architectInput.value.agpMajorMinor,
                    usesNdkCpp = report.elfResults.isNotEmpty() ||
                        report.detectedStacks.contains(EcosystemStack.NDK_CPP_RUST_GO) ||
                        report.detectedStacks.contains(EcosystemStack.UNITY_EXPORTED),
                    hasMultiGbAssets = uncompGb >= 1.8 || report.largeAssetsNeedingNoCompress.isNotEmpty(),
                )
            }
        }
    }

    fun updateLogInput(text: String) {
        _logInput.value = text
    }

    fun selectPatchCategory(category: PatchCategory) {
        _selectedPatchCategory.value = category
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val db = TitanDatabase.getInstance(application)
            val repo = ScanReportRepository(db.scanReportDao())
            val telemetry = HostTelemetryProvider(application)
            val analyzer = StreamingArchiveAnalyzer(application)
            return TitanViewModel(application, repo, telemetry, analyzer) as T
        }
    }
}
