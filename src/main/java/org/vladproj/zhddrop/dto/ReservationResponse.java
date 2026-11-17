package org.vladproj.zhddrop.dto;

import lombok.Builder;
import lombok.Getter;
import org.vladproj.zhddrop.entity.RateCode;
import org.vladproj.zhddrop.entity.Reservation;
import org.vladproj.zhddrop.entity.ReservationStatus;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Builder
public class ReservationResponse {

    private Integer id;

    private Integer tripId;

    private String trainNumber;

    private RateCode fareCode;

    private Integer passengerCount;

    private BigDecimal totalPrice;

    private ReservationStatus status;

    private Instant createdAt;

    private Instant expiresAt;

    public static ReservationResponse from(Reservation reservation) {
        return ReservationResponse.builder()
            .id(reservation.getId())
            .tripId(reservation.getTrip() != null ? reservation.getTrip().getId() : null)
            .trainNumber(reservation.getTrip() != null ? reservation.getTrip().getTrainNumber() : null)
            .fareCode(reservation.getRateCode())
            .passengerCount(reservation.getPassengerCount())
            .totalPrice(reservation.getTotalPrice())
            .status(reservation.getStatus())
            .createdAt(reservation.getCreatedAt())
            .expiresAt(reservation.getExpiresAt())
            .build();
    }
}
