package com.loadtrack.repository;

import com.loadtrack.entity.SandType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SandTypeRepository extends JpaRepository<SandType, Integer> {
    boolean existsByName(String name);
}
