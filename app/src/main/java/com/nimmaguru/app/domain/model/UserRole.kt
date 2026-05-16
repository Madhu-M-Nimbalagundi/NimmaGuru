package com.nimmaguru.app.domain.model

enum class UserRole(val displayName: String, val firestoreValue: String) {
    GURU("Guru", "guru"),
    STUDENT("Student", "student");

    companion object {
        fun fromStorage(value: String?): UserRole? {
            return entries.firstOrNull { role ->
                role.name.equals(value, ignoreCase = true) ||
                    role.firestoreValue.equals(value, ignoreCase = true)
            }
        }
    }
}
