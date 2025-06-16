package com.example.objectdetector.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.dimensionResource
import com.example.objectdetector.R
import com.example.objectdetector.ui.ApiResponse

@Composable
fun PredictionUi(predictions: ApiResponse?, modifier: Modifier = Modifier){
    Box(
        modifier.fillMaxSize()
    )
    {
        when (predictions) {
            is ApiResponse.Loading -> LoadingUi()
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
fun LoadingUi(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                MaterialTheme.colorScheme.scrim.copy(
                    alpha = 0.5f,
                )
            )
    ) {
        LinearProgressIndicator(
            Modifier
                .align(Alignment.Center)
        )
    }
}

@Composable
fun ErrorUi(
    error : String,
    modifier: Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                MaterialTheme.colorScheme.scrim.copy(
                    alpha = 0.5f,
                )
            )
    ) {
        Text(
            text = error,
            color = MaterialTheme.colorScheme.error,
            modifier = modifier
                .padding(
                    horizontal = dimensionResource(R.dimen.small),
                )
                .clip(
                    MaterialTheme.shapes.medium
                )
                .background(
                    MaterialTheme.colorScheme.errorContainer
                )
                .padding(
                    horizontal = dimensionResource(R.dimen.medium),
                    vertical = dimensionResource(R.dimen.small)
                )
        )
    }
}