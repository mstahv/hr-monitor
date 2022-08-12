package org.vaadin;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * A helper class to parse raw bytes coming from
 * a Polar H10 heart rate belt.
 *
 * If you need more complete Polar H10 helper library,
 * check Polar Android SDK, can probably be easily
 * compiled for "real Java". https://github.com/polarofficial/polar-ble-sdk
 */
public class PolarH10Tooling {

    /**
     * Parses the somewhat standard data coming from
     * Bluetooth Low Energy heart rate belts. Works
     * for at least Polar H10 and Suunto belts.
     *
     * @param bytes the raw data coming from the devices
     * @return the parsed HRM data
     */
    public static HrmData parseHeartRateBeltData(byte[] bytes) {
        int flags = bytes[0];
        boolean rate16Bits = (flags & 0x1) != 0;

        int index = 1;
        int heartRate;
        if (rate16Bits) {
            heartRate = Byte.toUnsignedInt(bytes[index++]) + Byte.toUnsignedInt(bytes[index++]) * 256;
        } else {
            heartRate = bytes[index++];
        }

        boolean rrIntervalPresent = (flags & 0x10) != 0;
        if (rrIntervalPresent) {
            int rrIndex = 0;
            int[] rrIntervals = new int[(bytes.length - index) / 2];
            while (index < bytes.length) {
                // 16 bits for one millisecond value, big endian
                rrIntervals[rrIndex++] = Byte.toUnsignedInt(bytes[index++]) + Byte.toUnsignedInt(bytes[index++]) * 256;
            }
            return new HrmData(heartRate, rrIntervals);
        }
        return new HrmData(heartRate, null);
    }

    /**
     * Reads raw ECG samples coming from H10 belt into Java record
     *
     * @param bytes
     * @return
     */
    public static EcgData parseEcgData(byte[] bytes) {
        // HEX: 00 38 6C 31 72 A4 D3 23 0D 03 FF
        // index    type                                data
        // 0:      Measurement type                     00 (Ecg data)
        // 1..8:   64-bit Timestamp                     38 6C 31 72 A4 D3 23 0D (0x0D23D3A472316C38 = 946833049921875000)
        // 9:      Frame type                           03 (raw, frame type 3)
        // 10:     Data                                 FF
        LocalDateTime ts = LocalDateTime.ofEpochSecond(PolarH10Tooling.convertArrayToUnsignedLong(bytes, 1, 8)/1000, 0, ZoneOffset.UTC);
        final int DATAOFFSET = 10;
        final int MEASUREMENTLENGHT = 3;
        int offset = DATAOFFSET;
        int[] samples = new int[(bytes.length - DATAOFFSET)/MEASUREMENTLENGHT];
        while(offset < bytes.length) {
            int microvolt = PolarH10Tooling.convertArrayToSignedInt(bytes, offset, MEASUREMENTLENGHT);
            samples[(offset-DATAOFFSET)/MEASUREMENTLENGHT] = microvolt;
            offset += MEASUREMENTLENGHT;
        }
        return new EcgData(ts, samples);
    }

    // Methods copied from Polar BleUtils (Java/Kotlin Android library)
    // to pick up various (big endian) numbers from the raw byte array
    // coming from the heart rate belt.

    public static int convertArrayToSignedInt(byte[] data, int offset, int length) {
        int result = (int) convertArrayToUnsignedLong(data, offset, length);
        if ((data[offset + length - 1] & 0x80) != 0) {
            int mask = 0xFFFFFFFF << length * 8;
            result |= mask;
        }
        return result;
    }

    public static long convertArrayToUnsignedLong(byte[] data, int offset, int length) {
        long result = 0;
        for (int i = 0; i < length; ++i) {
            result |= (((long) data[i + offset] & 0xFFL) << i * 8);
        }
        return result;
    }
}
