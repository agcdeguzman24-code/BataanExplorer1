package com.example.bataanexplorer

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

class quizintro : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_quizintro)

        val btnCloseQuiz: MaterialButton = findViewById(R.id.btnCloseQuiz)
        val btnStartQuiz: MaterialButton = findViewById(R.id.btnStartQuiz)

        // X / Close → Home
        btnCloseQuiz.setOnClickListener {
            val intent = Intent(this@quizintro, home::class.java)
            startActivity(intent)
            finish()
        }

        // Start Quiz → Quiz page
        btnStartQuiz.setOnClickListener {
            val intent = Intent(this@quizintro, quiz::class.java)
            startActivity(intent)
            finish()
        }
    }
}