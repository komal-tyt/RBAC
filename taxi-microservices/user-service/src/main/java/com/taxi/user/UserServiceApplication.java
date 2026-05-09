package com.taxi.trip.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class DistanceCalculator {

    private static final double EARTH_RADIUS_KM = 6371.0;

    public double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);

        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        double distance = EARTH_RADIUS_KM * c;

        return Math.round(distance * 100.0) / 100.0;
    }

 double estimateByAddress(String origin, String destination) {
        int originLen = origin == null ? 0 : origin.length();
        int destLen = destination == null ? 0 : destination.length();
        double distance = Math.max(1.0, (originLen + destLen) * 0.3);
        return Math.round(distance * 100.0) / 100.0;
    }
}