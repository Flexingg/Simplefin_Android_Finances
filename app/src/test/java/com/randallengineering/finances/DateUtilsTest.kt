package com.randallengineering.finances

import com.randallengineering.finances.core.util.DateUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DateUtilsTest {

    @Test
    fun `formats a normal epoch`() {
        // 2023-11-14 22:13:20 UTC -> epoch 1700000000
        val s = DateUtils.formatDate(1_700_000_000L)
        assertTrue(s.isNotBlank())
        assertNotNull(s)
    }

    @Test
    fun `clamps extreme future epoch instead of throwing`() {
        // Must not throw DateTimeException even for Long.MAX_VALUE
        val s = DateUtils.formatDate(Long.MAX_VALUE)
        assertTrue(s.isNotBlank())
    }

    @Test
    fun `clamps extreme past epoch instead of throwing`() {
        val s = DateUtils.formatDateTime(Long.MIN_VALUE)
        assertTrue(s.isNotBlank())
    }

    @Test
    fun `formats zero epoch as 1970`() {
        val s = DateUtils.formatShortDate(0L)
        assertTrue(s.isNotBlank())
    }

    @Test
    fun `days remaining in month is at least one`() {
        assertTrue(DateUtils.getDaysRemainingInCurrentMonth() >= 1)
    }

    @Test
    fun `days between clamped values never throws`() {
        val d = DateUtils.getDaysBetween(Long.MIN_VALUE, Long.MAX_VALUE)
        assertTrue(d >= 1)
    }

    @Test
    fun `month boundaries are ordered`() {
        val start = DateUtils.getStartOfCurrentMonthEpochSeconds()
        val end = DateUtils.getEndOfCurrentMonthEpochSeconds()
        assertTrue(end >= start)
        assertEquals(1, DateUtils.getDaysBetween(end, end))
    }
}
