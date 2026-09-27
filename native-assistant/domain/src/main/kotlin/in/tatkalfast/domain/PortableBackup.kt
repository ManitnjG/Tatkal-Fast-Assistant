package `in`.tatkalfast.domain

import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Versioned, bounded, authenticated portable backup. No password or key is persisted. */
object PortableBackup {
 const val MAX_BYTES=8*1024*1024
 private val header=byteArrayOf(84,70,65,1)
 private val json=Json {ignoreUnknownKeys=true;encodeDefaults=true}
 private fun derive(password:CharArray,salt:ByteArray):ByteArray {
  require(password.size in 12..128) {"backup_password"}
  val spec=PBEKeySpec(password,salt,210000,256)
  return try {SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded} finally {spec.clearPassword()}
 }
 fun encrypt(v:Vault,password:CharArray):ByteArray {
  validate(v)
  val plain=json.encodeToString(v).toByteArray(Charsets.UTF_8)
  require(plain.size<=MAX_BYTES-48) {"backup_invalid"}
  val random=SecureRandom();val salt=ByteArray(16).also(random::nextBytes);val iv=ByteArray(12).also(random::nextBytes)
  val key=derive(password,salt)
  return try {
   val prefix=header+salt+iv
   val cipher=Cipher.getInstance("AES/GCM/NoPadding")
   cipher.init(Cipher.ENCRYPT_MODE,SecretKeySpec(key,"AES"),GCMParameterSpec(128,iv));cipher.updateAAD(prefix)
   prefix+cipher.doFinal(plain)
  } finally {key.fill(0);plain.fill(0)}
 }
 fun decrypt(bytes:ByteArray,password:CharArray):Vault {
  require(bytes.size in 48..MAX_BYTES && bytes.copyOfRange(0,4).contentEquals(header)) {"backup_invalid"}
  val key=derive(password,bytes.copyOfRange(4,20))
  val plain=try {
   val cipher=Cipher.getInstance("AES/GCM/NoPadding")
   cipher.init(Cipher.DECRYPT_MODE,SecretKeySpec(key,"AES"),GCMParameterSpec(128,bytes.copyOfRange(20,32)))
   cipher.updateAAD(bytes.copyOfRange(0,32));cipher.doFinal(bytes.copyOfRange(32,bytes.size))
  } finally {key.fill(0)}
  return try {json.decodeFromString<Vault>(plain.decodeToString()).also(::validate)} finally {plain.fill(0)}
 }
 fun validate(v:Vault) {
  require(v.schema==1 && v.passengers.size<=1000 && v.journeys.size<=1000 && v.attempts.size<=5000) {"backup_invalid"}
  fun ids(values:List<String>)=values.all {it.isNotBlank()&&it.length<=128} && values.distinct().size==values.size
  require(ids(v.passengers.map {it.id}) && ids(v.journeys.map {it.id}) && ids(v.attempts.map {it.id})) {"backup_invalid"}
  require(v.passengers.all {Rules.passenger(it).isEmpty()}) {"backup_invalid"}
  fun bounded(j:Journey)=j.label.length<=50 && j.from.length<=5 && j.to.length<=5 && j.boarding.length<=5 && j.date.length<=10 && j.originDate.length<=10 && j.train.length<=5 && j.mobile.length<=10 && j.passengerIds.size<=6 && j.passengerIds.all {it.length<=128}
  require(v.journeys.all(::bounded)) {"backup_invalid"}
  require(v.attempts.all {a->a.allocation.length<=250 && a.passengers.size<=6 && a.passengers.all {Rules.passenger(it).isEmpty()} && bounded(a.journey) && (!a.resolved || a.stage in setOf(Stage.BOOKED,Stage.FAILED)) && (a.stage!=Stage.BOOKED || a.pnr.matches(Regex("[0-9]{10}")))}) {"backup_invalid"}
 }
 /** Local profiles win ID collisions. Unresolved attempts can never be erased by a restore. */
 fun merge(local:Vault,incoming:Vault):Vault {
  validate(incoming)
  val attempts=(local.attempts+incoming.attempts).groupBy {it.id}.values.map {same->
   BookingEngine.recover(same.firstOrNull {!it.resolved}?:same.first())
  }
  require(attempts.count {!it.resolved}<=1) {"backup_active_conflict"}
  val merged=local.copy(
   passengers=(local.passengers+incoming.passengers).distinctBy {it.id},
   journeys=(local.journeys+incoming.journeys).distinctBy {it.id},attempts=attempts,
   selectedJourney=local.selectedJourney?:incoming.selectedJourney)
  validate(merged)
  return merged
 }
}
