package dev.ml.smartattendance.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.fragment.app.FragmentActivity

val LocalFragmentActivity = staticCompositionLocalOf<FragmentActivity> {
    error("No FragmentActivity provided")
}