package com.loadtrack.service;

import com.loadtrack.dto.PaymentRequest;
import com.loadtrack.entity.*;
import com.loadtrack.exception.ResourceNotFoundException;
import com.loadtrack.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
public class PaymentService {

    @Autowired private PaymentRepository paymentRepository;
    @Autowired private SettingsRepository settingsRepository;
    @Autowired private ReceiptRepository receiptRepository;
    @Autowired private PaymentTransactionRepository transactionRepository;

    public List<Payment> getAllPayments() {
        return paymentRepository.findAll();
    }

    public Payment getPaymentById(Integer id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with id: " + id));
    }

    public List<Payment> getPaymentsByDealer(Integer dealerId) {
        return paymentRepository.findByTripDealerId(dealerId);
    }

    public Payment getPaymentByTrip(Integer tripId) {
        return paymentRepository.findByTripId(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found for trip: " + tripId));
    }

    // Get full transaction log for a payment
    public List<PaymentTransaction> getTransactionLog(Integer paymentId) {
        return transactionRepository.findByPaymentIdOrderByPaidAtAsc(paymentId);
    }

    @Transactional
    public Payment makePayment(Integer paymentId, PaymentRequest request) {
        Payment payment = getPaymentById(paymentId);

        if (payment.getPaymentStatus() == Payment.PaymentStatus.PAID) {
            throw new RuntimeException("Payment is already fully paid");
        }
        if (Boolean.TRUE.equals(payment.getDealerPaidPending())) {
            throw new RuntimeException("You already have a payment pending admin verification. Please wait.");
        }

        applyInterestIfDelayed(payment);

        BigDecimal remaining = payment.getFinalAmount().subtract(payment.getPaidAmount());

        if (request.getPaidAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Paid amount must be greater than 0");
        }
        if (request.getPaidAmount().compareTo(remaining) > 0) {
            throw new RuntimeException("Amount (₹" + request.getPaidAmount()
                    + ") cannot exceed remaining due (₹" + remaining + ")");
        }

        // Store as pending — do NOT update paidAmount yet
        payment.setDealerPaidPending(true);
        payment.setAdminVerified(false);
        payment.setPendingVerificationAmount(request.getPaidAmount());

        // Log the pending transaction (with a note)
        Payment savedPayment = paymentRepository.save(payment);

        PaymentTransaction txn = new PaymentTransaction();
        txn.setPayment(savedPayment);
        txn.setAmountPaid(request.getPaidAmount());
        txn.setPaidAt(LocalDateTime.now());
        txn.setBalanceAfter(remaining.subtract(request.getPaidAmount()).max(BigDecimal.ZERO));
        txn.setRemarks("[PENDING VERIFICATION] " + (request.getRemarks() != null ? request.getRemarks() : "Payment submitted by dealer"));
        transactionRepository.save(txn);

        return savedPayment;
    }

    // Admin verifies dealer payment → now update paidAmount
    @Transactional
    public Payment adminVerifyPayment(Integer paymentId) {
        Payment payment = getPaymentById(paymentId);
        if (!Boolean.TRUE.equals(payment.getDealerPaidPending())) {
            throw new RuntimeException("No pending payment to verify");
        }

        BigDecimal verifiedAmount = payment.getPendingVerificationAmount();
        BigDecimal newPaidAmount = payment.getPaidAmount().add(verifiedAmount);
        BigDecimal balanceAfter = payment.getFinalAmount().subtract(newPaidAmount);

        // Now actually update paidAmount
        payment.setPaidAmount(newPaidAmount);
        payment.setPaymentDate(LocalDate.now());
        payment.setAdminVerified(true);
        payment.setDealerPaidPending(false);
        payment.setPendingVerificationAmount(BigDecimal.ZERO);

        if (balanceAfter.compareTo(BigDecimal.ZERO) <= 0) {
            payment.setPaymentStatus(Payment.PaymentStatus.PAID);
        } else {
            payment.setPaymentStatus(Payment.PaymentStatus.PARTIAL);
        }

        Payment savedPayment = paymentRepository.save(payment);

        // Update the pending transaction log entry to verified
        List<PaymentTransaction> txns = transactionRepository.findByPaymentIdOrderByPaidAtAsc(paymentId);
        txns.stream()
            .filter(t -> t.getRemarks() != null && t.getRemarks().startsWith("[PENDING VERIFICATION]"))
            .reduce((first, second) -> second) // get last one
            .ifPresent(t -> {
                t.setRemarks(t.getRemarks().replace("[PENDING VERIFICATION] ", ""));
                transactionRepository.save(t);
            });

        // Generate receipt if fully paid
        if (savedPayment.getPaymentStatus() == Payment.PaymentStatus.PAID) {
            generateReceipt(savedPayment);
        }
        return savedPayment;
    }

    // Admin rejects dealer payment → clear pending, restore state
    @Transactional
    public Payment adminRejectPayment(Integer paymentId, String reason) {
        Payment payment = getPaymentById(paymentId);
        if (!Boolean.TRUE.equals(payment.getDealerPaidPending())) {
            throw new RuntimeException("No pending payment to reject");
        }

        // Remove the pending transaction log entry
        List<PaymentTransaction> txns = transactionRepository.findByPaymentIdOrderByPaidAtAsc(paymentId);
        txns.stream()
            .filter(t -> t.getRemarks() != null && t.getRemarks().startsWith("[PENDING VERIFICATION]"))
            .reduce((first, second) -> second)
            .ifPresent(transactionRepository::delete);

        payment.setDealerPaidPending(false);
        payment.setAdminVerified(false);
        payment.setPendingVerificationAmount(BigDecimal.ZERO);

        return paymentRepository.save(payment);
    }

    // Get all payments pending admin verification
    public List<Payment> getPendingVerification() {
        return paymentRepository.findAll().stream()
                .filter(p -> Boolean.TRUE.equals(p.getDealerPaidPending()))
                .collect(java.util.stream.Collectors.toList());
    }

    private void applyInterestIfDelayed(Payment payment) {
        if (payment.getDueDate() == null) return;

        LocalDate today = LocalDate.now();
        if (today.isAfter(payment.getDueDate())) {
            Settings settings = settingsRepository.findAll().stream().findFirst().orElse(null);
            if (settings == null) return;

            BigDecimal interestRate = settings.getInterestRatePercent()
                    .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
            BigDecimal interest = payment.getOriginalAmount().multiply(interestRate);

            // Only update if interest not already applied
            if (payment.getInterestAmount().compareTo(BigDecimal.ZERO) == 0) {
                payment.setInterestAmount(interest);
                payment.setFinalAmount(payment.getOriginalAmount().add(interest));
            }
        }
    }

    private void generateReceipt(Payment payment) {
        // Avoid duplicate receipts
        if (!receiptRepository.findByPaymentId(payment.getId()).isEmpty()) return;

        Receipt receipt = new Receipt();
        receipt.setPayment(payment);
        receipt.setReceiptNumber("RCP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        receiptRepository.save(receipt);
    }
}
