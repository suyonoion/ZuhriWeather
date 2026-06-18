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
            val lat = pref.getFloat("last_lat", -6.9535f).toDouble()
            val lon = pref.getFloat("last_lon", 110.2312f).toDouble()
            val lokasiNama = pref.getString("meta_lokasi", "Blorok, Kendal (Worker)") ?: "Blorok, Kendal"

            val respons = NetworkMatriks.api.getSinkronisasi(lat, lon, lokasiNama)
            
            val lokasi = respons.bencana.lokasi
            val skala = respons.bencana.skala
            val status = respons.bencana.status_bahaya
            val kodeWarna = respons.bencana.kode_warna

            // 1. Memahat memori lokal untuk sinkronisasi visual Widget/Aplikasi
            pref.edit().apply {
                putString("meta_lokasi", respons.meta_lokasi)
                putString("suhu", respons.cuaca.suhu)
                putString("angin", respons.cuaca.angin)
                putString("kelembapan", respons.cuaca.kelembapan)
                putString("awan", respons.cuaca.awan)
                putString("presipitasi", respons.cuaca.presipitasi)
                putString("lokasi", lokasi)
                putString("skala", skala)
                putString("status", status)
                putString("warna", kodeWarna)
                apply()
            }
            ZuhriWidget().updateAll(appContext)

            // ================= PROTOKOL 1 & 2: ANOMALI LITOSFER (GEMPA) ================= //
            
            val tandaTanganEventId = lokasi + skala
            val terakhirNotifikasi = pref.getString("LAST_NOTIFIED_EVENT", "")
            
            if (tandaTanganEventId != terakhirNotifikasi) {
                val IsIndonesia = lokasi.contains("Indonesia", ignoreCase = true) || 
                                  lokasi.contains("Java", ignoreCase = true) || 
                                  lokasi.contains("Kendal", ignoreCase = true)

                if ((kodeWarna == "Red" || kodeWarna == "Orange") && IsIndonesia) {
                    val IsKritisLokal = lokasi.contains("Kendal", ignoreCase = true) || 
                                        lokasi.contains("Jawa Tengah", ignoreCase = true) ||
                                        lokasi.contains("Java", ignoreCase = true)

                    if (IsKritisLokal) {
                        tembakAlarmKritisLokal(lokasi, skala, status)
                    } else {
                        tembakNotifikasiDomestik(lokasi, skala, status)
                    }
                    pref.edit().putString("LAST_NOTIFIED_EVENT", tandaTanganEventId).apply()
                }
            }

            // ================= PROTOKOL 3: ANOMALI TERMODINAMIKA (CUACA EKSTREM) ================= //
            
            val presipitasi = respons.cuaca.presipitasi
            val angkaHujan = presipitasi.replace(" mm/j", "").replace("[Deras]", "").trim().toDoubleOrNull() ?: 0.0
            
            // Pelatuk: Jika ada label "[Deras]" atau debit air lebih dari 5.0 mm/j
            val isHujanEkstrem = presipitasi.contains("Deras", ignoreCase = true) || angkaHujan >= 5.0
            
            if (isHujanEkstrem) {
                val tandaTanganCuaca = "CUACA_$presipitasi"
                val terakhirNotifCuaca = pref.getString("LAST_NOTIFIED_WEATHER", "")
                
                // Mencegah tembakan notifikasi berulang untuk status hujan yang sama dalam 15 menit
                if (tandaTanganCuaca != terakhirNotifCuaca) {
                    tembakNotifikasiCuacaEkstrem(lokasiNama, respons.cuaca.suhu, presipitasi)
                    pref.edit().putString("LAST_NOTIFIED_WEATHER", tandaTanganCuaca).apply()
                }
            } else {
                // Reset memori cuaca jika hujan reda, agar siap menembak lagi jika hujan deras kembali turun
                pref.edit().putString("LAST_NOTIFIED_WEATHER", "AMAN").apply()
            }
            
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    // Protokol 1: Alarm Audio Agresif (Gempa Lokal)
    private fun tembakAlarmKritisLokal(lokasi: String, skala: String, status: String) {
        val manager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val saluranId = "zf_alarm_kritis_lokal"
        val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val saluran = NotificationChannel(saluranId, "🚨 ALARM KRITIS LOKAL", NotificationManager.IMPORTANCE_HIGH).apply {
                enableVibration(true)
                setSound(alarmUri, AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
            }
            manager.createNotificationChannel(saluran)
        }

        val peluruAlarm = NotificationCompat.Builder(appContext, saluranId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("🚨 ANCAMAN DEKAT: RUPTUR LITOSFER")
            .setContentText("Lokasi: $lokasi | Mag: $skala | $status")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setSound(alarmUri)
            .setFullScreenIntent(null, true)
            .setAutoCancel(true)

        manager.notify(100, peluruAlarm.build())
    }

    // Protokol 2: Notifikasi Standar (Gempa Domestik)
    private fun tembakNotifikasiDomestik(lokasi: String, skala: String, status: String) {
        val manager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val saluranId = "zf_notifikasi_domestik"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val saluran = NotificationChannel(saluranId, "Info Gempa Nasional", NotificationManager.IMPORTANCE_DEFAULT)
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

    // Protokol 3: Notifikasi Cuaca Ekstrem (Hujan Deras / Badai)
    private fun tembakNotifikasiCuacaEkstrem(lokasi: String, suhu: String, presipitasi: String) {
        val manager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val saluranId = "zf_notifikasi_cuaca"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val saluran = NotificationChannel(saluranId, "Peringatan Cuaca Ekstrem", NotificationManager.IMPORTANCE_HIGH)
            manager.createNotificationChannel(saluran)
        }

        val peluruNotif = NotificationCompat.Builder(appContext, saluranId)
            .setSmallIcon(android.R.drawable.ic_dialog_alert) // Menggunakan ikon peringatan bawaan Android
            .setContentTitle("🌧️ PERINGATAN: PRESIPITASI EKSTREM")
            .setContentText("Target: $lokasi | Intensitas: $presipitasi | Suhu: $suhu")
            .setPriority(NotificationCompat.PRIORITY_HIGH) // Prioritas tinggi agar muncul di atas layar (Heads-up)
            .setAutoCancel(true)

        manager.notify(102, peluruNotif.build())
    }
}
