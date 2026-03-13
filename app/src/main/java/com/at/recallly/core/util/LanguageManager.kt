package com.at.recallly.core.util

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

data class AppLanguage(
    val code: String,
    val displayName: String,
    val nativeName: String
)

object LanguageManager {

    val supportedLanguages = listOf(
        AppLanguage("en", "English", "English"),
        AppLanguage("ar", "Arabic", "العربية"),
        AppLanguage("es", "Spanish", "Español"),
        AppLanguage("tr", "Turkish", "Türkçe")
    )

    fun applyLanguage(code: String) {
        AppCompatDelegate.setApplicationLocales(
            LocaleListCompat.forLanguageTags(code)
        )
    }

    fun getCurrentLanguageCode(): String {
        val locales = AppCompatDelegate.getApplicationLocales()
        if (!locales.isEmpty) {
            val tag = locales[0]?.language
            if (tag != null && tag in supportedLanguages.map { it.code }) {
                return tag
            }
        }
        return "en"
    }

    fun getLanguageDisplayName(code: String): String =
        supportedLanguages.find { it.code == code }?.nativeName ?: "English"

    fun getWhisperLanguageCode(appLanguage: String): String =
        if (appLanguage in listOf("en", "ar", "es", "tr")) appLanguage else "auto"

    fun getSpeechRecognizerLocale(appLanguage: String): String = when (appLanguage) {
        "ar" -> "ar"
        "es" -> "es"
        "tr" -> "tr"
        else -> "en-US"
    }

    fun getLanguageFullName(code: String): String = when (code) {
        "ar" -> "Arabic"
        "es" -> "Spanish"
        "tr" -> "Turkish"
        else -> "English"
    }
}
