package com.taxi.trip.controller;

import com.taxi.trip.dto.CreateTripRequest;
import com.taxi.trip.service.DistanceCalculator;
import com.taxi.trip.service.TripService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/price")
@RequiredArgsConstructor
@Slf4j
public class PriceController {

    private final DistanceCalculator distanceCalculator;
    private final TripService tripService;

    @PostMapping("/distance")
    public ResponseEntity<Map<String, Object>> calculateDistance(
            @RequestParam double lat1, @RequestParam double lon1,
            @RequestParam double lat2, @RequestParam double lon2) {

        double distance = distanceCalculator.calculateDistance(lat1, lon1, lat2, lon2);

        Map<String, Object> response = new HashMap<>();
        response.put("distanceKm", distance);
        response.put("pointA", Map.of("lat", lat1, "lng", lon1));
        response.put("pointB", Map.of("lat", lat2, "lng", lon2));

        log.info("Calculated distance: {} km between points", distance);
        return ResponseEntity.ok(response);
    }


    @PostMapping("/estimate")
    public ResponseEntity<Map<String, Object>> estimatePrice(@RequestBody CreateTripRequest request) {
        Double price = tripService.calculatePriceForEstimate(request);

        Map<String, Object> response = new HashMap<>();
        response.put("origin", request.getOrigin());
        response.put("destination", request.getDestination());
        response.put("tariff", request.getTariffName());
        response.put("estimatedPrice", price);

        if (request.hasCoordinates()) {
            double distance = distanceCalculator.calculateDistance(
                    request.getOriginLat(), request.getOriginLng(),
                    request.getDestLat(), request.getDestLng()
            );
            response.put("distanceKm", distance);
        }

        return ResponseEntity.ok(response);
    }
}