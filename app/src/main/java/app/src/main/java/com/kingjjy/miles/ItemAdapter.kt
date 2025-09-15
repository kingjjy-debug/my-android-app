package com.kingjjy.miles

import android.content.SharedPreferences
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.materialswitch.MaterialSwitch

class ItemAdapter(
    private val items: List<PointItem>,
    private val sharedPreferences: SharedPreferences,
    private val onUrlClick: (String) -> Unit,
    private val onCompletionChanged: (String, Boolean) -> Unit
) : RecyclerView.Adapter<ItemAdapter.ItemViewHolder>() {

    inner class ItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.item_title)
        val earnButton: Button = view.findViewById(R.id.item_earn_button)
        val convertButton: Button = view.findViewById(R.id.item_convert_button)
        val itemSwitch: MaterialSwitch = view.findViewById(R.id.item_switch)
        val completedText: TextView = view.findViewById(R.id.item_completed_text)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_card, parent, false)
        return ItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val item = items[position]

        holder.title.text = item.name

        if (item.earnUrl != null) {
            holder.earnButton.visibility = View.VISIBLE
            holder.earnButton.setOnClickListener { onUrlClick(item.earnUrl) }
        } else {
            holder.earnButton.visibility = View.GONE
        }
        holder.convertButton.setOnClickListener { onUrlClick(item.convertUrl) }

        val isCompleted = sharedPreferences.getBoolean(item.key, false)
        updateCompletedView(holder, isCompleted)

        holder.itemSwitch.setOnCheckedChangeListener(null)
        holder.itemSwitch.isChecked = isCompleted

        holder.itemSwitch.setOnCheckedChangeListener { _, isChecked ->
            onCompletionChanged(item.key, isChecked)
            updateCompletedView(holder, isChecked)
        }
    }

    private fun updateCompletedView(holder: ItemViewHolder, isCompleted: Boolean) {
        if (isCompleted) {
            holder.completedText.visibility = View.VISIBLE
        } else {
            holder.completedText.visibility = View.GONE
        }
    }

    override fun getItemCount() = items.size
}
