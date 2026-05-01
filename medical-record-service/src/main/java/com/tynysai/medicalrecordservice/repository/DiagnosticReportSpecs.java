package com.tynysai.medicalrecordservice.repository;

import com.tynysai.medicalrecordservice.model.DiagnosticReport;
import com.tynysai.medicalrecordservice.model.enums.DiseaseType;
import com.tynysai.medicalrecordservice.model.enums.Severity;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.UUID;

public final class DiagnosticReportSpecs {
    private DiagnosticReportSpecs() {}

    public static Specification<DiagnosticReport> byPatient(UUID patientId) {
        return (root, q, cb) -> cb.equal(root.get("patientId"), patientId);
    }

    public static Specification<DiagnosticReport> byDoctor(UUID doctorId) {
        return (root, q, cb) -> cb.equal(root.get("doctorId"), doctorId);
    }

    public static Specification<DiagnosticReport> bySeverity(Severity severity) {
        return (root, q, cb) -> severity == null ? cb.conjunction() : cb.equal(root.get("severity"), severity);
    }

    public static Specification<DiagnosticReport> byDiagnosis(DiseaseType diagnosis) {
        return (root, q, cb) -> diagnosis == null ? cb.conjunction()
                : cb.equal(root.get("finalDiagnosis"), diagnosis);
    }

    public static Specification<DiagnosticReport> createdFrom(LocalDateTime from) {
        return (root, q, cb) -> from == null ? cb.conjunction()
                : cb.greaterThanOrEqualTo(root.get("createdAt"), from);
    }

    public static Specification<DiagnosticReport> createdTo(LocalDateTime to) {
        return (root, q, cb) -> to == null ? cb.conjunction()
                : cb.lessThan(root.get("createdAt"), to);
    }

    public static Specification<DiagnosticReport> patientIdIn(Collection<UUID> patientIds) {
        if (patientIds == null || patientIds.isEmpty()) return (root, q, cb) -> cb.disjunction();
        return (root, q, cb) -> root.get("patientId").in(patientIds);
    }

    public static Specification<DiagnosticReport> matchesQuery(String query) {
        if (query == null || query.isBlank()) return (root, q, cb) -> cb.conjunction();
        String pattern = "%" + query.toLowerCase() + "%";
        return (root, q, cb) -> {
            Predicate p1 = cb.like(cb.lower(cb.coalesce(root.get("clinicalFindings"), "")), pattern);
            Predicate p2 = cb.like(cb.lower(cb.coalesce(root.get("reportText"), "")), pattern);
            Predicate p3 = cb.like(cb.lower(cb.coalesce(root.get("treatmentRecommendations"), "")), pattern);
            Predicate p4 = cb.like(cb.lower(cb.coalesce(root.get("reportNumber"), "")), pattern);
            return cb.or(p1, p2, p3, p4);
        };
    }
}