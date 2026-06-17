package com.zf.zuhriweather

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        segarkanMatriksFisis(this)

        setContent {
            val context = LocalContext.current
            
            // INJEKSI METRONOM WAKTU LOKAL (Detak 1 Detik)
            var waktuRealTime by remember { mutableStateOf("- | -") }
            LaunchedEffect(Unit) {
                val formatter = SimpleDateFormat("EEEE, dd MMMM yyyy | HH:mm:ss", Locale("id", "ID"))
                while(true) {
                    waktuRealTime = formatter.format(Date())
                    delay(1000)
                }
            }

            // SIKLUS SINKRONISASI SATELIT (Detak 60 Detik)
            LaunchedEffect(Unit) {
                while(true) {
                    delay(60000)
                    segarkanMatriksFisis(context)
                }
            }

            var tabIndex by remember { mutableStateOf(0) }
            val tabTitles = listOf("LOKAL", "DOMESTIK", "GLOBAL")
            
            val pref = context.getSharedPreferences("ZF_STORAGE", Context.MODE_PRIVATE)
            val gson = remember { Gson() }

            val suhu = pref.getString("suhu", "-") ?: "-"
            val angin = pref.getString("angin", "-") ?: "-"
            val kelembapan = pref.getString("kelembapan", "-") ?: "-"
            val awan = pref.getString("awan", "-") ?: "-"
            val presipitasi = pref.getString("presipitasi", "-") ?: "-"
            val lokasi = pref.getString("lokasi", "Menunggu...") ?: "-"
            val skala = pref.getString("skala", "-") ?: "-"
            val status = pref.getString("status", "Standby") ?: "Standby"
            val warnaCode = pref.getString("warna", "Gray") ?: "Gray"
            val waktuSinkron = pref.getString("waktu_sinkron", "-") ?: "-"

            val typeJam = object : TypeToken<List<ProyeksiJam>>() {}.type
            val typeHari = object : TypeToken<List<ProyeksiHari>>() {}.type
            val typeAnomali = object : TypeToken<List<MatriksAnomaliNetwork>>() {}.type

            val listJam: List<ProyeksiJam> = gson.fromJson(pref.getString("proyeksi_jam", "[]"), typeJam) ?: emptyList()
            val listHari: List<ProyeksiHari> = gson.fromJson(pref.getString("proyeksi_hari", "[]"), typeHari) ?: emptyList()
            val listDomestik: List<MatriksAnomaliNetwork> = gson.fromJson(pref.getString("data_domestik", "[]"), typeAnomali) ?: emptyList()
            val listGlobal: List<MatriksAnomaliNetwork> = gson.fromJson(pref.getString("data_global", "[]"), typeAnomali) ?: emptyList()

            val warnaStatus = parseWarnaZf(warnaCode)

            Column(modifier = Modifier.fillMaxSize().background(Color(0xFF0A0A0A))) {
                // HEADER ABSOLUT (WAKTU AKTIF DI POJOK KANAN ATAS)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("ZF SPATIAL", color = Color.Cyan, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        Text("Pusat Kendali: Kendal", color = Color.Gray, fontSize = 12.sp)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        val parts = waktuRealTime.split(" | ")
                        if (parts.size == 2) {
                            Text(parts[0].uppercase(), color = Color.Yellow, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text("${parts[1]} WIB", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                TabRow(
                    selectedTabIndex = tabIndex, containerColor = Color(0xFF121212), contentColor = Color.Cyan,
                    indicator = { tabPositions -> TabRowDefaults.Indicator(Modifier.tabIndicatorOffset(tabPositions[tabIndex]), color = Color.Cyan) }
                ) {
                    tabTitles.forEachIndexed { index, title ->
                        Tab(selected = tabIndex == index, onClick = { tabIndex = index }, text = { Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold) })
                    }
                }

                LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    when (tabIndex) {
                        0 -> { 
                            item {
                                SectionHeader("STATUS LITOSFER LOKAL", Color.Red, "Data: $waktuSinkron WIB")
                                KartuLokalStatus(lokasi, skala, status, waktuSinkron, warnaStatus)
                                Spacer(modifier = Modifier.height(24.dp))

                                SectionHeader("TERMODINAMIKA LOKAL (NOWCAST)", Color(0xFF00BFFF), "Data: $waktuSinkron WIB")
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    DataCard("Suhu Aktif", suhu, "🌡️", Modifier.weight(1f))
                                    Spacer(Modifier.width(8.dp))
                                    DataCard("Vektor Angin", angin, "💨", Modifier.weight(1f))
                                }
                                Spacer(Modifier.height(8.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    DataCard("Presipitasi", presipitasi, "🌧️", Modifier.weight(1f))
                                    Spacer(Modifier.width(8.dp))
                                    DataCard("Tutupan Foton", awan, "☁️", Modifier.weight(1f))
                                    Spacer(Modifier.width(8.dp))
                                    DataCard("Kerapatan (RH)", kelembapan, "💧", Modifier.weight(1f))
                                }
                                Spacer(modifier = Modifier.height(24.dp))

                                if (listJam.isNotEmpty()) {
                                    SectionHeader("PROYEKSI 6 JAM KE DEPAN", Color.Yellow)
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        items(listJam) { jam -> KartuProyeksiJam(jam) }
                                    }
                                    Spacer(modifier = Modifier.height(24.dp))
                                }

                                if (listHari.isNotEmpty()) {
                                    SectionHeader("PROYEKSI HARIAN (3 HARI)", Color.Yellow)
                                    listHari.forEach { hari -> KartuProyeksiHari(hari) }
                                }
                            }
                        }
                        1 -> { 
                            item { SectionHeader("RUPTUR TERITORIAL (INDONESIA)", Color.Yellow) }
                            if (listDomestik.isEmpty()) item { Text("Litosfer Domestik Stabil.", color = Color.Gray) }
                            items(listDomestik) { anomali -> KartuAnomaliJaringan(anomali); Spacer(modifier = Modifier.height(12.dp)) }
                        }
                        2 -> { 
                            item { SectionHeader("MATRIKS DESTRUKTIF GLOBAL (MAG ≥ 5.0)", Color.Red) }
                            if (listGlobal.isEmpty()) item { Text("Litosfer Global Stabil.", color = Color.Gray) }
                            items(listGlobal) { anomali -> KartuAnomaliJaringan(anomali); Spacer(modifier = Modifier.height(12.dp)) }
                        }
                    }
                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                        Divider(color = Color.DarkGray, thickness = 1.dp)
                        Text(
                            text = "[ ↻ PAKSA SINKRONISASI SATELIT ]",
                            color = Color.Green, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.fillMaxWidth().clickable { segarkanMatriksFisis(context) }.padding(vertical = 16.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }

    private fun segarkanMatriksFisis(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            val pref = context.getSharedPreferences("ZF_STORAGE", Context.MODE_PRIVATE)
            val gson = Gson()
            try {
                val respons = NetworkMatriks.api.getSinkronisasi()
                val waktuSekarang = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                pref.edit().apply {
                    putString("suhu", respons.cuaca.suhu); putString("angin", respons.cuaca.angin)
                    putString("kelembapan", respons.cuaca.kelembapan); putString("awan", respons.cuaca.awan)
                    putString("presipitasi", respons.cuaca.presipitasi); putString("lokasi", respons.bencana.lokasi)
                    putString("skala", respons.bencana.skala); putString("status", respons.bencana.status_bahaya)
                    putString("warna", respons.bencana.kode_warna); putString("waktu_sinkron", waktuSekarang)
                    putString("proyeksi_jam", gson.toJson(respons.proyeksi_cuaca.per_jam))
                    putString("proyeksi_hari", gson.toJson(respons.proyeksi_cuaca.harian))
                    putString("data_domestik", gson.toJson(respons.data_domestik))
                    putString("data_global", gson.toJson(respons.data_global))
                    apply()
                }
                ZuhriWidget().updateAll(context)
            } catch (e: Exception) {}
        }
    }
}

// ================= KOMPONEN UX / UI FISIS ================= //

fun parseWarnaZf(kode: String): Color {
    return when(kode) { "Red" -> Color.Red; "Orange" -> Color(0xFFFFA500); "Yellow" -> Color.Yellow; "Green" -> Color.Green; else -> Color.Gray }
}

@Composable
fun SectionHeader(judul: String, warna: Color, waktu: String = "") {
    Row(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(judul, color = warna, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        if (waktu.isNotEmpty()) { Text(waktu, color = Color.Gray, fontSize = 10.sp, textAlign = TextAlign.End) }
    }
}

@Composable
fun DataCard(label: String, nilai: String, ikon: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier.clip(RoundedCornerShape(6.dp)).background(Color(0xFF1A1A1A)).border(1.dp, Color(0xFF333333)).padding(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) { Text(ikon, fontSize = 14.sp, modifier = Modifier.padding(end = 4.dp)); Text(label, color = Color.Gray, fontSize = 10.sp) }
        Spacer(modifier = Modifier.height(8.dp))
        Text(nilai, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun KartuProyeksiJam(jam: ProyeksiJam) {
    Column(
        modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(Color(0xFF1A1A1A)).border(1.dp, Color(0xFF333333)).padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(jam.waktu, color = Color.LightGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Text(jam.suhu, color = Color.White, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text("🌧️ ${jam.probabilitas_hujan}", color = Color(0xFF00BFFF), fontSize = 10.sp)
    }
}

@Composable
fun KartuProyeksiHari(hari: ProyeksiHari) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp).clip(RoundedCornerShape(6.dp)).background(Color(0xFF151515)).padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(hari.hari, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Text("Max: ${hari.suhu_max} | Min: ${hari.suhu_min}", color = Color.LightGray, fontSize = 11.sp)
        Text("🌧️ ${hari.prob_hujan}", color = Color(0xFF00BFFF), fontSize = 12.sp)
    }
}

@Composable
fun KartuLokalStatus(lokasi: String, skala: String, status: String, waktu: String, warna: Color) {
    Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Color(0xFF151515)).border(1.dp, warna.copy(alpha=0.5f)).padding(16.dp)) {
        Text("PUSAT PELACAKAN SAAT INI", color = warna, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text(lokasi, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Text("Skala Fisis: $skala", color = Color.LightGray, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(12.dp))
        Text("Status: $status", color = warna, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun KartuAnomaliJaringan(data: MatriksAnomaliNetwork) {
    val warnaVisual = parseWarnaZf(data.warna_kode)
    Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Color(0xFF151515)).border(1.dp, warnaVisual.copy(alpha=0.3f)).padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(data.negara.uppercase(), color = warnaVisual, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(data.waktu, color = Color.Gray, fontSize = 10.sp)
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(data.entitas, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) { Text("Jenis", color = Color.DarkGray, fontSize = 10.sp); Text(data.jenis, color = Color.LightGray, fontSize = 12.sp) }
            Column(modifier = Modifier.weight(1f)) { Text("Skala", color = Color.DarkGray, fontSize = 10.sp); Text(data.skala, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Divider(color = Color(0xFF2A2A2A), thickness = 1.dp)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Status: ${data.bahaya}", color = warnaVisual, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}
