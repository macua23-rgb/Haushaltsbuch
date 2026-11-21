package de.haushaltsbuch.export

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import de.haushaltsbuch.data.*
import java.io.OutputStream
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Currency
import java.util.Locale
import kotlin.math.abs

data class ExportRow(
    val date: LocalDate, val type: TxType, val category: String, val account: String,
    val toAccount: String, val note: String, val tags: String,
    val amountCents: Long, val currency: String, val baseCents: Long
) {
    private fun sign(v: Long) = if (type == TxType.EXPENSE) -v else v
    val signed get() = sign(amountCents)
    val signedBase get() = sign(baseCents)
}

object Exporter {
    private val typeLabel = mapOf(TxType.INCOME to "Einnahme", TxType.EXPENSE to "Ausgabe", TxType.TRANSFER to "Umbuchung")

    fun rows(b: List<Booking>, cats: Map<Long, Category>, accounts: Map<Long, Account>, fx: Fx): List<ExportRow> =
        b.sortedWith(compareBy({ it.date }, { it.id })).map {
            ExportRow(
                date = it.date, type = it.type,
                category = it.categoryId?.let { id -> cats[id]?.name }.orEmpty(),
                account = accounts[it.accountId]?.name.orEmpty(),
                toAccount = it.toAccountId?.let { id -> accounts[id]?.name }.orEmpty(),
                note = it.note, tags = it.tags,
                amountCents = it.amountCents, currency = fx.currencyOf(it.accountId), baseCents = fx.toBase(it)
            )
        }

    /** 1250 -> "12,50", -5 -> "-0,05" */
    fun decimal(c: Long): String {
        val a = abs(c)
        return (if (c < 0) "-" else "") + "${a / 100},${(a % 100).toString().padStart(2, '0')}"
    }

    /** Maskiert ein CSV-Feld und entschärft Excel-Formeln (=, +, -, @ am Anfang). */
    fun cell(s: String): String {
        val safe = if (s.isNotEmpty() && s[0] in "=+-@") "'$s" else s
        return if (safe.any { it == ';' || it == '"' || it == '\n' || it == '\r' }) "\"" + safe.replace("\"", "\"\"") + "\"" else safe
    }

    fun csv(rows: List<ExportRow>): String = buildString {
        append("Datum;Typ;Kategorie;Konto;Zielkonto;Notiz;Tags;Betrag;Währung;Betrag (Basiswährung)\r\n")
        rows.forEach {
            append(listOf(
                it.date.toString(), typeLabel.getValue(it.type), cell(it.category), cell(it.account), cell(it.toAccount),
                cell(it.note), cell(it.tags), decimal(it.signed), it.currency, decimal(it.signedBase)
            ).joinToString(";")).append("\r\n")
        }
    }

    /** UTF-8 mit BOM, damit Excel Umlaute richtig anzeigt. */
    fun writeCsv(rows: List<ExportRow>, out: OutputStream) {
        out.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))
        out.write(csv(rows).toByteArray(Charsets.UTF_8))
    }

    fun writePdf(rows: List<ExportRow>, title: String, base: String, out: OutputStream) {
        val money = NumberFormat.getCurrencyInstance(Locale.GERMANY)
        fun fmt(c: Long, cur: String) = money.apply { currency = Currency.getInstance(cur) }.format(c / 100.0)
        val dateFmt = DateTimeFormatter.ofPattern("dd.MM.yyyy")
        val doc = PdfDocument()
        val text = Paint().apply { textSize = 9f }
        val bold = Paint(text).apply { isFakeBoldText = true }
        val right = Paint(text).apply { textAlign = Paint.Align.RIGHT }
        val boldRight = Paint(bold).apply { textAlign = Paint.Align.RIGHT }
        var page: PdfDocument.Page? = null
        var canvas: Canvas? = null
        var y = 0f
        var n = 0

        fun newPage() {
            page?.let { doc.finishPage(it) }
            n++
            val pg = doc.startPage(PdfDocument.PageInfo.Builder(595, 842, n).create())
            page = pg
            val c = pg.canvas
            canvas = c
            y = 48f
            if (n == 1) { c.drawText(title, 36f, y, Paint(bold).apply { textSize = 14f }); y += 24f }
            c.drawText("Datum", 36f, y, bold); c.drawText("Kategorie", 96f, y, bold)
            c.drawText("Notiz", 196f, y, bold); c.drawText("Konto", 370f, y, bold)
            c.drawText("Betrag", 559f, y, boldRight)
            y += 14f
        }

        fun clip(s: String, max: Int) = if (s.length > max) s.take(max - 1) + "…" else s

        newPage()
        rows.forEach {
            if (y > 800f) newPage()
            val c = canvas!!
            c.drawText(it.date.format(dateFmt), 36f, y, text)
            c.drawText(clip(if (it.type == TxType.TRANSFER) "Umbuchung" else it.category, 18), 96f, y, text)
            c.drawText(clip(it.note, 32), 196f, y, text)
            c.drawText(clip(if (it.toAccount.isNotEmpty()) "${it.account} → ${it.toAccount}" else it.account, 22), 370f, y, text)
            c.drawText(fmt(it.signed, it.currency), 559f, y, right)
            y += 13f
        }
        if (y > 760f) newPage()
        val inc = rows.filter { it.type == TxType.INCOME }.sumOf { it.baseCents }
        val exp = rows.filter { it.type == TxType.EXPENSE }.sumOf { it.baseCents }
        y += 14f
        listOf("Einnahmen" to inc, "Ausgaben" to -exp, "Saldo" to inc - exp).forEach { (l, v) ->
            canvas!!.drawText(l, 370f, y, bold); canvas!!.drawText(fmt(v, base), 559f, y, boldRight); y += 13f
        }
        page?.let { doc.finishPage(it) }
        doc.writeTo(out)
        doc.close()
    }
}
