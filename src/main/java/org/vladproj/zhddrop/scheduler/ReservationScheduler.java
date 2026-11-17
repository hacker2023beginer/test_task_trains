package org.vladproj.zhddrop.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.vladproj.zhddrop.service.TripService;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationScheduler {

    private final TripService tripService;

    @Scheduled(fixedDelay = 60_000, initialDelay = 60_000)
    public void processExpiredReservations() {
        try {
            int expired = tripService.expireHoldReservations();
            if (expired > 0) {
                log.info("Expired {} hold reservation(s)", expired);
            }
        } catch (RuntimeException ex) {
            log.error("Failed to expire hold reservations", ex);
        }
    }
}
