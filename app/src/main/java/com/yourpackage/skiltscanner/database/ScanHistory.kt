package com.yourpackage.skiltscanner.database
// dette er filen for entitetene som innebærer historikken (databasen)
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scan_history")
data class ScanHistory(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val licensePlate: String,
    val merke: String,
    val modell: String,
    val aarsmodell: String,
    val euKontroll: String,
    val timestamp: Long = System.currentTimeMillis()
)