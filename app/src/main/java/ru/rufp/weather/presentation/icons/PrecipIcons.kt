package ru.rufp.weather.presentation.icons

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material.icons.filled.Hail
import androidx.compose.material.icons.filled.SevereCold
import androidx.compose.material.icons.filled.Thunderstorm
import androidx.compose.material.icons.filled.Umbrella
import androidx.compose.material.icons.filled.Water
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.Icon
import androidx.compose.ui.Modifier
import ru.rufp.weather.domain.model.PrecipType

object PrecipIcons {

    fun imageVector(type: PrecipType): ImageVector = when (type) {
        PrecipType.NONE -> Icons.Filled.WbSunny
        PrecipType.DRIZZLE -> Icons.Filled.Grain
        PrecipType.RAIN -> Icons.Filled.WaterDrop
        PrecipType.HEAVY_RAIN -> Icons.Filled.Umbrella
        PrecipType.SNOW -> Icons.Filled.AcUnit
        PrecipType.WET_SNOW -> Icons.Filled.Water
        PrecipType.FREEZING_RAIN -> Icons.Filled.SevereCold
        PrecipType.HAIL -> Icons.Filled.Hail
        PrecipType.THUNDERSTORM -> Icons.Filled.Thunderstorm
    }

    fun tint(type: PrecipType, primary: Color, tertiary: Color, error: Color): Color = when (type) {
        PrecipType.NONE -> Color(0xFFFFB300)
        PrecipType.DRIZZLE, PrecipType.RAIN, PrecipType.HEAVY_RAIN -> primary
        PrecipType.SNOW, PrecipType.WET_SNOW -> tertiary
        PrecipType.FREEZING_RAIN, PrecipType.HAIL, PrecipType.THUNDERSTORM -> error
    }
}

@Composable
fun PrecipIcon(type: PrecipType, modifier: Modifier = Modifier, tint: Color? = null) {
    val resolvedTint = tint ?: PrecipIcons.tint(
        type = type,
        primary = androidx.compose.material3.MaterialTheme.colorScheme.primary,
        tertiary = androidx.compose.material3.MaterialTheme.colorScheme.tertiary,
        error = androidx.compose.material3.MaterialTheme.colorScheme.error
    )
    Icon(
        imageVector = PrecipIcons.imageVector(type),
        contentDescription = null,
        modifier = modifier,
        tint = resolvedTint
    )
}
