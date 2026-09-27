package `in`.tatkalfast.assistant
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import `in`.tatkalfast.assistant.data.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
@RunWith(AndroidJUnit4::class) class VaultMigrationTest {
 @Test fun migratesV1WithoutLosingCiphertext()=runBlocking {
  val context=ApplicationProvider.getApplicationContext<android.content.Context>()
  context.deleteDatabase("migration-test")
  val file=context.getDatabasePath("migration-test");file.parentFile?.mkdirs()
  SQLiteDatabase.openOrCreateDatabase(file,null).use {db->
   db.execSQL("CREATE TABLE vault (id INTEGER NOT NULL PRIMARY KEY, ciphertext BLOB NOT NULL)")
   db.execSQL("INSERT INTO vault (id,ciphertext) VALUES (1, X'010203')")
   db.version=1
  }
  val db=Room.databaseBuilder(context,VaultDatabase::class.java,"migration-test").addMigrations(VaultDatabase.MIGRATION_1_2).build()
  try {val row=db.vaultDao().get()!!;assertArrayEquals(byteArrayOf(1,2,3),row.ciphertext);assertEquals(0L,row.updatedAt)} finally {db.close();context.deleteDatabase("migration-test")}
 }
 @Test fun authenticatedEncryptionRejectsTampering() {
  val cipher=VaultCipher();val secret="private passenger".toByteArray();val sealed=cipher.encrypt(secret)
  assertArrayEquals(secret,cipher.decrypt(sealed));sealed[sealed.lastIndex]=(sealed.last().toInt() xor 1).toByte()
  var failed=false;try {cipher.decrypt(sealed)} catch(_:Exception) {failed=true};assertTrue(failed)
 }
}
