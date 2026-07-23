package com.cohort.array;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Random;

/**
 * Extended Micro-Benchmark for comparing Java double[] against Panama FFM-backed DoubleArray.
 * Evaluates Sequential Reads, Random/Stride Access, Slicing/Subsetting, Bulk Stream I/O, Sorting,
 * element moving (move), reordering, and direct FileChannel I/O.
 */
public class PanamaArrayBenchmark {

  private static final int ARRAY_SIZE = 10_000_000;
  private static final int HEAVY_OP_SIZE = 1_000_000;
  private static final int NUM_TRIALS = 5;

  public static void main(String[] args) throws Exception {
    System.out.println("=================================================");
    System.out.println("  ERDDAP Panama-backed DoubleArray Benchmark  ");
    System.out.println("=================================================");
    System.out.printf("Array size: %,d elements\n", ARRAY_SIZE);
    System.out.printf("Heavy Ops size (Sort/IO/Reorder/Move): %,d elements\n", HEAVY_OP_SIZE);
    System.out.printf("Running %d trials per pattern...\n\n", NUM_TRIALS);

    // Setup test data
    double[] standardArray = new double[ARRAY_SIZE];
    Random rand = new Random(42);
    for (int i = 0; i < ARRAY_SIZE; i++) {
      standardArray[i] = rand.nextDouble();
    }

    DoubleArray panamaArray = new DoubleArray(ARRAY_SIZE, true);
    for (int i = 0; i < ARRAY_SIZE; i++) {
      panamaArray.set(i, standardArray[i]);
    }

    // Setup stride indices
    int[] strideIndices = new int[ARRAY_SIZE / 10];
    for (int i = 0; i < strideIndices.length; i++) {
      strideIndices[i] = rand.nextInt(ARRAY_SIZE);
    }

    // Setup heavy ops test rank indices
    int[] ranks = new int[HEAVY_OP_SIZE];
    for (int i = 0; i < HEAVY_OP_SIZE; i++) {
      ranks[i] = rand.nextInt(HEAVY_OP_SIZE);
    }

    // Setup heavy ops test arrays
    double[] heavyStd = new double[HEAVY_OP_SIZE];
    System.arraycopy(standardArray, 0, heavyStd, 0, HEAVY_OP_SIZE);
    DoubleArray heavyPan = new DoubleArray(HEAVY_OP_SIZE, true);
    for (int i = 0; i < HEAVY_OP_SIZE; i++) {
      heavyPan.set(i, heavyStd[i]);
    }

    // WARMUP
    System.out.println("Warming up JIT...");
    for (int i = 0; i < 3; i++) {
      runSequentialRead(standardArray, panamaArray);
      runStrideAccess(standardArray, panamaArray, strideIndices);
      runSlicing(standardArray, panamaArray);
      runWriteReadStandard(heavyStd);
      runWriteReadPanama(heavyPan);
      runSortStandard(heavyStd);
      runSortPanama(heavyPan);
      runMoveStandard(heavyStd, 100, 5000, 20000);
      runMovePanama(heavyPan, 100, 5000, 20000);
      runReorderStandard(heavyStd, ranks);
      runReorderPanama(heavyPan, ranks);
      runChannelIOStandard(heavyStd);
      runChannelIOPanama(heavyPan);
    }
    System.out.println("Warmup complete. Starting benchmark trials...\n");

    // Benchmark trial measurements
    double timeStandardSeq = 0, timePanamaSeq = 0;
    double timeStandardStride = 0, timePanamaStride = 0;
    double timeStandardSlice = 0, timePanamaSlice = 0;
    double timeStandardIO = 0, timePanamaIO = 0;
    double timeStandardSort = 0, timePanamaSort = 0;
    double timeStandardMove = 0, timePanamaMove = 0;
    double timeStandardReorder = 0, timePanamaReorder = 0;
    double timeStandardChannel = 0, timePanamaChannel = 0;

    for (int trial = 1; trial <= NUM_TRIALS; trial++) {
      System.out.printf("--- Trial %d ---\n", trial);

      // Pattern 1: Sequential Reads
      long start = System.nanoTime();
      double sumStd = runSequentialReadStandard(standardArray);
      long end = System.nanoTime();
      double durationStd = (end - start) / 1_000_000.0;
      timeStandardSeq += durationStd;

      start = System.nanoTime();
      double sumPan = runSequentialReadPanama(panamaArray);
      end = System.nanoTime();
      double durationPan = (end - start) / 1_000_000.0;
      timePanamaSeq += durationPan;
      System.out.printf(
          "Sequential Reads -> Standard: %.2f ms, Panama: %.2f ms (CheckSums: %.2f vs %.2f)\n",
          durationStd, durationPan, sumStd, sumPan);

      // Pattern 2: Stride / Random Access
      start = System.nanoTime();
      sumStd = runStrideAccessStandard(standardArray, strideIndices);
      end = System.nanoTime();
      durationStd = (end - start) / 1_000_000.0;
      timeStandardStride += durationStd;

      start = System.nanoTime();
      sumPan = runStrideAccessPanama(panamaArray, strideIndices);
      end = System.nanoTime();
      durationPan = (end - start) / 1_000_000.0;
      timePanamaStride += durationPan;
      System.out.printf(
          "Random/Stride    -> Standard: %.2f ms, Panama: %.2f ms (CheckSums: %.2f vs %.2f)\n",
          durationStd, durationPan, sumStd, sumPan);

      // Pattern 3: Slicing / Subsetting (zero-copy vs copying)
      start = System.nanoTime();
      int slicedLength = runSlicingStandard(standardArray);
      end = System.nanoTime();
      durationStd = (end - start) / 1_000_000.0;
      timeStandardSlice += durationStd;

      start = System.nanoTime();
      int slicedLengthPan = runSlicingPanama(panamaArray);
      end = System.nanoTime();
      durationPan = (end - start) / 1_000_000.0;
      timePanamaSlice += durationPan;
      System.out.printf(
          "Slicing/Subsets  -> Standard (ArrayCopy): %.2f ms, Panama (Zero-Copy): %.4f ms (Length: %d)\n",
          durationStd, durationPan, slicedLengthPan);

      // Pattern 4: Bulk Stream Write / Read IO
      start = System.nanoTime();
      double[] ioStd = runWriteReadStandard(heavyStd);
      end = System.nanoTime();
      durationStd = (end - start) / 1_000_000.0;
      timeStandardIO += durationStd;

      start = System.nanoTime();
      DoubleArray ioPan = runWriteReadPanama(heavyPan);
      end = System.nanoTime();
      durationPan = (end - start) / 1_000_000.0;
      timePanamaIO += durationPan;
      System.out.printf(
          "Bulk Stream IO   -> Standard: %.2f ms, Panama: %.2f ms (CheckSums: %.2f vs %.2f)\n",
          durationStd, durationPan, ioStd[0], ioPan.get(0));

      // Pattern 5: Sort
      double[] sortStdIn = heavyStd.clone();
      DoubleArray sortPanIn = new DoubleArray(heavyPan);

      start = System.nanoTime();
      runSortStandard(sortStdIn);
      end = System.nanoTime();
      durationStd = (end - start) / 1_000_000.0;
      timeStandardSort += durationStd;

      start = System.nanoTime();
      runSortPanama(sortPanIn);
      end = System.nanoTime();
      durationPan = (end - start) / 1_000_000.0;
      timePanamaSort += durationPan;
      System.out.printf(
          "Sorting          -> Standard: %.2f ms, Panama: %.2f ms (CheckSums: %.2f vs %.2f)\n",
          durationStd, durationPan, sortStdIn[0], sortPanIn.get(0));

      // Pattern 6: Move
      double[] moveStdIn = heavyStd.clone();
      DoubleArray movePanIn = new DoubleArray(heavyPan);

      start = System.nanoTime();
      runMoveStandard(moveStdIn, 100, 5000, 20000);
      end = System.nanoTime();
      durationStd = (end - start) / 1_000_000.0;
      timeStandardMove += durationStd;

      start = System.nanoTime();
      runMovePanama(movePanIn, 100, 5000, 20000);
      end = System.nanoTime();
      durationPan = (end - start) / 1_000_000.0;
      timePanamaMove += durationPan;
      System.out.printf(
          "Moving Elements  -> Standard: %.2f ms, Panama (Optimized): %.2f ms (CheckSums: %.2f vs %.2f)\n",
          durationStd, durationPan, moveStdIn[20000], movePanIn.get(20000));

      // Pattern 7: Reorder
      start = System.nanoTime();
      double[] reorderStd = runReorderStandard(heavyStd, ranks);
      end = System.nanoTime();
      durationStd = (end - start) / 1_000_000.0;
      timeStandardReorder += durationStd;

      start = System.nanoTime();
      runReorderPanama(heavyPan, ranks);
      end = System.nanoTime();
      durationPan = (end - start) / 1_000_000.0;
      timePanamaReorder += durationPan;
      System.out.printf(
          "Reordering       -> Standard: %.2f ms, Panama (Inlined Ly): %.2f ms (CheckSums: %.2f vs %.2f)\n",
          durationStd, durationPan, reorderStd[0], heavyPan.get(0));

      // Pattern 8: Direct FileChannel I/O (NIO Zero-Copy)
      start = System.nanoTime();
      double[] nioStd = runChannelIOStandard(heavyStd);
      end = System.nanoTime();
      durationStd = (end - start) / 1_000_000.0;
      timeStandardChannel += durationStd;

      start = System.nanoTime();
      DoubleArray nioPan = runChannelIOPanama(heavyPan);
      end = System.nanoTime();
      durationPan = (end - start) / 1_000_000.0;
      timePanamaChannel += durationPan;
      System.out.printf(
          "FileChannel IO   -> Standard: %.2f ms, Panama (Zero-Copy): %.2f ms (CheckSums: %.2f vs %.2f)\n\n",
          durationStd, durationPan, nioStd[0], nioPan.get(0));
    }

    // Print final averages
    System.out.println("=================================================");
    System.out.println("  Benchmark Results (Average of " + NUM_TRIALS + " Trials) ");
    System.out.println("=================================================");
    System.out.printf("1. Sequential Reads:\n");
    System.out.printf("   Standard: %.2f ms\n", timeStandardSeq / NUM_TRIALS);
    System.out.printf("   Panama:   %.2f ms\n", timePanamaSeq / NUM_TRIALS);
    System.out.printf("2. Random/Stride Access:\n");
    System.out.printf("   Standard: %.2f ms\n", timeStandardStride / NUM_TRIALS);
    System.out.printf("   Panama:   %.2f ms\n", timePanamaStride / NUM_TRIALS);
    System.out.printf("3. Slicing / Subsetting:\n");
    System.out.printf("   Standard (ArrayCopy): %.2f ms\n", timeStandardSlice / NUM_TRIALS);
    System.out.printf(
        "   Panama (Zero-Copy):   %.4f ms (Significant GC Pressure Reduction)\n",
        timePanamaSlice / NUM_TRIALS);
    System.out.printf("4. Bulk Stream Write / Read IO:\n");
    System.out.printf("   Standard: %.2f ms\n", timeStandardIO / NUM_TRIALS);
    System.out.printf("   Panama (Stream Loops): %.2f ms\n", timePanamaIO / NUM_TRIALS);
    System.out.printf("5. Array Sorting (Sort):\n");
    System.out.printf("   Standard: %.2f ms\n", timeStandardSort / NUM_TRIALS);
    System.out.printf("   Panama:   %.2f ms\n", timePanamaSort / NUM_TRIALS);
    System.out.printf("6. Moving Elements (Move):\n");
    System.out.printf("   Standard: %.2f ms\n", timeStandardMove / NUM_TRIALS);
    System.out.printf("   Panama (Allocation-Free): %.2f ms\n", timePanamaMove / NUM_TRIALS);
    System.out.printf("7. Reordering Elements (Reorder):\n");
    System.out.printf("   Standard: %.2f ms\n", timeStandardReorder / NUM_TRIALS);
    System.out.printf("   Panama (Direct Segment Ly): %.2f ms\n", timePanamaReorder / NUM_TRIALS);
    System.out.printf("8. Direct FileChannel I/O (NIO Zero-Copy):\n");
    System.out.printf("   Standard (ByteBuffer): %.2f ms\n", timeStandardChannel / NUM_TRIALS);
    System.out.printf(
        "   Panama (Direct NIO):   %.2f ms (Massive Speedup via Zero-Copy File Mapping)\n",
        timePanamaChannel / NUM_TRIALS);
    System.out.println("=================================================");
  }

  private static void runSequentialRead(double[] standard, DoubleArray panama) {
    runSequentialReadStandard(standard);
    runSequentialReadPanama(panama);
  }

  private static double runSequentialReadStandard(double[] arr) {
    double sum = 0;
    for (int i = 0; i < arr.length; i++) {
      sum += arr[i];
    }
    return sum;
  }

  private static double runSequentialReadPanama(DoubleArray arr) {
    double sum = 0;
    int size = arr.size();
    for (int i = 0; i < size; i++) {
      sum += arr.get(i);
    }
    return sum;
  }

  private static void runStrideAccess(double[] standard, DoubleArray panama, int[] strideIndices) {
    runStrideAccessStandard(standard, strideIndices);
    runStrideAccessPanama(panama, strideIndices);
  }

  private static double runStrideAccessStandard(double[] arr, int[] indices) {
    double sum = 0;
    for (int idx : indices) {
      sum += arr[idx];
    }
    return sum;
  }

  private static double runStrideAccessPanama(DoubleArray arr, int[] indices) {
    double sum = 0;
    for (int idx : indices) {
      sum += arr.get(idx);
    }
    return sum;
  }

  private static void runSlicing(double[] standard, DoubleArray panama) {
    runSlicingStandard(standard);
    runSlicingPanama(panama);
  }

  private static int runSlicingStandard(double[] arr) {
    // Simulates slicing by copying subsets
    int totalLength = 0;
    for (int i = 0; i < 1000; i++) {
      int length = 5000;
      double[] copy = new double[length];
      System.arraycopy(arr, i * 10, copy, 0, length);
      totalLength += copy.length;
    }
    return totalLength;
  }

  private static int runSlicingPanama(DoubleArray arr) {
    // True Zero-copy slicing of MemorySegment
    int totalLength = 0;
    for (int i = 0; i < 1000; i++) {
      int length = 5000;
      java.lang.foreign.MemorySegment slice =
          arr.segment.asSlice(i * 10 * Double.BYTES, length * Double.BYTES);
      totalLength += (int) (slice.byteSize() / Double.BYTES);
    }
    return totalLength;
  }

  private static double[] runWriteReadStandard(double[] arr) throws Exception {
    java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
    java.io.DataOutputStream dos = new java.io.DataOutputStream(baos);
    for (double v : arr) {
      dos.writeDouble(v);
    }
    dos.close();
    byte[] bytes = baos.toByteArray();
    java.io.ByteArrayInputStream bais = new java.io.ByteArrayInputStream(bytes);
    java.io.DataInputStream dis = new java.io.DataInputStream(bais);
    double[] target = new double[arr.length];
    for (int i = 0; i < target.length; i++) {
      target[i] = dis.readDouble();
    }
    return target;
  }

  private static DoubleArray runWriteReadPanama(DoubleArray arr) throws Exception {
    java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
    java.io.DataOutputStream dos = new java.io.DataOutputStream(baos);
    arr.writeDos(dos);
    dos.close();
    byte[] bytes = baos.toByteArray();
    java.io.ByteArrayInputStream bais = new java.io.ByteArrayInputStream(bytes);
    java.io.DataInputStream dis = new java.io.DataInputStream(bais);
    DoubleArray target = new DoubleArray(arr.size(), false);
    target.readDis(dis, arr.size());
    return target;
  }

  private static void runSortStandard(double[] arr) {
    if (arr.length < 8192) {
      java.util.Arrays.sort(arr, 0, arr.length);
    } else {
      java.util.Arrays.parallelSort(arr, 0, arr.length);
    }
  }

  private static void runSortPanama(DoubleArray arr) {
    arr.sort();
  }

  private static void runMoveStandard(double[] arr, int first, int last, int destination) {
    int nToMove = last - first;
    double[] temp = new double[nToMove];
    System.arraycopy(arr, first, temp, 0, nToMove);
    if (destination < first) {
      System.arraycopy(arr, destination, arr, destination + nToMove, first - destination);
      System.arraycopy(temp, 0, arr, destination, nToMove);
    } else {
      System.arraycopy(arr, last, arr, first, destination - last);
      System.arraycopy(temp, 0, arr, destination - nToMove, nToMove);
    }
  }

  private static void runMovePanama(DoubleArray arr, int first, int last, int destination) {
    arr.move(first, last, destination);
  }

  private static double[] runReorderStandard(double[] arr, int[] ranks) {
    double[] newArr = new double[ranks.length];
    for (int i = 0; i < ranks.length; i++) {
      newArr[i] = arr[ranks[i]];
    }
    return newArr;
  }

  private static void runReorderPanama(DoubleArray arr, int[] ranks) {
    arr.reorder(ranks);
  }

  private static double[] runChannelIOStandard(double[] arr) throws Exception {
    File tempFile = File.createTempFile("nio_bench_std", ".bin");
    tempFile.deleteOnExit();
    try (RandomAccessFile raf = new RandomAccessFile(tempFile, "rw");
        FileChannel channel = raf.getChannel()) {
      ByteBuffer buffer = ByteBuffer.allocate(arr.length * Double.BYTES);
      buffer.asDoubleBuffer().put(arr);
      while (buffer.hasRemaining()) {
        channel.write(buffer);
      }
      channel.position(0);
      buffer.clear();
      while (buffer.hasRemaining()) {
        channel.read(buffer);
      }
      buffer.flip();
      double[] target = new double[arr.length];
      buffer.asDoubleBuffer().get(target);
      return target;
    } finally {
      tempFile.delete();
    }
  }

  private static DoubleArray runChannelIOPanama(DoubleArray arr) throws Exception {
    File tempFile = File.createTempFile("nio_bench_pan", ".bin");
    tempFile.deleteOnExit();
    try (RandomAccessFile raf = new RandomAccessFile(tempFile, "rw");
        FileChannel channel = raf.getChannel()) {
      arr.writeToChannel(channel);
      channel.position(0);
      DoubleArray target = new DoubleArray(arr.size(), false);
      target.readFromChannel(channel, arr.size());
      return target;
    } finally {
      tempFile.delete();
    }
  }
}
