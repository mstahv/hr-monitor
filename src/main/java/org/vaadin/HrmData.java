package org.vaadin;

/**
 * A Batch of heart rate monitor data.
 * @param heartRate current heart rate (calculated by the monitor itself)
 * @param rrIntervals latest R-R intervals as milliseconds
 */
public record HrmData(int heartRate, int[] rrIntervals) {
}
