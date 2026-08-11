package com.cohort.array;

import com.cohort.util.String2;
import com.cohort.util.Test;
import java.util.BitSet;

public class PrimitiveViewTests {

  @org.junit.jupiter.api.Test
  public void testPrimitiveViewSubsettingAndCOW() throws Throwable {
    String2.log("*** PrimitiveViewTests.testPrimitiveViewSubsettingAndCOW");

    // Create a base DoubleArray
    DoubleArray da = new DoubleArray(new double[] {0.0, 10.0, 20.0, 30.0, 40.0, 50.0});

    // 1. Check subset returns a PrimitiveView when target pa is null
    PrimitiveArray sub1 = da.subset(1, 2, 4); // should be values [10.0, 30.0]
    Test.ensureEqual(sub1.getClass().getName(), "com.cohort.array.PrimitiveView", "");
    Test.ensureEqual(sub1.size(), 2, "");
    Test.ensureEqual(sub1.getDouble(0), 10.0, "");
    Test.ensureEqual(sub1.getDouble(1), 30.0, "");

    // 2. Test Read-Only Delegation
    Test.ensureEqual(sub1.getString(0), "10.0", "");
    Test.ensureEqual(sub1.getNiceDouble(1), 30.0, "");

    // 3. Test View Chaining (Subset on PrimitiveView without materialization)
    PrimitiveView pv1 = (PrimitiveView) sub1;
    PrimitiveArray sub2 = pv1.subset(0, 1, 1); // should be value [10.0, 30.0]
    Test.ensureEqual(sub2.getClass().getName(), "com.cohort.array.PrimitiveView", "");
    PrimitiveView pv2 = (PrimitiveView) sub2;
    Test.ensureTrue(pv1.materialized == null, "pv1 should not be materialized");
    Test.ensureTrue(pv2.materialized == null, "pv2 should not be materialized");
    Test.ensureEqual(pv2.offset, 1, ""); // from 1st element mapped of pv1
    Test.ensureEqual(pv2.stride, 2, ""); // stride flattened
    Test.ensureEqual(pv2.getDouble(0), 10.0, "");
    Test.ensureEqual(pv2.getDouble(1), 30.0, "");

    // 4. Test Copy-On-Write (COW) Mutation Isolation
    pv1.setDouble(0, 99.0); // should trigger COW
    Test.ensureNotNull(pv1.materialized, "pv1 should be materialized after setDouble");
    Test.ensureEqual(pv1.getDouble(0), 99.0, "");
    Test.ensureEqual(pv1.getDouble(1), 30.0, "");

    // Verify source array is untouched
    Test.ensureEqual(da.getDouble(1), 10.0, "Source array MUST NOT be modified");

    // 5. Test Optimized Mutator: justKeep
    PrimitiveArray sub3 =
        da.subset(0, 1, 5); // view of all elements [0.0, 10.0, 20.0, 30.0, 40.0, 50.0]
    PrimitiveView pv3 = (PrimitiveView) sub3;
    BitSet bs = new BitSet();
    bitsetSet(bs, 1, 3, 5); // values at index 1, 3, 5 are 10.0, 30.0, 50.0
    pv3.justKeep(bs);
    Test.ensureNotNull(pv3.materialized, "pv3 must be materialized");
    Test.ensureEqual(pv3.size(), 3, "");
    Test.ensureEqual(pv3.getDouble(0), 10.0, "");
    Test.ensureEqual(pv3.getDouble(1), 30.0, "");
    Test.ensureEqual(pv3.getDouble(2), 50.0, "");

    // Verify source array is untouched
    Test.ensureEqual(da.getDouble(3), 30.0, "");

    // 6. Test Optimized Mutator: removeRange
    PrimitiveArray sub4 = da.subset(0, 1, 5); // view of all elements
    PrimitiveView pv4 = (PrimitiveView) sub4;
    pv4.removeRange(
        1, 4); // remove indices 1, 2, 3 (values 10.0, 20.0, 30.0) -> left with [0.0, 40.0, 50.0]
    Test.ensureNotNull(pv4.materialized, "pv4 must be materialized");
    Test.ensureEqual(pv4.size(), 3, "");
    Test.ensureEqual(pv4.getDouble(0), 0.0, "");
    Test.ensureEqual(pv4.getDouble(1), 40.0, "");
    Test.ensureEqual(pv4.getDouble(2), 50.0, "");

    // Verify source array is untouched
    Test.ensureEqual(da.getDouble(1), 10.0, "");

    String2.log("*** PrimitiveViewTests.testPrimitiveViewSubsettingAndCOW passed successfully!");
  }

  private void bitsetSet(BitSet bs, int... indices) {
    for (int idx : indices) {
      bs.set(idx);
    }
  }
}
