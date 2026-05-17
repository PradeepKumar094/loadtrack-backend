package com.loadtrack.service;

import com.loadtrack.dto.TripRequest;
import com.loadtrack.entity.*;
import com.loadtrack.exception.ResourceNotFoundException;
import com.loadtrack.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class TripService {

    @Autowired private TripRepository tripRepository;
    @Autowired private TruckRepository truckRepository;
    @Autowired private DriverRepository driverRepository;
    @Autowired private DealerRepository dealerRepository;
    @Autowired private SandTypeRepository sandTypeRepository;
    @Autowired private PaymentRepository paymentRepository;
    @Autowired private SettingsRepository settingsRepository;
    @Autowired private PaymentTransactionRepository paymentTransactionRepository;
    @Autowired private ReceiptRepository receiptRepository;
    @Autowired private SandRequestRepository sandRequestRepository;

    public List<Trip> getAllTrips() { return tripRepository.findAll(); }

    public Trip getTripById(Integer id) {
        return tripRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found: " + id));
    }

    public List<Trip> getTripsByDriver(Integer driverId) {
        return tripRepository.findByDriverId(driverId);
    }

    public List<Trip> getTripsByDealer(Integer dealerId) {
        return tripRepository.findByDealerId(dealerId);
    }

    // ── Create Trip ───────────────────────────────────────────────
    @Transactional
    public Trip createTrip(TripRequest request) {
        Trip trip = new Trip();
        mapRequestToTrip(request, trip);

        Settings settings = getSettings();

        // Calculate amounts
        BigDecimal baseAmount = trip.getTons().multiply(trip.getRatePerTon());
        BigDecimal extraCharge = calculateExtraDistanceCharge(request.getDistanceKm(), settings);
        BigDecimal driverExtra = extraCharge.multiply(settings.getDriverExtraSharePct())
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal baseSalary = trip.getDriver().getSalaryPerTrip();

        trip.setTotalAmount(baseAmount.add(extraCharge));
        trip.setExtraDistanceCharge(extraCharge);
        trip.setDriverExtraAmount(driverExtra);
        trip.setDriverSalary(baseSalary.add(driverExtra));
        trip.setStatus(Trip.TripStatus.PENDING);
        trip.setDriverAcknowledged(false);
        trip.setDriverCompleted(false);

        Trip savedTrip = tripRepository.save(trip);

        // Set truck ON_TRIP
        setTruckStatus(savedTrip.getTruck().getId(), Truck.TruckStatus.ON_TRIP);
        assignTruckToDriver(savedTrip.getDriver().getId(), savedTrip.getTruck().getId());

        // Create payment record
        createPaymentForTrip(savedTrip, request.getInitialPayment(), request.getPaymentRemarks(), settings);

        return savedTrip;
    }

    // ── Update Trip ───────────────────────────────────────────────
    @Transactional
    public Trip updateTrip(Integer id, TripRequest request) {
        Trip existing = getTripById(id);
        Integer oldTruckId = existing.getTruck().getId();
        Trip.TripStatus oldStatus = existing.getStatus();

        mapRequestToTrip(request, existing);

        Settings settings = getSettings();
        BigDecimal baseAmount = existing.getTons().multiply(existing.getRatePerTon());
        BigDecimal extraCharge = calculateExtraDistanceCharge(request.getDistanceKm(), settings);
        BigDecimal driverExtra = extraCharge.multiply(settings.getDriverExtraSharePct())
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        existing.setTotalAmount(baseAmount.add(extraCharge));
        existing.setExtraDistanceCharge(extraCharge);
        existing.setDriverExtraAmount(driverExtra);
        existing.setDriverSalary(existing.getDriver().getSalaryPerTrip().add(driverExtra));

        Trip.TripStatus newStatus = existing.getStatus();
        Integer newTruckId = existing.getTruck().getId();

        // Truck management
        if (!oldTruckId.equals(newTruckId)) {
            freeTruckIfNoActiveTrips(oldTruckId, id);
            if (newStatus == Trip.TripStatus.PENDING || newStatus == Trip.TripStatus.ACKNOWLEDGED
                    || newStatus == Trip.TripStatus.IN_PROGRESS) {
                setTruckStatus(newTruckId, Truck.TruckStatus.ON_TRIP);
                assignTruckToDriver(existing.getDriver().getId(), newTruckId);
            }
        }

        if (oldStatus != newStatus) {
            if (newStatus == Trip.TripStatus.COMPLETED || newStatus == Trip.TripStatus.CANCELLED) {
                freeTruckIfNoActiveTrips(newTruckId, id);
                unassignTruckFromDriver(existing.getDriver().getId());
            } else if (newStatus == Trip.TripStatus.PENDING) {
                setTruckStatus(newTruckId, Truck.TruckStatus.ON_TRIP);
                assignTruckToDriver(existing.getDriver().getId(), newTruckId);
            }
        }

        Trip savedTrip = tripRepository.save(existing);
        updatePaymentAmount(savedTrip);

        // If admin marks trip as COMPLETED → mark linked sand request as DELIVERED
        if (oldStatus != Trip.TripStatus.COMPLETED && newStatus == Trip.TripStatus.COMPLETED) {
            markSandRequestDelivered(savedTrip);
        }

        return savedTrip;
    }

    // ── Driver Acknowledges Trip ──────────────────────────────────
    @Transactional
    public Trip acknowledgeTrip(Integer tripId, Integer driverId) {
        Trip trip = getTripById(tripId);

        if (!trip.getDriver().getId().equals(driverId)) {
            throw new RuntimeException("You are not assigned to this trip");
        }
        if (trip.getDriverAcknowledged()) {
            throw new RuntimeException("Trip already acknowledged");
        }
        if (trip.getStatus() == Trip.TripStatus.CANCELLED) {
            throw new RuntimeException("Cannot acknowledge a cancelled trip");
        }

        trip.setDriverAcknowledged(true);
        trip.setAcknowledgedAt(LocalDateTime.now());
        trip.setStatus(Trip.TripStatus.ACKNOWLEDGED);
        return tripRepository.save(trip);
    }

    // ── Driver Marks Trip as Completed ───────────────────────────
    @Transactional
    public Trip driverCompleteTrip(Integer tripId, Integer driverId) {
        Trip trip = getTripById(tripId);

        if (!trip.getDriver().getId().equals(driverId)) {
            throw new RuntimeException("You are not assigned to this trip");
        }
        if (!trip.getDriverAcknowledged()) {
            throw new RuntimeException("Please acknowledge the trip first");
        }
        if (trip.getDriverCompleted()) {
            throw new RuntimeException("Trip already marked as completed");
        }

        trip.setDriverCompleted(true);
        trip.setCompletedAt(LocalDateTime.now());
        trip.setStatus(Trip.TripStatus.COMPLETED);

        // Free truck
        freeTruckIfNoActiveTrips(trip.getTruck().getId(), tripId);
        unassignTruckFromDriver(driverId);

        Trip savedTrip = tripRepository.save(trip);

        // Mark linked sand request as DELIVERED
        markSandRequestDelivered(savedTrip);

        return savedTrip;
    }

    // ── Delete Trip ───────────────────────────────────────────────
    @Transactional
    public void deleteTrip(Integer id) {
        Trip trip = getTripById(id);
        Integer truckId = trip.getTruck().getId();
        tripRepository.delete(trip);
        freeTruckIfNoActiveTrips(truckId, id);
        unassignTruckFromDriver(trip.getDriver().getId());
    }

    // ── Helpers ───────────────────────────────────────────────────

    private BigDecimal calculateExtraDistanceCharge(BigDecimal distanceKm, Settings settings) {
        if (distanceKm == null || distanceKm.compareTo(BigDecimal.ZERO) <= 0) return BigDecimal.ZERO;
        BigDecimal base = settings.getBaseDistanceKm();
        if (distanceKm.compareTo(base) <= 0) return BigDecimal.ZERO;
        BigDecimal extraKm = distanceKm.subtract(base);
        return extraKm.multiply(settings.getExtraChargePerKm()).setScale(2, RoundingMode.HALF_UP);
    }

    private void mapRequestToTrip(TripRequest request, Trip trip) {
        Truck truck = truckRepository.findById(request.getTruckId())
                .orElseThrow(() -> new ResourceNotFoundException("Truck not found"));
        Driver driver = driverRepository.findById(request.getDriverId())
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found"));
        Dealer dealer = dealerRepository.findById(request.getDealerId())
                .orElseThrow(() -> new ResourceNotFoundException("Dealer not found"));
        SandType sandType = sandTypeRepository.findById(request.getSandTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("Sand type not found"));

        // Validate truck capacity
        if (request.getTons() != null && truck.getCapacityTons().compareTo(request.getTons()) < 0) {
            throw new RuntimeException("Truck " + truck.getTruckNumber() + " capacity ("
                    + truck.getCapacityTons() + " tons) is less than requested " + request.getTons() + " tons");
        }

        trip.setTruck(truck);
        trip.setDriver(driver);
        trip.setDealer(dealer);
        trip.setSandType(sandType);
        trip.setTons(request.getTons());
        trip.setSourceLocation(request.getSourceLocation());
        trip.setDestinationLocation(request.getDestinationLocation());
        trip.setDistanceKm(request.getDistanceKm() != null ? request.getDistanceKm() : BigDecimal.ZERO);
        trip.setTripDate(request.getTripDate());
        trip.setRatePerTon(sandType.getPricePerTon());

        if (request.getStatus() != null) {
            trip.setStatus(Trip.TripStatus.valueOf(request.getStatus()));
        }
    }

    private void setTruckStatus(Integer truckId, Truck.TruckStatus status) {
        truckRepository.findById(truckId).ifPresent(t -> { t.setStatus(status); truckRepository.save(t); });
    }

    private void assignTruckToDriver(Integer driverId, Integer truckId) {
        driverRepository.findById(driverId).ifPresent(d ->
            truckRepository.findById(truckId).ifPresent(t -> { d.setAssignedTruck(t); driverRepository.save(d); })
        );
    }

    private void unassignTruckFromDriver(Integer driverId) {
        driverRepository.findById(driverId).ifPresent(d -> { d.setAssignedTruck(null); driverRepository.save(d); });
    }

    /**
     * Marks the sand request linked to this trip as DELIVERED
     */
    private void markSandRequestDelivered(Trip trip) {
        sandRequestRepository.findAll().stream()
                .filter(r -> r.getTrip() != null && r.getTrip().getId().equals(trip.getId()))
                .filter(r -> r.getStatus() == com.loadtrack.entity.SandRequest.RequestStatus.ACCEPTED)
                .forEach(r -> {
                    r.setStatus(com.loadtrack.entity.SandRequest.RequestStatus.DELIVERED);
                    sandRequestRepository.save(r);
                });
    }

    private void freeTruckIfNoActiveTrips(Integer truckId, Integer excludeTripId) {        long active = tripRepository.findByTruckId(truckId).stream()
                .filter(t -> !t.getId().equals(excludeTripId))
                .filter(t -> t.getStatus() == Trip.TripStatus.PENDING
                        || t.getStatus() == Trip.TripStatus.ACKNOWLEDGED
                        || t.getStatus() == Trip.TripStatus.IN_PROGRESS)
                .count();
        if (active == 0) setTruckStatus(truckId, Truck.TruckStatus.AVAILABLE);
    }

    private void updatePaymentAmount(Trip trip) {
        paymentRepository.findByTripId(trip.getId()).ifPresent(payment -> {
            if (payment.getPaymentStatus() == Payment.PaymentStatus.UNPAID) {
                payment.setOriginalAmount(trip.getTotalAmount());
                payment.setFinalAmount(trip.getTotalAmount());
                paymentRepository.save(payment);
            }
        });
    }

    private void createPaymentForTrip(Trip trip, BigDecimal initialPayment, String remarks, Settings settings) {
        if (paymentRepository.findByTripId(trip.getId()).isPresent()) return;

        BigDecimal paid = (initialPayment != null && initialPayment.compareTo(BigDecimal.ZERO) > 0)
                ? initialPayment : BigDecimal.ZERO;

        Payment.PaymentStatus status = paid.compareTo(BigDecimal.ZERO) == 0 ? Payment.PaymentStatus.UNPAID
                : paid.compareTo(trip.getTotalAmount()) >= 0 ? Payment.PaymentStatus.PAID
                : Payment.PaymentStatus.PARTIAL;

        Payment payment = new Payment();
        payment.setTrip(trip);
        payment.setOriginalAmount(trip.getTotalAmount());
        payment.setInterestAmount(BigDecimal.ZERO);
        payment.setFinalAmount(trip.getTotalAmount());
        payment.setPaidAmount(paid);
        payment.setPaymentStatus(status);
        payment.setDueDate(trip.getTripDate().plusDays(settings.getAllowedDays()));
        if (paid.compareTo(BigDecimal.ZERO) > 0) payment.setPaymentDate(trip.getTripDate());

        Payment savedPayment = paymentRepository.save(payment);

        if (paid.compareTo(BigDecimal.ZERO) > 0) {
            com.loadtrack.entity.PaymentTransaction txn = new com.loadtrack.entity.PaymentTransaction();
            txn.setPayment(savedPayment);
            txn.setAmountPaid(paid);
            txn.setPaidAt(java.time.LocalDateTime.now());
            txn.setBalanceAfter(trip.getTotalAmount().subtract(paid).max(BigDecimal.ZERO));
            txn.setRemarks(remarks != null ? remarks : "Initial payment at trip creation");
            paymentTransactionRepository.save(txn);
        }

        if (status == Payment.PaymentStatus.PAID) {
            com.loadtrack.entity.Receipt receipt = new com.loadtrack.entity.Receipt();
            receipt.setPayment(savedPayment);
            receipt.setReceiptNumber("RCP-" + java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase());
            receiptRepository.save(receipt);
        }
    }

    private Settings getSettings() {
        return settingsRepository.findAll().stream().findFirst()
                .orElseGet(() -> {
                    Settings s = new Settings();
                    return settingsRepository.save(s);
                });
    }
}
