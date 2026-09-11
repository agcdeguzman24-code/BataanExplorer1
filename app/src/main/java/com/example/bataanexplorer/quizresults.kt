package com.example.bataanexplorer

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton

class quizresults : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_quizresults)

        val btnCloseQuiz: MaterialButton = findViewById(R.id.btnCloseQuiz)

        btnCloseQuiz.setOnClickListener {
            val intent = Intent(this@quizresults, home::class.java)
            startActivity(intent)
            finish()
        }
    }
}