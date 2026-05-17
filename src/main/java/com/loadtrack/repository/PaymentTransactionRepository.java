package com.loadtrack.repository;

import com.loadtrack.entity.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Integer> {
    List<PaymentTransaction> findByPaymentIdOrderByPaidAtAsc(Integer paymentId);
}
