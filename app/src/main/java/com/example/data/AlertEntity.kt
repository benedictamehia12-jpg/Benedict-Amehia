package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "alerts")
data class AlertEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val severity: String, // "critical" (for BLOCKED) or "warning" (for FLAGGED)
    val message: String,
    val transactionId: String,
    val timestamp: Long
)
