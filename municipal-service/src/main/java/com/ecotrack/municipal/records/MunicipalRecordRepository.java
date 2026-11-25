package com.ecotrack.municipal.records;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MunicipalRecordRepository extends JpaRepository<MunicipalRecord, Long> {

    Optional<MunicipalRecord> findByRecordId(UUID recordId);

    List<MunicipalRecord> findAllByOrderByReceivedAtDesc();

    @Query(value = "SELECT nextval('record_ref_seq')", nativeQuery = true)
    long nextSequence();
}
