package com.cohort.array;

import com.cohort.util.Math2;
import com.cohort.util.String2;

/**
 * OffsetScaleView provides a zero-copy virtual view over a PrimitiveArray (or PrimitiveView)
 * applying a linear transformation (y = scale * x + addOffset) lazily.
 */
public class OffsetScaleView extends PrimitiveView {

  public final double scale;
  public final double addOffset;
  public final double sourceMissingValue;
  public final boolean sourceIsUnsigned;
  private final PAType targetPAType;

  public OffsetScaleView(PrimitiveArray source, double scale, double addOffset) {
    this(source, false, source != null ? source.elementType() : PAType.DOUBLE, scale, addOffset);
  }

  public OffsetScaleView(
      PrimitiveArray source, PAType targetPAType, double scale, double addOffset) {
    this(source, false, targetPAType, scale, addOffset);
  }

  public OffsetScaleView(
      PrimitiveArray source,
      boolean sourceIsUnsigned,
      PAType targetPAType,
      double scale,
      double addOffset) {
    this(
        source,
        sourceIsUnsigned,
        targetPAType,
        scale,
        addOffset,
        0,
        1,
        source != null ? source.size() : 0);
  }

  public OffsetScaleView(
      PrimitiveArray source,
      boolean sourceIsUnsigned,
      PAType targetPAType,
      double scale,
      double addOffset,
      int viewOffset,
      int viewStride,
      int viewLength) {
    super(source, viewOffset, viewStride, viewLength);

    double accumScale = scale;
    double accumOffset = addOffset;
    boolean accumUnsigned = sourceIsUnsigned;
    PrimitiveArray current = source;

    while (current instanceof OffsetScaleView osv && osv.materialized == null) {
      accumOffset = accumScale * osv.addOffset + accumOffset;
      accumScale = accumScale * osv.scale;
      accumUnsigned = accumUnsigned || osv.sourceIsUnsigned;
      current = osv.source;
    }

    this.scale = accumScale;
    this.addOffset = accumOffset;
    this.sourceIsUnsigned = accumUnsigned;

    this.targetPAType =
        targetPAType != null
            ? targetPAType
            : (this.source != null ? this.source.elementType() : PAType.DOUBLE);
    this.sourceMissingValue = this.source != null ? this.source.missingValueAsDouble() : Double.NaN;
  }

  private void checkIndexBounds(int index) {
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
  public PAType elementType() {
    PrimitiveArray m = materialized;
    return m != null ? m.elementType() : targetPAType;
  }

  @Override
  public double missingValueAsDouble() {
    return targetPAType == PAType.FLOAT ? Float.NaN : Double.NaN;
  }

  @Override
  public double getDouble(int index) {
    checkIndexBounds(index);
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
    checkIndexBounds(index);
    PrimitiveArray m = materialized;
    if (m != null) {
      return m.getNiceDouble(index);
    }
    double d = getDouble(index);
    return targetPAType == PAType.FLOAT ? Math2.floatToDouble((float) d) : d;
  }

  @Override
  public double getRawDouble(int index) {
    return getDouble(index);
  }

  @Override
  public double getUnsignedDouble(int index) {
    return getDouble(index);
  }

  @Override
  public double getRawNiceDouble(int index) {
    return getNiceDouble(index);
  }

  @Override
  public int getRawInt(int index) {
    return getInt(index);
  }

  @Override
  public float getFloat(int index) {
    checkIndexBounds(index);
    PrimitiveArray m = materialized;
    if (m != null) {
      return m.getFloat(index);
    }
    double val = getDouble(index);
    return Double.isNaN(val) ? Float.NaN : (float) val;
  }

  @Override
  public int getInt(int index) {
    checkIndexBounds(index);
    PrimitiveArray m = materialized;
    if (m != null) {
      return m.getInt(index);
    }
    double d = getDouble(index);
    if (Double.isNaN(d)) {
      return Integer.MAX_VALUE;
    }
    return Math2.roundToInt(d);
  }

  @Override
  public long getLong(int index) {
    checkIndexBounds(index);
    PrimitiveArray m = materialized;
    if (m != null) {
      return m.getLong(index);
    }
    double d = getDouble(index);
    if (Double.isNaN(d)) {
      return Long.MAX_VALUE;
    }
    return Math2.roundToLong(d);
  }

  @Override
  public String getString(int index) {
    checkIndexBounds(index);
    PrimitiveArray m = materialized;
    if (m != null) {
      return m.getString(index);
    }
    if (targetPAType == PAType.FLOAT) {
      float f = getFloat(index);
      return Float.isNaN(f) ? "" : String.valueOf(f);
    }
    double d = getDouble(index);
    return Double.isNaN(d) ? "" : String.valueOf(d);
  }

  @Override
  public String getRawString(int index) {
    return getString(index);
  }

  @Override
  public String getRawestString(int index) {
    return getString(index);
  }

  @Override
  public PAOne getPAOne(int index) {
    return getPAOne(index, new PAOne(elementType()));
  }

  @Override
  public PAOne getPAOne(int index, PAOne paOne) {
    checkIndexBounds(index);
    PrimitiveArray m = materialized;
    if (m != null) {
      return m.getPAOne(index, paOne);
    }
    if (paOne == null) {
      paOne = new PAOne(elementType());
    }
    if (targetPAType == PAType.FLOAT) {
      return paOne.setFloat(getFloat(index));
    }
    return paOne.setDouble(getDouble(index));
  }

  @Override
  public boolean isMissingValue(int index) {
    checkIndexBounds(index);
    PrimitiveArray m = materialized;
    if (m != null) {
      return m.isMissingValue(index);
    }
    double val =
        sourceIsUnsigned
            ? source.getUnsignedDouble(offset + index * stride)
            : source.getDouble(offset + index * stride);
    return Double.isNaN(val)
        || val == sourceMissingValue
        || source.isMissingValue(offset + index * stride);
  }

  @Override
  public synchronized PrimitiveArray materialize() {
    if (materialized == null) {
      PrimitiveArray newArray = PrimitiveArray.factory(elementType(), size, false);
      newArray.setMaxIsMV(getMaxIsMV());
      for (int i = 0; i < size; i++) {
        if (targetPAType == PAType.FLOAT) {
          newArray.addFloat(getFloat(i));
        } else {
          newArray.addDouble(getDouble(i));
        }
      }
      materialized = newArray;
    }
    return materialized;
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
      if (pa == null)
        return new OffsetScaleView(
            source, sourceIsUnsigned, targetPAType, scale, addOffset, 0, 1, 0);
      pa.clear();
      return pa;
    }
    int effectiveStop = Math.min(stopIndex, size - 1);
    int subLength = PrimitiveArray.strideWillFind(effectiveStop - startIndex + 1, stride);
    if (pa == null) {
      PrimitiveArray m = materialized;
      if (m != null) {
        return m.subset(null, startIndex, stride, effectiveStop);
      }
      return new OffsetScaleView(
          source,
          sourceIsUnsigned,
          targetPAType,
          scale,
          addOffset,
          (int) (this.offset + (long) startIndex * this.stride),
          stride * this.stride,
          subLength);
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

  // Comparison: must use transformed (scaled/offset) values, so materialize when needed.
  @Override
  public int compare(int index1, PrimitiveArray otherPA, int index2) {
    PrimitiveArray m = materialized;
    if (m != null) return m.compare(index1, otherPA, index2);
    return materialize().compare(index1, otherPA, index2);
  }

  @Override
  public int compare(int index1, int index2) {
    PrimitiveArray m = materialized;
    if (m != null) return m.compare(index1, index2);
    return materialize().compare(index1, index2);
  }

  // I/O: default PrimitiveView sometimes delegates directly to source which would bypass
  // the scale/offset transformation. Ensure writes use the materialized (transformed)
  // data to preserve semantics.
  @Override
  public long writeToChannel(java.nio.channels.FileChannel channel, int off, int len)
      throws Exception {
    PrimitiveArray m = materialized;
    if (m != null) return m.writeToChannel(channel, off, len);
    return materialize().writeToChannel(channel, off, len);
  }

  @Override
  public long writeToChannel(java.nio.channels.FileChannel channel) throws Exception {
    PrimitiveArray m = materialized;
    if (m != null) return m.writeToChannel(channel);
    return materialize().writeToChannel(channel);
  }

  @Override
  public int writeDos(java.io.DataOutputStream dos) throws Exception {
    PrimitiveArray m = materialized;
    if (m != null) return m.writeDos(dos);
    return materialize().writeDos(dos);
  }

  @Override
  public int writeDos(java.io.DataOutputStream dos, int i) throws Exception {
    PrimitiveArray m = materialized;
    if (m != null) return m.writeDos(dos, i);
    return materialize().writeDos(dos, i);
  }

  @Override
  public void writeToRAF(java.io.RandomAccessFile raf, int index) throws Exception {
    PrimitiveArray m = materialized;
    if (m != null) {
      m.writeToRAF(raf, index);
      return;
    }
    materialize().writeToRAF(raf, index);
  }

  @Override
  public void externalizeForDODS(java.io.DataOutputStream dos) throws Exception {
    PrimitiveArray m = materialized;
    if (m != null) {
      m.externalizeForDODS(dos);
      return;
    }
    materialize().externalizeForDODS(dos);
  }

  @Override
  public void externalizeForDODS(java.io.DataOutputStream dos, int i) throws Exception {
    PrimitiveArray m = materialized;
    if (m != null) {
      m.externalizeForDODS(dos, i);
      return;
    }
    materialize().externalizeForDODS(dos, i);
  }
}
