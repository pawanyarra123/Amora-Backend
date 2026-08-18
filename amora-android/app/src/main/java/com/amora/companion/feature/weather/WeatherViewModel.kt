package com.amora.companion.feature.weather

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amora.companion.core.data.network.AmoraApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import javax.inject.Inject

private const val TAG = "WeatherViewModel"

@HiltViewModel
class WeatherViewModel @Inject constructor(
    private val apiService: AmoraApiService
) : ViewModel() {

    /**
     * Backend first (keeps your own server as the source of truth / lets you cache,
     * rate-limit, or swap providers server-side later), falling back to calling
     * Open-Meteo directly if the backend is unreachable — so weather still works
     * even when your PC/backend is off, same fallback spirit as chat's
     * Groq → trained-data → generic-reply chain.
     */
    fun fetchWeather(
        city: SavedCity,
        onResult: (WeatherDetailState) -> Unit
    ) {
        viewModelScope.launch {
            val fromBackend = try {
                withContext(Dispatchers.IO) {
                    val response = apiService.getWeather(city.lat, city.lon, city.name)
                    if (response.isSuccessful) response.body() else null
                }
            } catch (e: Exception) {
                Log.w(TAG, "Backend weather fetch failed, falling back to direct API: ${e.message}")
                null
            }

            val state = if (fromBackend != null) {
                WeatherDetailState(
                    cityName = city.name,
                    temp = "${fromBackend.temperature_celsius.toInt()}°C",
                    condition = fromBackend.condition,
                    icon = getWmoIconForCondition(fromBackend.condition),
                    humidity = "${fromBackend.humidity}%",
                    wind = "${fromBackend.wind_speed_kmh.toInt()} km/h",
                    uv = fromBackend.uv_index.toString(),
                    hourly = fromBackend.hourly.map { LiveHourlyItem(it.time, it.temp, it.icon) },
                    weekly = fromBackend.weekly.map { LiveDailyItem(it.day, it.tempRange, it.condition, it.icon) },
                    isLoading = false
                )
            } else {
                fetchDirectFromOpenMeteo(city)
            }
            onResult(state)
        }
    }

    /** Fallback path — same Open-Meteo call the screen used to make unconditionally. */
    private suspend fun fetchDirectFromOpenMeteo(city: SavedCity): WeatherDetailState {
        return try {
            val url = "https://api.open-meteo.com/v1/forecast?latitude=${city.lat}&longitude=${city.lon}" +
                    "&current=temperature_2m,relative_humidity_2m,weather_code,wind_speed_10m" +
                    "&hourly=temperature_2m,weather_code" +
                    "&daily=weather_code,temperature_2m_max,temperature_2m_min&timezone=auto"
            val jsonStr = withContext(Dispatchers.IO) { URL(url).readText() }
            val root = JSONObject(jsonStr)
            val current = root.getJSONObject("current")

            val temp = current.getDouble("temperature_2m")
            val humidity = current.getInt("relative_humidity_2m")
            val wind = current.getDouble("wind_speed_10m")
            val (condText, condIcon) = getWmoInfo(current.getInt("weather_code"))

            val hourlyObj = root.getJSONObject("hourly")
            val hTimes = hourlyObj.getJSONArray("time")
            val hTemps = hourlyObj.getJSONArray("temperature_2m")
            val hCodes = hourlyObj.getJSONArray("weather_code")
            val hourlyList = (0 until minOf(12, hTimes.length())).map { i ->
                val rawT = hTimes.getString(i)
                val timeStr = if (rawT.contains("T")) rawT.split("T")[1].substring(0, 5) else "$i:00"
                val (_, hIcon) = getWmoInfo(hCodes.getInt(i))
                LiveHourlyItem(timeStr, "${hTemps.getDouble(i).toInt()}°C", hIcon)
            }

            val dailyObj = root.getJSONObject("daily")
            val dTimes = dailyObj.getJSONArray("time")
            val dMaxs = dailyObj.getJSONArray("temperature_2m_max")
            val dMins = dailyObj.getJSONArray("temperature_2m_min")
            val dCodes = dailyObj.getJSONArray("weather_code")
            val daysMap = listOf("Today", "Tomorrow", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
            val weeklyList = (0 until minOf(7, dTimes.length())).map { i ->
                val dayLabel = if (i < daysMap.size) daysMap[i] else dTimes.getString(i)
                val (dCond, dIcon) = getWmoInfo(dCodes.getInt(i))
                LiveDailyItem(dayLabel, "${dMins.getDouble(i).toInt()}° / ${dMaxs.getDouble(i).toInt()}°C", dCond, dIcon)
            }

            WeatherDetailState(
                cityName = city.name,
                temp = "${temp.toInt()}°C",
                condition = condText,
                icon = condIcon,
                humidity = "$humidity%",
                wind = "${wind.toInt()} km/h",
                uv = "3.5",
                hourly = hourlyList,
                weekly = weeklyList,
                isLoading = false
            )
        } catch (e: Exception) {
            Log.e(TAG, "Direct Open-Meteo fallback also failed", e)
            WeatherDetailState(cityName = city.name, isLoading = false)
        }
    }
}

private fun getWmoIconForCondition(condition: String): String = getWmoInfo(wmoCodeForConditionGuess(condition)).second

/** The backend already picked an icon server-side and returns condition text only in
 *  some fields — reuse the same WMO table so the icon matches what the backend meant. */
private fun wmoCodeForConditionGuess(condition: String): Int = when {
    condition.contains("Clear", true) && condition.contains("Mainly", true) -> 1
    condition.contains("Clear", true) -> 0
    condition.contains("Partly", true) -> 2
    condition.contains("Overcast", true) -> 3
    condition.contains("Fog", true) -> 45
    condition.contains("Drizzle", true) -> 51
    condition.contains("Thunder", true) -> 95
    condition.contains("Snow", true) -> 71
    condition.contains("Shower", true) -> 80
    condition.contains("Rain", true) -> 61
    else -> 0
}

fun getWmoInfo(code: Int): Pair<String, String> {
    return when (code) {
        0 -> "Clear Sky" to "☀️"
        1 -> "Mainly Clear" to "🌤️"
        2 -> "Partly Cloudy" to "⛅"
        3 -> "Overcast" to "☁️"
        45, 48 -> "Foggy" to "🌫️"
        51, 53, 55 -> "Drizzle" to "🌦️"
        61, 63, 65 -> "Rain" to "🌧️"
        71, 73, 75 -> "Snow" to "❄️"
        80, 81, 82 -> "Rain Showers" to "🌦️"
        95, 96, 99 -> "Thunderstorm" to "🌩️"
        else -> "Clear" to "☀️"
    }
}
