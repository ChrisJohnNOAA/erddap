/* This file is part of the EMA project and is
 * Copyright (c) 2005 Robert Simons (CoHortSoftware@gmail.com).
 * See the MIT/X-like license in LICENSE.txt.
 * For more information visit www.cohortsoftware.com or contact CoHortSoftware@gmail.com.
 */
package com.cohort.array;

import com.cohort.util.Math2;
import com.cohort.util.String2;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.math.BigInteger;
import java.text.MessageFormat;
import java.util.Arrays;
import ucar.ma2.StructureData;

/**
 * LongArray is a thin shell over a long[] with methods like ArrayList's methods; it extends
 * PrimitiveArray.
 *
 * <p>This class uses maxIsMV=true and Long.MAX_VALUE to represent a missing value (NaN).
 */
public class LongArray extends PrimitiveArray {

  private static final java.lang.foreign.ValueLayout.OfLong LAYOUT =
      java.lang.foreign.ValueLayout.JAVA_LONG.withOrder(java.nio.ByteOrder.nativeOrder());

  /**
   * This is the main data structure. This should be private, but is public so you can manipulate it
   * if you promise to be careful. Note that if the PrimitiveArray's capacity is increased, the
   * PrimitiveArray will use a different array for storage.
   */
  public java.lang.foreign.MemorySegment array;

  private long[] wrappedArray;

  public long getArrayVal(final int i) {
    return array.getAtIndex(LAYOUT, i);
  }

  public void setArrayVal(final int i, final long val) {
    array.setAtIndex(LAYOUT, i, val);
  }

  /**
   * This indicates if this class' type (e.g., PAType.SHORT) is an integer (in the math sense) type.
   * The integer type classes overwrite this.
   */
  @Override
  public final boolean isIntegerType() {
    return true;
  }

  /**
   * This returns the number of bytes per element for this PrimitiveArray. The value for "String"
   * isn't a constant, so this returns 20.
   *
   * @return the number of bytes per element for this PrimitiveArray. The value for "String" isn't a
   *     constant, so this returns 20.
   */
  @Override
  public final int elementSize() {
    return 8;
  }

  /**
   * This returns for cohort missing value for this class (e.g., Integer.MAX_VALUE), expressed as a
   * double. FloatArray and StringArray return Double.NaN.
   */
  @Override
  public final double missingValueAsDouble() {
    return Long.MAX_VALUE;
  }

  /**
   * This tests if the value at the specified index equals the data type's MAX_VALUE (for
   * integerTypes, which may or may not indicate a missing value, depending on maxIsMV), NaN (for
   * Float and Double), \\uffff (for CharArray), or "" (for StringArray).
   *
   * @param index The index in question
   * @return true if the value is a missing value.
   */
  @Override
  public final boolean isMaxValue(final int index) {
    return get(index) == Long.MAX_VALUE;
  }

  /**
   * This tests if the value at the specified index is a missing value. For integerTypes,
   * isMissingValue can only be true if maxIsMv is 'true'.
   *
   * @param index The index in question
   * @return true if the value is a missing value.
   */
  @Override
  public final boolean isMissingValue(final int index) {
    return maxIsMV && isMaxValue(index);
  }

  /** A constructor for a capacity of 8 elements. The initial 'size' will be 0. */
  public LongArray() {
    wrappedArray = new long[8];
    array = java.lang.foreign.MemorySegment.ofArray(wrappedArray);
  }

  /**
   * This constructs a LongArray by copying elements from the incoming PrimitiveArray (using
   * append()).
   *
   * @param primitiveArray a primitiveArray of any type
   */
  public LongArray(final PrimitiveArray primitiveArray) {
    Math2.ensureMemoryAvailable(8L * primitiveArray.size(), "LongArray");
    wrappedArray = new long[primitiveArray.size()];
    array = java.lang.foreign.MemorySegment.ofArray(wrappedArray); // exact size
    append(primitiveArray);
  }

  /**
   * A constructor for a specified number of elements. The initial 'size' will be 0.
   *
   * @param capacity creates an LongArray with the specified initial capacity.
   * @param active if true, size will be set to capacity and all elements will equal 0; else size =
   *     0.
   */
  public LongArray(final int capacity, final boolean active) {
    Math2.ensureMemoryAvailable(8L * capacity, "LongArray");
    wrappedArray = new long[capacity];
    array = java.lang.foreign.MemorySegment.ofArray(wrappedArray);
    if (active) size = capacity;
  }

  /**
   * A constructor which (at least initially) uses the array and all its elements ('size' will equal
   * anArray.length).
   *
   * @param anArray the array to be used as this object's array.
   */
  public LongArray(final long[] anArray) {
    if (anArray == null) {
      wrappedArray = new long[0];
      array = java.lang.foreign.MemorySegment.ofArray(wrappedArray);
      size = 0;
    } else {
      wrappedArray = anArray;
      array = java.lang.foreign.MemorySegment.ofArray(anArray);
      size = anArray.length;
    }
  }

  /**
   * This makes a LongArray from the comma-separated values. <br>
   * null becomes pa.length() == 0. <br>
   * "" becomes pa.length() == 0. <br>
   * " " becomes pa.length() == 1. <br>
   * See also PrimitiveArray.csvFactory(paType, csv);
   *
   * @param csv the comma-separated-value string
   * @return a LongArray from the comma-separated values.
   */
  public static final LongArray fromCSV(final String csv) {
    return (LongArray) PrimitiveArray.csvFactory(PAType.LONG, csv);
  }

  /**
   * This returns a new PAOne with the minimum value that can be held by this class.
   *
   * @return a new PAOne with the minimum value that can be held by this class, e.g., -128b for
   *     ByteArray.
   */
  @Override
  public final PAOne MINEST_VALUE() {
    return PAOne.fromLong(Long.MIN_VALUE);
  }

  /**
   * This returns a new PAOne with the maximum value that can be held by this class (not including
   * the cohort missing value).
   *
   * @return a new PAOne with the maximum value that can be held by this class, e.g., 126 for
   *     ByteArray.
   */
  @Override
  public final PAOne MAXEST_VALUE() {
    return PAOne.fromLong(Long.MAX_VALUE - 1);
  }

  /**
   * This returns the current capacity (number of elements) of the internal data array.
   *
   * @return the current capacity (number of elements) of the internal data array.
   */
  @Override
  public final int capacity() {
    return (int) (array.byteSize() / 8);
  }

  /**
   * This returns the hashcode for this LongArray (dependent only on values, not capacity). WARNING:
   * the algorithm used may change in future versions.
   *
   * @return the hashcode for this LongArray (dependent only on values, not capacity)
   */
  @Override
  public int hashCode() {
    // see
    // https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/List.html#hashCode()
    // and
    // https://stackoverflow.com/questions/299304/why-does-javas-hashcode-in-string-use-31-as-a-multiplier
    // and java docs for Long.hashCode()
    int code = 0;
    if (wrappedArray != null) {
      for (int i = 0; i < size; i++) code = 31 * code + Long.hashCode(wrappedArray[i]);
    } else {
      for (int i = 0; i < size; i++) code = 31 * code + Long.hashCode(getArrayVal(i));
    }
    return code;
  }

  /**
   * This makes a new subset of this PrimitiveArray based on startIndex, stride, and stopIndex.
   *
   * @param pa the pa to be filled (may be null). If not null, must be of same type as this class.
   * @param startIndex must be a valid index
   * @param stride must be at least 1
   * @param stopIndex (inclusive) If &gt;= size, it will be changed to size-1.
   * @return The same pa (or a new PrimitiveArray if it was null) with the desired subset. If new,
   *     it will have a backing array with a capacity equal to its size. If stopIndex &lt;
   *     startIndex, this returns PrimitiveArray with size=0;
   */
  @Override
  public PrimitiveArray subset(
      final PrimitiveArray pa, final int startIndex, final int stride, int stopIndex) {
    if (pa != null) pa.clear();
    if (startIndex < 0)
      throw new IndexOutOfBoundsException(
          MessageFormat.format(ArraySubsetStart, getClass().getSimpleName(), "" + startIndex));
    if (stride < 1)
      throw new IllegalArgumentException(
          MessageFormat.format(ArraySubsetStride, getClass().getSimpleName(), "" + stride));
    if (stopIndex >= size) stopIndex = size - 1;
    if (stopIndex < startIndex) return pa == null ? new LongArray(new long[0]) : pa;

    int willFind = strideWillFind(stopIndex - startIndex + 1, stride);
    LongArray da = null;
    if (pa == null) {
      da = new LongArray(willFind, true);
    } else {
      da = (LongArray) pa;
      da.ensureCapacity(willFind);
      da.size = willFind;
    }
    da.setMaxIsMV(maxIsMV);
    if (stride == 1) {
      PanamaHelper.copyElements(
          wrappedArray, array, startIndex, da.wrappedArray, da.array, 0, willFind, 8);
    } else {
      int po = 0;
      for (int i = startIndex; i <= stopIndex; i += stride) {
        da.setArrayVal(po++, getArrayVal(i));
      }
    }
    return da;
  }

  /**
   * This returns the PAType (PAType.LONG) of the element type.
   *
   * @return the PAType (PAType.LONG) of the element type.
   */
  @Override
  public PAType elementType() {
    return PAType.LONG;
  }

  /**
   * This returns the minimum PAType needed to completely and precisely contain the values in this
   * PA's PAType and tPAType (e.g., when merging two PrimitiveArrays).
   *
   * @return the minimum PAType needed to completely and precisely contain the values in this PA's
   *     PAType and tPAType (e.g., when merging two PrimitiveArrays).
   */
  @Override
  public PAType needPAType(PAType tPAType) {
    return switch (tPAType) {
      // if tPAType is smaller or same, return this.PAType
      case BYTE, UBYTE, SHORT, USHORT, INT, UINT, LONG -> PAType.LONG;

      // if sideways.   ULONG, FLOAT, DOUBLE, CHAR, STRING
      default -> PAType.STRING;
    };
  }

  /**
   * This adds an item to the array (increasing 'size' by 1).
   *
   * @param value the value to be added to the array
   */
  public final void add(final long value) {
    if (size == capacity()) // if we're at capacity
    ensureCapacity(size + 1L);
    setArrayVal(size++, value);
  }

  /**
   * This adds an item to the array (increasing 'size' by 1).
   *
   * @param value the value to be added to the array. If value instanceof Number, this uses
   *     Number.longValue(). If null or not a Number, this adds Long.MAX_VALUE.
   */
  @Override
  public final void addObject(final Object value) {
    if (value instanceof Number num) {
      if (value instanceof Double) addDouble(num.doubleValue()); // supports NaN
      else if (value instanceof Float) addFloat(num.floatValue()); // supports NaN
      else add(num.longValue());
    } else {
      addDouble(Double.NaN);
    }
  }

  /**
   * This reads one value from the StrutureData and adds it to this PA.
   *
   * @param sd from an .nc file
   * @param memberName
   */
  @Override
  public void add(final StructureData sd, final String memberName) {
    add(sd.getScalarLong(memberName));
  }

  /**
   * This adds all the values from ar.
   *
   * @param ar an array
   */
  public final void add(final long ar[]) {
    final int arSize = ar.length;
    ensureCapacity(size + (long) arSize);
    if (wrappedArray != null) {
      System.arraycopy(ar, 0, wrappedArray, size, arSize);
    } else {
      java.lang.foreign.MemorySegment.copy(
          java.lang.foreign.MemorySegment.ofArray(ar), (0) * 8L, array, (size) * 8L, (arSize) * 8L);
    }
    size += arSize;
  }

  /**
   * This adds n copies of value to the array (increasing 'size' by n).
   *
   * @param n if less than 0, this throws Exception
   * @param value the value to be added to the array. n &lt; 0 throws an Exception.
   */
  public final void addN(final int n, final long value) {
    if (n == 0) return;
    if (n < 0)
      throw new IllegalArgumentException(
          MessageFormat.format(ArrayAddN, getClass().getSimpleName(), "" + n));
    ensureCapacity(size + (long) n);
    if (wrappedArray != null) {
      Arrays.fill(wrappedArray, size, size + n, value);
    } else {
      for (int i = size; i < size + n; i++) {
        array.setAtIndex(LAYOUT, i, value);
      }
    }
    size += n;
  }

  /**
   * This inserts an item into the array at the specified index, pushing subsequent items to
   * oldIndex+1 and increasing 'size' by 1.
   *
   * @param index the position where the value should be inserted.
   * @param value the value to be inserted into the array
   */
  public void atInsert(final int index, final long value) {
    if (index < 0 || index > size)
      throw new IllegalArgumentException(
          MessageFormat.format(ArrayAtInsert, getClass().getSimpleName(), "" + index, "" + size));
    if (size == capacity()) // if we're at capacity
    ensureCapacity(size + 1L);
    if (wrappedArray != null) {
      System.arraycopy(wrappedArray, index, wrappedArray, index + 1, size - index);
    } else {
      java.lang.foreign.MemorySegment.copy(
          array, (index) * 8L, array, (index + 1) * 8L, (size - index) * 8L);
    }
    size++;
    setArrayVal(index, value);
  }

  /**
   * This inserts an item into the array at the specified index, pushing subsequent items to
   * oldIndex+1 and increasing 'size' by 1.
   *
   * @param index 0..
   * @param value the value, as a String.
   */
  @Override
  public void atInsertString(final int index, final String value) {
    final double d = String2.parseDouble(value); // supports NaN.   Parse with greater range
    if (Double.isNaN(d) || d < Long.MIN_VALUE || d > Long.MAX_VALUE) maxIsMV = true;
    atInsert(index, String2.parseLong(value)); // re-parse with greater precision
  }

  /**
   * This adds n PAOne's to the array.
   *
   * @param n the number of times 'value' should be added. If less than 0, this throws Exception.
   * @param value the value, as a PAOne (or null).
   */
  @Override
  public void addNPAOnes(final int n, final PAOne value) {
    final double d = value == null ? Double.NaN : value.getDouble(); // with greater range
    if (Double.isNaN(d) || d < Long.MIN_VALUE || d > Long.MAX_VALUE) addNDoubles(n, Double.NaN);
    else addN(n, value.getLong());
  }

  /**
   * This adds n Strings to the array.
   *
   * @param n the number of times 'value' should be added. If less than 0, this throws Exception.
   * @param value the value, as a String.
   */
  @Override
  public final void addNStrings(final int n, final String value) {
    final double d = String2.parseDouble(value); // with greater range
    if (Double.isNaN(d) || d < Long.MIN_VALUE || d > Long.MAX_VALUE) addNDoubles(n, Double.NaN);
    else addN(n, String2.parseLong(value));
  }

  /**
   * This adds n floats to the array.
   *
   * @param n the number of times 'value' should be added. If less than 0, this throws Exception.
   * @param value the value, as a float.
   */
  @Override
  public final void addNFloats(final int n, final float value) {
    if (!maxIsMV && (!Float.isFinite(value) || value < Long.MIN_VALUE || value > Long.MAX_VALUE))
      maxIsMV = true;
    addN(n, Math2.roundToLong(value));
  }

  /**
   * This adds n doubles to the array.
   *
   * @param n the number of times 'value' should be added. If less than 0, this throws Exception.
   * @param value the value, as a double.
   */
  @Override
  public final void addNDoubles(final int n, final double value) {
    if (!maxIsMV && (!Double.isFinite(value) || value < Long.MIN_VALUE || value > Long.MAX_VALUE))
      maxIsMV = true;
    addN(n, Math2.roundToLong(value));
  }

  /**
   * This adds n ints to the array.
   *
   * @param n the number of times 'value' should be added
   * @param value the value, as an int.
   */
  @Override
  public final void addNInts(final int n, final int value) {
    addN(n, value); // !!! assumes value=Integer.MAX_VALUE isn't maxIsMV
  }

  /**
   * This adds n longs to the array.
   *
   * @param n the number of times 'value' should be added
   * @param value the value, as an int.
   */
  @Override
  public final void addNLongs(final int n, final long value) {
    addN(n, value);
  }

  /**
   * This adds an element from another PrimitiveArray.
   *
   * @param otherPA the source PA
   * @param otherIndex the start index in otherPA
   * @param nValues the number of values to be added
   * @return 'this' for convenience
   */
  @Override
  public PrimitiveArray addFromPA(final PrimitiveArray otherPA, int otherIndex, final int nValues) {

    // add from same type
    if (otherPA.elementType() == elementType()) {
      if (otherIndex + nValues > otherPA.size)
        throw new IllegalArgumentException(
            String2.ERROR
                + " in LongArray.addFromPA: otherIndex="
                + otherIndex
                + " + nValues="
                + nValues
                + " > otherPA.size="
                + otherPA.size);
      ensureCapacity(size + nValues);
      {
        LongArray oPA = (LongArray) ((LongArray) otherPA);
        PanamaHelper.copyElements(
            oPA.wrappedArray, oPA.array, otherIndex, wrappedArray, array, size, nValues, 8);
      }
      size += nValues;
      if (otherPA.getMaxIsMV()) maxIsMV = true;
      return this;
    }

    // add from different type
    for (int i = 0; i < nValues; i++) {
      if (!maxIsMV && Double.isNaN(otherPA.getDouble(otherIndex))) maxIsMV = true;
      add(otherPA.getLong(otherIndex++)); // does error checking
    }
    return this;
  }

  /**
   * This sets an element from another PrimitiveArray.
   *
   * @param index the index to be set
   * @param otherPA the other PrimitiveArray
   * @param otherIndex the index of the item in otherPA
   */
  @Override
  public void setFromPA(final int index, final PrimitiveArray otherPA, final int otherIndex) {
    final double d = otherPA.getDouble(otherIndex); // has greater range and NaN
    if (Double.isNaN(d)) maxIsMV = true;
    set(index, otherPA.getLong(otherIndex));
  }

  /**
   * This removes the specified element.
   *
   * @param index the element to be removed, 0 ... size-1
   */
  @Override
  public void remove(final int index) {
    if (index >= size)
      throw new IllegalArgumentException(
          MessageFormat.format(ArrayRemove, getClass().getSimpleName(), "" + index, "" + size));
    PanamaHelper.remove(index, 8, size, wrappedArray, array);
    size--;
  }

  /**
   * This removes the specified range of elements.
   *
   * @param from the first element to be removed, 0 ... size
   * @param to one after the last element to be removed, from ... size
   */
  @Override
  public void removeRange(final int from, final int to) {
    if (to > size)
      throw new IllegalArgumentException(
          String2.ERROR + " in LongArray.removeRange: to (" + to + ") > size (" + size + ").");
    if (from >= to) {
      if (from == to) return;
      throw new IllegalArgumentException(
          String2.ERROR + " in LongArray.removeRange: from (" + from + ") > to (" + to + ").");
    }
    PanamaHelper.removeRange(from, to, 8, size, wrappedArray, array);
    size -= to - from;
  }

  /**
   * Moves elements 'first' through 'last' (inclusive) to 'destination'.
   *
   * @param first the first to be move
   * @param last (exclusive)
   * @param destination the destination, can't be in the range 'first+1..last-1'.
   */
  @Override
  public void move(final int first, final int last, final int destination) {
    final String errorIn = String2.ERROR + " in LongArray.move:\n";

    if (first < 0) throw new RuntimeException(errorIn + "first (" + first + ") must be >= 0.");
    if (last < first || last > size)
      throw new RuntimeException(
          errorIn
              + "last ("
              + last
              + ") must be >= first ("
              + first
              + ") and <= size ("
              + size
              + ").");
    if (destination < 0 || destination > size)
      throw new RuntimeException(
          errorIn + "destination (" + destination + ") must be between 0 and size (" + size + ").");
    if (destination > first && destination < last)
      throw new RuntimeException(
          errorIn
              + "destination ("
              + destination
              + ") must be <= first ("
              + first
              + ") or >= last ("
              + last
              + ").");
    PanamaHelper.move(first, last, destination, 8, size, wrappedArray, array);
  }

  /**
   * This ensures that the capacity is at least 'minCapacity'.
   *
   * @param minCapacity the minimum acceptable capacity. minCapacity is type long, but &gt;=
   *     Integer.MAX_VALUE will throw exception.
   */
  @Override
  public void ensureCapacity(final long minCapacity) {
    long currentCapacity = array.byteSize() / 8;
    if (currentCapacity < minCapacity) {
      int newCapacity =
          PanamaHelper.calculateNewCapacity(currentCapacity, minCapacity, "LongArray");
      Math2.ensureMemoryAvailable(8L * newCapacity, "LongArray");
      long[] newArray = new long[newCapacity];
      java.lang.foreign.MemorySegment newSegment =
          java.lang.foreign.MemorySegment.ofArray(newArray);
      java.lang.foreign.MemorySegment.copy(array, 0, newSegment, 0, size * 8L);
      array = newSegment;
      wrappedArray = newArray;
    }
  }

  /**
   * This returns an array (perhaps 'array') which has 'size' elements.
   *
   * @return an array (perhaps 'array') which has 'size' elements. Unsigned integer types will
   *     return an array with their storage type e.g., ULongArray returns a long[].
   */
  public long[] toArray() {
    if (size == (int) (array.byteSize() / 8) && wrappedArray != null) return wrappedArray;
    Math2.ensureMemoryAvailable(8L * size, "LongArray.toArray");
    if (wrappedArray != null) {
      return Arrays.copyOfRange(wrappedArray, 0, size);
    }
    return array.asSlice(0, size * 8L).toArray(LAYOUT);
  }

  /**
   * This returns a primitive[] (perhaps 'array') which has 'size' elements.
   *
   * @return a primitive[] (perhaps 'array') which has 'size' elements. Unsigned integer types will
   *     return an array with their storage type e.g., ULongArray returns a long[].
   */
  @Override
  public Object toObjectArray() {
    return toArray();
  }

  /**
   * This returns a double[] (perhaps 'array') which has 'size' elements.
   *
   * @return a double[] (perhaps 'array') which has 'size' elements. If maxIsMV, Long.MAX_VALUE is
   *     converted to Double.NaN.
   */
  @Override
  public double[] toDoubleArray() {
    Math2.ensureMemoryAvailable(8L * size, "LongArray.toDoubleArray");
    final double dar[] = new double[size];
    for (int i = 0; i < size; i++) {
      final long j = getArrayVal(i);
      dar[i] = maxIsMV && j == Long.MAX_VALUE ? Double.NaN : j;
    }
    return dar;
  }

  /**
   * This returns a String[] which has 'size' elements.
   *
   * @return a String[] which has 'size' elements. Long.MAX_VALUE appears as "".
   */
  @Override
  public String[] toStringArray() {
    Math2.ensureMemoryAvailable(
        12L * size, "LongArray.toStringArray"); // 12L is feeble minimal estimate
    final String sar[] = new String[size];
    for (int i = 0; i < size; i++) {
      final long tl = getArrayVal(i);
      sar[i] = maxIsMV && tl == Long.MAX_VALUE ? "" : String.valueOf(tl);
    }
    return sar;
  }

  /**
   * This gets a specified element.
   *
   * @param index 0 ... size-1
   * @return the specified element
   */
  public long get(final int index) {
    if (index >= size)
      throw new IllegalArgumentException(
          String2.ERROR + " in LongArray.get: index (" + index + ") >= size (" + size + ").");
    return array.getAtIndex(LAYOUT, index);
  }

  /**
   * This sets a specified element.
   *
   * @param index 0 ... size-1
   * @param value the value for that element
   */
  public void set(final int index, final long value) {
    if (index >= size)
      throw new IllegalArgumentException(
          String2.ERROR + " in LongArray.set: index (" + index + ") >= size (" + size + ").");
    array.setAtIndex(LAYOUT, index, value);
  }

  /**
   * Return a value from the array as an int.
   *
   * @param index the index number 0 ... size-1
   * @return the value as an int. This may return Integer.MAX_VALUE.
   */
  @Override
  public int getInt(final int index) {
    return Math2.roundToInt((double) get(index));
  }

  // getRawInt(index) uses default getInt(index) since missingValue is bigger than int.

  /**
   * Set a value in the array as an int.
   *
   * @param index the index number 0 .. size-1
   * @param i the value. Integer.MAX_VALUE is NOT converted to this type's missing value.
   */
  @Override
  public void setInt(final int index, final int i) {
    set(index, i);
  }

  /**
   * Return a value from the array as a long.
   *
   * @param index the index number 0 ... size-1
   * @return the value as a long.
   */
  @Override
  public long getLong(final int index) {
    return get(index);
  }

  /**
   * Set a value in the array as a long.
   *
   * @param index the index number 0 .. size-1
   * @param i the value.
   */
  @Override
  public void setLong(final int index, final long i) {
    set(index, i);
  }

  /**
   * Return a value from the array as a ulong.
   *
   * @param index the index number 0 ... size-1
   * @return the value as a ulong. If maxIsMV, MAX_VALUE is returned as null.
   */
  @Override
  public BigInteger getULong(final int index) {
    final long b = get(index);
    return maxIsMV && b == Long.MAX_VALUE ? null : new BigInteger("" + b);
  }

  /**
   * Set a value in the array as a ulong.
   *
   * @param index the index number 0 .. size-1
   * @param i the value. For numeric PrimitiveArray's, it is narrowed if needed by methods like
   *     Math2.narrowToByte(long).
   */
  @Override
  public void setULong(final int index, final BigInteger i) {
    final double d = i == null ? Double.NaN : i.doubleValue(); // wide range. handles out of range.
    if (!Double.isFinite(d) || d < Long.MIN_VALUE || d > Long.MAX_VALUE) {
      maxIsMV = true;
      set(index, Long.MAX_VALUE);
    } else {
      set(index, i.longValue());
    }
  }

  /**
   * Return a value from the array as a float.
   *
   * @param index the index number 0 .. size-1
   * @return the value as a float. String values are parsed with String2.parseFloat and so may
   *     return Float.NaN. If maxIsMV, Long.MAX_VALUE is returned as Float.NaN.
   */
  @Override
  public float getFloat(final int index) {
    final long tl = get(index);
    return maxIsMV && tl == Long.MAX_VALUE ? Float.NaN : tl;
  }

  /**
   * Set a value in the array as a float.
   *
   * @param index the index number 0 .. size-1
   * @param d the value. For numeric PrimitiveArray, it is narrowed if needed by methods like
   *     Math2.roundToLong(d).
   */
  @Override
  public void setFloat(final int index, final float d) {
    if (!maxIsMV && (!Float.isFinite(d) || d < Long.MIN_VALUE || d > Long.MAX_VALUE))
      maxIsMV = true;
    set(index, Math2.roundToLong(d));
  }

  /**
   * Return a value from the array as a double.
   *
   * @param index the index number 0 .. size-1
   * @return the value as a double. String values are parsed with String2.parseDouble and so may
   *     return Double.NaN. If maxIsMV, Long.MAX_VALUE is returned as Double.NaN.
   */
  @Override
  public double getDouble(final int index) {
    final long i = get(index);
    return maxIsMV && i == Long.MAX_VALUE ? Double.NaN : i;
  }

  /**
   * If this is a signed integer type, this makes an unsigned variant (e.g., PAType.BYTE returns a
   * PAType.UBYTE). The values from pa are then treated as unsigned, e.g., -1 in ByteArray becomes
   * 255 in a UByteArray.
   *
   * @return a new unsigned PrimitiveArray, or this pa.
   */
  @Override
  public PrimitiveArray makeUnsignedPA() {
    Math2.ensureMemoryAvailable(8L * size, "LongArray");
    final long ar[] = new long[size];
    if (wrappedArray != null) {
      System.arraycopy(wrappedArray, 0, ar, 0, size);
    } else {
      java.lang.foreign.MemorySegment.copy(
          array, (0) * 8L, java.lang.foreign.MemorySegment.ofArray(ar), (0) * 8L, (size) * 8L);
    }
    return new ULongArray(ar);
  }

  /**
   * Return a value from the array as a double. FloatArray converts float to double in a simplistic
   * way. For this variant: Integer source values will be treated as unsigned (e.g., a ByteArray
   * with -1 returns 255).
   *
   * @param index the index number 0 ... size-1
   * @return the value as a double. String values are parsed with String2.parseDouble, so may return
   *     Double.NaN.
   */
  @Override
  public double getUnsignedDouble(final int index) {
    return Math2.ulongToDouble(get(index)); // !!! possible loss of precision
  }

  /**
   * Return a value from the array as a double. This "raw" variant leaves missingValue from integer
   * data types (e.g., ByteArray missingValue=127) AS IS (even if maxIsMV=true).
   *
   * <p>All integerTypes overwrite this.
   *
   * @param index the index number 0 ... size-1
   * @return the value as a double. String values are parsed with String2.parseDouble and so may
   *     return Double.NaN.
   */
  @Override
  public double getRawDouble(final int index) {
    return get(index);
  }

  /**
   * Set a value in the array as a double.
   *
   * @param index the index number 0 .. size-1
   * @param d the value. For numeric PrimitiveArray, it is narrowed if needed by methods like
   *     Math2.roundToLong(d).
   */
  @Override
  public void setDouble(final int index, final double d) {
    if (!maxIsMV && (!Double.isFinite(d) || d < Long.MIN_VALUE || d > Long.MAX_VALUE))
      maxIsMV = true;
    set(index, Math2.roundToLong(d));
  }

  /**
   * Return a value from the array as a String (where the cohort missing value appears as "", not a
   * value).
   *
   * @param index the index number 0 ..
   * @return For numeric types, this returns (String.valueOf(ar[index])), or "" for NaN or infinity.
   *     If this PA is unsigned, this method returns the unsigned value.
   */
  @Override
  public String getString(final int index) {
    final long tl = get(index);
    return maxIsMV && tl == Long.MAX_VALUE ? "" : String.valueOf(tl);
  }

  /**
   * Return a value from the array as a String suitable for a JSON file. char returns a String with
   * 1 character. String returns a json String with chars above 127 encoded as \\udddd.
   *
   * @param index the index number 0 ... size-1
   * @return For numeric types, this returns ("" + ar[index]), or "null" for NaN or infinity.
   */
  @Override
  public String getJsonString(final int index) {
    final long tl = get(index);
    return maxIsMV && tl == Long.MAX_VALUE ? "null" : String.valueOf(tl);
  }

  /**
   * Return a value from the array as a String. This "raw" variant leaves missingValue from integer
   * data types (e.g., ByteArray missingValue=127) AS IS, regardless of maxIsMV. FloatArray and
   * DoubleArray return "" if the stored value is NaN.
   *
   * <p>All integerTypes overwrite this.
   *
   * @param index the index number 0 ... size-1
   * @return the value as a String.
   */
  @Override
  public String getRawString(final int index) {
    return String.valueOf(get(index));
  }

  /**
   * Set a value in the array as a String.
   *
   * @param index the index number 0 ..
   * @param s the value. For numeric PrimitiveArray's, it is parsed with String2.parseLong.
   */
  @Override
  public void setString(final int index, final String s) {
    final long tl = String2.parseLong(s);
    if (!maxIsMV
        && tl == Long.MAX_VALUE
        && (s == null
            // without leading 9 to allow for 9.2233...e18 etc //not perfect, but gets common cases
            || s.indexOf("223372036854775807") < 0)) {
      maxIsMV = true;
    }
    set(index, tl);
  }

  /**
   * This finds the first value which equals 'lookFor' starting at index 'startIndex'.
   *
   * @param lookFor the value to be looked for
   * @return the index where 'lookFor' is found, or -1 if not found.
   */
  public int indexOf(final long lookFor) {
    return indexOf(lookFor, 0);
  }

  /**
   * This finds the first value which equals 'lookFor' starting at index 'startIndex'.
   *
   * @param lookFor the value to be looked for
   * @param startIndex 0 ... size-1
   * @return the index where 'lookFor' is found, or -1 if not found.
   */
  public int indexOf(final long lookFor, final int startIndex) {
    if (wrappedArray != null) {
      for (int i = startIndex; i < size; i++) if (wrappedArray[i] == lookFor) return i;
    } else {
      for (int i = startIndex; i < size; i++) if (getArrayVal(i) == lookFor) return i;
    }
    return -1;
  }

  /**
   * This finds the first value which equals 'lookFor' starting at index 'startIndex'.
   *
   * @param lookFor the value to be looked for
   * @param startIndex 0 ... size-1
   * @return the index where 'lookFor' is found, or -1 if not found.
   */
  @Override
  public int indexOf(final String lookFor, final int startIndex) {
    if (startIndex >= size) return -1;
    return indexOf(String2.parseLong(lookFor), startIndex);
  }

  /**
   * This finds the last value which equals 'lookFor' starting at index 'startIndex'.
   *
   * @param lookFor the value to be looked for
   * @param startIndex 0 ... size-1. The search progresses towards 0.
   * @return the index where 'lookFor' is found, or -1 if not found.
   */
  public int lastIndexOf(final long lookFor, final int startIndex) {
    if (startIndex >= size)
      throw new IllegalArgumentException(
          String2.ERROR
              + " in LongArray.get: startIndex ("
              + startIndex
              + ") >= size ("
              + size
              + ").");
    if (wrappedArray != null) {
      for (int i = startIndex; i >= 0; i--) if (wrappedArray[i] == lookFor) return i;
    } else {
      for (int i = startIndex; i >= 0; i--) if (getArrayVal(i) == lookFor) return i;
    }
    return -1;
  }

  /**
   * This finds the last value which equals 'lookFor' starting at index 'startIndex'.
   *
   * @param lookFor the value to be looked for
   * @param startIndex 0 ... size-1. The search progresses towards 0.
   * @return the index where 'lookFor' is found, or -1 if not found.
   */
  @Override
  public int lastIndexOf(final String lookFor, final int startIndex) {
    return lastIndexOf(String2.parseLong(lookFor), startIndex);
  }

  /** If size != capacity, this makes a new 'array' of size 'size' so capacity will equal size. */
  @Override
  public void trimToSize() {
    int currentCapacity = capacity();
    if (size < currentCapacity) {
      long[] newArray = new long[size];
      java.lang.foreign.MemorySegment newSegment =
          java.lang.foreign.MemorySegment.ofArray(newArray);
      java.lang.foreign.MemorySegment.copy(array, 0, newSegment, 0, size * 8L);
      array = newSegment;
      wrappedArray = newArray;
    }
  }

  /**
   * Test if o is an LongArray with the same size and values.
   *
   * @param o the object that will be compared to this LongArray
   * @return true if equal. o=null returns false.
   */
  @Override
  public boolean equals(final Object o) {
    return testEquals(o).length() == 0;
  }

  /**
   * Test if o is an LongArray with the same size and values, but returns a String describing the
   * difference (or "" if equal).
   *
   * @param o
   * @return a String describing the difference (or "" if equal). o=null doesn't throw an exception.
   */
  @Override
  public String testEquals(final Object o) {
    if (!(o instanceof LongArray other))
      return "The two objects aren't equal: this object is a LongArray; the other is a "
          + (o == null ? "null" : o.getClass().getName())
          + ".";
    if (other.size() != size)
      return "The two LongArrays aren't equal: one has "
          + size
          + " value(s); the other has "
          + other.size()
          + " value(s).";
    if (wrappedArray != null && other.wrappedArray != null && maxIsMV == other.maxIsMV) {
      for (int i = 0; i < size; i++) {
        if (wrappedArray[i] != other.wrappedArray[i]) {
          return "The two LongArrays aren't equal: this["
              + i
              + "]="
              + wrappedArray[i]
              + "; other["
              + i
              + "]="
              + other.wrappedArray[i]
              + ".";
        }
      }
    } else {
      for (int i = 0; i < size; i++) {
        if (getArrayVal(i) != other.getArrayVal(i)
            || (getArrayVal(i) == Long.MAX_VALUE && maxIsMV != other.maxIsMV)) {
          return "The two LongArrays aren't equal: this["
              + i
              + "]="
              + getArrayVal(i)
              + "; other["
              + i
              + "]="
              + other.getArrayVal(i)
              + ".";
        }
      }
    }
    // if (maxIsMV != other.maxIsMV)
    //     return "The two ByteArrays aren't equal: this.maxIsMV=" + maxIsMV +
    //                                          "; other.maxIsMV=" + other.maxIsMV + ".";
    return "";
  }

  /**
   * This converts the elements into a Comma-Space-Separated-Value (CSSV) String. Integer types show
   * MAX_VALUE numbers (not "").
   *
   * @return a Comma-Space-Separated-Value (CSSV) String representation
   */
  @Override
  public String toString() {
    return String2.toCSSVString(toArray()); // toArray() get just 'size' elements
  }

  @Override
  protected void appendNccsvElement(final StringBuilder sb, final int i) {
    sb.append(wrappedArray != null ? wrappedArray[i] : getArrayVal(i)).append('L');
  }

  /**
   * This sorts the elements in ascending order. To get the elements in reverse order, just read
   * from the end of the list to the beginning.
   */
  @Override
  public void sort() {
    PanamaHelper.sort(size, wrappedArray, array, LAYOUT);
  }

  /**
   * This compares the values in this.row1 and otherPA.row2 and returns a negative integer, zero, or
   * a positive integer if the value at index1 is less than, equal to, or greater than the value at
   * index2. The cohort missing value sorts highest. Currently, this does range check index1 and
   * index2, so the caller should be careful.
   *
   * @param index1 an index number 0 ... size-1
   * @param otherPA the other PrimitiveArray which must be the same (or close) PAType.
   * @param index2 an index number 0 ... size-1
   * @return returns a negative integer, zero, or a positive integer if the value at index1 is less
   *     than, equal to, or greater than the value at index2. Think "getArrayVal(index1) -
   *     getArrayVal(index2)".
   */
  @Override
  public int compare(final int index1, final PrimitiveArray otherPA, final int index2) {
    if (otherPA.isIntegerType() && otherPA.elementType() != PAType.ULONG)
      return Long.compare(getLong(index1), otherPA.getLong(index2));

    // this is approximate (long, ulong, float, double)
    return Double.compare(getDouble(index1), otherPA.getDouble(index2));
  }

  /**
   * This copies the value in row 'from' to row 'to'. This does not check that 'from' and 'to' are
   * valid; the caller should be careful. The value for 'from' is unchanged.
   *
   * @param from an index number 0 ... size-1
   * @param to an index number 0 ... size-1
   */
  @Override
  public void copy(final int from, final int to) {
    setArrayVal(to, getArrayVal(from));
  }

  /**
   * This reorders the values in 'array' based on rank.
   *
   * @param rank is an int with values (0 ... size-1) which points to the row number for a row with
   *     a specific rank (e.g., rank[0] is the row number of the first item in the sorted list,
   *     rank[1] is the row number of the second item in the sorted list, ...).
   */
  @Override
  public void reorder(final int rank[]) {
    long[] newArray = PanamaHelper.reorder(rank, size, wrappedArray, array, LAYOUT, "LongArray");
    array = java.lang.foreign.MemorySegment.ofArray(newArray);
    wrappedArray = newArray;
  }

  /**
   * This reverses the order of the bytes in each value, e.g., if the data was read from a
   * little-endian source.
   */
  @Override
  public void reverseBytes() {
    for (int i = 0; i < size; i++) {
      long val = array.getAtIndex(LAYOUT, i);
      array.setAtIndex(LAYOUT, i, Long.reverseBytes(val));
    }
  }

  /**
   * This writes 'size' elements to a DataOutputStream.
   *
   * @param dos the DataOutputStream
   * @return the number of bytes used per element (for Strings, this is the size of one of the
   *     strings, not others, and so is useless; for other types the value is consistent). But if
   *     size=0, this returns 0.
   * @throws Exception if trouble
   */
  @Override
  public long writeToChannel(java.nio.channels.FileChannel channel) throws java.io.IOException {
    return writeToChannel(channel, java.nio.ByteOrder.nativeOrder());
  }

  @Override
  public long writeToChannel(java.nio.channels.FileChannel channel, java.nio.ByteOrder byteOrder)
      throws java.io.IOException {
    if (size == 0) {
      return 0L;
    }
    long byteCount = (long) size * elementSize();
    int elemSize = elementSize();
    boolean swap = (byteOrder != java.nio.ByteOrder.nativeOrder());

    if (!swap && wrappedArray == null) {
      java.nio.ByteBuffer buffer = array.asSlice(0, byteCount).asByteBuffer();
      while (buffer.hasRemaining()) {
        channel.write(buffer);
      }
      return byteCount;
    }

    PrimitiveArray.ScratchBuffer scratch = PrimitiveArray.SCRATCH_BUFFER.get();
    int chunkCapacity = scratch.bytes.length;
    java.lang.foreign.MemorySegment chunkSegment = scratch.segment;
    java.nio.ByteBuffer tempBuffer = scratch.buffer;

    int offset = 0;
    while (offset < byteCount) {
      int len = (int) Math.min(byteCount - offset, chunkCapacity);
      int numElems = len / elemSize;

      java.lang.foreign.MemorySegment.copy(array, offset, chunkSegment, 0, len);

      tempBuffer.clear();
      tempBuffer.limit(len);

      if (swap) {
        java.nio.LongBuffer view = tempBuffer.asLongBuffer();
        for (int i = 0; i < numElems; i++) {
          view.put(i, Long.reverseBytes(view.get(i)));
        }
      }

      while (tempBuffer.hasRemaining()) {
        channel.write(tempBuffer);
      }
      offset += len;
    }
    return byteCount;
  }

  @Override
  public int writeDos(final DataOutputStream dos) throws Exception {
    for (int i = 0; i < size; i++) dos.writeLong(getArrayVal(i));
    return size == 0 ? 0 : 8;
  }

  /**
   * This writes one element to a DataOutputStream.
   *
   * @param dos the DataOutputStream
   * @param i the index of the element to be written
   * @return the number of bytes used for this element (for Strings, this varies; for others it is
   *     consistent)
   * @throws Exception if trouble
   */
  @Override
  public int writeDos(final DataOutputStream dos, final int i) throws Exception {
    dos.writeLong(getArrayVal(i));
    return 8;
  }

  /**
   * This reads/adds n elements from a DataInputStream.
   *
   * @param dis the DataInputStream
   * @param n the number of elements to be read/added
   * @throws Exception if trouble
   */
  @Override
  public void readDis(final DataInputStream dis, final int n) throws Exception {
    ensureCapacity(size + (long) n);
    for (int i = 0; i < n; i++) setArrayVal(size++, dis.readLong());
  }

  /**
   * This reads/appends long values to this PrimitiveArray from a DODS DataInputStream, and is thus
   * the complement of externalizeForDODS.
   *
   * @param dis
   * @throws IOException if trouble
   */
  @Override
  public void internalizeFromDODS(final DataInputStream dis) throws java.io.IOException {
    final int nValues = dis.readInt();
    dis.readInt(); // skip duplicate of nValues
    ensureCapacity(size + (long) nValues);
    for (int i = 0; i < nValues; i++) setArrayVal(size++, dis.readLong());
  }

  /**
   * This writes getArrayVal(index) to a randomAccessFile at the current position.
   *
   * @param raf the RandomAccessFile
   * @param index
   * @throws Exception if trouble
   */
  @Override
  public void writeToRAF(final RandomAccessFile raf, final int index) throws Exception {
    raf.writeLong(get(index));
  }

  /**
   * This reads one value from a randomAccessFile at the current position and adds it to the
   * PrimitiveArraay.
   *
   * @param raf the RandomAccessFile
   * @throws Exception if trouble
   */
  @Override
  public void readFromRAF(final RandomAccessFile raf) throws Exception {
    add(raf.readLong());
  }

  /**
   * This appends the data in another pa to the current data. WARNING: information may be lost from
   * the incoming pa if this primitiveArray is of a smaller type; see needPAType().
   *
   * @param pa pa must be the same or a narrower data type, or the data will be narrowed with
   *     Math2.roundToLong.
   */
  @Override
  public void append(final PrimitiveArray pa) {
    final int otherSize = pa.size();
    ensureCapacity(size + (long) otherSize);
    if (pa instanceof LongArray la) {
      if (pa.getMaxIsMV()) setMaxIsMV(true);
      {
        LongArray oPA = (LongArray) la;
        PanamaHelper.copyElements(
            oPA.wrappedArray, oPA.array, 0, wrappedArray, array, size, otherSize, 8);
      }
      size += otherSize;
    } else {
      for (int i = 0; i < otherSize; i++) addString(pa.getString(i)); // this converts mv's
    }
  }

  /**
   * This appends the data in another pa to the current data. This "raw" variant leaves missingValue
   * from smaller data types (e.g., ByteArray missingValue=127) AS IS. WARNING: information may be
   * lost from the incoming pa if this primitiveArray is of a simpler type.
   *
   * @param pa pa must be the same or a narrower data type, or the data will be narrowed with
   *     Math2.roundToLong.
   */
  @Override
  public void rawAppend(final PrimitiveArray pa) {
    final int otherSize = pa.size();
    ensureCapacity(size + (long) otherSize);
    if (pa instanceof LongArray la) {
      {
        LongArray oPA = (LongArray) la;
        PanamaHelper.copyElements(
            oPA.wrappedArray, oPA.array, 0, wrappedArray, array, size, otherSize, 8);
      }
    } else if (pa.elementType() == PAType.STRING) {
      for (int i = 0; i < otherSize; i++)
        setArrayVal(size + i, pa.getLong(i)); // just parses the string
    } else {
      for (int i = 0; i < otherSize; i++)
        setArrayVal(size + i, Math2.roundToLong(pa.getRawDouble(i))); // this DOESN'T convert mv's
    }
    size += otherSize; // do last to minimize concurrency problems
  }

  /**
   * This populates 'indices' with the indices (ranks) of the values in this LongArray (ties get the
   * same index). For example, 10,10,25,3 returns 1,1,2,0.
   *
   * @param indices the intArray that will capture the indices of the values (ties get the same
   *     index). For example, 10,10,25,3 returns 1,1,2,0.
   * @return a PrimitveArray (the same type as this class) with the unique values, sorted. If all
   *     the values are unique and already sorted, this returns 'this'.
   */
  @Override
  public PrimitiveArray makeIndices(final IntArray indices) {
    indices.clear();
    if (size == 0) {
      return new LongArray();
    }

    long[] tempUnique = new long[size];
    if (wrappedArray != null) {
      System.arraycopy(wrappedArray, 0, tempUnique, 0, size);
    } else {
      for (int i = 0; i < size; i++) {
        tempUnique[i] = getArrayVal(i);
      }
    }

    // Sort the copy to easily find unique elements
    Arrays.sort(tempUnique);

    // Compact in place to get unique elements
    int nUnique = 0;
    tempUnique[nUnique++] = tempUnique[0];
    for (int i = 1; i < size; i++) {
      if (Long.compare(tempUnique[i], tempUnique[nUnique - 1]) != 0) {
        tempUnique[nUnique++] = tempUnique[i];
      }
    }

    // binarySearch each original element to find its rank
    indices.ensureCapacity(size);
    for (int i = 0; i < size; i++) {
      long val = wrappedArray != null ? wrappedArray[i] : getArrayVal(i);
      int rank = Arrays.binarySearch(tempUnique, 0, nUnique, val);
      indices.add(rank);
    }

    long[] uniqueResult = new long[nUnique];
    System.arraycopy(tempUnique, 0, uniqueResult, 0, nUnique);
    return new LongArray(uniqueResult);
  }

  /**
   * This changes all instances of the first value to the second value.
   *
   * @param tFrom the original value (use "" or "NaN" for standard missingValue)
   * @param tTo the new value (use "" or "NaN" for standard missingValue)
   * @return the number of values switched
   */
  @Override
  public int switchFromTo(final String tFrom, final String tTo) {
    final long from = String2.parseLong(tFrom);
    final Long tl = String2.parseLongObject(tTo);
    final long to = tl == null ? Long.MAX_VALUE : tl;
    if (from == to) return 0;
    int count = 0;
    if (wrappedArray != null) {
      for (int i = 0; i < size; i++) {
        if (wrappedArray[i] == from) {
          wrappedArray[i] = to;
          setArrayVal(i, to);
          count++;
        }
      }
    } else {
      for (int i = 0; i < size; i++) {
        if (getArrayVal(i) == from) {
          setArrayVal(i, to);
          count++;
        }
      }
    }
    if (count > 0 && tl == null) maxIsMV = true;
    return count;
  }

  /**
   * This tests for adjacent tied values and returns the index of the first tied value. Adjacent
   * NaNs are treated as ties.
   *
   * @return the index of the first tied value (or -1 if none).
   */
  @Override
  public int firstTie() {
    if (wrappedArray != null) {
      for (int i = 1; i < size; i++) {
        if (wrappedArray[i - 1] == wrappedArray[i]) {
          return i - 1;
        }
      }
    } else {
      for (int i = 1; i < size; i++) {
        if (getArrayVal(i - 1) == getArrayVal(i)) {
          return i - 1;
        }
      }
    }
    return -1;
  }

  /**
   * This finds the number of non-missing values, and the index of the min and max value.
   *
   * @return int[3], [0]=the number of non-missing values, [1]=index of min value (if tie, index of
   *     last found; -1 if all mv), [2]=index of max value (if tie, index of last found; -1 if all
   *     mv).
   */
  @Override
  public int[] getNMinMaxIndex() {
    int n = 0, tmini = -1, tmaxi = -1;
    long tmin = Long.MAX_VALUE;
    long tmax = Long.MIN_VALUE;
    if (wrappedArray != null) {
      for (int i = 0; i < size; i++) {
        final long v = wrappedArray[i];
        if (maxIsMV && v == Long.MAX_VALUE) {
        } else {
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
    } else {
      for (int i = 0; i < size; i++) {
        final long v = getArrayVal(i);
        if (maxIsMV && v == Long.MAX_VALUE) {
        } else {
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
    }
    // String2.log(">> LongArray.getNMinMaxIndex size=" + size + " n=" + n + " min=" + tmin + "
    // max=" + tmax);
    return new int[] {n, tmini, tmaxi};
  }

  /**
   * For integer types, this fixes unsigned bytes that were incorrectly read as signed so that they
   * have the correct ordering of values (0 to 255 becomes -128 to 127). <br>
   * What were read as signed: 0 127 -128 -1 <br>
   * should become unsigned: -128 -1 0 255 <br>
   * This also does the reverse. <br>
   * For non-integer types, this does nothing.
   */
  @Override
  public void changeSignedToFromUnsigned() {
    for (int i = 0; i < size; i++) {
      long i2 = getArrayVal(i);
      setArrayVal(
          i,
          i2 < 0 ? i2 + Long.MAX_VALUE + 1 : i2 - Long.MAX_VALUE - 1); // order of ops is important
    }
  }
}
