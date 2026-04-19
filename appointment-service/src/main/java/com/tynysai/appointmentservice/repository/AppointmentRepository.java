package com.tynysai.appointmentservice.repository;

import com.tynysai.appointmentservice.model.Appointment;
import com.tynysai.appointmentservice.model.enums.AppointmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    Page<Appointment> findByPatientIdOrderByCreatedAtDesc(Long patientId, Pageable pageable);

    Page<Appointment> findByDoctorIdOrderByCreatedAtDesc(Long doctorId, Pageable pageable);

    Page<Appointment> findByDoctorIdAndStatusOrderByCreatedAtDesc(Long doctorId,
                                                                  AppointmentStatus status,
                                                                  Pageable pageable);

    Page<Appointment> findByPatientIdAndStatusOrderByCreatedAtDesc(Long patientId,
                                                                   AppointmentStatus status,
                                                                   Pageable pageable);

    Optional<Appointment> findByIdAndPatientId(Long id, Long patientId);

    Optional<Appointment> findByIdAndDoctorId(Long id, Long doctorId);

    long countByDoctorIdAndStatus(Long doctorId, AppointmentStatus status);

    @Query("SELECT COUNT(a) > 0 FROM Appointment a " +
            "WHERE a.doctorId = :doctorId " +
            "AND a.status IN ('PENDING', 'ACCEPTED') " +
            "AND a.appointmentDate BETWEEN :from AND :to")
    boolean existsConflict(@Param("doctorId") Long doctorId,
                           @Param("from") LocalDateTime from,
                           @Param("to") LocalDateTime to);

    @Query("SELECT COUNT(a) > 0 FROM Appointment a " +
            "WHERE a.doctorId = :doctorId " +
            "AND a.id <> :excludeId " +
            "AND a.status = 'ACCEPTED' " +
            "AND a.appointmentDate BETWEEN :from AND :to")
    boolean existsAcceptedConflict(@Param("doctorId") Long doctorId,
                                   @Param("excludeId") Long excludeId,
                                   @Param("from") LocalDateTime from,
                                   @Param("to") LocalDateTime to);
}
