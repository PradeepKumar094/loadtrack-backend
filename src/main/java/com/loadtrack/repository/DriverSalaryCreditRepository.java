package com.loadtrack.repository;

import com.loadtrack.entity.DriverSalaryCredit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface DriverSalaryCreditRepository extends JpaRepository<DriverSalaryCredit, Integer> {

    List<DriverSalaryCredit> findByDriverIdOrderByCreditedAtDesc(Integer driverId);

    @Query("SELECT COALESCE(SUM(c.amount), 0) FROM DriverSalaryCredit c WHERE c.driver.id = :driverId")
    BigDecimal getTotalCreditedByDriver(@Param("driverId") Integer driverId);
}
