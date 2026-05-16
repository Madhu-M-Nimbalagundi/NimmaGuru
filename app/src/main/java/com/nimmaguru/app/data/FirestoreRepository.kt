package com.nimmaguru.app.data

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.nimmaguru.app.domain.model.ClassSession
import com.nimmaguru.app.domain.model.Guru
import com.nimmaguru.app.domain.model.Testimony

class FirestoreRepository(
    private val firestore: FirebaseFirestore
) {
    fun listenGurus(
        skillFilters: List<String>,
        village: String,
        onResult: (Result<List<Guru>>) -> Unit
    ): ListenerRegistration {
        val selectedSkills = skillFilters
            .map(String::trim)
            .filter(String::isNotBlank)
            .distinct()
            .take(MAX_ARRAY_CONTAINS_ANY_VALUES)

        var query: Query = firestore
            .collection(COLLECTION_GURU_PROFILES)
            .whereEqualTo(FIELD_ROLE, ROLE_GURU)

        if (selectedSkills.size == 1) {
            query = query.whereArrayContains(FIELD_SKILLS, selectedSkills.first())
        } else if (selectedSkills.size > 1) {
            query = query.whereArrayContainsAny(FIELD_SKILLS, selectedSkills)
        }

        val normalizedVillage = village.normalized()
        if (normalizedVillage.isNotBlank()) {
            query = query.whereEqualTo(FIELD_LOCATION_NORMALIZED, normalizedVillage)
        }

        return query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                onResult(Result.failure(error))
                return@addSnapshotListener
            }

            val gurus = snapshot
                ?.documents
                ?.map { it.toGuru() }
                ?.sortedWith(compareByDescending<Guru> { it.rating }.thenBy { it.name.lowercase() })
                ?: emptyList()

            onResult(Result.success(gurus))
        }
    }

    fun listenWallOfFame(
        onResult: (Result<List<Guru>>) -> Unit
    ): ListenerRegistration {
        return firestore.collection(COLLECTION_GURU_PROFILES)
            .whereEqualTo(FIELD_ROLE, ROLE_GURU)
            .orderBy(FIELD_AVERAGE_STUDENT_REVIEW, Query.Direction.DESCENDING)
            .limit(WALL_OF_FAME_LIMIT)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onResult(Result.failure(error))
                    return@addSnapshotListener
                }

                val gurus = snapshot?.documents?.map { it.toGuru() } ?: emptyList()
                onResult(Result.success(gurus))
            }
    }

    fun listenSamudayaBhavanaCalendar(
        onResult: (Result<List<ClassSession>>) -> Unit
    ): ListenerRegistration {
        return firestore.collection(COLLECTION_CLASS_SESSIONS)
            .whereEqualTo(FIELD_LOCATION_NORMALIZED, SAMUDAYA_BHAVANA)
            .orderBy(FIELD_STARTS_AT, Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onResult(Result.failure(error))
                    return@addSnapshotListener
                }

                val sessions = snapshot?.documents?.map { it.toClassSession() } ?: emptyList()
                onResult(Result.success(sessions))
            }
    }

    fun listenTestimonies(
        guruId: String? = null,
        onResult: (Result<List<Testimony>>) -> Unit
    ): ListenerRegistration {
        var query: Query = firestore
            .collection(COLLECTION_APPRECIATIONS)
            .orderBy(FIELD_CREATED_AT, Query.Direction.DESCENDING)

        if (!guruId.isNullOrBlank()) {
            query = firestore
                .collection(COLLECTION_APPRECIATIONS)
                .whereEqualTo(FIELD_GURU_ID, guruId)
                .orderBy(FIELD_CREATED_AT, Query.Direction.DESCENDING)
        }

        return query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                onResult(Result.failure(error))
                return@addSnapshotListener
            }

            val testimonies = snapshot?.documents?.map { it.toTestimony() } ?: emptyList()
            onResult(Result.success(testimonies))
        }
    }

    private fun DocumentSnapshot.toGuru(): Guru {
        return Guru(
            id = getString(FIELD_UID).orEmpty().ifBlank { id },
            name = getString(FIELD_NAME).orEmpty(),
            skills = (get(FIELD_SKILLS) as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
            freeHours = getString(FIELD_AVAILABLE_TIME).orEmpty(),
            village = getString(FIELD_LOCATION).orEmpty(),
            rating = getDouble(FIELD_AVERAGE_STUDENT_REVIEW) ?: 0.0
        )
    }

    private fun DocumentSnapshot.toClassSession(): ClassSession {
        return ClassSession(
            id = id,
            date = getString(FIELD_DATE).orEmpty(),
            time = getString(FIELD_TIME).orEmpty(),
            mentor = getString(FIELD_MENTOR).orEmpty(),
            subject = getString(FIELD_SUBJECT).orEmpty(),
            location = getString(FIELD_LOCATION).orEmpty(),
            gradeLevel = getString(FIELD_GRADE_LEVEL).orEmpty(),
            boardType = getString(FIELD_BOARD_TYPE).orEmpty(),
            startsAt = getTimestamp(FIELD_STARTS_AT)
        )
    }

    private fun DocumentSnapshot.toTestimony(): Testimony {
        return Testimony(
            id = id,
            studentName = getString(FIELD_STUDENT_NAME)
                ?: getString(FIELD_STUDENT_EMAIL).orEmpty().substringBefore("@").ifBlank { "Student" },
            guruId = getString(FIELD_GURU_ID).orEmpty(),
            message = getString(FIELD_MESSAGE).orEmpty()
        )
    }

    private fun String.normalized(): String = trim().lowercase()

    private companion object {
        const val COLLECTION_GURU_PROFILES = "guruProfiles"
        const val COLLECTION_CLASS_SESSIONS = "classSessions"
        const val COLLECTION_APPRECIATIONS = "appreciations"
        const val ROLE_GURU = "guru"
        const val MAX_ARRAY_CONTAINS_ANY_VALUES = 10
        const val WALL_OF_FAME_LIMIT = 10L
        const val SAMUDAYA_BHAVANA = "samudaya bhavana"
        const val FIELD_UID = "uid"
        const val FIELD_ROLE = "role"
        const val FIELD_NAME = "name"
        const val FIELD_SKILLS = "skills"
        const val FIELD_AVAILABLE_TIME = "availableTime"
        const val FIELD_LOCATION = "location"
        const val FIELD_LOCATION_NORMALIZED = "locationNormalized"
        const val FIELD_AVERAGE_STUDENT_REVIEW = "averageStudentReview"
        const val FIELD_DATE = "date"
        const val FIELD_TIME = "time"
        const val FIELD_MENTOR = "mentor"
        const val FIELD_SUBJECT = "subject"
        const val FIELD_GRADE_LEVEL = "gradeLevel"
        const val FIELD_BOARD_TYPE = "boardType"
        const val FIELD_STARTS_AT = "startsAt"
        const val FIELD_STUDENT_NAME = "studentName"
        const val FIELD_STUDENT_EMAIL = "studentEmail"
        const val FIELD_GURU_ID = "guruId"
        const val FIELD_MESSAGE = "message"
        const val FIELD_CREATED_AT = "createdAt"
    }
}
