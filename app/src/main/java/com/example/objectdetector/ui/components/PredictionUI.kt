package com.example.objectdetector.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.objectdetector.data.DataSource
import com.example.objectdetector.network.PredictionResponse
import com.example.objectdetector.ui.RestResponse

@Composable
fun PredictionUi(predictions: RestResponse?, modifier: Modifier = Modifier){
    Box(
        modifier.fillMaxSize()
    )
    {
        when (predictions) {
            is RestResponse.Loading -> LoadingUi(
                Modifier.align(
                    Alignment.Center
                )
            )
            is RestResponse.Error -> ErrorUi(predictions.message.toString() ,Modifier)
            is RestResponse.Success -> SuccessUi(predictions.data, Modifier)
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

@Composable
fun SuccessUi(
    predictions: PredictionResponse,
    modifier: Modifier
) {
    val prediction = predictions.predictions.first()
    val numDetections = prediction.numDetections.toInt()
    if (numDetections == 0) ErrorUi("no object detected", Modifier)
    else {
        Column {
            for (i in 1..numDetections) {
                val detectionClass = prediction.detectionClasses[i - 1].toInt()
                val detectionScore = prediction.detectionScores[i - 1]
                val detectionBox = prediction.detectionBoxes[i - 1]

                Text(
                    text = "${detectionScore*100}% ${stringResource(DataSource.cocoLabels[detectionClass - 1])} class: $detectionClass",
                    color = MaterialTheme.colorScheme.primary ,
                    modifier = modifier
                )
            }
        }
    }
    
}