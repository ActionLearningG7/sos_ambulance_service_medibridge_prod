package com.medibridge.sos_ambulance_service_medibridge.repository;

import com.medibridge.sos_ambulance_service_medibridge.domain.entity.EmergencyRequest;
import com.medibridge.sos_ambulance_service_medibridge.domain.enums.EmergencyStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmergencyRequestRepository extends JpaRepository<EmergencyRequest, UUID> {
    List<EmergencyRequest> findByPatientId(String patientId); // Changed to String as per entity

    Optional<EmergencyRequest> findByPatientIdAndStatusIn(String patientId, List<EmergencyStatus> statuses);

    @Query("SELECT r FROM EmergencyRequest r WHERE " +
            "(:status IS NULL OR r.status = :status) AND " +
            "(:search IS NULL OR " +
            "LOWER(r.patientName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "r.contactPhone LIKE CONCAT('%', :search, '%') OR " +
            "r.requestNumber LIKE CONCAT('%', :search, '%')) " +
            "ORDER BY r.createdAt DESC")
    Page<EmergencyRequest> findAllBySearch(
            @Param("search") String search,
            @Param("status") EmergencyStatus status,
            Pageable pageable);
}
