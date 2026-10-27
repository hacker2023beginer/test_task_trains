package org.vladproj.zhddrop.dto;

import lombok.Getter;
import lombok.Setter;
import org.vladproj.zhddrop.entity.RateCode;

import java.math.BigDecimal;

@Getter
@Setter
public class TripRateResponse {
    private RateCode rateCode;

    private BigDecimal price;

    private Integer availableSeats;
}
