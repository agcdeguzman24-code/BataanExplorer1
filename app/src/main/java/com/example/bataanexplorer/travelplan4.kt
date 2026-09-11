package com.example.bataanexplorer

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import java.text.SimpleDateFormat
import java.util.Locale

@Serializable
data class TravelPlan(
    val user_id: String? = null,
    val municipality_id: Long,
    val spot_id: Long,
    val travel_date: String,
    val planned_activities: String,
    val reminder_option: String? = null,
    val is_completed: Boolean = false,
    val reminder_sent: Boolean = false
)

@Serializable
data class TouristSpotIdResult(
    val id: Long,
    val municipality_id: Long
)

class travelplan4 : AppCompatActivity() {

    private lateinit var btnCloseQuiz: MaterialButton
    private lateinit var txtSummaryDestination: TextView
    private lateinit var txtSummaryDate: TextView
    private lateinit var txtSummaryActivity: TextView
    private lateinit var txtSummaryReminder: TextView
    private lateinit var btnPlanAnotherTrip: MaterialButton
    private lateinit var btnEditTrip: MaterialButton

    private var selectedMunicipalityId: Long = -1L
    private var selectedMunicipalityName: String = ""
    private var selectedSpotId: Long = -1L
    private var selectedSpotName: String = ""
    private var travelDate: String = ""
    private var activityType: String = ""
    private var reminder: String = ""

    private val supabase: SupabaseClient get() = SupabaseClientProvider.client

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_travelplan4)

        // 1. Tanggapin ang datos mula sa travelplan3
        selectedMunicipalityName = intent.getStringExtra("SELECTED_MUNICIPALITY_NAME") ?: ""
        selectedMunicipalityId = intent.getLongExtra("SELECTED_MUNICIPALITY_ID", -1L)
        selectedSpotName = intent.getStringExtra("SELECTED_SPOT_NAME") ?: ""
        selectedSpotId = intent.getLongExtra("SELECTED_SPOT_ID", -1L)
        travelDate = intent.getStringExtra("TRAVEL_DATE") ?: ""

        activityType = intent.getStringExtra("PLANNED_ACTIVITIES")
            ?: intent.getStringExtra("ACTIVITY_TYPE") ?: ""
        reminder = intent.getStringExtra("REMINDER_OPTION")
            ?: intent.getStringExtra("REMINDER") ?: ""

        initViews()
        displaySummaryData()

        // 2. I-save sa Supabase (may fallback lookup pag kulang ang ID)
        saveTripToSupabase()

        // 3. CLOSE BUTTON -> Direkta sa Home Screen
        btnCloseQuiz.setOnClickListener {
            val intent = Intent(this@travelplan4, home::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(intent)
            finish()
        }

        btnPlanAnotherTrip.setOnClickListener {
            val intent = Intent(this@travelplan4, travelplan1::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(intent)
            finish()
        }

        btnEditTrip.setOnClickListener {
            val intent = Intent(this@travelplan4, travelplan5::class.java).apply {
                putExtra("SELECTED_MUNICIPALITY_NAME", selectedMunicipalityName)
                putExtra("SELECTED_MUNICIPALITY_ID", selectedMunicipalityId)
                putExtra("SELECTED_SPOT_ID", selectedSpotId)
                putExtra("SELECTED_SPOT_NAME", selectedSpotName)
                putExtra("TRAVEL_DATE", travelDate)
                putExtra("PLANNED_ACTIVITIES", activityType)
                putExtra("REMINDER_OPTION", reminder)
            }
            startActivity(intent)
        }
    }

    private fun initViews() {
        btnCloseQuiz = findViewById(R.id.btnCloseQuiz)

        val cardCloseQuiz: MaterialCardView = findViewById(R.id.cardCloseQuiz)
        cardCloseQuiz.bringToFront()

        txtSummaryDestination = findViewById(R.id.txtSummaryDestination)
        txtSummaryDate = findViewById(R.id.txtSummaryDate)
        txtSummaryActivity = findViewById(R.id.txtSummaryActivity)
        txtSummaryReminder = findViewById(R.id.txtSummaryReminder)
        btnPlanAnotherTrip = findViewById(R.id.btnPlanAnotherTrip)
        btnEditTrip = findViewById(R.id.btnEditTrip)
    }

    private fun displaySummaryData() {
        txtSummaryDestination.text = if (selectedSpotName.isNotEmpty()) selectedSpotName else "Destination"
        txtSummaryDate.text = if (travelDate.isNotEmpty()) travelDate else "Not specified"
        txtSummaryActivity.text = if (activityType.isNotEmpty()) activityType else "General Tourism"
        txtSummaryReminder.text = if (reminder.isNotEmpty()) "Reminder: $reminder" else "No reminder set"
    }

    private fun saveTripToSupabase() {
        val currentUserId = supabase.auth.currentUserOrNull()?.id

        val formattedDate = try {
            val inputFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.US)
            val outputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val date = inputFormat.parse(travelDate)
            date?.let { outputFormat.format(it) } ?: travelDate
        } catch (e: Exception) {
            travelDate
        }

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Fallback Verification: Kung hindi napasa ang spot_id o municipality_id (-1L)
                var finalSpotId = selectedSpotId
                var finalMunicipalityId = selectedMunicipalityId

                if ((finalSpotId == -1L || finalMunicipalityId == -1L) && selectedSpotName.isNotEmpty()) {
                    val result = supabase.from("tourist_spots")
                        .select(Columns.list("id", "municipality_id")) {
                            filter {
                                eq("name", selectedSpotName)
                            }
                        }.decodeSingleOrNull<TouristSpotIdResult>()

                    if (result != null) {
                        finalSpotId = result.id
                        finalMunicipalityId = result.municipality_id
                    }
                }

                if (finalSpotId == -1L || finalMunicipalityId == -1L) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@travelplan4,
                            "Cannot save plan: Invalid spot or municipality selected.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    return@launch
                }

                val plan = TravelPlan(
                    user_id = currentUserId,
                    municipality_id = finalMunicipalityId,
                    spot_id = finalSpotId,
                    travel_date = formattedDate,
                    planned_activities = activityType,
                    reminder_option = reminder,
                    is_completed = false,
                    reminder_sent = false
                )

                supabase.from("travel_plans").insert(plan)

                withContext(Dispatchers.Main) {
                    Toast.makeText(this@travelplan4, "Trip saved to database!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("travelplan4", "Error saving trip: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@travelplan4, "Failed to save: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}