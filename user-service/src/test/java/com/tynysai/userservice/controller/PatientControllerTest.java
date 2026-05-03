package com.tynysai.userservice.controller;

import com.tynysai.common.dto.ApiResponse;
import com.tynysai.common.dto.PageResponse;
import com.tynysai.userservice.dto.request.UpdatePatientProfileRequest;
import com.tynysai.userservice.dto.response.PatientProfileResponse;
import com.tynysai.userservice.service.PatientProfileService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PatientControllerTest {

    @Mock
    private PatientProfileService patientProfileService;

    @InjectMocks
    private PatientController controller;

    @Test
    void getMyProfile_delegates() {
        UUID userId = UUID.randomUUID();
        PatientProfileResponse data = PatientProfileResponse.builder().userId(userId).build();
        when(patientProfileService.getMyProfile(userId)).thenReturn(data);

        ApiResponse<PatientProfileResponse> resp = controller.getMyProfile(userId);

        assertThat(resp.isSuccess()).isTrue();
        assertThat(resp.getData()).isSameAs(data);
    }

    @Test
    void getByUserId_delegates() {
        UUID userId = UUID.randomUUID();
        PatientProfileResponse data = PatientProfileResponse.builder().userId(userId).build();
        when(patientProfileService.getByUserId(userId)).thenReturn(data);

        ApiResponse<PatientProfileResponse> resp = controller.getByUserId(userId);

        assertThat(resp.getData()).isSameAs(data);
    }

    @Test
    void patientsList_delegates() {
        Pageable pageable = PageRequest.of(0, 10);
        PageResponse<PatientProfileResponse> page = PageResponse.<PatientProfileResponse>builder()
                .content(List.of()).page(0).size(10).totalElements(0).totalPages(0).build();
        when(patientProfileService.listPatients(pageable)).thenReturn(page);

        ApiResponse<PageResponse<PatientProfileResponse>> resp = controller.patientsList(pageable);

        assertThat(resp.getData()).isSameAs(page);
    }

    @Test
    void updateMyProfile_delegates() {
        UUID userId = UUID.randomUUID();
        UpdatePatientProfileRequest req = new UpdatePatientProfileRequest();
        PatientProfileResponse data = PatientProfileResponse.builder().userId(userId).build();
        when(patientProfileService.updateMyProfile(userId, req)).thenReturn(data);

        ApiResponse<PatientProfileResponse> resp = controller.updateMyProfile(userId, req);

        assertThat(resp.getData()).isSameAs(data);
        assertThat(resp.getMessage()).contains("updated");
    }
}
