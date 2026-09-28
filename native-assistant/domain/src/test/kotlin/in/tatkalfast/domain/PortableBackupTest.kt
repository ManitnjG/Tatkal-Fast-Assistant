package `in`.tatkalfast.domain
import org.junit.Assert.*
import org.junit.Test
class PortableBackupTest {
 private val password="long test passphrase".toCharArray()
 private val p=Passenger(name="Example Passenger")
 private val j=Journey(passengerIds=listOf(p.id),from="TJ",to="MS",boarding="TJ",train="16866",date="2026-09-28",originDate="2026-09-28",mobile="9000000000")
 private val a=Attempt(journey=j,passengers=listOf(p),payment=Payment.UPI,handedOff=true,stage=Stage.PAYMENT_PENDING)
 private val v=Vault(passengers=listOf(p),journeys=listOf(j),attempts=listOf(a))
 @Test fun backupRoundTrip() {assertEquals(v,PortableBackup.decrypt(PortableBackup.encrypt(v,password),password))}
 @Test fun randomizedEncryption() {assertFalse(PortableBackup.encrypt(v,password).contentEquals(PortableBackup.encrypt(v,password)))}
 @Test fun encryptedBytesHideNames() {assertFalse(PortableBackup.encrypt(v,password).decodeToString().contains(p.name))}
 @Test fun wrongPasswordFails() {val b=PortableBackup.encrypt(v,password);assertThrows(Exception::class.java) {PortableBackup.decrypt(b,"a wrong long password".toCharArray())}}
 @Test fun tamperFailsAuthentication() {val b=PortableBackup.encrypt(v,password);b[b.lastIndex]=(b.last().toInt() xor 1).toByte();assertThrows(Exception::class.java) {PortableBackup.decrypt(b,password)}}
 @Test fun unsupportedVersionRejected() {val b=PortableBackup.encrypt(v,password);b[3]=9;assertThrows(IllegalArgumentException::class.java) {PortableBackup.decrypt(b,password)}}
 @Test fun oversizedFileRejectedBeforeKdf() {assertThrows(IllegalArgumentException::class.java) {PortableBackup.decrypt(ByteArray(PortableBackup.MAX_BYTES+1),password)}}
 @Test fun weakPassphraseRejected() {assertThrows(IllegalArgumentException::class.java) {PortableBackup.encrypt(v,"short".toCharArray())}}
 @Test fun restoreRecoversUnknownAndKeepsLock() {val merged=PortableBackup.merge(Vault(),v);assertEquals(Stage.UNKNOWN,merged.attempts.single().stage);assertFalse(merged.attempts.single().resolved)}
 @Test fun incomingResolvedCannotEraseLocalPending() {val incoming=v.copy(attempts=listOf(a.copy(stage=Stage.FAILED,resolved=true)));assertFalse(PortableBackup.merge(v,incoming).attempts.single().resolved)}
 @Test fun differentActiveAttemptsRejectWholeMerge() {assertThrows(IllegalArgumentException::class.java) {PortableBackup.merge(v,v.copy(attempts=listOf(a.copy(id="different"))))}}
 @Test fun localProfileWinsIdCollision() {val changed=p.copy(name="Local Passenger");assertEquals(changed,PortableBackup.merge(v.copy(passengers=listOf(changed)),v).passengers.single())}
 @Test fun repeatedRestoreDoesNotDuplicate() {val first=PortableBackup.merge(Vault(),v);assertEquals(first,PortableBackup.merge(first,v))}
 @Test fun duplicateIdsRejected() {assertThrows(IllegalArgumentException::class.java) {PortableBackup.validate(v.copy(passengers=listOf(p,p)))}}
 @Test fun archivedProfileWithDeletedPassengerCanBeBackedUp() {val sparse=v.copy(passengers=emptyList());assertEquals(sparse,PortableBackup.decrypt(PortableBackup.encrypt(sparse,password),password))}
}
