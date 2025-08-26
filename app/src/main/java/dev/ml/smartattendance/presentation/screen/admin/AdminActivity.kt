package dev.ml.smartattendance.presentation.screen.admin

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

data class AdminActivity(
    val title: String,
    val description: String,
    val timeAgo: String,
    val icon: ImageVector,
    val color: Color
)