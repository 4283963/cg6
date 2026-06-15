package com.underpass.repository;

import com.underpass.entity.RainfallRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface RainfallRecordRepository extends JpaRepository<RainfallRecord, Long> {
    List<RainfallRecord> findTop1ByUnderpassIdOrderByReceivedAtDesc(String underpassId);
    List<RainfallRecord> findByUnderpassIdAndReceivedAtAfterOrderByReceivedAtDesc(
            String underpassId, LocalDateTime after);
}
