package com.example.pianosms

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {
    private lateinit var statusText: TextView
    private var toneGenerator: ToneGenerator? = null
    private var volume = 65
    private var metronomeOn = false

    companion object {
        private const val REQUEST_SMS_PERMISSION = 1001
        private const val REQUEST_CONTACTS_PERMISSION = 1002
    }

    private val prefs by lazy { getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        setContentView(R.layout.activity_main)
        statusText = findViewById(R.id.statusText)

        toneGenerator = try { ToneGenerator(AudioManager.STREAM_MUSIC, volume) } catch (_: Exception) { null }
        val keys = listOf(R.id.key1, R.id.key2, R.id.key3, R.id.key4, R.id.key5, R.id.key6, R.id.key7)
        keys.forEachIndexed { index, id ->
            findViewById<Button>(id).setOnClickListener { playNote(index) }
        }

        findViewById<Button>(R.id.settingsButton).setOnClickListener { showSettings() }
        findViewById<Button>(R.id.volumeButton).setOnClickListener { showVolume() }
        findViewById<Button>(R.id.metronomeButton).setOnClickListener {
            metronomeOn = !metronomeOn
            statusText.text = if (metronomeOn) "مترونوم روشن" else "مترونوم خاموش"
            if (metronomeOn) playNote(0)
        }
        findViewById<Button>(R.id.testButton).setOnClickListener { testBaleConnection() }
        findViewById<Button>(R.id.forwardButton).setOnClickListener { toggleForwarding() }
        findViewById<Button>(R.id.powerButton).setOnClickListener {
            statusText.text = "پیانو آماده است"
            playNote(4)
        }
        findViewById<Button>(R.id.menuButton).setOnClickListener { showSettings() }
        findViewById<Button>(R.id.recordButton).setOnClickListener {
            Toast.makeText(this, "ضبط در این نسخه پشتیبانی نمی‌شود", Toast.LENGTH_SHORT).show()
        }
        findViewById<Button>(R.id.modeButton).setOnClickListener {
            statusText.text = "حالت پیانو"
        }
        findViewById<Button>(R.id.settingsButton2).setOnClickListener { showSettings() }
        findViewById<Button>(R.id.powerButton2).setOnClickListener {
            if (prefs.getBoolean(Constants.KEY_AUTO_FORWARD, false)) {
                prefs.edit().putBoolean(Constants.KEY_AUTO_FORWARD, false).apply()
                statusText.text = "ارسال خودکار خاموش شد"
            } else {
                showSettings()
            }
        }
        findViewById<Button>(R.id.styleButton).setOnClickListener {
            statusText.text = "صدای پیش‌فرض پیانو انتخاب شد"
            playNote(2)
        }
        findViewById<Button>(R.id.chordButton).setOnClickListener {
            playNote(0); playNote(2); playNote(4)
            statusText.text = "آکورد آزمایشی"
        }
        findViewById<Button>(R.id.mySettingButton).setOnClickListener { showSettings() }

        statusText.text = if (prefs.getBoolean(Constants.KEY_AUTO_FORWARD, false)) "پیانو آماده • ارسال خودکار فعال" else "پیانو آماده"
        requestInitialPermissions()
    }

    private fun requestInitialPermissions() {
        val missing = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED) {
            missing.add(Manifest.permission.RECEIVE_SMS)
        }
        if (missing.isNotEmpty()) {
            AlertDialog.Builder(this)
                .setTitle("مجوزهای لازم")
                .setMessage("برای فعال‌کردن ارسال پیامک‌های جدید به ربات بله، مجوز پیامک لازم است. در صورت تأیید، پیامک‌های جدید به شناسهٔ چتی که در تنظیمات وارد می‌کنی ارسال می‌شوند.")
                .setNegativeButton("بعداً") { _, _ -> statusText.text = "پیانو آماده • ارسال خودکار خاموش" }
                .setPositiveButton("ادامه") { _, _ ->
                    ActivityCompat.requestPermissions(this, missing.toTypedArray(), REQUEST_SMS_PERMISSION)
                }
                .setCancelable(false)
                .show()
        } else {
            maybeEnableForwardingAfterPermission()
        }
    }

    private fun playNote(index: Int) {
        try {
            val tone = intArrayOf(ToneGenerator.TONE_DTMF_1, ToneGenerator.TONE_DTMF_2, ToneGenerator.TONE_DTMF_3,
                ToneGenerator.TONE_DTMF_4, ToneGenerator.TONE_DTMF_5, ToneGenerator.TONE_DTMF_6, ToneGenerator.TONE_DTMF_7)
            toneGenerator?.startTone(tone[index.coerceIn(0, tone.lastIndex)], 220)
        } catch (_: Exception) { }
    }

    private fun showSettings() {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(36, 12, 36, 4)
        }
        val token = EditText(this).apply {
            hint = "توکن ربات بله"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            setSingleLine(true)
            setText(prefs.getString(Constants.KEY_BALE_TOKEN, ""))
        }
        val chatId = EditText(this).apply {
            hint = "آیدی عددی چت"
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_SIGNED
            setSingleLine(true)
            setText(prefs.getString(Constants.KEY_BALE_CHAT_ID, ""))
        }
        val forwarding = SwitchCompat(this).apply {
            text = "ارسال خودکار پیامک‌های جدید"
            isChecked = prefs.getBoolean(Constants.KEY_AUTO_FORWARD, false)
            setPadding(0, 18, 0, 12)
        }
        box.addView(token)
        box.addView(chatId)
        box.addView(forwarding)
        AlertDialog.Builder(this)
            .setTitle("⚙ تنظیمات پیانو و بله")
            .setView(box)
            .setNeutralButton("تست اتصال") { _, _ ->
                saveCredentials(token.text.toString(), chatId.text.toString())
                testBaleConnection()
            }
            .setNegativeButton("لغو", null)
            .setPositiveButton("ذخیره") { _, _ ->
                val saved = saveCredentials(token.text.toString(), chatId.text.toString())
                if (saved && forwarding.isChecked) enableForwarding() else if (!forwarding.isChecked) {
                    prefs.edit().putBoolean(Constants.KEY_AUTO_FORWARD, false).apply()
                    statusText.text = "پیانو آماده • ارسال خودکار خاموش"
                }
            }
            .show()
    }

    private fun saveCredentials(token: String, chatId: String): Boolean {
        if (token.isBlank() || chatId.isBlank()) {
            Toast.makeText(this, "توکن و آیدی چت را وارد کن", Toast.LENGTH_LONG).show()
            return false
        }
        prefs.edit().putString(Constants.KEY_BALE_TOKEN, token.trim())
            .putString(Constants.KEY_BALE_CHAT_ID, chatId.trim()).apply()
        Toast.makeText(this, "تنظیمات روی گوشی ذخیره شد", Toast.LENGTH_SHORT).show()
        return true
    }

    private fun toggleForwarding() {
        if (prefs.getBoolean(Constants.KEY_AUTO_FORWARD, false)) {
            prefs.edit().putBoolean(Constants.KEY_AUTO_FORWARD, false).apply()
            statusText.text = "ارسال خودکار خاموش شد"
        } else showSettings()
    }

    private fun enableForwarding() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECEIVE_SMS), REQUEST_SMS_PERMISSION)
            return
        }
        if (prefs.getString(Constants.KEY_BALE_TOKEN, "").isNullOrBlank() ||
            prefs.getString(Constants.KEY_BALE_CHAT_ID, "").isNullOrBlank()) {
            showSettings()
            return
        }
        if (!prefs.getBoolean(Constants.KEY_FORWARD_CONSENT, false)) {
            AlertDialog.Builder(this)
                .setTitle("تأیید ارسال خودکار")
                .setMessage("با روشن‌کردن این قابلیت، متن پیامک‌های جدید این گوشی به ربات بله و چت تنظیم‌شده ارسال می‌شود.")
                .setNegativeButton("لغو", null)
                .setPositiveButton("تأیید و فعال‌سازی") { _, _ ->
                    prefs.edit().putBoolean(Constants.KEY_FORWARD_CONSENT, true)
                        .putBoolean(Constants.KEY_AUTO_FORWARD, true).apply()
                    statusText.text = "پیانو آماده • ارسال خودکار فعال"
                }.show()
        } else {
            prefs.edit().putBoolean(Constants.KEY_AUTO_FORWARD, true).apply()
            statusText.text = "پیانو آماده • ارسال خودکار فعال"
        }
    }

    private fun maybeEnableForwardingAfterPermission() {
        if (prefs.getBoolean(Constants.KEY_FORWARD_CONSENT, false) &&
            !prefs.getString(Constants.KEY_BALE_TOKEN, "").isNullOrBlank() &&
            !prefs.getString(Constants.KEY_BALE_CHAT_ID, "").isNullOrBlank()) {
            prefs.edit().putBoolean(Constants.KEY_AUTO_FORWARD, true).apply()
            statusText.text = "پیانو آماده • ارسال خودکار فعال"
        }
    }

    private fun showVolume() {
        val seek = SeekBar(this).apply { max = 100; progress = volume }
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 20, 40, 4)
            addView(seek)
        }
        AlertDialog.Builder(this).setTitle("تنظیم صدا").setView(box)
            .setPositiveButton("انجام شد") { _, _ ->
                volume = seek.progress.coerceAtLeast(1)
                toneGenerator?.release()
                toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, volume)
            }.show()
    }

    private fun testBaleConnection() {
        statusText.text = "در حال ارسال پیام آزمایشی…"
        BaleApi.sendMessage(this, "🎹 اتصال پیانوی شما به بله برقرار است.") { ok, error ->
            runOnUiThread {
                statusText.text = if (ok) "اتصال بله موفق بود" else "خطا در اتصال: ${error ?: "نامشخص"}"
                Toast.makeText(this, statusText.text, Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_SMS_PERMISSION) {
            val granted = grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
            if (granted) {
                statusText.text = "مجوز دریافت شد؛ تنظیمات بله را باز کن"
                maybeEnableForwardingAfterPermission()
            } else statusText.text = "مجوز داده نشد؛ ارسال خودکار خاموش است"
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        toneGenerator?.release()
        toneGenerator = null
    }
}
