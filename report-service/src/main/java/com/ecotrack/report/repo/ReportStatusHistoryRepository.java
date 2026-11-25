package com.ecotrack.report.repo;

import com.ecotrack.report.domain.ReportStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ReportStatusHistoryRepository extends JpaRepository<ReportStatusHistory, UUID> {
    List<ReportStatusHistory> findByReportIdOrderByChangedAtAsc(UUID reportId);
}
