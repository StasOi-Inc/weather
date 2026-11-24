package ru.rufp.weather.data.remote

import org.junit.Assert.assertEquals
import org.junit.Test
import ru.rufp.weather.domain.model.PrecipType

class PrecipMappersTest {

    @Test
    fun wmoCodeDrizzle() {
        assertEquals(PrecipType.DRIZZLE, PrecipMappers.fromWmoCode(53))
    }

    @Test
    fun wmoCodeFreezingRain() {
        assertEquals(PrecipType.FREEZING_RAIN, PrecipMappers.fromWmoCode(66))
    }

    @Test
    fun wmoCodeHeavyRain() {
        assertEquals(PrecipType.HEAVY_RAIN, PrecipMappers.fromWmoCode(65))
    }

    @Test
    fun wmoCodeSnow() {
        assertEquals(PrecipType.SNOW, PrecipMappers.fromWmoCode(73))
    }

    @Test
    fun wmoCodeThunderstorm() {
        assertEquals(PrecipType.THUNDERSTORM, PrecipMappers.fromWmoCode(95))
    }

    @Test
    fun wmoCodeHail() {
        assertEquals(PrecipType.HAIL, PrecipMappers.fromWmoCode(96))
    }

    @Test
    fun wmoCodeCloudsIsNone() {
        assertEquals(PrecipType.NONE, PrecipMappers.fromWmoCode(2))
        assertEquals(PrecipType.NONE, PrecipMappers.fromWmoCode(null))
    }

    @Test
    fun metNoSymbolSnowIgnoresDayNightSuffix() {
        assertEquals(PrecipType.SNOW, PrecipMappers.fromMetSymbol("snow"))
        assertEquals(PrecipType.SNOW, PrecipMappers.fromMetSymbol("heavysnow_night"))
    }

    @Test
    fun metNoSymbolSleetIsWetSnow() {
        assertEquals(PrecipType.WET_SNOW, PrecipMappers.fromMetSymbol("sleet"))
        assertEquals(PrecipType.WET_SNOW, PrecipMappers.fromMetSymbol("heavyrainandsleet_day"))
    }

    @Test
    fun metNoSymbolThunderstorm() {
        assertEquals(PrecipType.THUNDERSTORM, PrecipMappers.fromMetSymbol("rainandthunder"))
    }

    @Test
    fun metNoSymbolLightRainIsDrizzle() {
        assertEquals(PrecipType.DRIZZLE, PrecipMappers.fromMetSymbol("lightrain"))
    }

    @Test
    fun metNoSymbolNullIsNone() {
        assertEquals(PrecipType.NONE, PrecipMappers.fromMetSymbol(null))
        assertEquals(PrecipType.NONE, PrecipMappers.fromMetSymbol(""))
    }

    @Test
    fun openWeatherIdRanges() {
        assertEquals(PrecipType.THUNDERSTORM, PrecipMappers.fromOpenWeatherId(211))
        assertEquals(PrecipType.DRIZZLE, PrecipMappers.fromOpenWeatherId(301))
        assertEquals(PrecipType.RAIN, PrecipMappers.fromOpenWeatherId(500))
        assertEquals(PrecipType.HEAVY_RAIN, PrecipMappers.fromOpenWeatherId(503))
        assertEquals(PrecipType.WET_SNOW, PrecipMappers.fromOpenWeatherId(611))
        assertEquals(PrecipType.SNOW, PrecipMappers.fromOpenWeatherId(601))
        assertEquals(PrecipType.FREEZING_RAIN, PrecipMappers.fromOpenWeatherId(511))
        assertEquals(PrecipType.NONE, PrecipMappers.fromOpenWeatherId(800))
    }

    @Test
    fun weatherApiCodeRanges() {
        assertEquals(PrecipType.FREEZING_RAIN, PrecipMappers.fromWeatherApiCode(1198))
        assertEquals(PrecipType.DRIZZLE, PrecipMappers.fromWeatherApiCode(1153))
        assertEquals(PrecipType.RAIN, PrecipMappers.fromWeatherApiCode(1183))
        assertEquals(PrecipType.HEAVY_RAIN, PrecipMappers.fromWeatherApiCode(1195))
        assertEquals(PrecipType.WET_SNOW, PrecipMappers.fromWeatherApiCode(1204))
        assertEquals(PrecipType.SNOW, PrecipMappers.fromWeatherApiCode(1213))
        assertEquals(PrecipType.HAIL, PrecipMappers.fromWeatherApiCode(1237))
        assertEquals(PrecipType.THUNDERSTORM, PrecipMappers.fromWeatherApiCode(1276))
        assertEquals(PrecipType.NONE, PrecipMappers.fromWeatherApiCode(1000))
    }
}
