package ru.rufp.weather.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import ru.rufp.weather.R
import ru.rufp.weather.data.repository.CachedForecast
import ru.rufp.weather.data.repository.CachedHour
import ru.rufp.weather.data.repository.ForecastCache
import ru.rufp.weather.util.Format
import java.time.Instant

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun forecastCache(): ForecastCache
}

class WeatherWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val cache = EntryPointAccessors
            .fromApplication(context.applicationContext, WidgetEntryPoint::class.java)
            .forecastCache()
        val forecast = cache.flow().first()

        provideContent {
            GlanceTheme {
                WidgetContent(forecast, context)
            }
        }
    }
}

@Composable
private fun WidgetContent(forecast: CachedForecast?, context: Context) {
    Box(
        modifier = GlanceModifier.fillMaxSize().padding(8.dp)
            .background(GlanceTheme.colors.surface),
        contentAlignment = Alignment.CenterStart
    ) {
        if (forecast == null) {
            Text(
                context.getString(R.string.widget_no_data),
                style = TextStyle(color = GlanceTheme.colors.onSurface)
            )
            return@Box
        }

        val now = Instant.now().toEpochMilli()
        val current = forecast.hours.firstOrNull { it.timeEpochMillis >= now } ?: forecast.hours.firstOrNull()
        val upcoming = forecast.hours.filter { it.timeEpochMillis >= now }.take(5)

        Column(modifier = GlanceModifier.fillMaxSize()) {
            Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    forecast.locationName,
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurface,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = GlanceModifier.defaultWeight()
                )
                if (current != null) {
                    Text(
                        Format.temperature(current.temperature) + " " + Format.precipGlyph(current.type),
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            Spacer(modifier = GlanceModifier.height(4.dp))

            Row(modifier = GlanceModifier.fillMaxWidth()) {
                upcoming.forEach { hour ->
                    Column(modifier = GlanceModifier.defaultWeight()) {
                        Text(
                            Format.hhmm(Instant.ofEpochMilli(hour.timeEpochMillis)),
                            style = TextStyle(color = GlanceTheme.colors.onSurface)
                        )
                        Text(
                            Format.probability(hour.probability),
                            style = TextStyle(
                                color = if (hour.probability >= 0.5)
                                    GlanceTheme.colors.primary
                                else GlanceTheme.colors.onSurface
                            )
                        )
                        Text(
                            Format.precipGlyph(hour.type),
                            style = TextStyle(color = GlanceTheme.colors.onSurface)
                        )
                    }
                }
            }

            Text(
                context.getString(
                    R.string.data_from,
                    Format.hhmm(Instant.ofEpochMilli(forecast.issuedAtEpochMillis))
                ),
                style = TextStyle(color = GlanceTheme.colors.onSurface)
            )
        }
    }
}
