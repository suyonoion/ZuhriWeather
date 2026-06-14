package com.zf.zuhriweather

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET

// Struktur Penampung Kerapatan Data dari Peladen HF Anda
data class HfResponse(val cuaca: CuacaData, val bencana: BencanaData)
data class CuacaData(val suhu: String, val angin: String)
data class BencanaData(val lokasi: String, val skala: String, val status_bahaya: String, val kode_warna: String)

interface HfApi {
    // Menembak langsung ke Ruang Brankas Spasial
    @GET("spasial/sinkronisasi")
    suspend fun getSinkronisasi(): HfResponse
}

object NetworkMatriks {
    val api: HfApi by lazy {
        Retrofit.Builder()
            // Tautan Absolut Peladen Awan Anda
            .baseUrl("https://suyonoion-zuhribackend.hf.space/") 
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(HfApi::class.java)
    }
}
