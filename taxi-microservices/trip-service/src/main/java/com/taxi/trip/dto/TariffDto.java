package com.taxi.trip.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TariffDto {
    private Long id;
    private String name;
    private Double basePrice;
    private Double pricePerKm;
    private Double pricePerMinute;
    private Boolean active;
}