package com.example.mlkitocr.util

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import java.util.Locale

object LocaleHelper {

    private const val PREF_NAME = "mlkit_ocr_settings"
    private const val KEY_LANGUAGE = "app_language"

    fun getLanguage(context: Context): String {
        return getPreferences(context).getString(KEY_LANGUAGE, "zh") ?: "zh"
    }

    fun setLanguage(context: Context, language: String) {
        getPreferences(context).edit().putString(KEY_LANGUAGE, language).apply()
    }

    fun applyLanguage(context: Context): Context {
        val language = getLanguage(context)
        return updateResources(context, language)
    }

    private fun getPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    private fun updateResources(context: Context, language: String): Context {
        val locale = Locale(language)
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }
}
