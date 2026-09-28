package `in`.tatkalfast.assistant

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import `in`.tatkalfast.assistant.ui.*

@AndroidEntryPoint class MainActivity : FragmentActivity() {
 private val vm: BookingViewModel by viewModels()
 private var unlocked by mutableStateOf(false)
 private var authMessage by mutableStateOf(false)
 private var authenticating=false
 override fun onCreate(savedInstanceState: Bundle?) {
  super.onCreate(savedInstanceState)
  enableEdgeToEdge()
  window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
  setContent {
   val prefs by vm.preferences.collectAsStateWithLifecycle()
   val ready by vm.ready.collectAsStateWithLifecycle()
   val error by vm.error.collectAsStateWithLifecycle()
   val words=Words(prefs?.tamil?:false)
   AppTheme(prefs?.theme?:"SYSTEM") {
    Surface(Modifier.fillMaxSize()) {
     if(prefs==null) Box(Modifier.padding(32.dp)) { Text(words.t("loading")) }
     else if(prefs?.lock==true && !unlocked) {
      Column(Modifier.safeDrawingPadding().padding(28.dp),verticalArrangement=Arrangement.spacedBy(20.dp)) {
       Text(words.t("locked"),style=MaterialTheme.typography.headlineMedium)
       Button(onClick={ authenticate(words) }) { Text(words.t("unlock")) }
       if(authMessage) { Text(words.t("lock_setup")); OutlinedButton(onClick={ startActivity(Intent(Settings.ACTION_SECURITY_SETTINGS)) }) { Text(words.t("open_settings")) } }
      }
     } else {
      LaunchedEffect(Unit) { vm.unlock() }
      if(ready) TatkalScreen(vm,words) else Column(Modifier.padding(32.dp)) {
       Text(words.t("loading"))
       if(error!=null) { Text(words.t(error!!)); Button(onClick={vm.clearError();vm.unlock()}) { Text(words.t("unlock")) } }
      }
     }
    }
   }
  }
 }
 private fun authenticate(words: Words) {
  if(authenticating) return
  val allowed=BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL
  if(BiometricManager.from(this).canAuthenticate(allowed)!=BiometricManager.BIOMETRIC_SUCCESS) { authMessage=true;return }
  authenticating=true
  BiometricPrompt(this,ContextCompat.getMainExecutor(this),object: BiometricPrompt.AuthenticationCallback() {
   override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) { authenticating=false;authMessage=false;unlocked=true }
   override fun onAuthenticationError(errorCode: Int,errString: CharSequence) { authenticating=false }
  }).authenticate(BiometricPrompt.PromptInfo.Builder().setTitle(words.t("unlock")).setAllowedAuthenticators(allowed).build())
 }
 override fun onStop() { super.onStop(); unlocked=false }
}
