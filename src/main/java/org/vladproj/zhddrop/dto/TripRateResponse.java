package org.vladproj.zhddrop.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.vladproj.zhddrop.entity.RateCode;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class TripRateResponse {

    private RateCode fareCode;

    private BigDecimal price;

    private Integer availableSeats;
}
