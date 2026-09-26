package com.tatkal.fastassistant

import android.content.Context
import android.content.Intent
import android.content.ClipData
import android.content.ClipboardManager
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.isSystemInDarkTheme
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
    Home(this)
   }
  }
 }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Home(context: Context) {
 val prefs = remember { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE) }
 var from by remember { mutableStateOf(prefs.getString("from","") ?: "") }
 var to by remember { mutableStateOf(prefs.getString("to","") ?: "") }
 var date by remember { mutableStateOf(prefs.getString("date","") ?: "") }
 var train by remember { mutableStateOf(prefs.getString("train","") ?: "") }
 var passenger by remember { mutableStateOf(prefs.getString("passenger","") ?: "") }
 var travelClass by remember { mutableStateOf(prefs.getString("class","3A") ?: "3A") }
 var quota by remember { mutableStateOf(prefs.getString("quota","TATKAL") ?: "TATKAL") }
 var berth by remember { mutableStateOf(prefs.getString("berth","NO PREFERENCE") ?: "NO PREFERENCE") }
 var meal by remember { mutableStateOf(prefs.getString("meal","NO PREFERENCE") ?: "NO PREFERENCE") }
 var ac by remember { mutableStateOf(prefs.getBoolean("ac",true)) }
 var aadhaarReady by remember { mutableStateOf(prefs.getBoolean("aadhaar",false)) }
 var walletReady by remember { mutableStateOf(prefs.getBoolean("wallet",false)) }
 var saved by remember { mutableStateOf(false) }
 var copied by remember { mutableStateOf(false) }\n var showWeb by remember { mutableStateOf(false) }

 val base = listOf(from,to,date,train,passenger).count { it.isNotBlank() }
 val readinessItems = base + (if (aadhaarReady) 1 else 0) + (if (walletReady) 1 else 0)
 val readiness = (readinessItems * 100 / 7).coerceIn(0,100)
 val missing = buildList {
  if (from.isBlank()) add("From")
  if (to.isBlank()) add("To")
  if (date.isBlank()) add("Date")
  if (train.isBlank()) add("Train")
  if (passenger.isBlank()) add("Passenger")
  if (!aadhaarReady) add("IRCTC/Aadhaar")
  if (!walletReady) add("eWallet")
 }

 fun copySummary() {
  val summary = "FROM: $from\nTO: $to\nDATE: $date\nTRAIN: $train\nCLASS: $travelClass\nQUOTA: $quota\nBERTH: $berth\nMEAL: $meal\nPASSENGER: $passenger\nPAYMENT: IRCTC eWallet"
  val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
  clipboard.setPrimaryClip(ClipData.newPlainText("Tatkal booking details", summary))
  copied = true
 }

 fun save() {
  prefs.edit().putString("from",from).putString("to",to).putString("date",date)
   .putString("train",train).putString("passenger",passenger).putString("class",travelClass).putString("quota",quota).putString("berth",berth).putString("meal",meal)
   .putBoolean("ac",ac).putBoolean("aadhaar",aadhaarReady).putBoolean("wallet",walletReady).apply()
  saved = true
 }

 if (showWeb) {\n  IrctcWebView(context = context, onClose = { showWeb = false })\n  return\n }\n\n Scaffold(topBar={ TopAppBar(title={ Text("Tatkal Fast Assistant") }) }) { p ->
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
    Text(if(readiness==100) "Ready for handoff" else "Missing: ${missing.joinToString()}")
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
   Text("Quota",style=MaterialTheme.typography.titleSmall)
   Row(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
    listOf("TATKAL","PREMIUM TATKAL").forEach { q -> FilterChip(quota==q,{quota=q;saved=false},label={Text(q)}) }
   }
   Text("Berth preference",style=MaterialTheme.typography.titleSmall)
   Row(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
    listOf("NO PREFERENCE","LOWER","SIDE LOWER").forEach { b -> FilterChip(berth==b,{berth=b;saved=false},label={Text(b)}) }
   }
   Text("Meal preference",style=MaterialTheme.typography.titleSmall)
   Row(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
    listOf("NO PREFERENCE","VEG","NON VEG").forEach { m -> FilterChip(meal==m,{meal=m;saved=false},label={Text(m)}) }
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
   OutlinedButton({save();copySummary()},Modifier.fillMaxWidth()) { Text(if(copied) "DETAILS COPIED ✓" else "COPY BOOKING DETAILS") }
   Button({save();copySummary();showWeb=true},Modifier.fillMaxWidth()) { Text("OPEN IRCTC IN APP & AUTOFILL") }
   Text("IRCTC opens inside this app. The assistant attempts to fill the journey form from your saved preset; review every field before continuing.",style=MaterialTheme.typography.bodySmall)
   Text("CAPTCHA, OTP, payment authorization and final booking remain under your control on IRCTC.",style=MaterialTheme.typography.bodySmall)
   Spacer(Modifier.height(18.dp))
  }
 }
}

@Composable
fun IrctcWebView(context: Context, onClose: () -> Unit) {
 val prefs = remember { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE) }
 val from = prefs.getString("from", "") ?: ""
 val to = prefs.getString("to", "") ?: ""
 val date = prefs.getString("date", "") ?: ""
 val quota = prefs.getString("quota", "TATKAL") ?: "TATKAL"
 var webView by remember { mutableStateOf<WebView?>(null) }
 val js = remember(from, to, date, quota) { buildAutofillJs(from, to, date, quota) }

 Scaffold(topBar = { TopAppBar(
  title = { Text("IRCTC • In-app") },
  navigationIcon = { TextButton(onClick = onClose) { Text("BACK") } },
  actions = { TextButton(onClick = { webView?.evaluateJavascript(js, null) }) { Text("AUTOFILL") } }
 ) }) { padding ->
  AndroidView(
   modifier = Modifier.padding(padding).fillMaxSize(),
   factory = { ctx ->
    WebView(ctx).apply {
     webView = this
     settings.javaScriptEnabled = true
     settings.domStorageEnabled = true
     settings.databaseEnabled = true
     settings.userAgentString = settings.userAgentString + " TatkalFastAssistant/1.1"
     CookieManager.getInstance().setAcceptCookie(true)
     CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
     webChromeClient = WebChromeClient()
     webViewClient = object : WebViewClient() {
      override fun onPageFinished(view: WebView, url: String) {
       super.onPageFinished(view, url)
       if (url.contains("irctc.co.in")) {
        view.postDelayed({ view.evaluateJavascript(js, null) }, 900)
        view.postDelayed({ view.evaluateJavascript(js, null) }, 2200)
       }
      }
     }
     loadUrl("https://www.irctc.co.in/nget/train-search")
    }
   }
  )
 }
}

private fun jsString(value: String): String =
 value.replace("\\", "\\\\").replace("'", "\\'").replace("\n", " ")

private fun buildAutofillJs(from: String, to: String, date: String, quota: String): String {
 val f = jsString(from.uppercase())
 val t = jsString(to.uppercase())
 val d = jsString(date.replace("-", "/"))
 val q = jsString(quota)
 return """
 (function(){
   const fire=(el,type)=>el.dispatchEvent(new Event(type,{bubbles:true}));
   const setNative=(el,val)=>{
     if(!el)return false;
     const p=Object.getPrototypeOf(el), desc=Object.getOwnPropertyDescriptor(p,'value');
     if(desc&&desc.set) desc.set.call(el,val); else el.value=val;
     fire(el,'input'); fire(el,'change'); return true;
   };
   const visible=el=>!!(el&&el.offsetParent!==null);
   const inputs=[...document.querySelectorAll('input')].filter(visible);
   const byHint=(words)=>inputs.find(el=>{
     const s=((el.placeholder||'')+' '+(el.getAttribute('aria-label')||'')+' '+(el.id||'')+' '+(el.name||'')).toLowerCase();
     return words.some(w=>s.includes(w));
   });
   const chooseStation=(el,code)=>{
     if(!el||!code)return;
     el.focus(); setNative(el,code);
     el.dispatchEvent(new KeyboardEvent('keyup',{key:code.slice(-1),bubbles:true}));
     setTimeout(()=>{
       const opts=[...document.querySelectorAll('[role=option],li.ui-autocomplete-list-item,li.p-autocomplete-item')].filter(visible);
       const hit=opts.find(x=>(x.innerText||'').toUpperCase().includes(code))||opts[0];
       if(hit) hit.click();
     },650);
   };
   const fromEl=byHint(['from','origin'])||inputs[0];
   const toEl=byHint(['to station','destination'])||inputs.find(x=>x!==fromEl&&((x.placeholder||'').toLowerCase().includes('to')));
   chooseStation(fromEl,'$f');
   setTimeout(()=>chooseStation(toEl,'$t'),900);
   setTimeout(()=>{
     const dateEl=byHint(['dd/mm/yyyy','journey date','date']);
     if(dateEl){ dateEl.removeAttribute('readonly'); setNative(dateEl,'$d'); fire(dateEl,'blur'); }
     const texts=[...document.querySelectorAll('span,div,label')].filter(visible);
     const quotaNode=texts.find(x=>(x.innerText||'').trim().toUpperCase()==='$q');
     if(quotaNode) quotaNode.click();
   },1700);
   return 'autofill-attempted';
 })();
 """.trimIndent()
}
