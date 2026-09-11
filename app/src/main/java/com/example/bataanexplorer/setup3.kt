package com.example.bataanexplorer

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch

class setup3 : AppCompatActivity() {

    private data class CompanionItem(
        val card: MaterialCardView,
        val circle: View,
        val name: String
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_setup3)

        val btnBack = findViewById<MaterialButton>(R.id.btnBack)
        val btnFinish = findViewById<MaterialButton>(R.id.btnFinish)

        btnBack.setOnClickListener {
            startActivity(Intent(this, placessetup::class.java))
            finish()
        }

        val items = listOf(
            CompanionItem(findViewById(R.id.cardSolo), findViewById(R.id.circleSolo), "solo"),
            CompanionItem(findViewById(R.id.cardCouple), findViewById(R.id.circleCouple), "couple"),
            CompanionItem(findViewById(R.id.cardFamily), findViewById(R.id.circleFamily), "family"),
            CompanionItem(findViewById(R.id.cardFriends), findViewById(R.id.circleFriends), "friends"),
            CompanionItem(findViewById(R.id.cardSchool), findViewById(R.id.circleSchool), "school"),
            CompanionItem(findViewById(R.id.cardWorkmates), findViewById(R.id.circleWork), "workmates")
        )

        items.forEach { item ->
            item.card.tag = false

            item.card.setOnClickListener {
                val selected = item.card.tag as Boolean

                if (!selected) {
                    item.card.tag = true
                    item.card.strokeColor = Color.parseColor("#1565C0")
                    item.card.strokeWidth = 6
                    item.circle.background =
                        ContextCompat.getDrawable(this, R.drawable.circle_selected)
                } else {
                    item.card.tag = false
                    item.card.strokeColor = Color.parseColor("#DADADA")
                    item.card.strokeWidth = 2
                    item.circle.background =
                        ContextCompat.getDrawable(this, R.drawable.circle_unselected)
                }
            }
        }

        btnFinish.setOnClickListener {
            val selectedCompanions = items
                .filter { it.card.tag as Boolean }
                .map { it.name }

            if (selectedCompanions.isEmpty()) {
                Toast.makeText(this, "Pumili muna ng kahit isang travel companion", Toast.LENGTH_SHORT).show()
            } else {
                saveCompanionsToSupabase(selectedCompanions)
            }
        }
    }

    private fun saveCompanionsToSupabase(companions: List<String>) {
        lifecycleScope.launch {
            try {
                val client = SupabaseClientProvider.client
                val userId = client.auth.currentUserOrNull()?.id

                if (userId != null) {
                    val data = UserPreferenceDto(
                        userId = userId,
                        companions = companions
                    )
                    client.postgrest["user_preferences"].upsert<UserPreferenceDto>(data)
                }

                Toast.makeText(this@setup3, "Setup completed!", Toast.LENGTH_SHORT).show()

                val intent = Intent(this@setup3, home::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()

            } catch (e: Exception) {
                Toast.makeText(this@setup3, "Error saving: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}