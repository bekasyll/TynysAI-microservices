package com.tynysai.xrayservice.repository;

import com.tynysai.xrayservice.model.XrayAnalysis;
import com.tynysai.xrayservice.model.enums.AnalysisStatus;
import com.tynysai.xrayservice.model.enums.DiseaseType;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class XrayAnalysisSpecsTest {

    @SuppressWarnings({"rawtypes", "unchecked"})
    private Root<XrayAnalysis> root;
    @SuppressWarnings("rawtypes")
    private CriteriaQuery query;
    private CriteriaBuilder cb;
    @SuppressWarnings("rawtypes")
    private Predicate conjunctionPredicate;
    @SuppressWarnings("rawtypes")
    private Predicate disjunctionPredicate;
    @SuppressWarnings("rawtypes")
    private Predicate equalPredicate;
    @SuppressWarnings("rawtypes")
    private Predicate orPredicate;
    @SuppressWarnings("rawtypes")
    private Predicate likePredicate;
    @SuppressWarnings("rawtypes")
    private Predicate gtPredicate;
    @SuppressWarnings("rawtypes")
    private Predicate ltPredicate;
    @SuppressWarnings("rawtypes")
    private Predicate inPredicate;
    @SuppressWarnings("rawtypes")
    private Path path;
    @SuppressWarnings("rawtypes")
    private Expression expression;
    @SuppressWarnings("rawtypes")
    private CriteriaBuilder.In inExpression;

    @BeforeEach
    @SuppressWarnings({"unchecked", "rawtypes"})
    void setUp() {
        root = mock(Root.class);
        query = mock(CriteriaQuery.class);
        cb = mock(CriteriaBuilder.class);
        conjunctionPredicate = mock(Predicate.class);
        disjunctionPredicate = mock(Predicate.class);
        equalPredicate = mock(Predicate.class);
        orPredicate = mock(Predicate.class);
        likePredicate = mock(Predicate.class);
        gtPredicate = mock(Predicate.class);
        ltPredicate = mock(Predicate.class);
        inPredicate = mock(Predicate.class);
        path = mock(Path.class);
        expression = mock(Expression.class);
        inExpression = mock(CriteriaBuilder.In.class);

        lenient().doReturn(conjunctionPredicate).when(cb).conjunction();
        lenient().doReturn(disjunctionPredicate).when(cb).disjunction();
        // cb.equal has overloads (Expression, Object) and (Expression, Expression).
        lenient().doReturn(equalPredicate).when(cb).equal(any(Expression.class), any(Object.class));
        lenient().doReturn(equalPredicate).when(cb).equal(any(Expression.class), any(Expression.class));
        lenient().doReturn(orPredicate).when(cb).or(any(Predicate.class), any(Predicate.class));
        lenient().doReturn(orPredicate).when(cb).or(any(Predicate[].class));
        lenient().doReturn(likePredicate).when(cb).like(any(Expression.class), anyString());
        lenient().doReturn(expression).when(cb).lower(any(Expression.class));
        lenient().doReturn(expression).when(cb).coalesce(any(Expression.class), anyString());
        lenient().doReturn(gtPredicate).when(cb).greaterThanOrEqualTo(any(Expression.class), any(LocalDateTime.class));
        lenient().doReturn(ltPredicate).when(cb).lessThan(any(Expression.class), any(LocalDateTime.class));
        lenient().doReturn(path).when(root).get(anyString());
        lenient().doReturn(inPredicate).when(path).in(any(java.util.Collection.class));
    }

    @Test
    void byPatient_buildsEqualityPredicate() {
        UUID patientId = UUID.randomUUID();
        Specification<XrayAnalysis> spec = XrayAnalysisSpecs.byPatient(patientId);

        Predicate result = spec.toPredicate(root, query, cb);

        assertThat(result).isSameAs(equalPredicate);
        verify(root).get("patientId");
        verify(cb).equal(any(Expression.class), org.mockito.ArgumentMatchers.eq((Object) patientId));
    }

    @Test
    void byAssignedDoctor_buildsEqualityPredicate() {
        UUID doctorId = UUID.randomUUID();
        Specification<XrayAnalysis> spec = XrayAnalysisSpecs.byAssignedDoctor(doctorId);

        Predicate result = spec.toPredicate(root, query, cb);

        assertThat(result).isSameAs(equalPredicate);
        verify(root).get("assignedDoctorId");
    }

    @Test
    void byStatus_returnsConjunction_whenStatusIsNull() {
        Specification<XrayAnalysis> spec = XrayAnalysisSpecs.byStatus(null);

        Predicate result = spec.toPredicate(root, query, cb);

        assertThat(result).isSameAs(conjunctionPredicate);
        verify(cb).conjunction();
    }

    @Test
    void byStatus_buildsEquality_whenStatusGiven() {
        Specification<XrayAnalysis> spec = XrayAnalysisSpecs.byStatus(AnalysisStatus.COMPLETED);

        Predicate result = spec.toPredicate(root, query, cb);

        assertThat(result).isSameAs(equalPredicate);
        verify(root).get("status");
    }

    @Test
    void byDiagnosis_returnsConjunction_whenNull() {
        Specification<XrayAnalysis> spec = XrayAnalysisSpecs.byDiagnosis(null);

        Predicate result = spec.toPredicate(root, query, cb);

        assertThat(result).isSameAs(conjunctionPredicate);
    }

    @Test
    void byDiagnosis_buildsOrPredicate_whenDiagnosisGiven() {
        Specification<XrayAnalysis> spec = XrayAnalysisSpecs.byDiagnosis(DiseaseType.NORMAL);

        Predicate result = spec.toPredicate(root, query, cb);

        assertThat(result).isSameAs(orPredicate);
        verify(root, atLeastOnce()).get("aiPrimaryDiagnosis");
        verify(root, atLeastOnce()).get("doctorDiagnosis");
    }

    @Test
    void uploadedFrom_returnsConjunction_whenNull() {
        Specification<XrayAnalysis> spec = XrayAnalysisSpecs.uploadedFrom(null);

        Predicate result = spec.toPredicate(root, query, cb);

        assertThat(result).isSameAs(conjunctionPredicate);
    }

    @Test
    void uploadedFrom_buildsGtePredicate_whenDateGiven() {
        LocalDateTime from = LocalDateTime.now().minusDays(7);
        Specification<XrayAnalysis> spec = XrayAnalysisSpecs.uploadedFrom(from);

        Predicate result = spec.toPredicate(root, query, cb);

        assertThat(result).isSameAs(gtPredicate);
        verify(cb).greaterThanOrEqualTo(any(Expression.class), org.mockito.ArgumentMatchers.eq(from));
    }

    @Test
    void uploadedTo_returnsConjunction_whenNull() {
        Specification<XrayAnalysis> spec = XrayAnalysisSpecs.uploadedTo(null);

        Predicate result = spec.toPredicate(root, query, cb);

        assertThat(result).isSameAs(conjunctionPredicate);
    }

    @Test
    void uploadedTo_buildsLtPredicate_whenDateGiven() {
        LocalDateTime to = LocalDateTime.now();
        Specification<XrayAnalysis> spec = XrayAnalysisSpecs.uploadedTo(to);

        Predicate result = spec.toPredicate(root, query, cb);

        assertThat(result).isSameAs(ltPredicate);
        verify(cb).lessThan(any(Expression.class), org.mockito.ArgumentMatchers.eq(to));
    }

    @Test
    void patientIdIn_returnsDisjunction_whenCollectionNull() {
        Specification<XrayAnalysis> spec = XrayAnalysisSpecs.patientIdIn(null);

        Predicate result = spec.toPredicate(root, query, cb);

        assertThat(result).isSameAs(disjunctionPredicate);
    }

    @Test
    void patientIdIn_returnsDisjunction_whenCollectionEmpty() {
        Specification<XrayAnalysis> spec = XrayAnalysisSpecs.patientIdIn(List.of());

        Predicate result = spec.toPredicate(root, query, cb);

        assertThat(result).isSameAs(disjunctionPredicate);
    }

    @Test
    void patientIdIn_buildsInPredicate_whenCollectionPopulated() {
        List<UUID> ids = List.of(UUID.randomUUID(), UUID.randomUUID());
        Specification<XrayAnalysis> spec = XrayAnalysisSpecs.patientIdIn(ids);

        Predicate result = spec.toPredicate(root, query, cb);

        assertThat(result).isSameAs(inPredicate);
        verify(root).get("patientId");
        verify(path).in(ids);
    }

    @Test
    void matchesQuery_returnsConjunction_whenNull() {
        Specification<XrayAnalysis> spec = XrayAnalysisSpecs.matchesQuery(null);

        Predicate result = spec.toPredicate(root, query, cb);

        assertThat(result).isSameAs(conjunctionPredicate);
    }

    @Test
    void matchesQuery_returnsConjunction_whenBlank() {
        Specification<XrayAnalysis> spec = XrayAnalysisSpecs.matchesQuery("   ");

        Predicate result = spec.toPredicate(root, query, cb);

        assertThat(result).isSameAs(conjunctionPredicate);
    }

    @Test
    void matchesQuery_buildsOrOfLikes_whenQueryGiven() {
        Specification<XrayAnalysis> spec = XrayAnalysisSpecs.matchesQuery("Pneumonia");

        Predicate result = spec.toPredicate(root, query, cb);

        assertThat(result).isSameAs(orPredicate);
        verify(cb, atLeastOnce()).like(any(Expression.class), anyString());
    }
}
