package com.guthi.dsa;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Land dates for one plot. Approved rentals block the calendar. Pending requests are then chosen
 * with greedy interval scheduling: sort by finish date and take a request only when it does not
 * overlap a rental already on the calendar.
 */
public final class LandOccupancyCalendar {

    private LandOccupancyCalendar() {}

    public record Span(String requestId, LocalDate from, LocalDate to) {}

    public static boolean overlaps(LocalDate aFrom, LocalDate aTo, LocalDate bFrom, LocalDate bTo) {
        if (aFrom == null || aTo == null || bFrom == null || bTo == null) {
            return false;
        }
        return !aTo.isBefore(bFrom) && !bTo.isBefore(aFrom);
    }

    /** First approved span that overlaps {@code candidate}, or null when the dates are free. */
    public static Span approvedConflict(Span candidate, List<Span> approved) {
        if (candidate == null || candidate.from == null || candidate.to == null) {
            return null;
        }
        List<Span> sorted = new ArrayList<>();
        for (Span span : approved) {
            if (span != null && span.from != null && span.to != null) {
                sorted.add(span);
            }
        }
        sorted.sort(Comparator.comparing(Span::from).thenComparing(Span::to));
        for (Span span : sorted) {
            if (overlaps(candidate.from, candidate.to, span.from, span.to)) {
                return span;
            }
        }
        return null;
    }

    /**
     * Pending spans that fit beside approved rentals and beside each other. Earlier finish dates
     * are taken first.
     */
    public static Set<String> selectFitting(List<Span> pending, List<Span> approved) {
        List<Span> sorted = new ArrayList<>();
        for (Span span : pending) {
            if (span != null && span.requestId != null && span.from != null && span.to != null) {
                sorted.add(span);
            }
        }
        sorted.sort(Comparator.comparing(Span::to).thenComparing(Span::from).thenComparing(Span::requestId));
        List<Span> taken = new ArrayList<>();
        for (Span span : approved) {
            if (span != null && span.from != null && span.to != null) {
                taken.add(span);
            }
        }
        Set<String> ids = new LinkedHashSet<>();
        for (Span next : sorted) {
            boolean clash = false;
            for (Span block : taken) {
                if (overlaps(next.from, next.to, block.from, block.to)) {
                    clash = true;
                    break;
                }
            }
            if (!clash) {
                taken.add(next);
                ids.add(next.requestId);
            }
        }
        return ids;
    }
}
