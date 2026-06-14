package com.zf.zuhriweather

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import java.util.concurrent.TimeUnit

// Struktur Penampung Kerapatan Data dari Peladen HF
data class HfResponse(val cuaca: CuacaData, val bencana: BencanaData)
data class CuacaData(val suhu: String, val angin: String)
data class BencanaData(val lokasi: String, val skala: String, val status_bahaya: String, val kode_warna: String)

interface HfApi {
    @GET("spasial/sinkronisasi")
    suspend fun getSinkronisasi(): HfResponse
}

object NetworkMatriks {
    // Injeksi pelebaran dimensi waktu tunggu (60 Detik) untuk melawan Cold Start
    private val klienToleransi = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    val api: HfApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://suyonoion-zuhribackend.hf.space/")
            .client(klienToleransi)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(HfApi::class.java)
    }
}
