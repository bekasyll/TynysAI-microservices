package com.tynysai.userservice.repository;

import com.tynysai.userservice.model.DoctorProfile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DoctorProfileRepository extends JpaRepository<DoctorProfile, Long> {
    Optional<DoctorProfile> findByUserId(UUID id);

    boolean existsByLicenseNumber(String licenseNumber);

    Page<DoctorProfile> findByApproved(boolean approved, Pageable pageable);

    @Query("SELECT d FROM DoctorProfile d WHERE d.approved = true AND " +
            "LOWER(d.specialization) LIKE LOWER(CONCAT('%', :spec, '%'))")
    List<DoctorProfile> findApprovedBySpecialization(@Param("spec") String specialization);

    long countByApproved(boolean approved);
}
