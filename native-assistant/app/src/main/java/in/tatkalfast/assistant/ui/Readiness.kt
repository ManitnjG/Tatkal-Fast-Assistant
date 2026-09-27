package `in`.tatkalfast.assistant.ui

import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import android.provider.Settings
import android.view.autofill.AutofillManager
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import `in`.tatkalfast.assistant.autofill.BrowserTrust

@Composable fun ReadinessPanel(words:Words) {
 val context=LocalContext.current
 val lifecycle=LocalLifecycleOwner.current.lifecycle
 var refresh by remember {mutableIntStateOf(0)}
 DisposableEffect(lifecycle) {
  val observer=LifecycleEventObserver {_,event->if(event==Lifecycle.Event.ON_RESUME) refresh++}
  lifecycle.addObserver(observer);onDispose {lifecycle.removeObserver(observer)}
 }
 val states=remember(refresh) {listOf(
  "ready_android" to (Build.VERSION.SDK_INT>=28),
  "ready_chrome" to BrowserTrust.verified(context,BrowserTrust.CHROME),
  "ready_provider" to (context.getSystemService(AutofillManager::class.java)?.hasEnabledAutofillServices()==true),
  "ready_lock" to context.getSystemService(KeyguardManager::class.java).isDeviceSecure,
  "ready_clock" to (Settings.Global.getInt(context.contentResolver,Settings.Global.AUTO_TIME,0)==1),
  "ready_notifications" to NotificationManagerCompat.from(context).areNotificationsEnabled()
 )}
 Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
  Text(words.t("readiness"),style=MaterialTheme.typography.titleMedium)
  states.forEach {(key,ok)->Text("${words.t(if(ok) "ready_yes" else "ready_action")}: ${words.t(key)}")}
  Text(words.t("ready_note"),style=MaterialTheme.typography.bodySmall)
 }
}
