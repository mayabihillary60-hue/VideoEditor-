package com.yourname.videoeditor.feature.editor

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.yourname.videoeditor.library.rendering.filter.FilterType
import com.yourname.videoeditor.library.rendering.filter.FilterParameter

class FilterFragment : Fragment() {

    private val viewModel: FilterViewModel by viewModels()
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_filters, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupFilterRecyclerView()
        setupIntensitySlider()
        observeViewModel()
    }
    
    private fun setupFilterRecyclerView() {
        val filters = FilterType.values().toList()
        val adapter = FilterAdapter(filters) { filter ->
            viewModel.applyFilter(filter)
        }
        
        requireView().findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rv_filters).apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            this.adapter = adapter
        }
    }
    
    private fun setupIntensitySlider() {
        requireView().findViewById<SeekBar>(R.id.intensity_slider).setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    viewModel.setIntensity(progress / 100f)
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            }
        )
    }
    
    private fun observeViewModel() {
        viewModel.filterParameters.observe(viewLifecycleOwner) { parameters ->
            updateParameterControls(parameters)
        }
    }
    
    private fun updateParameterControls(parameters: List<FilterParameter>) {
        val container = requireView().findViewById<LinearLayout>(R.id.parameter_container)
        container.removeAllViews()
        
        parameters.forEach { param ->
            // Create parameter control UI
            // This would create sliders for each parameter
        }
    }
}