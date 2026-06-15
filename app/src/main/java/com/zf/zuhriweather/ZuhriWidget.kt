package com.zf.zuhriweather

import android.content.Context
import android.appwidget.AppWidgetManager
import androidx.work.*
import java.util.concurrent.TimeUnit
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ZuhriWidget : GlanceAppWidget() {
    
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // Mengambil paksa matriks data dari penyimpanan internal fisis (SharedPreferences)
        val pref = context.getSharedPreferences("ZF_STORAGE", Context.MODE_PRIVATE)
        
        val suhu = pref.getString("suhu", "-") ?: "-"
        val angin = pref.getString("angin", "-") ?: "-"
        val lokasi = pref.getString("lokasi", "Menunggu Transmisi...") ?: "Menunggu Transmisi..."
        val skala = pref.getString("skala", "-") ?: "-"
        val status = pref.getString("status", "Standby") ?: "Standby"
        val kodeWarna = pref.getString("warna", "Gray") ?: "Gray"
        
        val warnaStatus = when(kodeWarna) {
            "Red" -> ColorProvider(Color.Red)
            "Orange" -> ColorProvider(Color(0xFFFFA500))
            "Yellow" -> ColorProvider(Color.Yellow)
            "Green" -> ColorProvider(Color.Green)
            else -> ColorProvider(Color.Gray)
        }

        provideContent {
            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(ColorProvider(Color(0xFF212121)))
                    .padding(12.dp)
            ) {
                MatriksVisualPublik(suhu, angin, lokasi, skala, status, warnaStatus)
            }
        }
    }

    @Composable
    private fun MatriksVisualPublik(
        suhu: String, angin: String, lokasi: String, skala: String, status: String, warna: ColorProvider
    ) {
        Column {
            Text(text = "CUACA LOKAL (KENDAL)", style = TextStyle(color = ColorProvider(Color.Cyan)))
            Text(text = "Suhu: $suhu | Angin: $angin", style = TextStyle(color = ColorProvider(Color.White)))
            
            Spacer(modifier = GlanceModifier.padding(4.dp))
            
            Text(
                text = "↻ PERBARUI DATA",
                modifier = GlanceModifier.clickable(onClick = actionRunCallback<SegarkanMatriksAction>()),
                style = TextStyle(color = ColorProvider(Color.LightGray))
            )

            Spacer(modifier = GlanceModifier.padding(6.dp))

            Text(text = "INFO GEMPA TERBARU", style = TextStyle(color = ColorProvider(Color.Red)))
            Text(text = lokasi, style = TextStyle(color = ColorProvider(Color.White)))
            
            Row {
                Text(text = "$skala | ", style = TextStyle(color = ColorProvider(Color.White)))
                Text(text = status, style = TextStyle(color = warna))
            }
        }
    }
}

class SegarkanMatriksAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val pref = context.getSharedPreferences("ZF_STORAGE", Context.MODE_PRIVATE)
        
        // Fase 1: Pahat Status Memuat ke Penyimpanan Fisis (Aman dari Low-RAM Killer)
        pref.edit().apply {
            putString("suhu", "Siklus...")
            putString("angin", "Koneksi...")
            putString("lokasi", "Mengekstrak Litosfer...")
            putString("skala", "-")
            putString("status", "Proses")
            putString("warna", "Yellow")
            apply()
        }
        ZuhriWidget().update(context, glanceId)

        // Fase 2: Pengeboran Jaringan Internasional menuju Hugging Face
        try {
            val respons = withContext(Dispatchers.IO) {
                NetworkMatriks.api.getSinkronisasi()
            }
            
            // Pahat Data Matang ke Penyimpanan Fisis
            pref.edit().apply {
                putString("suhu", respons.cuaca.suhu)
                putString("angin", respons.cuaca.angin)
                putString("lokasi", respons.bencana.lokasi)
                putString("skala", respons.bencana.skala)
                putString("status", respons.bencana.status_bahaya)
                putString("warna", respons.bencana.kode_warna)
                apply()
            }
            
        } catch (e: java.net.UnknownHostException) {
            pref.edit().apply {
                putString("angin", "Ruptur Sinyal")
                putString("lokasi", "Gerbang Jaringan Putus")
                putString("status", "Offline")
                putString("warna", "Red")
                apply()
            }
        } catch (e: java.net.SocketTimeoutException) {
            pref.edit().apply {
                putString("angin", "Peladen Bangun")
                putString("lokasi", "Klik [PERBARUI DATA] Sekali Lagi")
                putString("status", "Timeout")
                putString("warna", "Orange")
                apply()
            }
        } catch (e: Exception) {
            pref.edit().apply {
                putString("angin", "Distorsi")
                putString("lokasi", "Kegagalan Fisis")
                putString("status", "Error")
                putString("warna", "Red")
                apply()
            }
        }

        // Fase 3: Rekonstruksi Visual Akhir secara Instan dari SharedPreferences
        ZuhriWidget().update(context, glanceId)
    }
}

class ZuhriWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ZuhriWidget()

    // Eksekusi Absolut saat Widget diaktifkan di layar
    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        
        // Mengunci siklus pembaruan otonom setiap 15 Menit
        val siklusOtomatis = PeriodicWorkRequestBuilder<ZuhriWorker>(15, TimeUnit.MINUTES).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "ZF_Sinkronisasi_Satelit",
            ExistingPeriodicWorkPolicy.KEEP,
            siklusOtomatis
        )
    }

    // Eksekusi Pemuatan Instan saat Widget diletakkan secara fisis
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        
        val inisiasiAwal = OneTimeWorkRequestBuilder<ZuhriWorker>().build()
        WorkManager.getInstance(context).enqueue(inisiasiAwal)
    }

    // Pembersihan Residu saat Widget dihapus dari layar
    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        WorkManager.getInstance(context).cancelUniqueWork("ZF_Sinkronisasi_Satelit")
    }
}

