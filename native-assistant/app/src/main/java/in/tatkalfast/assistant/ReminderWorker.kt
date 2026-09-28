package `in`.tatkalfast.assistant
import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.*
import java.time.Instant
import java.util.concurrent.TimeUnit
class ReminderWorker(context: Context, params: WorkerParameters): CoroutineWorker(context,params) {
 override suspend fun doWork(): Result {
  val context=applicationContext
  if(Build.VERSION.SDK_INT>=33 && ContextCompat.checkSelfPermission(context,Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED) return Result.success()
  if(System.currentTimeMillis()>inputData.getLong("expires",0L)) return Result.success()
  val manager=context.getSystemService(NotificationManager::class.java)
  manager.createNotificationChannel(NotificationChannel("booking",context.getString(R.string.channel_name),NotificationManager.IMPORTANCE_DEFAULT))
  val intent=PendingIntent.getActivity(context,0,Intent(context,MainActivity::class.java),PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
  val tamil=inputData.getBoolean("tamil",false)
  val notification=NotificationCompat.Builder(context,"booking").setSmallIcon(R.drawable.ic_train)
   .setContentTitle(if(tamil) "முன்பதிவு நேரம் நெருங்குகிறது" else context.getString(R.string.reminder_title))
   .setContentText(if(tamil) "பயண விவரங்களைச் சரிபார்க்க உதவியாளரைத் திறக்கவும்." else context.getString(R.string.reminder_body))
   .setContentIntent(intent).setAutoCancel(true).setVisibility(NotificationCompat.VISIBILITY_PRIVATE).build()
  try { NotificationManagerCompat.from(context).notify(id.hashCode(),notification) } catch(_: SecurityException) { return Result.success() }
  return Result.success()
 }
 companion object {
  fun schedule(context: Context,id: String,opening: Instant,tamil: Boolean) {
   val wait=(opening.minusSeconds(120).toEpochMilli()-System.currentTimeMillis()).coerceAtLeast(0)
   val work=OneTimeWorkRequestBuilder<ReminderWorker>().setInitialDelay(wait,TimeUnit.MILLISECONDS)
    .setInputData(workDataOf("expires" to opening.plusSeconds(300).toEpochMilli(),"tamil" to tamil)).build()
   WorkManager.getInstance(context).enqueueUniqueWork("reminder-$id",ExistingWorkPolicy.REPLACE,work)
  }
  fun cancel(context: Context,id: String) { WorkManager.getInstance(context).cancelUniqueWork("reminder-$id") }
 }
}
