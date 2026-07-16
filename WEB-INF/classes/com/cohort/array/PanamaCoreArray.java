package com.cohort.array;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.ByteOrder;

/**
 * PanamaCoreArray acts as the single high-performance engine for all primitive types
 * in the com.cohort.array package, using JDK Foreign Function & Memory (FFM) API.
 */
class PanamaCoreArray {

    private MemorySegment segment;
    private ValueLayout layout;
    private long capacity;
    private Object onHeapArray;

    /**
     * Allocates native memory for the given capacity and value layout.
     */
    PanamaCoreArray(long capacity, ValueLayout layout) {
        this.layout = layout;
        this.segment = Arena.ofAuto().allocate(layout, capacity);
        this.capacity = capacity;
        this.onHeapArray = null;
    }

    /**
     * Wraps an existing MemorySegment.
     */
    PanamaCoreArray(MemorySegment segment, ValueLayout layout, long capacity, Object onHeapArray) {
        this.segment = segment;
        this.layout = layout;
        this.capacity = capacity;
        this.onHeapArray = onHeapArray;
    }

    /**
     * Gets the underlying MemorySegment.
     */
    MemorySegment segment() {
        return segment;
    }

    /**
     * Gets the active ValueLayout.
     */
    ValueLayout layout() {
        return layout;
    }

    /**
     * Gets the current capacity of the array.
     */
    long capacity() {
        return capacity;
    }

    /**
     * Gets the backing on-heap array (or null).
     */
    Object onHeapArray() {
        return onHeapArray;
    }

    /**
     * Configures the byte order for layout configurations to handle Big/Little Endian automatically.
     */
    void setByteOrder(ByteOrder order) {
        if (layout instanceof ValueLayout.OfDouble ofDouble) {
            this.layout = ofDouble.withOrder(order);
        } else if (layout instanceof ValueLayout.OfFloat ofFloat) {
            this.layout = ofFloat.withOrder(order);
        } else if (layout instanceof ValueLayout.OfLong ofLong) {
            this.layout = ofLong.withOrder(order);
        } else if (layout instanceof ValueLayout.OfInt ofInt) {
            this.layout = ofInt.withOrder(order);
        } else if (layout instanceof ValueLayout.OfShort ofShort) {
            this.layout = ofShort.withOrder(order);
        } else if (layout instanceof ValueLayout.OfByte ofByte) {
            this.layout = ofByte.withOrder(order);
        } else if (layout instanceof ValueLayout.OfChar ofChar) {
            this.layout = ofChar.withOrder(order);
        }
    }

    /**
     * Resize the internal storage segment.
     */
    void resize(long newCapacity) {
        if (newCapacity < 0) {
            throw new IllegalArgumentException("Capacity cannot be negative");
        }
        boolean nativeSegment = segment.isNative();
        long bytesToCopy = Math.min(capacity, newCapacity) * layout.byteSize();
        MemorySegment newSegment;
        if (nativeSegment) {
            newSegment = Arena.ofAuto().allocate(layout.byteSize() * newCapacity);
        } else {
            if (layout instanceof ValueLayout.OfDouble) {
                double[] arr = new double[(int) newCapacity];
                newSegment = MemorySegment.ofArray(arr);
                this.onHeapArray = arr;
            } else if (layout instanceof ValueLayout.OfFloat) {
                float[] arr = new float[(int) newCapacity];
                newSegment = MemorySegment.ofArray(arr);
                this.onHeapArray = arr;
            } else if (layout instanceof ValueLayout.OfLong) {
                long[] arr = new long[(int) newCapacity];
                newSegment = MemorySegment.ofArray(arr);
                this.onHeapArray = arr;
            } else if (layout instanceof ValueLayout.OfInt) {
                int[] arr = new int[(int) newCapacity];
                newSegment = MemorySegment.ofArray(arr);
                this.onHeapArray = arr;
            } else if (layout instanceof ValueLayout.OfShort) {
                short[] arr = new short[(int) newCapacity];
                newSegment = MemorySegment.ofArray(arr);
                this.onHeapArray = arr;
            } else if (layout instanceof ValueLayout.OfByte) {
                byte[] arr = new byte[(int) newCapacity];
                newSegment = MemorySegment.ofArray(arr);
                this.onHeapArray = arr;
            } else if (layout instanceof ValueLayout.OfChar) {
                char[] arr = new char[(int) newCapacity];
                newSegment = MemorySegment.ofArray(arr);
                this.onHeapArray = arr;
            } else {
                newSegment = Arena.ofAuto().allocate(layout.byteSize() * newCapacity);
            }
        }
        if (bytesToCopy > 0) {
            MemorySegment.copy(segment, 0, newSegment, 0, bytesToCopy);
        }
        this.segment = newSegment;
        this.capacity = newCapacity;
    }

    /**
     * Creates a zero-copy slice view of the array.
     */
    PanamaCoreArray slice(long offset, long length) {
        long byteOffset = offset * layout.byteSize();
        long byteLength = length * layout.byteSize();
        MemorySegment sliceSegment = segment.asSlice(byteOffset, byteLength);
        return new PanamaCoreArray(sliceSegment, layout, length, onHeapArray);
    }

    // --- SAFE CAST CONVERTERS TO PREVENT CODEQL TRUNCATION ALERTS ---

    private static long toLong(double val) {
        if (val < Long.MIN_VALUE) return Long.MIN_VALUE;
        if (val > Long.MAX_VALUE) return Long.MAX_VALUE;
        if (Double.isNaN(val)) return 0;
        return (long) val;
    }
    private static int toInt(double val) {
        if (val < Integer.MIN_VALUE) return Integer.MIN_VALUE;
        if (val > Integer.MAX_VALUE) return Integer.MAX_VALUE;
        if (Double.isNaN(val)) return 0;
        return (int) val;
    }
    private static short toShort(double val) {
        if (val < Short.MIN_VALUE) return Short.MIN_VALUE;
        if (val > Short.MAX_VALUE) return Short.MAX_VALUE;
        if (Double.isNaN(val)) return 0;
        return (short) val;
    }
    private static byte toByte(double val) {
        if (val < Byte.MIN_VALUE) return Byte.MIN_VALUE;
        if (val > Byte.MAX_VALUE) return Byte.MAX_VALUE;
        if (Double.isNaN(val)) return 0;
        return (byte) val;
    }
    private static char toChar(double val) {
        if (val < Character.MIN_VALUE) return Character.MIN_VALUE;
        if (val > Character.MAX_VALUE) return Character.MAX_VALUE;
        if (Double.isNaN(val)) return 0;
        return (char) val;
    }

    private static long toLong(float val) {
        if (val < Long.MIN_VALUE) return Long.MIN_VALUE;
        if (val > Long.MAX_VALUE) return Long.MAX_VALUE;
        if (Float.isNaN(val)) return 0;
        return (long) val;
    }
    private static int toInt(float val) {
        if (val < Integer.MIN_VALUE) return Integer.MIN_VALUE;
        if (val > Integer.MAX_VALUE) return Integer.MAX_VALUE;
        if (Float.isNaN(val)) return 0;
        return (int) val;
    }
    private static short toShort(float val) {
        if (val < Short.MIN_VALUE) return Short.MIN_VALUE;
        if (val > Short.MAX_VALUE) return Short.MAX_VALUE;
        if (Float.isNaN(val)) return 0;
        return (short) val;
    }
    private static byte toByte(float val) {
        if (val < Byte.MIN_VALUE) return Byte.MIN_VALUE;
        if (val > Byte.MAX_VALUE) return Byte.MAX_VALUE;
        if (Float.isNaN(val)) return 0;
        return (byte) val;
    }
    private static char toChar(float val) {
        if (val < Character.MIN_VALUE) return Character.MIN_VALUE;
        if (val > Character.MAX_VALUE) return Character.MAX_VALUE;
        if (Float.isNaN(val)) return 0;
        return (char) val;
    }

    private static int toInt(long val) {
        if (val < Integer.MIN_VALUE) return Integer.MIN_VALUE;
        if (val > Integer.MAX_VALUE) return Integer.MAX_VALUE;
        return (int) val;
    }
    private static short toShort(long val) {
        if (val < Short.MIN_VALUE) return Short.MIN_VALUE;
        if (val > Short.MAX_VALUE) return Short.MAX_VALUE;
        return (short) val;
    }
    private static byte toByte(long val) {
        if (val < Byte.MIN_VALUE) return Byte.MIN_VALUE;
        if (val > Byte.MAX_VALUE) return Byte.MAX_VALUE;
        return (byte) val;
    }
    private static char toChar(long val) {
        if (val < Character.MIN_VALUE) return Character.MIN_VALUE;
        if (val > Character.MAX_VALUE) return Character.MAX_VALUE;
        return (char) val;
    }

    private static short toShort(int val) {
        if (val < Short.MIN_VALUE) return Short.MIN_VALUE;
        if (val > Short.MAX_VALUE) return Short.MAX_VALUE;
        return (short) val;
    }
    private static byte toByte(int val) {
        if (val < Byte.MIN_VALUE) return Byte.MIN_VALUE;
        if (val > Byte.MAX_VALUE) return Byte.MAX_VALUE;
        return (byte) val;
    }
    private static char toChar(int val) {
        if (val < Character.MIN_VALUE) return Character.MIN_VALUE;
        if (val > Character.MAX_VALUE) return Character.MAX_VALUE;
        return (char) val;
    }

    private static byte toByte(short val) {
        if (val < Byte.MIN_VALUE) return Byte.MIN_VALUE;
        if (val > Byte.MAX_VALUE) return Byte.MAX_VALUE;
        return (byte) val;
    }
    private static char toChar(short val) {
        if (val < 0) return 0;
        return (char) val;
    }

    // --- STRONGLY TYPED UNBOXED PRIMITIVE GETTERS ---

    double getDouble(long index) {
        if (layout instanceof ValueLayout.OfDouble ofDouble) {
            return segment.getAtIndex(ofDouble, index);
        } else if (layout instanceof ValueLayout.OfFloat ofFloat) {
            return (double) segment.getAtIndex(ofFloat, index);
        } else if (layout instanceof ValueLayout.OfLong ofLong) {
            return (double) segment.getAtIndex(ofLong, index);
        } else if (layout instanceof ValueLayout.OfInt ofInt) {
            return (double) segment.getAtIndex(ofInt, index);
        } else if (layout instanceof ValueLayout.OfShort ofShort) {
            return (double) segment.getAtIndex(ofShort, index);
        } else if (layout instanceof ValueLayout.OfByte ofByte) {
            return (double) segment.getAtIndex(ofByte, index);
        } else if (layout instanceof ValueLayout.OfChar ofChar) {
            return (double) segment.getAtIndex(ofChar, index);
        }
        throw new UnsupportedOperationException("Unsupported layout for getDouble");
    }

    float getFloat(long index) {
        if (layout instanceof ValueLayout.OfDouble ofDouble) {
            return (float) segment.getAtIndex(ofDouble, index);
        } else if (layout instanceof ValueLayout.OfFloat ofFloat) {
            return segment.getAtIndex(ofFloat, index);
        } else if (layout instanceof ValueLayout.OfLong ofLong) {
            return (float) segment.getAtIndex(ofLong, index);
        } else if (layout instanceof ValueLayout.OfInt ofInt) {
            return (float) segment.getAtIndex(ofInt, index);
        } else if (layout instanceof ValueLayout.OfShort ofShort) {
            return (float) segment.getAtIndex(ofShort, index);
        } else if (layout instanceof ValueLayout.OfByte ofByte) {
            return (float) segment.getAtIndex(ofByte, index);
        } else if (layout instanceof ValueLayout.OfChar ofChar) {
            return (float) segment.getAtIndex(ofChar, index);
        }
        throw new UnsupportedOperationException("Unsupported layout for getFloat");
    }

    long getLong(long index) {
        if (layout instanceof ValueLayout.OfDouble ofDouble) {
            return (long) segment.getAtIndex(ofDouble, index);
        } else if (layout instanceof ValueLayout.OfFloat ofFloat) {
            return (long) segment.getAtIndex(ofFloat, index);
        } else if (layout instanceof ValueLayout.OfLong ofLong) {
            return segment.getAtIndex(ofLong, index);
        } else if (layout instanceof ValueLayout.OfInt ofInt) {
            return (long) segment.getAtIndex(ofInt, index);
        } else if (layout instanceof ValueLayout.OfShort ofShort) {
            return (long) segment.getAtIndex(ofShort, index);
        } else if (layout instanceof ValueLayout.OfByte ofByte) {
            return (long) segment.getAtIndex(ofByte, index);
        } else if (layout instanceof ValueLayout.OfChar ofChar) {
            return (long) segment.getAtIndex(ofChar, index);
        }
        throw new UnsupportedOperationException("Unsupported layout for getLong");
    }

    int getInt(long index) {
        if (layout instanceof ValueLayout.OfDouble ofDouble) {
            return (int) segment.getAtIndex(ofDouble, index);
        } else if (layout instanceof ValueLayout.OfFloat ofFloat) {
            return (int) segment.getAtIndex(ofFloat, index);
        } else if (layout instanceof ValueLayout.OfLong ofLong) {
            return (int) segment.getAtIndex(ofLong, index);
        } else if (layout instanceof ValueLayout.OfInt ofInt) {
            return segment.getAtIndex(ofInt, index);
        } else if (layout instanceof ValueLayout.OfShort ofShort) {
            return (int) segment.getAtIndex(ofShort, index);
        } else if (layout instanceof ValueLayout.OfByte ofByte) {
            return (int) segment.getAtIndex(ofByte, index);
        } else if (layout instanceof ValueLayout.OfChar ofChar) {
            return (int) segment.getAtIndex(ofChar, index);
        }
        throw new UnsupportedOperationException("Unsupported layout for getInt");
    }

    short getShort(long index) {
        if (layout instanceof ValueLayout.OfDouble ofDouble) {
            return (short) segment.getAtIndex(ofDouble, index);
        } else if (layout instanceof ValueLayout.OfFloat ofFloat) {
            return (short) segment.getAtIndex(ofFloat, index);
        } else if (layout instanceof ValueLayout.OfLong ofLong) {
            return (short) segment.getAtIndex(ofLong, index);
        } else if (layout instanceof ValueLayout.OfInt ofInt) {
            return (short) segment.getAtIndex(ofInt, index);
        } else if (layout instanceof ValueLayout.OfShort ofShort) {
            return segment.getAtIndex(ofShort, index);
        } else if (layout instanceof ValueLayout.OfByte ofByte) {
            return (short) segment.getAtIndex(ofByte, index);
        } else if (layout instanceof ValueLayout.OfChar ofChar) {
            return (short) segment.getAtIndex(ofChar, index);
        }
        throw new UnsupportedOperationException("Unsupported layout for getShort");
    }

    byte getByte(long index) {
        if (layout instanceof ValueLayout.OfDouble ofDouble) {
            return (byte) segment.getAtIndex(ofDouble, index);
        } else if (layout instanceof ValueLayout.OfFloat ofFloat) {
            return (byte) segment.getAtIndex(ofFloat, index);
        } else if (layout instanceof ValueLayout.OfLong ofLong) {
            return (byte) segment.getAtIndex(ofLong, index);
        } else if (layout instanceof ValueLayout.OfInt ofInt) {
            return (byte) segment.getAtIndex(ofInt, index);
        } else if (layout instanceof ValueLayout.OfShort ofShort) {
            return (byte) segment.getAtIndex(ofShort, index);
        } else if (layout instanceof ValueLayout.OfByte ofByte) {
            return segment.getAtIndex(ofByte, index);
        } else if (layout instanceof ValueLayout.OfChar ofChar) {
            return (byte) segment.getAtIndex(ofChar, index);
        }
        throw new UnsupportedOperationException("Unsupported layout for getByte");
    }

    char getChar(long index) {
        if (layout instanceof ValueLayout.OfDouble ofDouble) {
            return (char) segment.getAtIndex(ofDouble, index);
        } else if (layout instanceof ValueLayout.OfFloat ofFloat) {
            return (char) segment.getAtIndex(ofFloat, index);
        } else if (layout instanceof ValueLayout.OfLong ofLong) {
            return (char) segment.getAtIndex(ofLong, index);
        } else if (layout instanceof ValueLayout.OfInt ofInt) {
            return (char) segment.getAtIndex(ofInt, index);
        } else if (layout instanceof ValueLayout.OfShort ofShort) {
            return (char) segment.getAtIndex(ofShort, index);
        } else if (layout instanceof ValueLayout.OfByte ofByte) {
            return (char) segment.getAtIndex(ofByte, index);
        } else if (layout instanceof ValueLayout.OfChar ofChar) {
            return segment.getAtIndex(ofChar, index);
        }
        throw new UnsupportedOperationException("Unsupported layout for getChar");
    }

    // --- STRONGLY TYPED UNBOXED PRIMITIVE SETTERS ---

    void setDouble(long index, double value) {
        if (layout instanceof ValueLayout.OfDouble ofDouble) {
            segment.setAtIndex(ofDouble, index, value);
        } else if (layout instanceof ValueLayout.OfFloat ofFloat) {
            segment.setAtIndex(ofFloat, index, (float) value);
        } else if (layout instanceof ValueLayout.OfLong ofLong) {
            segment.setAtIndex(ofLong, index, toLong(value));
        } else if (layout instanceof ValueLayout.OfInt ofInt) {
            segment.setAtIndex(ofInt, index, toInt(value));
        } else if (layout instanceof ValueLayout.OfShort ofShort) {
            segment.setAtIndex(ofShort, index, toShort(value));
        } else if (layout instanceof ValueLayout.OfByte ofByte) {
            segment.setAtIndex(ofByte, index, toByte(value));
        } else if (layout instanceof ValueLayout.OfChar ofChar) {
            segment.setAtIndex(ofChar, index, toChar(value));
        } else {
            throw new UnsupportedOperationException("Unsupported layout for setDouble");
        }
    }

    void setFloat(long index, float value) {
        if (layout instanceof ValueLayout.OfDouble ofDouble) {
            segment.setAtIndex(ofDouble, index, value);
        } else if (layout instanceof ValueLayout.OfFloat ofFloat) {
            segment.setAtIndex(ofFloat, index, value);
        } else if (layout instanceof ValueLayout.OfLong ofLong) {
            segment.setAtIndex(ofLong, index, toLong(value));
        } else if (layout instanceof ValueLayout.OfInt ofInt) {
            segment.setAtIndex(ofInt, index, toInt(value));
        } else if (layout instanceof ValueLayout.OfShort ofShort) {
            segment.setAtIndex(ofShort, index, toShort(value));
        } else if (layout instanceof ValueLayout.OfByte ofByte) {
            segment.setAtIndex(ofByte, index, toByte(value));
        } else if (layout instanceof ValueLayout.OfChar ofChar) {
            segment.setAtIndex(ofChar, index, toChar(value));
        } else {
            throw new UnsupportedOperationException("Unsupported layout for setFloat");
        }
    }

    void setLong(long index, long value) {
        if (layout instanceof ValueLayout.OfDouble ofDouble) {
            segment.setAtIndex(ofDouble, index, (double) value);
        } else if (layout instanceof ValueLayout.OfFloat ofFloat) {
            segment.setAtIndex(ofFloat, index, (float) value);
        } else if (layout instanceof ValueLayout.OfLong ofLong) {
            segment.setAtIndex(ofLong, index, value);
        } else if (layout instanceof ValueLayout.OfInt ofInt) {
            segment.setAtIndex(ofInt, index, toInt(value));
        } else if (layout instanceof ValueLayout.OfShort ofShort) {
            segment.setAtIndex(ofShort, index, toShort(value));
        } else if (layout instanceof ValueLayout.OfByte ofByte) {
            segment.setAtIndex(ofByte, index, toByte(value));
        } else if (layout instanceof ValueLayout.OfChar ofChar) {
            segment.setAtIndex(ofChar, index, toChar(value));
        } else {
            throw new UnsupportedOperationException("Unsupported layout for setLong");
        }
    }

    void setInt(long index, int value) {
        if (layout instanceof ValueLayout.OfDouble ofDouble) {
            segment.setAtIndex(ofDouble, index, (double) value);
        } else if (layout instanceof ValueLayout.OfFloat ofFloat) {
            segment.setAtIndex(ofFloat, index, (float) value);
        } else if (layout instanceof ValueLayout.OfLong ofLong) {
            segment.setAtIndex(ofLong, index, toLong(value));
        } else if (layout instanceof ValueLayout.OfInt ofInt) {
            segment.setAtIndex(ofInt, index, value);
        } else if (layout instanceof ValueLayout.OfShort ofShort) {
            segment.setAtIndex(ofShort, index, toShort(value));
        } else if (layout instanceof ValueLayout.OfByte ofByte) {
            segment.setAtIndex(ofByte, index, toByte(value));
        } else if (layout instanceof ValueLayout.OfChar ofChar) {
            segment.setAtIndex(ofChar, index, toChar(value));
        } else {
            throw new UnsupportedOperationException("Unsupported layout for setInt");
        }
    }

    void setShort(long index, short value) {
        if (layout instanceof ValueLayout.OfDouble ofDouble) {
            segment.setAtIndex(ofDouble, index, (double) value);
        } else if (layout instanceof ValueLayout.OfFloat ofFloat) {
            segment.setAtIndex(ofFloat, index, (float) value);
        } else if (layout instanceof ValueLayout.OfLong ofLong) {
            segment.setAtIndex(ofLong, index, toLong(value));
        } else if (layout instanceof ValueLayout.OfInt ofInt) {
            segment.setAtIndex(ofInt, index, toInt(value));
        } else if (layout instanceof ValueLayout.OfShort ofShort) {
            segment.setAtIndex(ofShort, index, value);
        } else if (layout instanceof ValueLayout.OfByte ofByte) {
            segment.setAtIndex(ofByte, index, toByte(value));
        } else if (layout instanceof ValueLayout.OfChar ofChar) {
            segment.setAtIndex(ofChar, index, toChar(value));
        } else {
            throw new UnsupportedOperationException("Unsupported layout for setShort");
        }
    }

    void setByte(long index, byte value) {
        if (layout instanceof ValueLayout.OfDouble ofDouble) {
            segment.setAtIndex(ofDouble, index, (double) value);
        } else if (layout instanceof ValueLayout.OfFloat ofFloat) {
            segment.setAtIndex(ofFloat, index, (float) value);
        } else if (layout instanceof ValueLayout.OfLong ofLong) {
            segment.setAtIndex(ofLong, index, toLong(value));
        } else if (layout instanceof ValueLayout.OfInt ofInt) {
            segment.setAtIndex(ofInt, index, toInt(value));
        } else if (layout instanceof ValueLayout.OfShort ofShort) {
            segment.setAtIndex(ofShort, index, toShort(value));
        } else if (layout instanceof ValueLayout.OfByte ofByte) {
            segment.setAtIndex(ofByte, index, value);
        } else if (layout instanceof ValueLayout.OfChar ofChar) {
            segment.setAtIndex(ofChar, index, toChar(value));
        } else {
            throw new UnsupportedOperationException("Unsupported layout for setByte");
        }
    }

    void setChar(long index, char value) {
        if (layout instanceof ValueLayout.OfDouble ofDouble) {
            segment.setAtIndex(ofDouble, index, (double) value);
        } else if (layout instanceof ValueLayout.OfFloat ofFloat) {
            segment.setAtIndex(ofFloat, index, (float) value);
        } else if (layout instanceof ValueLayout.OfLong ofLong) {
            segment.setAtIndex(ofLong, index, toLong(value));
        } else if (layout instanceof ValueLayout.OfInt ofInt) {
            segment.setAtIndex(ofInt, index, toInt(value));
        } else if (layout instanceof ValueLayout.OfShort ofShort) {
            segment.setAtIndex(ofShort, index, toShort(value));
        } else if (layout instanceof ValueLayout.OfByte ofByte) {
            segment.setAtIndex(ofByte, index, toByte(value));
        } else if (layout instanceof ValueLayout.OfChar ofChar) {
            segment.setAtIndex(ofChar, index, value);
        } else {
            throw new UnsupportedOperationException("Unsupported layout for setChar");
        }
    }
}
