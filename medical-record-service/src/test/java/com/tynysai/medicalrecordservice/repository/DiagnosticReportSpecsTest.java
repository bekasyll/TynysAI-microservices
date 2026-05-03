package com.tynysai.medicalrecordservice.repository;

import com.tynysai.medicalrecordservice.model.DiagnosticReport;
import com.tynysai.medicalrecordservice.model.enums.DiseaseType;
import com.tynysai.medicalrecordservice.model.enums.Severity;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DiagnosticReportSpecsTest {

    @Mock
    private Root<DiagnosticReport> root;
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
    @Mock
    private Predicate disjunction;

    @BeforeEach
    void setUp() {
        lenient().when(root.get(anyString())).thenReturn(path);
        lenient().when(cb.conjunction()).thenReturn(conjunction);
        lenient().when(cb.disjunction()).thenReturn(disjunction);
        lenient().when(cb.equal(any(Expression.class), any(Object.class))).thenReturn(predicate);
        lenient().when(cb.greaterThanOrEqualTo(any(Expression.class), any(LocalDateTime.class))).thenReturn(predicate);
        lenient().when(cb.lessThan(any(Expression.class), any(LocalDateTime.class))).thenReturn(predicate);
    }

    @Test
    void byPatient_buildsEqualPredicate() {
        UUID patientId = UUID.randomUUID();
        Specification<DiagnosticReport> spec = DiagnosticReportSpecs.byPatient(patientId);

        Predicate result = spec.toPredicate(root, query, cb);

        verify(cb).equal(path, patientId);
        assertThat(result).isSameAs(predicate);
    }

    @Test
    void byDoctor_buildsEqualPredicate() {
        UUID doctorId = UUID.randomUUID();
        Specification<DiagnosticReport> spec = DiagnosticReportSpecs.byDoctor(doctorId);

        Predicate result = spec.toPredicate(root, query, cb);

        verify(cb).equal(path, doctorId);
        assertThat(result).isSameAs(predicate);
    }

    @Test
    void bySeverity_returnsConjunction_whenNull() {
        Specification<DiagnosticReport> spec = DiagnosticReportSpecs.bySeverity(null);

        Predicate result = spec.toPredicate(root, query, cb);

        assertThat(result).isSameAs(conjunction);
    }

    @Test
    void bySeverity_buildsEqualPredicate_whenProvided() {
        Specification<DiagnosticReport> spec = DiagnosticReportSpecs.bySeverity(Severity.MILD);

        Predicate result = spec.toPredicate(root, query, cb);

        verify(cb).equal(path, Severity.MILD);
        assertThat(result).isSameAs(predicate);
    }

    @Test
    void byDiagnosis_returnsConjunction_whenNull() {
        Specification<DiagnosticReport> spec = DiagnosticReportSpecs.byDiagnosis(null);

        Predicate result = spec.toPredicate(root, query, cb);

        assertThat(result).isSameAs(conjunction);
    }

    @Test
    void byDiagnosis_buildsEqualPredicate_whenProvided() {
        Specification<DiagnosticReport> spec = DiagnosticReportSpecs.byDiagnosis(DiseaseType.NORMAL);

        Predicate result = spec.toPredicate(root, query, cb);

        verify(cb).equal(path, DiseaseType.NORMAL);
        assertThat(result).isSameAs(predicate);
    }

    @Test
    void createdFrom_returnsConjunction_whenNull() {
        assertThat(DiagnosticReportSpecs.createdFrom(null).toPredicate(root, query, cb))
                .isSameAs(conjunction);
    }

    @Test
    void createdFrom_buildsGreaterThanOrEqualPredicate() {
        LocalDateTime from = LocalDateTime.now();
        Predicate result = DiagnosticReportSpecs.createdFrom(from).toPredicate(root, query, cb);

        verify(cb).greaterThanOrEqualTo(any(Expression.class), eq(from));
        assertThat(result).isSameAs(predicate);
    }

    @Test
    void createdTo_returnsConjunction_whenNull() {
        assertThat(DiagnosticReportSpecs.createdTo(null).toPredicate(root, query, cb))
                .isSameAs(conjunction);
    }

    @Test
    void createdTo_buildsLessThanPredicate() {
        LocalDateTime to = LocalDateTime.now();
        Predicate result = DiagnosticReportSpecs.createdTo(to).toPredicate(root, query, cb);

        verify(cb).lessThan(any(Expression.class), eq(to));
        assertThat(result).isSameAs(predicate);
    }

    @Test
    void patientIdIn_returnsDisjunction_whenCollectionNullOrEmpty() {
        assertThat(DiagnosticReportSpecs.patientIdIn(null).toPredicate(root, query, cb))
                .isSameAs(disjunction);
        assertThat(DiagnosticReportSpecs.patientIdIn(List.of()).toPredicate(root, query, cb))
                .isSameAs(disjunction);
    }

    @Test
    void patientIdIn_buildsInPredicate_whenIdsPresent() {
        UUID id1 = UUID.randomUUID();
        Predicate inPredicate = org.mockito.Mockito.mock(Predicate.class);
        when(path.in(List.of(id1))).thenReturn(inPredicate);

        Predicate result = DiagnosticReportSpecs.patientIdIn(List.of(id1)).toPredicate(root, query, cb);

        assertThat(result).isSameAs(inPredicate);
    }

    @Test
    void matchesQuery_returnsConjunction_whenNullOrBlank() {
        assertThat(DiagnosticReportSpecs.matchesQuery(null).toPredicate(root, query, cb))
                .isSameAs(conjunction);
        assertThat(DiagnosticReportSpecs.matchesQuery("").toPredicate(root, query, cb))
                .isSameAs(conjunction);
        assertThat(DiagnosticReportSpecs.matchesQuery("   ").toPredicate(root, query, cb))
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
        when(cb.or(any(Predicate.class), any(Predicate.class), any(Predicate.class), any(Predicate.class))).thenReturn(orPredicate);

        Predicate result = DiagnosticReportSpecs.matchesQuery("infection").toPredicate(root, query, cb);

        assertThat(result).isSameAs(orPredicate);
    }
}
