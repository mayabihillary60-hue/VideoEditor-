package com.yourname.videoeditor.feature.editor

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.SeekBar
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import androidx.viewpager2.widget.ViewPager2
import com.yourname.videoeditor.feature.editor.databinding.FragmentTransitionsBinding

class TransitionFragment : Fragment() {

    private var _binding: FragmentTransitionsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TransitionViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTransitionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupPreviewControls()
        setupTabLayout()
        setupSettingsControls()
        setupObservers()
        setupButtons()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun setupPreviewControls() {
        binding.previewSeekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    viewModel.seekPreview(progress / 100f)
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        binding.btnPlayPreview.setOnClickListener {
            if (viewModel.isPlaying.value) {
                viewModel.pausePreview()
                binding.btnPlayPreview.setImageResource(android.R.drawable.ic_media_play)
            } else {
                viewModel.playPreview()
                binding.btnPlayPreview.setImageResource(android.R.drawable.ic_media_pause)
            }
        }
    }

    private fun setupTabLayout() {
        // Create adapter for ViewPager2
        val adapter = TransitionPagerAdapter(this, viewModel.transitionCategories)
        binding.transitionPager.adapter = adapter

        // Connect TabLayout with ViewPager2
        TabLayoutMediator(binding.tabLayout, binding.transitionPager) { tab, position ->
            tab.text = viewModel.transitionCategories[position].name
        }.attach()
    }

    private fun setupSettingsControls() {
        // Duration slider
        binding.durationSlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val durationMs = 100 + (progress * 100) // 100ms to 5000ms
                viewModel.setDuration(durationMs.toLong())
                binding.tvDuration.text = String.format("%.1fs", durationMs / 1000f)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Easing spinner
        val easingAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            viewModel.easingOptions.map { it.second }
        )
        easingAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.easingSpinner.adapter = easingAdapter
        binding.easingSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                viewModel.setEasing(viewModel.easingOptions[position].first)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Intensity slider
        binding.intensitySlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                viewModel.setIntensity(progress / 100f)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun setupObservers() {
        viewModel.selectedTransition.observe(viewLifecycleOwner) { transition ->
            transition?.let {
                // Show/hide intensity based on transition type
                val showIntensity = it in listOf(
                    TransitionType.GLITCH,
                    TransitionType.LIGHT_LEAK,
                    TransitionType.BURN,
                    TransitionType.DISSOLVE
                )
                binding.intensityLabel.visibility = if (showIntensity) View.VISIBLE else View.GONE
                binding.intensitySlider.visibility = if (showIntensity) View.VISIBLE else View.GONE
            }
        }

        viewModel.previewProgress.observe(viewLifecycleOwner) { progress ->
            binding.previewSeekbar.progress = (progress * 100).toInt()
        }

        viewModel.isPlaying.observe(viewLifecycleOwner) { isPlaying ->
            binding.btnPlayPreview.visibility = if (isPlaying) View.VISIBLE else View.GONE
        }
    }

    private fun setupButtons() {
        binding.btnCancel.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.btnApply.setOnClickListener {
            // Return transition config
            parentFragmentManager.setFragmentResult(
                "transition_result",
                Bundle().apply {
                    putSerializable("transition_config", viewModel.transitionConfig.value)
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

// ViewPager2 Adapter for transition categories
class TransitionPagerAdapter(
    fragment: Fragment,
    private val categories: List<TransitionCategory>
) : androidx.viewpager2.adapter.FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int = categories.size

    override fun createFragment(position: Int): Fragment {
        return TransitionGridFragment.newInstance(categories[position].transitions)
    }
}

// Grid fragment for each category
class TransitionGridFragment : Fragment() {

    companion object {
        fun newInstance(transitions: List<TransitionType>): TransitionGridFragment {
            val fragment = TransitionGridFragment()
            fragment.arguments = Bundle().apply {
                putSerializable("transitions", ArrayList(transitions))
            }
            return fragment
        }
    }

    private lateinit var adapter: TransitionAdapter
    private val viewModel: TransitionViewModel by viewModels(ownerProducer = { requireParentFragment() })

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return RecyclerView(requireContext()).apply {
            layoutManager = GridLayoutManager(context, 3)
            adapter = this@TransitionGridFragment.adapter
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val transitions = arguments?.getSerializable("transitions") as? List<TransitionType> ?: emptyList()
        
        adapter = TransitionAdapter(transitions, viewModel) { transition ->
            viewModel.selectTransition(transition)
        }
    }
}
