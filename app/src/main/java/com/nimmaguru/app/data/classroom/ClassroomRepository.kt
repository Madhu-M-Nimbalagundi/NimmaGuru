package com.nimmaguru.app.data.classroom

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.nimmaguru.app.domain.model.Classroom
import com.nimmaguru.app.domain.model.Task
import com.nimmaguru.app.util.NimmaDateFormatter
import kotlinx.coroutines.tasks.await

class ClassroomRepository(
    private val firestore: FirebaseFirestore
) {
    suspend fun createClassroom(classroom: Classroom): String {
        val document = firestore.collection(COLLECTION_CLASSROOMS).document()
        document.set(
            mapOf(
                FIELD_OWNER_GURU_ID to classroom.ownerGuruId,
                FIELD_TITLE to classroom.title.trim(),
                FIELD_BOARD_TYPE to classroom.boardType,
                FIELD_GRADE_LEVEL to classroom.gradeLevel,
                FIELD_STUDENT_IDS to classroom.studentIds,
                FIELD_CREATED_AT to Timestamp.now()
            )
        ).await()
        return document.id
    }

    suspend fun addStudent(classroomId: String, studentUid: String) {
        firestore.collection(COLLECTION_CLASSROOMS).document(classroomId)
            .update(FIELD_STUDENT_IDS, FieldValue.arrayUnion(studentUid))
            .await()
    }

    suspend fun createTask(task: Task): String {
        NimmaDateFormatter.parse(task.dueDate)
        val document = firestore.collection(COLLECTION_CLASSROOMS)
            .document(task.classroomId)
            .collection(COLLECTION_TASKS)
            .document()
        document.set(
            mapOf(
                FIELD_CLASSROOM_ID to task.classroomId,
                FIELD_OWNER_GURU_ID to task.ownerGuruId,
                FIELD_TITLE to task.title.trim(),
                FIELD_DESCRIPTION to task.description.trim(),
                FIELD_DUE_DATE to task.dueDate.trim(),
                FIELD_CREATED_AT to Timestamp.now()
            )
        ).await()
        return document.id
    }

    private companion object {
        const val COLLECTION_CLASSROOMS = "classrooms"
        const val COLLECTION_TASKS = "tasks"
        const val FIELD_CLASSROOM_ID = "classroomId"
        const val FIELD_OWNER_GURU_ID = "ownerGuruId"
        const val FIELD_TITLE = "title"
        const val FIELD_DESCRIPTION = "description"
        const val FIELD_DUE_DATE = "dueDate"
        const val FIELD_BOARD_TYPE = "boardType"
        const val FIELD_GRADE_LEVEL = "gradeLevel"
        const val FIELD_STUDENT_IDS = "studentIds"
        const val FIELD_CREATED_AT = "createdAt"
    }
}
