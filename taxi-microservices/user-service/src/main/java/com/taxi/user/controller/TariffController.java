package com.taxi.user.controller;

import com.taxi.user.dto.TariffDto;
import com.taxi.user.model.Tariff;
import com.taxi.user.repository.TariffRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/tariffs")
@RequiredArgsConstructor
@Slf4j
public class TariffController {
    private final TariffRepository tariffRepository;

    @GetMapping
    public ResponseEntity<List<TariffDto>> getAllTariffs() {
        log.info("GET /tariffs - Fetching all tariffs");
        List<TariffDto> tariffs = tariffRepository.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(tariffs);
    }

    @GetMapping("/active")
    public ResponseEntity<TariffDto> getActiveTariff() {
        log.info("GET /tariffs/active - Fetching active tariff");
        return tariffRepository.findFirstByActiveTrueOrderByIdAsc()
                .map(this::convertToDto)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/{name}")
    public ResponseEntity<TariffDto> getTariffByName(@PathVariable String name) {
        log.info("GET /tariffs/{} - Fetching tariff by name", name);
        return tariffRepository.findByName(name)
                .map(this::convertToDto)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<TariffDto> createTariff(@RequestBody Tariff tariff) {
        log.info("POST /tariffs - Creating tariff: {}", tariff.getName());
        Tariff saved = tariffRepository.save(tariff);
        return ResponseEntity.ok(convertToDto(saved));
    }

    private TariffDto convertToDto(Tariff tariff) {
        return new TariffDto(
                tariff.getId(),
                tariff.getName(),
                tariff.getBasePrice(),
                tariff.getPricePerKm(),
                tariff.getPricePerMinute(),
                tariff.getActive()
        );
    }
}