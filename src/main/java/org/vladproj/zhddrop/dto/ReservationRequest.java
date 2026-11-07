package org.vladproj.zhddrop.dto;

import lombok.Getter;
import lombok.Setter;
import org.vladproj.zhddrop.entity.RateCode;

@Getter
@Setter
public class ReservationRequest {
    private Integer id;

    private RateCode rateCode;

    private Integer passengers;
}
