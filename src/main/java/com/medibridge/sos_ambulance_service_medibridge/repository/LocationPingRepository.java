package com.medibridge.sos_ambulance_service_medibridge.repository;

import com.medibridge.sos_ambulance_service_medibridge.domain.entity.LocationPing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface LocationPingRepository extends JpaRepository<LocationPing, UUID> {
    @Query("SELECT lp FROM LocationPing lp WHERE lp.ambulanceId = :ambulanceId ORDER BY lp.createdAt DESC LIMIT 1")
    LocationPing findLatestByAmbulance(@Param("ambulanceId") UUID ambulanceId);

    @Query("SELECT lp FROM LocationPing lp WHERE lp.incidentId = :incidentId ORDER BY lp.createdAt ASC")
    List<LocationPing> findByIncident(@Param("incidentId") UUID incidentId);

    @Query("SELECT lp FROM LocationPing lp WHERE lp.driverUserId = :driverUserId AND lp.createdAt >= :since ORDER BY lp.createdAt DESC")
    List<LocationPing> findRecentByDriver(@Param("driverUserId") UUID driverUserId, @Param("since") LocalDateTime since);
}
