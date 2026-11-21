package de.haushaltsbuch.security

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/** Fingerabdruck/Gesicht, mit PIN/Muster des Geräts als Rückfall. */
object AppLock {
    private const val AUTH = BIOMETRIC_WEAK or DEVICE_CREDENTIAL

    fun available(ctx: Context) =
        BiometricManager.from(ctx).canAuthenticate(AUTH) == BiometricManager.BIOMETRIC_SUCCESS

    fun prompt(activity: FragmentActivity, onSuccess: () -> Unit) {
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Haushaltsbuch entsperren")
            .setAllowedAuthenticators(AUTH)
            .build()
        BiometricPrompt(
            activity, ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = onSuccess()
            }
        ).authenticate(info)
    }
}
