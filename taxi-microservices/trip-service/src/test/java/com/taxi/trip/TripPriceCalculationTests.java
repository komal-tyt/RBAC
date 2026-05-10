package com.taxi.trip;

import com.taxi.trip.client.UserServiceClient;
import com.taxi.trip.dto.CreateTripRequest;
import com.taxi.trip.dto.TariffDto;
import com.taxi.trip.service.DistanceCalculator;
import com.taxi.trip.service.TripService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
class TripPriceCalculationTests {

    @MockBean
    private UserServiceClient userServiceClient;

    @Autowired
    private TripService tripService;

    @Autowired
    private DistanceCalculator distanceCalculator;

    @BeforeEach
    void stubTariffs() {
        TariffDto standard = new TariffDto(null, "STANDARD", 0.0, 50.0, null, true);
        TariffDto comfort = new TariffDto(null, "COMFORT", 0.0, 95.0, null, true);
        TariffDto business = new TariffDto(null, "BUSINESS", 0.0, 170.0, null, true);
        when(userServiceClient.getTariff("STANDARD")).thenReturn(standard);
        when(userServiceClient.getTariff("COMFORT")).thenReturn(comfort);
        when(userServiceClient.getTariff("BUSINESS")).thenReturn(business);
        when(userServiceClient.getTariff(eq("INVALID_TARIFF_NAME"))).thenReturn(null);
        when(userServiceClient.getDefaultTariff()).thenReturn(standard);
    }

    @Test
    void priceCalculationWithCoordinatesTest() {
        CreateTripRequest request = new CreateTripRequest();
        request.setPassengerId(1L);
        request.setOrigin("Кремль");
        request.setDestination("Арбат");
        request.setOriginLat(55.751244);
        request.setOriginLng(37.618423);
        request.setDestLat(55.752121);
        request.setDestLng(37.590912);
        request.setTariffName("STANDARD");

        double km = distanceCalculator.calculateDistance(
                request.getOriginLat(), request.getOriginLng(),
                request.getDestLat(), request.getDestLng());
        Double price = tripService.calculatePriceForEstimate(request);

        assertThat(price).isCloseTo(km * 50.0, within(0.05));
    }

    @Test
    void priceCalculationWithDifferentTariffsTest() {
        CreateTripRequest request = new CreateTripRequest();
        request.setPassengerId(1L);
        request.setOrigin("Кремль");
        request.setDestination("Арбат");
        request.setOriginLat(55.751244);
        request.setOriginLng(37.618423);
        request.setDestLat(55.752121);
        request.setDestLng(37.590912);

        request.setTariffName("STANDARD");
        Double priceStandard = tripService.calculatePriceForEstimate(request);

        request.setTariffName("COMFORT");
        Double priceComfort = tripService.calculatePriceForEstimate(request);

        request.setTariffName("BUSINESS");
        Double priceBusiness = tripService.calculatePriceForEstimate(request);

        double km = distanceCalculator.calculateDistance(
                request.getOriginLat(), request.getOriginLng(),
                request.getDestLat(), request.getDestLng());
        assertThat(priceStandard).isCloseTo(km * 50.0, within(0.05));
        assertThat(priceComfort).isCloseTo(km * 95.0, within(0.05));
        assertThat(priceBusiness).isCloseTo(km * 170.0, within(0.05));
    }

    @Test
    void priceCalculationWithoutCoordinatesTest() {
        CreateTripRequest request = new CreateTripRequest();
        request.setPassengerId(1L);
        request.setOrigin("Очень длинное название улицы");
        request.setDestination("Другое очень длинное название");
        request.setTariffName("STANDARD");

        Double price = tripService.calculatePriceForEstimate(request);

        assertThat(price).isGreaterThan(0);
        assertThat(price).isNotNull();
    }

    @Test
    void priceCalculationLongDistanceTest() {
        CreateTripRequest request = new CreateTripRequest();
        request.setPassengerId(1L);
        request.setOrigin("Кремль");
        request.setDestination("ВДНХ");
        request.setOriginLat(55.751244);
        request.setOriginLng(37.618423);
        request.setDestLat(55.826163);
        request.setDestLng(37.640062);
        request.setTariffName("STANDARD");

        double km = distanceCalculator.calculateDistance(
                request.getOriginLat(), request.getOriginLng(),
                request.getDestLat(), request.getDestLng());
        Double price = tripService.calculatePriceForEstimate(request);

        assertThat(price).isCloseTo(km * 50.0, within(0.05));
    }

    @Test
    void priceCalculationInvalidTariffTest() {
        CreateTripRequest request = new CreateTripRequest();
        request.setPassengerId(1L);
        request.setOrigin("Кремль");
        request.setDestination("Арбат");
        request.setOriginLat(55.751244);
        request.setOriginLng(37.618423);
        request.setDestLat(55.752121);
        request.setDestLng(37.590912);
        request.setTariffName("INVALID_TARIFF_NAME");

        double km = distanceCalculator.calculateDistance(
                request.getOriginLat(), request.getOriginLng(),
                request.getDestLat(), request.getDestLng());
        Double price = tripService.calculatePriceForEstimate(request);

        assertThat(price).isCloseTo(km * 50.0, within(0.05));
    }

    /** Нет координат и нет estimatedDistance — только эвристика по строкам адреса. */
    @Test
    void priceCalculationMissingCoordinatesUsesHeuristic() {
        CreateTripRequest request = new CreateTripRequest();
        request.setPassengerId(1L);
        request.setOrigin("Кремль");
        request.setDestination("Арбат");
        request.setTariffName("STANDARD");
        assertThat(request.hasCoordinates()).isFalse();
        assertThat(request.getEstimatedDistanceKm()).isNull();

        double km = distanceCalculator.estimateByAddress(request.getOrigin(), request.getDestination());
        Double price = tripService.calculatePriceForEstimate(request);

        assertThat(price).isNotNull();
        assertThat(price).isCloseTo(km * 50.0, within(0.05));
    }
}