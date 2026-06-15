package com.underpass.repository;

import com.underpass.entity.WaterDepthRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WaterDepthRecordRepository extends JpaRepository<WaterDepthRecord, Long> {
    List<WaterDepthRecord> findTop20ByUnderpassIdOrderByReceivedAtDesc(String underpassId);
    List<WaterDepthRecord> findByUnderpassIdOrderByReceivedAtDesc(String underpassId);
}
