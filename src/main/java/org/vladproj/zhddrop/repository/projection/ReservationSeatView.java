package org.vladproj.zhddrop.repository.projection;

import org.vladproj.zhddrop.entity.RateCode;
import org.vladproj.zhddrop.entity.ReservationStatus;

import java.math.BigDecimal;

public interface ReservationSeatView {

    Integer getId();

    Integer getTripId();

    String getTrainNumber();

    RateCode getRateCode();

    Integer getPassengerCount();

    BigDecimal getTotalPrice();

    ReservationStatus getStatus();
}
