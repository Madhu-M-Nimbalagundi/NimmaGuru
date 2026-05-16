package com.nimmaguru.app.ui.home

import android.content.Context
import com.nimmaguru.app.R
import java.util.Calendar

fun getGreeting(context: Context): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val stringRes = when (hour) {
        in 5..11 -> R.string.greeting_morning
        in 12..16 -> R.string.greeting_afternoon
        else -> R.string.greeting_evening
    }
    return context.getString(stringRes)
}
