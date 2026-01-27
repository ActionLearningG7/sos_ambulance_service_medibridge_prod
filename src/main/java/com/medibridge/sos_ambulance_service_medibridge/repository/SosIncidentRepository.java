package com.medibridge.sos_ambulance_service_medibridge.repository;

import com.medibridge.sos_ambulance_service_medibridge.domain.entity.SosIncident;
import com.medibridge.sos_ambulance_service_medibridge.domain.enums.IncidentStatus;
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
public interface SosIncidentRepository extends JpaRepository<SosIncident, UUID> {
    List<SosIncident> findByPatientId(UUID patientId);

    @Query("SELECT si FROM SosIncident si WHERE si.organizationId = :orgId AND si.status IN :statuses ORDER BY si.createdAt DESC")
    List<SosIncident> findActiveByOrganization(@Param("orgId") UUID orgId,
            @Param("statuses") List<IncidentStatus> statuses);

    @Query("SELECT si FROM SosIncident si WHERE si.ambulanceId = :ambulanceId AND si.status NOT IN ('COMPLETED', 'CANCELLED')")
    Optional<SosIncident> findActiveByAmbulance(@Param("ambulanceId") UUID ambulanceId);

    List<SosIncident> findByAmbulanceId(UUID ambulanceId);

    @Query("SELECT i FROM SosIncident i WHERE " +
            "(:status IS NULL OR i.status = :status) AND " +
            "(:search IS NULL OR " +
            "LOWER(i.patientName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "i.patientPhone LIKE CONCAT('%', :search, '%') OR " +
            "LOWER(i.pickupAddress) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<SosIncident> findAllBySearch(
            @Param("search") String search,
            @Param("status") IncidentStatus status,
            Pageable pageable);
}
