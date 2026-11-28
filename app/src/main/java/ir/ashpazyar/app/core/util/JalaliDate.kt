package ir.ashpazyar.app.core.util

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

data class JalaliDate(val year: Int, val month: Int, val day: Int) {
    val monthName: String get() = monthNames[(month - 1).coerceIn(0, 11)]
    val shortLabel: String get() = PersianText.faDigits(day.toString()) + " " + monthName
    val fullLabel: String get() = PersianText.faDigits(day.toString()) + " " + monthName + " " + PersianText.faDigits(year.toString())

    companion object {
        val monthNames = listOf("فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور", "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند")
    }
}

object JalaliCalendar {
    fun from(epochMillis: Long, zoneId: ZoneId = ZoneId.systemDefault()): JalaliDate =
        from(Instant.ofEpochMilli(epochMillis).atZone(zoneId).toLocalDate())

    fun from(date: LocalDate): JalaliDate {
        val gy = date.year
        val gm = date.monthValue
        val gd = date.dayOfMonth
        val gDaysInMonth = intArrayOf(0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334)
        val gy2 = if (gm > 2) gy + 1 else gy
        var days = 355666 + (365 * gy) + ((gy2 + 3) / 4) - ((gy2 + 99) / 100) +
            ((gy2 + 399) / 400) + gd + gDaysInMonth[gm - 1]
        var jy = -1595 + (33 * (days / 12053))
        days %= 12053
        jy += 4 * (days / 1461)
        days %= 1461
        if (days > 365) {
            jy += (days - 1) / 365
            days = (days - 1) % 365
        }
        val jm: Int
        val jd: Int
        if (days < 186) {
            jm = 1 + days / 31
            jd = 1 + days % 31
        } else {
            jm = 7 + (days - 186) / 30
            jd = 1 + (days - 186) % 30
        }
        return JalaliDate(jy, jm, jd)
    }

    fun today(): JalaliDate = from(LocalDate.now())

    fun format(epochMillis: Long, withWeekday: Boolean = false): String {
        val date = from(epochMillis)
        val base = date.fullLabel
        if (!withWeekday) return base
        val weekday = weekdayName(epochMillis)
        return weekday + "، " + base
    }

    fun format(date: LocalDate): String = from(date).fullLabel

    fun short(epochMillis: Long): String = from(epochMillis).shortLabel

    fun weekdayName(epochMillis: Long, zoneId: ZoneId = ZoneId.systemDefault()): String {
        val dow = Instant.ofEpochMilli(epochMillis).atZone(zoneId).dayOfWeek
        return when (dow) {
            DayOfWeek.SATURDAY -> "شنبه"
            DayOfWeek.SUNDAY -> "یکشنبه"
            DayOfWeek.MONDAY -> "دوشنبه"
            DayOfWeek.TUESDAY -> "سه‌شنبه"
            DayOfWeek.WEDNESDAY -> "چهارشنبه"
            DayOfWeek.THURSDAY -> "پنجشنبه"
            DayOfWeek.FRIDAY -> "جمعه"
        }
    }

    fun startOfWeek(date: LocalDate = LocalDate.now()): LocalDate =
        date.with(TemporalAdjusters.previousOrSame(DayOfWeek.SATURDAY))

    fun weekDays(start: LocalDate = startOfWeek()): List<LocalDate> = (0L..6L).map { start.plusDays(it) }

    fun todayEpochDay(): Long = LocalDate.now().toEpochDay()

    fun epochDayOf(epochMillis: Long): Long =
        Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalDate().toEpochDay()

    fun epochMillisOfEpochDay(epochDay: Long): Long =
        LocalDate.ofEpochDay(epochDay).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    fun dateOfEpochDay(epochDay: Long): LocalDate = LocalDate.ofEpochDay(epochDay)

    fun relativeDayLabel(epochDay: Long): String {
        val today = todayEpochDay()
        return when (epochDay) {
            today -> "امروز"
            today + 1 -> "فردا"
            today - 1 -> "دیروز"
            else -> JalaliCalendar.from(dateOfEpochDay(epochDay)).shortLabel
        }
    }

    fun timeLabel(epochMillis: Long): String {
        val time = Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalTime()
        return PersianText.faDigits(String.format("%02d:%02d", time.hour, time.minute))
    }
}

object ClockFormat {
    fun countdown(totalSeconds: Long): String {
        val safe = totalSeconds.coerceAtLeast(0)
        val hours = safe / 3600
        val minutes = (safe % 3600) / 60
        val seconds = safe % 60
        val text = if (hours > 0) String.format("%02d:%02d:%02d", hours, minutes, seconds)
        else String.format("%02d:%02d", minutes, seconds)
        return PersianText.faDigits(text)
    }

    fun duration(minutes: Int): String {
        if (minutes <= 0) return "زمان نامشخص"
        return if (minutes >= 60) {
            val h = minutes / 60
            val m = minutes % 60
            if (m == 0) PersianText.faDigits(h.toString()) + " ساعت"
            else PersianText.faDigits(h.toString()) + ":" + PersianText.faDigits(m.toString().padStart(2, '0')) + " ساعت"
        } else PersianText.faDigits(minutes.toString()) + " دقیقه"
    }
}
