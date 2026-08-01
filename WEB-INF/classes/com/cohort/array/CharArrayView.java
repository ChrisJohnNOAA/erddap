package com.cohort.array;

import com.cohort.util.Math2;
import com.cohort.util.String2;
import java.io.DataOutputStream;
import java.io.RandomAccessFile;
import java.math.BigInteger;

/**
 * CharArrayView is a read-only virtual view over a CharArray.
 */
public class CharArrayView extends CharArray {
    CharArray parent;
    int offset;
    int stride;

    public CharArrayView(CharArray parent, int offset, int stride, int length) {
        super();
        this.parent = parent;
        this.offset = offset;
        this.stride = stride;
        this.size = length;
        this.array = new char[0];
    }

    @Override
    public char get(final int index) {
        if (index < 0 || index >= size) {
            throw new IllegalArgumentException(
                String2.ERROR + " in CharArrayView.get: index (" + index + ") >= size (" + size + ").");
        }
        return parent.get(offset + index * stride);
    }

    @Override
    public void set(final int index, final char value) {
        throw new UnsupportedOperationException("CharArrayView is read-only.");
    }

    @Override
    public void ensureCapacity(final long minCapacity) {
        if (minCapacity > size) {
            throw new UnsupportedOperationException("CharArrayView is read-only and cannot be expanded.");
        }
    }

    @Override
    public char[] toArray() {
        char[] result = new char[size];
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
        if (lookFor == null || lookFor.length() == 0) return -1;
        return indexOf(lookFor.charAt(0), startIndex);
    }

    public int indexOf(final char lookFor, final int startIndex) {
        for (int i = startIndex; i < size; i++) {
            if (get(i) == lookFor) return i;
        }
        return -1;
    }

    @Override
    public int lastIndexOf(final String lookFor, final int startIndex) {
        if (lookFor == null || lookFor.length() == 0) return -1;
        return lastIndexOf(lookFor.charAt(0), startIndex);
    }

    public int lastIndexOf(final char lookFor, final int startIndex) {
        if (startIndex >= size) {
            throw new IllegalArgumentException(
                String2.ERROR + " in CharArrayView.lastIndexOf: startIndex (" + startIndex + ") >= size (" + size + ").");
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
            dos.writeChar(get(i));
        }
        return size == 0 ? 0 : 2;
    }

    @Override
    public int writeDos(final DataOutputStream dos, final int i) throws Exception {
        dos.writeChar(get(i));
        return 2;
    }

    @Override
    public void writeToRAF(final RandomAccessFile raf, final int index) throws Exception {
        raf.writeChar(get(index));
    }

    @Override
    public void remove(final int index) {
        throw new UnsupportedOperationException("CharArrayView is read-only.");
    }

    @Override
    public void removeRange(final int from, final int to) {
        throw new UnsupportedOperationException("CharArrayView is read-only.");
    }

    @Override
    public void move(final int first, final int last, final int destination) {
        throw new UnsupportedOperationException("CharArrayView is read-only.");
    }

    @Override
    public void justKeep(final java.util.BitSet bitset) {
        throw new UnsupportedOperationException("CharArrayView is read-only.");
    }

    @Override
    public void sort() {
        throw new UnsupportedOperationException("CharArrayView is read-only.");
    }

    @Override
    public void reorder(final int rank[]) {
        throw new UnsupportedOperationException("CharArrayView is read-only.");
    }

    @Override
    public void reverseBytes() {
        throw new UnsupportedOperationException("CharArrayView is read-only.");
    }

    @Override
    public void append(final PrimitiveArray pa) {
        throw new UnsupportedOperationException("CharArrayView is read-only.");
    }

    @Override
    public void rawAppend(final PrimitiveArray pa) {
        throw new UnsupportedOperationException("CharArrayView is read-only.");
    }

    @Override
    public int hashCode() {
        int code = 0;
        for (int i = 0; i < size; i++) {
            code = 31 * code + Character.hashCode(get(i));
        }
        return code;
    }

    @Override
    public String testEquals(final Object o) {
        if (!(o instanceof CharArray other)) {
            return "The two objects aren't equal: this object is a CharArray; the other is a "
                + (o == null ? "null" : o.getClass().getName())
                + ".";
        }
        if (other.size() != size) {
            return "The two CharArrays aren't equal: one has "
                + size
                + " value(s); the other has "
                + other.size()
                + " value(s).";
        }
        for (int i = 0; i < size; i++) {
            if (get(i) != other.get(i)) {
                return "The two CharArrays aren't equal: this["
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
