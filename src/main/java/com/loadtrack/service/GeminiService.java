package com.loadtrack.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loadtrack.entity.*;
import com.loadtrack.repository.*;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class GeminiService {

    @Value("${openrouter.api.key}")
    private String apiKey;

    @Value("${openrouter.model:nvidia/nemotron-nano-9b-v2:free}")
    private String model;

    @Autowired private TripRepository tripRepository;
    @Autowired private PaymentRepository paymentRepository;
    @Autowired private DriverRepository driverRepository;
    @Autowired private DealerRepository dealerRepository;
    @Autowired private TruckRepository truckRepository;

    private static final String OPENROUTER_URL = "https://openrouter.ai/api/v1/chat/completions";

    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public String chat(String userQuestion) {
        try {
            String context = buildCompactContext();
            String systemPrompt = "You are an AI assistant for LoadTrack, a truck operations management system for sand transportation. "
                    + "Answer questions based only on the data provided. Be concise (max 100 words). Use ₹ for Indian Rupees.";
            String userMsg = "System Data:\n" + context + "\n\nQuestion: " + userQuestion;

            return callOpenRouter(systemPrompt, userMsg);
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : "Unknown error";
            System.err.println("AI error: " + msg);
            if (msg.contains("429")) return "⚠️ AI rate limit reached. Please wait 30 seconds and try again.";
            if (msg.contains("401")) return "⚠️ Invalid API key. Please check configuration.";
            return "⚠️ AI error: " + msg;
        }
    }

    private String callOpenRouter(String systemPrompt, String userMessage) throws Exception {
        String requestBody = objectMapper.writeValueAsString(new java.util.HashMap<String, Object>() {{
            put("model", model);
            put("messages", new Object[]{
                new java.util.HashMap<String, String>() {{ put("role", "system"); put("content", systemPrompt); }},
                new java.util.HashMap<String, String>() {{ put("role", "user"); put("content", userMessage); }}
            });
            put("max_tokens", 300);
            put("temperature", 0.3);
        }});

        Request request = new Request.Builder()
                .url(OPENROUTER_URL)
                .post(RequestBody.create(requestBody, MediaType.parse("application/json")))
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("HTTP-Referer", "http://localhost:4200")
                .addHeader("X-Title", "LoadTrack AI")
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            String body = response.body() != null ? response.body().string() : "";
            if (!response.isSuccessful()) {
                throw new RuntimeException(response.code() + " - " + body);
            }
            JsonNode root = objectMapper.readTree(body);
            return root.path("choices").get(0)
                    .path("message").path("content")
                    .asText("No response from AI");
        }
    }

    private String buildCompactContext() {
        StringBuilder ctx = new StringBuilder();

        // Trucks
        long totalTrucks = truckRepository.count();
        long available = truckRepository.findAll().stream().filter(t -> t.getStatus() == Truck.TruckStatus.AVAILABLE).count();
        long onTrip = truckRepository.findAll().stream().filter(t -> t.getStatus() == Truck.TruckStatus.ON_TRIP).count();
        ctx.append("Trucks: ").append(totalTrucks).append(" total (").append(available).append(" available, ").append(onTrip).append(" on trip)\n");

        // Drivers
        ctx.append("Drivers: ").append(driverRepository.count()).append(" total\n");
        driverRepository.findAll().forEach(d -> {
            long tc = tripRepository.countByDriverId(d.getId());
            ctx.append("  ").append(d.getName()).append(": ").append(tc).append(" trips, ₹").append(d.getSalaryPerTrip()).append("/trip\n");
        });

        // Dealers
        ctx.append("Dealers: ").append(dealerRepository.count()).append(" total\n");
        dealerRepository.findAll().forEach(d -> {
            BigDecimal pending = paymentRepository.getPendingAmountByDealer(d.getId());
            ctx.append("  ").append(d.getName()).append(": ₹").append(pending != null ? pending : BigDecimal.ZERO).append(" pending\n");
        });

        // Trips
        List<Trip> trips = tripRepository.findAll();
        long completed = trips.stream().filter(t -> t.getStatus() == Trip.TripStatus.COMPLETED).count();
        long pending = trips.stream().filter(t -> t.getStatus() == Trip.TripStatus.PENDING).count();
        BigDecimal totalEarnings = trips.stream().filter(t -> t.getStatus() == Trip.TripStatus.COMPLETED)
                .map(Trip::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        LocalDate now = LocalDate.now();
        BigDecimal monthly = tripRepository.getMonthlyEarnings(now.getMonthValue(), now.getYear());
        ctx.append("Trips: ").append(trips.size()).append(" total (").append(completed).append(" completed, ").append(pending).append(" pending)\n");
        ctx.append("Total Earnings: ₹").append(totalEarnings).append(", This Month: ₹").append(monthly != null ? monthly : BigDecimal.ZERO).append("\n");

        // Payments
        List<Payment> payments = paymentRepository.findAll();
        long unpaid = payments.stream().filter(p -> p.getPaymentStatus() == Payment.PaymentStatus.UNPAID).count();
        long partialPay = payments.stream().filter(p -> p.getPaymentStatus() == Payment.PaymentStatus.PARTIAL).count();
        long overdue = payments.stream().filter(p -> p.getDueDate() != null && p.getDueDate().isBefore(now) && p.getPaymentStatus() != Payment.PaymentStatus.PAID).count();
        BigDecimal totalPending = payments.stream().filter(p -> p.getPaymentStatus() != Payment.PaymentStatus.PAID)
                .map(p -> p.getFinalAmount().subtract(p.getPaidAmount())).reduce(BigDecimal.ZERO, BigDecimal::add);
        ctx.append("Payments: ").append(unpaid).append(" unpaid, ").append(partialPay).append(" partial, ").append(overdue).append(" overdue\n");
        ctx.append("Total Pending: ₹").append(totalPending).append("\n");

        return ctx.toString();
    }
}
