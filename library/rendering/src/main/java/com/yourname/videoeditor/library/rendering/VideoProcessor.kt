package com.yourname.videoeditor.library.rendering

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import java.io.IOException
import java.nio.ByteBuffer

class VideoProcessor(private val context: Context) {

    interface ProcessingCallback {
        fun onProgress(progress: Float)
        fun onComplete(outputPath: String)
        fun onError(error: String)
    }

    fun trimVideo(
        inputUri: Uri,
        outputPath: String,
        startTimeMs: Long,
        endTimeMs: Long,
        callback: ProcessingCallback
    ) {
        // This will use MediaCodec for precise trimming
        // For now, we'll implement a basic version
        Thread {
            try {
                val extractor = MediaExtractor()
                extractor.setDataSource(context, inputUri, null)
                
                val videoTrackIndex = findVideoTrackIndex(extractor)
                val audioTrackIndex = findAudioTrackIndex(extractor)
                
                val muxer = MediaMuxer(outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
                
                // Process video track
                if (videoTrackIndex >= 0) {
                    processTrack(extractor, muxer, videoTrackIndex, startTimeMs, endTimeMs, callback)
                }
                
                // Process audio track
                if (audioTrackIndex >= 0) {
                    processTrack(extractor, muxer, audioTrackIndex, startTimeMs, endTimeMs, callback)
                }
                
                muxer.stop()
                muxer.release()
                extractor.release()
                
                callback.onComplete(outputPath)
            } catch (e: Exception) {
                callback.onError(e.message ?: "Unknown error")
            }
        }.start()
    }

    private fun findVideoTrackIndex(extractor: MediaExtractor): Int {
        for (i in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(i)
            val mime = format.getString(MediaFormat.KEY_MIME)
            if (mime?.startsWith("video/") == true) {
                return i
            }
        }
        return -1
    }

    private fun findAudioTrackIndex(extractor: MediaExtractor): Int {
        for (i in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(i)
            val mime = format.getString(MediaFormat.KEY_MIME)
            if (mime?.startsWith("audio/") == true) {
                return i
            }
        }
        return -1
    }

    private fun processTrack(
        extractor: MediaExtractor,
        muxer: MediaMuxer,
        trackIndex: Int,
        startTimeUs: Long,
        endTimeUs: Long,
        callback: ProcessingCallback
    ) {
        extractor.selectTrack(trackIndex)
        val format = extractor.getTrackFormat(trackIndex)
        val muxerTrackIndex = muxer.addTrack(format)
        
        val startTimeUs = startTimeMs * 1000
        val endTimeUs = endTimeMs * 1000
        
        extractor.seekTo(startTimeUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
        
        val buffer = ByteBuffer.allocate(1024 * 1024) // 1MB buffer
        val info = MediaCodec.BufferInfo()
        
        while (true) {
            info.size = extractor.readSampleData(buffer, 0)
            if (info.size < 0) break
            
            val sampleTime = extractor.sampleTime
            if (sampleTime > endTimeUs) break
            
            if (sampleTime >= startTimeUs) {
                info.presentationTimeUs = sampleTime - startTimeUs
                info.flags = extractor.sampleFlags
                
                muxer.writeSampleData(muxerTrackIndex, buffer, info)
                
                val progress = ((sampleTime - startTimeUs) / (endTimeUs - startTimeUs) * 100).toFloat()
                callback.onProgress(progress)
            }
            
            extractor.advance()
        }
        
        extractor.unselectTrack(trackIndex)
    }
}