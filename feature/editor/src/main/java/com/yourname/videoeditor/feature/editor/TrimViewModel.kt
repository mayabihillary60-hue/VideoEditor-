package com.yourname.videoeditor.feature.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourname.videoeditor.feature.editor.model.TrimRange
import com.yourname.videoeditor.library.rendering.VideoProcessor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File

class TrimViewModel : ViewModel() {

    private val _trimRange = MutableStateFlow(TrimRange(0L, 0L))
    val trimRange: StateFlow<TrimRange> = _trimRange

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing

    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress

    fun setTrimRange(startMs: Long, endMs: Long) {
        _trimRange.value = TrimRange(startMs, endMs)
    }

    fun trimVideo(
        videoProcessor: VideoProcessor,
        inputPath: String,
        outputPath: String,
        onComplete: (String) -> Unit
    ) {
        viewModelScope.launch {
            _isProcessing.value = true
            _progress.value = 0f

            videoProcessor.trimVideo(
                inputUri = android.net.Uri.parse(inputPath),
                outputPath = outputPath,
                startTimeMs = _trimRange.value.startMs,
                endTimeMs = _trimRange.value.endMs,
                callback = object : VideoProcessor.ProcessingCallback {
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

    fun splitVideo(inputPath: String, splitPointMs: Long): Pair<String, String> {
        val dir = File(inputPath).parent
        val name = File(inputPath).nameWithoutExtension
        
        val part1Path = "$dir/${name}_part1.mp4"
        val part2Path = "$dir/${name}_part2.mp4"
        
        // We'll implement the actual splitting logic
        return Pair(part1Path, part2Path)
    }
}