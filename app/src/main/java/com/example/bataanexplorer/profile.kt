package com.example.bataanexplorer

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Serializable
data class UserData(
    @SerialName("id") val id: String = "",
    @SerialName("full_name") val full_name: String? = null,
    @SerialName("email") val email: String? = null,
    @SerialName("contact_number") val contact_number: String? = null,
    @SerialName("location") val location: String? = null
)

class profile : AppCompatActivity() {

    // Views
    private lateinit var txtProfileName: TextView
    private lateinit var txtProfileEmail: TextView
    private lateinit var txtProfileLocation: TextView
    private lateinit var txtFullName: TextView
    private lateinit var txtEmail: TextView
    private lateinit var txtContact: TextView
    private lateinit var txtLocation: TextView

    // Saved Destinations
    private lateinit var recyclerSavedDestinations: RecyclerView
    private lateinit var savedAdapter: destinationadapter
    private val savedList = ArrayList<Destination>()
    private var emptySavedView: TextView? = null

    // Travel Plans
    private lateinit var recyclerTravelPlans: RecyclerView
    private lateinit var travelPlanAdapter: TravelPlanAdapter
    private val travelPlanList = ArrayList<UserTravelPlan>()
    private var emptyTravelPlanView: TextView? = null

    // Reviews (Optional)
    private var recyclerReviews: RecyclerView? = null
    private var emptyReviewView: TextView? = null

    private val supabase: SupabaseClient get() = SupabaseClientProvider.client
    private var currentUserId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_profile)

        initViews()
        setupSavedDestinations()
        setupTravelPlansRecyclerView()
        setupReviewsSection()
        setupClickListeners()

        loadUserProfile()
    }

    override fun onResume() {
        super.onResume()
        if (currentUserId != null) {
            loadSavedDestinations()
            loadUserTravelPlans()
            loadUserReviews()
        }
    }

    private fun initViews() {
        txtProfileName = findViewById(R.id.txtProfileName)
        txtProfileEmail = findViewById(R.id.txtProfileEmail)
        txtProfileLocation = findViewById(R.id.txtProfileLocation)
        txtFullName = findViewById(R.id.txtFullName)
        txtEmail = findViewById(R.id.txtEmail)
        txtContact = findViewById(R.id.txtContact)
        txtLocation = findViewById(R.id.txtLocation)
    }

    private fun setupClickListeners() {
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnSearch)?.setOnClickListener {
            startActivity(Intent(this@profile, search::class.java))
        }

        findViewById<TextView>(R.id.btnSeeAllPlans)?.setOnClickListener {
            startActivity(Intent(this@profile, travelplan1::class.java))
        }

        findViewById<LinearLayout>(R.id.navHome)?.setOnClickListener {
            startActivity(Intent(this@profile, home::class.java))
            finish()
        }

        findViewById<LinearLayout>(R.id.navPlanner)?.setOnClickListener {
            startActivity(Intent(this@profile, travelplan1::class.java))
            finish()
        }

        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnLogout)?.setOnClickListener {
            logoutUser()
        }

        // Profile Editing Listeners
        findViewById<TextView>(R.id.btnEditName)?.setOnClickListener {
            showEditDialog("Edit Full Name", txtFullName.text.toString(), InputType.TYPE_CLASS_TEXT) { newValue ->
                updateProfileField("full_name", newValue) {
                    txtFullName.text = newValue
                    txtProfileName.text = newValue
                }
            }
        }

        findViewById<TextView>(R.id.btnEditEmail)?.setOnClickListener {
            showEditDialog("Edit Email", txtEmail.text.toString(), InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS) { newValue ->
                updateProfileField("email", newValue) {
                    txtEmail.text = newValue
                    txtProfileEmail.text = newValue
                }
            }
        }

        findViewById<TextView>(R.id.btnEditContact)?.setOnClickListener {
            val currentVal = if (txtContact.text.toString() == "No contact number set") "" else txtContact.text.toString()
            showEditDialog("Edit Contact Number", currentVal, InputType.TYPE_CLASS_PHONE) { newValue ->
                updateProfileField("contact_number", newValue) {
                    txtContact.text = newValue.ifEmpty { "No contact number set" }
                }
            }
        }

        findViewById<TextView>(R.id.btnEditLocation)?.setOnClickListener {
            val currentVal = if (txtLocation.text.toString() == "No location set") "" else txtLocation.text.toString()
            showEditDialog("Edit Location", currentVal, InputType.TYPE_CLASS_TEXT) { newValue ->
                updateProfileField("location", newValue) {
                    val displayVal = newValue.ifEmpty { "No location set" }
                    txtLocation.text = displayVal
                    txtProfileLocation.text = displayVal
                }
            }
        }
    }

    private fun setupSavedDestinations() {
        recyclerSavedDestinations = findViewById(R.id.recyclerSavedDestinations)
        savedAdapter = destinationadapter(savedList) { selectedDestination ->
            val intent = Intent(this@profile, destinationdetails::class.java).apply {
                putExtra("EXTRA_DESTINATION", selectedDestination)
            }
            startActivity(intent)
        }
        recyclerSavedDestinations.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        recyclerSavedDestinations.adapter = savedAdapter
    }

    private fun setupTravelPlansRecyclerView() {
        recyclerTravelPlans = findViewById(R.id.recyclerTravelPlans)
        travelPlanAdapter = TravelPlanAdapter(travelPlanList) { selectedPlan ->
            val intent = Intent(this@profile, travelplan5::class.java).apply {
                putExtra("TRIP_ID", selectedPlan.planId)
                putExtra("SPOT_ID", selectedPlan.spotId ?: -1L)
                putExtra("SELECTED_SPOT_NAME", selectedPlan.spotName)
                putExtra("SELECTED_SPOT_LOCATION", selectedPlan.spotLocation)
                putExtra("TRAVEL_DATE", selectedPlan.travelDate)
                putExtra("PLANNED_ACTIVITIES", selectedPlan.activities)
                putExtra("REMINDER_OPTION", selectedPlan.reminderOption)
                putExtra("NOTES", selectedPlan.notes)
            }
            startActivity(intent)
        }
        recyclerTravelPlans.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        recyclerTravelPlans.adapter = travelPlanAdapter
    }

    private fun setupReviewsSection() {
        recyclerReviews = findViewById(R.id.recyclerReviews)
        recyclerReviews?.layoutManager = LinearLayoutManager(this)
    }

    private fun loadUserProfile() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val userSession = supabase.auth.currentUserOrNull() ?: return@launch
                currentUserId = userSession.id
                val accountEmail = userSession.email ?: ""

                val fetchedUser = supabase.postgrest["users"]
                    .select {
                        filter { eq("id", currentUserId!!) }
                    }.decodeSingleOrNull<UserData>()

                withContext(Dispatchers.Main) {
                    val displayEmail = fetchedUser?.email?.ifBlank { accountEmail } ?: accountEmail
                    txtEmail.text = displayEmail
                    txtProfileEmail.text = displayEmail

                    val displayName = fetchedUser?.full_name?.ifBlank { null }
                        ?: accountEmail.substringBefore("@")
                    txtFullName.text = displayName
                    txtProfileName.text = displayName

                    val contactVal = fetchedUser?.contact_number
                    txtContact.text = if (!contactVal.isNullOrBlank()) contactVal else "No contact number set"

                    val locationVal = fetchedUser?.location
                    val displayLocation = if (!locationVal.isNullOrBlank()) locationVal else "No location set"
                    txtLocation.text = displayLocation
                    txtProfileLocation.text = displayLocation
                }

                loadSavedDestinations()
                loadUserTravelPlans()
                loadUserReviews()

            } catch (e: Exception) {
                Log.e("ProfileActivity", "Error loading user data: ${e.message}", e)
            }
        }
    }

    private fun loadSavedDestinations() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val userId = currentUserId ?: supabase.auth.currentUserOrNull()?.id ?: return@launch

                val favoriteRows = supabase.from("favorites")
                    .select { filter { eq("user_id", userId) } }
                    .decodeList<FavoriteItem>()

                val spotIds = favoriteRows.map { it.spotId }

                val fetchedSpots = if (spotIds.isNotEmpty()) {
                    supabase.from("tourist_spots")
                        .select { filter { isIn("id", spotIds) } }
                        .decodeList<Destination>()
                } else emptyList()

                withContext(Dispatchers.Main) {
                    savedAdapter.updateList(fetchedSpots)

                    // TOGGLE VIA KOTLIN DIRECTLY
                    toggleEmptyState(
                        recyclerView = recyclerSavedDestinations,
                        emptyTextView = emptySavedView,
                        message = "No saved destinations yet",
                        isEmpty = fetchedSpots.isEmpty()
                    ) { createdView -> emptySavedView = createdView }
                }
            } catch (e: Exception) {
                Log.e("ProfileActivity", "Error loading saved destinations: ${e.message}", e)
            }
        }
    }

    private fun loadUserTravelPlans() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val userId = currentUserId ?: supabase.auth.currentUserOrNull()?.id ?: return@launch

                val fetchedPlans = supabase.from("travel_plans")
                    .select { filter { eq("user_id", userId) } }
                    .decodeList<UserTravelPlan>()

                val todayCalendar = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val todayDate = todayCalendar.time

                val pendingPlans = fetchedPlans.filter { plan ->
                    val parsedDate = parseTravelDate(plan.travelDate)
                    parsedDate == null || !parsedDate.before(todayDate)
                }

                val spotIds = pendingPlans.mapNotNull { it.spotId }.distinct()

                if (spotIds.isNotEmpty()) {
                    val spots = supabase.from("tourist_spots")
                        .select { filter { isIn("id", spotIds) } }
                        .decodeList<Destination>()

                    val spotMap = spots.associateBy { it.id }

                    pendingPlans.forEach { plan ->
                        val matchedSpot = spotMap[plan.spotId]
                        if (matchedSpot != null) {
                            plan.spotName = matchedSpot.name
                            plan.spotLocation = matchedSpot.location
                        } else {
                            plan.spotName = plan.activities.ifBlank { "Travel Plan #${plan.planId}" }
                            plan.spotLocation = plan.travelDate
                        }
                    }
                }

                withContext(Dispatchers.Main) {
                    travelPlanAdapter.updateData(pendingPlans)

                    // TOGGLE VIA KOTLIN DIRECTLY
                    toggleEmptyState(
                        recyclerView = recyclerTravelPlans,
                        emptyTextView = emptyTravelPlanView,
                        message = "No travel plans yet",
                        isEmpty = pendingPlans.isEmpty()
                    ) { createdView -> emptyTravelPlanView = createdView }
                }
            } catch (e: Exception) {
                Log.e("ProfileActivity", "Error loading travel plans: ${e.message}", e)
            }
        }
    }

    private fun loadUserReviews() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val userId = currentUserId ?: supabase.auth.currentUserOrNull()?.id ?: return@launch

                // Fetch user reviews if applicable
                val reviews = emptyList<String>()

                withContext(Dispatchers.Main) {
                    recyclerReviews?.let { recycler ->
                        toggleEmptyState(
                            recyclerView = recycler,
                            emptyTextView = emptyReviewView,
                            message = "No reviews yet",
                            isEmpty = reviews.isEmpty()
                        ) { createdView -> emptyReviewView = createdView }
                    }
                }
            } catch (e: Exception) {
                Log.e("ProfileActivity", "Error loading reviews: ${e.message}", e)
            }
        }
    }

    // Helper Function na gagawa at magtatago ng TextView sa Kotlin gamit ang parent ng RecyclerView
    private fun toggleEmptyState(
        recyclerView: RecyclerView,
        emptyTextView: TextView?,
        message: String,
        isEmpty: Boolean,
        onViewCreated: (TextView) -> Unit
    ) {
        val parent = recyclerView.parent as? ViewGroup ?: return
        var view = emptyTextView

        if (view == null) {
            view = TextView(this).apply {
                text = message
                textSize = 14f
                setTextColor(android.graphics.Color.GRAY)
                gravity = Gravity.CENTER
                setPadding(16, 24, 16, 24)
            }
            val index = parent.indexOfChild(recyclerView)
            parent.addView(view, index, ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ))
            onViewCreated(view)
        }

        if (isEmpty) {
            recyclerView.visibility = View.GONE
            view.visibility = View.VISIBLE
        } else {
            recyclerView.visibility = View.VISIBLE
            view.visibility = View.GONE
        }
    }

    private fun updateProfileField(column: String, newValue: String, onSuccess: () -> Unit) {
        val userId = currentUserId ?: return

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                supabase.postgrest["users"].update(
                    mapOf(column to newValue)
                ) {
                    filter { eq("id", userId) }
                }

                withContext(Dispatchers.Main) {
                    onSuccess()
                    Toast.makeText(this@profile, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("ProfileActivity", "Error updating $column: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@profile, "Failed to update profile", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun parseTravelDate(dateStr: String): Date? {
        if (dateStr.isBlank()) return null
        val formats = arrayOf(
            SimpleDateFormat("yyyy-MM-dd", Locale.US),
            SimpleDateFormat("MMMM dd, yyyy", Locale.US),
            SimpleDateFormat("MMM dd, yyyy", Locale.US)
        )
        for (format in formats) {
            try {
                return format.parse(dateStr)
            } catch (_: Exception) { }
        }
        return null
    }

    private fun showEditDialog(title: String, currentValue: String, inputType: Int, onSave: (String) -> Unit) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(title)

        val input = EditText(this).apply {
            this.inputType = inputType
            setText(currentValue)
        }
        builder.setView(input)

        builder.setPositiveButton("Save") { dialog, _ ->
            onSave(input.text.toString().trim())
            dialog.dismiss()
        }
        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }
        builder.show()
    }

    private fun logoutUser() {
        lifecycleScope.launch {
            try {
                supabase.auth.signOut()
            } catch (e: Exception) {
                Log.e("ProfileActivity", "Signout error: ${e.message}")
            }
            val intent = Intent(this@profile, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            finish()
        }
    }
}