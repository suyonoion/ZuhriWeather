package com.zf.zuhriweather

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.RingtoneManager
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
            
            val lokasi = respons.bencana.lokasi
            val skala = respons.bencana.skala
            val status = respons.bencana.status_bahaya
            val kodeWarna = respons.bencana.kode_warna

            // 1. Memahat memori lokal untuk sinkronisasi visual Widget/Aplikasi
            pref.edit().apply {
                putString("suhu", respons.cuaca.suhu)
                putString("angin", respons.cuaca.angin)
                putString("lokasi", lokasi)
                putString("skala", skala)
                putString("status", status)
                putString("warna", kodeWarna)
                apply()
            }
            ZuhriWidget().updateAll(appContext)

            // 2. FILTER 1: Anti-Spam (Hanya proses jika ini adalah EVENT BARU)
            val tandaTanganEventId = lokasi + skala
            val terakhirNotifikasi = pref.getString("LAST_NOTIFIED_EVENT", "")
            
            if (tandaTanganEventId == terakhirNotifikasi) {
                // Event sama dengan 15 menit lalu. Hentikan eksekusi, cegah spam.
                return Result.success()
            }

            // 3. FILTER 2: Batasan Geografis (Hanya proses jika berdampak pada Indonesia)
            val IsIndonesia = lokasi.contains("Indonesia", ignoreCase = true) || 
                              lokasi.contains("Java", ignoreCase = true) || 
                              lokasi.contains("Kendal", ignoreCase = true)

            if ((kodeWarna == "Red" || kodeWarna == "Orange") && IsIndonesia) {
                
                // 4. FILTER 3: Eskalasi Audio Berdasarkan Jarak Deteksi Text
                val IsKritisLokal = lokasi.contains("Kendal", ignoreCase = true) || 
                                    lokasi.contains("Jawa Tengah", ignoreCase = true) ||
                                    lokasi.contains("Java", ignoreCase = true)

                if (IsKritisLokal) {
                    // Krisis Radius Dekat: Tembak Alarm Sirine Agresif
                    tembakAlarmKritisLokal(lokasi, skala, status)
                } else {
                    // Domestik Jauh (Misal: Sumatra/Sulawesi): Cukup Notifikasi Standar
                    tembakNotifikasiDomestik(lokasi, skala, status)
                }

                // Mengunci tanda tangan agar tidak berbunyi lagi pada siklus 15 menit berikutnya
                pref.edit().putString("LAST_NOTIFIED_EVENT", tandaTanganEventId).apply()
            }
            
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    // Protokol 1: Alarm Audio Agresif (Menggunakan Ringtone ALARM Sistem, Bukan Chime Notifikasi)
    private fun tembakAlarmKritisLokal(lokasi: String, skala: String, status: String) {
        val manager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val saluranId = "zf_alarm_kritis_lokal"
        val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM) // Paksa panggil audio sirine HP

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val saluran = NotificationChannel(
                saluranId,
                "🚨 ALARM KRITIS LOKAL (ZUHRI)",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                enableVibration(true)
                // Mengunci jalur audio agar sistem operasi meloloskan suara sirine penuh
                setSound(alarmUri, AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build())
            }
            manager.createNotificationChannel(saluran)
        }

        val peluruAlarm = NotificationCompat.Builder(appContext, saluranId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("🚨 ANCAMAN DEKAT: RUPTUR LITOSFER")
            .setContentText("Lokasi: $lokasi | Mag: $skala | $status")
            .setPriority(NotificationCompat.PRIORITY_MAX) // Kasta tertinggi Android UI
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setSound(alarmUri)
            .setFullScreenIntent(null, true) // Memaksa menembus layar jika HP terkunci
            .setAutoCancel(true)

        manager.notify(100, peluruAlarm.build())
    }

    // Protokol 2: Notifikasi Standar untuk Wilayah Domestik Luar Jangkauan Bahaya Fisis Kendal
    private fun tembakNotifikasiDomestik(lokasi: String, skala: String, status: String) {
        val manager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val saluranId = "zf_notifikasi_domestik"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val saluran = NotificationChannel(
                saluranId,
                "Info Gempa Nasional",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            manager.createNotificationChannel(saluran)
        }

        val peluruNotif = NotificationCompat.Builder(appContext, saluranId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("⚠️ Info Gempa Domestik")
            .setContentText("Kordinat: $lokasi | Mag: $skala")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        manager.notify(101, peluruNotif.build())
    }
}
