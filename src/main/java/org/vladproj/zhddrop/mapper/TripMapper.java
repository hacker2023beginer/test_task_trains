package org.vladproj.zhddrop.mapper;

import org.mapstruct.Mapper;
import org.vladproj.zhddrop.dto.TripRateResponse;
import org.vladproj.zhddrop.dto.TripSearchResponse;
import org.vladproj.zhddrop.entity.Trip;
import org.vladproj.zhddrop.entity.TripRate;

@Mapper(componentModel = "spring")
public interface TripMapper {
    TripSearchResponse toResponse(Trip trip);
    TripRateResponse toTripRateResponse(TripRate tripRate);
}
