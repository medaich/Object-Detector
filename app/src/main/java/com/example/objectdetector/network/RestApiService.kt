package com.example.objectdetector.network

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.POST

private val BASE_URL = "https://mobilenet-ssd-api-879557471105.europe-southwest1.run.app"

private val retrofit = Retrofit.Builder()
    .addConverterFactory(
        Json.asConverterFactory("application/json".toMediaType())
    )
    .baseUrl(BASE_URL)
    .build()

interface RestApiService {
    @POST("v1/models/mobilenet_ssd:predict")
    suspend fun predict(@Body request: PredictionRequest): PredictionResponse
}

object restApi {
    val retrofitService : RestApiService by lazy {
        retrofit.create(RestApiService::class.java)
    }
}