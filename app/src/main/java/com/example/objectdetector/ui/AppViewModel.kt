package com.example.objectdetector.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class AppViewModel: ViewModel() {
    private val _uiState = MutableStateFlow(AppUiState())
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    fun saveImage(bitmap: Bitmap?) {
        _uiState.update {currState ->
            currState.copy(
                currImageBitmap = bitmap,
                currStepIdx = if(bitmap == null) 0 else 1
            )
        }
    }

    fun onUriSelected(uri: Uri?, context: Context) = saveImage(uri?.let{
        loadBitmapFromUri(context, it)
    })

    fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(context.contentResolver, uri)
                ImageDecoder.decodeBitmap(source)
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }


    fun onSelectApi(idx: Int) {
        if(idx == _uiState.value.currApiIdx) return

        _uiState.update { currState ->
            currState.copy(
                currApiIdx = idx,
                currStepIdx = if (_uiState.value.currImageBitmap == null) 0 else 2
            )
        }
    }

    fun onUpload() {
        _uiState.update { currState ->
            currState.copy(
                currStepIdx = 3
            )
        }
        // image processing
    }
}