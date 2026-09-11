package com.example.bataanexplorer

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserPreference(
    @SerialName("user_id") val userId: String = "",
    @SerialName("municipalities") val municipalities: List<String> = emptyList(),
    @SerialName("place_types") val placeTypes: List<String> = emptyList(),
    @SerialName("companions") val companions: List<String> = emptyList(),
    @SerialName("is_skipped") val isSkipped: Boolean = false
)