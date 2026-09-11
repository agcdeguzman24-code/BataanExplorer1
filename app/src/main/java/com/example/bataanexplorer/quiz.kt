package com.example.bataanexplorer

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton

class quiz : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_quiz)

        val btnCloseQuiz: MaterialButton = findViewById(R.id.btnCloseQuiz)
        val btnStartQuiz: MaterialButton = findViewById(R.id.btnContinue)

        // X / Close → Home
        btnCloseQuiz.setOnClickListener {
            val intent = Intent(this@quiz, home::class.java)
            startActivity(intent)
            finish()
        }

        // Start Quiz → Quiz page
        btnStartQuiz.setOnClickListener {
            val intent = Intent(this@quiz, processingquiz::class.java)
            startActivity(intent)
            finish()
        }
    }
}