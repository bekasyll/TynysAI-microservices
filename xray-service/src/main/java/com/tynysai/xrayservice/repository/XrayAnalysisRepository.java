package com.tynysai.xrayservice.repository;

import com.tynysai.xrayservice.model.XrayAnalysis;
import com.tynysai.xrayservice.model.enums.AnalysisStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface XrayAnalysisRepository extends JpaRepository<XrayAnalysis, Long> {
    Page<XrayAnalysis> findByPatientIdOrderByUploadedAtDesc(UUID patientId, Pageable pageable);

    Page<XrayAnalysis> findByAssignedDoctorIdOrderByUploadedAtDesc(UUID doctorId, Pageable pageable);

    Optional<XrayAnalysis> findByIdAndPatientId(Long id, UUID patientId);

    Optional<XrayAnalysis> findByIdAndAssignedDoctorId(Long id, UUID doctorId);

    long countByStatus(AnalysisStatus status);
}
