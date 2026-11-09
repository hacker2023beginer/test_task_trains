package org.vladproj.zhddrop.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.vladproj.zhddrop.entity.RateCode;
import org.vladproj.zhddrop.entity.TripRate;

import java.util.Optional;

public interface TripRateRepository extends JpaRepository<TripRate, Integer> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT tr FROM TripRate tr WHERE tr.trip.id = :tripId AND tr.rateCode = :rateCode")
    Optional<TripRate> findForUpdate(@Param("tripId") Integer tripId, @Param("rateCode") RateCode rateCode);

    Optional<TripRate> getTripRateByTrip_IdAndRateCode(Integer id, RateCode rateCode);
}
