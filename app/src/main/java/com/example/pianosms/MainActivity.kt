package com.example.pianosms

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {
    private lateinit var statusText: TextView
    private lateinit var tokenInput: EditText
    private lateinit var chatIdInput: EditText
    private var toneGenerator: ToneGenerator? = null

    companion object { private const val REQUEST_CODE = 1001 }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        statusText = findViewById(R.id.statusText)
        tokenInput = findViewById(R.id.tokenInput)
        chatIdInput = findViewById(R.id.chatIdInput)
        val prefs = getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
        tokenInput.setText(prefs.getString(Constants.KEY_BALE_TOKEN, ""))
        chatIdInput.setText(prefs.getString(Constants.KEY_BALE_CHAT_ID, ""))

        toneGenerator = try { ToneGenerator(AudioManager.STREAM_MUSIC, 70) } catch (_: Exception) { null }
        listOf(R.id.key1, R.id.key2, R.id.key3, R.id.key4, R.id.key5, R.id.key6, R.id.key7).forEachIndexed { index, id ->
            findViewById<Button>(id).setOnClickListener {
                try { toneGenerator?.startTone(ToneGenerator.TONE_DTMF_1 + index, 120) } catch (_: Exception) { }
            }
        }

        findViewById<Button>(R.id.saveButton).setOnClickListener {
            val token = tokenInput.text.toString().trim()
            val chatId = chatIdInput.text.toString().trim()
            if (token.isBlank() || chatId.isBlank()) {
                Toast.makeText(this, "توکن و شناسه چت را وارد کن", Toast.LENGTH_LONG).show()
            } else {
                prefs.edit().putString(Constants.KEY_BALE_TOKEN, token)
                    .putString(Constants.KEY_BALE_CHAT_ID, chatId).apply()
                statusText.text = "تنظیمات روی همین گوشی ذخیره شد"
                Toast.makeText(this, "تنظیمات ذخیره شد", Toast.LENGTH_SHORT).show()
            }
        }

        findViewById<Button>(R.id.testButton).setOnClickListener {
            val token = tokenInput.text.toString().trim()
            val chatId = chatIdInput.text.toString().trim()
            if (token.isBlank() || chatId.isBlank()) {
                statusText.text = "اول توکن و شناسه چت را وارد کن"
                return@setOnClickListener
            }
            prefs.edit().putString(Constants.KEY_BALE_TOKEN, token)
                .putString(Constants.KEY_BALE_CHAT_ID, chatId).apply()
            statusText.text = "در حال ارسال پیام تست…"
            BaleApi.sendMessage(this, "🎹 تست اتصال Piano SMS") { ok, error -> runOnUiThread {
                statusText.text = if (ok) "✅ تست ارسال موفق بود" else "❌ تست ناموفق: ${error ?: "خطای نامشخص"}"
                Toast.makeText(this, statusText.text, Toast.LENGTH_LONG).show()
            } }
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED) {
            statusText.text = "آماده • پیامک‌های جدید پس از تنظیم بله ارسال می‌شوند"
        } else {
            statusText.text = "برای شروع، مجوز دریافت پیامک را بده"
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECEIVE_SMS), REQUEST_CODE)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE) statusText.text =
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                "آماده • پیامک‌های جدید پس از تنظیم بله ارسال می‌شوند"
            else "مجوز پیامک داده نشد"
    }

    override fun onDestroy() {
        super.onDestroy()
        toneGenerator?.release()
        toneGenerator = null
    }
}
