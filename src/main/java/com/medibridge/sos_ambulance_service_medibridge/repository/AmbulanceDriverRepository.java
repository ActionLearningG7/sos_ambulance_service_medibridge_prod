package com.medibridge.sos_ambulance_service_medibridge.repository;

import com.medibridge.sos_ambulance_service_medibridge.domain.entity.AmbulanceDriver;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AmbulanceDriverRepository extends JpaRepository<AmbulanceDriver, UUID> {

    @Query("SELECT d FROM AmbulanceDriver d WHERE " +
            "(:search IS NULL OR " +
            "LOWER(d.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(d.lastName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(d.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "d.phoneNumber LIKE CONCAT('%', :search, '%')) AND " +
            "(:status IS NULL OR d.certificationStatus = :status)")
    Page<AmbulanceDriver> findAllBySearch(
            @Param("search") String search,
            @Param("status") String status,
            Pageable pageable);
}
