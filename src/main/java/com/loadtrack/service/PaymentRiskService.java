package com.loadtrack.service;

import com.loadtrack.entity.Dealer;
import com.loadtrack.entity.Payment;
import com.loadtrack.repository.DealerRepository;
import com.loadtrack.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PaymentRiskService {

    @Autowired private DealerRepository dealerRepository;
    @Autowired private PaymentRepository paymentRepository;

    public enum RiskLevel { LOW, MEDIUM, HIGH }

    public static class DealerRisk {
        public Integer dealerId;
        public String dealerName;
        public RiskLevel riskLevel;
        public int riskScore;
        public String riskReason;
        public BigDecimal totalPending;
        public int overdueCount;
        public int totalPayments;
        public int latePayments;
    }

    public List<DealerRisk> getAllDealerRisks() {
        return dealerRepository.findAll().stream()
                .map(this::calculateRisk)
                .collect(Collectors.toList());
    }

    public DealerRisk getDealerRisk(Integer dealerId) {
        Dealer dealer = dealerRepository.findById(dealerId)
                .orElseThrow(() -> new RuntimeException("Dealer not found"));
        return calculateRisk(dealer);
    }

    private DealerRisk calculateRisk(Dealer dealer) {
        List<Payment> payments = paymentRepository.findByTripDealerId(dealer.getId());

        DealerRisk risk = new DealerRisk();
        risk.dealerId = dealer.getId();
        risk.dealerName = dealer.getName();
        risk.totalPayments = payments.size();

        if (payments.isEmpty()) {
            risk.riskLevel = RiskLevel.LOW;
            risk.riskScore = 0;
            risk.riskReason = "No payment history";
            risk.totalPending = BigDecimal.ZERO;
            risk.overdueCount = 0;
            risk.latePayments = 0;
            return risk;
        }

        int score = 0;
        StringBuilder reasons = new StringBuilder();
        LocalDate today = LocalDate.now();

        // 1. Count overdue payments
        long overdue = payments.stream()
                .filter(p -> p.getDueDate() != null
                        && p.getDueDate().isBefore(today)
                        && p.getPaymentStatus() != Payment.PaymentStatus.PAID)
                .count();
        risk.overdueCount = (int) overdue;

        if (overdue >= 3) { score += 40; reasons.append("Multiple overdue payments. "); }
        else if (overdue == 2) { score += 25; reasons.append("2 overdue payments. "); }
        else if (overdue == 1) { score += 15; reasons.append("1 overdue payment. "); }

        // 2. Average days overdue
        double avgDaysOverdue = payments.stream()
                .filter(p -> p.getDueDate() != null
                        && p.getDueDate().isBefore(today)
                        && p.getPaymentStatus() != Payment.PaymentStatus.PAID)
                .mapToLong(p -> ChronoUnit.DAYS.between(p.getDueDate(), today))
                .average().orElse(0);

        if (avgDaysOverdue > 30) { score += 25; reasons.append("Avg ").append((int)avgDaysOverdue).append(" days overdue. "); }
        else if (avgDaysOverdue > 15) { score += 15; reasons.append("Avg ").append((int)avgDaysOverdue).append(" days overdue. "); }
        else if (avgDaysOverdue > 7)  { score += 8; }

        // 3. Unpaid ratio
        long unpaidCount = payments.stream()
                .filter(p -> p.getPaymentStatus() == Payment.PaymentStatus.UNPAID)
                .count();
        double unpaidRatio = (double) unpaidCount / payments.size();

        if (unpaidRatio > 0.5) { score += 20; reasons.append("More than 50% unpaid. "); }
        else if (unpaidRatio > 0.3) { score += 10; reasons.append("30%+ unpaid. "); }

        // 4. Total pending amount
        BigDecimal totalPending = payments.stream()
                .filter(p -> p.getPaymentStatus() != Payment.PaymentStatus.PAID)
                .map(p -> p.getFinalAmount().subtract(p.getPaidAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        risk.totalPending = totalPending;

        if (totalPending.compareTo(new BigDecimal("50000")) > 0) { score += 15; reasons.append("High pending ₹").append(totalPending.intValue()).append(". "); }
        else if (totalPending.compareTo(new BigDecimal("20000")) > 0) { score += 8; }

        // 5. Interest applied (means they paid late before)
        long interestCount = payments.stream()
                .filter(p -> p.getInterestAmount().compareTo(BigDecimal.ZERO) > 0)
                .count();
        risk.latePayments = (int) interestCount;

        if (interestCount >= 2) { score += 10; reasons.append("Interest applied ").append(interestCount).append(" times. "); }

        // Determine risk level
        risk.riskScore = Math.min(score, 100);
        if (score >= 50) {
            risk.riskLevel = RiskLevel.HIGH;
            if (reasons.length() == 0) reasons.append("High risk based on payment pattern.");
        } else if (score >= 25) {
            risk.riskLevel = RiskLevel.MEDIUM;
            if (reasons.length() == 0) reasons.append("Moderate risk based on payment pattern.");
        } else {
            risk.riskLevel = RiskLevel.LOW;
            if (reasons.length() == 0) reasons.append("Good payment history.");
        }

        risk.riskReason = reasons.toString().trim();
        return risk;
    }
}
