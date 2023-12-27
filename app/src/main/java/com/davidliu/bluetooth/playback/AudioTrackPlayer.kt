package com.davidliu.bluetooth.playback

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.util.Log


class AudioTrackPlayer(val context: Context) {

    var audioTrack: AudioTrack? = null
    fun initialize() {

        if (audioTrack != null) {
            return
        }

        val channelConfig = channelCountToConfiguration(1)
        val sampleRate = 44100

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build()
        val minBufferSizeInBytes = AudioTrack.getMinBufferSize(
            sampleRate,
            channelConfig,
            AudioFormat.ENCODING_PCM_16BIT
        )
        audioTrack = AudioTrack(
            audioAttributes,
            AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(sampleRate)
                .setChannelMask(channelConfig)
                .build(),
            minBufferSizeInBytes,
            AudioTrack.MODE_STREAM,
            AudioManager.AUDIO_SESSION_ID_GENERATE
        )
    }

    fun start() {

        if (audioTrack == null) {
            return
        }

        audioTrack?.play()
        Log.e("LOL", "playstate: ${audioTrack?.playState}")

    }

    fun stop() {
        audioTrack?.stop()
        audioTrack?.release()
        audioTrack = null
    }

    fun writeBytes(bytes: ByteArray) {
        audioTrack?.write(bytes, 0, bytes.size)
    }

    private fun channelCountToConfiguration(channels: Int): Int {
        return if (channels == 1) AudioFormat.CHANNEL_OUT_MONO else AudioFormat.CHANNEL_OUT_STEREO
    }
}