package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.AlertEntity
import com.example.data.SentinelViewModel
import com.example.data.TransactionEntity
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                SentinelScreen()
            }
        }
    }
}

@Composable
fun SentinelScreen() {
    val viewModel: SentinelViewModel = viewModel()
    val transactions by viewModel.filteredTransactions.collectAsStateWithLifecycle()
    val allTransactionsRaw by viewModel.transactions.collectAsStateWithLifecycle()
    val alerts by viewModel.alerts.collectAsStateWithLifecycle()
    val activeFilter by viewModel.activeFilter.collectAsStateWithLifecycle()
    val activeTab by viewModel.activeTab.collectAsStateWithLifecycle()
    val isSimulationActive by viewModel.isSimulationActive.collectAsStateWithLifecycle()

    val selectedTxn by viewModel.selectedTransaction.collectAsStateWithLifecycle()
    val aiText by viewModel.aiAnalysisState.collectAsStateWithLifecycle()
    val isAiLoading by viewModel.isAiLoading.collectAsStateWithLifecycle()

    // Screen sizing for responsive grid layouts
    val configuration = LocalConfiguration.current
    val isWideScreen = configuration.screenWidthDp > 720

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = CarbonBlack,
        topBar = {
            SentinelTopBar(
                isLive = isSimulationActive,
                onToggleSimulation = { viewModel.toggleSimulation() },
                onTriggerOne = { viewModel.triggerSingleSimulatedTransaction() },
                onClearData = { viewModel.clearAllData() }
            )
        },
        bottomBar = {
            if (!isWideScreen) {
                SentinelBottomNav(
                    selectedTab = activeTab,
                    onTabSelected = { viewModel.activeTab.value = it }
                )
            }
        }
    ) { innerPadding ->
        ThemeSafeWrapper(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (isWideScreen) {
                // Expanded Canonical Layout (Desktop dashboard style)
                WideDashboardLayout(
                    transactions = transactions,
                    allTransactionsRaw = allTransactionsRaw,
                    alerts = alerts,
                    activeFilter = activeFilter,
                    onFilterClicked = { viewModel.activeFilter.value = it },
                    onTxnSelected = { viewModel.selectTransaction(it) }
                )
            } else {
                // Handy compact tab layouts
                AnimatedTabContent(
                    tab = activeTab,
                    transactions = transactions,
                    allTransactionsRaw = allTransactionsRaw,
                    alerts = alerts,
                    activeFilter = activeFilter,
                    onFilterClicked = { viewModel.activeFilter.value = it },
                    onTxnSelected = { viewModel.selectTransaction(it) }
                )
            }
        }
    }

    // AI summary modal dialog representation
    selectedTxn?.let { txn ->
        AiSummaryDialog(
            txn = txn,
            aiText = aiText,
            isLoading = isAiLoading,
            onDismiss = { viewModel.selectTransaction(null) }
        )
    }
}

@Composable
fun ThemeSafeWrapper(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .background(CarbonBlack)
    ) {
        content()
    }
}

// ══ TOP BAR COMPONENT ═══════════════════════════════════════════════════════
@Composable
fun SentinelTopBar(
    isLive: Boolean,
    onToggleSimulation: () -> Unit,
    onTriggerOne: () -> Unit,
    onClearData: () -> Unit
) {
    Surface(
        color = SophisticatedPanel,
        border = BorderStroke(1.dp, MutedSlate),
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 20.dp, vertical = 14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "SENTINEL.AI",
                        color = ApprovedGreen,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 2.sp
                    )
                    // High quality emerald-500 glowing green circle indicator
                    val pulse = rememberInfiniteTransition(label = "glow")
                    val alpha by pulse.animateFloat(
                        initialValue = 1f,
                        targetValue = 0.4f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(800, easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "alpha"
                    )
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .background(ApprovedGreen, shape = CircleShape)
                            .drawBehind {
                                drawCircle(
                                    color = ApprovedGreen,
                                    radius = size.minDimension * 1.6f,
                                    alpha = alpha * 0.5f
                                )
                            }
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "System Monitor",
                    color = OffWhiteText,
                    fontWeight = FontWeight.Medium,
                    fontSize = 18.sp,
                    letterSpacing = (-0.5).sp
                )
            }

            // Realtime dashboard configuration buttons
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Add test txn button
                IconButton(
                    onClick = onTriggerOne,
                    modifier = Modifier.size(36.dp),
                    colors = IconButtonDefaults.iconButtonColors(containerColor = CarbonBlack)
                ) {
                    Text("+", color = ApprovedGreen, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }

                // Delete all (cleanup)
                IconButton(
                    onClick = onClearData,
                    modifier = Modifier.size(36.dp),
                    colors = IconButtonDefaults.iconButtonColors(containerColor = CarbonBlack)
                ) {
                    Text("✖", color = MutedGreyText, fontSize = 11.sp)
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Pulsometer live indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(CarbonBlack, shape = RoundedCornerShape(8.dp))
                        .border(1.dp, MutedSlate, shape = RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .clickable { onToggleSimulation() }
                ) {
                    val pulse = rememberInfiniteTransition(label = "pulse")
                    val alpha by pulse.animateFloat(
                        initialValue = 1f,
                        targetValue = 0.2f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1000, easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "alpha"
                    )

                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(
                                color = if (isLive) ApprovedGreen else MutedGreyText,
                                shape = CircleShape
                            )
                            .drawBehind {
                                if (isLive) {
                                    drawCircle(
                                        color = ApprovedGreen,
                                        radius = size.minDimension * 1.5f,
                                        alpha = alpha
                                    )
                                }
                            }
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isLive) "LIVE" else "PAUSED",
                        color = if (isLive) ApprovedGreen else MutedGreyText,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "▼",
                        color = MutedGreyText,
                        fontSize = 7.sp
                    )
                }
            }
        }
    }
}

// ══ ADAPTIVE EXPANEDED DESKTOP LAYOUT ═══════════════════════════════════════
@Composable
fun WideDashboardLayout(
    transactions: List<TransactionEntity>,
    allTransactionsRaw: List<TransactionEntity>,
    alerts: List<AlertEntity>,
    activeFilter: String,
    onFilterClicked: (String) -> Unit,
    onTxnSelected: (TransactionEntity) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Left operational side: KPIs, Transaction stream, distribution
        Column(
            modifier = Modifier
                .weight(1.6f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Horizontal KPIs widget
            KpiGridSection(allTransactionsRaw = allTransactionsRaw)

            // Dynamic Transaction feed panel
            TransactionFeedPanel(
                modifier = Modifier.weight(1f),
                transactions = transactions,
                activeFilter = activeFilter,
                onFilterClicked = onFilterClicked,
                onTxnSelected = onTxnSelected
            )

            // Model performance distribution panel
            ModelPerformanceMetricsPanel(allTransactionsRaw = allTransactionsRaw)
        }

        // Right Operational panel: Pipeline, Real-time Warning alerts log, features weights
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SystemPipelinePanel()

            AlertLogPanel(
                modifier = Modifier.weight(0.9f),
                alerts = alerts
            )

            FeatureImportancePanel(modifier = Modifier.weight(1.1f))
        }
    }
}

// ══ COMPILED COMPACT MOBILE TAB VIEW SWITCHER ════════════════════════════════
@Composable
fun AnimatedTabContent(
    tab: String,
    transactions: List<TransactionEntity>,
    allTransactionsRaw: List<TransactionEntity>,
    alerts: List<AlertEntity>,
    activeFilter: String,
    onFilterClicked: (String) -> Unit,
    onTxnSelected: (TransactionEntity) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        when (tab) {
            "Feed" -> {
                KpiGridSection(allTransactionsRaw = allTransactionsRaw)
                TransactionFeedPanel(
                    modifier = Modifier.fillMaxSize(),
                    transactions = transactions,
                    activeFilter = activeFilter,
                    onFilterClicked = onFilterClicked,
                    onTxnSelected = onTxnSelected
                )
            }
            "Alerts" -> {
                AlertLogPanel(
                    modifier = Modifier.fillMaxSize(),
                    alerts = alerts
                )
            }
            "Model" -> {
                ModelPerformanceMetricsPanel(allTransactionsRaw = allTransactionsRaw, showHeader = false)
                Spacer(modifier = Modifier.height(10.dp))
                FeatureImportancePanel(modifier = Modifier.fillMaxSize())
            }
            "Pipeline" -> {
                SystemPipelinePanel()
            }
        }
    }
}

// ══ BOTTOM NAVIGATION BAR (MOBILE) ══════════════════════════════════════════
@Composable
fun SentinelBottomNav(
    selectedTab: String,
    onTabSelected: (String) -> Unit
) {
    NavigationBar(
        containerColor = SophisticatedPanel,
        tonalElevation = 0.dp,
        modifier = Modifier
            .drawBehind {
                drawLine(
                    color = MutedSlate,
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = 1f
                )
            }
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        val tabs = listOf("Feed", "Alerts", "Model", "Pipeline")
        val tabIcons = listOf("⚡", "🚨", "📊", "⚙")

        tabs.forEachIndexed { idx, tabTitle ->
            NavigationBarItem(
                selected = selectedTab == tabTitle,
                onClick = { onTabSelected(tabTitle) },
                icon = {
                    Text(
                        text = tabIcons[idx],
                        fontSize = 18.sp,
                        color = if (selectedTab == tabTitle) AccentBlue else MutedGreyText
                    )
                },
                label = {
                    Text(
                        text = tabIcons[idx] + " " + tabTitle.uppercase(),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = if (selectedTab == tabTitle) AccentBlue else MutedGreyText
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = MutedSlate
                )
            )
        }
    }
}

// ══ KPI CONTAINER ELEMENT (4-GRID SYSTEM) ═══════════════════════════════════
@Composable
fun KpiGridSection(allTransactionsRaw: List<TransactionEntity>) {
    val total = allTransactionsRaw.size.coerceAtLeast(1)
    val blockedList = allTransactionsRaw.filter { it.status == "BLOCKED" }
    val flaggedList = allTransactionsRaw.filter { it.status == "FLAGGED" }

    val blockedCount = blockedList.size
    val flaggedCount = flaggedList.size
    val approvedCount = allTransactionsRaw.filter { it.status == "APPROVED" }.size

    val blockedRate = (blockedCount.toDouble() / total) * 100
    val flaggedRate = (flaggedCount.toDouble() / total) * 100

    // Sum risk value: flagged and blocked amounts
    val riskValue = (blockedList.sumOf { it.amount } + flaggedList.sumOf { it.amount })

    val configuration = LocalConfiguration.current
    val gridCols = if (configuration.screenWidthDp > 600) 4 else 2

    if (gridCols == 4) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            KpiCard(
                title = "BLOCKED",
                value = blockedCount.toString(),
                subText = String.format("%.1f%% rate", blockedRate),
                color = BlockedRed,
                bgColor = BlockedBg,
                sparkPoints = allTransactionsRaw.take(20).map { if (it.status == "BLOCKED") it.score else 0.0 },
                modifier = Modifier.weight(1f)
            )
            KpiCard(
                title = "FLAGGED",
                value = flaggedCount.toString(),
                subText = String.format("%.1f%% rate", flaggedRate),
                color = FlaggedOrange,
                bgColor = FlaggedBg,
                sparkPoints = allTransactionsRaw.take(20).map { if (it.status == "FLAGGED") it.score else 0.0 },
                modifier = Modifier.weight(1f)
            )
            KpiCard(
                title = "APPROVED",
                value = approvedCount.toString(),
                subText = "clean txns",
                color = ApprovedGreen,
                bgColor = ApprovedBg,
                sparkPoints = allTransactionsRaw.take(20).map { if (it.status == "APPROVED") it.score else 0.0 },
                modifier = Modifier.weight(1f)
            )
            KpiCard(
                title = "RISK VALUE",
                value = String.format(Locale.US, "$%,.2f", riskValue),
                subText = "fraud exposure",
                color = RiskPurple,
                bgColor = Color(0xFF0D0A1A),
                sparkPoints = allTransactionsRaw.take(20).map { if (it.status != "APPROVED") it.score else 0.0 },
                modifier = Modifier.weight(1f)
            )
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KpiCard(
                    title = "BLOCKED",
                    value = blockedCount.toString(),
                    subText = String.format("%.1f%% rate", blockedRate),
                    color = BlockedRed,
                    bgColor = BlockedBg,
                    sparkPoints = allTransactionsRaw.take(15).map { if (it.status == "BLOCKED") it.score else 0.0 },
                    modifier = Modifier.weight(1f)
                )
                KpiCard(
                    title = "FLAGGED",
                    value = flaggedCount.toString(),
                    subText = String.format("%.1f%% rate", flaggedRate),
                    color = FlaggedOrange,
                    bgColor = FlaggedBg,
                    sparkPoints = allTransactionsRaw.take(15).map { if (it.status == "FLAGGED") it.score else 0.0 },
                    modifier = Modifier.weight(1f)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KpiCard(
                    title = "APPROVED",
                    value = approvedCount.toString(),
                    subText = "clean txns",
                    color = ApprovedGreen,
                    bgColor = ApprovedBg,
                    sparkPoints = allTransactionsRaw.take(15).map { if (it.status == "APPROVED") it.score else 0.0 },
                    modifier = Modifier.weight(1f)
                )
                KpiCard(
                    title = "RISK VALUE",
                    value = String.format(Locale.US, "$%,.0f", riskValue),
                    subText = "exposure",
                    color = RiskPurple,
                    bgColor = Color(0xFF0D0A1A),
                    sparkPoints = allTransactionsRaw.take(15).map { if (it.status != "APPROVED") it.score else 0.0 },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun KpiCard(
    title: String,
    value: String,
    subText: String,
    color: Color,
    bgColor: Color,
    sparkPoints: List<Double>,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = bgColor),
        border = BorderStroke(1.dp, color.copy(alpha = 0.15f)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .padding(vertical = 12.dp, horizontal = 14.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = title,
                    color = MutedGreyText,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = value,
                    color = color,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subText,
                    color = MutedGreyText,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            // Drawing mini reactive canvas Sparkline
            Box(
                modifier = Modifier
                    .width(60.dp)
                    .height(28.dp)
            ) {
                Sparkline(
                    points = if (sparkPoints.isEmpty()) listOf(0.1, 0.4, 0.2, 0.5, 0.3) else sparkPoints,
                    outlineColor = color
                )
            }
        }
    }
}

// ══ TRANSACTION FEED LIST PANEL ═════════════════════════════════════════════
@Composable
fun TransactionFeedPanel(
    modifier: Modifier = Modifier,
    transactions: List<TransactionEntity>,
    activeFilter: String,
    onFilterClicked: (String) -> Unit,
    onTxnSelected: (TransactionEntity) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = ObsidianCard),
        border = BorderStroke(1.dp, MutedSlate),
        modifier = modifier
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TRANSACTION FEED",
                    color = OffWhiteText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )

                // Inline filters matching desktop / mobile toggles
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val filters = listOf("ALL", "BLOCKED", "FLAGGED", "APPROVED")
                    filters.forEach { filterName ->
                        Box(
                            modifier = Modifier
                                .background(
                                    color = if (activeFilter == filterName) MutedSlate else Color.Transparent,
                                    shape = RoundedCornerShape(3.dp)
                                )
                                .border(1.dp, MutedSlate, shape = RoundedCornerShape(3.dp))
                                .clickable { onFilterClicked(filterName) }
                                .padding(horizontal = 7.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = filterName,
                                color = if (activeFilter == filterName) OffWhiteText else MutedGreyText,
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                if (transactions.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No recorded logs match filter",
                            color = MutedGreyText,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(transactions, key = { it.id }) { txn ->
                            TransactionRowItem(
                                txn = txn,
                                onClick = { onTxnSelected(txn) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TransactionRowItem(
    txn: TransactionEntity,
    onClick: () -> Unit
) {
    val scorePercent = (txn.score * 100).roundToInt()
    val badgeColor = when (txn.status) {
        "BLOCKED" -> BlockedRed
        "FLAGGED" -> FlaggedOrange
        else -> ApprovedGreen
    }
    val amountColor = if (txn.amount > 500) BlockedRed else OffWhiteText

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .background(
                when (txn.status) {
                    "BLOCKED" -> BlockedBg.copy(alpha = 0.2f)
                    "FLAGGED" -> FlaggedBg.copy(alpha = 0.15f)
                    else -> Color.Transparent
                }
            )
            .padding(vertical = 10.dp, horizontal = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1.2f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = txn.id,
                        color = AccentBlue,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = txn.merchant,
                        color = MutedGreyText,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Small card brand flag representation
                    Text(
                        text = txn.card,
                        color = MutedGreyText,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "•  ${txn.location}",
                        color = MutedGreyText,
                        fontSize = 9.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Score probability horizontal mini bar
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(50.dp)
                            .height(5.dp)
                            .background(MutedSlate, shape = RoundedCornerShape(2.dp))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(txn.score.toFloat())
                                .background(badgeColor, shape = RoundedCornerShape(2.dp))
                        )
                    }
                    Text(
                        text = "$scorePercent%",
                        color = badgeColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.width(28.dp),
                        textAlign = TextAlign.End
                    )
                }
                Text(
                    text = "FRAUD RESIDUAL",
                    color = MutedGreyText,
                    fontSize = 7.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            // Amount + Status Badge Column
            Column(
                modifier = Modifier.weight(0.9f),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = String.format(Locale.US, "$%,.2f", txn.amount),
                    color = amountColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(3.dp))
                // Clean tiny status badge
                Box(
                    modifier = Modifier
                        .background(
                            color = when (txn.status) {
                                "BLOCKED" -> BlockedBg
                                "FLAGGED" -> FlaggedBg
                                else -> ApprovedBg
                            },
                            shape = RoundedCornerShape(3.dp)
                        )
                        .border(1.dp, badgeColor.copy(alpha = 0.4f), shape = RoundedCornerShape(3.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = txn.status,
                        color = badgeColor,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Analyze CTA Button (opens dialog)
            IconButton(
                onClick = { onClick() },
                modifier = Modifier
                    .padding(start = 6.dp)
                    .size(26.dp)
                    .background(MutedSlate, shape = RoundedCornerShape(4.dp))
            ) {
                Text("▶", color = AccentBlue, fontSize = 8.sp)
            }
        }

        // Expanded anomalies tags if flagged/blocked
        txn.anomaly?.let { anomaly ->
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(BlockedBg.copy(alpha = 0.3f), shape = RoundedCornerShape(2.dp))
                    .border(BorderStroke(1.dp, BlockedRed.copy(alpha = 0.2f)), shape = RoundedCornerShape(2.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "⚠ $anomaly",
                    color = BlockedRed.copy(alpha = 0.8f),
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

// ══ MODEL PERFORMANCE METRICS PANEL ════════════════════════════════════════
@Composable
fun ModelPerformanceMetricsPanel(
    allTransactionsRaw: List<TransactionEntity>,
    showHeader: Boolean = true
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = ObsidianCard),
        border = BorderStroke(1.dp, MutedSlate),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            if (showHeader) {
                Text(
                    text = "MODEL PERFORMANCE",
                    color = OffWhiteText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Gauges Row matching standard static specifications from prompt
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatArcGauge(name = "PRECISION", score = 0.964, label = "96%", color = ApprovedGreen)
                StatArcGauge(name = "RECALL", score = 0.941, label = "94%", color = ApprovedGreen)
                StatArcGauge(name = "F1 SCORE", score = 0.952, label = "95%", color = ApprovedGreen)
                StatArcGauge(name = "ROC-AUC", score = 0.987, label = "99%", color = ApprovedGreen)
            }

            Spacer(modifier = Modifier.height(11.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(MutedSlate)
            )
            Spacer(modifier = Modifier.height(11.dp))

            // Score Distribution Canvas Chart (representing historic last 30 scores)
            Text(
                text = "SCORE DISTRIBUTION — LAST 30 TXN",
                color = MutedGreyText,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Row of little custom vertically scaled bars representing score counts
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(26.dp)
                    .clip(RoundedCornerShape(4.dp)),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                val last30Txs = allTransactionsRaw.take(30).reversed()
                val scorePoints = if (last30Txs.isNotEmpty()) {
                    last30Txs.map { it.score }
                } else {
                    List(30) { 0.1 + (it % 5) * 0.15 } // Mock starting points
                }

                scorePoints.forEach { score ->
                    val color = when {
                        score > 0.88 -> BlockedRed
                        score > 0.72 -> FlaggedOrange
                        else -> ApprovedGreen
                    }
                    val intensity = 0.6f + (score * 0.4f).toFloat()

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(score.coerceIn(0.05, 1.0).toFloat())
                            .background(color.copy(alpha = intensity))
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            // Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(6.dp).background(ApprovedGreen))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Clean", color = ApprovedGreen, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(6.dp).background(FlaggedOrange))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Flagged", color = FlaggedOrange, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(6.dp).background(BlockedRed))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Blocked", color = BlockedRed, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                }
            }
        }
    }
}

@Composable
fun StatArcGauge(
    name: String,
    score: Double,
    label: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(72.dp)
    ) {
        Text(
            text = name,
            color = MutedGreyText,
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 0.5.sp
        )
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier.size(46.dp),
            contentAlignment = Alignment.Center
        ) {
            // Underlay grey track arc
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawArc(
                    color = MutedSlate,
                    startAngle = -210f,
                    sweepAngle = 240f,
                    useCenter = false,
                    style = Stroke(width = 8f, cap = StrokeCap.Round)
                )
            }
            // Dynamic colorful progress arc
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawArc(
                    color = color,
                    startAngle = -210f,
                    sweepAngle = (240f * score).toFloat(),
                    useCenter = false,
                    style = Stroke(width = 8f, cap = StrokeCap.Round)
                )
            }
            Text(
                text = label,
                color = color,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Isolation",
            color = MutedGreyText,
            fontSize = 7.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}

// ══ SYSTEM PIPELINE PANEL COMPONENT ═════════════════════════════════════════
@Composable
fun SystemPipelinePanel() {
    Card(
        colors = CardDefaults.cardColors(containerColor = ObsidianCard),
        border = BorderStroke(1.dp, MutedSlate),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "SYSTEM PIPELINE",
                color = OffWhiteText,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(10.dp))

            PipelineRowItem(name = "Razorpay API", desc = "Payment gateway", status = "CONNECTED", color = ApprovedGreen)
            PipelineRowItem(name = "Feature Engine", desc = "IP • Device • Location", status = "ACTIVE", color = ApprovedGreen)
            PipelineRowItem(name = "Isolation Forest", desc = "Scikit-learn model", status = "RUNNING", color = ApprovedGreen)
            PipelineRowItem(name = "Autoencoder", desc = "TensorFlow anomaly", status = "RUNNING", color = ApprovedGreen)
            PipelineRowItem(name = "MongoDB Atlas", desc = "Transaction logs", status = "SYNCED", color = ApprovedGreen)
            PipelineRowItem(name = "Alert Engine", desc = "Real-time webhooks", status = "LIVE", color = ApprovedGreen, borderBottom = false)
        }
    }
}

@Composable
fun PipelineRowItem(
    name: String,
    desc: String,
    status: String,
    color: Color,
    borderBottom: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                if (borderBottom) {
                    drawLine(
                        color = MutedSlate,
                        start = Offset(0f, size.height),
                        end = Offset(size.width, size.height),
                        strokeWidth = 1f
                    )
                }
            }
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "⚡",
                color = AccentBlue,
                fontSize = 10.sp
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = name,
                    color = OffWhiteText,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = desc,
                    color = MutedGreyText,
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Box(
            modifier = Modifier
                .background(color.copy(alpha = 0.12f), shape = RoundedCornerShape(3.dp))
                .border(1.dp, color.copy(alpha = 0.3f), shape = RoundedCornerShape(3.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = status,
                color = color,
                fontSize = 7.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

// ══ ALERT LOGS COMPONENT ════════════════════════════════════════════════════
@Composable
fun AlertLogPanel(
    modifier: Modifier = Modifier,
    alerts: List<AlertEntity>
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = ObsidianCard),
        border = BorderStroke(1.dp, MutedSlate),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "ALERT LOG",
                color = OffWhiteText,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(10.dp))

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                if (alerts.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No recorded alerts yet…",
                            color = MutedGreyText,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(alerts) { alert ->
                            AlertLogItemRow(alert = alert)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AlertLogItemRow(alert: AlertEntity) {
    val borderColor = if (alert.severity == "critical") BlockedRed else FlaggedOrange
    val dateText = remember(alert.timestamp) {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        sdf.format(Date(alert.timestamp))
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                drawLine(
                    color = borderColor,
                    start = Offset(0f, 0f),
                    end = Offset(0f, size.height),
                    strokeWidth = 4f
                )
            }
            .padding(start = 10.dp, top = 2.dp, bottom = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = alert.message,
                color = if (alert.severity == "critical") Color(0xFFFCA5A5) else Color(0xFFFCD34D),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = dateText,
                color = MutedGreyText,
                fontSize = 8.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

// ══ FEATURE IMPORTANCE LIST ═════════════════════════════════════════════════
@Composable
fun FeatureImportancePanel(modifier: Modifier = Modifier) {
    Card(
        colors = CardDefaults.cardColors(containerColor = ObsidianCard),
        border = BorderStroke(1.dp, MutedSlate),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "FEATURE IMPORTANCE WEIGHTS",
                color = OffWhiteText,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(10.dp))

            // Scrollable feature columns
            Column(
                verticalArrangement = Arrangement.spacedBy(7.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                FeatureBar(name = "Transaction Amount", value = 0.89, color = BlockedRed)
                FeatureBar(name = "IP Reputation", value = 0.76, color = FlaggedOrange)
                FeatureBar(name = "Velocity (1hr)", value = 0.71, color = FlaggedOrange)
                FeatureBar(name = "Device Fingerprint", value = 0.65, color = AccentBlue)
                FeatureBar(name = "Geo Anomaly", value = 0.58, color = AccentBlue)
                FeatureBar(name = "Time Pattern", value = 0.44, color = RiskPurple)
                FeatureBar(name = "Merchant Risk", value = 0.37, color = RiskPurple)
            }
        }
    }
}

@Composable
fun FeatureBar(
    name: String,
    value: Double,
    color: Color
) {
    val percentValue = (value * 100).roundToInt()
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = name,
                color = MutedGreyText,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = "$percentValue%",
                color = color,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .background(MutedSlate, shape = RoundedCornerShape(1.5.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(value.toFloat())
                    .background(color, shape = RoundedCornerShape(1.5.dp))
            )
        }
    }
}

// ══ AI NARRATIVE ANALYST DIALOG / SHEETS ════════════════════════════════════
@Composable
fun AiSummaryDialog(
    txn: TransactionEntity,
    aiText: String?,
    isLoading: Boolean,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = ObsidianCard),
            border = BorderStroke(1.dp, BorderGrey),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            Column {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MutedSlate.copy(alpha = 0.3f))
                        .drawBehind {
                            drawLine(
                                color = MutedSlate,
                                start = Offset(0f, size.height),
                                end = Offset(size.width, size.height),
                                strokeWidth = 1f
                            )
                        }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .background(
                                    if (isLoading) FlaggedOrange else ApprovedGreen,
                                    shape = CircleShape
                                )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "AI ANALYST — ${txn.id}",
                            color = OffWhiteText,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Text(
                        text = "×",
                        color = MutedGreyText,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clickable { onDismiss() }
                            .padding(4.dp)
                    )
                }

                // Metadata cards
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .drawBehind {
                            drawLine(
                                color = MutedSlate,
                                start = Offset(0f, size.height),
                                end = Offset(size.width, size.height),
                                strokeWidth = 1f
                            )
                        }
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetadataPill(title = "AMOUNT", value = String.format(Locale.US, "$%,.2f", txn.amount), modifier = Modifier.weight(1f))
                    MetadataPill(title = "SCORE", value = "${(txn.score * 100).roundToInt()}%", modifier = Modifier.weight(1f))
                    MetadataPill(
                        title = "STATUS",
                        value = txn.status,
                        valueColor = when (txn.status) {
                            "BLOCKED" -> BlockedRed
                            "FLAGGED" -> FlaggedOrange
                            else -> ApprovedGreen
                        },
                        modifier = Modifier.weight(1.2f)
                    )
                }

                // AI Generated analysis body with styled typography
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "GEMINI AI — ISOLATION FOREST + BEHAVIORAL MODEL",
                        color = MutedGreyText,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 100.dp)
                            .background(CarbonBlack, shape = RoundedCornerShape(6.dp))
                            .border(1.dp, MutedSlate, shape = RoundedCornerShape(6.dp))
                            .padding(12.dp)
                    ) {
                        SelectionContainer {
                            Column {
                                Text(
                                    text = aiText ?: "Awaiting system context parsing...",
                                    color = OffWhiteText,
                                    fontSize = 12.sp,
                                    lineHeight = 18.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                if (isLoading) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    // Simulated cursor blink loader
                                    val pulse = rememberInfiniteTransition(label = "cursor")
                                    val alpha by pulse.animateFloat(
                                        initialValue = 1f,
                                        targetValue = 0f,
                                        animationSpec = infiniteRepeatable(
                                            animation = tween(500, easing = LinearEasing),
                                            repeatMode = RepeatMode.Reverse
                                        ),
                                        label = "alpha"
                                    )
                                    Box(
                                        modifier = Modifier
                                            .width(7.dp)
                                            .height(14.dp)
                                            .background(BlockedRed.copy(alpha = alpha))
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Safe container wrapper around plain SelectionContainer representation
@Composable
fun SelectionContainer(content: @Composable () -> Unit) {
    androidx.compose.foundation.text.selection.SelectionContainer {
        content()
    }
}

@Composable
fun MetadataPill(
    title: String,
    value: String,
    valueColor: Color = OffWhiteText,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(CarbonBlack, shape = RoundedCornerShape(6.dp))
            .border(1.dp, MutedSlate, shape = RoundedCornerShape(6.dp))
            .padding(vertical = 6.dp, horizontal = 8.dp)
    ) {
        Column {
            Text(
                text = title,
                color = MutedGreyText,
                fontSize = 7.sp,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                color = valueColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun Sparkline(
    points: List<Double>,
    outlineColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        if (points.size < 2) return@Canvas

        val width = size.width
        val height = size.height
        val maxVal = (points.maxOrNull() ?: 1.0).coerceAtLeast(0.01)
        val minVal = points.minOrNull() ?: 0.0
        val range = (maxVal - minVal).coerceAtLeast(0.01)

        val path = Path()
        points.forEachIndexed { index, value ->
            val x = index * (width / (points.size - 1))
            val normalizedY = ((value - minVal) / range)
            val y = height - (normalizedY.toFloat() * height)
            if (index == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }

        drawPath(
            path = path,
            color = outlineColor,
            style = Stroke(width = 4f, cap = StrokeCap.Round)
        )
    }
}
