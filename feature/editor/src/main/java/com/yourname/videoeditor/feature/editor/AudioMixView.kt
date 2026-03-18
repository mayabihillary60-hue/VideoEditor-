package com.yourname.videoeditor.feature.editor

import android.content.Context
import android.graphics.*
import android.net.Uri
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import com.google.android.material.button.MaterialButton
import com.google.android.material.slider.Slider
import com.yourname.videoeditor.feature.editor.R

class AudioMixView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val waveformPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    
    private var audioTracks = mutableListOf<AudioTrackInfo>()
    
    data class AudioTrackInfo(
        val id: String,
        val name: String,
        val uri: Uri,
        var volume: Float = 1.0f,
        var isMuted: Boolean = false,
        var isSolo: Boolean = false,
        var color: Int = Color.parseColor("#FF4081")
    )
    
    interface AudioMixListener {
        fun onVolumeChanged(trackId: String, volume: Float)
        fun onMuteToggled(trackId: String, muted: Boolean)
        fun onSoloToggled(trackId: String, soloed: Boolean)
        fun onAddTrackClicked()
        fun onRemoveTrackClicked(trackId: String)
    }
    
    private var listener: AudioMixListener? = null
    
    init {
        orientation = VERTICAL
        setBackgroundColor(Color.parseColor("#1E1E1E"))
        
        textPaint.color = Color.WHITE
        textPaint.textSize = 40f
        textPaint.textAlign = Paint.Align.CENTER
    }
    
    fun setAudioTracks(tracks: List<AudioTrackInfo>) {
        this.audioTracks.clear()
        this.audioTracks.addAll(tracks)
        removeAllViews()
        
        // Add header
        addView(createHeaderView())
        
        // Add track controls
        tracks.forEachIndexed { index, track ->
            addView(createTrackView(track, index))
        }
        
        // Add button to add more tracks
        addView(createAddTrackButton())
        
        invalidate()
    }
    
    private fun createHeaderView(): LinearLayout {
        return LinearLayout(context).apply {
            orientation = HORIZONTAL
            setPadding(16, 16, 16, 16)
            
            // Track name label
            addView(TextView(context).apply {
                text = "Audio Track"
                setTextColor(Color.WHITE)
                textSize = 16f
                layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 2f)
            })
            
            // Volume label
            addView(TextView(context).apply {
                text = "Volume"
                setTextColor(Color.WHITE)
                textSize = 16f
                textAlignment = TEXT_ALIGNMENT_CENTER
                layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
            })
            
            // Controls label
            addView(TextView(context).apply {
                text = "Controls"
                setTextColor(Color.WHITE)
                textSize = 16f
                textAlignment = TEXT_ALIGNMENT_CENTER
                layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
            })
        }
    }
    
    private fun createTrackView(track: AudioTrackInfo, index: Int): LinearLayout {
        return LinearLayout(context).apply {
            orientation = HORIZONTAL
            setPadding(16, 8, 16, 8)
            setBackgroundColor(if (index % 2 == 0) Color.parseColor("#2A2A2A") else Color.parseColor("#1E1E1E"))
            
            // Track info with color indicator
            val trackInfoLayout = LinearLayout(context).apply {
                orientation = HORIZONTAL
                layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 2f)
                
                // Color indicator
                addView(createColorIndicator(track.color))
                
                // Track name
                addView(TextView(context).apply {
                    text = track.name
                    setTextColor(Color.WHITE)
                    textSize = 14f
                    layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                        marginStart = 8
                    }
                })
            }
            addView(trackInfoLayout)
            
            // Volume slider
            val volumeLayout = LinearLayout(context).apply {
                orientation = HORIZONTAL
                layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
                
                // Volume slider
                addView(SeekBar(context).apply {
                    max = 200
                    progress = (track.volume * 100).toInt()
                    layoutParams = LayoutParams(200, LayoutParams.WRAP_CONTENT)
                    
                    setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                            if (fromUser) {
                                val volume = progress / 100f
                                listener?.onVolumeChanged(track.id, volume)
                            }
                        }
                        override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                        override fun onStopTrackingTouch(seekBar: SeekBar?) {}
                    })
                })
                
                // Volume percentage
                addView(TextView(context).apply {
                    text = "${(track.volume * 100).toInt()}%"
                    setTextColor(Color.WHITE)
                    textSize = 12f
                    layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                        marginStart = 8
                    }
                })
            }
            addView(volumeLayout)
            
            // Control buttons
            val controlLayout = LinearLayout(context).apply {
                orientation = HORIZONTAL
                layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
                gravity = android.view.Gravity.CENTER
                
                // Mute button
                addView(MaterialButton(context).apply {
                    text = "M"
                    setTextColor(if (track.isMuted) Color.RED else Color.WHITE)
                    setOnClickListener {
                        track.isMuted = !track.isMuted
                        listener?.onMuteToggled(track.id, track.isMuted)
                        this.setTextColor(if (track.isMuted) Color.RED else Color.WHITE)
                    }
                })
                
                // Solo button
                addView(MaterialButton(context).apply {
                    text = "S"
                    setTextColor(if (track.isSolo) Color.GREEN else Color.WHITE)
                    layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                        marginStart = 8
                    }
                    setOnClickListener {
                        track.isSolo = !track.isSolo
                        listener?.onSoloToggled(track.id, track.isSolo)
                        this.setTextColor(if (track.isSolo) Color.GREEN else Color.WHITE)
                    }
                })
            }
            addView(controlLayout)
        }
    }
    
    private fun createColorIndicator(color: Int): View {
        return object : View(context) {
            override fun onDraw(canvas: Canvas) {
                super.onDraw(canvas)
                paint.color = color
                canvas.drawCircle(12f, 12f, 8f, paint)
            }
        }.apply {
            layoutParams = LayoutParams(24, 24)
        }
    }
    
    private fun createAddTrackButton(): MaterialButton {
        return MaterialButton(context).apply {
            text = "Add Background Music"
            setOnClickListener {
                listener?.onAddTrackClicked()
            }
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                setMargins(16, 16, 16, 16)
            }
        }
    }
    
    fun setListener(listener: AudioMixListener) {
        this.listener = listener
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        // Draw waveform visualization if we have tracks
        if (audioTracks.isNotEmpty()) {
            drawWaveform(canvas)
        }
    }
    
    private fun drawWaveform(canvas: Canvas) {
        val width = width.toFloat()
        val height = 200f
        val y = height - 50f
        
        waveformPaint.color = Color.parseColor("#FF4081")
        waveformPaint.strokeWidth = 2f
        
        // Draw simple waveform (placeholder)
        for (i in 0 until 100) {
            val x1 = (i * width / 100)
            val x2 = ((i + 1) * width / 100)
            val amplitude = (Math.sin(i * 0.1) * 50).toFloat()
            
            canvas.drawLine(x1, y - amplitude, x2, y + amplitude, waveformPaint)
        }
    }
}