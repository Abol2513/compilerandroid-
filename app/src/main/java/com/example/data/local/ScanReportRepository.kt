package com.example.data.local

import com.example.domain.model.ScanReport
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ScanReportRepository(private val dao: ScanReportDao) {

    val scanHistory: Flow<List<ScanReport>> = dao.observeAll().map { entities ->
        entities.map(ScanReportMapper::toDomain)
    }

    suspend fun saveReport(report: ScanReport): ScanReport {
        val insertedId = dao.insert(ScanReportMapper.toEntity(report))
        return report.copy(id = insertedId)
    }

    suspend fun deleteReport(id: Long) {
        dao.deleteById(id)
    }

    suspend fun clearHistory() {
        dao.clearAll()
    }
}
