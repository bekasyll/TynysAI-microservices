package com.tynysai.userservice.service;

import com.tynysai.common.dto.PageResponse;
import com.tynysai.userservice.dto.request.UpdateDoctorProfileRequest;
import com.tynysai.userservice.dto.response.DoctorProfileResponse;
import com.tynysai.userservice.exception.BadRequestException;
import com.tynysai.userservice.exception.ResourceNotFoundException;
import com.tynysai.userservice.model.DoctorProfile;
import com.tynysai.userservice.model.User;
import com.tynysai.userservice.model.enums.Gender;
import com.tynysai.userservice.model.enums.Role;
import com.tynysai.userservice.repository.DoctorProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DoctorProfileServiceTest {
    @Mock
    private DoctorProfileRepository doctorProfileRepository;
    @Mock
    private UserService userService;

    @InjectMocks
    private DoctorProfileService service;

    private UUID userId;
    private User user;
    private DoctorProfile profile;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        user = User.builder()
                .id(userId)
                .email("doc@x.kz")
                .firstName("Doc")
                .lastName("Smith")
                .phoneNumber("+77011112233")
                .role(Role.DOCTOR)
                .build();
        profile = DoctorProfile.builder()
                .id(7L)
                .userId(userId)
                .specialization("Cardiology")
                .licenseNumber("LIC-1")
                .approved(false)
                .build();
    }

    @Test
    void getMyProfile_returnsExistingProfile() {
        when(userService.findById(userId)).thenReturn(user);
        when(doctorProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));

        DoctorProfileResponse resp = service.getMyProfile(userId);

        assertThat(resp.getUserId()).isEqualTo(userId);
        assertThat(resp.getSpecialization()).isEqualTo("Cardiology");
        verify(doctorProfileRepository, never()).save(any());
    }

    @Test
    void getMyProfile_createsDefaultProfile_whenMissing() {
        when(userService.findById(userId)).thenReturn(user);
        when(doctorProfileRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(doctorProfileRepository.save(any(DoctorProfile.class))).thenAnswer(inv -> inv.getArgument(0));

        DoctorProfileResponse resp = service.getMyProfile(userId);

        ArgumentCaptor<DoctorProfile> captor = ArgumentCaptor.forClass(DoctorProfile.class);
        verify(doctorProfileRepository).save(captor.capture());
        DoctorProfile created = captor.getValue();
        assertThat(created.getUserId()).isEqualTo(userId);
        assertThat(created.getSpecialization()).isEqualTo("General");
        assertThat(created.isApproved()).isFalse();
        assertThat(created.getWorkSchedule()).isNotEmpty();
        assertThat(resp.getUserId()).isEqualTo(userId);
    }

    @Test
    void getByUserId_returnsResponse_whenProfilePresent() {
        when(userService.findById(userId)).thenReturn(user);
        when(doctorProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));

        DoctorProfileResponse resp = service.getByUserId(userId);

        assertThat(resp.getId()).isEqualTo(7L);
    }

    @Test
    void getByUserId_throwsNotFound_whenProfileMissing() {
        when(userService.findById(userId)).thenReturn(user);
        when(doctorProfileRepository.findByUserId(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getByUserId(userId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateMyProfile_appliesAllProvidedFields() {
        UpdateDoctorProfileRequest req = new UpdateDoctorProfileRequest();
        req.setDateOfBirth(LocalDate.of(1985, 3, 15));
        req.setGender(Gender.FEMALE);
        req.setSpecialization("Neurology");
        req.setLicenseNumber("LIC-2");
        req.setHospitalName("Central");
        req.setDepartment("Neuro");
        req.setYearsOfExperience(10);
        req.setBio("Bio");
        req.setEducation("Edu");
        when(userService.findById(userId)).thenReturn(user);
        when(doctorProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));
        when(doctorProfileRepository.existsByLicenseNumber("LIC-2")).thenReturn(false);
        when(doctorProfileRepository.save(any(DoctorProfile.class))).thenAnswer(inv -> inv.getArgument(0));

        DoctorProfileResponse resp = service.updateMyProfile(userId, req);

        assertThat(profile.getDateOfBirth()).isEqualTo(LocalDate.of(1985, 3, 15));
        assertThat(profile.getGender()).isEqualTo(Gender.FEMALE);
        assertThat(profile.getSpecialization()).isEqualTo("Neurology");
        assertThat(profile.getLicenseNumber()).isEqualTo("LIC-2");
        assertThat(profile.getHospitalName()).isEqualTo("Central");
        assertThat(profile.getDepartment()).isEqualTo("Neuro");
        assertThat(profile.getYearsOfExperience()).isEqualTo(10);
        assertThat(profile.getBio()).isEqualTo("Bio");
        assertThat(profile.getEducation()).isEqualTo("Edu");
        assertThat(resp.getSpecialization()).isEqualTo("Neurology");
    }

    @Test
    void updateMyProfile_skipsLicenseLookup_whenLicenseUnchanged() {
        UpdateDoctorProfileRequest req = new UpdateDoctorProfileRequest();
        req.setLicenseNumber("LIC-1"); // same as profile
        when(userService.findById(userId)).thenReturn(user);
        when(doctorProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));
        when(doctorProfileRepository.save(any(DoctorProfile.class))).thenAnswer(inv -> inv.getArgument(0));

        service.updateMyProfile(userId, req);

        verify(doctorProfileRepository, never()).existsByLicenseNumber(any());
        assertThat(profile.getLicenseNumber()).isEqualTo("LIC-1");
    }

    @Test
    void updateMyProfile_throwsBadRequest_whenLicenseAlreadyTaken() {
        UpdateDoctorProfileRequest req = new UpdateDoctorProfileRequest();
        req.setLicenseNumber("LIC-NEW");
        when(userService.findById(userId)).thenReturn(user);
        when(doctorProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));
        when(doctorProfileRepository.existsByLicenseNumber("LIC-NEW")).thenReturn(true);

        assertThatThrownBy(() -> service.updateMyProfile(userId, req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("License number already registered");
        verify(doctorProfileRepository, never()).save(any());
    }

    @Test
    void updateMyProfile_keepsExisting_whenAllRequestFieldsNull() {
        profile.setHospitalName("Old");
        UpdateDoctorProfileRequest req = new UpdateDoctorProfileRequest();
        when(userService.findById(userId)).thenReturn(user);
        when(doctorProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));
        when(doctorProfileRepository.save(any(DoctorProfile.class))).thenAnswer(inv -> inv.getArgument(0));

        service.updateMyProfile(userId, req);

        assertThat(profile.getHospitalName()).isEqualTo("Old");
        assertThat(profile.getSpecialization()).isEqualTo("Cardiology");
    }

    @Test
    void updateMyProfile_createsProfile_whenMissing() {
        UpdateDoctorProfileRequest req = new UpdateDoctorProfileRequest();
        req.setBio("New bio");
        when(userService.findById(userId)).thenReturn(user);
        when(doctorProfileRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(doctorProfileRepository.save(any(DoctorProfile.class))).thenAnswer(inv -> inv.getArgument(0));

        service.updateMyProfile(userId, req);

        verify(doctorProfileRepository, times(2)).save(any(DoctorProfile.class));
    }

    @Test
    void listApproved_mapsApprovedDoctors() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<DoctorProfile> page = new PageImpl<>(List.of(profile), pageable, 1);
        when(doctorProfileRepository.findByApproved(true, pageable)).thenReturn(page);
        when(userService.findById(userId)).thenReturn(user);

        PageResponse<DoctorProfileResponse> resp = service.listApproved(pageable);

        assertThat(resp.getContent()).hasSize(1);
        assertThat(resp.getContent().get(0).getUserId()).isEqualTo(userId);
        assertThat(resp.getTotalElements()).isEqualTo(1);
    }

    @Test
    void approve_setsApprovedFlagAndPersists() {
        when(doctorProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));
        when(userService.findById(userId)).thenReturn(user);
        when(doctorProfileRepository.save(any(DoctorProfile.class))).thenAnswer(inv -> inv.getArgument(0));

        DoctorProfileResponse resp = service.approve(userId, true);

        assertThat(profile.isApproved()).isTrue();
        assertThat(resp.isApproved()).isTrue();
    }

    @Test
    void approve_throwsNotFound_whenProfileMissing() {
        when(doctorProfileRepository.findByUserId(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.approve(userId, true))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void approve_canRevokeApproval() {
        profile.setApproved(true);
        when(doctorProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));
        when(userService.findById(userId)).thenReturn(user);
        when(doctorProfileRepository.save(any(DoctorProfile.class))).thenAnswer(inv -> inv.getArgument(0));

        DoctorProfileResponse resp = service.approve(userId, false);

        assertThat(profile.isApproved()).isFalse();
        assertThat(resp.isApproved()).isFalse();
    }
}
