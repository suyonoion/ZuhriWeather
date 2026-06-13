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

        try {
            val respons = NetworkMatriks.api.getKerapatanSpasial()
            nilaiSuhu = "${respons.current_weather.temperature}°C"
            nilaiAngin = "${respons.current_weather.windspeed} km/j"
        } catch (e: Exception) {
            nilaiSuhu = "Distorsi Sinyal"
            nilaiAngin = "Distorsi"
        }

        provideContent {
            MatriksVisualSpasial(suhu = nilaiSuhu, angin = nilaiAngin)
        }
    }

    @Composable
    private fun MatriksVisualSpasial(suhu: String, angin: String) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(Color.DarkGray)
                .padding(12.dp)
        ) {
            Text(
                text = "[ZF] SPASIAL: GRID 110.20E (KENDAL)", 
                style = TextStyle(color = ColorProvider(Color.Cyan))
            )
            Text(
                text = "Termal: $suhu | Vektor Angin: $angin", 
                style = TextStyle(color = ColorProvider(Color.White))
            )
            
            Spacer(modifier = GlanceModifier.padding(8.dp))
            
            // Injeksi Pemicu Transmisi Manual
            Text(
                text = "[SINKRONISASI MATRIKS]",
                modifier = GlanceModifier.clickable(onClick = actionRunCallback<SegarkanMatriksAction>()),
                style = TextStyle(color = ColorProvider(Color.Green))
            )

            Spacer(modifier = GlanceModifier.padding(8.dp))

            Text(
                text = "LEDGER ZONA MERAH", 
                style = TextStyle(color = ColorProvider(Color.Red))
            )
            Row {
                Text(text = "Filipina | ", style = TextStyle(color = ColorProvider(Color.White)))
                Text(text = "7.8 Mw | ", style = TextStyle(color = ColorProvider(Color.Yellow)))
                Text(text = "Ruptur", style = TextStyle(color = ColorProvider(Color.White)))
            }
        }
    }
}

// Protokol Pemaksaan Ekuilibrium Ulang
class SegarkanMatriksAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        // Memaksa sistem memutar ulang fungsi provideGlance()
        ZuhriWidget().update(context, glanceId)
    }
}

class ZuhriWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ZuhriWidget()
}
