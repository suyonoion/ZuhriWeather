package com.zf.zuhriweather

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Eksekusi Sinkronisasi Instan setiap kali aplikasi dibuka oleh pengguna
        segarkanMatriksFisis(this)

        setContent {
            var suhu by remember { mutableStateOf("-") }
            var angin by remember { mutableStateOf("-") }
            var lokasi by remember { mutableStateOf("Menunggu Transmisi...") }
            var skala by remember { mutableStateOf("-") }
            var status by remember { mutableStateOf("Standby") }
            var warnaCode by remember { mutableStateOf("Gray") }

            // Mengikat State Visual dengan Penyimpanan Non-Volatil (Persistent Storage)
            LaunchedEffect(Unit) {
                val pref = getSharedPreferences("ZF_STORAGE", Context.MODE_PRIVATE)
                suhu = pref.getString("suhu", "-") ?: "-"
                angin = pref.getString("angin", "-") ?: "-"
                lokasi = pref.getString("lokasi", "Menunggu Transmisi...") ?: "Menunggu Transmisi..."
                skala = pref.getString("skala", "-") ?: "-"
                status = pref.getString("status", "Standby") ?: "Standby"
                warnaCode = pref.getString("warna", "Gray") ?: "Gray"
                
                // Reaktivitas Otomatis: Update UI jika WorkManager latar belakang memasukkan data baru
                pref.registerOnSharedPreferenceChangeListener { p, key ->
                    when (key) {
                        "suhu" -> suhu = p.getString("suhu", "-") ?: "-"
                        "angin" -> angin = p.getString("angin", "-") ?: "-"
                        "lokasi" -> lokasi = p.getString("lokasi", "Menunggu Transmisi...") ?: "Menunggu Transmisi..."
                        "skala" -> skala = p.getString("skala", "-") ?: "-"
                        "status" -> status = p.getString("status", "Standby") ?: "Standby"
                        "warna" -> warnaCode = p.getString("warna", "Gray") ?: "Gray"
                    }
                }
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
                Text(
                    text = "ZF SPATIAL MONITOR - KENDAL",
                    color = Color.Cyan,
                    fontSize = 20.sp,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

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

                // Tombol Eksekusi Manual - Mekanisme Pembersihan Jaringan Latar Depan
                Text(
                    text = "[ ↻ SINKRONISASI MATRIKS LOKAL ]",
                    color = Color.Green,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .clickable {
                            val pref = getSharedPreferences("ZF_STORAGE", Context.MODE_PRIVATE)
                            pref.edit().apply {
                                putString("suhu", "Siklus...")
                                putString("angin", "Koneksi Kinetik...")
                                putString("lokasi", "Memindai Kerak...")
                                putString("status", "Proses")
                                putString("warna", "Yellow")
                                apply()
                            }
                            segarkanMatriksFisis(this@MainActivity)
                        }
                        .padding(8.dp)
                )
            }
        }
    }

    private fun segarkanMatriksFisis(context: Context) {
        lifecycleScope.launch(Dispatchers.IO) {
            val pref = context.getSharedPreferences("ZF_STORAGE", Context.MODE_PRIVATE)
            try {
                val respons = NetworkMatriks.api.getSinkronisasi()
                pref.edit().apply {
                    putString("suhu", respons.cuaca.suhu)
                    putString("angin", respons.cuaca.angin)
                    putString("lokasi", respons.bencana.lokasi)
                    putString("skala", respons.bencana.skala)
                    putString("status", respons.bencana.status_bahaya)
                    putString("warna", respons.bencana.kode_warna)
                    apply()
                }
                // Memaksa proyektor pasif (Widget) di beranda ikut bergetar memperbarui diri
                ZuhriWidget().updateAll(context)
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    val currentAngin = pref.getString("angin", "-")
                    if (currentAngin == "Koneksi Kinetik...") {
                        pref.edit().putString("angin", "Ruptur Jaringan").apply()
                    }
                }
            }
        }
    }
}
