package org.vladproj.zhddrop.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;

@Getter
@Setter
public class TripSearchResponse {
    private String trainNumber;

    private Instant departureTime;

    private Instant arrivalTime;

    private ArrayList<TripRateResponse> tripRates;
}
