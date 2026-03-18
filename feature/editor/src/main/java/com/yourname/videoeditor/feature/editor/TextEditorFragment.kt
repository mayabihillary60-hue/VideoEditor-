package com.yourname.videoeditor.feature.editor

import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.slider.Slider
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.yourname.videoeditor.feature.editor.databinding.FragmentTextEditorBinding
import com.yourname.videoeditor.library.rendering.overlay.TextAlignment
import com.yourname.videoeditor.library.rendering.overlay.TextStyle

class TextEditorFragment : Fragment() {

    private var _binding: FragmentTextEditorBinding? = null
    private val binding get() = _binding!!

    private val viewModel: OverlayViewModel by viewModels(ownerProducer = { requireParentFragment() })

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTextEditorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupTextInput()
        setupFontSpinner()
        setupColorGrid()
        setupStyleButtons()
        setupAlignmentButtons()
        setupSizeSlider()
        setupSpacingControls()
        setupShadowControls()
        setupObservers()
        setupButtons()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun setupTextInput() {
        val selectedElement = viewModel.getSelectedElement()
        binding.etText.setText(selectedElement?.textOverlay?.text ?: "")

        binding.etText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                viewModel.getSelectedElement()?.id?.let { id ->
                    viewModel.updateElementText(id, s.toString())
                }
            }
        })
    }

    private fun setupFontSpinner() {
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            viewModel.availableFonts
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerFont.adapter = adapter

        binding.spinnerFont.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                // Handle font change
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupColorGrid() {
        val adapter = ColorAdapter(viewModel.availableColors) { color ->
            viewModel.getSelectedElement()?.id?.let { id ->
                viewModel.updateElementTextColor(id, color)
            }
        }
        binding.recyclerColors.layoutManager = GridLayoutManager(requireContext(), 4)
        binding.recyclerColors.adapter = adapter
    }

    private fun setupStyleButtons() {
        binding.btnBold.setOnClickListener {
            viewModel.getSelectedElement()?.id?.let { id ->
                val element = viewModel.getSelectedElement()
                val currentStyle = element?.textOverlay?.style ?: TextStyle.NORMAL
                val newStyle = if (currentStyle == TextStyle.BOLD) TextStyle.NORMAL else TextStyle.BOLD
                viewModel.updateElementTextStyle(id, newStyle)
                updateStyleButtons()
            }
        }

        binding.btnItalic.setOnClickListener {
            viewModel.getSelectedElement()?.id?.let { id ->
                val element = viewModel.getSelectedElement()
                val currentStyle = element?.textOverlay?.style ?: TextStyle.NORMAL
                val newStyle = if (currentStyle == TextStyle.ITALIC) TextStyle.NORMAL else TextStyle.ITALIC
                viewModel.updateElementTextStyle(id, newStyle)
                updateStyleButtons()
            }
        }

        binding.btnUnderline.setOnClickListener {
            viewModel.getSelectedElement()?.id?.let { id ->
                val element = viewModel.getSelectedElement()
                val newUnderline = !(element?.textOverlay?.underline ?: false)
                viewModel.updateElementTextStyle(id, if (newUnderline) TextStyle.BOLD else TextStyle.NORMAL)
                // Note: You'd need to add underline to TextOverlay
            }
        }
    }

    private fun setupAlignmentButtons() {
        binding.btnAlignLeft.setOnClickListener {
            viewModel.getSelectedElement()?.id?.let { id ->
                // Set left alignment
            }
        }

        binding.btnAlignCenter.setOnClickListener {
            viewModel.getSelectedElement()?.id?.let { id ->
                // Set center alignment
            }
        }

        binding.btnAlignRight.setOnClickListener {
            viewModel.getSelectedElement()?.id?.let { id ->
                // Set right alignment
            }
        }
    }

    private fun setupSizeSlider() {
        binding.sliderFontSize.addOnChangeListener { slider, value, fromUser ->
            if (fromUser) {
                viewModel.getSelectedElement()?.id?.let { id ->
                    viewModel.updateElementFontSize(id, value)
                }
            }
        }
    }

    private fun setupSpacingControls() {
        binding.sliderLetterSpacing.addOnChangeListener { slider, value, fromUser ->
            if (fromUser) {
                // Update letter spacing
            }
        }

        binding.sliderLineSpacing.addOnChangeListener { slider, value, fromUser ->
            if (fromUser) {
                // Update line spacing
            }
        }
    }

    private fun setupShadowControls() {
        binding.checkboxShadow.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                binding.shadowControls.visibility = View.VISIBLE
            } else {
                binding.shadowControls.visibility = View.GONE
            }
        }

        binding.sliderShadowRadius.addOnChangeListener { slider, value, fromUser ->
            if (fromUser) {
                // Update shadow radius
            }
        }
    }

    private fun setupObservers() {
        viewModel.getSelectedElement()?.let { element ->
            element.textOverlay?.let { text ->
                binding.sliderFontSize.value = text.fontSize
                updateStyleButtons()
            }
        }
    }

    private fun updateStyleButtons() {
        val element = viewModel.getSelectedElement()
        element?.textOverlay?.let { text ->
            binding.btnBold.isChecked = text.style == TextStyle.BOLD || text.style == TextStyle.BOLD_ITALIC
            binding.btnItalic.isChecked = text.style == TextStyle.ITALIC || text.style == TextStyle.BOLD_ITALIC
            binding.btnUnderline.isChecked = text.underline
        }
    }

    private fun setupButtons() {
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

// Color adapter for color grid
class ColorAdapter(
    private val colors: List<Int>,
    private val onColorSelected: (Int) -> Unit
) : RecyclerView.Adapter<ColorAdapter.ColorViewHolder>() {

    class ColorViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val colorView: View = itemView.findViewById(R.id.view_color)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ColorViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_color, parent, false)
        return ColorViewHolder(view)
    }

    override fun onBindViewHolder(holder: ColorViewHolder, position: Int) {
        holder.colorView.setBackgroundColor(colors[position])
        holder.itemView.setOnClickListener {
            onColorSelected(colors[position])
        }
    }

    override fun getItemCount() = colors.size
}
