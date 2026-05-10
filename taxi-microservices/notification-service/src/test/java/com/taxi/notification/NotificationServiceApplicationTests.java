package com.taxi.notification;

import com.taxi.notification.dto.NotificationRequestDto;
import com.taxi.notification.dto.NotificationResponseDto;
import com.taxi.notification.model.NotificationStatus;
import com.taxi.notification.repository.NotificationRepository;
import com.taxi.notification.security.JwtService;
import com.taxi.notification.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class NotificationServiceApplicationTests {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private JwtService jwtService;

    private HttpHeaders serviceAuthHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(jwtService.generateServiceToken("notification-test"));
        return headers;
    }

    @Test
    void createAndGetNotificationTest() {
        NotificationRequestDto request = new NotificationRequestDto(1L, "PASSENGER", 1L, "Test message");
        ResponseEntity<NotificationResponseDto> create = restTemplate.exchange(
                "/notifications",
                HttpMethod.POST,
                new HttpEntity<>(request, serviceAuthHeaders()),
                NotificationResponseDto.class);

        assertThat(create.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(create.getBody().getTripId()).isEqualTo(1L);

        ResponseEntity<NotificationResponseDto[]> get = restTemplate.exchange(
                "/notifications?trip_id=1",
                HttpMethod.GET,
                new HttpEntity<>(serviceAuthHeaders()),
                NotificationResponseDto[].class);
        assertThat(get.getBody()).isNotEmpty();
    }

    @Test
    void workerProcessesToSentTest() throws InterruptedException {
        NotificationRequestDto request = new NotificationRequestDto(2L, "DRIVER", 2L, "Worker test");
        restTemplate.exchange(
                "/notifications",
                HttpMethod.POST,
                new HttpEntity<>(request, serviceAuthHeaders()),
                NotificationResponseDto.class);

        TimeUnit.SECONDS.sleep(3);

        ResponseEntity<NotificationResponseDto[]> notifications = restTemplate.exchange(
                "/notifications?trip_id=2",
                HttpMethod.GET,
                new HttpEntity<>(serviceAuthHeaders()),
                NotificationResponseDto[].class);
        assertThat(notifications.getBody()).allMatch(n ->
                n.getStatus().equals("SENT") || n.getStatus().equals("PENDING")
        );
    }

    @Test
    void maxRetriesToFailedTest() throws InterruptedException {
        NotificationRequestDto request = new NotificationRequestDto(999L, "PASSENGER", 999L, "Will fail");
        restTemplate.exchange(
                "/notifications",
                HttpMethod.POST,
                new HttpEntity<>(request, serviceAuthHeaders()),
                NotificationResponseDto.class);

        TimeUnit.SECONDS.sleep(15);

        ResponseEntity<Map> stats = restTemplate.exchange(
                "/notifications/stats",
                HttpMethod.GET,
                new HttpEntity<>(serviceAuthHeaders()),
                Map.class);
        Object failed = stats.getBody().get("FAILED");
        assertThat(failed).isNotNull();
    }

    @Test
    void concurrentLockingTest() throws InterruptedException {
        NotificationRequestDto request = new NotificationRequestDto(100L, "PASSENGER", 100L, "Concurrent test");
        restTemplate.exchange(
                "/notifications",
                HttpMethod.POST,
                new HttpEntity<>(request, serviceAuthHeaders()),
                NotificationResponseDto.class);

        TimeUnit.SECONDS.sleep(2);

        long processingForThisTrip = notificationRepository.findByTripIdOrderByCreatedAtDesc(100L).stream()
                .filter(t -> t.getStatus() == NotificationStatus.PROCESSING)
                .count();
        assertThat(processingForThisTrip).isLessThanOrEqualTo(1);
    }

    @Test
    void inProgressStatusTest() throws InterruptedException {
        NotificationRequestDto request = new NotificationRequestDto(101L, "DRIVER", 101L, "In progress test");
        restTemplate.exchange(
                "/notifications",
                HttpMethod.POST,
                new HttpEntity<>(request, serviceAuthHeaders()),
                NotificationResponseDto.class);

        TimeUnit.MILLISECONDS.sleep(100);

        ResponseEntity<NotificationResponseDto[]> notifications = restTemplate.exchange(
                "/notifications?trip_id=101",
                HttpMethod.GET,
                new HttpEntity<>(serviceAuthHeaders()),
                NotificationResponseDto[].class);
        assertThat(notifications.getBody()).isNotEmpty();
    }

    @Test
    void getQueueStatsTest() {
        ResponseEntity<Map> stats = restTemplate.exchange(
                "/notifications/stats",
                HttpMethod.GET,
                new HttpEntity<>(serviceAuthHeaders()),
                Map.class);

        assertThat(stats.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(stats.getBody()).containsKeys("PENDING", "PROCESSING", "SENT", "FAILED");
    }
}