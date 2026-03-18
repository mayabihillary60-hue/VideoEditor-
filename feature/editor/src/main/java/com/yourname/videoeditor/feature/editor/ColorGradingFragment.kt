package com.yourname.videoeditor.feature.editor

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.button.MaterialButton
import com.google.android.material.slider.Slider
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.yourname.videoeditor.feature.editor.databinding.FragmentColorGradingBinding
import com.yourname.videoeditor.library.rendering.color.ColorGradingParams

class ColorGradingFragment : Fragment() {

    private var _binding: FragmentColorGradingBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ColorGradingViewModel by viewModels()

    private lateinit var tools: List<ColorToolItem>
    private var activeToolButton: MaterialButton? = null

    data class ColorToolItem(
        val name: String,
        val tool: ColorGradingViewModel.ColorTool,
        val icon: Int
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentColorGradingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupTools()
        setupToolbar()
        setupViewPager()
        setupObservers()
        setupBottomButtons()
    }

    private fun setupTools() {
        tools = listOf(
            ColorToolItem("Basic", ColorGradingViewModel.ColorTool.BASIC, android.R.drawable.ic_menu_edit),
            ColorToolItem("Tone", ColorGradingViewModel.ColorTool.TONE, android.R.drawable.ic_menu_gallery),
            ColorToolItem("Color", ColorGradingViewModel.ColorTool.COLOR, android.R.drawable.ic_menu_color),
            ColorToolItem("Curves", ColorGradingViewModel.ColorTool.CURVES, android.R.drawable.ic_menu_slideshow),
            ColorToolItem("HSL", ColorGradingViewModel.ColorTool.HSL, android.R.drawable.ic_menu_manage),
            ColorToolItem("RGB", ColorGradingViewModel.ColorTool.RGB, android.R.drawable.ic_menu_camera)
        )

        binding.toolContainer.removeAllViews()
        
        tools.forEach { tool ->
            val button = MaterialButton(requireContext()).apply {
                text = tool.name
                setIconResource(tool.icon)
                iconSize = 24
                isIconGravity = MaterialButton.ICON_GRAVITY_TEXT_START
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { marginEnd = 8 }
                
                setOnClickListener {
                    selectTool(tool, this)
                }
            }
            binding.toolContainer.addView(button)
            
            // Select first tool by default
            if (tool.tool == ColorGradingViewModel.ColorTool.BASIC) {
                selectTool(tool, button)
            }
        }
    }

    private fun selectTool(tool: ColorToolItem, button: MaterialButton) {
        activeToolButton?.isChecked = false
        button.isChecked = true
        activeToolButton = button
        
        viewModel.selectTool(tool.tool)
        updateControlsForTool(tool.tool)
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_histogram -> {
                    toggleHistogram()
                    true
                }
                R.id.action_split_view -> {
                    toggleSplitView()
                    true
                }
                R.id.action_presets -> {
                    showPresetsDialog()
                    true
                }
                R.id.action_undo -> {
                    viewModel.undo()
                    true
                }
                R.id.action_redo -> {
                    viewModel.redo()
                    true
                }
                else -> false
            }
        }
    }

    private fun setupViewPager() {
        // Setup before/after view pager
        // This would show original vs graded video
    }

    private fun setupObservers() {
        viewModel.colorParams.observe(viewLifecycleOwner) { params ->
            updateUIWithParams(params)
        }
    }

    private fun updateControlsForTool(tool: ColorGradingViewModel.ColorTool) {
        binding.controlsContainer.removeAllViews()
        
        when (tool) {
            ColorGradingViewModel.ColorTool.BASIC -> {
                binding.controlsContainer.addView(createBasicControls())
            }
            ColorGradingViewModel.ColorTool.TONE -> {
                binding.controlsContainer.addView(createToneControls())
            }
            ColorGradingViewModel.ColorTool.COLOR -> {
                binding.controlsContainer.addView(createColorControls())
            }
            ColorGradingViewModel.ColorTool.CURVES -> {
                binding.controlsContainer.addView(createCurvesControls())
            }
            ColorGradingViewModel.ColorTool.HSL -> {
                binding.controlsContainer.addView(createHSLControls())
            }
            ColorGradingViewModel.ColorTool.RGB -> {
                binding.controlsContainer.addView(createRGBControls())
            }
        }
    }

    private fun createBasicControls(): View {
        return LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

            addView(createSliderControl("Exposure", -2f, 2f, viewModel.colorParams.value.exposure) { value ->
                viewModel.beginAdjustment()
                viewModel.setExposure(value)
                viewModel.endAdjustment()
            })

            addView(createSliderControl("Contrast", 0f, 3f, viewModel.colorParams.value.contrast) { value ->
                viewModel.beginAdjustment()
                viewModel.setContrast(value)
                viewModel.endAdjustment()
            })

            addView(createSliderControl("Saturation", 0f, 2f, viewModel.colorParams.value.saturation) { value ->
                viewModel.beginAdjustment()
                viewModel.setSaturation(value)
                viewModel.endAdjustment()
            })

            addView(createSliderControl("Vibrance", 0f, 2f, viewModel.colorParams.value.vibrance) { value ->
                viewModel.beginAdjustment()
                viewModel.setVibrance(value)
                viewModel.endAdjustment()
            })

            addView(createSliderControl("Hue Shift", -180f, 180f, viewModel.colorParams.value.hueShift) { value ->
                viewModel.beginAdjustment()
                viewModel.setHueShift(value)
                viewModel.endAdjustment()
            })
        }
    }

    private fun createToneControls(): View {
        return LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL

            addView(createSliderControl("Highlights", -1f, 1f, viewModel.colorParams.value.highlights) { value ->
                viewModel.beginAdjustment()
                viewModel.setHighlights(value)
                viewModel.endAdjustment()
            })

            addView(createSliderControl("Shadows", -1f, 1f, viewModel.colorParams.value.shadows) { value ->
                viewModel.beginAdjustment()
                viewModel.setShadows(value)
                viewModel.endAdjustment()
            })

            addView(createSliderControl("Whites", -1f, 1f, viewModel.colorParams.value.whites) { value ->
                viewModel.beginAdjustment()
                viewModel.setWhites(value)
                viewModel.endAdjustment()
            })

            addView(createSliderControl("Blacks", -1f, 1f, viewModel.colorParams.value.blacks) { value ->
                viewModel.beginAdjustment()
                viewModel.setBlacks(value)
                viewModel.endAdjustment()
            })

            addView(createSliderControl("Temperature", -1f, 1f, viewModel.colorParams.value.temperature) { value ->
                viewModel.beginAdjustment()
                viewModel.setTemperature(value)
                viewModel.endAdjustment()
            })

            addView(createSliderControl("Tint", -1f, 1f, viewModel.colorParams.value.tint) { value ->
                viewModel.beginAdjustment()
                viewModel.setTint(value)
                viewModel.endAdjustment()
            })
        }
    }

    private fun createColorControls(): View {
        return LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL

            addView(createSliderControl("Gamma", 0.1f, 3f, viewModel.colorParams.value.gamma) { value ->
                viewModel.beginAdjustment()
                viewModel.setGamma(value)
                viewModel.endAdjustment()
            })

            // Add RGB sliders for lift
            addView(createColorSliderControl("Lift R", 0f, 1f, viewModel.colorParams.value.lift[0], Color.RED) { value ->
                viewModel.beginAdjustment()
                viewModel.setLift(value, viewModel.colorParams.value.lift[1], viewModel.colorParams.value.lift[2])
                viewModel.endAdjustment()
            })

            addView(createColorSliderControl("Lift G", 0f, 1f, viewModel.colorParams.value.lift[1], Color.GREEN) { value ->
                viewModel.beginAdjustment()
                viewModel.setLift(viewModel.colorParams.value.lift[0], value, viewModel.colorParams.value.lift[2])
                viewModel.endAdjustment()
            })

            addView(createColorSliderControl("Lift B", 0f, 1f, viewModel.colorParams.value.lift[2], Color.BLUE) { value ->
                viewModel.beginAdjustment()
                viewModel.setLift(viewModel.colorParams.value.lift[0], viewModel.colorParams.value.lift[1], value)
                viewModel.endAdjustment()
            })
        }
    }

    private fun createCurvesControls(): View {
        return LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL

            // This would contain a curve editor view
            val textView = TextView(requireContext()).apply {
                text = "RGB Curves (Coming Soon)"
                textSize = 16f
                setTextColor(Color.WHITE)
                textAlignment = TextView.TEXT_ALIGNMENT_CENTER
            }
            addView(textView)
        }
    }

    private fun createHSLControls(): View {
        return LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL

            val textView = TextView(requireContext()).apply {
                text = "HSL Adjustment (Coming Soon)"
                textSize = 16f
                setTextColor(Color.WHITE)
                textAlignment = TextView.TEXT_ALIGNMENT_CENTER
            }
            addView(textView)
        }
    }

    private fun createRGBControls(): View {
        return createColorControls() // Reuse RGB controls for now
    }

    private fun createSliderControl(
        label: String,
        min: Float,
        max: Float,
        value: Float,
        onValueChange: (Float) -> Unit
    ): View {
        return LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 8, 0, 8) }

            // Label and value
            val labelLayout = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )

                addView(TextView(requireContext()).apply {
                    text = label
                    setTextColor(Color.WHITE)
                    textSize = 14f
                    layoutParams = LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f
                    )
                })

                addView(TextView(requireContext()).apply {
                    text = String.format("%.2f", value)
                    setTextColor(Color.parseColor("#FF4081"))
                    textSize = 14f
                    id = R.id.tv_value
                })
            }
            addView(labelLayout)

            // Slider
            addView(com.google.android.material.slider.Slider(requireContext()).apply {
                valueFrom = min
                valueTo = max
                this.value = value
                setLabelFormatter { value ->
                    String.format("%.2f", value)
                }
                
                addOnChangeListener { slider, valValue, fromUser ->
                    if (fromUser) {
                        findViewById<TextView>(R.id.tv_value)?.text = String.format("%.2f", valValue)
                        onValueChange(valValue)
                    }
                }
            })
        }
    }

    private fun createColorSliderControl(
        label: String,
        min: Float,
        max: Float,
        value: Float,
        color: Int,
        onValueChange: (Float) -> Unit
    ): View {
        return LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 8, 0, 8) }

            val labelLayout = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL

                addView(TextView(requireContext()).apply {
                    text = label
                    setTextColor(color)
                    textSize = 14f
                    layoutParams = LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f
                    )
                })

                addView(TextView(requireContext()).apply {
                    text = String.format("%.2f", value)
                    setTextColor(color)
                    textSize = 14f
                    id = R.id.tv_value
                })
            }
            addView(labelLayout)

            addView(com.google.android.material.slider.Slider(requireContext()).apply {
                valueFrom = min
                valueTo = max
                this.value = value
                setLabelFormatter { value ->
                    String.format("%.2f", value)
                }
                
                addOnChangeListener { slider, valValue, fromUser ->
                    if (fromUser) {
                        findViewById<TextView>(R.id.tv_value)?.text = String.format("%.2f", valValue)
                        onValueChange(valValue)
                    }
                }
            })
        }
    }

    private fun updateUIWithParams(params: ColorGradingParams) {
        // Update UI based on current tool
        // This would refresh slider values
    }

    private fun toggleHistogram() {
        binding.histogramContainer.visibility = if (binding.histogramContainer.visibility == View.VISIBLE) {
            View.GONE
        } else {
            View.VISIBLE
        }
    }

    private fun toggleSplitView() {
        binding.splitView.visibility = if (binding.splitView.visibility == View.VISIBLE) {
            View.GONE
        } else {
            View.VISIBLE
        }
    }

    private fun showPresetsDialog() {
        // Show presets dialog
    }

    private fun setupBottomButtons() {
        binding.btnReset.setOnClickListener {
            viewModel.resetCurrentTool()
        }

        binding.btnCancel.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.btnApply.setOnClickListener {
            // Apply color grading and return
            parentFragmentManager.setFragmentResult(
                "color_grading_result",
                Bundle().apply {
                    putSerializable("params", viewModel.colorParams.value)
                }
            )
            parentFragmentManager.popBackStack()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}