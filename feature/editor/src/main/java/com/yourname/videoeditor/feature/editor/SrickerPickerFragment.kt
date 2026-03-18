package com.yourname.videoeditor.feature.editor

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.yourname.videoeditor.feature.editor.databinding.FragmentStickerPickerBinding

class StickerPickerFragment : Fragment() {

    private var _binding: FragmentStickerPickerBinding? = null
    private val binding get() = _binding!!

    private val viewModel: OverlayViewModel by viewModels(ownerProducer = { requireParentFragment() })

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStickerPickerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupTabLayout()
        setupCategories()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun setupTabLayout() {
        // Create adapter for ViewPager2
        val adapter = StickerPagerAdapter(this, viewModel.stickerCategories.keys.toList())
        binding.viewPager.adapter = adapter

        // Connect TabLayout with ViewPager2
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = viewModel.stickerCategories.keys.toList()[position]
        }.attach()
    }

    private fun setupCategories() {
        // Categories are handled by ViewPager adapter
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

// ViewPager2 Adapter for sticker categories
class StickerPagerAdapter(
    fragment: Fragment,
    private val categories: List<String>
) : androidx.viewpager2.adapter.FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int = categories.size

    override fun createFragment(position: Int): Fragment {
        return StickerGridFragment.newInstance(categories[position])
    }
}

// Grid fragment for each sticker category
class StickerGridFragment : Fragment() {

    companion object {
        private const val ARG_CATEGORY = "category"

        fun newInstance(category: String): StickerGridFragment {
            val fragment = StickerGridFragment()
            fragment.arguments = Bundle().apply {
                putString(ARG_CATEGORY, category)
            }
            return fragment
        }
    }

    private val viewModel: OverlayViewModel by viewModels(ownerProducer = { requireParentFragment() })
    private lateinit var category: String
    private lateinit var adapter: StickerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        category = arguments?.getString(ARG_CATEGORY) ?: "Emoji"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val recyclerView = RecyclerView(requireContext())
        recyclerView.layoutManager = GridLayoutManager(requireContext(), 4)
        
        val stickers = viewModel.stickerCategories[category] ?: emptyList()
        adapter = StickerAdapter(stickers) { sticker ->
            viewModel.addStickerOverlay(sticker)
            parentFragmentManager.popBackStack()
        }
        recyclerView.adapter = adapter
        
        return recyclerView
    }
}

// Sticker adapter for grid items
class StickerAdapter(
    private val stickers: List<String>,
    private val onStickerClick: (String) -> Unit
) : RecyclerView.Adapter<StickerAdapter.StickerViewHolder>() {

    class StickerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textView: TextView = itemView as TextView
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StickerViewHolder {
        val textView = TextView(parent.context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                100.dpToPx(parent.context)
            )
            textSize = 32f
            gravity = android.view.Gravity.CENTER
        }
        return StickerViewHolder(textView)
    }

    override fun onBindViewHolder(holder: StickerViewHolder, position: Int) {
        holder.textView.text = stickers[position]
        holder.itemView.setOnClickListener {
            onStickerClick(stickers[position])
        }
    }

    override fun getItemCount() = stickers.size

    private fun Int.dpToPx(context: Context): Int {
        return (this * context.resources.displayMetrics.density).toInt()
    }
}
