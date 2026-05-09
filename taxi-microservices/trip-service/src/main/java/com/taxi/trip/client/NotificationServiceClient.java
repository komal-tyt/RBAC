package com.taxi.trip.client;

import com.taxi.trip.dto.NotificationRequestDto;
import com.taxi.trip.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceClient {
    private final WebClient.Builder webClientBuilder;
    private final JwtService jwtService;

    @Value("${notification-service.url}")
    private String notificationServiceUrl;

    @Value("${spring.application.name:trip-service}")
    private String serviceName;

    private String authHeaderValue() {
        return "Bearer " + jwtService.generateServiceToken(serviceName);
    }

    public void createNotification(NotificationRequestDto request) {
        try {
            webClientBuilder.build()
                    .post()
                    .uri(notificationServiceUrl + "/notifications")
                    .header("Authorization", authHeaderValue())
                    .bodyValue(request)
                    .retrieve()
                    .toBodilessEntity()
                    .block();
            log.info("Notification enqueued for trip {} and recipient {} {}",
                    request.getTripId(), request.getRecipientType(), request.getRecipientId());
        } catch (Exception e) {
            log.error("Failed to enqueue notification for trip {}: {}", request.getTripId(), e.getMessage());
        }
    }
}
