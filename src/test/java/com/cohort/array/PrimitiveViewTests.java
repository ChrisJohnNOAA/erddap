package com.cohort.array;

import com.cohort.util.Test;
import java.util.BitSet;

class PrimitiveViewTests {

  @org.junit.jupiter.api.Test
  void testBasicViewAndGetters() {
    IntArray ia = new IntArray(new int[] {10, 20, 30, 40, 50, 60, 70, 80});
    PrimitiveView view = new PrimitiveView(ia, 1, 2, 3); // indices 1, 3, 5 -> 20, 40, 60

    Test.ensureEqual(view.size(), 3, "view size");
    Test.ensureEqual(view.getInt(0), 20, "get(0)");
    Test.ensureEqual(view.getInt(1), 40, "get(1)");
    Test.ensureEqual(view.getInt(2), 60, "get(2)");
    Test.ensureEqual(view.getDouble(0), 20.0, "getDouble(0)");
    Test.ensureEqual(view.getString(1), "40", "getString(1)");
    Test.ensureTrue(view.materialized == null, "materialized should be null");
  }

  @org.junit.jupiter.api.Test
  void testChainingAndFlattening() {
    DoubleArray da =
        new DoubleArray(new double[] {0.5, 1.5, 2.5, 3.5, 4.5, 5.5, 6.5, 7.5, 8.5, 9.5});
    PrimitiveView v1 =
        new PrimitiveView(da, 1, 2, 4); // indices 1, 3, 5, 7 -> values 1.5, 3.5, 5.5, 7.5
    PrimitiveView v2 =
        new PrimitiveView(v1, 1, 2, 2); // v1 indices 1, 3 -> da indices 3, 7 -> values 3.5, 7.5

    Test.ensureEqual(v2.source, da, "v2 source flattened to original array");
    Test.ensureEqual(v2.offset, 3, "v2 offset");
    Test.ensureEqual(v2.stride, 4, "v2 stride");
    Test.ensureEqual(v2.size(), 2, "v2 size");
    Test.ensureEqual(v2.getDouble(0), 3.5, "v2 getDouble(0)");
    Test.ensureEqual(v2.getDouble(1), 7.5, "v2 getDouble(1)");
  }

  @org.junit.jupiter.api.Test
  void testChainingOverMaterializedView() {
    IntArray ia = new IntArray(new int[] {10, 20, 30, 40, 50, 60});
    PrimitiveView v1 = new PrimitiveView(ia, 0, 1, 6); // 10, 20, 30, 40, 50, 60
    v1.setInt(1, 999); // Materializes v1: 10, 999, 30, 40, 50, 60

    PrimitiveView v2 = new PrimitiveView(v1, 1, 2, 2); // indices 1, 3 of v1 -> 999, 40

    Test.ensureEqual(v2.getInt(0), 999, "v2 getInt(0) from materialized source");
    Test.ensureEqual(v2.getInt(1), 40, "v2 getInt(1) from materialized source");
  }

  @org.junit.jupiter.api.Test
  void testRawMissingValueFidelity() {
    // ShortArray max value 32767 as raw missing value
    ShortArray sa = new ShortArray(new short[] {100, Short.MAX_VALUE, 300});
    PrimitiveView view = new PrimitiveView(sa, 0, 1, 3);

    Test.ensureEqual(view.getRawInt(1), 32767, "raw int missing value preserved");

    PrimitiveArray mat = view.materialize();
    Test.ensureEqual(mat.getRawInt(1), 32767, "materialized raw int missing value preserved");
    Test.ensureTrue(mat instanceof ShortArray, "materialized type is ShortArray");
  }

  @org.junit.jupiter.api.Test
  void testPointSetterCOWIsolation() {
    IntArray ia = new IntArray(new int[] {1, 2, 3, 4, 5});
    PrimitiveView view = new PrimitiveView(ia, 1, 1, 3); // values 2, 3, 4

    Test.ensureTrue(view.materialized == null, "initial materialized is null");

    // Point setter should materialize
    view.setInt(1, 99);

    Test.ensureTrue(view.materialized != null, "materialized is set after mutation");
    Test.ensureEqual(view.getInt(1), 99, "view updated value");
    Test.ensureEqual(ia.getInt(2), 3, "original source array unaltered");
  }

  @org.junit.jupiter.api.Test
  void testOptimizedMutatorJustKeep() {
    StringArray sa = new StringArray(new String[] {"a", "b", "c", "d", "e"});
    PrimitiveView view = new PrimitiveView(sa, 0, 1, 5);

    BitSet keep = new BitSet();
    keep.set(1); // "b"
    keep.set(3); // "d"

    view.justKeep(keep);

    Test.ensureTrue(view.materialized != null, "materialized set after justKeep");
    Test.ensureEqual(view.size(), 2, "size updated");
    Test.ensureEqual(view.offset, 0, "offset reset");
    Test.ensureEqual(view.stride, 1, "stride reset");
    Test.ensureEqual(view.getString(0), "b", "first kept element");
    Test.ensureEqual(view.getString(1), "d", "second kept element");
  }

  @org.junit.jupiter.api.Test
  void testOptimizedMutatorRemoveRangeAndRemove() {
    FloatArray fa = new FloatArray(new float[] {10f, 20f, 30f, 40f, 50f});
    PrimitiveView view = new PrimitiveView(fa, 0, 1, 5);

    view.remove(2); // removes 30f -> 10f, 20f, 40f, 50f
    Test.ensureEqual(view.size(), 4, "size after remove");
    Test.ensureEqual(view.getFloat(2), 40f, "element at index 2 after remove");

    view.removeRange(1, 3); // removes 20f, 40f -> 10f, 50f
    Test.ensureEqual(view.size(), 2, "size after removeRange");
    Test.ensureEqual(view.getFloat(0), 10f, "element at index 0");
    Test.ensureEqual(view.getFloat(1), 50f, "element at index 1");
  }

  @org.junit.jupiter.api.Test
  void testClearStateReinitialization() {
    ByteArray ba = new ByteArray(new byte[] {1, 2, 3, 4});
    PrimitiveView view = new PrimitiveView(ba, 1, 1, 2); // values 2, 3

    view.materialize();
    Test.ensureTrue(view.materialized != null, "materialized not null");

    view.clear();
    Test.ensureEqual(view.size(), 0, "size 0 after clear");
    Test.ensureTrue(view.materialized == null, "materialized null after clear");
    Test.ensureEqual(view.offset, 0, "offset 0 after clear");
    Test.ensureEqual(view.stride, 1, "stride 1 after clear");
  }

  @org.junit.jupiter.api.Test
  void testSubsetReturnsFlatView() {
    LongArray la = new LongArray(new long[] {100L, 200L, 300L, 400L, 500L, 600L});
    PrimitiveView view = new PrimitiveView(la, 1, 1, 4); // 200L, 300L, 400L, 500L

    PrimitiveArray sub = view.subset(null, 1, 2, 3); // indices 1, 3 of view -> 300L, 500L

    Test.ensureTrue(sub instanceof PrimitiveView, "subset returns PrimitiveView");
    PrimitiveView subView = (PrimitiveView) sub;
    Test.ensureEqual(subView.source, la, "flattened source is original LongArray");
    Test.ensureEqual(subView.size(), 2, "subset size");
    Test.ensureEqual(subView.getLong(0), 300L, "subset getLong(0)");
    Test.ensureEqual(subView.getLong(1), 500L, "subset getLong(1)");
  }

  @org.junit.jupiter.api.Test
  void testFormatters() {
    IntArray ia = new IntArray(new int[] {1, 2, 3, 4});
    PrimitiveView view = new PrimitiveView(ia, 1, 1, 2); // 2, 3

    Test.ensureEqual(view.toString(), "2, 3", "toString format");
    Test.ensureEqual(view.toCSVString(), "2,3", "toCSVString format");
    Test.ensureEqual(view.toJsonCsvString(), "2, 3", "toJsonCsvString format");
  }

  @org.junit.jupiter.api.Test
  void testOffsetScaleViewEvaluation() {
    IntArray ia = new IntArray(new int[] {-12430, -12418, 100});
    PrimitiveArray osv = ia.asOffsetScaleView(false, PAType.FLOAT, 0.01, 0.0);

    Test.ensureTrue(osv instanceof OffsetScaleView, "is OffsetScaleView");
    Test.ensureEqual(osv.size(), 3, "size");
    Test.ensureEqual(osv.getFloat(0), -124.3f, "getFloat(0)");
    Test.ensureEqual(osv.getFloat(1), -124.18f, "getFloat(1)");
    Test.ensureEqual(osv.getFloat(2), 1.0f, "getFloat(2)");

    // Negative scale and offset
    ByteArray ba = new ByteArray(new byte[] {10, 20, 30});
    PrimitiveArray osv2 = ba.asOffsetScaleView(false, PAType.DOUBLE, -2.0, 100.0);
    Test.ensureEqual(osv2.getDouble(0), 80.0, "10 * -2 + 100");
    Test.ensureEqual(osv2.getDouble(1), 60.0, "20 * -2 + 100");
    Test.ensureEqual(osv2.getDouble(2), 40.0, "30 * -2 + 100");
  }

  @org.junit.jupiter.api.Test
  void testOffsetScaleViewMissingValues() {
    ShortArray sa = new ShortArray(new short[] {10, Short.MAX_VALUE});
    sa.setMaxIsMV(true);
    PrimitiveArray osv = sa.asOffsetScaleView(false, PAType.DOUBLE, 0.1, 5.0);

    Test.ensureEqual(osv.getDouble(0), 6.0, "10 * 0.1 + 5");
    Test.ensureTrue(Double.isNaN(osv.getDouble(1)), "Short.MAX_VALUE is missing value -> NaN");
  }

  @org.junit.jupiter.api.Test
  void testOffsetScaleViewChainingAndFlattening() {
    IntArray ia = new IntArray(new int[] {10, 20, 30, 40});
    PrimitiveArray v1 = ia.asOffsetScaleView(false, PAType.DOUBLE, 2.0, 10.0); // 30, 50, 70, 90
    PrimitiveArray v2 = v1.asOffsetScaleView(false, PAType.DOUBLE, 0.5, -5.0); // 0.5*(2x+10)-5 = x

    Test.ensureTrue(v2 instanceof OffsetScaleView, "v2 is OffsetScaleView");
    OffsetScaleView osv2 = (OffsetScaleView) v2;
    Test.ensureEqual(osv2.source, ia, "flattened to root IntArray");
    Test.ensureEqual(osv2.scale, 1.0, "composed scale 2.0 * 0.5 = 1.0");
    Test.ensureEqual(osv2.addOffset, 0.0, "composed offset 0.5 * 10 - 5 = 0.0");
    Test.ensureEqual(osv2.getDouble(0), 10.0, "v2.getDouble(0)");
    Test.ensureEqual(osv2.getDouble(1), 20.0, "v2.getDouble(1)");
  }

  @org.junit.jupiter.api.Test
  void testOffsetScaleViewMaterializationAndSubsetting() {
    FloatArray fa = new FloatArray(new float[] {1f, 2f, 3f, 4f});
    PrimitiveArray osv = fa.asOffsetScaleView(false, PAType.DOUBLE, 10.0, 1.0); // 11, 21, 31, 41

    // Subsetting
    PrimitiveArray sub = osv.subset(null, 1, 2, 3); // indices 1, 3 -> 21, 41
    Test.ensureTrue(sub instanceof OffsetScaleView, "subset is OffsetScaleView");
    Test.ensureEqual(sub.getDouble(0), 21.0, "sub getDouble(0)");
    Test.ensureEqual(sub.getDouble(1), 41.0, "sub getDouble(1)");

    // Materialization on mutation
    osv.setDouble(0, 999.0);
    Test.ensureEqual(osv.getDouble(0), 999.0, "updated mutated view");
    Test.ensureEqual(fa.getFloat(0), 1f, "source unchanged");
  }

  @org.junit.jupiter.api.Test
  void testOffsetScaleViewTypePredicates() {
    ByteArray ba = new ByteArray(new byte[] {1, 2, 3});
    PrimitiveArray osv = ba.asOffsetScaleView(false, PAType.FLOAT, 0.1, 0.0);

    Test.ensureEqual(osv.elementType(), PAType.FLOAT, "elementType is FLOAT");
    Test.ensureTrue(osv.isFloatingPointType(), "isFloatingPointType is true");
    Test.ensureTrue(!osv.isIntegerType(), "isIntegerType is false");
    Test.ensureTrue(!osv.isUnsigned(), "isUnsigned is false");
    Test.ensureEqual(osv.elementSize(), 4, "elementSize is 4 for FLOAT");
    Test.ensureTrue(Double.isNaN(osv.missingValueAsDouble()), "missingValueAsDouble is NaN");
  }
}
