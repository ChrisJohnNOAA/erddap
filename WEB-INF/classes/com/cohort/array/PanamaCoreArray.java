package com.cohort.array;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.ByteOrder;

/**
 * PanamaCoreArray acts as the single high-performance engine for all primitive types
 * in the com.cohort.array package, using JDK Foreign Function & Memory (FFM) API with final fields
 * to enable aggressive JIT constant-folding and direct assembly-level native access.
 */
class PanamaCoreArray {

    final MemorySegment segment;
    final ValueLayout layout;
    final long capacity;
    final Object onHeapArray;

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
     * Configures the byte order by returning a new immutable PanamaCoreArray instance.
     */
    PanamaCoreArray withByteOrder(ByteOrder order) {
        ValueLayout newLayout = layout;
        if (layout instanceof ValueLayout.OfDouble ofDouble) {
            newLayout = ofDouble.withOrder(order);
        } else if (layout instanceof ValueLayout.OfFloat ofFloat) {
            newLayout = ofFloat.withOrder(order);
        } else if (layout instanceof ValueLayout.OfLong ofLong) {
            newLayout = ofLong.withOrder(order);
        } else if (layout instanceof ValueLayout.OfInt ofInt) {
            newLayout = ofInt.withOrder(order);
        } else if (layout instanceof ValueLayout.OfShort ofShort) {
            newLayout = ofShort.withOrder(order);
        } else if (layout instanceof ValueLayout.OfByte ofByte) {
            newLayout = ofByte.withOrder(order);
        } else if (layout instanceof ValueLayout.OfChar ofChar) {
            newLayout = ofChar.withOrder(order);
        }
        return new PanamaCoreArray(segment, newLayout, capacity, onHeapArray);
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
}
