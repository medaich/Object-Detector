package com.example.objectdetector.data

import com.example.objectdetector.R
import com.example.objectdetector.model.ApiType

object DataSource {
    val apiList = listOf(
        ApiType(
            label = R.string.rest_api,
            description = R.string.rest_api_description,
            icon = R.drawable.rest_api_icon
        ),
        ApiType(
            label = R.string.grpc_api,
            description = R.string.grpc_api_description,
            icon = R.drawable.grpc_logo
        )
    )

    val uiSteps = listOf(
        R.string.select_image,
        R.string.select_api,
        R.string.start_detection
    )
}