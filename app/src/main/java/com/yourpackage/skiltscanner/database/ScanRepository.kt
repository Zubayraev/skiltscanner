package com.yourpackage.skiltscanner.database

import kotlinx.coroutines.flow.Flow

class ScanRepository(private val scanHistoryDao: ScanHistoryDao) {

    val allHistory: Flow<List<ScanHistory>> = scanHistoryDao.getAllHistory()

    suspend fun insert(scanHistory: ScanHistory) {
        scanHistoryDao.insert(scanHistory)
    }

    suspend fun getByLicensePlate(plate: String): ScanHistory? {
        return scanHistoryDao.getByLicensePlate(plate)
    }

    suspend fun delete(scanHistory: ScanHistory) {
        scanHistoryDao.delete(scanHistory)
    }

    suspend fun deleteAll() {
        scanHistoryDao.deleteAll()
    }

    suspend fun getCount(): Int {
        return scanHistoryDao.getCount()
    }
}