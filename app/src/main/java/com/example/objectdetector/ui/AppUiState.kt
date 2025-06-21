package com.example.objectdetector.ui

import android.graphics.Bitmap
import com.example.objectdetector.network.PredictionResponse

data class AppUiState(
    val currImageBitmap: Bitmap? = null,
    val annotatedBitmap : Bitmap? = null,
    val currApiIdx: Int? = null,
    val currStepIdx: Int = 0,
    val apiResponse : ApiResponse? = null
)

sealed interface ApiResponse {
    data class Success(val data: PredictionResponse) : ApiResponse
    data class Error(val message: String) : ApiResponse
    object Loading : ApiResponse
}