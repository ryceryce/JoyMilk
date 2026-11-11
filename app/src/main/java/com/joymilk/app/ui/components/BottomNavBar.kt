package com.joymilk.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.Composable

@Composable
fun BottomNavBar(
    currentRoute: String,
    onDashboard: () -> Unit,
    onOrders: () -> Unit,
    onReports: () -> Unit,
    onMore: () -> Unit
) {
    NavigationBar {
        NavigationBarItem(
            selected = currentRoute == "dashboard",
            onClick = onDashboard,
            icon = { Icon(Icons.Default.Home, null) },
            label = { Text("Beranda") }
        )
        NavigationBarItem(
            selected = currentRoute == "orders",
            onClick = onOrders,
            icon = { Icon(Icons.Default.List, null) },
            label = { Text("Order") }
        )
        NavigationBarItem(
            selected = currentRoute == "reports",
            onClick = onReports,
            icon = { Icon(Icons.Default.BarChart, null) },
            label = { Text("Laporan") }
        )
        NavigationBarItem(
            selected = currentRoute == "more",
            onClick = onMore,
            icon = { Icon(Icons.Default.MoreHoriz, null) },
            label = { Text("Lainnya") }
        )
    }
}

