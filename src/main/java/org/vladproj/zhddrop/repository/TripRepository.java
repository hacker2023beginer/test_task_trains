package org.vladproj.zhddrop.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.vladproj.zhddrop.entity.Trip;

import java.time.Instant;
import java.util.List;

@Repository
public interface TripRepository extends JpaRepository<Trip, Integer> {

    @Query("""
        SELECT DISTINCT trip FROM Trip trip
        JOIN FETCH trip.tripRates rate
        WHERE trip.fromStation.code = :from
          AND trip.toStation.code = :to
          AND trip.departureTime >= :startOfDay
          AND trip.departureTime < :endOfDay
          AND trip.departureTime > :now
          AND rate.availableSeats >= :passengers
        """)
    List<Trip> findAvailableTrips(@Param("from") String from,
                                  @Param("to") String to,
                                  @Param("startOfDay") Instant startOfDay,
                                  @Param("endOfDay") Instant endOfDay,
                                  @Param("now") Instant now,
                                  @Param("passengers") int passengers);
}
