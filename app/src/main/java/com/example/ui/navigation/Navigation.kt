package com.example.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Dashboard : Screen("dashboard", "Dashboard", Icons.Default.Dashboard)
    object Products : Screen("products", "Products", Icons.Default.Inventory2)
    object Records : Screen("records", "Business Records", Icons.Default.ReceiptLong)
    object Reports : Screen("reports", "Reports", Icons.Default.Assessment)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
    object AslamAi : Screen("ai", "ASLAM AI", Icons.Default.AutoAwesome)
}

val bottomNavItems = listOf(
    Screen.Dashboard,
    Screen.Products,
    Screen.Records,
    Screen.Reports,
    Screen.AslamAi,
    Screen.Settings
)
