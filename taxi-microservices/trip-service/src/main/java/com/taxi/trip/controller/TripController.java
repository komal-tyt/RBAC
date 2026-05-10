package com.taxi.trip.controller;

import com.taxi.trip.dto.CreateTripRequest;
import com.taxi.trip.dto.RateTripRequest;
import com.taxi.trip.dto.TripDayStatisticsDto;
import com.taxi.trip.dto.TripResponseDto;
import com.taxi.trip.dto.UpdateTripStatusRequest;
import com.taxi.trip.model.TripStatus;
import com.taxi.trip.service.TripService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/trips")
@RequiredArgsConstructor
@Slf4j
public class TripController {
    private final TripService tripService;

    @PostMapping
    public ResponseEntity<TripResponseDto> createTrip(@Valid @RequestBody CreateTripRequest request) {
        log.info("POST /trips - Creating trip for passenger: {}", request.getPassengerId());
        TripResponseDto trip = tripService.createTrip(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(trip);
    }

    @GetMapping("/statistics/daily")
    public ResponseEntity<TripDayStatisticsDto> getDailyStatistics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        log.info("GET /trips/statistics/daily date={}", date);
        return ResponseEntity.ok(tripService.getDailyStatistics(date));
    }

    @GetMapping
    public ResponseEntity<List<TripResponseDto>> getTrips(@RequestParam(name = "passenger_id") Long passengerId) {
        log.info("GET /trips?passenger_id={} - Fetching passenger trip history", passengerId);
        List<TripResponseDto> trips = tripService.getTripsByPassenger(passengerId);
        return ResponseEntity.ok(trips);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TripResponseDto> getTrip(@PathVariable Long id) {
        log.info("GET /trips/{} - Fetching trip", id);
        TripResponseDto trip = tripService.getTrip(id);
        return ResponseEntity.ok(trip);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<TripResponseDto> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTripStatusRequest request,
            @RequestHeader(value = "X-Driver-Id", required = false) Long driverId) {
        log.info("PATCH /trips/{}/status - Updating status to: {}", id, request.getStatus());
        TripResponseDto trip = tripService.updateTripStatus(id, request.getStatus(), driverId);
        return ResponseEntity.ok(trip);
    }

    @PostMapping("/{id}/rate")
    public ResponseEntity<TripResponseDto> rateTrip(
            @PathVariable Long id,
            @Valid @RequestBody RateTripRequest request) {
        log.info("POST /trips/{}/rate rating={}", id, request.getRating());
        TripResponseDto trip = tripService.rateTrip(id, request.getRating(), request.getComment());
        return ResponseEntity.ok(trip);
    }
}