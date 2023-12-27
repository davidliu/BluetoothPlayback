package com.davidliu.bluetooth.playback

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.os.Process
import android.util.Log
import java.nio.ByteBuffer
import java.util.Arrays
import kotlin.math.max


class AudioRecorder(val onSamplesReady: (ByteArray) -> Unit) {

    private var audioRecord: AudioRecord? = null
    private var audioThread: AudioThread? = null
    lateinit var byteBuffer: ByteBuffer

    fun start() {
        initRecord()
        startRecording()
    }

    fun stop() {
        audioThread?.stopThread()
    }

    @SuppressLint("MissingPermission")
    private fun initRecord() {
        if (audioRecord != null) {
            return
        }
        val sampleRate = 44100
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        val bytesPerFrame = 1 * getBytesPerSample(audioFormat)
        val framesPerBuffer: Int = sampleRate / 100
        byteBuffer = ByteBuffer.allocateDirect(bytesPerFrame * framesPerBuffer)
        if (!byteBuffer.hasArray()) {
            return
        }

        val emptyBytes = ByteArray(byteBuffer.capacity())
        val channelConfig = channelCountToConfiguration(1)
        val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
        if (minBufferSize < 0) {
            return
        }

        val bufferSizeInBytes = max(2 * minBufferSize, byteBuffer.capacity())
        try {
//            audioRecord = AudioRecord(
//                MediaRecorder.AudioSource.DEFAULT,
//                AudioFormat.SAMPLE_RATE_UNSPECIFIED,
//                channelConfig,
//                audioFormat,
//                bufferSizeInBytes
//            )

            audioRecord = AudioRecord.Builder()
                .setAudioSource(MediaRecorder.AudioSource.VOICE_COMMUNICATION)
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(audioFormat)
                        .setSampleRate(sampleRate)
                        .setChannelMask(channelConfig)
                        .build()
                )
                .setBufferSizeInBytes(bufferSizeInBytes)
                .build()
        } catch (e: Exception) {
            Log.e("AudioRecorder", "Exception", e)
        }
    }

    private fun log(message: String, e: Exception? = null) {

        if (e != null) {
            Log.e("AudioRecorder", message, e)
        } else {
            Log.e("AudioRecorder", message)
        }
    }

    private fun startRecording() {
        if (audioRecord == null || audioThread != null) {
            return
        }

        try {
            audioRecord?.startRecording()
        } catch (e: Exception) {
            log("StartRecording", e)
        }

        if ((audioRecord?.recordingState ?: -1) != AudioRecord.RECORDSTATE_RECORDING) {
            log("not recording!")
            return
        }

        audioThread = AudioThread("AudioRecordThread")
        audioThread?.start()
    }

    private fun channelCountToConfiguration(channels: Int): Int {
        return if (channels == 1) AudioFormat.CHANNEL_IN_MONO else AudioFormat.CHANNEL_IN_STEREO
    }

    // Reference from Android code, AudioFormat.getBytesPerSample. BitPerSample / 8
    // Default audio data format is PCM 16 bits per sample.
    // Guaranteed to be supported by all devices
    private fun getBytesPerSample(audioFormat: Int): Int {
        return when (audioFormat) {
            AudioFormat.ENCODING_PCM_8BIT -> 1
            AudioFormat.ENCODING_PCM_16BIT, AudioFormat.ENCODING_IEC61937, AudioFormat.ENCODING_DEFAULT -> 2
            AudioFormat.ENCODING_PCM_FLOAT -> 4
            AudioFormat.ENCODING_INVALID -> throw IllegalArgumentException("Bad audio format $audioFormat")
            else -> throw IllegalArgumentException("Bad audio format $audioFormat")
        }
    }

    private inner class AudioThread(name: String) : Thread(name) {
        @Volatile
        private var keepAlive = true

        override fun run() {
            Process.setThreadPriority(-19)

            while (keepAlive) {
                val bytesRead = audioRecord?.read(byteBuffer, byteBuffer.capacity())
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
//                    audioRecord?.activeRecordingConfiguration?.let { config ->
//                        Log.e("LOL", "${config.audioDevice.type}")
//                        Log.e("LOL", config.clientAudioSource.toString())
//                    }
                }
                if (bytesRead == byteBuffer.capacity()) {
                    if (!keepAlive) {
                        break
                    }

                    val data =
                        Arrays.copyOfRange(byteBuffer.array(), byteBuffer.arrayOffset(), byteBuffer.capacity() + byteBuffer.arrayOffset())

                    onSamplesReady(data)
                } else {
                    if (bytesRead == AudioRecord.ERROR_INVALID_OPERATION) {
                        keepAlive = false
                        log("bad operation, stop recording")
                    }
                }
            }

            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null
            audioThread = null
        }

        fun stopThread() {
            keepAlive = false
        }
    }
}