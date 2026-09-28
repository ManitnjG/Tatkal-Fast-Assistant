package `in`.tatkalfast.assistant.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import `in`.tatkalfast.domain.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

@Entity(tableName="vault") data class VaultRow(@PrimaryKey val id: Int = 1, val ciphertext: ByteArray, @ColumnInfo(defaultValue="0") val updatedAt: Long)
@Dao interface VaultDao {
 @Query("SELECT * FROM vault WHERE id=1") suspend fun get(): VaultRow?
 @Insert(onConflict=OnConflictStrategy.REPLACE) suspend fun put(row: VaultRow)
}
@Database(entities=[VaultRow::class], version=2, exportSchema=true)
abstract class VaultDatabase : RoomDatabase() {
 abstract fun vaultDao(): VaultDao
 companion object {
  val MIGRATION_1_2 = object : Migration(1,2) {
   override fun migrate(db: SupportSQLiteDatabase) { db.execSQL("ALTER TABLE vault ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0") }
  }
 }
}
@Singleton class VaultCipher @Inject constructor() {
 private fun key(): SecretKey {
  val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
  return (store.getKey("tatkal_vault_v1",null) as? SecretKey) ?: KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore").run {
   init(KeyGenParameterSpec.Builder("tatkal_vault_v1",KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
    .setKeySize(256).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).setRandomizedEncryptionRequired(true).build())
   generateKey()
  }
 }
 fun encrypt(bytes: ByteArray): ByteArray = Cipher.getInstance("AES/GCM/NoPadding").run {
  init(Cipher.ENCRYPT_MODE,key()); updateAAD("tatkal-vault:1".toByteArray()); byteArrayOf(1) + iv + doFinal(bytes)
 }
 fun decrypt(bytes: ByteArray): ByteArray {
  require(bytes.size >= 29 && bytes[0] == 1.toByte())
  return Cipher.getInstance("AES/GCM/NoPadding").run {
   init(Cipher.DECRYPT_MODE,key(),GCMParameterSpec(128,bytes.copyOfRange(1,13)))
   updateAAD("tatkal-vault:1".toByteArray()); doFinal(bytes.copyOfRange(13,bytes.size))
  }
 }
}
@Singleton class VaultStore @Inject constructor(private val db: VaultDatabase, private val cipher: VaultCipher) {
 private val mutex=Mutex()
 private val json=Json { ignoreUnknownKeys=true; encodeDefaults=true }
 private val _state=MutableStateFlow<Vault?>(null)
 val state=_state.asStateFlow()
 suspend fun load() = withContext(Dispatchers.IO) { mutex.withLock {
  if (_state.value == null) {
   val row=db.vaultDao().get()
   val data=if(row==null) Vault() else json.decodeFromString<Vault>(cipher.decrypt(row.ciphertext).decodeToString())
   require(data.schema==1) { "unsupported_schema" }
   val recovered=data.copy(attempts=data.attempts.map(BookingEngine::recover))
   persist(recovered)
  }
 } }
 private suspend fun persist(v: Vault) {
  val plain=json.encodeToString(v).toByteArray()
  try { db.withTransaction { db.vaultDao().put(VaultRow(ciphertext=cipher.encrypt(plain),updatedAt=System.currentTimeMillis())) } }
  finally { plain.fill(0) }
  _state.value=v
 }
 suspend fun update(block: (Vault)->Vault) = withContext(Dispatchers.IO) { mutex.withLock {
  persist(block(requireNotNull(_state.value)))
 } }
}
@Module @InstallIn(SingletonComponent::class) object DataModule {
 @Provides @Singleton fun database(@ApplicationContext context: Context): VaultDatabase = Room.databaseBuilder(context,VaultDatabase::class.java,"tatkal.db")
  .addMigrations(VaultDatabase.MIGRATION_1_2).build()
}
