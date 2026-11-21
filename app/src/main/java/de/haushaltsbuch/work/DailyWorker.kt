package de.haushaltsbuch.work

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import de.haushaltsbuch.data.LedgerRepository
import de.haushaltsbuch.widget.BalanceWidget
import java.time.LocalDate

@HiltWorker
class DailyWorker @AssistedInject constructor(
    @Assisted ctx: Context,
    @Assisted params: WorkerParameters,
    private val repo: LedgerRepository
) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        repo.runDailyJobs(LocalDate.now())
        BalanceWidget().updateAll(applicationContext)
        return Result.success()
    }
}
