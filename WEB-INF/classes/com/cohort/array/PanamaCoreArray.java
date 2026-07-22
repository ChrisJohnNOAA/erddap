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

    private ValueLayout.OfDouble doubleLayout;
    private ValueLayout.OfFloat floatLayout;
    private ValueLayout.OfLong longLayout;
    private ValueLayout.OfInt intLayout;
    private ValueLayout.OfShort shortLayout;
    private ValueLayout.OfByte byteLayout;
    private ValueLayout.OfChar charLayout;

    /**
     * Allocates native memory for the given capacity and value layout.
     */
    PanamaCoreArray(long capacity, ValueLayout layout) {
        this.layout = layout;
        this.segment = Arena.ofAuto().allocate(layout, capacity);
        this.capacity = capacity;
        this.onHeapArray = null;
        initLayoutFields();
    }

    /**
     * Wraps an existing MemorySegment.
     */
    PanamaCoreArray(MemorySegment segment, ValueLayout layout, long capacity, Object onHeapArray) {
        this.segment = segment;
        this.layout = layout;
        this.capacity = capacity;
        this.onHeapArray = onHeapArray;
        initLayoutFields();
    }

    private void initLayoutFields() {
        this.doubleLayout = null;
        this.floatLayout = null;
        this.longLayout = null;
        this.intLayout = null;
        this.shortLayout = null;
        this.byteLayout = null;
        this.charLayout = null;

        if (layout instanceof ValueLayout.OfDouble ofDouble) {
            this.doubleLayout = ofDouble;
        } else if (layout instanceof ValueLayout.OfFloat ofFloat) {
            this.floatLayout = ofFloat;
        } else if (layout instanceof ValueLayout.OfLong ofLong) {
            this.longLayout = ofLong;
        } else if (layout instanceof ValueLayout.OfInt ofInt) {
            this.intLayout = ofInt;
        } else if (layout instanceof ValueLayout.OfShort ofShort) {
            this.shortLayout = ofShort;
        } else if (layout instanceof ValueLayout.OfByte ofByte) {
            this.byteLayout = ofByte;
        } else if (layout instanceof ValueLayout.OfChar ofChar) {
            this.charLayout = ofChar;
        }
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
        initLayoutFields();
    }

    /**
     * Resize the internal storage segment.
     */
    void resize(long newCapacity) {
        if (newCapacity < 0) {
            throw new IllegalArgumentException("Capacity cannot be negative");
        }
        long bytesToCopy = Math.min(capacity, newCapacity) * layout.byteSize();
        // Since native segments are more efficient, convert to a native off-heap segment on resize
        MemorySegment newSegment = Arena.ofAuto().allocate(layout.byteSize() * newCapacity);
        this.onHeapArray = null;

        if (bytesToCopy > 0) {
            MemorySegment.copy(segment, 0, newSegment, 0, bytesToCopy);
        }
        this.segment = newSegment;
        this.capacity = newCapacity;
        initLayoutFields();
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

    // --- STRONGLY TYPED UNBOXED PRIMITIVE GETTERS ---

    double getDouble(long index) {
        if (doubleLayout != null) {
            return segment.getAtIndex(doubleLayout, index);
        } else if (floatLayout != null) {
            return (double) segment.getAtIndex(floatLayout, index);
        } else if (longLayout != null) {
            return (double) segment.getAtIndex(longLayout, index);
        } else if (intLayout != null) {
            return (double) segment.getAtIndex(intLayout, index);
        } else if (shortLayout != null) {
            return (double) segment.getAtIndex(shortLayout, index);
        } else if (byteLayout != null) {
            return (double) segment.getAtIndex(byteLayout, index);
        } else if (charLayout != null) {
            return (double) segment.getAtIndex(charLayout, index);
        }
        throw new UnsupportedOperationException("Unsupported layout for getDouble");
    }

    float getFloat(long index) {
        if (doubleLayout != null) {
            return NumbersSafeCast.toFloat(segment.getAtIndex(doubleLayout, index));
        } else if (floatLayout != null) {
            return segment.getAtIndex(floatLayout, index);
        } else if (longLayout != null) {
            return NumbersSafeCast.toFloat(segment.getAtIndex(longLayout, index));
        } else if (intLayout != null) {
            return NumbersSafeCast.toFloat(segment.getAtIndex(intLayout, index));
        } else if (shortLayout != null) {
            return (float) segment.getAtIndex(shortLayout, index);
        } else if (byteLayout != null) {
            return (float) segment.getAtIndex(byteLayout, index);
        } else if (charLayout != null) {
            return (float) segment.getAtIndex(charLayout, index);
        }
        throw new UnsupportedOperationException("Unsupported layout for getFloat");
    }

    long getLong(long index) {
        if (doubleLayout != null) {
            return NumbersSafeCast.toLong(segment.getAtIndex(doubleLayout, index));
        } else if (floatLayout != null) {
            return NumbersSafeCast.toLong(segment.getAtIndex(floatLayout, index));
        } else if (longLayout != null) {
            return segment.getAtIndex(longLayout, index);
        } else if (intLayout != null) {
            return (long) segment.getAtIndex(intLayout, index);
        } else if (shortLayout != null) {
            return (long) segment.getAtIndex(shortLayout, index);
        } else if (byteLayout != null) {
            return (long) segment.getAtIndex(byteLayout, index);
        } else if (charLayout != null) {
            return (long) segment.getAtIndex(charLayout, index);
        }
        throw new UnsupportedOperationException("Unsupported layout for getLong");
    }

    int getInt(long index) {
        if (doubleLayout != null) {
            return NumbersSafeCast.toInt(segment.getAtIndex(doubleLayout, index));
        } else if (floatLayout != null) {
            return NumbersSafeCast.toInt(segment.getAtIndex(floatLayout, index));
        } else if (longLayout != null) {
            return NumbersSafeCast.toInt(segment.getAtIndex(longLayout, index));
        } else if (intLayout != null) {
            return segment.getAtIndex(intLayout, index);
        } else if (shortLayout != null) {
            return (int) segment.getAtIndex(shortLayout, index);
        } else if (byteLayout != null) {
            return (int) segment.getAtIndex(byteLayout, index);
        } else if (charLayout != null) {
            return (int) segment.getAtIndex(charLayout, index);
        }
        throw new UnsupportedOperationException("Unsupported layout for getInt");
    }

    short getShort(long index) {
        if (doubleLayout != null) {
            return NumbersSafeCast.toShort(segment.getAtIndex(doubleLayout, index));
        } else if (floatLayout != null) {
            return NumbersSafeCast.toShort(segment.getAtIndex(floatLayout, index));
        } else if (longLayout != null) {
            return NumbersSafeCast.toShort(segment.getAtIndex(longLayout, index));
        } else if (intLayout != null) {
            return NumbersSafeCast.toShort(segment.getAtIndex(intLayout, index));
        } else if (shortLayout != null) {
            return segment.getAtIndex(shortLayout, index);
        } else if (byteLayout != null) {
            return (short) segment.getAtIndex(byteLayout, index);
        } else if (charLayout != null) {
            return (short) segment.getAtIndex(charLayout, index);
        }
        throw new UnsupportedOperationException("Unsupported layout for getShort");
    }

    byte getByte(long index) {
        if (doubleLayout != null) {
            return NumbersSafeCast.toByte(segment.getAtIndex(doubleLayout, index));
        } else if (floatLayout != null) {
            return NumbersSafeCast.toByte(segment.getAtIndex(floatLayout, index));
        } else if (longLayout != null) {
            return NumbersSafeCast.toByte(segment.getAtIndex(longLayout, index));
        } else if (intLayout != null) {
            return NumbersSafeCast.toByte(segment.getAtIndex(intLayout, index));
        } else if (shortLayout != null) {
            return NumbersSafeCast.toByte(segment.getAtIndex(shortLayout, index));
        } else if (byteLayout != null) {
            return segment.getAtIndex(byteLayout, index);
        } else if (charLayout != null) {
            return NumbersSafeCast.toByte(segment.getAtIndex(charLayout, index));
        }
        throw new UnsupportedOperationException("Unsupported layout for getByte");
    }

    char getChar(long index) {
        if (doubleLayout != null) {
            return NumbersSafeCast.toChar(segment.getAtIndex(doubleLayout, index));
        } else if (floatLayout != null) {
            return NumbersSafeCast.toChar(segment.getAtIndex(floatLayout, index));
        } else if (longLayout != null) {
            return NumbersSafeCast.toChar(segment.getAtIndex(longLayout, index));
        } else if (intLayout != null) {
            return NumbersSafeCast.toChar(segment.getAtIndex(intLayout, index));
        } else if (shortLayout != null) {
            return NumbersSafeCast.toChar(segment.getAtIndex(shortLayout, index));
        } else if (byteLayout != null) {
            return NumbersSafeCast.toChar(segment.getAtIndex(byteLayout, index));
        } else if (charLayout != null) {
            return segment.getAtIndex(charLayout, index);
        }
        throw new UnsupportedOperationException("Unsupported layout for getChar");
    }

    // --- STRONGLY TYPED UNBOXED PRIMITIVE SETTERS ---

    void setDouble(long index, double value) {
        if (doubleLayout != null) {
            segment.setAtIndex(doubleLayout, index, value);
        } else if (floatLayout != null) {
            segment.setAtIndex(floatLayout, index, NumbersSafeCast.toFloat(value));
        } else if (longLayout != null) {
            segment.setAtIndex(longLayout, index, NumbersSafeCast.toLong(value));
        } else if (intLayout != null) {
            segment.setAtIndex(intLayout, index, NumbersSafeCast.toInt(value));
        } else if (shortLayout != null) {
            segment.setAtIndex(shortLayout, index, NumbersSafeCast.toShort(value));
        } else if (byteLayout != null) {
            segment.setAtIndex(byteLayout, index, NumbersSafeCast.toByte(value));
        } else if (charLayout != null) {
            segment.setAtIndex(charLayout, index, NumbersSafeCast.toChar(value));
        } else {
            throw new UnsupportedOperationException("Unsupported layout for setDouble");
        }
    }

    void setFloat(long index, float value) {
        if (doubleLayout != null) {
            segment.setAtIndex(doubleLayout, index, value);
        } else if (floatLayout != null) {
            segment.setAtIndex(floatLayout, index, value);
        } else if (longLayout != null) {
            segment.setAtIndex(longLayout, index, NumbersSafeCast.toLong(value));
        } else if (intLayout != null) {
            segment.setAtIndex(intLayout, index, NumbersSafeCast.toInt(value));
        } else if (shortLayout != null) {
            segment.setAtIndex(shortLayout, index, NumbersSafeCast.toShort(value));
        } else if (byteLayout != null) {
            segment.setAtIndex(byteLayout, index, NumbersSafeCast.toByte(value));
        } else if (charLayout != null) {
            segment.setAtIndex(charLayout, index, NumbersSafeCast.toChar(value));
        } else {
            throw new UnsupportedOperationException("Unsupported layout for setFloat");
        }
    }

    void setLong(long index, long value) {
        if (doubleLayout != null) {
            segment.setAtIndex(doubleLayout, index, (double) value);
        } else if (floatLayout != null) {
            segment.setAtIndex(floatLayout, index, NumbersSafeCast.toFloat(value));
        } else if (longLayout != null) {
            segment.setAtIndex(longLayout, index, value);
        } else if (intLayout != null) {
            segment.setAtIndex(intLayout, index, NumbersSafeCast.toInt(value));
        } else if (shortLayout != null) {
            segment.setAtIndex(shortLayout, index, NumbersSafeCast.toShort(value));
        } else if (byteLayout != null) {
            segment.setAtIndex(byteLayout, index, NumbersSafeCast.toByte(value));
        } else if (charLayout != null) {
            segment.setAtIndex(charLayout, index, NumbersSafeCast.toChar(value));
        } else {
            throw new UnsupportedOperationException("Unsupported layout for setLong");
        }
    }

    void setInt(long index, int value) {
        if (doubleLayout != null) {
            segment.setAtIndex(doubleLayout, index, (double) value);
        } else if (floatLayout != null) {
            segment.setAtIndex(floatLayout, index, NumbersSafeCast.toFloat(value));
        } else if (longLayout != null) {
            segment.setAtIndex(longLayout, index, NumbersSafeCast.toLong(value));
        } else if (intLayout != null) {
            segment.setAtIndex(intLayout, index, value);
        } else if (shortLayout != null) {
            segment.setAtIndex(shortLayout, index, NumbersSafeCast.toShort(value));
        } else if (byteLayout != null) {
            segment.setAtIndex(byteLayout, index, NumbersSafeCast.toByte(value));
        } else if (charLayout != null) {
            segment.setAtIndex(charLayout, index, NumbersSafeCast.toChar(value));
        } else {
            throw new UnsupportedOperationException("Unsupported layout for setInt");
        }
    }

    void setShort(long index, short value) {
        if (doubleLayout != null) {
            segment.setAtIndex(doubleLayout, index, (double) value);
        } else if (floatLayout != null) {
            segment.setAtIndex(floatLayout, index, NumbersSafeCast.toFloat(value));
        } else if (longLayout != null) {
            segment.setAtIndex(longLayout, index, NumbersSafeCast.toLong(value));
        } else if (intLayout != null) {
            segment.setAtIndex(intLayout, index, NumbersSafeCast.toInt(value));
        } else if (shortLayout != null) {
            segment.setAtIndex(shortLayout, index, value);
        } else if (byteLayout != null) {
            segment.setAtIndex(byteLayout, index, NumbersSafeCast.toByte(value));
        } else if (charLayout != null) {
            segment.setAtIndex(charLayout, index, NumbersSafeCast.toChar(value));
        } else {
            throw new UnsupportedOperationException("Unsupported layout for setShort");
        }
    }

    void setByte(long index, byte value) {
        if (doubleLayout != null) {
            segment.setAtIndex(doubleLayout, index, (double) value);
        } else if (floatLayout != null) {
            segment.setAtIndex(floatLayout, index, NumbersSafeCast.toFloat(value));
        } else if (longLayout != null) {
            segment.setAtIndex(longLayout, index, NumbersSafeCast.toLong(value));
        } else if (intLayout != null) {
            segment.setAtIndex(intLayout, index, NumbersSafeCast.toInt(value));
        } else if (shortLayout != null) {
            segment.setAtIndex(shortLayout, index, NumbersSafeCast.toShort(value));
        } else if (byteLayout != null) {
            segment.setAtIndex(byteLayout, index, value);
        } else if (charLayout != null) {
            segment.setAtIndex(charLayout, index, NumbersSafeCast.toChar(value));
        } else {
            throw new UnsupportedOperationException("Unsupported layout for setByte");
        }
    }

    void setChar(long index, char value) {
        if (doubleLayout != null) {
            segment.setAtIndex(doubleLayout, index, (double) value);
        } else if (floatLayout != null) {
            segment.setAtIndex(floatLayout, index, NumbersSafeCast.toFloat(value));
        } else if (longLayout != null) {
            segment.setAtIndex(longLayout, index, NumbersSafeCast.toLong(value));
        } else if (intLayout != null) {
            segment.setAtIndex(intLayout, index, NumbersSafeCast.toInt(value));
        } else if (shortLayout != null) {
            segment.setAtIndex(shortLayout, index, NumbersSafeCast.toShort(value));
        } else if (byteLayout != null) {
            segment.setAtIndex(byteLayout, index, NumbersSafeCast.toByte(value));
        } else if (charLayout != null) {
            segment.setAtIndex(charLayout, index, value);
        } else {
            throw new UnsupportedOperationException("Unsupported layout for setChar");
        }
    }
}
