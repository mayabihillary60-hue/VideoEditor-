package com.yourname.videoeditor.feature.gallery

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.yourname.videoeditor.feature.gallery.data.GalleryRepository
import com.yourname.videoeditor.feature.gallery.databinding.FragmentGalleryBinding
import com.yourname.videoeditor.feature.gallery.model.VideoItem
import kotlinx.coroutines.launch

class GalleryFragment : Fragment() {

    private var _binding: FragmentGalleryBinding? = null
    private val binding get() = _binding!!

    private val viewModel: GalleryViewModel by viewModels {
        GalleryViewModelFactory(GalleryRepository(requireContext()))
    }

    private lateinit var adapter: GalleryAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGalleryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        setupObservers()
        checkPermissions()
    }

    private fun setupRecyclerView() {
        adapter = GalleryAdapter(
            videos = emptyList(),
            onItemClick = { video ->
                if (viewModel.selectedVideos.value.isEmpty()) {
                    // Single selection - navigate to editor
                    navigateToEditor(video)
                } else {
                    // Multi selection mode
                    viewModel.toggleVideoSelection(video)
                }
            },
            onItemLongClick = { video ->
                viewModel.toggleVideoSelection(video)
                true
            },
            isSelected = { video -> viewModel.isSelected(video) }
        )

        binding.rvGallery.apply {
            layoutManager = GridLayoutManager(requireContext(), 3)
            adapter = this@GalleryFragment.adapter
        }
    }

    private fun setupObservers() {
        viewModel.videos.observe(viewLifecycleOwner) { videos ->
            adapter.updateVideos(videos)
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                binding.tvError.text = it
                binding.tvError.visibility = View.VISIBLE
            } ?: run {
                binding.tvError.visibility = View.GONE
            }
        }

        viewModel.selectedVideos.observe(viewLifecycleOwner) { selectedVideos ->
            updateSelectionMode(selectedVideos.isNotEmpty())
        }
    }

    private fun updateSelectionMode(isSelectionMode: Boolean) {
        if (isSelectionMode) {
            binding.toolbar.title = "${viewModel.selectedVideos.value.size} selected"
            binding.toolbar.menu.findItem(R.id.action_select_all).isVisible = true
            binding.toolbar.menu.findItem(R.id.action_import).isVisible = true
            binding.toolbar.menu.findItem(R.id.action_cancel).isVisible = true
        } else {
            binding.toolbar.title = "Select Videos"
            binding.toolbar.menu.findItem(R.id.action_select_all).isVisible = false
            binding.toolbar.menu.findItem(R.id.action_import).isVisible = false
            binding.toolbar.menu.findItem(R.id.action_cancel).isVisible = false
        }
    }

    private fun checkPermissions() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_VIDEO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        when {
            ContextCompat.checkSelfPermission(requireContext(), permission) == PackageManager.PERMISSION_GRANTED -> {
                viewModel.loadVideos()
            }
            shouldShowRequestPermissionRationale(permission) -> {
                binding.tvError.text = "Storage permission is needed to access videos"
                binding.tvError.visibility = View.VISIBLE
                binding.btnRequestPermission.visibility = View.VISIBLE
                binding.btnRequestPermission.setOnClickListener {
                    requestPermissions(arrayOf(permission), PERMISSION_REQUEST_CODE)
                }
            }
            else -> {
                requestPermissions(arrayOf(permission), PERMISSION_REQUEST_CODE)
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                viewModel.loadVideos()
                binding.btnRequestPermission.visibility = View.GONE
                binding.tvError.visibility = View.GONE
            } else {
                binding.tvError.text = "Permission denied. Cannot access videos."
                binding.tvError.visibility = View.VISIBLE
            }
        }
    }

    private fun navigateToEditor(video: VideoItem) {
        // Implement navigation to editor fragment
        // You can use interface callback or navigation component
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val PERMISSION_REQUEST_CODE = 100
    }
}