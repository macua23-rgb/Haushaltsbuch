package de.haushaltsbuch.data

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/** Beleg-Fotos liegen im App-Speicher (nicht in der Galerie) und werden per FileProvider geteilt. */
@Singleton
class ReceiptStore @Inject constructor(@ApplicationContext private val ctx: Context) {
    private val dir get() = File(ctx.filesDir, "receipts").also { it.mkdirs() }

    fun newFile(): File = File(dir, "r_${System.currentTimeMillis()}.jpg")
    fun uriFor(f: File): Uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", f)

    /** Kopiert ein Bild aus der Galerie in den App-Speicher. */
    fun importFrom(src: Uri): Uri {
        val f = newFile()
        ctx.contentResolver.openInputStream(src)?.use { i -> f.outputStream().use { o -> i.copyTo(o) } }
        return uriFor(f)
    }

    fun delete(uri: String) {
        val name = Uri.parse(uri).lastPathSegment ?: return
        File(dir, name).takeIf { it.parentFile == dir }?.delete()
    }

    fun deleteFile(f: File) { if (f.parentFile == dir) f.delete() }
}
