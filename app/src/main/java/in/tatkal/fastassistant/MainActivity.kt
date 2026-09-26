package in.tatkal.fastassistant

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private const val PREFS = "tatkal_prefs"

class MainActivity : ComponentActivity() {
 override fun onCreate(savedInstanceState: Bundle?) {
  super.onCreate(savedInstanceState)
  setContent {
   MaterialTheme(colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()) {
    Home(this) { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.irctc.co.in/"))) }
   }
  }
 }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Home(context: Context, openIrctc: () -> Unit) {
 val prefs = remember { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE) }
 var from by remember { mutableStateOf(prefs.getString("from","") ?: "") }
 var to by remember { mutableStateOf(prefs.getString("to","") ?: "") }
 var date by remember { mutableStateOf(prefs.getString("date","") ?: "") }
 var train by remember { mutableStateOf(prefs.getString("train","") ?: "") }
 var passenger by remember { mutableStateOf(prefs.getString("passenger","") ?: "") }
 var travelClass by remember { mutableStateOf(prefs.getString("class","3A") ?: "3A") }
 var ac by remember { mutableStateOf(prefs.getBoolean("ac",true)) }
 var aadhaarReady by remember { mutableStateOf(prefs.getBoolean("aadhaar",false)) }
 var walletReady by remember { mutableStateOf(prefs.getBoolean("wallet",false)) }
 var saved by remember { mutableStateOf(false) }

 val base = listOf(from,to,date,train,passenger).count { it.isNotBlank() }
 val readiness = ((base + if(aadhaarReady) 1 else 0 + if(walletReady) 1 else 0) * 100 / 7).coerceIn(0,100)

 fun save() {
  prefs.edit().putString("from",from).putString("to",to).putString("date",date)
   .putString("train",train).putString("passenger",passenger).putString("class",travelClass)
   .putBoolean("ac",ac).putBoolean("aadhaar",aadhaarReady).putBoolean("wallet",walletReady).apply()
  saved = true
 }

 Scaffold(topBar={ TopAppBar(title={ Text("Tatkal Fast Assistant") }) }) { p ->
  Column(
   Modifier.padding(p).padding(horizontal=18.dp).verticalScroll(rememberScrollState()).fillMaxSize(),
   verticalArrangement=Arrangement.spacedBy(12.dp)
  ) {
   Spacer(Modifier.height(4.dp))
   Text("Quick Book",style=MaterialTheme.typography.headlineMedium)
   Text("Prepare your details before Tatkal opens, then continue on official IRCTC.")

   Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
     Text("Readiness",style=MaterialTheme.typography.titleMedium); Text("$readiness%")
    }
    LinearProgressIndicator(progress={readiness/100f},modifier=Modifier.fillMaxWidth())
    Text(if(readiness==100) "Ready for handoff" else "Complete the missing items below")
   }}

   OutlinedTextField(from,{from=it;saved=false},label={Text("From station / code")},modifier=Modifier.fillMaxWidth(),singleLine=true)
   OutlinedTextField(to,{to=it;saved=false},label={Text("To station / code")},modifier=Modifier.fillMaxWidth(),singleLine=true)
   OutlinedTextField(date,{date=it;saved=false},label={Text("Journey date (DD-MM-YYYY)")},modifier=Modifier.fillMaxWidth(),singleLine=true)
   OutlinedTextField(train,{train=it;saved=false},label={Text("Train number / name")},modifier=Modifier.fillMaxWidth(),singleLine=true)
   OutlinedTextField(passenger,{passenger=it;saved=false},label={Text("Passenger preset / names")},modifier=Modifier.fillMaxWidth())

   Text("Class",style=MaterialTheme.typography.titleSmall)
   Row(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
    listOf("1A","2A","3A","SL").forEach { c -> FilterChip(travelClass==c,{travelClass=c;saved=false},label={Text(c)}) }
   }
   Text("Tatkal opening reference",style=MaterialTheme.typography.titleSmall)
   Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
    FilterChip(ac,{ac=true;saved=false},label={Text("AC • 10:00")})
    FilterChip(!ac,{ac=false;saved=false},label={Text("Non-AC • 11:00")})
   }

   Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(4.dp)) {
    Text("Payment",style=MaterialTheme.typography.titleMedium)
    Text("Preferred: IRCTC eWallet")
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
     Text("eWallet balance checked"); Switch(walletReady,{walletReady=it;saved=false})
    }
   }}
   Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(4.dp)) {
    Text("Pre-booking checklist",style=MaterialTheme.typography.titleMedium)
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
     Text("IRCTC / Aadhaar ready"); Switch(aadhaarReady,{aadhaarReady=it;saved=false})
    }
    Text("Also check network, device time and payment access before opening.")
   }}

   OutlinedButton({save()},Modifier.fillMaxWidth()) { Text(if(saved) "SAVED ✓" else "SAVE PRESET") }
   Button({save();openIrctc()},Modifier.fillMaxWidth()) { Text("QUICK BOOK — OPEN IRCTC") }
   Text("CAPTCHA, OTP, payment authorization and final booking remain under your control on IRCTC.",style=MaterialTheme.typography.bodySmall)
   Spacer(Modifier.height(18.dp))
  }
 }
}