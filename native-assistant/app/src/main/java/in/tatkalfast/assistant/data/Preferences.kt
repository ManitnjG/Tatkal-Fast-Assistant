package `in`.tatkalfast.assistant.data
import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton
private val Context.settings by preferencesDataStore("settings")
data class Preferences(val tamil: Boolean=false, val theme: String="SYSTEM", val lock: Boolean=true)
@Singleton class PreferenceStore @Inject constructor(@ApplicationContext private val context: Context) {
 private val language=booleanPreferencesKey("tamil")
 private val theme=stringPreferencesKey("theme")
 private val lock=booleanPreferencesKey("lock")
 val flow=context.settings.data.map { Preferences(it[language]?:false,it[theme]?:"SYSTEM",it[lock]?:true) }
 suspend fun save(p: Preferences) { context.settings.edit { it[language]=p.tamil; it[theme]=p.theme; it[lock]=p.lock } }
}
