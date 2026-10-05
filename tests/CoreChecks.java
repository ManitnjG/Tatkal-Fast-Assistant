import com.tatkal.fastassistant.core.BookingFlow;
import com.tatkal.fastassistant.core.BookingFlow.*;
import java.time.*;
public final class CoreChecks {
 static int checks;
 static void check(boolean b) { checks++; if(!b) throw new AssertionError("Check " + checks); }
 static void blocked(Runnable r) { boolean blocked=false; try { r.run(); } catch(IllegalStateException e) { blocked=true; } check(blocked); }
 static BookingFlow payment() { BookingFlow f=new BookingFlow(); f.report(State.SEARCHING); f.report(State.TRAIN_SELECTED); f.report(State.PASSENGERS_FILLED); f.reviewForPayment(true,10000,20000); f.beginManualPayment(); return f; }
 public static void main(String[] args) {
  BookingFlow f=payment(); check(f.paymentLocked()); blocked(f::beginManualPayment); f.interrupted(); check(f.state()==State.PAYMENT_UNKNOWN);
  BookingFlow restart=new BookingFlow(f.state(), f.payment()); check(restart.paymentLocked()); blocked(()->restart.report(State.SEARCHING)); blocked(()->restart.report(State.BOOKED));
  blocked(()->restart.recordUserCheckedStatus(Payment.FAILED,true,false));
  restart.recordUserCheckedStatus(Payment.PENDING,true,true); check(restart.paymentLocked());
  restart.recordUserCheckedStatus(Payment.SUCCESS,true,true); check(restart.state()!=State.BOOKED); check(restart.paymentLocked());
  BookingFlow failed=payment(); failed.recordUserCheckedStatus(Payment.FAILED,true,true); check(!failed.paymentLocked()); blocked(failed::beginManualPayment);
  failed.report(State.SEARCHING); failed.report(State.TRAIN_SELECTED); failed.report(State.PASSENGERS_FILLED);
  blocked(()->failed.reviewForPayment(false,100,200)); blocked(()->failed.reviewForPayment(true,300,200)); blocked(()->failed.reviewForPayment(true,-1,200));
  failed.reviewForPayment(true,100,200); failed.beginManualPayment(); check(failed.paymentLocked());
  for(State state:State.values()) { BookingFlow locked=new BookingFlow(state,Payment.UNKNOWN); check(locked.paymentLocked()); blocked(()->locked.report(State.FAILED)); }
  check(BookingFlow.opening(LocalDate.of(2026,10,7),"3A").equals(Instant.parse("2026-10-06T04:30:00Z")));
  check(BookingFlow.opening(LocalDate.of(2026,10,7),"SL").equals(Instant.parse("2026-10-06T05:30:00Z")));
  for(int i=0;i<4;i++) check(BookingFlow.retryDelay(i,4,0,true,false,false).getAsLong()==(1000L<<i));
  check(!BookingFlow.retryDelay(4,4,0,true,false,false).isPresent());
  check(!BookingFlow.retryDelay(0,4,0,false,false,false).isPresent());
  check(!BookingFlow.retryDelay(0,4,0,true,true,false).isPresent());
  check(!BookingFlow.retryDelay(0,4,0,true,false,true).isPresent());
  check(BookingFlow.retryDelay(0,4,120000,true,false,false).getAsLong()==120000);
  System.out.println("Passed " + checks + " safety checks");
 }
}
