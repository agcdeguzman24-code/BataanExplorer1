package com.example.bataanexplorer

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch

class setup1 : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_setup1)

        val btnNext = findViewById<MaterialButton>(R.id.btnNext)
        val btnBack = findViewById<MaterialButton>(R.id.btnBack)

        val buttons = listOf(
            findViewById<MaterialButton>(R.id.btnAbucay),
            findViewById<MaterialButton>(R.id.btnBagac),
            findViewById<MaterialButton>(R.id.btnBalanga),
            findViewById<MaterialButton>(R.id.btnDinalupihan),
            findViewById<MaterialButton>(R.id.btnHermosa),
            findViewById<MaterialButton>(R.id.btnLimay),
            findViewById<MaterialButton>(R.id.btnMariveles),
            findViewById<MaterialButton>(R.id.btnMorong),
            findViewById<MaterialButton>(R.id.btnOrani),
            findViewById<MaterialButton>(R.id.btnOrion),
            findViewById<MaterialButton>(R.id.btnPilar),
            findViewById<MaterialButton>(R.id.btnSamal)
        )

        buttons.forEach { button ->
            button.tag = false

            button.setOnClickListener {
                val selected = button.tag as Boolean

                if (!selected) {
                    button.tag = true
                    button.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#1565C0"))
                    button.setTextColor(Color.WHITE)
                } else {
                    button.tag = false
                    button.backgroundTintList = ColorStateList.valueOf(Color.WHITE)
                    button.setTextColor(Color.parseColor("#202124"))
                }
            }
        }

        btnBack.setOnClickListener {
            startActivity(Intent(this, setupintro::class.java))
            finish()
        }

        btnNext.setOnClickListener {
            val selectedMunicipalities = buttons
                .filter { it.tag as Boolean }
                .map { it.text.toString() }

            if (selectedMunicipalities.isEmpty()) {
                Toast.makeText(this, "Pumili muna ng kahit isang bayan", Toast.LENGTH_SHORT).show()
            } else {
                saveMunicipalitiesToSupabase(selectedMunicipalities)
            }
        }
    }

    private fun saveMunicipalitiesToSupabase(municipalities: List<String>) {
        lifecycleScope.launch {
            try {
                val client = SupabaseClientProvider.client
                val userId = client.auth.currentUserOrNull()?.id

                if (userId != null) {
                    val data = UserPreferenceDto(
                        userId = userId,
                        municipalities = municipalities,
                        isSkipped = false
                    )
                    client.postgrest["user_preferences"].upsert<UserPreferenceDto>(data)
                }

                startActivity(Intent(this@setup1, placessetup::class.java))

            } catch (e: Exception) {
                Toast.makeText(this@setup1, "Error saving: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}