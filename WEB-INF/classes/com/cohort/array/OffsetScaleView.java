package com.cohort.array;

import com.cohort.util.Math2;
import com.cohort.util.String2;

/** OffsetScaleView provides a zero-copy virtual view over a PrimitiveArray with scale and offset. */
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
    super(
        source,
        offset,
        stride,
        length);

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
            this, 0, 1, 0, sourceIsUnsigned, targetPAType, 1.0, 0.0);
      }
      pa.clear();
      return pa;
    }
    int effectiveStop = Math.min(stopIndex, size - 1);
    int subLength = PrimitiveArray.strideWillFind(effectiveStop - startIndex + 1, stride);
    if (pa == null) {
      return new OffsetScaleView(
          this, startIndex, stride, subLength, sourceIsUnsigned, targetPAType, 1.0, 0.0);
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
