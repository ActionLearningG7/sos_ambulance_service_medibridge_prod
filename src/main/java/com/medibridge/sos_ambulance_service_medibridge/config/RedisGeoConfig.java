package com.medibridge.sos_ambulance_service_medibridge.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Redis Configuration for GEO indices and live state management
 * Handles ambulance geolocation tracking and real-time driver status
 */
@Configuration
public class RedisGeoConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // String serializer for keys
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);

        // JSON serializer for values
        Jackson2JsonRedisSerializer<Object> jsonSerializer = new Jackson2JsonRedisSerializer<>(Object.class);
        ObjectMapper objectMapper = new ObjectMapper();
        jsonSerializer.setObjectMapper(objectMapper);

        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);

        template.afterPropertiesSet();
        return template;
    }

    /**
     * Redis GEO Index Keys (for GEOSEARCH operations)
     * Pattern: geo:ambulances:available:{organizationId}
     * Stores available ambulances with their coordinates
     */
    public static String getAvailableAmbulancesGeoKey(String organizationId) {
        return "geo:ambulances:available:" + organizationId;
    }

    /**
     * Redis Live State Key (for real-time status)
     * Pattern: live:ambulance:{ambulanceId}
     * Stores driver info, status, current coordinates
     */
    public static String getAmbulanceLiveStateKey(String ambulanceId) {
        return "live:ambulance:" + ambulanceId;
    }

    /**
     * Redis Lock Key (for distributed locking)
     * Pattern: lock:ambulance:{ambulanceId}:{incidentId}
     */
    public static String getAmbulanceLockKey(String ambulanceId, String incidentId) {
        return "lock:ambulance:" + ambulanceId + ":" + incidentId;
    }

    /**
     * Redis Driver Location Key
     * Pattern: driver:location:{driverUserId}
     */
    public static String getDriverLocationKey(String driverUserId) {
        return "driver:location:" + driverUserId;
    }

    /**
     * Configuration constants
     */
    public static final int GEOSEARCH_RADIUS_KM = 50000;
    public static final long LOCK_TIMEOUT_SECONDS = 30;
    public static final long LOCATION_TTL_SECONDS = 3600;
}
