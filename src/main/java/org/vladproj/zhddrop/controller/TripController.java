package org.vladproj.zhddrop.controller;

import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.vladproj.zhddrop.dto.ReservationRequest;
import org.vladproj.zhddrop.dto.ReservationResponse;
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

    @PostMapping("/api/reservations")
    public ResponseEntity<ReservationResponse> reserveTrip(@RequestBody ReservationRequest request){
        ReservationResponse response = tripService.reserveTrip(request.getId(), request.getRateCode(), request.getPassengers());
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(response);
    }

    @GetMapping("/api/reservations/{id}")
    public ResponseEntity<ReservationResponse> getReservationById(@PathVariable Integer id){
        return ResponseEntity.ok(tripService.getReservationById(id));
    }

    @PutMapping("/api/reservations/{id}/confirm")
    public ResponseEntity<Void> confirmReservation(@PathVariable Integer id) {
        tripService.confirmReservation(id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/api/reservations/{id}/cancel")
    public ResponseEntity<Void> cancelReservation(@PathVariable Integer id) {
        tripService.cancelReservation(id);
        return ResponseEntity.ok().build();
    }
}
