package com.joymilk.app.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class PriceHistory(
    @DocumentId val id: String = "",
    val price: Long = 0L,
    val effectiveDate: Timestamp = Timestamp.now(),
    val changedBy: String = ""
)
