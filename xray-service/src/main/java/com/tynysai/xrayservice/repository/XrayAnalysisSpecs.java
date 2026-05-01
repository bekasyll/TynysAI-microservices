package com.tynysai.xrayservice.repository;

import com.tynysai.xrayservice.model.XrayAnalysis;
import com.tynysai.xrayservice.model.enums.AnalysisStatus;
import com.tynysai.xrayservice.model.enums.DiseaseType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.UUID;

public final class XrayAnalysisSpecs {
    private XrayAnalysisSpecs() {}

    public static Specification<XrayAnalysis> byPatient(UUID patientId) {
        return (root, q, cb) -> cb.equal(root.get("patientId"), patientId);
    }

    public static Specification<XrayAnalysis> byAssignedDoctor(UUID doctorId) {
        return (root, q, cb) -> cb.equal(root.get("assignedDoctorId"), doctorId);
    }

    public static Specification<XrayAnalysis> byStatus(AnalysisStatus status) {
        return (root, q, cb) -> status == null ? cb.conjunction() : cb.equal(root.get("status"), status);
    }

    public static Specification<XrayAnalysis> byDiagnosis(DiseaseType diagnosis) {
        return (root, q, cb) -> diagnosis == null ? cb.conjunction()
                : cb.or(
                    cb.equal(root.get("aiPrimaryDiagnosis"), diagnosis),
                    cb.equal(root.get("doctorDiagnosis"), diagnosis));
    }

    public static Specification<XrayAnalysis> uploadedFrom(LocalDateTime from) {
        return (root, q, cb) -> from == null ? cb.conjunction()
                : cb.greaterThanOrEqualTo(root.get("uploadedAt"), from);
    }

    public static Specification<XrayAnalysis> uploadedTo(LocalDateTime to) {
        return (root, q, cb) -> to == null ? cb.conjunction()
                : cb.lessThan(root.get("uploadedAt"), to);
    }

    public static Specification<XrayAnalysis> patientIdIn(Collection<UUID> patientIds) {
        if (patientIds == null || patientIds.isEmpty()) return (root, q, cb) -> cb.disjunction();
        return (root, q, cb) -> root.get("patientId").in(patientIds);
    }

    public static Specification<XrayAnalysis> matchesQuery(String query) {
        if (query == null || query.isBlank()) return (root, q, cb) -> cb.conjunction();
        String pattern = "%" + query.toLowerCase() + "%";
        return (root, q, cb) -> {
            Predicate p1 = cb.like(cb.lower(cb.coalesce(root.get("aiFindings"), "")), pattern);
            Predicate p2 = cb.like(cb.lower(cb.coalesce(root.get("patientNotes"), "")), pattern);
            Predicate p3 = cb.like(cb.lower(cb.coalesce(root.get("doctorNotes"), "")), pattern);
            Predicate p4 = cb.like(cb.lower(root.get("originalFileName")), pattern);
            return cb.or(p1, p2, p3, p4);
        };
    }
}