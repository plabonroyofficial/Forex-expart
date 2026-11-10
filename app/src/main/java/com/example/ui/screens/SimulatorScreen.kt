package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PositionStatus
import com.example.data.model.TradePosition
import com.example.data.model.TradeType
import com.example.ui.theme.BearishRed
import com.example.ui.theme.BorderDark
import com.example.ui.theme.BullishGreen
import com.example.ui.theme.CanvasBackground
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.ForexViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SimulatorScreen(
    viewModel: ForexViewModel,
    onOpenTradeDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    val virtualBalance by viewModel.virtualBalance.collectAsState()
    val allPositions by viewModel.allPositions.collectAsState()
    val allPairs by viewModel.repository.pairs.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    var showResetDialog by remember { mutableStateOf(false) }

    val openPositions = remember(allPositions) {
        allPositions.filter { it.status == PositionStatus.OPEN }
    }
    val closedPositions = remember(allPositions) {
        allPositions.filter { it.status != PositionStatus.OPEN }
    }

    // Calculate current live unrealized PnL
    val unrealizedPnL = remember(openPositions, allPairs) {
        openPositions.sumOf { pos ->
            val pair = allPairs.find { it.symbol == pos.symbol }
            val currentPrice = if (pos.type == TradeType.BUY) pair?.bid ?: pos.openPrice else pair?.ask ?: pos.openPrice
            val digits = pair?.digits ?: 5
            val pipMultiplier = if (digits == 3 || digits == 2) 100.0 else 10000.0
            val pips = if (pos.type == TradeType.BUY) {
                (currentPrice - pos.openPrice) * pipMultiplier
            } else {
                (pos.openPrice - currentPrice) * pipMultiplier
            }
            val dollarPerPip = if (pos.symbol.contains("XAU") || pos.symbol.contains("BTC")) 1.0 else 10.0
            pips * pos.lots * dollarPerPip
        }
    }

    val equity = virtualBalance + unrealizedPnL

    val winCount = closedPositions.count { it.profitLoss > 0 }
    val totalClosed = closedPositions.size
    val winRate = if (totalClosed > 0) (winCount.toDouble() / totalClosed) * 100.0 else 0.0

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CanvasBackground)
            .testTag("simulator_screen")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header: Account Telemetry Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceDark)
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "PAPER TRADING TERMINAL",
                            style = MaterialTheme.typography.labelSmall,
                            color = ElectricCyan,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "$%,.2f".format(equity),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    Row {
                        IconButton(
                            onClick = { showResetDialog = true },
                            modifier = Modifier.testTag("reset_balance_button")
                        ) {
                            Icon(Icons.Default.RestartAlt, contentDescription = "Reset Balance", tint = TextMuted)
                        }
                        if (closedPositions.isNotEmpty()) {
                            IconButton(
                                onClick = { viewModel.clearTradeHistory() },
                                modifier = Modifier.testTag("clear_history_button")
                            ) {
                                Icon(Icons.Default.DeleteSweep, contentDescription = "Clear History", tint = TextMuted)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Metric Pills
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetricCard(
                        title = "Virtual Balance",
                        value = "$%,.2f".format(virtualBalance),
                        color = TextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Unrealized PnL",
                        value = (if (unrealizedPnL >= 0) "+$" else "-$") + "%,.2f".format(kotlin.math.abs(unrealizedPnL)),
                        color = if (unrealizedPnL >= 0) BullishGreen else BearishRed,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Win Rate",
                        value = if (totalClosed > 0) "%.1f%%".format(winRate) else "N/A",
                        color = ElectricCyan,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Tabs: Open Positions vs Trade Journal
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = SurfaceDark,
                contentColor = ElectricCyan,
                indicator = { tabPositions ->
                    TabRowDefaults.Indicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = ElectricCyan,
                        height = 2.dp
                    )
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Text(
                            text = "Open Positions (${openPositions.size})",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == 0) ElectricCyan else TextSecondary
                        )
                    },
                    modifier = Modifier.testTag("tab_open_positions")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Text(
                            text = "Journal History (${closedPositions.size})",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == 1) ElectricCyan else TextSecondary
                        )
                    },
                    modifier = Modifier.testTag("tab_journal_history")
                )
            }

            // Tab Content
            if (selectedTab == 0) {
                if (openPositions.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 70.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.TrendingUp,
                                contentDescription = "No Open Trades",
                                tint = TextMuted,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No active open positions",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Tap the + button to test an AI signal or manual trade",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(openPositions, key = { it.id }) { pos ->
                            val pair = allPairs.find { it.symbol == pos.symbol }
                            val currentPrice = if (pos.type == TradeType.BUY) pair?.bid ?: pos.openPrice else pair?.ask ?: pos.openPrice
                            val digits = pair?.digits ?: 5
                            val pipMultiplier = if (digits == 3 || digits == 2) 100.0 else 10000.0
                            val pips = if (pos.type == TradeType.BUY) {
                                (currentPrice - pos.openPrice) * pipMultiplier
                            } else {
                                (pos.openPrice - currentPrice) * pipMultiplier
                            }
                            val dollarPerPip = if (pos.symbol.contains("XAU") || pos.symbol.contains("BTC")) 1.0 else 10.0
                            val livePnL = pips * pos.lots * dollarPerPip

                            OpenPositionCard(
                                position = pos,
                                currentPrice = currentPrice,
                                livePnL = livePnL,
                                pips = pips,
                                digits = digits,
                                onClose = { viewModel.closeTrade(pos) }
                            )
                        }
                    }
                }
            } else {
                if (closedPositions.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 70.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = "No History",
                                tint = TextMuted,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Trade journal is empty",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextSecondary
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(closedPositions, key = { it.id }) { pos ->
                            ClosedPositionCard(position = pos)
                        }
                    }
                }
            }
        }

        // FAB: Open New Trade
        FloatingActionButton(
            onClick = onOpenTradeDialog,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 18.dp, bottom = 18.dp)
                .testTag("open_new_trade_fab"),
            containerColor = ElectricCyan,
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Open Trade",
                    tint = Color.Black
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "NEW TRADE",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }

        // Reset Balance Confirmation Dialog
        if (showResetDialog) {
            AlertDialog(
                onDismissRequest = { showResetDialog = false },
                title = { Text("Reset Virtual Account?", color = TextPrimary) },
                text = {
                    Text(
                        "This will reset your paper trading balance back to $10,000.00 virtual equity.",
                        color = TextSecondary
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.resetBalance(10000.0)
                            showResetDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan)
                    ) {
                        Text("Reset ($10K)", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { showResetDialog = false }) {
                        Text("Cancel", color = TextSecondary)
                    }
                },
                containerColor = SurfaceDark
            )
        }
    }
}

@Composable
private fun MetricCard(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(SurfaceCard)
            .border(0.5.dp, BorderDark, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Text(text = title, style = MaterialTheme.typography.labelSmall, color = TextMuted, fontSize = 9.sp)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = value, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
fun OpenPositionCard(
    position: TradePosition,
    currentPrice: Double,
    livePnL: Double,
    pips: Double,
    digits: Int,
    onClose: () -> Unit
) {
    val isBuy = position.type == TradeType.BUY
    val typeColor = if (isBuy) BullishGreen else BearishRed
    val pnlColor = if (livePnL >= 0) BullishGreen else BearishRed

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(SurfaceCard)
            .border(0.5.dp, BorderDark, RoundedCornerShape(10.dp))
            .padding(12.dp)
            .testTag("open_pos_${position.id}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = position.symbol,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(typeColor.copy(alpha = 0.2f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "${position.type} ${position.lots} lots",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = typeColor
                    )
                }
            }

            // Close Position Button
            Button(
                onClick = onClose,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C1618)),
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                modifier = Modifier.testTag("close_pos_button_${position.id}")
            ) {
                Text("Close", color = BearishRed, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Open: %.${digits}f  ➜  Now: %.${digits}f".format(position.openPrice, currentPrice),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Text(
                    text = "SL: %.${digits}f  |  TP: %.${digits}f".format(position.stopLoss, position.takeProfit),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    fontSize = 10.sp
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = (if (livePnL >= 0) "+$" else "-$") + "%.2f".format(kotlin.math.abs(livePnL)),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = pnlColor
                )
                Text(
                    text = (if (pips >= 0) "+" else "") + "%.1f pips".format(pips),
                    style = MaterialTheme.typography.labelSmall,
                    color = pnlColor,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
fun ClosedPositionCard(position: TradePosition) {
    val isWin = position.profitLoss >= 0
    val pnlColor = if (isWin) BullishGreen else BearishRed
    val dateFormat = remember { SimpleDateFormat("dd MMM HH:mm", Locale.getDefault()) }
    val formattedTime = remember(position.closeTimestamp) {
        position.closeTimestamp?.let { dateFormat.format(Date(it)) } ?: "Closed"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(SurfaceCard)
            .border(0.5.dp, BorderDark, RoundedCornerShape(10.dp))
            .padding(12.dp)
            .testTag("closed_pos_${position.id}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = position.symbol,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${position.type} ${position.lots} lots",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }

            Text(
                text = (if (isWin) "+$" else "-$") + "%.2f".format(kotlin.math.abs(position.profitLoss)),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = pnlColor
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "In: %.5f  ➜  Out: %.5f".format(position.openPrice, position.closePrice ?: position.openPrice),
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
                fontSize = 10.sp
            )
            Text(
                text = formattedTime,
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
                fontSize = 10.sp
            )
        }
    }
}
