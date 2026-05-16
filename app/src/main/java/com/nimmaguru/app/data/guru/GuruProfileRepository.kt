package com.nimmaguru.app.data.guru

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.nimmaguru.app.domain.model.GuruProfile
import kotlinx.coroutines.tasks.await

class GuruProfileRepository(
    private val firestore: FirebaseFirestore
) {
    suspend fun getProfile(uid: String): GuruProfile? {
        val snapshot = profilesDocument(uid).get().await()
        if (!snapshot.exists()) return null

        return snapshot.toGuruProfile(uid)
    }

    suspend fun saveProfile(profile: GuruProfile) {
        val data = hashMapOf(
            FIELD_UID to profile.uid,
            FIELD_ROLE to ROLE_GURU,
            FIELD_NAME to profile.name.trim(),
            FIELD_SKILLS to profile.skills,
            FIELD_EXPERIENCE to profile.experience.trim(),
            FIELD_AVAILABLE_TIME to profile.availableTime.trim(),
            FIELD_LOCATION to profile.location.trim(),
            FIELD_LOCATION_NORMALIZED to profile.location.normalized(),
            FIELD_SAMUDAYA_BHAVANA_AVAILABLE to profile.samudayaBhavanaAvailable,
            FIELD_SAMUDAYA_BHAVANA_ADDRESS to profile.samudayaBhavanaAddress.trim(),
            FIELD_BADGES to earnedBadges(profile),
            FIELD_UPDATED_AT to Timestamp.now()
        )

        profilesDocument(profile.uid)
            .set(data, SetOptions.merge())
            .await()
    }

    fun listenProfiles(
        skillFilters: List<String>,
        location: String,
        onResult: (Result<List<GuruProfile>>) -> Unit
    ): ListenerRegistration {
        // Firestore supports up to 10 values in array-contains-any, so keep filters bounded.
        val normalizedSkills = skillFilters
            .map(String::trim)
            .filter(String::isNotBlank)
            .distinct()
            .take(MAX_ARRAY_CONTAINS_ANY_VALUES)

        var query: Query = firestore
            .collection(COLLECTION_GURU_PROFILES)
            .whereEqualTo(FIELD_ROLE, ROLE_GURU)

        if (normalizedSkills.size == 1) {
            query = query.whereArrayContains(FIELD_SKILLS, normalizedSkills.first())
        } else if (normalizedSkills.size > 1) {
            query = query.whereArrayContainsAny(FIELD_SKILLS, normalizedSkills)
        }

        val normalizedLocation = location.normalized()
        if (normalizedLocation.isNotBlank()) {
            query = query.whereEqualTo(FIELD_LOCATION_NORMALIZED, normalizedLocation)
        }

        return query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                onResult(Result.failure(error))
                return@addSnapshotListener
            }

            // addSnapshotListener keeps the mentor list fresh without a manual refresh button.
            val profiles = snapshot
                ?.documents
                ?.map { it.toGuruProfile(it.id) }
                ?.sortedBy { it.name.lowercase() }
                ?: emptyList()

            onResult(Result.success(profiles))
        }
    }

    private fun profilesDocument(uid: String) =
        firestore.collection(COLLECTION_GURU_PROFILES).document(uid)

    private fun com.google.firebase.firestore.DocumentSnapshot.toGuruProfile(documentUid: String): GuruProfile {
        return GuruProfile(
            uid = getString(FIELD_UID).orEmpty().ifBlank { documentUid },
            name = getString(FIELD_NAME).orEmpty(),
            skills = (get(FIELD_SKILLS) as? List<*>)
                ?.filterIsInstance<String>()
                ?: emptyList(),
            experience = getString(FIELD_EXPERIENCE).orEmpty(),
            availableTime = getString(FIELD_AVAILABLE_TIME).orEmpty(),
            location = getString(FIELD_LOCATION).orEmpty(),
            samudayaBhavanaAvailable = getBoolean(FIELD_SAMUDAYA_BHAVANA_AVAILABLE) ?: false,
            samudayaBhavanaAddress = getString(FIELD_SAMUDAYA_BHAVANA_ADDRESS).orEmpty(),
            thankYouCount = getLong(FIELD_THANK_YOU_COUNT)?.toInt() ?: 0,
            classesTaken = getLong(FIELD_CLASSES_TAKEN)?.toInt() ?: 0,
            averageStudentReview = getDouble(FIELD_AVERAGE_STUDENT_REVIEW) ?: 0.0,
            badges = (get(FIELD_BADGES) as? List<*>)?.filterIsInstance<String>() ?: emptyList()
        )
    }

    private fun earnedBadges(profile: GuruProfile): List<String> {
        return buildList {
            if (profile.classesTaken >= 5) add(BADGE_COMMUNITY_PILLAR)
            if (profile.thankYouCount >= 1) add(BADGE_KNOWLEDGE_DONOR)
            if (profile.averageStudentReview >= 4.5) add(BADGE_TOP_RATED)
        }.ifEmpty { profile.badges }
    }

    private fun String.normalized(): String = trim().lowercase()

    private companion object {
        const val COLLECTION_GURU_PROFILES = "guruProfiles"
        const val ROLE_GURU = "guru"
        const val MAX_ARRAY_CONTAINS_ANY_VALUES = 10
        const val FIELD_UID = "uid"
        const val FIELD_ROLE = "role"
        const val FIELD_NAME = "name"
        const val FIELD_SKILLS = "skills"
        const val FIELD_EXPERIENCE = "experience"
        const val FIELD_AVAILABLE_TIME = "availableTime"
        const val FIELD_LOCATION = "location"
        const val FIELD_LOCATION_NORMALIZED = "locationNormalized"
        const val FIELD_SAMUDAYA_BHAVANA_AVAILABLE = "samudayaBhavanaAvailable"
        const val FIELD_SAMUDAYA_BHAVANA_ADDRESS = "samudayaBhavanaAddress"
        const val FIELD_THANK_YOU_COUNT = "thankYouCount"
        const val FIELD_CLASSES_TAKEN = "classesTaken"
        const val FIELD_AVERAGE_STUDENT_REVIEW = "averageStudentReview"
        const val FIELD_BADGES = "badges"
        const val FIELD_UPDATED_AT = "updatedAt"
        const val BADGE_COMMUNITY_PILLAR = "Community Pillar"
        const val BADGE_KNOWLEDGE_DONOR = "Knowledge Donor"
        const val BADGE_TOP_RATED = "Top Rated"
    }
}
