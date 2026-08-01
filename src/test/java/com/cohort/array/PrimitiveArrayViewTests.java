package com.cohort.array;

import com.cohort.util.Test;
import java.math.BigInteger;

class PrimitiveArrayViewTests {

  @org.junit.jupiter.api.Test
  void testDoubleArrayView() throws Throwable {
    DoubleArray parent = new DoubleArray(new double[] {1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0});

    // Test subsetting returns a DoubleArrayView
    PrimitiveArray sub = parent.subset(null, 1, 2, 6); // index: 1, 3, 5. values: 2.0, 4.0, 6.0
    Test.ensureTrue(sub instanceof DoubleArrayView, "Expected DoubleArrayView");
    Test.ensureEqual(sub.size(), 3, "Expected size 3");

    DoubleArrayView view = (DoubleArrayView) sub;
    Test.ensureEqual(view.get(0), 2.0, "");
    Test.ensureEqual(view.get(1), 4.0, "");
    Test.ensureEqual(view.get(2), 6.0, "");

    // Test bounds checking
    try {
      view.get(-1);
      Test.ensureTrue(false, "Should have thrown exception for negative index");
    } catch (IllegalArgumentException e) {
      // Expected
    }
    try {
      view.get(3);
      Test.ensureTrue(false, "Should have thrown exception for out of bounds index");
    } catch (IllegalArgumentException e) {
      // Expected
    }

    // Test read-only constraint
    try {
      view.set(0, 99.0);
      Test.ensureTrue(false, "Should have thrown exception for write attempt");
    } catch (UnsupportedOperationException e) {
      // Expected
    }

    // Test toArray and other conversion methods
    double[] dArr = view.toArray();
    Test.ensureEqual(dArr, new double[] {2.0, 4.0, 6.0}, "");

    String[] sArr = view.toStringArray();
    Test.ensureEqual(sArr, new String[] {"2.0", "4.0", "6.0"}, "");

    // Test testEquals
    DoubleArray expected = new DoubleArray(new double[] {2.0, 4.0, 6.0});
    Test.ensureEqual(view.testEquals(expected), "", "Should be equal");
  }

  @org.junit.jupiter.api.Test
  void testByteArrayView() throws Throwable {
    ByteArray parent = new ByteArray(new byte[] {10, 20, 30, 40, 50});
    PrimitiveArray sub = parent.subset(null, 0, 2, 4); // 10, 30, 50
    Test.ensureTrue(sub instanceof ByteArrayView, "Expected ByteArrayView");
    Test.ensureEqual(sub.size(), 3, "");

    ByteArrayView view = (ByteArrayView) sub;
    Test.ensureEqual(view.get(0), (byte) 10, "");
    Test.ensureEqual(view.get(1), (byte) 30, "");
    Test.ensureEqual(view.get(2), (byte) 50, "");

    try {
      view.set(0, (byte) 99);
      Test.ensureTrue(false, "Expected write to fail");
    } catch (UnsupportedOperationException e) {
      // Expected
    }
  }

  @org.junit.jupiter.api.Test
  void testCharArrayView() throws Throwable {
    CharArray parent = new CharArray(new char[] {'a', 'b', 'c', 'd', 'e'});
    PrimitiveArray sub = parent.subset(null, 1, 2, 4); // 'b', 'd'
    Test.ensureTrue(sub instanceof CharArrayView, "Expected CharArrayView");
    Test.ensureEqual(sub.size(), 2, "");

    CharArrayView view = (CharArrayView) sub;
    Test.ensureEqual(view.get(0), 'b', "");
    Test.ensureEqual(view.get(1), 'd', "");

    try {
      view.set(0, 'x');
      Test.ensureTrue(false, "Expected write to fail");
    } catch (UnsupportedOperationException e) {
      // Expected
    }
  }

  @org.junit.jupiter.api.Test
  void testFloatArrayView() throws Throwable {
    FloatArray parent = new FloatArray(new float[] {1.1f, 2.2f, 3.3f, 4.4f});
    PrimitiveArray sub = parent.subset(null, 1, 2, 3); // 2.2f, 4.4f
    Test.ensureTrue(sub instanceof FloatArrayView, "Expected FloatArrayView");
    Test.ensureEqual(sub.size(), 2, "");

    FloatArrayView view = (FloatArrayView) sub;
    Test.ensureEqual(view.get(0), 2.2f, "");
    Test.ensureEqual(view.get(1), 4.4f, "");

    try {
      view.set(0, 9.9f);
      Test.ensureTrue(false, "Expected write to fail");
    } catch (UnsupportedOperationException e) {
      // Expected
    }
  }

  @org.junit.jupiter.api.Test
  void testIntArrayView() throws Throwable {
    IntArray parent = new IntArray(new int[] {100, 200, 300, 400});
    PrimitiveArray sub = parent.subset(null, 0, 3, 3); // 100, 400
    Test.ensureTrue(sub instanceof IntArrayView, "Expected IntArrayView");
    Test.ensureEqual(sub.size(), 2, "");

    IntArrayView view = (IntArrayView) sub;
    Test.ensureEqual(view.get(0), 100, "");
    Test.ensureEqual(view.get(1), 400, "");

    try {
      view.set(0, 999);
      Test.ensureTrue(false, "Expected write to fail");
    } catch (UnsupportedOperationException e) {
      // Expected
    }
  }

  @org.junit.jupiter.api.Test
  void testLongArrayView() throws Throwable {
    LongArray parent = new LongArray(new long[] {1000L, 2000L, 3000L});
    PrimitiveArray sub = parent.subset(null, 1, 1, 2); // 2000L, 3000L
    Test.ensureTrue(sub instanceof LongArrayView, "Expected LongArrayView");
    Test.ensureEqual(sub.size(), 2, "");

    LongArrayView view = (LongArrayView) sub;
    Test.ensureEqual(view.get(0), 2000L, "");
    Test.ensureEqual(view.get(1), 3000L, "");

    try {
      view.set(0, 9999L);
      Test.ensureTrue(false, "Expected write to fail");
    } catch (UnsupportedOperationException e) {
      // Expected
    }
  }

  @org.junit.jupiter.api.Test
  void testShortArrayView() throws Throwable {
    ShortArray parent = new ShortArray(new short[] {5, 10, 15, 20});
    PrimitiveArray sub = parent.subset(null, 0, 2, 2); // 5, 15
    Test.ensureTrue(sub instanceof ShortArrayView, "Expected ShortArrayView");
    Test.ensureEqual(sub.size(), 2, "");

    ShortArrayView view = (ShortArrayView) sub;
    Test.ensureEqual(view.get(0), (short) 5, "");
    Test.ensureEqual(view.get(1), (short) 15, "");

    try {
      view.set(0, (short) 9);
      Test.ensureTrue(false, "Expected write to fail");
    } catch (UnsupportedOperationException e) {
      // Expected
    }
  }

  @org.junit.jupiter.api.Test
  void testStringArrayView() throws Throwable {
    StringArray parent = new StringArray(new String[] {"A", "B", "C", "D"});
    PrimitiveArray sub = parent.subset(null, 1, 2, 3); // "B", "D"
    Test.ensureTrue(sub instanceof StringArrayView, "Expected StringArrayView");
    Test.ensureEqual(sub.size(), 2, "");

    StringArrayView view = (StringArrayView) sub;
    Test.ensureEqual(view.get(0), "B", "");
    Test.ensureEqual(view.get(1), "D", "");

    try {
      view.set(0, "X");
      Test.ensureTrue(false, "Expected write to fail");
    } catch (UnsupportedOperationException e) {
      // Expected
    }
  }

  @org.junit.jupiter.api.Test
  void testUByteArrayView() throws Throwable {
    UByteArray parent = new UByteArray(new byte[] {10, 20, 30});
    PrimitiveArray sub = parent.subset(null, 1, 1, 2); // 20, 30
    Test.ensureTrue(sub instanceof UByteArrayView, "Expected UByteArrayView");
    Test.ensureEqual(sub.size(), 2, "");

    UByteArrayView view = (UByteArrayView) sub;
    Test.ensureEqual(view.get(0), (short) 20, "");
    Test.ensureEqual(view.get(1), (short) 30, "");

    try {
      view.set(0, (short) 99);
      Test.ensureTrue(false, "Expected write to fail");
    } catch (UnsupportedOperationException e) {
      // Expected
    }
  }

  @org.junit.jupiter.api.Test
  void testUShortArrayView() throws Throwable {
    UShortArray parent = new UShortArray(new short[] {100, 200, 300});
    PrimitiveArray sub = parent.subset(null, 0, 2, 2); // 100, 300
    Test.ensureTrue(sub instanceof UShortArrayView, "Expected UShortArrayView");
    Test.ensureEqual(sub.size(), 2, "");

    UShortArrayView view = (UShortArrayView) sub;
    Test.ensureEqual(view.get(0), 100, "");
    Test.ensureEqual(view.get(1), 300, "");

    try {
      view.set(0, 999);
      Test.ensureTrue(false, "Expected write to fail");
    } catch (UnsupportedOperationException e) {
      // Expected
    }
  }

  @org.junit.jupiter.api.Test
  void testUIntArrayView() throws Throwable {
    UIntArray parent = new UIntArray(new int[] {1000, 2000, 3000});
    PrimitiveArray sub = parent.subset(null, 1, 1, 2); // 2000, 3000
    Test.ensureTrue(sub instanceof UIntArrayView, "Expected UIntArrayView");
    Test.ensureEqual(sub.size(), 2, "");

    UIntArrayView view = (UIntArrayView) sub;
    Test.ensureEqual(view.get(0), 2000L, "");
    Test.ensureEqual(view.get(1), 3000L, "");

    try {
      view.set(0, 9999L);
      Test.ensureTrue(false, "Expected write to fail");
    } catch (UnsupportedOperationException e) {
      // Expected
    }
  }

  @org.junit.jupiter.api.Test
  void testULongArrayView() throws Throwable {
    ULongArray parent = new ULongArray(new long[] {10000L, 20000L, 30000L});
    PrimitiveArray sub = parent.subset(null, 0, 2, 2); // 10000, 30000
    Test.ensureTrue(sub instanceof ULongArrayView, "Expected ULongArrayView");
    Test.ensureEqual(sub.size(), 2, "");

    ULongArrayView view = (ULongArrayView) sub;
    Test.ensureEqual(view.get(0), new BigInteger("10000"), "");
    Test.ensureEqual(view.get(1), new BigInteger("30000"), "");

    try {
      view.set(0, new BigInteger("9999"));
      Test.ensureTrue(false, "Expected write to fail");
    } catch (UnsupportedOperationException e) {
      // Expected
    }
  }
}
