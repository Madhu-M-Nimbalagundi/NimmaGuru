package com.nimmaguru.app.data.appreciation

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.nimmaguru.app.domain.model.AppreciationMessage
import kotlinx.coroutines.tasks.await

class AppreciationRepository(
    private val firestore: FirebaseFirestore
) {
    fun listenMessages(
        onResult: (Result<List<AppreciationMessage>>) -> Unit
    ): ListenerRegistration {
        // Real-time listener powers the Wall of Fame feed as soon as students post.
        return firestore.collection(COLLECTION_APPRECIATIONS)
            .orderBy(FIELD_CREATED_AT, Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onResult(Result.failure(error))
                    return@addSnapshotListener
                }

                val messages = snapshot
                    ?.documents
                    ?.map { it.toAppreciationMessage() }
                    ?: emptyList()

                onResult(Result.success(messages))
            }
    }

    suspend fun postMessage(
        studentUid: String,
        studentEmail: String,
        studentName: String,
        guruName: String,
        message: String
    ) {
        val data = hashMapOf(
            FIELD_STUDENT_UID to studentUid,
            FIELD_STUDENT_EMAIL to studentEmail,
            FIELD_STUDENT_NAME to studentName.trim(),
            FIELD_GURU_NAME to guruName.trim(),
            FIELD_MESSAGE to message.trim(),
            FIELD_CREATED_AT to Timestamp.now()
        )

        firestore.collection(COLLECTION_APPRECIATIONS)
            .document()
            .set(data)
            .await()
    }

    private fun DocumentSnapshot.toAppreciationMessage(): AppreciationMessage {
        return AppreciationMessage(
            id = id,
            studentUid = getString(FIELD_STUDENT_UID).orEmpty(),
            studentEmail = getString(FIELD_STUDENT_EMAIL).orEmpty(),
            studentName = getString(FIELD_STUDENT_NAME).orEmpty(),
            guruName = getString(FIELD_GURU_NAME).orEmpty(),
            message = getString(FIELD_MESSAGE).orEmpty(),
            createdAt = getTimestamp(FIELD_CREATED_AT)
        )
    }

    private companion object {
        const val COLLECTION_APPRECIATIONS = "appreciations"
        const val FIELD_STUDENT_UID = "studentUid"
        const val FIELD_STUDENT_EMAIL = "studentEmail"
        const val FIELD_STUDENT_NAME = "studentName"
        const val FIELD_GURU_NAME = "guruName"
        const val FIELD_MESSAGE = "message"
        const val FIELD_CREATED_AT = "createdAt"
    }
}
