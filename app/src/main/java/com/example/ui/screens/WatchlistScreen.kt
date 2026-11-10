package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ForexPair
import com.example.data.model.MarketCategory
import com.example.ui.theme.BearishRed
import com.example.ui.theme.BearishRedBg
import com.example.ui.theme.BorderDark
import com.example.ui.theme.BullishGreen
import com.example.ui.theme.BullishGreenBg
import com.example.ui.theme.CanvasBackground
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningGold
import com.example.ui.viewmodel.ForexViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchlistScreen(
    viewModel: ForexViewModel,
    onNavigateToChart: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val pairs by viewModel.filteredPairs.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CanvasBackground)
            .testTag("watchlist_screen")
    ) {
        // Search Input
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.setSearchQuery(it) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp)
                .testTag("watchlist_search_input"),
            placeholder = {
                Text(
                    text = "Search pairs (e.g. EUR/USD, Gold, BTC)...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = TextMuted
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.setSearchQuery("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TextMuted)
                    }
                }
            },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = SurfaceDark,
                unfocusedContainerColor = SurfaceDark,
                focusedBorderColor = ElectricCyan,
                unfocusedBorderColor = BorderDark,
                cursorColor = ElectricCyan,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            shape = RoundedCornerShape(10.dp)
        )

        // Category Filter Chips
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(MarketCategory.entries) { category ->
                val isSelected = category == selectedCategory
                FilterChip(
                    selected = isSelected,
                    onClick = { viewModel.setCategory(category) },
                    label = {
                        Text(
                            text = category.label,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = SurfaceDark,
                        selectedContainerColor = Color(0xFF003844),
                        labelColor = TextSecondary,
                        selectedLabelColor = ElectricCyan
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor = BorderDark,
                        selectedBorderColor = ElectricCyan,
                        enabled = true,
                        selected = isSelected
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("chip_${category.name.lowercase()}")
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Pairs List
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(pairs, key = { it.symbol }) { pair ->
                WatchlistPairItem(
                    pair = pair,
                    onSelect = {
                        viewModel.selectPair(pair.symbol)
                        onNavigateToChart(pair.symbol)
                    },
                    onToggleFavorite = { viewModel.toggleFavorite(pair) }
                )
            }
        }
    }
}

@Composable
fun WatchlistPairItem(
    pair: ForexPair,
    onSelect: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isPositive = pair.change24h >= 0
    val trendColor = if (isPositive) BullishGreen else BearishRed
    val trendBg = if (isPositive) BullishGreenBg else BearishRedBg
    val interactionSource = remember { MutableInteractionSource() }

    SurfaceCard(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceCard)
            .border(0.5.dp, BorderDark, RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = androidx.compose.material3.ripple(bounded = true, color = ElectricCyan),
                onClick = onSelect
            )
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .testTag("pair_item_${pair.symbol.replace('/', '_')}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Symbol & Name
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = if (pair.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = "Favorite",
                        tint = if (pair.isFavorite) WarningGold else TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = pair.symbol,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${pair.spreadPips}p",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted,
                            fontSize = 10.sp
                        )
                    }
                    Text(
                        text = pair.name,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        fontSize = 11.sp,
                        maxLines = 1
                    )
                }
            }

            // Mini Sparkline
            SparklineCanvas(
                points = pair.sparkline,
                color = trendColor,
                modifier = Modifier
                    .width(60.dp)
                    .height(28.dp)
                    .padding(horizontal = 4.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Live Prices & 24h Change
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = pair.formatPrice(pair.bid),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(trendBg)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = (if (isPositive) "+" else "") + "%.2f%%".format(pair.change24h),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = trendColor,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
fun SparklineCanvas(
    points: List<Double>,
    color: Color,
    modifier: Modifier = Modifier
) {
    if (points.size < 2) return

    Canvas(modifier = modifier) {
        val minP = points.minOrNull() ?: 0.0
        val maxP = points.maxOrNull() ?: 1.0
        val range = kotlin.math.max(0.00001, maxP - minP)

        val path = Path()
        val stepX = size.width / (points.size - 1)

        points.forEachIndexed { i, p ->
            val x = i * stepX
            val normalized = (p - minP) / range
            val y = (size.height * (1f - normalized)).toFloat()
            if (i == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }

        drawPath(
            path = path,
            color = color,
            style = Stroke(width = 1.5.dp.toPx())
        )
    }
}

@Composable
fun SurfaceCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(modifier = modifier) {
        content()
    }
}
