package com.nimmaguru.app.data.user

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.nimmaguru.app.data.local.UserPreferences
import com.nimmaguru.app.domain.model.CurriculumType
import com.nimmaguru.app.domain.model.UserRole
import kotlinx.coroutines.tasks.await

class UserRoleRepository(
    private val firestore: FirebaseFirestore,
    private val userPreferences: UserPreferences,
) {
    suspend fun saveRole(
        uid: String,
        email: String,
        role: UserRole,
        curriculumType: CurriculumType,
        gradeLevel: String
    ) {
        val data = hashMapOf(
            FIELD_UID to uid,
            FIELD_EMAIL to email.trim(),
            FIELD_ROLE to role.firestoreValue,
            FIELD_CURRICULUM_TYPE to curriculumType.firestoreValue,
            FIELD_GRADE_LEVEL to gradeLevel.trim(),
            FIELD_UPDATED_AT to Timestamp.now()
        )

        firestore.collection(COLLECTION_USERS)
            .document(uid)
            .collection(COLLECTION_PROFILE)
            .document(DOCUMENT_PROFILE)
            .set(data, SetOptions.merge())
            .await()
        firestore.collection(COLLECTION_USERS)
            .document(uid)
            .set(data, SetOptions.merge())
            .await()

        userPreferences.saveRole(uid, role)
    }

    suspend fun loadRole(uid: String): UserRole? {
        userPreferences.getRole(uid)?.let { return it }

        val snapshot = profileDocument(uid).get().await()
            .takeIf { it.exists() }
            ?: firestore.collection(COLLECTION_USERS).document(uid).get().await()

        val role = UserRole.fromStorage(snapshot.getString(FIELD_ROLE))
        role?.let { userPreferences.saveRole(uid, it) }
        return role
    }

    fun getCachedRole(uid: String): UserRole? {
        return userPreferences.getRole(uid)
    }

    suspend fun loadCurriculumType(uid: String): CurriculumType? {
        val snapshot = profileDocument(uid).get().await()
            .takeIf { it.exists() }
            ?: firestore.collection(COLLECTION_USERS).document(uid).get().await()

        return CurriculumType.fromStorage(snapshot.getString(FIELD_CURRICULUM_TYPE))
    }

    suspend fun loadGradeLevel(uid: String): String {
        val snapshot = profileDocument(uid).get().await()
            .takeIf { it.exists() }
            ?: firestore.collection(COLLECTION_USERS).document(uid).get().await()
        return snapshot.getString(FIELD_GRADE_LEVEL).orEmpty()
    }

    suspend fun updateGradeLevel(uid: String, gradeLevel: String) {
        profileDocument(uid)
            .set(
                mapOf(
                    FIELD_GRADE_LEVEL to gradeLevel.trim(),
                    FIELD_UPDATED_AT to Timestamp.now()
                ),
                SetOptions.merge()
            )
            .await()
        firestore.collection(COLLECTION_USERS).document(uid)
            .set(
                mapOf(
                    FIELD_GRADE_LEVEL to gradeLevel.trim(),
                    FIELD_UPDATED_AT to Timestamp.now()
                ),
                SetOptions.merge()
            )
            .await()
    }

    suspend fun saveDisplayName(uid: String, email: String, displayName: String, avatar: String = "") {
        val data = mutableMapOf(
            FIELD_UID to uid,
            FIELD_EMAIL to email.trim(),
            FIELD_DISPLAY_NAME to displayName.trim(),
            FIELD_UPDATED_AT to Timestamp.now()
        )
        if (avatar.isNotBlank()) {
            data[FIELD_AVATAR] = avatar
        }
        profileDocument(uid).set(data, SetOptions.merge()).await()
        firestore.collection(COLLECTION_USERS).document(uid).set(data, SetOptions.merge()).await()
    }

    suspend fun loadAvatar(uid: String): String {
        val snapshot = profileDocument(uid).get().await()
            .takeIf { it.exists() }
            ?: firestore.collection(COLLECTION_USERS).document(uid).get().await()
        return snapshot.getString(FIELD_AVATAR).orEmpty()
    }

    suspend fun loadDisplayName(uid: String): String {
        val snapshot = profileDocument(uid).get().await()
            .takeIf { it.exists() }
            ?: firestore.collection(COLLECTION_USERS).document(uid).get().await()
        return snapshot.getString(FIELD_DISPLAY_NAME).orEmpty()
    }

    fun clearCache() {
        userPreferences.clear()
    }

    fun listenProfile(uid: String, onResult: (Result<Map<String, Any?>>) -> Unit): ListenerRegistration {
        return firestore.collection(COLLECTION_USERS).document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onResult(Result.failure(error))
                    return@addSnapshotListener
                }
                onResult(Result.success(snapshot?.data ?: emptyMap()))
            }
    }

    fun profileDocumentForDialog(uid: String) = firestore.collection(COLLECTION_USERS).document(uid)

    private companion object {
        const val COLLECTION_USERS = "users"
        const val COLLECTION_PROFILE = "profile"
        const val DOCUMENT_PROFILE = "main"
        const val FIELD_UID = "uid"
        const val FIELD_EMAIL = "email"
        const val FIELD_ROLE = "role"
        const val FIELD_CURRICULUM_TYPE = "curriculum_type"
        const val FIELD_GRADE_LEVEL = "gradeLevel"
        const val FIELD_DISPLAY_NAME = "displayName"
        const val FIELD_AVATAR = "avatar"
        const val FIELD_UPDATED_AT = "updatedAt"
    }

    private fun profileDocument(uid: String) =
        firestore.collection(COLLECTION_USERS).document(uid)
            .collection(COLLECTION_PROFILE).document(DOCUMENT_PROFILE)
}
