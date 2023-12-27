package com.davidliu.bluetooth.playback

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.davidliu.bluetooth.playback.ui.theme.BluetoothPlaybackTheme
import kotlin.experimental.and
import kotlin.math.round
import kotlin.math.sqrt

class MainActivity : ComponentActivity() {

    lateinit var audioRecorder: AudioRecorder
    lateinit var audioTrack: AudioTrackPlayer

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val audioManager = getSystemService(AudioManager::class.java)
        val playbackAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build()
        val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(playbackAttributes)
            .setAcceptsDelayedFocusGain(true)
            .setOnAudioFocusChangeListener {}
            .build()
        audioManager.requestAudioFocus(focusRequest)
        audioManager.isSpeakerphoneOn = false
        audioManager.isMicrophoneMute = false
        audioManager.mode = AudioManager.MODE_NORMAL


        startAudio()
        registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val state = intent.getIntExtra(AudioManager.EXTRA_SCO_AUDIO_STATE, AudioManager.SCO_AUDIO_STATE_DISCONNECTED)

                Log.e("LOL", "onReceive, state = $state")
                if (state == AudioManager.SCO_AUDIO_STATE_CONNECTED) {
                }

            }

        }, IntentFilter(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val devices = audioManager.availableCommunicationDevices
            devices.forEach {
                Log.e("LOL", it.toString())
            }
            val bluetooth = devices.firstOrNull { it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO }

            if (bluetooth != null) {
                Log.e("LOL", "setting: $bluetooth")
                audioManager.setCommunicationDevice(bluetooth)
            }
        } else {
            audioManager.startBluetoothSco()
        }
        setContent {
            BluetoothPlaybackTheme {
                // A surface container using the 'background' color from the theme
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Greeting("Android")
                }
            }
        }
    }

    fun startAudio() {

        Log.e("LOL", "starting audio")
        audioTrack = AudioTrackPlayer(this)
        audioTrack.initialize()
        audioRecorder = AudioRecorder {
            var average = 0L
            for (i in it.indices step 2) {
                val value = it.getShort(i).toLong()
                average += value * value
            }

            mapOf(1 to 2)
            average /= (it.size / 2)

            val volume = round(sqrt(average.toDouble()))

            Log.e("LOL", "volume: $volume")

            audioTrack.writeBytes(it)
        }

        audioRecorder.start()
        audioTrack.start()

    }

    override fun onBackPressed() {
        finish()
    }

    override fun onDestroy() {
        audioTrack.stop()
        audioRecorder.stop()
        val audioManager = getSystemService(AudioManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            audioManager.clearCommunicationDevice()
        }
        audioManager.mode = AudioManager.MODE_NORMAL
        audioManager.isSpeakerphoneOn = false
        audioManager.isMicrophoneMute = false
        audioManager.stopBluetoothSco()

        super.onDestroy()
    }
}

fun ByteArray.getShort(byteIndex: Int): Short {
    val b1 = (get(byteIndex) and 0xFF.toByte()).toInt()
    val b2 = (get(byteIndex + 1) and 0xFF.toByte()).toInt()

    return ((b1 shl 8) or (b2)).toShort()
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    BluetoothPlaybackTheme {
        Greeting("Android")
    }
}