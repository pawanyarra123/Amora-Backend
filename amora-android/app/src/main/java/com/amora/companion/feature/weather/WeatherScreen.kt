package com.amora.companion.feature.weather

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.amora.companion.core.theme.*
import com.amora.companion.feature.home.DashboardViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.net.URLEncoder

data class SavedCity(
    val name: String,
    val country: String,
    val lat: Double,
    val lon: Double,
    val isCurrentLocation: Boolean = false
)

data class LiveHourlyItem(val time: String, val temp: String, val icon: String)
data class LiveDailyItem(val day: String, val tempRange: String, val condition: String, val icon: String)

data class WeatherDetailState(
    val cityName: String = "Chennai",
    val temp: String = "28°C",
    val condition: String = "Clear Sky",
    val icon: String = "☀️",
    val humidity: String = "65%",
    val wind: String = "12 km/h",
    val uv: String = "3.5",
    val hourly: List<LiveHourlyItem> = emptyList(),
    val weekly: List<LiveDailyItem> = emptyList(),
    val isLoading: Boolean = false
)

@Composable
fun WeatherScreen(
    themeName: String = "Midnight Void",
    viewModel: WeatherViewModel = hiltViewModel()
) {
    val currentPalette = remember(themeName) { AmoraThemeSystem.getPalette(themeName) }
    val scope = rememberCoroutineScope()

    // Default saved cities
    val savedCities = remember {
        mutableStateListOf(
            SavedCity("Current Location", "Local GPS", 13.0827, 80.2707, isCurrentLocation = true),
            SavedCity("Chennai", "India", 13.0827, 80.2707),
            SavedCity("London", "UK", 51.5074, -0.1278),
            SavedCity("New York", "USA", 40.7128, -74.0060),
            SavedCity("Tokyo", "Japan", 35.6762, 139.6503)
        )
    }

    var selectedCity by remember { mutableStateOf(savedCities[0]) }
    var weatherState by remember { mutableStateOf(WeatherDetailState()) }

    // Dialog state for adding a new city
    var showAddCityDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<SavedCity>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }

    // Fetches weather via the backend (with a direct-API fallback if the backend's
    // unreachable) instead of always hitting Open-Meteo directly from the phone.
    fun fetchLiveWeather(city: SavedCity) {
        weatherState = weatherState.copy(isLoading = true)
        viewModel.fetchWeather(city) { result ->
            weatherState = result
        }
    }

    // Trigger initial fetch when city is selected
    LaunchedEffect(selectedCity) {
        fetchLiveWeather(selectedCity)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // ── City Switcher Bar ───────────────────────────────────────────────
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🌤️", fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("Live Real-Time Weather", color = currentPalette.textColor, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("Current GPS location & custom saved cities", color = currentPalette.subtextColor, fontSize = 11.sp)
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(currentPalette.accentColor.copy(alpha = 0.15f))
                        .border(1.dp, currentPalette.accentColor.copy(alpha = 0.35f), CircleShape)
                        .clickable { showAddCityDialog = true }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text("+ Add City", color = currentPalette.accentColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Saved Cities Selector Chips
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(savedCities) { city ->
                    val isSelected = selectedCity.name == city.name
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(
                                if (isSelected) currentPalette.accentColor else currentPalette.surfaceColor.copy(alpha = 0.6f)
                            )
                            .border(
                                1.dp,
                                if (isSelected) currentPalette.accentColor else currentPalette.subtextColor.copy(alpha = 0.2f),
                                CircleShape
                            )
                            .clickable { selectedCity = city }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (city.isCurrentLocation) "📍 ${city.name}" else city.name,
                            color = if (isSelected) Color.White else currentPalette.textColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // ── Main Weather Display Card ─────────────────────────────────────────
        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 20.dp,
                backgroundColor = currentPalette.surfaceColor.copy(alpha = 0.85f),
                borderColor = currentPalette.accentColor.copy(alpha = 0.25f)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "LOCATION: ${weatherState.cityName.uppercase()}",
                            color = currentPalette.accentColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        )
                        if (weatherState.isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = currentPalette.accentColor, strokeWidth = 2.dp)
                        } else {
                            Text("● LIVE", color = FigmaGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(weatherState.icon, fontSize = 48.sp)
                    Text(
                        text = "${weatherState.temp} ${weatherState.condition}",
                        color = currentPalette.textColor,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        WeatherStatTile("Humidity", weatherState.humidity, currentPalette)
                        WeatherStatTile("Wind Speed", weatherState.wind, currentPalette)
                        WeatherStatTile("UV Index", weatherState.uv, currentPalette)
                    }
                }
            }
        }

        // ── Hourly Forecast ───────────────────────────────────────────────────
        item {
            SectionLabel("Hourly Forecast")
            Spacer(modifier = Modifier.height(4.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(weatherState.hourly) { item ->
                    GlassCard(
                        modifier = Modifier.width(72.dp),
                        cornerRadius = 16.dp,
                        backgroundColor = currentPalette.surfaceColor.copy(alpha = 0.7f)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(item.time, color = currentPalette.subtextColor, fontSize = 10.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(item.icon, fontSize = 22.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(item.temp, color = currentPalette.textColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // ── 7-Day Forecast ────────────────────────────────────────────────────
        item {
            SectionLabel("7-Day Forecast")
        }

        items(weatherState.weekly) { day ->
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 16.dp,
                backgroundColor = currentPalette.surfaceColor.copy(alpha = 0.7f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(day.icon, fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(day.day, color = currentPalette.textColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text(day.condition, color = currentPalette.subtextColor, fontSize = 11.sp)
                        }
                    }
                    Text(day.tempRange, color = currentPalette.accentColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // Add City Dialog
    if (showAddCityDialog) {
        AlertDialog(
            onDismissRequest = { showAddCityDialog = false },
            confirmButton = {
                TextButton(onClick = { showAddCityDialog = false }) {
                    Text("Close", color = currentPalette.subtextColor)
                }
            },
            title = { Text("Search & Add City", color = currentPalette.textColor, fontSize = 17.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { query ->
                            searchQuery = query
                            if (query.length >= 2) {
                                scope.launch {
                                    isSearching = true
                                    try {
                                        val encoded = URLEncoder.encode(query, "UTF-8")
                                        val url = "https://geocoding-api.open-meteo.com/v1/search?name=$encoded&count=5&language=en&format=json"
                                        val jsonStr = withContext(Dispatchers.IO) { URL(url).readText() }
                                        val root = JSONObject(jsonStr)
                                        val array = root.optJSONArray("results")
                                        val list = mutableListOf<SavedCity>()
                                        if (array != null) {
                                            for (i in 0 until array.length()) {
                                                val obj = array.getJSONObject(i)
                                                val name = obj.getString("name")
                                                val country = obj.optString("country", "")
                                                val lat = obj.getDouble("latitude")
                                                val lon = obj.getDouble("longitude")
                                                list.add(SavedCity(name, country, lat, lon))
                                            }
                                        }
                                        searchResults = list
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    } finally {
                                        isSearching = false
                                    }
                                }
                            }
                        },
                        label = { Text("Enter city name (e.g. Mumbai, Tokyo)", color = currentPalette.subtextColor) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = currentPalette.textColor,
                            unfocusedTextColor = currentPalette.textColor,
                            focusedBorderColor = currentPalette.accentColor,
                            unfocusedBorderColor = currentPalette.subtextColor.copy(alpha = 0.4f),
                            cursorColor = currentPalette.accentColor
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    if (isSearching) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally).size(24.dp), color = currentPalette.accentColor)
                    } else {
                        searchResults.forEach { cityResult ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (savedCities.none { it.name.lowercase() == cityResult.name.lowercase() }) {
                                            savedCities.add(cityResult)
                                        }
                                        selectedCity = cityResult
                                        showAddCityDialog = false
                                    }
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("${cityResult.name}, ${cityResult.country}", color = currentPalette.textColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Text("+ Add", color = currentPalette.accentColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                        }
                    }
                }
            },
            containerColor = currentPalette.surfaceColor
        )
    }
}

@Composable
fun WeatherStatTile(label: String, value: String, palette: AmoraThemePalette) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = palette.textColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Text(label, color = palette.subtextColor, fontSize = 10.sp)
    }
}
