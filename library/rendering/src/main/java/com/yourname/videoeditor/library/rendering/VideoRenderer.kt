package com.yourname.videoeditor.library.rendering

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.TextureView
import java.io.IOException

class VideoRenderer(private val context: Context) {
    private var mediaPlayer: MediaPlayer? = null
    private var currentSurface: Surface? = null
    var isPlaying: Boolean = false
        private set
    var currentPosition: Int = 0
        private set
    var duration: Int = 0
        private set
    
    interface RendererCallback {
        fun onPrepared()
        fun onError(error: String)
        fun onCompletion()
        fun onVideoSizeChanged(width: Int, height: Int)
    }
    
    private var callback: RendererCallback? = null
    
    fun setCallback(callback: RendererCallback) {
        this.callback = callback
    }
    
    fun prepareVideo(uri: Uri, surfaceView: SurfaceView) {
        surfaceView.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                prepareMediaPlayer(uri, holder.surface)
            }
            
            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}
            
            override fun surfaceDestroyed(holder: SurfaceHolder) {
                release()
            }
        })
    }
    
    fun prepareVideo(uri: Uri, textureView: TextureView) {
        textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(surface: android.graphics.SurfaceTexture, width: Int, height: Int) {
                prepareMediaPlayer(uri, Surface(surface))
            }
            
            override fun onSurfaceTextureSizeChanged(surface: android.graphics.SurfaceTexture, width: Int, height: Int) {}
            
            override fun onSurfaceTextureDestroyed(surface: android.graphics.SurfaceTexture): Boolean {
                release()
                return true
            }
            
            override fun onSurfaceTextureUpdated(surface: android.graphics.SurfaceTexture) {}
        }
    }
    
    private fun prepareMediaPlayer(uri: Uri, surface: Surface) {
        try {
            release()
            
            this.currentSurface = surface
            mediaPlayer = MediaPlayer().apply {
                setDataSource(context, uri)
                setSurface(surface)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                
                setOnPreparedListener { mp ->
                    duration = mp.duration
                    callback?.onPrepared()
                    callback?.onVideoSizeChanged(mp.videoWidth, mp.videoHeight)
                }
                
                setOnErrorListener { _, what, extra ->
                    callback?.onError("MediaPlayer error: $what, $extra")
                    true
                }
                
                setOnCompletionListener {
                    isPlaying = false
                    callback?.onCompletion()
                }
                
                setOnVideoSizeChangedListener { _, width, height ->
                    callback?.onVideoSizeChanged(width, height)
                }
                
                prepareAsync()
            }
        } catch (e: IOException) {
            callback?.onError("Failed to prepare video: ${e.message}")
        }
    }
    
    fun play() {
        mediaPlayer?.start()
        isPlaying = true
    }
    
    fun pause() {
        mediaPlayer?.pause()
        isPlaying = false
    }
    
    fun seekTo(positionMs: Int) {
        mediaPlayer?.seekTo(positionMs)
        currentPosition = positionMs
    }
    
    fun release() {
        mediaPlayer?.release()
        mediaPlayer = null
        currentSurface?.release()
        currentSurface = null
        isPlaying = false
        currentPosition = 0
    }
    
    fun setVolume(volume: Float) {
        mediaPlayer?.setVolume(volume, volume)
    }
    
    fun isPlaying(): Boolean = isPlaying
    
    fun getCurrentPosition(): Int = mediaPlayer?.currentPosition ?: 0
    
    fun getDuration(): Int = mediaPlayer?.duration ?: 0
}