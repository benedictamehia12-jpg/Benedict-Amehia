package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.util.Random
import kotlin.math.roundToInt

class SentinelRepository(
    private val transactionDao: TransactionDao,
    private val alertDao: AlertDao
) {
    val allTransactions: Flow<List<TransactionEntity>> = transactionDao.getAllTransactionsFlow()
    val allAlerts: Flow<List<AlertEntity>> = alertDao.getAllAlertsFlow()

    private val random = Random()

    private val devices = listOf("iPhone 14 Pro", "Samsung S23", "Chrome/Win11", "Firefox/Mac", "Edge/Win10", "Pixel 7", "Safari/iPad")
    private val locations = listOf("Lagos, NG", "Accra, GH", "London, UK", "New York, US", "Dubai, UAE", "Berlin, DE", "Mumbai, IN", "Singapore, SG")
    private val merchants = listOf("Shopify #4421", "Amazon Mktpl", "AliExpress GH", "Jumia Nigeria", "PayStack Demo", "Razorpay Merch", "Stripe Atlas")
    private val cards = listOf("Visa", "Mastercard", "Verve", "Amex")
    private val anomalies = listOf("Velocity spike", "New device", "Geo mismatch", "High-risk IP", "Unusual hour", "Card test")

    suspend fun generateAndInsertNextTransaction(): TransactionEntity {
        // Query current list to get latest ID to increment
        val currentTxs = allTransactions.first()
        val nextIdNum = if (currentTxs.isEmpty()) {
            1
        } else {
            val maxId = currentTxs.mapNotNull {
                it.id.replace("TXN-", "").toIntOrNull()
            }.maxOrNull() ?: 0
            maxId + 1
        }

        val txId = "TXN-${String.format("%06d", nextIdNum)}"
        val score = random.nextDouble()
        val isFraud = score > 0.72

        val amount = if (isFraud) {
            random.nextDouble() * (9999.0 - 800.0) + 800.0
        } else {
            random.nextDouble() * (600.0 - 5.0) + 5.0
        }

        val status = when {
            score > 0.88 -> "BLOCKED"
            score > 0.72 -> "FLAGGED"
            else -> "APPROVED"
        }

        val anomaly = if (isFraud) anomalies[random.nextInt(anomalies.size)] else null
        val txn = TransactionEntity(
            id = txId,
            amount = (amount * 100.0).roundToInt() / 100.0,
            score = (score * 100.0).roundToInt() / 100.0,
            status = status,
            device = devices[random.nextInt(devices.size)],
            location = locations[random.nextInt(locations.size)],
            merchant = merchants[random.nextInt(merchants.size)],
            ip = "${random.nextInt(254) + 1}.${random.nextInt(255)}.${random.nextInt(255)}.${random.nextInt(255)}",
            card = cards[random.nextInt(cards.size)],
            anomaly = anomaly,
            timestamp = System.currentTimeMillis()
        )

        transactionDao.insertTransaction(txn)

        if (status != "APPROVED") {
            val alertMessage = if (status == "BLOCKED") {
                "🚨 $txId BLOCKED — Score ${(txn.score * 100).roundToInt()}%"
            } else {
                "⚠ $txId flagged — $anomaly"
            }

            val alert = AlertEntity(
                severity = if (status == "BLOCKED") "critical" else "warning",
                message = alertMessage,
                transactionId = txId,
                timestamp = txn.timestamp
            )
            alertDao.insertAlert(alert)
        }

        return txn
    }

    suspend fun preseedIfEmpty() {
        val count = allTransactions.first().size
        if (count == 0) {
            // Preseed with 12 older transactions
            val initialTxs = mutableListOf<TransactionEntity>()
            val initialAlerts = mutableListOf<AlertEntity>()
            var timeOffset = 12 * 60 * 1000L // start 12 min ago

            for (i in 1..15) {
                val score = random.nextDouble()
                val isFraud = score > 0.72
                val amount = if (isFraud) {
                    random.nextDouble() * (3000.0 - 800.0) + 800.0
                } else {
                    random.nextDouble() * (450.0 - 10.0) + 10.0
                }
                val status = when {
                    score > 0.88 -> "BLOCKED"
                    score > 0.72 -> "FLAGGED"
                    else -> "APPROVED"
                }
                val txId = "TXN-${String.format("%06d", i)}"
                val anomaly = if (isFraud) anomalies[random.nextInt(anomalies.size)] else null

                val txn = TransactionEntity(
                    id = txId,
                    amount = (amount * 100.0).roundToInt() / 100.0,
                    score = (score * 100.0).roundToInt() / 100.0,
                    status = status,
                    device = devices[random.nextInt(devices.size)],
                    location = locations[random.nextInt(locations.size)],
                    merchant = merchants[random.nextInt(merchants.size)],
                    ip = "${random.nextInt(254) + 1}.${random.nextInt(255)}.${random.nextInt(255)}.${random.nextInt(255)}",
                    card = cards[random.nextInt(cards.size)],
                    anomaly = anomaly,
                    timestamp = System.currentTimeMillis() - timeOffset
                )
                initialTxs.add(txn)

                if (status != "APPROVED") {
                    val alertMessage = if (status == "BLOCKED") {
                        "🚨 $txId BLOCKED — Score ${(txn.score * 100).roundToInt()}%"
                    } else {
                        "⚠ $txId flagged — $anomaly"
                    }
                    initialAlerts.add(
                        AlertEntity(
                            severity = if (status == "BLOCKED") "critical" else "warning",
                            message = alertMessage,
                            transactionId = txId,
                            timestamp = txn.timestamp
                        )
                    )
                }
                timeOffset -= 45 * 1000L // 45 seconds frequency
            }

            // Batch insert
            transactionDao.insertTransactions(initialTxs)
            initialAlerts.forEach { alertDao.insertAlert(it) }
        }
    }

    suspend fun clearAllData() {
        transactionDao.clearAllTransactions()
        alertDao.clearAllAlerts()
    }

    suspend fun analyzeTransactionWithGemini(txn: TransactionEntity): String {
        val apiKey = com.example.BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return "Analysis engine unavailable (API Key not configured in Studio Secrets).\n\n" +
                   "Please ask the user to configure 'GEMINI_API_KEY' in the AI Studio Secrets panel.\n\n" +
                   "FALLBACK SIMULATED ANALYSIS:\n" +
                   "1) Risk Summary: High confidence anomalies identified in payment parameters showing high fraud risk of ${(txn.score * 100).roundToInt()}% on card network ${txn.card}.\n" +
                   "2) Key Indicators: ${txn.anomaly ?: "Abnormal monetary transaction velocity"} processed via ${txn.device} from unconfirmed geo IP locations (${txn.ip}).\n" +
                   "3) Recommended Action: Maintain strict status ${txn.status} for transaction ${txn.id} and initiate charge verification protocol."
        }

        val systemPrompt = "You are a fraud detection AI analyst. Be direct, concise, and technical. Max 150 words."
        val prompt = """
            Transaction ID: ${txn.id} | Amount: $${String.format("%.2f", txn.amount)} | Fraud Score: ${String.format("%.0f", txn.score * 100)}%
            Status: ${txn.status} | Device: ${txn.device} | Location: ${txn.location}
            IP: ${txn.ip} | Card: ${txn.card} | Merchant: ${txn.merchant} | Anomaly: ${txn.anomaly ?: "None"}

            Provide: 1) Risk summary  2) Key indicators  3) Recommended action
        """.trimIndent()

        return try {
            val request = GeminiRequest.create(prompt = prompt, systemPrompt = systemPrompt)
            val response = GeminiClient.service.generateContent(apiKey, request)
            response.getText() ?: "Analysis summary could not be retrieved from AI response."
        } catch (e: Exception) {
            "Error generating analysis: ${e.localizedMessage ?: "Connection error"}\n\n" +
            "FALLBACK CONCISE ANALYSIS:\n" +
            "• Risk Summary: Severity matches status ${txn.status} due to suspicious trigger metadata.\n" +
            "• Key Indicators: IP address ${txn.ip} reported from ${txn.location} using ${txn.device}.\n" +
            "• Recommendation: Review the transaction logs and alert status in Sentinel Engine."
        }
    }
}
