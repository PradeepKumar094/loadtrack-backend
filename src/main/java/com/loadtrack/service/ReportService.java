package com.loadtrack.service;

import com.loadtrack.dto.DealerReportDto;
import com.loadtrack.dto.DriverReportDto;
import com.loadtrack.entity.*;
import com.loadtrack.repository.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ReportService {

    @Autowired private TripRepository tripRepository;
    @Autowired private PaymentRepository paymentRepository;
    @Autowired private DriverRepository driverRepository;
    @Autowired private DealerRepository dealerRepository;

    // ── Driver Report ─────────────────────────────────────────────

    public List<DriverReportDto> getAllDriverReports() {
        return driverRepository.findAll().stream()
                .map(this::buildDriverReport)
                .collect(Collectors.toList());
    }

    public DriverReportDto getDriverReport(Integer driverId) {
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new RuntimeException("Driver not found"));
        return buildDriverReport(driver);
    }

    private DriverReportDto buildDriverReport(Driver driver) {
        List<Trip> trips = tripRepository.findByDriverId(driver.getId());

        long completed  = trips.stream().filter(t -> t.getStatus() == Trip.TripStatus.COMPLETED).count();
        long pending    = trips.stream().filter(t -> t.getStatus() == Trip.TripStatus.PENDING).count();
        long cancelled  = trips.stream().filter(t -> t.getStatus() == Trip.TripStatus.CANCELLED).count();

        BigDecimal totalTons = trips.stream()
                .map(Trip::getTons)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Salary earned = completed trips × salary per trip
        BigDecimal salaryEarned = driver.getSalaryPerTrip()
                .multiply(BigDecimal.valueOf(completed));

        List<DriverReportDto.TripSummary> tripSummaries = trips.stream()
                .map(t -> DriverReportDto.TripSummary.builder()
                        .tripId(t.getId())
                        .tripDate(t.getTripDate().toString())
                        .truckNumber(t.getTruck().getTruckNumber())
                        .dealerName(t.getDealer().getName())
                        .sandType(t.getSandType().getName())
                        .tons(t.getTons())
                        .source(t.getSourceLocation())
                        .destination(t.getDestinationLocation())
                        .totalAmount(t.getTotalAmount())
                        .status(t.getStatus().name())
                        .salaryForTrip(t.getStatus() == Trip.TripStatus.COMPLETED
                                ? driver.getSalaryPerTrip() : BigDecimal.ZERO)
                        .build())
                .collect(Collectors.toList());

        return DriverReportDto.builder()
                .driverId(driver.getId())
                .driverName(driver.getName())
                .phone(driver.getPhone())
                .licenseNumber(driver.getLicenseNumber())
                .salaryPerTrip(driver.getSalaryPerTrip())
                .assignedTruck(driver.getAssignedTruck() != null
                        ? driver.getAssignedTruck().getTruckNumber() : "—")
                .totalTrips(trips.size())
                .completedTrips(completed)
                .pendingTrips(pending)
                .cancelledTrips(cancelled)
                .totalTonsCarried(totalTons)
                .totalSalaryEarned(salaryEarned)
                .salaryCredited(BigDecimal.ZERO)
                .salaryPending(salaryEarned)
                .trips(tripSummaries)
                .build();
    }

    // ── Dealer Report ─────────────────────────────────────────────

    public List<DealerReportDto> getAllDealerReports() {
        return dealerRepository.findAll().stream()
                .map(this::buildDealerReport)
                .collect(Collectors.toList());
    }

    public DealerReportDto getDealerReport(Integer dealerId) {
        Dealer dealer = dealerRepository.findById(dealerId)
                .orElseThrow(() -> new RuntimeException("Dealer not found"));
        return buildDealerReport(dealer);
    }

    private DealerReportDto buildDealerReport(Dealer dealer) {
        List<Trip> trips = tripRepository.findByDealerId(dealer.getId());

        long completed = trips.stream().filter(t -> t.getStatus() == Trip.TripStatus.COMPLETED).count();
        long pending   = trips.stream().filter(t -> t.getStatus() == Trip.TripStatus.PENDING).count();

        BigDecimal totalTons = trips.stream()
                .map(Trip::getTons)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalBilled  = BigDecimal.ZERO;
        BigDecimal totalPaid    = BigDecimal.ZERO;
        BigDecimal totalInterest = BigDecimal.ZERO;

        List<DealerReportDto.TripPaymentSummary> summaries = new java.util.ArrayList<>();

        for (Trip trip : trips) {
            Optional<Payment> payOpt = paymentRepository.findByTripId(trip.getId());

            BigDecimal billed   = trip.getTotalAmount();
            BigDecimal paid     = BigDecimal.ZERO;
            BigDecimal interest = BigDecimal.ZERO;
            BigDecimal pending2 = billed;
            String payStatus    = "NO PAYMENT";
            String dueDate      = "—";

            if (payOpt.isPresent()) {
                Payment pay = payOpt.get();
                paid     = pay.getPaidAmount();
                interest = pay.getInterestAmount();
                billed   = pay.getFinalAmount();
                pending2 = billed.subtract(paid);
                payStatus = pay.getPaymentStatus().name();
                dueDate  = pay.getDueDate() != null ? pay.getDueDate().toString() : "—";
            }

            totalBilled   = totalBilled.add(billed);
            totalPaid     = totalPaid.add(paid);
            totalInterest = totalInterest.add(interest);

            summaries.add(DealerReportDto.TripPaymentSummary.builder()
                    .tripId(trip.getId())
                    .tripDate(trip.getTripDate().toString())
                    .truckNumber(trip.getTruck().getTruckNumber())
                    .driverName(trip.getDriver().getName())
                    .sandType(trip.getSandType().getName())
                    .tons(trip.getTons())
                    .tripAmount(trip.getTotalAmount())
                    .paidAmount(paid)
                    .pendingAmount(pending2)
                    .interestAmount(interest)
                    .paymentStatus(payStatus)
                    .dueDate(dueDate)
                    .tripStatus(trip.getStatus().name())
                    .build());
        }

        BigDecimal totalPending = totalBilled.subtract(totalPaid);

        return DealerReportDto.builder()
                .dealerId(dealer.getId())
                .dealerName(dealer.getName())
                .phone(dealer.getPhone())
                .address(dealer.getAddress() != null ? dealer.getAddress() : "—")
                .totalTrips(trips.size())
                .completedTrips(completed)
                .pendingTrips(pending)
                .totalTonsReceived(totalTons)
                .totalBilled(totalBilled)
                .totalPaid(totalPaid)
                .totalPending(totalPending)
                .totalInterest(totalInterest)
                .trips(summaries)
                .build();
    }

    // ── Excel Exports ─────────────────────────────────────────────

    public byte[] exportTripsToExcel(LocalDate from, LocalDate to) throws IOException {
        List<Trip> trips = (from != null && to != null)
                ? tripRepository.findByTripDateBetween(from, to)
                : tripRepository.findAll();

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Trips Report");
            String[] columns = {"Trip ID","Truck","Driver","Dealer","Sand Type",
                    "Tons","Source","Destination","Date","Rate/Ton","Total Amount","Status"};
            createHeaderRow(workbook, sheet, columns);

            int rowNum = 1;
            for (Trip trip : trips) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(trip.getId());
                row.createCell(1).setCellValue(trip.getTruck().getTruckNumber());
                row.createCell(2).setCellValue(trip.getDriver().getName());
                row.createCell(3).setCellValue(trip.getDealer().getName());
                row.createCell(4).setCellValue(trip.getSandType().getName());
                row.createCell(5).setCellValue(trip.getTons().doubleValue());
                row.createCell(6).setCellValue(trip.getSourceLocation());
                row.createCell(7).setCellValue(trip.getDestinationLocation());
                row.createCell(8).setCellValue(trip.getTripDate().toString());
                row.createCell(9).setCellValue(trip.getRatePerTon().doubleValue());
                row.createCell(10).setCellValue(trip.getTotalAmount().doubleValue());
                row.createCell(11).setCellValue(trip.getStatus().name());
            }
            autoSize(sheet, columns.length);
            return toBytes(workbook);
        }
    }

    public byte[] exportPendingPaymentsToExcel() throws IOException {
        List<Payment> payments = paymentRepository.findByPaymentStatus(Payment.PaymentStatus.UNPAID);
        payments.addAll(paymentRepository.findByPaymentStatus(Payment.PaymentStatus.PARTIAL));

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Pending Payments");
            String[] columns = {"Payment ID","Trip ID","Dealer","Original Amount",
                    "Interest","Final Amount","Paid Amount","Due Date","Status"};
            createHeaderRow(workbook, sheet, columns);

            int rowNum = 1;
            for (Payment p : payments) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(p.getId());
                row.createCell(1).setCellValue(p.getTrip().getId());
                row.createCell(2).setCellValue(p.getTrip().getDealer().getName());
                row.createCell(3).setCellValue(p.getOriginalAmount().doubleValue());
                row.createCell(4).setCellValue(p.getInterestAmount().doubleValue());
                row.createCell(5).setCellValue(p.getFinalAmount().doubleValue());
                row.createCell(6).setCellValue(p.getPaidAmount().doubleValue());
                row.createCell(7).setCellValue(p.getDueDate() != null ? p.getDueDate().toString() : "");
                row.createCell(8).setCellValue(p.getPaymentStatus().name());
            }
            autoSize(sheet, columns.length);
            return toBytes(workbook);
        }
    }

    public byte[] exportDriverReportToExcel(Integer driverId) throws IOException {
        DriverReportDto report = driverId != null
                ? getDriverReport(driverId)
                : null;
        List<DriverReportDto> reports = driverId != null
                ? List.of(report)
                : getAllDriverReports();

        try (Workbook workbook = new XSSFWorkbook()) {
            for (DriverReportDto dr : reports) {
                String sheetName = dr.getDriverName().replaceAll("[^a-zA-Z0-9]", "_");
                if (sheetName.length() > 31) sheetName = sheetName.substring(0, 31);
                Sheet sheet = workbook.createSheet(sheetName);

                // Summary section
                int r = 0;
                Row titleRow = sheet.createRow(r++);
                titleRow.createCell(0).setCellValue("DRIVER REPORT — " + dr.getDriverName());

                sheet.createRow(r++); // blank
                addSummaryRow(sheet, r++, "Driver Name",     dr.getDriverName());
                addSummaryRow(sheet, r++, "Phone",           dr.getPhone());
                addSummaryRow(sheet, r++, "License Number",  dr.getLicenseNumber());
                addSummaryRow(sheet, r++, "Salary Per Trip", "₹" + dr.getSalaryPerTrip());
                addSummaryRow(sheet, r++, "Assigned Truck",  dr.getAssignedTruck());
                sheet.createRow(r++);
                addSummaryRow(sheet, r++, "Total Trips",     String.valueOf(dr.getTotalTrips()));
                addSummaryRow(sheet, r++, "Completed Trips", String.valueOf(dr.getCompletedTrips()));
                addSummaryRow(sheet, r++, "Pending Trips",   String.valueOf(dr.getPendingTrips()));
                addSummaryRow(sheet, r++, "Cancelled Trips", String.valueOf(dr.getCancelledTrips()));
                addSummaryRow(sheet, r++, "Total Tons Carried", dr.getTotalTonsCarried().toString());
                sheet.createRow(r++);
                addSummaryRow(sheet, r++, "Total Salary Earned", "₹" + dr.getTotalSalaryEarned());
                addSummaryRow(sheet, r++, "Salary Credited",     "₹" + dr.getSalaryCredited());
                addSummaryRow(sheet, r++, "Salary Pending",      "₹" + dr.getSalaryPending());
                sheet.createRow(r++);

                // Trip details header
                String[] cols = {"Trip ID","Date","Truck","Dealer","Sand Type","Tons","Source","Destination","Trip Amount","Status","Salary"};
                Row hRow = sheet.createRow(r++);
                CellStyle hStyle = workbook.createCellStyle();
                Font hFont = workbook.createFont();
                hFont.setBold(true);
                hStyle.setFont(hFont);
                for (int i = 0; i < cols.length; i++) {
                    Cell c = hRow.createCell(i);
                    c.setCellValue(cols[i]);
                    c.setCellStyle(hStyle);
                }

                for (DriverReportDto.TripSummary t : dr.getTrips()) {
                    Row row = sheet.createRow(r++);
                    row.createCell(0).setCellValue(t.getTripId());
                    row.createCell(1).setCellValue(t.getTripDate());
                    row.createCell(2).setCellValue(t.getTruckNumber());
                    row.createCell(3).setCellValue(t.getDealerName());
                    row.createCell(4).setCellValue(t.getSandType());
                    row.createCell(5).setCellValue(t.getTons().doubleValue());
                    row.createCell(6).setCellValue(t.getSource());
                    row.createCell(7).setCellValue(t.getDestination());
                    row.createCell(8).setCellValue(t.getTotalAmount().doubleValue());
                    row.createCell(9).setCellValue(t.getStatus());
                    row.createCell(10).setCellValue(t.getSalaryForTrip().doubleValue());
                }
                autoSize(sheet, cols.length);
            }
            return toBytes(workbook);
        }
    }

    public byte[] exportDealerReportToExcel(Integer dealerId) throws IOException {
        List<DealerReportDto> reports = dealerId != null
                ? List.of(getDealerReport(dealerId))
                : getAllDealerReports();

        try (Workbook workbook = new XSSFWorkbook()) {
            for (DealerReportDto dr : reports) {
                String sheetName = dr.getDealerName().replaceAll("[^a-zA-Z0-9]", "_");
                if (sheetName.length() > 31) sheetName = sheetName.substring(0, 31);
                Sheet sheet = workbook.createSheet(sheetName);

                int r = 0;
                sheet.createRow(r++).createCell(0).setCellValue("DEALER REPORT — " + dr.getDealerName());
                sheet.createRow(r++);
                addSummaryRow(sheet, r++, "Dealer Name", dr.getDealerName());
                addSummaryRow(sheet, r++, "Phone",       dr.getPhone());
                addSummaryRow(sheet, r++, "Address",     dr.getAddress());
                sheet.createRow(r++);
                addSummaryRow(sheet, r++, "Total Trips",     String.valueOf(dr.getTotalTrips()));
                addSummaryRow(sheet, r++, "Completed Trips", String.valueOf(dr.getCompletedTrips()));
                addSummaryRow(sheet, r++, "Pending Trips",   String.valueOf(dr.getPendingTrips()));
                addSummaryRow(sheet, r++, "Total Tons",      dr.getTotalTonsReceived().toString());
                sheet.createRow(r++);
                addSummaryRow(sheet, r++, "Total Billed",   "₹" + dr.getTotalBilled());
                addSummaryRow(sheet, r++, "Total Paid",     "₹" + dr.getTotalPaid());
                addSummaryRow(sheet, r++, "Total Pending",  "₹" + dr.getTotalPending());
                addSummaryRow(sheet, r++, "Total Interest", "₹" + dr.getTotalInterest());
                sheet.createRow(r++);

                String[] cols = {"Trip ID","Date","Truck","Driver","Sand Type","Tons",
                        "Trip Amount","Paid","Pending","Interest","Payment Status","Due Date","Trip Status"};
                Row hRow = sheet.createRow(r++);
                CellStyle hStyle = workbook.createCellStyle();
                Font hFont = workbook.createFont();
                hFont.setBold(true);
                hStyle.setFont(hFont);
                for (int i = 0; i < cols.length; i++) {
                    Cell c = hRow.createCell(i);
                    c.setCellValue(cols[i]);
                    c.setCellStyle(hStyle);
                }

                for (DealerReportDto.TripPaymentSummary t : dr.getTrips()) {
                    Row row = sheet.createRow(r++);
                    row.createCell(0).setCellValue(t.getTripId());
                    row.createCell(1).setCellValue(t.getTripDate());
                    row.createCell(2).setCellValue(t.getTruckNumber());
                    row.createCell(3).setCellValue(t.getDriverName());
                    row.createCell(4).setCellValue(t.getSandType());
                    row.createCell(5).setCellValue(t.getTons().doubleValue());
                    row.createCell(6).setCellValue(t.getTripAmount().doubleValue());
                    row.createCell(7).setCellValue(t.getPaidAmount().doubleValue());
                    row.createCell(8).setCellValue(t.getPendingAmount().doubleValue());
                    row.createCell(9).setCellValue(t.getInterestAmount().doubleValue());
                    row.createCell(10).setCellValue(t.getPaymentStatus());
                    row.createCell(11).setCellValue(t.getDueDate());
                    row.createCell(12).setCellValue(t.getTripStatus());
                }
                autoSize(sheet, cols.length);
            }
            return toBytes(workbook);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────

    private void createHeaderRow(Workbook wb, Sheet sheet, String[] columns) {
        Row header = sheet.createRow(0);
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        style.setFont(font);
        for (int i = 0; i < columns.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(columns[i]);
            cell.setCellStyle(style);
        }
    }

    private void addSummaryRow(Sheet sheet, int rowNum, String label, String value) {
        Row row = sheet.createRow(rowNum);
        row.createCell(0).setCellValue(label);
        row.createCell(1).setCellValue(value);
    }

    private void autoSize(Sheet sheet, int cols) {
        for (int i = 0; i < cols; i++) sheet.autoSizeColumn(i);
    }

    private byte[] toBytes(Workbook workbook) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        return out.toByteArray();
    }
}
