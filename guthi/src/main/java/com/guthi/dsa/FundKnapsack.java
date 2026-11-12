package com.guthi.dsa;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 0/1 knapsack. Each pending fund request is an item: weight is the rupees it needs, value is its
 * score. Capacity is the donation pool. The result is the subset with the best total score that
 * still fits in the pool.
 */
public final class FundKnapsack {

    /** Keeps the DP table small enough for a large pool by scaling rupees down together. */
    static final long MAX_CAPACITY_UNITS = 200_000L;

    private FundKnapsack() {}

    public record Item(String id, long weight, long value) {}

    public record Plan(Set<String> chosenIds, long spent, long score) {}

    public static Plan solve(List<Item> items, long capacity) {
        List<Item> usable = new ArrayList<>();
        if (items != null) {
            for (Item item : items) {
                if (item != null && item.id != null && item.weight > 0 && item.value > 0) {
                    usable.add(item);
                }
            }
        }
        if (capacity < 0) {
            capacity = 0;
        }
        long scale = 1;
        if (capacity > MAX_CAPACITY_UNITS) {
            scale = (capacity + MAX_CAPACITY_UNITS - 1) / MAX_CAPACITY_UNITS;
        }
        int cap = (int) (capacity / scale);
        int n = usable.size();
        if (n == 0 || cap == 0) {
            return new Plan(Set.of(), 0, 0);
        }
        int[] weight = new int[n];
        long[] value = new long[n];
        for (int i = 0; i < n; i++) {
            long raw = usable.get(i).weight;
            int scaled = (int) (raw / scale);
            if (scaled <= 0) {
                scaled = 1;
            }
            weight[i] = scaled;
            value[i] = usable.get(i).value;
        }
        long[][] best = new long[n + 1][cap + 1];
        for (int i = 1; i <= n; i++) {
            int itemWeight = weight[i - 1];
            long itemValue = value[i - 1];
            for (int c = 0; c <= cap; c++) {
                best[i][c] = best[i - 1][c];
                if (itemWeight <= c) {
                    long withItem = best[i - 1][c - itemWeight] + itemValue;
                    if (withItem > best[i][c]) {
                        best[i][c] = withItem;
                    }
                }
            }
        }
        Set<String> chosen = new LinkedHashSet<>();
        long spent = 0;
        int remaining = cap;
        for (int i = n; i >= 1; i--) {
            if (best[i][remaining] != best[i - 1][remaining]) {
                chosen.add(usable.get(i - 1).id);
                spent += usable.get(i - 1).weight;
                remaining -= weight[i - 1];
            }
        }
        return new Plan(chosen, spent, best[n][cap]);
    }
}
