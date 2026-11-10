package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
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
import androidx.compose.ui.window.Dialog
import com.example.data.model.ForexPair
import com.example.data.model.TradeType
import com.example.ui.theme.BearishRed
import com.example.ui.theme.BorderDark
import com.example.ui.theme.BullishGreen
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningGold

@Composable
fun TradeDialog(
    pair: ForexPair,
    initialType: TradeType = TradeType.BUY,
    initialSL: Double? = null,
    initialTP: Double? = null,
    onDismiss: () -> Unit,
    onConfirmTrade: (symbol: String, type: TradeType, lots: Double, sl: Double, tp: Double) -> Unit
) {
    var tradeType by remember { mutableStateOf(initialType) }
    var lotSize by remember { mutableDoubleStateOf(0.10) }

    val currentPrice = if (tradeType == TradeType.BUY) pair.ask else pair.bid
    val defaultPips = if (pair.digits == 3 || pair.digits == 2) 0.50 else 0.0030

    var stopLossText by remember {
        val slVal = initialSL ?: (if (tradeType == TradeType.BUY) currentPrice - defaultPips else currentPrice + defaultPips)
        mutableStateOf(pair.formatPrice(slVal))
    }
    var takeProfitText by remember {
        val tpVal = initialTP ?: (if (tradeType == TradeType.BUY) currentPrice + (defaultPips * 2) else currentPrice - (defaultPips * 2))
        mutableStateOf(pair.formatPrice(tpVal))
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .border(0.5.dp, BorderDark, RoundedCornerShape(16.dp))
                .testTag("trade_order_dialog"),
            color = SurfaceDark
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "ORDER EXECUTION",
                            style = MaterialTheme.typography.labelSmall,
                            color = ElectricCyan,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = pair.symbol,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_trade_dialog")) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // BUY / SELL Toggle Tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(SurfaceCard)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (tradeType == TradeType.BUY) BullishGreen else Color.Transparent)
                            .clickable {
                                tradeType = TradeType.BUY
                                stopLossText = pair.formatPrice(pair.ask - defaultPips)
                                takeProfitText = pair.formatPrice(pair.ask + (defaultPips * 2))
                            }
                            .padding(vertical = 10.dp)
                            .testTag("toggle_buy_order"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "BUY (ASK %.${pair.digits}f)".format(pair.ask),
                            fontWeight = FontWeight.Bold,
                            color = if (tradeType == TradeType.BUY) Color.Black else TextSecondary,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (tradeType == TradeType.SELL) BearishRed else Color.Transparent)
                            .clickable {
                                tradeType = TradeType.SELL
                                stopLossText = pair.formatPrice(pair.bid + defaultPips)
                                takeProfitText = pair.formatPrice(pair.bid - (defaultPips * 2))
                            }
                            .padding(vertical = 10.dp)
                            .testTag("toggle_sell_order"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "SELL (BID %.${pair.digits}f)".format(pair.bid),
                            fontWeight = FontWeight.Bold,
                            color = if (tradeType == TradeType.SELL) Color.White else TextSecondary,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Lot Size Selector
                Text(
                    text = "POSITION SIZE (LOTS)",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = { lotSize = kotlin.math.max(0.01, lotSize - 0.05) },
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(SurfaceCard)
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Decrease Lots", tint = TextPrimary)
                    }

                    Text(
                        text = "%.2f Lots".format(lotSize),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = ElectricCyan
                    )

                    IconButton(
                        onClick = { lotSize = kotlin.math.min(10.0, lotSize + 0.05) },
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(SurfaceCard)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Increase Lots", tint = TextPrimary)
                    }
                }

                // Quick Lot Buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(0.01, 0.05, 0.10, 0.50, 1.00).forEach { size ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (lotSize == size) Color(0xFF003844) else SurfaceCard)
                                .border(0.5.dp, if (lotSize == size) ElectricCyan else BorderDark, RoundedCornerShape(6.dp))
                                .clickable { lotSize = size }
                                .padding(vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "%.2f".format(size),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (lotSize == size) ElectricCyan else TextSecondary,
                                fontSize = 10.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Stop Loss (SL) & Take Profit (TP) Inputs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = stopLossText,
                        onValueChange = { stopLossText = it },
                        modifier = Modifier.weight(1f),
                        label = { Text("Stop Loss (SL)", color = BearishRed, fontSize = 11.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = SurfaceCard,
                            unfocusedContainerColor = SurfaceCard,
                            focusedBorderColor = BearishRed,
                            unfocusedBorderColor = BorderDark,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )

                    OutlinedTextField(
                        value = takeProfitText,
                        onValueChange = { takeProfitText = it },
                        modifier = Modifier.weight(1f),
                        label = { Text("Take Profit (TP)", color = BullishGreen, fontSize = 11.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = SurfaceCard,
                            unfocusedContainerColor = SurfaceCard,
                            focusedBorderColor = BullishGreen,
                            unfocusedBorderColor = BorderDark,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Confirm Order Button
                val buttonColor = if (tradeType == TradeType.BUY) BullishGreen else BearishRed
                Button(
                    onClick = {
                        val sl = stopLossText.toDoubleOrNull() ?: currentPrice
                        val tp = takeProfitText.toDoubleOrNull() ?: currentPrice
                        onConfirmTrade(pair.symbol, tradeType, lotSize, sl, tp)
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("confirm_order_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FlashOn,
                        contentDescription = "Confirm",
                        tint = if (tradeType == TradeType.BUY) Color.Black else Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "CONFIRM ${tradeType.name} (%.2f LOTS)".format(lotSize),
                        fontWeight = FontWeight.Bold,
                        color = if (tradeType == TradeType.BUY) Color.Black else Color.White,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}
