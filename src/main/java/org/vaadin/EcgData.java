package org.vaadin;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * A set of ECG data coming from Polar H10 heart rate monitor.
 * @param ts timestamp of the first sample
 * @param samples microvolt samples, 130Hz
 */
public record EcgData(LocalDateTime ts, int[] samples) {
    public List<Integer> samplesAsList() {
        return Arrays.stream(samples).boxed().toList();
    }
}
