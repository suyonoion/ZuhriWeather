package com.zf.zuhriweather

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.layout.ContentScale
import coil.compose.rememberAsyncImagePainter
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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

            // STATE MANAJEMEN SPASIAL
            var modeSpasial by remember { mutableStateOf(pref.getString("opsi_mode", "RADAR") ?: "RADAR") }
            var namaLokasiDinamis by remember { mutableStateOf(pref.getString("meta_lokasi", "Blorok, Kendal") ?: "Blorok, Kendal") }
            var hasKickedToSettings by remember { mutableStateOf(false) }
            
                        // PANEL KONTROL POP-UP
            var tampilDialogInfo by remember { mutableStateOf(false) }
            var tampilDetailGempa by remember { mutableStateOf(false) }
            var dataForensikAktif by remember { mutableStateOf(ParameterForensik()) } // MEMORI DINAMIS

            val requestPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestMultiplePermissions()
            ) { perms ->
                if (perms[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
                    eksekusiPindaiSatelit(context, "RADAR", hasKickedToSettings) { state -> hasKickedToSettings = state }
                }
            }

            LaunchedEffect(Unit) {
                if (modeSpasial == "RADAR" && !cekIzinLokasi(context)) {
                    requestPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                }
            }

            LaunchedEffect(modeSpasial) {
                while(true) {
                    eksekusiPindaiSatelit(context, modeSpasial, true) { state -> hasKickedToSettings = state }
                    delay(60000)
                }
            }

            var triggerRender by remember { mutableStateOf(0) }
            val listener = remember {
                android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                    if (key == "waktu_sinkron" || key == "meta_lokasi") {
                        namaLokasiDinamis = pref.getString("meta_lokasi", "Blorok, Kendal") ?: "Blorok, Kendal"
                        triggerRender++
                    }
                }
            }
            LaunchedEffect(Unit) { pref.registerOnSharedPreferenceChangeListener(listener) }

            // RESTRUKTURISASI MEMORI LOKAL
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
            
            // TARIK DATA KOORDINAT ANDA DAN GEMPA LOKAL DARI MEMORI
            val userLat = pref.getFloat("last_lat", -6.9535f).toDouble()
            val userLon = pref.getFloat("last_lon", 110.2312f).toDouble()
            
            val bencanaLat = pref.getFloat("bencana_lat", 0f).toDouble()
            val bencanaLon = pref.getFloat("bencana_lon", 0f).toDouble()
            val bencanaKedalaman = pref.getString("bencana_kedalaman", "-") ?: "-"
            val bencanaUrl = pref.getString("bencana_url", "-") ?: "-"
            var tabIndex by remember { mutableStateOf(0) }
            val tabTitles = listOf("LOKAL", "DOMESTIK", "GLOBAL") // TABULASI KEMBALI KE STRUKTUR TERITORIAL MURNI

            // KANVAS INDUK
            Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0A0A0A))) {
                
                // KONTEN UTAMA DASBOR
                Column(modifier = Modifier.fillMaxSize()) {
                    
                    // HEADER UTAMA
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("ZF SPATIAL", color = Color.Cyan, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("[ i ]", color = Color.Gray, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { tampilDialogInfo = true })
                            }
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

                    // SAKELAR TARGET SPASIAL
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
                                        if (mode == "RADAR") {
                                            if (!cekIzinLokasi(context)) {
                                                requestPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                                            } else {
                                                hasKickedToSettings = false
                                                eksekusiPindaiSatelit(context, mode, false) { state -> hasKickedToSettings = state }
                                            }
                                        } else {
                                            eksekusiPindaiSatelit(context, mode, true) { state -> hasKickedToSettings = state }
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

                    // BARIS TABULASI REGIONAL
                    TabRow(
                        selectedTabIndex = tabIndex, containerColor = Color(0xFF121212), contentColor = Color.Cyan,
                        indicator = { tabPositions -> TabRowDefaults.Indicator(Modifier.tabIndicatorOffset(tabPositions[tabIndex]), color = Color.Cyan) }
                    ) {
                        tabTitles.forEachIndexed { index, title ->
                            Tab(selected = tabIndex == index, onClick = { tabIndex = index }, text = { Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold) })
                        }
                    }

                    // DAFTAR DATA DINAMIS
                    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                                                when (tabIndex) {
                            0 -> { 
                                item {
                                    SectionHeader("STATUS GEMPA LOKAL", Color.Red, "Data: $waktuSinkron WIB")
               // FASE 3 LOKAL:
          KartuLokalStatus(lokasi, skala, status, warnaStatus) {
                 val jarakNyata = hitungJarakGeodesis(
                  lat1 = userLat,  // Koordinat mutlak Anda
                  lon1 = userLon, 
                  lat2 = bencanaLat, // Koordinat mutlak Gempa Lokal
                  lon2 = bencanaLon
                  )

              dataForensikAktif = ParameterForensik(
                  magnitudo = skala,
                  kedalaman = bencanaKedalaman,
                  koordinat = "$bencanaLat | $bencanaLon",
                  potensi = status,
                  waktu = "$waktuSinkron WIB",
                  lokasi = lokasi,
                  jarak = "$jarakNyata dari Posisi Radar Aktif",
                  urlPeta = bencanaUrl
              )
                  tampilDetailGempa = true
          }

            Spacer(modifier = Modifier.height(24.dp))

                                    SectionHeader("KONDISI CUACA SAAT INI", Color(0xFF00BFFF), "Target: $namaLokasiDinamis")
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        DataCard("Suhu", suhu, "🌡️", Modifier.weight(1f))
                                        Spacer(Modifier.width(8.dp))
                                        DataCard("Kec. Angin", angin, "💨", Modifier.weight(1f))
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        DataCard("Curah Hujan", presipitasi, "🌧️", Modifier.weight(1f))
                                        Spacer(Modifier.width(8.dp))
                                        DataCard("Awan", awan, "☁️", Modifier.weight(1f))
                                        Spacer(Modifier.width(8.dp))
                                        DataCard("Kelembapan", kelembapan, "💧", Modifier.weight(1f))
                                    }
                                    Spacer(modifier = Modifier.height(24.dp))

                                    if (listJam.isNotEmpty()) {
                                        SectionHeader("PRAKIRAAN CUACA PER-JAM", Color.Yellow)
                                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            items(listJam) { jam -> KartuProyeksiJam(jam) }
                                        }
                                        Spacer(modifier = Modifier.height(24.dp))
                                    }

                                    if (listHari.isNotEmpty()) {
                                        SectionHeader("PRAKIRAAN CUACA HARIAN (3 HARI)", Color.Yellow)
                                        listHari.forEach { hari -> KartuProyeksiHari(hari) }
                                    }
                                }
                            }
                            1 -> { 
        item { SectionHeader("GEMPA BUMI DOMESTIK (INDONESIA)", Color.Yellow) }
        if (listDomestik.isEmpty()) item { Text("Tidak ada gempa domestik signifikan.", color = Color.Gray) }
        
        items(listDomestik) { anomali -> 
            // FASE 3 DOMESTIK:
            KartuAnomaliJaringan(anomali) {
                // 1. Ambil data koordinat dari objek 'anomali' yang dikirim peladen
                // (Asumsi objek 'anomali' memiliki properti lat dan lon dari JSON Backend)
                val jarakKeEpisenter = hitungJarakGeodesis(
                    lat1 = userLat, 
                    lon1 = userLon, 
                    lat2 = anomali.latitude,  // Menarik data spasial peladen
                    lon2 = anomali.longitude
                )

                // 2. Timpa isi panel dengan data gempa domestik yang spesifik Anda ketuk
                dataForensikAktif = ParameterForensik(
                    magnitudo = anomali.skala,
                    kedalaman = anomali.kedalaman, 
                    koordinat = "${anomali.latitude} | ${anomali.longitude}",
                    potensi = anomali.bahaya,
                    waktu = anomali.waktu,
                    lokasi = anomali.entitas,
                    jarak = "$jarakKeEpisenter dari Posisi Anda",
                    urlPeta = anomali.url_peta // Mengambil gambar shakemap BMKG dari backend
                )
                tampilDetailGempa = true // Buka panel
            }
            Spacer(modifier = Modifier.height(12.dp)) 
        }
    }
                              2 -> { 
        item { SectionHeader("GEMPA BUMI GLOBAL (MAG ≥ 5.0)", Color.Red) }
        if (listGlobal.isEmpty()) item { Text("Tidak ada gempa global signifikan.", color = Color.Gray) }
        
        items(listGlobal) { anomali -> 
            // FASE 3 GLOBAL:
            KartuAnomaliJaringan(anomali) {
                // 1. Eksekusi kalkulasi jarak lintang benua/samudra (Haversine Absolut)
                val jarakKeEpisenter = hitungJarakGeodesis(
                    lat1 = userLat, 
                    lon1 = userLon, 
                    lat2 = anomali.latitude,  // Variabel Lintang dari peladen
                    lon2 = anomali.longitude  // Variabel Bujur dari peladen
                )

                // 2. Transmisi data fisis ke Panel Master Forensik
                dataForensikAktif = ParameterForensik(
                    magnitudo = anomali.skala,
                    kedalaman = anomali.kedalaman ?: "Data Tidak Tersedia", 
                    koordinat = "${anomali.latitude} | ${anomali.longitude}",
                    potensi = anomali.bahaya,
                    waktu = anomali.waktu,
                    lokasi = "${anomali.entitas} (${anomali.negara.uppercase()})",
                    jarak = "$jarakKeEpisenter dari Posisi Anda",
                    urlPeta = anomali.url_peta ?: "-" // Tautan visual USGS/BMKG jika ada
                )
                tampilDetailGempa = true // Buka Tirai Forensik
            }
            Spacer(modifier = Modifier.height(12.dp)) 
        }
    }

                        }

                        item {
                            Spacer(modifier = Modifier.height(24.dp))
                            Divider(color = Color.DarkGray, thickness = 1.dp)
                            Text(
                                text = "[ ↻ PERBARUI DATA SATELIT ]",
                                color = Color.Green, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                                modifier = Modifier.fillMaxWidth().clickable { 
                                    hasKickedToSettings = false
                                    eksekusiPindaiSatelit(context, modeSpasial, false) { state -> hasKickedToSettings = state } 
                                }.padding(vertical = 16.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // DIALOG INFORMASI (ABOUT)
                if (tampilDialogInfo) {
                    AlertDialog(
                        onDismissRequest = { tampilDialogInfo = false },
                        containerColor = Color(0xFF1A1A1A),
                        title = { Text("PROTOKOL IDENTITAS", color = Color.Cyan, fontSize = 16.sp, fontWeight = FontWeight.Bold) },
                        text = {
                            Column {
                                Text("ZUHRI SPATIAL MONITOR", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text("Versi Absolut 1.0.0", color = Color.Gray, fontSize = 12.sp)
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("Arsitektur Inti: Zuhri Formalism", color = Color.Yellow, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text("Otoritas Lapangan: Mas Ion (Submawil Kendal)", color = Color.White, fontSize = 12.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Sistem ini mengekstrak data litosfer dan termodinamika secara mutlak tanpa rekayasa antarmuka.", color = Color.LightGray, fontSize = 11.sp)
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { tampilDialogInfo = false }) {
                                Text("[ TUTUP ]", color = Color.Red, fontWeight = FontWeight.Bold)
                            }
                        }
                    )
                }

                // --- LAYER INTERAKTIF: PANEL DETAIL GEMPA FORENSIK (M >= 5) ---
                AnimatedVisibility(
                    visible = tampilDetailGempa,
                    enter = slideInVertically(initialOffsetY = { it }),
                    exit = slideOutVertically(targetOffsetY = { it })
                ) {
                    // Tirai Gelap Belakang
                    Box(
                        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.7f)).clickable { tampilDetailGempa = false },
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        // Kotak Utama Konsol Forensik
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(0.85f) // Memakan 85% dimensi vertikal layar
                                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                                .background(Color(0xFF121212))
                                .border(1.dp, Color(0xFF2A2A2A), RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                                .clickable(enabled = false) { /* Menghentikan penutupan tak sengaja jika area dalam ditekan */ }
                                .padding(16.dp)
                        ) {
                            // Header Panel Forensik
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("FORENSIK GEMPA OPERASIONAL", color = Color.Red, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text("[ X ] CLOSE", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { tampilDetailGempa = false })
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))

                                                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                                item {
                                    SectionHeader("PETA SEBARAN GUNCANGAN (EPISENTER)", Color.Yellow)
                              KartuPetaGuncangan(urlPeta = dataForensikAktif.urlPeta)
                                    Spacer(modifier = Modifier.height(16.dp))
                                    
                                    SectionHeader("PARAMETER INTENSITAS UTAMA", Color.Red)
                                    KartuGempaUtama(
                                        magnitudo = dataForensikAktif.magnitudo,
                                        kedalaman = dataForensikAktif.kedalaman,
                                        koordinat = dataForensikAktif.koordinat,
                                        potensi = dataForensikAktif.potensi
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    
                                    SectionHeader("DETAIL SPASIAL & TEMPORAL", Color(0xFF00BFFF))
                                    KartuDetailGempa(
                                        waktu = dataForensikAktif.waktu,
                                        lokasiGempa = dataForensikAktif.lokasi,
                                        jarak = dataForensikAktif.jarak
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                }
                            }

                        }
                    }
                }
                // --- AKHIR LAYER INTERAKTIF ---
            }
        }
    }

    private fun cekIzinLokasi(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    private fun eksekusiPindaiSatelit(context: Context, mode: String, hasKicked: Boolean, updateKickState: (Boolean) -> Unit) {
        val pref = context.getSharedPreferences("ZF_STORAGE", Context.MODE_PRIVATE)
        var targetLat = pref.getFloat("last_lat", -6.9535f).toDouble()
        var targetLon = pref.getFloat("last_lon", 110.2312f).toDouble()

        val lastUserLokasi = pref.getString("last_user_lokasi", "Blorok, Brangsong, Kab. Kendal") ?: "Blorok, Brangsong, Kab. Kendal"
        val namaBersih = lastUserLokasi.split(" (")[0]

        when (mode) {
            "SAUNG" -> tembakJaringanFisis(context, -7.0500, 110.3000, "Saung JAWA, Kendal", false)
            "KENDAL" -> tembakJaringanFisis(context, -6.9200, 110.2000, "Kota Kendal", false)
            "WELERI" -> tembakJaringanFisis(context, -6.9740, 110.0650, "Kota Weleri", false)
            "RADAR" -> {
                if (cekIzinLokasi(context)) {
                    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                    val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
                    
                    if (!isGpsEnabled) {
                        if (!hasKicked) {
                            tembakJaringanFisis(context, targetLat, targetLon, "$namaBersih (Menunggu Aktivasi GPS)", false)
                            context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                            updateKickState(true)
                        } else {
                            tembakJaringanFisis(context, targetLat, targetLon, "$namaBersih (Memori Tersimpan)", false)
                        }
                        return
                    }

                    try {
                        val fusedClient = LocationServices.getFusedLocationProviderClient(context)
                        fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                            .addOnSuccessListener { loc ->
                                if (loc != null) {
                                    targetLat = loc.latitude
                                    targetLon = loc.longitude
                                    pref.edit().apply {
                                        putFloat("last_lat", targetLat.toFloat())
                                        putFloat("last_lon", targetLon.toFloat())
                                        apply()
                                    }
                                    tembakJaringanFisis(context, targetLat, targetLon, "Mendeteksi Koordinat...", true)
                                } else {
                                    tembakJaringanFisis(context, targetLat, targetLon, "$namaBersih (Memori Tersimpan)", false)
                                }
                            }
                            .addOnFailureListener {
                                tembakJaringanFisis(context, targetLat, targetLon, "$namaBersih (Memori Tersimpan)", false)
                            }
                    } catch (e: SecurityException) {
                        tembakJaringanFisis(context, targetLat, targetLon, "$namaBersih (Izin Lokasi Diblokir)", false)
                    }
                } else {
                    tembakJaringanFisis(context, targetLat, targetLon, "Izin Lokasi Ditolak", false)
                }
            }
        }
    }

    private fun tembakJaringanFisis(context: Context, lat: Double, lon: Double, namaAwal: String, perluGeocode: Boolean) {
        CoroutineScope(Dispatchers.IO).launch {
            val pref = context.getSharedPreferences("ZF_STORAGE", Context.MODE_PRIVATE)
            val gson = Gson()
            
            var namaFinal = namaAwal
            if (perluGeocode) {
                try {
                    val geocoder = Geocoder(context, Locale("id", "ID"))
                    val alamat = geocoder.getFromLocation(lat, lon, 1)
                    if (!alamat.isNullOrEmpty()) {
                        val detail = alamat[0]
                        val desa = detail.locality ?: detail.subAdminArea ?: detail.thoroughfare ?: "Koordinat Lokasi"
                        val kota = detail.adminArea ?: "Tidak Terdaftar"
                        namaFinal = "$desa, $kota"
                    }
                } catch (e: Exception) {
                    namaFinal = "Satelit: ${lat.toString().take(7)}, ${lon.toString().take(7)}"
                }
            }

            try {
                val respons = NetworkMatriks.api.getSinkronisasi(lat, lon, namaFinal)
                val waktuSekarang = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                pref.edit().apply {
                    putString("suhu", respons.cuaca.suhu)
                    putString("angin", respons.cuaca.angin)
                    putString("kelembapan", respons.cuaca.kelembapan)
                    putString("awan", respons.cuaca.awan)
                    putString("presipitasi", respons.cuaca.presipitasi)
                    
                    putString("meta_lokasi", respons.meta_lokasi)
                    
                    if (perluGeocode) {
                        putString("last_user_lokasi", respons.meta_lokasi)
                    }
                    
                    putString("lokasi", respons.bencana.lokasi)
                    putString("skala", respons.bencana.skala)
                    putString("status", respons.bencana.status_bahaya)
                    putString("warna", respons.bencana.kode_warna)
                    // SUNTIKKAN MEMORI SPASIAL BARU:
                    putFloat("bencana_lat", respons.bencana.latitude.toFloat())
                    putFloat("bencana_lon", respons.bencana.longitude.toFloat())
                    putString("bencana_kedalaman", respons.bencana.kedalaman)
                    putString("bencana_url", respons.bencana.url_peta)
                    putString("waktu_sinkron", waktuSekarang)
                    
                    putString("proyeksi_jam", gson.toJson(respons.proyeksi_cuaca.per_jam))
                    putString("proyeksi_hari", gson.toJson(respons.proyeksi_cuaca.harian))
                    putString("data_domestik", gson.toJson(respons.data_domestik))
                    putString("data_global", gson.toJson(respons.data_global))
                    apply()
                }
                ZuhriWidget().updateAll(context)
            } catch (e: Exception) {
                val lokasiTerakhir = pref.getString("meta_lokasi", "Lokasi Tidak Diketahui") ?: "Lokasi Tidak Diketahui"
                val lokasiBersih = lokasiTerakhir.replace(" ⚠️ [OFFLINE]", "")
                pref.edit().apply {
                    putString("meta_lokasi", "$lokasiBersih \u26A0\uFE0F [OFFLINE]")
                    apply()
                }
                ZuhriWidget().updateAll(context)
            }
        }
    }
}

// ================= KOMPONEN UX / UI PUBLIK ================= //

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
    val persenAngka = jam.probabilitas_hujan.replace("%", "").toIntOrNull() ?: 0
    val (ikon, warnaIkon) = when {
        persenAngka >= 60 -> "🌧️" to Color(0xFF00BFFF)
        persenAngka >= 20 -> "🌦️" to Color(0xFFFFA500)
        else -> "☀️" to Color.Yellow
    }
    Column(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(Color(0xFF1A1A1A)).border(1.dp, Color(0xFF333333)).padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(jam.waktu, color = Color.LightGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Text(jam.suhu, color = Color.White, fontSize = 13.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text("$ikon ${jam.probabilitas_hujan}", color = warnaIkon, fontSize = 9.sp)
    }
}

@Composable
fun KartuProyeksiHari(hari: ProyeksiHari) {
    val persenAngka = hari.prob_hujan.replace("%", "").toIntOrNull() ?: 0
    val (ikon, warnaIkon) = when {
        persenAngka >= 60 -> "🌧️" to Color(0xFF00BFFF)
        persenAngka >= 20 -> "🌦️" to Color(0xFFFFA500)
        else -> "☀️" to Color.Yellow
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp).clip(RoundedCornerShape(6.dp)).background(Color(0xFF151515)).padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(hari.hari, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Text("Max: ${hari.suhu_max} | Min: ${hari.suhu_min}", color = Color.LightGray, fontSize = 11.sp)
        Text("$ikon ${hari.prob_hujan}", color = warnaIkon, fontSize = 11.sp)
    }
}

// INJEKSI CIRKUIT LAMBUNG KETUKAN (ONCLICK) PADA KARTU LOKAL
@Composable
fun KartuLokalStatus(lokasi: String, skala: String, status: String, warna: Color, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF151515))
            .border(1.dp, warna.copy(alpha=0.5f))
            .clickable { onClick() } // MENGAKTIFKAN DETEKSI SENTUHAN UTAMA
            .padding(16.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("TITIK PANTAU GEMPA", color = warna, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text("[ FORENSIK ↗ ]", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(lokasi, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Text("Magnitudo: $skala", color = Color.LightGray, fontSize = 13.sp)
        Spacer(modifier = Modifier.height(12.dp))
        Text("Status: $status", color = warna, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun KartuAnomaliJaringan(data: MatriksAnomaliNetwork, onClick: () -> Unit) {
    val warnaVisual = parseWarnaZf(data.warna_kode)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF151515))
            .border(1.dp, warnaVisual.copy(alpha=0.3f))
            .clickable { onClick() } // MENGAKTIFKAN DETEKSI SENTUHAN
            .padding(16.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(data.negara.uppercase(), color = warnaVisual, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(data.waktu, color = Color.Gray, fontSize = 10.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text("[ FORENSIK ↗ ]", color = Color.LightGray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(data.entitas, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) { Text("Kategori", color = Color.DarkGray, fontSize = 10.sp); Text(data.jenis, color = Color.LightGray, fontSize = 12.sp) }
            Column(modifier = Modifier.weight(1f)) { Text("Magnitudo", color = Color.DarkGray, fontSize = 10.sp); Text(data.skala, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Divider(color = Color(0xFF2A2A2A), thickness = 1.dp)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Status: ${data.bahaya}", color = warnaVisual, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}


// ================= LAYOUT FORENSIK POP-UP INDEPENDEN ================= //

// ================= RENDER PETA GUNCANGAN DINAMIS ================= //
@Composable
fun KartuPetaGuncangan(urlPeta: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF0F0F0F))
            .border(1.dp, Color(0xFF333333)),
        contentAlignment = Alignment.Center
    ) {
        if (urlPeta.isNotEmpty() && urlPeta != "-") {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(urlPeta)
                    // SUNTIKAN IDENTITAS PALSU UNTUK MENEMBUS BLOKADE SERVER PETA (OSM)
                    .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
                    .crossfade(true)
                    .build(),
                contentDescription = "Matriks Sebaran Guncangan Litosfer",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("DATA PETA GUNCANGAN ABSEN", color = Color.Red, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Menunggu transmisi dari jaringan...", color = Color.Gray, fontSize = 10.sp)
            }
        }
    }
}

@Composable
fun KartuGempaUtama(magnitudo: String, kedalaman: String, koordinat: String, potensi: String) {
    Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Color(0xFF151515)).border(1.dp, Color.Red.copy(alpha=0.3f)).padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("MAGNITUDO", color = Color.DarkGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text(magnitudo, color = Color.Red, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("KEDALAMAN", color = Color.DarkGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text(kedalaman, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(koordinat, color = Color.Cyan, fontSize = 11.sp)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Divider(color = Color(0xFF2A2A2A), thickness = 1.dp)
        Spacer(modifier = Modifier.height(12.dp))
        val warnaPotensial = if (potensi.contains("Tidak")) Color.Green else Color.Red
        Text("Peringatan: $potensi", color = warnaPotensial, fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
fun KartuDetailGempa(waktu: String, lokasiGempa: String, jarak: String) {
    Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Color(0xFF151515)).padding(16.dp)) {
        DetailBarisData("Waktu Kejadian", waktu)
        Spacer(modifier = Modifier.height(12.dp))
        DetailBarisData("Lokasi Episenter", lokasiGempa)
        Spacer(modifier = Modifier.height(12.dp))
        DetailBarisData("Jarak Geodesis", jarak)
    }
}

@Composable
fun DetailBarisData(label: String, nilai: String) {
    Column {
        Text(label, color = Color.Gray, fontSize = 10.sp)
        Spacer(modifier = Modifier.height(2.dp))
        Text(nilai, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

// ================= STRUKTUR MEMORI FORENSIK ================= //
data class ParameterForensik(
    val magnitudo: String = "-",
    val kedalaman: String = "-",
    val koordinat: String = "-",
    val potensi: String = "-",
    val waktu: String = "-",
    val lokasi: String = "-",
    val jarak: String = "-",
    val urlPeta: String = ""
)

// ================= KALKULATOR SPASIAL (HAVERSINE FORMULA) ================= //
fun hitungJarakGeodesis(lat1: Double, lon1: Double, lat2: Double, lon2: Double): String {
    val rBumi = 6371.0 // Radius mutlak bumi dalam kilometer
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    
    val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
            Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
            Math.sin(dLon / 2) * Math.sin(dLon / 2)
            
    val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
    val jarak = rBumi * c
    
    return String.format("%.1f KM", jarak)
}
