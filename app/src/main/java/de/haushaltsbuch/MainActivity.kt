package de.haushaltsbuch

import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import dagger.hilt.android.AndroidEntryPoint
import de.haushaltsbuch.data.Settings
import de.haushaltsbuch.data.SettingsRepository
import de.haushaltsbuch.data.TxType
import de.haushaltsbuch.security.AppLock
import de.haushaltsbuch.ui.AppRoot
import de.haushaltsbuch.ui.HaushaltsbuchTheme
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    @Inject lateinit var settings: SettingsRepository

    private var unlocked by mutableStateOf(false)
    private var pendingType by mutableStateOf<TxType?>(null)
    private var lockOn = false
    private var stoppedAt = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        readIntent(intent)
        setContent {
            HaushaltsbuchTheme {
                val s by produceState<Settings?>(null) { settings.flow.collect { value = it } }
                SideEffect { lockOn = s?.appLock == true }
                LaunchedEffect(s?.appLock) {   // App-Inhalt in der Aufgabenübersicht verbergen
                    if (s?.appLock == true) window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                    else window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                }
                val locked = s?.appLock == true && !unlocked
                when {
                    s == null -> Box(Modifier.fillMaxSize())      // Einstellungen laden, nichts anzeigen
                    locked -> {
                        LaunchedEffect(Unit) { AppLock.prompt(this@MainActivity) { unlocked = true } }
                        LockScreen { AppLock.prompt(this@MainActivity) { unlocked = true } }
                    }
                    else -> {
                        LaunchedEffect(Unit) { unlocked = true }
                        AppRoot(initialType = pendingType, onInitialConsumed = { pendingType = null })
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        readIntent(intent)
    }

    private fun readIntent(i: Intent?) {
        val name = i?.getStringExtra(EXTRA_NEW) ?: return
        pendingType = runCatching { TxType.valueOf(name) }.getOrNull()
        i.removeExtra(EXTRA_NEW)
    }

    override fun onStop() {
        super.onStop()
        stoppedAt = SystemClock.elapsedRealtime()
    }

    /** Nach mehr als 30 s im Hintergrund wieder sperren. */
    override fun onStart() {
        super.onStart()
        if (lockOn && stoppedAt > 0 && SystemClock.elapsedRealtime() - stoppedAt > 30_000) unlocked = false
    }

    companion object { const val EXTRA_NEW = "new_type" }
}

@Composable
private fun LockScreen(onRetry: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(32.dp), Arrangement.Center, Alignment.CenterHorizontally) {
        Text("🔒", style = MaterialTheme.typography.displayMedium)
        Text("Haushaltsbuch ist gesperrt", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(vertical = 12.dp))
        Button(onClick = onRetry) { Text("Entsperren") }
    }
}
