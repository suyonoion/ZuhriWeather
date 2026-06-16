package com.zf.zuhriweather

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
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
            val respons = withContext(Dispatchers.IO) {
                NetworkMatriks.api.getSinkronisasi()
            }
            
            // Pahat memori lokal
            pref.edit().apply {
                putString("suhu", respons.cuaca.suhu)
                putString("angin", respons.cuaca.angin)
                putString("lokasi", respons.bencana.lokasi)
                putString("skala", respons.bencana.skala)
                putString("status", respons.bencana.status_bahaya)
                putString("warna", respons.bencana.kode_warna)
                apply()
            }
            
            // Evaluasi Fisis: Pelatuk Notifikasi Darurat
            // Jika peladen Barat mendeteksi ruptur merusak (Merah) atau siaga (Oranye)
            if (respons.bencana.kode_warna == "Red" || respons.bencana.kode_warna == "Orange") {
                tembakNotifikasiDarurat(
                    respons.bencana.lokasi, 
                    respons.bencana.skala, 
                    respons.bencana.status_bahaya
                )
            }
            
            // Bergetar ke Widget pasif
            ZuhriWidget().updateAll(appContext)
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private fun tembakNotifikasiDarurat(lokasi: String, skala: String, status: String) {
        val manager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val saluranId = "zf_darurat_litosfer"

        // Menembus batasan Android Oreo ke atas (Wajib membuat saluran spesifik)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val saluran = NotificationChannel(
                saluranId,
                "Peringatan Dini Bencana",
                NotificationManager.IMPORTANCE_HIGH // Interupsi Absolut: Paksa bunyi dan pop-up
            ).apply {
                enableVibration(true)
            }
            manager.createNotificationChannel(saluran)
        }

        val peluruNotifikasi = NotificationCompat.Builder(appContext, saluranId)
            .setSmallIcon(android.R.drawable.ic_dialog_alert) // Ikon peringatan bawaan sistem
            .setContentTitle("⚠️ RUPTUR LITOSFER DETEKSI")
            .setContentText("Kordinat: $lokasi | Mag: $skala | $status")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL) // Aktifkan getaran dan suara default maksimal
            .setAutoCancel(true)

        manager.notify(99, peluruNotifikasi.build())
    }
}
