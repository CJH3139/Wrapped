package com.wrapped.stats;

import java.time.Duration;
import java.time.LocalDate;

public record ServerStat(String host, Duration time, int joins, int activeDays, LocalDate firstVisit) {
}
