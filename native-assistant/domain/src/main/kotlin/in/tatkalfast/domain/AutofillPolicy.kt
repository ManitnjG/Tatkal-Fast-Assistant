package `in`.tatkalfast.domain

import java.util.Locale

enum class PreparedField { PASSENGER_NAME, PASSENGER_AGE, MOBILE, FROM, TO, BOARDING, TRAIN }
/** Pure policy: never guess unknown fields or fill security/payment credentials. */
object AutofillPolicy {
 private val blocked=listOf("captcha","otp","onetime","verification","securitycode","password","passwd","pin","cvv","cvc","card","accountnumber","aadhaar","aadhar","transaction","wallet","username","login")
 private val aliases=mapOf(
  PreparedField.PASSENGER_NAME to setOf("passengername","psgnname","travellername","travelername"),
  PreparedField.PASSENGER_AGE to setOf("passengerage","psgnage","age"),
  PreparedField.MOBILE to setOf("mobilenumber","passengermobile","contactnumber","contactmobile","phonenumber"),
  PreparedField.FROM to setOf("fromstation","fromstationcode","journeyfrom"),
  PreparedField.TO to setOf("tostation","tostationcode","journeyto"),
  PreparedField.BOARDING to setOf("boardingstation","boardingstationcode"),
  PreparedField.TRAIN to setOf("trainnumber","trainno")
 )
 fun trustedOrigin(host:String?,scheme:String?) = scheme=="https" && host?.lowercase(Locale.ROOT) in setOf("www.irctc.co.in","irctc.co.in")
 fun classify(descriptors:List<String>,password:Boolean=false):PreparedField? {
  if(password) return null
  val tokens=descriptors.map {it.lowercase(Locale.ROOT).filter(Char::isLetterOrDigit)}
  if(tokens.any {s->blocked.any {it in s}}) return null
  val candidates=aliases.filterValues {known->tokens.any {it in known}}.keys
  return candidates.singleOrNull()
 }
 fun canFill(a:Attempt) = !a.resolved && a.handedOff && a.stage in setOf(Stage.LOGIN_REQUIRED,Stage.TRAIN_SELECTION,Stage.PASSENGER_DETAILS,Stage.CAPTCHA_REQUIRED,Stage.REVIEW)
 fun value(field:PreparedField,a:Attempt,passengerId:String?):String? {
  if(!canFill(a)) return null
  val p=a.passengers.find {it.id==passengerId}
  return when(field) {
   PreparedField.PASSENGER_NAME->p?.name
   PreparedField.PASSENGER_AGE->p?.age?.toString()
   PreparedField.MOBILE->a.journey.mobile
   PreparedField.FROM->a.journey.from
   PreparedField.TO->a.journey.to
   PreparedField.BOARDING->a.journey.boarding
   PreparedField.TRAIN->a.journey.train
  }?.takeIf {it.isNotBlank()}
 }
}

/** Only unambiguous sibling fields from the focused data partition may share a review. */
data class FillCandidate(val key:String,val field:PreparedField,val host:String,val group:String,val focused:Boolean)
object FillGrouping {
 private val passenger=setOf(PreparedField.PASSENGER_NAME,PreparedField.PASSENGER_AGE)
 fun select(candidates:List<FillCandidate>):List<String> {
  val focus=candidates.singleOrNull {it.focused}?:return emptyList()
  val same=candidates.filter {it.host==focus.host && it.group==focus.group && (it.field in passenger)==(focus.field in passenger)}
  val unique=same.groupBy {it.field}.values.filter {it.size==1}.flatten()
  // Ambiguous focused metadata falls back to exactly the focused field.
  return if(focus !in unique) listOf(focus.key) else unique.map {it.key}.take(7)
 }
}
