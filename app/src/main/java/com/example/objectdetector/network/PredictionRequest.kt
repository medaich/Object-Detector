package com.example.objectdetector.network

import kotlinx.serialization.Serializable

@Serializable
data class PredictionRequest(
    val instances: List<List<List<List<Int>>>>
)
