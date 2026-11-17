package org.vladproj.zhddrop.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.vladproj.zhddrop.entity.RateCode;
import org.vladproj.zhddrop.entity.TripRate;

import java.math.BigDecimal;
import java.util.Optional;

public interface TripRateRepository extends JpaRepository<TripRate, Integer> {

    @Query("SELECT tr.price FROM TripRate tr WHERE tr.trip.id = :tripId AND tr.rateCode = :rateCode")
    Optional<BigDecimal> findPrice(@Param("tripId") Integer tripId, @Param("rateCode") RateCode rateCode);

    @Modifying(flushAutomatically = true)
    @Query("""
        UPDATE TripRate tr
           SET tr.availableSeats = tr.availableSeats - :seats
         WHERE tr.trip.id = :tripId
           AND tr.rateCode = :rateCode
           AND tr.availableSeats >= :seats
        """)
    int takeSeats(@Param("tripId") Integer tripId,
                  @Param("rateCode") RateCode rateCode,
                  @Param("seats") int seats);

    @Modifying(flushAutomatically = true)
    @Query("""
        UPDATE TripRate tr
           SET tr.availableSeats = tr.availableSeats + :seats
         WHERE tr.trip.id = :tripId
           AND tr.rateCode = :rateCode
        """)
    int releaseSeats(@Param("tripId") Integer tripId,
                     @Param("rateCode") RateCode rateCode,
                     @Param("seats") int seats);
}
