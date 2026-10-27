package org.vladproj.zhddrop.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.vladproj.zhddrop.entity.Station;
import org.vladproj.zhddrop.entity.Trip;

import java.time.Instant;
import java.util.List;

@Repository
public interface TripRepository extends JpaRepository<Trip, Integer> {
    @Query("SELECT trip FROM Trip trip " +
        "LEFT JOIN FETCH trip.tripRates " +
        "WHERE trip.fromStation = :from_station " +
        "AND trip.toStation = :to_station " +
        "AND trip.departureTime BETWEEN :start_of_day AND :end_of_day " +
        "AND trip.departureTime > :current_instant " +
        "AND EXISTS (SELECT r FROM TripRate r WHERE r.trip = trip AND r.availableSeats >= :passengers)")
    List<Trip> findAvailableTrips(@Param("from_station") Station fromStation,
                                  @Param("to_station") Station toStation,
                                  @Param("start_of_day") Instant startOfDay,
                                  @Param("end_of_day") Instant endOfDay,
                                  @Param("current_instant") Instant currentInstant,
                                  @Param("passengers") Integer passengers);
}
