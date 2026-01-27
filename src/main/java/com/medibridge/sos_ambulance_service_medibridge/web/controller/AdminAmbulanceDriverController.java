package com.medibridge.sos_ambulance_service_medibridge.web.controller;

import com.medibridge.sos_ambulance_service_medibridge.domain.entity.AmbulanceDriver;
import com.medibridge.sos_ambulance_service_medibridge.repository.AmbulanceDriverRepository;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/ambulance-drivers")
@RequiredArgsConstructor
public class AdminAmbulanceDriverController {

    private final AmbulanceDriverRepository driverRepository;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<AmbulanceDriver>> getDrivers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            Pageable pageable) {
        return ResponseEntity.ok(driverRepository.findAllBySearch(search, status, pageable));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AmbulanceDriver> createDriver(@RequestBody CreateDriverRequest request) {
        AmbulanceDriver driver = AmbulanceDriver.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .phoneNumber(request.getPhoneNumber())
                .licenseNumber(request.getLicenseNumber())
                .licenseExpiryDate(request.getLicenseExpiryDate())
                .certificationStatus(request.getCertificationStatus())
                .userId(UUID.randomUUID().toString()) // Placeholder: In real app, create User in User Service first
                .isActive(true)
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(driverRepository.save(driver));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AmbulanceDriver> updateDriver(
            @PathVariable UUID id,
            @RequestBody UpdateDriverRequest request) {

        AmbulanceDriver driver = driverRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Driver not found"));

        if (request.getFirstName() != null)
            driver.setFirstName(request.getFirstName());
        if (request.getLastName() != null)
            driver.setLastName(request.getLastName());
        if (request.getEmail() != null)
            driver.setEmail(request.getEmail());
        if (request.getPhoneNumber() != null)
            driver.setPhoneNumber(request.getPhoneNumber());
        if (request.getLicenseNumber() != null)
            driver.setLicenseNumber(request.getLicenseNumber());
        if (request.getLicenseExpiryDate() != null)
            driver.setLicenseExpiryDate(request.getLicenseExpiryDate());
        if (request.getCertificationStatus() != null)
            driver.setCertificationStatus(request.getCertificationStatus());

        return ResponseEntity.ok(driverRepository.save(driver));
    }

    @PatchMapping("/{id}/reset-credentials")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ResetCredentialsResponse> resetCredentials(@PathVariable UUID id) {
        AmbulanceDriver driver = driverRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Driver not found"));

        // Mock logic: In real system, call User Service to reset password
        String tempPassword = "Temp@" + UUID.randomUUID().toString().substring(0, 8);

        return ResponseEntity.ok(new ResetCredentialsResponse(tempPassword, driver.getEmail()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteDriver(@PathVariable UUID id) {
        driverRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // ===== DTOs =====

    @Data
    public static class CreateDriverRequest {
        private String firstName;
        private String lastName;
        private String email;
        private String phoneNumber;
        private String licenseNumber;
        private LocalDate licenseExpiryDate;
        private String certificationStatus;
    }

    @Data
    public static class UpdateDriverRequest {
        private String firstName;
        private String lastName;
        private String email;
        private String phoneNumber;
        private String licenseNumber;
        private LocalDate licenseExpiryDate;
        private String certificationStatus;
    }

    @Data
    @AllArgsConstructor
    public static class ResetCredentialsResponse {
        private String temporaryPassword;
        private String tempEmail;
    }
}
