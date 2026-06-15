package com.underpass.repository;

import com.underpass.entity.UpstreamCatchment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UpstreamCatchmentRepository extends JpaRepository<UpstreamCatchment, String> {
    Optional<UpstreamCatchment> findByFlowMeterId(String flowMeterId);
}
