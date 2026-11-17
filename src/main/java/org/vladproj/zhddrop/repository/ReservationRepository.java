package org.vladproj.zhddrop.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.vladproj.zhddrop.entity.Reservation;
import org.vladproj.zhddrop.entity.ReservationStatus;
import org.vladproj.zhddrop.repository.projection.ExpiredHoldView;
import org.vladproj.zhddrop.repository.projection.ReservationSeatView;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Integer> {

    @Query("""
        SELECT r.id AS id, r.trip.id AS tripId, r.rateCode AS rateCode,
               r.passengerCount AS passengerCount
          FROM Reservation r
         WHERE r.status = :status
           AND r.expiresAt <= :now
        """)
    List<ExpiredHoldView> findExpiredHolds(@Param("status") ReservationStatus status,
                                           @Param("now") Instant now);

    @Query("""
        SELECT r.id AS id, r.trip.id AS tripId, r.trip.trainNumber AS trainNumber,
               r.rateCode AS rateCode, r.passengerCount AS passengerCount,
               r.totalPrice AS totalPrice, r.status AS status
          FROM Reservation r
         WHERE r.id = :id
        """)
    Optional<ReservationSeatView> findSeatView(@Param("id") Integer id);

    @Query("SELECT r.status FROM Reservation r WHERE r.id = :id")
    Optional<ReservationStatus> findStatusById(@Param("id") Integer id);

    @Modifying(flushAutomatically = true)
    @Query("""
        UPDATE Reservation r
           SET r.status = :target
         WHERE r.id = :id
           AND r.status = :expected
        """)
    int updateStatus(@Param("id") Integer id,
                     @Param("expected") ReservationStatus expected,
                     @Param("target") ReservationStatus target);

    @Modifying(flushAutomatically = true)
    @Query("""
        UPDATE Reservation r
           SET r.status = :target
         WHERE r.id = :id
           AND r.status = :expected
           AND r.expiresAt > :now
        """)
    int updateStatusIfNotExpired(@Param("id") Integer id,
                                 @Param("expected") ReservationStatus expected,
                                 @Param("target") ReservationStatus target,
                                 @Param("now") Instant now);
}
