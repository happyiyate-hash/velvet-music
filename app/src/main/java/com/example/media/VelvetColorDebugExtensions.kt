package com.example.media

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

internal fun Color.toHex(): String =
    "#${toArgb().toUInt().toString(16).padStart(8, '0')}"
