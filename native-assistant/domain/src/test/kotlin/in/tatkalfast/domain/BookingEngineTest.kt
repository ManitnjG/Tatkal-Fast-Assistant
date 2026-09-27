package `in`.tatkalfast.domain
import org.junit.Assert.*
import org.junit.Test
import java.time.*
class BookingEngineTest {
 private val today=LocalDate.of(2026,9,26)
 private val passenger=Passenger(name="Example Passenger",age=34)
 private val journey=Journey(from="TJ",to="MS",boarding="TJ",date="2026-09-27",originDate="2026-09-27",train="16866",mobile="9000000000",passengerIds=listOf(passenger.id))
 private fun vault()=Vault(passengers=listOf(passenger))
 private fun prepared()=BookingEngine.start(vault(),journey,today).attempts.single()
 @Test fun validJourney() {assertTrue(Rules.journey(journey,listOf(passenger),today).isEmpty())}
 @Test fun stationCodesMustDiffer() {assertTrue("invalid_stations" in Rules.journey(journey.copy(to="TJ"),listOf(passenger),today))}
 @Test fun pastJourneyRejected() {assertTrue("invalid_date" in Rules.journey(journey.copy(date="2026-09-25"),listOf(passenger),today))}
 @Test fun invalidPassengerName() {assertTrue("invalid_name" in Rules.passenger(passenger.copy(name="<script>")))}
 @Test fun tamilPassengerNameAccepted() {assertTrue(Rules.passenger(passenger.copy(name="மணிகண்டன்")).isEmpty())}
 @Test fun ageBoundaries() {assertTrue(Rules.passenger(passenger.copy(age=0)).isEmpty());assertTrue(Rules.passenger(passenger.copy(age=126)).isNotEmpty())}
 @Test fun missingSelectedPassengerRejected() {assertTrue("invalid_passengers" in Rules.journey(journey,emptyList(),today))}
 @Test fun childDetailsCanBePreparedForOfficialReview() {assertTrue(Rules.journey(journey,listOf(passenger.copy(age=10)),today).isEmpty())}
 @Test fun tatkalPassengerLimit() {
  val people=(1..5).map {passenger.copy(id="p$it")}
  assertTrue("invalid_passengers" in Rules.journey(journey.copy(passengerIds=people.map {it.id}),people,today))
  assertTrue(Rules.journey(journey.copy(quota=Quota.GENERAL,passengerIds=people.map {it.id}),people,today).isEmpty())
 }
 @Test fun sleeperOpensAt11IST() {assertEquals(Instant.parse("2026-09-26T05:30:00Z"),Rules.opening(journey))}
 @Test fun acOpensAt10IST() {assertEquals(Instant.parse("2026-09-26T04:30:00Z"),Rules.opening(journey.copy(travelClass=TravelClass.A3)))}
 @Test fun originDateNotBoardingDate() {assertEquals(Instant.parse("2026-09-25T05:30:00Z"),Rules.opening(journey.copy(originDate="2026-09-26")))}
 @Test fun countdownClampsAtZero() {assertEquals(0L,Rules.countdown(journey,Instant.parse("2026-09-27T00:00:00Z")))}
 @Test fun countdownBeforeOpening() {assertEquals(120L,Rules.countdown(journey,Instant.parse("2026-09-26T05:28:00Z")))}
 @Test fun noTatkalFor1AOrGeneral() {assertNull(Rules.opening(journey.copy(travelClass=TravelClass.A1)));assertNull(Rules.opening(journey.copy(quota=Quota.GENERAL)))}
 @Test fun premiumUsesSameOpening() {assertEquals(Rules.opening(journey),Rules.opening(journey.copy(quota=Quota.PREMIUM_TATKAL)))}
 @Test fun calendarRollover() {assertEquals(Instant.parse("2026-12-31T05:30:00Z"),Rules.opening(journey.copy(originDate="2027-01-01")))}
 @Test fun malformedDateDoesNotCrashCountdown() {assertNull(Rules.opening(journey.copy(originDate="wrong")))}
 @Test(expected=IllegalArgumentException::class) fun duplicatePreparedBlocked() {val v=BookingEngine.start(vault(),journey,today);BookingEngine.start(v,journey,today)}
 @Test(expected=IllegalArgumentException::class) fun duplicateHandoffBlocked() {BookingEngine.handoff(BookingEngine.handoff(prepared()))}
 @Test fun processRecoveryLocksUnknown() {val a=BookingEngine.recover(BookingEngine.handoff(prepared()));assertEquals(Stage.UNKNOWN,a.stage);assertFalse(a.resolved)}
 @Test fun unlaunchedPreparationRestores() {assertEquals(Stage.PREPARED,BookingEngine.recover(prepared()).stage)}
 @Test fun happyChecklist() {var a=BookingEngine.handoff(prepared());repeat(6) {a=BookingEngine.advance(a)};assertEquals(Stage.PAYMENT_PENDING,a.stage)}
 @Test fun paymentSessionLossStaysUnknown() {assertEquals(Stage.UNKNOWN,BookingEngine.sessionExpired(prepared().copy(stage=Stage.PAYMENT,handedOff=true)).stage)}
 @Test fun loginSessionCanRestartLogin() {assertEquals(Stage.LOGIN_REQUIRED,BookingEngine.sessionExpired(prepared().copy(stage=Stage.TRAIN_SELECTION,handedOff=true)).stage)}
 @Test(expected=IllegalArgumentException::class) fun cannotResolveWithoutVerification() {BookingEngine.resolve(prepared(),false,false)}
 @Test(expected=IllegalArgumentException::class) fun invalidPnrRejected() {BookingEngine.resolve(prepared(),true,true,"123")}
 @Test fun verifiedFailureAllowsAnotherAttempt() {val v=BookingEngine.start(vault(),journey,today);val resolved=BookingEngine.resolve(v.attempts.single(),false,true);assertEquals(2,BookingEngine.start(v.copy(attempts=listOf(resolved)),journey,today).attempts.size)}
 @Test fun confirmedSnapshotIsIndependentOfProfiles() {val a=BookingEngine.resolve(prepared(),true,true,"1234567890",status=TicketStatus.RAC);assertEquals(Stage.BOOKED,a.stage);assertEquals(TicketStatus.RAC,a.ticketStatus);assertEquals(passenger,a.passengers.single());assertEquals(a,BookingEngine.recover(a))}
 @Test(expected=IllegalArgumentException::class) fun cannotCancelAfterHandoff() {BookingEngine.cancelPrepared(BookingEngine.handoff(prepared()))}
 @Test fun uncertainNetworkNeverUnlocks() {val a=BookingEngine.recover(prepared().copy(handedOff=true,stage=Stage.PAYMENT_PENDING));assertFalse(a.resolved);assertEquals(Stage.UNKNOWN,a.stage)}
}
