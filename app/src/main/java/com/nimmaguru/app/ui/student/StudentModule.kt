package com.nimmaguru.app.ui.student

import android.content.Context
import android.content.Intent
import com.nimmaguru.app.ui.learning.AssignmentsActivity
import com.nimmaguru.app.ui.learning.MaterialsActivity
import com.nimmaguru.app.ui.learning.ProgressActivity

object StudentModule {
    fun openMentors(context: Context) {
        context.startActivity(Intent(context, StudentSearchActivity::class.java))
    }

    fun openMaterials(context: Context) {
        context.startActivity(Intent(context, MaterialsActivity::class.java))
    }

    fun openProgress(context: Context) {
        context.startActivity(Intent(context, ProgressActivity::class.java))
    }

    fun openAssignments(context: Context) {
        context.startActivity(Intent(context, AssignmentsActivity::class.java))
    }
}
