package com.yourname.videoeditor.feature.gallery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourname.videoeditor.feature.gallery.data.GalleryRepository
import com.yourname.videoeditor.feature.gallery.model.VideoItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class GalleryViewModel(
    private val repository: GalleryRepository
) : ViewModel() {

    private val _videos = MutableStateFlow<List<VideoItem>>(emptyList())
    val videos: StateFlow<List<VideoItem>> = _videos

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _selectedVideos = MutableStateFlow<List<VideoItem>>(emptyList())
    val selectedVideos: StateFlow<List<VideoItem>> = _selectedVideos

    fun loadVideos() {
        viewModelScope.launch {
            _isLoading.value = true
            repository.getVideos()
                .catch { exception ->
                    _error.value = exception.message
                    _isLoading.value = false
                }
                .collect { videoList ->
                    _videos.value = videoList
                    _isLoading.value = false
                }
        }
    }

    fun toggleVideoSelection(video: VideoItem) {
        val currentSelection = _selectedVideos.value.toMutableList()
        if (currentSelection.contains(video)) {
            currentSelection.remove(video)
        } else {
            currentSelection.add(video)
        }
        _selectedVideos.value = currentSelection
    }

    fun clearSelection() {
        _selectedVideos.value = emptyList()
    }

    fun isSelected(video: VideoItem): Boolean {
        return _selectedVideos.value.contains(video)
    }
}