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
    // PEMOTONGAN LORONG FIKTIF: SINKRONISASI PRESISI DENGAN MAIN.PY
    @GET("sinkronisasi")
    suspend fun getSinkronisasi(): HfResponse
}

object NetworkMatriks {
    // Injeksi Asimetri Waktu
    private val klienToleransi = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS) // Deteksi fisis: Jika dalam 10 detik gerbang internet tidak ditemukan, langsung batalkan.
        .readTimeout(60, TimeUnit.SECONDS)    // Toleransi peladen: Jika internet ada, tunggu hingga 60 detik agar HF selesai kompilasi (Cold Start).
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
