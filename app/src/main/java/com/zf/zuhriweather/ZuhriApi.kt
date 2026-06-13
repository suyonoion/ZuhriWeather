package com.zf.zuhriweather

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET

// --- MATRIKS CUACA (OPEN-METEO) ---
data class CuacaResponse(val current_weather: CurrentWeather)
data class CurrentWeather(val temperature: Double, val windspeed: Double)

interface ZuhriApi {
    @GET("v1/forecast?latitude=-6.92&longitude=110.20&current_weather=true")
    suspend fun getKerapatanSpasial(): CuacaResponse
}

object NetworkMatriks {
    val api: ZuhriApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.open-meteo.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ZuhriApi::class.java)
    }
}

// --- MATRIKS RUPTUR LITOSFER (USGS) BARU ---

// 1. Struktur Penampung Kerapatan Seismik
data class UsgsResponse(val features: List<RupturFeature>)
data class RupturFeature(val properties: RupturProperties)
data class RupturProperties(val place: String, val mag: Double)

// 2. Batasan Formal Gerbang Peladen USGS (Mengambil Gempa Signifikan Seminggu Terakhir)
interface UsgsApi {
    @GET("earthquakes/feed/v1.0/summary/significant_week.geojson")
    suspend fun getLedgerZonaMerah(): UsgsResponse
}

// 3. Mesin Transmisi Paralel
object UsgsMatriks {
    val api: UsgsApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://earthquake.usgs.gov/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(UsgsApi::class.java)
    }
}
