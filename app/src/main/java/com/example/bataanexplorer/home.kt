package com.example.bataanexplorer

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class home : AppCompatActivity() {

    private lateinit var recyclerPopular: RecyclerView
    private lateinit var recyclerRecommended: RecyclerView

    private val supabase: SupabaseClient get() = SupabaseClientProvider.client

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_home)

        // Popular Destinations RecyclerView Setup
        recyclerPopular = findViewById(R.id.recyclerPopular)
        recyclerPopular.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        // Recommended For You RecyclerView Setup
        recyclerRecommended = findViewById(R.id.recyclerRecommended)
        recyclerRecommended.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        // Load pareho mula sa Supabase
        loadPopularDestinations()
        loadRecommendedDestinations()

        // NAVIGATION BUTTONS
        val btnNearby: MaterialButton = findViewById(R.id.btnNearby)
        btnNearby.setOnClickListener {
            startActivity(Intent(this@home, explorenearby::class.java))
        }

        val btnQuiz: MaterialButton = findViewById(R.id.btnQuiz)
        btnQuiz.setOnClickListener {
            startActivity(Intent(this@home, quizintro::class.java))
        }

        val btnSearch: MaterialButton = findViewById(R.id.btnSearch)
        btnSearch.setOnClickListener {
            startActivity(Intent(this@home, search::class.java))
        }

        val navProfile = findViewById<android.widget.LinearLayout>(R.id.navProfile)
        navProfile.setOnClickListener {
            startActivity(Intent(this@home, profile::class.java))
        }

        val navPlanner = findViewById<android.widget.LinearLayout>(R.id.navPlanner)
        navPlanner.setOnClickListener {
            startActivity(Intent(this@home, travelplan1::class.java))
        }
    }

    private fun loadPopularDestinations() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Kukuha ng tourist spots at ililimit sa top 5 popular/top-rated items
                val destinations = supabase.postgrest["tourist_spots"]
                    .select()
                    .decodeList<Destination>()
                    .filter { it.isPopular || it.rating >= 4.0 }
                    .take(5) // Top 5 popular spots lang

                withContext(Dispatchers.Main) {
                    if (destinations.isNotEmpty()) {
                        setPopularAdapter(destinations)
                    }
                }
            } catch (e: Exception) {
                Log.e("BataanExplorer", "POPULAR FETCH ERROR: ${e.message}", e)
            }
        }
    }

    private fun loadRecommendedDestinations() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val currentUser = supabase.auth.currentUserOrNull()
                var userPref: UserPreference? = null

                // 1. Kunin ang user preference sa database kung nakalogin
                if (currentUser != null) {
                    userPref = supabase.from("user_preferences")
                        .select {
                            filter { eq("user_id", currentUser.id) }
                        }
                        .decodeSingleOrNull<UserPreference>()
                }

                // 2. Kunin ang lahat ng tourist spots
                val allSpots = supabase.from("tourist_spots")
                    .select()
                    .decodeList<Destination>()

                // 3. Filter/Matching Algorithm
                val recommendedList = if (userPref == null || userPref.isSkipped) {
                    // DEFAULT KAPAG INISKIP (is_popular == true OR high rating)
                    allSpots.filter { it.isPopular || it.rating >= 4.5 }
                } else {
                    // MATCHING SYSTEM KAPAG MAY SAGOT SA QUIZ
                    allSpots.map { spot ->
                        var score = 0

                        // Match Municipality
                        if (userPref.municipalities.any { m -> spot.location.contains(m, ignoreCase = true) }) {
                            score += 3
                        }

                        // Match Categories
                        if (userPref.placeTypes.any { type -> spot.categories.any { cat -> cat.contains(type, ignoreCase = true) } }) {
                            score += 2
                        }

                        // Match Companions
                        if (userPref.companions.any { comp -> spot.suitableFor.any { s -> s.contains(comp, ignoreCase = true) } }) {
                            score += 1
                        }

                        Pair(spot, score)
                    }
                        .filter { it.second > 0 }
                        .sortedByDescending { it.second }
                        .map { it.first }
                }.take(5) // Kukuha lang ng Top 5 items!

                withContext(Dispatchers.Main) {
                    setRecommendedAdapter(recommendedList)
                }

            } catch (e: Exception) {
                Log.e("BataanExplorer", "RECOMMENDED FETCH ERROR: ${e.message}", e)
            }
        }
    }

    private fun setPopularAdapter(list: List<Destination>) {
        val adapter = destinationadapter(ArrayList(list)) { selectedDestination ->
            val intent = Intent(this@home, destinationdetails::class.java)
            intent.putExtra("EXTRA_DESTINATION", selectedDestination)
            startActivity(intent)
        }
        recyclerPopular.adapter = adapter
    }

    private fun setRecommendedAdapter(list: List<Destination>) {
        val adapter = destinationadapter(ArrayList(list)) { selectedDestination ->
            val intent = Intent(this@home, destinationdetails::class.java)
            intent.putExtra("EXTRA_DESTINATION", selectedDestination)
            startActivity(intent)
        }
        recyclerRecommended.adapter = adapter // siguraduhing tama ang RecyclerView ID mo rito
    }
}