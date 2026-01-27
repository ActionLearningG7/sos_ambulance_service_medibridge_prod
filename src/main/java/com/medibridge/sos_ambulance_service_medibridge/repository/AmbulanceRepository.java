package com.medibridge.sos_ambulance_service_medibridge.repository;

import com.medibridge.sos_ambulance_service_medibridge.domain.entity.Ambulance;
import com.medibridge.sos_ambulance_service_medibridge.domain.enums.AmbulanceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AmbulanceRepository extends JpaRepository<Ambulance, UUID> {

    Optional<Ambulance> findByDriverUserId(String driverUserId);

    boolean existsByRegistrationNumber(String registrationNumber);

    @Query(value = "SELECT * FROM ambulances a " +
            "WHERE a.status = :#{#status.name()} " +
            "AND ST_Distance_Sphere(point(a.last_lng, a.last_lat), point(:lng, :lat)) <= :radiusInMeters " +
            "ORDER BY ST_Distance_Sphere(point(a.last_lng, a.last_lat), point(:lng, :lat)) ASC " +
            "LIMIT :limit", nativeQuery = true)
    List<Ambulance> findNearbyAmbulances(
            @Param("lat") double lat,
            @Param("lng") double lng,
            @Param("radiusInMeters") double radiusInMeters,
            @Param("status") AmbulanceStatus status,
            @Param("limit") int limit);
}
