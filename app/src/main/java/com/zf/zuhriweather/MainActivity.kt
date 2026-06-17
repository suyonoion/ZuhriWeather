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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Divider
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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

// --- STRUKTUR DATA KERAPATAN TINGGI ZF ---
data class MatriksAnomali(
    val negara: String,
    val entitas: String,
    val jenis: String,
    val probabilitas: String,
    val skala: String,
    val bahaya: String,
    val waktu: String,
    val warna: Color
)

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

            // State Data Latar Depan (Litosfer & Atmosfer)
            var suhu by remember { mutableStateOf("-") }
            var angin by remember { mutableStateOf("-") }
            var kelembapan by remember { mutableStateOf("-") }
            var awan by remember { mutableStateOf("-") }
            var presipitasi by remember { mutableStateOf("-") }
            
            var lokasi by remember { mutableStateOf("Menunggu Transmisi...") }
            var skala by remember { mutableStateOf("-") }
            var status by remember { mutableStateOf("Standby") }
            var warnaCode by remember { mutableStateOf("Gray") }
            var waktuSinkron by remember { mutableStateOf("-") }

            var tabIndex by remember { mutableStateOf(0) }
            val tabTitles = listOf("LOKAL", "DOMESTIK", "GLOBAL")

            LaunchedEffect(Unit) {
                val pref = context.getSharedPreferences("ZF_STORAGE", Context.MODE_PRIVATE)
                suhu = pref.getString("suhu", "-") ?: "-"
                angin = pref.getString("angin", "-") ?: "-"
                kelembapan = pref.getString("kelembapan", "Menunggu...") ?: "Menunggu..."
                awan = pref.getString("awan", "Menunggu...") ?: "Menunggu..."
                presipitasi = pref.getString("presipitasi", "Menunggu...") ?: "Menunggu..."
                
                lokasi = pref.getString("lokasi", "-") ?: "-"
                skala = pref.getString("skala", "-") ?: "-"
                status = pref.getString("status", "-") ?: "-"
                warnaCode = pref.getString("warna", "Gray") ?: "Gray"
                waktuSinkron = pref.getString("waktu_sinkron", "-") ?: "-"
                
                val listener = SharedPreferences.OnSharedPreferenceChangeListener { p, key ->
                    when (key) {
                        "suhu" -> suhu = p.getString("suhu", "-") ?: "-"
                        "angin" -> angin = p.getString("angin", "-") ?: "-"
                        "kelembapan" -> kelembapan = p.getString("kelembapan", "Menunggu...") ?: "Menunggu..."
                        "awan" -> awan = p.getString("awan", "Menunggu...") ?: "Menunggu..."
                        "presipitasi" -> presipitasi = p.getString("presipitasi", "Menunggu...") ?: "Menunggu..."
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

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF0A0A0A))
            ) {
                // HEADER ABSOLUT
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "ZF SPATIAL MONITOR", color = Color.Cyan, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text(text = "Pusat Kendali: Kendal, Jawa Tengah", color = Color.Gray, fontSize = 12.sp)
                }

                // TABULASI NAVIGASI
                TabRow(
                    selectedTabIndex = tabIndex,
                    containerColor = Color(0xFF121212),
                    contentColor = Color.Cyan,
                    indicator = { tabPositions ->
                        TabRowDefaults.Indicator(
                            Modifier.tabIndicatorOffset(tabPositions[tabIndex]),
                            color = Color.Cyan
                        )
                    }
                ) {
                    tabTitles.forEachIndexed { index, title ->
                        Tab(
                            selected = tabIndex == index,
                            onClick = { tabIndex = index },
                            text = { Text(text = title, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                        )
                    }
                }

                // KONTEN BERDASARKAN TAB
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    when (tabIndex) {
                        0 -> { // TAB 0: LOKAL (ALARM & CUACA)
                            item {
                                SectionHeader("TERMODINAMIKA LOKAL (KENDAL)", Color(0xFF00BFFF))
                                
                                // GRID CUACA BARIS 1
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    DataCard("Suhu Aktif", suhu, "🌡️", modifier = Modifier.weight(1f))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    DataCard("Vektor Angin", angin, "💨", modifier = Modifier.weight(1f))
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                // GRID CUACA BARIS 2
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    DataCard("Presipitasi", presipitasi, "🌧️", modifier = Modifier.weight(1f))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    DataCard("Tutupan Foton", awan, "☁️", modifier = Modifier.weight(1f))
                                }
                                Spacer(modifier = Modifier.height(8.dp))

                                // GRID CUACA BARIS 3
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    DataCard("Kerapatan Air (RH)", kelembapan, "💧", modifier = Modifier.weight(1f))
                                }
                                
                                Spacer(modifier = Modifier.height(24.dp))

                                SectionHeader("STATUS LITOSFER LOKAL (RADAR EVAKUASI)", Color.Red)
                                KartuAnomali(
                                    data = MatriksAnomali(
                                        negara = "Pusat Pelacakan Saat Ini",
                                        entitas = lokasi,
                                        jenis = "Pemindaian Tektonik",
                                        probabilitas = "Real-Time",
                                        skala = skala,
                                        bahaya = status,
                                        waktu = "Terakhir: $waktuSinkron WIB",
                                        warna = warnaStatus
                                    )
                                )
                            }
                        }
                        1 -> { // TAB 1: DOMESTIK INDONESIA
                            item { SectionHeader("RUPTUR TERITORIAL (INDONESIA)", Color.Yellow) }
                            items(DataSimulasi.domestik) { anomali ->
                                KartuAnomali(data = anomali)
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                        }
                        2 -> { // TAB 2: GLOBAL ZONA MERAH/ORANYE
                            item { SectionHeader("MATRIKS DESTRUKTIF GLOBAL (MAG ≥ 5.0)", Color.Red) }
                            items(DataSimulasi.global) { anomali ->
                                KartuAnomali(data = anomali)
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                        }
                    }

                    // FOOTER: SINKRONISASI
                    item {
                        Spacer(modifier = Modifier.height(24.dp))
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
                            text = "Detak Komputasi Akhir: $waktuSinkron WIB",
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
                    // Variabel cuaca baru ini belum ada di NetworkMatriks.kt, 
                    // akan terisi nilai default sampai backend & retrofit diupgrade.
                    putString("lokasi", respons.bencana.lokasi)
                    putString("skala", respons.bencana.skala)
                    putString("status", respons.bencana.status_bahaya)
                    putString("warna", respons.bencana.kode_warna)
                    putString("waktu_sinkron", waktuSekarang)
                    apply()
                }
                ZuhriWidget().updateAll(context)
            } catch (e: Exception) {
                // Tangani kegagalan jaringan
            }
        }
    }
}

// ================= KOMPONEN UX / UI KLINIS ================= //

@Composable
fun SectionHeader(judul: String, warna: Color) {
    Text(
        text = judul,
        color = warna,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 12.dp)
    )
}

// Evolusi DataCard: Injeksi Ikonografi Spasial
@Composable
fun DataCard(label: String, nilai: String, ikon: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF1A1A1A))
            .border(1.dp, Color(0xFF333333), RoundedCornerShape(6.dp))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = ikon, fontSize = 16.sp, modifier = Modifier.padding(end = 6.dp))
            Text(text = label, color = Color.Gray, fontSize = 11.sp)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = nilai, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun KartuAnomali(data: MatriksAnomali) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF151515))
            .border(1.dp, data.warna.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .padding(16.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = data.negara.uppercase(), color = data.warna, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(text = data.waktu, color = Color.Gray, fontSize = 10.sp)
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(text = data.entitas, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Jenis Anomali", color = Color.DarkGray, fontSize = 10.sp)
                Text(text = data.jenis, color = Color.LightGray, fontSize = 12.sp)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Skala Fisis", color = Color.DarkGray, fontSize = 10.sp)
                Text(text = data.skala, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Probabilitas", color = Color.DarkGray, fontSize = 10.sp)
                Text(text = data.probabilitas, color = Color.LightGray, fontSize = 12.sp)
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        Divider(color = Color(0xFF2A2A2A), thickness = 1.dp)
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(text = "Status: ${data.bahaya}", color = data.warna, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

// Data Proyektor Tetap Utuh
object DataSimulasi {
    val global = listOf(
        MatriksAnomali("Filipina", "Mindanao (Sesar Cotabato)", "Gempa Tektonik", "100% (Faktual)", "7.8 SR", "[AWAS] Keruntuhan Fatal", "17 Jun 2026, 08:12 WIB", Color.Red),
        MatriksAnomali("Jepang", "Ibaraki (Area Tokyo)", "Gempa Tektonik", "100% (Faktual)", "5.5 SR", "[SIAGA] Guncangan Signifikan", "17 Jun 2026, 06:45 WIB", Color(0xFFFFA500))
    )
    val domestik = listOf(
        MatriksAnomali("Indonesia", "Gunung Lewotobi, NTT", "Erupsi Vulkanik", "100% (Faktual)", "Level III", "[SIAGA] Interupsi Aviasi", "17 Jun 2026, 09:10 WIB", Color(0xFFFFA500)),
        MatriksAnomali("Indonesia", "Selatan Jawa, DIY", "Gempa Tektonik", "100% (Faktual)", "4.2 SR", "[WASPADA] Aktivitas Minor", "16 Jun 2026, 23:14 WIB", Color.Yellow)
    )
}
