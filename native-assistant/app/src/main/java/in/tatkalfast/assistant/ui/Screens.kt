package `in`.tatkalfast.assistant.ui

import android.Manifest
import android.content.*
import android.net.Uri
import android.os.*
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import `in`.tatkalfast.assistant.*
import `in`.tatkalfast.assistant.data.Preferences
import `in`.tatkalfast.domain.*
import kotlinx.coroutines.delay
import java.time.*
import java.util.Locale

@Composable fun AppTheme(mode: String, content: @Composable ()->Unit) {
 val dark=mode=="DARK" || (mode=="SYSTEM" && isSystemInDarkTheme())
 val palette=if(dark) darkColorScheme(primary=Color(0xFF87D6BB),secondary=Color(0xFFF2C879),background=Color(0xFF101916),surface=Color(0xFF18221E))
 else lightColorScheme(primary=Color(0xFF176B57),secondary=Color(0xFF875C14),background=Color(0xFFF5F8F5),surface=Color.White)
 MaterialTheme(colorScheme=palette,shapes=Shapes(medium=RoundedCornerShape(16.dp),large=RoundedCornerShape(24.dp)),content=content)
}
private fun openOfficial(context: Context) {
 val tab=CustomTabsIntent.Builder().setShowTitle(true).build()
 if(`in`.tatkalfast.assistant.autofill.BrowserTrust.verified(context,`in`.tatkalfast.assistant.autofill.BrowserTrust.CHROME)) tab.intent.setPackage(`in`.tatkalfast.assistant.autofill.BrowserTrust.CHROME)
 tab.launchUrl(context,Uri.parse("https://www.irctc.co.in/nget/train-search"))
}
private fun copy(context: Context, text: String) {
 val clipboard=context.getSystemService(ClipboardManager::class.java)
 val clip=ClipData.newPlainText("Tatkal prepared detail",text)
 if(Build.VERSION.SDK_INT>=33) clip.description.extras=PersistableBundle().apply { putBoolean(ClipDescription.EXTRA_IS_SENSITIVE,true) }
 clipboard.setPrimaryClip(clip)
 Handler(Looper.getMainLooper()).postDelayed({
  if(clipboard.primaryClip?.getItemAt(0)?.text?.toString()==text) {
   if(Build.VERSION.SDK_INT>=28) clipboard.clearPrimaryClip() else clipboard.setPrimaryClip(ClipData.newPlainText("",""))
  }
 },60000)
}
@Composable private fun Panel(content: @Composable ColumnScope.()->Unit) {
 Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(10.dp),content=content) }
}
@Composable fun Field(label: String,value: String,onValue: (String)->Unit,numeric: Boolean=false) {
 OutlinedTextField(value,onValue,label={Text(label)},singleLine=true,modifier=Modifier.fillMaxWidth(),keyboardOptions=KeyboardOptions(keyboardType=if(numeric) KeyboardType.Number else KeyboardType.Text))
}
@Composable private fun DateField(label:String,value:String,onValue:(String)->Unit) {
 val context=LocalContext.current
 OutlinedTextField(value,onValue,label={Text(label)},singleLine=true,modifier=Modifier.fillMaxWidth(),trailingIcon={
  IconButton(onClick={
   val date=runCatching {LocalDate.parse(value)}.getOrDefault(LocalDate.now(Rules.zone))
   android.app.DatePickerDialog(context,{_,year,month,day->onValue(LocalDate.of(year,month+1,day).toString())},date.year,date.monthValue-1,date.dayOfMonth).show()
  }) {Icon(Icons.Outlined.CalendarMonth,label)}
 })
}
@Composable fun Choice(label: String,value: String,options: List<String>,words: Words,onSelect:(String)->Unit) {
 var expanded by remember { mutableStateOf(false) }
 Box {
  OutlinedButton(onClick={expanded=true},modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)) { Text("$label: ${words.t(value)}"); Spacer(Modifier.weight(1f)); Icon(Icons.Outlined.ExpandMore,null) }
  DropdownMenu(expanded,{expanded=false}) { options.forEach { option -> DropdownMenuItem(text={Text(words.t(option))},onClick={expanded=false;onSelect(option)}) } }
 }
}
@Composable fun Check(label:String,value:Boolean,onChange:(Boolean)->Unit) {
 Row(Modifier.fillMaxWidth().clickable { onChange(!value) }.padding(vertical=4.dp)) { Checkbox(value,onChange); Text(label,Modifier.padding(top=12.dp)) }
}
@Composable private fun Editor(title: String,words: Words,onDismiss:()->Unit,content:@Composable ColumnScope.()->Unit) {
 Dialog(onDismissRequest=onDismiss) { Surface(shape=RoundedCornerShape(24.dp)) {
  Column(Modifier.fillMaxWidth().heightIn(max=680.dp).imePadding().verticalScroll(rememberScrollState()).padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
   Text(title,style=MaterialTheme.typography.titleLarge);content();TextButton(onClick=onDismiss) { Text(words.t("cancel")) }
  }
 } }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun TatkalScreen(vm:BookingViewModel,words:Words) {
 val v by vm.vault.collectAsStateWithLifecycle()
 val prefs by vm.preferences.collectAsStateWithLifecycle()
 val busy by vm.busy.collectAsStateWithLifecycle()
 val error by vm.error.collectAsStateWithLifecycle()
 val vault=v?:return
 val context=LocalContext.current
 var tab by rememberSaveable { mutableStateOf("home") }
 var journeyEditor by remember { mutableStateOf<Journey?>(null) }
 var passengerEditor by remember { mutableStateOf<Passenger?>(null) }
 var review by remember { mutableStateOf<Journey?>(null) }
 var resolving by remember { mutableStateOf<Attempt?>(null) }
 var cancelling by remember { mutableStateOf<Attempt?>(null) }
 var message by remember { mutableStateOf<String?>(null) }
 var pendingReminder by remember { mutableStateOf<Journey?>(null) }
 fun launchOfficial() { try { openOfficial(context) } catch(_:Exception) { message="browser_error" } }
 fun copied(text:String) { copy(context,text);message="copy_notice" }
 val notificationPermission=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
  val j=pendingReminder
  if(granted && j!=null) { Rules.opening(j)?.let { ReminderWorker.schedule(context,j.id,it,words.tamil) };message="reminder_set" }
  else message="reminder_denied"
  pendingReminder=null
 }
 Scaffold(topBar={TopAppBar(title={Text(words.t("brand"),fontWeight=FontWeight.Bold)},actions={if(busy) CircularProgressIndicator(Modifier.size(24.dp).padding(4.dp))})},bottomBar={
  NavigationBar { listOf("home" to Icons.Outlined.Home,"bookings" to Icons.Outlined.ConfirmationNumber,"passengers" to Icons.Outlined.People,"profile" to Icons.Outlined.Tune).forEach { (key,icon)->
   NavigationBarItem(selected=tab==key,onClick={tab=key},icon={Icon(icon,words.t(key))},label={Text(words.t(key))}) }
 } }) { padding ->
  LazyColumn(Modifier.fillMaxSize().padding(padding),contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
   if(tab=="home") {
    item { Text(words.t("tagline"),style=MaterialTheme.typography.bodyLarge) }
    val active=vault.attempts.firstOrNull { !it.resolved }
    if(active!=null) item { ActivePanel(active,words,busy,onOpen={if(!active.handedOff) vm.handoff(active.id) { openOfficial(context) } else launchOfficial()},onAdvance={vm.change(active.id,BookingEngine::advance)},onExpired={vm.change(active.id,BookingEngine::sessionExpired)},onUncertain={vm.networkUncertain(active.id)},onResolve={resolving=active},onCancel={vm.change(active.id,BookingEngine::cancelPrepared)},onCopy={copied(it)}) }
    if(vault.journeys.isEmpty()) item { Panel { Text(words.t("empty_journey"),style=MaterialTheme.typography.titleLarge);Text(words.t("empty_hint")) } }
    val selected=vault.journeys.find { it.id==vault.selectedJourney }?:vault.journeys.firstOrNull()
    if(selected!=null) {
     item { Countdown(selected,words) }
     item { Panel {
      Text(selected.label.ifBlank { "${selected.from} → ${selected.to}" },style=MaterialTheme.typography.titleLarge)
      JourneySummary(selected,vault.passengers.filter { it.id in selected.passengerIds },words)
      Button(onClick={review=selected},enabled=active==null&&!busy,modifier=Modifier.fillMaxWidth()) { Text(words.t("start")) }
      Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
       OutlinedButton(onClick={journeyEditor=selected}) { Text(words.t("edit")) }
       OutlinedButton(onClick={launchOfficial()}) { Text(words.t("check_train")) }
      }
      val opening=Rules.opening(selected)
      if(opening!=null && opening>Instant.now()) TextButton(onClick={
       if(Build.VERSION.SDK_INT>=33) {pendingReminder=selected;notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)}
       else {ReminderWorker.schedule(context,selected.id,opening,words.tamil);message="reminder_set"}
      }) { Text(words.t("remind")) }
     } }
    }
    item { Button(onClick={journeyEditor=Journey()},modifier=Modifier.fillMaxWidth()) { Icon(Icons.Outlined.Add,null);Text(words.t("add_journey")) } }
    vault.journeys.filter { it.id!=selected?.id }.forEach { j-> item(key=j.id) { OutlinedCard(onClick={vm.select(j.id)},modifier=Modifier.fillMaxWidth()) { Text("${j.label.ifBlank { j.train }} • ${j.from} → ${j.to} • ${j.date}",Modifier.padding(16.dp)) } } }
    vault.attempts.lastOrNull { it.stage==Stage.BOOKED }?.let { recent -> item { Panel {
     Text(words.t("recent_booking"),style=MaterialTheme.typography.titleMedium)
     Text("${recent.journey.from} → ${recent.journey.to} • ${recent.journey.date}")
     Text(words.t(recent.ticketStatus.name));TextButton(onClick={tab="bookings"}) {Text(words.t("bookings"))}
    } } }
    item { TextButton(onClick={tab="passengers"}) {Text(words.t("passengers"))};TextButton(onClick={tab="profile"}) {Text(words.t("payment_settings"))} }
   }
   if(tab=="passengers") {
    item { Button(onClick={passengerEditor=Passenger()},modifier=Modifier.fillMaxWidth()) {Text(words.t("add_passenger"))} }
    if(vault.passengers.isEmpty()) item {Text(words.t("no_passengers"))}
    vault.passengers.forEach { p-> item(key=p.id) { Panel {
     Text(p.name,style=MaterialTheme.typography.titleMedium);Text("${p.age} • ${words.t(p.gender.name)} • ${p.group}")
     Text(words.t(p.berth.name));Row {TextButton(onClick={passengerEditor=p}) {Text(words.t("edit"))};TextButton(onClick={vm.deletePassenger(p.id)}) {Text(words.t("delete"))}}
    } } }
   }
   if(tab=="bookings") {
    if(vault.attempts.isEmpty()) item {Text(words.t("no_history"))}
    val today=LocalDate.now(Rules.zone).toString()
    val groups=linkedMapOf("upcoming" to vault.attempts.filter {it.stage==Stage.BOOKED && it.ticketStatus!=TicketStatus.CANCELLED && it.journey.date>=today},"completed" to vault.attempts.filter {it.stage==Stage.BOOKED && it.ticketStatus!=TicketStatus.CANCELLED && it.journey.date<today},"cancelled" to vault.attempts.filter {it.stage==Stage.BOOKED && it.ticketStatus==TicketStatus.CANCELLED},"attempts" to vault.attempts.filter {it.stage!=Stage.BOOKED})
    groups.forEach { (group,entries)->
     if(entries.isNotEmpty()) item {Text(words.t(group),style=MaterialTheme.typography.titleLarge)}
     entries.reversed().forEach { a-> item(key=a.id) { Panel {
      Text("${a.journey.from} → ${a.journey.to}",style=MaterialTheme.typography.titleLarge)
      Text("${a.journey.train} • ${a.journey.date} • ${words.t(a.stage.name)}")
      Text(words.t("user_record"),style=MaterialTheme.typography.labelSmall)
      Text(a.passengers.joinToString { it.name })
      if(a.pnr.isNotEmpty()) {
       Text("PNR ${a.pnr}",fontWeight=FontWeight.Bold);Text(words.t(a.ticketStatus.name));if(a.allocation.isNotBlank()) Text(a.allocation)
       TextButton(onClick={copied(a.pnr)}) {Text(words.t("copy_pnr"))}
       TextButton(onClick={
        val text="${a.journey.from} → ${a.journey.to}\n${a.journey.date} • ${a.journey.train} • ${a.journey.travelClass.code}\nPNR ${a.pnr} • ${words.t(a.ticketStatus.name)}\n${a.allocation}"
        context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT,text),words.t("share")))
       }) {Text(words.t("share"))}
       if(a.ticketStatus!=TicketStatus.CANCELLED) TextButton(onClick={cancelling=a}) {Text(words.t("cancel_record"))}
      }
      TextButton(onClick={launchOfficial()}) {Text(words.t("official"))}
      if(!a.resolved) TextButton(onClick={tab="home"}) {Text(words.t("active"))}
     } } }
    }
   }
   if(tab=="profile") item { Panel {
    val p=prefs?:Preferences()
    Choice(words.t("language"),if(p.tamil) "தமிழ்" else "English",listOf("English","தமிழ்"),words) {vm.prefs(p.copy(tamil=it=="தமிழ்"))}
    Choice(words.t("theme"),p.theme,listOf("SYSTEM","LIGHT","DARK"),words) {vm.prefs(p.copy(theme=it))}
    Check(words.t("lock"),p.lock) {vm.prefs(p.copy(lock=it))}
    ReadinessPanel(words)
    Text(words.t("autofill_title"),style=MaterialTheme.typography.titleMedium)
    Text(words.t("autofill_setup_note"))
    OutlinedButton(onClick={
     try {context.startActivity(Intent(android.provider.Settings.ACTION_REQUEST_SET_AUTOFILL_SERVICE,Uri.parse("package:${context.packageName}")))} catch(_:Exception) {message="autofill_unavailable"}
    }) {Text(words.t("autofill_enable"))}
    OutlinedButton(onClick={context.startActivity(Intent(context,BackupActivity::class.java))}) {Text(words.t("backup_title"))}
    Choice(words.t("payment_settings"),vault.payment.name,Payment.entries.map {it.name},words) {vm.payment(Payment.valueOf(it))}
    Text(words.t("payment_note"));HorizontalDivider();Text(words.t("privacy"),style=MaterialTheme.typography.titleMedium);Text(words.t("privacy_body"));Text(words.t("identity_note"))
   } }
  }
 }
 journeyEditor?.let { j->JourneyEditor(j,vault.passengers,words,onDismiss={journeyEditor=null},onSave={ReminderWorker.cancel(context,it.id);vm.journey(it);journeyEditor=null},onDelete={ReminderWorker.cancel(context,j.id);vm.deleteJourney(j.id);journeyEditor=null}) }
 passengerEditor?.let {p->PassengerEditor(p,words,{passengerEditor=null}) {vm.passenger(it);passengerEditor=null} }
 review?.let { j->Editor(words.t("review_title"),words,{review=null}) {
  JourneySummary(j,vault.passengers.filter {it.id in j.passengerIds},words);Text(words.t(vault.payment.name));Text(words.t("handoff_note"));Text(words.t("identity_note"))
  val problems=Rules.journey(j,vault.passengers)
  problems.forEach {Text(words.t(it),color=MaterialTheme.colorScheme.error)}
  var checked by remember {mutableStateOf(false)}
  Check(words.t("review_check"),checked) {checked=it}
  Button(onClick={vm.prepare(j);review=null},enabled=checked&&problems.isEmpty()&&!busy) {Text(words.t("start"))}
 } }
 resolving?.let {a->ResolveEditor(a,words,{resolving=null}) {booked,history,paymentChecked,payment,pnr,allocation,status->vm.change(a.id) {BookingEngine.resolveVerified(it,booked,history,paymentChecked,payment,pnr,allocation,status)};resolving=null} }
 cancelling?.let { a->AlertDialog(onDismissRequest={cancelling=null},title={Text(words.t("cancel_record"))},text={Text(words.t("cancel_confirm"))},confirmButton={TextButton(onClick={vm.change(a.id) {it.copy(ticketStatus=TicketStatus.CANCELLED)};cancelling=null}) {Text(words.t("done"))}},dismissButton={TextButton(onClick={cancelling=null}) {Text(words.t("cancel"))}}) }
 (error?:message)?.let {m->AlertDialog(onDismissRequest={vm.clearError();message=null},text={Text(words.t(m))},confirmButton={TextButton(onClick={vm.clearError();message=null}) {Text(words.t("done"))}}) }
}

@Composable private fun Countdown(j:Journey,words:Words) {
 var now by remember {mutableStateOf(Instant.now())}
 LaunchedEffect(j.id) {while(true) {now=Instant.now();delay(1000)}}
 val seconds=Rules.countdown(j,now)
 Card(colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.primaryContainer)) {
  Column(Modifier.fillMaxWidth().padding(22.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
   Text(words.t("countdown"),style=MaterialTheme.typography.labelLarge)
   if(seconds!=null && seconds>0) Text(String.format(Locale.ROOT,"%02d : %02d : %02d",seconds/3600,seconds%3600/60,seconds%60),style=MaterialTheme.typography.displaySmall,fontWeight=FontWeight.Bold)
   else Text(words.t(if(seconds==null) "no_countdown" else "opened"))
   Text(words.t("clock_note"),style=MaterialTheme.typography.bodySmall)
  }
 }
}
@Composable private fun JourneySummary(j:Journey,passengers:List<Passenger>,words:Words) {
 Text("${j.from} → ${j.to}",style=MaterialTheme.typography.headlineSmall)
 Text("${j.train} • ${j.date} • ${j.travelClass.code} • ${words.t(j.quota.name)}")
 Text("${words.t("boarding")}: ${j.boarding}")
 Text("${words.t("origin_date")}: ${j.originDate}")
 Text("${words.t("mobile")}: ${j.mobile}")
 Text("${words.t(j.berth.name)} • ${words.t(j.meal.name)}")
 passengers.forEach {
  Text("${it.name} • ${it.age} • ${words.t(it.gender.name)} • ${words.t(it.berth.name)} • ${it.nationality}")
  if(it.seniorPreference) Text(words.t("senior"),style=MaterialTheme.typography.bodySmall)
  if(it.childBerthRequired) Text(words.t("child"),style=MaterialTheme.typography.bodySmall)
 }
}
@Composable private fun ActivePanel(a:Attempt,words:Words,busy:Boolean,onOpen:()->Unit,onAdvance:()->Unit,onExpired:()->Unit,onUncertain:()->Unit,onResolve:()->Unit,onCancel:()->Unit,onCopy:(String)->Unit) {
 Panel {
  Text(words.t("active"),style=MaterialTheme.typography.titleLarge)
  Text("${words.t("local_stage")}: ${words.t(a.stage.name)}",fontWeight=FontWeight.Bold)
  Text(words.t("lock_note"));JourneySummary(a.journey,a.passengers,words)
  Text(words.t(a.payment.name));Text(words.t("handoff_note"))
  if(a.stage==Stage.CAPTCHA_REQUIRED) Text(words.t("captcha_note"))
  if(a.stage in setOf(Stage.UNKNOWN,Stage.PAYMENT,Stage.PAYMENT_PENDING)) Text(words.t("pending_note"),color=MaterialTheme.colorScheme.error)
  Button(onClick=onOpen,enabled=!busy,modifier=Modifier.fillMaxWidth()) {Text(words.t("official"))}
  val fields=listOf(words.t("from") to a.journey.from,words.t("to") to a.journey.to,words.t("train") to a.journey.train,words.t("boarding") to a.journey.boarding,words.t("mobile") to a.journey.mobile, words.t("date") to a.journey.date) + a.passengers.flatMap {listOf(words.t("name") to it.name, words.t("age") to it.age.toString(), words.t("nationality") to it.nationality)}
  fields.forEach { (label,value)->TextButton(onClick={onCopy(value)}) {Text("${words.t("copy")} $label: $value")} }
  if(a.handedOff) {
   if(a.stage !in setOf(Stage.UNKNOWN,Stage.PAYMENT_PENDING)) OutlinedButton(onClick=onAdvance,enabled=!busy) {Text(words.t("next_stage"))}
   TextButton(onClick=onExpired,enabled=!busy) {Text(words.t("session_expired"))}
   TextButton(onClick=onUncertain,enabled=!busy) {Text(words.t("uncertain"))}
   Button(onClick=onResolve,enabled=!busy) {Text(words.t("resolve"))}
  } else TextButton(onClick=onCancel,enabled=!busy) {Text(words.t("cancel"))}
 }
}

@Composable fun PassengerEditor(initial:Passenger,words:Words,onDismiss:()->Unit,onSave:(Passenger)->Unit) {
 var p by remember {mutableStateOf(initial)}
 var age by remember {mutableStateOf(initial.age.toString())}
 var submitted by remember {mutableStateOf(false)}
 Editor(words.t("add_passenger"),words,onDismiss) {
  Field(words.t("name"),p.name,{p=p.copy(name=it.take(60))})
  Field(words.t("age"),age,{age=it.take(3);p=p.copy(age=it.toIntOrNull()?:-1)},true)
  Choice(words.t("gender"),p.gender.name,Gender.entries.map {it.name},words) {p=p.copy(gender=Gender.valueOf(it))}
  Choice(words.t("berth"),p.berth.name,Berth.entries.map {it.name},words) {p=p.copy(berth=Berth.valueOf(it))}
  Field(words.t("nationality"),p.nationality,{p=p.copy(nationality=it.uppercase(Locale.ROOT).take(2))})
  Field(words.t("group"),p.group,{p=p.copy(group=it.take(40))})
  Check(words.t("senior"),p.seniorPreference) {p=p.copy(seniorPreference=it)}
  if(p.age<12) Check(words.t("child"),p.childBerthRequired) {p=p.copy(childBerthRequired=it)}
  val clean=p.copy(name=p.name.trim(),group=p.group.trim(),childBerthRequired=p.age<12&&p.childBerthRequired)
  val problems=Rules.passenger(clean)
  if(submitted) problems.forEach {Text(words.t(it),color=MaterialTheme.colorScheme.error)}
  Button(onClick={submitted=true;if(problems.isEmpty()) onSave(clean)}) {Text(words.t("save"))}
 }
}
@Composable fun JourneyEditor(initial:Journey,passengers:List<Passenger>,words:Words,onDismiss:()->Unit,onSave:(Journey)->Unit,onDelete:()->Unit) {
 var j by remember {mutableStateOf(initial)}
 var submitted by remember {mutableStateOf(false)}
 Editor(words.t("add_journey"),words,onDismiss) {
  Field(words.t("journey_name"),j.label,{j=j.copy(label=it.take(50))})
  Field(words.t("from"),j.from,{j=j.copy(from=it.uppercase(Locale.ROOT).take(5))})
  Field(words.t("to"),j.to,{j=j.copy(to=it.uppercase(Locale.ROOT).take(5))})
  DateField(words.t("date"),j.date,{j=j.copy(date=it.take(10))})
  DateField(words.t("origin_date"),j.originDate,{j=j.copy(originDate=it.take(10))})
  Text(words.t("origin_help"),style=MaterialTheme.typography.bodySmall)
  Field(words.t("train"),j.train,{j=j.copy(train=it.take(5))},true)
  Choice(words.t("class"),j.travelClass.code,TravelClass.entries.map {it.code},words) {code->j=j.copy(travelClass=TravelClass.entries.first {it.code==code})}
  Choice(words.t("quota"),j.quota.name,Quota.entries.map {it.name},words) {j=j.copy(quota=Quota.valueOf(it))}
  Field(words.t("boarding"),j.boarding,{j=j.copy(boarding=it.uppercase(Locale.ROOT).take(5))})
  Choice(words.t("berth"),j.berth.name,Berth.entries.map {it.name},words) {j=j.copy(berth=Berth.valueOf(it))}
  Choice(words.t("meal"),j.meal.name,Meal.entries.map {it.name},words) {j=j.copy(meal=Meal.valueOf(it))}
  Field(words.t("mobile"),j.mobile,{j=j.copy(mobile=it.take(10))},true)
  Text(words.t("passengers"),style=MaterialTheme.typography.titleMedium)
  passengers.groupBy {it.group}.forEach { (group,members)->
   TextButton(onClick={j=j.copy(passengerIds=(j.passengerIds+members.map {it.id}).distinct())}) {Text("${words.t("select_group")}: $group")}
   members.forEach {p->Check("${p.name} • ${p.age}",p.id in j.passengerIds) {yes->j=j.copy(passengerIds=if(yes) (j.passengerIds+p.id).distinct() else j.passengerIds-p.id)} }
  }
  if(passengers.isEmpty()) Text(words.t("no_passengers"))
  val problems=Rules.journey(j,passengers)
  if(submitted) problems.forEach {Text(words.t(it),color=MaterialTheme.colorScheme.error)}
  Button(onClick={submitted=true;if(problems.isEmpty()) onSave(j)}) {Text(words.t("save"))}
  if(initial.date.isNotEmpty()) TextButton(onClick=onDelete) {Text(words.t("delete"))}
 }
}
@Composable private fun ResolveEditor(a:Attempt,words:Words,onDismiss:()->Unit,onResolve:(Boolean,Boolean,Boolean,PaymentCheck,String,String,TicketStatus)->Unit) {
 var booked by remember {mutableStateOf(true)}
 var verified by remember {mutableStateOf(false)}
 var bankChecked by remember {mutableStateOf(false)}
 var paymentCheck by remember {mutableStateOf(PaymentCheck.UNKNOWN_CHECK)}
 var pnr by remember {mutableStateOf("")}
 var allocation by remember {mutableStateOf("")}
 var status by remember {mutableStateOf(TicketStatus.CONFIRMED)}
 var pasted by remember {mutableStateOf("")}
 var pasteError by remember {mutableStateOf(false)}
 Editor(words.t("resolve"),words,onDismiss) {
  Text("${a.journey.train} • ${a.journey.date}");Text(words.t("pending_note"))
  Choice(words.t("status"),if(booked) "booked" else "not_booked",listOf("booked","not_booked"),words) {booked=it=="booked"}
  if(booked) {
   Text(words.t("confirmation_note"))
   OutlinedTextField(pasted,{pasted=it.take(12000)},label={Text(words.t("confirmation_paste"))},modifier=Modifier.fillMaxWidth(),maxLines=4)
   TextButton(onClick={val parsed=ConfirmationParser.pnr(pasted);pasteError=parsed==null;if(parsed!=null) {pnr=parsed;pasted=""}}) {Text(words.t("confirmation_extract"))}
   if(pasteError) Text(words.t("confirmation_invalid"))
   Field(words.t("pnr"),pnr,{pnr=it.filter(Char::isDigit).take(10)},true)
   Field(words.t("allocation"),allocation,{allocation=it.take(250)})
   Choice(words.t("status"),status.name,TicketStatus.entries.map {it.name},words) {status=TicketStatus.valueOf(it)}
  }
  Choice(words.t("payment_check"),paymentCheck.name,PaymentCheck.entries.map {it.name},words) {paymentCheck=PaymentCheck.valueOf(it)}
  Check(words.t("history_checked"),verified) {verified=it}
  Check(words.t("payment_checked"),bankChecked) {bankChecked=it}
  val consistent=if(booked) paymentCheck==PaymentCheck.PAID else paymentCheck in setOf(PaymentCheck.NOT_STARTED,PaymentCheck.FAILED_PAYMENT,PaymentCheck.REFUNDED)
  if(!consistent) Text(words.t("pending_note"),color=MaterialTheme.colorScheme.error)
  Button(onClick={onResolve(booked,verified,bankChecked,paymentCheck,pnr,allocation,status)},enabled=verified&&bankChecked&&consistent&&(!booked||pnr.length==10)) {Text(words.t("save"))}
 }
}
