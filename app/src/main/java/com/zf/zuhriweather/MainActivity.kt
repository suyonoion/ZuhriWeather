package com.zf.zuhriweather

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        segarkanMatriksFisis(this)

        setContent {
            val context = LocalContext.current
            
            // Perulangan Absolut 60 Detik
            LaunchedEffect(Unit) {
                while(true) {
                    delay(60000)
                    segarkanMatriksFisis(context)
                }
            }

            var suhu by remember { mutableStateOf("-") }
            var angin by remember { mutableStateOf("-") }
            var lokasi by remember { mutableStateOf("Menunggu Transmisi...") }
            var skala by remember { mutableStateOf("-") }
            var status by remember { mutableStateOf("Standby") }
            var warnaCode by remember { mutableStateOf("Gray") }
            var waktuSinkron by remember { mutableStateOf("-") } // Matriks Waktu

            LaunchedEffect(Unit) {
                val pref = context.getSharedPreferences("ZF_STORAGE", Context.MODE_PRIVATE)
                suhu = pref.getString("suhu", "-") ?: "-"
                angin = pref.getString("angin", "-") ?: "-"
                lokasi = pref.getString("lokasi", "Menunggu Transmisi...") ?: "Menunggu Transmisi..."
                skala = pref.getString("skala", "-") ?: "-"
                status = pref.getString("status", "Standby") ?: "Standby"
                warnaCode = pref.getString("warna", "Gray") ?: "Gray"
                waktuSinkron = pref.getString("waktu_sinkron", "-") ?: "-"
                
                val listener = SharedPreferences.OnSharedPreferenceChangeListener { p, key ->
                    when (key) {
                        "suhu" -> suhu = p.getString("suhu", "-") ?: "-"
                        "angin" -> angin = p.getString("angin", "-") ?: "-"
                        "lokasi" -> lokasi = p.getString("lokasi", "Menunggu Transmisi...") ?: "Menunggu Transmisi..."
                        "skala" -> skala = p.getString("skala", "-") ?: "-"
                        "status" -> status = p.getString("status", "Standby") ?: "Standby"
                        "warna" -> warnaCode = p.getString("warna", "Gray") ?: "Gray"
                        "waktu_sinkron" -> waktuSinkron = p.getString("waktu_sinkron", "-") ?: "-"
                    }
                }
                pref.registerOnSharedPreferenceChangeListener(listener)
            }

            val warnaStatus = when (warnaCode) {
                "Red" -> Color.Red
                "Orange" -> Color(0xFFFFA500)
                "Yellow" -> Color.Yellow
                "Green" -> Color.Green
                else -> Color.Gray
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF121212))
                    .padding(24.dp)
            ) {
                Text(text = "ZF SPATIAL MONITOR - KENDAL", color = Color.Cyan, fontSize = 20.sp, modifier = Modifier.padding(bottom = 24.dp))
                
                Text(text = "MATRIKS ATMOSFER", color = Color.Gray, fontSize = 12.sp)
                Text(text = "Suhu Lingkungan : $suhu", color = Color.White, fontSize = 18.sp)
                Text(text = "Vektor Angin     : $angin", color = Color.White, fontSize = 18.sp)
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(text = "MATRIKS LITOSFER (USGS)", color = Color.Red, fontSize = 12.sp)
                Text(text = "Pusat Ruptur : $lokasi", color = Color.White, fontSize = 16.sp)
                
                Row(modifier = Modifier.padding(top = 4.dp)) {
                    Text(text = "Magnitudo: $skala | Magnitudo Status: ", color = Color.White, fontSize = 16.sp)
                    Text(text = status, color = warnaStatus, fontSize = 16.sp)
                }
                
                Spacer(modifier = Modifier.height(48.dp))
                
                Text(
                    text = "[ ↻ SINKRONISASI MATRIKS LOKAL ]",
                    color = Color.Green,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .clickable {
                            val pref = context.getSharedPreferences("ZF_STORAGE", Context.MODE_PRIVATE)
                            val editor = pref.edit()
                            editor.putString("suhu", "Siklus...")
                            editor.putString("angin", "Koneksi Kinetik...")
                            editor.putString("lokasi", "Memindai Kerak...")
                            editor.putString("status", "Proses")
                            editor.putString("warna", "Yellow")
                            editor.apply()
                            
                            segarkanMatriksFisis(context)
                        }
                        .padding(8.dp)
                )

                // Indikator Fisis Pergerakan Mesin
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = "Kontak Satelit Terakhir: $waktuSinkron WIB", color = Color.DarkGray, fontSize = 12.sp)
            }
        }
    }

    private fun segarkanMatriksFisis(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            val pref = context.getSharedPreferences("ZF_STORAGE", Context.MODE_PRIVATE)
            try {
                val respons = NetworkMatriks.api.getSinkronisasi()
                
                // Menangkap presisi waktu saat data berhasil ditarik
                val waktuSekarang = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())

                val editor = pref.edit()
                editor.putString("suhu", respons.cuaca.suhu)
                editor.putString("angin", respons.cuaca.angin)
                editor.putString("lokasi", respons.bencana.lokasi)
                editor.putString("skala", respons.bencana.skala)
                editor.putString("status", respons.bencana.status_bahaya)
                editor.putString("warna", respons.bencana.kode_warna)
                editor.putString("waktu_sinkron", waktuSekarang) // Memahat jam
                editor.apply()
                
                ZuhriWidget().updateAll(context)
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    val currentAngin = pref.getString("angin", "-")
                    if (currentAngin == "Koneksi Kinetik...") {
                        val editor = pref.edit()
                        editor.putString("angin", "Ruptur Jaringan")
                        editor.apply()
                    }
                }
            }
        }
    }
}
