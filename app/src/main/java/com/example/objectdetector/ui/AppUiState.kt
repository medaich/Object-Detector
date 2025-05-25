package com.example.objectdetector.ui

import android.graphics.Bitmap

data class AppUiState(
    val currImageBitmap: Bitmap? = null,
    val currApiIdx: Int? = 0,
    val currStepIdx: Int = 0
)
