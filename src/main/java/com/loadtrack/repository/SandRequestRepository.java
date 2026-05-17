package com.loadtrack.repository;

import com.loadtrack.entity.SandRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SandRequestRepository extends JpaRepository<SandRequest, Integer> {
    List<SandRequest> findByDealerIdOrderByCreatedAtDesc(Integer dealerId);
    List<SandRequest> findByStatusOrderByCreatedAtDesc(SandRequest.RequestStatus status);
    List<SandRequest> findAllByOrderByCreatedAtDesc();
    long countByStatus(SandRequest.RequestStatus status);
}
