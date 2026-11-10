package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Timeframe
import com.example.ui.components.CandlestickChart
import com.example.ui.theme.BorderDark
import com.example.ui.theme.BullishGreen
import com.example.ui.theme.CanvasBackground
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.ForexViewModel
import com.example.ui.viewmodel.SubChartType

@Composable
fun ChartScreen(
    viewModel: ForexViewModel,
    onOpenTradeDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedPairSymbol by viewModel.selectedPairSymbol.collectAsState()
    val selectedTimeframe by viewModel.selectedTimeframe.collectAsState()
    val indicatorConfig by viewModel.indicatorConfig.collectAsState()
    val allPairs by viewModel.repository.pairs.collectAsState()
    val isAnalyzing by viewModel.isAnalyzing.collectAsState()

    val currentPair = allPairs.find { it.symbol == selectedPairSymbol } ?: allPairs.first()
    val candles = remember(selectedPairSymbol, selectedTimeframe, allPairs) {
        viewModel.getCurrentCandles()
    }

    var isPairDropdownOpen by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CanvasBackground)
            .testTag("chart_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 76.dp)
        ) {
            // Control Header: Pair Selector + Current Price + Spread
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceDark)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Pair Selector Dropdown
                Box {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(SurfaceCard)
                            .border(0.5.dp, BorderDark, RoundedCornerShape(8.dp))
                            .clickable { isPairDropdownOpen = true }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .testTag("pair_selector_dropdown"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = currentPair.symbol,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Select Pair",
                            tint = ElectricCyan
                        )
                    }

                    DropdownMenu(
                        expanded = isPairDropdownOpen,
                        onDismissRequest = { isPairDropdownOpen = false },
                        modifier = Modifier
                            .background(SurfaceDark)
                            .border(0.5.dp, BorderDark)
                    ) {
                        allPairs.forEach { pair ->
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(pair.symbol, fontWeight = FontWeight.Bold, color = TextPrimary)
                                        Text(
                                            pair.formatPrice(pair.bid),
                                            color = if (pair.change24h >= 0) BullishGreen else Color(0xFFFF5252)
                                        )
                                    }
                                },
                                onClick = {
                                    viewModel.selectPair(pair.symbol)
                                    isPairDropdownOpen = false
                                },
                                modifier = Modifier.testTag("dropdown_item_${pair.symbol.replace('/', '_')}")
                            )
                        }
                    }
                }

                // Current Market Info
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = currentPair.formatPrice(currentPair.bid),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Spread: ${currentPair.spreadPips}p",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted,
                                fontSize = 10.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = (if (currentPair.change24h >= 0) "+" else "") + "%.2f%%".format(currentPair.change24h),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (currentPair.change24h >= 0) BullishGreen else Color(0xFFFF5252),
                                fontSize = 10.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    // Quick Trade Button
                    Button(
                        onClick = onOpenTradeDialog,
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("chart_quick_trade_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FlashOn,
                            contentDescription = "Trade",
                            tint = Color.Black,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "TRADE",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Black,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Timeframe Selector Chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceDark)
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(Timeframe.entries) { tf ->
                    val isSelected = tf == selectedTimeframe
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSelected) Color(0xFF003844) else SurfaceCard)
                            .border(0.5.dp, if (isSelected) ElectricCyan else BorderDark, RoundedCornerShape(6.dp))
                            .clickable { viewModel.selectTimeframe(tf) }
                            .padding(horizontal = 12.dp, vertical = 5.dp)
                            .testTag("timeframe_${tf.name.lowercase()}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tf.label,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) ElectricCyan else TextSecondary
                        )
                    }
                }
            }

            // Indicator Overlay Selector Bar
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                item {
                    IndicatorChip(
                        label = "EMA 9/21",
                        isActive = indicatorConfig.showEMA9 && indicatorConfig.showEMA21,
                        onClick = {
                            val active = !(indicatorConfig.showEMA9 && indicatorConfig.showEMA21)
                            viewModel.updateIndicatorConfig { it.copy(showEMA9 = active, showEMA21 = active) }
                        }
                    )
                }
                item {
                    IndicatorChip(
                        label = "EMA 50",
                        isActive = indicatorConfig.showEMA50,
                        onClick = {
                            viewModel.updateIndicatorConfig { it.copy(showEMA50 = !it.showEMA50) }
                        }
                    )
                }
                item {
                    IndicatorChip(
                        label = "Bollinger Bands",
                        isActive = indicatorConfig.showBollinger,
                        onClick = {
                            viewModel.updateIndicatorConfig { it.copy(showBollinger = !it.showBollinger) }
                        }
                    )
                }
                item {
                    IndicatorChip(
                        label = "Supertrend",
                        isActive = indicatorConfig.showSupertrend,
                        onClick = {
                            viewModel.updateIndicatorConfig { it.copy(showSupertrend = !it.showSupertrend) }
                        }
                    )
                }
                item {
                    IndicatorChip(
                        label = "SMC Blocks",
                        isActive = indicatorConfig.showSMC,
                        onClick = {
                            viewModel.updateIndicatorConfig { it.copy(showSMC = !it.showSMC) }
                        }
                    )
                }
                item {
                    IndicatorChip(
                        label = if (indicatorConfig.activeSubChart == SubChartType.RSI) "RSI (Active)" else "RSI",
                        isActive = indicatorConfig.activeSubChart == SubChartType.RSI,
                        onClick = {
                            viewModel.updateIndicatorConfig {
                                it.copy(activeSubChart = if (it.activeSubChart == SubChartType.RSI) SubChartType.NONE else SubChartType.RSI)
                            }
                        }
                    )
                }
                item {
                    IndicatorChip(
                        label = if (indicatorConfig.activeSubChart == SubChartType.MACD) "MACD (Active)" else "MACD",
                        isActive = indicatorConfig.activeSubChart == SubChartType.MACD,
                        onClick = {
                            viewModel.updateIndicatorConfig {
                                it.copy(activeSubChart = if (it.activeSubChart == SubChartType.MACD) SubChartType.NONE else SubChartType.MACD)
                            }
                        }
                    )
                }
            }

            // Interactive Candlestick Chart
            CandlestickChart(
                candles = candles,
                config = indicatorConfig,
                digits = currentPair.digits,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            )

            // Market Statistics Panel
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(SurfaceCard)
                    .border(0.5.dp, BorderDark, RoundedCornerShape(10.dp))
                    .padding(12.dp)
            ) {
                Text(
                    text = "24H MARKET TELEMETRY",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StatItem(label = "24h High", value = currentPair.formatPrice(currentPair.high24h), color = BullishGreen)
                    StatItem(label = "24h Low", value = currentPair.formatPrice(currentPair.low24h), color = Color(0xFFFF5252))
                    StatItem(label = "Spread", value = "${currentPair.spreadPips} Pips", color = TextPrimary)
                    StatItem(label = "Precision", value = "${currentPair.digits} Digits", color = TextSecondary)
                }
            }
        }

        // Floating Action Button: "🤖 AI Analyze"
        FloatingActionButton(
            onClick = { viewModel.runAiAnalysis() },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 18.dp, bottom = 18.dp)
                .testTag("ai_analyze_floating_button"),
            containerColor = Color.Transparent,
            shape = RoundedCornerShape(24.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(ElectricCyan, NeonViolet)
                        )
                    )
                    .padding(horizontal = 18.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "AI Analyze",
                        tint = Color.Black,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isAnalyzing) "ANALYZING..." else "🤖 AI ANALYZE",
                        color = Color.Black,
                        fontWeight = FontWeight.ExtraBold,
                        style = MaterialTheme.typography.labelLarge,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun IndicatorChip(
    label: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (isActive) Color(0xFF003844) else SurfaceDark)
            .border(0.5.dp, if (isActive) ElectricCyan else BorderDark, RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
            color = if (isActive) ElectricCyan else TextMuted
        )
    }
}

@Composable
private fun StatItem(label: String, value: String, color: Color) {
    Column {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = TextMuted, fontSize = 10.sp)
        Text(text = value, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = color)
    }
}
