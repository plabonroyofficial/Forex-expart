package com.example.data.engine

import com.example.data.model.CandleStick
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

data class MACDResult(
    val macd: List<Double?>,
    val signal: List<Double?>,
    val histogram: List<Double?>
)

data class BollingerBandsResult(
    val upper: List<Double?>,
    val middle: List<Double?>,
    val lower: List<Double?>
)

data class SupertrendResult(
    val values: List<Double?>,
    val isBullish: List<Boolean>
)

data class IchimokuResult(
    val tenkan: List<Double?>,
    val kijun: List<Double?>,
    val spanA: List<Double?>,
    val spanB: List<Double?>
)

data class StochasticResult(
    val k: List<Double?>,
    val d: List<Double?>
)

object IndicatorEngine {

    // Simple Moving Average
    fun calculateSMA(candles: List<CandleStick>, period: Int): List<Double?> {
        val n = candles.size
        val result = MutableList<Double?>(n) { null }
        if (n < period || period <= 0) return result

        var sum = 0.0
        for (i in 0 until period) {
            sum += candles[i].close
        }
        result[period - 1] = sum / period

        for (i in period until n) {
            sum += candles[i].close - candles[i - period].close
            result[i] = sum / period
        }
        return result
    }

    // Exponential Moving Average
    fun calculateEMA(candles: List<CandleStick>, period: Int): List<Double?> {
        val n = candles.size
        val result = MutableList<Double?>(n) { null }
        if (n < period || period <= 0) return result

        // Seed with SMA
        var sum = 0.0
        for (i in 0 until period) {
            sum += candles[i].close
        }
        var prevEMA = sum / period
        result[period - 1] = prevEMA

        val multiplier = 2.0 / (period + 1.0)
        for (i in period until n) {
            val close = candles[i].close
            val currentEMA = (close - prevEMA) * multiplier + prevEMA
            result[i] = currentEMA
            prevEMA = currentEMA
        }
        return result
    }

    // Relative Strength Index (Wilder's RSI)
    fun calculateRSI(candles: List<CandleStick>, period: Int = 14): List<Double?> {
        val n = candles.size
        val result = MutableList<Double?>(n) { null }
        if (n <= period) return result

        var avgGain = 0.0
        var avgLoss = 0.0

        for (i in 1..period) {
            val change = candles[i].close - candles[i - 1].close
            if (change > 0) avgGain += change else avgLoss += abs(change)
        }
        avgGain /= period
        avgLoss /= period

        if (avgLoss == 0.0) {
            result[period] = 100.0
        } else {
            val rs = avgGain / avgLoss
            result[period] = 100.0 - (100.0 / (1.0 + rs))
        }

        for (i in (period + 1) until n) {
            val change = candles[i].close - candles[i - 1].close
            val gain = if (change > 0) change else 0.0
            val loss = if (change < 0) abs(change) else 0.0

            avgGain = ((avgGain * (period - 1)) + gain) / period
            avgLoss = ((avgLoss * (period - 1)) + loss) / period

            if (avgLoss == 0.0) {
                result[i] = 100.0
            } else {
                val rs = avgGain / avgLoss
                result[i] = 100.0 - (100.0 / (1.0 + rs))
            }
        }
        return result
    }

    // MACD (12, 26, 9)
    fun calculateMACD(
        candles: List<CandleStick>,
        fastPeriod: Int = 12,
        slowPeriod: Int = 26,
        signalPeriod: Int = 9
    ): MACDResult {
        val n = candles.size
        val fastEMA = calculateEMA(candles, fastPeriod)
        val slowEMA = calculateEMA(candles, slowPeriod)

        val macdLine = MutableList<Double?>(n) { null }
        val macdValuesForSignal = mutableListOf<CandleStick>()
        val macdIndices = mutableListOf<Int>()

        for (i in 0 until n) {
            val f = fastEMA[i]
            val s = slowEMA[i]
            if (f != null && s != null) {
                val valMACD = f - s
                macdLine[i] = valMACD
                macdValuesForSignal.add(CandleStick(candles[i].timestamp, valMACD, valMACD, valMACD, valMACD, 0.0))
                macdIndices.add(i)
            }
        }

        val signalLine = MutableList<Double?>(n) { null }
        val histogram = MutableList<Double?>(n) { null }

        if (macdValuesForSignal.size >= signalPeriod) {
            val signalCalc = calculateEMA(macdValuesForSignal, signalPeriod)
            for (idx in signalCalc.indices) {
                val s = signalCalc[idx]
                val origIdx = macdIndices[idx]
                signalLine[origIdx] = s
                val m = macdLine[origIdx]
                if (m != null && s != null) {
                    histogram[origIdx] = m - s
                }
            }
        }

        return MACDResult(macdLine, signalLine, histogram)
    }

    // Bollinger Bands (20, 2)
    fun calculateBollingerBands(
        candles: List<CandleStick>,
        period: Int = 20,
        multiplier: Double = 2.0
    ): BollingerBandsResult {
        val n = candles.size
        val upper = MutableList<Double?>(n) { null }
        val middle = calculateSMA(candles, period)
        val lower = MutableList<Double?>(n) { null }

        for (i in (period - 1) until n) {
            val mid = middle[i] ?: continue
            var varianceSum = 0.0
            for (j in (i - period + 1)..i) {
                val diff = candles[j].close - mid
                varianceSum += diff * diff
            }
            val stdDev = sqrt(varianceSum / period)
            upper[i] = mid + (multiplier * stdDev)
            lower[i] = mid - (multiplier * stdDev)
        }

        return BollingerBandsResult(upper, middle, lower)
    }

    // Average True Range (ATR)
    fun calculateATR(candles: List<CandleStick>, period: Int = 14): List<Double?> {
        val n = candles.size
        val result = MutableList<Double?>(n) { null }
        if (n <= period) return result

        val trList = mutableListOf<Double>()
        trList.add(candles[0].high - candles[0].low)

        for (i in 1 until n) {
            val c = candles[i]
            val prevClose = candles[i - 1].close
            val tr = max(c.high - c.low, max(abs(c.high - prevClose), abs(c.low - prevClose)))
            trList.add(tr)
        }

        var sum = 0.0
        for (i in 0 until period) {
            sum += trList[i]
        }
        var prevATR = sum / period
        result[period - 1] = prevATR

        for (i in period until n) {
            val currentATR = ((prevATR * (period - 1)) + trList[i]) / period
            result[i] = currentATR
            prevATR = currentATR
        }
        return result
    }

    // Supertrend (10, 3)
    fun calculateSupertrend(
        candles: List<CandleStick>,
        period: Int = 10,
        multiplier: Double = 3.0
    ): SupertrendResult {
        val n = candles.size
        val values = MutableList<Double?>(n) { null }
        val isBullish = MutableList(n) { true }
        if (n <= period) return SupertrendResult(values, isBullish)

        val atr = calculateATR(candles, period)

        var prevUpperBand = 0.0
        var prevLowerBand = 0.0
        var trend = true // true = bullish, false = bearish

        for (i in period until n) {
            val currentATR = atr[i] ?: continue
            val hl2 = (candles[i].high + candles[i].low) / 2.0

            var basicUpperBand = hl2 + (multiplier * currentATR)
            var basicLowerBand = hl2 - (multiplier * currentATR)

            if (i > period) {
                if (candles[i - 1].close > prevLowerBand) {
                    basicLowerBand = max(basicLowerBand, prevLowerBand)
                }
                if (candles[i - 1].close < prevUpperBand) {
                    basicUpperBand = min(basicUpperBand, prevUpperBand)
                }
            }

            if (trend) {
                if (candles[i].close < basicLowerBand) {
                    trend = false
                    values[i] = basicUpperBand
                } else {
                    values[i] = basicLowerBand
                }
            } else {
                if (candles[i].close > basicUpperBand) {
                    trend = true
                    values[i] = basicLowerBand
                } else {
                    values[i] = basicUpperBand
                }
            }

            isBullish[i] = trend
            prevUpperBand = basicUpperBand
            prevLowerBand = basicLowerBand
        }

        return SupertrendResult(values, isBullish)
    }

    // Ichimoku Kinko Hyo
    fun calculateIchimoku(candles: List<CandleStick>): IchimokuResult {
        val n = candles.size
        val tenkan = MutableList<Double?>(n) { null } // 9
        val kijun = MutableList<Double?>(n) { null }   // 26
        val spanA = MutableList<Double?>(n) { null }   // (Tenkan + Kijun) / 2
        val spanB = MutableList<Double?>(n) { null }   // 52

        fun midPrice(period: Int, idx: Int): Double? {
            if (idx < period - 1) return null
            var high = Double.MIN_VALUE
            var low = Double.MAX_VALUE
            for (j in (idx - period + 1)..idx) {
                high = max(high, candles[j].high)
                low = min(low, candles[j].low)
            }
            return (high + low) / 2.0
        }

        for (i in 0 until n) {
            tenkan[i] = midPrice(9, i)
            kijun[i] = midPrice(26, i)
            spanB[i] = midPrice(52, i)

            val t = tenkan[i]
            val k = kijun[i]
            if (t != null && k != null) {
                spanA[i] = (t + k) / 2.0
            }
        }

        return IchimokuResult(tenkan, kijun, spanA, spanB)
    }

    // Stochastic (%K, %D)
    fun calculateStochastic(
        candles: List<CandleStick>,
        kPeriod: Int = 14,
        dPeriod: Int = 3
    ): StochasticResult {
        val n = candles.size
        val rawK = MutableList<Double?>(n) { null }
        val d = MutableList<Double?>(n) { null }

        for (i in (kPeriod - 1) until n) {
            var highest = Double.MIN_VALUE
            var lowest = Double.MAX_VALUE
            for (j in (i - kPeriod + 1)..i) {
                highest = max(highest, candles[j].high)
                lowest = min(lowest, candles[j].low)
            }
            val range = highest - lowest
            rawK[i] = if (range > 0.0) {
                ((candles[i].close - lowest) / range) * 100.0
            } else 50.0
        }

        for (i in (kPeriod + dPeriod - 2) until n) {
            var sum = 0.0
            var count = 0
            for (j in (i - dPeriod + 1)..i) {
                rawK[j]?.let {
                    sum += it
                    count++
                }
            }
            if (count == dPeriod) {
                d[i] = sum / dPeriod
            }
        }

        return StochasticResult(rawK, d)
    }

    // On-Balance Volume (OBV)
    fun calculateOBV(candles: List<CandleStick>): List<Double> {
        val n = candles.size
        val obv = MutableList(n) { 0.0 }
        if (n == 0) return obv

        var currentOBV = candles[0].volume
        obv[0] = currentOBV

        for (i in 1 until n) {
            val c = candles[i].close
            val prevC = candles[i - 1].close
            if (c > prevC) {
                currentOBV += candles[i].volume
            } else if (c < prevC) {
                currentOBV -= candles[i].volume
            }
            obv[i] = currentOBV
        }
        return obv
    }
}
