package com.zf.zuhriweather

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
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
    
    // Injeksi Transmisi Asinkron sebelum layar dirender
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        
        var nilaiSuhu = "Memuat..."
        var nilaiAngin = "Memuat..."

        try {
            // Menarik kerapatan informasi spasial langsung dari API Peladen
            val respons = NetworkMatriks.api.getKerapatanSpasial()
            nilaiSuhu = "${respons.current_weather.temperature}°C"
            nilaiAngin = "${respons.current_weather.windspeed} km/j"
        } catch (e: Exception) {
            // Tangkapan ekuilibrium jika terjadi ruptur jaringan (offline)
            nilaiSuhu = "Distorsi Sinyal"
            nilaiAngin = "Distorsi"
        }

        provideContent {
            // Meneruskan data fisis nyata ke dalam matriks visual
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
            // Parameter dinamis merender di sini
            Text(
                text = "Termal: $suhu | Vektor Angin: $angin", 
                style = TextStyle(color = ColorProvider(Color.White))
            )
            
            Spacer(modifier = GlanceModifier.padding(8.dp))
            
            // Ledger Bencana sementara masih dipertahankan statis sebelum transisi
            Text(
                text = "LEDGER ZONA MERAH", 
                style = TextStyle(color = ColorProvider(Color.Red))
            )
            Row {
                Text(text = "Filipina | ", style = TextStyle(color = ColorProvider(Color.White)))
                Text(text = "7.8 Mw | ", style = TextStyle(color = ColorProvider(Color.Yellow)))
                Text(text = "Ruptur Litosfer", style = TextStyle(color = ColorProvider(Color.White)))
            }
        }
    }
}

class ZuhriWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ZuhriWidget()
}
