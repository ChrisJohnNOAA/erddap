/* This file is part of the EMA project and is
 * Copyright (c) 2005 Robert Simons (CoHortSoftware@gmail.com).
 * See the MIT/X-like license in LICENSE.txt.
 * For more information visit www.cohortsoftware.com or contact CoHortSoftware@gmail.com.
 */
package com.cohort.array;

import com.cohort.util.String2;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.RandomAccessFile;
import java.math.BigInteger;
import java.nio.channels.FileChannel;
import java.util.BitSet;
import ucar.ma2.StructureData;

/**
 * PrimitiveView provides a zero-copy virtual view over a PrimitiveArray.
 */
public class PrimitiveView extends PrimitiveArray {

  public PrimitiveArray source;
  public int offset;
  public int stride;
  public PrimitiveArray materialized;

  /**
   * Constructs a PrimitiveView over a source PrimitiveArray.
   *
   * @param source the source PrimitiveArray
   * @param offset the starting offset in source
   * @param stride the step size between elements
   * @param length the number of elements in this view
   */
  public PrimitiveView(PrimitiveArray source, int offset, int stride, int length) {
    if (source == null) {
      throw new IllegalArgumentException("PrimitiveView source cannot be null.");
    }
    if (offset < 0) {
      throw new IllegalArgumentException("PrimitiveView offset (" + offset + ") must be >= 0.");
    }
    if (stride <= 0) {
      throw new IllegalArgumentException("PrimitiveView stride (" + stride + ") must be > 0.");
    }
    if (length < 0) {
      throw new IllegalArgumentException("PrimitiveView length (" + length + ") must be >= 0.");
    }
    if (offset > source.size()) {
      throw new IndexOutOfBoundsException("PrimitiveView offset (" + offset + ") > source size (" + source.size() + ").");
    }
    if (length > 0 && (offset + (long) (length - 1) * stride) >= source.size()) {
      throw new IndexOutOfBoundsException("PrimitiveView range exceeds source size.");
    }

    while (source instanceof PrimitiveView pv) {
      if (pv.materialized != null) {
        source = pv.materialized;
        break;
      }
      offset = pv.offset + (offset * pv.stride);
      stride = pv.stride * stride;
      source = pv.source;
    }

    this.source = source;
    this.offset = offset;
    this.stride = stride;
    this.size = length;
    this.materialized = null;
  }

  /**
   * Materializes this view into a concrete PrimitiveArray if not already materialized.
   *
   * @return the materialized PrimitiveArray
   */
  public PrimitiveArray materialize() {
    if (materialized == null) {
      PrimitiveArray newArray = PrimitiveArray.factory(elementType(), size, false);
      for (int i = 0; i < size; i++) {
        newArray.addFromPA(source, offset + i * stride, 1);
      }
      materialized = newArray;
    }
    return materialized;
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
  public PAOne MINEST_VALUE() {
    return source.MINEST_VALUE();
  }

  @Override
  public PAOne MAXEST_VALUE() {
    return source.MAXEST_VALUE();
  }

  @Override
  public int capacity() {
    return materialized != null ? materialized.capacity() : size;
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
  public boolean supportsMaxIsMV() {
    return source.supportsMaxIsMV();
  }

  // Getters
  @Override
  public int getInt(int index) {
    return materialized != null ? materialized.getInt(index) : source.getInt(offset + index * stride);
  }

  @Override
  public int getRawInt(int index) {
    return materialized != null ? materialized.getRawInt(index) : source.getRawInt(offset + index * stride);
  }

  @Override
  public long getLong(int index) {
    return materialized != null ? materialized.getLong(index) : source.getLong(offset + index * stride);
  }

  @Override
  public BigInteger getULong(int index) {
    return materialized != null ? materialized.getULong(index) : source.getULong(offset + index * stride);
  }

  @Override
  public float getFloat(int index) {
    return materialized != null ? materialized.getFloat(index) : source.getFloat(offset + index * stride);
  }

  @Override
  public double getDouble(int index) {
    return materialized != null ? materialized.getDouble(index) : source.getDouble(offset + index * stride);
  }

  @Override
  public double getUnsignedDouble(int index) {
    return materialized != null ? materialized.getUnsignedDouble(index) : source.getUnsignedDouble(offset + index * stride);
  }

  @Override
  public double getRawDouble(int index) {
    return materialized != null ? materialized.getRawDouble(index) : source.getRawDouble(offset + index * stride);
  }

  @Override
  public double getNiceDouble(int index) {
    return materialized != null ? materialized.getNiceDouble(index) : source.getNiceDouble(offset + index * stride);
  }

  @Override
  public double getRawNiceDouble(int index) {
    return materialized != null ? materialized.getRawNiceDouble(index) : source.getRawNiceDouble(offset + index * stride);
  }

  @Override
  public String getString(int index) {
    return materialized != null ? materialized.getString(index) : source.getString(offset + index * stride);
  }

  @Override
  public String getJsonString(int index) {
    return materialized != null ? materialized.getJsonString(index) : source.getJsonString(offset + index * stride);
  }

  @Override
  public String getNccsvDataString(int index) {
    return materialized != null ? materialized.getNccsvDataString(index) : source.getNccsvDataString(offset + index * stride);
  }

  @Override
  public String getNccsv127DataString(int index) {
    return materialized != null ? materialized.getNccsv127DataString(index) : source.getNccsv127DataString(offset + index * stride);
  }

  @Override
  public String getSVString(int index) {
    return materialized != null ? materialized.getSVString(index) : source.getSVString(offset + index * stride);
  }

  @Override
  public String getUtf8TsvString(int index) {
    return materialized != null ? materialized.getUtf8TsvString(index) : source.getUtf8TsvString(offset + index * stride);
  }

  @Override
  public String getRawString(int index) {
    return materialized != null ? materialized.getRawString(index) : source.getRawString(offset + index * stride);
  }

  @Override
  public String getRawestString(int index) {
    return materialized != null ? materialized.getRawestString(index) : source.getRawestString(offset + index * stride);
  }

  @Override
  public PAOne getPAOne(int index) {
    return materialized != null ? materialized.getPAOne(index) : source.getPAOne(offset + index * stride);
  }

  @Override
  public PAOne getPAOne(int index, PAOne paOne) {
    return materialized != null ? materialized.getPAOne(index, paOne) : source.getPAOne(offset + index * stride, paOne);
  }

  @Override
  public boolean isMaxValue(int index) {
    return materialized != null ? materialized.isMaxValue(index) : source.isMaxValue(offset + index * stride);
  }

  @Override
  public boolean isMissingValue(int index) {
    return materialized != null ? materialized.isMissingValue(index) : source.isMissingValue(offset + index * stride);
  }

  @Override
  public int indexOf(String lookFor, int startIndex) {
    if (materialized != null) {
      return materialized.indexOf(lookFor, startIndex);
    }
    for (int i = Math.max(0, startIndex); i < size; i++) {
      String s = getString(i);
      if (s == null ? lookFor == null : s.equals(lookFor)) {
        return i;
      }
    }
    return -1;
  }

  @Override
  public int lastIndexOf(String lookFor, int startIndex) {
    if (materialized != null) {
      return materialized.lastIndexOf(lookFor, startIndex);
    }
    for (int i = Math.min(size - 1, startIndex); i >= 0; i--) {
      String s = getString(i);
      if (s == null ? lookFor == null : s.equals(lookFor)) {
        return i;
      }
    }
    return -1;
  }

  // Point Setters & Mutating Methods
  @Override
  public void setInt(int index, int i) {
    materialize().setInt(index, i);
  }

  @Override
  public void setLong(int index, long i) {
    materialize().setLong(index, i);
  }

  @Override
  public void setULong(int index, BigInteger i) {
    materialize().setULong(index, i);
  }

  @Override
  public void setFloat(int index, float d) {
    materialize().setFloat(index, d);
  }

  @Override
  public void setDouble(int index, double d) {
    materialize().setDouble(index, d);
  }

  @Override
  public void setString(int index, String s) {
    materialize().setString(index, s);
  }

  @Override
  public void setPAOne(int index, PAOne paOne) {
    materialize().setPAOne(index, paOne);
  }

  @Override
  public void setFromPA(int index, PrimitiveArray otherPA, int otherIndex) {
    materialize().setFromPA(index, otherPA, otherIndex);
  }

  @Override
  public void atInsertString(int index, String value) {
    materialize().atInsertString(index, value);
    size = materialized.size();
  }

  @Override
  public void addObject(Object value) {
    materialize().addObject(value);
    size = materialized.size();
  }

  @Override
  public void add(StructureData sd, String memberName) {
    materialize().add(sd, memberName);
    size = materialized.size();
  }

  @Override
  public void addNPAOnes(int n, PAOne value) {
    materialize().addNPAOnes(n, value);
    size = materialized.size();
  }

  @Override
  public void addNStrings(int n, String value) {
    materialize().addNStrings(n, value);
    size = materialized.size();
  }

  @Override
  public void addNFloats(int n, float value) {
    materialize().addNFloats(n, value);
    size = materialized.size();
  }

  @Override
  public void addNDoubles(int n, double value) {
    materialize().addNDoubles(n, value);
    size = materialized.size();
  }

  @Override
  public void addNInts(int n, int value) {
    materialize().addNInts(n, value);
    size = materialized.size();
  }

  @Override
  public void addNLongs(int n, long value) {
    materialize().addNLongs(n, value);
    size = materialized.size();
  }

  @Override
  public PrimitiveArray addFromPA(PrimitiveArray otherPA, int otherIndex, int nValues) {
    materialize().addFromPA(otherPA, otherIndex, nValues);
    size = materialized.size();
    return this;
  }

  @Override
  public void append(PrimitiveArray primitiveArray) {
    materialize().append(primitiveArray);
    size = materialized.size();
  }

  @Override
  public void rawAppend(PrimitiveArray primitiveArray) {
    materialize().rawAppend(primitiveArray);
    size = materialized.size();
  }

  @Override
  public void copy(int from, int to) {
    materialize().copy(from, to);
  }

  @Override
  public void move(int first, int last, int destination) {
    materialize().move(first, last, destination);
  }

  @Override
  public void sort() {
    materialize().sort();
  }

  @Override
  public void sortIgnoreCase() {
    materialize().sortIgnoreCase();
  }

  @Override
  public void reverseBytes() {
    materialize().reverseBytes();
  }

  @Override
  public void reorder(int[] rank) {
    materialize().reorder(rank);
  }

  @Override
  public void trimToSize() {
    materialize().trimToSize();
  }

  @Override
  public void ensureCapacity(long minCapacity) {
    materialize().ensureCapacity(minCapacity);
  }

  // Optimized Mutators (Rule 5)
  @Override
  public void remove(int index) {
    if (index < 0 || index >= size) {
      throw new IllegalArgumentException(String2.ERROR + " in PrimitiveView.remove: index (" + index + ") >= size (" + size + ").");
    }
    int targetSize = size - 1;
    PrimitiveArray target = PrimitiveArray.factory(elementType(), targetSize, false);
    if (materialized != null) {
      if (index > 0) target.addFromPA(materialized, 0, index);
      if (index < targetSize) target.addFromPA(materialized, index + 1, targetSize - index);
    } else {
      for (int i = 0; i < index; i++) {
        target.addFromPA(source, offset + i * stride, 1);
      }
      for (int i = index + 1; i < size; i++) {
        target.addFromPA(source, offset + i * stride, 1);
      }
    }
    materialized = target;
    size = targetSize;
    offset = 0;
    stride = 1;
  }

  @Override
  public void removeRange(int from, int to) {
    if (from < 0 || to < from || to > size) {
      throw new IllegalArgumentException("Invalid range in removeRange: from=" + from + ", to=" + to + ", size=" + size);
    }
    int numToRemove = to - from;
    if (numToRemove == 0) return;
    int targetSize = size - numToRemove;
    PrimitiveArray target = PrimitiveArray.factory(elementType(), targetSize, false);
    if (materialized != null) {
      if (from > 0) target.addFromPA(materialized, 0, from);
      if (to < size) target.addFromPA(materialized, to, size - to);
    } else {
      for (int i = 0; i < from; i++) {
        target.addFromPA(source, offset + i * stride, 1);
      }
      for (int i = to; i < size; i++) {
        target.addFromPA(source, offset + i * stride, 1);
      }
    }
    materialized = target;
    size = targetSize;
    offset = 0;
    stride = 1;
  }

  @Override
  public void justKeep(BitSet bitset) {
    int keepCount = 0;
    for (int i = bitset.nextSetBit(0); i >= 0 && i < size; i = bitset.nextSetBit(i + 1)) {
      keepCount++;
    }
    PrimitiveArray target = PrimitiveArray.factory(elementType(), keepCount, false);
    if (materialized != null) {
      for (int i = bitset.nextSetBit(0); i >= 0 && i < size; i = bitset.nextSetBit(i + 1)) {
        target.addFromPA(materialized, i, 1);
      }
    } else {
      for (int i = bitset.nextSetBit(0); i >= 0 && i < size; i = bitset.nextSetBit(i + 1)) {
        target.addFromPA(source, offset + i * stride, 1);
      }
    }
    materialized = target;
    size = keepCount;
    offset = 0;
    stride = 1;
  }

  // State Re-Initialization (Rule 6)
  @Override
  public void clear() {
    super.clear();
    materialized = null;
    offset = 0;
    stride = 1;
  }

  // Subsets
  @Override
  public PrimitiveArray subset(PrimitiveArray pa, int startIndex, int stride, int stopIndex) {
    if (startIndex < 0) {
      throw new IllegalArgumentException(String2.ERROR + " in PrimitiveView.subset: startIndex=" + startIndex + " must be at least 0.");
    }
    if (stride < 1) {
      throw new IllegalArgumentException(String2.ERROR + " in PrimitiveView.subset: stride=" + stride + " must be greater than 0.");
    }
    if (stopIndex < startIndex) {
      if (pa == null) return new PrimitiveView(this, 0, 1, 0);
      pa.clear();
      return pa;
    }
    int effectiveStop = Math.min(stopIndex, size - 1);
    int subLength = PrimitiveArray.strideWillFind(effectiveStop - startIndex + 1, stride);
    if (pa == null) {
      return new PrimitiveView(this, startIndex, stride, subLength);
    }
    pa.clear();
    for (int i = 0; i < subLength; i++) {
      pa.addFromPA(this, startIndex + i * stride, 1);
    }
    return pa;
  }

  // Formatters (Rule 7)
  @Override
  public String toString() {
    return materialize().toString();
  }

  @Override
  public String toCSVString() {
    return materialize().toCSVString();
  }

  @Override
  public String toNccsvAttString() {
    return materialize().toNccsvAttString();
  }

  @Override
  public String toNccsv127AttString() {
    return materialize().toNccsv127AttString();
  }

  @Override
  public String toJsonCsvString() {
    return materialize().toJsonCsvString();
  }

  // Array conversion methods
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

  // Comparison
  @Override
  public int compare(int index1, PrimitiveArray otherPA, int index2) {
    if (materialized != null) {
      return materialized.compare(index1, otherPA, index2);
    }
    return source.compare(offset + index1 * stride, otherPA, index2);
  }

  // Write I/O Methods
  @Override
  public long writeToChannel(FileChannel channel) throws Exception {
    return writeToChannel(channel, 0, size);
  }

  @Override
  public long writeToChannel(FileChannel channel, int off, int len) throws Exception {
    if (materialized != null) {
      return materialized.writeToChannel(channel, off, len);
    }
    if (stride == 1) {
      return source.writeToChannel(channel, offset + off, len);
    }
    return materialize().writeToChannel(channel, off, len);
  }

  @Override
  public int writeDos(DataOutputStream dos) throws Exception {
    if (materialized != null) {
      return materialized.writeDos(dos);
    }
    int bytesWritten = 0;
    for (int i = 0; i < size; i++) {
      bytesWritten += writeDos(dos, i);
    }
    return bytesWritten;
  }

  @Override
  public int writeDos(DataOutputStream dos, int i) throws Exception {
    if (materialized != null) {
      return materialized.writeDos(dos, i);
    }
    return source.writeDos(dos, offset + i * stride);
  }

  @Override
  public void writeToRAF(RandomAccessFile raf, int index) throws Exception {
    if (materialized != null) {
      materialized.writeToRAF(raf, index);
    } else {
      source.writeToRAF(raf, offset + index * stride);
    }
  }

  @Override
  public void externalizeForDODS(DataOutputStream dos) throws Exception {
    if (materialized != null) {
      materialized.externalizeForDODS(dos);
    } else {
      dos.writeInt(size);
      dos.writeInt(size);
      writeDos(dos);
    }
  }

  @Override
  public void externalizeForDODS(DataOutputStream dos, int i) throws Exception {
    if (materialized != null) {
      materialized.externalizeForDODS(dos, i);
    } else {
      source.externalizeForDODS(dos, offset + i * stride);
    }
  }

  // Read I/O Methods (Materialize then delegate)
  @Override
  public void readFromChannel(FileChannel channel, int n) throws Exception {
    materialize().readFromChannel(channel, n);
    size = materialized.size();
  }

  @Override
  public void readDis(DataInputStream dis, int n) throws Exception {
    materialize().readDis(dis, n);
    size = materialized.size();
  }

  @Override
  public void readFromRAF(RandomAccessFile raf) throws Exception {
    materialize().readFromRAF(raf);
    size = materialized.size();
  }

  @Override
  public void internalizeFromDODS(DataInputStream dis) throws java.io.IOException {
    materialize().internalizeFromDODS(dis);
    size = materialized.size();
  }

  // Analysis / Helper Methods
  @Override
  public PrimitiveArray makeIndices(IntArray indices) {
    return materialize().makeIndices(indices);
  }

  @Override
  public int switchFromTo(String from, String to) {
    return materialize().switchFromTo(from, to);
  }

  @Override
  public int firstTie() {
    if (materialized != null) {
      return materialized.firstTie();
    }
    for (int i = 1; i < size; i++) {
      if (compare(i - 1, i) == 0) {
        return i - 1;
      }
    }
    return -1;
  }

  @Override
  public int[] getNMinMaxIndex() {
    if (materialized != null) {
      return materialized.getNMinMaxIndex();
    }
    int nValid = 0;
    int minIndex = -1;
    int maxIndex = -1;

    for (int i = 0; i < size; i++) {
      if (isMissingValue(i)) continue;
      nValid++;
      if (minIndex == -1) {
        minIndex = i;
        maxIndex = i;
      } else {
        if (compare(i, minIndex) < 0) minIndex = i;
        if (compare(i, maxIndex) > 0) maxIndex = i;
      }
    }
    return new int[] {nValid, minIndex, maxIndex};
  }

  @Override
  public String testEquals(Object other) {
    if (this == other) return "";
    if (other == null || !(other instanceof PrimitiveArray otherPA)) {
      return "The other object is null or not a PrimitiveArray.";
    }
    if (elementType() != otherPA.elementType()) {
      return "The other PrimitiveArray is a different type (" + otherPA.elementType() + " != " + elementType() + ").";
    }
    if (size != otherPA.size()) {
      return String2.ERROR + " in PrimitiveView.testEquals: size (" + size + ") != other.size (" + otherPA.size() + ").";
    }
    for (int i = 0; i < size; i++) {
      if (compare(i, otherPA, i) != 0) {
        return "The PrimitiveArrays differ at [" + i + "] (" + getRawestString(i) + " != " + otherPA.getRawestString(i) + ").";
      }
    }
    return "";
  }
}
