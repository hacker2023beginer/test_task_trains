package org.vladproj.zhddrop.entity;

public enum StationCode {
    MSK("Москва"),
    SPB("Санкт-Петербург"),
    NSK("Новосибирск");

    private final String cityName;

    StationCode(String cityName) {
        this.cityName = cityName;
    }

    public String getCityName() {
        return cityName;
    }

    public static StationCode fromString(String code) {
        for (StationCode station : StationCode.values()) {
            if (station.name().equalsIgnoreCase(code)) {
                return station;
            }
        }
        throw new IllegalArgumentException("Неизвестный код станции: " + code);
    }
}
