package com.cohort.array;

import com.cohort.util.Math2;
import com.cohort.util.String2;
import java.io.DataOutputStream;
import java.io.RandomAccessFile;
import java.math.BigInteger;

/**
 * ULongArrayView is a read-only virtual view over a ULongArray.
 */
public class ULongArrayView extends ULongArray {
    ULongArray parent;
    int offset;
    int stride;

    public ULongArrayView(ULongArray parent, int offset, int stride, int length) {
        super();
        this.parent = parent;
        this.offset = offset;
        this.stride = stride;
        this.size = length;
        this.array = new long[0];
    }

    @Override
    public BigInteger get(final int index) {
        if (index < 0 || index >= size) {
            throw new IllegalArgumentException(
                String2.ERROR + " in ULongArrayView.get: index (" + index + ") >= size (" + size + ").");
        }
        return parent.get(offset + index * stride);
    }

    @Override
    public BigInteger getIgnoreMaxIsMV(final int index) {
        if (index < 0 || index >= size) {
            throw new IllegalArgumentException(
                String2.ERROR + " in ULongArrayView.getIgnoreMaxIsMV: index (" + index + ") >= size (" + size + ").");
        }
        return parent.getIgnoreMaxIsMV(offset + index * stride);
    }

    @Override
    public long getPacked(final int index) {
        if (index < 0 || index >= size) {
            throw new IllegalArgumentException(
                String2.ERROR + " in ULongArrayView.getPacked: index (" + index + ") >= size (" + size + ").");
        }
        return parent.getPacked(offset + index * stride);
    }

    @Override
    public void set(final int index, final BigInteger value) {
        throw new UnsupportedOperationException("ULongArrayView is read-only.");
    }

    @Override
    public void ensureCapacity(final long minCapacity) {
        if (minCapacity > size) {
            throw new UnsupportedOperationException("ULongArrayView is read-only and cannot be expanded.");
        }
    }

    @Override
    public long[] toArray() {
        long[] result = new long[size];
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
        return indexOf(new BigInteger(lookFor), startIndex);
    }

    public int indexOf(final BigInteger lookFor, final int startIndex) {
        for (int i = startIndex; i < size; i++) {
            BigInteger val = get(i);
            if (lookFor == null) {
                if (val == null) return i;
            } else if (lookFor.equals(val)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public int lastIndexOf(final String lookFor, final int startIndex) {
        return lastIndexOf(new BigInteger(lookFor), startIndex);
    }

    public int lastIndexOf(final BigInteger lookFor, final int startIndex) {
        if (startIndex >= size) {
            throw new IllegalArgumentException(
                String2.ERROR + " in ULongArrayView.lastIndexOf: startIndex (" + startIndex + ") >= size (" + size + ").");
        }
        for (int i = startIndex; i >= 0; i--) {
            BigInteger val = get(i);
            if (lookFor == null) {
                if (val == null) return i;
            } else if (lookFor.equals(val)) {
                return i;
            }
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
            dos.writeLong(getPacked(i));
        }
        return size == 0 ? 0 : 8;
    }

    @Override
    public int writeDos(final DataOutputStream dos, final int i) throws Exception {
        dos.writeLong(getPacked(i));
        return 8;
    }

    @Override
    public void writeToRAF(final RandomAccessFile raf, final int index) throws Exception {
        raf.writeLong(getPacked(index));
    }

    @Override
    public void remove(final int index) {
        throw new UnsupportedOperationException("ULongArrayView is read-only.");
    }

    @Override
    public void removeRange(final int from, final int to) {
        throw new UnsupportedOperationException("ULongArrayView is read-only.");
    }

    @Override
    public void move(final int first, final int last, final int destination) {
        throw new UnsupportedOperationException("ULongArrayView is read-only.");
    }

    @Override
    public void justKeep(final java.util.BitSet bitset) {
        throw new UnsupportedOperationException("ULongArrayView is read-only.");
    }

    @Override
    public void sort() {
        throw new UnsupportedOperationException("ULongArrayView is read-only.");
    }

    @Override
    public void reorder(final int rank[]) {
        throw new UnsupportedOperationException("ULongArrayView is read-only.");
    }

    @Override
    public void reverseBytes() {
        throw new UnsupportedOperationException("ULongArrayView is read-only.");
    }

    @Override
    public void append(final PrimitiveArray pa) {
        throw new UnsupportedOperationException("ULongArrayView is read-only.");
    }

    @Override
    public void rawAppend(final PrimitiveArray pa) {
        throw new UnsupportedOperationException("ULongArrayView is read-only.");
    }

    @Override
    public int hashCode() {
        int code = 0;
        for (int i = 0; i < size; i++) {
            BigInteger val = get(i);
            code = 31 * code + (val == null ? 0 : val.hashCode());
        }
        return code;
    }

    @Override
    public String testEquals(final Object o) {
        if (!(o instanceof ULongArray other)) {
            return "The two objects aren't equal: this object is a ULongArray; the other is a "
                + (o == null ? "null" : o.getClass().getName())
                + ".";
        }
        if (other.size() != size) {
            return "The two ULongArrays aren't equal: one has "
                + size
                + " value(s); the other has "
                + other.size()
                + " value(s).";
        }
        for (int i = 0; i < size; i++) {
            BigInteger s1 = get(i);
            BigInteger s2 = other.get(i);
            if (s1 == null ? s2 != null : !s1.equals(s2)) {
                return "The two ULongArrays aren't equal: this["
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
