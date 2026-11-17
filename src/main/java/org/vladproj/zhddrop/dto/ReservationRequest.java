package org.vladproj.zhddrop.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;
import org.vladproj.zhddrop.entity.RateCode;

@Getter
@Setter
public class ReservationRequest {

    @NotNull(message = "tripId is required")
    @Positive(message = "tripId must be positive")
    private Integer tripId;

    @NotNull(message = "fareCode is required")
    private RateCode fareCode;

    @NotNull(message = "passengerCount is required")
    @Min(value = 1, message = "passengerCount must be at least 1")
    @Max(value = 10, message = "passengerCount must not exceed 10")
    private Integer passengerCount;
}
