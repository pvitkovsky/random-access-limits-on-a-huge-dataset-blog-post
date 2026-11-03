package com.example.dbtest.util;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class BenchmarkUtil {
    public static void printStats(String testName, List<Double> durations) {
        if (durations == null || durations.isEmpty()) {
            System.out.printf(Locale.US, "%s: 0.0%n", testName);
            return;
        }
        Collections.sort(durations);
        double median;
        int size = durations.size();
        if (size % 2 == 1) {
            median = durations.get(size / 2);
        } else {
            median = (durations.get(size / 2 - 1) + durations.get(size / 2)) / 2.0;
        }
        System.out.printf(Locale.US, "%s: %.1f%n", testName, median);
    }
}
