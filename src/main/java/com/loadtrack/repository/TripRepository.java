package com.loadtrack.repository;

import com.loadtrack.entity.Trip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface TripRepository extends JpaRepository<Trip, Integer> {

    List<Trip> findByDriverId(Integer driverId);

    List<Trip> findByDealerId(Integer dealerId);

    List<Trip> findByTruckId(Integer truckId);

    List<Trip> findByTripDateBetween(LocalDate start, LocalDate end);

    List<Trip> findByStatus(Trip.TripStatus status);

    @Query("SELECT SUM(t.totalAmount) FROM Trip t WHERE MONTH(t.tripDate) = :month AND YEAR(t.tripDate) = :year")
    BigDecimal getMonthlyEarnings(@Param("month") int month, @Param("year") int year);

    @Query("SELECT COUNT(t) FROM Trip t WHERE t.driver.id = :driverId")
    long countByDriverId(@Param("driverId") Integer driverId);
}
