package com.example.bataanexplorer

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch

class placessetup : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_placessetup)

        val btnNext = findViewById<MaterialButton>(R.id.btnNext)
        val btnBack = findViewById<MaterialButton>(R.id.btnBack)

        val cardsMap = mapOf(
            findViewById<MaterialCardView>(R.id.cardBeach) to "beach",
            findViewById<MaterialCardView>(R.id.cardNature) to "nature",
            findViewById<MaterialCardView>(R.id.cardHistoryCulture) to "history and culture",
            findViewById<MaterialCardView>(R.id.cardFood) to "food",
            findViewById<MaterialCardView>(R.id.cardScenicAdventure) to "scenic and adventure"
        )

        cardsMap.keys.forEach { card ->
            card.tag = false

            card.setOnClickListener {
                val isSelected = card.tag as Boolean

                if (!isSelected) {
                    card.tag = true
                    card.setCardBackgroundColor(Color.parseColor("#E3F2FD"))
                    card.strokeColor = Color.parseColor("#1565C0")
                    card.strokeWidth = 4
                } else {
                    card.tag = false
                    card.setCardBackgroundColor(Color.parseColor("#FFFFFF"))
                    card.strokeColor = Color.parseColor("#DADADA")
                    card.strokeWidth = 2
                }
            }
        }

        btnBack.setOnClickListener {
            startActivity(Intent(this, setup1::class.java))
            finish()
        }

        btnNext.setOnClickListener {
            val selectedCategories = cardsMap
                .filter { (card, _) -> card.tag as Boolean }
                .map { (_, categoryName) -> categoryName }

            if (selectedCategories.isEmpty()) {
                Toast.makeText(this, "Pumili muna ng kahit isang category", Toast.LENGTH_SHORT).show()
            } else {
                saveCategoriesToSupabase(selectedCategories)
            }
        }
    }

    private fun saveCategoriesToSupabase(placeTypes: List<String>) {
        lifecycleScope.launch {
            try {
                val client = SupabaseClientProvider.client
                val userId = client.auth.currentUserOrNull()?.id

                if (userId != null) {
                    val data = UserPreferenceDto(
                        userId = userId,
                        placeTypes = placeTypes
                    )
                    // DITO MO LAGYAN NG <UserPreferenceDto>:
                    client.postgrest["user_preferences"].upsert<UserPreferenceDto>(data)
                }

                startActivity(Intent(this@placessetup, setup3::class.java))

            } catch (e: Exception) {
                Toast.makeText(this@placessetup, "Error saving: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}