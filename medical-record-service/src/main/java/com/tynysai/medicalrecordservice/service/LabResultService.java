package com.tynysai.medicalrecordservice.service;

import com.tynysai.medicalrecordservice.client.UserClient;
import com.tynysai.medicalrecordservice.client.dto.UserDto;
import com.tynysai.medicalrecordservice.dto.PageResponse;
import com.tynysai.medicalrecordservice.dto.request.LabResultRequest;
import com.tynysai.medicalrecordservice.dto.response.LabResultResponse;
import com.tynysai.medicalrecordservice.exception.BadRequestException;
import com.tynysai.medicalrecordservice.exception.ResourceNotFoundException;
import com.tynysai.medicalrecordservice.kafka.NotificationEventPublisher;
import com.tynysai.medicalrecordservice.model.LabResult;
import com.tynysai.medicalrecordservice.repository.LabResultRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LabResultService {
    private final LabResultRepository labResultRepository;
    private final UserClient userClient;
    private final NotificationEventPublisher notificationEventPublisher;

    @Transactional
    public LabResultResponse create(Long doctorId, LabResultRequest request) {
        UserDto doctor = userClient.getById(doctorId);
        UserDto patient = userClient.getById(request.getPatientId());

        if (!"PATIENT".equalsIgnoreCase(patient.getRole())) {
            throw new BadRequestException("Target user is not a patient");
        }

        LabResult lab = mapToEntity(request, patient.getId(), doctor.getId());
        LabResult saved = labResultRepository.save(lab);

        notificationEventPublisher.publish(patient.getId(),
                "LAB_RESULT_ADDED",
                saved.getId().toString(),
                "LabResult",
                Map.of("testType", request.getTestType().name()));

        return toResponse(saved, patient, doctor);
    }

    public LabResultResponse getById(Long labResultId) {
        return toResponse(labResultRepository.findById(labResultId)
                .orElseThrow(() -> new ResourceNotFoundException("LabResult", "id", labResultId)));
    }

    public LabResultResponse getByIdForPatient(Long labResultId, Long patientId) {
        return toResponse(labResultRepository.findByIdAndPatientId(labResultId, patientId)
                .orElseThrow(() -> new ResourceNotFoundException("LabResult", "id", labResultId)));
    }

    public PageResponse<LabResultResponse> getPatientLabResults(Long patientId, Pageable pageable) {
        Page<LabResult> page = labResultRepository.findByPatientIdOrderByTestDateDesc(patientId, pageable);
        return PageResponse.from(page.map(this::toResponse));
    }

    @Transactional
    public void delete(Long labResultId) {
        LabResult lab = labResultRepository.findById(labResultId)
                .orElseThrow(() -> new ResourceNotFoundException("LabResult", "id", labResultId));
        labResultRepository.delete(lab);
    }

    private LabResult mapToEntity(LabResultRequest request, Long patientId, Long doctorId) {
        return LabResult.builder()
                .patientId(patientId)
                .addedByDoctorId(doctorId)
                .testType(request.getTestType())
                .testDate(request.getTestDate())
                .labName(request.getLabName())
                .hemoglobin(request.getHemoglobin())
                .wbc(request.getWbc())
                .rbc(request.getRbc())
                .platelets(request.getPlatelets())
                .hematocrit(request.getHematocrit())
                .neutrophils(request.getNeutrophils())
                .lymphocytes(request.getLymphocytes())
                .monocytes(request.getMonocytes())
                .eosinophils(request.getEosinophils())
                .crp(request.getCrp())
                .esr(request.getEsr())
                .proCalcitonin(request.getProCalcitonin())
                .ferritin(request.getFerritin())
                .ldh(request.getLdh())
                .dDimer(request.getDDimer())
                .glucose(request.getGlucose())
                .creatinine(request.getCreatinine())
                .urea(request.getUrea())
                .albumin(request.getAlbumin())
                .totalProtein(request.getTotalProtein())
                .alt(request.getAlt())
                .ast(request.getAst())
                .bilirubin(request.getBilirubin())
                .ph(request.getPh())
                .pao2(request.getPao2())
                .paco2(request.getPaco2())
                .hco3(request.getHco3())
                .spo2(request.getSpo2())
                .fev1(request.getFev1())
                .fvc(request.getFvc())
                .fev1FvcRatio(request.getFev1FvcRatio())
                .cultureResult(request.getCultureResult())
                .pathogenFound(request.getPathogenFound())
                .sensitivityResult(request.getSensitivityResult())
                .igraResult(request.getIgraResult())
                .mantouxResult(request.getMantouxResult())
                .mantouxInduratMm(request.getMantouxInduratMm())
                .pcrResult(request.getPcrResult())
                .pcrCtValue(request.getPcrCtValue())
                .notes(request.getNotes())
                .rawResultText(request.getRawResultText())
                .build();
    }

    private LabResultResponse toResponse(LabResult labResult) {
        UserDto patient = userClient.tryGetById(labResult.getPatientId());
        UserDto doctor = labResult.getAddedByDoctorId() != null ? userClient.tryGetById(labResult.getAddedByDoctorId()) : null;
        return toResponse(labResult, patient, doctor);
    }

    private LabResultResponse toResponse(LabResult labResult, UserDto patient, UserDto doctor) {
        return LabResultResponse.builder()
                .id(labResult.getId())
                .patientId(labResult.getPatientId())
                .patientName(patient != null ? patient.getFullName() : null)
                .addedByDoctorId(labResult.getAddedByDoctorId())
                .addedByDoctorName(doctor != null ? doctor.getFullName() : null)
                .testType(labResult.getTestType())
                .testTypeDisplayName(labResult.getTestType().getDisplayName())
                .testDate(labResult.getTestDate())
                .labName(labResult.getLabName())
                .hemoglobin(labResult.getHemoglobin())
                .wbc(labResult.getWbc())
                .rbc(labResult.getRbc())
                .platelets(labResult.getPlatelets())
                .hematocrit(labResult.getHematocrit())
                .neutrophils(labResult.getNeutrophils())
                .lymphocytes(labResult.getLymphocytes())
                .monocytes(labResult.getMonocytes())
                .eosinophils(labResult.getEosinophils())
                .crp(labResult.getCrp())
                .esr(labResult.getEsr())
                .proCalcitonin(labResult.getProCalcitonin())
                .ferritin(labResult.getFerritin())
                .ldh(labResult.getLdh())
                .dDimer(labResult.getDDimer())
                .glucose(labResult.getGlucose())
                .creatinine(labResult.getCreatinine())
                .urea(labResult.getUrea())
                .albumin(labResult.getAlbumin())
                .totalProtein(labResult.getTotalProtein())
                .alt(labResult.getAlt())
                .ast(labResult.getAst())
                .bilirubin(labResult.getBilirubin())
                .ph(labResult.getPh())
                .pao2(labResult.getPao2())
                .paco2(labResult.getPaco2())
                .hco3(labResult.getHco3())
                .spo2(labResult.getSpo2())
                .fev1(labResult.getFev1())
                .fvc(labResult.getFvc())
                .fev1FvcRatio(labResult.getFev1FvcRatio())
                .cultureResult(labResult.getCultureResult())
                .pathogenFound(labResult.getPathogenFound())
                .sensitivityResult(labResult.getSensitivityResult())
                .igraResult(labResult.getIgraResult())
                .mantouxResult(labResult.getMantouxResult())
                .mantouxInduratMm(labResult.getMantouxInduratMm())
                .pcrResult(labResult.getPcrResult())
                .pcrCtValue(labResult.getPcrCtValue())
                .notes(labResult.getNotes())
                .rawResultText(labResult.getRawResultText())
                .createdAt(labResult.getCreatedAt())
                .build();
    }
}