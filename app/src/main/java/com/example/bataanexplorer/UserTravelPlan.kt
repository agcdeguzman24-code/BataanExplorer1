package com.example.bataanexplorer

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class UserTravelPlan(
    @SerialName("id") val planId: Long = -1L,
    @SerialName("user_id") val userId: String = "",
    @SerialName("municipality_id") val municipalityId: Long? = null,
    @SerialName("spot_id") val spotId: Long? = null,
    @SerialName("travel_date") val travelDate: String = "",
    @SerialName("planned_activities") val activities: String = "",
    @SerialName("is_completed") val isCompleted: Boolean = false,
    @SerialName("reminder_sent") val reminderSent: Boolean = false,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("reminder_option") val reminderOption: String? = null,
    @SerialName("notes") val notes: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,

    // Transient fields para sa UI display
    @Transient var spotName: String = "",
    @Transient var spotLocation: String = "",
    @Transient var imageUrl: String? = null,
    @Transient var imageResId: Int = 0
)