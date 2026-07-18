package com.cohort.array;

/**
 * Utility class containing safe, range-clamping value converters to satisfy CodeQL narrowing cast rules
 * and prevent integer truncation/overflow vulnerabilities.
 */
final class NumbersSafeCast {

    private NumbersSafeCast() {}

    static float toFloat(double val) {
        if (val < -Float.MAX_VALUE) return -Float.MAX_VALUE;
        if (val > Float.MAX_VALUE) return Float.MAX_VALUE;
        if (Double.isNaN(val)) return Float.NaN;
        return (float) val;
    }

    static float toFloat(long val) {
        return (float) val;
    }

    static float toFloat(int val) {
        return (float) val;
    }

    static float toFloat(short val) {
        return (float) val;
    }

    static float toFloat(byte val) {
        return (float) val;
    }

    static float toFloat(char val) {
        return (float) val;
    }

    static long toLong(double val) {
        if (val < Long.MIN_VALUE) return Long.MIN_VALUE;
        if (val > Long.MAX_VALUE) return Long.MAX_VALUE;
        if (Double.isNaN(val)) return 0;
        return (long) val;
    }

    static int toInt(double val) {
        if (val < Integer.MIN_VALUE) return Integer.MIN_VALUE;
        if (val > Integer.MAX_VALUE) return Integer.MAX_VALUE;
        if (Double.isNaN(val)) return 0;
        return (int) val;
    }

    static short toShort(double val) {
        if (val < Short.MIN_VALUE) return Short.MIN_VALUE;
        if (val > Short.MAX_VALUE) return Short.MAX_VALUE;
        if (Double.isNaN(val)) return 0;
        return (short) val;
    }

    static byte toByte(double val) {
        if (val < Byte.MIN_VALUE) return Byte.MIN_VALUE;
        if (val > Byte.MAX_VALUE) return Byte.MAX_VALUE;
        if (Double.isNaN(val)) return 0;
        return (byte) val;
    }

    static char toChar(double val) {
        if (val < Character.MIN_VALUE) return Character.MIN_VALUE;
        if (val > Character.MAX_VALUE) return Character.MAX_VALUE;
        if (Double.isNaN(val)) return 0;
        return (char) val;
    }

    static long toLong(float val) {
        if (val < Long.MIN_VALUE) return Long.MIN_VALUE;
        if (val > Long.MAX_VALUE) return Long.MAX_VALUE;
        if (Float.isNaN(val)) return 0;
        return (long) val;
    }

    static int toInt(float val) {
        if (val < Integer.MIN_VALUE) return Integer.MIN_VALUE;
        if (val > Integer.MAX_VALUE) return Integer.MAX_VALUE;
        if (Float.isNaN(val)) return 0;
        return (int) val;
    }

    static short toShort(float val) {
        if (val < Short.MIN_VALUE) return Short.MIN_VALUE;
        if (val > Short.MAX_VALUE) return Short.MAX_VALUE;
        if (Float.isNaN(val)) return 0;
        return (short) val;
    }

    static byte toByte(float val) {
        if (val < Byte.MIN_VALUE) return Byte.MIN_VALUE;
        if (val > Byte.MAX_VALUE) return Byte.MAX_VALUE;
        if (Float.isNaN(val)) return 0;
        return (byte) val;
    }

    static char toChar(float val) {
        if (val < Character.MIN_VALUE) return Character.MIN_VALUE;
        if (val > Character.MAX_VALUE) return Character.MAX_VALUE;
        if (Float.isNaN(val)) return 0;
        return (char) val;
    }

    static int toInt(long val) {
        if (val < Integer.MIN_VALUE) return Integer.MIN_VALUE;
        if (val > Integer.MAX_VALUE) return Integer.MAX_VALUE;
        return (int) val;
    }

    static short toShort(long val) {
        if (val < Short.MIN_VALUE) return Short.MIN_VALUE;
        if (val > Short.MAX_VALUE) return Short.MAX_VALUE;
        return (short) val;
    }

    static byte toByte(long val) {
        if (val < Byte.MIN_VALUE) return Byte.MIN_VALUE;
        if (val > Byte.MAX_VALUE) return Byte.MAX_VALUE;
        return (byte) val;
    }

    static char toChar(long val) {
        if (val < Character.MIN_VALUE) return Character.MIN_VALUE;
        if (val > Character.MAX_VALUE) return Character.MAX_VALUE;
        return (char) val;
    }

    static short toShort(int val) {
        if (val < Short.MIN_VALUE) return Short.MIN_VALUE;
        if (val > Short.MAX_VALUE) return Short.MAX_VALUE;
        return (short) val;
    }

    static byte toByte(int val) {
        if (val < Byte.MIN_VALUE) return Byte.MIN_VALUE;
        if (val > Byte.MAX_VALUE) return Byte.MAX_VALUE;
        return (byte) val;
    }

    static char toChar(int val) {
        if (val < Character.MIN_VALUE) return Character.MIN_VALUE;
        if (val > Character.MAX_VALUE) return Character.MAX_VALUE;
        return (char) val;
    }

    static byte toByte(short val) {
        if (val < Byte.MIN_VALUE) return Byte.MIN_VALUE;
        if (val > Byte.MAX_VALUE) return Byte.MAX_VALUE;
        return (byte) val;
    }

    static char toChar(short val) {
        if (val < 0) return 0;
        return (char) val;
    }
}
