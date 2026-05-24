package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey val id: String, // "TXN-XXXXXX"
    val amount: Double,
    val score: Double,
    val status: String, // "APPROVED", "FLAGGED", "BLOCKED"
    val device: String,
    val location: String,
    val merchant: String,
    val ip: String,
    val card: String,
    val anomaly: String?,
    val timestamp: Long
)
