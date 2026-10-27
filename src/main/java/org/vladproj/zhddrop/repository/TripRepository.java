package org.vladproj.zhddrop.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.vladproj.zhddrop.entity.Trip;

public interface TripRepository extends JpaRepository<Trip, Integer> {
}
