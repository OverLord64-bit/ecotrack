package com.ecotrack.report.repo;

import com.ecotrack.report.domain.TransmissionStatus;
import com.ecotrack.report.domain.Report;
import com.ecotrack.report.domain.ReportStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReportRepository extends JpaRepository<Report, UUID>, JpaSpecificationExecutor<Report> {

    Optional<Report> findByCollectionId(UUID collectionId);

    List<Report> findByStatus(ReportStatus status);

    List<Report> findByStatusIn(Collection<ReportStatus> statuses);

    List<Report> findByTransmissionStatus(TransmissionStatus status);
}
