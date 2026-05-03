package com.tynysai.userservice.mapper;

import com.tynysai.userservice.dto.response.DoctorProfileResponse;
import com.tynysai.userservice.dto.response.PatientProfileResponse;
import com.tynysai.userservice.dto.response.UserResponse;
import com.tynysai.userservice.model.DoctorProfile;
import com.tynysai.userservice.model.PatientProfile;
import com.tynysai.userservice.model.User;
import com.tynysai.userservice.model.enums.BloodType;
import com.tynysai.userservice.model.enums.Gender;
import com.tynysai.userservice.model.enums.Role;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UserMapperTest {

    @Test
    void toUserResponse_copiesAllFields() {
        UUID id = UUID.randomUUID();
        User user = User.builder()
                .id(id)
                .email("a@b.kz")
                .firstName("First")
                .lastName("Last")
                .middleName("Mid")
                .phoneNumber("+77011112222")
                .role(Role.PATIENT)
                .enabled(true)
                .emailVerified(false)
                .avatarPath("av/path.png")
                .build();

        UserResponse response = UserMapper.toUserResponse(user);

        assertThat(response.getId()).isEqualTo(id);
        assertThat(response.getEmail()).isEqualTo("a@b.kz");
        assertThat(response.getFirstName()).isEqualTo("First");
        assertThat(response.getLastName()).isEqualTo("Last");
        assertThat(response.getMiddleName()).isEqualTo("Mid");
        assertThat(response.getFullName()).isEqualTo("Last First Mid");
        assertThat(response.getPhoneNumber()).isEqualTo("+77011112222");
        assertThat(response.getRole()).isEqualTo(Role.PATIENT);
        assertThat(response.isEnabled()).isTrue();
        assertThat(response.isEmailVerified()).isFalse();
        assertThat(response.getAvatarPath()).isEqualTo("av/path.png");
    }

    @Test
    void toUserResponse_buildsFullNameWithoutMiddleName() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .email("x@y.kz")
                .firstName("First")
                .lastName("Last")
                .phoneNumber("+77011112222")
                .role(Role.DOCTOR)
                .build();

        UserResponse response = UserMapper.toUserResponse(user);

        assertThat(response.getFullName()).isEqualTo("Last First");
        assertThat(response.getMiddleName()).isNull();
    }

    @Test
    void toPatientResponse_copiesUserAndProfileFields() {
        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .id(userId)
                .email("p@x.kz")
                .firstName("P")
                .lastName("D")
                .middleName("Mid")
                .phoneNumber("+77011112222")
                .role(Role.PATIENT)
                .build();
        PatientProfile profile = PatientProfile.builder()
                .id(42L)
                .userId(userId)
                .dateOfBirth(LocalDate.of(2000, 1, 1))
                .gender(Gender.MALE)
                .bloodType(BloodType.O_POSITIVE)
                .heightCm(180.0)
                .weightKg(75.0)
                .allergies("none")
                .chronicDiseases("none")
                .emergencyContactName("Mom")
                .emergencyContactPhone("+77017778899")
                .address("Astana")
                .insuranceNumber("INS-1")
                .occupation("Eng")
                .smoker(false)
                .alcoholUser(true)
                .medicalHistory("clean")
                .build();

        PatientProfileResponse response = UserMapper.toPatientResponse(profile, user);

        assertThat(response.getId()).isEqualTo(42L);
        assertThat(response.getUserId()).isEqualTo(userId);
        assertThat(response.getEmail()).isEqualTo("p@x.kz");
        assertThat(response.getFirstName()).isEqualTo("P");
        assertThat(response.getLastName()).isEqualTo("D");
        assertThat(response.getMiddleName()).isEqualTo("Mid");
        assertThat(response.getFullName()).isEqualTo("D P Mid");
        assertThat(response.getPhoneNumber()).isEqualTo("+77011112222");
        assertThat(response.getDateOfBirth()).isEqualTo(LocalDate.of(2000, 1, 1));
        assertThat(response.getAge()).isNotNull();
        assertThat(response.getGender()).isEqualTo(Gender.MALE);
        assertThat(response.getBloodType()).isEqualTo(BloodType.O_POSITIVE);
        assertThat(response.getHeightCm()).isEqualTo(180.0);
        assertThat(response.getWeightKg()).isEqualTo(75.0);
        assertThat(response.getAllergies()).isEqualTo("none");
        assertThat(response.getChronicDiseases()).isEqualTo("none");
        assertThat(response.getEmergencyContactName()).isEqualTo("Mom");
        assertThat(response.getEmergencyContactPhone()).isEqualTo("+77017778899");
        assertThat(response.getAddress()).isEqualTo("Astana");
        assertThat(response.getInsuranceNumber()).isEqualTo("INS-1");
        assertThat(response.getOccupation()).isEqualTo("Eng");
        assertThat(response.getSmoker()).isFalse();
        assertThat(response.getAlcoholUser()).isTrue();
        assertThat(response.getMedicalHistory()).isEqualTo("clean");
    }

    @Test
    void toPatientResponse_handlesNullDateOfBirth() {
        User user = User.builder().id(UUID.randomUUID()).email("e").firstName("F").lastName("L")
                .phoneNumber("+77011112222").role(Role.PATIENT).build();
        PatientProfile profile = PatientProfile.builder().userId(user.getId()).build();

        PatientProfileResponse response = UserMapper.toPatientResponse(profile, user);

        assertThat(response.getAge()).isNull();
    }

    @Test
    void toDoctorResponse_copiesUserAndProfileFields() {
        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .id(userId)
                .email("doc@x.kz")
                .firstName("Doc")
                .lastName("Smith")
                .phoneNumber("+77011112222")
                .role(Role.DOCTOR)
                .build();
        DoctorProfile profile = DoctorProfile.builder()
                .id(8L)
                .userId(userId)
                .dateOfBirth(LocalDate.of(1980, 6, 1))
                .gender(Gender.FEMALE)
                .specialization("Cardio")
                .licenseNumber("LIC-1")
                .hospitalName("Central")
                .department("Card")
                .yearsOfExperience(15)
                .bio("Bio")
                .education("Edu")
                .approved(true)
                .workSchedule(DoctorProfile.defaultWorkSchedule())
                .build();

        DoctorProfileResponse response = UserMapper.toDoctorResponse(profile, user);

        assertThat(response.getId()).isEqualTo(8L);
        assertThat(response.getUserId()).isEqualTo(userId);
        assertThat(response.getEmail()).isEqualTo("doc@x.kz");
        assertThat(response.getFirstName()).isEqualTo("Doc");
        assertThat(response.getLastName()).isEqualTo("Smith");
        assertThat(response.getFullName()).isEqualTo("Smith Doc");
        assertThat(response.getDateOfBirth()).isEqualTo(LocalDate.of(1980, 6, 1));
        assertThat(response.getAge()).isNotNull();
        assertThat(response.getGender()).isEqualTo(Gender.FEMALE);
        assertThat(response.getSpecialization()).isEqualTo("Cardio");
        assertThat(response.getLicenseNumber()).isEqualTo("LIC-1");
        assertThat(response.getHospitalName()).isEqualTo("Central");
        assertThat(response.getDepartment()).isEqualTo("Card");
        assertThat(response.getYearsOfExperience()).isEqualTo(15);
        assertThat(response.getBio()).isEqualTo("Bio");
        assertThat(response.getEducation()).isEqualTo("Edu");
        assertThat(response.isApproved()).isTrue();
        assertThat(response.getWorkSchedule()).isNotEmpty();
    }

    @Test
    void toDoctorResponse_handlesNullDateOfBirth() {
        User user = User.builder().id(UUID.randomUUID()).email("e").firstName("F").lastName("L")
                .phoneNumber("+77011112222").role(Role.DOCTOR).build();
        DoctorProfile profile = DoctorProfile.builder().userId(user.getId()).specialization("Gen").build();

        DoctorProfileResponse response = UserMapper.toDoctorResponse(profile, user);

        assertThat(response.getAge()).isNull();
        assertThat(response.isApproved()).isFalse();
    }
}
