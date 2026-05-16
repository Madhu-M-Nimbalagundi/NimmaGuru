package com.nimmaguru.app.data.guru

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.nimmaguru.app.domain.model.ChatMessage
import kotlinx.coroutines.tasks.await

class GuruChatRepository(
    private val firestore: FirebaseFirestore
) {
    fun listenMessages(
        currentGuruId: String,
        otherGuruId: String,
        onResult: (Result<List<ChatMessage>>) -> Unit
    ): ListenerRegistration {
        return chatDocument(currentGuruId, otherGuruId)
            .collection(COLLECTION_MESSAGES)
            .orderBy(FIELD_CREATED_AT, Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onResult(Result.failure(error))
                    return@addSnapshotListener
                }

                val messages = snapshot?.documents?.map { it.toChatMessage() } ?: emptyList()
                onResult(Result.success(messages))
            }
    }

    suspend fun sendMessage(
        currentGuruId: String,
        currentGuruName: String,
        otherGuruId: String,
        otherGuruName: String,
        text: String
    ) {
        val cleanText = text.trim()
        if (cleanText.isBlank()) return

        val now = Timestamp.now()
        val chatData = mapOf(
            FIELD_PARTICIPANTS to listOf(currentGuruId, otherGuruId).sorted(),
            FIELD_PARTICIPANT_NAMES to mapOf(
                currentGuruId to currentGuruName,
                otherGuruId to otherGuruName
            ),
            FIELD_LAST_MESSAGE to cleanText,
            FIELD_UPDATED_AT to now
        )

        val chat = chatDocument(currentGuruId, otherGuruId)
        chat.set(chatData, SetOptions.merge()).await()
        chat.collection(COLLECTION_MESSAGES).add(
            mapOf(
                FIELD_SENDER_ID to currentGuruId,
                FIELD_SENDER_NAME to currentGuruName,
                FIELD_TEXT to cleanText,
                FIELD_CREATED_AT to now
            )
        ).await()
    }

    private fun chatDocument(firstGuruId: String, secondGuruId: String) =
        firestore.collection(COLLECTION_GURU_CHATS).document(chatId(firstGuruId, secondGuruId))

    private fun chatId(firstGuruId: String, secondGuruId: String): String {
        return listOf(firstGuruId, secondGuruId).sorted().joinToString("_")
    }

    private fun DocumentSnapshot.toChatMessage(): ChatMessage {
        return ChatMessage(
            id = id,
            senderId = getString(FIELD_SENDER_ID).orEmpty(),
            senderName = getString(FIELD_SENDER_NAME).orEmpty(),
            text = getString(FIELD_TEXT).orEmpty(),
            createdAt = getTimestamp(FIELD_CREATED_AT)
        )
    }

    private companion object {
        const val COLLECTION_GURU_CHATS = "guruChats"
        const val COLLECTION_MESSAGES = "messages"
        const val FIELD_PARTICIPANTS = "participants"
        const val FIELD_PARTICIPANT_NAMES = "participantNames"
        const val FIELD_LAST_MESSAGE = "lastMessage"
        const val FIELD_UPDATED_AT = "updatedAt"
        const val FIELD_SENDER_ID = "senderId"
        const val FIELD_SENDER_NAME = "senderName"
        const val FIELD_TEXT = "text"
        const val FIELD_CREATED_AT = "createdAt"
    }
}
