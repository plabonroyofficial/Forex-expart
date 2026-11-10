package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class Direction(val label: String, val isBullish: Boolean) {
    STRONG_BUY("STRONG BUY", true),
    BUY("BUY", true),
    NEUTRAL("NEUTRAL / WAIT", false),
    SELL("SELL", false),
    STRONG_SELL("STRONG SELL", false)
}

data class OrderBlock(
    val high: Double,
    val low: Double,
    val isBullish: Boolean,
    val timeframe: String,
    val timestamp: Long
)

data class ConfluenceCheck(
    val title: String,
    val detail: String,
    val isBullish: Boolean,
    val weightPercent: Int,
    val isTriggered: Boolean
)

data class AiSignal(
    val id: String,
    val pairSymbol: String,
    val timeframe: Timeframe,
    val direction: Direction,
    val confluenceScore: Int, // 0 - 100%
    val entryPrice: Double,
    val stopLoss: Double,
    val takeProfit1: Double,
    val takeProfit2: Double,
    val takeProfit3: Double,
    val riskRewardRatio: String,
    val timestamp: Long,
    val mtfSummary: String, // Multi-timeframe trend alignment
    val confluenceChecks: List<ConfluenceCheck>,
    val smcZone: String,
    val aiReasoning: String,
    val newsShieldWarning: Boolean = false
)

enum class TradeType {
    BUY, SELL
}

enum class PositionStatus {
    OPEN, CLOSED_TP, CLOSED_SL, CLOSED_MANUAL
}

@Entity(tableName = "trade_positions")
data class TradePosition(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val symbol: String,
    val type: TradeType,
    val lots: Double,
    val openPrice: Double,
    val stopLoss: Double,
    val takeProfit: Double,
    val openTimestamp: Long,
    var closePrice: Double? = null,
    var closeTimestamp: Long? = null,
    var profitLoss: Double = 0.0,
    var status: PositionStatus = PositionStatus.OPEN,
    val notes: String = ""
)

enum class NewsImpact {
    HIGH, MEDIUM, LOW
}

data class EconomicEvent(
    val id: String,
    val title: String,
    val currency: String,
    val impact: NewsImpact,
    val timeFormatted: String,
    val actual: String = "",
    val forecast: String = "",
    val previous: String = "",
    val minutesUntil: Long = 0,
    val isShieldActive: Boolean = false
)
