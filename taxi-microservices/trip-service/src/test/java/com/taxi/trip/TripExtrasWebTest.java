package com.taxi.trip;

import com.taxi.trip.client.NotificationServiceClient;
import com.taxi.trip.client.UserServiceClient;
import com.taxi.trip.dto.RateTripRequest;
import com.taxi.trip.dto.TripDayStatisticsDto;
import com.taxi.trip.dto.TripResponseDto;
import com.taxi.trip.model.Trip;
import com.taxi.trip.model.TripStatus;
import com.taxi.trip.repository.TripRepository;
import com.taxi.trip.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;

/**
 * Доп. сценарии: JWT для защищённых маршрутов trip-service и оценка поездки через API.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class TripExtrasWebTest {

    @MockBean
    private UserServiceClient userServiceClient;

    @MockBean
    private NotificationServiceClient notificationServiceClient;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private TripRepository tripRepository;

    @BeforeEach
    void setUp() {
        tripRepository.deleteAll();
        doNothing().when(notificationServiceClient).createNotification(any());
        doNothing().when(userServiceClient).updateDriverRating(anyLong(), anyInt());
    }

    @Test
    void statisticsWithoutBearerTokenReturnsClientError() {
        var response = restTemplate.getForEntity("/trips/statistics/daily", String.class);
        assertThat(response.getStatusCode().value()).isBetween(401, 403);
    }

    @Test
    void statisticsWithServiceJwtReturnsOkAndAggregates() {
        seedTrip(100.0);
        seedTrip(200.0);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(jwtService.generateServiceToken("trip-it"));
        var entity = new HttpEntity<Void>(headers);

        var response = restTemplate.exchange(
                "/trips/statistics/daily",
                HttpMethod.GET,
                entity,
                TripDayStatisticsDto.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTripCount()).isEqualTo(2);
        assertThat(response.getBody().getAveragePrice()).isEqualTo(150.0);
    }

    @Test
    void completedTripCanBeRatedViaApiWithJwt() {
        Trip trip = new Trip();
        trip.setPassengerId(1L);
        trip.setOrigin("A");
        trip.setDestination("B");
        trip.setStatus(TripStatus.COMPLETED);
        trip.setDriverId(42L);
        trip.setPrice(90.0);
        trip = tripRepository.save(trip);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(jwtService.generateServiceToken("trip-it"));
        headers.setContentType(MediaType.APPLICATION_JSON);
        var body = new RateTripRequest(4, "thanks");
        var response = restTemplate.exchange(
                "/trips/" + trip.getId() + "/rate",
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                TripResponseDto.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getRating()).isEqualTo(4);
    }

    private void seedTrip(double price) {
        Trip trip = new Trip();
        trip.setPassengerId(1L);
        trip.setOrigin("x");
        trip.setDestination("y");
        trip.setStatus(TripStatus.PENDING);
        trip.setPrice(price);
        tripRepository.save(trip);
    }
}
