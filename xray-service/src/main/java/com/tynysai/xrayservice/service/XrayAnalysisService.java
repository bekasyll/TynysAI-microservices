package com.tynysai.xrayservice.service;

import com.tynysai.xrayservice.client.UserClient;
import com.tynysai.xrayservice.client.dto.UserDto;
import com.tynysai.common.dto.PageResponse;
import com.tynysai.xrayservice.dto.request.DoctorValidationRequest;
import com.tynysai.xrayservice.dto.response.AiAnalysisResult;
import com.tynysai.xrayservice.dto.response.XrayAnalysisResponse;
import com.tynysai.xrayservice.exception.BadRequestException;
import com.tynysai.xrayservice.exception.ResourceNotFoundException;
import com.tynysai.xrayservice.kafka.NotificationEventPublisher;
import com.tynysai.xrayservice.model.XrayAnalysis;
import com.tynysai.xrayservice.model.enums.AnalysisStatus;
import com.tynysai.xrayservice.repository.XrayAnalysisRepository;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class XrayAnalysisService {
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("image/jpeg", "image/jpg", "image/png");

    private final XrayAnalysisRepository repository;
    private final FileStorageService fileStorageService;
    private final AiAnalysisService aiAnalysisService;
    private final UserClient userClient;
    private final NotificationEventPublisher notificationPublisher;
    private final ObjectMapper objectMapper;
    private final ApplicationContext applicationContext;

    public XrayAnalysisResponse getById(Long analysisId) {
        return toResponse(repository.findById(analysisId)
                .orElseThrow(() -> new ResourceNotFoundException("XrayAnalysis", "id", analysisId)));
    }

    public XrayAnalysisResponse getByIdForPatient(Long analysisId, UUID patientId) {
        return toResponse(repository.findByIdAndPatientId(analysisId, patientId)
                .orElseThrow(() -> new ResourceNotFoundException("XrayAnalysis", "id", analysisId)));
    }

    public XrayAnalysisResponse getByIdForDoctor(Long analysisId, UUID doctorId) {
        return toResponse(repository.findByIdAndAssignedDoctorId(analysisId, doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("XrayAnalysis", "id", analysisId)));
    }

    public PageResponse<XrayAnalysisResponse> getPatientAnalyses(UUID patientId, Pageable pageable) {
        Page<XrayAnalysis> page = repository.findByPatientIdOrderByUploadedAtDesc(patientId, pageable);
        return PageResponse.from(page.map(this::toResponse));
    }

    public PageResponse<XrayAnalysisResponse> getAssignedToDoctor(UUID doctorId, Pageable pageable) {
        Page<XrayAnalysis> page = repository.findByAssignedDoctorIdOrderByUploadedAtDesc(doctorId, pageable);
        return PageResponse.from(page.map(this::toResponse));
    }

    public PageResponse<XrayAnalysisResponse> getAllAnalyses(Pageable pageable) {
        Page<XrayAnalysis> page = repository.findAll(pageable);
        return PageResponse.from(page.map(this::toResponse));
    }

    @Transactional
    public XrayAnalysisResponse uploadAndAnalyze(UUID patientId,
                                                 MultipartFile file,
                                                 String patientNotes,
                                                 UUID assignedDoctorId) {
        validateChestXrayFile(file);
        UserDto patient = userClient.getById(patientId);
        if (assignedDoctorId != null) {
            userClient.getById(assignedDoctorId);
        }

        XrayAnalysis saved = saveAndStoreFile(patient.getId(), assignedDoctorId,
                file, patientNotes, patientId);
        Long analysisId = saved.getId();

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                applicationContext.getBean(XrayAnalysisService.class).processAnalysisAsync(analysisId);
                if (assignedDoctorId != null) {
                    notificationPublisher.publish(assignedDoctorId,
                            "XRAY_ASSIGNED",
                            analysisId.toString(),
                            "XrayAnalysis",
                            Map.of("patientName", patient.getFullName()));
                }
            }
        });

        log.info("X-ray uploaded for patient {}, analysis ID: {}", patientId, analysisId);
        return toResponse(saved);
    }

    @Transactional
    public XrayAnalysisResponse uploadAndAnalyzeByDoctor(UUID doctorId,
                                                         MultipartFile file,
                                                         String notes) {
        validateChestXrayFile(file);
        userClient.getById(doctorId);
        XrayAnalysis saved = saveAndStoreFile(null, doctorId, file, notes, doctorId);
        Long analysisId = saved.getId();

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                applicationContext.getBean(XrayAnalysisService.class).processAnalysisAsync(analysisId);
            }
        });

        log.info("Doctor {} uploaded self-analysis, analysis ID: {}", doctorId, analysisId);
        return toResponse(saved);
    }

    private XrayAnalysis saveAndStoreFile(UUID patientId, UUID assignedDoctorId,
                                          MultipartFile file,
                                          String notes, UUID fileOwnerId) {
        XrayAnalysis analysis = XrayAnalysis.builder()
                .patientId(patientId)
                .assignedDoctorId(assignedDoctorId)
                .originalFileName(file.getOriginalFilename())
                .contentType(file.getContentType())
                .fileSizeBytes(file.getSize())
                .status(AnalysisStatus.PENDING)
                .patientNotes(notes)
                .storedFilePath("")
                .build();

        XrayAnalysis saved = repository.save(analysis);
        String storedPath = fileStorageService.store(file, fileOwnerId, saved.getId());
        saved.setStoredFilePath(storedPath);
        return repository.save(saved);
    }

    private void validateChestXrayFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File is required");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new BadRequestException(
                    "Only chest X-ray images in JPEG or PNG format are supported");
        }
    }

    @Async("aiAnalysisExecutor")
    @Transactional
    public void processAnalysisAsync(Long analysisId) {
        XrayAnalysis analysis = repository.findById(analysisId)
                .orElseThrow(() -> new ResourceNotFoundException("XrayAnalysis", "id", analysisId));

        analysis.setStatus(AnalysisStatus.PROCESSING);
        repository.save(analysis);

        try {
            AiAnalysisResult result =
                    aiAnalysisService.analyzeImage(fileStorageService.resolve(analysis.getStoredFilePath()).toString());

            analysis.setAiPrimaryDiagnosis(result.getPrimaryDiagnosis());
            analysis.setAiConfidence(result.getPrimaryConfidence());
            analysis.setAiFindings(result.getFindings());

            if (result.getDetectedAbnormalities() != null && !result.getDetectedAbnormalities().isEmpty()) {
                analysis.setAiDetectedAbnormalities(String.join(", ", result.getDetectedAbnormalities()));
            }

            try {
                analysis.setAiAllPredictionsJson(objectMapper.writeValueAsString(result.getAllPredictions()));
            } catch (JacksonException e) {
                log.warn("Failed to serialize AI predictions: {}", e.getMessage());
            }

            boolean requiresReview = analysis.getAssignedDoctorId() != null || result.isRequiresDoctorReview();
            analysis.setStatus(requiresReview ? AnalysisStatus.REQUIRES_REVIEW : AnalysisStatus.COMPLETED);

            if (analysis.getPatientId() != null) {
                notificationPublisher.publish(analysis.getPatientId(),
                        requiresReview ? "ANALYSIS_REQUIRES_REVIEW" : "ANALYSIS_COMPLETED",
                        analysisId.toString(),
                        "XrayAnalysis",
                        Map.of("diagnosis", result.getPrimaryDiagnosis().name()));
            }

            analysis.setAnalyzedAt(LocalDateTime.now());
            repository.save(analysis);

            log.info("AI analysis completed for analysis {}: {}", analysisId, result.getPrimaryDiagnosis());
        } catch (Exception e) {
            log.error("AI analysis failed for analysis {}: {}", analysisId, e.getMessage(), e);
            analysis.setStatus(AnalysisStatus.FAILED);
            repository.save(analysis);
        }
    }

    @Transactional
    public XrayAnalysisResponse validate(Long analysisId, UUID doctorId, DoctorValidationRequest request) {
        XrayAnalysis analysis = repository.findById(analysisId)
                .orElseThrow(() -> new ResourceNotFoundException("XrayAnalysis", "id", analysisId));

        if (analysis.getStatus() == AnalysisStatus.PENDING ||
                analysis.getStatus() == AnalysisStatus.PROCESSING) {
            throw new BadRequestException("Analysis is not yet ready for validation");
        }

        UserDto doctor = userClient.getById(doctorId);

        analysis.setValidatedByDoctorId(doctorId);
        analysis.setDoctorDiagnosis(request.getDoctorDiagnosis());
        analysis.setDoctorNotes(request.getDoctorNotes());
        analysis.setValidatedAt(LocalDateTime.now());
        analysis.setStatus(AnalysisStatus.VALIDATED);

        repository.save(analysis);

        if (analysis.getPatientId() != null) {
            notificationPublisher.publish(analysis.getPatientId(),
                    "ANALYSIS_VALIDATED",
                    analysisId.toString(),
                    "XrayAnalysis",
                    Map.of(
                            "doctorName", doctor.getFullName(),
                            "diagnosis", request.getDoctorDiagnosis().name()
                    ));
        }

        return toResponse(analysis);
    }

    /**
     * Called from the Kafka report-events listener when a diagnostic report
     * referencing this analysis is created.
     */
    @Transactional
    public void markCompleted(Long analysisId) {
        repository.findById(analysisId).ifPresent(analysis -> {
            analysis.setStatus(AnalysisStatus.COMPLETED);
            repository.save(analysis);
        });
    }

    @Transactional
    public void delete(Long analysisId, UUID patientId) {
        XrayAnalysis analysis = repository.findByIdAndPatientId(analysisId, patientId)
                .orElseThrow(() -> new ResourceNotFoundException("XrayAnalysis", "id", analysisId));
        fileStorageService.delete(analysis.getStoredFilePath());
        repository.delete(analysis);
    }

    private XrayAnalysisResponse toResponse(XrayAnalysis a) {
        UserDto patient = a.getPatientId() != null ? userClient.tryFetchUser(a.getPatientId()) : null;
        UserDto assignedDoctor = a.getAssignedDoctorId() != null ? userClient.tryFetchUser(a.getAssignedDoctorId()) : null;
        UserDto validator = a.getValidatedByDoctorId() != null ? userClient.tryFetchUser(a.getValidatedByDoctorId()) : null;

        return XrayAnalysisResponse.builder()
                .id(a.getId())
                .patientId(a.getPatientId())
                .patientName(patient != null ? patient.getFullName() : null)
                .assignedDoctorId(a.getAssignedDoctorId())
                .assignedDoctorName(assignedDoctor != null ? assignedDoctor.getFullName() : null)
                .originalFileName(a.getOriginalFileName())
                .contentType(a.getContentType())
                .fileSizeBytes(a.getFileSizeBytes())
                .status(a.getStatus())
                .aiPrimaryDiagnosis(a.getAiPrimaryDiagnosis())
                .aiPrimaryDiagnosisDisplayName(a.getAiPrimaryDiagnosis() != null
                        ? a.getAiPrimaryDiagnosis().getDisplayName() : null)
                .aiConfidence(a.getAiConfidence())
                .aiFindings(a.getAiFindings())
                .aiDetectedAbnormalities(a.getAiDetectedAbnormalities())
                .aiAllPredictionsJson(a.getAiAllPredictionsJson())
                .validatedByDoctorId(a.getValidatedByDoctorId())
                .validatedByDoctorName(validator != null ? validator.getFullName() : null)
                .doctorDiagnosis(a.getDoctorDiagnosis())
                .doctorDiagnosisDisplayName(a.getDoctorDiagnosis() != null
                        ? a.getDoctorDiagnosis().getDisplayName() : null)
                .doctorNotes(a.getDoctorNotes())
                .validatedAt(a.getValidatedAt())
                .patientNotes(a.getPatientNotes())
                .uploadedAt(a.getUploadedAt())
                .analyzedAt(a.getAnalyzedAt())
                .build();
    }
}
