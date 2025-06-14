package com.example.objectdetector.network

import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tensorflow.framework.DataType
import org.tensorflow.framework.TensorProto
import org.tensorflow.framework.TensorShapeProto
import tensorflow.serving.Model.ModelSpec
import tensorflow.serving.Predict.PredictRequest
import tensorflow.serving.Predict.PredictResponse
import tensorflow.serving.PredictionServiceGrpcKt
import java.util.concurrent.TimeUnit

/**
 * gRPC client service for TensorFlow Serving object detection predictions.
 * Provides type-safe interface for communicating with MobileNet SSD model on Cloud Run.
 */
class GrpcApiService {
    companion object {
        private const val HOST = "mobilenet-ssd-grpc-879557471105.europe-southwest1.run.app"
        private const val PORT = 443
        private const val MODEL_NAME = "mobilenet_ssd"
        private const val SIGNATURE_NAME = "serving_default"
        // Unified timeout configuration - matches AppViewModel
        private const val REQUEST_TIMEOUT_SECONDS = 30L
        private const val MAX_MESSAGE_SIZE = 16 * 1024 * 1024 // 16MB
    }
    
    private val channel: ManagedChannel by lazy {
        ManagedChannelBuilder
            .forAddress(HOST, PORT)
            .useTransportSecurity()
            .keepAliveTime(30, TimeUnit.SECONDS)
            .keepAliveTimeout(10, TimeUnit.SECONDS)
            .keepAliveWithoutCalls(true)
            .maxInboundMessageSize(MAX_MESSAGE_SIZE)
            .maxInboundMetadataSize(MAX_MESSAGE_SIZE)
            .idleTimeout(60, TimeUnit.SECONDS)
            .userAgent("AndroidObjectDetector/1.0")
            .overrideAuthority(HOST)
            .build()
    }

    private val stub: PredictionServiceGrpcKt.PredictionServiceCoroutineStub by lazy {
        PredictionServiceGrpcKt.PredictionServiceCoroutineStub(channel)
    }

    /**
     * Performs object detection prediction on the provided image array.
     * 
     * @param imageArray 3D array representing image pixels [height][width][channels]
     * @return PredictionResponse containing detection results
     * @throws Exception if prediction fails after retries
     */
    suspend fun predict(imageArray: Array<Array<Array<Int>>>): PredictionResponse {
        return withContext(Dispatchers.IO) {
            val inputTensor = createTensorProto(imageArray)
            val request = buildPredictRequest(inputTensor)
            val grpcResponse = performPrediction(request)
            convertGrpcResponseToRest(grpcResponse)
        }
    }

    private suspend fun performPrediction(request: PredictRequest): PredictResponse {
        return stub.withDeadlineAfter(REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .predict(request)
    }

    private fun buildPredictRequest(inputTensor: TensorProto): PredictRequest {
        return PredictRequest.newBuilder()
            .setModelSpec(
                ModelSpec.newBuilder()
                    .setName(MODEL_NAME)
                    .setSignatureName(SIGNATURE_NAME)
                    .build()
            )
            .putInputs("inputs", inputTensor)
            .addOutputFilter("num_detections")
            .addOutputFilter("detection_boxes")
            .addOutputFilter("detection_classes")
            .addOutputFilter("detection_scores")
            .build()
    }

    private fun convertGrpcResponseToRest(grpcResponse: PredictResponse): PredictionResponse {
        val outputs = grpcResponse.outputsMap

        val numDetections = outputs["num_detections"]?.floatValList?.firstOrNull() ?: 0f
        val detectionClasses = outputs["detection_classes"]?.floatValList?.toList() ?: emptyList()
        val detectionScores = outputs["detection_scores"]?.floatValList?.toList() ?: emptyList()
        
        val detectionBoxes = convertDetectionBoxes(
            outputs["detection_boxes"]?.floatValList?.toList() ?: emptyList()
        )

        val prediction = Prediction(
            numDetections = numDetections,
            detectionClasses = detectionClasses,
            detectionScores = detectionScores,
            detectionBoxes = detectionBoxes
        )

        return PredictionResponse(predictions = listOf(prediction))
    }

    private fun convertDetectionBoxes(flatBoxes: List<Float>): List<List<Float>> {
        val detectionBoxes = mutableListOf<List<Float>>()
        
        for (i in flatBoxes.indices step 4) {
            if (i + 3 < flatBoxes.size) {
                detectionBoxes.add(
                    listOf(
                        flatBoxes[i],     // y1
                        flatBoxes[i + 1], // x1
                        flatBoxes[i + 2], // y2
                        flatBoxes[i + 3]  // x2
                    )
                )
            }
        }
        
        return detectionBoxes
    }

    private fun createTensorProto(imageArray: Array<Array<Array<Int>>>): TensorProto {
        val height = imageArray.size
        val width = imageArray[0].size
        val channels = imageArray[0][0].size

        val tensorShape = TensorShapeProto.newBuilder()
            .addDim(TensorShapeProto.Dim.newBuilder().setSize(1)) // batch size
            .addDim(TensorShapeProto.Dim.newBuilder().setSize(height.toLong()))
            .addDim(TensorShapeProto.Dim.newBuilder().setSize(width.toLong()))
            .addDim(TensorShapeProto.Dim.newBuilder().setSize(channels.toLong()))
            .build()
        
        val tensorBuilder = TensorProto.newBuilder()
            .setDtype(DataType.DT_UINT8)
            .setTensorShape(tensorShape)

        // Flatten array in TensorFlow expected order: height, width, channels
        for (y in 0 until height) {
            for (x in 0 until width) {
                tensorBuilder.addIntVal(imageArray[y][x][0]) // Red
                tensorBuilder.addIntVal(imageArray[y][x][1]) // Green  
                tensorBuilder.addIntVal(imageArray[y][x][2]) // Blue
            }
        }
        
        return tensorBuilder.build()
    }
    
    /**
     * Gracefully shuts down the gRPC channel.
     * Should be called when the service is no longer needed.
     */
    fun shutdown() {
        if (!channel.isShutdown) {
            channel.shutdown()
        }
    }
}

object grpcApi {
    val service: GrpcApiService by lazy {
        GrpcApiService()
    }
}
