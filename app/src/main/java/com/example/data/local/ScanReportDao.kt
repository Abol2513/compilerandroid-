package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanReportDao {
    @Query("SELECT * FROM scan_reports ORDER BY scannedAtEpochMs DESC")
    fun observeAll(): Flow<List<ScanReportEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ScanReportEntity): Long

    @Query("DELETE FROM scan_reports WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM scan_reports")
    suspend fun clearAll()
}
