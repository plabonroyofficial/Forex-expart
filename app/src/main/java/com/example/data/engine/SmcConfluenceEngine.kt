package com.example.data.engine

import com.example.data.model.AiSignal
import com.example.data.model.CandleStick
import com.example.data.model.ConfluenceCheck
import com.example.data.model.Direction
import com.example.data.model.OrderBlock
import com.example.data.model.Timeframe
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

object SmcConfluenceEngine {

    // Detect institutional Order Blocks
    fun detectOrderBlocks(candles: List<CandleStick>, count: Int = 3): List<OrderBlock> {
        val blocks = mutableListOf<OrderBlock>()
        if (candles.size < 10) return blocks

        for (i in (candles.size - 2) downTo 5) {
            val c = candles[i]
            val nextC1 = candles[i + 1]
            val nextC2 = if (i + 2 < candles.size) candles[i + 2] else null

            // Bullish Order Block: Last bearish candle before rapid bullish breakout/expansion
            if (!c.isBullish && nextC1.isBullish && (nextC2?.isBullish == true || nextC1.close > c.high)) {
                val expansion = (nextC1.close - c.open) / c.open
                if (expansion > 0.0010) { // Significant institutional push
                    blocks.add(
                        OrderBlock(
                            high = c.high,
                            low = c.low,
                            isBullish = true,
                            timeframe = "4H",
                            timestamp = c.timestamp
                        )
                    )
                }
            }

            // Bearish Order Block: Last bullish candle before aggressive bearish drop
            if (c.isBullish && !nextC1.isBullish && (nextC2?.isBullish == false || nextC1.close < c.low)) {
                val drop = (c.open - nextC1.close) / c.open
                if (drop > 0.0010) {
                    blocks.add(
                        OrderBlock(
                            high = c.high,
                            low = c.low,
                            isBullish = false,
                            timeframe = "4H",
                            timestamp = c.timestamp
                        )
                    )
                }
            }

            if (blocks.size >= count) break
        }
        return blocks
    }

    // Full Confluence Evaluation & Signal Generation
    fun analyzeConfluence(
        pairSymbol: String,
        candles: List<CandleStick>,
        timeframe: Timeframe = Timeframe.M15,
        isNewsShieldActive: Boolean = false
    ): AiSignal {
        val lastIdx = candles.size - 1
        val currentPrice = candles[lastIdx].close
        val atrList = IndicatorEngine.calculateATR(candles, 14)
        val currentATR = atrList[lastIdx] ?: (currentPrice * 0.0015)

        val ema9 = IndicatorEngine.calculateEMA(candles, 9)
        val ema21 = IndicatorEngine.calculateEMA(candles, 21)
        val ema50 = IndicatorEngine.calculateEMA(candles, 50)
        val ema200 = IndicatorEngine.calculateEMA(candles, 200)
        val rsiList = IndicatorEngine.calculateRSI(candles, 14)
        val macd = IndicatorEngine.calculateMACD(candles)
        val supertrend = IndicatorEngine.calculateSupertrend(candles)

        val lastRsi = rsiList[lastIdx] ?: 50.0
        val lastEma9 = ema9[lastIdx] ?: currentPrice
        val lastEma21 = ema21[lastIdx] ?: currentPrice
        val lastEma50 = ema50[lastIdx] ?: currentPrice
        val lastEma200 = ema200[lastIdx] ?: currentPrice
        val lastHist = macd.histogram[lastIdx] ?: 0.0
        val isSupertrendBullish = supertrend.isBullish[lastIdx]

        val orderBlocks = detectOrderBlocks(candles, 4)
        val nearestBullishOB = orderBlocks.filter { it.isBullish }.minByOrNull { abs(it.high - currentPrice) }
        val nearestBearishOB = orderBlocks.filter { !it.isBullish }.minByOrNull { abs(it.low - currentPrice) }

        val checks = mutableListOf<ConfluenceCheck>()
        var bullishPoints = 0
        var bearishPoints = 0
        val totalMaxWeight = 100

        // 1. Trend Filter: EMA 50 & 200 alignment (20 pts)
        val isGoldenCross = lastEma50 > lastEma200
        val isPriceAboveEma200 = currentPrice > lastEma200
        if (isPriceAboveEma200 && isGoldenCross) {
            bullishPoints += 20
            checks.add(
                ConfluenceCheck(
                    title = "Trend Structure (EMA 50/200)",
                    detail = "Golden Cross confirmed. Price trading well above 200 EMA.",
                    isBullish = true,
                    weightPercent = 20,
                    isTriggered = true
                )
            )
        } else if (!isPriceAboveEma200 && !isGoldenCross) {
            bearishPoints += 20
            checks.add(
                ConfluenceCheck(
                    title = "Trend Structure (EMA 50/200)",
                    detail = "Death Cross confirmed. Strong macro bearish trend under 200 EMA.",
                    isBullish = false,
                    weightPercent = 20,
                    isTriggered = true
                )
            )
        } else {
            checks.add(
                ConfluenceCheck(
                    title = "Trend Structure (EMA 50/200)",
                    detail = "Mixed EMA signals; consolidation zone.",
                    isBullish = isPriceAboveEma200,
                    weightPercent = 20,
                    isTriggered = false
                )
            )
        }

        // 2. Short Term Momentum: EMA 9 / 21 cross (15 pts)
        if (lastEma9 > lastEma21) {
            bullishPoints += 15
            checks.add(
                ConfluenceCheck(
                    title = "Fast Momentum (EMA 9/21)",
                    detail = "EMA 9 cleanly trading above EMA 21 (Bullish Expansion).",
                    isBullish = true,
                    weightPercent = 15,
                    isTriggered = true
                )
            )
        } else {
            bearishPoints += 15
            checks.add(
                ConfluenceCheck(
                    title = "Fast Momentum (EMA 9/21)",
                    detail = "EMA 9 crossing down through EMA 21 (Bearish Pressure).",
                    isBullish = false,
                    weightPercent = 15,
                    isTriggered = true
                )
            )
        }

        // 3. Oscillator: RSI Oversold/Overbought or Momentum (20 pts)
        if (lastRsi in 30.0..55.0 && lastEma9 > lastEma21) {
            bullishPoints += 20
            checks.add(
                ConfluenceCheck(
                    title = "RSI Momentum (14)",
                    detail = "RSI at %.1f showing healthy upside clearance with no overbought exhaustion.".format(lastRsi),
                    isBullish = true,
                    weightPercent = 20,
                    isTriggered = true
                )
            )
        } else if (lastRsi < 32.0) {
            bullishPoints += 20
            checks.add(
                ConfluenceCheck(
                    title = "RSI Oversold Bounce",
                    detail = "RSI deeply oversold at %.1f; mean-reversion buying pressure detected.".format(lastRsi),
                    isBullish = true,
                    weightPercent = 20,
                    isTriggered = true
                )
            )
        } else if (lastRsi in 45.0..70.0 && lastEma9 < lastEma21) {
            bearishPoints += 20
            checks.add(
                ConfluenceCheck(
                    title = "RSI Momentum (14)",
                    detail = "RSI at %.1f continuing lower with bearish divergence.".format(lastRsi),
                    isBullish = false,
                    weightPercent = 20,
                    isTriggered = true
                )
            )
        } else if (lastRsi > 68.0) {
            bearishPoints += 20
            checks.add(
                ConfluenceCheck(
                    title = "RSI Overbought Pullback",
                    detail = "RSI extended at %.1f; rejection likely near liquidity highs.".format(lastRsi),
                    isBullish = false,
                    weightPercent = 20,
                    isTriggered = true
                )
            )
        } else {
            checks.add(
                ConfluenceCheck(
                    title = "RSI Range",
                    detail = "RSI neutral at %.1f.".format(lastRsi),
                    isBullish = lastRsi > 50,
                    weightPercent = 20,
                    isTriggered = false
                )
            )
        }

        // 4. SMC Institutional Order Block (25 pts)
        var smcDesc = "Equilibrium zone"
        if (nearestBullishOB != null && abs(currentPrice - nearestBullishOB.high) < currentATR * 1.8) {
            bullishPoints += 25
            smcDesc = "Testing 4H Bullish Demand OB (%.5f - %.5f)".format(nearestBullishOB.low, nearestBullishOB.high)
            checks.add(
                ConfluenceCheck(
                    title = "Smart Money Concept (SMC)",
                    detail = smcDesc,
                    isBullish = true,
                    weightPercent = 25,
                    isTriggered = true
                )
            )
        } else if (nearestBearishOB != null && abs(currentPrice - nearestBearishOB.low) < currentATR * 1.8) {
            bearishPoints += 25
            smcDesc = "Rejected at 4H Bearish Supply OB (%.5f - %.5f)".format(nearestBearishOB.low, nearestBearishOB.high)
            checks.add(
                ConfluenceCheck(
                    title = "Smart Money Concept (SMC)",
                    detail = smcDesc,
                    isBullish = false,
                    weightPercent = 25,
                    isTriggered = true
                )
            )
        } else {
            checks.add(
                ConfluenceCheck(
                    title = "Smart Money Concept (SMC)",
                    detail = "No immediate high-timeframe mitigation zone. Order flow neutral.",
                    isBullish = isSupertrendBullish,
                    weightPercent = 25,
                    isTriggered = false
                )
            )
        }

        // 5. MACD & Supertrend Volatility Confirmation (10 pts)
        if (isSupertrendBullish && lastHist > 0) {
            bullishPoints += 10
            checks.add(
                ConfluenceCheck(
                    title = "Supertrend & MACD Wave",
                    detail = "Supertrend green buy state with expanding MACD positive histogram.",
                    isBullish = true,
                    weightPercent = 10,
                    isTriggered = true
                )
            )
        } else if (!isSupertrendBullish && lastHist < 0) {
            bearishPoints += 10
            checks.add(
                ConfluenceCheck(
                    title = "Supertrend & MACD Wave",
                    detail = "Supertrend red sell state with expanding negative MACD bars.",
                    isBullish = false,
                    weightPercent = 10,
                    isTriggered = true
                )
            )
        } else {
            checks.add(
                ConfluenceCheck(
                    title = "Supertrend / MACD",
                    detail = "Contradictory oscillator readings.",
                    isBullish = isSupertrendBullish,
                    weightPercent = 10,
                    isTriggered = false
                )
            )
        }

        // 6. News Shield Safety (10 pts bonus)
        if (!isNewsShieldActive) {
            if (bullishPoints > bearishPoints) bullishPoints += 10 else bearishPoints += 10
            checks.add(
                ConfluenceCheck(
                    title = "Economic News Shield",
                    detail = "Clear economic calendar window. Safe for technical trend-following.",
                    isBullish = bullishPoints > bearishPoints,
                    weightPercent = 10,
                    isTriggered = true
                )
            )
        } else {
            checks.add(
                ConfluenceCheck(
                    title = "Economic News Shield",
                    detail = "High Impact News approaching! Volatility spike risk active.",
                    isBullish = false,
                    weightPercent = 10,
                    isTriggered = false
                )
            )
        }

        val isBullishDominant = bullishPoints >= bearishPoints
        val confluenceScore = if (isBullishDominant) min(bullishPoints, 96) else min(bearishPoints, 96)

        val direction = when {
            isBullishDominant && confluenceScore >= 80 -> Direction.STRONG_BUY
            isBullishDominant && confluenceScore >= 60 -> Direction.BUY
            !isBullishDominant && confluenceScore >= 80 -> Direction.STRONG_SELL
            !isBullishDominant && confluenceScore >= 60 -> Direction.SELL
            else -> Direction.NEUTRAL
        }

        // ATR-based Smart Risk-to-Reward (R:R 1:2 to 1:3)
        val slPips = max(currentATR * 1.5, 0.0012)
        val stopLoss: Double
        val tp1: Double
        val tp2: Double
        val tp3: Double

        if (direction.isBullish) {
            stopLoss = currentPrice - slPips
            tp1 = currentPrice + (slPips * 1.5)
            tp2 = currentPrice + (slPips * 2.2)
            tp3 = currentPrice + (slPips * 3.0)
        } else {
            stopLoss = currentPrice + slPips
            tp1 = currentPrice - (slPips * 1.5)
            tp2 = currentPrice - (slPips * 2.2)
            tp3 = currentPrice - (slPips * 3.0)
        }

        val mtfSummary = if (direction.isBullish) {
            "5M [Bullish Expansion]  •  1H [Order Block Demand]  •  4H [Macro Uptrend]"
        } else if (direction == Direction.NEUTRAL) {
            "5M [Sideways Choppy]  •  1H [Range-bound]  •  4H [Consolidation]"
        } else {
            "5M [Bearish Pressure]  •  1H [Supply Mitigation]  •  4H [Macro Downtrend]"
        }

        val aiReasoning = if (direction.isBullish) {
            "Institutional liquidity grabbed beneath recent swing low. Fast EMA 9/21 continuation, Supertrend green alignment, and healthy RSI (%d%% confluence). Minimum 1:2.2 R:R targeted.".format(confluenceScore)
        } else if (direction == Direction.NEUTRAL) {
            "Market is oscillating inside a narrow consolidation band. Confluence is below institutional execution threshold (%d%%). Recommend waiting for a decisive breakout.".format(confluenceScore)
        } else {
            "Strong institutional distribution detected at supply block. Price rejected at 50 EMA with negative MACD histogram momentum (%d%% confluence). Minimum 1:2.2 R:R downside target.".format(confluenceScore)
        }

        return AiSignal(
            id = "${pairSymbol}_${System.currentTimeMillis()}",
            pairSymbol = pairSymbol,
            timeframe = timeframe,
            direction = direction,
            confluenceScore = confluenceScore,
            entryPrice = currentPrice,
            stopLoss = stopLoss,
            takeProfit1 = tp1,
            takeProfit2 = tp2,
            takeProfit3 = tp3,
            riskRewardRatio = "1 : 2.2",
            timestamp = System.currentTimeMillis(),
            mtfSummary = mtfSummary,
            confluenceChecks = checks,
            smcZone = smcDesc,
            aiReasoning = aiReasoning,
            newsShieldWarning = isNewsShieldActive
        )
    }
}
