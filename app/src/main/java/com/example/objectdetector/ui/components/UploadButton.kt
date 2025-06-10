package com.example.objectdetector.ui.components

import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.objectdetector.R
import com.example.objectdetector.ui.RestResponse

@Composable
fun UploadButton(
    currStepIdx: Int,
    onUpload: () -> Unit,
    predictions: RestResponse?,
    modifier: Modifier = Modifier
) {
    Button(
        enabled = currStepIdx != 0 && predictions !is RestResponse.Loading,
        onClick = onUpload,
        modifier = modifier
    ) {
        Text(
            text = stringResource(R.string.start_detection),
            style = MaterialTheme.typography.bodyLarge
        )

    }
}