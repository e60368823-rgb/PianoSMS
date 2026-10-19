package com.example.pianosms

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {
    private lateinit var statusText: TextView
    private var toneGenerator: ToneGenerator? = null
    companion object { private const val REQUEST_CODE = 1001 }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        statusText = findViewById(R.id.statusText)
        toneGenerator = try { ToneGenerator(AudioManager.STREAM_MUSIC, 70) } catch (_: Exception) { null }
        listOf(R.id.key1, R.id.key2, R.id.key3, R.id.key4, R.id.key5, R.id.key6, R.id.key7).forEachIndexed { index, id ->
            findViewById<Button>(id).setOnClickListener {
                try { toneGenerator?.startTone(ToneGenerator.TONE_DTMF_1 + index, 120) } catch (_: Exception) { }
            }
        }
        findViewById<Button>(R.id.testButton).setOnClickListener {
            statusText.text = "در حال ارسال پیام تست…"
            BaleApi.sendMessage("🎹 تست اتصال Piano SMS") { ok, error -> runOnUiThread {
                statusText.text = if (ok) "✅ تست ارسال موفق بود" else "❌ تست ناموفق: ${error ?: "خطای نامشخص"}"
                Toast.makeText(this, statusText.text, Toast.LENGTH_LONG).show()
            } }
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED) {
            statusText.text = "آماده • پیامک‌های جدید ارسال می‌شوند"
        } else {
            statusText.text = "برای شروع، مجوز دریافت پیامک را بده"
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECEIVE_SMS), REQUEST_CODE)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE) statusText.text = if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) "آماده • پیامک‌های جدید ارسال می‌شوند" else "مجوز پیامک داده نشد"
    }

    override fun onDestroy() { super.onDestroy(); toneGenerator?.release(); toneGenerator = null }
}
