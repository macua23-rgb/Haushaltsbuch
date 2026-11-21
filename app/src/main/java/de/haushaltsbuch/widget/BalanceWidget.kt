package de.haushaltsbuch.widget

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.Button
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.actionStartActivity
import androidx.glance.appwidget.action.actionStartActivity as startActivityWithIntent
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import de.haushaltsbuch.MainActivity
import de.haushaltsbuch.data.AccountType
import de.haushaltsbuch.data.Fx
import de.haushaltsbuch.data.LedgerRepository
import de.haushaltsbuch.data.TxType
import de.haushaltsbuch.ui.money
import kotlinx.coroutines.flow.first

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint { fun repo(): LedgerRepository }

class BalanceWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repo = EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java).repo()
        val s = repo.settingsFlow.first()
        val fx = Fx(s.baseCurrency, repo.rates.first().associate { it.currency to it.toBase })
        // Die Sperre schützt auch das Widget: bei aktiver App-Sperre wird kein Saldo angezeigt.
        val text = if (s.appLock) "••••••" else
            repo.accounts.first().filter { it.account.type != AccountType.SAVINGS }
                .sumOf { fx.toBase(it.balanceCents, it.account.currency) }.money(s.baseCurrency)
        provideContent { WidgetContent(context, text) }
    }
}

class BalanceWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = BalanceWidget()
}

private fun newBooking(ctx: Context, type: TxType) =
    Intent(ctx, MainActivity::class.java)
        .setData(Uri.parse("haushaltsbuch://new/${type.name}"))     // eindeutig pro Button
        .putExtra(MainActivity.EXTRA_NEW, type.name)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)

@Composable
private fun WidgetContent(ctx: Context, balance: String) {
    Column(
        GlanceModifier.fillMaxSize().background(ColorProvider(Color(0xFF12201A))).padding(12.dp)
            .clickable(actionStartActivity<MainActivity>()),
        verticalAlignment = Alignment.Vertical.CenterVertically
    ) {
        Text("Kontostand", style = TextStyle(color = ColorProvider(Color(0xFFB9C9C0)), fontSize = 12.sp))
        Text(balance, style = TextStyle(color = ColorProvider(Color.White), fontSize = 24.sp, fontWeight = FontWeight.Bold))
        Spacer(GlanceModifier.height(8.dp))
        Row(GlanceModifier.fillMaxWidth()) {
            Button("− Ausgabe", actionStartActivity(newBooking(ctx, TxType.EXPENSE)), GlanceModifier.defaultWeight())
            Spacer(GlanceModifier.width(8.dp))
            Button("+ Einnahme", actionStartActivity(newBooking(ctx, TxType.INCOME)), GlanceModifier.defaultWeight())
        }
    }
}
