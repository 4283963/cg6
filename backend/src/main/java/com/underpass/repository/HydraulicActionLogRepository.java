package com.underpass.repository;

import com.underpass.entity.HydraulicActionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HydraulicActionLogRepository extends JpaRepository<HydraulicActionLog, Long> {
}
