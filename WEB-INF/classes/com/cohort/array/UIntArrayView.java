package com.cohort.array;

import com.cohort.util.Math2;
import com.cohort.util.String2;
import java.io.DataOutputStream;
import java.io.RandomAccessFile;
import java.math.BigInteger;

/**
 * UIntArrayView is a read-only virtual view over a UIntArray.
 */
public class UIntArrayView extends UIntArray {
    UIntArray parent;
    int offset;
    int stride;

    public UIntArrayView(UIntArray parent, int offset, int stride, int length) {
        super();
        this.parent = parent;
        this.offset = offset;
        this.stride = stride;
        this.size = length;
        this.array = new int[0];
    }

    @Override
    public long get(final int index) {
        if (index < 0 || index >= size) {
            throw new IllegalArgumentException(
                String2.ERROR + " in UIntArrayView.get: index (" + index + ") >= size (" + size + ").");
        }
        return parent.get(offset + index * stride);
    }

    @Override
    public int getPacked(final int index) {
        if (index < 0 || index >= size) {
            throw new IllegalArgumentException(
                String2.ERROR + " in UIntArrayView.getPacked: index (" + index + ") >= size (" + size + ").");
        }
        return parent.getPacked(offset + index * stride);
    }

    @Override
    public void set(final int index, final long value) {
        throw new UnsupportedOperationException("UIntArrayView is read-only.");
    }

    @Override
    public void ensureCapacity(final long minCapacity) {
        if (minCapacity > size) {
            throw new UnsupportedOperationException("UIntArrayView is read-only and cannot be expanded.");
        }
    }

    @Override
    public int[] toArray() {
        int[] result = new int[size];
        for (int i = 0; i < size; i++) {
            result[i] = getPacked(i);
        }
        return result;
    }

    @Override
    public Object toObjectArray() {
        return toArray();
    }

    @Override
    public double[] toDoubleArray() {
        double[] result = new double[size];
        for (int i = 0; i < size; i++) {
            result[i] = getDouble(i);
        }
        return result;
    }

    @Override
    public String[] toStringArray() {
        String[] result = new String[size];
        for (int i = 0; i < size; i++) {
            result[i] = getString(i);
        }
        return result;
    }

    @Override
    public int indexOf(final String lookFor, final int startIndex) {
        if (startIndex >= size) return -1;
        return indexOf(String2.parseLong(lookFor), startIndex);
    }

    public int indexOf(final long lookFor, final int startIndex) {
        for (int i = startIndex; i < size; i++) {
            if (get(i) == lookFor) return i;
        }
        return -1;
    }

    @Override
    public int lastIndexOf(final String lookFor, final int startIndex) {
        return lastIndexOf(String2.parseLong(lookFor), startIndex);
    }

    public int lastIndexOf(final long lookFor, final int startIndex) {
        if (startIndex >= size) {
            throw new IllegalArgumentException(
                String2.ERROR + " in UIntArrayView.lastIndexOf: startIndex (" + startIndex + ") >= size (" + size + ").");
        }
        for (int i = startIndex; i >= 0; i--) {
            if (get(i) == lookFor) return i;
        }
        return -1;
    }

    @Override
    public void trimToSize() {
        // no-op
    }

    @Override
    public int writeDos(final DataOutputStream dos) throws Exception {
        for (int i = 0; i < size; i++) {
            dos.writeInt(getPacked(i));
        }
        return size == 0 ? 0 : 4;
    }

    @Override
    public int writeDos(final DataOutputStream dos, final int i) throws Exception {
        dos.writeInt(getPacked(i));
        return 4;
    }

    @Override
    public void writeToRAF(final RandomAccessFile raf, final int index) throws Exception {
        raf.writeInt(getPacked(index));
    }

    @Override
    public void remove(final int index) {
        throw new UnsupportedOperationException("UIntArrayView is read-only.");
    }

    @Override
    public void removeRange(final int from, final int to) {
        throw new UnsupportedOperationException("UIntArrayView is read-only.");
    }

    @Override
    public void move(final int first, final int last, final int destination) {
        throw new UnsupportedOperationException("UIntArrayView is read-only.");
    }

    @Override
    public void justKeep(final java.util.BitSet bitset) {
        throw new UnsupportedOperationException("UIntArrayView is read-only.");
    }

    @Override
    public void sort() {
        throw new UnsupportedOperationException("UIntArrayView is read-only.");
    }

    @Override
    public void reorder(final int rank[]) {
        throw new UnsupportedOperationException("UIntArrayView is read-only.");
    }

    @Override
    public void reverseBytes() {
        throw new UnsupportedOperationException("UIntArrayView is read-only.");
    }

    @Override
    public void append(final PrimitiveArray pa) {
        throw new UnsupportedOperationException("UIntArrayView is read-only.");
    }

    @Override
    public void rawAppend(final PrimitiveArray pa) {
        throw new UnsupportedOperationException("UIntArrayView is read-only.");
    }

    @Override
    public int hashCode() {
        int code = 0;
        for (int i = 0; i < size; i++) {
            code = 31 * code + Long.hashCode(get(i));
        }
        return code;
    }

    @Override
    public String testEquals(final Object o) {
        if (!(o instanceof UIntArray other)) {
            return "The two objects aren't equal: this object is a UIntArray; the other is a "
                + (o == null ? "null" : o.getClass().getName())
                + ".";
        }
        if (other.size() != size) {
            return "The two UIntArrays aren't equal: one has "
                + size
                + " value(s); the other has "
                + other.size()
                + " value(s).";
        }
        for (int i = 0; i < size; i++) {
            if (getPacked(i) != other.getPacked(i)) {
                return "The two UIntArrays aren't equal: this["
                    + i
                    + "]="
                    + get(i)
                    + "; other["
                    + i
                    + "]="
                    + other.get(i)
                    + ".";
            }
        }
        return "";
    }
}
