package com.tatkal.fastassistant

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tatkal.fastassistant.core.BookingFlow
import com.tatkal.fastassistant.core.BookingFlow.State
import com.tatkal.fastassistant.core.BookingFlow.Payment
import kotlinx.coroutines.delay
import org.json.JSONObject
import java.time.*
import java.time.format.DateTimeFormatter

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val migration = getSharedPreferences("security_migration", Context.MODE_PRIVATE)
        if (!migration.getBoolean("webview_cleared", false)) {
            android.webkit.CookieManager.getInstance().removeAllCookies { success ->
                android.webkit.CookieManager.getInstance().flush()
                migration.edit().putBoolean("webview_cleared", true).apply()
            }
            android.webkit.WebStorage.getInstance().deleteAllData()
            android.webkit.WebView(this).apply { clearCache(true); clearFormData(); clearHistory(); destroy() }
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        setContent { MaterialTheme(colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()) { Home(this) } }
    }
}

@Composable
fun Home(context: Context) {
    val store = remember { SecureStore(context) }
    var storageError by remember { mutableStateOf(false) }
    val data = remember { try { store.read() } catch (_: Exception) { storageError = true; JSONObject() } }
    var flow by remember { mutableStateOf(BookingFlow(
        runCatching { State.valueOf(data.optString("state", "READY")) }.getOrDefault(State.READY),
        runCatching { Payment.valueOf(data.optString("payment", "NONE")) }.getOrDefault(Payment.UNKNOWN))) }
    var revision by remember { mutableIntStateOf(0) }
    var tab by remember { mutableIntStateOf(0) }
    var message by remember { mutableStateOf("Preparation companion • booking steps remain manual on IRCTC.") }
    var now by remember { mutableStateOf(Instant.now()) }
    var reviewed by remember { mutableStateOf(false) }
    var historyChecked by remember { mutableStateOf(false) }
    var transactionChecked by remember { mutableStateOf(false) }
    var deleteDialog by remember { mutableStateOf(false) }
    // Migrate only known preparation fields. Never import browser cookies or credentials.
    LaunchedEffect(Unit) {
        val legacy = context.getSharedPreferences("tatkal_prefs", Context.MODE_PRIVATE)
        if (!storageError && legacy.all.isNotEmpty()) {
            listOf("from", "to").forEach { if (!data.has(it)) data.put(it, legacy.getString(it, "")) }
            try { data.put("state", flow.state().name).put("payment", flow.payment().name); store.write(data); legacy.edit().clear().commit() } catch (_: Exception) { storageError = true }
        }
        while (true) { now = Instant.now(); delay(1000) }
    }
    fun persist(): Boolean = try {
        data.put("state", flow.state().name).put("payment", flow.payment().name)
        store.write(data); revision++; true
    } catch (_: Exception) { storageError = true; message = "Encrypted save failed. Booking handoff is blocked; check official status if payment was attempted."; false }
    fun update(key: String, value: String) { data.put(key, value); reviewed = false; persist() }
    fun act(action: () -> Unit) {
        try { action(); persist() } catch (e: IllegalStateException) { message = e.message ?: "Action blocked." }
    }
    fun official() {
        try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.irctc.co.in/nget/train-search"))) }
        catch (_: Exception) { message = "No browser is available. Open the official IRCTC app manually." }
    }
    fun options() = data.optString("priorities").lines().filter { it.isNotBlank() }.map { it.trim().uppercase().split(Regex("\\s+")) }
    fun errors(): List<String> = buildList {
        val station = Regex("[A-Z]{2,5}")
        listOf("from", "to", "boarding").forEach { if (!station.matches(data.optString(it))) add("Enter a valid $it station code.") }
        if (data.optString("from") == data.optString("to")) add("From and To must differ.")
        val date = runCatching { LocalDate.parse(data.optString("date")) }.getOrNull()
        if (date == null || date < LocalDate.now(ZoneId.of("Asia/Kolkata"))) add("Enter a current/future journey date: YYYY-MM-DD.")
        val origin = runCatching { LocalDate.parse(data.optString("origin")) }.getOrNull()
        if (origin == null || (date != null && origin > date)) add("Check the train's origin departure date.")
        if ((data.optString("maxFare").toBigDecimalOrNull() ?: java.math.BigDecimal.ZERO) <= java.math.BigDecimal.ZERO) add("Enter a positive maximum total fare.")
        val opts = options()
        if (opts.isEmpty() || opts.size > 12 || opts.any { it.size != 2 || !Regex("[0-9]{5}").matches(it[0]) || it[1] !in listOf("2A","3A","3E","CC","EC","SL","2S","FC") }) add("Use one approved train/class per line, e.g. 16866 SL (maximum 12).")
        if (opts.distinct().size != opts.size) add("Remove duplicate train/class choices.")
        val passengers = data.optString("passengers").lines().filter { it.isNotBlank() }
        if (passengers.isEmpty() || passengers.size > 4) add("Enter 1–4 passengers.")
        passengers.forEachIndexed { i, line ->
            val p = line.split('|').map { it.trim() }
            if (p.size != 5 || p[0].length !in 3..60 || (p.getOrNull(1)?.toIntOrNull() ?: -1) !in 1..125 || p.getOrNull(2)?.uppercase() !in listOf("M","F","T") || p.getOrNull(3).isNullOrBlank() || p.getOrNull(4).isNullOrBlank()) add("Passenger ${i+1}: use Name | Age | M/F/T | Berth | Meal.")
        }
    }
    val renderRevision = revision // Observe edits made to the JSON document.
    val locked = flow.paymentLocked() || storageError
    val canEdit = !locked && flow.state() in listOf(State.READY, State.FAILED)
    val field: @Composable (String, String, Boolean) -> Unit = { key, label, multi ->
        OutlinedTextField(value = data.optString(key), onValueChange = { update(key, it) }, label = { Text(label) },
            modifier = Modifier.fillMaxWidth(), enabled = canEdit, singleLine = !multi, minLines = if(multi) 3 else 1)
    }
    Surface(Modifier.fillMaxSize()) {
        Column(Modifier.safeDrawingPadding().padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Tatkal Fast Assistant", style = MaterialTheme.typography.headlineSmall)
            Text("Prepare calmly. Recover safely.", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Prepare", "Review", "Recovery").forEachIndexed { i, label -> FilterChip(tab == i, { tab = i }, label = { Text(label) }) }
            }
            Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(flow.state().name.replace('_', ' '), style = MaterialTheme.typography.titleLarge)
                Text("Local progress only — not live IRCTC status.")
                Text(message)
                if (locked) Text("Do not pay again. Verify Booking History and Failed Transactions on official IRCTC.", color = MaterialTheme.colorScheme.error)
                if (storageError) Text("Saved data could not be read or written. Restart after resolving device storage. The app will not reset payment safety state.")
            } }
            if (tab == 0) {
                Text("Journey", style = MaterialTheme.typography.titleLarge)
                field("from", "From station code (e.g. TJ)", false)
                field("to", "To station code (e.g. MS)", false)
                field("boarding", "Boarding station code", false)
                field("date", "Your journey date • YYYY-MM-DD", false)
                field("origin", "First choice train origin date • YYYY-MM-DD", false)
                Text("Quota: TATKAL. Verify the origin departure date separately for each alternative train.")
                field("priorities", "Approved priority order • one train/class per line", true)
                Text("Example: 16866 3A, then 16866 3E, then 16866 SL — put each on its own line.")
                field("maxFare", "Maximum total fare ₹", false)
                Text("Acceptable availability")
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("CNF", "RAC", "WL").forEach { kind -> FilterChip(data.optString("accept", "CNF") == kind,
                        { update("accept", kind) }, enabled = canEdit, label = { Text(kind) }) }
                }
                Text("CNF: confirmed only. RAC: confirmed or RAC. WL: confirmed, RAC or waiting list.")
                Text("Passenger profiles", style = MaterialTheme.typography.titleLarge)
                field("passengers", "Name | Age | M/F/T | Berth | Meal", true)
                Text("One passenger per line, up to four. Berth/meal preferences depend on class and availability. Enter any additional required information directly on IRCTC.")
                Text("Saved automatically with Android Keystore encryption. No passwords, OTPs, payment secrets or login sessions are collected.")
                OutlinedButton({ deleteDialog = true }, enabled = !storageError) { Text("Delete saved passenger information") }
                Button({ val e = errors(); message = if(e.isEmpty()) "Preparation validated. Verify station route and train schedule on IRCTC." else e.joinToString("\n"); if(e.isEmpty()) tab = 1 }) { Text("Validate preparation") }
            }
            if (tab == 1) {
                Text("Opening countdown", style = MaterialTheme.typography.titleLarge)
                Text("Device time: " + now.atZone(ZoneId.of("Asia/Kolkata")).format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm:ss 'IST'")))
                val opening = runCatching { BookingFlow.opening(LocalDate.parse(data.optString("origin")), options().first()[1]) }.getOrNull()
                if (opening != null) {
                    Text("First choice opens: " + opening.atZone(ZoneId.of("Asia/Kolkata")).format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm 'IST'")))
                    val seconds = Duration.between(now, opening).seconds.coerceAtLeast(0)
                    Text(if (seconds == 0L) "Scheduled opening time reached; availability is unverified." else "${seconds/3600}h ${(seconds%3600)/60}m ${seconds%60}s remaining")
                } else Text("Complete origin date and priority list to calculate opening.")
                Text("AC 10:00 / non-AC 11:00 IST, one day before origin departure. Reference checked 5 Oct 2026. Device clock must be correct. No requests are sent by this countdown.")
                Text("Booking summary", style = MaterialTheme.typography.titleLarge)
                Text("${data.optString("from")} → ${data.optString("to")}\nDate: ${data.optString("date")}\nBoarding: ${data.optString("boarding")}\nQuota: TATKAL\nMaximum: ₹${data.optString("maxFare")}\nAccepted: ${data.optString("accept", "CNF")}\nPassengers:\n${data.optString("passengers")}")
                Text("Approved alternatives (manual selection on IRCTC)")
                options().forEachIndexed { index, parts -> Text("${index+1}. ${parts.joinToString(" • ")}") }
                Button({ if (errors().isEmpty() && persist()) official() else message = errors().joinToString("\n") }, enabled = !storageError) { Text("Open official IRCTC") }
                Text("Use IRCTC's own Master List for permitted fast passenger entry. Aadhaar authentication and any requested OTP are completed there. Login remains with your browser.")
                Text("Record manual progress", style = MaterialTheme.typography.titleMedium)
                OutlinedButton({ act { flow.report(State.SEARCHING) }; reviewed = false }, enabled = !locked && errors().isEmpty()) { Text("I started / resumed search") }
                OutlinedButton({ act { flow.report(State.TRAIN_SELECTED) } }, enabled = !locked && flow.state() == State.SEARCHING) { Text("I selected an approved train/class") }
                OutlinedButton({ act { flow.report(State.PASSENGERS_FILLED) } }, enabled = !locked && flow.state() == State.TRAIN_SELECTED) { Text("I verified passenger details") }
                if (flow.state() == State.PASSENGERS_FILLED || flow.state() == State.PAYMENT_REQUIRED) {
                    Text("Before paying, record the exact selected option and total fare shown by IRCTC.")
                    OutlinedTextField(data.optString("selected"), { data.put("selected", it.uppercase()); reviewed = false; persist() }, label = { Text("Selected train + class (e.g. 16866 SL)") }, enabled = flow.state() == State.PASSENGERS_FILLED)
                    OutlinedTextField(data.optString("fare"), { data.put("fare", it); reviewed = false; persist() }, label = { Text("Official total fare ₹") }, enabled = flow.state() == State.PASSENGERS_FILLED)
                    Row { Checkbox(reviewed, { reviewed = it }); Text("I checked train, date, route, boarding, class, quota, passengers, accepted availability and fare on IRCTC.") }
                    Button({
                        act {
                            check(options().any { it.joinToString(" ") == data.optString("selected").trim() }) { "Selected train/class must match your approved list." }
                            fun paise(key: String): Long = runCatching { data.optString(key).toBigDecimal().movePointRight(2).longValueExact() }.getOrDefault(-1)
                            flow.reviewForPayment(reviewed, paise("fare"), paise("maxFare"))
                        }
                    }, enabled = reviewed && flow.state() == State.PASSENGERS_FILLED) { Text("Confirm reviewed summary") }
                    Button({
                        try { flow.beginManualPayment(); if (persist()) { tab = 2; message = "Payment handoff recorded. Authenticate only on IRCTC. Return here if the outcome is uncertain."; official() } }
                        catch (e: IllegalStateException) { message = e.message ?: "Blocked" }
                    }, enabled = reviewed && flow.state() == State.PAYMENT_REQUIRED && !locked) { Text("Record payment handoff & open IRCTC") }
                }
            }
            if (tab == 2) {
                Text("Safe recovery", style = MaterialTheme.typography.titleLarge)
                Text("This app cannot detect browser errors or check transactions automatically. Keep the official browser tab open; do not repeat submission after a timeout.")
                OutlinedButton({ act { flow.interrupted() }; message = "Connection failed or result uncertain. Details are preserved. Check official status before continuing." }, enabled = !storageError) { Text("Connection failed / result unknown") }
                OutlinedButton({ act { flow.report(State.SESSION_EXPIRED) }; message = "Log in on official IRCTC, check booking/payment history, then resume the safest verified step." }, enabled = !storageError) { Text("Session expired") }
                OutlinedButton({ act { flow.report(State.CAPTCHA_REQUIRED) }; message = "CAPTCHA required. Please complete it manually on IRCTC." }, enabled = !storageError) { Text("CAPTCHA required") }
                OutlinedButton({ act { flow.report(State.OTP_REQUIRED) }; message = "OTP required. Enter it only on official IRCTC. No OTP is read or saved here." }, enabled = !storageError) { Text("OTP required") }
                Button({ official() }) { Text("Open IRCTC to check status / print ticket") }
                Text("Go to My Account → My Transactions. Inspect Booked Ticket History and Failed Transactions. A bank debit alone does not confirm a ticket. Retrieve the PNR and official ticket there.")
                if (flow.paymentLocked()) {
                    Row { Checkbox(historyChecked, { historyChecked = it }); Text("I checked official Booked Ticket History.") }
                    Row { Checkbox(transactionChecked, { transactionChecked = it }); Text("I checked official transaction status for this attempt.") }
                    listOf(Payment.UNKNOWN, Payment.PENDING, Payment.FAILED, Payment.SUCCESS).forEach { status ->
                        OutlinedButton({ act { flow.recordUserCheckedStatus(status, historyChecked, transactionChecked) }; historyChecked = false; transactionChecked = false; message = when(status) {
                            Payment.FAILED -> "Recorded your FAILED report. Retry only if IRCTC confirms no ticket was booked and permits retry; review all details again."
                            Payment.SUCCESS -> "Recorded your SUCCESS report, not independently verified. View PNR and download/print the official ticket in IRCTC."
                            else -> "Payment remains unresolved. Do not pay again. Follow IRCTC's status-check instructions."
                        } }, enabled = historyChecked && transactionChecked && !storageError) { Text("Record my status check: ${status.name}") }
                    }
                }
                Text("No automatic refresh or retries run against IRCTC. Follow any queue, maintenance, rate-limit or retry instructions shown there.")
            }
            Spacer(Modifier.height(16.dp))
        }
    }
    if (deleteDialog) AlertDialog(onDismissRequest = { deleteDialog = false }, title = { Text("Delete passenger information?") },
        text = { Text("Passenger names and preferences will be removed. Payment safety status is retained.") },
        confirmButton = { TextButton({ data.remove("passengers"); context.getSharedPreferences("tatkal_prefs", Context.MODE_PRIVATE).edit().clear().commit(); persist(); reviewed = false; deleteDialog = false }) { Text("Delete") } },
        dismissButton = { TextButton({ deleteDialog = false }) { Text("Cancel") } })
}
