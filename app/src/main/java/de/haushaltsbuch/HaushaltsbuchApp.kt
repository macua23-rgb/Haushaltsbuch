package de.haushaltsbuch

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.HiltAndroidApp
import de.haushaltsbuch.data.LedgerRepository
import de.haushaltsbuch.notify.BudgetNotifier
import de.haushaltsbuch.work.DailyWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class HaushaltsbuchApp : Application(), Configuration.Provider {
    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var repo: LedgerRepository

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        BudgetNotifier.ensureChannel(this)
        CoroutineScope(Dispatchers.IO).launch { repo.seedIfEmpty() }
        // Täglich: wiederkehrende Buchungen anlegen, Kreditkarten abrechnen
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "daily", ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<DailyWorker>(1, TimeUnit.DAYS).build()
        )
    }
}
