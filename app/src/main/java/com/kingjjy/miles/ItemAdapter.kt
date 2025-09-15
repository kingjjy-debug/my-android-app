package com.kingjjy.miles

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Space
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.materialswitch.MaterialSwitch

class ItemAdapter(
    private val items: List<MileageItem>,
    private val onUrlClick: (String) -> Unit,
    private val onCompletionChanged: (MileageItem, Boolean) -> Unit
) : RecyclerView.Adapter<ItemAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.item_title)
        val earnButton: Button = view.findViewById(R.id.item_earn_button)
        val convertButton: Button = view.findViewById(R.id.item_convert_button)
        val completionSwitch: MaterialSwitch = view.findViewById(R.id.item_switch)
        val completedText: TextView = view.findViewById(R.id.item_completed_text)
        val buttonSpacer: Space = view.findViewById(R.id.button_spacer)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]

        holder.title.text = item.name

        if (item.earnUrl != null) {
            holder.earnButton.visibility = View.VISIBLE
            holder.buttonSpacer.visibility = View.VISIBLE
            holder.earnButton.setOnClickListener { onUrlClick(item.earnUrl) }
        } else {
            holder.earnButton.visibility = View.GONE
            holder.buttonSpacer.visibility = View.GONE
        }

        holder.convertButton.setOnClickListener { onUrlClick(item.convertUrl) }
        
        holder.completionSwitch.setOnCheckedChangeListener(null)
        holder.completionSwitch.isChecked = item.isCompleted
        updateCompletedView(holder, item.isCompleted)

        holder.completionSwitch.setOnCheckedChangeListener { _, isChecked ->
            item.isCompleted = isChecked 
            onCompletionChanged(item, isChecked)
            updateCompletedView(holder, isChecked)
        }
    }

    override fun getItemCount() = items.size

    private fun updateCompletedView(holder: ViewHolder, isCompleted: Boolean) {
        holder.completedText.visibility = if (isCompleted) View.VISIBLE else View.GONE
    }
}