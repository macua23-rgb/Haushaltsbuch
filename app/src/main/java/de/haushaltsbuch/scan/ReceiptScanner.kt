package de.haushaltsbuch.scan

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/** Texterkennung auf dem Gerät (ML Kit, lateinische Schrift). Das Foto verlässt das Handy nicht. */
object ReceiptScanner {
    /** Liefert den erkannten Text oder null, wenn das Bild nicht gelesen werden konnte. */
    suspend fun readText(ctx: Context, uri: Uri): String? = suspendCancellableCoroutine { cont ->
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val image = try {
            InputImage.fromFilePath(ctx, uri)
        } catch (e: Exception) {
            recognizer.close()
            cont.resume(null)
            return@suspendCancellableCoroutine
        }
        recognizer.process(image)
            .addOnSuccessListener { if (cont.isActive) cont.resume(it.text) }
            .addOnFailureListener { if (cont.isActive) cont.resume(null) }
            .addOnCompleteListener { recognizer.close() }
    }
}
