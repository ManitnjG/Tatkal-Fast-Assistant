package in.tatkal.fastassistant

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
 override fun onCreate(savedInstanceState: Bundle?) {
  super.onCreate(savedInstanceState)
  setContent { MaterialTheme { Home { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.irctc.co.in/"))) } } }
 }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Home(openIrctc: () -> Unit) {
 var from by remember { mutableStateOf("") }
 var to by remember { mutableStateOf("") }
 var train by remember { mutableStateOf("") }
 var passenger by remember { mutableStateOf("") }
 var ac by remember { mutableStateOf(true) }
 Scaffold(topBar={ TopAppBar(title={ Text("Tatkal Fast Assistant") }) }) { p ->
  Column(Modifier.padding(p).padding(20.dp).fillMaxSize(), verticalArrangement=Arrangement.spacedBy(14.dp)) {
   Text("Quick Book", style=MaterialTheme.typography.headlineMedium)
   Text("Prepare first. Complete CAPTCHA, OTP, payment and booking yourself on official IRCTC.")
   OutlinedTextField(from,{from=it},label={Text("From station")},modifier=Modifier.fillMaxWidth())
   OutlinedTextField(to,{to=it},label={Text("To station")},modifier=Modifier.fillMaxWidth())
   OutlinedTextField(train,{train=it},label={Text("Train number / name")},modifier=Modifier.fillMaxWidth())
   OutlinedTextField(passenger,{passenger=it},label={Text("Passenger preset name")},modifier=Modifier.fillMaxWidth())
   Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
    FilterChip(ac,{ac=true},label={Text("AC • 10:00")})
    FilterChip(!ac,{ac=false},label={Text("Non-AC • 11:00")})
   }
   Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) {
    Text("Preferred payment",style=MaterialTheme.typography.titleMedium)
    Text("IRCTC eWallet")
    Text("Check eWallet balance, network and IRCTC/Aadhaar readiness before opening.")
   }}
   Button(openIrctc,Modifier.fillMaxWidth()) { Text("OPEN OFFICIAL IRCTC") }
   Text("No CAPTCHA solving, queue/rate-limit bypass, OTP interception, or unattended purchasing.")
  }
 }
}