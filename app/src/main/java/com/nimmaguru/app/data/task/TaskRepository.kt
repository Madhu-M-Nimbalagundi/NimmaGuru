package com.nimmaguru.app.data.task

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.nimmaguru.app.domain.model.Task
import kotlinx.coroutines.tasks.await

class TaskRepository(
    private val firestore: FirebaseFirestore
) {
    fun listenAssignedTasks(
        targetGrade: String,
        onResult: (Result<List<Task>>) -> Unit
    ): ListenerRegistration {
        return firestore.collection(COLLECTION_TASKS)
            .whereEqualTo(FIELD_TARGET_GRADE, targetGrade.trim())
            .orderBy(FIELD_CREATED_AT, Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onResult(Result.failure(error))
                    return@addSnapshotListener
                }
                onResult(Result.success(snapshot?.documents?.map { it.toTask() } ?: emptyList()))
            }
    }

    fun listenGuruTasks(
        guruUid: String,
        onResult: (Result<List<Task>>) -> Unit
    ): ListenerRegistration {
        return firestore.collection(COLLECTION_TASKS)
            .whereEqualTo(FIELD_OWNER_GURU_ID, guruUid)
            .orderBy(FIELD_CREATED_AT, Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onResult(Result.failure(error))
                    return@addSnapshotListener
                }
                onResult(Result.success(snapshot?.documents?.map { it.toTask() } ?: emptyList()))
            }
    }

    suspend fun createTask(task: Task): String {
        require(task.ownerGuruId.isNotBlank()) { "Guru id is required." }
        require(task.targetGrade.isNotBlank()) { "Target grade is required." }
        require(task.title.isNotBlank()) { "Task title is required." }
        require(task.dueDate.isNotBlank()) { "Due date is required." }

        val document = firestore.collection(COLLECTION_TASKS).document()
        document.set(
            mapOf(
                FIELD_CLASSROOM_ID to task.classroomId.trim(),
                FIELD_OWNER_GURU_ID to task.ownerGuruId.trim(),
                FIELD_TARGET_GRADE to task.targetGrade.trim(),
                FIELD_TITLE to task.title.trim(),
                FIELD_DESCRIPTION to task.description.trim(),
                FIELD_DUE_DATE to task.dueDate.trim(),
                FIELD_CREATED_AT to Timestamp.now()
            )
        ).await()
        return document.id
    }

    suspend fun deleteTask(taskId: String) {
        firestore.collection(COLLECTION_TASKS).document(taskId).delete().await()
    }

    private fun DocumentSnapshot.toTask(): Task {
        return Task(
            id = id,
            classroomId = getString(FIELD_CLASSROOM_ID).orEmpty(),
            ownerGuruId = getString(FIELD_OWNER_GURU_ID).orEmpty(),
            targetGrade = getString(FIELD_TARGET_GRADE).orEmpty(),
            title = getString(FIELD_TITLE).orEmpty(),
            description = getString(FIELD_DESCRIPTION).orEmpty(),
            dueDate = getString(FIELD_DUE_DATE).orEmpty(),
            createdAt = getTimestamp(FIELD_CREATED_AT)
        )
    }

    private companion object {
        const val COLLECTION_TASKS = "tasks"
        const val FIELD_CLASSROOM_ID = "classroomId"
        const val FIELD_OWNER_GURU_ID = "ownerGuruId"
        const val FIELD_TARGET_GRADE = "targetGrade"
        const val FIELD_TITLE = "title"
        const val FIELD_DESCRIPTION = "description"
        const val FIELD_DUE_DATE = "dueDate"
        const val FIELD_CREATED_AT = "createdAt"
    }
}
