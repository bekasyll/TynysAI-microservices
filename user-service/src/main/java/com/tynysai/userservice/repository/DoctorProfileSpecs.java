package com.tynysai.userservice.repository;

import com.tynysai.userservice.model.DoctorProfile;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.Collection;
import java.util.UUID;

public final class DoctorProfileSpecs {
    private DoctorProfileSpecs() {}

    public static Specification<DoctorProfile> byApproved(boolean approved) {
        return (root, q, cb) -> cb.equal(root.get("approved"), approved);
    }

    public static Specification<DoctorProfile> matchesProfileText(String query) {
        if (query == null || query.isBlank()) return (root, q, cb) -> cb.disjunction();
        String pattern = "%" + query.toLowerCase() + "%";
        return (root, q, cb) -> {
            Predicate p1 = cb.like(cb.lower(cb.coalesce(root.get("specialization"), "")), pattern);
            Predicate p2 = cb.like(cb.lower(cb.coalesce(root.get("licenseNumber"), "")), pattern);
            Predicate p3 = cb.like(cb.lower(cb.coalesce(root.get("hospitalName"), "")), pattern);
            Predicate p4 = cb.like(cb.lower(cb.coalesce(root.get("department"), "")), pattern);
            return cb.or(p1, p2, p3, p4);
        };
    }

    public static Specification<DoctorProfile> userIdIn(Collection<UUID> userIds) {
        if (userIds == null || userIds.isEmpty()) return (root, q, cb) -> cb.disjunction();
        return (root, q, cb) -> root.get("userId").in(userIds);
    }
}