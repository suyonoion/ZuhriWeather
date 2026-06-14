package com.zf.zuhriweather

import android.content.Context
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
        var nilaiSuhu = "Memuat..."
        var nilaiAngin = "Memuat..."
        var lokasiAwam = "Memindai..."
        var skalaRuptur = "-"
        var statusBahaya = "Menunggu..."
        var warnaStatus = Color.Gray

        // Transmisi 1: Cuaca (Dialihkan ke Jalur IO Absolut)
        try {
            val respons = withContext(Dispatchers.IO) {
                NetworkMatriks.api.getKerapatanSpasial()
            }
            nilaiSuhu = "${respons.current_weather.temperature}°C"
            nilaiAngin = "${respons.current_weather.windspeed} km/j"
        } catch (e: Exception) {
            nilaiSuhu = "Error Cuaca"
            // Cetak Log Fisis Mesin ke Layar
            nilaiAngin = (e.localizedMessage ?: "Distorsi").take(20) 
        }

        // Transmisi 2: Bencana (Dialihkan ke Jalur IO Absolut)
        try {
            val usgsRespons = withContext(Dispatchers.IO) {
                UsgsMatriks.api.getLedgerZonaMerah()
            }
            if (usgsRespons.features.isNotEmpty()) {
                val ruptur = usgsRespons.features[0].properties
                val rawLokasi = ruptur.place
                
                lokasiAwam = if (rawLokasi.contains(" of ")) {
                    rawLokasi.substringAfter(" of ").take(25) + "..."
                } else {
                    rawLokasi.take(25) + "..."
                }

                skalaRuptur = "${ruptur.mag} SR"

                when {
                    ruptur.mag >= 6.0 -> {
                        statusBahaya = "[AWAS] Potensi Merusak!"
                        warnaStatus = Color.Red
                    }
                    ruptur.mag >= 5.0 -> {
                        statusBahaya = "[WASPADA] Getaran Kuat"
                        warnaStatus = Color(0xFFFFA500)
                    }
                    else -> {
                        statusBahaya = "[INFO] Getaran Ringan"
                        warnaStatus = Color.Yellow
                    }
                }
            } else {
                lokasiAwam = "Nihil Gempa Signifikan"
                skalaRuptur = ""
                statusBahaya = "Aman"
                warnaStatus = Color.Green
            }
        } catch (e: Exception) {
            // Cetak Log Fisis Mesin ke Layar
            lokasiAwam = (e.localizedMessage ?: "Error Transmisi").take(30)
            skalaRuptur = "-"
            statusBahaya = "Gagal"
            warnaStatus = Color.Red
        }

        provideContent {
            MatriksVisualPublik(nilaiSuhu, nilaiAngin, lokasiAwam, skalaRuptur, statusBahaya, warnaStatus)
        }
    }

    @Composable
    private fun MatriksVisualPublik(
        suhu: String, 
        angin: String, 
        lokasi: String, 
        skala: String, 
        status: String, 
        warna: Color
    ) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(Color.DarkGray)
                .padding(12.dp)
        ) {
            Text(text = "CUACA LOKAL (KENDAL)", style = TextStyle(color = ColorProvider(Color.Cyan)))
            Text(text = "Suhu: $suhu | Angin: $angin", style = TextStyle(color = ColorProvider(Color.White)))
            
            Spacer(modifier = GlanceModifier.padding(4.dp))
            
            Text(
                text = "↻ PERBARUI DATA",
                modifier = GlanceModifier.clickable(onClick = actionRunCallback<SegarkanMatriksAction>()),
                style = TextStyle(color = ColorProvider(Color.LightGray))
            )

            Spacer(modifier = GlanceModifier.padding(8.dp))

            Text(text = "INFO GEMPA TERBARU", style = TextStyle(color = ColorProvider(Color.Red)))
            Text(text = lokasi, style = TextStyle(color = ColorProvider(Color.White)))
            Row {
                Text(text = "$skala | ", style = TextStyle(color = ColorProvider(Color.White)))
                Text(text = status, style = TextStyle(color = ColorProvider(warna)))
            }
        }
    }
}

class SegarkanMatriksAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        ZuhriWidget().update(context, glanceId)
    }
}

class ZuhriWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ZuhriWidget()
}
