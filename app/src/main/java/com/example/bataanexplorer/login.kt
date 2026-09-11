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
import kotlinx.coroutines.launch

class login : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)

        val etEmail: TextInputEditText = findViewById(R.id.etEmail)
        val etPassword: TextInputEditText = findViewById(R.id.etPassword)
        val btnLogin: MaterialButton = findViewById(R.id.btnLogin)

        btnLogin.setOnClickListener {

            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString()

            if (email.isEmpty()) {
                etEmail.error = "Please enter your email"
                return@setOnClickListener
            }

            if (password.isEmpty()) {
                etPassword.error = "Please enter your password"
                return@setOnClickListener
            }

            // Disable button para maiwasan ang multiple clicks
            btnLogin.isEnabled = false

            // Supabase Login Logic (Coroutines)
            lifecycleScope.launch {
                try {
                    // Ginagamit na rito ang tamang singleton: SupabaseClientProvider.client
                    SupabaseClientProvider.client.auth.signInWith(Email) {
                        this.email = email
                        this.password = password
                    }

                    Toast.makeText(
                        this@login,
                        "Login successful!",
                        Toast.LENGTH_SHORT
                    ).show()

                    // Direct to Home Activity
                    val intent = Intent(this@login, home::class.java)
                    startActivity(intent)
                    finish()

                } catch (e: Exception) {
                    Toast.makeText(
                        this@login,
                        "Login failed: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                } finally {
                    btnLogin.isEnabled = true
                }
            }
        }

        // Sign Up link navigation
        val tvSignup = findViewById<TextView>(R.id.tvSignup)

        val text = "Don't have an account? Sign Up"
        val spannable = SpannableString(text)

        val clickable = object : ClickableSpan() {

            override fun onClick(widget: View) {
                startActivity(Intent(this@login, signup::class.java))
            }

            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                ds.color = Color.parseColor("#1565C0")
                ds.isUnderlineText = false
            }
        }

        spannable.setSpan(
            clickable,
            text.indexOf("Sign Up"),
            text.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        tvSignup.text = spannable
        tvSignup.movementMethod = LinkMovementMethod.getInstance()
        tvSignup.highlightColor = Color.TRANSPARENT
    }
}