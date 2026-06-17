package com.zf.zuhriweather

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import java.util.concurrent.TimeUnit

// STRUKTUR PENAMPUNG KERAPATAN DATA EVOLUSI ZF
data class HfResponse(
    val cuaca: CuacaData, 
    val bencana: BencanaData,
    val data_domestik: List<MatriksAnomaliNetwork>,
    val data_global: List<MatriksAnomaliNetwork>
)

data class CuacaData(
    val suhu: String, 
    val angin: String,
    val kelembapan: String,
    val awan: String,
    val presipitasi: String
)

data class BencanaData(
    val lokasi: String, 
    val skala: String, 
    val status_bahaya: String, 
    val kode_warna: String
)

// Penampung Array Litosfer dari Peladen Awan
data class MatriksAnomaliNetwork(
    val negara: String,
    val entitas: String,
    val jenis: String,
    val probabilitas: String,
    val skala: String,
    val bahaya: String,
    val waktu: String,
    val warna_kode: String
)

interface HfApi {
    @GET("sinkronisasi")
    suspend fun getSinkronisasi(): HfResponse
}

object NetworkMatriks {
    private val klienToleransi = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS) 
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
