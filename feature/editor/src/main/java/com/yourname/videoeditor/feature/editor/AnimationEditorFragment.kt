package com.yourname.videoeditor.feature.editor

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.SeekBar
import android.widget.Spinner
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.android.material.button.MaterialButton
import com.google.android.material.slider.Slider
import com.yourname.videoeditor.feature.editor.databinding.FragmentAnimationEditorBinding
import com.yourname.videoeditor.library.rendering.overlay.AnimationType

class AnimationEditorFragment : Fragment() {

    private var _binding: FragmentAnimationEditorBinding? = null
    private val binding get() = _binding!!

    private val viewModel: OverlayViewModel by viewModels(ownerProducer = { requireParentFragment() })

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAnimationEditorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupAnimationSpinners()
        setupDurationSlider()
        setupObservers()
        setupButtons()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun setupAnimationSpinners() {
        val animations = viewModel.animationPresets.map { it.second }
        
        // In Animation spinner
        val inAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, animations)
        inAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerAnimationIn.adapter = inAdapter
        
        binding.spinnerAnimationIn.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                viewModel.getSelectedElement()?.id?.let { elementId ->
                    val inAnim = viewModel.animationPresets[position].first
                    val outAnim = viewModel.animationPresets[binding.spinnerAnimationOut.selectedItemPosition].first
                    viewModel.updateElementAnimation(elementId, inAnim, outAnim)
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Out Animation spinner
        val outAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, animations)
        outAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerAnimationOut.adapter = outAdapter
        
        binding.spinnerAnimationOut.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                viewModel.getSelectedElement()?.id?.let { elementId ->
                    val inAnim = viewModel.animationPresets[binding.spinnerAnimationIn.selectedItemPosition].first
                    val outAnim = viewModel.animationPresets[position].first
                    viewModel.updateElementAnimation(elementId, inAnim, outAnim)
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupDurationSlider() {
        binding.sliderDuration.addOnChangeListener { slider, value, fromUser ->
            if (fromUser) {
                binding.tvDurationValue.text = "${value.toInt()} ms"
                viewModel.getSelectedElement()?.id?.let { elementId ->
                    val element = viewModel.getSelectedElement()
                    element?.let {
                        viewModel.updateElementTiming(
                            elementId,
                            it.timing.startTimeMs,
                            it.timing.startTimeMs + value.toLong()
                        )
                    }
                }
            }
        }
    }

    private fun setupObservers() {
        viewModel.getSelectedElement()?.let { element ->
            // Set spinner selections based on current animation
            val inAnimIndex = viewModel.animationPresets.indexOfFirst { it.first == element.timing.animationIn }
            val outAnimIndex = viewModel.animationPresets.indexOfFirst { it.first == element.timing.animationOut }
            
            if (inAnimIndex >= 0) {
                binding.spinnerAnimationIn.setSelection(inAnimIndex)
            }
            if (outAnimIndex >= 0) {
                binding.spinnerAnimationOut.setSelection(outAnimIndex)
            }
            
            // Set duration
            val duration = element.timing.endTimeMs - element.timing.startTimeMs
            binding.sliderDuration.value = duration.toFloat()
            binding.tvDurationValue.text = "$duration ms"
        }
    }

    private fun setupButtons() {
        binding.btnPreview.setOnClickListener {
            // Preview animation
            viewModel.setCurrentTime(viewModel.getSelectedElement()?.timing?.startTimeMs ?: 0)
            // Trigger animation preview
        }

        binding.btnCancel.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.btnApply.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
