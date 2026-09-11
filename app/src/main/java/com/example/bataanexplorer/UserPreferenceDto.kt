package com.example.bataanexplorer

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserPreferenceDto(
    @SerialName("user_id") val userId: String,
    @SerialName("municipalities") val municipalities: List<String>? = null,
    @SerialName("place_types") val placeTypes: List<String>? = null,
    @SerialName("companions") val companions: List<String>? = null,
    @SerialName("is_skipped") val isSkipped: Boolean? = null
)