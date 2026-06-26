package com.example.mlkitocr

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.mlkitocr.util.LocaleHelper
import com.google.android.material.button.MaterialButton
import com.google.android.material.switchmaterial.SwitchMaterial

class SettingsActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.applyLanguage(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_settings)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.settingsRoot)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val currentLang = LocaleHelper.getLanguage(this)
        val languageSwitch = findViewById<SwitchMaterial>(R.id.languageSwitch)
        val languageLabel = findViewById<TextView>(R.id.languageLabel)

        // Initialize switch state
        languageSwitch.isChecked = currentLang == "en"
        updateLanguageLabel(languageLabel, currentLang)

        languageSwitch.setOnCheckedChangeListener { _, isEnglish ->
            val newLang = if (isEnglish) "en" else "zh"
            LocaleHelper.setLanguage(this, newLang)
            updateLanguageLabel(languageLabel, newLang)
            // Restart activity to apply new language
            recreate()
        }

        findViewById<MaterialButton>(R.id.logoutButton).setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            finishAffinity()
        }
    }

    private fun updateLanguageLabel(label: TextView, lang: String) {
        label.text = if (lang == "en") "English" else "中文"
    }
}
