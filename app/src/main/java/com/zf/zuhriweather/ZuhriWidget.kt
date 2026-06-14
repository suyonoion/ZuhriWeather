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

class ZuhriWidget : GlanceAppWidget() {
    
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        var nilaiSuhu = "Memuat..."
        var nilaiAngin = "Memuat..."
        var lokasiAwam = "Memindai..."
        var skalaRuptur = "-"
        var statusBahaya = "Aman"
        var warnaStatus = Color.Green

        // Transmisi 1: Cuaca
        try {
            val respons = NetworkMatriks.api.getKerapatanSpasial()
            nilaiSuhu = "${respons.current_weather.temperature}°C"
            nilaiAngin = "${respons.current_weather.windspeed} km/j"
        } catch (e: Exception) {
            nilaiSuhu = "Offline"
            nilaiAngin = "Offline"
        }

        // Transmisi 2: Bencana dengan Lapis Translasi Publik
        try {
            val usgsRespons = UsgsMatriks.api.getLedgerZonaMerah()
            if (usgsRespons.features.isNotEmpty()) {
                val ruptur = usgsRespons.features[0].properties
                
                // 1. Translasi Lokasi: Membuang noise "23 km E of..."
                // Jika mengandung kata " of ", ambil teks setelahnya saja.
                val rawLokasi = ruptur.place
                lokasiAwam = if (rawLokasi.contains(" of ")) {
                    rawLokasi.substringAfter(" of ").take(25) + "..."
                } else {
                    rawLokasi.take(25) + "..."
                }

                skalaRuptur = "${ruptur.mag} SR" // Awam lebih paham SR (Skala Richter) daripada Mw

                // 2. Translasi Status Bahaya Berdasarkan Skala
                when {
                    ruptur.mag >= 6.0 -> {
                        statusBahaya = "[AWAS] Potensi Merusak!"
                        warnaStatus = Color.Red
                    }
                    ruptur.mag >= 5.0 -> {
                        statusBahaya = "[WASPADA] Getaran Kuat"
                        warnaStatus = Color(0xFFFFA500) // Warna Oranye
                    }
                    else -> {
                        statusBahaya = "[INFO] Getaran Ringan"
                        warnaStatus = Color.Yellow
                    }
                }
            } else {
                lokasiAwam = "Tidak Ada Gempa Signifikan"
                skalaRuptur = ""
                statusBahaya = "Aman Terkendali"
            }
        } catch (e: Exception) {
            lokasiAwam = "Gagal Memuat Data Gempa"
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
            // Sektor Cuaca disederhanakan bahasanya
            Text(text = "CUACA LOKAL (KENDAL)", style = TextStyle(color = ColorProvider(Color.Cyan)))
            Text(text = "Suhu: $suhu | Angin: $angin", style = TextStyle(color = ColorProvider(Color.White)))
            
            Spacer(modifier = GlanceModifier.padding(4.dp))
            
            Text(
                text = "↻ PERBARUI DATA",
                modifier = GlanceModifier.clickable(onClick = actionRunCallback<SegarkanMatriksAction>()),
                style = TextStyle(color = ColorProvider(Color.LightGray))
            )

            Spacer(modifier = GlanceModifier.padding(8.dp))

            // Sektor Bencana diterjemahkan menjadi peringatan awam
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
