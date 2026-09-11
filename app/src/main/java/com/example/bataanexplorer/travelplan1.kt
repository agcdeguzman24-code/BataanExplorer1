package com.example.bataanexplorer

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SpotMunicipalityCheck(
    @SerialName("municipality_id") val municipalityId: Long
)

class travelplan1 : AppCompatActivity() {

    private lateinit var radioMunicipalities: RadioGroup
    private lateinit var btnContinue: MaterialButton
    private lateinit var btnCloseQuiz: MaterialButton

    private val supabase: SupabaseClient get() = SupabaseClientProvider.client

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_travelplan1)

        radioMunicipalities = findViewById(R.id.radioMunicipalities)
        btnContinue = findViewById(R.id.btnContinue)
        btnCloseQuiz = findViewById(R.id.btnCloseQuiz)

        // I-bring to front ang card container para clickable at hindi matakpan
        val cardCloseQuiz: MaterialCardView = findViewById(R.id.cardCloseQuiz)
        cardCloseQuiz.bringToFront()

        // CLOSE BUTTON -> Direkta na sa Home Screen
        btnCloseQuiz.setOnClickListener {
            val intent = Intent(this@travelplan1, home::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(intent)
            finish()
        }

        btnContinue.setOnClickListener {
            val selectedId = radioMunicipalities.checkedRadioButtonId

            if (selectedId == -1) {
                Toast.makeText(this, "Please select a municipality first!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val selectedRadioButton: RadioButton = findViewById(selectedId)
            val selectedName = selectedRadioButton.text.toString().trim()

            // Direktang hahanapin sa tourist_spots table!
            fetchFromTouristSpots(selectedName)
        }
    }

    private fun fetchFromTouristSpots(municipalityName: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Direktang mag-query sa tourist_spots table
                val results = supabase.from("tourist_spots")
                    .select {
                        filter {
                            // Hahanapin kung sa location column nakasulat ang pangalan ng municipality (e.g., "Pilar", "Orion", "Balanga")
                            ilike("location", "%$municipalityName%")
                        }
                    }.decodeList<SpotMunicipalityCheck>()

                withContext(Dispatchers.Main) {
                    if (results.isNotEmpty()) {
                        // Kukunin ang municipality_id mula sa unang nahanap na record sa tourist_spots
                        val foundMunicipalityId = results[0].municipalityId

                        val intent = Intent(this@travelplan1, travelplan2::class.java).apply {
                            putExtra("SELECTED_MUNICIPALITY_NAME", municipalityName)
                            putExtra("SELECTED_MUNICIPALITY_ID", foundMunicipalityId)
                        }
                        startActivity(intent)
                    } else {
                        Toast.makeText(
                            this@travelplan1,
                            "No tourist spots found in $municipalityName yet!",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("travelplan1", "Error: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@travelplan1, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}