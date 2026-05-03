package com.tynysai.userservice.repository;

import com.tynysai.userservice.model.DoctorProfile;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DoctorProfileSpecsTest {

    @Mock
    private Root<DoctorProfile> root;
    @Mock
    private CriteriaQuery<?> query;
    @Mock
    private CriteriaBuilder cb;
    @Mock
    @SuppressWarnings("rawtypes")
    private Path path;
    @Mock
    private Predicate predicate;
    @Mock
    @SuppressWarnings("rawtypes")
    private Expression expression;

    @Test
    @SuppressWarnings("unchecked")
    void byApproved_buildsEqualityPredicate_onApprovedColumn() {
        when(root.get(anyString())).thenReturn(path);
        when(cb.equal(any(), eq(true))).thenReturn(predicate);

        Specification<DoctorProfile> spec = DoctorProfileSpecs.byApproved(true);
        Predicate result = spec.toPredicate(root, query, cb);

        assertThat(result).isSameAs(predicate);
        verify(root).get("approved");
        verify(cb).equal(any(), eq(true));
    }

    @Test
    @SuppressWarnings("unchecked")
    void byApproved_buildsEqualityPredicate_forFalseValue() {
        when(root.get(anyString())).thenReturn(path);
        when(cb.equal(any(), eq(false))).thenReturn(predicate);

        Specification<DoctorProfile> spec = DoctorProfileSpecs.byApproved(false);
        Predicate result = spec.toPredicate(root, query, cb);

        assertThat(result).isSameAs(predicate);
        verify(cb).equal(any(), eq(false));
    }

    @Test
    void matchesProfileText_returnsDisjunction_whenQueryNull() {
        when(cb.disjunction()).thenReturn(predicate);

        Specification<DoctorProfile> spec = DoctorProfileSpecs.matchesProfileText(null);
        Predicate result = spec.toPredicate(root, query, cb);

        assertThat(result).isSameAs(predicate);
        verify(cb).disjunction();
    }

    @Test
    void matchesProfileText_returnsDisjunction_whenQueryBlank() {
        when(cb.disjunction()).thenReturn(predicate);

        Specification<DoctorProfile> spec = DoctorProfileSpecs.matchesProfileText("   ");
        Predicate result = spec.toPredicate(root, query, cb);

        assertThat(result).isSameAs(predicate);
    }

    @Test
    @SuppressWarnings("unchecked")
    void matchesProfileText_buildsOrOfFourLikeClauses_whenQueryProvided() {
        when(root.get(anyString())).thenReturn(path);
        when(cb.coalesce(any(Expression.class), anyString())).thenReturn(expression);
        when(cb.lower(any(Expression.class))).thenReturn(expression);
        when(cb.like(any(Expression.class), anyString())).thenReturn(predicate);
        when(cb.or(any(Predicate.class), any(Predicate.class), any(Predicate.class), any(Predicate.class)))
                .thenReturn(predicate);

        Specification<DoctorProfile> spec = DoctorProfileSpecs.matchesProfileText("Cardio");
        Predicate result = spec.toPredicate(root, query, cb);

        assertThat(result).isSameAs(predicate);
        verify(root).get("specialization");
        verify(root).get("licenseNumber");
        verify(root).get("hospitalName");
        verify(root).get("department");
        verify(cb, atLeastOnce()).like(any(Expression.class), eq("%cardio%"));
        verify(cb).or(any(Predicate.class), any(Predicate.class), any(Predicate.class), any(Predicate.class));
    }

    @Test
    void userIdIn_returnsDisjunction_whenCollectionNull() {
        when(cb.disjunction()).thenReturn(predicate);

        Specification<DoctorProfile> spec = DoctorProfileSpecs.userIdIn(null);
        Predicate result = spec.toPredicate(root, query, cb);

        assertThat(result).isSameAs(predicate);
    }

    @Test
    void userIdIn_returnsDisjunction_whenCollectionEmpty() {
        when(cb.disjunction()).thenReturn(predicate);

        Specification<DoctorProfile> spec = DoctorProfileSpecs.userIdIn(List.of());
        Predicate result = spec.toPredicate(root, query, cb);

        assertThat(result).isSameAs(predicate);
    }

    @Test
    @SuppressWarnings("unchecked")
    void userIdIn_callsInOnUserIdPath_whenCollectionNonEmpty() {
        when(root.get(anyString())).thenReturn(path);
        List<UUID> ids = List.of(UUID.randomUUID(), UUID.randomUUID());
        when(path.in(ids)).thenReturn(predicate);

        Specification<DoctorProfile> spec = DoctorProfileSpecs.userIdIn(ids);
        Predicate result = spec.toPredicate(root, query, cb);

        assertThat(result).isSameAs(predicate);
        verify(root).get("userId");
        verify(path).in(ids);
    }
}
