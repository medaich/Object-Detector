package com.example.objectdetector.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ImageDecoder
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.scale
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.objectdetector.R
import com.example.objectdetector.data.DataSource
import com.example.objectdetector.network.Prediction
import com.example.objectdetector.network.PredictionRequest
import com.example.objectdetector.network.restApi
import com.example.objectdetector.ui.theme.onPrimaryLight
import com.example.objectdetector.ui.theme.primaryLight
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.math.max
import kotlin.math.min

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

    @RequiresApi(Build.VERSION_CODES.O)
    fun saveImage(bitmap: Bitmap?, context: Context){
        _uiState.update {currState ->
            currState.copy(
                currImageBitmap = bitmap,
                apiResponse = null,
                annotatedBitmap = null,
                currStepIdx = when {
                    bitmap == null -> if (currState.currStepIdx >= 1) 1 else 0
                    else -> if (currState.currStepIdx >= 1) 2 else 0
                }
            )
        }
        if(_uiState.value.currStepIdx == 2)
            onUpload(context)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun onUriSelected(uri: Uri?, context: Context) = saveImage(uri?.let{
        loadBitmapFromUri(context, it)
    }, context)

    @RequiresApi(Build.VERSION_CODES.O)
    fun onSelectApi(idx: Int, context: Context) {
        if(idx == _uiState.value.currApiIdx) return
        if(_uiState.value.apiResponse is ApiResponse.Loading) return

        _uiState.update { currState ->
            currState.copy(
                currApiIdx = idx,
                annotatedBitmap = null,
                currStepIdx = if (currState.currImageBitmap == null) 1 else 2
            )
        }
        if(_uiState.value.currStepIdx == 2)
            onUpload(context)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun onUpload(context: Context) {
        // toast preparing the image
        Toast.makeText(
            context,
            context.getString(R.string.preparing_image),
            Toast.LENGTH_SHORT
        ).show()

        _uiState.update { currState ->
            currState.copy(
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
                                currStepIdx = 3,
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
                        apiResponse = ApiResponse.Error(
                            context.getString(R.string.something_went_wrong)
                        )
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
                    if (attempt == 0) context.getString(R.string.processing_image)
                    else context.getString(R.string.retrying),
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
                
                if (attempt < MAX_RETRY_ATTEMPTS - 1) {
                    // Exponential backoff delay
                    delay(INITIAL_RETRY_DELAY_MS * (attempt + 1))
                } else {
                    // Final attempt failed - show user-friendly error
                    val userFriendlyError = getUserFriendlyError(e)
                    _uiState.update {
                        it.copy(
                            apiResponse = ApiResponse.Error(
                                context.getString(userFriendlyError)
                            )
                        )
                    }
                }
            }
        }
        
        Toast.makeText(
            context,
            if (lastException != null) context.getString(R.string.something_went_wrong)
            else context.getString(R.string.done),
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
                    if (attempt == 0) context.getString(R.string.processing_image)
                    else context.getString(R.string.retrying),
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
                
                if (attempt < MAX_RETRY_ATTEMPTS - 1) {
                    // Exponential backoff delay (same as REST)
                    delay(INITIAL_RETRY_DELAY_MS * (attempt + 1))
                } else {
                    // Final attempt failed - show user-friendly error
                    val userFriendlyError = getUserFriendlyError(e)
                    _uiState.update {
                        it.copy(
                            apiResponse = ApiResponse.Error(
                                context.getString(userFriendlyError)
                            )
                        )
                    }
                }
            }
        }
        
        Toast.makeText(
            context,
            if (lastException != null) context.getString(R.string.something_went_wrong)
            else context.getString(R.string.done),
            Toast.LENGTH_SHORT
        ).show()
    }

    // Simple user-friendly error mapping
    private fun getUserFriendlyError(exception: Exception): Int {
        return when {
            // Network connectivity issues
            exception.message?.contains("ConnectException") == true ||
            exception.message?.contains("UnknownHostException") == true ||
            exception.message?.contains("SocketTimeoutException") == true ||
            exception.message?.contains("UNAVAILABLE") == true ||
            exception is kotlinx.coroutines.TimeoutCancellationException -> {
                R.string.no_internet_connection
            }
            // Everything else - server errors, parsing errors, etc.
            else -> R.string.something_went_wrong_please_try_again
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
fun Bitmap.annotateWithDetections(
    prediction: Prediction,
    context: Context
): Bitmap {
    val annotatedBitmap = this.copy(Bitmap.Config.ARGB_8888, true)
    val canvas = Canvas(annotatedBitmap)

    val boxPaint = Paint().apply {
        color = primaryLight.toArgb()
        style = Paint.Style.STROKE
        strokeWidth = if (prediction.numDetections.toInt() > 10) 4f else 8f // Thinner in dense scenes
        isAntiAlias = true
    }

    val labelBackgroundPaint = Paint().apply {
        color = primaryLight.toArgb()
        style = Paint.Style.FILL
        isAntiAlias = true
        alpha = 220 // Slight transparency for readability
    }

    val labelTextPaint = Paint().apply {
        color = onPrimaryLight.toArgb()
        textSize = max(12f, min(height * 0.05f, 48f))
        isAntiAlias = true
        typeface = Typeface.DEFAULT_BOLD
    }

    val drawnLabels = mutableListOf<RectF>()
    val paddingY = 4f // Reintroduced vertical padding
    val paddingX = 4f // Increased horizontal padding

    prediction.detectionBoxes.slice(0..prediction.numDetections.toInt() - 1)
        .forEachIndexed { index, boundingBox ->
            if (prediction.detectionScores[index] < 0.3) return@forEachIndexed // Skip low-confidence

            val boxLeft = boundingBox[1] * width
            val boxTop = boundingBox[0] * height
            val boxRight = boundingBox[3] * width
            val boxBottom = boundingBox[2] * height
            canvas.drawRect(boxLeft, boxTop, boxRight, boxBottom, boxPaint)

            val labelText = "${context.getString(
                DataSource.cocoLabels[prediction.detectionClasses[index].toInt() - 1]
            )} (${(prediction.detectionScores[index] * 100).toInt()}%)"

            val fontMetrics = labelTextPaint.fontMetrics
            val textHeight = fontMetrics.descent - fontMetrics.ascent
            val textWidth = labelTextPaint.measureText(labelText)
            val labelWidth = textWidth + 2 * paddingX
            val labelHeight = textHeight + 2 * paddingY

            val positions = listOf(
                Triple("above", boxTop - labelHeight - 4f, boxLeft),
                Triple("below", boxBottom + 4f, boxLeft),
                Triple("left", boxTop + (boxBottom - boxTop - labelHeight) / 2, boxLeft - labelWidth - 4f),
                Triple("right", boxTop + (boxBottom - boxTop - labelHeight) / 2, boxRight + 4f),
                Triple("inside", boxTop + (boxBottom - boxTop - labelHeight) / 2, boxLeft + (boxRight - boxLeft - labelWidth) / 2)
            )

            var labelDrawn = false
            for ((_, desiredTop, desiredLeft) in positions) {
                val labelRectTop = max(0f, min(desiredTop, height - labelHeight))
                val labelRectLeft = max(paddingX, min(desiredLeft, width - labelWidth))
                val labelRect = RectF(
                    labelRectLeft,
                    labelRectTop,
                    labelRectLeft + labelWidth,
                    labelRectTop + labelHeight
                )

                val intersects = drawnLabels.any { RectF.intersects(it, labelRect) }
                if (!intersects) {
                    canvas.drawRoundRect(labelRect, 8f, 8f, labelBackgroundPaint)
                    val baselineY = labelRect.top + paddingY - fontMetrics.ascent
                    canvas.drawText(labelText, labelRect.left + paddingX, baselineY, labelTextPaint)
                    drawnLabels.add(labelRect)
                    labelDrawn = true
                    break
                }
            }

            if (!labelDrawn) {
                // Fallback: Draw inside the box
                val labelRectLeft = max(paddingX, min(boxLeft + (boxRight - boxLeft - labelWidth) / 2, width - labelWidth))
                val labelRectTop = max(0f, min(boxTop + (boxBottom - boxTop - labelHeight) / 2, height - labelHeight))
                val labelRect = RectF(
                    labelRectLeft,
                    labelRectTop,
                    labelRectLeft + labelWidth,
                    labelRectTop + labelHeight
                )
                canvas.drawRoundRect(labelRect, 8f, 8f, labelBackgroundPaint)
                val baselineY = labelRect.top + paddingY - fontMetrics.ascent
                canvas.drawText(labelText, labelRect.left + paddingX, baselineY, labelTextPaint)
                drawnLabels.add(labelRect)
            }
        }

    return annotatedBitmap
}