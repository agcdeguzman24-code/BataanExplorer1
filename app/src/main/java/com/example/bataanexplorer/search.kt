package com.example.bataanexplorer

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class search : AppCompatActivity() {

    private lateinit var btnCloseQuiz: MaterialButton
    private lateinit var etSearch: EditText
    private lateinit var chipGroupCategories: ChipGroup
    private lateinit var recyclerResults: RecyclerView

    // Pinalitan ng searchadapter para sa pahabang search result cards
    private lateinit var adapter: searchadapter
    private var masterDestinationList = ArrayList<Destination>()

    private var currentSelectedCategory = "All"
    private var currentSearchQuery = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_search)

        initViews()

        // Ginamit ang searchadapter
        adapter = searchadapter(ArrayList()) { selectedDestination ->
            val intent = Intent(this, destinationdetails::class.java)
            intent.putExtra("EXTRA_DESTINATION", selectedDestination)
            startActivity(intent)
        }
        recyclerResults.layoutManager = LinearLayoutManager(this)
        recyclerResults.adapter = adapter

        setupListeners()

        // Kumuha ng data mula sa Supabase Table ("tourist_spots")
        fetchDestinationsFromSupabase()
    }

    private fun initViews() {
        btnCloseQuiz = findViewById(R.id.btnCloseQuiz)
        etSearch = findViewById(R.id.etSearch)
        chipGroupCategories = findViewById(R.id.chipGroupCategories)
        recyclerResults = findViewById(R.id.recyclerResults)
    }

    private fun setupListeners() {
        btnCloseQuiz.setOnClickListener { finish() }

        chipGroupCategories.setOnCheckedStateChangeListener { group, checkedIds ->
            if (checkedIds.isEmpty()) {
                currentSelectedCategory = "All"
            } else {
                val selectedChip = group.findViewById<Chip>(checkedIds[0])
                currentSelectedCategory = selectedChip?.text?.toString() ?: "All"
            }
            applyFilters()
        }

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                currentSearchQuery = s?.toString()?.trim() ?: ""
                applyFilters()
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun fetchDestinationsFromSupabase() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val results = SupabaseClientProvider.client.from("tourist_spots")
                    .select()
                    .decodeList<Destination>()

                withContext(Dispatchers.Main) {
                    masterDestinationList.clear()
                    masterDestinationList.addAll(results)
                    applyFilters()

                    if (results.isEmpty()) {
                        Toast.makeText(this@search, "Walang nakuhang data mula sa Supabase!", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@search, "Supabase Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun applyFilters() {
        val filteredList = ArrayList<Destination>()

        for (item in masterDestinationList) {
            val matchesCategory = if (currentSelectedCategory.equals("All", ignoreCase = true)) {
                true
            } else {
                item.category.contains(currentSelectedCategory, ignoreCase = true) ||
                        item.categories.any { it.contains(currentSelectedCategory, ignoreCase = true) }
            }

            val matchesSearch = if (currentSearchQuery.isEmpty()) {
                true
            } else {
                item.name.contains(currentSearchQuery, ignoreCase = true) ||
                        item.location.contains(currentSearchQuery, ignoreCase = true)
            }

            if (matchesCategory && matchesSearch) {
                filteredList.add(item)
            }
        }

        // Maximum 5 items lang ang ipapakita sa listahan
        val limitedList = filteredList.take(5)
        adapter.updateList(limitedList)
    }
}