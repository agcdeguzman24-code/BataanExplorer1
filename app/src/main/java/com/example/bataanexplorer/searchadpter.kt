package com.example.bataanexplorer

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class searchadapter(
    private var items: ArrayList<Destination>,
    private val onItemClick: (Destination) -> Unit
) : RecyclerView.Adapter<searchadapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imgPlace: ImageView = view.findViewById(R.id.imgCardPlace)
        val txtName: TextView = view.findViewById(R.id.txtCardName)
        val txtLocation: TextView = view.findViewById(R.id.txtCardLocation)
        val txtRating: TextView = view.findViewById(R.id.txtCardRating)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // Dito natin ginamit ang bagong pahabang card layout
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_search_result_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.txtName.text = item.name.ifEmpty { "N/A" }
        holder.txtLocation.text = item.location.ifEmpty { "Bataan" }
        holder.txtRating.text = "⭐ ${item.rating}"

        if (item.image_url.isNotEmpty()) {
            Glide.with(holder.itemView.context)
                .load(item.image_url)
                .placeholder(R.drawable.quiz_bg)
                .error(R.drawable.quiz_bg)
                .into(holder.imgPlace)
        } else {
            holder.imgPlace.setImageResource(R.drawable.quiz_bg)
        }

        holder.itemView.setOnClickListener {
            onItemClick(item)
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateList(newList: List<Destination>) {
        items.clear()
        items.addAll(newList)
        notifyDataSetChanged()
    }
}