package com.cohort.array;

import com.cohort.util.Math2;
import com.cohort.util.String2;
import java.io.DataOutputStream;
import java.io.RandomAccessFile;

/**
 * UIntArrayView is a read-only-to-parent virtual view over a UIntArray that materializes on mutation (Copy-on-Write).
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

    private void materialize() {
        if (array == null || array.length < size) {
            int[] realArray = new int[size];
            for (int i = 0; i < size; i++) {
                realArray[i] = parent.getPacked(offset + i * stride);
            }
            this.array = realArray;
        }
    }

    @Override
    public long get(final int index) {
        if (index < 0 || index >= size) {
            throw new IllegalArgumentException(
                String2.ERROR + " in UIntArrayView.get: index (" + index + ") >= size (" + size + ").");
        }
        if (array != null && array.length >= size) {
            return unpack(array[index]);
        }
        return parent.get(offset + index * stride);
    }

    @Override
    public int getPacked(final int index) {
        if (index < 0 || index >= size) {
            throw new IllegalArgumentException(
                String2.ERROR + " in UIntArrayView.getPacked: index (" + index + ") >= size (" + size + ").");
        }
        if (array != null && array.length >= size) {
            return array[index];
        }
        return parent.getPacked(offset + index * stride);
    }

    @Override
    public void set(final int index, final long value) {
        materialize();
        super.set(index, value);
    }

    @Override
    public void ensureCapacity(final long minCapacity) {
        if (minCapacity > size) {
            materialize();
            super.ensureCapacity(minCapacity);
        }
    }

    @Override
    public int[] toArray() {
        if (array != null && array.length >= size) {
            return super.toArray();
        }
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
        if (array != null && array.length >= size) {
            return super.toDoubleArray();
        }
        double[] result = new double[size];
        for (int i = 0; i < size; i++) {
            result[i] = getDouble(i);
        }
        return result;
    }

    @Override
    public String[] toStringArray() {
        if (array != null && array.length >= size) {
            return super.toStringArray();
        }
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

    @Override
    public int indexOf(final long lookFor, final int startIndex) {
        if (array != null && array.length >= size) {
            for (int i = startIndex; i < size; i++) {
                if (unpack(array[i]) == lookFor) return i;
            }
            return -1;
        }
        for (int i = startIndex; i < size; i++) {
            if (get(i) == lookFor) return i;
        }
        return -1;
    }

    @Override
    public int lastIndexOf(final String lookFor, final int startIndex) {
        return lastIndexOf(String2.parseLong(lookFor), startIndex);
    }

    @Override
    public int lastIndexOf(final long lookFor, final int startIndex) {
        if (startIndex >= size) {
            throw new IllegalArgumentException(
                String2.ERROR + " in UIntArrayView.lastIndexOf: startIndex (" + startIndex + ") >= size (" + size + ").");
        }
        if (array != null && array.length >= size) {
            for (int i = startIndex; i >= 0; i--) {
                if (unpack(array[i]) == lookFor) return i;
            }
            return -1;
        }
        for (int i = startIndex; i >= 0; i--) {
            if (get(i) == lookFor) return i;
        }
        return -1;
    }

    @Override
    public void trimToSize() {
        if (array != null && array.length >= size) {
            super.trimToSize();
        }
    }

    @Override
    public int writeDos(final DataOutputStream dos) throws Exception {
        if (array != null && array.length >= size) {
            return super.writeDos(dos);
        }
        for (int i = 0; i < size; i++) {
            dos.writeInt(getPacked(i));
        }
        return size == 0 ? 0 : 4;
    }

    @Override
    public int writeDos(final DataOutputStream dos, final int i) throws Exception {
        if (array != null && array.length >= size) {
            return super.writeDos(dos, i);
        }
        dos.writeInt(getPacked(i));
        return 4;
    }

    @Override
    public void writeToRAF(final RandomAccessFile raf, final int index) throws Exception {
        if (array != null && array.length >= size) {
            super.writeToRAF(raf, index);
            return;
        }
        raf.writeInt(getPacked(index));
    }

    @Override
    public void remove(final int index) {
        materialize();
        super.remove(index);
    }

    @Override
    public void removeRange(final int from, final int to) {
        materialize();
        super.removeRange(from, to);
    }

    @Override
    public void move(final int first, final int last, final int destination) {
        materialize();
        super.move(first, last, destination);
    }

    @Override
    public void justKeep(final java.util.BitSet bitset) {
        materialize();
        super.justKeep(bitset);
    }

    @Override
    public void sort() {
        materialize();
        super.sort();
    }

    @Override
    public void reorder(final int rank[]) {
        materialize();
        super.reorder(rank);
    }

    @Override
    public void reverseBytes() {
        materialize();
        super.reverseBytes();
    }

    @Override
    public void append(final PrimitiveArray pa) {
        materialize();
        super.append(pa);
    }

    @Override
    public void rawAppend(final PrimitiveArray pa) {
        materialize();
        super.rawAppend(pa);
    }

    @Override
    public int hashCode() {
        if (array != null && array.length >= size) {
            return super.hashCode();
        }
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
