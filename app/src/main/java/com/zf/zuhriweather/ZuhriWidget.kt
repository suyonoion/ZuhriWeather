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
        var suhu = "Memuat..."
        var angin = "Memuat..."
        var lokasi = "Memindai Litosfer..."
        var skala = "-"
        var status = "Menunggu..."
        var warnaStatus = Color.Gray

        try {
            val respons = withContext(Dispatchers.IO) {
                NetworkMatriks.api.getSinkronisasi()
            }
            
            suhu = respons.cuaca.suhu
            angin = respons.cuaca.angin
            lokasi = respons.bencana.lokasi
            skala = respons.bencana.skala
            status = respons.bencana.status_bahaya
            
            warnaStatus = when(respons.bencana.kode_warna) {
                "Red" -> Color.Red
                "Orange" -> Color(0xFFFFA500)
                "Yellow" -> Color.Yellow
                else -> Color.Green
            }
            
        } catch (e: Exception) {
            suhu = "Distorsi"
            // Mengekstraksi paksa alasan kegagalan mesin ke parameter Angin
            angin = (e.localizedMessage ?: "Unknown").take(20)
            lokasi = "Ruptur Transmisi"
            skala = "-"
            status = "Offline"
            warnaStatus = Color.Red
        }

        provideContent {
            MatriksVisualPublik(suhu, angin, lokasi, skala, status, warnaStatus)
        }
    }

    @Composable
    private fun MatriksVisualPublik(
        suhu: String, angin: String, lokasi: String, skala: String, status: String, warna: Color
    ) {
        Column(
            modifier = GlanceModifier.fillMaxSize().background(Color.DarkGray).padding(12.dp)
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
