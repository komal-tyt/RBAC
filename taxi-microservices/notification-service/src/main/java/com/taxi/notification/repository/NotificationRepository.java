package com.taxi.notification.repository;

import com.taxi.notification.model.NotificationStatus;
import com.taxi.notification.model.NotificationTask;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<NotificationTask, Long> {

    List<NotificationTask> findByTripIdOrderByCreatedAtDesc(Long tripId);

    @Modifying
    @Query("UPDATE NotificationTask n SET n.status = 'PROCESSING' " +
            "WHERE n.id = :id AND n.status = 'PENDING'")
    int markAsProcessing(@Param("id") Long id);

    @Modifying
    @Query("UPDATE NotificationTask n SET n.status = :status, " +
            "n.attempts = n.attempts + 1, " +
            "n.errorMessage = :errorMessage " +
            "WHERE n.id = :id")
    void updateStatusWithAttempt(@Param("id") Long id,
                                 @Param("status") NotificationStatus status,
                                 @Param("errorMessage") String errorMessage);

    @Modifying
    @Query("UPDATE NotificationTask n SET n.status = :status, n.errorMessage = :errorMessage WHERE n.id = :id")
    void updateStatus(@Param("id") Long id,
                      @Param("status") NotificationStatus status,
                      @Param("errorMessage") String errorMessage);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<NotificationTask> findByStatusAndAttemptsLessThanOrderByCreatedAtAsc(
            NotificationStatus status,
            Integer maxRetries,
            Pageable pageable
    );

    long countByStatus(NotificationStatus status);
}