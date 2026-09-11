package com.example.bataanexplorer

import android.content.Intent
import android.graphics.Color
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class travelplan2 : AppCompatActivity() {

    private lateinit var txtMunicipality: TextView
    private lateinit var recyclerDestinations: RecyclerView
    private lateinit var btnSelectDestination: MaterialButton
    private lateinit var btnCloseQuiz: MaterialButton

    private lateinit var adapter: destinationadapter
    private var destinationList = ArrayList<Destination>()

    private var selectedDestination: Destination? = null
    private var selectedMunicipalityId: Long = -1L
    private var selectedMunicipalityName: String = ""

    // Reference sa active card view para sa blue visual highlight
    private var selectedCardView: MaterialCardView? = null

    private val supabase: SupabaseClient get() = SupabaseClientProvider.client

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_travelplan2)

        selectedMunicipalityName = intent.getStringExtra("SELECTED_MUNICIPALITY_NAME") ?: ""
        selectedMunicipalityId = intent.getLongExtra("SELECTED_MUNICIPALITY_ID", -1L)

        initViews()
        setupRecyclerView()

        if (selectedMunicipalityName.isNotEmpty()) {
            txtMunicipality.text = "Explore places you can visit in $selectedMunicipalityName."
        }

        fetchTouristSpots()

        // CLOSE BUTTON -> Direkta na sa Home Screen
        btnCloseQuiz.setOnClickListener {
            val intent = Intent(this@travelplan2, home::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(intent)
            finish()
        }

        btnSelectDestination.setOnClickListener {
            if (selectedDestination == null) {
                Toast.makeText(this, "Please select a destination first!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val intent = Intent(this@travelplan2, travelplan3::class.java)
            intent.putExtra("SELECTED_MUNICIPALITY_NAME", selectedMunicipalityName)
            intent.putExtra("SELECTED_MUNICIPALITY_ID", selectedMunicipalityId)
            intent.putExtra("SELECTED_SPOT_ID", selectedDestination?.id ?: -1L)
            intent.putExtra("SELECTED_SPOT_NAME", selectedDestination?.name ?: "")
            startActivity(intent)
        }
    }

    private fun initViews() {
        txtMunicipality = findViewById(R.id.txtMunicipality)
        recyclerDestinations = findViewById(R.id.recyclerDestinations)
        btnSelectDestination = findViewById(R.id.btnSelectDestination)
        btnCloseQuiz = findViewById(R.id.btnCloseQuiz)

        // I-bring to front ang card container para clickable at hindi matakpan
        val cardCloseQuiz: MaterialCardView = findViewById(R.id.cardCloseQuiz)
        cardCloseQuiz.bringToFront()
    }

    private fun setupRecyclerView() {
        adapter = destinationadapter(destinationList) { clickedDestination ->
            selectedDestination = clickedDestination
            Toast.makeText(this, "Selected: ${clickedDestination.name}", Toast.LENGTH_SHORT).show()

            // Hanapin ang ViewHolder/CardView ng napiling item gamit ang index
            val position = destinationList.indexOf(clickedDestination)
            if (position != -1) {
                val viewHolder = recyclerDestinations.findViewHolderForAdapterPosition(position)
                val cardView = viewHolder?.itemView as? MaterialCardView

                if (cardView != null) {
                    // I-reset ang dating napiling card
                    selectedCardView?.apply {
                        strokeColor = Color.parseColor("#E5E7EB")
                        strokeWidth = 2
                        setCardBackgroundColor(Color.WHITE)
                    }

                    // Lagyan ng Blue Highlight ang bagong card
                    cardView.strokeColor = Color.parseColor("#1565C0")
                    cardView.strokeWidth = 6
                    cardView.setCardBackgroundColor(Color.parseColor("#F0F6FF"))

                    selectedCardView = cardView
                }
            }
        }

        recyclerDestinations.layoutManager = LinearLayoutManager(this)
        recyclerDestinations.adapter = adapter

        // Spacing sa pagitan ng cards
        val spaceInDp = 16
        val scale = resources.displayMetrics.density
        val spaceInPx = (spaceInDp * scale + 0.5f).toInt()

        recyclerDestinations.addItemDecoration(object : RecyclerView.ItemDecoration() {
            override fun getItemOffsets(
                outRect: Rect,
                view: View,
                parent: RecyclerView,
                state: RecyclerView.State
            ) {
                outRect.bottom = spaceInPx
            }
        })
    }

    private fun fetchTouristSpots() {
        if (selectedMunicipalityId == -1L) {
            Toast.makeText(this, "Invalid municipality ID!", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val fetchedSpots = supabase.from("tourist_spots")
                    .select {
                        filter {
                            eq("municipality_id", selectedMunicipalityId)
                        }
                    }.decodeList<Destination>()

                withContext(Dispatchers.Main) {
                    if (fetchedSpots.isEmpty()) {
                        Toast.makeText(this@travelplan2, "No spots found for $selectedMunicipalityName!", Toast.LENGTH_SHORT).show()
                    } else {
                        adapter.updateList(fetchedSpots)
                    }
                }
            } catch (e: Exception) {
                Log.e("travelplan2", "Error fetching spots: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@travelplan2, "Error loading destinations: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}