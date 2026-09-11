package com.example.bataanexplorer

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch

class setupintro : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_setupintro)

        val btnContinue = findViewById<MaterialButton>(R.id.btnContinue)
        val btnSkip = findViewById<MaterialButton>(R.id.btnSkip)

        btnContinue.setOnClickListener {
            startActivity(Intent(this, setup1::class.java))
        }

        btnSkip.setOnClickListener {
            saveDefaultPreferencesAndProceed()
        }
    }

    private fun saveDefaultPreferencesAndProceed() {
        lifecycleScope.launch {
            try {
                val client = SupabaseClientProvider.client
                val userId = client.auth.currentUserOrNull()?.id

                if (userId != null) {
                    val data = UserPreferenceDto(
                        userId = userId,
                        municipalities = emptyList(),
                        placeTypes = listOf("beach", "nature", "history and culture", "food", "scenic and adventure"),
                        companions = emptyList(),
                        isSkipped = true
                    )
                    client.postgrest["user_preferences"].upsert<UserPreferenceDto>(data)
                }
                navigateToHome()

            } catch (e: Exception) {
                Toast.makeText(this@setupintro, "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                navigateToHome()
            }
        }
    }

    private fun navigateToHome() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}