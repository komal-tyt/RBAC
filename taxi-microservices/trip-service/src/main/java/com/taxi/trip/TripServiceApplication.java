package com.taxi.trip;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class TripServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(TripServiceApplication.class, args);
    }

    @Transactional
    public TripResponseDto rateTrip(Long tripId, Integer rating, String comment) {
        log.info("Rating trip {} with {} stars", tripId, rating);

        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new RuntimeException("Trip not found: " + tripId));

        if (trip.getStatus() != TripStatus.COMPLETED) {
            throw new RuntimeException("Cannot rate trip that is not completed. Current status: " + trip.getStatus());
        }

        if (trip.getRating() != null) {
            throw new RuntimeException("Trip already rated with " + trip.getRating() + " stars");
        }

        trip.setRating(rating);
        tripRepository.save(trip);

        if (trip.getDriverId() != null) {
            userServiceClient.updateDriverRating(trip.getDriverId(), rating);
        }

        NotificationRequestDto thankYouNotification = new NotificationRequestDto(
                tripId,
                "PASSENGER",
                trip.getPassengerId(),
                "Thank you for rating trip " + tripId + " with " + rating + " stars!"
        );
        notificationServiceClient.createNotification(thankYouNotification);

        log.info("Trip {} rated successfully", tripId);
        return convertToDto(trip);
    }
}