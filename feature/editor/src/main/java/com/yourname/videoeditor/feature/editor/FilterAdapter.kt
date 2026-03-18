package com.yourname.videoeditor.feature.editor

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.yourname.videoeditor.library.rendering.filter.FilterType

class FilterAdapter(
    private val filters: List<FilterType>,
    private val onFilterSelected: (FilterType) -> Unit
) : RecyclerView.Adapter<FilterAdapter.FilterViewHolder>() {

    class FilterViewHolder(itemView: android.view.View) : RecyclerView.ViewHolder(itemView) {
        val thumbnail: ImageView = itemView.findViewById(R.id.iv_filter_thumbnail)
        val name: TextView = itemView.findViewById(R.id.tv_filter_name)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FilterViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_filter, parent, false)
        return FilterViewHolder(view)
    }

    override fun onBindViewHolder(holder: FilterViewHolder, position: Int) {
        val filter = filters[position]
        holder.name.text = filter.name
        holder.itemView.setOnClickListener { onFilterSelected(filter) }
    }

    override fun getItemCount() = filters.size
}