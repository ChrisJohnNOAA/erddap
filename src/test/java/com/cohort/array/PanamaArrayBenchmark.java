package com.cohort.array;

import java.util.Random;

/**
 * Isolated Micro-Benchmark for comparing Java double[] against Panama-backed DoubleArray.
 * Evaluates Sequential Reads, Random/Stride Access, and Slicing/Subsetting.
 */
public class PanamaArrayBenchmark {

    private static final int ARRAY_SIZE = 10_000_000;
    private static final int NUM_TRIALS = 5;

    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("  ERDDAP Panama-backed DoubleArray Benchmark  ");
        System.out.println("=================================================");
        System.out.printf("Array size: %,d elements\n", ARRAY_SIZE);
        System.out.printf("Warmup and running %d trials per pattern...\n\n", NUM_TRIALS);

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

        // WARMUP
        System.out.println("Warming up JIT...");
        runSequentialRead(standardArray, panamaArray);
        runStrideAccess(standardArray, panamaArray, strideIndices);
        runSlicing(standardArray, panamaArray);
        System.out.println("Warmup complete. Starting benchmark trials...\n");

        // Benchmark trial measurements
        double timeStandardSeq = 0, timePanamaSeq = 0;
        double timeStandardStride = 0, timePanamaStride = 0;
        double timeStandardSlice = 0, timePanamaSlice = 0;

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
            System.out.printf("Sequential Reads -> Standard: %.2f ms, Panama: %.2f ms (CheckSums: %.2f vs %.2f)\n",
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
            System.out.printf("Random/Stride    -> Standard: %.2f ms, Panama: %.2f ms (CheckSums: %.2f vs %.2f)\n",
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
            System.out.printf("Slicing/Subsets  -> Standard (ArrayCopy): %.2f ms, Panama (Zero-Copy): %.4f ms (Length: %d)\n\n",
                durationStd, durationPan, slicedLengthPan);
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
        System.out.printf("   Panama (Zero-Copy):   %.4f ms (Significant GC Pressure Reduction)\n", timePanamaSlice / NUM_TRIALS);
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
            PanamaCoreArray slice = arr.core.slice(i * 10, length);
            totalLength += (int) slice.capacity();
        }
        return totalLength;
    }
}
