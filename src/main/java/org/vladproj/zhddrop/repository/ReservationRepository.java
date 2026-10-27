package org.vladproj.zhddrop.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.vladproj.zhddrop.entity.Reservation;

public interface ReservationRepository extends JpaRepository<Reservation, Integer> {
}
