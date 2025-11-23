package com.yourpackage.skiltscanner

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface VegvesenApi {
    @GET("enkeltoppslag/kjoretoydata")
    suspend fun getVehicleInfo(
        @Query("kjennemerke") licensePlate: String,
        @Header("SVV-Authorization") apiKey: String
    ): VehicleResponse

    companion object {
        private const val BASE_URL = "https://www.vegvesen.no/ws/no/vegvesen/kjoretoy/felles/datautlevering/"

        fun create(): VegvesenApi {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            val client = OkHttpClient.Builder()
                .addInterceptor(logging)
                .build()

            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(VegvesenApi::class.java)
        }
    }
}