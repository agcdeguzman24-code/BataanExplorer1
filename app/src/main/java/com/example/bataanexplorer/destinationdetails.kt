package com.example.bataanexplorer

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.material.button.MaterialButton
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Count
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.io.Serializable as JavaSerializable

@Serializable
data class Destination(
    @SerialName("id") val id: Long = 0,
    @SerialName("municipality_id") val municipality_id: Long? = null,
    @SerialName("name") val name: String = "",
    @SerialName("category") val category: String = "",
    @SerialName("location") val location: String = "",
    @SerialName("is_open") val is_open: Boolean = true,
    @SerialName("rating") val rating: Double = 0.0,
    @SerialName("review_count") val review_count: Long = 0,
    @SerialName("description") val description: String = "",
    @SerialName("entrance_fee") val entrance_fee: String = "",
    @SerialName("operating_hours") val operating_hours: String = "",
    @SerialName("image_url") val image_url: String = "",

    @SerialName("extra_photo_1") val extraPhoto1: String = "",
    @SerialName("extra_photo_2") val extraPhoto2: String = "",
    @SerialName("extra_photo_3") val extraPhoto3: String = "",

    @SerialName("categories") val categories: List<String> = emptyList(),
    @SerialName("suitable_for") val suitableFor: List<String> = emptyList(),
    @SerialName("is_popular") val isPopular: Boolean = false,

    // NEW FIELDS FOR GOOGLE MAPS NAVIGATION
    @SerialName("latitude") val latitude: Double? = null,
    @SerialName("longitude") val longitude: Double? = null
) : JavaSerializable


class destinationdetails : AppCompatActivity() {

    private lateinit var imgPlace: ImageView
    private lateinit var txtPlaceName: TextView
    private lateinit var txtCategory: TextView
    private lateinit var txtOpenStatus: TextView
    private lateinit var txtLocation: TextView
    private lateinit var txtVisitorLocation: TextView
    private lateinit var txtRating: TextView
    private lateinit var txtImageRating: TextView
    private lateinit var txtReviews: TextView
    private lateinit var txtDescription: TextView
    private lateinit var txtEntranceFee: TextView
    private lateinit var txtOperatingHours: TextView
    private lateinit var btnCloseQuiz: MaterialButton

    // Get Directions Action (LinearLayout)
    private lateinit var actionGetDirections: LinearLayout

    // Action Planner / Plan Trip (LinearLayout o Button)
    private lateinit var actionPlanTrip: LinearLayout

    // Header Favorite (ImageButton)
    private lateinit var btnFavorite: ImageButton

    // Floating Action Bar Favorites (LinearLayout & ImageView)
    private lateinit var actionFavorite: LinearLayout
    private lateinit var imgActionFavorite: ImageView

    private lateinit var imgExtraPhoto1: ImageView
    private lateinit var imgExtraPhoto2: ImageView
    private lateinit var imgExtraPhoto3: ImageView

    private var isFavorite = false
    private var currentSpotId: Long? = null
    private var currentDestination: Destination? = null

    private val supabase: SupabaseClient get() = SupabaseClientProvider.client

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_destinationdetails)

        initViews()

        val destination = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra("EXTRA_DESTINATION", Destination::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getSerializableExtra("EXTRA_DESTINATION") as? Destination
        }

        if (destination != null) {
            currentDestination = destination
            currentSpotId = destination.id
            bindDataToUI(destination)
            checkFavoriteStatus()
        } else {
            Toast.makeText(this, "Destination details not found!", Toast.LENGTH_SHORT).show()
        }

        btnCloseQuiz.setOnClickListener {
            finish()
        }

        // Click Listener para sa Get Directions
        actionGetDirections.setOnClickListener {
            currentDestination?.let { dest -> openGoogleMapsNavigation(dest) }
        }

        // Click Listener para sa Action Planner / Plan Trip -> DIRECT TO TRAVELPLAN3
        actionPlanTrip.setOnClickListener {
            currentDestination?.let { dest ->
                val intent = Intent(this@destinationdetails, travelplan3::class.java).apply {
                    putExtra("SPOT_ID", dest.id)
                    putExtra("SPOT_NAME", dest.name)
                    putExtra("SPOT_LOCATION", dest.location)
                    putExtra("FROM_DESTINATION_DETAILS", true)
                }
                startActivity(intent)
            } ?: run {
                Toast.makeText(this, "Cannot create plan. Destination missing.", Toast.LENGTH_SHORT).show()
            }
        }

        // Favorites Click Listeners
        btnFavorite.setOnClickListener { toggleFavorite() }
        actionFavorite.setOnClickListener { toggleFavorite() }
    }

    private fun initViews() {
        imgPlace = findViewById(R.id.imgPlace)
        txtPlaceName = findViewById(R.id.txtPlaceName)
        txtCategory = findViewById(R.id.txtCategory)
        txtOpenStatus = findViewById(R.id.txtOpenStatus)
        txtLocation = findViewById(R.id.txtLocation)
        txtVisitorLocation = findViewById(R.id.txtVisitorLocation)
        txtRating = findViewById(R.id.txtRating)
        txtImageRating = findViewById(R.id.txtImageRating)
        txtReviews = findViewById(R.id.txtReviews)
        txtDescription = findViewById(R.id.txtDescription)
        txtEntranceFee = findViewById(R.id.txtEntranceFee)
        txtOperatingHours = findViewById(R.id.txtOperatingHours)
        btnCloseQuiz = findViewById(R.id.btnCloseQuiz)

        // Navigation & Planning Action Containers
        actionGetDirections = findViewById(R.id.actionDirections)
        actionPlanTrip = findViewById(R.id.actionAddPlanner) // Siguraduhing katugma ng android:id ng Planner/Plan button sa XML mo

        // Header Favorite
        btnFavorite = findViewById(R.id.btnFavorite)

        // Floating Action Bar Favorite
        actionFavorite = findViewById(R.id.actionFavorite)
        imgActionFavorite = findViewById(R.id.imgActionFavorite)

        imgExtraPhoto1 = findViewById(R.id.imgExtraPhoto1)
        imgExtraPhoto2 = findViewById(R.id.imgExtraPhoto2)
        imgExtraPhoto3 = findViewById(R.id.imgExtraPhoto3)
    }

    private fun bindDataToUI(item: Destination) {
        txtPlaceName.text = item.name.ifEmpty { "N/A" }
        txtCategory.text = item.category.ifEmpty { "General" }
        txtLocation.text = item.location.ifEmpty { "Bataan" }
        txtVisitorLocation.text = item.location.ifEmpty { "Bataan" }
        txtDescription.text = item.description.ifEmpty { "No description available." }
        txtEntranceFee.text = item.entrance_fee.ifEmpty { "Free / Not specified" }
        txtOperatingHours.text = item.operating_hours.ifEmpty { "Not specified" }

        txtRating.text = item.rating.toString()
        txtImageRating.text = item.rating.toString()
        txtReviews.text = "(${item.review_count} reviews)"

        txtOpenStatus.text = if (item.is_open) "OPEN NOW" else "CLOSED"

        loadImageSmoothly(item.image_url, imgPlace)
        loadImageSmoothly(item.extraPhoto1, imgExtraPhoto1)
        loadImageSmoothly(item.extraPhoto2, imgExtraPhoto2)
        loadImageSmoothly(item.extraPhoto3, imgExtraPhoto3)
    }

    private fun loadImageSmoothly(url: String, imageView: ImageView) {
        if (url.isNotBlank()) {
            Glide.with(this)
                .load(url)
                .centerCrop()
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(R.drawable.quiz_bg3)
                .error(R.drawable.quiz_bg3)
                .into(imageView)
        } else {
            imageView.setImageResource(R.drawable.quiz_bg3)
        }
    }

    private fun openGoogleMapsNavigation(dest: Destination) {
        val lat = dest.latitude
        val lng = dest.longitude

        if (lat != null && lng != null && lat != 0.0 && lng != 0.0) {
            val gmmIntentUri = Uri.parse("google.navigation:q=$lat,$lng")
            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri).apply {
                setPackage("com.google.android.apps.maps")
            }

            if (mapIntent.resolveActivity(packageManager) != null) {
                startActivity(mapIntent)
            } else {
                val browserUri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=$lat,$lng")
                startActivity(Intent(Intent.ACTION_VIEW, browserUri))
            }
        } else if (dest.name.isNotBlank()) {
            val encodedName = Uri.encode("${dest.name}, Bataan")
            val gmmIntentUri = Uri.parse("google.navigation:q=$encodedName")
            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri).apply {
                setPackage("com.google.android.apps.maps")
            }

            if (mapIntent.resolveActivity(packageManager) != null) {
                startActivity(mapIntent)
            } else {
                val browserUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=$encodedName")
                startActivity(Intent(Intent.ACTION_VIEW, browserUri))
            }
        } else {
            Toast.makeText(this, "Location details not available.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkFavoriteStatus() {
        val spotId = currentSpotId ?: return

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val currentUserId = supabase.auth.currentUserOrNull()?.id ?: return@launch

                val count = supabase.from("favorites")
                    .select {
                        filter {
                            eq("user_id", currentUserId)
                            eq("spot_id", spotId)
                        }
                        count(Count.EXACT)
                    }.countOrNull()

                withContext(Dispatchers.Main) {
                    isFavorite = count != null && count > 0
                    updateFavoriteIcon()
                }

            } catch (e: Exception) {
                Log.e("DestinationDetails", "Error checking favorite status: ${e.message}", e)
            }
        }
    }

    private fun toggleFavorite() {
        val spotId = currentSpotId ?: return

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val userId = supabase.auth.currentUserOrNull()?.id
                if (userId == null) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@destinationdetails, "Please log in first", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                if (isFavorite) {
                    supabase.from("favorites").delete {
                        filter {
                            eq("user_id", userId)
                            eq("spot_id", spotId)
                        }
                    }
                    isFavorite = false
                } else {
                    supabase.from("favorites").insert(
                        FavoriteItem(userId = userId, spotId = spotId)
                    )
                    isFavorite = true
                }

                withContext(Dispatchers.Main) {
                    updateFavoriteIcon()
                    val statusMsg = if (isFavorite) "Added to favorites" else "Removed from favorites"
                    Toast.makeText(this@destinationdetails, statusMsg, Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("DestinationDetails", "Error updating favorite: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@destinationdetails, "Error updating favorite: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun updateFavoriteIcon() {
        val iconRes = if (isFavorite) R.drawable.ic_favorite_filled else R.drawable.ic_favorite_outline

        btnFavorite.setImageResource(iconRes)
        imgActionFavorite.setImageResource(iconRes)
    }
}