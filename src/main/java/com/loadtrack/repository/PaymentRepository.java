package com.loadtrack.repository;

import com.loadtrack.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Integer> {

    Optional<Payment> findByTripId(Integer tripId);

    List<Payment> findByPaymentStatus(Payment.PaymentStatus status);

    List<Payment> findByTripDealerId(Integer dealerId);

    @Query("SELECT SUM(p.finalAmount - p.paidAmount) FROM Payment p WHERE p.trip.dealer.id = :dealerId AND p.paymentStatus != 'PAID'")
    BigDecimal getPendingAmountByDealer(@Param("dealerId") Integer dealerId);

    @Query("SELECT COUNT(p) FROM Payment p WHERE p.paymentStatus != 'PAID'")
    long countPendingPayments();
}
