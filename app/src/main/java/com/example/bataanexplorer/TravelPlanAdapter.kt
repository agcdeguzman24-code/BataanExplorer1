package com.example.bataanexplorer

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

// Helper class para sa Supabase query
@Serializable
private data class SpotInfo(
    val image_url: String? = null
)

class TravelPlanAdapter(
    private var tripList: List<UserTravelPlan>,
    private val onItemClick: (UserTravelPlan) -> Unit
) : RecyclerView.Adapter<TravelPlanAdapter.TripViewHolder>() {

    class TripViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgCardPlace: ImageView = itemView.findViewById(R.id.imgCardPlace)
        val txtCardName: TextView = itemView.findViewById(R.id.txtCardName)
        val txtCardLocation: TextView = itemView.findViewById(R.id.txtCardLocation)
        val txtCardRating: TextView = itemView.findViewById(R.id.txtCardRating)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TripViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_destination_card, parent, false)
        return TripViewHolder(view)
    }

    override fun onBindViewHolder(holder: TripViewHolder, position: Int) {
        val trip = tripList[position]

        holder.txtCardName.text = if (trip.spotName.isNotBlank()) trip.spotName else trip.activities
        holder.txtCardLocation.text =
            if (trip.spotLocation.isNotBlank()) "${trip.spotLocation} • ${trip.travelDate}" else trip.travelDate
        holder.txtCardRating.text = if (trip.isCompleted) "Completed" else "Pending"

        // 1. KUNG MAY DIRECT IMAGE URL NA:
        if (!trip.imageUrl.isNullOrEmpty()) {
            Glide.with(holder.itemView.context)
                .load(trip.imageUrl)
                .placeholder(R.drawable.quiz_bg)
                .error(R.drawable.quiz_bg)
                .centerCrop()
                .into(holder.imgCardPlace)
        }
        // 2. KUNG WALA PA AT MAY SPOT ID, KUKUNIN DIRECT SA SUPABASE:
        else if (trip.spotId != null && trip.spotId > 0) {
            holder.imgCardPlace.setImageResource(R.drawable.quiz_bg)

            (holder.itemView.context as? AppCompatActivity)?.lifecycleScope?.launch(Dispatchers.IO) {
                try {
                    val spot = SupabaseClientProvider.client
                        .from("tourist_spots")
                        .select(columns = io.github.jan.supabase.postgrest.query.Columns.list("image_url")) {
                            filter { eq("id", trip.spotId) }
                        }.decodeSingleOrNull<SpotInfo>()

                    val fetchedUrl = spot?.image_url
                    trip.imageUrl = fetchedUrl

                    withContext(Dispatchers.Main) {
                        if (!fetchedUrl.isNullOrEmpty()) {
                            Glide.with(holder.itemView.context)
                                .load(fetchedUrl)
                                .placeholder(R.drawable.quiz_bg)
                                .error(R.drawable.quiz_bg)
                                .centerCrop()
                                .into(holder.imgCardPlace)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("Adapter", "Failed to fetch image: ${e.message}")
                }
            }
        }
        // 3. FALLBACK SA LOCAL RES ID O DEFAULT
        else if (trip.imageResId != 0) {
            holder.imgCardPlace.setImageResource(trip.imageResId)
        } else {
            holder.imgCardPlace.setImageResource(R.drawable.quiz_bg)
        }

        holder.itemView.setOnClickListener {
            onItemClick(trip)
        }
    }

    override fun getItemCount(): Int = tripList.size

    fun updateData(newList: List<UserTravelPlan>) {
        tripList = newList
        notifyDataSetChanged()
    }
}