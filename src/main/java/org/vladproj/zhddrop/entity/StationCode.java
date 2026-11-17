package org.vladproj.zhddrop.entity;

import org.vladproj.zhddrop.exception.BadRequestException;

public enum StationCode {
    MSK("Moscow"),
    SPB("Saint-Petersburg"),
    NSK("Novosibirsk");

    private final String cityName;

    StationCode(String cityName) {
        this.cityName = cityName;
    }

    public String getCityName() {
        return cityName;
    }

    public static StationCode parse(String value) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException("INVALID_STATION", "Station must not be blank");
        }
        String normalized = value.trim();
        for (StationCode code : values()) {
            if (code.name().equalsIgnoreCase(normalized) || code.cityName.equalsIgnoreCase(normalized)) {
                return code;
            }
        }
        throw new BadRequestException(
            "UNKNOWN_STATION",
            "Unknown station: '%s'. Supported codes: MSK, SPB, NSK".formatted(normalized));
    }
}
