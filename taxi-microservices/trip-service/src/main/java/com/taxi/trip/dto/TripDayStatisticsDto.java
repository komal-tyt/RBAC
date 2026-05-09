package com.taxi.trip.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TripDayStatisticsDto {
    /** Календарный день, за который посчитана статистика */
    private LocalDate date;
    /** Число поездок, созданных в этот день */
    private long tripCount;
    /** Средняя цена по поездкам с известной ценой; null, если поездок нет */
    private Double averagePrice;
}
