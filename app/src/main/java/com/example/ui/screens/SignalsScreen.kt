package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AiSignal
import com.example.data.model.Direction
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
import com.example.ui.theme.WarningGold
import com.example.ui.viewmodel.ForexViewModel

enum class SignalFilter {
    ALL, HIGH_CONFLUENCE, BUYS_ONLY, SELLS_ONLY
}

@Composable
fun SignalsScreen(
    viewModel: ForexViewModel,
    onInspectSignal: (AiSignal) -> Unit,
    onExecuteTrade: (AiSignal) -> Unit,
    modifier: Modifier = Modifier
) {
    val signals by viewModel.aiSignals.collectAsState()
    val isNewsShieldActive by viewModel.isNewsShieldEnabled.collectAsState()
    var selectedFilter by remember { mutableStateOf(SignalFilter.ALL) }

    val filteredSignals = remember(signals, selectedFilter) {
        when (selectedFilter) {
            SignalFilter.ALL -> signals
            SignalFilter.HIGH_CONFLUENCE -> signals.filter { it.confluenceScore >= 80 }
            SignalFilter.BUYS_ONLY -> signals.filter { it.direction.isBullish }
            SignalFilter.SELLS_ONLY -> signals.filter { !it.direction.isBullish && it.direction != Direction.NEUTRAL }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CanvasBackground)
            .testTag("signals_screen")
    ) {
        // Signals Header: Refresh + News Shield Status
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceDark)
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "AI Signals",
                    tint = ElectricCyan,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "ALGORITHMIC AI FEED",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            IconButton(
                onClick = { viewModel.refreshSignals() },
                modifier = Modifier.testTag("refresh_signals_button")
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = ElectricCyan)
            }
        }

        // News Event Shield Alert if active
        if (isNewsShieldActive) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF241A00))
                    .border(0.5.dp, WarningGold, RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = "News Shield",
                    tint = WarningGold,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "News Shield Active: AI halts / discounts signals during high impact news.",
                    style = MaterialTheme.typography.labelSmall,
                    color = WarningGold
                )
            }
        }

        // Filter Chips Row
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(SignalFilter.entries) { filter ->
                val isSelected = filter == selectedFilter
                val label = when (filter) {
                    SignalFilter.ALL -> "All Signals (${signals.size})"
                    SignalFilter.HIGH_CONFLUENCE -> "High Confluence (80%+)"
                    SignalFilter.BUYS_ONLY -> "Buys Only"
                    SignalFilter.SELLS_ONLY -> "Sells Only"
                }
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedFilter = filter },
                    label = { Text(label, style = MaterialTheme.typography.labelMedium) },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = SurfaceDark,
                        selectedContainerColor = Color(0xFF003844),
                        labelColor = TextSecondary,
                        selectedLabelColor = ElectricCyan
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor = BorderDark,
                        selectedBorderColor = ElectricCyan,
                        enabled = true,
                        selected = isSelected
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("filter_${filter.name.lowercase()}")
                )
            }
        }

        // Signals List
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(filteredSignals, key = { it.id }) { signal ->
                SignalCard(
                    signal = signal,
                    onInspect = { onInspectSignal(signal) },
                    onExecute = { onExecuteTrade(signal) }
                )
            }
        }
    }
}

@Composable
fun SignalCard(
    signal: AiSignal,
    onInspect: () -> Unit,
    onExecute: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dirColor = when (signal.direction) {
        Direction.STRONG_BUY, Direction.BUY -> BullishGreen
        Direction.STRONG_SELL, Direction.SELL -> BearishRed
        Direction.NEUTRAL -> WarningGold
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceCard)
            .border(0.5.dp, BorderDark, RoundedCornerShape(12.dp))
            .padding(14.dp)
            .testTag("signal_card_${signal.pairSymbol.replace('/', '_')}")
    ) {
        // Top: Symbol + Timeframe + Direction Badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = signal.pairSymbol,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "• ${signal.timeframe.label}",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextMuted
                )
            }

            // Direction Pill
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(dirColor.copy(alpha = 0.15f))
                    .border(0.5.dp, dirColor, RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = signal.direction.label,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = dirColor
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Confluence Gauge & R:R Rating Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Confluence Score: ",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Text(
                    text = "${signal.confluenceScore}%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (signal.confluenceScore >= 80) ElectricCyan else TextPrimary
                )
                if (signal.confluenceScore >= 80) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "HIGH PROBABILITY",
                        style = MaterialTheme.typography.labelSmall,
                        color = ElectricCyan,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Text(
                text = "R:R ${signal.riskRewardRatio}",
                style = MaterialTheme.typography.labelSmall,
                color = WarningGold,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Levels Grid: Entry, SL, TP1, TP2
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF131722))
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            LevelText(label = "ENTRY", value = "%.5f".format(signal.entryPrice), color = TextPrimary)
            LevelText(label = "STOP LOSS", value = "%.5f".format(signal.stopLoss), color = BearishRed)
            LevelText(label = "TP 1", value = "%.5f".format(signal.takeProfit1), color = BullishGreen)
            LevelText(label = "TP 2", value = "%.5f".format(signal.takeProfit2), color = ElectricCyan)
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Triggered SMC / Confluence Notes
        Text(
            text = signal.smcZone,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
            fontSize = 11.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Action Buttons: Deep Analysis & Execute Trade
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onInspect,
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
                    .testTag("inspect_signal_${signal.pairSymbol.replace('/', '_')}"),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderDark)
            ) {
                Text(
                    text = "Deep Analysis",
                    style = MaterialTheme.typography.labelMedium,
                    color = ElectricCyan
                )
            }

            Button(
                onClick = onExecute,
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
                    .testTag("execute_signal_${signal.pairSymbol.replace('/', '_')}"),
                colors = ButtonDefaults.buttonColors(containerColor = dirColor),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.FlashOn,
                    contentDescription = "Execute",
                    tint = Color.Black,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Execute",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.Black,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun LevelText(label: String, value: String, color: Color) {
    Column {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = TextMuted, fontSize = 9.sp)
        Text(text = value, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = color)
    }
}
