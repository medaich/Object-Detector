package com.example.objectdetector.ui

import ImageCard
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.objectdetector.R
import com.example.objectdetector.data.DataSource
import com.example.objectdetector.ui.components.ApiSelector
import com.example.objectdetector.ui.components.StepsIndicator
import com.example.objectdetector.ui.components.UploadButton

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AppUiScreen (
    modifier: Modifier = Modifier,
    appViewModel: AppViewModel = viewModel()
) {
    val context = LocalContext.current
    val primaryColor = MaterialTheme.colorScheme.primary
    val onPrimaryColor = MaterialTheme.colorScheme.onPrimary

    val apiList = DataSource.apiList
    val appUiState by appViewModel.uiState.collectAsState()

    val uiSteps = DataSource.uiSteps

    Column (
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.extra_large)),
        modifier = modifier
            .padding(dimensionResource(R.dimen.large))
    ) {
        StepsIndicator(
            steps = uiSteps,
            currStepIdx= appUiState.currStepIdx,
            modifier = Modifier.fillMaxWidth()
        )
        ImageCard(
            currImageBitmap = appUiState.annotatedBitmap ?: appUiState.currImageBitmap,
            onUriSelected = { uri, context -> appViewModel.onUriSelected(uri, context) },
            predictions = appUiState.apiResponse,
            modifier = Modifier
        )
        ApiSelector(
            currApiIdx = appUiState.currApiIdx,
            apiList = apiList,
            onApiSelected = {
                appViewModel.onSelectApi(it)
            }, modifier = Modifier
        )
        UploadButton(
            currStepIdx = appUiState.currStepIdx,
            predictions = appUiState.apiResponse,
            onUpload = { appViewModel.onUpload(context, primaryColor, onPrimaryColor) }
        )
    }
}
