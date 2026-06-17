package com.zf.zuhriweather

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.setContent
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
import androidx.core.content.ContextCompat
import androidx.glance.appwidget.updateAll
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
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
        
        setContent {
            val context = LocalContext.current
            val pref = remember { context.getSharedPreferences("ZF_STORAGE", Context.MODE_PRIVATE) }
            val gson = remember { Gson() }

            // METRONOM REAL-TIME (1 DETIK)
            var waktuRealTime by remember { mutableStateOf("- | -") }
            LaunchedEffect(Unit) {
                val formatter = SimpleDateFormat("EEEE, dd MMMM yyyy | HH:mm:ss", Locale("id", "ID"))
                while(true) {
                    waktuRealTime = formatter.format(Date())
                    delay(1000)
                }
            }

            // AMBANG BATAS OPERASIONAL MODE SPASIAL
            var modeSpasial by remember { mutableStateOf(pref.getString("opsi_mode", "RADAR") ?: "RADAR") }
            var namaLokasiDinamis by remember { mutableStateOf(pref.getString("meta_lokasi", "Blorok, Kendal") ?: "Blorok, Kendal") }

            // Pemicu Sinkronisasi Berkala (60 Detik)
            LaunchedEffect(modeSpasial) {
                while(true) {
                    eksekusiPindaiSatelit(context, modeSpasial)
                    delay(60000)
                }
            }

            // RE-RENDER TRIGGER KETIKA STORAGE BERUBAH
            var triggerRender by remember { mutableStateOf(0) }
            val listener = remember {
                Context.getSharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                    if (key == "waktu_sinkron" || key == "meta_lokasi") {
                        namaLokasiDinamis = pref.getString("meta_lokasi", "Blorok, Kendal") ?: "Blorok, Kendal"
                        triggerRender++
                    }
                }
            }
            
            LaunchedEffect(Unit) {
                pref.registerOnSharedPreferenceChangeListener(listener)
            }

            // LAUNCHER OTORISASI GPS ANDROID
            val requestPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestMultiplePermissions()
            ) { perms ->
                if (perms[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
                    eksekusiPindaiSatelit(context, "RADAR")
                }
            }

            // READ STATE DATA
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
            var tabIndex by remember { mutableStateOf(0) }
            val tabTitles = listOf("LOKAL", "DOMESTIK", "GLOBAL")

            Column(modifier = Modifier.fillMaxSize().background(Color(0xFF0A0A0A))) {
                
                // HEADER ABSOLUT (DENGAN DETAK METRONOM WAKTU)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("ZF SPATIAL", color = Color.Cyan, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        Text(namaLokasiDinamis, color = Color.LightGray, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        val parts = waktuRealTime.split(" | ")
                        if (parts.size == 2) {
                            Text(parts[0].uppercase(), color = Color.Yellow, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text("${parts[1]} WIB", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // ARSITEKTUR SAKELAR TARGET MANUAL (SELECTOR ROW)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val daftarMode = listOf("RADAR", "SAUNG", "KENDAL", "WELERI")
                    daftarMode.forEach { mode ->
                        val aktif = modeSpasial == mode
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (aktif) Color.Cyan else Color(0xFF1E1E1E))
                                .clickable {
                                    modeSpasial = mode
                                    pref.edit().putString("opsi_mode", mode).apply()
                                    if (mode == "RADAR" && !cekIzinLokasi(context)) {
                                        requestPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                                    } else {
                                        eksekusiPindaiSatelit(context, mode)
                                    }
                                }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(mode, color = if (aktif) Color.Black else Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))

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
                                SectionHeader("STATUS LITOSFER SEKITAR", Color.Red, "Data: $waktuSinkron WIB")
                                KartuLokalStatus(lokasi, skala, status, warnaStatus)
                                Spacer(modifier = Modifier.height(24.dp))

                                SectionHeader("TERMODINAMIKA ATMOSFER (NOWCAST)", Color(0xFF00BFFF), "Target: $namaLokasiDinamis")
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
                                    SectionHeader("PROYEKSI METEOROLOGI PER-JAM", Color.Yellow)
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        items(listJam) { jam -> KartuProyeksiJam(jam) }
                                    }
                                    Spacer(modifier = Modifier.height(24.dp))
                                }

                                if (listHari.isNotEmpty()) {
                                    SectionHeader("PROYEKSI TEMPORAL HARIAN (3 HARI)", Color.Yellow)
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
                            modifier = Modifier.fillMaxWidth().clickable { eksekusiPindaiSatelit(context, modeSpasial) }.padding(vertical = 16.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }

    private fun cekIzinLokasi(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    // ARSITEKTUR KOORDINAT DAN PROTOKOL FALLBACK LOKASI
    private fun eksekusiPindaiSatelit(context: Context, mode: String) {
        val pref = context.getSharedPreferences("ZF_STORAGE", Context.MODE_PRIVATE)
        
        // Matriks Koordinat Statis Override Manual
        var targetLat = pref.getFloat("last_lat", -6.9535f).toDouble()
        var targetLon = pref.getFloat("last_lon", 110.2312f).toDouble()
        var targetNama = "Blorok, Brangsong (Retensi)"

        when (mode) {
            "SAUNG" -> {
                targetLat = -7.0500 // Kalibrasikan koordinat fisis Saung JAWA di sini
                targetLon = 110.3000
                targetNama = "Saung JAWA, Kendal"
                tembakJaringanFisis(context, targetLat, targetLon, targetNama)
            }
            "KENDAL" -> {
                targetLat = -6.9200
                targetLon = 110.2000
                targetNama = "Kota Kendal"
                tembakJaringanFisis(context, targetLat, targetLon, targetNama)
            }
            "WELERI" -> {
                targetLat = -6.9740
                targetLon = 110.0650
                targetNama = "Kota Weleri"
                tembakJaringanFisis(context, targetLat, targetLon, targetNama)
            }
            "RADAR" -> {
                // PRIORITAS ABSOLUT: Ambil Data Perangkat GPS Aktif
                if (cekIzinLokasi(context)) {
                    try {
                        val fusedClient = LocationServices.getFusedLocationProviderClient(context)
                        fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                            .addOnSuccessListener { loc ->
                                if (loc != null) {
                                    targetLat = loc.latitude
                                    targetLon = loc.longitude
                                    targetNama = "Radar GPS: Dinamis"
                                    
                                    // Kunci Kerapatan Koordinat Ke Brankas Memori (Retensi)
                                    pref.edit().apply {
                                        putFloat("last_lat", targetLat.toFloat())
                                        putFloat("last_lon", targetLon.toFloat())
                                        apply()
                                    }
                                } else {
                                    targetNama = "Radar GPS Pasif (Blorok)"
                                }
                                tembakJaringanFisis(context, targetLat, targetLon, targetNama)
                            }
                            .addOnFailureListener {
                                tembakJaringanFisis(context, targetLat, targetLon, "Radar GPS Gagal (Blorok)")
                            }
                    } catch (e: SecurityException) {
                        tembakJaringanFisis(context, targetLat, targetLon, "Radar GPS Terblokir (Blorok)")
                    }
                } else {
                    // PRIORITAS TERSIER: Fallback Ke Desa Blorok Jika Izin Ditolak
                    tembakJaringanFisis(context, -6.9535, 110.2312, "Radar GPS Off (Blorok)")
                }
            }
        }
    }

    private fun tembakJaringanFisis(context: Context, lat: Double, lon: Double, namaMode: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val pref = context.getSharedPreferences("ZF_STORAGE", Context.MODE_PRIVATE)
            val gson = Gson()
            try {
                val respons = NetworkMatriks.api.getSinkronisasi(lat, lon, namaMode)
                val waktuSekarang = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                pref.edit().apply {
                    putString("suhu", respons.cuaca.suhu)
                    putString("angin", respons.cuaca.angin)
                    putString("kelembapan", respons.cuaca.kelembapan)
                    putString("awan", respons.cuaca.awan)
                    putString("presipitasi", respons.cuaca.presipitasi)
                    
                    putString("meta_lokasi", respons.meta_lokasi) // Menyimpan nama lokasi riil dari server
                    
                    putString("lokasi", respons.bencana.lokasi)
                    putString("skala", respons.bencana.skala)
                    putString("status", respons.bencana.status_bahaya)
                    putString("warna", respons.bencana.kode_warna)
                    putString("waktu_sinkron", waktuSekarang)
                    
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
    Column(modifier = modifier.clip(RoundedCornerShape(6.dp)).background(Color(0xFF1A1A1A)).border(1.dp, Color(0xFF333333)).padding(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) { Text(ikon, fontSize = 12.sp, modifier = Modifier.padding(end = 4.dp)); Text(label, color = Color.Gray, fontSize = 9.sp) }
        Spacer(modifier = Modifier.height(6.dp))
        Text(nilai, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun KartuProyeksiJam(jam: ProyeksiJam) {
    Column(
        modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(Color(0xFF1A1A1A)).border(1.dp, Color(0xFF333333)).padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(jam.waktu, color = Color.LightGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Text(jam.suhu, color = Color.White, fontSize = 13.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text("🌧️ ${jam.probabilitas_hujan}", color = Color(0xFF00BFFF), fontSize = 9.sp)
    }
}

@Composable
fun KartuProyeksiHari(hari: ProyeksiHari) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp).clip(RoundedCornerShape(6.dp)).background(Color(0xFF151515)).padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(hari.hari, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Text("Max: ${hari.suhu_max} | Min: ${hari.suhu_min}", color = Color.LightGray, fontSize = 11.sp)
        Text("🌧️ ${hari.prob_hujan}", color = Color(0xFF00BFFF), fontSize = 11.sp)
    }
}

@Composable
fun KartuLokalStatus(lokasi: String, skala: String, status: String, warna: Color) {
    Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Color(0xFF151515)).border(1.dp, warna.copy(alpha=0.5f)).padding(16.dp)) {
        Text("PUSAT EVALUASI RADIUS GEODESIS", color = warna, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text(lokasi, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Text("Skala Fisis: $skala", color = Color.LightGray, fontSize = 13.sp)
        Spacer(modifier = Modifier.height(12.dp))
        Text("Status Ancaman: $status", color = warna, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun KartuAnomaliJaringan(data: MatriksAnomaliNetwork) {
    val warnaVisual = parseWarnaZf(data.warna_kode)
    Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Color(0xFF151515)).border(1.dp, warnaVisual.copy(alpha=0.3f)).padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(data.negara.uppercase(), color = warnaVisual, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(data.waktu, color = Color.Gray, fontSize = 10.sp)
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(data.entitas, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) { Text("Jenis", color = Color.DarkGray, fontSize = 10.sp); Text(data.jenis, color = Color.LightGray, fontSize = 12.sp) }
            Column(modifier = Modifier.weight(1f)) { Text("Skala", color = Color.DarkGray, fontSize = 10.sp); Text(data.skala, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Divider(color = Color(0xFF2A2A2A), thickness = 1.dp)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Status: ${data.bahaya}", color = warnaVisual, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}
