package org.vladproj.zhddrop.scheduler;

import lombok.AllArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.vladproj.zhddrop.service.TripService;

@Component
@AllArgsConstructor
public class ReservationScheduler {

    private final TripService tripService;

    @Scheduled(fixedRate = 60000)
    public void processExpiredReservations() {
        tripService.expireHoldReservations();
    }
}
