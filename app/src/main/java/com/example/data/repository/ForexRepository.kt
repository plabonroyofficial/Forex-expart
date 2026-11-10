package com.example.data.repository

import com.example.data.engine.NewsShieldEngine
import com.example.data.engine.SmcConfluenceEngine
import com.example.data.local.FavoritePairEntity
import com.example.data.local.TradeDao
import com.example.data.model.AiSignal
import com.example.data.model.CandleStick
import com.example.data.model.EconomicEvent
import com.example.data.model.ForexPair
import com.example.data.model.MarketCategory
import com.example.data.model.PositionStatus
import com.example.data.model.Timeframe
import com.example.data.model.TradePosition
import com.example.data.model.TradeType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

class ForexRepository(
    private val tradeDao: TradeDao
) {
    private val scope = CoroutineScope(Dispatchers.Default)

    // Virtual Balance
    private val _virtualBalance = MutableStateFlow(10000.0)
    val virtualBalance: StateFlow<Double> = _virtualBalance.asStateFlow()

    // News Event Shield Toggle
    private val _isNewsShieldEnabled = MutableStateFlow(true)
    val isNewsShieldEnabled: StateFlow<Boolean> = _isNewsShieldEnabled.asStateFlow()

    fun toggleNewsShield(enabled: Boolean) {
        _isNewsShieldEnabled.value = enabled
    }

    fun resetBalance(newBalance: Double = 10000.0) {
        _virtualBalance.value = newBalance
    }

    // Pairs State
    private val _pairs = MutableStateFlow<List<ForexPair>>(initialPairs())
    val pairs: StateFlow<List<ForexPair>> = _pairs.asStateFlow()

    // Candle Cache by "Symbol_Timeframe"
    private val candleCache = ConcurrentHashMap<String, MutableList<CandleStick>>()

    // Economic Events
    private val _economicEvents = MutableStateFlow<List<EconomicEvent>>(NewsShieldEngine.getUpcomingEvents())
    val economicEvents: StateFlow<List<EconomicEvent>> = _economicEvents.asStateFlow()

    // Live AI Signals Feed
    private val _aiSignals = MutableStateFlow<List<AiSignal>>(emptyList())
    val aiSignals: StateFlow<List<AiSignal>> = _aiSignals.asStateFlow()

    init {
        // Pre-populate candle cache
        initialPairs().forEach { pair ->
            Timeframe.entries.forEach { tf ->
                val key = "${pair.symbol}_${tf.name}"
                candleCache[key] = generateInitialCandles(pair.bid, tf, 80).toMutableList()
            }
        }

        // Generate initial signals
        refreshSignals()

        // Start live market tick loop
        startTickSimulation()
    }

    fun getFavoriteSymbols(): Flow<List<String>> = tradeDao.getFavorites()

    suspend fun toggleFavorite(symbol: String, isFav: Boolean) {
        if (isFav) {
            tradeDao.removeFavorite(symbol)
        } else {
            tradeDao.addFavorite(FavoritePairEntity(symbol))
        }
    }

    fun getAllPositions(): Flow<List<TradePosition>> = tradeDao.getAllPositions()

    suspend fun openPosition(
        symbol: String,
        type: TradeType,
        lots: Double,
        stopLoss: Double,
        takeProfit: Double,
        notes: String = ""
    ): Long {
        val pair = _pairs.value.find { it.symbol == symbol }
        val currentPrice = if (type == TradeType.BUY) pair?.ask ?: 1.0 else pair?.bid ?: 1.0
        val position = TradePosition(
            symbol = symbol,
            type = type,
            lots = lots,
            openPrice = currentPrice,
            stopLoss = stopLoss,
            takeProfit = takeProfit,
            openTimestamp = System.currentTimeMillis(),
            notes = notes
        )
        return tradeDao.insertPosition(position)
    }

    suspend fun closePosition(position: TradePosition) {
        val pair = _pairs.value.find { it.symbol == position.symbol }
        val currentPrice = if (position.type == TradeType.BUY) pair?.bid ?: position.openPrice else pair?.ask ?: position.openPrice
        val pnl = calculatePnl(position, currentPrice)

        val updated = position.copy(
            closePrice = currentPrice,
            closeTimestamp = System.currentTimeMillis(),
            profitLoss = pnl,
            status = PositionStatus.CLOSED_MANUAL
        )
        tradeDao.updatePosition(updated)
        _virtualBalance.value += pnl
    }

    suspend fun clearHistory() {
        tradeDao.clearAllTrades()
    }

    fun getCandles(symbol: String, timeframe: Timeframe): List<CandleStick> {
        val key = "${symbol}_${timeframe.name}"
        return candleCache.getOrPut(key) {
            val pair = _pairs.value.find { it.symbol == symbol }
            generateInitialCandles(pair?.bid ?: 1.0, timeframe, 80).toMutableList()
        }
    }

    fun analyzePair(symbol: String, timeframe: Timeframe = Timeframe.M15): AiSignal {
        val candles = getCandles(symbol, timeframe)
        val isImminent = _isNewsShieldEnabled.value && NewsShieldEngine.isHighImpactImminent(_economicEvents.value, 45)
        return SmcConfluenceEngine.analyzeConfluence(symbol, candles, timeframe, isImminent)
    }

    fun refreshSignals() {
        val focusPairs = listOf("EUR/USD", "GBP/USD", "USD/JPY", "XAU/USD", "BTC/USD", "AUD/USD", "GBP/JPY")
        val signals = focusPairs.map { symbol ->
            analyzePair(symbol, Timeframe.M15)
        }
        _aiSignals.value = signals.sortedByDescending { it.confluenceScore }
    }

    private fun calculatePnl(pos: TradePosition, currentPrice: Double): Double {
        val pair = _pairs.value.find { it.symbol == pos.symbol }
        val digits = pair?.digits ?: 5
        val pipMultiplier = if (digits == 3 || digits == 2) 100.0 else 10000.0
        val pips = if (pos.type == TradeType.BUY) {
            (currentPrice - pos.openPrice) * pipMultiplier
        } else {
            (pos.openPrice - currentPrice) * pipMultiplier
        }
        // 1 standard lot = $10 per pip on major pairs
        val dollarPerPipPerLot = if (pos.symbol.contains("XAU") || pos.symbol.contains("BTC")) 1.0 else 10.0
        return pips * pos.lots * dollarPerPipPerLot
    }

    private fun startTickSimulation() {
        scope.launch {
            while (true) {
                delay(1200) // Realistic tick interval
                val current = _pairs.value
                val updatedList = current.map { pair ->
                    val pipScale = if (pair.digits == 3 || pair.digits == 2) 0.01 else 0.0001
                    val deltaPips = (Random.nextDouble() - 0.495) * 0.8
                    val newBid = max(0.0001, pair.bid + (deltaPips * pipScale))
                    val newAsk = newBid + (pair.spreadPips * pipScale)

                    // Update live candlestick for this pair
                    Timeframe.entries.forEach { tf ->
                        val key = "${pair.symbol}_${tf.name}"
                        val candles = candleCache[key]
                        if (!candles.isNullOrEmpty()) {
                            val lastIdx = candles.size - 1
                            val last = candles[lastIdx]
                            val newHigh = max(last.high, newBid)
                            val newLow = min(last.low, newBid)
                            candles[lastIdx] = last.copy(
                                high = newHigh,
                                low = newLow,
                                close = newBid,
                                volume = last.volume + Random.nextDouble(1.0, 8.0)
                            )
                        }
                    }

                    // Update sparkline
                    val newSpark = if (pair.sparkline.isNotEmpty()) {
                        pair.sparkline.drop(1) + newBid
                    } else listOf(newBid)

                    val changeDelta = ((newBid - pair.open24h()) / pair.open24h()) * 100.0

                    pair.copy(
                        bid = newBid,
                        ask = newAsk,
                        change24h = changeDelta,
                        high24h = max(pair.high24h, newBid),
                        low24h = min(pair.low24h, newBid),
                        sparkline = newSpark
                    )
                }
                _pairs.value = updatedList
            }
        }
    }

    private fun ForexPair.open24h(): Double = bid / (1.0 + (change24h / 100.0))

    companion object {
        fun initialPairs(): List<ForexPair> = listOf(
            // Majors
            ForexPair("EUR/USD", "Euro / US Dollar", MarketCategory.MAJORS, 1.08450, 1.08458, 0.8, 0.32, 1.08720, 1.08180, 5, true, makeSparkline(1.0845)),
            ForexPair("GBP/USD", "British Pound / US Dollar", MarketCategory.MAJORS, 1.29640, 1.29652, 1.2, -0.24, 1.30100, 1.29400, 5, true, makeSparkline(1.2964)),
            ForexPair("USD/JPY", "US Dollar / Japanese Yen", MarketCategory.MAJORS, 154.320, 154.332, 1.2, 0.58, 154.800, 153.750, 3, true, makeSparkline(154.32)),
            ForexPair("USD/CHF", "US Dollar / Swiss Franc", MarketCategory.MAJORS, 0.87650, 0.87664, 1.4, -0.15, 0.88000, 0.87450, 5, false, makeSparkline(0.8765)),
            ForexPair("AUD/USD", "Australian Dollar / US Dollar", MarketCategory.MAJORS, 0.65820, 0.65831, 1.1, 0.42, 0.66100, 0.65500, 5, true, makeSparkline(0.6582)),
            ForexPair("USD/CAD", "US Dollar / Canadian Dollar", MarketCategory.MAJORS, 1.38920, 1.38934, 1.4, -0.18, 1.39300, 1.38600, 5, false, makeSparkline(1.3892)),
            ForexPair("NZD/USD", "New Zealand Dollar / US Dollar", MarketCategory.MAJORS, 0.59840, 0.59855, 1.5, 0.12, 0.60200, 0.59600, 5, false, makeSparkline(0.5984)),

            // Minors
            ForexPair("EUR/GBP", "Euro / British Pound", MarketCategory.MINORS, 0.83650, 0.83664, 1.4, 0.18, 0.83900, 0.83400, 5, false, makeSparkline(0.8365)),
            ForexPair("EUR/JPY", "Euro / Japanese Yen", MarketCategory.MINORS, 167.380, 167.398, 1.8, 0.84, 167.900, 166.400, 3, false, makeSparkline(167.38)),
            ForexPair("GBP/JPY", "British Pound / Japanese Yen", MarketCategory.MINORS, 200.050, 200.072, 2.2, 0.45, 200.700, 199.100, 3, true, makeSparkline(200.05)),
            ForexPair("AUD/JPY", "Australian Dollar / Japanese Yen", MarketCategory.MINORS, 101.580, 101.597, 1.7, 0.62, 102.100, 100.900, 3, false, makeSparkline(101.58)),
            ForexPair("EUR/AUD", "Euro / Australian Dollar", MarketCategory.MINORS, 1.64750, 1.64770, 2.0, -0.22, 1.65300, 1.64200, 5, false, makeSparkline(1.6475)),

            // Commodities
            ForexPair("XAU/USD", "Gold Spot / US Dollar", MarketCategory.COMMODITIES, 2748.50, 2748.85, 3.5, 1.15, 2762.00, 2724.00, 2, true, makeSparkline(2748.5)),
            ForexPair("XTI/USD", "Crude Oil WTI", MarketCategory.COMMODITIES, 71.40, 71.44, 4.0, -1.82, 73.10, 70.80, 2, false, makeSparkline(71.40)),

            // Crypto
            ForexPair("BTC/USD", "Bitcoin / US Dollar", MarketCategory.CRYPTO, 89450.00, 89462.00, 12.0, 3.42, 90200.00, 86800.00, 2, true, makeSparkline(89450.0)),
            ForexPair("ETH/USD", "Ethereum / US Dollar", MarketCategory.CRYPTO, 3280.50, 3281.80, 1.3, 2.18, 3350.00, 3190.00, 2, false, makeSparkline(3280.5)),
            ForexPair("SOL/USD", "Solana / US Dollar", MarketCategory.CRYPTO, 218.40, 218.75, 3.5, 5.84, 224.00, 206.00, 2, false, makeSparkline(218.4)),

            // Exotics
            ForexPair("USD/SGD", "US Dollar / Singapore Dollar", MarketCategory.EXOTICS, 1.33400, 1.33425, 2.5, -0.08, 1.33700, 1.33150, 5, false, makeSparkline(1.3340)),
            ForexPair("USD/ZAR", "US Dollar / South African Rand", MarketCategory.EXOTICS, 17.8500, 17.8580, 8.0, 0.72, 18.0500, 17.7200, 4, false, makeSparkline(17.850)),
            ForexPair("USD/TRY", "US Dollar / Turkish Lira", MarketCategory.EXOTICS, 34.6200, 34.6350, 15.0, 0.12, 34.7500, 34.5000, 4, false, makeSparkline(34.620))
        )

        private fun makeSparkline(base: Double): List<Double> {
            val list = mutableListOf<Double>()
            var p = base * 0.994
            for (i in 0 until 16) {
                p += (Random.nextDouble() - 0.48) * (base * 0.001)
                list.add(p)
            }
            list.add(base)
            return list
        }

        private fun generateInitialCandles(basePrice: Double, tf: Timeframe, count: Int = 80): List<CandleStick> {
            val list = mutableListOf<CandleStick>()
            val intervalMs = tf.minutes * 60 * 1000L
            val startTime = System.currentTimeMillis() - (count * intervalMs)
            var prevClose = basePrice * (1.0 - (Random.nextDouble(0.005, 0.015)))

            val volatility = when (tf) {
                Timeframe.M1 -> basePrice * 0.0003
                Timeframe.M5 -> basePrice * 0.0006
                Timeframe.M15 -> basePrice * 0.0010
                Timeframe.H1 -> basePrice * 0.0025
                Timeframe.H4 -> basePrice * 0.0060
                Timeframe.D1 -> basePrice * 0.0120
            }

            for (i in 0 until count) {
                val open = prevClose + (Random.nextDouble() - 0.5) * (volatility * 0.3)
                val trendBias = if (i > count - 15) 0.08 else 0.0 // slight recent trend
                val delta = (Random.nextDouble() - (0.46 - trendBias)) * volatility
                val close = open + delta
                val high = max(open, close) + Random.nextDouble(0.0, volatility * 0.6)
                val low = min(open, close) - Random.nextDouble(0.0, volatility * 0.6)
                val volume = Random.nextDouble(20.0, 150.0) * (if (tf.minutes >= 60) 5.0 else 1.0)

                list.add(
                    CandleStick(
                        timestamp = startTime + (i * intervalMs),
                        open = open,
                        high = high,
                        low = low,
                        close = close,
                        volume = volume
                    )
                )
                prevClose = close
            }
            return list
        }
    }
}
