package de.haushaltsbuch.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import de.haushaltsbuch.data.Booking
import de.haushaltsbuch.data.ReceiptStore
import de.haushaltsbuch.data.TxType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate

/** Neue Buchung erfassen oder bestehende bearbeiten ([initial] != null). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingSheet(
    ui: UiState, initial: Booking?, startType: TxType, receipts: ReceiptStore,
    onDismiss: () -> Unit, onSave: (Booking) -> Unit, onDelete: (() -> Unit)? = null
) {
    var type by remember { mutableStateOf(initial?.type ?: startType) }
    var amount by remember { mutableStateOf(initial?.let { centsToInput(it.amountCents) } ?: "") }
    var accountId by remember { mutableStateOf(initial?.accountId ?: ui.accounts.firstOrNull()?.account?.id ?: 0L) }
    var toId by remember { mutableStateOf(initial?.toAccountId) }
    var toAmount by remember { mutableStateOf(initial?.toAmountCents?.let { centsToInput(it) } ?: "") }
    var catId by remember { mutableStateOf(initial?.categoryId) }
    var note by remember { mutableStateOf(initial?.note ?: "") }
    var tags by remember { mutableStateOf(initial?.tags ?: "") }
    var date by remember { mutableStateOf(initial?.date ?: LocalDate.now()) }
    var receipt by remember { mutableStateOf(initial?.receiptUri) }
    var saved by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var pendingPath by rememberSaveable { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    /** Ein noch nicht gespeichertes Foto wird sofort gelöscht; das gespeicherte Original erst beim Speichern. */
    fun dropCurrent() {
        val r = receipt
        if (r != null && r != initial?.receiptUri) receipts.delete(r)
    }
    val latest by rememberUpdatedState(receipt)
    DisposableEffect(Unit) {
        onDispose {
            val r = latest
            if (!saved && r != null && r != initial?.receiptUri) receipts.delete(r)
        }
    }

    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val f = pendingPath?.let { File(it) }
        pendingPath = null
        if (ok && f != null) { dropCurrent(); receipt = receipts.uriFor(f).toString() }
        else if (f != null) receipts.deleteFile(f)
    }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch {
            val u = withContext(Dispatchers.IO) { receipts.importFrom(uri) }
            dropCurrent(); receipt = u.toString()
        }
    }

    val fromCur = ui.currencyOf(accountId)
    val toCur = ui.currencyOf(toId)
    val cross = type == TxType.TRANSFER && toId != null && fromCur != toCur
    val rateOk = ui.fx.hasRate(fromCur) && ui.fx.hasRate(toCur)
    val cents = parseCents(amount)
    val suggested = if (cross && rateOk) ui.fx.convert(cents, fromCur, toCur) else 0L

    fun save() {
        if (ui.accounts.none { it.account.id == accountId }) { error = "Konto wählen"; return }
        if (cents <= 0) { error = "Betrag eingeben"; return }
        if (type == TxType.TRANSFER && (toId == null || toId == accountId)) { error = "Zielkonto wählen"; return }
        var toCents: Long? = null
        if (cross) {
            toCents = if (toAmount.isNotBlank()) parseCents(toAmount) else if (rateOk) suggested else null
            if (toCents == null || toCents <= 0) { error = "Betrag im Zielkonto eingeben"; return }
        }
        val base = initial ?: Booking(type = type, amountCents = cents, date = date, accountId = accountId)
        saved = true
        onSave(
            base.copy(
                type = type, amountCents = cents, date = date, accountId = accountId,
                toAccountId = toId.takeIf { type == TxType.TRANSFER },
                toAmountCents = toCents.takeIf { type == TxType.TRANSFER },
                categoryId = catId.takeIf { type != TxType.TRANSFER },
                note = note.trim(), tags = tags.trim(), receiptUri = receipt
            )
        )
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            Modifier.padding(horizontal = 16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val types = listOf(TxType.EXPENSE to "Ausgabe", TxType.INCOME to "Einnahme", TxType.TRANSFER to "Umbuchung")
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                types.forEachIndexed { i, (t, l) ->
                    SegmentedButton(
                        selected = type == t,
                        onClick = { type = t; catId = null },
                        shape = SegmentedButtonDefaults.itemShape(i, types.size)
                    ) { Text(l) }
                }
            }
            Text(
                "${amount.ifEmpty { "0" }} ${symbolOf(fromCur)}",
                Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
                style = MaterialTheme.typography.displaySmall,
                color = when (type) {
                    TxType.EXPENSE -> Expense
                    TxType.INCOME -> Income
                    else -> MaterialTheme.colorScheme.onSurface
                }
            )
            if (type != TxType.TRANSFER) {
                ChipRow(
                    ui.categories.values.filter { it.type == type }.map { it.id to "${it.icon} ${it.name}" },
                    catId
                ) { catId = it }
            }
            Text(if (type == TxType.TRANSFER) "Von" else "Konto", style = MaterialTheme.typography.labelMedium)
            ChipRow(ui.accounts.map { it.account.id to "${it.account.icon} ${it.account.name}" }, accountId) { accountId = it }
            if (type == TxType.TRANSFER) {
                Text("Nach", style = MaterialTheme.typography.labelMedium)
                ChipRow(
                    ui.accounts.filter { it.account.id != accountId }.map { it.account.id to "${it.account.icon} ${it.account.name}" },
                    toId
                ) { toId = it }
                if (cross) {
                    AmountField(
                        toAmount, { toAmount = it }, "Betrag im Zielkonto ($toCur)", Modifier.fillMaxWidth()
                    )
                    Text(
                        if (rateOk) "Leer lassen für ${suggested.money(toCur)} (gespeicherter Kurs)"
                        else "Für $fromCur oder $toCur fehlt ein Kurs (Einstellungen), bitte Betrag eingeben.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            DatePickerField(date, { date = it })
            OutlinedTextField(note, { note = it }, Modifier.fillMaxWidth(), label = { Text("Notiz") }, singleLine = true)
            OutlinedTextField(tags, { tags = it }, Modifier.fillMaxWidth(), label = { Text("Tags (mit Komma trennen)") }, singleLine = true)

            Text("Beleg", style = MaterialTheme.typography.labelMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                receipt?.let { AsyncImage(it, "Beleg", Modifier.size(64.dp)) }
                OutlinedButton(onClick = {
                    val f = receipts.newFile()
                    pendingPath = f.absolutePath
                    camera.launch(receipts.uriFor(f))
                }) { Text("Foto") }
                OutlinedButton(onClick = {
                    gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }) { Text("Galerie") }
                if (receipt != null) TextButton(onClick = { dropCurrent(); receipt = null }) { Text("Entfernen") }
            }

            Numpad { amount = amountKey(amount, it) }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(onClick = { save() }, Modifier.fillMaxWidth()) {
                Text(if (initial == null) "Buchung speichern" else "Änderungen speichern")
            }
            if (onDelete != null) {
                TextButton(onClick = { confirmDelete = true }, Modifier.fillMaxWidth()) {
                    Text("Buchung löschen", color = MaterialTheme.colorScheme.error)
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
    if (confirmDelete) {
        ConfirmDialog("Buchung löschen?", "Die Buchung und ein angehängter Beleg werden entfernt.", "Löschen",
            onConfirm = { confirmDelete = false; dropCurrent(); saved = true; onDelete?.invoke() },
            onDismiss = { confirmDelete = false })
    }
}

@Composable
private fun Numpad(onKey: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", ",", "0", "⌫").chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { k ->
                    FilledTonalButton(onClick = { onKey(k) }, Modifier.weight(1f)) {
                        Text(k, style = MaterialTheme.typography.titleLarge)
                    }
                }
            }
        }
    }
}
