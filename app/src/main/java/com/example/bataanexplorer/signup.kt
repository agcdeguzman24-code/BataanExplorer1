package com.example.bataanexplorer

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Serializable
data class UserProfile(
    val id: String,
    val email: String,
    val full_name: String
)

class signup : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_signup)

        val etName: TextInputEditText = findViewById(R.id.etName)
        val etEmail: TextInputEditText = findViewById(R.id.etEmail)
        val etPassword: TextInputEditText = findViewById(R.id.etPassword)
        val etConfirmPassword: TextInputEditText = findViewById(R.id.etConfirmPassword)
        val btnCreate: MaterialButton = findViewById(R.id.btnCreate)

        btnCreate.setOnClickListener {

            val name = etName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString()
            val confirmPassword = etConfirmPassword.text.toString()

            if (name.isEmpty()) {
                etName.error = "Please enter your name"
                return@setOnClickListener
            }

            if (email.isEmpty()) {
                etEmail.error = "Please enter your email"
                return@setOnClickListener
            }

            if (password.isEmpty()) {
                etPassword.error = "Please enter a password"
                return@setOnClickListener
            }

            if (password.length < 6) {
                etPassword.error = "Password must be at least 6 characters"
                return@setOnClickListener
            }

            if (confirmPassword.isEmpty()) {
                etConfirmPassword.error = "Please confirm your password"
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                etConfirmPassword.error = "Passwords do not match"
                return@setOnClickListener
            }

            btnCreate.isEnabled = false

            lifecycleScope.launch {
                try {
                    // 1. Sign up user
                    SupabaseClient.client.auth.signUpWith(Email) {
                        this.email = email
                        this.password = password
                        this.data = buildJsonObject {
                            put("full_name", name)
                        }
                    }

                    // 2. Get registered User ID
                    val user = SupabaseClient.client.auth.currentUserOrNull()

                    if (user != null) {
                        // 3. Save to public.users table
                        val profile = UserProfile(
                            id = user.id,
                            email = email,
                            full_name = name
                        )
                        SupabaseClient.client.postgrest["users"].insert(profile)
                    }

                    Toast.makeText(
                        this@signup,
                        "Account created successfully!",
                        Toast.LENGTH_SHORT
                    ).show()

                    // Direct to intro page
                    val intent = Intent(this@signup, setupintro::class.java)
                    startActivity(intent)
                    finish()

                } catch (e: Exception) {
                    Toast.makeText(
                        this@signup,
                        "Registration failed: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                } finally {
                    btnCreate.isEnabled = true
                }
            }
        }

        // Login link setup
        val tvLogin = findViewById<TextView>(R.id.txtLogin)
        val text = "Already have an account? Log In"
        val spannable = SpannableString(text)

        val clickable = object : ClickableSpan() {
            override fun onClick(widget: View) {
                startActivity(Intent(this@signup, login::class.java))
            }

            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                ds.color = Color.parseColor("#1565C0")
                ds.isUnderlineText = false
            }
        }

        spannable.setSpan(
            clickable,
            text.indexOf("Log In"),
            text.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        tvLogin.text = spannable
        tvLogin.movementMethod = LinkMovementMethod.getInstance()
        tvLogin.highlightColor = Color.TRANSPARENT
    }
}