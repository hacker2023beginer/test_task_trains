package org.vladproj.zhddrop.event;

import org.vladproj.zhddrop.entity.RateCode;

import java.math.BigDecimal;
import java.time.Instant;

public record TicketConfirmedEvent(
    Integer reservationId,
    Integer tripId,
    String trainNumber,
    RateCode fareCode,
    Integer passengerCount,
    BigDecimal totalPrice,
    Instant confirmedAt
) {
}
