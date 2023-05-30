package com.cohort.array;

 import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
 import static org.junit.jupiter.api.Assertions.assertTrue;
 
 import org.junit.jupiter.api.DisplayName;
 import org.junit.jupiter.api.Test;

import com.cohort.util.MustBe;
import com.cohort.util.String2;
import java.util.BitSet;
 
 public class ByteArrayTests {

     /**
     * This runs all of the interactive or not interactive tests for this class.
     *
     * @param errorSB all caught exceptions are logged to this.
     * @param interactive  If true, this runs all of the interactive tests; 
     *   otherwise, this runs all of the non-interactive tests.
     * @param doSlowTestsToo If true, this runs the slow tests, too.
     * @param firstTest The first test to be run (0...).  Test numbers may change.
     * @param lastTest The last test to be run, inclusive (0..., or -1 for the last test). 
     *   Test numbers may change.
     */
    public static void test(StringBuilder errorSB, boolean interactive, 
        boolean doSlowTestsToo, int firstTest, int lastTest) {
        if (lastTest < 0)
            lastTest = interactive? -1 : 0;
        String msg = "\n^^^ ByteArrayTests.test(" + interactive + ") test=";

        for (int test = firstTest; test <= lastTest; test++) {
            try {
                long time = System.currentTimeMillis();
                String2.log(msg + test);
            
                if (interactive) {
                    //if (test ==  0) ...;

                } else {
                    if (test ==  0) {
                        ByteArrayTests tests = new ByteArrayTests();
                        tests.basicTest();
                    }
                }

                String2.log(msg + test + " finished successfully in " + (System.currentTimeMillis() - time) + " ms.");
            } catch (Throwable testThrowable) {
                String eMsg = msg + test + " caught throwable:\n" + 
                    MustBe.throwableToString(testThrowable);
                errorSB.append(eMsg);
                String2.log(eMsg);
                if (interactive) 
                    String2.pressEnterToContinue("");
            }
        }
    }

    /**
     * This tests the methods of this class.
     *
     * @throws Throwable if trouble.
     */
    @Test
    @DisplayName("Byte Array Test Suite")
    void basicTest() throws Throwable {
        String2.log("*** ByteArray.basicTest");

        ByteArray anArray = ByteArray.fromCSV(          " -128, -1, 0, 126, ,     127, 200 ");
        assertEquals(anArray.toString(),            "-128, -1, 0, 126, 127, 127, 127", "");
        assertEquals(anArray.toNccsvAttString(),    "-128b,-1b,0b,126b,127b,127b,127b", "");
        assertEquals(anArray.toNccsv127AttString(), "-128b,-1b,0b,126b,127b,127b,127b", "");

        //** test default constructor and many of the methods
        anArray = new ByteArray();
        assertEquals(anArray.isIntegerType(), true, "");
        assertEquals(anArray.missingValue().getRawDouble(), Byte.MAX_VALUE, "");
        anArray.addString("");
        assertEquals(anArray.get(0),               Byte.MAX_VALUE, "");
        assertEquals(anArray.getRawInt(0),         Byte.MAX_VALUE, "");
        assertEquals(anArray.getRawDouble(0),      Byte.MAX_VALUE, "");
        assertEquals(anArray.getUnsignedDouble(0), Byte.MAX_VALUE, "");
        assertEquals(anArray.getRawString(0), "" + Byte.MAX_VALUE, "");
        assertEquals(anArray.getRawNiceDouble(0),  Byte.MAX_VALUE, "");
        assertEquals(anArray.getInt(0),            Integer.MAX_VALUE, "");
        assertEquals(anArray.getDouble(0),         Double.NaN, "");
        assertEquals(anArray.getString(0), "", "");
        assertEquals(anArray.toString(), "127", "");

        anArray.set(0, (byte)-128); assertEquals(anArray.getUnsignedDouble(0), 128, "");
        anArray.set(0, (byte)-127); assertEquals(anArray.getUnsignedDouble(0), 129, "");
        anArray.set(0, (byte)  -1); assertEquals(anArray.getUnsignedDouble(0), 255, "");
        anArray.clear();
 
        assertEquals(anArray.size(), 0, "");
        anArray.add((byte)120);
        assertEquals(anArray.size(), 1, "");
        assertEquals(anArray.get(0), 120, "");
        assertEquals(anArray.getInt(0), 120, "");
        assertEquals(anArray.getFloat(0), 120, "");
        assertEquals(anArray.getDouble(0), 120, "");
        assertEquals(anArray.getString(0), "120", "");
        assertEquals(anArray.elementType(), PAType.BYTE, "");
        byte tArray[] = anArray.toArray();
        assertArrayEquals(tArray, new byte[]{(byte)120}, "");

        //intentional errors
        try {anArray.get(1);              throw new Throwable("It should have failed.");
        } catch (Exception e) {
            assertEquals(e.toString(), 
                "java.lang.IllegalArgumentException: ERROR in ByteArray.get: index (1) >= size (1).", "");
        }
        try {anArray.set(1, (byte)100);         throw new Throwable("It should have failed.");
        } catch (Exception e) {
            assertEquals(e.toString(), 
                "java.lang.IllegalArgumentException: ERROR in ByteArray.set: index (1) >= size (1).", "");
        }
        try {anArray.getInt(1);           throw new Throwable("It should have failed.");
        } catch (Exception e) {
            assertEquals(e.toString(), 
                "java.lang.IllegalArgumentException: ERROR in ByteArray.get: index (1) >= size (1).", "");
        }
        try {anArray.setInt(1, 100);      throw new Throwable("It should have failed.");
        } catch (Exception e) {
            assertEquals(e.toString(), 
                "java.lang.IllegalArgumentException: ERROR in ByteArray.set: index (1) >= size (1).", "");
        }
        try {anArray.getLong(1);          throw new Throwable("It should have failed.");
        } catch (Exception e) {
            assertEquals(e.toString(), 
                "java.lang.IllegalArgumentException: ERROR in ByteArray.get: index (1) >= size (1).", "");
        }
        try {anArray.setLong(1, 100);     throw new Throwable("It should have failed.");
        } catch (Exception e) {
            assertEquals(e.toString(), 
                "java.lang.IllegalArgumentException: ERROR in ByteArray.set: index (1) >= size (1).", "");
        }
        try {anArray.getFloat(1);         throw new Throwable("It should have failed.");
        } catch (Exception e) {
            assertEquals(e.toString(), 
                "java.lang.IllegalArgumentException: ERROR in ByteArray.get: index (1) >= size (1).", "");
        }
        try {anArray.setFloat(1, 100);    throw new Throwable("It should have failed.");
        } catch (Exception e) {
            assertEquals(e.toString(), 
                "java.lang.IllegalArgumentException: ERROR in ByteArray.set: index (1) >= size (1).", "");
        }
        try {anArray.getDouble(1);        throw new Throwable("It should have failed.");
        } catch (Exception e) {
            assertEquals(e.toString(), 
                "java.lang.IllegalArgumentException: ERROR in ByteArray.get: index (1) >= size (1).", "");
        }
        try {anArray.setDouble(1, 100);   throw new Throwable("It should have failed.");
        } catch (Exception e) {
            assertEquals(e.toString(), 
                "java.lang.IllegalArgumentException: ERROR in ByteArray.set: index (1) >= size (1).", "");
        }
        try {anArray.getString(1);        throw new Throwable("It should have failed.");
        } catch (Exception e) {
            assertEquals(e.toString(), 
                "java.lang.IllegalArgumentException: ERROR in ByteArray.get: index (1) >= size (1).", "");
        }
        try {anArray.setString(1, "100"); throw new Throwable("It should have failed.");
        } catch (Exception e) {
            assertEquals(e.toString(), 
                "java.lang.IllegalArgumentException: ERROR in ByteArray.set: index (1) >= size (1).", "");
        }

        //set NaN returned as NaN
        anArray.setDouble(0, Double.NaN);   assertEquals(anArray.getDouble(0), Double.NaN, ""); 
        anArray.setDouble(0, -1e300);       assertEquals(anArray.getDouble(0), Double.NaN, ""); 
        anArray.setDouble(0, 2.2);          assertEquals(anArray.getDouble(0), 2,          ""); 
        anArray.setFloat( 0, Float.NaN);    assertEquals(anArray.getFloat(0),  Float.NaN,  ""); 
        anArray.setFloat( 0, -1e33f);       assertEquals(anArray.getFloat(0),  Float.NaN,  ""); 
        anArray.setFloat( 0, 3.3f);         assertEquals(anArray.getFloat(0),  3,          ""); 
        anArray.setLong(0, Long.MAX_VALUE); assertEquals(anArray.getLong(0),   Long.MAX_VALUE, ""); 
        anArray.setLong(0, 9123456789L);    assertEquals(anArray.getLong(0),   Long.MAX_VALUE, ""); 
        anArray.setLong(0, 4);              assertEquals(anArray.getLong(0),   4, ""); 
        anArray.setInt(0,Integer.MAX_VALUE);assertEquals(anArray.getInt(0),    Integer.MAX_VALUE, ""); 
        anArray.setInt(0, 1123456789);      assertEquals(anArray.getInt(0),    Integer.MAX_VALUE, ""); 
        anArray.setInt(0, 5);               assertEquals(anArray.getInt(0),    5, ""); 

        //makeUnsignedPA
        anArray = new ByteArray(new byte[] {-128, -2, -1, 0, 1, 126, 127});
        UByteArray uArray = (UByteArray)anArray.makeUnsignedPA();
        assertEquals(uArray.toString(), "128, 254, 255, 0, 1, 126, 127", ""); // -1 -> mv
        anArray.clear();        

       
        //** test capacity constructor, test expansion, test clear
        anArray = new ByteArray(2, false);
        assertEquals(anArray.size(), 0, "");
        for (int i = 0; i < 10; i++) {
            anArray.add((byte)i);   
            assertEquals(anArray.get(i), i, "");
            assertEquals(anArray.size(), i+1, "");
        }
        assertEquals(anArray.size(), 10, "");
        anArray.clear();
        assertEquals(anArray.size(), 0, "");

        //active
        anArray = new ByteArray(3, true);
        assertEquals(anArray.size(), 3, "");
        assertEquals(anArray.get(2), 0, "");

        
        //** test array constructor
        anArray = new ByteArray(new byte[]{0,2,4,6,8});
        assertEquals(anArray.size(), 5, "");
        assertEquals(anArray.get(0), 0, "");
        assertEquals(anArray.get(1), 2, "");
        assertEquals(anArray.get(2), 4, "");
        assertEquals(anArray.get(3), 6, "");
        assertEquals(anArray.get(4), 8, "");

        //test compare
        assertEquals(anArray.compare(1, 3), -1, "");
        assertEquals(anArray.compare(1, 1),  0, "");
        assertEquals(anArray.compare(3, 1),  1, "");

        //test toString
        assertEquals(anArray.toString(), "0, 2, 4, 6, 8", "");

        //test calculateStats
        anArray.addString("");
        double stats[] = anArray.calculateStats();
        anArray.remove(5);
        assertEquals(stats[PrimitiveArray.STATS_N], 5, "");
        assertEquals(stats[PrimitiveArray.STATS_MIN], 0, "");
        assertEquals(stats[PrimitiveArray.STATS_MAX], 8, "");
        assertEquals(stats[PrimitiveArray.STATS_SUM], 20, "");

        //test indexOf(int) indexOf(String)
        assertEquals(anArray.indexOf((byte)0, 0),  0, "");
        assertEquals(anArray.indexOf((byte)0, 1), -1, "");
        assertEquals(anArray.indexOf((byte)8, 0),  4, "");
        assertEquals(anArray.indexOf((byte)9, 0), -1, "");

        assertEquals(anArray.indexOf("0", 0),  0, "");
        assertEquals(anArray.indexOf("0", 1), -1, "");
        assertEquals(anArray.indexOf("8", 0),  4, "");
        assertEquals(anArray.indexOf("9", 0), -1, "");

        //test remove
        anArray.remove(1);
        assertEquals(anArray.size(), 4, "");
        assertEquals(anArray.get(0), 0, "");
        assertEquals(anArray.get(1), 4, "");
        assertEquals(anArray.get(3), 8, "");

        //test atInsert(index, value)
        anArray.atInsert(1, (byte)22);
        assertEquals(anArray.size(), 5, "");
        assertEquals(anArray.get(0), 0, "");
        assertEquals(anArray.get(1),22, "");
        assertEquals(anArray.get(2), 4, "");
        assertEquals(anArray.get(4), 8, "");
        anArray.remove(1);

        //test removeRange
        anArray.removeRange(4, 4); //make sure it is allowed
        anArray.removeRange(1, 3);
        assertEquals(anArray.size(), 2, "");
        assertEquals(anArray.get(0), 0, "");
        assertEquals(anArray.get(1), 8, "");

        //test (before trimToSize) that toString, toDoubleArray, and toStringArray use 'size'
        assertEquals(anArray.toString(), "0, 8", "");
        assertArrayEquals(anArray.toDoubleArray(), new double[]{0, 8}, "");
        assertArrayEquals(anArray.toStringArray(), new String[]{"0", "8"}, "");

        //test trimToSize
        anArray.trimToSize();
        assertEquals(anArray.array.length, 2, "");

        //test equals
        ByteArray anArray2 = new ByteArray();
        anArray2.add((byte)0); 
        assertEquals(anArray.testEquals(null), 
            "The two objects aren't equal: this object is a ByteArray; the other is a null.", "");
        assertEquals(anArray.testEquals("A String"), 
            "The two objects aren't equal: this object is a ByteArray; the other is a java.lang.String.", "");
        assertEquals(anArray.testEquals(anArray2), 
            "The two ByteArrays aren't equal: one has 2 value(s); the other has 1 value(s).", "");
        assertTrue(!anArray.equals(anArray2), "");
        anArray2.addString("7");
        assertEquals(anArray.testEquals(anArray2), 
            "The two ByteArrays aren't equal: this[1]=8; other[1]=7.", "");
            assertTrue(!anArray.equals(anArray2), "");
        anArray2.setString(1, "8");
        assertEquals(anArray.testEquals(anArray2), "", "");
        assertTrue(anArray.equals(anArray2), "");

        //test toObjectArray
        assertEquals(anArray.toArray(), anArray.toObjectArray(), "");

        //test toDoubleArray
        assertArrayEquals(anArray.toDoubleArray(), new double[]{0, 8}, "");

        //test reorder
        int rank[] = {1, 0};
        anArray.reorder(rank);
        assertArrayEquals(anArray.toDoubleArray(), new double[]{8, 0}, "");


        //** test append  and clone
        anArray = new ByteArray(new byte[]{1});
        anArray.append(new ByteArray(new byte[]{5, -5}));
        assertArrayEquals(anArray.toDoubleArray(), new double[]{1, 5, -5}, "");
        anArray.append(new StringArray(new String[]{"a", "9"}));
        assertEquals(anArray.getMaxIsMV(), true, "");
        assertEquals(anArray.toString(), "1, 5, -5, 127, 9", ""); //toString shows numbers as is, regardless of maxIsMV
        assertArrayEquals(anArray.toDoubleArray(), new double[]{1, 5, -5, Double.NaN, 9}, "");
        anArray2 = (ByteArray)anArray.clone();
        assertArrayEquals(anArray2.toDoubleArray(), new double[]{1, 5, -5, Double.NaN, 9}, "");

        //test move
        anArray = new ByteArray(new byte[]{0,1,2,3,4});
        anArray.move(1,3,0);
        assertArrayEquals(anArray.toArray(), new byte[]{1,2,0,3,4}, "");

        anArray = new ByteArray(new byte[]{0,1,2,3,4});
        anArray.move(1,2,4);
        assertArrayEquals(anArray.toArray(), new byte[]{0,2,3,1,4}, "");

        //move does nothing, but is allowed
        anArray = new ByteArray(new byte[]{0,1,2,3,4});
        anArray.move(1,1,0);
        assertArrayEquals(anArray.toArray(), new byte[]{0,1,2,3,4}, "");
        anArray.move(1,2,1);
        assertArrayEquals(anArray.toArray(), new byte[]{0,1,2,3,4}, "");
        anArray.move(1,2,2);
        assertArrayEquals(anArray.toArray(), new byte[]{0,1,2,3,4}, "");
        anArray.move(5,5,0);
        assertArrayEquals(anArray.toArray(), new byte[]{0,1,2,3,4}, "");
        anArray.move(3,5,5);
        assertArrayEquals(anArray.toArray(), new byte[]{0,1,2,3,4}, "");

        //makeIndices
        anArray = new ByteArray(new byte[] {25,1,1,10});
        IntArray indices = new IntArray();
        assertEquals(anArray.makeIndices(indices).toString(), "1, 10, 25", "");
        assertEquals(indices.toString(), "2, 0, 0, 1", "");

        anArray = new ByteArray(new byte[] {35,35,Byte.MAX_VALUE,1,2});
        indices = new IntArray();
        assertEquals(anArray.makeIndices(indices).toString(), "1, 2, 35, 127", "");
        assertEquals(indices.toString(), "2, 2, 3, 0, 1", "");

        anArray = new ByteArray(new byte[] {10,20,30,40});
        assertEquals(anArray.makeIndices(indices).toString(), "10, 20, 30, 40", "");
        assertEquals(indices.toString(), "0, 1, 2, 3", "");

        //switchToFakeMissingValue
        anArray = new ByteArray(new byte[] {Byte.MAX_VALUE,1,2,Byte.MAX_VALUE,3,Byte.MAX_VALUE});
        assertEquals(anArray.switchFromTo("", "75"), 3, "");
        assertEquals(anArray.toString(), "75, 1, 2, 75, 3, 75", "");
        anArray.switchFromTo("75", "");
        assertEquals(anArray.toString(), "127, 1, 2, 127, 3, 127", "");
        assertArrayEquals(anArray.getNMinMaxIndex(), new int[]{3, 1, 4}, "");

        //addN
        anArray = new ByteArray(new byte[] {25});
        anArray.addN(2, (byte)5);
        assertEquals(anArray.toString(), "25, 5, 5", "");
        assertArrayEquals(anArray.getNMinMaxIndex(), new int[]{3, 2, 0}, "");

        //add array
        anArray.add(new byte[]{17, 19});
        assertEquals(anArray.toString(), "25, 5, 5, 17, 19", "");

        //subset
        PrimitiveArray ss = anArray.subset(1, 3, 4);
        assertEquals(ss.toString(), "5, 19", "");
        ss = anArray.subset(0, 1, 0);
        assertEquals(ss.toString(), "25", "");
        ss = anArray.subset(0, 1, -1);
        assertEquals(ss.toString(), "", "");
        ss = anArray.subset(1, 1, 0);
        assertEquals(ss.toString(), "", "");

        ss.trimToSize();
        anArray.subset(ss, 1, 3, 4);
        assertEquals(ss.toString(), "5, 19", "");
        anArray.subset(ss, 0, 1, 0);
        assertEquals(ss.toString(), "25", "");
        anArray.subset(ss, 0, 1, -1);
        assertEquals(ss.toString(), "", "");
        anArray.subset(ss, 1, 1, 0);
        assertEquals(ss.toString(), "", "");
        
        //evenlySpaced
        anArray = new ByteArray(new byte[] {10,20,30});
        assertEquals(anArray.isEvenlySpaced(), "", "");
        anArray.set(2, (byte)31);
        assertEquals(anArray.isEvenlySpaced(), 
            "ByteArray isn't evenly spaced: [0]=10.0, [1]=20.0, spacing=10.0, expected spacing=10.5.", "");
        assertEquals(anArray.smallestBiggestSpacing(),
            "    smallest spacing=10.0: [0]=10.0, [1]=20.0\n" +
            "    biggest  spacing=11.0: [1]=20.0, [2]=31.0", "");

        //isAscending
        anArray = new ByteArray(new byte[] {10,10,30});
        assertEquals(anArray.isAscending(), "", "");
        anArray.set(2, Byte.MAX_VALUE);
        assertEquals(anArray.isAscending(), "", "");
        anArray.setMaxIsMV(true);
        assertEquals(anArray.isAscending(), 
            "ByteArray isn't sorted in ascending order: [2]=(missing value).", "");
        anArray.set(1, (byte)9);
        assertEquals(anArray.isAscending(), 
            "ByteArray isn't sorted in ascending order: [0]=10 > [1]=9.", "");

        //isDescending
        anArray = new ByteArray(new byte[] {30,10,10});
        assertEquals(anArray.isDescending(), "", "");
        anArray.set(2, Byte.MAX_VALUE);
        anArray.setMaxIsMV(true);
        assertEquals(anArray.isDescending(), 
            "ByteArray isn't sorted in descending order: [1]=10 < [2]=127.", "");
        anArray.set(1, (byte)35);
        assertEquals(anArray.isDescending(), 
            "ByteArray isn't sorted in descending order: [0]=30 < [1]=35.", "");

        //firstTie
        anArray = new ByteArray(new byte[] {30,35,10});
        assertEquals(anArray.firstTie(), -1, "");
        anArray.set(1, (byte)30);
        assertEquals(anArray.firstTie(), 0, "");

        //hashcode
        anArray = new ByteArray();
        for (int i = 5; i < 1000; i++)
            anArray.add((byte)i);
        String2.log("hashcode1=" + anArray.hashCode());
        anArray2 = (ByteArray)anArray.clone();
        assertEquals(anArray.hashCode(), anArray2.hashCode(), "");
        anArray.atInsert(0, (byte)2);
        assertTrue(anArray.hashCode() != anArray2.hashCode(), "");

        //justKeep
        BitSet bitset = new BitSet();
        anArray = new ByteArray(new byte[] {0, 11, 22, 33, 44});
        bitset.set(1);
        bitset.set(4);
        anArray.justKeep(bitset);
        assertEquals(anArray.toString(), "11, 44", "");

        //min max
        anArray = new ByteArray();
        anArray.addPAOne(anArray.MINEST_VALUE());
        anArray.addPAOne(anArray.MAXEST_VALUE());
        assertEquals(anArray.getString(0), anArray.MINEST_VALUE().toString(), "");
        assertEquals(anArray.getString(0), "-128", "");
        assertEquals(anArray.getString(1), anArray.MAXEST_VALUE().toString(), "");
        assertEquals(anArray.getString(1), "126", "");

        //tryToFindNumericMissingValue() 
        assertEquals((new ByteArray(new byte[] {     })).tryToFindNumericMissingValue(), null, "");
        assertEquals((new ByteArray(new byte[] {1, 2 })).tryToFindNumericMissingValue(), null, "");
        assertEquals((new ByteArray(new byte[] {-128 })).tryToFindNumericMissingValue().getInt(), -128, "");
        assertEquals((new ByteArray(new byte[] {127  })).tryToFindNumericMissingValue().getInt(),  127, "");
        assertEquals((new ByteArray(new byte[] {1, 99})).tryToFindNumericMissingValue().getInt(),   99, "");

    }
 }