package com.tynysai.xrayservice.repository;

import com.tynysai.xrayservice.model.XrayAnalysis;
import com.tynysai.xrayservice.model.enums.AnalysisStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface XrayAnalysisRepository extends JpaRepository<XrayAnalysis, Long> {
    Page<XrayAnalysis> findByPatientIdOrderByUploadedAtDesc(Long patientId, Pageable pageable);

    Page<XrayAnalysis> findByAssignedDoctorIdOrderByUploadedAtDesc(Long doctorId, Pageable pageable);

    Optional<XrayAnalysis> findByIdAndPatientId(Long id, Long patientId);

    Optional<XrayAnalysis> findByIdAndAssignedDoctorId(Long id, Long doctorId);

    long countByStatus(AnalysisStatus status);
}
