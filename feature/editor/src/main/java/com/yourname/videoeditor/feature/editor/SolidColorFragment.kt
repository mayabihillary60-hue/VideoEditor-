package com.yourname.videoeditor.feature.editor

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.yourname.videoeditor.feature.editor.databinding.FragmentSolidColorBinding

class SolidColorFragment : Fragment() {

    private var _binding: FragmentSolidColorBinding? = null
    private val binding get() = _binding!!

    private val viewModel: BackgroundViewModel by viewModels(ownerProducer = { requireParentFragment() })

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSolidColorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupColorGrid()
        setupOpacitySlider()
    }

    private fun setupColorGrid() {
        val adapter = SolidColorAdapter(viewModel.availableColors) { color ->
            viewModel.setBackgroundType(BackgroundType.SOLID_COLOR)
            viewModel.setSolidColor(color)
        }
        binding.recyclerColors.layoutManager = GridLayoutManager(requireContext(), 4)
        binding.recyclerColors.adapter = adapter
    }

    private fun setupOpacitySlider() {
        binding.sliderOpacity.addOnChangeListener { slider, value, fromUser ->
            if (fromUser) {
                viewModel.setOpacity(value)
            }
        }
    }
}

// Solid Color Adapter
class SolidColorAdapter(
    private val colors: List<Int>,
    private val onColorSelected: (Int) -> Unit
) : RecyclerView.Adapter<SolidColorAdapter.ColorViewHolder>() {

    class ColorViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val colorView: ImageView = itemView.findViewById(R.id.image_color)
        val checkmark: ImageView = itemView.findViewById(R.id.image_check)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ColorViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_solid_color, parent, false)
        return ColorViewHolder(view)
    }

    override fun onBindViewHolder(holder: ColorViewHolder, position: Int) {
        val color = colors[position]
        holder.colorView.setBackgroundColor(color)
        holder.itemView.setOnClickListener {
            onColorSelected(color)
            // Update checkmark visibility
            notifyDataSetChanged()
        }
    }

    override fun getItemCount() = colors.size
}
