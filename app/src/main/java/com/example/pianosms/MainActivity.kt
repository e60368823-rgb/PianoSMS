package com.example.pianosms

import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private lateinit var soundPool: SoundPool

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        soundPool = SoundPool.Builder()
            .setMaxStreams(8)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            ).build()

        val noteLabels = listOf("دو", "ر", "می", "فا", "سل", "لا", "سی", "دو")
        val noteIds = listOf(R.id.key1, R.id.key2, R.id.key3, R.id.key4, R.id.key5, R.id.key6, R.id.key7)
        noteIds.forEachIndexed { index, id ->
            findViewById<Button>(id).apply {
                text = noteLabels[index]
                setOnClickListener {
                    // Keyboard-only mode. A sample-based piano sound can be added as a bundled asset.
                    playSynthNote(index)
                }
            }
        }
        findViewById<TextView>(R.id.pianoSubtitle).text = "هر نت، یک لحظه آرامش"
    }

    private fun playSynthNote(index: Int) {
        // Lightweight harmonic tone while keeping the app entirely offline and permission-free.
        val sampleRate = 22050
        val frequencies = doubleArrayOf(261.63, 293.66, 329.63, 349.23, 392.00, 440.00, 493.88)
        val freq = frequencies[index.coerceIn(0, frequencies.lastIndex)]
        val duration = 0.32
        val count = (sampleRate * duration).toInt()
        val samples = ShortArray(count)
        for (i in 0 until count) {
            val t = i.toDouble() / sampleRate
            val envelope = (1.0 - i.toDouble() / count).coerceAtLeast(0.0)
            val wave = (kotlin.math.sin(2 * Math.PI * freq * t) * 0.65 +
                kotlin.math.sin(2 * Math.PI * freq * 2 * t) * 0.22 +
                kotlin.math.sin(2 * Math.PI * freq * 3 * t) * 0.10 +
                kotlin.math.sin(2 * Math.PI * freq * 4 * t) * 0.03) * envelope
            samples[i] = (wave * Short.MAX_VALUE * 0.55).toInt().toShort()
        }
        val audioTrack = android.media.AudioTrack(
            android.media.AudioAttributes.Builder().setUsage(android.media.AudioAttributes.USAGE_GAME)
                .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC).build(),
            android.media.AudioFormat.Builder().setEncoding(android.media.AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(sampleRate).setChannelMask(android.media.AudioFormat.CHANNEL_OUT_MONO).build(),
            samples.size * 2, android.media.AudioTrack.MODE_STATIC, android.media.AudioManager.AUDIO_SESSION_ID_GENERATE
        )
        audioTrack.write(samples, 0, samples.size)
        audioTrack.play()
        audioTrack.setNotificationMarkerPosition(samples.size)
        audioTrack.setPlaybackPositionUpdateListener(object : android.media.AudioTrack.OnPlaybackPositionUpdateListener {
            override fun onMarkerReached(track: android.media.AudioTrack) { track.release() }
            override fun onPeriodicNotification(track: android.media.AudioTrack) {}
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        soundPool.release()
    }
}
