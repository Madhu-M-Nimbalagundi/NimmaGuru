package com.nimmaguru.app.ui.guru

import android.content.Context
import android.content.Intent
import com.nimmaguru.app.ui.calendar.ClassCalendarActivity
import com.nimmaguru.app.ui.learning.MaterialsActivity

object GuruModule {
    fun openProfile(context: Context) {
        context.startActivity(Intent(context, GuruProfileActivity::class.java))
    }

    fun openMaterials(context: Context) {
        context.startActivity(Intent(context, MaterialsActivity::class.java))
    }

    fun openAvailability(context: Context) {
        context.startActivity(Intent(context, ClassCalendarActivity::class.java))
    }
}
