package com.cohort.array;

import com.cohort.util.Test;
import gov.noaa.pfel.coastwatch.pointdata.Table;

class PaddedPrimitiveViewTests {

  @org.junit.jupiter.api.Test
  void testBasicPaddedViewAndGetters() {
    DoubleArray da = new DoubleArray(new double[] {1.0, 2.5, 3.7});
    PaddedPrimitiveView view = new PaddedPrimitiveView(da, 6);

    Test.ensureEqual(view.size(), 6, "view size");
    Test.ensureEqual(view.getDouble(0), 1.0, "get(0)");
    Test.ensureEqual(view.getDouble(1), 2.5, "get(1)");
    Test.ensureEqual(view.getDouble(2), 3.7, "get(2)");
    Test.ensureTrue(Double.isNaN(view.getDouble(3)), "get(3) is NaN");
    Test.ensureTrue(Double.isNaN(view.getDouble(5)), "get(5) is NaN");
    Test.ensureTrue(view.isMissingValue(3), "isMissingValue(3)");
    Test.ensureTrue(!view.isMissingValue(0), "isMissingValue(0)");

    // Boundary index checks
    try {
      view.getDouble(-1);
      Test.ensureTrue(false, "should throw IndexOutOfBoundsException for -1");
    } catch (IndexOutOfBoundsException e) {
      // expected
    }

    try {
      view.getDouble(6);
      Test.ensureTrue(false, "should throw IndexOutOfBoundsException for index == targetSize");
    } catch (IndexOutOfBoundsException e) {
      // expected
    }
  }

  @org.junit.jupiter.api.Test
  void testPaddedViewTypesAndMissingValues() {
    IntArray ia = new IntArray(new int[] {10, 20, 30});
    PaddedPrimitiveView pIa = new PaddedPrimitiveView(ia, 5);

    Test.ensureEqual(pIa.getInt(0), 10, "int get(0)");
    Test.ensureEqual(pIa.getInt(2), 30, "int get(2)");
    Test.ensureEqual(pIa.getInt(3), Integer.MAX_VALUE, "int padded missing value");
    Test.ensureTrue(pIa.isMissingValue(3), "int isMissingValue(3)");

    StringArray sa = new StringArray(new String[] {"alpha", "beta"});
    PaddedPrimitiveView pSa = new PaddedPrimitiveView(sa, 4);

    Test.ensureEqual(pSa.getString(0), "alpha", "string get(0)");
    Test.ensureEqual(pSa.getString(1), "beta", "string get(1)");
    Test.ensureEqual(pSa.getString(2), "", "string padded missing value");
    Test.ensureEqual(pSa.getJsonString(2), "null", "string padded json missing value");
    Test.ensureTrue(pSa.isMissingValue(2), "string isMissingValue(2)");

    FloatArray fa = new FloatArray(new float[] {1.1f, 2.2f});
    PaddedPrimitiveView pFa = new PaddedPrimitiveView(fa, 4);
    Test.ensureEqual(pFa.getFloat(0), 1.1f, "float get(0)");
    Test.ensureTrue(Float.isNaN(pFa.getFloat(2)), "float padded missing value");
  }

  @org.junit.jupiter.api.Test
  void testCustomMissingValue() {
    DoubleArray da = new DoubleArray(new double[] {10.0, 20.0});
    PaddedPrimitiveView pCustom = new PaddedPrimitiveView(da, 4, -999.0);

    Test.ensureEqual(pCustom.getDouble(0), 10.0, "get(0)");
    Test.ensureEqual(pCustom.getDouble(2), -999.0, "get(2) custom missing value");
    Test.ensureEqual(pCustom.getDouble(3), -999.0, "get(3) custom missing value");
  }

  @org.junit.jupiter.api.Test
  void testFlatteningNestedPaddedViews() {
    IntArray ia = new IntArray(new int[] {1, 2, 3});
    PaddedPrimitiveView pv1 = new PaddedPrimitiveView(ia, 5);
    PaddedPrimitiveView pv2 = new PaddedPrimitiveView(pv1, 8);

    Test.ensureEqual(pv2.source, ia, "pv2 source unwrapped to original IntArray");
    Test.ensureEqual(pv2.size(), 8, "pv2 target size");
    Test.ensureEqual(pv2.getInt(0), 1, "get(0)");
    Test.ensureEqual(pv2.getInt(2), 3, "get(2)");
    Test.ensureEqual(pv2.getInt(4), Integer.MAX_VALUE, "get(4)");
    Test.ensureEqual(pv2.getInt(7), Integer.MAX_VALUE, "get(7)");
  }

  @org.junit.jupiter.api.Test
  void testMinMaxAndStatsSafeguards() {
    DoubleArray da = new DoubleArray(new double[] {10.0, 50.0, 20.0});
    PaddedPrimitiveView pDa = new PaddedPrimitiveView(da, 10);

    double[] daStats = da.calculateStats();
    double[] viewStats = pDa.calculateStats();

    Test.ensureEqual(viewStats[PrimitiveArray.STATS_N], daStats[PrimitiveArray.STATS_N], "stats N");
    Test.ensureEqual(viewStats[PrimitiveArray.STATS_MIN], daStats[PrimitiveArray.STATS_MIN], "stats MIN");
    Test.ensureEqual(viewStats[PrimitiveArray.STATS_MAX], daStats[PrimitiveArray.STATS_MAX], "stats MAX");
    Test.ensureEqual(viewStats[PrimitiveArray.STATS_SUM], daStats[PrimitiveArray.STATS_SUM], "stats SUM");

    int[] nmmDa = da.getNMinMaxIndex();
    int[] nmmView = pDa.getNMinMaxIndex();
    Test.ensureEqual(nmmView[0], nmmDa[0], "getNMinMaxIndex valid count");
    Test.ensureEqual(nmmView[1], nmmDa[1], "getNMinMaxIndex minIndex");
    Test.ensureEqual(nmmView[2], nmmDa[2], "getNMinMaxIndex maxIndex");
  }

  @org.junit.jupiter.api.Test
  void testMaterializeAndMutation() {
    IntArray ia = new IntArray(new int[] {100, 200});
    PaddedPrimitiveView view = new PaddedPrimitiveView(ia, 4);

    Test.ensureTrue(view.materialized == null, "materialized initially null");

    PrimitiveArray mat = view.materialize();
    Test.ensureEqual(mat.size(), 4, "materialized size");
    Test.ensureEqual(mat.getInt(0), 100, "mat(0)");
    Test.ensureEqual(mat.getInt(1), 200, "mat(1)");
    Test.ensureEqual(mat.getInt(2), Integer.MAX_VALUE, "mat(2)");
    Test.ensureEqual(mat.getInt(3), Integer.MAX_VALUE, "mat(3)");

    // Setter materializes and isolates source
    PaddedPrimitiveView view2 = new PaddedPrimitiveView(ia, 4);
    view2.setInt(2, 300);
    Test.ensureEqual(view2.getInt(2), 300, "view2 updated index 2");
    Test.ensureEqual(ia.size(), 2, "source ia size untouched");
  }

  @org.junit.jupiter.api.Test
  void testTableMakeColumnsSameSizeRefactoring() throws Exception {
    Table table = new Table();
    DoubleArray col1 = new DoubleArray(new double[] {1.0, 2.0, 3.0, 4.0, 5.0});
    StringArray col2 = new StringArray(new String[] {"A", "B"});

    table.addColumn("col1", col1);
    table.addColumn("col2", col2);

    Test.ensureEqual(table.getColumn(0).size(), 5, "col1 initial size");
    Test.ensureEqual(table.getColumn(1).size(), 2, "col2 initial size");

    table.makeColumnsSameSize();

    Test.ensureEqual(table.nRows(), 5, "table nRows after makeColumnsSameSize");
    Test.ensureEqual(table.getColumn(0).size(), 5, "col1 size after makeColumnsSameSize");
    Test.ensureEqual(table.getColumn(1).size(), 5, "col2 size after makeColumnsSameSize");

    Test.ensureTrue(!(table.getColumn(0) instanceof PaddedPrimitiveView), "col1 was already maxSize");
    Test.ensureTrue(table.getColumn(1) instanceof PaddedPrimitiveView, "col2 is PaddedPrimitiveView");

    PaddedPrimitiveView viewCol2 = (PaddedPrimitiveView) table.getColumn(1);
    Test.ensureEqual(viewCol2.getString(0), "A", "row 0");
    Test.ensureEqual(viewCol2.getString(1), "B", "row 1");
    Test.ensureEqual(viewCol2.getString(2), "", "row 2 missing value");
    Test.ensureEqual(viewCol2.getString(4), "", "row 4 missing value");

    // Check stats safeguard
    double[] col2Stats = viewCol2.calculateStats();
    Test.ensureEqual(col2Stats[PrimitiveArray.STATS_N], 0.0, "col2 stats N (strings give 0 valid doubles)");

    // Test formatting output
    String csv = table.saveAsCsvASCIIString();
    Test.ensureTrue(csv.contains("col1,col2"), "CSV header");
    Test.ensureTrue(csv.contains("1.0,A"), "CSV row 0");
    Test.ensureTrue(csv.contains("3.0,"), "CSV row 2 padded empty string");
  }
}
