package ru.rufp.weather.data.remote

import ru.rufp.weather.domain.model.PrecipType

object PrecipMappers {

    fun fromWmoCode(code: Int?): PrecipType = when (code) {
        null -> PrecipType.NONE
        51, 53, 55 -> PrecipType.DRIZZLE
        56, 57, 66, 67 -> PrecipType.FREEZING_RAIN
        61, 63, 80, 81 -> PrecipType.RAIN
        65, 82 -> PrecipType.HEAVY_RAIN
        71, 73, 75, 77, 85, 86 -> PrecipType.SNOW
        95 -> PrecipType.THUNDERSTORM
        96, 99 -> PrecipType.HAIL
        else -> PrecipType.NONE
    }

    fun fromMetSymbol(symbol: String?): PrecipType {
        if (symbol.isNullOrEmpty()) return PrecipType.NONE
        val s = symbol.removeSuffix("_day").removeSuffix("_night").removeSuffix("_polartwilight")
        return when {
            s.contains("thunder") && (s.contains("hail") || s.contains("sleetandthunder")) ->
                if (s.contains("hail")) PrecipType.HAIL else PrecipType.THUNDERSTORM
            s.contains("thunder") -> PrecipType.THUNDERSTORM
            s.contains("heavyrain") && s.contains("sleet") -> PrecipType.WET_SNOW
            s.contains("sleet") -> PrecipType.WET_SNOW
            s.contains("snow") -> PrecipType.SNOW
            s.contains("heavyrain") -> PrecipType.HEAVY_RAIN
            s.contains("lightrainshowers") -> PrecipType.DRIZZLE
            s.contains("rainshowers") -> PrecipType.RAIN
            s.contains("lightrain") -> PrecipType.DRIZZLE
            s.contains("rain") -> PrecipType.RAIN
            else -> PrecipType.NONE
        }
    }

    fun fromOpenWeatherId(id: Int?): PrecipType = when (id) {
        null -> PrecipType.NONE
        in 200..232 -> PrecipType.THUNDERSTORM
        in 300..321 -> PrecipType.DRIZZLE
        511, 616 -> PrecipType.FREEZING_RAIN
        500, 501 -> PrecipType.RAIN
        in 502..531 -> PrecipType.HEAVY_RAIN
        611, 612, 615, 617 -> PrecipType.WET_SNOW
        613 -> PrecipType.WET_SNOW
        in 600..622 -> PrecipType.SNOW
        else -> PrecipType.NONE
    }

    fun fromWeatherApiCode(code: Int?): PrecipType = when (code) {
        null -> PrecipType.NONE
        1168, 1171, 1198, 1201 -> PrecipType.FREEZING_RAIN
        1150, 1153 -> PrecipType.DRIZZLE
        1180, 1183, 1186, 1189 -> PrecipType.RAIN
        1192, 1195, 1240, 1243, 1246 -> PrecipType.HEAVY_RAIN
        1204, 1207, 1249, 1252 -> PrecipType.WET_SNOW
        1210, 1213, 1216, 1219, 1222, 1225, 1255, 1258 -> PrecipType.SNOW
        1237, 1261, 1264 -> PrecipType.HAIL
        1273, 1276 -> PrecipType.THUNDERSTORM
        1279, 1282 -> PrecipType.THUNDERSTORM
        else -> PrecipType.NONE
    }
}
