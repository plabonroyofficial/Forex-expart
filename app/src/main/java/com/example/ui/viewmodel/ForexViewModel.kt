package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.example.data.local.AppDatabase
import com.example.data.model.AiSignal
import com.example.data.model.CandleStick
import com.example.data.model.EconomicEvent
import com.example.data.model.ForexPair
import com.example.data.model.MarketCategory
import com.example.data.model.PositionStatus
import com.example.data.model.Timeframe
import com.example.data.model.TradePosition
import com.example.data.model.TradeType
import com.example.data.repository.ForexRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class SubChartType {
    NONE, RSI, MACD, STOCHASTIC
}

data class ChartIndicatorConfig(
    val showEMA9: Boolean = true,
    val showEMA21: Boolean = true,
    val showEMA50: Boolean = false,
    val showEMA200: Boolean = false,
    val showBollinger: Boolean = false,
    val showSupertrend: Boolean = true,
    val showIchimoku: Boolean = false,
    val showSMC: Boolean = true,
    val activeSubChart: SubChartType = SubChartType.RSI
)

class ForexViewModel(application: Application) : AndroidViewModel(application) {

    private val db = Room.databaseBuilder(
        application,
        AppDatabase::class.java,
        "forex_pulse.db"
    ).fallbackToDestructiveMigration().build()

    val repository = ForexRepository(db.tradeDao())

    // Active Tab in Navigation Bar (0: Watchlist, 1: Chart, 2: Signals, 3: Simulator, 4: Settings)
    private val _currentTab = MutableStateFlow(0)
    val currentTab: StateFlow<Int> = _currentTab.asStateFlow()

    fun setTab(index: Int) {
        _currentTab.value = index
    }

    // Selected Pair & Timeframe for Chart & Deep Analysis
    private val _selectedPairSymbol = MutableStateFlow("EUR/USD")
    val selectedPairSymbol: StateFlow<String> = _selectedPairSymbol.asStateFlow()

    private val _selectedTimeframe = MutableStateFlow(Timeframe.M15)
    val selectedTimeframe: StateFlow<Timeframe> = _selectedTimeframe.asStateFlow()

    fun selectPair(symbol: String) {
        _selectedPairSymbol.value = symbol
        runAiAnalysis(symbol)
    }

    fun selectTimeframe(tf: Timeframe) {
        _selectedTimeframe.value = tf
        runAiAnalysis(_selectedPairSymbol.value)
    }

    // Watchlist Filters
    private val _selectedCategory = MutableStateFlow(MarketCategory.ALL)
    val selectedCategory: StateFlow<MarketCategory> = _selectedCategory.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    fun setCategory(category: MarketCategory) {
        _selectedCategory.value = category
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    val favoriteSymbols: StateFlow<List<String>> = repository.getFavoriteSymbols()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredPairs: StateFlow<List<ForexPair>> = combine(
        repository.pairs,
        _selectedCategory,
        _searchQuery,
        favoriteSymbols
    ) { pairs, category, query, favs ->
        pairs.map { p ->
            p.copy(isFavorite = favs.contains(p.symbol))
        }.filter { p ->
            val matchesCategory = when (category) {
                MarketCategory.ALL -> true
                else -> p.category == category
            }
            val matchesQuery = query.isBlank() ||
                p.symbol.contains(query, ignoreCase = true) ||
                p.name.contains(query, ignoreCase = true)
            matchesCategory && matchesQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ForexRepository.initialPairs())

    fun toggleFavorite(pair: ForexPair) {
        viewModelScope.launch {
            repository.toggleFavorite(pair.symbol, pair.isFavorite)
        }
    }

    // Chart Indicators
    private val _indicatorConfig = MutableStateFlow(ChartIndicatorConfig())
    val indicatorConfig: StateFlow<ChartIndicatorConfig> = _indicatorConfig.asStateFlow()

    fun updateIndicatorConfig(transform: (ChartIndicatorConfig) -> ChartIndicatorConfig) {
        _indicatorConfig.value = transform(_indicatorConfig.value)
    }

    // Active AI Signal / Analysis Modal
    private val _activeAnalysis = MutableStateFlow<AiSignal?>(null)
    val activeAnalysis: StateFlow<AiSignal?> = _activeAnalysis.asStateFlow()

    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()

    fun runAiAnalysis(symbol: String = _selectedPairSymbol.value) {
        viewModelScope.launch {
            _isAnalyzing.value = true
            kotlinx.coroutines.delay(350) // Realistic instant analysis feedback (<400ms)
            val signal = repository.analyzePair(symbol, _selectedTimeframe.value)
            _activeAnalysis.value = signal
            _isAnalyzing.value = false
        }
    }

    fun dismissAnalysis() {
        _activeAnalysis.value = null
    }

    // Live AI Signals tab
    val aiSignals: StateFlow<List<AiSignal>> = repository.aiSignals

    fun refreshSignals() {
        repository.refreshSignals()
    }

    // Paper Trading Positions
    val allPositions: StateFlow<List<TradePosition>> = repository.getAllPositions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val virtualBalance: StateFlow<Double> = repository.virtualBalance
    val isNewsShieldEnabled: StateFlow<Boolean> = repository.isNewsShieldEnabled
    val economicEvents: StateFlow<List<EconomicEvent>> = repository.economicEvents

    fun toggleNewsShield(enabled: Boolean) {
        repository.toggleNewsShield(enabled)
    }

    fun resetBalance(amount: Double = 10000.0) {
        repository.resetBalance(amount)
    }

    fun openTrade(
        symbol: String,
        type: TradeType,
        lots: Double,
        stopLoss: Double,
        takeProfit: Double,
        notes: String = ""
    ) {
        viewModelScope.launch {
            repository.openPosition(symbol, type, lots, stopLoss, takeProfit, notes)
        }
    }

    fun closeTrade(position: TradePosition) {
        viewModelScope.launch {
            repository.closePosition(position)
        }
    }

    fun clearTradeHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    fun getCurrentCandles(): List<CandleStick> {
        return repository.getCandles(_selectedPairSymbol.value, _selectedTimeframe.value)
    }

    fun getSelectedPair(): ForexPair {
        return repository.pairs.value.find { it.symbol == _selectedPairSymbol.value }
            ?: ForexRepository.initialPairs().first()
    }
}
