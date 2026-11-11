package com.joymilk.app.util

import java.text.NumberFormat
import java.util.Locale

object CurrencyUtils {
    private val localeID = Locale("id", "ID")

    /** Formats a Long IDR value to "Rp 10.000" */
    fun formatRupiah(amount: Long): String {
        val formatter = NumberFormat.getNumberInstance(localeID)
        return "Rp ${formatter.format(amount)}"
    }
}

