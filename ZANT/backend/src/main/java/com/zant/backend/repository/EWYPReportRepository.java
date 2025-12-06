package com.zant.backend.repository;

import com.zant.backend.model.ewyp.EWYPReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EWYPReportRepository extends JpaRepository<EWYPReport, Long> {
}
