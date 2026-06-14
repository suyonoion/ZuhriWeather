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

// 1. Matriks Memori Statis (Cache Lokal) - Bebas dari pemblokiran OS
object MatriksCache {
    var suhu = "-"
    var angin = "-"
    var lokasi = "Menunggu Transmisi..."
    var skala = "-"
    var status = "Standby"
    var warnaStatus = Color.Gray
}

class ZuhriWidget : GlanceAppWidget() {
    
    // 2. Proyektor Visual Murni - Merender instan dalam 1 milidetik
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(ColorProvider(Color(0xFF212121)))
                    .padding(12.dp)
            ) {
                MatriksVisualPublik(
                    MatriksCache.suhu, 
                    MatriksCache.angin, 
                    MatriksCache.lokasi, 
                    MatriksCache.skala, 
                    MatriksCache.status, 
                    ColorProvider(MatriksCache.warnaStatus)
                )
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

// 3. Mesin Transmisi Independen - Dieksekusi hanya saat tombol ditekan
class SegarkanMatriksAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        
        // Fase 1: Pembaruan Layar menjadi status Memuat (Instan)
        MatriksCache.suhu = "Siklus..."
        MatriksCache.angin = "Koneksi..."
        MatriksCache.lokasi = "Mengekstrak Litosfer..."
        MatriksCache.status = "Proses"
        MatriksCache.warnaStatus = Color.Yellow
        ZuhriWidget().update(context, glanceId)

        // Fase 2: Pengeboran Jaringan ke Peladen Barat
        try {
            val respons = withContext(Dispatchers.IO) {
                NetworkMatriks.api.getSinkronisasi()
            }
            
            MatriksCache.suhu = respons.cuaca.suhu
            MatriksCache.angin = respons.cuaca.angin
            MatriksCache.lokasi = respons.bencana.lokasi
            MatriksCache.skala = respons.bencana.skala
            MatriksCache.status = respons.bencana.status_bahaya
            
            MatriksCache.warnaStatus = when(respons.bencana.kode_warna) {
                "Red" -> Color.Red
                "Orange" -> Color(0xFFFFA500)
                "Yellow" -> Color.Yellow
                else -> Color.Green
            }
            
        } catch (e: java.net.UnknownHostException) {
            MatriksCache.angin = "Sinyal Terputus"
            MatriksCache.lokasi = "Gerbang Lokal Tertutup"
            MatriksCache.status = "Offline"
            MatriksCache.warnaStatus = Color.Red
        } catch (e: java.net.SocketTimeoutException) {
            MatriksCache.angin = "Peladen Bangun"
            MatriksCache.lokasi = "Klik [PERBARUI DATA] Sekali Lagi" // Instruksi mekanis
            MatriksCache.status = "Timeout"
            MatriksCache.warnaStatus = Color(0xFFFFA500) 
        } catch (e: Exception) {
            MatriksCache.angin = "Ruptur Sistem"
            MatriksCache.lokasi = "Kegagalan Fisis"
            MatriksCache.status = "Error"
            MatriksCache.warnaStatus = Color.Red
        }

        // Fase 3: Pencetakan Data Matang ke Layar (Instan)
        ZuhriWidget().update(context, glanceId)
    }
}

class ZuhriWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ZuhriWidget()
}
