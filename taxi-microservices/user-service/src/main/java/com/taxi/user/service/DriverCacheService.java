package com.taxi.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class DriverCacheService {

    private final RedisTemplate<String, String> redisTemplate;

    @Value("${cache.available-drivers-key}")
    private String availableDriversKey;

    public void addAvailableDriver(Long driverId) {
        try {
            redisTemplate.opsForSet().add(availableDriversKey, driverId.toString());
            log.debug("Driver {} added to Redis cache", driverId);
        } catch (Exception e) {
            log.error("Failed to add driver {} to Redis: {}", driverId, e.getMessage());
        }
    }

    public void removeAvailableDriver(Long driverId) {
        try {
            redisTemplate.opsForSet().remove(availableDriversKey, driverId.toString());
            log.debug("Driver {} removed from Redis cache", driverId);
        } catch (Exception e) {
            log.error("Failed to remove driver {} from Redis: {}", driverId, e.getMessage());
        }
    }

    public String popAvailableDriver() {
        try {
            return redisTemplate.opsForSet().pop(availableDriversKey);
        } catch (Exception e) {
            log.error("Failed to pop driver from Redis: {}", e.getMessage());
            return null;
        }
    }


    public Set<String> getAllAvailableDrivers() {
        try {
            return redisTemplate.opsForSet().members(availableDriversKey);
        } catch (Exception e) {
            log.error("Failed to get available drivers from Redis: {}", e.getMessage());
            return Set.of();
        }
    }

    public void clearCache() {
        try {
            redisTemplate.delete(availableDriversKey);
            log.info("Redis cache cleared");
        } catch (Exception e) {
            log.error("Failed to clear Redis cache: {}", e.getMessage());
        }
    }
}