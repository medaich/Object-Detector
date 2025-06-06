package com.example.objectdetector.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import androidx.core.graphics.scale
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.objectdetector.network.PredictionRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.example.objectdetector.network.restApi
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


    fun onSelectApi(idx: Int) {
        if(idx == _uiState.value.currApiIdx) return

        _uiState.update { currState ->
            currState.copy(
                currApiIdx = idx,
                currStepIdx = if (_uiState.value.currImageBitmap == null) 0 else 2
            )
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun onUpload() {
        _uiState.update { currState ->
            currState.copy(
                currStepIdx = 3
            )
        }
        // image processing
        val originalBitmap = _uiState.value.currImageBitmap
        originalBitmap?.let { bitmap ->
            val resizedBitmap = bitmap.scale(300, 300)
            val pixelatedImage = pixelizeImageFromBitmap(resizedBitmap)
            predictRest(pixelatedImage)
        }
    }

    fun predictRest(pixelatedImage: Array<Array<Array<Int>>>) {
        val predictionRequest = PredictionRequest(
            instances = listOf(
                pixelatedImage.toList().map{
                    it.toList().map {
                        it.toList()
                    }
                }
            )
        )
        viewModelScope.launch {
            try {
                _uiState.update {
                    it.copy(
                        restResponse = RestResponse.Loading
                    )
                }

                val response = restApi.retrofitService.predict(predictionRequest)
                _uiState.update {
                    it.copy(
                        restResponse = RestResponse.Success(response)
                    )
                }
            }
            catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        restResponse = RestResponse.Error(e.message.toString())
                    )
                }
            }
        }
    }
}

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

@RequiresApi(Build.VERSION_CODES.O)
fun pixelizeImageFromBitmap(bitmap: Bitmap): Array<Array<Array<Int>>> {
    return try {
        val width = bitmap.width
        val height = bitmap.height

        // Ensure we have a software bitmap
        val workingBitmap = when {
            bitmap.config == Bitmap.Config.HARDWARE -> {
                bitmap.copy(Bitmap.Config.ARGB_8888, false)
            }
            bitmap.config == null -> {
                // Handle edge case where config might be null
                bitmap.copy(Bitmap.Config.ARGB_8888, false)
            }
            else -> bitmap
        }

        // Initialize 3D array
        val pixelArray = Array(height) { Array(width) { Array(3) { 0 } } }

        // Process pixels
        val pixels = IntArray(width * height)
        workingBitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        for (y in 0 until height) {
            for (x in 0 until width) {
                val pixelIndex = y * width + x
                val pixel = pixels[pixelIndex]

                pixelArray[y][x][0] = (pixel shr 16) and 0xFF // Red
                pixelArray[y][x][1] = (pixel shr 8) and 0xFF  // Green
                pixelArray[y][x][2] = pixel and 0xFF          // Blue
            }
        }

        pixelArray
    } catch (e: Exception) {
        e.printStackTrace()
        // Return empty array or throw exception based on your needs
        throw IllegalStateException("Failed to pixelize bitmap: ${e.message}", e)
    }
}