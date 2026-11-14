package ua.syt0r.kanji.presentation.screen.main

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarVisuals

data class MainSnackbarNotification(
    override val message: String,
    val isError: Boolean,
    override val actionLabel: String? = null,
    override val withDismissAction: Boolean = true,
    override val duration: SnackbarDuration = SnackbarDuration.Long,
    val handleAction: () -> MainDestination?
) : SnackbarVisuals
