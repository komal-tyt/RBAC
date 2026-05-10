package com.taxi.trip;

import com.taxi.trip.dto.TripDayStatisticsDto;
import com.taxi.trip.model.Trip;
import com.taxi.trip.model.TripStatus;
import com.taxi.trip.repository.TripRepository;
import com.taxi.trip.service.TripService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class TripStatisticsTests {

    @Autowired
    private TripService tripService;

    @Autowired
    private TripRepository tripRepository;

    @BeforeEach
    void cleanTrips() {
        tripRepository.deleteAll();
    }

    @Test
    void statisticsReturnsZeroTripsWhenEmpty() {
        LocalDate today = LocalDate.now();
        TripDayStatisticsDto stats = tripService.getDailyStatistics(today);

        assertThat(stats.getDate()).isEqualTo(today);
        assertThat(stats.getTripCount()).isZero();
        assertThat(stats.getAveragePrice()).isNull();
    }

    @Test
    void statisticsDailyAggregatesCountAndAveragePrice() {
        LocalDate today = LocalDate.now();

        Trip a = new Trip();
        a.setPassengerId(1L);
        a.setOrigin("o1");
        a.setDestination("d1");
        a.setStatus(TripStatus.COMPLETED);
        a.setPrice(80.0);
        tripRepository.save(a);

        Trip b = new Trip();
        b.setPassengerId(2L);
        b.setOrigin("o2");
        b.setDestination("d2");
        b.setStatus(TripStatus.PENDING);
        b.setPrice(120.0);
        tripRepository.save(b);

        TripDayStatisticsDto stats = tripService.getDailyStatistics(today);

        assertThat(stats.getTripCount()).isEqualTo(2);
        assertThat(stats.getAveragePrice()).isEqualTo(100.0);
    }
}