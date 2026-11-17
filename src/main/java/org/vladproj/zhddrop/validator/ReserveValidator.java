package org.vladproj.zhddrop.validator;

import org.springframework.stereotype.Component;
import org.vladproj.zhddrop.exception.ConflictException;

import java.time.Instant;

@Component
public class ReserveValidator {

    public void requireNotDeparted(Instant departureTime, Instant now) {
        if (departureTime == null || !departureTime.isAfter(now)) {
            throw new ConflictException("TRIP_ALREADY_DEPARTED", "Trip has already departed and cannot be reserved");
        }
    }

}
