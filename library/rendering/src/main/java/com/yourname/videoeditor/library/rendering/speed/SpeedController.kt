package com.yourname.videoeditor.library.rendering.speed

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import java.io.File
import java.nio.ByteBuffer

enum class SpeedPreset {
    SLOWEST,    // 0.25x
    SLOWER,     // 0.5x
    SLOW,       // 0.75x
    NORMAL,     // 1.0x
    FAST,       // 1.5x
    FASTER,     // 2.0x
    FASTEST,    // 4.0x
    CUSTOM
}

data class SpeedConfig(
    val speedFactor: Float = 1.0f,
    val preservePitch: Boolean = true,
    val frameBlending: Boolean = true,
    val preset: SpeedPreset = SpeedPreset.NORMAL
) {
    val effectiveSpeed: Float
        get() = when (preset) {
            SpeedPreset.SLOWEST -> 0.25f
            SpeedPreset.SLOWER -> 0.5f
            SpeedPreset.SLOW -> 0.75f
            SpeedPreset.NORMAL -> 1.0f
            SpeedPreset.FAST -> 1.5f
            SpeedPreset.FASTER -> 2.0f
            SpeedPreset.FASTEST -> 4.0f
            SpeedPreset.CUSTOM -> speedFactor
        }
}

class SpeedController {

    interface SpeedCallback {
        fun onProgress(progress: Float)
        fun onComplete(outputPath: String)
        fun onError(error: String)
    }

    fun changeSpeed(
        inputPath: String,
        outputPath: String,
        config: SpeedConfig,
        callback: SpeedCallback
    ) {
        Thread {
            try {
                val extractor = MediaExtractor()
                extractor.setDataSource(inputPath)
                
                val videoTrackIndex = findTrackIndex(extractor, "video/")
                val audioTrackIndex = findTrackIndex(extractor, "audio/")
                
                val muxer = MediaMuxer(outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
                
                // Process video track
                if (videoTrackIndex >= 0) {
                    processVideoTrack(extractor, muxer, videoTrackIndex, config, callback)
                }
                
                // Process audio track
                if (audioTrackIndex >= 0) {
                    processAudioTrack(extractor, muxer, audioTrackIndex, config, callback)
                }
                
                muxer.stop()
                muxer.release()
                extractor.release()
                
                callback.onComplete(outputPath)
            } catch (e: Exception) {
                callback.onError(e.message ?: "Speed change failed")
            }
        }.start()
    }

    private fun findTrackIndex(extractor: MediaExtractor, mimePrefix: String): Int {
        for (i in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(i)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
            if (mime.startsWith(mimePrefix)) {
                return i
            }
        }
        return -1
    }

    private fun processVideoTrack(
        extractor: MediaExtractor,
        muxer: MediaMuxer,
        trackIndex: Int,
        config: SpeedConfig,
        callback: SpeedCallback
    ) {
        extractor.selectTrack(trackIndex)
        val inputFormat = extractor.getTrackFormat(trackIndex)
        
        // Create video decoder
        val decoder = MediaCodec.createDecoderByType(inputFormat.getString(MediaFormat.KEY_MIME) ?: "video/avc")
        decoder.configure(inputFormat, null, null, 0)
        decoder.start()
        
        // Create video encoder
        val outputFormat = createOutputFormat(inputFormat, config)
        val encoder = MediaCodec.createEncoderByType(outputFormat.getString(MediaFormat.KEY_MIME) ?: "video/avc")
        encoder.configure(outputFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        encoder.start()
        
        val muxerTrackIndex = muxer.addTrack(outputFormat)
        
        val bufferInfo = MediaCodec.BufferInfo()
        val inputBuffers = decoder.inputBuffers
        val outputBuffers = decoder.outputBuffers
        val encoderInputBuffers = encoder.inputBuffers
        val encoderOutputBuffers = encoder.outputBuffers
        
        var isEOS = false
        var frameCount = 0
        
        while (!isEOS) {
            // Feed decoder
            val inputBufferIndex = decoder.dequeueInputBuffer(10000)
            if (inputBufferIndex >= 0) {
                val inputBuffer = inputBuffers[inputBufferIndex]
                val sampleSize = extractor.readSampleData(inputBuffer, 0)
                
                if (sampleSize < 0) {
                    decoder.queueInputBuffer(inputBufferIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                    isEOS = true
                } else {
                    val presentationTime = extractor.sampleTime
                    decoder.queueInputBuffer(inputBufferIndex, 0, sampleSize, presentationTime, extractor.sampleFlags)
                    extractor.advance()
                }
            }
            
            // Get decoder output
            val outputBufferIndex = decoder.dequeueOutputBuffer(bufferInfo, 10000)
            if (outputBufferIndex >= 0) {
                val outputBuffer = outputBuffers[outputBufferIndex]
                
                // Apply speed change by adjusting presentation time
                val adjustedTime = (bufferInfo.presentationTimeUs / config.effectiveSpeed).toLong()
                
                // Feed to encoder
                val encoderInputIndex = encoder.dequeueInputBuffer(10000)
                if (encoderInputIndex >= 0) {
                    val encoderInputBuffer = encoderInputBuffers[encoderInputIndex]
                    encoderInputBuffer.clear()
                    encoderInputBuffer.put(outputBuffer)
                    encoder.queueInputBuffer(encoderInputIndex, 0, bufferInfo.size, adjustedTime, bufferInfo.flags)
                }
                
                decoder.releaseOutputBuffer(outputBufferIndex, false)
                
                // Get encoder output and write to muxer
                val encoderOutputIndex = encoder.dequeueOutputBuffer(bufferInfo, 10000)
                if (encoderOutputIndex >= 0) {
                    val encoderOutputBuffer = encoderOutputBuffers[encoderOutputIndex]
                    muxer.writeSampleData(muxerTrackIndex, encoderOutputBuffer, bufferInfo)
                    encoder.releaseOutputBuffer(encoderOutputIndex, false)
                    
                    frameCount++
                    if (frameCount % 30 == 0) {
                        callback.onProgress(50f) // Simplified progress
                    }
                }
            }
        }
        
        decoder.stop()
        decoder.release()
        encoder.stop()
        encoder.release()
        extractor.unselectTrack(trackIndex)
    }

    private fun processAudioTrack(
        extractor: MediaExtractor,
        muxer: MediaMuxer,
        trackIndex: Int,
        config: SpeedConfig,
        callback: SpeedCallback
    ) {
        extractor.selectTrack(trackIndex)
        val inputFormat = extractor.getTrackFormat(trackIndex)
        
        // For audio, we use a simpler approach if preserving pitch
        if (config.preservePitch && config.effectiveSpeed != 1.0f) {
            // Use Sonic library or similar for pitch-preserving speed change
            processAudioWithPreservedPitch(extractor, muxer, trackIndex, config, callback)
        } else {
            // Simple time stretching
            processAudioSimple(extractor, muxer, trackIndex, config, callback)
        }
    }

    private fun processAudioSimple(
        extractor: MediaExtractor,
        muxer: MediaMuxer,
        trackIndex: Int,
        config: SpeedConfig,
        callback: SpeedCallback
    ) {
        val format = extractor.getTrackFormat(trackIndex)
        val muxerTrackIndex = muxer.addTrack(format)
        
        val buffer = ByteBuffer.allocate(1024 * 1024)
        val info = MediaCodec.BufferInfo()
        
        while (true) {
            info.size = extractor.readSampleData(buffer, 0)
            if (info.size < 0) break
            
            info.presentationTimeUs = (extractor.sampleTime / config.effectiveSpeed).toLong()
            info.flags = extractor.sampleFlags
            
            muxer.writeSampleData(muxerTrackIndex, buffer, info)
            extractor.advance()
        }
        
        extractor.unselectTrack(trackIndex)
    }

    private fun processAudioWithPreservedPitch(
        extractor: MediaExtractor,
        muxer: MediaMuxer,
        trackIndex: Int,
        config: SpeedConfig,
        callback: SpeedCallback
    ) {
        // This would use a library like Sonic or SoundTouch
        // For now, fall back to simple processing
        processAudioSimple(extractor, muxer, trackIndex, config, callback)
    }

    private fun createOutputFormat(inputFormat: MediaFormat, config: SpeedConfig): MediaFormat {
        val mime = inputFormat.getString(MediaFormat.KEY_MIME) ?: "video/avc"
        val width = inputFormat.getInteger(MediaFormat.KEY_WIDTH)
        val height = inputFormat.getInteger(MediaFormat.KEY_HEIGHT)
        val bitRate = inputFormat.getInteger(MediaFormat.KEY_BIT_RATE)
        val frameRate = inputFormat.getInteger(MediaFormat.KEY_FRAME_RATE)
        
        val outputFormat = MediaFormat.createVideoFormat(mime, width, height)
        outputFormat.setInteger(MediaFormat.KEY_BIT_RATE, bitRate)
        outputFormat.setInteger(MediaFormat.KEY_FRAME_RATE, (frameRate * config.effectiveSpeed).toInt())
        outputFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
        outputFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            outputFormat.setFeatureEnabled(MediaCodecInfo.CodecCapabilities.FEATURE_Encoding, true)
        }
        
        return outputFormat
    }
}