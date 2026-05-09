package com.taxi.notification.service;

import com.taxi.notification.dto.NotificationRequestDto;
import com.taxi.notification.dto.NotificationResponseDto;
import com.taxi.notification.model.NotificationStatus;
import com.taxi.notification.model.NotificationTask;
import com.taxi.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {
    private final NotificationRepository notificationRepository;

    @Value("${notification.worker.max-retries:3}")
    private int maxRetries;

    @Transactional
    public NotificationResponseDto createNotification(NotificationRequestDto request) {
        log.info("Creating notification task for trip {} to {} {}",
                request.getTripId(), request.getRecipientType(), request.getRecipientId());

        NotificationTask task = new NotificationTask();
        task.setTripId(request.getTripId());
        task.setRecipientType(request.getRecipientType());
        task.setRecipientId(request.getRecipientId());
        task.setMessage(request.getMessage());
        task.setStatus(NotificationStatus.PENDING);
        task.setAttempts(0);

        NotificationTask saved = notificationRepository.save(task);
        log.info("Notification task created with id: {}", saved.getId());

        return convertToDto(saved);
    }

    public List<NotificationResponseDto> getNotificationsByTrip(Long tripId) {
        log.debug("Fetching notifications for trip: {}", tripId);
        List<NotificationTask> tasks = notificationRepository.findByTripIdOrderByCreatedAtDesc(tripId);
        return tasks.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public Optional<NotificationTask> claimNextPendingTask() {
        List<NotificationTask> pendingTasks = notificationRepository.findByStatusAndAttemptsLessThanOrderByCreatedAtAsc(
                NotificationStatus.PENDING,
                maxRetries,
                PageRequest.of(0, 1)
        );
        if (pendingTasks.isEmpty()) {
            return Optional.empty();
        }

        NotificationTask task = pendingTasks.get(0);
        int claimed = notificationRepository.markAsProcessing(task.getId());
        if (claimed == 0) {
            return Optional.empty();
        }

        NotificationTask processingTask = notificationRepository.findById(task.getId())
                .orElseThrow(() -> new RuntimeException("Claimed notification task not found: " + task.getId()));
        return Optional.of(processingTask);
    }

    @Transactional
    public boolean sendNotification(NotificationTask task) {
        log.info("Sending notification to {} {}: {}",
                task.getRecipientType(), task.getRecipientId(), task.getMessage());

        try {
            Thread.sleep(1000);

            if (Math.random() > 0.1) {
                log.info("Notification {} sent successfully to {} {}",
                        task.getId(), task.getRecipientType(), task.getRecipientId());
                notificationRepository.updateStatus(
                        task.getId(),
                        NotificationStatus.SENT,
                        null
                );
                return true;
            } else {
                throw new RuntimeException("Failed to send notification (simulated error)");
            }
        } catch (Exception e) {
            log.error("Failed to send notification {}: {}", task.getId(), e.getMessage());

            int newAttempts = task.getAttempts() + 1;
            if (newAttempts >= maxRetries) {
                notificationRepository.updateStatusWithAttempt(
                        task.getId(),
                        NotificationStatus.FAILED,
                        e.getMessage() + " (max retries exceeded)"
                );
                log.warn("Notification {} failed after {} attempts", task.getId(), maxRetries);
            } else {
                notificationRepository.updateStatusWithAttempt(
                        task.getId(),
                        NotificationStatus.PENDING,
                        e.getMessage() + " (retry " + newAttempts + "/" + maxRetries + ")"
                );
            }
            return false;
        }
    }

    public Map<String, Long> getQueueStats() {
        Map<String, Long> stats = new HashMap<>();
        stats.put("PENDING", notificationRepository.countByStatus(NotificationStatus.PENDING));
        stats.put("PROCESSING", notificationRepository.countByStatus(NotificationStatus.PROCESSING));
        stats.put("SENT", notificationRepository.countByStatus(NotificationStatus.SENT));
        stats.put("FAILED", notificationRepository.countByStatus(NotificationStatus.FAILED));
        return stats;
    }

    private NotificationResponseDto convertToDto(NotificationTask task) {
        return new NotificationResponseDto(
                task.getId(),
                task.getTripId(),
                task.getRecipientType(),
                task.getRecipientId(),
                task.getMessage(),
                task.getStatus().toString(),
                task.getAttempts(),
                task.getErrorMessage(),
                task.getCreatedAt()
        );
    }
}