package com.zf.zuhriweather

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

// --- STRUKTUR PENAMPUNG MATRIKS RAKSASA DYNAMIC ZF ---
data class HfResponse(
    val meta_lokasi: String, // Menangkap nama lokasi dinamis dari peladen
    val cuaca: CuacaData,
    val proyeksi_cuaca: ProyeksiCuacaData,
    val bencana: BencanaData,
    val data_domestik: List<MatriksAnomaliNetwork>,
    val data_global: List<MatriksAnomaliNetwork>
)

data class CuacaData(val suhu: String, val angin: String, val kelembapan: String, val awan: String, val presipitasi: String)
data class ProyeksiCuacaData(val per_jam: List<ProyeksiJam>, val harian: List<ProyeksiHari>)
data class ProyeksiJam(val waktu: String, val suhu: String, val probabilitas_hujan: String)
data class ProyeksiHari(val hari: String, val suhu_max: String, val suhu_min: String, val prob_hujan: String)
data class BencanaData(val lokasi: String, val skala: String, val status_bahaya: String, val kode_warna: String)

data class MatriksAnomaliNetwork(
    val negara: String, val entitas: String, val jenis: String, 
    val probabilitas: String, val skala: String, val bahaya: String, 
    val waktu: String, val warna_kode: String
)

interface HfApi {
    @GET("sinkronisasi")
    suspend fun getSinkronisasi(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("lokasi_nama") lokasiNama: String
    ): HfResponse
}

object NetworkMatriks {
    private val klienToleransi = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
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
