package com.example.objectdetector.ui.components

import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.objectdetector.R
import com.example.objectdetector.ui.ApiResponse

@Composable
fun UploadButton(
    currStepIdx: Int,
    onUpload: () -> Unit,
    predictions: ApiResponse?,
    modifier: Modifier = Modifier
) {
    Button(
        enabled = currStepIdx != 0 && predictions !is ApiResponse.Loading,
        onClick = onUpload,
        modifier = modifier
    ) {
        Text(
            text = stringResource(R.string.start_detection),
            style = MaterialTheme.typography.bodyLarge
        )

    }
}