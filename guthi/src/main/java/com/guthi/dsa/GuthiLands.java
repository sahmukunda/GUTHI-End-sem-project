package com.guthi.dsa;

import java.util.List;

/** Plots the Guthi actually holds. A rent request must pick one of these names. */
public final class GuthiLands {

    public static final List<String> NAMES =
            List.of(
                    "Patan North Plot #12",
                    "Bhaktapur East Field",
                    "Kirtipur Guthi Khet",
                    "Bungamati South Terrace",
                    "Khokana West Field",
                    "Chapagaun Orchard Plot",
                    "Lubhu Rice Plot",
                    "Thecho North Parcel",
                    "Sunakothi Community Field",
                    "Harisiddhi East Khet");

    private GuthiLands() {}

    /** Official spelling, or null when {@code raw} is not one of the Guthi plots. */
    public static String canonical(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        for (String name : NAMES) {
            if (name.equalsIgnoreCase(trimmed)) {
                return name;
            }
        }
        return null;
    }
}
