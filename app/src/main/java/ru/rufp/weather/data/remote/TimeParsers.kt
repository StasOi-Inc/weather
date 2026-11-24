package ru.rufp.weather.data.remote

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

object TimeParsers {

    private val isoFormatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    fun openMeteoTime(value: String, utcOffsetSeconds: Long): Instant {
        val local = LocalDateTime.parse(value, isoFormatter)
        return local.toInstant(ZoneOffset.ofTotalSeconds(utcOffsetSeconds.toInt()))
    }

    fun metNoTime(value: String): Instant = Instant.parse(value)
}
