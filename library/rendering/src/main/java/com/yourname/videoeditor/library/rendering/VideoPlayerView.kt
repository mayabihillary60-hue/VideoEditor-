package com.yourname.videoeditor.library.rendering

import android.content.Context
import android.net.Uri
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.TextView
import androidx.core.view.isVisible

class VideoPlayerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    
    private var videoRenderer: VideoRenderer
    private var surfaceView: android.view.SurfaceView
    private var playPauseButton: ImageButton
    private var seekBar: SeekBar
    private var currentTimeText: TextView
    private var durationText: TextView
    private var controlsLayout: View
    
    private var isControlsVisible = true
    private val controlsHideRunnable = Runnable { hideControls() }
    
    init {
        LayoutInflater.from(context).inflate(R.layout.view_video_player, this, true)
        
        surfaceView = findViewById(R.id.surface_view)
        playPauseButton = findViewById(R.id.btn_play_pause)
        seekBar = findViewById(R.id.seek_bar)
        currentTimeText = findViewById(R.id.tv_current_time)
        durationText = findViewById(R.id.tv_duration)
        controlsLayout = findViewById(R.id.controls_layout)
        
        videoRenderer = VideoRenderer(context)
        videoRenderer.setCallback(object : VideoRenderer.RendererCallback {
            override fun onPrepared() {
                durationText.text = formatTime(videoRenderer.duration)
                seekBar.max = videoRenderer.duration
                playPauseButton.isEnabled = true
            }
            
            override fun onError(error: String) {
                // Handle error
            }
            
            override fun onCompletion() {
                playPauseButton.setImageResource(android.R.drawable.ic_media_play)
                seekBar.progress = 0
            }
            
            override fun onVideoSizeChanged(width: Int, height: Int) {
                // Handle video size change if needed
            }
        })
        
        setupListeners()
        startUpdatingProgress()
    }
    
    private fun setupListeners() {
        setOnClickListener {
            toggleControls()
        }
        
        playPauseButton.setOnClickListener {
            togglePlayPause()
        }
        
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    videoRenderer.seekTo(progress)
                    currentTimeText.text = formatTime(progress)
                }
            }
            
            override fun onStartTrackingTouch(seekBar: SeekBar) {
                removeCallbacks(controlsHideRunnable)
            }
            
            override fun onStopTrackingTouch(seekBar: SeekBar) {
                postDelayed(controlsHideRunnable, 3000)
            }
        })
    }
    
    private fun startUpdatingProgress() {
        post(object : Runnable {
            override fun run() {
                if (videoRenderer.isPlaying) {
                    val position = videoRenderer.getCurrentPosition()
                    seekBar.progress = position
                    currentTimeText.text = formatTime(position)
                }
                postDelayed(this, 100)
            }
        })
    }
    
    private fun togglePlayPause() {
        if (videoRenderer.isPlaying) {
            videoRenderer.pause()
            playPauseButton.setImageResource(android.R.drawable.ic_media_play)
        } else {
            videoRenderer.play()
            playPauseButton.setImageResource(android.R.drawable.ic_media_pause)
        }
    }
    
    private fun toggleControls() {
        if (isControlsVisible) {
            hideControls()
        } else {
            showControls()
        }
    }
    
    private fun showControls() {
        controlsLayout.isVisible = true
        isControlsVisible = true
        postDelayed(controlsHideRunnable, 3000)
    }
    
    private fun hideControls() {
        controlsLayout.isVisible = false
        isControlsVisible = false
    }
    
    fun setVideoUri(uri: Uri) {
        videoRenderer.prepareVideo(uri, surfaceView)
    }
    
    fun release() {
        removeCallbacks(controlsHideRunnable)
        videoRenderer.release()
    }
    
    private fun formatTime(millis: Int): String {
        val seconds = (millis / 1000) % 60
        val minutes = (millis / (1000 * 60)) % 60
        val hours = (millis / (1000 * 60 * 60))
        
        return if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }
}