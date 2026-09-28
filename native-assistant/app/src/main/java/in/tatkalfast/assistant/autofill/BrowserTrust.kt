package `in`.tatkalfast.assistant.autofill

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import java.security.MessageDigest

object BrowserTrust {
 const val CHROME="com.android.chrome"
 // Google signing certificate published at https://www.google.com/.well-known/assetlinks.json
 // Package name alone is insufficient. Unknown signers fail closed; never allow arbitrary apps.
 private const val GOOGLE="F0FD6C5B410F25CB25C3B53346C8972FAE30F8EE7411DF910480AD6B2D60DB83"
 @Suppress("DEPRECATION")
 fun verified(context:Context,packageName:String):Boolean {
  if(packageName!=CHROME) return false
  return runCatching {
   val pm=context.packageManager
   val signatures=if(Build.VERSION.SDK_INT>=28) {
    val info=pm.getPackageInfo(packageName,PackageManager.GET_SIGNING_CERTIFICATES).signingInfo ?: return false
    if(info.hasMultipleSigners()) return false else info.signingCertificateHistory
   } else pm.getPackageInfo(packageName,PackageManager.GET_SIGNATURES).signatures ?: return false
   signatures.any {sig->MessageDigest.getInstance("SHA-256").digest(sig.toByteArray()).joinToString("") {"%02X".format(it)}==GOOGLE}
  }.getOrDefault(false)
 }
}
