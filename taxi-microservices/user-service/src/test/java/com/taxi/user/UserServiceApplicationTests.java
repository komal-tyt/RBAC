package com.taxi.user;

import com.taxi.user.dto.AuthRequest;
import com.taxi.user.dto.AuthResponse;
import com.taxi.user.dto.DriverDto;
import com.taxi.user.dto.PassengerDto;
import com.taxi.user.model.DriverStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class UserServiceApplicationTests {

    @LocalServerPort
    private int port;

    @Autowired
    private RestTemplateBuilder restTemplateBuilder;

    private TestRestTemplate restTemplate;

    @BeforeEach
    void initRestTemplate() {
        restTemplate = new TestRestTemplate(
                restTemplateBuilder
                        .rootUri("http://localhost:" + port)
                        .requestFactory(() -> new HttpComponentsClientHttpRequestFactory()));
    }

    // ========== POSITIVE TESTS ==========

    @Test
    void passengerFlowWorks() {

        PassengerDto request = new PassengerDto(null, "John Doe", "john@test.com", "+79991234567");
        ResponseEntity<PassengerDto> response = restTemplate.postForEntity("/passengers", request, PassengerDto.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isNotNull();
        assertThat(response.getBody().getEmail()).isEqualTo("john@test.com");


        ResponseEntity<PassengerDto> getResponse = restTemplate.getForEntity("/passengers/" + response.getBody().getId(), PassengerDto.class);
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getResponse.getBody().getName()).isEqualTo("John Doe");
    }

    @Test
    void driverStatusUpdateWorks() {
        DriverDto request = new DriverDto(null, "Mike Driver", "mike@test.com", "+79998887766", "LIC123", DriverStatus.OFFLINE, null, null);
        ResponseEntity<DriverDto> createResponse = restTemplate.postForEntity("/drivers", request, DriverDto.class);
        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Long driverId = createResponse.getBody().getId();


        String statusRequest = "{\"status\":\"ONLINE\"}";
        HttpHeaders patchHeaders = new HttpHeaders();
        patchHeaders.setContentType(MediaType.APPLICATION_JSON);
        restTemplate.exchange(
                "/drivers/" + driverId + "/status",
                HttpMethod.PATCH,
                new HttpEntity<>(statusRequest, patchHeaders),
                DriverDto.class);


        ResponseEntity<DriverDto> getResponse = restTemplate.getForEntity("/drivers/" + driverId, DriverDto.class);
        assertThat(getResponse.getBody().getStatus()).isEqualTo(DriverStatus.ONLINE);
    }

    @Test
    void jwtTokenGenerationTest() {

        AuthRequest request = new AuthRequest("jwt_test@test.com", "password123");
        ResponseEntity<AuthResponse> registerResponse = restTemplate.postForEntity("/auth/register/passenger", request, AuthResponse.class);

        assertThat(registerResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(registerResponse.getBody().getToken()).isNotNull();
        assertThat(registerResponse.getBody().getRole()).isEqualTo("PASSENGER");
    }

    @Test
    void jwtTokenValidationTest() {
        restTemplate.postForEntity("/auth/register/passenger", new AuthRequest("jwt_validate@test.com", "password123"), AuthResponse.class);

        ResponseEntity<AuthResponse> loginResponse = restTemplate.postForEntity("/auth/login", new AuthRequest("jwt_validate@test.com", "password123"), AuthResponse.class);
        assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        String token = loginResponse.getBody().getToken();
        assertThat(token).isNotNull();
        assertThat(token.split("\\.")).hasSize(3);
    }

    // ========== NEGATIVE TESTS ==========

    @Test
    void getUnknownPassengerReturns404() {
        ResponseEntity<PassengerDto> response = restTemplate.getForEntity("/passengers/99999", PassengerDto.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void duplicatePassengerEmailReturnsConflict() {
        PassengerDto request = new PassengerDto(null, "John Smith", "duplicate@test.com", "+79990000000");

        restTemplate.postForEntity("/passengers", request, PassengerDto.class);

        ResponseEntity<String> secondResponse = restTemplate.postForEntity("/passengers", request, String.class);
        assertThat(secondResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void duplicateDriverLicenseReturnsConflict() {
        DriverDto request = new DriverDto(null, "Driver Dup", "dup@test.com", "+79990000001", "DUP123", DriverStatus.OFFLINE, null, null);

        restTemplate.postForEntity("/drivers", request, DriverDto.class);
        ResponseEntity<String> secondResponse = restTemplate.postForEntity("/drivers", request, String.class);

        assertThat(secondResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void invalidPassengerPayloadReturns400() {
        PassengerDto invalid = new PassengerDto(null, "", "invalid@test.com", "+79990000002");
        ResponseEntity<String> response = restTemplate.postForEntity("/passengers", invalid, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void unknownDriverStatusReturns400() {
        DriverDto request = new DriverDto(null, "Bad Status", "bad@test.com", "+79990000003", "BAD123", DriverStatus.OFFLINE, null, null);
        ResponseEntity<DriverDto> createResponse = restTemplate.postForEntity("/drivers", request, DriverDto.class);
        Long driverId = createResponse.getBody().getId();

        // Невалидный статус
        String invalidStatus = "{\"status\":\"INVALID\"}";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<String> response = restTemplate.exchange(
                "/drivers/" + driverId + "/status",
                HttpMethod.PATCH,
                new HttpEntity<>(invalidStatus, headers),
                String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}