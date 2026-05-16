package com.nimmaguru.app.data.badge

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.nimmaguru.app.domain.model.ProgressSummary

class BadgeManager(
    private val firestore: FirebaseFirestore
) {
    fun listenBadges(
        uid: String,
        onResult: (Result<List<String>>) -> Unit
    ): ListenerRegistration {
        return firestore.collection(COLLECTION_USERS).document(uid)
            .collection(COLLECTION_PROGRESS).document(DOCUMENT_SUMMARY)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onResult(Result.failure(error))
                    return@addSnapshotListener
                }
                val badges = (snapshot?.get(FIELD_BADGES) as? List<*>)
                    ?.filterIsInstance<String>()
                    ?: emptyList()
                onResult(Result.success(badges))
            }
    }

    fun hasUploadBadge(summary: ProgressSummary): Boolean {
        return summary.badges.contains(UPLOAD_BADGE)
    }

    fun listenGuruThankYouBadges(
        guruUid: String,
        onResult: (Result<List<String>>) -> Unit
    ): ListenerRegistration {
        return firestore.collection(COLLECTION_GURU_PROFILES).document(guruUid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onResult(Result.failure(error))
                    return@addSnapshotListener
                }
                val thankYouCount = snapshot?.getLong(FIELD_THANK_YOU_COUNT)?.toInt() ?: 0
                val badges = badgesForThankYouCount(thankYouCount)
                snapshot?.reference?.set(mapOf(FIELD_BADGES to badges), SetOptions.merge())
                onResult(Result.success(badges))
            }
    }

    fun badgesForThankYouCount(count: Int): List<String> {
        return buildList {
            if (count >= 1) add(BRONZE_BADGE)
            if (count >= 5) add(SILVER_BADGE)
            if (count >= 15) add(GOLD_BADGE)
            if (count >= 30) add(DIAMOND_BADGE)
        }
    }

    companion object {
        const val UPLOAD_BADGE = "Upload Badge"
        const val CONTRIBUTOR_BADGE = "Contributor"
        const val COMMUNITY_PILLAR = "Community Pillar"
        const val KNOWLEDGE_DONOR = "Knowledge Donor"
        const val TOP_RATED = "Top Rated"
        const val BRONZE_BADGE = "Bronze Guru"
        const val SILVER_BADGE = "Silver Guru"
        const val GOLD_BADGE = "Gold Guru"
        const val DIAMOND_BADGE = "Diamond Guru"
        private const val COLLECTION_USERS = "users"
        private const val COLLECTION_GURU_PROFILES = "guruProfiles"
        private const val COLLECTION_PROGRESS = "progress"
        private const val DOCUMENT_SUMMARY = "summary"
        private const val FIELD_BADGES = "badges"
        private const val FIELD_THANK_YOU_COUNT = "thankYouCount"
    }
}
