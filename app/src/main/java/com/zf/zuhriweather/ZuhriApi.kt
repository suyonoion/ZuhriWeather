package com.zf.zuhriweather

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET

// 1. Struktur Penampung Kerapatan Informasi (Suhu & Angin)
data class CuacaResponse(val current_weather: CurrentWeather)
data class CurrentWeather(val temperature: Double, val windspeed: Double)

// 2. Definisi Batasan Formal Gerbang Peladen
interface ZuhriApi {
    @GET("v1/forecast?latitude=-6.92&longitude=110.20&current_weather=true")
    suspend fun getKerapatanSpasial(): CuacaResponse
}

// 3. Mesin Transmisi Absolut
object NetworkMatriks {
    val api: ZuhriApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.open-meteo.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ZuhriApi::class.java)
    }
}
