package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.engine.IndicatorEngine
import com.example.data.engine.SmcConfluenceEngine
import com.example.data.model.CandleStick
import com.example.ui.theme.BearishRed
import com.example.ui.theme.BorderDark
import com.example.ui.theme.BullishGreen
import com.example.ui.theme.CanvasBackground
import com.example.ui.theme.DividerDark
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningGold
import com.example.ui.viewmodel.ChartIndicatorConfig
import com.example.ui.viewmodel.SubChartType
import kotlin.math.max
import kotlin.math.min

@Composable
fun CandlestickChart(
    candles: List<CandleStick>,
    config: ChartIndicatorConfig,
    digits: Int = 5,
    modifier: Modifier = Modifier
) {
    if (candles.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(360.dp)
                .background(CanvasBackground),
            contentAlignment = Alignment.Center
        ) {
            Text("Loading candlestick data...", color = TextMuted)
        }
        return
    }

    var scrollOffset by remember { mutableFloatStateOf(0f) }
    var selectedCandleIndex by remember { mutableIntStateOf(-1) }
    val textMeasurer = rememberTextMeasurer()

    // Precalculate indicators
    val ema9 = remember(candles) { IndicatorEngine.calculateEMA(candles, 9) }
    val ema21 = remember(candles) { IndicatorEngine.calculateEMA(candles, 21) }
    val ema50 = remember(candles) { IndicatorEngine.calculateEMA(candles, 50) }
    val bollinger = remember(candles) { IndicatorEngine.calculateBollingerBands(candles, 20) }
    val supertrend = remember(candles) { IndicatorEngine.calculateSupertrend(candles, 10, 3.0) }
    val rsi = remember(candles) { IndicatorEngine.calculateRSI(candles, 14) }
    val macd = remember(candles) { IndicatorEngine.calculateMACD(candles) }
    val orderBlocks = remember(candles) { SmcConfluenceEngine.detectOrderBlocks(candles, 3) }

    val activeCandle = if (selectedCandleIndex in candles.indices) {
        candles[selectedCandleIndex]
    } else candles.last()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(CanvasBackground)
            .testTag("candlestick_chart_container")
    ) {
        // Top Scrub Bar: O-H-L-C & Volume Details
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(SurfaceCard)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val formatStr = "%.${digits}f"
            Text(
                text = "O: ${formatStr.format(activeCandle.open)}",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "H: ${formatStr.format(activeCandle.high)}",
                style = MaterialTheme.typography.labelSmall,
                color = BullishGreen
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "L: ${formatStr.format(activeCandle.low)}",
                style = MaterialTheme.typography.labelSmall,
                color = BearishRed
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "C: ${formatStr.format(activeCandle.close)}",
                style = MaterialTheme.typography.labelSmall,
                color = if (activeCandle.isBullish) BullishGreen else BearishRed,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "Vol: %.1f".format(activeCandle.volume),
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted
            )
        }

        // Active Indicator Legend
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (config.showEMA9) {
                Text("EMA 9", style = MaterialTheme.typography.labelSmall, color = ElectricCyan, fontSize = 9.sp)
                Spacer(modifier = Modifier.width(6.dp))
            }
            if (config.showEMA21) {
                Text("EMA 21", style = MaterialTheme.typography.labelSmall, color = NeonViolet, fontSize = 9.sp)
                Spacer(modifier = Modifier.width(6.dp))
            }
            if (config.showEMA50) {
                Text("EMA 50", style = MaterialTheme.typography.labelSmall, color = WarningGold, fontSize = 9.sp)
                Spacer(modifier = Modifier.width(6.dp))
            }
            if (config.showBollinger) {
                Text("BB (20,2)", style = MaterialTheme.typography.labelSmall, color = Color(0xFF64B5F6), fontSize = 9.sp)
                Spacer(modifier = Modifier.width(6.dp))
            }
            if (config.showSupertrend) {
                Text("Supertrend (10,3)", style = MaterialTheme.typography.labelSmall, color = BullishGreen, fontSize = 9.sp)
                Spacer(modifier = Modifier.width(6.dp))
            }
            if (config.showSMC) {
                Text("SMC Blocks", style = MaterialTheme.typography.labelSmall, color = Color(0xFFFF80AB), fontSize = 9.sp)
            }
        }

        // Main Candlestick Canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .pointerInput(candles.size) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        scrollOffset += dragAmount.x
                    }
                }
                .pointerInput(candles.size) {
                    detectTapGestures(
                        onPress = { offset ->
                            val width = size.width - 55.dp.toPx()
                            val visibleCount = 28
                            val candleWidth = width / visibleCount
                            val indexFromRight = ((width - offset.x) / candleWidth).toInt()
                            val targetIndex = (candles.size - 1) - indexFromRight
                            if (targetIndex in candles.indices) {
                                selectedCandleIndex = targetIndex
                            }
                        }
                    )
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val priceAreaWidth = size.width - 55.dp.toPx()
                val mainHeight = size.height * 0.78f
                val volumeHeight = size.height * 0.22f

                val visibleCount = 28
                val candleWidth = priceAreaWidth / visibleCount
                val bodyWidth = candleWidth * 0.65f

                // Horizontal shift bounded
                val maxScroll = 0f
                val minScroll = -((candles.size - visibleCount) * candleWidth)
                val clampedScroll = scrollOffset.coerceIn(minScroll, maxScroll)

                val startIndex = max(0, candles.size - visibleCount - (-clampedScroll / candleWidth).toInt())
                val endIndex = min(candles.size, startIndex + visibleCount + 1)
                val visibleCandles = candles.subList(startIndex, endIndex)

                if (visibleCandles.isEmpty()) return@Canvas

                var minPrice = visibleCandles.minOf { it.low }
                var maxPrice = visibleCandles.maxOf { it.high }
                val pricePadding = (maxPrice - minPrice) * 0.08
                minPrice -= pricePadding
                maxPrice += pricePadding
                val priceRange = max(0.00001, maxPrice - minPrice)

                val maxVolume = max(1.0, visibleCandles.maxOf { it.volume })

                fun priceToY(price: Double): Float {
                    val normalized = (price - minPrice) / priceRange
                    return (mainHeight * (1f - normalized)).toFloat()
                }

                // Grid lines & price labels
                val gridSteps = 4
                for (i in 0..gridSteps) {
                    val p = minPrice + (priceRange * (i.toDouble() / gridSteps))
                    val y = priceToY(p)
                    drawLine(
                        color = DividerDark,
                        start = Offset(0f, y),
                        end = Offset(priceAreaWidth, y),
                        strokeWidth = 0.8f
                    )
                    // Draw right axis price
                    val textLayout = textMeasurer.measure(
                        text = "%.${digits}f".format(p),
                        style = TextStyle(fontSize = 9.sp, color = TextMuted)
                    )
                    drawText(
                        textLayoutResult = textLayout,
                        topLeft = Offset(priceAreaWidth + 6.dp.toPx(), y - (textLayout.size.height / 2))
                    )
                }

                // Volume separator line
                drawLine(
                    color = BorderDark,
                    start = Offset(0f, mainHeight),
                    end = Offset(size.width, mainHeight),
                    strokeWidth = 1f
                )

                // Draw SMC Order Block Zones if enabled
                if (config.showSMC) {
                    orderBlocks.forEach { ob ->
                        val topY = priceToY(ob.high)
                        val bottomY = priceToY(ob.low)
                        val blockHeight = max(2f, bottomY - topY)
                        val obColor = if (ob.isBullish) Color(0x2200E5FF) else Color(0x22FF5252)
                        val obBorder = if (ob.isBullish) Color(0x6600E5FF) else Color(0x66FF5252)

                        drawRect(
                            color = obColor,
                            topLeft = Offset(0f, topY),
                            size = Size(priceAreaWidth, blockHeight)
                        )
                        drawLine(
                            color = obBorder,
                            start = Offset(0f, topY),
                            end = Offset(priceAreaWidth, topY),
                            strokeWidth = 1f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
                        )
                        drawLine(
                            color = obBorder,
                            start = Offset(0f, bottomY),
                            end = Offset(priceAreaWidth, bottomY),
                            strokeWidth = 1f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
                        )
                    }
                }

                // Draw Bollinger Bands fill and lines
                if (config.showBollinger) {
                    val upperPath = Path()
                    val lowerPath = Path()
                    var firstUpper = true
                    var firstLower = true

                    for (i in startIndex until endIndex) {
                        val x = (i - startIndex) * candleWidth + (candleWidth / 2f)
                        val u = bollinger.upper[i]
                        val l = bollinger.lower[i]
                        if (u != null) {
                            val uy = priceToY(u)
                            if (firstUpper) { upperPath.moveTo(x, uy); firstUpper = false } else upperPath.lineTo(x, uy)
                        }
                        if (l != null) {
                            val ly = priceToY(l)
                            if (firstLower) { lowerPath.moveTo(x, ly); firstLower = false } else lowerPath.lineTo(x, ly)
                        }
                    }
                    drawPath(upperPath, Color(0x9964B5F6), style = Stroke(1.2f))
                    drawPath(lowerPath, Color(0x9964B5F6), style = Stroke(1.2f))
                }

                // Draw EMAs
                fun drawIndicatorLine(values: List<Double?>, color: Color, strokeWidth: Float = 1.6f) {
                    val path = Path()
                    var isFirst = true
                    for (i in startIndex until endIndex) {
                        val v = values[i] ?: continue
                        val x = (i - startIndex) * candleWidth + (candleWidth / 2f)
                        val y = priceToY(v)
                        if (isFirst) {
                            path.moveTo(x, y)
                            isFirst = false
                        } else {
                            path.lineTo(x, y)
                        }
                    }
                    drawPath(path, color, style = Stroke(strokeWidth))
                }

                if (config.showEMA9) drawIndicatorLine(ema9, ElectricCyan)
                if (config.showEMA21) drawIndicatorLine(ema21, NeonViolet)
                if (config.showEMA50) drawIndicatorLine(ema50, WarningGold)

                // Supertrend
                if (config.showSupertrend) {
                    val stPath = Path()
                    var isFirst = true
                    for (i in startIndex until endIndex) {
                        val v = supertrend.values[i] ?: continue
                        val x = (i - startIndex) * candleWidth + (candleWidth / 2f)
                        val y = priceToY(v)
                        if (isFirst) {
                            stPath.moveTo(x, y)
                            isFirst = false
                        } else {
                            stPath.lineTo(x, y)
                        }
                    }
                    val isBull = supertrend.isBullish.getOrNull(endIndex - 1) ?: true
                    drawPath(stPath, if (isBull) BullishGreen else BearishRed, style = Stroke(2f))
                }

                // Draw Candlesticks & Volume
                for (i in startIndex until endIndex) {
                    val candle = candles[i]
                    val x = (i - startIndex) * candleWidth + (candleWidth / 2f)
                    val color = if (candle.isBullish) BullishGreen else BearishRed

                    // Wick
                    val highY = priceToY(candle.high)
                    val lowY = priceToY(candle.low)
                    drawLine(
                        color = color,
                        start = Offset(x, highY),
                        end = Offset(x, lowY),
                        strokeWidth = 1.2f
                    )

                    // Candle Body
                    val openY = priceToY(candle.open)
                    val closeY = priceToY(candle.close)
                    val topY = min(openY, closeY)
                    val bodyHeight = max(2f, kotlin.math.abs(closeY - openY))

                    drawRect(
                        color = color,
                        topLeft = Offset(x - (bodyWidth / 2f), topY),
                        size = Size(bodyWidth, bodyHeight),
                        style = Fill
                    )

                    // Volume bar
                    val vNormalized = (candle.volume / maxVolume).toFloat()
                    val vBarHeight = vNormalized * (volumeHeight - 8.dp.toPx())
                    val vTopY = size.height - vBarHeight
                    drawRect(
                        color = color.copy(alpha = 0.4f),
                        topLeft = Offset(x - (bodyWidth / 2f), vTopY),
                        size = Size(bodyWidth, vBarHeight)
                    )
                }

                // Current Price Line & Badge on the right
                val lastCandle = candles.last()
                val currentY = priceToY(lastCandle.close)
                val badgeColor = if (lastCandle.isBullish) BullishGreen else BearishRed

                drawLine(
                    color = badgeColor.copy(alpha = 0.7f),
                    start = Offset(0f, currentY),
                    end = Offset(priceAreaWidth, currentY),
                    strokeWidth = 1.2f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f), 0f)
                )

                // Current Price Pill
                val pillWidth = 50.dp.toPx()
                val pillHeight = 18.dp.toPx()
                drawRoundRect(
                    color = badgeColor,
                    topLeft = Offset(priceAreaWidth + 3.dp.toPx(), currentY - (pillHeight / 2f)),
                    size = Size(pillWidth, pillHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
                )

                val pillTextLayout = textMeasurer.measure(
                    text = "%.${digits}f".format(lastCandle.close),
                    style = TextStyle(fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                )
                drawText(
                    textLayoutResult = pillTextLayout,
                    topLeft = Offset(
                        priceAreaWidth + 6.dp.toPx(),
                        currentY - (pillTextLayout.size.height / 2f)
                    )
                )
            }
        }

        // Sub-chart Oscillator Panel (RSI or MACD)
        if (config.activeSubChart != SubChartType.NONE) {
            SubChartPanel(
                candles = candles,
                type = config.activeSubChart,
                rsi = rsi,
                macd = macd,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
            )
        }
    }
}

@Composable
fun SubChartPanel(
    candles: List<CandleStick>,
    type: SubChartType,
    rsi: List<Double?>,
    macd: com.example.data.engine.MACDResult,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(SurfaceCard)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val visibleCount = 28
            val w = size.width - 55.dp.toPx()
            val candleWidth = w / visibleCount
            val startIndex = max(0, candles.size - visibleCount)
            val endIndex = candles.size

            if (type == SubChartType.RSI) {
                // RSI 70 and 30 guide lines
                val y70 = size.height * 0.3f
                val y30 = size.height * 0.7f
                drawLine(
                    color = DividerDark,
                    start = Offset(0f, y70),
                    end = Offset(w, y70),
                    strokeWidth = 1f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                )
                drawLine(
                    color = DividerDark,
                    start = Offset(0f, y30),
                    end = Offset(w, y30),
                    strokeWidth = 1f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                )

                val rsiPath = Path()
                var isFirst = true
                for (i in startIndex until endIndex) {
                    val r = rsi[i] ?: continue
                    val x = (i - startIndex) * candleWidth + (candleWidth / 2f)
                    val y = (size.height * (1f - (r / 100.0))).toFloat()
                    if (isFirst) { rsiPath.moveTo(x, y); isFirst = false } else rsiPath.lineTo(x, y)
                }
                drawPath(rsiPath, ElectricCyan, style = Stroke(1.8f))
            } else if (type == SubChartType.MACD) {
                val midY = size.height * 0.5f
                drawLine(
                    color = DividerDark,
                    start = Offset(0f, midY),
                    end = Offset(w, midY),
                    strokeWidth = 1f
                )

                // Histogram bars
                for (i in startIndex until endIndex) {
                    val h = macd.histogram[i] ?: continue
                    val x = (i - startIndex) * candleWidth + (candleWidth / 2f)
                    val barHeight = (h * 1500.0).toFloat().coerceIn(-size.height * 0.45f, size.height * 0.45f)
                    val color = if (h >= 0) BullishGreen else BearishRed
                    drawLine(
                        color = color,
                        start = Offset(x, midY),
                        end = Offset(x, midY - barHeight),
                        strokeWidth = candleWidth * 0.6f
                    )
                }
            }
        }

        // Sub chart label
        Text(
            text = if (type == SubChartType.RSI) {
                val lastVal = rsi.lastOrNull() ?: 50.0
                "RSI (14): %.1f".format(lastVal)
            } else {
                "MACD (12, 26, 9)"
            },
            style = MaterialTheme.typography.labelSmall,
            color = ElectricCyan,
            fontSize = 9.sp,
            modifier = Modifier.padding(start = 4.dp, top = 2.dp)
        )
    }
}
