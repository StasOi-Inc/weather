package ru.rufp.weather.work

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import ru.rufp.weather.widget.WeatherWidget
import java.util.concurrent.TimeUnit

object SyncScheduler {

    private const val SYNC_PERIODIC = "Weather_forecast_sync"
    private const val SYNC_ADAPTIVE = "Weather_forecast_sync_adaptive"
    private const val WEIGHTS_RECALC = "Weather_weights_recalc"
    private const val UPDATE_CHECK = "Weather_update_check"

    fun schedule(context: Context) {
        val wm = WorkManager.getInstance(context)
        wm.enqueueUniquePeriodicWork(
            SYNC_PERIODIC,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<ForecastSyncWorker>(30, TimeUnit.MINUTES)
                .setConstraints(
                    Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
                )
                .build()
        )
        wm.enqueueUniquePeriodicWork(
            WEIGHTS_RECALC,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<WeightRecalcWorker>(24, TimeUnit.HOURS).build()
        )
        wm.enqueueUniquePeriodicWork(
            UPDATE_CHECK,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<UpdateCheckWorker>(24, TimeUnit.HOURS)
                .setConstraints(
                    Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
                )
                .build()
        )
    }

    fun scheduleAdaptiveIfRain(context: Context, rainImminent: Boolean) {
        val wm = WorkManager.getInstance(context)
        if (rainImminent) {
            wm.enqueueUniqueWork(
                SYNC_ADAPTIVE,
                ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequestBuilder<ForecastSyncWorker>()
                    .setInitialDelay(15, TimeUnit.MINUTES)
                    .build()
            )
        } else {
            wm.cancelUniqueWork(SYNC_ADAPTIVE)
        }
    }
}

@HiltWorker
class ForecastSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val forecastRepository: ru.rufp.weather.data.repository.ForecastRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        try {
            val ok = forecastRepository.sync()
            if (ok) {
                WeatherWidget().updateAll(applicationContext)
                val rainImminent = forecastRepository.rainImminentNextTwoHours()
                SyncScheduler.scheduleAdaptiveIfRain(applicationContext, rainImminent)
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            android.util.Log.w("ForecastSyncWorker", "sync failed", e)
            return Result.success()
        }
        return Result.success()
    }
}

@HiltWorker
class WeightRecalcWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val forecastRepository: ru.rufp.weather.data.repository.ForecastRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        try {
            forecastRepository.recomputeMetrics()
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            android.util.Log.w("WeightRecalcWorker", "recompute failed", e)
            return Result.success()
        }
        return Result.success()
    }
}

@HiltWorker
class UpdateCheckWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val updateChecker: ru.rufp.weather.update.UpdateChecker,
    private val updateStorage: ru.rufp.weather.update.UpdateStorage
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        try {
            val currentCode = applicationContext.packageManager
                .getPackageInfo(applicationContext.packageName, 0).let { pi ->
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                        pi.longVersionCode
                    } else {
                        @Suppress("DEPRECATION")
                        pi.versionCode.toLong()
                    }
                }
            val result = updateChecker.check(currentCode)
            if (result.update != null) updateStorage.save(result.update)
            else if (result.networkOk) updateStorage.clear()
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            return Result.success()
        }
        return Result.success()
    }
}
