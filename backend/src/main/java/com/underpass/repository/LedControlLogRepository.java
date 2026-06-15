package com.underpass.repository;

import com.underpass.entity.LedControlLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LedControlLogRepository extends JpaRepository<LedControlLog, Long> {
}
