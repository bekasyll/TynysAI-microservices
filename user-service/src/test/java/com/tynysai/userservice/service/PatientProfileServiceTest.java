package com.tynysai.userservice.service;

import com.tynysai.common.dto.PageResponse;
import com.tynysai.userservice.dto.request.UpdatePatientProfileRequest;
import com.tynysai.userservice.dto.response.PatientProfileResponse;
import com.tynysai.userservice.exception.ResourceNotFoundException;
import com.tynysai.userservice.model.PatientProfile;
import com.tynysai.userservice.model.User;
import com.tynysai.userservice.model.enums.BloodType;
import com.tynysai.userservice.model.enums.Gender;
import com.tynysai.userservice.model.enums.Role;
import com.tynysai.userservice.repository.PatientProfileRepository;
import com.tynysai.userservice.repository.UserRepository;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PatientProfileServiceTest {

    @Mock
    private PatientProfileRepository patientProfileRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private UserService userService;

    @InjectMocks
    private PatientProfileService service;

    private UUID userId;
    private User user;
    private PatientProfile profile;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        user = User.builder()
                .id(userId)
                .email("p@x.kz")
                .firstName("Pat")
                .lastName("Doe")
                .phoneNumber("+77011112233")
                .role(Role.PATIENT)
                .build();
        profile = PatientProfile.builder()
                .id(11L)
                .userId(userId)
                .build();
    }

    @Test
    void getMyProfile_returnsExistingProfile() {
        when(userService.findById(userId)).thenReturn(user);
        when(patientProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));

        PatientProfileResponse resp = service.getMyProfile(userId);

        assertThat(resp.getUserId()).isEqualTo(userId);
        assertThat(resp.getEmail()).isEqualTo("p@x.kz");
        verify(patientProfileRepository, never()).save(any());
    }

    @Test
    void getMyProfile_createsAndPersistsProfile_whenMissing() {
        when(userService.findById(userId)).thenReturn(user);
        when(patientProfileRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(patientProfileRepository.save(any(PatientProfile.class))).thenAnswer(inv -> inv.getArgument(0));

        PatientProfileResponse resp = service.getMyProfile(userId);

        ArgumentCaptor<PatientProfile> captor = ArgumentCaptor.forClass(PatientProfile.class);
        verify(patientProfileRepository).save(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(userId);
        assertThat(resp.getUserId()).isEqualTo(userId);
    }

    @Test
    void getByUserId_returnsResponse_whenProfilePresent() {
        when(userService.findById(userId)).thenReturn(user);
        when(patientProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));

        PatientProfileResponse resp = service.getByUserId(userId);

        assertThat(resp.getId()).isEqualTo(11L);
        assertThat(resp.getEmail()).isEqualTo("p@x.kz");
    }

    @Test
    void getByUserId_throwsNotFound_whenProfileMissing() {
        when(userService.findById(userId)).thenReturn(user);
        when(patientProfileRepository.findByUserId(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getByUserId(userId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateMyProfile_appliesAllProvidedFields() {
        UpdatePatientProfileRequest req = new UpdatePatientProfileRequest();
        req.setDateOfBirth(LocalDate.of(2000, 5, 1));
        req.setGender(Gender.MALE);
        req.setBloodType(BloodType.O_POSITIVE);
        req.setHeightCm(180.0);
        req.setWeightKg(75.0);
        req.setAllergies("none");
        req.setChronicDiseases("none");
        req.setEmergencyContactName("Mom");
        req.setEmergencyContactPhone("+77017778899");
        req.setAddress("Astana");
        req.setInsuranceNumber("INS-1");
        req.setOccupation("Eng");
        req.setSmoker(false);
        req.setAlcoholUser(false);
        req.setMedicalHistory("clean");
        when(userService.findById(userId)).thenReturn(user);
        when(patientProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));
        when(patientProfileRepository.save(any(PatientProfile.class))).thenAnswer(inv -> inv.getArgument(0));

        PatientProfileResponse resp = service.updateMyProfile(userId, req);

        assertThat(profile.getDateOfBirth()).isEqualTo(LocalDate.of(2000, 5, 1));
        assertThat(profile.getGender()).isEqualTo(Gender.MALE);
        assertThat(profile.getBloodType()).isEqualTo(BloodType.O_POSITIVE);
        assertThat(profile.getHeightCm()).isEqualTo(180.0);
        assertThat(profile.getWeightKg()).isEqualTo(75.0);
        assertThat(profile.getAllergies()).isEqualTo("none");
        assertThat(profile.getChronicDiseases()).isEqualTo("none");
        assertThat(profile.getEmergencyContactName()).isEqualTo("Mom");
        assertThat(profile.getEmergencyContactPhone()).isEqualTo("+77017778899");
        assertThat(profile.getAddress()).isEqualTo("Astana");
        assertThat(profile.getInsuranceNumber()).isEqualTo("INS-1");
        assertThat(profile.getOccupation()).isEqualTo("Eng");
        assertThat(profile.getSmoker()).isFalse();
        assertThat(profile.getAlcoholUser()).isFalse();
        assertThat(profile.getMedicalHistory()).isEqualTo("clean");
        assertThat(resp.getEmail()).isEqualTo("p@x.kz");
    }

    @Test
    void updateMyProfile_keepsExistingValues_whenRequestFieldsNull() {
        profile.setOccupation("Doctor");
        profile.setHeightCm(170.0);
        UpdatePatientProfileRequest req = new UpdatePatientProfileRequest();
        when(userService.findById(userId)).thenReturn(user);
        when(patientProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));
        when(patientProfileRepository.save(any(PatientProfile.class))).thenAnswer(inv -> inv.getArgument(0));

        service.updateMyProfile(userId, req);

        assertThat(profile.getOccupation()).isEqualTo("Doctor");
        assertThat(profile.getHeightCm()).isEqualTo(170.0);
    }

    @Test
    void updateMyProfile_createsProfile_whenMissing() {
        UpdatePatientProfileRequest req = new UpdatePatientProfileRequest();
        req.setOccupation("Pilot");
        when(userService.findById(userId)).thenReturn(user);
        when(patientProfileRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(patientProfileRepository.save(any(PatientProfile.class))).thenAnswer(inv -> inv.getArgument(0));

        service.updateMyProfile(userId, req);

        // save invoked twice: once to create, once to update
        verify(patientProfileRepository, org.mockito.Mockito.times(2)).save(any(PatientProfile.class));
    }

    @Test
    void listPatients_returnsProfileResponses_forPatientsWithProfile() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> usersPage = new PageImpl<>(List.of(user), pageable, 1);
        when(userRepository.findByRole(Role.PATIENT, pageable)).thenReturn(usersPage);
        when(patientProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));

        PageResponse<PatientProfileResponse> resp = service.listPatients(pageable);

        assertThat(resp.getContent()).hasSize(1);
        assertThat(resp.getContent().get(0).getEmail()).isEqualTo("p@x.kz");
        assertThat(resp.getTotalElements()).isEqualTo(1);
    }

    @Test
    void listPatients_buildsFallbackResponse_whenProfileMissing() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> usersPage = new PageImpl<>(List.of(user), pageable, 1);
        when(userRepository.findByRole(Role.PATIENT, pageable)).thenReturn(usersPage);
        when(patientProfileRepository.findByUserId(userId)).thenReturn(Optional.empty());

        PageResponse<PatientProfileResponse> resp = service.listPatients(pageable);

        assertThat(resp.getContent()).hasSize(1);
        PatientProfileResponse fallback = resp.getContent().get(0);
        assertThat(fallback.getId()).isNull();
        assertThat(fallback.getUserId()).isEqualTo(userId);
        assertThat(fallback.getEmail()).isEqualTo("p@x.kz");
        assertThat(fallback.getFirstName()).isEqualTo("Pat");
        assertThat(fallback.getLastName()).isEqualTo("Doe");
        assertThat(fallback.getFullName()).isEqualTo("Doe Pat");
    }
}
