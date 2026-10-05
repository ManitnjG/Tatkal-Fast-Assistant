package com.tatkal.fastassistant.core;

import java.time.*;
import java.util.*;

/** Local companion state. No network, form injection, credential access or payment submission. */
public final class BookingFlow {
    public enum State { READY, SEARCHING, TRAIN_SELECTED, PASSENGERS_FILLED, CAPTCHA_REQUIRED,
        OTP_REQUIRED, PAYMENT_REQUIRED, PAYMENT_PENDING, PAYMENT_UNKNOWN, BOOKING_PROCESSING,
        SERVER_UNAVAILABLE, SESSION_EXPIRED, BOOKED, FAILED }
    public enum Payment { NONE, PENDING, UNKNOWN, FAILED, SUCCESS }
    private State state;
    private Payment payment;
    public BookingFlow() { this(State.READY, Payment.NONE); }
    public BookingFlow(State state, Payment payment) {
        this.payment = Objects.requireNonNull(payment);
        // Restart never assumes a browser action succeeded; settlement must be checked manually.
        this.state = payment == Payment.PENDING || payment == Payment.UNKNOWN || payment == Payment.SUCCESS
            ? State.PAYMENT_UNKNOWN : state == State.BOOKED ? State.BOOKING_PROCESSING : state;
    }
    public State state() { return state; }
    public Payment payment() { return payment; }
    public boolean paymentLocked() { return payment == Payment.PENDING || payment == Payment.UNKNOWN || payment == Payment.SUCCESS; }
    public void report(State next) {
        if (next == State.BOOKED) throw new IllegalStateException("Official verification is unavailable; cannot claim BOOKED.");
        if (paymentLocked() && next != State.SESSION_EXPIRED && next != State.SERVER_UNAVAILABLE &&
            next != State.PAYMENT_UNKNOWN && next != State.BOOKING_PROCESSING && next != State.OTP_REQUIRED && next != State.CAPTCHA_REQUIRED)
            throw new IllegalStateException("Check official transaction status before another attempt.");
        if (next == State.PAYMENT_PENDING || next == State.PAYMENT_REQUIRED)
            throw new IllegalStateException("Use the reviewed payment flow.");
        boolean allowed = next == State.SESSION_EXPIRED || next == State.SERVER_UNAVAILABLE ||
            next == State.CAPTCHA_REQUIRED || next == State.OTP_REQUIRED || next == State.FAILED ||
            (next == State.SEARCHING && (state == State.READY || state == State.FAILED || state == State.SESSION_EXPIRED || state == State.SERVER_UNAVAILABLE || state == State.CAPTCHA_REQUIRED || state == State.OTP_REQUIRED)) ||
            (next == State.TRAIN_SELECTED && state == State.SEARCHING) ||
            (next == State.PASSENGERS_FILLED && state == State.TRAIN_SELECTED);
        if (!allowed) throw new IllegalStateException("Resume from the last verified manual step.");
        state = next;
    }
    public void reviewForPayment(boolean explicitlyConfirmed, long farePaise, long maxPaise) {
        if (state != State.PASSENGERS_FILLED || paymentLocked() || !explicitlyConfirmed || farePaise <= 0 || farePaise > maxPaise)
            throw new IllegalStateException("Review passengers, train, availability and fare before payment.");
        state = State.PAYMENT_REQUIRED;
    }
    public void beginManualPayment() {
        if (state != State.PAYMENT_REQUIRED || paymentLocked()) throw new IllegalStateException("Payment is blocked.");
        // Caller MUST commit this state durably before handing control to official IRCTC.
        payment = Payment.PENDING;
        state = State.PAYMENT_PENDING;
    }
    public void interrupted() {
        state = paymentLocked() ? State.PAYMENT_UNKNOWN : State.SERVER_UNAVAILABLE;
        if (paymentLocked()) payment = Payment.UNKNOWN;
    }
    public void recordUserCheckedStatus(Payment result, boolean checkedBookingHistory, boolean checkedTransactionHistory) {
        if (!paymentLocked() || !checkedBookingHistory || !checkedTransactionHistory || result == Payment.NONE)
            throw new IllegalStateException("Check both official booking and transaction histories first.");
        payment = result;
        // A user report is not machine-verified booking confirmation.
        state = result == Payment.FAILED ? State.FAILED : result == Payment.PENDING ? State.PAYMENT_PENDING :
            result == Payment.SUCCESS ? State.BOOKING_PROCESSING : State.PAYMENT_UNKNOWN;
    }
    public static Instant opening(LocalDate originDeparture, String travelClass) {
        Set<String> ac = new HashSet<>(Arrays.asList("2A", "3A", "3E", "CC", "EC"));
        Set<String> nonAc = new HashSet<>(Arrays.asList("SL", "2S", "FC"));
        if (!ac.contains(travelClass) && !nonAc.contains(travelClass)) throw new IllegalArgumentException("Unsupported Tatkal class");
        return originDeparture.minusDays(1).atTime(ac.contains(travelClass) ? 10 : 11, 0).atZone(ZoneId.of("Asia/Kolkata")).toInstant();
    }
    /** Only for a future permitted read-only integration. Security/queue responses never retry. */
    public static OptionalLong retryDelay(int attempt, int maximum, long retryAfterMillis,
                                           boolean readOnly, boolean securityOrQueue, boolean paymentLocked) {
        if (!readOnly || securityOrQueue || paymentLocked || attempt < 0 || attempt >= Math.min(4, Math.max(0, maximum)))
            return OptionalLong.empty();
        return OptionalLong.of(Math.max(1000L << attempt, Math.max(0, retryAfterMillis)));
    }
}
