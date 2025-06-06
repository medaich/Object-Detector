package com.example.objectdetector.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PredictionResponse(
    val predictions : List<Prediction>
)

@Serializable
data class Prediction (
    @SerialName("num_detections")
    val numDetections: Float,
    @SerialName("detection_classes")
    val detectionClasses: List<Float>,
    @SerialName("detection_scores")
    val detectionScores: List<Float>,
    @SerialName("detection_boxes")
    val detectionBoxes: List<List<Float>>
)