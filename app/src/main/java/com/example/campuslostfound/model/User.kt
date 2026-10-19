package com.example.campuslostfound.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class User(
    val uid: String = "",
    @SerialName("full_name")
    val fullName: String = "",
    @SerialName("student_id")
    val studentId: String = "",
    val email: String = "",
    val course: String = "",
    @SerialName("year_level")
    val yearLevel: String = "",
    @SerialName("phone_number")
    val phoneNumber: String = ""
)
