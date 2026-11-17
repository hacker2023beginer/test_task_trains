package org.vladproj.zhddrop.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

@Getter
@AllArgsConstructor
public class TripSearchResponse {

    private String trainNumber;

    private Instant departureTime;

    private Instant arrivalTime;

    private List<TripRateResponse> rates;
}
