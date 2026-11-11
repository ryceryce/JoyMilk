package com.joymilk.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.joymilk.app.util.Constants

@Composable
fun StatusChip(status: String, modifier: Modifier = Modifier) {
    val (bg, label) = when (status) {
        Constants.STATUS_PENDING -> Color(0xFFFF9800) to "Pending"
        Constants.STATUS_DIPROSES -> Color(0xFF2196F3) to "Diproses"
        Constants.STATUS_SELESAI -> Color(0xFF4CAF50) to "Selesai"
        Constants.STATUS_DIBATALKAN -> Color(0xFF9E9E9E) to "Dibatalkan"
        else -> Color(0xFF9E9E9E) to status
    }
    Box(
        modifier = modifier
            .background(bg.copy(alpha = 0.15f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = bg
        )
    }
}
