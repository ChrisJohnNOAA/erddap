package com.cohort.array;

import com.cohort.util.Math2;
import com.cohort.util.String2;
import gov.noaa.pfel.erddap.util.BufferedFileChannel;
import java.io.DataOutputStream;
import java.io.RandomAccessFile;
import java.math.BigInteger;

/**
 * OffsetScaleView provides a zero-copy virtual view over a PrimitiveArray with scale and offset.
 */
public class OffsetScaleView extends PrimitiveView {

  public final double scale;
  public final double addOffset;
  public final double sourceMissingValue;
  public final boolean sourceIsUnsigned;
  private final PAType targetPAType;

  /**
   * Constructs an OffsetScaleView over a source PrimitiveArray.
   *
   * @param source the source PrimitiveArray
   * @param offset the starting offset in source
   * @param stride the step size between elements
   * @param length the number of elements in this view
   * @param sourceIsUnsigned true if source integer values are unsigned
   * @param targetPAType the target element PAType
   * @param scale the scale factor
   * @param addOffset the add offset
   */
  public OffsetScaleView(
      PrimitiveArray source,
      int offset,
      int stride,
      int length,
      boolean sourceIsUnsigned,
      PAType targetPAType,
      double scale,
      double addOffset) {
    super(source, offset, stride, length);

    PrimitiveArray cur = source;
    double tScale = scale;
    double tAddOffset = addOffset;
    boolean tSourceIsUnsigned = sourceIsUnsigned;
    double tSourceMissingValue =
        this.source == null ? Double.NaN : this.source.missingValueAsDouble();

    while (cur instanceof PrimitiveView pv && pv.materialized == null) {
      if (pv instanceof OffsetScaleView osv) {
        tAddOffset = tScale * osv.addOffset + tAddOffset;
        tScale = osv.scale * tScale;
        tSourceIsUnsigned = osv.sourceIsUnsigned;
        tSourceMissingValue = osv.sourceMissingValue;
      }
      cur = pv.source;
    }

    this.scale = tScale;
    this.addOffset = tAddOffset;
    this.sourceIsUnsigned = tSourceIsUnsigned;
    this.sourceMissingValue = tSourceMissingValue;
    this.targetPAType =
        targetPAType == null
            ? (this.source == null ? PAType.DOUBLE : this.source.elementType())
            : targetPAType;
  }

  /**
   * Constructs an OffsetScaleView over a source PrimitiveArray starting at offset 0 and stride 1.
   *
   * @param source the source PrimitiveArray
   * @param sourceIsUnsigned true if source integer values are unsigned
   * @param targetPAType the target element PAType
   * @param scale the scale factor
   * @param addOffset the add offset
   */
  public OffsetScaleView(
      PrimitiveArray source,
      boolean sourceIsUnsigned,
      PAType targetPAType,
      double scale,
      double addOffset) {
    this(
        source,
        0,
        1,
        source == null ? 0 : source.size(),
        sourceIsUnsigned,
        targetPAType,
        scale,
        addOffset);
  }

  @Override
  public PAType elementType() {
    return targetPAType;
  }

  @Override
  public boolean isIntegerType() {
    return PAType.isIntegerType(targetPAType);
  }

  @Override
  public boolean isFloatingPointType() {
    return targetPAType == PAType.FLOAT || targetPAType == PAType.DOUBLE;
  }

  @Override
  public boolean isUnsigned() {
    return targetPAType.isUnsigned();
  }

  @Override
  public int elementSize() {
    return PAType.elementSize(targetPAType);
  }

  @Override
  public double missingValueAsDouble() {
    return Double.NaN;
  }

  @Override
  public synchronized PrimitiveArray materialize() {
    if (materialized == null) {
      PrimitiveArray newArray = PrimitiveArray.factory(targetPAType, size, false);
      newArray.setMaxIsMV(getMaxIsMV());
      for (int i = 0; i < size; i++) {
        double d = getDouble(i);
        newArray.addDouble(d);
      }
      materialized = newArray;
    }
    return materialized;
  }

  private void checkIndex(int index) {
    if (index < 0 || index >= size) {
      throw new IndexOutOfBoundsException(
          String2.ERROR
              + " in OffsetScaleView: index ("
              + index
              + ") out of bounds for view size ("
              + size
              + ").");
    }
  }

  @Override
  public double getDouble(int index) {
    checkIndex(index);
    PrimitiveArray m = materialized;
    if (m != null) {
      return m.getDouble(index);
    }
    double val =
        sourceIsUnsigned
            ? source.getUnsignedDouble(offset + index * stride)
            : source.getDouble(offset + index * stride);
    if (Double.isNaN(val)
        || val == sourceMissingValue
        || source.isMissingValue(offset + index * stride)) {
      return Double.NaN;
    }
    return val * scale + addOffset;
  }

  @Override
  public double getNiceDouble(int index) {
    checkIndex(index);
    PrimitiveArray m = materialized;
    if (m != null) {
      return m.getNiceDouble(index);
    }
    double d = getDouble(index);
    if (Double.isNaN(d)) return Double.NaN;
    return targetPAType == PAType.FLOAT ? Math2.floatToDouble((float) d) : d;
  }

  @Override
  public double getRawDouble(int index) {
    checkIndex(index);
    PrimitiveArray m = materialized;
    if (m != null) {
      return m.getRawDouble(index);
    }
    return getNiceDouble(index);
  }

  @Override
  public double getUnsignedDouble(int index) {
    checkIndex(index);
    PrimitiveArray m = materialized;
    if (m != null) {
      return m.getUnsignedDouble(index);
    }
    return getNiceDouble(index);
  }

  @Override
  public double getRawNiceDouble(int index) {
    checkIndex(index);
    PrimitiveArray m = materialized;
    if (m != null) {
      return m.getRawNiceDouble(index);
    }
    return getNiceDouble(index);
  }

  @Override
  public float getFloat(int index) {
    checkIndex(index);
    PrimitiveArray m = materialized;
    if (m != null) {
      return m.getFloat(index);
    }
    return (float) getDouble(index);
  }

  @Override
  public int getInt(int index) {
    checkIndex(index);
    PrimitiveArray m = materialized;
    if (m != null) {
      return m.getInt(index);
    }
    return Math2.roundToInt(getDouble(index));
  }

  @Override
  public int getRawInt(int index) {
    checkIndex(index);
    PrimitiveArray m = materialized;
    if (m != null) {
      return m.getRawInt(index);
    }
    return getInt(index);
  }

  @Override
  public long getLong(int index) {
    checkIndex(index);
    PrimitiveArray m = materialized;
    if (m != null) {
      return m.getLong(index);
    }
    return Math2.roundToLong(getDouble(index));
  }

  @Override
  public BigInteger getULong(int index) {
    checkIndex(index);
    PrimitiveArray m = materialized;
    if (m != null) {
      return m.getULong(index);
    }
    double d = getDouble(index);
    if (!Double.isFinite(d) || d < 0) {
      return null;
    }
    return Math2.roundToULong(d);
  }

  @Override
  public String getString(int index) {
    checkIndex(index);
    PrimitiveArray m = materialized;
    if (m != null) {
      return m.getString(index);
    }
    double d = getDouble(index);
    if (!Double.isFinite(d)) {
      return "";
    }
    if (targetPAType == PAType.FLOAT) {
      float f = (float) d;
      return Float.isFinite(f) ? String.valueOf(f) : "";
    }
    return String.valueOf(d);
  }

  @Override
  public String getJsonString(int index) {
    checkIndex(index);
    PrimitiveArray m = materialized;
    if (m != null) {
      return m.getJsonString(index);
    }
    double d = getDouble(index);
    if (!Double.isFinite(d)) {
      return "null";
    }
    if (targetPAType == PAType.FLOAT) {
      float f = (float) d;
      return Float.isFinite(f) ? String.valueOf(f) : "null";
    }
    return String.valueOf(d);
  }

  @Override
  public String getNccsvDataString(int index) {
    checkIndex(index);
    PrimitiveArray m = materialized;
    if (m != null) {
      return m.getNccsvDataString(index);
    }
    return getString(index);
  }

  @Override
  public String getNccsv127DataString(int index) {
    checkIndex(index);
    PrimitiveArray m = materialized;
    if (m != null) {
      return m.getNccsv127DataString(index);
    }
    return getString(index);
  }

  @Override
  public String getSVString(int index) {
    checkIndex(index);
    PrimitiveArray m = materialized;
    if (m != null) {
      return m.getSVString(index);
    }
    return getString(index);
  }

  @Override
  public String getUtf8TsvString(int index) {
    checkIndex(index);
    PrimitiveArray m = materialized;
    if (m != null) {
      return m.getUtf8TsvString(index);
    }
    return getString(index);
  }

  @Override
  public String getRawString(int index) {
    checkIndex(index);
    PrimitiveArray m = materialized;
    if (m != null) {
      return m.getRawString(index);
    }
    return getString(index);
  }

  @Override
  public String getRawestString(int index) {
    checkIndex(index);
    PrimitiveArray m = materialized;
    if (m != null) {
      return m.getRawestString(index);
    }
    return getString(index);
  }

  @Override
  public PAOne getPAOne(int index) {
    checkIndex(index);
    PrimitiveArray m = materialized;
    if (m != null) {
      return m.getPAOne(index);
    }
    return getPAOne(index, new PAOne(targetPAType));
  }

  @Override
  public PAOne getPAOne(int index, PAOne paOne) {
    checkIndex(index);
    PrimitiveArray m = materialized;
    if (m != null) {
      return m.getPAOne(index, paOne);
    }
    if (paOne == null) {
      paOne = new PAOne(targetPAType);
    }
    if (targetPAType == PAType.FLOAT) {
      paOne.setFloat((float) getDouble(index));
    } else {
      paOne.setDouble(getDouble(index));
    }
    return paOne;
  }

  @Override
  public boolean isMissingValue(int index) {
    checkIndex(index);
    PrimitiveArray m = materialized;
    if (m != null) {
      return m.isMissingValue(index);
    }
    return Double.isNaN(getDouble(index));
  }

  @Override
  public double[] calculateStats(Attributes atts) {
    PrimitiveArray m = materialized;
    if (m != null) {
      return m.calculateStats(atts);
    }
    int n = size();
    int count = 0;
    double min = Double.NaN;
    double max = Double.NaN;
    double sum = 0;
    double sumSqr = 0;

    for (int i = 0; i < n; i++) {
      double d = getDouble(i);
      if (Double.isNaN(d)) {
        continue;
      }

      if (count == 0) {
        min = d;
        max = d;
      } else {
        if (d < min) min = d;
        if (d > max) max = d;
      }

      count++;
      sum += d;
      sumSqr += d * d;
    }

    double[] stats = new double[5];
    stats[0] = count;
    stats[1] = min;
    stats[2] = max;
    stats[3] = sum;
    stats[4] = sumSqr;
    return stats;
  }

  @Override
  public PAOne[] calculatePAOneStats(Attributes atts) {
    PrimitiveArray m = materialized;
    if (m != null) {
      return m.calculatePAOneStats(atts);
    }
    double[] stats = calculateStats(atts);
    int n = (int) stats[0];
    PAOne pMin = new PAOne(targetPAType);
    PAOne pMax = new PAOne(targetPAType);
    if (n > 0) {
      pMin.setDouble(stats[1]);
      pMax.setDouble(stats[2]);
    }
    return new PAOne[] {PAOne.fromInt(n), pMin, pMax, PAOne.fromDouble(stats[3])};
  }

  @Override
  public int compare(int index1, PrimitiveArray otherPA, int index2) {
    checkIndex(index1);
    PrimitiveArray m = materialized;
    if (m != null) {
      return m.compare(index1, otherPA, index2);
    }
    return Double.compare(getDouble(index1), otherPA.getDouble(index2));
  }

  @Override
  public int compare(int index1, int index2) {
    checkIndex(index1);
    checkIndex(index2);
    PrimitiveArray m = materialized;
    if (m != null) {
      return m.compare(index1, index2);
    }
    return Double.compare(getDouble(index1), getDouble(index2));
  }

  @Override
  public long writeToChannel(BufferedFileChannel channel) throws Exception {
    return writeToChannel(channel, 0, size);
  }

  @Override
  public long writeToChannel(BufferedFileChannel channel, int off, int len) throws Exception {
    if (off < 0 || len < 0 || (long) off + len > size) {
      throw new IndexOutOfBoundsException(
          "Invalid range: offset=" + off + ", len=" + len + ", view size=" + size);
    }
    PrimitiveArray m = materialized;
    if (m != null) {
      return m.writeToChannel(channel, off, len);
    }
    return materialize().writeToChannel(channel, off, len);
  }

  @Override
  public int writeDos(DataOutputStream dos) throws Exception {
    return materialize().writeDos(dos);
  }

  @Override
  public int writeDos(DataOutputStream dos, int i) throws Exception {
    checkIndex(i);
    return materialize().writeDos(dos, i);
  }

  @Override
  public void writeToRAF(RandomAccessFile raf, int index) throws Exception {
    checkIndex(index);
    materialize().writeToRAF(raf, index);
  }

  @Override
  public void externalizeForDODS(DataOutputStream dos) throws Exception {
    materialize().externalizeForDODS(dos);
  }

  @Override
  public void externalizeForDODS(DataOutputStream dos, int i) throws Exception {
    checkIndex(i);
    materialize().externalizeForDODS(dos, i);
  }

  @Override
  public PrimitiveArray subset(PrimitiveArray pa, int startIndex, int stride, int stopIndex) {
    if (startIndex < 0) {
      throw new IllegalArgumentException(
          String2.ERROR
              + " in OffsetScaleView.subset: startIndex="
              + startIndex
              + " must be at least 0.");
    }
    if (stride < 1) {
      throw new IllegalArgumentException(
          String2.ERROR
              + " in OffsetScaleView.subset: stride="
              + stride
              + " must be greater than 0.");
    }
    if (stopIndex < startIndex) {
      if (pa == null) {
        return new OffsetScaleView(
            this.source,
            this.offset,
            this.stride,
            0,
            sourceIsUnsigned,
            targetPAType,
            scale,
            addOffset);
      }
      pa.clear();
      return pa;
    }
    int effectiveStop = Math.min(stopIndex, size - 1);
    int subLength = PrimitiveArray.strideWillFind(effectiveStop - startIndex + 1, stride);
    if (pa == null) {
      int newOffset = offset + startIndex * this.stride;
      int newStride = this.stride * stride;
      return new OffsetScaleView(
          this.source,
          newOffset,
          newStride,
          subLength,
          sourceIsUnsigned,
          targetPAType,
          scale,
          addOffset);
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
