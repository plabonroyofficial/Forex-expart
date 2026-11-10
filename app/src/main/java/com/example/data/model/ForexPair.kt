package com.example.data.model

enum class MarketCategory(val label: String) {
    ALL("All"),
    MAJORS("Majors"),
    MINORS("Minors"),
    EXOTICS("Exotics"),
    COMMODITIES("Commodities"),
    CRYPTO("Crypto")
}

data class ForexPair(
    val symbol: String,
    val name: String,
    val category: MarketCategory,
    val bid: Double,
    val ask: Double,
    val spreadPips: Double,
    val change24h: Double,
    val high24h: Double,
    val low24h: Double,
    val digits: Int = 5,
    val isFavorite: Boolean = false,
    val sparkline: List<Double> = emptyList()
) {
    fun formatPrice(price: Double): String = "%.${digits}f".format(price)
}

enum class Timeframe(val label: String, val minutes: Int) {
    M1("1m", 1),
    M5("5m", 5),
    M15("15m", 15),
    H1("1H", 60),
    H4("4H", 240),
    D1("1D", 1440)
}

data class CandleStick(
    val timestamp: Long,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Double
) {
    val isBullish: Boolean get() = close >= open
    val bodyHeight: Double get() = kotlin.math.abs(close - open)
    val upperWick: Double get() = high - kotlin.math.max(open, close)
    val lowerWick: Double get() = kotlin.math.min(open, close) - low
}
