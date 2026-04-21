package com.tynysai.medicalrecordservice.repository;

import com.tynysai.medicalrecordservice.model.LabResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface LabResultRepository extends JpaRepository<LabResult, Long> {
    Page<LabResult> findByPatientIdOrderByTestDateDesc(UUID patientId, Pageable pageable);

    Optional<LabResult> findByIdAndPatientId(Long id, UUID patientId);
}
