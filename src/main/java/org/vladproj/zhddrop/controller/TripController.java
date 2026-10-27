package org.vladproj.zhddrop.controller;

import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.vladproj.zhddrop.dto.TripSearchResponse;
import org.vladproj.zhddrop.entity.Station;
import org.vladproj.zhddrop.entity.StationCode;
import org.vladproj.zhddrop.service.TripService;

import java.time.LocalDate;
import java.util.List;

@RestController
@AllArgsConstructor
public class TripController {
    private TripService tripService;

    @GetMapping("/api/trips/search")
    public ResponseEntity<List<TripSearchResponse>> searchAvailableTrips(@RequestParam("from") String from,
                                                                   @RequestParam("to") String to,
                                                                   @RequestParam("date") LocalDate date,
                                                                   @RequestParam("passengers") Integer passengers) {
        Station stationFrom = new Station(from, StationCode.fromString(from).getCityName());
        Station stationTo= new Station(to, StationCode.fromString(to).getCityName());
        return ResponseEntity
            .status(HttpStatus.OK)
            .body(tripService.searchAvailableTrips(stationFrom, stationTo, date, passengers));
    }
}
