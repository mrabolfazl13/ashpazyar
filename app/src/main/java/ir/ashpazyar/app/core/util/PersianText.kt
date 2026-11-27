package ir.ashpazyar.app.core.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

object PersianText {
    private val diacritics = Regex("[\\u064B-\\u065F\\u0670\\u06D6-\\u06ED]")
    private val tatweel = Regex("\\u0640")
    private val spaces = Regex("\\s+")

    fun normalize(input: String): String {
        return input.trim()
            .replace('ي', 'ی')
            .replace('ى', 'ی')
            .replace('ك', 'ک')
            .replace('ة', 'ه')
            .replace('ۀ', 'ه')
            .replace('أ', 'ا')
            .replace('إ', 'ا')
            .replace('ؤ', 'و')
            .replace('ئ', 'ی')
            .replace('\u200c', ' ')
            .replace('\u200d', ' ')
            .replace('\u200e', ' ')
            .replace('\u200f', ' ')
            .replace('ـ', ' ')
            .replace(diacritics, "")
            .replace(tatweel, "")
            .replace(spaces, " ")
            .lowercase(Locale.forLanguageTag("fa"))
    }

    fun faDigits(value: String): String {
        val builder = StringBuilder(value.length)
        for (ch in value) {
            builder.append(when (ch) {
                '0' -> '۰'; '1' -> '۱'; '2' -> '۲'; '3' -> '۳'; '4' -> '۴'
                '5' -> '۵'; '6' -> '۶'; '7' -> '۷'; '8' -> '۸'; '9' -> '۹'
                else -> ch
            })
        }
        return builder.toString()
    }

    fun latinDigits(value: String): String {
        val builder = StringBuilder(value.length)
        for (ch in value) {
            builder.append(when (ch) {
                '۰' -> '0'; '۱' -> '1'; '۲' -> '2'; '۳' -> '3'; '۴' -> '4'
                '۵' -> '5'; '۶' -> '6'; '۷' -> '7'; '۸' -> '8'; '۹' -> '9'
                '٠' -> '0'; '١' -> '1'; '٢' -> '2'; '٣' -> '3'; '٤' -> '4'
                '٥' -> '5'; '٦' -> '6'; '٧' -> '7'; '٨' -> '8'; '٩' -> '9'
                else -> ch
            })
        }
        return builder.toString()
    }

    fun number(value: Double, maxFraction: Int = 1): String {
        val pattern = if (maxFraction <= 0) "0" else "0." + "0".repeat(maxFraction.coerceIn(1, 3))
        val symbols = DecimalFormatSymbols(Locale.US).apply { decimalSeparator = '٫' }
        val formatter = DecimalFormat(pattern, symbols)
        val visible = if (kotlin.math.abs(value - value.toLong()) < 0.001) value.toLong().toString()
        else formatter.format(value)
        return faDigits(visible)
    }

    fun money(value: Double): String {
        if (value <= 0.0) return "ثبت نشده"
        val rounded = (value / 1000.0).let {
            if (it >= 100) Math.round(it).toString()
            else DecimalFormat("0.#", DecimalFormatSymbols(Locale.US)).format(it)
        }
        return faDigits(rounded.replace(".", "٫")) + " هزار تومان"
    }

    fun countLabel(count: Int): String = faDigits(count.toString())

    fun firstLetters(input: String): String = input.trim().split(Regex("\\s+")).take(2).joinToString(" ") { it.take(1) }
}
