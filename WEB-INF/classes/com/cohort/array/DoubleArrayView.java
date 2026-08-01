package com.cohort.array;

import com.cohort.util.Math2;
import com.cohort.util.String2;
import java.io.DataOutputStream;
import java.io.RandomAccessFile;

/**
 * DoubleArrayView is a read-only virtual view over a DoubleArray.
 */
public class DoubleArrayView extends DoubleArray {
    DoubleArray parent;
    int offset;
    int stride;

    public DoubleArrayView(DoubleArray parent, int offset, int stride, int length) {
        super();
        this.parent = parent;
        this.offset = offset;
        this.stride = stride;
        this.size = length;
        this.array = new double[0]; // Avoid null pointer exceptions in capacity() and keep it lightweight
    }

    @Override
    public double get(final int index) {
        if (index < 0 || index >= size) {
            throw new IllegalArgumentException(
                String2.ERROR + " in DoubleArrayView.get: index (" + index + ") >= size (" + size + ").");
        }
        return parent.get(offset + index * stride);
    }

    @Override
    public void set(final int index, final double value) {
        throw new UnsupportedOperationException("DoubleArrayView is read-only.");
    }

    @Override
    public void ensureCapacity(final long minCapacity) {
        if (minCapacity > size) {
            throw new UnsupportedOperationException("DoubleArrayView is read-only and cannot be expanded.");
        }
    }

    @Override
    public double[] toArray() {
        double[] result = new double[size];
        for (int i = 0; i < size; i++) {
            result[i] = get(i);
        }
        return result;
    }

    @Override
    public Object toObjectArray() {
        return toArray();
    }

    @Override
    public double[] toDoubleArray() {
        return toArray();
    }

    @Override
    public String[] toStringArray() {
        String[] result = new String[size];
        for (int i = 0; i < size; i++) {
            double d = get(i);
            result[i] = Double.isFinite(d) ? String.valueOf(d) : "";
        }
        return result;
    }

    @Override
    public int indexOf(final double lookFor, final int startIndex) {
        if (Double.isNaN(lookFor)) {
            for (int i = startIndex; i < size; i++) {
                if (Double.isNaN(get(i))) return i;
            }
            return -1;
        }
        for (int i = startIndex; i < size; i++) {
            if (get(i) == lookFor) return i;
        }
        return -1;
    }

    @Override
    public int indexOf(final String lookFor, final int startIndex) {
        if (startIndex >= size) return -1;
        return indexOf(String2.parseDouble(lookFor), startIndex);
    }

    @Override
    public int lastIndexOf(final double lookFor, final int startIndex) {
        if (startIndex >= size) {
            throw new IllegalArgumentException(
                String2.ERROR + " in DoubleArrayView.lastIndexOf: startIndex (" + startIndex + ") >= size (" + size + ").");
        }
        for (int i = startIndex; i >= 0; i--) {
            if (get(i) == lookFor) return i;
        }
        return -1;
    }

    @Override
    public int lastIndexOf(final String lookFor, final int startIndex) {
        return lastIndexOf(String2.parseDouble(lookFor), startIndex);
    }

    @Override
    public void trimToSize() {
        // no-op
    }

    @Override
    public int writeDos(final DataOutputStream dos) throws Exception {
        for (int i = 0; i < size; i++) {
            dos.writeDouble(get(i));
        }
        return size == 0 ? 0 : 8;
    }

    @Override
    public int writeDos(final DataOutputStream dos, final int i) throws Exception {
        dos.writeDouble(get(i));
        return 8;
    }

    @Override
    public void writeToRAF(final RandomAccessFile raf, final int index) throws Exception {
        raf.writeDouble(get(index));
    }

    @Override
    public void remove(final int index) {
        throw new UnsupportedOperationException("DoubleArrayView is read-only.");
    }

    @Override
    public void removeRange(final int from, final int to) {
        throw new UnsupportedOperationException("DoubleArrayView is read-only.");
    }

    @Override
    public void move(final int first, final int last, final int destination) {
        throw new UnsupportedOperationException("DoubleArrayView is read-only.");
    }

    @Override
    public void justKeep(final java.util.BitSet bitset) {
        throw new UnsupportedOperationException("DoubleArrayView is read-only.");
    }

    @Override
    public void sort() {
        throw new UnsupportedOperationException("DoubleArrayView is read-only.");
    }

    @Override
    public void reorder(final int rank[]) {
        throw new UnsupportedOperationException("DoubleArrayView is read-only.");
    }

    @Override
    public void reverseBytes() {
        throw new UnsupportedOperationException("DoubleArrayView is read-only.");
    }

    @Override
    public void append(final PrimitiveArray pa) {
        throw new UnsupportedOperationException("DoubleArrayView is read-only.");
    }

    @Override
    public void rawAppend(final PrimitiveArray pa) {
        throw new UnsupportedOperationException("DoubleArrayView is read-only.");
    }

    @Override
    public int hashCode() {
        int code = 0;
        for (int i = 0; i < size; i++) {
            code = 31 * code + Double.hashCode(get(i));
        }
        return code;
    }

    @Override
    public String testEquals(final Object o) {
        if (!(o instanceof DoubleArray other)) {
            return "The two objects aren't equal: this object is a DoubleArray; the other is a "
                + (o == null ? "null" : o.getClass().getName())
                + ".";
        }
        if (other.size() != size) {
            return "The two DoubleArrays aren't equal: one has "
                + size
                + " value(s); the other has "
                + other.size()
                + " value(s).";
        }
        for (int i = 0; i < size; i++) {
            if (!Math2.equalsIncludingNanOrInfinite(get(i), other.get(i))) {
                return "The two DoubleArrays aren't equal: this["
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
