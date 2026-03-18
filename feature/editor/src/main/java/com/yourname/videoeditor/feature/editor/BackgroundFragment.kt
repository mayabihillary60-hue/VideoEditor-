package com.yourname.videoeditor.feature.editor

import android.graphics.Color
import android.os.Bundle
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
import com.yourname.videoeditor.feature.editor.databinding.FragmentBackgroundBinding
import com.yourname.videoeditor.library.rendering.background.BackgroundType
import com.yourname.videoeditor.library.rendering.background.GradientType
import com.yourname.videoeditor.library.rendering.background.PatternType

class BackgroundFragment : Fragment() {

    private var _binding: FragmentBackgroundBinding? = null
    private val binding get() = _binding!!

    private val viewModel: BackgroundViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBackgroundBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupTabLayout()
        setupObservers()
        setupButtons()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_none -> {
                    viewModel.disableBackground()
                    true
                }
                else -> false
            }
        }
    }

    private fun setupTabLayout() {
        val adapter = BackgroundPagerAdapter(this)
        binding.viewPager.adapter = adapter

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Solid"
                1 -> "Gradient"
                2 -> "Pattern"
                3 -> "Image"
                else -> "Video"
            }
        }.attach()
    }

    private fun setupObservers() {
        viewModel.backgroundConfig.observe(viewLifecycleOwner) { config ->
            // Update UI based on config
        }
    }

    private fun setupButtons() {
        binding.btnCancel.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.btnApply.setOnClickListener {
            parentFragmentManager.setFragmentResult(
                "background_result",
                Bundle().apply {
                    putSerializable("background_config", viewModel.backgroundConfig.value)
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

// ViewPager2 Adapter for background tabs
class BackgroundPagerAdapter(fragment: Fragment) : 
    androidx.viewpager2.adapter.FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int = 5

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> SolidColorFragment()
            1 -> GradientFragment()
            2 -> PatternFragment()
            3 -> ImageBackgroundFragment()
            else -> VideoBackgroundFragment()
        }
    }
}
