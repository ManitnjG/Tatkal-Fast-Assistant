package `in`.tatkalfast.assistant.autofill

import android.app.PendingIntent
import android.app.assist.AssistStructure
import android.content.Intent
import android.os.Build
import android.os.CancellationSignal
import android.os.SystemClock
import android.service.autofill.*
import android.text.InputType
import android.view.View
import android.view.autofill.AutofillId
import android.widget.RemoteViews
import `in`.tatkalfast.assistant.R
import `in`.tatkalfast.domain.*
import java.util.UUID

internal data class FillField(val id:AutofillId,val field:PreparedField)
internal data class FillTarget(val id:AutofillId,val field:PreparedField,val host:String,val browser:String,val expires:Long,val additional:List<FillField> = emptyList()) {
 val fields:List<FillField> get()=listOf(FillField(id,this.field))+additional
}
/** Ephemeral framework IDs only, no passenger data. Process death invalidates every request. */
internal object PendingFills {
 private val entries=linkedMapOf<String,FillTarget>()
 @Synchronized fun add(t:FillTarget):String {
  entries.entries.removeAll {it.value.expires<SystemClock.elapsedRealtime()}
  while(entries.size>=8) entries.remove(entries.keys.first())
  return UUID.randomUUID().toString().also {entries[it]=t}
 }
 @Synchronized fun take(token:String?):FillTarget? = entries.remove(token)?.takeIf {it.expires>=SystemClock.elapsedRealtime()}
}

class PreparedAutofillService:AutofillService() {
 override fun onFillRequest(request:FillRequest,cancellationSignal:CancellationSignal,callback:FillCallback) {
  if(cancellationSignal.isCanceled) return
  try {
   // Android 8 remains supported for preparation; web scheme verification needs Android 9+.
   if(Build.VERSION.SDK_INT<28) {callback.onSuccess(null);return}
   val structure=request.fillContexts.lastOrNull()?.structure ?: run {callback.onSuccess(null);return}
   val browser=structure.activityComponent.packageName
   if(!BrowserTrust.verified(this,browser)) {callback.onSuccess(null);return}
   val matches=mutableMapOf<String,FillTarget>();val candidates=mutableListOf<FillCandidate>();var visited=0;var truncated=false
   fun visit(node:AssistStructure.ViewNode,host:String?,scheme:String?,depth:Int,path:String,parent:String,ancestorVisible:Boolean) {
    if(depth>60 || ++visited>5000) {truncated=true;return}
    val domain=node.webDomain?:host
    val protocol=node.webScheme?:scheme
    val visible=ancestorVisible && node.visibility==View.VISIBLE
    if(visible && node.autofillType==View.AUTOFILL_TYPE_TEXT && AutofillPolicy.trustedOrigin(domain,protocol)) {
     val attributes=node.htmlInfo?.attributes.orEmpty().filter {it.first in setOf("id","name","placeholder","autocomplete","formcontrolname","aria-label","type")}.map {it.second}
     val descriptors=listOfNotNull(node.idEntry,node.hint)+node.autofillHints.orEmpty()+attributes
     val variation=node.inputType and InputType.TYPE_MASK_VARIATION
     val password=variation in setOf(InputType.TYPE_TEXT_VARIATION_PASSWORD,InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD,InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD,InputType.TYPE_NUMBER_VARIATION_PASSWORD) || attributes.any {it.equals("password",true)}
     val field=AutofillPolicy.classify(descriptors,password)
     val id=node.autofillId
     if(field!=null && id!=null) {matches[path]=FillTarget(id,field,domain!!,browser,SystemClock.elapsedRealtime()+120000);candidates+=FillCandidate(path,field,domain,parent,node.isFocused)}
    }
    for(i in 0 until node.childCount) visit(node.getChildAt(i),domain,protocol,depth+1,"$path/$i",path,visible)
   }
   for(i in 0 until structure.windowNodeCount) visit(structure.getWindowNodeAt(i).rootViewNode,null,null,0,"$i","root/$i",true)
   val keys=if(truncated) emptyList() else FillGrouping.select(candidates)
   val focus=candidates.singleOrNull {it.focused}
   val primary=focus?.key?.let(matches::get)
   val target=primary?.takeIf {keys.isNotEmpty()}?.copy(additional=keys.filter {it!=focus.key}.mapNotNull {matches[it]?.let {t->FillField(t.id,t.field)}})
   if(target==null || cancellationSignal.isCanceled) {if(!cancellationSignal.isCanceled) callback.onSuccess(null);return}
   val token=PendingFills.add(target)
   val intent=Intent(this,AutofillReviewActivity::class.java).setData(android.net.Uri.parse("tatkal-internal://fill/$token")).putExtra("request_token",token)
   val pending=PendingIntent.getActivity(this,token.hashCode(),intent,PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE)
   val view=RemoteViews(packageName,android.R.layout.simple_list_item_1).apply {setTextViewText(android.R.id.text1,getString(R.string.autofill_unlock))}
   val builder=Dataset.Builder(view).setAuthentication(pending.intentSender)
   target.fields.forEach {builder.setValue(it.id,null)}
   val dataset=builder.build()
   if(!cancellationSignal.isCanceled) callback.onSuccess(FillResponse.Builder().addDataset(dataset).build())
  } catch(_:Exception) {if(!cancellationSignal.isCanceled) callback.onSuccess(null)}
 }
 // No SaveInfo is offered: never capture passwords, OTPs, CAPTCHA, cookies or form contents.
 override fun onSaveRequest(request:SaveRequest,callback:SaveCallback) {callback.onSuccess()}
}
