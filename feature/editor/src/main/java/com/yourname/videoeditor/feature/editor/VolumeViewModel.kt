package com.yourname.videoeditor.feature.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourname.videoeditor.core.domain.model.VideoClip
import com.yourname.videoeditor.library.rendering.VolumeController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class VolumeViewModel : ViewModel() {

    private val volumeController = VolumeController()
    
    private val _clips = MutableStateFlow<List<VideoClip>>(emptyList())
    val clips: StateFlow<List<VideoClip>> = _clips
    
    private val _selectedClipIndex = MutableStateFlow(0)
    val selectedClipIndex: StateFlow<Int> = _selectedClipIndex
    
    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing
    
    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress
    
    fun setClips(clips: List<VideoClip>) {
        _clips.value = clips
    }
    
    fun selectClip(index: Int) {
        if (index in _clips.value.indices) {
            _selectedClipIndex.value = index
        }
    }
    
    fun updateClipVolume(clipIndex: Int, volume: Float) {
        val currentClips = _clips.value.toMutableList()
        if (clipIndex in currentClips.indices) {
            val updatedClip = currentClips[clipIndex].copy(volume = volume.coerceIn(0f, 2f))
            currentClips[clipIndex] = updatedClip
            _clips.value = currentClips
        }
    }
    
    fun adjustVolumeForClip(
        clip: VideoClip,
        inputPath: String,
        outputPath: String,
        volumeFactor: Float,
        onComplete: (String) -> Unit
    ) {
        viewModelScope.launch {
            _isProcessing.value = true
            _progress.value = 0f
            
            volumeController.adjustVolume(
                inputPath = inputPath,
                outputPath = outputPath,
                volumeFactor = volumeFactor,
                callback = object : VolumeController.VolumeProcessingCallback {
                    override fun onProgress(progress: Float) {
                        _progress.value = progress
                    }
                    
                    override fun onComplete(outputPath: String) {
                        _isProcessing.value = false
                        onComplete(outputPath)
                    }
                    
                    override fun onError(error: String) {
                        _isProcessing.value = false
                        // Handle error
                    }
                }
            )
        }
    }
    
    fun getVolumeLevels(): FloatArray {
        return volumeController.getVolumeLevels(android.media.AudioTrack.Builder().build())
    }
    
    fun muteClip(clipIndex: Int, mute: Boolean) {
        val currentClips = _clips.value.toMutableList()
        if (clipIndex in currentClips.indices) {
            val updatedClip = currentClips[clipIndex].copy(volume = if (mute) 0f else 1f)
            currentClips[clipIndex] = updatedClip
            _clips.value = currentClips
        }
    }
}