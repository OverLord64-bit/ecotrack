package com.ecotrack.collection.repo;

import com.ecotrack.collection.domain.CollectionJob;
import com.ecotrack.collection.domain.CollectionState;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CollectionJobRepository extends JpaRepository<CollectionJob, UUID> {

    Optional<CollectionJob> findByRequestId(UUID requestId);

    boolean existsByReportIdAndStatusNot(UUID reportId, CollectionState status);

    List<CollectionJob> findByStatusNot(CollectionState status);
}
