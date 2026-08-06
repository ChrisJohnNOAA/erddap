package com.cohort.array;

import com.cohort.util.Math2;
import com.cohort.util.String2;
import java.io.DataOutputStream;
import java.io.RandomAccessFile;

/**
 * FloatArrayView is a read-only-to-parent virtual view over a FloatArray that materializes on mutation (Copy-on-Write).
 */
public class FloatArrayView extends FloatArray {
    FloatArray parent;
    int offset;
    int stride;

    public FloatArrayView(FloatArray parent, int offset, int stride, int length) {
        super();
        this.parent = parent;
        this.offset = offset;
        this.stride = stride;
        this.size = length;
        this.array = new float[0];
    }

    private void materialize() {
        if (array == null || array.length < size) {
            float[] realArray = new float[size];
            for (int i = 0; i < size; i++) {
                realArray[i] = parent.get(offset + i * stride);
            }
            this.array = realArray;
        }
    }

    @Override
    public float get(final int index) {
        if (index < 0 || index >= size) {
            throw new IllegalArgumentException(
                String2.ERROR + " in FloatArrayView.get: index (" + index + ") >= size (" + size + ").");
        }
        if (array != null && array.length >= size) {
            return array[index];
        }
        return parent.get(offset + index * stride);
    }

    @Override
    public void set(final int index, final float value) {
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
    public float[] toArray() {
        if (array != null && array.length >= size) {
            return super.toArray();
        }
        float[] result = new float[size];
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
    public int indexOf(final float lookFor, final int startIndex) {
        if (array != null && array.length >= size) {
            return super.indexOf(lookFor, startIndex);
        }
        if (Float.isNaN(lookFor)) {
            for (int i = startIndex; i < size; i++) {
                if (Float.isNaN(get(i))) return i;
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
        return indexOf(String2.parseFloat(lookFor), startIndex);
    }

    @Override
    public int lastIndexOf(final float lookFor, final int startIndex) {
        if (startIndex >= size) {
            throw new IllegalArgumentException(
                String2.ERROR + " in FloatArrayView.lastIndexOf: startIndex (" + startIndex + ") >= size (" + size + ").");
        }
        if (array != null && array.length >= size) {
            return super.lastIndexOf(lookFor, startIndex);
        }
        for (int i = startIndex; i >= 0; i--) {
            if (get(i) == lookFor) return i;
        }
        return -1;
    }

    @Override
    public int lastIndexOf(final String lookFor, final int startIndex) {
        return lastIndexOf(String2.parseFloat(lookFor), startIndex);
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
            dos.writeFloat(get(i));
        }
        return size == 0 ? 0 : 4;
    }

    @Override
    public int writeDos(final DataOutputStream dos, final int i) throws Exception {
        if (array != null && array.length >= size) {
            return super.writeDos(dos, i);
        }
        dos.writeFloat(get(i));
        return 4;
    }

    @Override
    public void writeToRAF(final RandomAccessFile raf, final int index) throws Exception {
        if (array != null && array.length >= size) {
            super.writeToRAF(raf, index);
            return;
        }
        raf.writeFloat(get(index));
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
            code = 31 * code + Float.hashCode(get(i));
        }
        return code;
    }

    @Override
    public String testEquals(final Object o) {
        if (!(o instanceof FloatArray other)) {
            return "The two objects aren't equal: this object is a FloatArray; the other is a "
                + (o == null ? "null" : o.getClass().getName())
                + ".";
        }
        if (other.size() != size) {
            return "The two FloatArrays aren't equal: one has "
                + size
                + " value(s); the other has "
                + other.size()
                + " value(s).";
        }
        for (int i = 0; i < size; i++) {
            if (!Math2.equalsIncludingNanOrInfinite(get(i), other.get(i))) {
                return "The two FloatArrays aren't equal: this["
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
