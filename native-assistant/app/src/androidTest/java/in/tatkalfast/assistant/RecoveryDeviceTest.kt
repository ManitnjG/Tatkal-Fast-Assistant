package `in`.tatkalfast.assistant
import android.widget.EditText
import android.os.SystemClock
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import `in`.tatkalfast.assistant.autofill.*
import `in`.tatkalfast.assistant.data.*
import `in`.tatkalfast.domain.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
@RunWith(AndroidJUnit4::class) class RecoveryDeviceTest {
 private val context get()=ApplicationProvider.getApplicationContext<android.content.Context>()
 @Test fun encryptedPendingStateSurvivesStoreRecreation()=runBlocking {
  val db=Room.inMemoryDatabaseBuilder(context,VaultDatabase::class.java).build()
  try {
   val cipher=VaultCipher();val first=VaultStore(db,cipher);first.load()
   val a=Attempt(journey=Journey(from="TJ",to="MS"),passengers=listOf(Passenger(name="Private Passenger")),payment=Payment.IRCTC_EWALLET,handedOff=true,stage=Stage.PAYMENT_PENDING)
   first.update {it.copy(attempts=listOf(a))}
   assertFalse(db.vaultDao().get()!!.ciphertext.decodeToString().contains("Private Passenger"))
   val reopened=VaultStore(db,cipher);reopened.load()
   assertEquals(Stage.UNKNOWN,reopened.state.value!!.attempts.single().stage)
   assertFalse(reopened.state.value!!.attempts.single().resolved)
  } finally {db.close()}
 }
 @Test fun targetTokensAreOneTimeAndExpire() {
  val id=EditText(context).autofillId
  val t=FillTarget(id,PreparedField.PASSENGER_NAME,"www.irctc.co.in","com.android.chrome",SystemClock.elapsedRealtime()+10000)
  val token=PendingFills.add(t);assertEquals(t,PendingFills.take(token));assertNull(PendingFills.take(token))
  assertNull(PendingFills.take(PendingFills.add(t.copy(expires=SystemClock.elapsedRealtime()-1))))
 }
 @Test fun arbitraryPackagesCannotImpersonateBrowser() {assertFalse(BrowserTrust.verified(context,context.packageName));assertFalse(BrowserTrust.verified(context,"evil.browser"))}
}
