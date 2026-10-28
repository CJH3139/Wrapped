package com.wrapped.logs;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record LogFileName(LocalDate date, int index, boolean latest) {
    private static final Pattern ROTATED = Pattern.compile("^(\\d{4})-(\\d{2})-(\\d{2})-(\\d+)\\.log(?:\\.gz)?$");

    public static Optional<LogFileName> parse(String fileName) {
        return parse(fileName, null);
    }

    public static Optional<LogFileName> parse(String fileName, LocalDate latestDate) {
        if (fileName == null) {
            return Optional.empty();
        }
        if (fileName.equals("latest.log")) {
            if (latestDate == null) {
                return Optional.empty();
            }
            return Optional.of(new LogFileName(latestDate, 0, true));
        }
        Matcher m = ROTATED.matcher(fileName);
        if (!m.matches()) {
            return Optional.empty();
        }
        try {
            LocalDate date = LocalDate.of(Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)), Integer.parseInt(m.group(3)));
            return Optional.of(new LogFileName(date, Integer.parseInt(m.group(4)), false));
        } catch (DateTimeException | NumberFormatException e) {
            return Optional.empty();
        }
    }
}
