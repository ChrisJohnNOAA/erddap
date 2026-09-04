package com.cohort.array;

import com.cohort.util.String2;
import gov.noaa.pfel.erddap.util.BufferedFileChannel;
import java.io.DataOutputStream;
import java.io.RandomAccessFile;
import java.math.BigInteger;

/**
 * PaddedPrimitiveView provides a zero-copy virtual view that pads a PrimitiveArray to a target size
 * with missing values.
 */
public class PaddedPrimitiveView extends PrimitiveView {

  public final PrimitiveArray source;
  public final int targetSize;
  public final double missingDouble;
  public final boolean padAtFront;
  private final int frontPaddingCount;

  /**
   * Constructs a PaddedPrimitiveView padding the source to targetSize with default missing value
   * at the end (back padding).
   *
   * @param source the source PrimitiveArray
   * @param targetSize the target size after padding
   */
  public PaddedPrimitiveView(PrimitiveArray source, int targetSize) {
    this(source, targetSize, Double.NaN, false);
  }

  /**
   * Constructs a PaddedPrimitiveView padding the source to targetSize with a custom missing value
   * at the end (back padding).
   *
   * @param source the source PrimitiveArray
   * @param targetSize the target size after padding
   * @param missingDouble custom missing value as double
   */
  public PaddedPrimitiveView(PrimitiveArray source, int targetSize, double missingDouble) {
    this(source, targetSize, missingDouble, false);
  }

  /**
   * Constructs a PaddedPrimitiveView padding the source to targetSize with default missing value
   * and specifies whether padding is applied at the front or back.
   *
   * @param source the source PrimitiveArray
   * @param targetSize the target size after padding
   * @param padAtFront true if missing values should pad at front (indices 0..padCount-1)
   */
  public PaddedPrimitiveView(PrimitiveArray source, int targetSize, boolean padAtFront) {
    this(source, targetSize, Double.NaN, padAtFront);
  }

  /**
   * Constructs a PaddedPrimitiveView padding the source to targetSize with a custom missing value
   * and specifies whether padding is applied at the front or back.
   *
   * @param source the source PrimitiveArray
   * @param targetSize the target size after padding
   * @param missingDouble custom missing value as double
   * @param padAtFront true if missing values should pad at front (indices 0..padCount-1)
   */
  public PaddedPrimitiveView(
      PrimitiveArray source, int targetSize, double missingDouble, boolean padAtFront) {
    super(
        unwrapSource(source), 0, 1, unwrapSource(source) == null ? 0 : unwrapSource(source).size());

    PrimitiveArray realSource = unwrapSource(source);
    if (realSource == null) {
      throw new IllegalArgumentException("PaddedPrimitiveView source cannot be null.");
    }
    if (targetSize < 0) {
      throw new IllegalArgumentException(
          "PaddedPrimitiveView targetSize (" + targetSize + ") must be >= 0.");
    }

    this.source = realSource;
    this.targetSize = Math.max(realSource.size(), targetSize);
    this.missingDouble = missingDouble;
    this.padAtFront = padAtFront;
    this.frontPaddingCount = padAtFront ? (this.targetSize - realSource.size()) : 0;
    this.size = this.targetSize;
    if (realSource.supportsMaxIsMV()) {
      this.setMaxIsMV(true);
    } else {
      this.setMaxIsMV(realSource.getMaxIsMV());
    }
  }

  /**
   * Static helper method to construct a front-padded PaddedPrimitiveView.
   *
   * @param source the source PrimitiveArray
   * @param targetSize the target size after padding
   * @return a new PaddedPrimitiveView with front padding
   */
  public static PaddedPrimitiveView padFront(PrimitiveArray source, int targetSize) {
    return new PaddedPrimitiveView(source, targetSize, Double.NaN, true);
  }

  private static PrimitiveArray unwrapSource(PrimitiveArray pa) {
    while (pa instanceof PaddedPrimitiveView ppv) {
      pa = ppv.source;
    }
    return pa;
  }

  @Override
  public synchronized PrimitiveArray materialize() {
    if (materialized == null) {
      PrimitiveArray newArray = PrimitiveArray.factory(elementType(), targetSize, false);
      newArray.setMaxIsMV(getMaxIsMV());
      int sourceSize = source.size();
      if (padAtFront) {
        if (frontPaddingCount > 0) {
          if (elementType() == PAType.STRING) {
            newArray.addNStrings(frontPaddingCount, "");
          } else {
            newArray.addNDoubles(frontPaddingCount, missingDoubleForType());
          }
        }
        for (int i = 0; i < sourceSize; i++) {
          newArray.addFromPA(source, i, 1);
        }
      } else {
        for (int i = 0; i < sourceSize; i++) {
          newArray.addFromPA(source, i, 1);
        }
        int padCount = targetSize - sourceSize;
        if (padCount > 0) {
          if (elementType() == PAType.STRING) {
            newArray.addNStrings(padCount, "");
          } else {
            newArray.addNDoubles(padCount, missingDoubleForType());
          }
        }
      }
      materialized = newArray;
    }
    return materialized;
  }

  private void checkIndex(int index) {
    if (index < 0 || index >= targetSize) {
      throw new IndexOutOfBoundsException(
          String2.ERROR
              + " in PaddedPrimitiveView: index ("
              + index
              + ") out of bounds for size ("
              + targetSize
              + ").");
    }
  }

  private double missingDoubleForType() {
    if (!Double.isNaN(missingDouble)) {
      return missingDouble;
    }
    return source.missingValueAsDouble();
  }

  @Override
  public int capacity() {
    PrimitiveArray m = materialized;
    return m != null ? m.capacity() : targetSize;
  }

  // Getters
  @Override
  public int getInt(int index) {
    checkIndex(index);
    PrimitiveArray m = materialized;
    if (m != null) return m.getInt(index);
    if (padAtFront) {
      if (index >= frontPaddingCount) return source.getInt(index - frontPaddingCount);
    } else {
      if (index < source.size()) return source.getInt(index);
    }
    double mv = missingDoubleForType();
    if (Double.isNaN(mv)) {
      return switch (source.elementType()) {
        case BYTE -> Byte.MAX_VALUE;
        case UBYTE -> UByteArray.MAX_VALUE;
        case SHORT -> Short.MAX_VALUE;
        case USHORT -> UShortArray.MAX_VALUE;
        case INT -> Integer.MAX_VALUE;
        case UINT -> (int) UIntArray.MAX_VALUE;
        case CHAR -> Character.MAX_VALUE;
        default -> Integer.MAX_VALUE;
      };
    }
    return (int) mv;
  }

  @Override
  public int getRawInt(int index) {
    checkIndex(index);
    PrimitiveArray m = materialized;
    if (m != null) return m.getRawInt(index);
    if (padAtFront) {
      if (index >= frontPaddingCount) return source.getRawInt(index - frontPaddingCount);
    } else {
      if (index < source.size()) return source.getRawInt(index);
    }
    return getInt(index);
  }

  @Override
  public long getLong(int index) {
    checkIndex(index);
    PrimitiveArray m = materialized;
    if (m != null) return m.getLong(index);
    if (padAtFront) {
      if (index >= frontPaddingCount) return source.getLong(index - frontPaddingCount);
    } else {
      if (index < source.size()) return source.getLong(index);
    }
    double mv = missingDoubleForType();
    if (Double.isNaN(mv)) return Long.MAX_VALUE;
    return (long) mv;
  }

  @Override
  public BigInteger getULong(int index) {
    checkIndex(index);
    PrimitiveArray m = materialized;
    if (m != null) return m.getULong(index);
    if (padAtFront) {
      if (index >= frontPaddingCount) return source.getULong(index - frontPaddingCount);
    } else {
      if (index < source.size()) return source.getULong(index);
    }
    return ULongArray.MAX_VALUE;
  }

  @Override
  public float getFloat(int index) {
    checkIndex(index);
    PrimitiveArray m = materialized;
    if (m != null) return m.getFloat(index);
    if (padAtFront) {
      if (index >= frontPaddingCount) return source.getFloat(index - frontPaddingCount);
    } else {
      if (index < source.size()) return source.getFloat(index);
    }
    return Double.isNaN(missingDouble) ? Float.NaN : (float) missingDouble;
  }

  @Override
  public double getDouble(int index) {
    checkIndex(index);
    PrimitiveArray m = materialized;
    if (m != null) return m.getDouble(index);
    if (padAtFront) {
      if (index >= frontPaddingCount) return source.getDouble(index - frontPaddingCount);
    } else {
      if (index < source.size()) return source.getDouble(index);
    }
    return Double.isNaN(missingDouble) ? Double.NaN : missingDouble;
  }

  @Override
  public double getUnsignedDouble(int index) {
    checkIndex(index);
    PrimitiveArray m = materialized;
    if (m != null) return m.getUnsignedDouble(index);
    if (padAtFront) {
      if (index >= frontPaddingCount) return source.getUnsignedDouble(index - frontPaddingCount);
    } else {
      if (index < source.size()) return source.getUnsignedDouble(index);
    }
    return Double.isNaN(missingDouble) ? Double.NaN : missingDouble;
  }

  @Override
  public double getRawDouble(int index) {
    checkIndex(index);
    PrimitiveArray m = materialized;
    if (m != null) return m.getRawDouble(index);
    if (padAtFront) {
      if (index >= frontPaddingCount) return source.getRawDouble(index - frontPaddingCount);
    } else {
      if (index < source.size()) return source.getRawDouble(index);
    }
    return Double.isNaN(missingDouble) ? Double.NaN : missingDouble;
  }

  @Override
  public double getNiceDouble(int index) {
    checkIndex(index);
    PrimitiveArray m = materialized;
    if (m != null) return m.getNiceDouble(index);
    if (padAtFront) {
      if (index >= frontPaddingCount) return source.getNiceDouble(index - frontPaddingCount);
    } else {
      if (index < source.size()) return source.getNiceDouble(index);
    }
    return Double.isNaN(missingDouble) ? Double.NaN : missingDouble;
  }

  @Override
  public double getRawNiceDouble(int index) {
    checkIndex(index);
    PrimitiveArray m = materialized;
    if (m != null) return m.getRawNiceDouble(index);
    if (padAtFront) {
      if (index >= frontPaddingCount) return source.getRawNiceDouble(index - frontPaddingCount);
    } else {
      if (index < source.size()) return source.getRawNiceDouble(index);
    }
    return Double.isNaN(missingDouble) ? Double.NaN : missingDouble;
  }

  @Override
  public String getString(int index) {
    checkIndex(index);
    PrimitiveArray m = materialized;
    if (m != null) return m.getString(index);
    if (padAtFront) {
      if (index >= frontPaddingCount) return source.getString(index - frontPaddingCount);
    } else {
      if (index < source.size()) return source.getString(index);
    }
    return Double.isNaN(missingDouble) ? "" : String2.genEFormat10(missingDouble);
  }

  @Override
  public String getJsonString(int index) {
    checkIndex(index);
    PrimitiveArray m = materialized;
    if (m != null) return m.getJsonString(index);
    if (padAtFront) {
      if (index >= frontPaddingCount) return source.getJsonString(index - frontPaddingCount);
    } else {
      if (index < source.size()) return source.getJsonString(index);
    }
    return Double.isNaN(missingDouble) ? "null" : String2.genEFormat10(missingDouble);
  }

  @Override
  public String getNccsvDataString(int index) {
    checkIndex(index);
    PrimitiveArray m = materialized;
    if (m != null) return m.getNccsvDataString(index);
    if (padAtFront) {
      if (index >= frontPaddingCount) return source.getNccsvDataString(index - frontPaddingCount);
    } else {
      if (index < source.size()) return source.getNccsvDataString(index);
    }
    return Double.isNaN(missingDouble) ? "" : String2.genEFormat10(missingDouble);
  }

  @Override
  public String getNccsv127DataString(int index) {
    checkIndex(index);
    PrimitiveArray m = materialized;
    if (m != null) return m.getNccsv127DataString(index);
    if (padAtFront) {
      if (index >= frontPaddingCount)
        return source.getNccsv127DataString(index - frontPaddingCount);
    } else {
      if (index < source.size()) return source.getNccsv127DataString(index);
    }
    return Double.isNaN(missingDouble) ? "" : String2.genEFormat10(missingDouble);
  }

  @Override
  public String getSVString(int index) {
    checkIndex(index);
    PrimitiveArray m = materialized;
    if (m != null) return m.getSVString(index);
    if (padAtFront) {
      if (index >= frontPaddingCount) return source.getSVString(index - frontPaddingCount);
    } else {
      if (index < source.size()) return source.getSVString(index);
    }
    return getString(index);
  }

  @Override
  public String getUtf8TsvString(int index) {
    checkIndex(index);
    PrimitiveArray m = materialized;
    if (m != null) return m.getUtf8TsvString(index);
    if (padAtFront) {
      if (index >= frontPaddingCount) return source.getUtf8TsvString(index - frontPaddingCount);
    } else {
      if (index < source.size()) return source.getUtf8TsvString(index);
    }
    return getString(index);
  }

  @Override
  public String getRawString(int index) {
    checkIndex(index);
    PrimitiveArray m = materialized;
    if (m != null) return m.getRawString(index);
    if (padAtFront) {
      if (index >= frontPaddingCount) return source.getRawString(index - frontPaddingCount);
    } else {
      if (index < source.size()) return source.getRawString(index);
    }
    return getString(index);
  }

  @Override
  public String getRawestString(int index) {
    checkIndex(index);
    PrimitiveArray m = materialized;
    if (m != null) return m.getRawestString(index);
    if (padAtFront) {
      if (index >= frontPaddingCount) return source.getRawestString(index - frontPaddingCount);
    } else {
      if (index < source.size()) return source.getRawestString(index);
    }
    return getString(index);
  }

  @Override
  public PAOne getPAOne(int index) {
    return getPAOne(index, null);
  }

  @Override
  public PAOne getPAOne(int index, PAOne paOne) {
    checkIndex(index);
    PrimitiveArray m = materialized;
    if (m != null) return m.getPAOne(index, paOne);
    if (padAtFront) {
      if (index >= frontPaddingCount) return source.getPAOne(index - frontPaddingCount, paOne);
    } else {
      if (index < source.size()) return source.getPAOne(index, paOne);
    }
    if (paOne == null) paOne = new PAOne(source.elementType());
    if (source.elementType() == PAType.STRING) return paOne.setString("");
    return paOne.fromDouble(
        Double.isNaN(missingDouble) ? source.missingValueAsDouble() : missingDouble);
  }

  @Override
  public boolean isMaxValue(int index) {
    checkIndex(index);
    PrimitiveArray m = materialized;
    if (m != null) return m.isMaxValue(index);
    if (padAtFront) {
      if (index >= frontPaddingCount) return source.isMaxValue(index - frontPaddingCount);
    } else {
      if (index < source.size()) return source.isMaxValue(index);
    }
    return source.getMaxIsMV();
  }

  @Override
  public boolean isMissingValue(int index) {
    checkIndex(index);
    PrimitiveArray m = materialized;
    if (m != null) return m.isMissingValue(index);
    if (padAtFront) {
      if (index >= frontPaddingCount) return source.isMissingValue(index - frontPaddingCount);
    } else {
      if (index < source.size()) return source.isMissingValue(index);
    }
    return true;
  }

  // Min/Max and Statistics Safeguards (actual_range metadata preservation)
  @Override
  public double[] calculateStats(Attributes atts) {
    return source.calculateStats(atts);
  }

  @Override
  public PAOne[] calculatePAOneStats(Attributes atts) {
    return source.calculatePAOneStats(atts);
  }

  @Override
  public double[] calculateStats2(Attributes atts) {
    return source.calculateStats2(atts);
  }

  @Override
  public double calculateMedian(Attributes atts) {
    return source.calculateMedian(atts);
  }

  @Override
  public int[] getNMinMaxIndex() {
    return source.getNMinMaxIndex();
  }

  @Override
  public String[] getNMinMax() {
    return source.getNMinMax();
  }

  // I/O Methods
  @Override
  public long writeToChannel(BufferedFileChannel channel) throws Exception {
    return writeToChannel(channel, 0, targetSize);
  }

  @Override
  public long writeToChannel(BufferedFileChannel channel, int off, int len) throws Exception {
    if (off < 0 || len < 0 || (long) off + len > targetSize) {
      throw new IndexOutOfBoundsException(
          "Invalid range: offset=" + off + ", len=" + len + ", size=" + targetSize);
    }
    PrimitiveArray m = materialized;
    if (m != null) {
      return m.writeToChannel(channel, off, len);
    }
    long totalWritten = 0;
    if (padAtFront) {
      int frontPadLen = Math.min(len, Math.max(0, frontPaddingCount - off));
      if (frontPadLen > 0) {
        totalWritten += materialize().writeToChannel(channel, off, frontPadLen);
      }
      int sourceOff = Math.max(0, off - frontPaddingCount);
      int sourceLen = Math.min(len - frontPadLen, Math.max(0, source.size() - sourceOff));
      if (sourceLen > 0) {
        totalWritten += source.writeToChannel(channel, sourceOff, sourceLen);
      }
    } else {
      int sourceSize = source.size();
      int sourceOff = Math.min(off, sourceSize);
      int sourceLen = Math.min(len, Math.max(0, sourceSize - off));
      if (sourceLen > 0) {
        totalWritten += source.writeToChannel(channel, sourceOff, sourceLen);
      }
      int padLen = len - sourceLen;
      if (padLen > 0) {
        int padOff = off + sourceLen;
        totalWritten += materialize().writeToChannel(channel, padOff, padLen);
      }
    }
    return totalWritten;
  }

  @Override
  public int writeDos(DataOutputStream dos, int i) throws Exception {
    checkIndex(i);
    PrimitiveArray m = materialized;
    if (m != null) {
      return m.writeDos(dos, i);
    }
    if (padAtFront) {
      if (i >= frontPaddingCount) {
        return source.writeDos(dos, i - frontPaddingCount);
      }
    } else {
      if (i < source.size()) {
        return source.writeDos(dos, i);
      }
    }
    return materialize().writeDos(dos, i);
  }

  @Override
  public void writeToRAF(RandomAccessFile raf, int index) throws Exception {
    checkIndex(index);
    PrimitiveArray m = materialized;
    if (m != null) {
      m.writeToRAF(raf, index);
    } else if (padAtFront) {
      if (index >= frontPaddingCount) {
        source.writeToRAF(raf, index - frontPaddingCount);
      } else {
        materialize().writeToRAF(raf, index);
      }
    } else if (index < source.size()) {
      source.writeToRAF(raf, index);
    } else {
      materialize().writeToRAF(raf, index);
    }
  }

  @Override
  public void externalizeForDODS(DataOutputStream dos) throws Exception {
    PrimitiveArray m = materialized;
    if (m != null) {
      m.externalizeForDODS(dos);
    } else {
      dos.writeInt(targetSize);
      dos.writeInt(targetSize);
      writeDos(dos);
    }
  }

  @Override
  public void externalizeForDODS(DataOutputStream dos, int i) throws Exception {
    checkIndex(i);
    PrimitiveArray m = materialized;
    if (m != null) {
      m.externalizeForDODS(dos, i);
    } else if (padAtFront) {
      if (i >= frontPaddingCount) {
        source.externalizeForDODS(dos, i - frontPaddingCount);
      } else {
        materialize().externalizeForDODS(dos, i);
      }
    } else if (i < source.size()) {
      source.externalizeForDODS(dos, i);
    } else {
      materialize().externalizeForDODS(dos, i);
    }
  }

  @Override
  public Object toObjectArray() {
    PrimitiveArray m = materialized;
    if (m != null) return m.toObjectArray();
    return materialize().toObjectArray();
  }

  @Override
  public double[] toDoubleArray() {
    PrimitiveArray m = materialized;
    if (m != null) return m.toDoubleArray();
    double[] dar = new double[targetSize];
    for (int i = 0; i < targetSize; i++) {
      dar[i] = getDouble(i);
    }
    return dar;
  }

  @Override
  public String[] toStringArray() {
    PrimitiveArray m = materialized;
    if (m != null) return m.toStringArray();
    String[] sar = new String[targetSize];
    for (int i = 0; i < targetSize; i++) {
      sar[i] = getString(i);
    }
    return sar;
  }

  @Override
  public PrimitiveArray subset(PrimitiveArray pa, int startIndex, int stride, int stopIndex) {
    if (startIndex < 0) {
      throw new IllegalArgumentException(
          String2.ERROR
              + " in PaddedPrimitiveView.subset: startIndex="
              + startIndex
              + " must be at least 0.");
    }
    if (stride < 1) {
      throw new IllegalArgumentException(
          String2.ERROR
              + " in PaddedPrimitiveView.subset: stride="
              + stride
              + " must be greater than 0.");
    }
    if (stopIndex < startIndex) {
      if (pa == null) return new PrimitiveView(this, 0, 1, 0);
      pa.clear();
      return pa;
    }
    int effectiveStop = Math.min(stopIndex, targetSize - 1);
    int subLength = PrimitiveArray.strideWillFind(effectiveStop - startIndex + 1, stride);
    if (pa == null) {
      return new PrimitiveView(this, startIndex, stride, subLength);
    }

    pa.clear();
    PrimitiveArray m = materialized;
    if (m != null) {
      return m.subset(pa, startIndex, stride, effectiveStop);
    }
    for (int i = 0; i < subLength; i++) {
      pa.addFromPA(this, startIndex + i * stride, 1);
    }
    return pa;
  }
}
