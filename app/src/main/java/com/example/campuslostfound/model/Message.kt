package com.example.campuslostfound.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Message(
    val id: String = "",
    @SerialName("chat_id")
    val chatId: String = "",
    @SerialName("sender_id")
    val senderId: String = "",
    val content: String = "",
    /** Epoch milliseconds (UTC) — was a Firestore Timestamp. */
    @SerialName("created_at")
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
data class Chat(
    val id: String = "",
    @SerialName("participant_ids")
    val participantIds: List<String> = emptyList(),
    @SerialName("participant_names")
    val participantNames: Map<String, String> = emptyMap(), // UID to Name mapping
    @SerialName("last_message")
    val lastMessage: String = "",
    /** Epoch milliseconds (UTC) — was a Firestore Timestamp. */
    @SerialName("last_timestamp")
    val lastTimestamp: Long = System.currentTimeMillis()
)
