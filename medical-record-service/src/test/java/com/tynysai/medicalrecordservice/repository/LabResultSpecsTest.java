package com.tynysai.medicalrecordservice.repository;

import com.tynysai.medicalrecordservice.model.LabResult;
import com.tynysai.medicalrecordservice.model.enums.LabTestType;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LabResultSpecsTest {

    @Mock
    private Root<LabResult> root;
    @Mock
    private CriteriaQuery<?> query;
    @Mock
    private CriteriaBuilder cb;
    @Mock
    private Path<Object> path;
    @Mock
    private Predicate predicate;
    @Mock
    private Predicate conjunction;

    @BeforeEach
    void setUp() {
        lenient().when(root.get(anyString())).thenReturn(path);
        lenient().when(cb.conjunction()).thenReturn(conjunction);
        lenient().when(cb.equal(any(Expression.class), any(Object.class))).thenReturn(predicate);
        lenient().when(cb.greaterThanOrEqualTo(any(Expression.class), any(LocalDate.class))).thenReturn(predicate);
        lenient().when(cb.lessThanOrEqualTo(any(Expression.class), any(LocalDate.class))).thenReturn(predicate);
    }

    @Test
    void byPatient_buildsEqualPredicate() {
        UUID patientId = UUID.randomUUID();
        Specification<LabResult> spec = LabResultSpecs.byPatient(patientId);

        Predicate result = spec.toPredicate(root, query, cb);

        verify(cb).equal(path, patientId);
        assertThat(result).isSameAs(predicate);
    }

    @Test
    void byTestType_returnsConjunction_whenNull() {
        assertThat(LabResultSpecs.byTestType(null).toPredicate(root, query, cb))
                .isSameAs(conjunction);
    }

    @Test
    void byTestType_buildsEqualPredicate_whenProvided() {
        Predicate result = LabResultSpecs.byTestType(LabTestType.COMPLETE_BLOOD_COUNT).toPredicate(root, query, cb);

        verify(cb).equal(path, LabTestType.COMPLETE_BLOOD_COUNT);
        assertThat(result).isSameAs(predicate);
    }

    @Test
    void testDateFrom_returnsConjunction_whenNull() {
        assertThat(LabResultSpecs.testDateFrom(null).toPredicate(root, query, cb))
                .isSameAs(conjunction);
    }

    @Test
    void testDateFrom_buildsGreaterThanOrEqualPredicate() {
        LocalDate date = LocalDate.now();
        Predicate result = LabResultSpecs.testDateFrom(date).toPredicate(root, query, cb);

        verify(cb).greaterThanOrEqualTo(any(Expression.class), eq(date));
        assertThat(result).isSameAs(predicate);
    }

    @Test
    void testDateTo_returnsConjunction_whenNull() {
        assertThat(LabResultSpecs.testDateTo(null).toPredicate(root, query, cb))
                .isSameAs(conjunction);
    }

    @Test
    void testDateTo_buildsLessThanOrEqualPredicate() {
        LocalDate date = LocalDate.now();
        Predicate result = LabResultSpecs.testDateTo(date).toPredicate(root, query, cb);

        verify(cb).lessThanOrEqualTo(any(Expression.class), eq(date));
        assertThat(result).isSameAs(predicate);
    }

    @Test
    void matchesQuery_returnsConjunction_whenNullOrBlank() {
        assertThat(LabResultSpecs.matchesQuery(null).toPredicate(root, query, cb))
                .isSameAs(conjunction);
        assertThat(LabResultSpecs.matchesQuery("").toPredicate(root, query, cb))
                .isSameAs(conjunction);
        assertThat(LabResultSpecs.matchesQuery("   ").toPredicate(root, query, cb))
                .isSameAs(conjunction);
    }

    @Test
    void matchesQuery_buildsOrPredicate_whenQueryProvided() {
        Predicate orPredicate = org.mockito.Mockito.mock(Predicate.class);
        Expression coalescedExpr = org.mockito.Mockito.mock(Expression.class);
        Expression lowerExpr = org.mockito.Mockito.mock(Expression.class);
        when(cb.coalesce(any(Expression.class), eq(""))).thenReturn(coalescedExpr);
        when(cb.lower(coalescedExpr)).thenReturn(lowerExpr);
        when(cb.like(eq(lowerExpr), anyString())).thenReturn(predicate);
        when(cb.or(any(Predicate.class), any(Predicate.class), any(Predicate.class))).thenReturn(orPredicate);

        Predicate result = LabResultSpecs.matchesQuery("hemoglobin").toPredicate(root, query, cb);

        assertThat(result).isSameAs(orPredicate);
    }
}
