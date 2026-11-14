package ru.rufp.weather.util

import android.content.Context
import ru.rufp.weather.R
import ru.rufp.weather.domain.model.PrecipType
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object Format {

    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())

    fun hhmm(instant: Instant): String =
        timeFormatter.format(instant.atZone(ZoneId.systemDefault()))

    fun temperature(value: Double?): String =
        if (value == null) "—" else "${Math.round(value)}°"

    fun probability(value: Double): String = "${Math.round(value * 100)}%"

    fun intensity(value: Double?): String =
        if (value == null || value <= 0.0) "0"
        else String.format(Locale.getDefault(), "%.1f", value)

    fun intensityMmH(value: Double?): String =
        if (value == null || value <= 0.0) "0 мм/ч"
        else String.format(Locale.getDefault(), "%.1f мм/ч", value)

    fun pressureMm(value: Double?): String =
        if (value == null) "—" else "${Math.round(value)} мм рт. ст."

    fun wind(value: Double?): String =
        if (value == null) "—" else String.format(Locale.getDefault(), "%.0f м/с", value)

    fun humidity(value: Double?): String =
        if (value == null) "—" else "${Math.round(value)}%"

    private fun String.toPrecipType(): PrecipType =
        runCatching { PrecipType.valueOf(this) }.getOrDefault(PrecipType.NONE)

    fun precipTypeName(type: String, context: Context): String = precipTypeName(type.toPrecipType(), context)

    fun precipGlyph(type: String): String = precipGlyph(type.toPrecipType())

    fun precipTypeName(type: PrecipType, context: Context): String = when (type) {
        PrecipType.NONE -> context.getString(R.string.precip_none)
        PrecipType.DRIZZLE -> context.getString(R.string.precip_drizzle)
        PrecipType.RAIN -> context.getString(R.string.precip_rain)
        PrecipType.HEAVY_RAIN -> context.getString(R.string.precip_heavy_rain)
        PrecipType.SNOW -> context.getString(R.string.precip_snow)
        PrecipType.WET_SNOW -> context.getString(R.string.precip_wet_snow)
        PrecipType.FREEZING_RAIN -> context.getString(R.string.precip_freezing_rain)
        PrecipType.HAIL -> context.getString(R.string.precip_hail)
        PrecipType.THUNDERSTORM -> context.getString(R.string.precip_thunderstorm)
    }

    fun precipGlyph(type: PrecipType): String = when (type) {
        PrecipType.NONE -> "○"
        PrecipType.DRIZZLE -> "·"
        PrecipType.RAIN -> "\u2573"
        PrecipType.HEAVY_RAIN -> "≡"
        PrecipType.SNOW -> "*"
        PrecipType.WET_SNOW -> "/*"
        PrecipType.FREEZING_RAIN -> "/·"
        PrecipType.HAIL -> "▲"
        PrecipType.THUNDERSTORM -> "≋"
    }
}
