package com.example.bataanexplorer

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FavoriteItem(
    @SerialName("id") val id: Long = 0,
    @SerialName("user_id") val userId: String = "",
    @SerialName("spot_id") val spotId: Long = 0
)