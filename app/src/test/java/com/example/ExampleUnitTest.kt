package com.example

import com.example.data.engine.IndicatorEngine
import com.example.data.engine.NewsShieldEngine
import com.example.data.engine.SmcConfluenceEngine
import com.example.data.model.CandleStick
import com.example.data.model.Direction
import com.example.data.model.EconomicEvent
import com.example.data.model.NewsImpact
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    private fun generateSampleCandles(count: Int = 50, startPrice: Double = 1.1000): List<CandleStick> {
        val list = mutableListOf<CandleStick>()
        var price = startPrice
        for (i in 0 until count) {
            val open = price
            val close = open + (if (i % 2 == 0) 0.0010 else -0.0006)
            val high = maxOf(open, close) + 0.0005
            val low = minOf(open, close) - 0.0005
            list.add(CandleStick(i * 60000L, open, high, low, close, 100.0))
            price = close
        }
        return list
    }

    @Test
    fun testIndicatorEngine_EMA_and_RSI() {
        val candles = generateSampleCandles(40)
        val ema9 = IndicatorEngine.calculateEMA(candles, 9)
        assertEquals(candles.size, ema9.size)
        assertNotNull(ema9.last())

        val rsi14 = IndicatorEngine.calculateRSI(candles, 14)
        assertEquals(candles.size, rsi14.size)
        val lastRsi = rsi14.last()
        assertNotNull(lastRsi)
        assertTrue(lastRsi!! in 0.0..100.0)
    }

    @Test
    fun testIndicatorEngine_MACD_and_Bollinger() {
        val candles = generateSampleCandles(40)
        val macd = IndicatorEngine.calculateMACD(candles)
        assertEquals(candles.size, macd.macd.size)
        assertNotNull(macd.macd.last())

        val bb = IndicatorEngine.calculateBollingerBands(candles, 20)
        assertNotNull(bb.upper.last())
        assertNotNull(bb.lower.last())
        assertTrue(bb.upper.last()!! >= bb.lower.last()!!)
    }

    @Test
    fun testSmcConfluenceEngine_AnalyzeSignal() {
        val candles = generateSampleCandles(40)
        val signal = SmcConfluenceEngine.analyzeConfluence("EUR/USD", candles)
        assertNotNull(signal)
        assertEquals("EUR/USD", signal.pairSymbol)
        assertTrue(signal.confluenceScore in 0..100)
        assertTrue(signal.stopLoss != signal.entryPrice)
        assertTrue(signal.confluenceChecks.isNotEmpty())
    }

    @Test
    fun testNewsShieldEngine() {
        val events = listOf(
            EconomicEvent(
                id = "nfp",
                title = "NFP",
                currency = "USD",
                impact = NewsImpact.HIGH,
                timeFormatted = "Today",
                minutesUntil = 25
            )
        )
        val isImminent = NewsShieldEngine.isHighImpactImminent(events, 45)
        assertTrue(isImminent)
    }
}
