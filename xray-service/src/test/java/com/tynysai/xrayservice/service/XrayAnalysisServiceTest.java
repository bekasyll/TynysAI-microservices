package com.tynysai.xrayservice.service;

import com.tynysai.common.client.dto.UserDto;
import com.tynysai.common.dto.PageResponse;
import com.tynysai.xrayservice.client.UserClient;
import com.tynysai.xrayservice.dto.response.AiAnalysisResult;
import com.tynysai.xrayservice.dto.response.XrayAnalysisResponse;
import com.tynysai.xrayservice.exception.BadRequestException;
import com.tynysai.xrayservice.exception.ResourceNotFoundException;
import com.tynysai.xrayservice.kafka.NotificationEventPublisher;
import com.tynysai.xrayservice.model.XrayAnalysis;
import com.tynysai.xrayservice.model.enums.AnalysisStatus;
import com.tynysai.xrayservice.model.enums.DiseaseType;
import com.tynysai.xrayservice.repository.XrayAnalysisRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.ObjectMapper;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class XrayAnalysisServiceTest {

    @Mock
    private XrayAnalysisRepository repository;
    @Mock
    private FileStorageService fileStorageService;
    @Mock
    private AiAnalysisService aiAnalysisService;
    @Mock
    private UserClient userClient;
    @Mock
    private NotificationEventPublisher notificationPublisher;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private ApplicationContext applicationContext;

    @InjectMocks
    private XrayAnalysisService service;

    private UUID patientId;
    private UUID doctorId;

    @BeforeEach
    void setUp() {
        patientId = UUID.randomUUID();
        doctorId = UUID.randomUUID();
        // Activate transaction sync so registerSynchronization() doesn't blow up
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.initSynchronization();
        }
    }

    @AfterEach
    void tearDown() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clear();
        }
    }

    // ---------- uploadAndAnalyze ----------

    @Test
    void uploadAndAnalyze_savesFileAndPersists_whenAllInputsValid() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "chest.png", "image/png", "fake-bytes".getBytes());

        UserDto patient = patientDto();
        when(userClient.getById(patientId)).thenReturn(patient);
        UserDto doctor = doctorDto();
        when(userClient.getById(doctorId)).thenReturn(doctor);

        when(repository.save(any(XrayAnalysis.class))).thenAnswer(inv -> {
            XrayAnalysis a = inv.getArgument(0);
            if (a.getId() == null) a.setId(11L);
            return a;
        });
        when(fileStorageService.store(eq(file), eq(patientId), anyLong())).thenReturn("path/to/file.png");
        lenient().when(userClient.tryFetchUser(any())).thenReturn(null);

        XrayAnalysisResponse response = service.uploadAndAnalyze(patientId, file, "Some notes", doctorId);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(11L);
        verify(fileStorageService).store(eq(file), eq(patientId), eq(11L));
        verify(repository, times(2)).save(any(XrayAnalysis.class));

        // Capture the saved analysis to confirm fields
        ArgumentCaptor<XrayAnalysis> captor = ArgumentCaptor.forClass(XrayAnalysis.class);
        verify(repository, times(2)).save(captor.capture());
        XrayAnalysis last = captor.getAllValues().get(captor.getAllValues().size() - 1);
        assertThat(last.getStoredFilePath()).isEqualTo("path/to/file.png");
        assertThat(last.getOriginalFileName()).isEqualTo("chest.png");
        assertThat(last.getContentType()).isEqualTo("image/png");
        assertThat(last.getStatus()).isEqualTo(AnalysisStatus.PENDING);
    }

    @Test
    void uploadAndAnalyze_throwsBadRequest_whenFileEmpty() {
        MockMultipartFile empty = new MockMultipartFile(
                "file", "x.png", "image/png", new byte[0]);

        assertThatThrownBy(() -> service.uploadAndAnalyze(patientId, empty, null, null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("File is required");

        verify(repository, never()).save(any());
        verify(fileStorageService, never()).store(any(), any(), anyLong());
    }

    @Test
    void uploadAndAnalyze_throwsBadRequest_whenFileNull() {
        assertThatThrownBy(() -> service.uploadAndAnalyze(patientId, null, null, null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("File is required");
    }

    @Test
    void uploadAndAnalyze_throwsBadRequest_whenContentTypeUnsupported() {
        MockMultipartFile pdf = new MockMultipartFile(
                "file", "report.pdf", "application/pdf", "%PDF".getBytes());

        assertThatThrownBy(() -> service.uploadAndAnalyze(patientId, pdf, null, null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("JPEG or PNG");
    }

    @Test
    void uploadAndAnalyze_throwsBadRequest_whenContentTypeNull() {
        MockMultipartFile noType = new MockMultipartFile(
                "file", "x.png", null, "data".getBytes());

        assertThatThrownBy(() -> service.uploadAndAnalyze(patientId, noType, null, null))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void uploadAndAnalyze_skipsDoctorLookup_whenAssignedDoctorIdNull() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "chest.jpg", "image/jpeg", "bytes".getBytes());

        UserDto patient = patientDto();
        when(userClient.getById(patientId)).thenReturn(patient);
        when(repository.save(any(XrayAnalysis.class))).thenAnswer(inv -> {
            XrayAnalysis a = inv.getArgument(0);
            if (a.getId() == null) a.setId(7L);
            return a;
        });
        when(fileStorageService.store(eq(file), eq(patientId), anyLong())).thenReturn("p");
        lenient().when(userClient.tryFetchUser(any())).thenReturn(null);

        service.uploadAndAnalyze(patientId, file, null, null);

        // userClient.getById should be called once - just for patient
        verify(userClient, times(1)).getById(any());
    }

    // ---------- uploadAndAnalyzeByDoctor ----------

    @Test
    void uploadAndAnalyzeByDoctor_savesAnalysisWithoutPatient_whenCalled() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "doc.png", "image/png", "data".getBytes());

        when(userClient.getById(doctorId)).thenReturn(doctorDto());
        when(repository.save(any(XrayAnalysis.class))).thenAnswer(inv -> {
            XrayAnalysis a = inv.getArgument(0);
            if (a.getId() == null) a.setId(33L);
            return a;
        });
        when(fileStorageService.store(eq(file), eq(doctorId), anyLong())).thenReturn("doc/33.png");
        lenient().when(userClient.tryFetchUser(any())).thenReturn(null);

        XrayAnalysisResponse response = service.uploadAndAnalyzeByDoctor(doctorId, file, "self note");

        assertThat(response.getId()).isEqualTo(33L);
        ArgumentCaptor<XrayAnalysis> captor = ArgumentCaptor.forClass(XrayAnalysis.class);
        verify(repository, times(2)).save(captor.capture());
        XrayAnalysis first = captor.getAllValues().get(0);
        assertThat(first.getPatientId()).isNull();
        assertThat(first.getAssignedDoctorId()).isEqualTo(doctorId);
        assertThat(first.getPatientNotes()).isEqualTo("self note");
    }

    @Test
    void uploadAndAnalyzeByDoctor_throwsBadRequest_whenFileInvalid() {
        MockMultipartFile bad = new MockMultipartFile(
                "file", "x.txt", "text/plain", "x".getBytes());

        assertThatThrownBy(() -> service.uploadAndAnalyzeByDoctor(doctorId, bad, null))
                .isInstanceOf(BadRequestException.class);

        verify(repository, never()).save(any());
    }

    // ---------- markCompleted ----------

    @Test
    void markCompleted_updatesStatusToCompleted_whenAnalysisExists() {
        XrayAnalysis existing = baseAnalysis(AnalysisStatus.REQUIRES_REVIEW);
        when(repository.findById(5L)).thenReturn(Optional.of(existing));

        service.markCompleted(5L);

        assertThat(existing.getStatus()).isEqualTo(AnalysisStatus.COMPLETED);
        verify(repository).save(existing);
    }

    @Test
    void markCompleted_doesNothing_whenAnalysisMissing() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        service.markCompleted(99L);

        verify(repository, never()).save(any());
    }

    // ---------- delete ----------

    @Test
    void delete_removesFileAndAnalysis_whenAnalysisExists() {
        XrayAnalysis a = baseAnalysis(AnalysisStatus.COMPLETED);
        a.setStoredFilePath("p/3.png");
        when(repository.findByIdAndPatientId(3L, patientId)).thenReturn(Optional.of(a));

        service.delete(3L, patientId);

        verify(fileStorageService).delete("p/3.png");
        verify(repository).delete(a);
    }

    @Test
    void delete_throwsNotFound_whenAnalysisMissing() {
        when(repository.findByIdAndPatientId(99L, patientId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(99L, patientId))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(fileStorageService, never()).delete(any());
        verify(repository, never()).delete(any(XrayAnalysis.class));
    }

    // ---------- processAnalysisAsync ----------

    @Test
    void processAnalysisAsync_marksCompleted_whenAiSucceedsWithoutDoctor() throws Exception {
        XrayAnalysis a = baseAnalysis(AnalysisStatus.PENDING);
        a.setStoredFilePath("p/1.png");
        a.setPatientId(patientId);
        a.setAssignedDoctorId(null);

        when(repository.findById(1L)).thenReturn(Optional.of(a));
        when(repository.save(any(XrayAnalysis.class))).thenAnswer(inv -> inv.getArgument(0));
        Path resolved = Paths.get("/tmp/xray/p/1.png");
        when(fileStorageService.resolve("p/1.png")).thenReturn(resolved);

        AiAnalysisResult result = AiAnalysisResult.builder()
                .primaryDiagnosis(DiseaseType.NORMAL)
                .primaryConfidence(0.95)
                .findings("clear")
                .detectedAbnormalities(List.of())
                .requiresDoctorReview(false)
                .modelVersion("py-1")
                .build();
        when(aiAnalysisService.analyzeImage(resolved.toString())).thenReturn(result);
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        service.processAnalysisAsync(1L);

        assertThat(a.getStatus()).isEqualTo(AnalysisStatus.COMPLETED);
        assertThat(a.getAiPrimaryDiagnosis()).isEqualTo(DiseaseType.NORMAL);
        assertThat(a.getAiConfidence()).isEqualTo(0.95);
        assertThat(a.getAiFindings()).isEqualTo("clear");
        assertThat(a.getAnalyzedAt()).isNotNull();
        verify(notificationPublisher).publish(eq(patientId), eq("ANALYSIS_COMPLETED"), any(), any(), any());
    }

    @Test
    void processAnalysisAsync_marksRequiresReview_whenDoctorAssigned() throws Exception {
        XrayAnalysis a = baseAnalysis(AnalysisStatus.PENDING);
        a.setStoredFilePath("p/2.png");
        a.setPatientId(patientId);
        a.setAssignedDoctorId(doctorId);

        when(repository.findById(2L)).thenReturn(Optional.of(a));
        when(repository.save(any(XrayAnalysis.class))).thenAnswer(inv -> inv.getArgument(0));
        when(fileStorageService.resolve("p/2.png")).thenReturn(Paths.get("/tmp/xray/p/2.png"));

        AiAnalysisResult result = AiAnalysisResult.builder()
                .primaryDiagnosis(DiseaseType.NORMAL)
                .primaryConfidence(0.99)
                .findings("ok")
                .detectedAbnormalities(List.of("Cardiomegaly", "Effusion"))
                .requiresDoctorReview(false)
                .build();
        when(aiAnalysisService.analyzeImage(any())).thenReturn(result);
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        service.processAnalysisAsync(2L);

        assertThat(a.getStatus()).isEqualTo(AnalysisStatus.REQUIRES_REVIEW);
        assertThat(a.getAiDetectedAbnormalities()).contains("Cardiomegaly");
        verify(notificationPublisher).publish(eq(patientId), eq("ANALYSIS_REQUIRES_REVIEW"),
                any(), any(), any());
    }

    @Test
    void processAnalysisAsync_skipsNotification_whenPatientIdNull() throws Exception {
        XrayAnalysis a = baseAnalysis(AnalysisStatus.PENDING);
        a.setStoredFilePath("p/4.png");
        a.setPatientId(null);
        a.setAssignedDoctorId(doctorId);

        when(repository.findById(4L)).thenReturn(Optional.of(a));
        when(repository.save(any(XrayAnalysis.class))).thenAnswer(inv -> inv.getArgument(0));
        when(fileStorageService.resolve("p/4.png")).thenReturn(Paths.get("/tmp/xray/p/4.png"));

        AiAnalysisResult result = AiAnalysisResult.builder()
                .primaryDiagnosis(DiseaseType.NORMAL)
                .primaryConfidence(0.5)
                .findings("ok")
                .detectedAbnormalities(List.of())
                .requiresDoctorReview(true)
                .build();
        when(aiAnalysisService.analyzeImage(any())).thenReturn(result);
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        service.processAnalysisAsync(4L);

        verify(notificationPublisher, never()).publish(any(), any(), any(), any(), any());
    }

    @Test
    void processAnalysisAsync_handlesSerializationFailure_butStillSavesAnalysis() throws Exception {
        XrayAnalysis a = baseAnalysis(AnalysisStatus.PENDING);
        a.setStoredFilePath("p/8.png");
        a.setPatientId(null);
        a.setAssignedDoctorId(null);

        when(repository.findById(8L)).thenReturn(Optional.of(a));
        when(repository.save(any(XrayAnalysis.class))).thenAnswer(inv -> inv.getArgument(0));
        when(fileStorageService.resolve("p/8.png")).thenReturn(Paths.get("/tmp/xray/p/8.png"));

        AiAnalysisResult result = AiAnalysisResult.builder()
                .primaryDiagnosis(DiseaseType.NORMAL)
                .primaryConfidence(0.9)
                .findings("ok")
                .detectedAbnormalities(List.of())
                .requiresDoctorReview(false)
                .build();
        when(aiAnalysisService.analyzeImage(any())).thenReturn(result);
        when(objectMapper.writeValueAsString(any()))
                .thenThrow(new tools.jackson.databind.exc.InvalidDefinitionException(
                        (tools.jackson.core.JsonGenerator) null, "boom", null) {});

        service.processAnalysisAsync(8L);

        assertThat(a.getStatus()).isEqualTo(AnalysisStatus.COMPLETED);
        verify(repository, times(2)).save(a); // initial PROCESSING save + final
    }

    @Test
    void processAnalysisAsync_marksFailed_whenAiThrows() {
        XrayAnalysis a = baseAnalysis(AnalysisStatus.PENDING);
        a.setStoredFilePath("p/5.png");

        when(repository.findById(5L)).thenReturn(Optional.of(a));
        when(repository.save(any(XrayAnalysis.class))).thenAnswer(inv -> inv.getArgument(0));
        when(fileStorageService.resolve("p/5.png")).thenReturn(Paths.get("/tmp/x/5.png"));
        when(aiAnalysisService.analyzeImage(any())).thenThrow(new RuntimeException("ai exploded"));

        service.processAnalysisAsync(5L);

        assertThat(a.getStatus()).isEqualTo(AnalysisStatus.FAILED);
        // First save sets PROCESSING, second sets FAILED
        verify(repository, times(2)).save(a);
        verify(notificationPublisher, never()).publish(any(), any(), any(), any(), any());
    }

    @Test
    void processAnalysisAsync_throwsNotFound_whenAnalysisMissing() {
        when(repository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.processAnalysisAsync(404L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ---------- list/page methods ----------

    @Test
    void getPatientAnalyses_returnsPageResponse_wrappingRepoResult() {
        XrayAnalysis a = baseAnalysis(AnalysisStatus.COMPLETED);
        a.setPatientId(patientId);
        Page<XrayAnalysis> page = new PageImpl<>(List.of(a), PageRequest.of(0, 10), 1);
        when(repository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        lenient().when(userClient.tryFetchUser(any())).thenReturn(null);

        PageResponse<XrayAnalysisResponse> result = service.getPatientAnalyses(
                patientId, AnalysisStatus.COMPLETED, DiseaseType.NORMAL,
                LocalDateTime.now().minusDays(1), LocalDateTime.now(), "x", PageRequest.of(0, 10));

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(repository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void getPatientAnalyses_appliesDefaultSort_whenPageableUnsorted() {
        Page<XrayAnalysis> page = new PageImpl<>(List.of(), PageRequest.of(0, 5), 0);
        when(repository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        service.getPatientAnalyses(patientId, null, null, null, null, null, PageRequest.of(0, 5));

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).findAll(any(Specification.class), captor.capture());
        Sort sort = captor.getValue().getSort();
        assertThat(sort.isSorted()).isTrue();
        assertThat(sort.getOrderFor("uploadedAt")).isNotNull();
    }

    @Test
    void getPatientAnalyses_keepsExistingSort_whenPageableSorted() {
        Page<XrayAnalysis> page = new PageImpl<>(List.of(), PageRequest.of(0, 5, Sort.by("id")), 0);
        when(repository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        service.getPatientAnalyses(patientId, null, null, null, null, null,
                PageRequest.of(0, 5, Sort.by("id")));

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).findAll(any(Specification.class), captor.capture());
        assertThat(captor.getValue().getSort().getOrderFor("id")).isNotNull();
    }

    @Test
    void getAssignedToDoctor_returnsPageResponse_wrappingRepoResult() {
        Page<XrayAnalysis> page = new PageImpl<>(List.of(baseAnalysis(AnalysisStatus.PENDING)),
                PageRequest.of(0, 10), 1);
        when(repository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        lenient().when(userClient.tryFetchUser(any())).thenReturn(null);

        PageResponse<XrayAnalysisResponse> result = service.getAssignedToDoctor(
                doctorId, null, null, null, null, null, PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        verify(repository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void getAssignedToDoctor_invokesPatientNameLookup_whenQueryGiven() {
        Page<XrayAnalysis> page = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
        when(repository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(userClient.searchPatientIds("ivan")).thenReturn(List.of(UUID.randomUUID()));

        service.getAssignedToDoctor(doctorId, null, null, null, null, "ivan", PageRequest.of(0, 10));

        verify(userClient).searchPatientIds("ivan");
    }

    @Test
    void getByPatientId_returnsPageResponse_wrappingRepoResult() {
        Page<XrayAnalysis> page = new PageImpl<>(List.of(baseAnalysis(AnalysisStatus.COMPLETED)),
                PageRequest.of(0, 5), 1);
        when(repository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        lenient().when(userClient.tryFetchUser(any())).thenReturn(null);

        PageResponse<XrayAnalysisResponse> result = service.getByPatientId(
                patientId, null, null, null, null, null, PageRequest.of(0, 5));

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void getAllAnalyses_returnsPageResponse_wrappingRepoResult() {
        Page<XrayAnalysis> page = new PageImpl<>(
                List.of(baseAnalysis(AnalysisStatus.COMPLETED), baseAnalysis(AnalysisStatus.PENDING)),
                PageRequest.of(0, 10), 2);
        when(repository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        lenient().when(userClient.tryFetchUser(any())).thenReturn(null);

        PageResponse<XrayAnalysisResponse> result = service.getAllAnalyses(
                null, null, null, null, null, PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(2);
    }

    // ---------- getById/getByIdForPatient/getByIdForDoctor ----------

    @Test
    void getById_returnsResponse_whenAnalysisExists() {
        XrayAnalysis a = baseAnalysis(AnalysisStatus.COMPLETED);
        a.setId(7L);
        a.setPatientId(patientId);
        when(repository.findById(7L)).thenReturn(Optional.of(a));
        lenient().when(userClient.tryFetchUser(any())).thenReturn(null);

        XrayAnalysisResponse response = service.getById(7L);

        assertThat(response.getId()).isEqualTo(7L);
    }

    @Test
    void getById_throwsNotFound_whenAnalysisMissing() {
        when(repository.findById(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(7L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getByIdForPatient_returnsResponse_whenFound() {
        XrayAnalysis a = baseAnalysis(AnalysisStatus.COMPLETED);
        a.setId(7L);
        a.setPatientId(patientId);
        when(repository.findByIdAndPatientId(7L, patientId)).thenReturn(Optional.of(a));
        lenient().when(userClient.tryFetchUser(any())).thenReturn(null);

        XrayAnalysisResponse response = service.getByIdForPatient(7L, patientId);

        assertThat(response.getId()).isEqualTo(7L);
    }

    @Test
    void getByIdForPatient_throwsNotFound_whenMissing() {
        when(repository.findByIdAndPatientId(7L, patientId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getByIdForPatient(7L, patientId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getByIdForDoctor_returnsResponse_whenFound() {
        XrayAnalysis a = baseAnalysis(AnalysisStatus.COMPLETED);
        a.setId(7L);
        when(repository.findById(7L)).thenReturn(Optional.of(a));
        lenient().when(userClient.tryFetchUser(any())).thenReturn(null);

        XrayAnalysisResponse response = service.getByIdForDoctor(7L, doctorId);

        assertThat(response.getId()).isEqualTo(7L);
    }

    // ---------- toResponse coverage via populated fields ----------

    @Test
    void getById_populatesUserNames_whenUsersResolvable() {
        XrayAnalysis a = baseAnalysis(AnalysisStatus.VALIDATED);
        a.setId(7L);
        a.setPatientId(patientId);
        a.setAssignedDoctorId(doctorId);
        a.setValidatedByDoctorId(doctorId);
        a.setAiPrimaryDiagnosis(DiseaseType.NORMAL);
        a.setDoctorDiagnosis(DiseaseType.BACTERIAL_PNEUMONIA);

        UserDto patient = patientDto();
        UserDto doctor = doctorDto();
        when(repository.findById(7L)).thenReturn(Optional.of(a));
        when(userClient.tryFetchUser(patientId)).thenReturn(patient);
        when(userClient.tryFetchUser(doctorId)).thenReturn(doctor);

        XrayAnalysisResponse response = service.getById(7L);

        assertThat(response.getPatientName()).isEqualTo(patient.getFullName());
        assertThat(response.getAssignedDoctorName()).isEqualTo(doctor.getFullName());
        assertThat(response.getValidatedByDoctorName()).isEqualTo(doctor.getFullName());
        assertThat(response.getAiPrimaryDiagnosisDisplayName()).isNotNull();
        assertThat(response.getDoctorDiagnosisDisplayName()).isNotNull();
    }

    // ---------- helpers ----------

    private XrayAnalysis baseAnalysis(AnalysisStatus status) {
        return XrayAnalysis.builder()
                .id(1L)
                .status(status)
                .originalFileName("x.png")
                .contentType("image/png")
                .storedFilePath("")
                .build();
    }

    private UserDto patientDto() {
        UserDto u = new UserDto();
        u.setId(patientId);
        u.setFullName("Иван Иванов");
        return u;
    }

    private UserDto doctorDto() {
        UserDto u = new UserDto();
        u.setId(doctorId);
        u.setFullName("Dr. House");
        return u;
    }

    @SuppressWarnings("unused")
    private static MultipartFile dummyFile() {
        return new MockMultipartFile("file", "x.png", "image/png", "x".getBytes());
    }
}
