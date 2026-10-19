package com.example.campuslostfound.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Item(
    val id: String = "",
    val name: String = "",
    val category: String = "",
    val location: String = "",
    val date: String = "",
    val description: String = "",
    val type: String = "", // "Lost" or "Found"
    @SerialName("reported_by")
    val reportedBy: String = "", // User UID
    @SerialName("reporter_name")
    val reporterName: String = "",
    /** Epoch milliseconds (UTC) — was a Firestore Timestamp. */
    @SerialName("created_at")
    val timestamp: Long = System.currentTimeMillis(),
    @SerialName("image_url")
    val imageUrl: String? = null,
    val status: String = "Open" // "Open" or "Resolved"
)
