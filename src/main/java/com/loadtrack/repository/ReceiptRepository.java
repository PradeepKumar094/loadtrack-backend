package com.loadtrack.repository;

import com.loadtrack.entity.Receipt;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ReceiptRepository extends JpaRepository<Receipt, Integer> {
    Optional<Receipt> findByReceiptNumber(String receiptNumber);
    List<Receipt> findByPaymentId(Integer paymentId);
}
