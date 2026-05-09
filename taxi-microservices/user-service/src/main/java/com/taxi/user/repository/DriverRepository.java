package com.taxi.user.repository;

import com.taxi.user.model.Driver;
import com.taxi.user.model.DriverStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface DriverRepository extends JpaRepository<Driver, Long> {
    Optional<Driver> findByEmail(String email);
    Optional<Driver> findByLicenseNumber(String licenseNumber);
    boolean existsByEmail(String email);
    boolean existsByLicenseNumber(String licenseNumber);

    List<Driver> findByStatus(DriverStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Driver> findFirstByStatusOrderByIdAsc(DriverStatus status);

    @Modifying
    @Query("UPDATE Driver d SET d.status = :newStatus WHERE d.id = :driverId AND d.status = :expectedStatus")
    int updateStatusIfCurrent(@Param("driverId") Long driverId,
                              @Param("expectedStatus") DriverStatus expectedStatus,
                              @Param("newStatus") DriverStatus newStatus);
}