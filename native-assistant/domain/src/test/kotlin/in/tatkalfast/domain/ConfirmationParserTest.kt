package `in`.tatkalfast.domain
import org.junit.Assert.*
import org.junit.Test
class ConfirmationParserTest {
 @Test fun labelledPnr() {assertEquals("1234567890",ConfirmationParser.pnr("PNR No: 1234567890, Train 16866"))}
 @Test fun rejectsPhoneAndOtp() {assertNull(ConfirmationParser.pnr("Mobile 9000000000 OTP 123456"))}
 @Test fun rejectsAmbiguousPnrs() {assertNull(ConfirmationParser.pnr("PNR:1234567890 PNR:9876543210"))}
 @Test fun rejectsLongNumber() {assertNull(ConfirmationParser.pnr("PNR:123456789012"))}
 @Test fun repeatedSamePnrAccepted() {assertEquals("1234567890",ConfirmationParser.pnr("PNR:1234567890 PNR:1234567890"))}
}
