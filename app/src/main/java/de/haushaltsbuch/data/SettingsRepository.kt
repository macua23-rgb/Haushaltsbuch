package de.haushaltsbuch.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.store by preferencesDataStore("settings")

data class Settings(
    val appLock: Boolean = false,
    val budgetAlerts: Boolean = true,
    val baseCurrency: String = "EUR"
)

@Singleton
class SettingsRepository @Inject constructor(@ApplicationContext private val ctx: Context) {
    private val lock = booleanPreferencesKey("app_lock")
    private val alerts = booleanPreferencesKey("budget_alerts")
    private val base = stringPreferencesKey("base_currency")

    val flow: Flow<Settings> = ctx.store.data.map {
        Settings(it[lock] ?: false, it[alerts] ?: true, it[base] ?: "EUR")
    }

    suspend fun setAppLock(v: Boolean) { ctx.store.edit { it[lock] = v } }
    suspend fun setBudgetAlerts(v: Boolean) { ctx.store.edit { it[alerts] = v } }
    suspend fun setBaseCurrency(v: String) { ctx.store.edit { it[base] = v } }

    /** Merkt sich pro Kategorie und Monat, welche Warnstufe (80/100) schon gemeldet wurde. */
    suspend fun alertLevel(key: String): Int = ctx.store.data.first()[intPreferencesKey("alert_$key")] ?: 0
    suspend fun setAlertLevel(key: String, level: Int) { ctx.store.edit { it[intPreferencesKey("alert_$key")] = level } }
}
