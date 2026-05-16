package com.nimmaguru.app.data.learning

import android.content.Context
import com.nimmaguru.app.R
import com.nimmaguru.app.domain.model.CurriculumType

object SubjectCatalog {
    fun subjects(context: Context, curriculumType: CurriculumType?, gradeLevel: String): List<String> {
        val boardTopics = SyllabusCatalog.filtered(curriculumType)
        val gradeTopics = boardTopics.filter { gradeLevel.isBlank() || it.classLevel.contains(gradeLevel) }
        val subjects = gradeTopics.map { it.subject }.distinct().sorted()
        
        return if (subjects.isEmpty()) {
            listOf(
                context.getString(R.string.mathematics_subject),
                context.getString(R.string.science_subject),
                context.getString(R.string.social_science_subject),
                context.getString(R.string.english_subject),
                context.getString(R.string.kannada_subject)
            )
        } else {
            subjects
        }
    }
}
