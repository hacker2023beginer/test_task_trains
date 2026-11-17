package org.vladproj.zhddrop.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.vladproj.zhddrop.dto.ReservationRequest;
import org.vladproj.zhddrop.dto.ReservationResponse;
import org.vladproj.zhddrop.dto.TripSearchResponse;
import org.vladproj.zhddrop.entity.StationCode;
import org.vladproj.zhddrop.service.TripService;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api")
@Validated
@RequiredArgsConstructor
public class TripController {

    private final TripService tripService;

    @GetMapping("/trips/search")
    public List<TripSearchResponse> searchAvailableTrips(
            @RequestParam("from") @NotBlank String from,
            @RequestParam("to") @NotBlank String to,
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(value = "passengers", defaultValue = "1") @Min(1) @Max(10) Integer passengers) {

        return tripService.searchAvailableTrips(
            StationCode.parse(from),
            StationCode.parse(to),
            date,
            passengers);
    }

    @PostMapping("/reservations")
    public ResponseEntity<ReservationResponse> reserveTrip(@Valid @RequestBody ReservationRequest request) {
        ReservationResponse response = tripService.reserveTrip(request);
        return ResponseEntity
            .created(URI.create("/api/reservations/" + response.getId()))
            .body(response);
    }

    @GetMapping("/reservations/{id}")
    public ReservationResponse getReservationById(@PathVariable("id") Integer id) {
        return tripService.getReservationById(id);
    }

    @PutMapping("/reservations/{id}/confirm")
    public ResponseEntity<Void> confirmReservation(@PathVariable("id") Integer id) {
        tripService.confirmReservation(id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/reservations/{id}/cancel")
    public ResponseEntity<Void> cancelReservation(@PathVariable("id") Integer id) {
        tripService.cancelReservation(id);
        return ResponseEntity.ok().build();
    }
}
