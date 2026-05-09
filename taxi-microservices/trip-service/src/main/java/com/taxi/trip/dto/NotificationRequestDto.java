package com.taxi.trip.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationRequestDto {
    private Long tripId;
    private String recipientType;
    private Long recipientId;
    private String message;
}
