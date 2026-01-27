package com.medibridge.sos_ambulance_service_medibridge.service;

import com.medibridge.sos_ambulance_service_medibridge.config.RedisGeoConfig;
import com.medibridge.sos_ambulance_service_medibridge.domain.entity.Ambulance;
import com.medibridge.sos_ambulance_service_medibridge.domain.entity.DriverAssignment;
import com.medibridge.sos_ambulance_service_medibridge.domain.entity.SosIncident;
import com.medibridge.sos_ambulance_service_medibridge.domain.enums.AmbulanceStatus;
import com.medibridge.sos_ambulance_service_medibridge.domain.enums.AssignmentStatus;
import com.medibridge.sos_ambulance_service_medibridge.domain.enums.IncidentStatus;
import com.medibridge.sos_ambulance_service_medibridge.repository.AmbulanceRepository;
import com.medibridge.sos_ambulance_service_medibridge.repository.DriverAssignmentRepository;
import com.medibridge.sos_ambulance_service_medibridge.repository.SosIncidentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Nearest Ambulance Dispatch Service
 * Handles intelligent ambulance assignment using Redis GEOSEARCH
 * Implements distributed locking for consistency
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NearestDispatchService {

    private final org.springframework.data.redis.core.StringRedisTemplate redisTemplate;
    private final AmbulanceRepository ambulanceRepository;
    private final SosIncidentRepository sosIncidentRepository;
    private final DriverAssignmentRepository driverAssignmentRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final NotificationService notificationService;

    @jakarta.annotation.PostConstruct
    public void syncRedisOnStartup() {
        log.info("Syncing all ON_DUTY ambulances to Redis Geo Index...");
        try {
            var ambulances = ambulanceRepository.findAll();
            log.info("Found {} total ambulances in DB", ambulances.size());
            int count = 0;
            java.util.Set<String> clearedKeys = new java.util.HashSet<>();

            for (Ambulance amb : ambulances) {
                log.debug("Checking ambulance: id={} status={} lat={} lng={} orgId={}",
                        amb.getId(), amb.getStatus(), amb.getLastLat(), amb.getLastLng(), amb.getOrganizationId());

                if (amb.getStatus() == AmbulanceStatus.ON_DUTY && amb.getLastLat() != null
                        && amb.getLastLng() != null) {
                    try {
                        String geoKey = RedisGeoConfig.getAvailableAmbulancesGeoKey(amb.getOrganizationId());

                        // Clear key once to remove potentially bad serialized data
                        if (!clearedKeys.contains(geoKey)) {
                            redisTemplate.delete(geoKey);
                            clearedKeys.add(geoKey);
                            log.info("Cleared Redis key before sync: {}", geoKey);
                        }

                        redisTemplate.opsForGeo().add(geoKey,
                                new Point(amb.getLastLng(), amb.getLastLat()),
                                amb.getId().toString());
                        count++;
                        log.info("Added to Redis: {} at key {}", amb.getId(), geoKey);
                    } catch (Exception e) {
                        log.error("Failed to sync ambulance {}", amb.getId(), e);
                    }
                } else {
                    log.warn("Skipping sync for ambulance {}: Status or Location invalid", amb.getId());
                }
            }
            log.info("Synced {} ambulances to Redis.", count);
        } catch (Exception e) {
            log.error("Error during Redis sync on startup", e);
        }
    }

    /**
     * Find and assign nearest available ambulance to SOS incident
     */
    @Transactional
    @SuppressWarnings("unused") // Called by SOS controller when creating incidents
    // public void assignNearestAmbulance(SosIncident incident) {
    // try {
    // // Validate location data
    // if (incident.getPickupLatitude() == null || incident.getPickupLongitude() ==
    // null) {
    // log.error("Invalid pickup location for incident {}",
    // incident.getIncidentId());
    // incident.setStatus(IncidentStatus.CANCELLED);
    // incident.setCancellationReason("Invalid pickup location");
    // sosIncidentRepository.save(incident);
    // return;
    // }
    //
    // // Search for nearest available ambulance
    // Point patientLocation = new Point(incident.getPickupLongitude(),
    // incident.getPickupLatitude());
    // Distance searchRadius = new Distance(RedisGeoConfig.GEOSEARCH_RADIUS_KM,
    // Metrics.KILOMETERS);
    //
    // String geoKey =
    // RedisGeoConfig.getAvailableAmbulancesGeoKey(incident.getOrganizationId().toString());
    //
    // // Use Redis GEOSEARCH to find nearby ambulances
    // var results = redisTemplate.opsForGeo().radius(geoKey, patientLocation,
    // searchRadius);
    //
    // if (results == null || results.getContent().isEmpty()) {
    // log.warn("No available ambulances within {} km for incident {}",
    // RedisGeoConfig.GEOSEARCH_RADIUS_KM, incident.getIncidentId());
    // incident.setStatus(IncidentStatus.CANCELLED);
    // incident.setCancellationReason("No available ambulances in service area");
    // sosIncidentRepository.save(incident);
    // return;
    // }
    //
    // // Try to lock and assign the first available ambulance
    // for (var geoResult : results.getContent()) {
    // String ambulanceId = (String) geoResult.getContent().getName();
    //
    // if (tryLockAndAssign(incident, UUID.fromString(ambulanceId))) {
    // return;
    // }
    // }
    //
    // // If no ambulance could be locked
    // log.warn("Could not lock any ambulance for incident {}",
    // incident.getIncidentId());
    // incident.setStatus(IncidentStatus.CANCELLED);
    // incident.setCancellationReason("Could not secure ambulance");
    // sosIncidentRepository.save(incident);
    //
    // } catch (Exception e) {
    // log.error("Error in nearest ambulance dispatch", e);
    // incident.setStatus(IncidentStatus.CANCELLED);
    // incident.setCancellationReason("System error: " + e.getMessage());
    // sosIncidentRepository.save(incident);
    // }
    // }
    public void assignNearestAmbulance(SosIncident incident) {
        log.debug("[DISPATCH] assignNearestAmbulance() START | incidentId={} orgId={} status={}",
                incident.getIncidentId(),
                incident.getOrganizationId(),
                incident.getStatus());

        try {
            // ✅ Validate location data
            log.debug("[DISPATCH] Validating pickup location | incidentId={} lat={} lng={}",
                    incident.getIncidentId(),
                    incident.getPickupLatitude(),
                    incident.getPickupLongitude());

            if (incident.getPickupLatitude() == null || incident.getPickupLongitude() == null) {
                log.error("[DISPATCH] Invalid pickup location -> CANCEL incident | incidentId={}",
                        incident.getIncidentId());

                incident.setStatus(IncidentStatus.CANCELLED);
                incident.setCancellationReason("Invalid pickup location");
                sosIncidentRepository.save(incident);

                log.debug("[DISPATCH] Incident saved as CANCELLED | incidentId={} reason={}",
                        incident.getIncidentId(),
                        incident.getCancellationReason());
                return;
            }

            // ✅ Prepare location and search radius
            Point patientLocation = new Point(incident.getPickupLongitude(), incident.getPickupLatitude());
            Distance searchRadius = new Distance(RedisGeoConfig.GEOSEARCH_RADIUS_KM, Metrics.KILOMETERS);
            org.springframework.data.geo.Circle circle = new org.springframework.data.geo.Circle(patientLocation,
                    searchRadius);

            String geoKey = RedisGeoConfig.getAvailableAmbulancesGeoKey(incident.getOrganizationId().toString());

            log.debug("[DISPATCH] Searching Redis GEO | incidentId={} geoKey={} radiusKm={} patientPoint=[{}, {}]",
                    incident.getIncidentId(),
                    geoKey,
                    RedisGeoConfig.GEOSEARCH_RADIUS_KM,
                    incident.getPickupLatitude(),
                    incident.getPickupLongitude());

            // ✅ Redis GEOSEARCH
            // Using Circle to avoid ambiguity
            // Explicit type
            org.springframework.data.geo.GeoResults<org.springframework.data.redis.connection.RedisGeoCommands.GeoLocation<String>> results = redisTemplate
                    .opsForGeo().radius(geoKey, circle);

            if (results == null) {
                log.warn("[DISPATCH] Redis GEO result is NULL | incidentId={} geoKey={}",
                        incident.getIncidentId(), geoKey);
            } else {
                log.debug("[DISPATCH] Redis GEO result count={} | incidentId={}",
                        results.getContent().size(),
                        incident.getIncidentId());
            }

            // ✅ No available ambulances
            if (results == null || results.getContent().isEmpty()) {
                log.warn("[DISPATCH] No available ambulances within {} km | incidentId={} orgId={} geoKey={}",
                        RedisGeoConfig.GEOSEARCH_RADIUS_KM,
                        incident.getIncidentId(),
                        incident.getOrganizationId(),
                        geoKey);

                incident.setStatus(IncidentStatus.CANCELLED);
                incident.setCancellationReason("No available ambulances in service area");
                sosIncidentRepository.save(incident);

                log.debug("[DISPATCH] Incident saved as CANCELLED | incidentId={} reason={}",
                        incident.getIncidentId(),
                        incident.getCancellationReason());
                return;
            }

            // ✅ Try locking ambulances one by one
            log.debug("[DISPATCH] Attempting lock & assign | incidentId={} candidates={}",
                    incident.getIncidentId(),
                    results.getContent().size());

            for (org.springframework.data.geo.GeoResult<org.springframework.data.redis.connection.RedisGeoCommands.GeoLocation<String>> geoResult : results
                    .getContent()) {
                String ambulanceId = geoResult.getContent().getName();

                log.debug("[DISPATCH] Candidate ambulance found | incidentId={} ambulanceId={}",
                        incident.getIncidentId(),
                        ambulanceId);

                boolean assigned = false;

                try {
                    assigned = tryLockAndAssign(incident, UUID.fromString(ambulanceId));
                } catch (IllegalArgumentException ex) {
                    log.warn("[DISPATCH] Invalid ambulance UUID format -> skipping | incidentId={} ambulanceId={}",
                            incident.getIncidentId(), ambulanceId);
                    continue;
                }

                log.debug("[DISPATCH] tryLockAndAssign result | incidentId={} ambulanceId={} assigned={}",
                        incident.getIncidentId(),
                        ambulanceId,
                        assigned);

                if (assigned) {
                    log.info("[DISPATCH] Ambulance assigned successfully ✅ | incidentId={} ambulanceId={}",
                            incident.getIncidentId(),
                            ambulanceId);
                    return;
                }
            }

            // ✅ No ambulance could be locked
            log.warn("[DISPATCH] Could not lock any ambulance -> CANCEL incident | incidentId={}",
                    incident.getIncidentId());

            incident.setStatus(IncidentStatus.CANCELLED);
            incident.setCancellationReason("Could not secure ambulance");
            sosIncidentRepository.save(incident);

            log.debug("[DISPATCH] Incident saved as CANCELLED | incidentId={} reason={}",
                    incident.getIncidentId(),
                    incident.getCancellationReason());

        } catch (Exception e) {
            log.error("[DISPATCH] Error in nearest ambulance dispatch ❌ | incidentId={}",
                    incident.getIncidentId(), e);

            incident.setStatus(IncidentStatus.CANCELLED);
            incident.setCancellationReason("System error: " + e.getMessage());
            sosIncidentRepository.save(incident);

            log.debug("[DISPATCH] Incident saved as CANCELLED after exception | incidentId={} reason={}",
                    incident.getIncidentId(),
                    incident.getCancellationReason());
        } finally {
            log.debug("[DISPATCH] assignNearestAmbulance() END | incidentId={} finalStatus={} cancellationReason={}",
                    incident.getIncidentId(),
                    incident.getStatus(),
                    incident.getCancellationReason());
        }
    }

    /**
     * Try to lock and assign ambulance atomically
     */
    private boolean tryLockAndAssign(SosIncident incident, UUID ambulanceId) {
        String lockKey = RedisGeoConfig.getAmbulanceLockKey(ambulanceId.toString(),
                incident.getIncidentId().toString());

        try {
            // Try to acquire distributed lock
            Boolean lockAcquired = redisTemplate.opsForValue()
                    .setIfAbsent(lockKey, "locked", RedisGeoConfig.LOCK_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            if (lockAcquired == null || !lockAcquired) {
                log.debug("Could not acquire lock for ambulance {}", ambulanceId);
                return false;
            }

            // Lock acquired - proceed with assignment
            Ambulance ambulance = ambulanceRepository.findById(ambulanceId)
                    .orElse(null);

            if (ambulance == null || ambulance.getStatus() != AmbulanceStatus.ON_DUTY) {
                log.warn("Ambulance {} no longer available", ambulanceId);
                redisTemplate.delete(lockKey);
                return false;
            }

            // Update incident status
            incident.setStatus(IncidentStatus.ASSIGNED);
            incident.setAmbulanceId(ambulanceId);
            incident.setDriverUserId(UUID.fromString(ambulance.getDriverUserId()));
            incident.setAssignedAt(LocalDateTime.now());
            sosIncidentRepository.save(incident);

            // Update ambulance status
            ambulance.setStatus(AmbulanceStatus.BUSY);
            ambulanceRepository.save(ambulance);

            // Create driver assignment
            DriverAssignment assignment = DriverAssignment.builder()
                    .driverUserId(UUID.fromString(ambulance.getDriverUserId()))
                    .incidentId(incident.getIncidentId())
                    .ambulanceId(ambulanceId)
                    .status(AssignmentStatus.ASSIGNED)
                    .assignedAt(LocalDateTime.now())
                    .estimatedEtaMinutes(estimateETA(ambulance, incident))
                    .distanceKm(calculateDistance(ambulance, incident))
                    .build();

            driverAssignmentRepository.save(assignment);

            // Publish Kafka event
            publishAmbulanceAssignedEvent(incident, ambulance, assignment);

            // Send WebSocket notification
            notificationService.notifyDriverAssignment(
                    UUID.fromString(ambulance.getDriverUserId()),
                    incident,
                    assignment);

            log.info("Successfully assigned ambulance {} to incident {}", ambulanceId, incident.getIncidentId());
            return true;

        } catch (Exception e) {
            log.error("Error during ambulance assignment", e);
            redisTemplate.delete(lockKey);
            return false;
        }
    }

    /**
     * Update ambulance location in Redis GEO and live state
     */
    @Transactional
    public void updateAmbulanceLocation(UUID ambulanceId, Double latitude, Double longitude, String organizationId) {
        try {
            // Update in database
            Ambulance ambulance = ambulanceRepository.findById(ambulanceId)
                    .orElseThrow(() -> new IllegalArgumentException("Ambulance not found"));

            ambulance.setLastLat(latitude);
            ambulance.setLastLng(longitude);
            ambulance.setLastLocationAt(LocalDateTime.now());
            ambulanceRepository.save(ambulance);

            // Update Redis GEO index (if available)
            if (ambulance.getStatus() == AmbulanceStatus.ON_DUTY) {
                String geoKey = RedisGeoConfig.getAvailableAmbulancesGeoKey(organizationId);
                redisTemplate.opsForGeo().add(geoKey,
                        new Point(longitude, latitude),
                        ambulanceId.toString());
            }

            // Update live state hash
            String liveKey = RedisGeoConfig.getAmbulanceLiveStateKey(ambulanceId.toString());
            redisTemplate.opsForHash().put(liveKey, "latitude", String.valueOf(latitude));
            redisTemplate.opsForHash().put(liveKey, "longitude", String.valueOf(longitude));
            redisTemplate.opsForHash().put(liveKey, "updatedAt", String.valueOf(System.currentTimeMillis()));
            redisTemplate.expire(liveKey, RedisGeoConfig.LOCATION_TTL_SECONDS, TimeUnit.SECONDS);

            log.debug("Updated location for ambulance {}: {}, {}", ambulanceId, latitude, longitude);

        } catch (Exception e) {
            log.error("Error updating ambulance location", e);
        }
    }

    /**
     * Estimate ETA in minutes (simplified - can integrate with Google Maps API)
     */
    private Integer estimateETA(Ambulance ambulance, SosIncident incident) {
        Double distance = calculateDistance(ambulance, incident);
        // Assume average speed of 40 km/h in urban area
        return (int) Math.ceil((distance / 40) * 60);
    }

    /**
     * Calculate distance using Haversine formula
     */
    private Double calculateDistance(Ambulance ambulance, SosIncident incident) {
        return haversineDistance(
                ambulance.getLastLat(), ambulance.getLastLng(),
                incident.getPickupLatitude(), incident.getPickupLongitude());
    }

    /**
     * Haversine formula for distance calculation
     */
    private Double haversineDistance(Double lat1, Double lon1, Double lat2, Double lon2) {
        final int R = 6371; // Earth radius in km
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    /**
     * Publish Kafka event for ambulance assignment
     */
    private void publishAmbulanceAssignedEvent(SosIncident incident, Ambulance ambulance,
            @SuppressWarnings("unused") DriverAssignment assignment) {
        try {
            String message = String.format(
                    "{\"incidentId\":\"%s\",\"ambulanceId\":\"%s\",\"driverId\":\"%s\",\"status\":\"%s\",\"timestamp\":%d}",
                    incident.getIncidentId(), ambulance.getId(), ambulance.getDriverUserId(),
                    AssignmentStatus.ASSIGNED, System.currentTimeMillis());
            kafkaTemplate.send("ambulance-assignment-events", message);
            log.info("Published ambulance assignment event for incident {}", incident.getIncidentId());
        } catch (Exception e) {
            log.error("Error publishing Kafka event", e);
        }
    }
}
