package com.nimmaguru.app.data.calendar

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.nimmaguru.app.domain.model.ClassSession
import kotlinx.coroutines.tasks.await

class ClassSessionRepository(
    private val firestore: FirebaseFirestore,
) {
    fun listenUpcomingSessions(
        onResult: (Result<List<ClassSession>>) -> Unit
    ): ListenerRegistration {
        // startsAt is a Timestamp so upcoming sessions sort correctly across dates and times.
        return firestore.collection(COLLECTION_CLASS_SESSIONS)
            .whereGreaterThanOrEqualTo(FIELD_STARTS_AT, Timestamp.now())
            .orderBy(FIELD_STARTS_AT, Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onResult(Result.failure(error))
                    return@addSnapshotListener
                }

                val sessions = snapshot
                    ?.documents
                    ?.map { it.toClassSession() }
                    ?: emptyList()

                onResult(Result.success(sessions))
            }
    }

    suspend fun enrollInSession(sessionId: String, studentUid: String) {
        firestore.collection(COLLECTION_CLASS_SESSIONS)
            .document(sessionId)
            .update(FIELD_ENROLLED_STUDENT_IDS, FieldValue.arrayUnion(studentUid))
            .await()
    }

    fun listenSessionEnrollments(
        sessionId: String,
        onResult: (Result<List<String>>) -> Unit
    ): ListenerRegistration {
        return firestore.collection(COLLECTION_CLASS_SESSIONS)
            .document(sessionId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onResult(Result.failure(error))
                    return@addSnapshotListener
                }
                val ids = (snapshot?.get(FIELD_ENROLLED_STUDENT_IDS) as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                onResult(Result.success(ids))
            }
    }

    fun listenSamudayaBhavanaSessions(
        onResult: (Result<List<ClassSession>>) -> Unit
    ): ListenerRegistration {
        return firestore.collection(COLLECTION_CLASS_SESSIONS)
            .whereEqualTo(FIELD_LOCATION_NORMALIZED, SAMUDAYA_BHAVANA)
            .whereGreaterThanOrEqualTo(FIELD_STARTS_AT, Timestamp.now())
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

    suspend fun saveSession(session: ClassSession): String {
        val document = if (session.id.isBlank()) {
            firestore.collection(COLLECTION_CLASS_SESSIONS).document()
        } else {
            firestore.collection(COLLECTION_CLASS_SESSIONS).document(session.id)
        }

        val data = hashMapOf(
            FIELD_DATE to session.date.trim(),
            FIELD_TIME to session.time.trim(),
            FIELD_MENTOR to session.mentor.trim(),
            FIELD_SUBJECT to session.subject.trim(),
            FIELD_LOCATION to session.location.trim(),
            FIELD_FULL_ADDRESS to session.fullAddress.trim(),
            FIELD_GRADE_LEVEL to session.gradeLevel.trim(),
            FIELD_BOARD_TYPE to session.boardType.trim(),
            FIELD_LOCATION_NORMALIZED to session.location.trim().lowercase(),
            FIELD_STARTS_AT to session.startsAt,
            FIELD_ENROLLED_STUDENT_IDS to session.enrolledStudentIds,
            FIELD_UPDATED_AT to Timestamp.now()
        )

        document.set(data).await()
        return document.id
    }

    suspend fun deleteSession(sessionId: String) {
        firestore.collection(COLLECTION_CLASS_SESSIONS).document(sessionId).delete().await()
    }

    private fun DocumentSnapshot.toClassSession(): ClassSession {
        return ClassSession(
            id = id,
            date = getString(FIELD_DATE).orEmpty(),
            time = getString(FIELD_TIME).orEmpty(),
            mentor = getString(FIELD_MENTOR).orEmpty(),
            subject = getString(FIELD_SUBJECT).orEmpty(),
            location = getString(FIELD_LOCATION).orEmpty(),
            fullAddress = getString(FIELD_FULL_ADDRESS).orEmpty(),
            gradeLevel = getString(FIELD_GRADE_LEVEL).orEmpty(),
            boardType = getString(FIELD_BOARD_TYPE).orEmpty(),
            startsAt = getTimestamp(FIELD_STARTS_AT),
            enrolledStudentIds = (get(FIELD_ENROLLED_STUDENT_IDS) as? List<*>)?.filterIsInstance<String>() ?: emptyList()
        )
    }

    private companion object {
        const val COLLECTION_CLASS_SESSIONS = "classSessions"
        const val FIELD_DATE = "date"
        const val FIELD_TIME = "time"
        const val FIELD_MENTOR = "mentor"
        const val FIELD_SUBJECT = "subject"
        const val FIELD_LOCATION = "location"
        const val FIELD_FULL_ADDRESS = "fullAddress"
        const val FIELD_GRADE_LEVEL = "gradeLevel"
        const val FIELD_BOARD_TYPE = "boardType"
        const val FIELD_LOCATION_NORMALIZED = "locationNormalized"
        const val FIELD_STARTS_AT = "startsAt"
        const val FIELD_ENROLLED_STUDENT_IDS = "enrolledStudentIds"
        const val FIELD_UPDATED_AT = "updatedAt"
        const val SAMUDAYA_BHAVANA = "samudaya bhavana"
    }
}
