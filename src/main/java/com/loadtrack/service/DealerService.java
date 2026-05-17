package com.loadtrack.service;

import com.loadtrack.dto.DealerRequest;
import com.loadtrack.entity.*;
import com.loadtrack.exception.DuplicateEntryException;
import com.loadtrack.exception.ResourceNotFoundException;
import com.loadtrack.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
@Service
public class DealerService {

    @Autowired private DealerRepository dealerRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private PaymentRepository paymentRepository;
    @Autowired private PaymentTransactionRepository paymentTransactionRepository;
    @Autowired private ReceiptRepository receiptRepository;
    @Autowired private SandRequestRepository sandRequestRepository;

    public List<Dealer> getAllDealers() {
        return dealerRepository.findAll();
    }

    public Dealer getDealerById(Integer id) {
        return dealerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Dealer not found with id: " + id));
    }

    @Transactional
    public Dealer addDealer(DealerRequest request) {
        // Check username uniqueness
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateEntryException("Username '" + request.getUsername() + "' is already taken. Please choose another.");
        }

        Dealer dealer = new Dealer();
        mapRequestToDealer(request, dealer);
        Dealer savedDealer = dealerRepository.save(dealer);

        // Auto-create login account
        Role role = roleRepository.findByName("DEALER")
                .orElseThrow(() -> new ResourceNotFoundException("Role DEALER not found"));

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(role);
        user.setStatus(true);
        user.setLinkedDealer(savedDealer);
        userRepository.save(user);

        return savedDealer;
    }

    @Transactional
    public Dealer updateDealer(Integer id, DealerRequest request) {
        Dealer dealer = getDealerById(id);

        // Check username uniqueness (only if changed)
        User existingUser = userRepository.findAll().stream()
                .filter(u -> u.getLinkedDealer() != null && u.getLinkedDealer().getId().equals(id))
                .findFirst().orElse(null);

        if (existingUser != null && !existingUser.getUsername().equals(request.getUsername())) {
            if (userRepository.existsByUsername(request.getUsername())) {
                throw new DuplicateEntryException("Username '" + request.getUsername() + "' is already taken. Please choose another.");
            }
            existingUser.setUsername(request.getUsername());
            if (request.getPassword() != null && !request.getPassword().isBlank()) {
                existingUser.setPassword(passwordEncoder.encode(request.getPassword()));
            }
            userRepository.save(existingUser);
        }

        mapRequestToDealer(request, dealer);
        return dealerRepository.save(dealer);
    }

    @Transactional
    public void deleteDealer(Integer id) {
        Dealer dealer = getDealerById(id);

        // 1. Delete linked user account
        userRepository.findAll().stream()
                .filter(u -> u.getLinkedDealer() != null && u.getLinkedDealer().getId().equals(id))
                .forEach(userRepository::delete);

        // 2. Delete sand requests for this dealer
        sandRequestRepository.findByDealerIdOrderByCreatedAtDesc(id)
                .forEach(sandRequestRepository::delete);

        // 3. Delete payments and their transactions/receipts
        paymentRepository.findByTripDealerId(id).forEach(payment -> {
            // Delete receipts
            receiptRepository.findByPaymentId(payment.getId())
                    .forEach(receiptRepository::delete);
            // Delete transactions
            paymentTransactionRepository.findByPaymentIdOrderByPaidAtAsc(payment.getId())
                    .forEach(paymentTransactionRepository::delete);
            // Delete payment
            paymentRepository.delete(payment);
        });

        // 4. Delete dealer (trips will have dealer set to NULL via FK ON DELETE SET NULL)
        dealerRepository.delete(dealer);
    }

    private void mapRequestToDealer(DealerRequest request, Dealer dealer) {
        dealer.setName(request.getName());
        dealer.setPhone(request.getPhone());
        dealer.setAddress(request.getAddress());
    }
}
