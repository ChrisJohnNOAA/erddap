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
 * ULongArray is a thin shell over a long[] with methods like ArrayList's methods; it extends
 * PrimitiveArray.
 *
 * <p>This class uses maxIsMV=true and MAX_VALUE to represent a missing value (NaN).
 */
public class ULongArray extends PrimitiveArray {

  private static final java.lang.foreign.ValueLayout.OfLong LAYOUT =
      java.lang.foreign.ValueLayout.JAVA_LONG.withOrder(java.nio.ByteOrder.nativeOrder());

  /** This is the minimum unsigned value, stored as a BigInteger. */
  public static final BigInteger MIN_VALUE = Math2.ULONG_MIN_VALUE;

  /** This is the maximum unsigned value, stored/packed as a signed long. */
  public static final long PACKED_MAX_VALUE = -1;

  /** This is the maximum unsigned value, stored as a BigInteger. */
  public static final BigInteger MAX_VALUE = Math2.ULONG_MAX_VALUE;

  /** This is the MAX_VALUE+1 stored as a BigInteger. */
  public static final BigInteger MAX_VALUE1 = MAX_VALUE.add(BigInteger.ONE);

  /** This is the Long.MAX_VALUE+1 stored as a BigInteger. */
  public static final BigInteger LONG_MAX_VALUE1 =
      new BigInteger("" + Long.MAX_VALUE).add(BigInteger.ONE);

  /** This is the maximum value, stored as a double. */
  public static final double MAX_VALUE_AS_DOUBLE = MAX_VALUE.doubleValue();

  /** This is the minimum value, stored as a double. */
  public static final double MIN_VALUE_AS_DOUBLE = MIN_VALUE.doubleValue();

  /** This is Integer.MAX_VALUE as a BigInteger. */
  public static final BigInteger INT_MAX_VALUE = new BigInteger("" + Integer.MAX_VALUE);

  /**
   * This indicates if this class' type (e.g., PAType.SHORT) is an unsigned integer type. The
   * unsigned integer type classes overwrite this.
   */
  @Override
  public boolean isUnsigned() {
    return true;
  }

  /**
   * This indicates if this class' type (e.g., PAType.SHORT) is an integer (in the math sense) type.
   * The integer type classes overwrite this.
   */
  @Override
  public boolean isIntegerType() {
    // trouble: ULong can't be contained in a long.
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
  public int elementSize() {
    return 8;
  }

  /**
   * This returns for cohort missing value for this class (e.g., Integer.MAX_VALUE), expressed as a
   * double. FloatArray and StringArray return Double.NaN.
   */
  @Override
  public double missingValueAsDouble() {
    return MAX_VALUE.doubleValue();
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
  public boolean isMaxValue(final int index) {
    return getPacked(index) == PACKED_MAX_VALUE;
  }

  /**
   * This tests if the value at the specified index is a missing value. For integerTypes,
   * isMissingValue can only be true if maxIsMv is 'true'.
   *
   * @param index The index in question
   * @return true if the value is a missing value.
   */
  @Override
  public boolean isMissingValue(final int index) {
    return maxIsMV && isMaxValue(index);
  }

  /** This packs a signed BigInteger holding a ulong as a long. */
  public static long pack(final BigInteger bi) {
    if (bi == null || bi.compareTo(BigInteger.ZERO) < 0 || bi.compareTo(MAX_VALUE) >= 0)
      return PACKED_MAX_VALUE;
    if (bi.compareTo(LONG_MAX_VALUE1) >= 0) return bi.subtract(MAX_VALUE1).longValue();
    return bi.longValue();
  }

  /**
   * This packs a signed BigInteger holding a ulong as a long. If "trouble" (null or out of range)
   * this sets maxIsMV to true. Use this before setting a value in this ULongArraay.
   */
  public long packAndSetMaxIsMV(final BigInteger bi) {
    if (bi == null || bi.compareTo(BigInteger.ZERO) < 0) {
      maxIsMV = true;
      return PACKED_MAX_VALUE;
    }
    final int com = bi.compareTo(MAX_VALUE);
    if (com >= 0) {
      if (com > 0) maxIsMV = true;
      return PACKED_MAX_VALUE;
    }

    // actually pack the value
    if (bi.compareTo(LONG_MAX_VALUE1) >= 0) return bi.subtract(MAX_VALUE1).longValue();
    return bi.longValue();
  }

  /** This unpacks a ulong stored in a long as a BigInteger (or null if trouble/NaN). */
  public BigInteger unpack(final long tl) {
    if (tl == PACKED_MAX_VALUE) return maxIsMV ? null : MAX_VALUE;
    final BigInteger bi = new BigInteger("" + tl);
    if (tl >= 0) return bi;
    return bi.add(MAX_VALUE1); // convert packed signed long to unsigned long stored in BigInteger
  }

  /** This unpacks a ulong stored in a long as a BigInteger, as if maxIsMV=false. */
  public BigInteger unpackIgnoreMaxIsMV(final long tl) {
    if (tl == PACKED_MAX_VALUE) return MAX_VALUE;
    final BigInteger bi = new BigInteger("" + tl);
    if (tl >= 0) return bi;
    return bi.add(MAX_VALUE1); // convert packed signed long to unsigned long stored in BigInteger
  }

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

  /** A constructor for a capacity of 8 elements. The initial 'size' will be 0. */
  public ULongArray() {
    wrappedArray = new long[8];
    array = java.lang.foreign.MemorySegment.ofArray(wrappedArray);
  }

  /**
   * This constructs a ULongArray by copying elements from the incoming PrimitiveArray (using
   * append()).
   *
   * @param primitiveArray a primitiveArray of any type
   */
  public ULongArray(final PrimitiveArray primitiveArray) {
    Math2.ensureMemoryAvailable(8L * primitiveArray.size(), "ULongArray");
    wrappedArray = new long[primitiveArray.size()];
    array = java.lang.foreign.MemorySegment.ofArray(wrappedArray); // exact size
    append(primitiveArray);
  }

  /**
   * A constructor for a specified number of elements. The initial 'size' will be 0.
   *
   * @param capacity creates an ULongArray with the specified initial capacity.
   * @param active if true, size will be set to capacity and all elements will equal 0; else size =
   *     0.
   */
  public ULongArray(final int capacity, final boolean active) {
    Math2.ensureMemoryAvailable(8L * capacity, "ULongArray");
    wrappedArray = new long[capacity];
    array = java.lang.foreign.MemorySegment.ofArray(wrappedArray);
    if (active) size = capacity;
  }

  /**
   * A constructor which (at least initially) uses the array and all its elements ('size' will equal
   * anArray.length).
   *
   * @param anArray the array with already packed values to be used as this object's array.
   */
  public ULongArray(final long[] anArray) {
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
   * A constructor which does NOT use the array and all its elements ('size' will equal
   * anArray.length).
   *
   * @param anArray the array with not-yet-packed values.
   */
  public ULongArray(final BigInteger[] anArray) {
    size = anArray.length;
    Math2.ensureMemoryAvailable(8L * size, "ULongArray");
    wrappedArray = new long[size];
    array = java.lang.foreign.MemorySegment.ofArray(wrappedArray);
    for (int i = 0; i < size; i++) setArrayVal(i, packAndSetMaxIsMV(anArray[i]));
  }

  /**
   * If this is an unsigned integer type, this makes a signed variant (e.g., PAType.UBYTE returns a
   * PAType.BYTE). The values from pa are then treated as unsigned, e.g., 255 in UByteArray becomes
   * -1 in a ByteArray.
   *
   * @return a new unsigned PrimitiveArray, or this pa.
   */
  @Override
  public PrimitiveArray makeSignedPA() {
    Math2.ensureMemoryAvailable(8L * size, "ULongArray");
    final long ar[] = new long[size];
    if (wrappedArray != null) {
      System.arraycopy(wrappedArray, 0, ar, 0, size);
    } else {
      java.lang.foreign.MemorySegment.copy(
          array, (0) * 8L, java.lang.foreign.MemorySegment.ofArray(ar), (0) * 8L, (size) * 8L);
    }
    return new LongArray(ar);
  }

  /**
   * This makes a ULongArray from the comma-separated values. <br>
   * null becomes pa.length() == 0. <br>
   * "" becomes pa.length() == 0. <br>
   * " " becomes pa.length() == 1. <br>
   * See also PrimitiveArray.csvFactory(paType, csv);
   *
   * @param csv the comma-separated-value string
   * @return a ULongArray from the comma-separated values.
   */
  public static ULongArray fromCSV(final String csv) {
    return (ULongArray) PrimitiveArray.csvFactory(PAType.ULONG, csv);
  }

  /**
   * This returns a new PAOne with the minimum value that can be held by this class.
   *
   * @return a new PAOne with the minimum value that can be held by this class, e.g., -128b for
   *     ByteArray.
   */
  @Override
  public PAOne MINEST_VALUE() {
    return new PAOne(PAType.ULONG).setULong(MIN_VALUE);
  }

  /**
   * This returns a new PAOne with the maximum value that can be held by this class (not including
   * the cohort missing value).
   *
   * @return a new PAOne with the maximum value that can be held by this class, e.g., 126 for
   *     ByteArray.
   */
  @Override
  public PAOne MAXEST_VALUE() {
    return new PAOne(PAType.ULONG).setULong(MAX_VALUE.subtract(BigInteger.ONE));
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
   * This returns the hashcode for this ULongArray (dependent only on values, not capacity).
   * WARNING: the algorithm used may change in future versions.
   *
   * @return the hashcode for this ULongArray (dependent only on values, not capacity)
   */
  @Override
  public int hashCode() {
    // see
    // https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/List.html#hashCode()
    // and
    // https://stackoverflow.com/questions/299304/why-does-javas-hashcode-in-string-use-31-as-a-multiplier
    // and java docs for ULong.hashCode()
    int code = 0;
    for (int i = 0; i < size; i++) code = 31 * code + Long.hashCode(getArrayVal(i));
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
    if (stopIndex < startIndex) return pa == null ? new ULongArray(new long[0]) : pa;

    int willFind = strideWillFind(stopIndex - startIndex + 1, stride);
    ULongArray da = null;
    if (pa == null) {
      da = new ULongArray(willFind, true);
    } else {
      da = (ULongArray) pa;
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
    return PAType.ULONG;
  }

  /**
   * This returns the minimum PAType needed to completely and precisely contain the values in this
   * PA's PAType and tPAType (e.g., when merging two PrimitiveArrays).
   *
   * @return the minimum PAType needed to completely and precisely contain the values in this PA's
   *     PAType and tPAType (e.g., when merging two PrimitiveArrays).
   */
  @Override
  public PAType needPAType(final PAType tPAType) {
    return switch (tPAType) {
      // if tPAType is smaller or same, return this.PAType
      case UBYTE, USHORT, UINT, ULONG -> PAType.ULONG;

      // if sideways
      default -> PAType.STRING;
    };
  }

  /**
   * This adds an item to the array (increasing 'size' by 1).
   *
   * @param value the value to be added to the array
   */
  public void add(final BigInteger value) {
    if (size == capacity()) // if we're at capacity
    ensureCapacity(size + 1L);
    setArrayVal(size++, packAndSetMaxIsMV(value));
  }

  /**
   * This adds an already packed value to the array (increasing 'size' by 1).
   *
   * @param packedValue the already packed value to be added to the array
   */
  public void addPacked(final long packedValue) {
    if (size == capacity()) // if we're at capacity
    ensureCapacity(size + 1L);
    setArrayVal(size++, packedValue);
  }

  /**
   * This adds an item to the array (increasing 'size' by 1).
   *
   * @param value the value to be added to the array. If value instanceof Number, this uses
   *     Number.longValue(). If null or not a Number, this adds MAX_VALUE.
   */
  @Override
  public void addObject(final Object value) {
    if (value instanceof Number num) {
      if (value instanceof Double) addDouble(num.doubleValue()); // supports NaN
      else if (value instanceof Float) addFloat(num.floatValue()); // supports NaN
      else addLong(num.longValue());
    } else {
      addDouble(Double.NaN); // sets maxIsMV
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
    addPacked(sd.getScalarLong(memberName));
  }

  /**
   * This adds all the values from ar.
   *
   * @param ar an array
   */
  public void add(final BigInteger ar[]) {
    final int arSize = ar.length;
    ensureCapacity(size + (long) arSize);
    for (BigInteger bigInteger : ar) setArrayVal(size++, packAndSetMaxIsMV(bigInteger));
  }

  /**
   * This adds n copies of value to the array (increasing 'size' by n).
   *
   * @param n if less than 0, this throws Exception
   * @param value the value to be added to the array. n &lt; 0 throws an Exception.
   */
  public void addN(final int n, final BigInteger value) {
    if (n == 0) return;
    if (n < 0)
      throw new IllegalArgumentException(
          MessageFormat.format(ArrayAddN, getClass().getSimpleName(), "" + n));
    ensureCapacity(size + (long) n);
    if (wrappedArray != null) {
      Arrays.fill(wrappedArray, size, size + n, packAndSetMaxIsMV(value));
    } else {
      for (int i = size; i < size + n; i++) {
        array.setAtIndex(LAYOUT, i, packAndSetMaxIsMV(value));
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
  public void atInsert(final int index, final BigInteger value) {
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
    setArrayVal(index, packAndSetMaxIsMV(value));
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
    atInsert(index, String2.parseULongObject(value));
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
    if (Double.isNaN(d) || d < MIN_VALUE_AS_DOUBLE || d > MAX_VALUE_AS_DOUBLE)
      addNDoubles(n, Double.NaN);
    else addN(n, value.getULong());
  }

  /**
   * This adds n Strings to the array.
   *
   * @param n the number of times 'value' should be added. If less than 0, this throws Exception.
   * @param value the value, as a String.
   */
  @Override
  public void addNStrings(final int n, final String value) {
    addN(n, String2.parseULongObject(value));
  }

  /**
   * This adds n floats to the array.
   *
   * @param n the number of times 'value' should be added. If less than 0, this throws Exception.
   * @param value the value, as a float.
   */
  @Override
  public void addNFloats(final int n, final float value) {
    if (!maxIsMV
        && (!Float.isFinite(value) || value < MIN_VALUE_AS_DOUBLE || value > MAX_VALUE_AS_DOUBLE))
      maxIsMV = true;
    addN(n, Math2.roundToULong(value));
  }

  /**
   * This adds n doubles to the array.
   *
   * @param n the number of times 'value' should be added. If less than 0, this throws Exception.
   * @param value the value, as a double.
   */
  @Override
  public void addNDoubles(final int n, final double value) {
    if (!maxIsMV
        && (!Double.isFinite(value) || value < MIN_VALUE_AS_DOUBLE || value > MAX_VALUE_AS_DOUBLE))
      maxIsMV = true;
    addN(n, Math2.roundToULong(value));
  }

  /**
   * This adds n ints to the array.
   *
   * @param n the number of times 'value' should be added
   * @param value the value, as an int.
   */
  @Override
  public void addNInts(final int n, final int value) {
    if (value < 0) {
      maxIsMV = true;
      addN(n, MAX_VALUE);
    } else {
      addN(n, new BigInteger("" + value)); // !!! assumes value=Integer.MAX_VALUE isn't maxIsMV
    }
  }

  /**
   * This adds n long to the array.
   *
   * @param n the number of times 'value' should be added
   * @param value the value, as an int.
   */
  @Override
  public void addNLongs(final int n, final long value) {
    if (value < 0) {
      maxIsMV = true;
      addN(n, MAX_VALUE);
    } else {
      addN(n, new BigInteger("" + value)); // !!! assumes value=Integer.MAX_VALUE isn't maxIsMV
    }
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
                + " in ULongArray.addFromPA: otherIndex="
                + otherIndex
                + " + nValues="
                + nValues
                + " > otherPA.size="
                + otherPA.size);
      ensureCapacity(size + nValues);
      {
        ULongArray oPA = (ULongArray) ((ULongArray) otherPA);
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
      add(otherPA.getULong(otherIndex++)); // does error checking
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
    set(index, otherPA.getULong(otherIndex));
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
          String2.ERROR + " in ULongArray.removeRange: to (" + to + ") > size (" + size + ").");
    if (from >= to) {
      if (from == to) return;
      throw new IllegalArgumentException(
          String2.ERROR + " in ULongArray.removeRange: from (" + from + ") > to (" + to + ").");
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
    final String errorIn = String2.ERROR + " in ULongArray.move:\n";

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
          PanamaHelper.calculateNewCapacity(currentCapacity, minCapacity, "ULongArray");
      Math2.ensureMemoryAvailable(8L * newCapacity, "ULongArray");
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
    Math2.ensureMemoryAvailable(8L * size, "ULongArray.toArray");
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
   * @return a double[] (perhaps 'array') which has 'size' elements. If maxIsMV, MAX_VALUE is
   *     converted to Double.NaN.
   */
  @Override
  public double[] toDoubleArray() {
    Math2.ensureMemoryAvailable(8L * size, "ULongArray.toDoubleArray");
    final double dar[] = new double[size];
    for (int i = 0; i < size; i++) {
      final BigInteger j = get(i);
      dar[i] = j == null ? Double.NaN : j.doubleValue();
    }
    return dar;
  }

  /**
   * This returns a String[] which has 'size' elements.
   *
   * @return a String[] which has 'size' elements. MAX_VALUE appears as "".
   */
  @Override
  public String[] toStringArray() {
    Math2.ensureMemoryAvailable(
        12L * size, "ULongArray.toStringArray"); // 12L is feeble minimal estimate
    final String sar[] = new String[size];
    for (int i = 0; i < size; i++) {
      final BigInteger tl = unpack(getArrayVal(i));
      sar[i] = tl == null ? "" : String.valueOf(tl);
    }
    return sar;
  }

  /**
   * This gets a specified element.
   *
   * @param index 0 ... size-1
   * @return the specified element (or null if trouble/NaN)
   */
  public BigInteger get(final int index) {
    if (index >= size)
      throw new IllegalArgumentException(
          String2.ERROR + " in ULongArray.get: index (" + index + ") >= size (" + size + ").");
    return unpack(getArrayVal(index));
  }

  /**
   * This gets a specified element.
   *
   * @param index 0 ... size-1
   * @return the specified element (or MAX_VALUE)
   */
  public BigInteger getIgnoreMaxIsMV(final int index) {
    if (index >= size)
      throw new IllegalArgumentException(
          String2.ERROR + " in ULongArray.get: index (" + index + ") >= size (" + size + ").");
    return unpackIgnoreMaxIsMV(getArrayVal(index));
  }

  /**
   * This sets a specified element.
   *
   * @param index 0 ... size-1
   * @param value the value for that element
   */
  public void set(final int index, final BigInteger value) {
    if (index >= size)
      throw new IllegalArgumentException(
          String2.ERROR + " in ULongArray.set: index (" + index + ") >= size (" + size + ").");
    setArrayVal(index, packAndSetMaxIsMV(value));
  }

  /**
   * Return a value from the array as an int.
   *
   * @param index the index number 0 ... size-1
   * @return the value as an int. This may return Integer.MAX_VALUE.
   */
  @Override
  public int getInt(final int index) {
    final BigInteger bi = get(index);
    return bi == null || bi.compareTo(INT_MAX_VALUE) >= 0 || bi.compareTo(BigInteger.ZERO) < 0
        ? Integer.MAX_VALUE
        : bi.intValue();
  }

  /**
   * This gets a specified element as a packed value.
   *
   * @param index 0 ... size-1
   * @return the specified element
   */
  public long getPacked(final int index) {
    if (index >= size)
      throw new IllegalArgumentException(
          String2.ERROR + " in ULongArray.get: index (" + index + ") >= size (" + size + ").");
    return getArrayVal(index);
  }

  // getRawInt(index) uses default getInt(index) since missingValue is bigger than int.

  /**
   * Set a value in the array as an int.
   *
   * @param index the index number 0 .. size-1
   * @param i the value. Integer.MAX_VALUE doesn't change maxIsMV setting.
   */
  @Override
  public void setInt(final int index, final int i) {
    if (i < 0) {
      maxIsMV = true;
      set(index, MAX_VALUE);
    } else {
      set(index, new BigInteger("" + i));
    }
  }

  /**
   * Return a value from the array as a long.
   *
   * @param index the index number 0 ... size-1
   * @return the value as a long.
   */
  @Override
  public long getLong(final int index) {
    return Math2.narrowToLong(get(index));
  }

  /**
   * Set a value in the array as a long.
   *
   * @param index the index number 0 .. size-1
   * @param i the value. i=MAX_VALUE doesn't change maxIsMV setting.
   */
  @Override
  public void setLong(final int index, final long i) {
    if (i < 0) {
      maxIsMV = true;
      set(index, MAX_VALUE);
    } else {
      set(index, new BigInteger("" + i));
    }
  }

  /**
   * Return a value from the array as a ulong.
   *
   * @param index the index number 0 ... size-1
   * @return the value as a ulong (or null if trouble/NaN).
   */
  @Override
  public BigInteger getULong(final int index) {
    return get(index);
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
    set(index, i);
  }

  /**
   * Return a value from the array as a float.
   *
   * @param index the index number 0 .. size-1
   * @return the value as a float. String values are parsed with String2.parseFloat and so may
   *     return Float.NaN. If maxIsMV, MAX_VALUE is returned as Float.NaN.
   */
  @Override
  public float getFloat(final int index) {
    final BigInteger tl = get(index);
    return tl == null ? Float.NaN : tl.floatValue();
  }

  /**
   * Set a value in the array as a float.
   *
   * @param index the index number 0 .. size-1
   * @param d the value. For numeric PrimitiveArray, it is narrowed if needed by methods like
   *     Math2.roundToULong(d).
   */
  @Override
  public void setFloat(final int index, final float d) {
    set(index, Math2.roundToULongOrNull(d));
  }

  /**
   * Return a value from the array as a double.
   *
   * @param index the index number 0 .. size-1
   * @return the value as a double. String values are parsed with String2.parseDouble and so may
   *     return Double.NaN. If maxIsMV, MAX_VALUE is returned as Double.NaN.
   */
  @Override
  public double getDouble(final int index) {
    final BigInteger tl = get(index);
    return tl == null ? Double.NaN : tl.doubleValue();
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
    return getDouble(index); // already unsigned // !!! possible loss of precision
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
    return getIgnoreMaxIsMV(index).doubleValue();
  }

  /**
   * Set a value in the array as a double.
   *
   * @param index the index number 0 .. size-1
   * @param d the value. For numeric PrimitiveArray, it is narrowed if needed by methods like
   *     Math2.roundToULong(d).
   */
  @Override
  public void setDouble(final int index, final double d) {
    set(index, Math2.roundToULongOrNull(d));
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
    final BigInteger bi = get(index);
    return bi == null ? "" : String.valueOf(bi);
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
    return "" + get(index);
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
    return getIgnoreMaxIsMV(index).toString();
  }

  /**
   * Set a value in the array as a String.
   *
   * @param index the index number 0 ..
   * @param s the value. For numeric PrimitiveArray's, it is parsed with String2.parseBigInteger.
   */
  @Override
  public void setString(final int index, final String s) {
    set(index, String2.parseULongObject(s));
  }

  /**
   * This finds the first value which equals 'lookFor' starting at index 'startIndex'.
   *
   * @param lookFor the value to be looked for
   * @return the index where 'lookFor' is found, or -1 if not found.
   */
  public int indexOf(final BigInteger lookFor) {
    return indexOf(lookFor, 0);
  }

  /**
   * This finds the first value which equals 'lookFor' starting at index 'startIndex'.
   *
   * @param lookFor the value to be looked for
   * @param startIndex 0 ... size-1
   * @return the index where 'lookFor' is found, or -1 if not found.
   */
  public int indexOf(final BigInteger lookFor, final int startIndex) {
    final long packedLookFor = pack(lookFor);
    for (int i = startIndex; i < size; i++) if (getArrayVal(i) == packedLookFor) return i;
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
    return indexOf(String2.parseULongObject(lookFor), startIndex);
  }

  /**
   * This finds the last value which equals 'lookFor' starting at index 'startIndex'.
   *
   * @param lookFor the value to be looked for
   * @param startIndex 0 ... size-1. The search progresses towards 0.
   * @return the index where 'lookFor' is found, or -1 if not found.
   */
  public int lastIndexOf(final BigInteger lookFor, final int startIndex) {
    if (startIndex >= size)
      throw new IllegalArgumentException(
          String2.ERROR
              + " in ULongArray.get: startIndex ("
              + startIndex
              + ") >= size ("
              + size
              + ").");
    final long packedLookFor = pack(lookFor);
    for (int i = startIndex; i >= 0; i--) if (getArrayVal(i) == packedLookFor) return i;
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
    return lastIndexOf(String2.parseULongObject(lookFor), startIndex);
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
   * Test if o is an ULongArray with the same size and values.
   *
   * @param o the object that will be compared to this ULongArray
   * @return true if equal. o=null returns false.
   */
  @Override
  public boolean equals(final Object o) {
    return testEquals(o).length() == 0;
  }

  /**
   * Test if o is an ULongArray with the same size and values, but returns a String describing the
   * difference (or "" if equal).
   *
   * @param o
   * @return a String describing the difference (or "" if equal). o=null doesn't throw an exception.
   */
  @Override
  public String testEquals(final Object o) {
    if (!(o instanceof ULongArray other))
      return "The two objects aren't equal: this object is a ULongArray; the other is a "
          + (o == null ? "null" : o.getClass().getName())
          + ".";
    if (other.size() != size)
      return "The two ULongArrays aren't equal: one has "
          + size
          + " value(s); the other has "
          + other.size()
          + " value(s).";
    for (int i = 0; i < size; i++)
      if (getArrayVal(i) != other.getArrayVal(i)
          || (getArrayVal(i) == PACKED_MAX_VALUE && maxIsMV != other.maxIsMV)) // handles mv
      return "The two ULongArrays aren't equal: this["
            + i
            + "]="
            + get(i)
            + "; other["
            + i
            + "]="
            + other.get(i)
            + ".";
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
    // estimate 11 bytes/element
    final StringBuilder sb =
        new StringBuilder(11 * Math.min(size, (Integer.MAX_VALUE - 8192) / 11));
    for (int i = 0; i < size; i++) {
      if (i > 0) sb.append(", ");
      sb.append(unpackIgnoreMaxIsMV(getArrayVal(i)));
    }
    return sb.toString();
  }

  @Override
  protected void appendNccsvElement(final StringBuilder sb, final int i) {
    sb.append(unpackIgnoreMaxIsMV(getArrayVal(i))).append("uL");
  }

  /**
   * This sorts the elements in ascending order. To get the elements in reverse order, just read
   * from the end of the list to the beginning.
   */
  @Override
  public void sort() {
    PanamaHelper.sort(size, wrappedArray, array, LAYOUT);
    int which = 0;
    while (which < size && getArrayVal(which) < 0) which++;
    move(which, size, 0);
  }

  /**
   * This compares the values in this.row1 and otherPA.row2 and returns a negative integer, zero, or
   * a positive integer if the value at index1 is less than, equal to, or greater than the value at
   * index2. The cohort missing value sorts highest. Currently, this does not range check index1 and
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
    if (otherPA.elementType() == PAType.ULONG)
      return getIgnoreMaxIsMV(index1).compareTo(((ULongArray) otherPA).getIgnoreMaxIsMV(index2));

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
    long[] newArray = PanamaHelper.reorder(rank, size, wrappedArray, array, LAYOUT, "ULongArray");
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
    raf.writeLong(getArrayVal(index));
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
    addPacked(raf.readLong());
  }

  /**
   * This appends the data in another pa to the current data. WARNING: information may be lost from
   * the incoming pa if this primitiveArray is of a smaller type; see needPAType().
   *
   * @param pa pa must be the same or a narrower data type, or the data will be narrowed with
   *     Math2.roundToULong.
   */
  @Override
  public void append(final PrimitiveArray pa) {
    final int otherSize = pa.size();
    ensureCapacity(size + (long) otherSize);
    if (pa instanceof ULongArray ua) {
      if (pa.getMaxIsMV()) setMaxIsMV(true);
      {
        ULongArray oPA = (ULongArray) ua;
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
   *     Math2.roundToULong.
   */
  @Override
  public void rawAppend(final PrimitiveArray pa) {
    final int otherSize = pa.size();
    ensureCapacity(size + (long) otherSize);
    if (pa instanceof ULongArray ulpa) {
      {
        ULongArray oPA = (ULongArray) ulpa;
        PanamaHelper.copyElements(
            oPA.wrappedArray, oPA.array, 0, wrappedArray, array, size, otherSize, 8);
      }
      if (pa.getMaxIsMV() && ulpa.indexOf(MAX_VALUE) >= 0) setMaxIsMV(true);
    } else if (pa instanceof StringArray) {
      for (int i = 0; i < otherSize; i++)
        setArrayVal(size + i, packAndSetMaxIsMV(pa.getULong(i))); // just parses the string
    } else {
      for (int i = 0; i < otherSize; i++)
        setArrayVal(
            size + i,
            packAndSetMaxIsMV(
                Math2.roundToULong(pa.getRawDouble(i)))); // this DOESN'T convert smaller mv's
    }
    size += otherSize; // do last to minimize concurrency problems
  }

  /**
   * This populates 'indices' with the indices (ranks) of the values in this ULongArray (ties get
   * the same index). For example, 10,10,25,3 returns 1,1,2,0.
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
      return new ULongArray();
    }

    long[] tempUnique = new long[size];
    if (wrappedArray != null) {
      System.arraycopy(wrappedArray, 0, tempUnique, 0, size);
    } else {
      for (int i = 0; i < size; i++) {
        tempUnique[i] = getArrayVal(i);
      }
    }

    // Sort the copy signedly
    Arrays.sort(tempUnique);

    // Rotate to get unsigned sorted order
    int boundary = 0;
    while (boundary < size && tempUnique[boundary] < 0) {
      boundary++;
    }
    if (boundary > 0 && boundary < size) {
      long[] rotated = new long[size];
      System.arraycopy(tempUnique, boundary, rotated, 0, size - boundary);
      System.arraycopy(tempUnique, 0, rotated, size - boundary, boundary);
      tempUnique = rotated;
    }

    // Compact in place to get unique elements
    int nUnique = 0;
    tempUnique[nUnique++] = tempUnique[0];
    for (int i = 1; i < size; i++) {
      if (tempUnique[i] != tempUnique[nUnique - 1]) {
        tempUnique[nUnique++] = tempUnique[i];
      }
    }

    // binarySearch each original element to find its rank
    indices.ensureCapacity(size);
    for (int i = 0; i < size; i++) {
      long val = wrappedArray != null ? wrappedArray[i] : getArrayVal(i);
      int low = 0;
      int high = nUnique - 1;
      int rank = -1;
      while (low <= high) {
        int mid = (low + high) >>> 1;
        long midVal = tempUnique[mid];
        int cmp = Long.compareUnsigned(midVal, val);
        if (cmp < 0) {
          low = mid + 1;
        } else if (cmp > 0) {
          high = mid - 1;
        } else {
          rank = mid;
          break;
        }
      }
      indices.add(rank);
    }

    BigInteger[] uniqueResult = new BigInteger[nUnique];
    for (int i = 0; i < nUnique; i++) {
      uniqueResult[i] = unpackIgnoreMaxIsMV(tempUnique[i]);
    }
    return new ULongArray(uniqueResult);
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
    final long packedFrom = pack(String2.parseULongObject(tFrom));
    final BigInteger bi = String2.parseULongObject(tTo);
    final long packedTo = pack(bi);
    if (packedFrom == packedTo) return 0;
    int count = 0;
    for (int i = 0; i < size; i++) {
      if (getArrayVal(i) == packedFrom) {
        setArrayVal(i, packedTo);
        count++;
      }
    }
    if (count > 0 && bi == null) maxIsMV = true;
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
    for (int i = 1; i < size; i++) {
      if (getArrayVal(i - 1) == getArrayVal(i)) {
        return i - 1;
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
    BigInteger tmin = MAX_VALUE;
    BigInteger tmax = MIN_VALUE;
    for (int i = 0; i < size; i++) {
      BigInteger v = get(i);
      if (maxIsMV && v == null) {
      } else {
        n++;
        if (v.compareTo(tmin) <= 0) {
          tmini = i;
          tmin = v;
        }
        if (v.compareTo(tmax) >= 0) {
          tmaxi = i;
          tmax = v;
        }
      }
    }
    // String2.log(">> ULongArray.getNMinMaxIndex size=" + size + " n=" + n + " min=" + tmin + "
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
      final BigInteger i2 = getIgnoreMaxIsMV(i);
      set(
          i,
          i2.compareTo(BigInteger.ZERO) < 0
              ? i2.add(MAX_VALUE).add(BigInteger.ONE)
              : i2.subtract(MAX_VALUE).subtract(BigInteger.ONE)); // order of ops is important
    }
  }
}
