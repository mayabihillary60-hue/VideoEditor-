package com.yourname.videoeditor.feature.editor

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.SeekBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.android.material.button.MaterialButton
import com.yourname.videoeditor.feature.editor.databinding.FragmentSpeedControlBinding
import com.yourname.videoeditor.library.rendering.speed.SpeedPreset
import com.yourname.videoeditor.library.rendering.VideoPlayerView

class SpeedControlFragment : Fragment() {

    private var _binding: FragmentSpeedControlBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SpeedViewModel by viewModels()

    private var videoUri: Uri? = null
    private var inputPath: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSpeedControlBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        arguments?.let {
            videoUri = it.getParcelable("video_uri")
            inputPath = it.getString("input_path")
        }

        setupToolbar()
        setupPresetButtons()
        setupSliders()
        setupOptions()
        setupObservers()
        setupVideoPlayer()
        setupButtons()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun setupPresetButtons() {
        viewModel.presetOptions.value.forEach { (preset, label) ->
            val button = MaterialButton(requireContext()).apply {
                text = label
                layoutParams = ViewGroup.LayoutParams(
                    100.dpToPx(),
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                setOnClickListener {
                    viewModel.setPreset(preset)
                    updatePresetSelection(this)
                }
            }
            binding.presetContainer.addView(button)
        }
    }

    private fun setupSliders() {
        binding.speedSlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    val speed = 0.25f + (progress / 375f) * 3.75f
                    viewModel.setCustomSpeed(speed)
                    binding.tvSpeedValue.text = String.format("%.2fx", speed)
                    binding.tvSpeedIndicator.text = String.format("%.1fx", speed)
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun setupOptions() {
        binding.cbPreservePitch.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setPreservePitch(isChecked)
        }
        
        binding.cbFrameBlending.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setFrameBlending(isChecked)
        }
    }

    private fun setupObservers() {
        viewModel.speedConfig.observe(viewLifecycleOwner) { config ->
            // Update UI based on config
            binding.tvSpeedValue.text = viewModel.getCurrentSpeedLabel()
            binding.tvSpeedIndicator.text = viewModel.getCurrentSpeedLabel()
        }

        viewModel.isProcessing.observe(viewLifecycleOwner) { isProcessing ->
            binding.progressContainer.visibility = if (isProcessing) View.VISIBLE else View.GONE
            binding.btnApply.isEnabled = !isProcessing
        }

        viewModel.progress.observe(viewLifecycleOwner) { progress ->
            binding.progressBar.progress = progress.toInt()
            binding.tvProgress.text = "${progress.toInt()}%"
        }
    }

    private fun setupVideoPlayer() {
        videoUri?.let { uri ->
            binding.playerView.setVideoUri(uri)
        }
    }

    private fun setupButtons() {
        binding.btnCancel.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.btnApply.setOnClickListener {
            inputPath?.let { path ->
                val outputPath = generateOutputPath(path)
                viewModel.applySpeedChange(path, outputPath) { outputFile ->
                    // Return result
                    parentFragmentManager.setFragmentResult(
                        "speed_result",
                        Bundle().apply {
                            putString("output_path", outputFile)
                        }
                    )
                    parentFragmentManager.popBackStack()
                }
            }
        }
    }

    private fun generateOutputPath(inputPath: String): String {
        val file = java.io.File(inputPath)
        val parent = file.parent
        val name = file.nameWithoutExtension
        return "$parent/${name}_speed_${viewModel.getCurrentSpeedLabel().replace("x", "")}.mp4"
    }

    private fun updatePresetSelection(selectedButton: MaterialButton) {
        for (i in 0 until binding.presetContainer.childCount) {
            val child = binding.presetContainer.getChildAt(i)
            if (child is MaterialButton) {
                child.isChecked = child == selectedButton
            }
        }
    }

    private fun Int.dpToPx(): Int {
        return (this * resources.displayMetrics.density).toInt()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.playerView.release()
        _binding = null
    }
}
