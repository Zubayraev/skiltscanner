package com.yourpackage.skiltscanner.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanHistoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(scanHistory: ScanHistory)

    @Query("SELECT * FROM scan_history ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<ScanHistory>>

    @Query("SELECT * FROM scan_history WHERE licensePlate = :plate LIMIT 1")
    suspend fun getByLicensePlate(plate: String): ScanHistory?

    @Delete
    suspend fun delete(scanHistory: ScanHistory)

    @Query("DELETE FROM scan_history")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM scan_history")
    suspend fun getCount(): Int
}