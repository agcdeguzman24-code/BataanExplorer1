package com.example.bataanexplorer

import android.Manifest
import android.app.AlarmManager
import android.app.DatePickerDialog
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class travelplan5 : AppCompatActivity() {

    private lateinit var btnCloseQuiz: MaterialButton
    private lateinit var edtDestination: TextInputEditText
    private lateinit var edtDate: TextInputEditText
    private lateinit var dropdownActivity: AutoCompleteTextView
    private lateinit var dropdownReminder: AutoCompleteTextView
    private lateinit var btnDelete: MaterialButton
    private lateinit var btnSaveChanges: MaterialButton

    private var tripId: Long = -1L
    private var selectedMunicipalityId: Long = -1L
    private var selectedSpotId: Long = -1L
    private var selectedSpotName: String = ""

    private val calendar: Calendar = Calendar.getInstance()
    private val supabase: SupabaseClient get() = SupabaseClientProvider.client

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (!isGranted) {
            Toast.makeText(this, "Notification permission needed for reminders.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_travelplan5)

        try {
            tripId = intent.getLongExtra("TRIP_ID", -1L)
            selectedMunicipalityId = intent.getLongExtra("SELECTED_MUNICIPALITY_ID", -1L)
            selectedSpotId = intent.getLongExtra("SPOT_ID", intent.getLongExtra("SELECTED_SPOT_ID", -1L))

            selectedSpotName = intent.getStringExtra("SELECTED_SPOT_NAME") ?: ""
            val travelDate = intent.getStringExtra("TRAVEL_DATE") ?: ""
            val activityType = intent.getStringExtra("PLANNED_ACTIVITIES")
                ?: intent.getStringExtra("ACTIVITY_TYPE") ?: ""
            val reminder = intent.getStringExtra("REMINDER_OPTION")
                ?: intent.getStringExtra("REMINDER") ?: ""

            initViews()
            setupDropdowns()
            setupDatePicker()
            checkNotificationPermission()

            edtDestination.setText(selectedSpotName)
            edtDate.setText(travelDate)

            if (travelDate.isNotBlank()) {
                parseAndSetCalendarDate(travelDate)
            }

            if (activityType.isNotBlank()) {
                dropdownActivity.setText(activityType, false)
            }
            if (reminder.isNotBlank()) {
                dropdownReminder.setText(reminder, false)
            }

            btnCloseQuiz.setOnClickListener {
                val intent = Intent(this@travelplan5, home::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(intent)
                finish()
            }

            btnSaveChanges.setOnClickListener {
                updateTripInSupabase()
            }

            btnDelete.setOnClickListener {
                confirmDeleteTrip()
            }

        } catch (e: Exception) {
            Log.e("travelplan5", "Error in onCreate: ${e.message}", e)
            Toast.makeText(this, "Error loading trip details: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    private fun initViews() {
        btnCloseQuiz = findViewById(R.id.btnCloseQuiz)
        val cardCloseQuiz: MaterialCardView? = findViewById(R.id.cardCloseQuiz)
        cardCloseQuiz?.bringToFront()

        edtDestination = findViewById(R.id.edtDestination)
        edtDate = findViewById(R.id.edtDate)
        dropdownActivity = findViewById(R.id.dropdownActivity)
        dropdownReminder = findViewById(R.id.dropdownReminder)
        btnDelete = findViewById(R.id.btnDelete)
        btnSaveChanges = findViewById(R.id.btnSaveChanges)
    }

    private fun setupDropdowns() {
        val activities = arrayOf(
            "Beach",
            "Sightseeing",
            "Food Trip",
            "Trekking",
            "Historical",
            "Swimming",
            "Relaxation"
        )
        val activityAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, activities)
        dropdownActivity.setAdapter(activityAdapter)

        val reminders = arrayOf("1 day before", "2 days before", "On the day of trip", "1 week before")
        val reminderAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, reminders)
        dropdownReminder.setAdapter(reminderAdapter)
    }

    private fun setupDatePicker() {
        edtDate.setOnClickListener {
            val dateSetListener = DatePickerDialog.OnDateSetListener { _, year, monthOfYear, dayOfMonth ->
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, monthOfYear)
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                calendar.set(Calendar.HOUR_OF_DAY, 8)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)

                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                edtDate.setText(dateFormat.format(calendar.time))
            }

            val datePickerDialog = DatePickerDialog(
                this,
                dateSetListener,
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )

            datePickerDialog.datePicker.minDate = System.currentTimeMillis() - 1000
            datePickerDialog.show()
        }
    }

    private fun parseAndSetCalendarDate(dateStr: String) {
        val formats = arrayOf(
            SimpleDateFormat("yyyy-MM-dd", Locale.US),
            SimpleDateFormat("MMMM dd, yyyy", Locale.US),
            SimpleDateFormat("MMM dd, yyyy", Locale.US)
        )
        for (format in formats) {
            try {
                val parsedDate = format.parse(dateStr)
                if (parsedDate != null) {
                    calendar.time = parsedDate
                    calendar.set(Calendar.HOUR_OF_DAY, 8)
                    calendar.set(Calendar.MINUTE, 0)
                    calendar.set(Calendar.SECOND, 0)
                    break
                }
            } catch (_: Exception) { }
        }
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun updateTripInSupabase() {
        val updatedDateStr = edtDate.text.toString().trim()
        val updatedActivity = dropdownActivity.text.toString().trim()
        val updatedReminder = dropdownReminder.text.toString().trim()

        if (updatedDateStr.isEmpty()) {
            Toast.makeText(this, "Please select a travel date!", Toast.LENGTH_SHORT).show()
            return
        }

        parseAndSetCalendarDate(updatedDateStr)

        val formattedDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(calendar.time)

        val updatePayload = buildJsonObject {
            put("travel_date", formattedDate)
            put("planned_activities", updatedActivity)
            put("reminder_option", updatedReminder)
            put("reminder_sent", false)
        }

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                supabase.from("travel_plans").update(updatePayload) {
                    filter {
                        when {
                            tripId > 0L -> eq("id", tripId)
                            selectedSpotId > 0L -> eq("spot_id", selectedSpotId)
                            else -> eq("spot_name", selectedSpotName)
                        }
                    }
                }

                scheduleTripReminder(updatedReminder, selectedSpotName)

                withContext(Dispatchers.Main) {
                    Toast.makeText(this@travelplan5, "Trip updated successfully!", Toast.LENGTH_SHORT).show()
                    finish()
                }
            } catch (e: Exception) {
                Log.e("travelplan5", "Error updating trip: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@travelplan5, "Failed to update: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun confirmDeleteTrip() {
        AlertDialog.Builder(this)
            .setTitle("Delete Trip")
            .setMessage("Are you sure you want to delete this travel plan?")
            .setPositiveButton("Delete") { _, _ -> deleteTripFromSupabase() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteTripFromSupabase() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                supabase.from("travel_plans").delete {
                    filter {
                        when {
                            tripId > 0L -> eq("id", tripId)
                            selectedSpotId > 0L -> eq("spot_id", selectedSpotId)
                            else -> eq("spot_name", selectedSpotName)
                        }
                    }
                }

                withContext(Dispatchers.Main) {
                    Toast.makeText(this@travelplan5, "Trip deleted successfully!", Toast.LENGTH_SHORT).show()
                    finish()
                }
            } catch (e: Exception) {
                Log.e("travelplan5", "Error deleting trip: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@travelplan5, "Failed to delete: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun scheduleTripReminder(reminderOption: String, destination: String) {
        if (reminderOption.isBlank()) return

        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val reminderCalendar = calendar.clone() as Calendar

        when (reminderOption) {
            "1 day before" -> reminderCalendar.add(Calendar.DAY_OF_YEAR, -1)
            "2 days before" -> reminderCalendar.add(Calendar.DAY_OF_YEAR, -2)
            "1 week before" -> reminderCalendar.add(Calendar.DAY_OF_YEAR, -7)
            "On the day of trip" -> { }
        }

        val triggerTime = reminderCalendar.timeInMillis
        if (triggerTime <= System.currentTimeMillis()) return

        val requestCode = if (tripId > 0L) tripId.toInt() else System.currentTimeMillis().toInt()
        val intent = Intent(this, ReminderReceiver::class.java).apply {
            putExtra("PLAN_TITLE", destination)
            putExtra("NOTIFICATION_ID", requestCode)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            this,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            }
        } catch (e: SecurityException) {
            Log.e("travelplan5", "SecurityException setting alarm: ${e.message}")
            Toast.makeText(this, "Unable to schedule exact reminder.", Toast.LENGTH_SHORT).show()
        }
    }
}