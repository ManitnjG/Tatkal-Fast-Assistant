package `in`.tatkalfast.domain
import org.junit.Assert.*
import org.junit.Test
class AutofillPolicyTest {
 private val p=Passenger(name="Example Person")
 private val a=Attempt(journey=Journey(from="TJ",to="MS",mobile="9000000000"),passengers=listOf(p),payment=Payment.IRCTC_EWALLET,handedOff=true,stage=Stage.PASSENGER_DETAILS)
 @Test fun exactOfficialHttpsOnly() {assertTrue(AutofillPolicy.trustedOrigin("www.irctc.co.in","https"));assertFalse(AutofillPolicy.trustedOrigin("www.irctc.co.in.evil.test","https"));assertFalse(AutofillPolicy.trustedOrigin("irctc.co.in","http"));assertFalse(AutofillPolicy.trustedOrigin("payments.irctc.co.in","https"))}
 @Test fun recognizesPassengerName() {assertEquals(PreparedField.PASSENGER_NAME,AutofillPolicy.classify(listOf("Passenger Name")))}
 @Test fun recognizesAgeAndStation() {assertEquals(PreparedField.PASSENGER_AGE,AutofillPolicy.classify(listOf("passengerAge")));assertEquals(PreparedField.FROM,AutofillPolicy.classify(listOf("fromStation")))}
 @Test fun rejectsCaptchaEvenIfFieldClaimsName() {assertNull(AutofillPolicy.classify(listOf("passengerName","captcha")))}
 @Test fun rejectsOtpEvenIfFieldClaimsPhone() {assertNull(AutofillPolicy.classify(listOf("mobileNumber","OTP Verification")))}
 @Test fun rejectsPaymentSecretsAndPasswords() {for(secret in listOf("UPI PIN","card number","CVV","password","Aadhaar","wallet","transaction")) assertNull(AutofillPolicy.classify(listOf("passengerName",secret)));assertNull(AutofillPolicy.classify(listOf("passengerName"),true))}
 @Test fun rejectsUnknownAndConflictingFields() {assertNull(AutofillPolicy.classify(listOf("name")));assertNull(AutofillPolicy.classify(listOf("passengerName","passengerAge")))}
 @Test fun onlySelectedSnapshotPassenger() {assertEquals(p.name,AutofillPolicy.value(PreparedField.PASSENGER_NAME,a,p.id));assertNull(AutofillPolicy.value(PreparedField.PASSENGER_NAME,a,"not-in-attempt"))}
 @Test fun pendingUnknownAndResolvedNeverFill() {for(stage in listOf(Stage.PAYMENT,Stage.PAYMENT_PENDING,Stage.UNKNOWN,Stage.BOOKED,Stage.FAILED)) assertFalse(AutofillPolicy.canFill(a.copy(stage=stage)));assertFalse(AutofillPolicy.canFill(a.copy(resolved=true)));assertFalse(AutofillPolicy.canFill(a.copy(handedOff=false)))}
 @Test fun agePhoneAndRouteValues() {assertEquals("30",AutofillPolicy.value(PreparedField.PASSENGER_AGE,a,p.id));assertEquals("9000000000",AutofillPolicy.value(PreparedField.MOBILE,a,null));assertEquals("TJ",AutofillPolicy.value(PreparedField.FROM,a,null))}
 @Test fun paidWithoutBookingCannotUnlock() {assertThrows(IllegalArgumentException::class.java) {BookingEngine.resolveVerified(a,false,true,true,PaymentCheck.PAID)}}
 @Test fun pendingOrUnknownCannotUnlock() {for(check in listOf(PaymentCheck.PENDING_CHECK,PaymentCheck.UNKNOWN_CHECK)) assertThrows(IllegalArgumentException::class.java) {BookingEngine.resolveVerified(a,false,true,true,check)}}
 @Test fun bothHistoryAndPaymentMustBeChecked() {assertThrows(IllegalArgumentException::class.java) {BookingEngine.resolveVerified(a,false,true,false,PaymentCheck.FAILED_PAYMENT)}}
 @Test fun terminalReconciliationPersistsEvidence() {val result=BookingEngine.resolveVerified(a,false,true,true,PaymentCheck.REFUNDED);assertTrue(result.resolved);assertEquals(PaymentCheck.REFUNDED,result.paymentCheck);assertNotNull(result.verifiedAt)}
 @Test fun confirmedPaidBookingRequiresPnr() {assertThrows(IllegalArgumentException::class.java) {BookingEngine.resolveVerified(a,true,true,true,PaymentCheck.PAID,"123")};assertEquals(Stage.BOOKED,BookingEngine.resolveVerified(a,true,true,true,PaymentCheck.PAID,"1234567890").stage)}
}
