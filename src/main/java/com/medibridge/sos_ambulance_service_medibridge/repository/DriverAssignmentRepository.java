package com.medibridge.sos_ambulance_service_medibridge.repository;

import com.medibridge.sos_ambulance_service_medibridge.domain.entity.DriverAssignment;
import com.medibridge.sos_ambulance_service_medibridge.domain.enums.AssignmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DriverAssignmentRepository extends JpaRepository<DriverAssignment, UUID> {
    Optional<DriverAssignment> findByIncidentId(UUID incidentId);
    List<DriverAssignment> findByDriverUserId(UUID driverUserId);
    @Query("SELECT da FROM DriverAssignment da WHERE da.status IN :statuses ORDER BY da.assignedAt DESC")
    List<DriverAssignment> findByStatusIn(@Param("statuses") List<AssignmentStatus> statuses);
}
