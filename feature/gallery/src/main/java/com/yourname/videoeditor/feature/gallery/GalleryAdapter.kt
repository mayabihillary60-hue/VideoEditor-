package com.yourname.videoeditor.feature.gallery

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.yourname.videoeditor.feature.gallery.model.VideoItem

class GalleryAdapter(
    private var videos: List<VideoItem>,
    private val onItemClick: (VideoItem) -> Unit,
    private val onItemLongClick: (VideoItem) -> Boolean,
    private val isSelected: (VideoItem) -> Boolean
) : RecyclerView.Adapter<GalleryAdapter.VideoViewHolder>() {

    class VideoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val thumbnail: ImageView = itemView.findViewById(R.id.iv_thumbnail)
        val title: TextView = itemView.findViewById(R.id.tv_title)
        val duration: TextView = itemView.findViewById(R.id.tv_duration)
        val resolution: TextView = itemView.findViewById(R.id.tv_resolution)
        val selectionOverlay: View = itemView.findViewById(R.id.selection_overlay)
        val checkmark: ImageView = itemView.findViewById(R.id.iv_checkmark)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_gallery_video, parent, false)
        return VideoViewHolder(view)
    }

    override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
        val video = videos[position]
        
        holder.title.text = video.title
        holder.duration.text = formatDuration(video.duration)
        holder.resolution.text = video.resolution
        
        // Load thumbnail (you'll need to implement this)
        // You can use Glide or Coil to load thumbnails
        
        // Handle selection state
        val selected = isSelected(video)
        holder.selectionOverlay.visibility = if (selected) View.VISIBLE else View.GONE
        holder.checkmark.visibility = if (selected) View.VISIBLE else View.GONE
        
        holder.itemView.setOnClickListener {
            onItemClick(video)
        }
        
        holder.itemView.setOnLongClickListener {
            onItemLongClick(video)
        }
    }

    override fun getItemCount() = videos.size

    fun updateVideos(newVideos: List<VideoItem>) {
        videos = newVideos
        notifyDataSetChanged()
    }

    private fun formatDuration(durationMs: Long): String {
        val seconds = (durationMs / 1000) % 60
        val minutes = (durationMs / (1000 * 60)) % 60
        val hours = (durationMs / (1000 * 60 * 60))
        
        return if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }
}