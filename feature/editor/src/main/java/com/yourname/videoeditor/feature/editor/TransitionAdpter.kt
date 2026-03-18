package com.yourname.videoeditor.feature.editor

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.yourname.videoeditor.library.rendering.transition.TransitionType

class TransitionAdapter(
    private var transitions: List<TransitionType>,
    private val viewModel: TransitionViewModel,
    private val onItemClick: (TransitionType) -> Unit
) : RecyclerView.Adapter<TransitionAdapter.TransitionViewHolder>() {

    private var selectedPosition = -1

    class TransitionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val icon: ImageView = itemView.findViewById(R.id.iv_transition_icon)
        val name: TextView = itemView.findViewById(R.id.tv_transition_name)
        val selectedOverlay: View = itemView.findViewById(R.id.selected_overlay)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransitionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_transition, parent, false)
        return TransitionViewHolder(view)
    }

    override fun onBindViewHolder(holder: TransitionViewHolder, position: Int) {
        val transition = transitions[position]
        
        holder.name.text = viewModel.getTransitionName(transition)
        holder.icon.setImageResource(viewModel.getTransitionIcon(transition))
        
        // Show selection
        val isSelected = position == selectedPosition
        holder.selectedOverlay.visibility = if (isSelected) View.VISIBLE else View.GONE
        
        holder.itemView.setOnClickListener {
            val previousSelected = selectedPosition
            selectedPosition = position
            notifyItemChanged(previousSelected)
            notifyItemChanged(selectedPosition)
            onItemClick(transition)
        }
    }

    override fun getItemCount() = transitions.size

    fun updateTransitions(newTransitions: List<TransitionType>) {
        transitions = newTransitions
        notifyDataSetChanged()
    }
}
