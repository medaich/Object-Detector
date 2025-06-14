package com.example.objectdetector.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ImageDecoder
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.graphics.scale
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.objectdetector.data.DataSource
import com.example.objectdetector.network.Prediction
import com.example.objectdetector.network.PredictionRequest
import com.example.objectdetector.network.restApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

class AppViewModel: ViewModel() {

    override fun onCleared() {
        super.onCleared()
        // Clean up gRPC resources when ViewModel is destroyed
        com.example.objectdetector.network.grpcApi.service.shutdown()
    }

    companion object {
        private const val MAX_RETRY_ATTEMPTS = 3
        private const val INITIAL_RETRY_DELAY_MS = 1000L
        private const val REQUEST_TIMEOUT_MS = 30000L
    }

    private val _uiState = MutableStateFlow(AppUiState())
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    fun saveImage(bitmap: Bitmap?) {
        _uiState.update {currState ->
            currState.copy(
                currImageBitmap = bitmap,
                annotatedBitmap = null,
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
    fun onUpload(context: Context) {
        // toast preparing the image
        Toast.makeText(
            context,
            "Preparing image...",
            Toast.LENGTH_SHORT
        ).show()

        _uiState.update { currState ->
            currState.copy(
                currStepIdx = 3,
                apiResponse = ApiResponse.Loading
            )
        }

        // heavy processing in background thread
        viewModelScope.launch {
            try {
                val originalBitmap = _uiState.value.currImageBitmap
                originalBitmap?.let { bitmap ->
                    // Process image on IO thread
                    val pixelatedImage = withContext(Dispatchers.IO) {
                        val resizedBitmap = bitmap.scale(300, 300)
                        pixelImageFromBitmap(resizedBitmap)
                    }

                    // Make network request based on selected API
                    when (_uiState.value.currApiIdx) {
                        0 -> predictRest(pixelatedImage, context)
                        1 -> predictGrpc(pixelatedImage, context)
                    }

                    _uiState.update { currState ->
                        if (currState.apiResponse is ApiResponse.Success)
                            currState.copy(
                                annotatedBitmap = currState.currImageBitmap?.annotateWithDetections(
                                    currState.apiResponse.data.predictions.first(),
                                    context
                                )
                            )
                        else
                            currState
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        apiResponse = ApiResponse.Error("Image processing failed: ${e.message}")
                    )
                }
            }
        }
    }

    private suspend fun predictRest(pixelatedImage: Array<Array<Array<Int>>>, context: Context) {
        val predictionRequest = PredictionRequest(
            instances = listOf(
                pixelatedImage.toList().map{
                    it.toList().map {
                        it.toList()
                    }
                }
            )
        )

        var lastException: Exception? = null
        
        // Unified retry logic
        repeat(MAX_RETRY_ATTEMPTS) { attempt ->
            try {
                Toast.makeText(
                    context,
                    if (attempt == 0) "Processing image via REST..." 
                    else "Retrying REST request (${attempt + 1}/$MAX_RETRY_ATTEMPTS)...",
                    Toast.LENGTH_SHORT
                ).show()

                // Network request with timeout handling
                val response = withContext(Dispatchers.IO) {
                    withTimeout(REQUEST_TIMEOUT_MS) {
                        restApi.retrofitService.predict(predictionRequest)
                    }
                }

                _uiState.update {
                    it.copy(
                        apiResponse = ApiResponse.Success(response)
                    )
                }
                return // Success, exit retry loop
                
            } catch (e: Exception) {
                lastException = e
                val errorMessage = when {
                    e is kotlinx.coroutines.TimeoutCancellationException -> "REST request timeout"
                    e.message?.contains("ConnectException") == true -> "REST connection failed"
                    e.message?.contains("SocketTimeoutException") == true -> "REST request timeout"
                    e.message?.contains("UnknownHostException") == true -> "REST server unreachable"
                    else -> "REST error: ${e.message}"
                }
                
                if (attempt < MAX_RETRY_ATTEMPTS - 1) {
                    // Exponential backoff delay
                    delay(INITIAL_RETRY_DELAY_MS * (attempt + 1))
                } else {
                    // Final attempt failed
                    _uiState.update {
                        it.copy(
                            apiResponse = ApiResponse.Error(errorMessage)
                        )
                    }
                }
            }
        }
        
        Toast.makeText(
            context,
            "REST processing ${if (lastException != null) "failed" else "finished"}...",
            Toast.LENGTH_SHORT
        ).show()
    }

    private suspend fun predictGrpc(pixelatedImage: Array<Array<Array<Int>>>, context: Context) {
        var lastException: Exception? = null
        
        // Unified retry logic (same as REST)
        repeat(MAX_RETRY_ATTEMPTS) { attempt ->
            try {
                Toast.makeText(
                    context,
                    if (attempt == 0) "Processing image via gRPC..." 
                    else "Retrying gRPC request (${attempt + 1}/$MAX_RETRY_ATTEMPTS)...",
                    Toast.LENGTH_SHORT
                ).show()

                // Network request with unified timeout handling
                val response = withContext(Dispatchers.IO) {
                    withTimeout(REQUEST_TIMEOUT_MS) {
                        com.example.objectdetector.network.grpcApi.service.predict(pixelatedImage)
                    }
                }

                _uiState.update {
                    it.copy(
                        apiResponse = ApiResponse.Success(response)
                    )
                }
                return // Success, exit retry loop
                
            } catch (e: Exception) {
                lastException = e
                val errorMessage = when {
                    e is kotlinx.coroutines.TimeoutCancellationException -> "gRPC request timeout"
                    e.message?.contains("UNAVAILABLE") == true -> "gRPC server unavailable"
                    e.message?.contains("DEADLINE_EXCEEDED") == true -> "gRPC request timeout"
                    e.message?.contains("UNAUTHENTICATED") == true -> "gRPC authentication failed"
                    e.message?.contains("ConnectException") == true -> "gRPC connection failed"
                    else -> "gRPC error: ${e.message}"
                }
                
                if (attempt < MAX_RETRY_ATTEMPTS - 1) {
                    // Exponential backoff delay (same as REST)
                    delay(INITIAL_RETRY_DELAY_MS * (attempt + 1))
                } else {
                    // Final attempt failed
                    _uiState.update {
                        it.copy(
                            apiResponse = ApiResponse.Error(errorMessage)
                        )
                    }
                }
            }
        }
        
        Toast.makeText(
            context,
            "gRPC processing ${if (lastException != null) "failed" else "finished"}...",
            Toast.LENGTH_SHORT
        ).show()
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
fun pixelImageFromBitmap(bitmap: Bitmap): Array<Array<Array<Int>>> {
    return try {
        val width = bitmap.width
        val height = bitmap.height

        // Ensure we have a software bitmap
        val workingBitmap = when {
            bitmap.config == Bitmap.Config.HARDWARE -> {
                bitmap.copy(Bitmap.Config.ARGB_8888, false)
            }
            bitmap.config == null -> {
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
        throw IllegalStateException("Failed to pixelize bitmap: ${e.message}", e)
    }
}

// Extension function to annotate bitmap with detection results
fun Bitmap.annotateWithDetections(
    prediction: Prediction,
    context: Context
    ): Bitmap {
    // Create a mutable copy of the bitmap
    val annotatedBitmap = this.copy(Bitmap.Config.ARGB_8888, true)
    val canvas = Canvas(annotatedBitmap)

    // Define colors for different object classes
    val colors = listOf(
        Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW,
        Color.CYAN, Color.MAGENTA
    )

    // Paint for bounding boxes
    val boxPaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 8f
        isAntiAlias = true
    }

    // Paint for labels background
    val labelBackgroundPaint = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    // Paint for label text
    val labelTextPaint = Paint().apply {
        color = Color.WHITE
        textSize = 48f
        isAntiAlias = true
        typeface = Typeface.DEFAULT_BOLD
    }

    prediction.detectionBoxes.slice(0..prediction.numDetections.toInt() - 1).forEachIndexed { index, boundingBox ->
        // Get color for this detection (cycle through colors)
        val color = colors[index % colors.size]

        // Set box color
        boxPaint.color = color
        labelBackgroundPaint.color = color

        // Draw bounding box
        canvas.drawRect(
            boundingBox[1] * width,
            boundingBox[0] * height,
            boundingBox[3] * width,
            boundingBox[2] * height,
            boxPaint
        )

        // Prepare label text
        val labelText = "${context.getString(
            DataSource.cocoLabels[
                prediction.detectionClasses[index].toInt() - 1
            ]
        )} (${(prediction.detectionScores[index] * 100).toInt()}%)"

        // Calculate text bounds
        val textBounds = Rect()
        labelTextPaint.getTextBounds(labelText, 0, labelText.length, textBounds)

        // Calculate label position
        val labelX = boundingBox[1] * width
        val labelY = boundingBox[0] * height - textBounds.height() + 32f

        // Draw label background
        val labelBackgroundRect = RectF(
            labelX - 8f,
            labelY - 8f,
            labelX + textBounds.width() + 16f,
            labelY + textBounds.height() + 8f
        )
        canvas.drawRoundRect(labelBackgroundRect, 8f, 8f, labelBackgroundPaint)

        // Draw label text
        canvas.drawText(labelText, labelX, labelY + textBounds.height(), labelTextPaint)
    }

    return annotatedBitmap
}
