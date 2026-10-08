package com.jumincho.cvpass.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/** Formats dates and times in the user's locale and the device's time zone. */
class Formats(locale: Locale) {

    private val zone: ZoneId = ZoneId.systemDefault()
    private val dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)
    private val timeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale)
    private val dateTimeFormatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT).withLocale(locale)

    fun date(date: LocalDate): String = dateFormatter.format(date)

    fun time(instant: Instant): String = timeFormatter.format(instant.atZone(zone))

    fun dateTime(instant: Instant): String = dateTimeFormatter.format(instant.atZone(zone))
}

/** [Formats] for the current locale. */
@Composable
fun rememberFormats(): Formats {
    val locale = LocalConfiguration.current.locales[0]
    return remember(locale) { Formats(locale) }
}
