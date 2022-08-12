package org.vaadin;

import java.util.Arrays;

public class HeartRateVariabilityAnalysis {
    private static final int MAX_DATA_POINTS = 200;
    private int i = 0;
    /*
     * Overflowing sample array for last samples
     */
    private RRInterval[] samples = new RRInterval[MAX_DATA_POINTS];

    public RRInterval logRRInterval(int duration) {
        Double v = null;
        Double relToAvg = null;
        Integer diff = null;
        Double rms10 = null;
        Double rms100 = null;
        if(i != 0) {
            diff = duration - samples[(i-1)%MAX_DATA_POINTS].duration();
        }
        if(i > 5) {
            // calculate variance among last 5 samles
            int[] last5samples = new int[5];
            last5samples[4] = duration;
            for(int j = 0; j < 4; j++) {
                last5samples[j] = samples[(i - j -1)%MAX_DATA_POINTS].duration();
            }

            // The mean average
            double mean = Arrays.stream(last5samples).average().getAsDouble();
            relToAvg = 100*duration / mean;

            // variance
            v = 0.0;
            for (int j = 0; j < last5samples.length; j++) {
                v += Math.pow(last5samples[j] - mean, 2);
            }
            v /= last5samples.length;

            if (i > 10) {
                // Heart rate variations calculation (RMSSD)
                last5samples = new int[10];
                last5samples[9] = (int) duration;
                for(int j = 0; j < 9; j++) {
                    int d = samples[(i - j -1)%MAX_DATA_POINTS].diff();
                    last5samples[j] = d*d;
                }
                rms10 = Math.sqrt(Arrays.stream(last5samples).average().getAsDouble());
                if( i%100 == 1) {
                    last5samples = new int[100];
                    last5samples[99] = (int) duration;
                    for(int j = 0; j < 99; j++) {
                        int d = samples[(i - j -1)%MAX_DATA_POINTS].diff();
                        last5samples[j] = d*d;
                    }
                    rms100 = Math.sqrt(Arrays.stream(last5samples).average().getAsDouble());
                }
            }

        }
        RRInterval rrInterval = new RRInterval(i, (int) duration, diff, v, relToAvg, rms10, rms100);
        samples[i++] = rrInterval;
        return rrInterval;
    }

    public static record RRInterval(int index, int duration, Integer diff, Double variance, Double relToAvg, Double rms10, Double rms100) {
        public Double std() {
            return variance == null ? null : Math.sqrt(variance);
        }
    }
}
