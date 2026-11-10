package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.AiSignal
import com.example.data.model.Direction
import com.example.data.model.ForexPair
import com.example.data.model.TradeType
import com.example.ui.components.AiAnalysisBottomSheet
import com.example.ui.components.BottomNavBar
import com.example.ui.components.TopBar
import com.example.ui.components.TradeDialog
import com.example.ui.screens.ChartScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SignalsScreen
import com.example.ui.screens.SimulatorScreen
import com.example.ui.screens.WatchlistScreen
import com.example.ui.theme.ForexPulseTheme
import com.example.ui.viewmodel.ForexViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ForexPulseTheme {
                val viewModel: ForexViewModel = viewModel()
                ForexAppContent(viewModel)
            }
        }
    }
}

@Composable
fun ForexAppContent(viewModel: ForexViewModel) {
    val currentTab by viewModel.currentTab.collectAsState()
    val virtualBalance by viewModel.virtualBalance.collectAsState()
    val isNewsShieldActive by viewModel.isNewsShieldEnabled.collectAsState()
    val activeAnalysis by viewModel.activeAnalysis.collectAsState()
    val isAnalyzing by viewModel.isAnalyzing.collectAsState()
    val allPairs by viewModel.repository.pairs.collectAsState()

    var tradeDialogPair by remember { mutableStateOf<ForexPair?>(null) }
    var tradeDialogType by remember { mutableStateOf(TradeType.BUY) }
    var tradeDialogSL by remember { mutableStateOf<Double?>(null) }
    var tradeDialogTP by remember { mutableStateOf<Double?>(null) }

    // Android Back Handler: return to Tab 0 if on another tab
    BackHandler(enabled = currentTab != 0) {
        viewModel.setTab(0)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopBar(
                virtualBalance = virtualBalance,
                isNewsShieldActive = isNewsShieldActive
            )
        },
        bottomBar = {
            BottomNavBar(
                selectedTab = currentTab,
                onTabSelected = { viewModel.setTab(it) }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                0 -> WatchlistScreen(
                    viewModel = viewModel,
                    onNavigateToChart = { symbol ->
                        viewModel.selectPair(symbol)
                        viewModel.setTab(1)
                    }
                )
                1 -> ChartScreen(
                    viewModel = viewModel,
                    onOpenTradeDialog = {
                        val current = viewModel.getSelectedPair()
                        tradeDialogPair = current
                        tradeDialogType = TradeType.BUY
                        tradeDialogSL = null
                        tradeDialogTP = null
                    }
                )
                2 -> SignalsScreen(
                    viewModel = viewModel,
                    onInspectSignal = { signal ->
                        viewModel.selectPair(signal.pairSymbol)
                        viewModel.runAiAnalysis(signal.pairSymbol)
                    },
                    onExecuteTrade = { signal ->
                        val pair = allPairs.find { it.symbol == signal.pairSymbol } ?: viewModel.getSelectedPair()
                        tradeDialogPair = pair
                        tradeDialogType = if (signal.direction.isBullish) TradeType.BUY else TradeType.SELL
                        tradeDialogSL = signal.stopLoss
                        tradeDialogTP = signal.takeProfit1
                    }
                )
                3 -> SimulatorScreen(
                    viewModel = viewModel,
                    onOpenTradeDialog = {
                        val current = viewModel.getSelectedPair()
                        tradeDialogPair = current
                        tradeDialogType = TradeType.BUY
                        tradeDialogSL = null
                        tradeDialogTP = null
                    }
                )
                4 -> SettingsScreen(
                    viewModel = viewModel
                )
            }
        }
    }

    // AI Analysis Bottom Sheet
    AiAnalysisBottomSheet(
        signal = activeAnalysis,
        isAnalyzing = isAnalyzing,
        onDismiss = { viewModel.dismissAnalysis() },
        onExecuteTrade = { signal ->
            val pair = allPairs.find { it.symbol == signal.pairSymbol } ?: viewModel.getSelectedPair()
            tradeDialogPair = pair
            tradeDialogType = if (signal.direction.isBullish) TradeType.BUY else TradeType.SELL
            tradeDialogSL = signal.stopLoss
            tradeDialogTP = signal.takeProfit1
        }
    )

    // Trade Dialog
    tradeDialogPair?.let { pair ->
        TradeDialog(
            pair = pair,
            initialType = tradeDialogType,
            initialSL = tradeDialogSL,
            initialTP = tradeDialogTP,
            onDismiss = { tradeDialogPair = null },
            onConfirmTrade = { symbol, type, lots, sl, tp ->
                viewModel.openTrade(symbol, type, lots, sl, tp, notes = "AI Execution")
                viewModel.setTab(3) // Jump to simulator to see active position
            }
        )
    }
}
