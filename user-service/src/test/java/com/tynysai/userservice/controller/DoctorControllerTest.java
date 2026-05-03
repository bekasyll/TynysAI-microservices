package com.tynysai.userservice.controller;

import com.tynysai.common.dto.ApiResponse;
import com.tynysai.common.dto.PageResponse;
import com.tynysai.userservice.dto.request.UpdateDoctorProfileRequest;
import com.tynysai.userservice.dto.response.DoctorProfileResponse;
import com.tynysai.userservice.service.DoctorProfileService;
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
class DoctorControllerTest {
    @Mock
    private DoctorProfileService doctorProfileService;

    @InjectMocks
    private DoctorController controller;

    @Test
    void getMyProfile_delegates() {
        UUID userId = UUID.randomUUID();
        DoctorProfileResponse data = DoctorProfileResponse.builder().userId(userId).build();
        when(doctorProfileService.getMyProfile(userId)).thenReturn(data);

        ApiResponse<DoctorProfileResponse> resp = controller.getMyProfile(userId);

        assertThat(resp.getData()).isSameAs(data);
    }

    @Test
    void getByUserId_delegates() {
        UUID userId = UUID.randomUUID();
        DoctorProfileResponse data = DoctorProfileResponse.builder().userId(userId).build();
        when(doctorProfileService.getByUserId(userId)).thenReturn(data);

        ApiResponse<DoctorProfileResponse> resp = controller.getByUserId(userId);

        assertThat(resp.getData()).isSameAs(data);
    }

    @Test
    void listApprovedDoctors_delegates() {
        Pageable pageable = PageRequest.of(0, 10);
        PageResponse<DoctorProfileResponse> page = PageResponse.<DoctorProfileResponse>builder()
                .content(List.of()).page(0).size(10).totalElements(0).totalPages(0).build();
        when(doctorProfileService.listApproved(pageable)).thenReturn(page);

        ApiResponse<PageResponse<DoctorProfileResponse>> resp = controller.listApprovedDoctors(pageable);

        assertThat(resp.getData()).isSameAs(page);
    }

    @Test
    void updateMyProfile_delegates() {
        UUID userId = UUID.randomUUID();
        UpdateDoctorProfileRequest req = new UpdateDoctorProfileRequest();
        DoctorProfileResponse data = DoctorProfileResponse.builder().userId(userId).build();
        when(doctorProfileService.updateMyProfile(userId, req)).thenReturn(data);

        ApiResponse<DoctorProfileResponse> resp = controller.updateMyProfile(userId, req);

        assertThat(resp.getData()).isSameAs(data);
        assertThat(resp.getMessage()).contains("updated");
    }
}
