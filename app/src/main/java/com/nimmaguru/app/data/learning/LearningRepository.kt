package com.nimmaguru.app.data.learning

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.nimmaguru.app.util.NimmaDateFormatter
import com.nimmaguru.app.domain.model.Assignment
import com.nimmaguru.app.domain.model.CurriculumType
import com.nimmaguru.app.domain.model.LearningMaterial
import com.nimmaguru.app.domain.model.NoteDocument
import com.nimmaguru.app.domain.model.ProgressSummary
import com.nimmaguru.app.domain.model.SyllabusTopic
import kotlinx.coroutines.tasks.await

class LearningRepository(
    private val firestore: FirebaseFirestore
) {
    fun listenMaterials(
        subject: String,
        curriculumType: CurriculumType?,
        onResult: (Result<List<LearningMaterial>>) -> Unit
    ): ListenerRegistration {
        var query: Query = firestore.collection(COLLECTION_MATERIALS)
            .orderBy(FIELD_CREATED_AT, Query.Direction.DESCENDING)

        val normalizedSubject = subject.trim()
        if (curriculumType != null && curriculumType != CurriculumType.BOTH) {
            query = firestore.collection(COLLECTION_MATERIALS)
                .whereEqualTo(FIELD_CURRICULUM_TYPE, curriculumType.firestoreValue)
                .orderBy(FIELD_CREATED_AT, Query.Direction.DESCENDING)
        }
        if (normalizedSubject.isNotBlank() && curriculumType != null && curriculumType != CurriculumType.BOTH) {
            query = firestore.collection(COLLECTION_MATERIALS)
                .whereEqualTo(FIELD_CURRICULUM_TYPE, curriculumType.firestoreValue)
                .whereEqualTo(FIELD_SUBJECT, normalizedSubject)
                .orderBy(FIELD_CREATED_AT, Query.Direction.DESCENDING)
        } else if (normalizedSubject.isNotBlank()) {
            query = firestore.collection(COLLECTION_MATERIALS)
                .whereEqualTo(FIELD_SUBJECT, normalizedSubject)
                .orderBy(FIELD_CREATED_AT, Query.Direction.DESCENDING)
        }

        return query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                onResult(Result.failure(error))
                return@addSnapshotListener
            }
            onResult(Result.success(snapshot?.documents?.map { it.toMaterial() } ?: emptyList()))
        }
    }

    fun listenRecentMaterials(
        limit: Long = 1,
        onResult: (Result<List<LearningMaterial>>) -> Unit
    ): ListenerRegistration {
        return firestore.collection(COLLECTION_MATERIALS)
            .orderBy(FIELD_CREATED_AT, Query.Direction.DESCENDING)
            .limit(limit)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onResult(Result.failure(error))
                    return@addSnapshotListener
                }
                onResult(Result.success(snapshot?.documents?.map { it.toMaterial() } ?: emptyList()))
            }
    }

    suspend fun toggleSavedMaterial(materialId: String, uid: String, isSaved: Boolean) {
        val update = if (isSaved) FieldValue.arrayRemove(uid) else FieldValue.arrayUnion(uid)
        firestore.collection(COLLECTION_MATERIALS).document(materialId)
            .update(FIELD_SAVED_BY, update)
            .await()
    }

    suspend fun loadCurriculum(curriculumType: CurriculumType?): List<SyllabusTopic> {
        var query: Query = firestore.collection(COLLECTION_CURRICULUM)
            .orderBy(FIELD_CLASS_LEVEL, Query.Direction.ASCENDING)

        if (curriculumType != null && curriculumType != CurriculumType.BOTH) {
            query = firestore.collection(COLLECTION_CURRICULUM)
                .whereEqualTo(FIELD_CURRICULUM_TYPE, curriculumType.firestoreValue)
                .orderBy(FIELD_CLASS_LEVEL, Query.Direction.ASCENDING)
        }

        return query.get().await().documents.map { snapshot ->
            SyllabusTopic(
                board = snapshot.getString(FIELD_BOARD_TYPE).orEmpty()
                    .ifBlank { CurriculumType.fromStorage(snapshot.getString(FIELD_CURRICULUM_TYPE))?.displayName.orEmpty() },
                classLevel = snapshot.getString(FIELD_CLASS_LEVEL).orEmpty(),
                subject = snapshot.getString(FIELD_SUBJECT).orEmpty(),
                chapter = snapshot.getString(FIELD_CHAPTER).orEmpty(),
                topics = (snapshot.get(FIELD_TOPICS) as? List<*>)?.filterIsInstance<String>() ?: emptyList()
            )
        }
    }

    suspend fun uploadMaterial(material: LearningMaterial, uid: String) {
        val data = hashMapOf(
            FIELD_TITLE to material.title.trim(),
            FIELD_SUBJECT to material.subject.trim(),
            FIELD_CURRICULUM_TYPE to material.curriculumType.trim(),
            FIELD_CLASS_LEVEL to material.classLevel.trim(),
            FIELD_TYPE to material.type.trim().ifBlank { "Notes" },
            FIELD_SIZE to material.size.trim(),
            FIELD_DESCRIPTION to material.description.trim(),
            FIELD_RESOURCE_URL to material.resourceUrl.trim(),
            FIELD_CREATED_AT to Timestamp.now(),
            FIELD_UPLOAD_DATE to NimmaDateFormatter.format(java.util.Date()),
            FIELD_SAVED_BY to listOf(uid),
            "uploadedBy" to uid
        )
        val materialRef = firestore.collection(COLLECTION_MATERIALS).document()
        val noteRef = firestore.collection(COLLECTION_NOTES).document()
        val progressRef = firestore.collection(COLLECTION_USERS).document(uid)
            .collection(COLLECTION_PROGRESS).document(DOCUMENT_SUMMARY)
        val note = NoteDocument(
            id = noteRef.id,
            fileUrl = material.resourceUrl.trim(),
            boardType = material.curriculumType.trim(),
            subject = material.subject.trim(),
            uploadedBy = uid,
            createdAt = Timestamp.now()
        )
        firestore.runBatch { batch ->
            batch.set(materialRef, data)
            batch.set(
                noteRef,
                mapOf(
                    FIELD_FILE_URL to note.fileUrl,
                    FIELD_BOARD_TYPE to note.boardType,
                    FIELD_SUBJECT to note.subject,
                    FIELD_UPLOADED_BY to note.uploadedBy,
                    FIELD_CREATED_AT to note.createdAt
                )
            )
            batch.set(
                progressRef,
                mapOf(
                    FIELD_BADGES to FieldValue.arrayUnion(CONTRIBUTOR_BADGE, UPLOAD_BADGE),
                    FIELD_NOTES_COUNT to FieldValue.increment(1),
                    FIELD_RECENT_ACTIVITY to FieldValue.arrayUnion("Uploaded ${material.title.trim()}"),
                    FIELD_UPDATED_AT to Timestamp.now()
                ),
                SetOptions.merge()
            )
        }.await()
    }

    fun listenAssignments(
        uid: String,
        onResult: (Result<List<Assignment>>) -> Unit
    ): ListenerRegistration {
        return firestore.collection(COLLECTION_USERS).document(uid)
            .collection(COLLECTION_ASSIGNMENTS)
            .orderBy(FIELD_UPDATED_AT, Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onResult(Result.failure(error))
                    return@addSnapshotListener
                }
                onResult(Result.success(snapshot?.documents?.map { it.toAssignment() } ?: emptyList()))
            }
    }

    suspend fun submitAssignment(uid: String, assignmentId: String, submissionText: String) {
        firestore.collection(COLLECTION_USERS).document(uid)
            .collection(COLLECTION_ASSIGNMENTS).document(assignmentId)
            .set(
                mapOf(
                    FIELD_STATUS to Assignment.STATUS_SUBMITTED,
                    FIELD_SUBMISSION_TEXT to submissionText.trim(),
                    FIELD_UPDATED_AT to Timestamp.now()
                ),
                SetOptions.merge()
            )
            .await()
    }

    fun listenProgress(
        uid: String,
        onResult: (Result<ProgressSummary>) -> Unit
    ): ListenerRegistration {
        return firestore.collection(COLLECTION_USERS).document(uid)
            .collection(COLLECTION_PROGRESS).document(DOCUMENT_SUMMARY)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onResult(Result.failure(error))
                    return@addSnapshotListener
                }
                onResult(Result.success(snapshot?.toProgressSummary() ?: ProgressSummary()))
            }
    }

    private fun DocumentSnapshot.toMaterial(): LearningMaterial {
        return LearningMaterial(
            id = id,
            title = getString(FIELD_TITLE).orEmpty(),
            subject = getString(FIELD_SUBJECT).orEmpty(),
            curriculumType = getString(FIELD_CURRICULUM_TYPE).orEmpty(),
            classLevel = getString(FIELD_CLASS_LEVEL).orEmpty(),
            type = getString(FIELD_TYPE).orEmpty(),
            size = getString(FIELD_SIZE).orEmpty(),
            description = getString(FIELD_DESCRIPTION).orEmpty(),
            resourceUrl = getString(FIELD_RESOURCE_URL).orEmpty(),
            createdAt = getTimestamp(FIELD_CREATED_AT),
            savedBy = (get(FIELD_SAVED_BY) as? List<*>)?.filterIsInstance<String>() ?: emptyList()
        )
    }

    private fun DocumentSnapshot.toAssignment(): Assignment {
        return Assignment(
            id = id,
            title = getString(FIELD_TITLE).orEmpty(),
            subject = getString(FIELD_SUBJECT).orEmpty(),
            dueDate = getString(FIELD_DUE_DATE).orEmpty(),
            instructions = getString(FIELD_INSTRUCTIONS).orEmpty(),
            status = getString(FIELD_STATUS).orEmpty().ifBlank { Assignment.STATUS_PENDING },
            submissionText = getString(FIELD_SUBMISSION_TEXT).orEmpty(),
            score = getLong(FIELD_SCORE)?.toInt(),
            feedback = getString(FIELD_FEEDBACK).orEmpty(),
            updatedAt = getTimestamp(FIELD_UPDATED_AT)
        )
    }

    private fun DocumentSnapshot.toProgressSummary(): ProgressSummary {
        return ProgressSummary(
            classesAttended = getLong(FIELD_CLASSES_ATTENDED)?.toInt() ?: 0,
            classesTaken = getLong(FIELD_CLASSES_TAKEN)?.toInt() ?: 0,
            assignmentsSubmitted = getLong(FIELD_ASSIGNMENTS_SUBMITTED)?.toInt() ?: 0,
            averageScore = getLong(FIELD_AVERAGE_SCORE)?.toInt() ?: 0,
            streakDays = getLong(FIELD_STREAK_DAYS)?.toInt() ?: 0,
            notesCount = getLong(FIELD_NOTES_COUNT)?.toInt() ?: 0,
            badges = (get(FIELD_BADGES) as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
            recentActivity = (get(FIELD_RECENT_ACTIVITY) as? List<*>)?.filterIsInstance<String>() ?: emptyList()
        )
    }

    private companion object {
        const val COLLECTION_MATERIALS = "materials"
        const val COLLECTION_NOTES = "notes"
        const val COLLECTION_CURRICULUM = "curriculum"
        const val COLLECTION_USERS = "users"
        const val COLLECTION_ASSIGNMENTS = "assignments"
        const val COLLECTION_PROGRESS = "progress"
        const val DOCUMENT_SUMMARY = "summary"
        const val FIELD_TITLE = "title"
        const val FIELD_SUBJECT = "subject"
        const val FIELD_CURRICULUM_TYPE = "curriculum_type"
        const val FIELD_CLASS_LEVEL = "classLevel"
        const val FIELD_TYPE = "type"
        const val FIELD_SIZE = "size"
        const val FIELD_DESCRIPTION = "description"
        const val FIELD_RESOURCE_URL = "resourceUrl"
        const val FIELD_CREATED_AT = "createdAt"
        const val FIELD_UPLOAD_DATE = "uploadDate"
        const val FIELD_SAVED_BY = "savedBy"
        const val FIELD_DUE_DATE = "dueDate"
        const val FIELD_INSTRUCTIONS = "instructions"
        const val FIELD_STATUS = "status"
        const val FIELD_SUBMISSION_TEXT = "submissionText"
        const val FIELD_SCORE = "score"
        const val FIELD_FEEDBACK = "feedback"
        const val FIELD_UPDATED_AT = "updatedAt"
        const val FIELD_CLASSES_ATTENDED = "classesAttended"
        const val FIELD_CLASSES_TAKEN = "classesTaken"
        const val FIELD_ASSIGNMENTS_SUBMITTED = "assignmentsSubmitted"
        const val FIELD_AVERAGE_SCORE = "averageScore"
        const val FIELD_STREAK_DAYS = "streakDays"
        const val FIELD_BADGES = "badges"
        const val FIELD_RECENT_ACTIVITY = "recentActivity"
        const val FIELD_FILE_URL = "fileUrl"
        const val FIELD_BOARD_TYPE = "boardType"
        const val FIELD_CHAPTER = "chapter"
        const val FIELD_TOPICS = "topics"
        const val FIELD_UPLOADED_BY = "uploadedBy"
        const val FIELD_NOTES_COUNT = "notes_count"
        const val CONTRIBUTOR_BADGE = "Contributor"
        const val UPLOAD_BADGE = "Upload Badge"
    }
}
