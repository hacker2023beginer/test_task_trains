package org.vladproj.zhddrop.repository.projection;

import org.vladproj.zhddrop.entity.RateCode;

public interface ExpiredHoldView {

    Integer getId();

    Integer getTripId();

    RateCode getRateCode();

    Integer getPassengerCount();
}
