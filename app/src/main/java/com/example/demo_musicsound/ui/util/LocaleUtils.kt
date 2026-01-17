package com.example.demo_musicsound.ui.util

import android.app.Activity
import android.content.Context
import android.os.Build
import android.os.LocaleList
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.util.Locale

object LocaleUtils {

    private const val PREFS = "mybeat_prefs"
    private const val KEY_LANG = "app_lang" // "en", "it", "fr"

    fun getCurrentLanguage(context: Context): String {
        // 1) se salvata, usa quella
        val saved = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_LANG, null)
        if (!saved.isNullOrBlank()) return saved

        // 2) fallback: lingua di sistema
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.resources.configuration.locales[0].language
        } else {
            @Suppress("DEPRECATION")
            context.resources.configuration.locale.language
        }
    }

    fun setLanguage(context: Context, lang: String) {
        // salva
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LANG, lang)
            .apply()

        // applica (API 33+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val localeManager = context.getSystemService(android.app.LocaleManager::class.java)
            localeManager.applicationLocales = LocaleList(Locale.forLanguageTag(lang))
        } else {
            // pre-33: AppCompat
            AppCompatDelegate.setApplicationLocales(
                LocaleListCompat.forLanguageTags(lang)
            )
        }

        // IMPORTANTISSIMO: ricarica l'Activity corrente
        (context as? Activity)?.recreate()
    }

    fun applySavedLanguage(context: Context) {
        val lang = getCurrentLanguage(context)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val localeManager = context.getSystemService(android.app.LocaleManager::class.java)
            localeManager.applicationLocales = LocaleList(Locale.forLanguageTag(lang))
        } else {
            AppCompatDelegate.setApplicationLocales(
                LocaleListCompat.forLanguageTags(lang)
            )
        }
    }

}
