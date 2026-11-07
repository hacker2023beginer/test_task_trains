package org.vladproj.zhddrop.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@AllArgsConstructor
public class ReservationResponse {
    private String trainNumber;

    private String rateCode;

    private Integer passengers;

    private Instant date;

    private String status;
}
