package com.kingjjy.miles

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class PointsAdapter(private val items: List<String>) :
    RecyclerView.Adapter<PointsAdapter.VH>() {

    class VH(parent: ViewGroup) : RecyclerView.ViewHolder(
        LayoutInflater.from(parent.context).inflate(R.layout.item_point, parent, false)
    ) {
        val title: TextView = itemView.findViewById(R.id.point_text)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH = VH(parent)
    override fun getItemCount(): Int = items.size
    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.title.text = items[position]
    }
}
