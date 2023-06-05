package wangdaye.com.geometricweather.weather.converters

import android.content.Context
import android.graphics.Color
import aqikotlin.library.constants.EEA
import aqikotlin.library.constants.EPA
import aqikotlin.library.constants.MEE
import us.dustinj.timezonemap.TimeZoneMap
import wangdaye.com.geometricweather.common.basic.models.Location
import wangdaye.com.geometricweather.common.basic.models.options.provider.WeatherSource
import wangdaye.com.geometricweather.common.basic.models.weather.AirQuality
import wangdaye.com.geometricweather.common.basic.models.weather.Alert
import wangdaye.com.geometricweather.common.basic.models.weather.Astro
import wangdaye.com.geometricweather.common.basic.models.weather.Base
import wangdaye.com.geometricweather.common.basic.models.weather.Current
import wangdaye.com.geometricweather.common.basic.models.weather.Daily
import wangdaye.com.geometricweather.common.basic.models.weather.HalfDay
import wangdaye.com.geometricweather.common.basic.models.weather.Hourly
import wangdaye.com.geometricweather.common.basic.models.weather.Minutely
import wangdaye.com.geometricweather.common.basic.models.weather.MoonPhase
import wangdaye.com.geometricweather.common.basic.models.weather.Precipitation
import wangdaye.com.geometricweather.common.basic.models.weather.PrecipitationProbability
import wangdaye.com.geometricweather.common.basic.models.weather.Temperature
import wangdaye.com.geometricweather.common.basic.models.weather.UV
import wangdaye.com.geometricweather.common.basic.models.weather.Weather
import wangdaye.com.geometricweather.common.basic.models.weather.WeatherCode
import wangdaye.com.geometricweather.common.basic.models.weather.Wind
import wangdaye.com.geometricweather.common.basic.models.weather.WindDegree
import wangdaye.com.geometricweather.common.utils.DisplayUtils
import wangdaye.com.geometricweather.weather.json.atmoaura.AtmoAuraQAResult
import wangdaye.com.geometricweather.weather.json.mf.MfCurrentResult
import wangdaye.com.geometricweather.weather.json.mf.MfEphemerisResult
import wangdaye.com.geometricweather.weather.json.mf.MfForecastV2Result
import wangdaye.com.geometricweather.weather.json.mf.MfForecastV2Result.ForecastProperties.ForecastV2
import wangdaye.com.geometricweather.weather.json.mf.MfForecastV2Result.ForecastProperties.HourForecast
import wangdaye.com.geometricweather.weather.json.mf.MfForecastV2Result.ForecastProperties.ProbabilityForecastV2
import wangdaye.com.geometricweather.weather.json.mf.MfLocationResult
import wangdaye.com.geometricweather.weather.json.mf.MfRainResult
import wangdaye.com.geometricweather.weather.json.mf.MfWarningsResult
import wangdaye.com.geometricweather.weather.services.WeatherService.WeatherResultWrapper
import java.util.Calendar
import java.util.Collections
import java.util.Date
import java.util.TimeZone
import kotlin.math.roundToInt

// Result of a coordinates search
fun convert(location: Location?, result: MfForecastV2Result): Location {
    return if (location != null && !location.province.isNullOrEmpty()
        && location.city.isNotEmpty()
        && !location.district.isNullOrEmpty()
    ) {
        Location(
            cityId = result.properties.insee,
            latitude = result.geometry.coordinates[1],
            longitude = result.geometry.coordinates[0],
            timeZone = TimeZone.getTimeZone(result.properties.timezone),
            country = result.properties.country,
            province = location.province, // Département
            city = location.city,
            district = location.district,
            weatherSource = WeatherSource.MF,
            isChina = !result.properties.country.isNullOrEmpty()
                    && (result.properties.country.equals("cn", ignoreCase = true)
                    || result.properties.country.equals("hk", ignoreCase = true)
                    || result.properties.country.equals("tw", ignoreCase = true))
        )
    } else {
        Location(
            cityId = result.properties.insee,
            latitude = result.geometry.coordinates[1],
            longitude = result.geometry.coordinates[0],
            timeZone = TimeZone.getTimeZone(result.properties.timezone),
            country = result.properties.country,
            province = result.properties.frenchDepartment, // Département
            city = result.properties.name,
            weatherSource = WeatherSource.MF,
            isChina = !result.properties.country.isNullOrEmpty()
                    && (result.properties.country.equals("cn", ignoreCase = true)
                    || result.properties.country.equals("hk", ignoreCase = true)
                    || result.properties.country.equals("tw", ignoreCase = true))
        )
    }
}

// Result of a query string search
fun convert(resultList: List<MfLocationResult>?): List<Location> {
    val locationList: MutableList<Location> = ArrayList()
    if (!resultList.isNullOrEmpty()) {
        // Since we don't have timezones in the result, we need to initialize a TimeZoneMap
        // Since it takes a lot of time, we make boundaries
        // However, even then, it can take a lot of time, even on good performing smartphones.
        // TODO: To improve performances, create a Location() with a null TimeZone.
        // When clicking in the location search result on a specific location, if TimeZone is
        // null, then make a TimeZoneMap of the lat/lon and find its TimeZone
        val minLat = resultList.minOf { it.lat }
        val maxLat = resultList.maxOf { it.lat } + 0.00001
        val minLon = resultList.minOf { it.lon }
        val maxLon = resultList.maxOf { it.lon } + 0.00001
        val map = TimeZoneMap.forRegion(minLat, minLon, maxLat, maxLon)
        for (r in resultList) {
            locationList.add(convert(null, r, map))
        }
    }
    return locationList
}

internal fun convert(
    location: Location?,
    result: MfLocationResult,
    map: TimeZoneMap?
): Location {
    return if (location != null && !location.province.isNullOrEmpty()
        && location.city.isNotEmpty()
        && !location.district.isNullOrEmpty()
    ) {
        Location(
            cityId = result.lat.toString() + "," + result.lon,
            latitude = result.lat.toFloat(),
            longitude = result.lon.toFloat(),
            timeZone = getTimeZoneForPosition(map!!, result.lat, result.lon),
            country = result.country,
            province = location.province, // Département
            city = location.city,
            district = location.district,
            weatherSource = WeatherSource.MF,
            isChina = !result.country.isNullOrEmpty()
                    && (result.country.equals("cn", ignoreCase = true)
                    || result.country.equals("hk", ignoreCase = true)
                    || result.country.equals("tw", ignoreCase = true))
        )
    } else {
        Location(
            cityId = result.lat.toString() + "," + result.lon,
            latitude = result.lat.toFloat(),
            longitude = result.lon.toFloat(),
            timeZone = getTimeZoneForPosition(map!!, result.lat, result.lon),
            country = result.country,
            province = if (result.admin2 != null) result.admin2 else null, // Département
            city = result.name + if (result.postCode == null) "" else " (" + result.postCode + ")",
            weatherSource = WeatherSource.MF,
            isChina = !result.country.isNullOrEmpty()
                    && (result.country.equals("cn", ignoreCase = true)
                    || result.country.equals("hk", ignoreCase = true)
                    || result.country.equals("tw", ignoreCase = true))
        )
    }
}

fun convert(
    context: Context,
    location: Location,
    currentResult: MfCurrentResult,
    forecastV2Result: MfForecastV2Result,
    ephemerisResult: MfEphemerisResult,
    rainResult: MfRainResult?,
    warningsResult: MfWarningsResult,
    aqiAtmoAuraResult: AtmoAuraQAResult?
): WeatherResultWrapper {
    return try {
        val hourlyByDate: MutableMap<String?, Map<String, MutableList<Hourly>>> = HashMap()
        val hourlyList: MutableList<Hourly> = ArrayList(forecastV2Result.properties.forecast.size)

        for (i in forecastV2Result.properties.forecast.indices) {
            val hourlyForecast = forecastV2Result.properties.forecast[i]
            val hourly = Hourly(
                date = hourlyForecast.time,
                // TODO: Use CommonConverter.isDaylight(sunrise, sunset, new Date(hourlyForecast.time * 1000)) instead:
                isDaylight = if (hourlyForecast.weatherIcon == null) true else !hourlyForecast.weatherIcon.endsWith("n"),
                weatherText = hourlyForecast.weatherDescription,
                weatherCode = getWeatherCode(hourlyForecast.weatherIcon),
                temperature = Temperature(
                    temperature = hourlyForecast.t?.roundToInt(),
                    windChillTemperature = hourlyForecast.tWindchill?.roundToInt()
                ),
                precipitation = getHourlyPrecipitation(hourlyForecast),
                precipitationProbability = if (forecastV2Result.properties.probabilityForecast != null) getHourlyPrecipitationProbability(
                    forecastV2Result.properties.probabilityForecast,
                    hourlyForecast.time
                ) else null,
                wind = Wind(
                    hourlyForecast.windIcon,
                    if (hourlyForecast.windDirection != null) WindDegree(
                        if (hourlyForecast.windDirection == "Variable") 0.0f else hourlyForecast.windDirection.toFloat(),
                        hourlyForecast.windDirection == "Variable"
                    ) else null,
                    if (hourlyForecast.windSpeed != null) hourlyForecast.windSpeed * 3.6f else null,
                    if (hourlyForecast.windSpeed != null) getWindLevel(context, hourlyForecast.windSpeed * 3.6f) else null
                ),
                airQuality = getAirQuality(hourlyForecast.time, aqiAtmoAuraResult)
                // uV = TODO
            )

            // We shift by 6 hours the hourly date, otherwise night times (00:00 to 05:59) would be on the wrong day
            val theDayAtMidnight = DisplayUtils.toTimezoneNoHour(
                Date(hourlyForecast.time.time - (6 * 3600 * 1000)),
                location.timeZone
            )
            val theDayFormatted = DisplayUtils.getFormattedDate(theDayAtMidnight, location.timeZone, "yyyyMMdd")
            if (!hourlyByDate.containsKey(theDayFormatted)) {
                hourlyByDate[theDayFormatted] = hashMapOf(
                    "day" to ArrayList(),
                    "night" to ArrayList()
                )
            }
            if (hourlyForecast.time.time < theDayAtMidnight.time + 18 * 3600 * 1000) {
                // 06:00 to 17:59 is the day
                hourlyByDate[theDayFormatted]!!["day"]!!.add(hourly)
            } else {
                // 18:00 to 05:59 is the night
                hourlyByDate[theDayFormatted]!!["night"]!!.add(hourly)
            }

            // Add to the app only if starts in the current hour
            if (hourlyForecast.time.time >= System.currentTimeMillis() - 3600 * 1000) {
                hourlyList.add(hourly)
            }
        }
        val dailyList = getInitialDailyList(context, location.timeZone, forecastV2Result.properties.dailyForecast, ephemerisResult.properties.ephemeris, hourlyByDate)
        val weather = Weather(
            base = Base(
                cityId = location.cityId,
                publishDate = forecastV2Result.updateTime
            ),
            current = Current(
                weatherText = currentResult.properties?.gridded?.weatherDescription,
                weatherCode = getWeatherCode(currentResult.properties?.gridded?.weatherIcon),
                temperature = Temperature(
                    temperature = currentResult.properties?.gridded?.temperature?.roundToInt() ?: hourlyList.getOrNull(1)?.temperature?.temperature
                ),
                wind = if (currentResult.properties?.gridded != null) Wind(
                    direction = currentResult.properties.gridded.windIcon,
                    degree = WindDegree(
                        degree = currentResult.properties.gridded.windDirection.toFloat(),
                        isNoDirection = currentResult.properties.gridded.windDirection == -1
                    ),
                    speed = currentResult.properties.gridded.windSpeed * 3.6f,
                    level = getWindLevel(context, currentResult.properties.gridded.windSpeed * 3.6f)
                ) else null,
                uV = getCurrentUV(
                    context,
                    dailyList.getOrNull(0)?.uV?.index,
                    Date(),
                    dailyList.getOrNull(0)?.sun?.riseDate,
                    dailyList.getOrNull(0)?.sun?.setDate,
                    location.timeZone
                ),
                airQuality = hourlyList.getOrNull(1)?.airQuality,
            ),
            dailyForecast = dailyList,
            hourlyForecast = hourlyList,
            minutelyForecast = getMinutelyList(rainResult),
            alertList = getWarningsList(warningsResult)
        )
        WeatherResultWrapper(weather)
    } catch (e: Exception) {
        e.printStackTrace()
        WeatherResultWrapper(null)
    }
}

private fun getInitialDailyList(
    context: Context,
    timeZone: TimeZone,
    dailyForecasts: List<ForecastV2>,
    ephemerisResult: MfEphemerisResult.Properties.Ephemeris,
    hourlyByDate: Map<String?, Map<String, MutableList<Hourly>>>
): List<Daily> {
    val initialDailyList: MutableList<Daily> = ArrayList(dailyForecasts.size)
    for (dailyForecast in dailyForecasts) {
        // Given as UTC, we need to convert in the correct timezone at 00:00
        val dayInUTCCalendar =
            DisplayUtils.toCalendarWithTimeZone(dailyForecast.time, TimeZone.getTimeZone("UTC"))
        val dayInLocalCalendar = Calendar.getInstance(timeZone)
        dayInLocalCalendar[Calendar.YEAR] = dayInUTCCalendar[Calendar.YEAR]
        dayInLocalCalendar[Calendar.MONTH] = dayInUTCCalendar[Calendar.MONTH]
        dayInLocalCalendar[Calendar.DAY_OF_MONTH] = dayInUTCCalendar[Calendar.DAY_OF_MONTH]
        dayInLocalCalendar[Calendar.HOUR_OF_DAY] = 0
        dayInLocalCalendar[Calendar.MINUTE] = 0
        dayInLocalCalendar[Calendar.SECOND] = 0
        val theDayInLocal = dayInLocalCalendar.time
        val dailyDateFormatted = DisplayUtils.getFormattedDate(theDayInLocal, timeZone, "yyyyMMdd")
        initialDailyList.add(
            Daily(
                date = theDayInLocal,
                day = completeHalfDayFromHourlyList(
                    dailyDate = theDayInLocal,
                    initialHalfDay = HalfDay(
                        // Too complicated to get weather from hourly, so let's just use daily info for both day and night
                        weatherText = dailyForecast.dailyWeatherDescription,
                        weatherPhase = dailyForecast.dailyWeatherDescription,
                        weatherCode = getWeatherCode(dailyForecast.dailyWeatherIcon),
                        temperature = Temperature(temperature = dailyForecast.tMax?.roundToInt())
                        // TODO temperatureWindChill with hourly data
                        // TODO cloudCover with hourly data
                    ),
                    halfDayHourlyList = hourlyByDate.getOrDefault(dailyDateFormatted, null)?.get("day"),
                    isDay = true
                ),
                night = completeHalfDayFromHourlyList(
                    dailyDate = theDayInLocal,
                    initialHalfDay = HalfDay(
                        weatherText = dailyForecast.dailyWeatherDescription,
                        weatherPhase = dailyForecast.dailyWeatherDescription,
                        weatherCode = getWeatherCode(dailyForecast.dailyWeatherIcon),
                        temperature = Temperature(temperature = dailyForecast.tMin?.roundToInt())
                        // TODO temperatureWindChill with hourly data
                        // TODO cloudCover with hourly data
                    ),
                    halfDayHourlyList = hourlyByDate.getOrDefault(dailyDateFormatted, null)?.get("night"),
                    isDay = false
                ),
                sun = Astro(
                    riseDate = dailyForecast.sunriseTime,
                    setDate = dailyForecast.sunsetTime
                ),
                moon = Astro( // FIXME: It's valid only for the first day
                    riseDate = ephemerisResult.moonriseTime,
                    setDate = ephemerisResult.moonsetTime
                ),
                moonPhase = MoonPhase( // FIXME: It's valid only for the first day
                    angle = getMoonPhaseAngle(ephemerisResult.moonPhaseDescription),
                    description = ephemerisResult.moonPhaseDescription
                ),
                // TODO airQuality with hourly data
                uV = UV(
                    index = dailyForecast.uvIndex,
                    level = getUVLevel(context, dailyForecast.uvIndex)
                ),
                hoursOfSun = getHoursOfDay(dailyForecast.sunriseTime, dailyForecast.sunsetTime)
            )
        )
    }
    return initialDailyList
}

// This can be improved by adding results from other regions
private fun getAirQuality(
    requestedDate: Date,
    aqiAtmoAuraResult: AtmoAuraQAResult?
): AirQuality? {
    return if (aqiAtmoAuraResult == null) {
        null
    } else {
        var pm25: Float? = null
        var pm10: Float? = null
        var so2: Float? = null
        var no2: Float? = null
        var o3: Float? = null
        aqiAtmoAuraResult.polluants
            .filter { p -> p.horaires.firstOrNull { it.datetimeEcheance == requestedDate } != null }
            .forEach { p -> when (p.polluant) {
                    "o3" -> o3 = p.horaires.firstOrNull { it.datetimeEcheance == requestedDate }?.concentration?.toFloat()
                    "no2" -> no2 = p.horaires.firstOrNull { it.datetimeEcheance == requestedDate }?.concentration?.toFloat()
                    "pm2.5" -> pm25 = p.horaires.firstOrNull { it.datetimeEcheance == requestedDate }?.concentration?.toFloat()
                    "pm10" -> pm10 = p.horaires.firstOrNull { it.datetimeEcheance == requestedDate }?.concentration?.toFloat()
                    "so2" -> so2 = p.horaires.firstOrNull { it.datetimeEcheance == requestedDate }?.concentration?.toFloat()
                }
            }

        AirQuality(
            epaIndex = getAirQualityIndex(algorithm = EPA, pm25 = pm25, pm10 = pm10, no2_1h = no2, o3_1h = o3, so2_1h = so2),
            meeIndex = getAirQualityIndex(algorithm = MEE, pm25 = pm25, pm10 = pm10, no2_1h = no2, o3_1h = o3, so2_1h = so2),
            eeaIndex = getAirQualityIndex(algorithm = EEA, pm25 = pm25, pm10 = pm10, no2_1h = no2, o3_1h = o3, so2_1h = so2),
            pM25 = pm25,
            pM10 = pm10,
            sO2 = so2,
            nO2 = no2,
            o3 = o3
        )
    }
}

private fun getHourlyPrecipitation(hourlyForecast: HourForecast): Precipitation {
    val rainCumul = with (hourlyForecast) {
        rain1h ?: rain3h ?: rain6h ?: rain12h ?: rain24h
    }
    val snowCumul = with (hourlyForecast) {
        snow1h ?: snow3h ?: snow6h ?: snow12h ?: snow24h
    }
    val totalCumul = if (rainCumul == null) {
        snowCumul
    } else if (snowCumul == null) {
        rainCumul
    } else {
        snowCumul + rainCumul
    }
    return Precipitation(
        total = totalCumul,
        rain = rainCumul,
        snow = snowCumul
    )
}

private fun getHourlyPrecipitationProbability(
    probabilityForecastResult: List<ProbabilityForecastV2>,
    dt: Date
): PrecipitationProbability {
    var rainProbability: Float? = null
    var snowProbability: Float? = null
    var iceProbability: Float? = null
    for (probabilityForecast in probabilityForecastResult) {
        /*
         * Probablity are given every 3 hours, sometimes every 6 hours.
         * Sometimes every 3 hour-schedule give 3 hours probability AND 6 hours probability,
         * sometimes only one of them
         * It's not very clear but we take all hours in order.
         */
        if (probabilityForecast.time.time == dt.time || probabilityForecast.time.time + 3600 * 1000 == dt.time || probabilityForecast.time.time + 3600 * 2 * 1000 == dt.time) {
            if (probabilityForecast.rainHazard3h != null) {
                rainProbability = probabilityForecast.rainHazard3h?.toFloat()
            } else if (probabilityForecast.rainHazard6h != null) {
                rainProbability = probabilityForecast.rainHazard6h?.toFloat()
            }
            if (probabilityForecast.snowHazard3h != null) {
                snowProbability = probabilityForecast.snowHazard3h?.toFloat()
            } else if (probabilityForecast.snowHazard6h != null) {
                snowProbability = probabilityForecast.snowHazard6h?.toFloat()
            }
            iceProbability = probabilityForecast.freezingHazard?.toFloat()
        }

        /*
         * If it's found as part of the "6 hour schedule" and we find later a "3 hour schedule"
         * the "3 hour schedule" will overwrite the "6 hour schedule" below with the above
         */
        if (probabilityForecast.time.time + 3600 * 3 * 1000 == dt.time || probabilityForecast.time.time + 3600 * 4 * 1000 == dt.time || probabilityForecast.time.time + 3600 * 5 * 1000 == dt.time) {
            if (probabilityForecast.rainHazard6h != null) {
                rainProbability = probabilityForecast.rainHazard6h?.toFloat()
            }
            if (probabilityForecast.snowHazard6h != null) {
                snowProbability = probabilityForecast.snowHazard6h?.toFloat()
            }
            iceProbability = probabilityForecast.freezingHazard?.toFloat()
        }
    }
    val allProbabilities: MutableList<Float> = ArrayList()
    allProbabilities.add(rainProbability ?: 0f)
    allProbabilities.add(snowProbability ?: 0f)
    allProbabilities.add(iceProbability ?: 0f)
    return PrecipitationProbability(
        Collections.max(allProbabilities, null),
        null,
        rainProbability,
        snowProbability,
        iceProbability
    )
}

private fun getMinutelyList(rainResult: MfRainResult?): List<Minutely> {
    if (rainResult?.properties == null || rainResult.properties.rainForecasts == null) {
        return ArrayList()
    }
    val minutelyList: MutableList<Minutely> = ArrayList(rainResult.properties.rainForecasts.size)
    var minuteInterval: Int
    for (i in rainResult.properties.rainForecasts.indices) {
        minuteInterval = if (i < rainResult.properties.rainForecasts.size - 1) {
            ((rainResult.properties.rainForecasts[i + 1].time.time - rainResult.properties.rainForecasts[i].time.time) / (60 * 1000)).toDouble().roundToInt()
        } else {
            10 // Last one is 10 minutes
        }
        minutelyList.add(
            Minutely(
                rainResult.properties.rainForecasts[i].time,
                rainResult.properties.rainForecasts[i].time.time,
                rainResult.properties.rainForecasts[i].rainIntensityDescription,
                if (rainResult.properties.rainForecasts[i].rainIntensity > 1) WeatherCode.RAIN else null,
                minuteInterval,
                getPrecipitationIntensity(rainResult.properties.rainForecasts[i].rainIntensity),
                null
            )
        )
    }
    return minutelyList
}

private fun getPrecipitationIntensity(rain: Int): Double {
    return when (rain) {
        4 -> 10.0
        3 -> 5.5
        2 -> 2.0
        else -> 0.0
    }
}

private fun getWarningsList(warningsResult: MfWarningsResult): List<Alert> {
    val alertList: MutableList<Alert> =
        ArrayList(if (warningsResult.phenomenonsItems == null) 0 else warningsResult.phenomenonsItems.size)
    if (warningsResult.timelaps != null) {
        for (i in warningsResult.timelaps.indices) {
            for (j in warningsResult.timelaps[i].timelapsItems.indices) {
                // Do not warn when there is nothing to warn (green alert)
                if (warningsResult.timelaps[i].timelapsItems[j].colorId > 1) {
                    alertList.add(
                        Alert(
                            warningsResult.timelaps[i].phenomenonId.toLong(),
                            warningsResult.timelaps[i].timelapsItems[j].beginTime,
                            warningsResult.timelaps[i].timelapsItems[j].beginTime.time,
                            getWarningType(warningsResult.timelaps[i].phenomenonId) + " — " + getWarningText(
                                warningsResult.timelaps[i].timelapsItems[j].colorId
                            ),
                            "",  // TODO: Longer description (I think there is a report in the web service when alert is orange or red)
                            getWarningType(warningsResult.timelaps[i].phenomenonId),
                            warningsResult.timelaps[i].timelapsItems[j].colorId,
                            getWarningColor(warningsResult.timelaps[i].timelapsItems[j].colorId)
                        )
                    )
                }
            }
        }
        Alert.deduplication(alertList)
    }
    return alertList
}

private fun getWarningType(phemononId: Int): String {
    return when (phemononId) {
        1 -> "Vent"
        2 -> "Pluie-Inondation"
        3 -> "Orages"
        4 -> "Crues"
        5 -> "Neige-Verglas"
        6 -> "Canicule"
        7 -> "Grand Froid"
        8 -> "Avalanches"
        9 -> "Vagues-Submersion"
        else -> "Divers"
    }
}

private fun getWarningText(colorId: Int): String {
    return when (colorId) {
        4 -> "Vigilance absolue"
        3 -> "Soyez très vigilant"
        2 -> "Soyez attentif"
        else -> "Pas de vigilance particulière"
    }
}

private fun getWarningColor(colorId: Int): Int {
    return when (colorId) {
        4 -> Color.rgb(204, 0, 0)
        3 -> Color.rgb(255, 184, 43)
        2 -> Color.rgb(255, 246, 0)
        else -> Color.rgb(49, 170, 53)
    }
}

private fun getWeatherCode(icon: String?): WeatherCode? {
    return if (icon == null) {
        null
    } else with (icon) {
        when {
            // We need to take care of two-digits first
            startsWith("p32") || startsWith("p33")
                    || startsWith("p34") -> WeatherCode.WIND
            startsWith("p31") -> null // What is this?
            startsWith("p26") || startsWith("p27") || startsWith("p28")
                    || startsWith("p29") -> WeatherCode.THUNDER
            startsWith("p26") || startsWith("p27") || startsWith("p28")
                    || startsWith("p29") -> WeatherCode.THUNDER
            startsWith("p21") || startsWith("p22")
                    || startsWith("p23") -> WeatherCode.SNOW
            startsWith("p19") || startsWith("p20") -> WeatherCode.HAIL
            startsWith("p17") || startsWith("p18") -> WeatherCode.SLEET
            startsWith("p16") || startsWith("p24")
                    || startsWith("p25") || startsWith("p30") -> WeatherCode.THUNDERSTORM
            startsWith("p9") || startsWith("p10") || startsWith("p11")
                    || startsWith("p12") || startsWith("p13")
                    || startsWith("p14") || startsWith("p15") -> WeatherCode.RAIN
            startsWith("p6") || startsWith("p7")
                    || startsWith("p8") -> WeatherCode.FOG
            startsWith("p4") || startsWith("p5") -> WeatherCode.HAZE
            startsWith("p3") -> WeatherCode.CLOUDY
            startsWith("p2") -> WeatherCode.PARTLY_CLOUDY
            startsWith("p1") -> WeatherCode.CLEAR
            else -> null
        }
    }
}