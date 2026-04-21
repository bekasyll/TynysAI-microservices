package com.tynysai.userservice.service;

import com.tynysai.userservice.dto.PageResponse;
import com.tynysai.userservice.dto.request.UpdateDoctorProfileRequest;
import com.tynysai.userservice.dto.response.DoctorProfileResponse;
import com.tynysai.userservice.exception.BadRequestException;
import com.tynysai.userservice.exception.ResourceNotFoundException;
import com.tynysai.userservice.mapper.UserMapper;
import com.tynysai.userservice.model.DoctorProfile;
import com.tynysai.userservice.model.User;
import com.tynysai.userservice.repository.DoctorProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DoctorProfileService {
    private final DoctorProfileRepository doctorProfileRepository;
    private final UserService userService;

    @Transactional
    public DoctorProfileResponse getMyProfile(UUID userId) {
        User user = userService.findById(userId);
        DoctorProfile doctorProfile = getOrCreate(user);
        return UserMapper.toDoctorResponse(doctorProfile, user);
    }

    public DoctorProfileResponse getByUserId(UUID doctorUserId) {
        User user = userService.findById(doctorUserId);
        DoctorProfile doctorProfile = doctorProfileRepository.findByUserId(doctorUserId)
                .orElseThrow(() -> new ResourceNotFoundException("DoctorProfile", "userId", doctorUserId));
        return UserMapper.toDoctorResponse(doctorProfile, user);
    }

    @Transactional
    public DoctorProfileResponse updateMyProfile(UUID userId, UpdateDoctorProfileRequest request) {
        User user = userService.findById(userId);
        DoctorProfile doctorProfile = getOrCreate(user);

        if (request.getDateOfBirth() != null) doctorProfile.setDateOfBirth(request.getDateOfBirth());
        if (request.getGender() != null) doctorProfile.setGender(request.getGender());
        if (request.getSpecialization() != null) doctorProfile.setSpecialization(request.getSpecialization());
        if (request.getLicenseNumber() != null) {
            if (!request.getLicenseNumber().equals(doctorProfile.getLicenseNumber())
                    && doctorProfileRepository.existsByLicenseNumber(request.getLicenseNumber())) {
                throw new BadRequestException("License number already registered");
            }
            doctorProfile.setLicenseNumber(request.getLicenseNumber());
        }
        if (request.getHospitalName() != null) doctorProfile.setHospitalName(request.getHospitalName());
        if (request.getDepartment() != null) doctorProfile.setDepartment(request.getDepartment());
        if (request.getYearsOfExperience() != null) doctorProfile.setYearsOfExperience(request.getYearsOfExperience());
        if (request.getBio() != null) doctorProfile.setBio(request.getBio());
        if (request.getEducation() != null) doctorProfile.setEducation(request.getEducation());
        if (request.getWorkSchedule() != null) doctorProfile.setWorkSchedule(request.getWorkSchedule());

        return UserMapper.toDoctorResponse(doctorProfileRepository.save(doctorProfile), user);
    }

    public PageResponse<DoctorProfileResponse> listApproved(Pageable pageable) {
        Page<DoctorProfile> page = doctorProfileRepository.findByApproved(true, pageable);
        return PageResponse.from(page.map(d -> {
            User user = userService.findById(d.getUserId());
            return UserMapper.toDoctorResponse(d, user);
        }));
    }

    @Transactional
    public DoctorProfileResponse approve(UUID doctorUserId, boolean approved) {
        DoctorProfile doctorProfile = doctorProfileRepository.findByUserId(doctorUserId)
                .orElseThrow(() -> new ResourceNotFoundException("DoctorProfile", "userId", doctorUserId));
        doctorProfile.setApproved(approved);

        User user = userService.findById(doctorUserId);
        return UserMapper.toDoctorResponse(doctorProfileRepository.save(doctorProfile), user);
    }

    private DoctorProfile getOrCreate(User user) {
        return doctorProfileRepository.findByUserId(user.getId())
                .orElseGet(() -> doctorProfileRepository.save(
                        DoctorProfile.builder()
                                .userId(user.getId())
                                .specialization("General")
                                .approved(false)
                                .build()));
    }
}
