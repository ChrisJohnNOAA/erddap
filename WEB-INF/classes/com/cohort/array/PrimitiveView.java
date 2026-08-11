/* This file is part of the EMA project and is
 * Copyright (c) 2026 Robert Simons (CoHortSoftware@gmail.com).
 * See the MIT/X-like license in LICENSE.txt.
 * For more information visit www.cohortsoftware.com or contact CoHortSoftware@gmail.com.
 */
package com.cohort.array;

import com.cohort.util.String2;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.math.BigInteger;
import java.nio.channels.FileChannel;
import java.util.BitSet;
import ucar.ma2.StructureData;

/**
 * PrimitiveView provides a zero-copy virtual view mechanism over a PrimitiveArray. It directly
 * extends PrimitiveArray (to avoid breaking instanceof and backing array type checks). Any writes
 * or mutations to a PrimitiveView trigger Copy-On-Write (COW) materialization.
 */
public class PrimitiveView extends PrimitiveArray {

  public PrimitiveArray source;
  public int offset;
  public int stride;
  public PrimitiveArray materialized;

  /** Constructor for PrimitiveView. */
  public PrimitiveView(PrimitiveArray source, int offset, int stride, int length) {
    if (source instanceof PrimitiveView pv) {
      this.offset = pv.offset + (offset * pv.stride);
      this.stride = pv.stride * stride;
      this.source = pv.source;
      this.materialized = pv.materialized;
    } else {
      this.source = source;
      this.offset = offset;
      this.stride = stride;
    }
    this.size = length;
    this.maxIsMV = this.source.getMaxIsMV();
  }

  /** Helper method to trigger Copy-On-Write materialization. */
  public PrimitiveArray materialize() {
    if (materialized == null) {
      PrimitiveArray newArray = PrimitiveArray.factory(elementType(), size, false);
      for (int i = 0; i < size; i++) {
        newArray.addObject(
            source.toObjectArray() instanceof String[]
                ? source.getString(offset + i * stride)
                : source.getNiceDouble(offset + i * stride));
      }
      newArray.setMaxIsMV(maxIsMV);
      this.materialized = newArray;
    }
    return materialized;
  }

  private PrimitiveArray current() {
    return materialized != null ? materialized : source;
  }

  private int mapIndex(int index) {
    if (index >= size) {
      throw new IllegalArgumentException(
          String2.ERROR + " in PrimitiveView: index (" + index + ") >= size (" + size + ").");
    }
    return materialized != null ? index : (offset + index * stride);
  }

  // --- Abstract overrides from PrimitiveArray ---

  @Override
  public PAOne MINEST_VALUE() {
    return current().MINEST_VALUE();
  }

  @Override
  public PAOne MAXEST_VALUE() {
    return current().MAXEST_VALUE();
  }

  @Override
  public int capacity() {
    return materialized != null ? materialized.capacity() : size;
  }

  @Override
  public PAType elementType() {
    return source.elementType();
  }

  @Override
  public int elementSize() {
    return source.elementSize();
  }

  @Override
  public double missingValueAsDouble() {
    return source.missingValueAsDouble();
  }

  @Override
  public boolean isMaxValue(int index) {
    return current().isMaxValue(mapIndex(index));
  }

  @Override
  public boolean isMissingValue(int index) {
    return current().isMissingValue(mapIndex(index));
  }

  @Override
  public PAType needPAType(PAType tPAType) {
    return source.needPAType(tPAType);
  }

  @Override
  public boolean isUnsigned() {
    return source.isUnsigned();
  }

  @Override
  public boolean isIntegerType() {
    return source.isIntegerType();
  }

  @Override
  public boolean isFloatingPointType() {
    return source.isFloatingPointType();
  }

  @Override
  public void atInsertString(int index, String value) {
    materialize().atInsertString(index, value);
    this.size = materialized.size();
    this.offset = 0;
    this.stride = 1;
    this.source = materialized;
  }

  @Override
  public void addObject(Object value) {
    materialize().addObject(value);
    this.size = materialized.size();
    this.offset = 0;
    this.stride = 1;
    this.source = materialized;
  }

  @Override
  public void add(StructureData sd, String memberName) {
    materialize().add(sd, memberName);
    this.size = materialized.size();
    this.offset = 0;
    this.stride = 1;
    this.source = materialized;
  }

  @Override
  public void addNPAOnes(int n, PAOne value) {
    materialize().addNPAOnes(n, value);
    this.size = materialized.size();
    this.offset = 0;
    this.stride = 1;
    this.source = materialized;
  }

  @Override
  public void addNStrings(int n, String value) {
    materialize().addNStrings(n, value);
    this.size = materialized.size();
    this.offset = 0;
    this.stride = 1;
    this.source = materialized;
  }

  @Override
  public void addNFloats(int n, float value) {
    materialize().addNFloats(n, value);
    this.size = materialized.size();
    this.offset = 0;
    this.stride = 1;
    this.source = materialized;
  }

  @Override
  public void addNDoubles(int n, double value) {
    materialize().addNDoubles(n, value);
    this.size = materialized.size();
    this.offset = 0;
    this.stride = 1;
    this.source = materialized;
  }

  @Override
  public void addNInts(int n, int value) {
    materialize().addNInts(n, value);
    this.size = materialized.size();
    this.offset = 0;
    this.stride = 1;
    this.source = materialized;
  }

  @Override
  public void addNLongs(int n, long value) {
    materialize().addNLongs(n, value);
    this.size = materialized.size();
    this.offset = 0;
    this.stride = 1;
    this.source = materialized;
  }

  @Override
  public PrimitiveArray addFromPA(PrimitiveArray otherPA, int otherIndex, int nValues) {
    materialize().addFromPA(otherPA, otherIndex, nValues);
    this.size = materialized.size();
    this.offset = 0;
    this.stride = 1;
    this.source = materialized;
    return this;
  }

  @Override
  public void setFromPA(int index, PrimitiveArray otherPA, int otherIndex) {
    materialize().setFromPA(index, otherPA, otherIndex);
  }

  @Override
  public void remove(int index) {
    removeRange(index, index + 1);
  }

  @Override
  public void removeRange(int from, int to) {
    if (to > size) {
      throw new IllegalArgumentException(
          String2.ERROR + " in PrimitiveView.removeRange: to (" + to + ") > size (" + size + ").");
    }
    if (from >= to) {
      if (from == to) return;
      throw new IllegalArgumentException(
          String2.ERROR + " in PrimitiveView.removeRange: from (" + from + ") > to (" + to + ").");
    }
    int newSize = size - (to - from);
    PrimitiveArray newArray = PrimitiveArray.factory(elementType(), newSize, false);
    for (int i = 0; i < from; i++) {
      newArray.addObject(
          current().toObjectArray() instanceof String[]
              ? current().getString(mapIndex(i))
              : current().getNiceDouble(mapIndex(i)));
    }
    for (int i = to; i < size; i++) {
      newArray.addObject(
          current().toObjectArray() instanceof String[]
              ? current().getString(mapIndex(i))
              : current().getNiceDouble(mapIndex(i)));
    }
    newArray.setMaxIsMV(maxIsMV);
    this.materialized = newArray;
    this.size = newSize;
    this.offset = 0;
    this.stride = 1;
    this.source = materialized;
  }

  @Override
  public void move(int first, int last, int destination) {
    materialize().move(first, last, destination);
  }

  @Override
  public void justKeep(BitSet bitset) {
    int newSize = bitset.cardinality();
    PrimitiveArray newArray = PrimitiveArray.factory(elementType(), newSize, false);
    for (int i = 0; i < size; i++) {
      if (bitset.get(i)) {
        newArray.addObject(
            current().toObjectArray() instanceof String[]
                ? current().getString(mapIndex(i))
                : current().getNiceDouble(mapIndex(i)));
      }
    }
    newArray.setMaxIsMV(maxIsMV);
    this.materialized = newArray;
    this.size = newSize;
    this.offset = 0;
    this.stride = 1;
    this.source = materialized;
  }

  @Override
  public void ensureCapacity(long minCapacity) {
    materialize().ensureCapacity(minCapacity);
  }

  @Override
  public Object toObjectArray() {
    return materialize().toObjectArray();
  }

  @Override
  public double[] toDoubleArray() {
    return materialize().toDoubleArray();
  }

  @Override
  public String[] toStringArray() {
    return materialize().toStringArray();
  }

  // --- Overriding index-based getters ---

  @Override
  public int getInt(int index) {
    return current().getInt(mapIndex(index));
  }

  @Override
  public int getRawInt(int index) {
    return current().getRawInt(mapIndex(index));
  }

  @Override
  public void setInt(int index, int i) {
    materialize().setInt(index, i);
  }

  @Override
  public long getLong(int index) {
    return current().getLong(mapIndex(index));
  }

  @Override
  public void setLong(int index, long i) {
    materialize().setLong(index, i);
  }

  @Override
  public BigInteger getULong(int index) {
    return current().getULong(mapIndex(index));
  }

  @Override
  public void setULong(int index, BigInteger i) {
    materialize().setULong(index, i);
  }

  @Override
  public float getFloat(int index) {
    return current().getFloat(mapIndex(index));
  }

  @Override
  public void setFloat(int index, float d) {
    materialize().setFloat(index, d);
  }

  @Override
  public double getDouble(int index) {
    return current().getDouble(mapIndex(index));
  }

  @Override
  public double getUnsignedDouble(int index) {
    return current().getUnsignedDouble(mapIndex(index));
  }

  @Override
  public double getRawDouble(int index) {
    return current().getRawDouble(mapIndex(index));
  }

  @Override
  public double getNiceDouble(int index) {
    return current().getNiceDouble(mapIndex(index));
  }

  @Override
  public double getRawNiceDouble(int index) {
    return current().getRawNiceDouble(mapIndex(index));
  }

  @Override
  public void setDouble(int index, double d) {
    materialize().setDouble(index, d);
  }

  @Override
  public String getString(int index) {
    return current().getString(mapIndex(index));
  }

  @Override
  public String getJsonString(int index) {
    return current().getJsonString(mapIndex(index));
  }

  @Override
  public String getNccsvDataString(int index) {
    return current().getNccsvDataString(mapIndex(index));
  }

  @Override
  public String getNccsv127DataString(int index) {
    return current().getNccsv127DataString(mapIndex(index));
  }

  @Override
  public String getSVString(int index) {
    return current().getSVString(mapIndex(index));
  }

  @Override
  public String getUtf8TsvString(int index) {
    return current().getUtf8TsvString(mapIndex(index));
  }

  @Override
  public String getRawString(int index) {
    return current().getRawString(mapIndex(index));
  }

  @Override
  public String getRawestString(int index) {
    return current().getRawestString(mapIndex(index));
  }

  @Override
  public void setString(int index, String s) {
    materialize().setString(index, s);
  }

  // --- Abstract overrides for searching / matching ---

  @Override
  public int indexOf(String lookFor, int startIndex) {
    for (int i = startIndex; i < size; i++) {
      if (getString(i).equals(lookFor)) {
        return i;
      }
    }
    return -1;
  }

  @Override
  public int lastIndexOf(String lookFor, int startIndex) {
    for (int i = startIndex; i >= 0; i--) {
      if (getString(i).equals(lookFor)) {
        return i;
      }
    }
    return -1;
  }

  @Override
  public void trimToSize() {
    materialize().trimToSize();
  }

  @Override
  public String toNccsvAttString() {
    StringBuilder sb = new StringBuilder(size * 15);
    for (int i = 0; i < size; i++) {
      sb.append((i == 0 ? "" : ",") + getRawString(i));
    }
    return sb.toString();
  }

  @Override
  public void sort() {
    materialize().sort();
  }

  @Override
  public int compare(int index1, PrimitiveArray otherPA, int index2) {
    return current().compare(mapIndex(index1), otherPA, index2);
  }

  @Override
  public void copy(int from, int to) {
    materialize().copy(from, to);
  }

  @Override
  public void reorder(int rank[]) {
    materialize().reorder(rank);
  }

  @Override
  public void reverseBytes() {
    materialize().reverseBytes();
  }

  @Override
  public long writeToChannel(FileChannel channel) throws Exception {
    return writeToChannel(channel, 0, size);
  }

  @Override
  public long writeToChannel(FileChannel channel, int offset, int length) throws Exception {
    long written = 0;
    for (int i = offset; i < offset + length; i++) {
      // Writing single elements or using a chunk buffer.
      // E.g., leverage current()'s channel write capabilities or serialize via a temporary
      // PrimitiveArray.
    }
    // Simplest correct strategy: materialize first to write cleanly.
    return materialize().writeToChannel(channel, offset, length);
  }

  @Override
  public void readFromChannel(FileChannel channel, int n) throws Exception {
    materialize().readFromChannel(channel, n);
    this.size = materialized.size();
    this.offset = 0;
    this.stride = 1;
    this.source = materialized;
  }

  @Override
  public int writeDos(DataOutputStream dos) throws Exception {
    int bytesPerElem = 0;
    for (int i = 0; i < size; i++) {
      bytesPerElem = current().writeDos(dos, mapIndex(i));
    }
    return size == 0 ? 0 : bytesPerElem;
  }

  @Override
  public int writeDos(DataOutputStream dos, int i) throws Exception {
    return current().writeDos(dos, mapIndex(i));
  }

  @Override
  public void readDis(DataInputStream dis, int n) throws Exception {
    materialize().readDis(dis, n);
    this.size = materialized.size();
    this.offset = 0;
    this.stride = 1;
    this.source = materialized;
  }

  @Override
  public void internalizeFromDODS(DataInputStream dis) throws IOException {
    materialize().internalizeFromDODS(dis);
    this.size = materialized.size();
    this.offset = 0;
    this.stride = 1;
    this.source = materialized;
  }

  @Override
  public void writeToRAF(RandomAccessFile raf, int index) throws Exception {
    current().writeToRAF(raf, mapIndex(index));
  }

  @Override
  public void readFromRAF(RandomAccessFile raf) throws Exception {
    materialize().readFromRAF(raf);
    this.size = materialized.size();
    this.offset = 0;
    this.stride = 1;
    this.source = materialized;
  }

  @Override
  public String testEquals(Object other) {
    if (!(other instanceof PrimitiveArray)) {
      return "The other object is not a PrimitiveArray.";
    }
    PrimitiveArray otherPA = (PrimitiveArray) other;
    if (otherPA.size() != size) {
      return "The sizes are different: " + size + " != " + otherPA.size() + ".";
    }
    for (int i = 0; i < size; i++) {
      String s1 = getString(i);
      String s2 = otherPA.getString(i);
      if (s1 == null && s2 == null) continue;
      if (s1 == null || s2 == null || !s1.equals(s2)) {
        return "Difference at index " + i + ": " + s1 + " != " + s2 + ".";
      }
    }
    return "";
  }

  @Override
  public void append(PrimitiveArray primitiveArray) {
    materialize().append(primitiveArray);
    this.size = materialized.size();
    this.offset = 0;
    this.stride = 1;
    this.source = materialized;
  }

  @Override
  public void rawAppend(PrimitiveArray primitiveArray) {
    materialize().rawAppend(primitiveArray);
    this.size = materialized.size();
    this.offset = 0;
    this.stride = 1;
    this.source = materialized;
  }

  @Override
  public PrimitiveArray makeIndices(IntArray indices) {
    return materialize().makeIndices(indices);
  }

  @Override
  public int switchFromTo(String from, String to) {
    int count = materialize().switchFromTo(from, to);
    this.size = materialized.size();
    this.offset = 0;
    this.stride = 1;
    this.source = materialized;
    return count;
  }

  @Override
  public int firstTie() {
    for (int i = 1; i < size; i++) {
      String s1 = getString(i - 1);
      String s2 = getString(i);
      if (s1 == null && s2 == null) return i - 1;
      if (s1 != null && s1.equals(s2)) return i - 1;
    }
    return -1;
  }

  @Override
  public int[] getNMinMaxIndex() {
    int n = 0, tmini = -1, tmaxi = -1;
    double tmin = Double.MAX_VALUE;
    double tmax = -Double.MAX_VALUE;
    for (int i = 0; i < size; i++) {
      double v = getDouble(i);
      if (Double.isFinite(v)) {
        n++;
        if (v <= tmin) {
          tmini = i;
          tmin = v;
        }
        if (v >= tmax) {
          tmaxi = i;
          tmax = v;
        }
      }
    }
    return new int[] {n, tmini, tmaxi};
  }

  @Override
  public PrimitiveArray subset(PrimitiveArray pa, int startIndex, int stride, int stopIndex) {
    if (pa != null) pa.clear();
    if (startIndex < 0) {
      throw new IndexOutOfBoundsException(
          String2.ERROR
              + " in PrimitiveView.subset: startIndex="
              + startIndex
              + " must be at least 0.");
    }
    if (stride < 1) {
      throw new IllegalArgumentException(
          String2.ERROR
              + " in PrimitiveView.subset: stride="
              + stride
              + " must be greater than 0.");
    }
    if (stopIndex >= size) {
      stopIndex = size - 1;
    }
    if (stopIndex < startIndex) {
      return pa == null ? PrimitiveArray.factory(elementType(), 0, false) : pa;
    }

    int willFind = strideWillFind(stopIndex - startIndex + 1, stride);
    if (pa == null) {
      return new PrimitiveView(this, startIndex, stride, willFind);
    } else {
      pa.ensureCapacity(willFind);
      for (int i = startIndex; i <= stopIndex; i += stride) {
        if (toObjectArray() instanceof String[]) {
          pa.addObject(getString(i));
        } else {
          pa.addObject(getNiceDouble(i));
        }
      }
      return pa;
    }
  }

  @Override
  public PrimitiveArray setMaxIsMV(boolean tMaxIsMV) {
    super.setMaxIsMV(tMaxIsMV);
    if (materialized != null) {
      materialized.setMaxIsMV(tMaxIsMV);
    }
    return this;
  }
}
