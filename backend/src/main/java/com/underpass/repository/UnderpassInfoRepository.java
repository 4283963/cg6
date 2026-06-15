package com.underpass.repository;

import com.underpass.entity.UnderpassInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UnderpassInfoRepository extends JpaRepository<UnderpassInfo, String> {
    List<UnderpassInfo> findByStatus(String status);
}
