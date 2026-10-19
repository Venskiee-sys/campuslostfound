package com.example.campuslostfound.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Notification(
    val id: String = "",
    @SerialName("user_id")
    val userId: String = "", // Recipient
    val title: String = "",
    val message: String = "",
    val type: String = "Info", // "Message", "Resolved", "System"
    /** Epoch milliseconds (UTC) — was a Firestore Timestamp. */
    @SerialName("created_at")
    val timestamp: Long = System.currentTimeMillis(),
    val seen: Boolean = false,
    @SerialName("sender_name")
    val senderName: String? = null, // Added to show real name
    @SerialName("related_id")
    val relatedId: String? = null // e.g. Chat ID or Item ID
)
