package com.taxi.user;

import com.taxi.user.dto.DriverDto;
import com.taxi.user.model.DriverStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Redis: кэш доступных водителей при переходе ONLINE / с offline.
 * Требуется Docker; без Docker тесты пропускаются.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class DriverRedisCacheIntegrationTest {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379).toString());
    }

    @LocalServerPort
    private int port;

    @Autowired
    private RestTemplateBuilder restTemplateBuilder;

    private TestRestTemplate restTemplate;

    @BeforeEach
    void setUpRestAndClearCache() {
        restTemplate = new TestRestTemplate(
                restTemplateBuilder
                        .rootUri("http://localhost:" + port)
                        .requestFactory(() -> new HttpComponentsClientHttpRequestFactory()));
        restTemplate.exchange("/drivers/cache", HttpMethod.DELETE, HttpEntity.EMPTY, Void.class);
    }

    private void patchStatus(Long driverId, String jsonBody) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        restTemplate.exchange(
                "/drivers/" + driverId + "/status",
                HttpMethod.PATCH,
                new HttpEntity<>(jsonBody, headers),
                DriverDto.class);
    }

    @Test
    void redisCacheAddsDriverWhenStatusBecomesOnline() {
        DriverDto create = new DriverDto(
                null, "Redis Driver", "redis.driver@test.com", "+79991112233",
                "LIC-REDIS-1", DriverStatus.OFFLINE, null, null);
        ResponseEntity<DriverDto> created = restTemplate.postForEntity("/drivers", create, DriverDto.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Long id = created.getBody().getId();

        patchStatus(id, "{\"status\":\"ONLINE\"}");

        ResponseEntity<Set<String>> cache = restTemplate.exchange(
                "/drivers/cache/available",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<Set<String>>() {});

        assertThat(cache.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(cache.getBody()).contains(id.toString());
    }

    @Test
    void redisCacheRemovesDriverWhenLeavingOnline() {
        DriverDto create = new DriverDto(
                null, "Redis Driver 2", "redis.driver2@test.com", "+79991112244",
                "LIC-REDIS-2", DriverStatus.OFFLINE, null, null);
        ResponseEntity<DriverDto> created = restTemplate.postForEntity("/drivers", create, DriverDto.class);
        Long id = created.getBody().getId();

        patchStatus(id, "{\"status\":\"ONLINE\"}");
        patchStatus(id, "{\"status\":\"OFFLINE\"}");

        ResponseEntity<Set<String>> cache = restTemplate.exchange(
                "/drivers/cache/available",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<Set<String>>() {});

        assertThat(cache.getBody()).doesNotContain(id.toString());
    }
}
