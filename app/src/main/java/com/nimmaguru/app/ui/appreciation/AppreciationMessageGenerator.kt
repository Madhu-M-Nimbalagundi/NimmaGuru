package com.nimmaguru.app.ui.appreciation

import android.content.Context
import com.nimmaguru.app.R
import kotlin.random.Random

object AppreciationMessageGenerator {
    fun generate(context: Context): String {
        // Local template-based generation works offline and follows the current app language.
        val suggestions = listOf(
            context.getString(R.string.suggested_appreciation_1),
            context.getString(R.string.suggested_appreciation_2),
            context.getString(R.string.suggested_appreciation_3)
        )
        return suggestions[Random.nextInt(suggestions.size)]
    }
}
