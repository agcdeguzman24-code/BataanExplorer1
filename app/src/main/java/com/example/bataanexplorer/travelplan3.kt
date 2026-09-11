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
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class travelplan3 : AppCompatActivity() {

    private lateinit var btnCloseQuiz: MaterialButton
    private lateinit var txtDestination: TextView
    private lateinit var edtTravelDate: TextInputEditText
    private lateinit var dropdownActivity: AutoCompleteTextView
    private lateinit var dropdownReminder: AutoCompleteTextView
    private lateinit var btnSaveTrip: MaterialButton

    private var selectedMunicipalityId: Long = -1L
    private var selectedMunicipalityName: String = ""
    private var selectedSpotId: Long = -1L
    private var selectedSpotName: String = ""

    private val calendar = Calendar.getInstance()

    // Permission Launcher para sa Notifications (Android 13+)
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (!isGranted) {
            Toast.makeText(
                this,
                "Notification permission needed for reminders.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_travelplan3)

        // 1. Tanggapin ang intent extras (Suportado ang galing sa travelplan2 AT destinationdetails)
        selectedSpotId = intent.getLongExtra("SPOT_ID", intent.getLongExtra("SELECTED_SPOT_ID", -1L))
        selectedSpotName = intent.getStringExtra("SPOT_NAME")
            ?: intent.getStringExtra("SELECTED_SPOT_NAME")
                    ?: ""

        selectedMunicipalityId = intent.getLongExtra("SELECTED_MUNICIPALITY_ID", -1L)
        selectedMunicipalityName = intent.getStringExtra("SELECTED_MUNICIPALITY_NAME")
            ?: intent.getStringExtra("SPOT_LOCATION")
                    ?: ""

        initViews()
        setupDropdowns()
        setupDatePicker()
        checkNotificationPermission()

        if (selectedSpotName.isNotEmpty()) {
            txtDestination.text = selectedSpotName
        } else {
            txtDestination.text = "No spot selected"
        }

        // CLOSE BUTTON -> Home Screen
        btnCloseQuiz.setOnClickListener {
            val intent = Intent(this@travelplan3, home::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(intent)
            finish()
        }

        btnSaveTrip.setOnClickListener {
            val travelDate = edtTravelDate.text.toString().trim()
            val activityType = dropdownActivity.text.toString().trim()
            val reminderOption = dropdownReminder.text.toString().trim()

            if (travelDate.isEmpty()) {
                Toast.makeText(this, "Please select a travel date!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Schedule System Notification sa Phone
            scheduleTripReminder(reminderOption, selectedSpotName)

            // 2. Ipasa ang KUMPLETONG data papuntang Step 4 / Summary (Para ready na sa Database Insertion!)
            val intent = Intent(this@travelplan3, travelplan4::class.java).apply {
                putExtra("SELECTED_MUNICIPALITY_NAME", selectedMunicipalityName)
                putExtra("SELECTED_MUNICIPALITY_ID", selectedMunicipalityId)
                putExtra("SELECTED_SPOT_ID", selectedSpotId)
                putExtra("SELECTED_SPOT_NAME", selectedSpotName)
                putExtra("TRAVEL_DATE", travelDate) // Format: YYYY-MM-DD
                putExtra("PLANNED_ACTIVITIES", activityType)
                putExtra("REMINDER_OPTION", reminderOption)
            }
            startActivity(intent)
        }
    }

    private fun initViews() {
        btnCloseQuiz = findViewById(R.id.btnCloseQuiz)
        val cardCloseQuiz: MaterialCardView = findViewById(R.id.cardCloseQuiz)
        cardCloseQuiz.bringToFront()

        txtDestination = findViewById(R.id.txtDestination)
        edtTravelDate = findViewById(R.id.edtTravelDate)
        dropdownActivity = findViewById(R.id.dropdownActivity)
        dropdownReminder = findViewById(R.id.dropdownReminder)
        btnSaveTrip = findViewById(R.id.btnSaveTrip)
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
        val activityAdapter =
            ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, activities)
        dropdownActivity.setAdapter(activityAdapter)

        val reminders =
            arrayOf("1 day before", "2 days before", "On the day of trip", "1 week before")
        val reminderAdapter =
            ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, reminders)
        dropdownReminder.setAdapter(reminderAdapter)
    }

    private fun setupDatePicker() {
        edtTravelDate.setOnClickListener {
            val dateSetListener =
                DatePickerDialog.OnDateSetListener { _, year, monthOfYear, dayOfMonth ->
                    calendar.set(Calendar.YEAR, year)
                    calendar.set(Calendar.MONTH, monthOfYear)
                    calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)

                    // Standard reminder trigger time: 8:00 AM
                    calendar.set(Calendar.HOUR_OF_DAY, 8)
                    calendar.set(Calendar.MINUTE, 0)
                    calendar.set(Calendar.SECOND, 0)

                    val dateFormat = SimpleDateFormat(
                        "yyyy-MM-dd",
                        Locale.US
                    ) // Standard Date format para madaling mai-save sa Database
                    edtTravelDate.setText(dateFormat.format(calendar.time))
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

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun scheduleTripReminder(reminderOption: String, destination: String) {
        if (reminderOption.isEmpty()) return

        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val reminderCalendar = calendar.clone() as Calendar

        // I-compute ang eksaktong araw batay sa napagpilian sa dropdown
        when (reminderOption) {
            "1 day before" -> reminderCalendar.add(Calendar.DAY_OF_YEAR, -1)
            "2 days before" -> reminderCalendar.add(Calendar.DAY_OF_YEAR, -2)
            "1 week before" -> reminderCalendar.add(Calendar.DAY_OF_YEAR, -7)
            "On the day of trip" -> { /* Same date */
            }
        }

        val triggerTime = reminderCalendar.timeInMillis

        // Kung nakalipas na ang nakatakdang oras/araw, huwag nang i-schedule
        if (triggerTime <= System.currentTimeMillis()) return

        val intent = Intent(this, ReminderReceiver::class.java).apply {
            putExtra("PLAN_TITLE", destination)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            this,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            }
        } catch (e: SecurityException) {
            Toast.makeText(this, "Failed to set alarm: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}