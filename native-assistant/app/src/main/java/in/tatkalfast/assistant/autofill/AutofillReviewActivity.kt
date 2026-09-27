package `in`.tatkalfast.assistant.autofill

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.service.autofill.Dataset
import android.view.WindowManager
import android.view.autofill.AutofillManager
import android.view.autofill.AutofillValue
import android.widget.RemoteViews
import androidx.activity.compose.setContent
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import `in`.tatkalfast.assistant.data.*
import `in`.tatkalfast.assistant.ui.*
import `in`.tatkalfast.domain.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint class AutofillReviewActivity:FragmentActivity() {
 @Inject lateinit var store:VaultStore
 @Inject lateinit var preferences:PreferenceStore
 private var unlocked by mutableStateOf(false)
 private var data by mutableStateOf<Vault?>(null)
 private var tamil by mutableStateOf(false)
 private var theme by mutableStateOf("SYSTEM")
 private var error by mutableStateOf<String?>(null)
 private var target:FillTarget?=null
 private var prompting=false
 override fun onCreate(savedInstanceState:Bundle?) {
  super.onCreate(savedInstanceState)
  setResult(Activity.RESULT_CANCELED)
  window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
  target=PendingFills.take(intent.getStringExtra("request_token"))
  if(target==null) {finish();return}
  lifecycleScope.launch {val prefs=preferences.flow.first();tamil=prefs.tamil;theme=prefs.theme}
  setContent {
   val words=Words(tamil)
   AppTheme(theme) { Surface(Modifier.fillMaxSize()) {
    Column(Modifier.safeDrawingPadding().padding(24.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(16.dp)) {
     Text(words.t("autofill_review"),style=MaterialTheme.typography.headlineSmall)
     Text("https://${target?.host} • Google Chrome")
     if(!unlocked) Button(onClick={authenticate(words)}) {Text(words.t("unlock"))}
     else {
      val a=data?.attempts?.lastOrNull {AutofillPolicy.canFill(it)}
      if(a==null) Text(words.t("autofill_no_attempt")) else {
       var person by remember {mutableStateOf(a.passengers.firstOrNull()?.id)}
       if(target?.fields?.any {it.field in setOf(PreparedField.PASSENGER_NAME,PreparedField.PASSENGER_AGE)}==true) {
        a.passengers.forEach {p->Check("${p.name} • ${p.age}",person==p.id) {if(it) person=p.id}}
       }
       val values=target!!.fields.map {it to AutofillPolicy.value(it.field,a,person)}
       values.forEach {(field,value)->
        Text(words.t(field.field.name),style=MaterialTheme.typography.titleMedium)
        Text(value.orEmpty(),style=MaterialTheme.typography.headlineSmall)
       }
       Text(words.t("autofill_review_note"))
       Button(onClick={complete(a.id,person,words)},enabled=values.all {it.second!=null}) {Text(words.t("autofill_fill"))}
      }
     }
     error?.let {Text(words.t(it),color=MaterialTheme.colorScheme.error)}
     TextButton(onClick={finish()}) {Text(words.t("cancel"))}
    }
   } }
  }
 }
 private fun authenticate(words:Words) {
  if(prompting) return
  val allowed=BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL
  if(BiometricManager.from(this).canAuthenticate(allowed)!=BiometricManager.BIOMETRIC_SUCCESS) {error="lock_setup";return}
  prompting=true
  BiometricPrompt(this,ContextCompat.getMainExecutor(this),object:BiometricPrompt.AuthenticationCallback() {
   override fun onAuthenticationSucceeded(result:BiometricPrompt.AuthenticationResult) {
    prompting=false
    lifecycleScope.launch {
     try {store.load();data=store.state.value;unlocked=true;error=null} catch(_:Exception) {error="storage_error"}
    }
   }
   override fun onAuthenticationError(errorCode:Int,errString:CharSequence) {prompting=false}
  }).authenticate(BiometricPrompt.PromptInfo.Builder().setTitle(words.t("unlock")).setAllowedAuthenticators(allowed).build())
 }
 private fun complete(attemptId:String,person:String?,words:Words) {
  val t=target?:return
  if(!unlocked || t.expires<SystemClock.elapsedRealtime() || !BrowserTrust.verified(this,t.browser)) {finish();return}
  val a=store.state.value?.attempts?.find {it.id==attemptId} ?: run {finish();return}
  val values=t.fields.map {it to (AutofillPolicy.value(it.field,a,person)?:run {finish();return})}
  val presentation=RemoteViews(packageName,android.R.layout.simple_list_item_1).apply {setTextViewText(android.R.id.text1,words.t("autofill_fill"))}
  val builder=Dataset.Builder(presentation)
  values.forEach {(field,value)->builder.setValue(field.id,AutofillValue.forText(value))}
  val dataset=builder.build()
  setResult(Activity.RESULT_OK,Intent().putExtra(AutofillManager.EXTRA_AUTHENTICATION_RESULT,dataset))
  finish()
 }
 override fun onStop() {super.onStop();unlocked=false;data=null}
}
