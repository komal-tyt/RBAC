package com.taxi.user;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Проверяет, что docker-compose в каталоге taxi-microservices описывает стек сервисов.
 */
class DockerComposeLayoutTest {

    @Test
    void dockerComposeDefinesRedisAndMicroservices() throws Exception {
        Path compose = resolveComposeFile();
        assertThat(compose).exists();
        String yaml = Files.readString(compose);
        assertThat(yaml)
                .contains("user-service:")
                .contains("trip-service:")
                .contains("notification-service:")
                .contains("redis:");
    }

    private static Path resolveComposeFile() {
        Path cwd = Paths.get("").toAbsolutePath();
        Path[] candidates = new Path[] {
                cwd.resolve("docker-compose.yml"),
                cwd.resolve("../docker-compose.yml").normalize(),
                cwd.resolve("../../taxi-microservices/docker-compose.yml").normalize()
        };
        for (Path p : candidates) {
            if (Files.exists(p)) {
                return p;
            }
        }
        throw new IllegalStateException(
                "docker-compose.yml not found; tried relative to " + cwd);
    }
}
