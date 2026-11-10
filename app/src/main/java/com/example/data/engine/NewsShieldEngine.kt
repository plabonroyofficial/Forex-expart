package com.example.data.engine

import com.example.data.model.EconomicEvent
import com.example.data.model.NewsImpact

object NewsShieldEngine {

    fun getUpcomingEvents(): List<EconomicEvent> {
        val now = System.currentTimeMillis()
        return listOf(
            EconomicEvent(
                id = "nfp_us",
                title = "Non-Farm Payrolls (NFP)",
                currency = "USD",
                impact = NewsImpact.HIGH,
                timeFormatted = "Today 13:30 GMT",
                forecast = "185K",
                previous = "175K",
                actual = "Pending",
                minutesUntil = 42,
                isShieldActive = false
            ),
            EconomicEvent(
                id = "cpi_us",
                title = "Core CPI m/m",
                currency = "USD",
                impact = NewsImpact.HIGH,
                timeFormatted = "Today 15:30 GMT",
                forecast = "0.3%",
                previous = "0.3%",
                actual = "Pending",
                minutesUntil = 162,
                isShieldActive = false
            ),
            EconomicEvent(
                id = "fomc_rate",
                title = "FOMC Fed Funds Rate Decision",
                currency = "USD",
                impact = NewsImpact.HIGH,
                timeFormatted = "Tomorrow 19:00 GMT",
                forecast = "5.25%",
                previous = "5.25%",
                actual = "Pending",
                minutesUntil = 1440,
                isShieldActive = false
            ),
            EconomicEvent(
                id = "ecb_press",
                title = "ECB Monetary Policy Statement",
                currency = "EUR",
                impact = NewsImpact.HIGH,
                timeFormatted = "Tomorrow 12:45 GMT",
                forecast = "3.75%",
                previous = "4.00%",
                actual = "Pending",
                minutesUntil = 1200,
                isShieldActive = false
            ),
            EconomicEvent(
                id = "uk_gdp",
                title = "GDP m/m",
                currency = "GBP",
                impact = NewsImpact.MEDIUM,
                timeFormatted = "Friday 07:00 GMT",
                forecast = "0.2%",
                previous = "-0.1%",
                actual = "Pending",
                minutesUntil = 2800,
                isShieldActive = false
            ),
            EconomicEvent(
                id = "crude_inventories",
                title = "Crude Oil Inventories (EIA)",
                currency = "USD",
                impact = NewsImpact.MEDIUM,
                timeFormatted = "Wednesday 15:30 GMT",
                forecast = "-1.2M",
                previous = "-2.5M",
                actual = "Pending",
                minutesUntil = 3400,
                isShieldActive = false
            )
        )
    }

    fun isHighImpactImminent(events: List<EconomicEvent>, thresholdMinutes: Long = 60): Boolean {
        return events.any { it.impact == NewsImpact.HIGH && it.minutesUntil in 0..thresholdMinutes }
    }
}
