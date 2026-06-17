package com.zf.zuhriweather

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
            LaunchedEffect(Unit) {
                while(true) {
                    delay(60000)
                    segarkanMatriksFisis(context)
                }
            }

            // State Data Fisis
            var suhu by remember { mutableStateOf("-") }
            var angin by remember { mutableStateOf("-") }
            var lokasi by remember { mutableStateOf("Menunggu Transmisi...") }
            var skala by remember { mutableStateOf("-") }
            var status by remember { mutableStateOf("Standby") }
            var warnaCode by remember { mutableStateOf("Gray") }
            var waktuSinkron by remember { mutableStateOf("-") }

            LaunchedEffect(Unit) {
                val pref = context.getSharedPreferences("ZF_STORAGE", Context.MODE_PRIVATE)
                suhu = pref.getString("suhu", "-") ?: "-"
                angin = pref.getString("angin", "-") ?: "-"
                lokasi = pref.getString("lokasi", "-") ?: "-"
                skala = pref.getString("skala", "-") ?: "-"
                status = pref.getString("status", "-") ?: "-"
                warnaCode = pref.getString("warna", "Gray") ?: "Gray"
                waktuSinkron = pref.getString("waktu_sinkron", "-") ?: "-"
                
                val listener = SharedPreferences.OnSharedPreferenceChangeListener { p, key ->
                    when (key) {
                        "suhu" -> suhu = p.getString("suhu", "-") ?: "-"
                        "angin" -> angin = p.getString("angin", "-") ?: "-"
                        "lokasi" -> lokasi = p.getString("lokasi", "-") ?: "-"
                        "skala" -> skala = p.getString("skala", "-") ?: "-"
                        "status" -> status = p.getString("status", "-") ?: "-"
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

            // ARSITEKTUR UX/UI: SCROLLABLE MATRIX
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF0A0A0A))
                    .padding(16.dp)
            ) {
                // HEADER
                item {
                    Text(text = "ZF SPATIAL MONITOR", color = Color.Cyan, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text(text = "Koordinat Absolut: Kendal, Jawa Tengah", color = Color.Gray, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(24.dp))
                }

                // 1. MATRIKS LOKAL (PRIORITAS TERTINGGI / ALARM)
                item {
                    SectionHeader("ANOMALI SPASIAL LOKAL (RADIUS EVAKUASI)", Color.Red)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF1A1A1A))
                            .border(1.dp, warnaStatus, RoundedCornerShape(8.dp))
                            .padding(16.dp)
                    ) {
                        Column {
                            Text(text = status, color = warnaStatus, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = "Pusat Ruptur: $lokasi", color = Color.White, fontSize = 14.sp)
                            Text(text = "Magnitudo Fisis: $skala", color = Color.LightGray, fontSize = 14.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }

                // 2. MATRIKS ATMOSFER (CUACA PROFESIONAL)
                item {
                    SectionHeader("TERMODINAMIKA LOKAL", Color(0xFF00BFFF))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        DataCard("Suhu Lingkungan", suhu, modifier = Modifier.weight(1f))
                        Spacer(modifier = Modifier.width(8.dp))
                        DataCard("Vektor Kinetik Angin", angin, modifier = Modifier.weight(1f))
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }

                // 3. MATRIKS DOMESTIK INDONESIA
                item {
                    SectionHeader("LITOSFER INDONESIA (SEMUA ZONA)", Color.Yellow)
                    // Placeholder Tabel UX (Menunggu pasokan Array dari main.py)
                    TabelBencana(listOf(
                        DataBaris("Maluku", "Gempa", "5.2 SR", "WASPADA"),
                        DataBaris("Papua", "Gempa", "4.1 SR", "AMAN")
                    ))
                    Spacer(modifier = Modifier.height(24.dp))
                }

                // 4. MATRIKS GLOBAL (ZONA MERAH)
                item {
                    SectionHeader("GLOBAL RUPTUR: ZONA MERAH (MAG ≥ 6.0)", Color.Red)
                    TabelBencana(listOf(
                        DataBaris("Filipina", "Gempa", "7.2 SR", "AWAS"),
                        DataBaris("Jepang", "Gempa", "6.5 SR", "AWAS")
                    ), warnaTabel = Color(0x33FF0000))
                    Spacer(modifier = Modifier.height(24.dp))
                }

                // FOOTER: SINKRONISASI
                item {
                    Divider(color = Color.DarkGray, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "[ ↻ PAKSA SINKRONISASI SATELIT ]",
                        color = Color.Green,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { segarkanMatriksFisis(context) }
                            .padding(vertical = 12.dp),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Kontak Satelit Terakhir: $waktuSinkron WIB",
                        color = Color.DarkGray,
                        fontSize = 12.sp,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }

    private fun segarkanMatriksFisis(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            val pref = context.getSharedPreferences("ZF_STORAGE", Context.MODE_PRIVATE)
            try {
                val respons = NetworkMatriks.api.getSinkronisasi()
                val waktuSekarang = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                pref.edit().apply {
                    putString("suhu", respons.cuaca.suhu)
                    putString("angin", respons.cuaca.angin)
                    putString("lokasi", respons.bencana.lokasi)
                    putString("skala", respons.bencana.skala)
                    putString("status", respons.bencana.status_bahaya)
                    putString("warna", respons.bencana.kode_warna)
                    putString("waktu_sinkron", waktuSekarang)
                    apply()
                }
                ZuhriWidget().updateAll(context)
            } catch (e: Exception) {
                // Penanganan Ruptur
            }
        }
    }
}

// ================= KOMPONEN UX / UI ================= //

@Composable
fun SectionHeader(judul: String, warna: Color) {
    Text(
        text = judul,
        color = warna,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
fun DataCard(label: String, nilai: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF1A1A1A))
            .padding(16.dp)
    ) {
        Text(text = label, color = Color.Gray, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = nilai, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

data class DataBaris(val lokasi: String, val jenis: String, val skala: String, val bahaya: String)

@Composable
fun TabelBencana(data: List<DataBaris>, warnaTabel: Color = Color(0xFF1A1A1A)) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(warnaTabel)
            .border(0.5.dp, Color.DarkGray, RoundedCornerShape(8.dp))
    ) {
        // Header Tabel
        Row(modifier = Modifier.background(Color(0xFF2A2A2A)).padding(8.dp)) {
            Text("Wilayah", color = Color.LightGray, fontSize = 12.sp, modifier = Modifier.weight(2f))
            Text("Skala", color = Color.LightGray, fontSize = 12.sp, modifier = Modifier.weight(1f))
            Text("Bahaya", color = Color.LightGray, fontSize = 12.sp, modifier = Modifier.weight(1.5f))
        }
        // Isi Tabel
        data.forEach { baris ->
            Divider(color = Color.DarkGray, thickness = 0.5.dp)
            Row(modifier = Modifier.padding(8.dp)) {
                Text(baris.lokasi, color = Color.White, fontSize = 12.sp, modifier = Modifier.weight(2f))
                Text(baris.skala, color = Color.White, fontSize = 12.sp, modifier = Modifier.weight(1f))
                Text(baris.bahaya, color = Color.White, fontSize = 12.sp, modifier = Modifier.weight(1.5f))
            }
        }
    }
}
