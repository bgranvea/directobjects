package com.granveaud.directobjects;

import com.codahale.metrics.Histogram;
import com.codahale.metrics.Snapshot;

public class MetricsUtils {

    public static void displayHistoResults(String name, Histogram histo, String unit) {
        Snapshot snapshot = histo.getSnapshot();

        System.out.printf(
                "Results for %s (times in %s): count=%d min=%d max=%d mean=%f median=%f 75p=%f 95p=%f 98p=%f 99p=%f\n",
                name,
                unit,
                histo.getCount(),
                snapshot.getMin(),
                snapshot.getMax(),
                snapshot.getMean(),
                snapshot.getMedian(),
                snapshot.get75thPercentile(),
                snapshot.get95thPercentile(),
                snapshot.get98thPercentile(),
                snapshot.get99thPercentile()
        );
    }
}
