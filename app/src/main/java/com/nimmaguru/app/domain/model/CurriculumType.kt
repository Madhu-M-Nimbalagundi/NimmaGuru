package com.nimmaguru.app.domain.model

enum class CurriculumType(val firestoreValue: String, val displayName: String) {
    KARNATAKA_STATE_BOARD("karnataka_state_board", "Karnataka State Board"),
    CBSE("cbse", "CBSE"),
    BOTH("both", "Both");

    companion object {
        fun fromStorage(value: String?): CurriculumType? {
            return entries.firstOrNull { it.firestoreValue.equals(value, ignoreCase = true) }
        }
    }
}
