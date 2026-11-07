package org.vladproj.zhddrop.validator;

import org.springframework.stereotype.Component;
import org.vladproj.zhddrop.exception.ReserveValidatorException;

import java.time.Instant;
import java.time.ZoneId;

@Component
public class ReserveValidator {
    public void isOverallPassengers(Integer availablePassengers, Integer reservePassengers) {
        if(reservePassengers.equals(0) || availablePassengers.compareTo(reservePassengers) < 0) throw new ReserveValidatorException("Not enough available seats");
    }

    public void isMatchedByDate(Instant departureDate,Instant currentDate) {
        if (departureDate.isBefore(currentDate)) throw new ReserveValidatorException("Current date bigger than departure date");
    }
}
