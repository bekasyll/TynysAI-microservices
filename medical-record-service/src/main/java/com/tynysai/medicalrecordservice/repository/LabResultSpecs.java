package com.tynysai.medicalrecordservice.repository;

import com.tynysai.medicalrecordservice.model.LabResult;
import com.tynysai.medicalrecordservice.model.enums.LabTestType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.UUID;

public final class LabResultSpecs {
    private LabResultSpecs() {}

    public static Specification<LabResult> byPatient(UUID patientId) {
        return (root, q, cb) -> cb.equal(root.get("patientId"), patientId);
    }

    public static Specification<LabResult> byTestType(LabTestType type) {
        return (root, q, cb) -> type == null ? cb.conjunction() : cb.equal(root.get("testType"), type);
    }

    public static Specification<LabResult> testDateFrom(LocalDate from) {
        return (root, q, cb) -> from == null ? cb.conjunction()
                : cb.greaterThanOrEqualTo(root.get("testDate"), from);
    }

    public static Specification<LabResult> testDateTo(LocalDate to) {
        return (root, q, cb) -> to == null ? cb.conjunction()
                : cb.lessThanOrEqualTo(root.get("testDate"), to);
    }

    public static Specification<LabResult> matchesQuery(String query) {
        if (query == null || query.isBlank()) return (root, q, cb) -> cb.conjunction();
        String pattern = "%" + query.toLowerCase() + "%";
        return (root, q, cb) -> {
            Predicate p1 = cb.like(cb.lower(cb.coalesce(root.get("labName"), "")), pattern);
            Predicate p2 = cb.like(cb.lower(cb.coalesce(root.get("notes"), "")), pattern);
            Predicate p3 = cb.like(cb.lower(cb.coalesce(root.get("rawResultText"), "")), pattern);
            return cb.or(p1, p2, p3);
        };
    }
}