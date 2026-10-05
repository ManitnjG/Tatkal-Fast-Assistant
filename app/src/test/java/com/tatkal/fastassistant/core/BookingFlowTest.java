package com.tatkal.fastassistant.core;
import org.junit.Test;
import static org.junit.Assert.*;
public class BookingFlowTest {
 @Test public void unknownPaymentCannotRestart() {
  BookingFlow f=new BookingFlow(BookingFlow.State.SESSION_EXPIRED,BookingFlow.Payment.UNKNOWN);
  assertTrue(f.paymentLocked());
  assertThrows(IllegalStateException.class, () -> f.report(BookingFlow.State.SEARCHING));
  assertThrows(IllegalStateException.class, () -> f.report(BookingFlow.State.BOOKED));
 }
 @Test public void queuesNeverRetry() { assertFalse(BookingFlow.retryDelay(0,4,0,true,true,false).isPresent()); }
}
