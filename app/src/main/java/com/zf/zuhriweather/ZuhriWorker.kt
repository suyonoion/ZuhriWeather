package com.zf.zuhriweather

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ZuhriWorker(private val appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val pref = appContext.getSharedPreferences("ZF_STORAGE", Context.MODE_PRIVATE)
        
        return try {
            // Pengeboran Jaringan Otonom
            val respons = withContext(Dispatchers.IO) {
                NetworkMatriks.api.getSinkronisasi()
            }
            
            // Memahat Data Fisis ke Penyimpanan
            pref.edit().apply {
                putString("suhu", respons.cuaca.suhu)
                putString("angin", respons.cuaca.angin)
                putString("lokasi", respons.bencana.lokasi)
                putString("skala", respons.bencana.skala)
                putString("status", respons.bencana.status_bahaya)
                putString("warna", respons.bencana.kode_warna)
                apply()
            }
            
            // Memaksa Widget di Layar untuk Menggambar Ulang Dirinya
            ZuhriWidget().updateAll(appContext)
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
