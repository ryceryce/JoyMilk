package com.joymilk.app.data.model

import java.time.Instant

data class PriceHistory(
    val id: String = "",
    val price: Long = 0L,
    val effectiveDate: Instant = Instant.now(),
    val changedBy: String = ""
)
