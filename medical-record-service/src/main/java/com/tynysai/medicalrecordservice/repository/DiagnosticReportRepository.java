package com.tynysai.medicalrecordservice.repository;

import com.tynysai.medicalrecordservice.model.DiagnosticReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DiagnosticReportRepository extends JpaRepository<DiagnosticReport, Long> {
    Page<DiagnosticReport> findByPatientIdOrderByCreatedAtDesc(Long patientId, Pageable pageable);

    Page<DiagnosticReport> findByDoctorIdOrderByCreatedAtDesc(Long doctorId, Pageable pageable);

    Optional<DiagnosticReport> findByIdAndPatientId(Long id, Long patientId);

    Optional<DiagnosticReport> findByIdAndDoctorId(Long id, Long doctorId);
}
