package com.underpass.repository;

import com.underpass.entity.FlowRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface FlowRecordRepository extends JpaRepository<FlowRecord, Long> {
    List<FlowRecord> findByCatchmentIdAndReceivedAtAfterOrderByReceivedAtDesc(
            String catchmentId, LocalDateTime after);
}
