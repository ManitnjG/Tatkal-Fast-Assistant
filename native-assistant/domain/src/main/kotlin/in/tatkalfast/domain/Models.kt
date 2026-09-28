package `in`.tatkalfast.domain

import kotlinx.serialization.Serializable
import java.time.*
import java.util.UUID
import java.util.Locale

@Serializable enum class Quota { TATKAL, PREMIUM_TATKAL, GENERAL }
@Serializable enum class TravelClass { A1, A2, A3, E3, CC, EC, SL, S2;
 val code: String get() = when(this) { A1 -> "1A"; A2 -> "2A"; A3 -> "3A"; E3 -> "3E"; S2 -> "2S"; else -> name }
}
@Serializable enum class Gender { MALE, FEMALE, TRANSGENDER }
@Serializable enum class Berth { NO_PREFERENCE, LOWER, MIDDLE, UPPER, SIDE_LOWER, SIDE_UPPER }
@Serializable enum class Meal { NO_PREFERENCE, VEG, NON_VEG }
@Serializable enum class Payment { IRCTC_EWALLET, UPI, NET_BANKING, CARD, OTHER }
@Serializable enum class Stage { PREPARED, LOGIN_REQUIRED, TRAIN_SELECTION, PASSENGER_DETAILS, CAPTCHA_REQUIRED, REVIEW, PAYMENT, PAYMENT_PENDING, BOOKED, FAILED, UNKNOWN }
@Serializable enum class PaymentCheck { NOT_STARTED, FAILED_PAYMENT, REFUNDED, PAID, PENDING_CHECK, UNKNOWN_CHECK }
@Serializable enum class TicketStatus { CONFIRMED, RAC, WAITLIST, CANCELLED }
@Serializable data class Passenger(
 val id: String = UUID.randomUUID().toString(), val name: String = "", val age: Int = 30,
 val gender: Gender = Gender.MALE, val berth: Berth = Berth.NO_PREFERENCE,
 val nationality: String = "IN", val group: String = "Family",
 val seniorPreference: Boolean = false, val childBerthRequired: Boolean = false
)
@Serializable data class Journey(
 val id: String = UUID.randomUUID().toString(), val label: String = "",
 val from: String = "", val to: String = "", val date: String = "", val originDate: String = "",
 val train: String = "", val travelClass: TravelClass = TravelClass.SL,
 val quota: Quota = Quota.TATKAL, val boarding: String = "",
 val berth: Berth = Berth.NO_PREFERENCE, val meal: Meal = Meal.NO_PREFERENCE,
 val mobile: String = "", val passengerIds: List<String> = emptyList()
)
@Serializable data class Attempt(
 val id: String = UUID.randomUUID().toString(), val journey: Journey,
 val passengers: List<Passenger>, val payment: Payment,
 val stage: Stage = Stage.PREPARED, val createdAt: Long = System.currentTimeMillis(),
 val handedOff: Boolean = false, val resolved: Boolean = false,
 val paymentCheck: PaymentCheck? = null, val verifiedAt: Long? = null,
 val pnr: String = "", val allocation: String = "", val ticketStatus: TicketStatus = TicketStatus.CONFIRMED
)
@Serializable data class Vault(
 val schema: Int = 1, val passengers: List<Passenger> = emptyList(),
 val journeys: List<Journey> = emptyList(), val attempts: List<Attempt> = emptyList(),
 val selectedJourney: String? = null, val payment: Payment = Payment.IRCTC_EWALLET
)

object Rules {
 val zone: ZoneId = ZoneId.of("Asia/Kolkata")
 fun opening(j: Journey): Instant? {
  if (j.quota == Quota.GENERAL || j.travelClass == TravelClass.A1) return null
  val date = runCatching { LocalDate.parse(j.originDate) }.getOrNull() ?: return null
  val hour = if (j.travelClass in setOf(TravelClass.SL, TravelClass.S2)) 11 else 10
  return date.minusDays(1).atTime(hour, 0).atZone(zone).toInstant()
 }
 fun countdown(j: Journey, now: Instant): Long? = opening(j)?.let { Duration.between(now,it).seconds.coerceAtLeast(0) }
 fun passenger(p: Passenger): List<String> = buildList {
  if (p.name.trim().length !in 2..60 || !p.name.all { it.isLetter() || Character.getType(it) in setOf(Character.NON_SPACING_MARK.toInt(), Character.COMBINING_SPACING_MARK.toInt()) || it == ' ' || it == '.' || it == '-' || it == '\'' }) add("invalid_name")
  if (p.age !in 0..125) add("invalid_age")
  if (p.nationality !in Locale.getISOCountries().toSet()) add("invalid_nationality")
  if (p.group.isBlank() || p.group.length > 40) add("invalid_group")
  if (p.age >= 12 && p.childBerthRequired) add("invalid_child")
 }
 fun journey(j: Journey, passengers: List<Passenger>, today: LocalDate = LocalDate.now(zone)): List<String> = buildList {
  if (!j.from.matches(Regex("[A-Z]{2,5}")) || !j.to.matches(Regex("[A-Z]{2,5}")) || j.from == j.to) add("invalid_stations")
  if (!j.boarding.matches(Regex("[A-Z]{2,5}"))) add("invalid_boarding")
  val d = runCatching { LocalDate.parse(j.date) }.getOrNull()
  val origin = runCatching { LocalDate.parse(j.originDate) }.getOrNull()
  if (d == null || d < today) add("invalid_date")
  if (origin == null || d == null || origin > d || origin < d.minusDays(7)) add("invalid_origin")
  if (!j.train.matches(Regex("[0-9]{5}"))) add("invalid_train")
  if (!j.mobile.matches(Regex("[6-9][0-9]{9}"))) add("invalid_mobile")
  if (j.quota != Quota.GENERAL && j.travelClass == TravelClass.A1) add("invalid_class")
  val selected = passengers.filter { it.id in j.passengerIds }
  val limit = if (j.quota == Quota.GENERAL) 6 else 4
  if (selected.isEmpty() || selected.size > limit || selected.size != j.passengerIds.distinct().size) add("invalid_passengers")
  if (selected.any { passenger(it).isNotEmpty() }) add("invalid_passengers")
 }
}

object BookingEngine {
 private val next = mapOf(Stage.PREPARED to Stage.LOGIN_REQUIRED, Stage.LOGIN_REQUIRED to Stage.TRAIN_SELECTION,
  Stage.TRAIN_SELECTION to Stage.PASSENGER_DETAILS, Stage.PASSENGER_DETAILS to Stage.CAPTCHA_REQUIRED,
  Stage.CAPTCHA_REQUIRED to Stage.REVIEW, Stage.REVIEW to Stage.PAYMENT, Stage.PAYMENT to Stage.PAYMENT_PENDING)
 fun start(v: Vault, j: Journey, today: LocalDate = LocalDate.now(Rules.zone)): Vault {
  require(v.attempts.none { !it.resolved }) { "unresolved_attempt" }
  require(Rules.journey(j, v.passengers, today).isEmpty()) { "invalid_journey" }
  val a = Attempt(journey=j, passengers=v.passengers.filter { it.id in j.passengerIds }, payment=v.payment)
  return v.copy(attempts=v.attempts + a)
 }
 fun handoff(a: Attempt): Attempt {
  require(!a.resolved && !a.handedOff) { "unresolved_attempt" }
  return a.copy(handedOff=true, stage=Stage.LOGIN_REQUIRED)
 }
 fun advance(a: Attempt): Attempt {
  require(!a.resolved && a.handedOff) { "invalid_transition" }
  return a.copy(stage=next[a.stage] ?: error("invalid_transition"))
 }
 fun recover(a: Attempt): Attempt = if (!a.resolved && a.handedOff) a.copy(stage=Stage.UNKNOWN) else a
 fun sessionExpired(a: Attempt): Attempt {
  require(!a.resolved)
  return if (a.stage in setOf(Stage.PAYMENT, Stage.PAYMENT_PENDING, Stage.UNKNOWN)) a.copy(stage=Stage.UNKNOWN)
   else a.copy(stage=Stage.LOGIN_REQUIRED)
 }
 fun resolve(a: Attempt, booked: Boolean, verified: Boolean, pnr: String = "", allocation: String = "", status: TicketStatus = TicketStatus.CONFIRMED): Attempt {
  require(!a.resolved && verified) { "verify_history" }
  require(!booked || pnr.matches(Regex("[0-9]{10}"))) { "invalid_pnr" }
  return a.copy(resolved=true, stage=if(booked) Stage.BOOKED else Stage.FAILED, pnr=if(booked) pnr else "", allocation=allocation, ticketStatus=status)
 }
 fun resolveVerified(a:Attempt,booked:Boolean,historyChecked:Boolean,paymentChecked:Boolean,payment:PaymentCheck,pnr:String="",allocation:String="",status:TicketStatus=TicketStatus.CONFIRMED):Attempt {
  require(historyChecked && paymentChecked) { "verify_history" }
  val consistent=if(booked) payment==PaymentCheck.PAID else payment in setOf(PaymentCheck.NOT_STARTED,PaymentCheck.FAILED_PAYMENT,PaymentCheck.REFUNDED)
  require(consistent) { "pending_note" }
  return resolve(a,booked,true,pnr,allocation,status).copy(paymentCheck=payment,verifiedAt=System.currentTimeMillis())
 }
 fun cancelPrepared(a: Attempt): Attempt { require(!a.handedOff && !a.resolved); return a.copy(stage=Stage.FAILED,resolved=true) }
}
