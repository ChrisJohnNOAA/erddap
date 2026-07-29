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
}
