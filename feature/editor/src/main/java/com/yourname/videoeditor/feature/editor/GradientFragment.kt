package com.yourname.videoeditor.feature.editor

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RadioGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.yourname.videoeditor.feature.editor.databinding.FragmentGradientBinding
import com.yourname.videoeditor.library.rendering.background.GradientType

class GradientFragment : Fragment() {

    private var _binding: FragmentGradientBinding? = null
    private val binding get() = _binding!!

    private val viewModel: BackgroundViewModel by viewModels(ownerProducer = { requireParentFragment() })

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGradientBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupGradientTypeRadio()
        setupPresets()
        setupColorPickers()
    }

    private fun setupGradientTypeRadio() {
        binding.radioGroup.setOnCheckedChangeListener { _, checkedId ->
            val type = when (checkedId) {
                R.id.radio_linear -> GradientType.LINEAR
                R.id.radio_radial -> GradientType.RADIAL
                R.id.radio_sweep -> GradientType.SWEEP
                else -> GradientType.LINEAR
            }
            // Update gradient type
        }
    }

    private fun setupPresets() {
        val adapter = GradientPresetAdapter(viewModel.gradientPresets) { preset ->
            viewModel.applyPreset(preset.first)
        }
        binding.recyclerPresets.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.recyclerPresets.adapter = adapter
    }

    private fun setupColorPickers() {
        // Start color picker
        binding.btnStartColor.setOnClickListener {
            showColorPickerDialog { color ->
                // Update start color
            }
        }

        // End color picker
        binding.btnEndColor.setOnClickListener {
            showColorPickerDialog { color ->
                // Update end color
            }
        }
    }

    private fun showColorPickerDialog(onColorSelected: (Int) -> Unit) {
        // Show color picker dialog
        // For simplicity, using a predefined color picker
        onColorSelected(Color.parseColor("#FF4081"))
    }
}

// Gradient Preset Adapter
class GradientPresetAdapter(
    private val presets: List<Triple<String, Int, Int>>,
    private val onPresetSelected: (Triple<String, Int, Int>) -> Unit
) : RecyclerView.Adapter<GradientPresetAdapter.PresetViewHolder>() {

    class PresetViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.image_preset)
        val textView: TextView = itemView.findViewById(R.id.text_preset_name)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PresetViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_gradient_preset, parent, false)
        return PresetViewHolder(view)
    }

    override fun onBindViewHolder(holder: PresetViewHolder, position: Int) {
        val preset = presets[position]
        holder.textView.text = preset.first
        
        val gradientDrawable = GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(preset.second, preset.third)
        )
        holder.imageView.background = gradientDrawable
        
        holder.itemView.setOnClickListener {
            onPresetSelected(preset)
        }
    }

    override fun getItemCount() = presets.size
}
