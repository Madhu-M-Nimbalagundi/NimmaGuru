package com.nimmaguru.app.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object NimmaDateFormatter {
    private val datePattern = "dd-MM-yyyy"
    
    fun format(date: Date): String {
        val sdf = SimpleDateFormat(datePattern, Locale.getDefault())
        return sdf.format(date)
    }

    fun parse(dateString: String): Date? {
        return try {
            SimpleDateFormat(datePattern, Locale.getDefault()).parse(dateString.trim())
        } catch (e: Exception) {
            null
        }
    }

    fun localize(text: String, locale: Locale = Locale.getDefault()): String {
        if (locale.language != "kn") return text
        val knNumbers = mapOf(
            '0' to '೦', '1' to '೧', '2' to '೨', '3' to '೩', '4' to '೪',
            '5' to '೫', '6' to '೬', '7' to '೭', '8' to '೮', '9' to '೯'
        )
        return text.map { knNumbers[it] ?: it }.joinToString("")
    }

    fun localize(number: Int, locale: Locale = Locale.getDefault()): String {
        return localize(number.toString(), locale)
    }
}
