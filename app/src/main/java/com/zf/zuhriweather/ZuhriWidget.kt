package com.zf.zuhriweather

import android.content.Context
import android.appwidget.AppWidgetManager
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.updateAll
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.work.*
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ZuhriWidget : GlanceAppWidget() {
    
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val pref = context.getSharedPreferences("ZF_STORAGE", Context.MODE_PRIVATE)
            
            val suhu = pref.getString("suhu", "-") ?: "-"
            val angin = pref.getString("angin", "-") ?: "-"
            val kelembapan = pref.getString("kelembapan", "-") ?: "-"
            val awan = pref.getString("awan", "-") ?: "-"
            val presipitasi = pref.getString("presipitasi", "-") ?: "-"
            
            val metaLokasi = pref.getString("meta_lokasi", "Menunggu Transmisi...") ?: "Menunggu Transmisi..."
            val lokasi = pref.getString("lokasi", "Menelusuri Gempa...") ?: "Menelusuri Gempa..."
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

            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(ColorProvider(Color.Transparent))
                    .padding(4.dp)
            ) {
                MatriksVisualPublik(suhu, angin, kelembapan, awan, presipitasi, metaLokasi, lokasi, skala, status, warnaStatus)
            }
        }
    }

    @Composable
    private fun MatriksVisualPublik(
        suhu: String, angin: String, kelembapan: String, awan: String, presipitasi: String,
        metaLokasi: String, lokasi: String, skala: String, status: String, warna: ColorProvider
    ) {
        val angkaHujan = presipitasi.replace(" mm/j", "").replace("[Deras]", "").trim().toDoubleOrNull() ?: 0.0
        val angkaAwan = awan.replace("%", "").trim().toIntOrNull() ?: 0
        
        val (ikonCuaca, teksCuaca) = when {
            angkaHujan > 0.0 -> "🌧️" to "Hujan"
            angkaAwan >= 60 -> "☁️" to "Berawan"
            angkaAwan >= 20 -> "⛅" to "Cerah Berawan"
            else -> "☀️" to "Cerah"
        }

        Column(modifier = GlanceModifier.fillMaxSize()) {
            // PANEL 1: HEADER (FONT DITINGKATKAN KE 13sp & 12sp)
            Row(
                modifier = GlanceModifier.fillMaxWidth().background(ColorProvider(Color(0x990A0A0A))).padding(8.dp), 
                verticalAlignment = Alignment.Vertical.CenterVertically
            ) {
                Column(modifier = GlanceModifier.defaultWeight()) {
                    Text(text = "ZF SPATIAL MONITOR", style = TextStyle(color = ColorProvider(Color.Cyan), fontWeight = FontWeight.Bold, fontSize = 13.sp))
                    Text(text = metaLokasi, style = TextStyle(color = ColorProvider(Color.LightGray), fontSize = 12.sp))
                }
                Row(verticalAlignment = Alignment.Vertical.CenterVertically) {
                    Text(text = "$ikonCuaca $teksCuaca", style = TextStyle(color = ColorProvider(Color.Yellow), fontWeight = FontWeight.Bold, fontSize = 12.sp))
                    Spacer(modifier = GlanceModifier.width(8.dp))
                    Text(
                        text = "[ ↻ ]",
                        modifier = GlanceModifier.clickable(onClick = actionRunCallback<SegarkanMatriksAction>()),
                        style = TextStyle(color = ColorProvider(Color.Green), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    )
                }
            }

            Spacer(modifier = GlanceModifier.height(4.dp))

            // PANEL 2: METEOROLOGI (FONT SUHU DITINGKATKAN KE 16sp, DATA KE 12sp)
            Column(modifier = GlanceModifier.fillMaxWidth().background(ColorProvider(Color(0x99151515))).padding(8.dp)) {
                Row(modifier = GlanceModifier.fillMaxWidth()) {
                    Text("💡 Kondisi Saat Ini", style = TextStyle(color = ColorProvider(Color.Cyan), fontSize = 11.sp), modifier = GlanceModifier.defaultWeight())
                }
                Spacer(modifier = GlanceModifier.height(4.dp))
                Row(modifier = GlanceModifier.fillMaxWidth()) {
                    Text("🌡️ $suhu", style = TextStyle(color = ColorProvider(Color.White), fontSize = 16.sp, fontWeight = FontWeight.Bold), modifier = GlanceModifier.defaultWeight())
                    Text("💨 $angin", style = TextStyle(color = ColorProvider(Color.White), fontSize = 16.sp, fontWeight = FontWeight.Bold), modifier = GlanceModifier.defaultWeight())
                }
                Spacer(modifier = GlanceModifier.height(4.dp))
                Row(modifier = GlanceModifier.fillMaxWidth()) {
                    Text("🌧️ Air: $presipitasi", style = TextStyle(color = ColorProvider(Color.LightGray), fontSize = 12.sp), modifier = GlanceModifier.defaultWeight())
                    Text("☁️ Awan: $awan", style = TextStyle(color = ColorProvider(Color.LightGray), fontSize = 12.sp), modifier = GlanceModifier.defaultWeight())
                    Text("💧 RH: $kelembapan", style = TextStyle(color = ColorProvider(Color.LightGray), fontSize = 12.sp), modifier = GlanceModifier.defaultWeight())
                }
            }

            Spacer(modifier = GlanceModifier.height(4.dp))

            // PANEL 3: LITOSFER (FONT DITINGKATKAN KE 11sp, 13sp, 12sp)
            Column(modifier = GlanceModifier.fillMaxWidth().background(ColorProvider(Color(0x99151515))).padding(8.dp)) {
                Text(text = "TITIK PANTAU GEMPA", style = TextStyle(color = ColorProvider(Color.Red), fontSize = 11.sp, fontWeight = FontWeight.Bold))
                Spacer(modifier = GlanceModifier.height(2.dp))
                Text(text = lokasi, style = TextStyle(color = ColorProvider(Color.White), fontSize = 13.sp, fontWeight = FontWeight.Bold))
                Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.Vertical.CenterVertically) {
                    Text(text = "Magnitudo: $skala | ", style = TextStyle(color = ColorProvider(Color.LightGray), fontSize = 12.sp))
                    Text(text = status, style = TextStyle(color = warna, fontSize = 12.sp, fontWeight = FontWeight.Bold))
                }
            }
        }
    }

}

class SegarkanMatriksAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val pref = context.getSharedPreferences("ZF_STORAGE", Context.MODE_PRIVATE)
        
        pref.edit().apply {
            putString("suhu", "Siklus...")
            putString("angin", "Koneksi...")
            putString("kelembapan", "...")
            putString("awan", "...")
            putString("presipitasi", "...")
            putString("lokasi", "Mengekstrak Satelit...")
            putString("status", "Proses")
            putString("warna", "Yellow")
            apply()
        }
        ZuhriWidget().update(context, glanceId)

        kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
            try {
                val lat = pref.getFloat("last_lat", -6.9535f).toDouble()
                val lon = pref.getFloat("last_lon", 110.2312f).toDouble()
                val lokasiNama = pref.getString("meta_lokasi", "Blorok, Kendal") ?: "Blorok, Kendal"

                val respons = NetworkMatriks.api.getSinkronisasi(lat, lon, lokasiNama)
                
                pref.edit().apply {
                    putString("meta_lokasi", respons.meta_lokasi)
                    putString("suhu", respons.cuaca.suhu)
                    putString("angin", respons.cuaca.angin)
                    putString("kelembapan", respons.cuaca.kelembapan)
                    putString("awan", respons.cuaca.awan)
                    putString("presipitasi", respons.cuaca.presipitasi)
                    putString("lokasi", respons.bencana.lokasi)
                    putString("skala", respons.bencana.skala)
                    putString("status", respons.bencana.status_bahaya)
                    putString("warna", respons.bencana.kode_warna)
                    apply()
                }
            } catch (e: Exception) {
            } finally {
                ZuhriWidget().updateAll(context)
            }
        }
    }
}

class ZuhriWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ZuhriWidget()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        val siklusOtomatis = PeriodicWorkRequestBuilder<ZuhriWorker>(15, TimeUnit.MINUTES).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "ZF_Sinkronisasi_Satelit",
            ExistingPeriodicWorkPolicy.KEEP,
            siklusOtomatis
        )
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        val pref = context.getSharedPreferences("ZF_STORAGE", Context.MODE_PRIVATE)
        kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
            try {
                val lat = pref.getFloat("last_lat", -6.9535f).toDouble()
                val lon = pref.getFloat("last_lon", 110.2312f).toDouble()
                val lokasiNama = pref.getString("meta_lokasi", "Blorok, Kendal") ?: "Blorok, Kendal"

                val respons = NetworkMatriks.api.getSinkronisasi(lat, lon, lokasiNama)

                pref.edit().apply {
                    putString("meta_lokasi", respons.meta_lokasi)
                    putString("suhu", respons.cuaca.suhu)
                    putString("angin", respons.cuaca.angin)
                    putString("kelembapan", respons.cuaca.kelembapan)
                    putString("awan", respons.cuaca.awan)
                    putString("presipitasi", respons.cuaca.presipitasi)
                    putString("lokasi", respons.bencana.lokasi)
                    putString("skala", respons.bencana.skala)
                    putString("status", respons.bencana.status_bahaya)
                    putString("warna", respons.bencana.kode_warna)
                    apply()
                }
                ZuhriWidget().updateAll(context)
            } catch (e: Exception) {}
        }
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        WorkManager.getInstance(context).cancelUniqueWork("ZF_Sinkronisasi_Satelit")
    }
}
