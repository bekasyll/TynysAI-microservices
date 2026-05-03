package com.tynysai.userservice.controller;

import com.tynysai.common.dto.ApiResponse;
import com.tynysai.common.dto.PageResponse;
import com.tynysai.userservice.dto.request.RegisterDoctorRequest;
import com.tynysai.userservice.dto.request.RegisterPatientRequest;
import com.tynysai.userservice.dto.request.UpdateWorkScheduleRequest;
import com.tynysai.userservice.dto.response.AdminStatsResponse;
import com.tynysai.userservice.dto.response.DoctorProfileResponse;
import com.tynysai.userservice.dto.response.RegisterResponse;
import com.tynysai.userservice.dto.response.UserResponse;
import com.tynysai.userservice.model.TimeRange;
import com.tynysai.userservice.model.enums.Role;
import com.tynysai.userservice.service.AdminService;
import com.tynysai.userservice.service.AuthService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminControllerTest {
    @Mock
    private AdminService adminService;
    @Mock
    private AuthService authService;

    @InjectMocks
    private AdminController controller;

    @Test
    void getStats_delegates() {
        AdminStatsResponse data = AdminStatsResponse.builder().totalUsers(5).build();
        when(adminService.getStats()).thenReturn(data);

        ApiResponse<AdminStatsResponse> resp = controller.getStats();

        assertThat(resp.getData()).isSameAs(data);
    }

    @Test
    void getAllUsers_buildsPageRequestSortedByCreatedAtDesc() {
        PageResponse<UserResponse> page = PageResponse.<UserResponse>builder()
                .content(List.of()).page(0).size(20).build();
        when(adminService.getAllUsers(any(Pageable.class))).thenReturn(page);

        ApiResponse<PageResponse<UserResponse>> resp = controller.getAllUsers(0, 20);

        assertThat(resp.getData()).isSameAs(page);
    }

    @Test
    void getUsersByRole_delegates() {
        PageResponse<UserResponse> page = PageResponse.<UserResponse>builder()
                .content(List.of()).page(0).size(20).build();
        when(adminService.getUsersByRole(eq(Role.DOCTOR), any(Pageable.class))).thenReturn(page);

        ApiResponse<PageResponse<UserResponse>> resp = controller.getUsersByRole(Role.DOCTOR, 0, 20);

        assertThat(resp.getData()).isSameAs(page);
    }

    @Test
    void searchUsers_delegates() {
        PageResponse<UserResponse> page = PageResponse.<UserResponse>builder()
                .content(List.of()).page(0).size(20).build();
        when(adminService.searchUsers(eq(Role.PATIENT), eq("ann"), any(Pageable.class))).thenReturn(page);

        ApiResponse<PageResponse<UserResponse>> resp = controller.searchUsers(Role.PATIENT, "ann", 0, 20);

        assertThat(resp.getData()).isSameAs(page);
    }

    @Test
    void createPatient_delegates() {
        RegisterPatientRequest req = new RegisterPatientRequest();
        RegisterResponse data = RegisterResponse.builder().role(Role.PATIENT).build();
        when(authService.registerPatient(req)).thenReturn(data);

        ApiResponse<RegisterResponse> resp = controller.createPatient(req);

        assertThat(resp.getData()).isSameAs(data);
        assertThat(resp.getMessage()).contains("Patient created");
    }

    @Test
    void createDoctor_delegates() {
        RegisterDoctorRequest req = new RegisterDoctorRequest();
        RegisterResponse data = RegisterResponse.builder().role(Role.DOCTOR).build();
        when(authService.registerDoctor(req)).thenReturn(data);

        ApiResponse<RegisterResponse> resp = controller.createDoctor(req);

        assertThat(resp.getData()).isSameAs(data);
        assertThat(resp.getMessage()).contains("awaiting approval");
    }

    @Test
    void toggleUserStatus_delegates() {
        UUID id = UUID.randomUUID();
        UserResponse data = UserResponse.builder().id(id).build();
        when(adminService.toggleStatus(id)).thenReturn(data);

        ApiResponse<UserResponse> resp = controller.toggleUserStatus(id);

        assertThat(resp.getData()).isSameAs(data);
    }

    @Test
    void deleteUser_delegates() {
        UUID id = UUID.randomUUID();

        ApiResponse<Void> resp = controller.deleteUser(id);

        verify(adminService).deleteUser(id);
        assertThat(resp.getMessage()).contains("User deleted");
    }

    @Test
    void logoutSessions_delegates() {
        UUID id = UUID.randomUUID();

        ApiResponse<Void> resp = controller.logoutSessions(id);

        verify(adminService).logoutSessions(id);
        assertThat(resp.getMessage()).contains("Sessions revoked");
    }

    @Test
    void getPendingDoctors_delegates() {
        PageResponse<DoctorProfileResponse> page = PageResponse.<DoctorProfileResponse>builder()
                .content(List.of()).page(0).size(20).build();
        when(adminService.getPendingDoctors(eq("ann"), any(Pageable.class))).thenReturn(page);

        ApiResponse<PageResponse<DoctorProfileResponse>> resp = controller.getPendingDoctors(0, 20, "ann");

        assertThat(resp.getData()).isSameAs(page);
    }

    @Test
    void approveDoctor_delegates() {
        UUID id = UUID.randomUUID();
        DoctorProfileResponse data = DoctorProfileResponse.builder().userId(id).approved(true).build();
        when(adminService.approveDoctor(id)).thenReturn(data);

        ApiResponse<DoctorProfileResponse> resp = controller.approveDoctor(id);

        assertThat(resp.getData()).isSameAs(data);
    }

    @Test
    void rejectDoctor_delegates() {
        UUID id = UUID.randomUUID();

        ApiResponse<Void> resp = controller.rejectDoctor(id);

        verify(adminService).deleteUser(id);
        assertThat(resp.getMessage()).contains("Doctor rejected");
    }

    @Test
    void updateDoctorWorkSchedule_delegates() {
        UUID id = UUID.randomUUID();
        Map<DayOfWeek, List<TimeRange>> schedule = Map.of(
                DayOfWeek.MONDAY, List.of(new TimeRange(LocalTime.of(9, 0), LocalTime.of(17, 0))));
        UpdateWorkScheduleRequest req = new UpdateWorkScheduleRequest();
        req.setWorkSchedule(schedule);
        DoctorProfileResponse data = DoctorProfileResponse.builder().userId(id).build();
        when(adminService.updateDoctorWorkSchedule(id, schedule)).thenReturn(data);

        ApiResponse<DoctorProfileResponse> resp = controller.updateDoctorWorkSchedule(id, req);

        assertThat(resp.getData()).isSameAs(data);
        assertThat(resp.getMessage()).contains("Work schedule updated");
    }
}