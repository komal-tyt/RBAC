package com.taxi.user.service;

import com.taxi.user.dto.AuthResponse;
import com.taxi.user.model.Driver;
import com.taxi.user.model.Passenger;
import com.taxi.user.repository.DriverRepository;
import com.taxi.user.repository.PassengerRepository;
import com.taxi.user.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final PassengerRepository passengerRepository;
    private final DriverRepository driverRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    public AuthResponse authenticate(String email, String password) {
        log.info("Authenticating user: {}", email);

        Passenger passenger = passengerRepository.findByEmail(email).orElse(null);
        if (passenger != null && passwordEncoder.matches(password, passenger.getPassword())) {
            String token = jwtService.generateToken(passenger.getId(), passenger.getEmail(), "PASSENGER");
            return new AuthResponse(token, "PASSENGER", passenger.getId(), passenger.getEmail(), passenger.getName());
        }

        Driver driver = driverRepository.findByEmail(email).orElse(null);
        if (driver != null && passwordEncoder.matches(password, driver.getPassword())) {
            String token = jwtService.generateToken(driver.getId(), driver.getEmail(), "DRIVER");
            return new AuthResponse(token, "DRIVER", driver.getId(), driver.getEmail(), driver.getName());
        }

        throw new RuntimeException("Invalid email or password");
    }

    public AuthResponse registerPassenger(String email, String password) {
        log.info("Registering new passenger: {}", email);

        if (passengerRepository.existsByEmail(email)) {
            throw new RuntimeException("Passenger with email " + email + " already exists");
        }

        Passenger passenger = new Passenger();
        passenger.setEmail(email);
        passenger.setPassword(passwordEncoder.encode(password));
        passenger.setName(email.split("@")[0]);
        passenger.setPhone("+70000000000");

        Passenger saved = passengerRepository.save(passenger);

        String token = jwtService.generateToken(saved.getId(), saved.getEmail(), "PASSENGER");
        return new AuthResponse(token, "PASSENGER", saved.getId(), saved.getEmail(), saved.getName());
    }

    public AuthResponse registerDriver(String email, String password) {
        log.info("Registering new driver: {}", email);

        if (driverRepository.existsByEmail(email)) {
            throw new RuntimeException("Driver with email " + email + " already exists");
        }

        Driver driver = new Driver();
        driver.setEmail(email);
        driver.setPassword(passwordEncoder.encode(password));
        driver.setName(email.split("@")[0]);
        driver.setPhone("+70000000000");
        driver.setLicenseNumber("TEMP" + System.currentTimeMillis());

        Driver saved = driverRepository.save(driver);

        String token = jwtService.generateToken(saved.getId(), saved.getEmail(), "DRIVER");
        return new AuthResponse(token, "DRIVER", saved.getId(), saved.getEmail(), saved.getName());
    }
}