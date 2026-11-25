package com.ecotrack.report.repo;

import com.ecotrack.report.domain.CollectionPoint;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CollectionPointRepository extends JpaRepository<CollectionPoint, UUID> {
    List<CollectionPoint> findByActiveTrueOrderByName();
}
