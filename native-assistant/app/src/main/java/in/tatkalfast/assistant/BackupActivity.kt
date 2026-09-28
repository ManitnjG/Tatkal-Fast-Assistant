package `in`.tatkalfast.assistant

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import `in`.tatkalfast.assistant.data.*
import `in`.tatkalfast.assistant.ui.*
import `in`.tatkalfast.domain.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import java.io.ByteArrayOutputStream
import javax.inject.Inject

@AndroidEntryPoint class BackupActivity:FragmentActivity() {
 @Inject lateinit var store:VaultStore
 @Inject lateinit var preferences:PreferenceStore
 private var unlocked by mutableStateOf(false)
 private var busy by mutableStateOf(false)
 private var password by mutableStateOf("")
 private var repeat by mutableStateOf("")
 private var tamil by mutableStateOf(false)
 private var theme by mutableStateOf("SYSTEM")
 private var message by mutableStateOf<String?>(null)
 private var imported by mutableStateOf<Vault?>(null)
 private var incoming by mutableStateOf<ByteArray?>(null)
 private var outgoing:ByteArray?=null
 private var prompting=false
 private var generation=0
 private val saveFile=registerForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) {uri->
  val bytes=outgoing;outgoing=null
  if(uri!=null && bytes==null) message="backup_io"
  if(bytes!=null) lifecycleScope.launch {
   busy=true
   try {if(uri!=null) {withContext(Dispatchers.IO) {contentResolver.openOutputStream(uri,"wt")?.use {it.write(bytes);it.flush()}?:error("backup_io")};message="backup_saved"}}
   catch(_:Exception) {message="backup_io"} finally {bytes.fill(0);busy=false}
  }
 }
 private val openFile=registerForActivityResult(ActivityResultContracts.OpenDocument()) {uri->
  if(uri!=null) lifecycleScope.launch {
   busy=true
   try {
    val bytes=withContext(Dispatchers.IO) {
     contentResolver.openInputStream(uri)?.use {input->
      val out=ByteArrayOutputStream();val buffer=ByteArray(8192)
      while(true) {val count=input.read(buffer);if(count<0) break;require(out.size()+count<=PortableBackup.MAX_BYTES);out.write(buffer,0,count)}
      out.toByteArray()
     }?:error("backup_io")
    }
    incoming?.fill(0);incoming=bytes;message="backup_enter_password"
   } catch(_:Exception) {message="backup_io"} finally {busy=false}
  }
 }
 override fun onCreate(savedInstanceState:Bundle?) {
  super.onCreate(savedInstanceState);window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
  window.decorView.importantForAutofill=android.view.View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS
  lifecycleScope.launch {val p=preferences.flow.first();tamil=p.tamil;theme=p.theme}
  setContent {
   val w=Words(tamil)
   AppTheme(theme) {Surface(Modifier.fillMaxSize()) {
    Column(Modifier.safeDrawingPadding().padding(20.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)) {
     Text(w.t("backup_title"),style=MaterialTheme.typography.headlineSmall)
     if(!unlocked) Button(onClick={authenticate(w)},enabled=!busy) {Text(w.t("unlock"))}
     else {
      Text(w.t("backup_note"))
      if(imported==null) {
       OutlinedTextField(password,{password=it.take(128)},label={Text(w.t("backup_password_label"))},visualTransformation=PasswordVisualTransformation(),singleLine=true,modifier=Modifier.fillMaxWidth())
       if(incoming==null) OutlinedTextField(repeat,{repeat=it.take(128)},label={Text(w.t("backup_repeat"))},visualTransformation=PasswordVisualTransformation(),singleLine=true,modifier=Modifier.fillMaxWidth())
       if(incoming!=null) Button(onClick={decrypt()},enabled=!busy&&password.length>=12) {Text(w.t("backup_decrypt"))}
       else Button(onClick={export()},enabled=!busy&&password.length>=12&&password==repeat) {Text(w.t("backup_export"))}
       OutlinedButton(onClick={incoming?.fill(0);incoming=null;password="";repeat="";openFile.launch(arrayOf("application/octet-stream","*/*"))},enabled=!busy) {Text(w.t("backup_import"))}
      }
      imported?.let {v->
       Text("${w.t("passengers")}: ${v.passengers.size} • ${w.t("journeys_count")}: ${v.journeys.size} • ${w.t("bookings")}: ${v.attempts.size}")
       Text(w.t("backup_merge_note"))
       Button(onClick={restore()},enabled=!busy) {Text(w.t("backup_restore"))}
       TextButton(onClick={imported=null;incoming?.fill(0);incoming=null}) {Text(w.t("cancel"))}
      }
     }
     if(busy) LinearProgressIndicator(Modifier.fillMaxWidth())
     message?.let {Text(w.t(it))}
     TextButton(onClick={finish()}) {Text(w.t("done"))}
    }
   }}
  }
 }
 private fun authenticate(w:Words) {
  if(prompting) return
  val allowed=BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL
  if(BiometricManager.from(this).canAuthenticate(allowed)!=BiometricManager.BIOMETRIC_SUCCESS) {message="lock_setup";return}
  prompting=true
  BiometricPrompt(this,ContextCompat.getMainExecutor(this),object:BiometricPrompt.AuthenticationCallback() {
   override fun onAuthenticationSucceeded(result:BiometricPrompt.AuthenticationResult) {prompting=false;unlocked=true}
   override fun onAuthenticationError(errorCode:Int,errString:CharSequence) {prompting=false}
  }).authenticate(BiometricPrompt.PromptInfo.Builder().setTitle(w.t("unlock")).setAllowedAuthenticators(allowed).build())
 }
 private fun export() {
  if(!unlocked||busy||password!=repeat) return
  val secret=password.toCharArray();password="";repeat="";val epoch=generation
  busy=true
  lifecycleScope.launch {
   try {
    store.load()
    val bytes=withContext(Dispatchers.Default) {PortableBackup.encrypt(requireNotNull(store.state.value),secret)}
    if(unlocked&&epoch==generation) {outgoing=bytes;saveFile.launch("TatkalFastAssistant-backup.tfa")} else bytes.fill(0)
   } catch(_:Exception) {message="backup_io"} finally {secret.fill('\u0000');busy=false}
  }
 }
 private fun decrypt() {
  val bytes=incoming?:return
  if(!unlocked||busy) return
  val secret=password.toCharArray();password="";val epoch=generation;busy=true
  lifecycleScope.launch {
   try {val v=withContext(Dispatchers.Default) {PortableBackup.decrypt(bytes,secret)};if(unlocked&&epoch==generation) {imported=v;message=null}}
   catch(_:Exception) {message="backup_invalid"} finally {secret.fill('\u0000');busy=false}
  }
 }
 private fun restore() {
  val v=imported?:return
  if(!unlocked||busy) return
  busy=true
  lifecycleScope.launch {
   try {store.load();store.update {PortableBackup.merge(it,v)};imported=null;incoming?.fill(0);incoming=null;message="backup_restored"}
   catch(e:Exception) {message=if(e.message=="backup_active_conflict") "backup_active_conflict" else "backup_invalid"}
   finally {busy=false}
  }
 }
 override fun onStop() {super.onStop();generation++;unlocked=false;password="";repeat="";imported=null}
 override fun onDestroy() {outgoing?.fill(0);incoming?.fill(0);super.onDestroy()}
}
