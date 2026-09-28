package `in`.tatkalfast.assistant
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import `in`.tatkalfast.assistant.data.*
import `in`.tatkalfast.domain.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
@RunWith(AndroidJUnit4::class) class BackupStoreTest {
 @Test fun portableRestoreReencryptsAndRetainsPaymentLock()=runBlocking {
  val db=Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(),VaultDatabase::class.java).build()
  try {
   val passenger=Passenger(name="Backup Passenger")
   val journey=Journey(passengerIds=listOf(passenger.id))
   val pending=Attempt(journey=journey,passengers=listOf(passenger),payment=Payment.UPI,handedOff=true,stage=Stage.PAYMENT_PENDING)
   val v=Vault(passengers=listOf(passenger),journeys=listOf(journey),attempts=listOf(pending))
   val secret="device-test backup passphrase".toCharArray()
   val decoded=PortableBackup.decrypt(PortableBackup.encrypt(v,secret),secret)
   val store=VaultStore(db,VaultCipher());store.load();store.update {PortableBackup.merge(it,decoded)}
   val reopened=VaultStore(db,VaultCipher());reopened.load()
   assertEquals(Stage.UNKNOWN,reopened.state.value!!.attempts.single().stage)
   assertFalse(reopened.state.value!!.attempts.single().resolved)
   assertFalse(db.vaultDao().get()!!.ciphertext.decodeToString().contains(passenger.name))
  } finally {db.close()}
 }
 @Test fun conflictingRestoreDoesNotCommitPartialProfiles()=runBlocking {
  val db=Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(),VaultDatabase::class.java).build()
  try {
   val p=Passenger(name="Local Passenger")
   val a=Attempt(journey=Journey(),passengers=listOf(p),payment=Payment.UPI,handedOff=true)
   val store=VaultStore(db,VaultCipher());store.load();store.update {Vault(passengers=listOf(p),attempts=listOf(a))}
   val before=store.state.value
   val incoming=Vault(passengers=listOf(p.copy(id="new")),attempts=listOf(a.copy(id="other")))
   try {
    store.update {PortableBackup.merge(it,incoming)}
    fail("expected conflict")
   } catch(_:IllegalArgumentException) {}
   assertEquals(before,store.state.value)
  } finally {db.close()}
 }
}
