package com.tynysai.userservice.controller;

import com.tynysai.userservice.dto.ApiResponse;
import com.tynysai.userservice.dto.request.RegisterPatientRequest;
import com.tynysai.userservice.dto.response.RegisterResponse;
import com.tynysai.userservice.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/register/patient")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<RegisterResponse> registerPatient(@Valid @RequestBody RegisterPatientRequest request) {
        return ApiResponse.success("Account created. You can now sign in.",
                authService.registerPatient(request, /* adminInitiated */ false));
    }
}