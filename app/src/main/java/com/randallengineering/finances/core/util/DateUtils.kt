package com.randallengineering.finances.core.util

import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

object DateUtils {

    private val dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy")
    private val shortDateFormatter = DateTimeFormatter.ofPattern("MMM dd")
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy hh:mm a")

    fun getDaysRemainingInCurrentMonth(zoneId: ZoneId = ZoneId.systemDefault()): Int {
        val today = LocalDate.now(zoneId)
        val yearMonth = YearMonth.from(today)
        val totalDays = yearMonth.lengthOfMonth()
        val daysPassed = today.dayOfMonth
        // Include today in remaining days
        return (totalDays - daysPassed + 1).coerceAtLeast(1)
    }

    fun getTotalDaysInCurrentMonth(zoneId: ZoneId = ZoneId.systemDefault()): Int {
        return YearMonth.now(zoneId).lengthOfMonth()
    }

    fun getCurrentDayOfMonth(zoneId: ZoneId = ZoneId.systemDefault()): Int {
        return LocalDate.now(zoneId).dayOfMonth
    }

    fun getStartOfCurrentMonthEpochSeconds(zoneId: ZoneId = ZoneId.systemDefault()): Long {
        val today = LocalDate.now(zoneId)
        val firstDay = today.withDayOfMonth(1)
        return firstDay.atStartOfDay(zoneId).toEpochSecond()
    }

    fun getEndOfCurrentMonthEpochSeconds(zoneId: ZoneId = ZoneId.systemDefault()): Long {
        val today = LocalDate.now(zoneId)
        val lastDay = today.withDayOfMonth(today.lengthOfMonth())
        return lastDay.atTime(23, 59, 59).atZone(zoneId).toEpochSecond()
    }

    fun getDaysBetween(startEpochSeconds: Long, endEpochSeconds: Long): Long {
        val start = Instant.ofEpochSecond(startEpochSeconds).atZone(ZoneId.systemDefault()).toLocalDate()
        val end = Instant.ofEpochSecond(endEpochSeconds).atZone(ZoneId.systemDefault()).toLocalDate()
        return ChronoUnit.DAYS.between(start, end).coerceAtLeast(1)
    }

    fun getMonthsBetween(startEpochSeconds: Long, endEpochSeconds: Long): Double {
        val days = getDaysBetween(startEpochSeconds, endEpochSeconds)
        return (days / 30.4375).coerceAtLeast(0.1)
    }

    fun formatDate(epochSeconds: Long): String {
        return Instant.ofEpochSecond(epochSeconds)
            .atZone(ZoneId.systemDefault())
            .format(dateFormatter)
    }

    fun formatShortDate(epochSeconds: Long): String {
        return Instant.ofEpochSecond(epochSeconds)
            .atZone(ZoneId.systemDefault())
            .format(shortDateFormatter)
    }

    fun formatDateTime(epochSeconds: Long): String {
        return Instant.ofEpochSecond(epochSeconds)
            .atZone(ZoneId.systemDefault())
            .format(dateTimeFormatter)
    }

    fun getSimpleFinSyncStartEpochSeconds(daysBack: Int = 90, overlapDays: Int = 5): Long {
        val today = LocalDate.now()
        val targetDate = today.minusDays((daysBack + overlapDays).toLong())
        return targetDate.atStartOfDay(ZoneId.systemDefault()).toEpochSecond()
    }
}
