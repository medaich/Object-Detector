package com.example.objectdetector.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.objectdetector.ui.ApiResponse

@Composable
fun PredictionUi(predictions: ApiResponse?, modifier: Modifier = Modifier){
    Box(
        modifier.fillMaxSize()
    )
    {
        when (predictions) {
            is ApiResponse.Loading -> LoadingUi(
                Modifier.align(
                    Alignment.Center
                )
            )
            is ApiResponse.Error -> ErrorUi(
                predictions.message.toString(),
                Modifier.align(
                Alignment.Center
                )
            )
            is ApiResponse.Success -> if (predictions.data.predictions.first().numDetections.toInt() == 0)
                ErrorUi("No objects detected", Modifier.align(
                    Alignment.Center
                    )
                )
            else -> {}
        }
    }
}

@Composable
fun LoadingUi(modifier: Modifier) {
    LinearProgressIndicator(
        modifier = modifier
    )
}

@Composable
fun ErrorUi(
    error : String,
    modifier: Modifier
) {
    Text(
        text = error,
        color = MaterialTheme.colorScheme.error,
        modifier = modifier
    )


}