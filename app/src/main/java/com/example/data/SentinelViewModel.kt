package com.example.data

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SentinelViewModel(application: Application) : AndroidViewModel(application) {

    private val db = SentinelDatabase.getDatabase(application)
    private val repository = SentinelRepository(db.transactionDao(), db.alertDao())

    val transactions: StateFlow<List<TransactionEntity>> = repository.allTransactions
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val alerts: StateFlow<List<AlertEntity>> = repository.allAlerts
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val activeFilter = MutableStateFlow("ALL") // "ALL", "APPROVED", "FLAGGED", "BLOCKED"
    val activeTab = MutableStateFlow("Feed") // "Feed", "Alerts", "Model", "Pipeline"
    val isSimulationActive = MutableStateFlow(true)

    val selectedTransaction = MutableStateFlow<TransactionEntity?>(null)
    val aiAnalysisState = MutableStateFlow<String?>(null)
    val isAiLoading = MutableStateFlow(false)

    // Filtered transaction list
    val filteredTransactions: StateFlow<List<TransactionEntity>> = combine(
        transactions,
        activeFilter
    ) { txList, filter ->
        if (filter == "ALL") txList else txList.filter { it.status == filter }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var simulationJob: Job? = null

    init {
        viewModelScope.launch {
            repository.preseedIfEmpty()
            if (isSimulationActive.value) {
                startSimulation()
            }
        }
    }

    fun startSimulation() {
        stopSimulation()
        simulationJob = viewModelScope.launch(Dispatchers.IO) {
            while (true) {
                delay(2200L)
                repository.generateAndInsertNextTransaction()
            }
        }
    }

    fun stopSimulation() {
        simulationJob?.cancel()
        simulationJob = null
    }

    fun toggleSimulation() {
        val current = isSimulationActive.value
        isSimulationActive.value = !current
        if (!current) {
            startSimulation()
        } else {
            stopSimulation()
        }
    }

    fun selectTransaction(txn: TransactionEntity?) {
        selectedTransaction.value = txn
        if (txn == null) {
            aiAnalysisState.value = null
            isAiLoading.value = false
        } else {
            fetchAiAnalysis(txn)
        }
    }

    private fun fetchAiAnalysis(txn: TransactionEntity) {
        isAiLoading.value = true
        aiAnalysisState.value = "Analyzing transaction patterns..."
        viewModelScope.launch(Dispatchers.IO) {
            val result = repository.analyzeTransactionWithGemini(txn)
            withContext(Dispatchers.Main) {
                aiAnalysisState.value = result
                isAiLoading.value = false
            }
        }
    }

    fun triggerSingleSimulatedTransaction() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.generateAndInsertNextTransaction()
        }
    }

    fun clearAllData() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearAllData()
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopSimulation()
    }
}
