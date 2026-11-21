package de.haushaltsbuch.notify

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import de.haushaltsbuch.MainActivity
import de.haushaltsbuch.data.*
import kotlinx.coroutines.flow.first
import java.time.YearMonth
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BudgetNotifier @Inject constructor(
    @ApplicationContext private val ctx: Context,
    private val db: AppDatabase,
    private val settings: SettingsRepository
) {
    /** Prüft nach jeder Ausgabe, ob eine neue Warnstufe erreicht wurde (je Kategorie und Monat nur einmal). */
    suspend fun onBooking(b: Booking) {
        val cat = b.categoryId ?: return
        if (b.type != TxType.EXPENSE) return
        val s = settings.flow.first()
        if (!s.budgetAlerts) return
        val limit = db.budgets().get(cat)?.limitCents ?: return
        val ym = YearMonth.from(b.date)
        val fx = Fx(
            s.baseCurrency,
            db.fx().all().associate { it.currency to it.toBase },
            db.accounts().all().associate { it.id to it.currency }
        )
        val spent = db.bookings().expenses(cat, ym.atDay(1), ym.atEndOfMonth()).sumOf { fx.toBase(it) }
        val level = alertLevel(spent, limit)
        val key = "${cat}_$ym"
        if (level == 0 || level <= settings.alertLevel(key)) return
        settings.setAlertLevel(key, level)
        val name = db.categories().get(cat)?.name ?: "Budget"
        show(
            id = (cat * 10 + level / 100).toInt(),
            title = if (level >= 100) "Budget aufgebraucht: $name" else "80 % des Budgets verbraucht: $name",
            text = "Dieser Monat: ${format(spent, s.baseCurrency)} von ${format(limit, s.baseCurrency)}"
        )
    }

    private fun format(c: Long, cur: String) =
        java.text.NumberFormat.getCurrencyInstance(java.util.Locale.GERMANY)
            .apply { currency = java.util.Currency.getInstance(cur) }.format(c / 100.0)

    @SuppressLint("MissingPermission")
    private fun show(id: Int, title: String, text: String) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        val open = PendingIntent.getActivity(
            ctx, 0, Intent(ctx, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val n = NotificationCompat.Builder(ctx, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_dialog_info)   // TODO: eigenes Icon
            .setContentTitle(title).setContentText(text)
            .setContentIntent(open).setAutoCancel(true).build()
        NotificationManagerCompat.from(ctx).notify(id, n)
    }

    companion object {
        const val CHANNEL = "budget"
        fun ensureChannel(ctx: Context) {
            if (Build.VERSION.SDK_INT >= 26) {
                ctx.getSystemService(NotificationManager::class.java)
                    .createNotificationChannel(NotificationChannel(CHANNEL, "Budget-Warnungen", NotificationManager.IMPORTANCE_DEFAULT))
            }
        }
    }
}
