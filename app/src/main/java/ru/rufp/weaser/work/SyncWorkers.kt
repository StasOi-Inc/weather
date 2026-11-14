package ru.rufp.weaser.work

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
import ru.rufp.weaser.widget.WeaserWidget
import java.util.concurrent.TimeUnit

object SyncScheduler {

    private const val SYNC_PERIODIC = "weaser_forecast_sync"
    private const val SYNC_ADAPTIVE = "weaser_forecast_sync_adaptive"
    private const val WEIGHTS_RECALC = "weaser_weights_recalc"

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
    private val forecastRepository: ru.rufp.weaser.data.repository.ForecastRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val ok = runCatching { forecastRepository.sync() }.getOrDefault(false)
        if (ok) {
            WeaserWidget().updateAll(applicationContext)
            val rainImminent = runCatching { forecastRepository.rainImminentNextTwoHours() }.getOrDefault(false)
            SyncScheduler.scheduleAdaptiveIfRain(applicationContext, rainImminent)
        }
        return Result.success()
    }
}

@HiltWorker
class WeightRecalcWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val forecastRepository: ru.rufp.weaser.data.repository.ForecastRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        runCatching { forecastRepository.recomputeMetrics() }
        return Result.success()
    }
}
