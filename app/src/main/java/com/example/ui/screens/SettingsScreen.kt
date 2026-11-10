package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.EconomicEvent
import com.example.data.model.NewsImpact
import com.example.ui.theme.BearishRed
import com.example.ui.theme.BorderDark
import com.example.ui.theme.BullishGreen
import com.example.ui.theme.CanvasBackground
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningGold
import com.example.ui.viewmodel.ForexViewModel

@Composable
fun SettingsScreen(
    viewModel: ForexViewModel,
    modifier: Modifier = Modifier
) {
    val isNewsShieldEnabled by viewModel.isNewsShieldEnabled.collectAsState()
    val events by viewModel.economicEvents.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CanvasBackground)
            .verticalScroll(rememberScrollState())
            .padding(14.dp)
            .testTag("settings_screen")
    ) {
        // Section: News Event Shield
        Text(
            text = "ECONOMIC NEWS EVENT SHIELD",
            style = MaterialTheme.typography.labelSmall,
            color = ElectricCyan,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(SurfaceCard)
                .border(0.5.dp, BorderDark, RoundedCornerShape(12.dp))
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = "News Shield",
                        tint = if (isNewsShieldEnabled) WarningGold else TextMuted,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "News Event Shield Filter",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Suspends or warns on signals 45m before high-impact economic releases (NFP, CPI, FOMC)",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }

                Switch(
                    checked = isNewsShieldEnabled,
                    onCheckedChange = { viewModel.toggleNewsShield(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.Black,
                        checkedTrackColor = WarningGold,
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = SurfaceDark
                    ),
                    modifier = Modifier.testTag("news_shield_switch")
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Economic Calendar Feed
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "HIGH & MEDIUM IMPACT CALENDAR",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                fontWeight = FontWeight.Bold
            )
            Icon(Icons.Default.CalendarMonth, contentDescription = "Calendar", tint = TextMuted, modifier = Modifier.size(16.dp))
        }

        Spacer(modifier = Modifier.height(8.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(SurfaceCard)
                .border(0.5.dp, BorderDark, RoundedCornerShape(12.dp))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            events.forEach { event ->
                EconomicEventItem(event = event)
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Section: Latency & Engine Performance
        Text(
            text = "QUANTITATIVE ENGINE TELEMETRY",
            style = MaterialTheme.typography.labelSmall,
            color = ElectricCyan,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(SurfaceCard)
                .border(0.5.dp, BorderDark, RoundedCornerShape(12.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Speed, contentDescription = "Latency", tint = BullishGreen, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Local Edge Calculation Engine", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                }
                Text("< 35 ms", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = BullishGreen)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Bolt, contentDescription = "SMC", tint = ElectricCyan, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("SMC Institutional Order Block Engine", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                }
                Text("Enabled", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = ElectricCyan)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = "Confluence", tint = WarningGold, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Confluence Score Threshold", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                }
                Text("80% Strong / 60% Valid", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = WarningGold)
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Simulator Quick Reset
        Text(
            text = "SIMULATOR ACCOUNT PREFERENCES",
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { viewModel.resetBalance(10000.0) },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = SurfaceCard),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.RestartAlt, contentDescription = "Reset", tint = ElectricCyan, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Reset $10K", color = TextPrimary, style = MaterialTheme.typography.labelMedium)
            }

            Button(
                onClick = { viewModel.resetBalance(50000.0) },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = SurfaceCard),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.RestartAlt, contentDescription = "Reset", tint = ElectricCyan, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Reset $50K", color = TextPrimary, style = MaterialTheme.typography.labelMedium)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // App Footer
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "ForexPulse AI Terminal v1.0.0",
                style = MaterialTheme.typography.labelMedium,
                color = TextMuted
            )
            Text(
                text = "Cyberpunk OLED Dark Theme • Pure Financial Engineering",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
                fontSize = 10.sp
            )
        }

        Spacer(modifier = Modifier.height(30.dp))
    }
}

@Composable
fun EconomicEventItem(event: EconomicEvent) {
    val impactColor = when (event.impact) {
        NewsImpact.HIGH -> BearishRed
        NewsImpact.MEDIUM -> WarningGold
        NewsImpact.LOW -> Color(0xFF64B5F6)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF0F1218))
                    .border(0.5.dp, BorderDark, RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = event.currency,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column {
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary,
                    maxLines = 1
                )
                Text(
                    text = "${event.timeFormatted} • F: ${event.forecast} | P: ${event.previous}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    fontSize = 10.sp
                )
            }
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(impactColor.copy(alpha = 0.2f))
                .border(0.5.dp, impactColor, RoundedCornerShape(4.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = event.impact.name,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = impactColor,
                fontSize = 9.sp
            )
        }
    }
}
