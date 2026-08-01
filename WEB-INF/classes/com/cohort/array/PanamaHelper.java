package com.cohort.array;

public final class PanamaHelper {

  public static void remove(
      final int index,
      final int elementSize,
      final int size,
      final Object wrappedArray,
      final java.lang.foreign.MemorySegment array) {
    if (wrappedArray != null) {
      System.arraycopy(wrappedArray, index + 1, wrappedArray, index, size - index - 1);
    } else {
      java.lang.foreign.MemorySegment.copy(
          array,
          (index + 1) * (long) elementSize,
          array,
          index * (long) elementSize,
          (size - index - 1) * (long) elementSize);
    }
  }

  public static void removeRange(
      final int from,
      final int to,
      final int elementSize,
      final int size,
      final Object wrappedArray,
      final java.lang.foreign.MemorySegment array) {
    if (wrappedArray != null) {
      System.arraycopy(wrappedArray, to, wrappedArray, from, size - to);
    } else {
      java.lang.foreign.MemorySegment.copy(
          array,
          to * (long) elementSize,
          array,
          from * (long) elementSize,
          (size - to) * (long) elementSize);
    }
  }

  public static void move(
      final int first,
      final int last,
      final int destination,
      final int elementSize,
      final int size,
      final Object wrappedArray,
      final java.lang.foreign.MemorySegment array) {
    if (first == last || destination == first || destination == last) return;

    final int nToMove = last - first;

    if (wrappedArray != null) {
      Object temp =
          java.lang.reflect.Array.newInstance(wrappedArray.getClass().getComponentType(), nToMove);
      System.arraycopy(wrappedArray, first, temp, 0, nToMove);
      if (destination < first) {
        System.arraycopy(
            wrappedArray, destination, wrappedArray, destination + nToMove, first - destination);
        System.arraycopy(temp, 0, wrappedArray, destination, nToMove);
      } else {
        System.arraycopy(wrappedArray, last, wrappedArray, first, destination - last);
        System.arraycopy(temp, 0, wrappedArray, destination - nToMove, nToMove);
      }
    } else {
      // Off-heap segment copy
      java.lang.foreign.MemorySegment temp =
          java.lang.foreign.Arena.ofAuto().allocate(nToMove * (long) elementSize);
      java.lang.foreign.MemorySegment.copy(
          array, first * (long) elementSize, temp, 0, nToMove * (long) elementSize);
      if (destination < first) {
        java.lang.foreign.MemorySegment.copy(
            array,
            destination * (long) elementSize,
            array,
            (destination + nToMove) * (long) elementSize,
            (first - destination) * (long) elementSize);
        java.lang.foreign.MemorySegment.copy(
            temp, 0, array, destination * (long) elementSize, nToMove * (long) elementSize);
      } else {
        java.lang.foreign.MemorySegment.copy(
            array,
            last * (long) elementSize,
            array,
            first * (long) elementSize,
            (destination - last) * (long) elementSize);
        java.lang.foreign.MemorySegment.copy(
            temp,
            0,
            array,
            (destination - nToMove) * (long) elementSize,
            nToMove * (long) elementSize);
      }
    }
  }

  public static void copyElements(
      final Object srcWrapped,
      final java.lang.foreign.MemorySegment srcArray,
      final long srcOffsetElements,
      final Object dstWrapped,
      final java.lang.foreign.MemorySegment dstArray,
      final long dstOffsetElements,
      final long nElements,
      final int elementSize) {
    if (srcWrapped != null && dstWrapped != null) {
      System.arraycopy(
          srcWrapped,
          (int) srcOffsetElements,
          dstWrapped,
          (int) dstOffsetElements,
          (int) nElements);
    } else {
      java.lang.foreign.MemorySegment.copy(
          srcArray,
          srcOffsetElements * elementSize,
          dstArray,
          dstOffsetElements * elementSize,
          nElements * elementSize);
    }
  }

  public static int calculateNewCapacity(
      final long currentCapacity, final long minCapacity, final String className) {
    com.cohort.util.Math2.ensureArraySizeOkay(minCapacity, className);
    int newCapacity = (int) Math.min(Integer.MAX_VALUE - 1, currentCapacity + currentCapacity);
    if (newCapacity < minCapacity) {
      newCapacity = (int) minCapacity;
    }
    return newCapacity;
  }

  // Double sort & reorder
  public static void sort(
      final int size,
      final double[] wrappedArray,
      final java.lang.foreign.MemorySegment array,
      final java.lang.foreign.ValueLayout.OfDouble layout) {
    if (size <= 1) return;
    if (wrappedArray != null) {
      if (size < 8192) {
        java.util.Arrays.sort(wrappedArray, 0, size);
      } else {
        java.util.Arrays.parallelSort(wrappedArray, 0, size);
      }
    } else {
      double[] temp = array.asSlice(0, size * 8L).toArray(layout);
      if (size < 8192) {
        java.util.Arrays.sort(temp, 0, size);
      } else {
        java.util.Arrays.parallelSort(temp, 0, size);
      }
      java.lang.foreign.MemorySegment.copy(
          java.lang.foreign.MemorySegment.ofArray(temp), 0, array, 0, size * 8L);
    }
  }

  public static double[] reorder(
      final int[] rank,
      final int size,
      final double[] wrappedArray,
      final java.lang.foreign.MemorySegment array,
      final java.lang.foreign.ValueLayout.OfDouble layout,
      final String arrayType) {
    long currentCapacity = array.byteSize() / 8;
    com.cohort.util.Math2.ensureMemoryAvailable(8L * currentCapacity, arrayType);
    double[] newArray = new double[(int) currentCapacity];
    if (wrappedArray != null) {
      for (int i = 0; i < rank.length; i++) {
        newArray[i] = wrappedArray[rank[i]];
      }
    } else {
      for (int i = 0; i < rank.length; i++) {
        newArray[i] = array.getAtIndex(layout, rank[i]);
      }
    }
    return newArray;
  }

  // Float sort & reorder
  public static void sort(
      final int size,
      final float[] wrappedArray,
      final java.lang.foreign.MemorySegment array,
      final java.lang.foreign.ValueLayout.OfFloat layout) {
    if (size <= 1) return;
    if (wrappedArray != null) {
      if (size < 8192) {
        java.util.Arrays.sort(wrappedArray, 0, size);
      } else {
        java.util.Arrays.parallelSort(wrappedArray, 0, size);
      }
    } else {
      float[] temp = array.asSlice(0, size * 4L).toArray(layout);
      if (size < 8192) {
        java.util.Arrays.sort(temp, 0, size);
      } else {
        java.util.Arrays.parallelSort(temp, 0, size);
      }
      java.lang.foreign.MemorySegment.copy(
          java.lang.foreign.MemorySegment.ofArray(temp), 0, array, 0, size * 4L);
    }
  }

  public static float[] reorder(
      final int[] rank,
      final int size,
      final float[] wrappedArray,
      final java.lang.foreign.MemorySegment array,
      final java.lang.foreign.ValueLayout.OfFloat layout,
      final String arrayType) {
    long currentCapacity = array.byteSize() / 4;
    com.cohort.util.Math2.ensureMemoryAvailable(4L * currentCapacity, arrayType);
    float[] newArray = new float[(int) currentCapacity];
    if (wrappedArray != null) {
      for (int i = 0; i < rank.length; i++) {
        newArray[i] = wrappedArray[rank[i]];
      }
    } else {
      for (int i = 0; i < rank.length; i++) {
        newArray[i] = array.getAtIndex(layout, rank[i]);
      }
    }
    return newArray;
  }

  // Int sort & reorder (IntArray, UIntArray)
  public static void sort(
      final int size,
      final int[] wrappedArray,
      final java.lang.foreign.MemorySegment array,
      final java.lang.foreign.ValueLayout.OfInt layout) {
    if (size <= 1) return;
    if (wrappedArray != null) {
      if (size < 8192) {
        java.util.Arrays.sort(wrappedArray, 0, size);
      } else {
        java.util.Arrays.parallelSort(wrappedArray, 0, size);
      }
    } else {
      int[] temp = array.asSlice(0, size * 4L).toArray(layout);
      if (size < 8192) {
        java.util.Arrays.sort(temp, 0, size);
      } else {
        java.util.Arrays.parallelSort(temp, 0, size);
      }
      java.lang.foreign.MemorySegment.copy(
          java.lang.foreign.MemorySegment.ofArray(temp), 0, array, 0, size * 4L);
    }
  }

  public static int[] reorder(
      final int[] rank,
      final int size,
      final int[] wrappedArray,
      final java.lang.foreign.MemorySegment array,
      final java.lang.foreign.ValueLayout.OfInt layout,
      final String arrayType) {
    long currentCapacity = array.byteSize() / 4;
    com.cohort.util.Math2.ensureMemoryAvailable(4L * currentCapacity, arrayType);
    int[] newArray = new int[(int) currentCapacity];
    if (wrappedArray != null) {
      for (int i = 0; i < rank.length; i++) {
        newArray[i] = wrappedArray[rank[i]];
      }
    } else {
      for (int i = 0; i < rank.length; i++) {
        newArray[i] = array.getAtIndex(layout, rank[i]);
      }
    }
    return newArray;
  }

  // Long sort & reorder (LongArray, ULongArray)
  public static void sort(
      final int size,
      final long[] wrappedArray,
      final java.lang.foreign.MemorySegment array,
      final java.lang.foreign.ValueLayout.OfLong layout) {
    if (size <= 1) return;
    if (wrappedArray != null) {
      if (size < 8192) {
        java.util.Arrays.sort(wrappedArray, 0, size);
      } else {
        java.util.Arrays.parallelSort(wrappedArray, 0, size);
      }
    } else {
      long[] temp = array.asSlice(0, size * 8L).toArray(layout);
      if (size < 8192) {
        java.util.Arrays.sort(temp, 0, size);
      } else {
        java.util.Arrays.parallelSort(temp, 0, size);
      }
      java.lang.foreign.MemorySegment.copy(
          java.lang.foreign.MemorySegment.ofArray(temp), 0, array, 0, size * 8L);
    }
  }

  public static long[] reorder(
      final int[] rank,
      final int size,
      final long[] wrappedArray,
      final java.lang.foreign.MemorySegment array,
      final java.lang.foreign.ValueLayout.OfLong layout,
      final String arrayType) {
    long currentCapacity = array.byteSize() / 8;
    com.cohort.util.Math2.ensureMemoryAvailable(8L * currentCapacity, arrayType);
    long[] newArray = new long[(int) currentCapacity];
    if (wrappedArray != null) {
      for (int i = 0; i < rank.length; i++) {
        newArray[i] = wrappedArray[rank[i]];
      }
    } else {
      for (int i = 0; i < rank.length; i++) {
        newArray[i] = array.getAtIndex(layout, rank[i]);
      }
    }
    return newArray;
  }

  // Short sort & reorder (ShortArray, UShortArray)
  public static void sort(
      final int size,
      final short[] wrappedArray,
      final java.lang.foreign.MemorySegment array,
      final java.lang.foreign.ValueLayout.OfShort layout) {
    if (size <= 1) return;
    if (wrappedArray != null) {
      if (size < 8192) {
        java.util.Arrays.sort(wrappedArray, 0, size);
      } else {
        java.util.Arrays.parallelSort(wrappedArray, 0, size);
      }
    } else {
      short[] temp = array.asSlice(0, size * 2L).toArray(layout);
      if (size < 8192) {
        java.util.Arrays.sort(temp, 0, size);
      } else {
        java.util.Arrays.parallelSort(temp, 0, size);
      }
      java.lang.foreign.MemorySegment.copy(
          java.lang.foreign.MemorySegment.ofArray(temp), 0, array, 0, size * 2L);
    }
  }

  public static short[] reorder(
      final int[] rank,
      final int size,
      final short[] wrappedArray,
      final java.lang.foreign.MemorySegment array,
      final java.lang.foreign.ValueLayout.OfShort layout,
      final String arrayType) {
    long currentCapacity = array.byteSize() / 2;
    com.cohort.util.Math2.ensureMemoryAvailable(2L * currentCapacity, arrayType);
    short[] newArray = new short[(int) currentCapacity];
    if (wrappedArray != null) {
      for (int i = 0; i < rank.length; i++) {
        newArray[i] = wrappedArray[rank[i]];
      }
    } else {
      for (int i = 0; i < rank.length; i++) {
        newArray[i] = array.getAtIndex(layout, rank[i]);
      }
    }
    return newArray;
  }

  // Byte sort & reorder (ByteArray, UByteArray)
  public static void sort(
      final int size,
      final byte[] wrappedArray,
      final java.lang.foreign.MemorySegment array,
      final java.lang.foreign.ValueLayout.OfByte layout) {
    if (size <= 1) return;
    if (wrappedArray != null) {
      if (size < 8192) {
        java.util.Arrays.sort(wrappedArray, 0, size);
      } else {
        java.util.Arrays.parallelSort(wrappedArray, 0, size);
      }
    } else {
      byte[] temp = array.asSlice(0, size * 1L).toArray(layout);
      if (size < 8192) {
        java.util.Arrays.sort(temp, 0, size);
      } else {
        java.util.Arrays.parallelSort(temp, 0, size);
      }
      java.lang.foreign.MemorySegment.copy(
          java.lang.foreign.MemorySegment.ofArray(temp), 0, array, 0, size * 1L);
    }
  }

  public static byte[] reorder(
      final int[] rank,
      final int size,
      final byte[] wrappedArray,
      final java.lang.foreign.MemorySegment array,
      final java.lang.foreign.ValueLayout.OfByte layout,
      final String arrayType) {
    long currentCapacity = array.byteSize() / 1;
    com.cohort.util.Math2.ensureMemoryAvailable(1L * currentCapacity, arrayType);
    byte[] newArray = new byte[(int) currentCapacity];
    if (wrappedArray != null) {
      for (int i = 0; i < rank.length; i++) {
        newArray[i] = wrappedArray[rank[i]];
      }
    } else {
      for (int i = 0; i < rank.length; i++) {
        newArray[i] = array.getAtIndex(layout, rank[i]);
      }
    }
    return newArray;
  }

  // Char sort & reorder (CharArray)
  public static void sort(
      final int size,
      final char[] wrappedArray,
      final java.lang.foreign.MemorySegment array,
      final java.lang.foreign.ValueLayout.OfChar layout) {
    if (size <= 1) return;
    if (wrappedArray != null) {
      if (size < 8192) {
        java.util.Arrays.sort(wrappedArray, 0, size);
      } else {
        java.util.Arrays.parallelSort(wrappedArray, 0, size);
      }
    } else {
      char[] temp = array.asSlice(0, size * 2L).toArray(layout);
      if (size < 8192) {
        java.util.Arrays.sort(temp, 0, size);
      } else {
        java.util.Arrays.parallelSort(temp, 0, size);
      }
      java.lang.foreign.MemorySegment.copy(
          java.lang.foreign.MemorySegment.ofArray(temp), 0, array, 0, size * 2L);
    }
  }

  public static char[] reorder(
      final int[] rank,
      final int size,
      final char[] wrappedArray,
      final java.lang.foreign.MemorySegment array,
      final java.lang.foreign.ValueLayout.OfChar layout,
      final String arrayType) {
    long currentCapacity = array.byteSize() / 2;
    com.cohort.util.Math2.ensureMemoryAvailable(2L * currentCapacity, arrayType);
    char[] newArray = new char[(int) currentCapacity];
    if (wrappedArray != null) {
      for (int i = 0; i < rank.length; i++) {
        newArray[i] = wrappedArray[rank[i]];
      }
    } else {
      for (int i = 0; i < rank.length; i++) {
        newArray[i] = array.getAtIndex(layout, rank[i]);
      }
    }
    return newArray;
  }
}
