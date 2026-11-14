package ru.rufp.weaser.data.repository

import ru.rufp.weaser.domain.model.SourceStatus
import java.time.Instant

data class SourceRow(
    val id: String,
    val displayName: String,
    val enabled: Boolean,
    val requiresKey: Boolean,
    val keyPresent: Boolean,
    val status: SourceStatus,
    val lastSuccessAt: Instant?,
    val weight: Double,
    val warmUp: Boolean,
    val brier: Double,
    val maeTemperature: Double,
    val hitRate: Double,
    val nextHours: List<Pair<Instant, Double>>
)
