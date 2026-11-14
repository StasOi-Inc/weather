package ru.rufp.weaser.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class MetNoResponse(
    val properties: MetNoProperties = MetNoProperties()
)

@Serializable
data class MetNoProperties(
    val timeseries: List<MetNoTimeSeries> = emptyList()
)

@Serializable
data class MetNoTimeSeries(
    val time: String = "",
    val data: MetNoData = MetNoData()
)

@Serializable
data class MetNoData(
    val instant: MetNoInstant = MetNoInstant(),
    val next_1_hours: MetNoNext1Hours? = null,
    val next_6_hours: MetNoNext6Hours? = null
)

@Serializable
data class MetNoInstant(
    val details: MetNoInstantDetails = MetNoInstantDetails()
)

@Serializable
data class MetNoInstantDetails(
    val air_temperature: Double? = null,
    val apparent_temperature: Double? = null,
    val wind_speed: Double? = null,
    val relative_humidity: Double? = null,
    val air_pressure_at_sea_level: Double? = null
)

@Serializable
data class MetNoNext1Hours(
    val summary: MetNoSummary = MetNoSummary(),
    val details: MetNoPrecipDetails = MetNoPrecipDetails()
)

@Serializable
data class MetNoNext6Hours(
    val summary: MetNoSummary = MetNoSummary(),
    val details: MetNoPrecipDetails = MetNoPrecipDetails()
)

@Serializable
data class MetNoSummary(
    val symbol_code: String = ""
)

@Serializable
data class MetNoPrecipDetails(
    val precipitation_amount: Double? = null,
    val precipitation_amount_max: Double? = null,
    val probability_of_precipitation: Double? = null
)
