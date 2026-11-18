package com.joymilk.app.util

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object DateUtils {
    private val format = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.forLanguageTag("id-ID"))
        .withZone(ZoneId.of("Asia/Jakarta"))

    fun formatDate(date: Instant): String = format.format(date)
}
