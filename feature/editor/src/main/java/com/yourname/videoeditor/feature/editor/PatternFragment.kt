package com.yourname.videoeditor.feature.editor

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.slider.Slider
import com.yourname.videoeditor.feature.editor.databinding.FragmentPatternBinding
import com.yourname.videoeditor.library.rendering.background.PatternType

class PatternFragment : Fragment() {

    private var _binding: FragmentPatternBinding? = null
    private val binding get() = _binding!!

    private val viewModel: BackgroundViewModel by viewModels(ownerProducer = { requireParentFragment() })

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPatternBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupPatternGrid()
        setupColorPickers()
        setupControls()
    }

    private fun setupPatternGrid() {
        val adapter = PatternAdapter(viewModel.patternPresets) { pattern ->
            viewModel.setBackgroundType(BackgroundType.PATTERN)
            viewModel.setPattern(
                pattern.second,
                Color.WHITE,
                Color.GRAY
            )
        }
        binding.recyclerPatterns.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.recyclerPatterns.adapter = adapter
    }

    private fun setupColorPickers() {
        binding.btnPrimaryColor.setOnClickListener {
            showColorPickerDialog { color ->
                // Update primary color
            }
        }

        binding.btnSecondaryColor.setOnClickListener {
            showColorPickerDialog { color ->
                // Update secondary color
            }
        }
    }

    private fun setupControls() {
        binding.sliderDensity.addOnChangeListener { slider, value, fromUser ->
            if (fromUser) {
                viewModel.setPatternDensity(value)
            }
        }

        binding.sliderRotation.addOnChangeListener { slider, value, fromUser ->
            if (fromUser) {
                viewModel.setPatternRotation(value)
            }
        }
    }

    private fun showColorPickerDialog(onColorSelected: (Int) -> Unit) {
        onColorSelected(Color.WHITE)
    }
}

// Pattern Adapter
class PatternAdapter(
    private val patterns: List<Pair<String, PatternType>>,
    private val onPatternSelected: (Pair<String, PatternType>) -> Unit
) : RecyclerView.Adapter<PatternAdapter.PatternViewHolder>() {

    class PatternViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.image_pattern)
        val textView: TextView = itemView.findViewById(R.id.text_pattern_name)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PatternViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_pattern, parent, false)
        return PatternViewHolder(view)
    }

    override fun onBindViewHolder(holder: PatternViewHolder, position: Int) {
        val pattern = patterns[position]
        holder.textView.text = pattern.first
        holder.itemView.setOnClickListener {
            onPatternSelected(pattern)
        }
    }

    override fun getItemCount() = patterns.size
}
