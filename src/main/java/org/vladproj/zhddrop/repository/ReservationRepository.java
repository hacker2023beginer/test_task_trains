package org.vladproj.zhddrop.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.vladproj.zhddrop.entity.Reservation;
import org.vladproj.zhddrop.entity.ReservationStatus;

import java.time.Instant;
import java.util.List;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Integer> {
    List<Reservation> findByStatusAndExpiresAtBefore(ReservationStatus reservationStatus, Instant now);
}
