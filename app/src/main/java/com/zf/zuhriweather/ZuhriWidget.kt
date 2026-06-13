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
        // Matriks Awal (Kosong)
        var nilaiSuhu = "Memuat..."
        var nilaiAngin = "Memuat..."
        var lokasiRuptur = "Memindai Litosfer..."
        var skalaRuptur = "-"

        // Transmisi Paralel 1: Ekstraksi Termal (Cuaca Lokal)
        try {
            val respons = NetworkMatriks.api.getKerapatanSpasial()
            nilaiSuhu = "${respons.current_weather.temperature}°C"
            nilaiAngin = "${respons.current_weather.windspeed} km/j"
        } catch (e: Exception) {
            nilaiSuhu = "Distorsi"
            nilaiAngin = "Distorsi"
        }

        // Transmisi Paralel 2: Ekstraksi Ruptur (Gempa Global)
        try {
            val usgsRespons = UsgsMatriks.api.getLedgerZonaMerah()
            // Jika ada ruptur struktural terdeteksi di atas 6.0 Mw
            if (usgsRespons.features.isNotEmpty()) {
                val rupturTerbesar = usgsRespons.features[0].properties
                // Ekstrak string lokasi agar tidak merusak batas dimensi UI
                lokasiRuptur = rupturTerbesar.place.take(25) + "..." 
                skalaRuptur = "${rupturTerbesar.mag} Mw"
            } else {
                lokasiRuptur = "Nihil Anomali Kritis"
                skalaRuptur = "-"
            }
        } catch (e: Exception) {
            lokasiRuptur = "Distorsi USGS"
        }

        provideContent {
            MatriksVisualSpasial(nilaiSuhu, nilaiAngin, lokasiRuptur, skalaRuptur)
        }
    }

    @Composable
    private fun MatriksVisualSpasial(suhu: String, angin: String, lokasiGempa: String, skalaGempa: String) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(Color.DarkGray)
                .padding(12.dp)
        ) {
            Text(text = "[ZF] SPASIAL: GRID 110.20E", style = TextStyle(color = ColorProvider(Color.Cyan)))
            Text(text = "Termal: $suhu | Angin: $angin", style = TextStyle(color = ColorProvider(Color.White)))
            
            Spacer(modifier = GlanceModifier.padding(4.dp))
            
            Text(
                text = "[SINKRONISASI MATRIKS]",
                modifier = GlanceModifier.clickable(onClick = actionRunCallback<SegarkanMatriksAction>()),
                style = TextStyle(color = ColorProvider(Color.Green))
            )

            Spacer(modifier = GlanceModifier.padding(8.dp))

            Text(text = "LEDGER ZONA MERAH (USGS)", style = TextStyle(color = ColorProvider(Color.Red)))
            Row {
                Text(text = "$lokasiGempa | ", style = TextStyle(color = ColorProvider(Color.White)))
                Text(text = skalaGempa, style = TextStyle(color = ColorProvider(Color.Yellow)))
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
