package com.example.objectdetector.ui

import android.graphics.Bitmap
import com.example.objectdetector.network.PredictionResponse

data class AppUiState(
    val currImageBitmap: Bitmap? = null,
    val annotatedBitmap : Bitmap? = null,
    val currApiIdx: Int? = 0,
    val currStepIdx: Int = 0,
    val restResponse : RestResponse? = null
)

sealed interface RestResponse {
    data class Success(val data: PredictionResponse) : RestResponse
    data class Error(val message: String) : RestResponse
    object Loading : RestResponse
}