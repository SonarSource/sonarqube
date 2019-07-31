/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.ce.task.projectanalysis.measure;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.Optional;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.ce.task.projectanalysis.util.cache.DoubleCache;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;

public interface Measure {

  enum ValueType {
    NO_VALUE, BOOLEAN, INT, LONG, DOUBLE, STRING, LEVEL
  }

  enum Level {
    OK("Green"),
    ERROR("Red"),

    /**
     * @deprecated since 7.6, warning quality gates doesn't exist anymore on new analysis
     */
    @Deprecated
    WARN("Orange");

    private final String colorName;

    Level(String colorName) {
      this.colorName = colorName;
    }

    public String getColorName() {
      return colorName;
    }

    public static Optional<Level> toLevel(@Nullable String level) {
      if (level == null) {
        return Optional.empty();
      }

      try {
        return Optional.of(Level.valueOf(level));
      } catch (IllegalArgumentException e) {
        return Optional.empty();
      }
    }
  }

  /**
   * The type of value stored in the measure.
   */
  ValueType getValueType();

  /**
   * The value of this measure as a boolean if the type is {@link Measure.ValueType#BOOLEAN}.
   *
   * @throws IllegalStateException if the value type of the measure is not {@link Measure.ValueType#BOOLEAN}
   */
  boolean getBooleanValue();

  /**
   * The value of this measure as a int if the type is {@link Measure.ValueType#INT}.
   *
   * @throws IllegalStateException if the value type of the measure is not {@link Measure.ValueType#INT}
   */
  int getIntValue();

  /**
   * The value of this measure as a long if the type is {@link Measure.ValueType#LONG}.
   *
   * @throws IllegalStateException if the value type of the measure is not {@link Measure.ValueType#LONG}
   */
  long getLongValue();

  /**
   * The value of this measure as a double if the type is {@link Measure.ValueType#DOUBLE}.
   *
   * @throws IllegalStateException if the value type of the measure is not {@link Measure.ValueType#DOUBLE}
   */
  double getDoubleValue();

  /**
   * The value of this measure as a String if the type is {@link Measure.ValueType#STRING}.
   *
   * @throws IllegalStateException if the value type of the measure is not {@link Measure.ValueType#STRING}
   */
  String getStringValue();

  /**
   * The value of this measure as a Level if the type is {@link Measure.ValueType#LEVEL}.
   *
   * @throws IllegalStateException if the value type of the measure is not {@link Measure.ValueType#LEVEL}
   */
  Level getLevelValue();

  /**
   * The data of this measure if it exists.
   * <p>
   * If the measure type is {@link Measure.ValueType#STRING}, the value returned by this function is the same as {@link #getStringValue()}.
   * </p>
   */
  String getData();

  /**
   * Any Measure, which ever is its value type, can have a QualityGate status.
   */
  boolean hasQualityGateStatus();

  /**
   * The QualityGate status for this measure.
   * <strong>Don't call this method unless you've checked the result of {@link #hasQualityGateStatus()} first</strong>
   *
   * @throws IllegalStateException if the measure has no QualityGate status
   */
  QualityGateStatus getQualityGateStatus();

  /**
   * Any Measure, which ever is its value type, can have a variation.
   */
  boolean hasVariation();

  /**
   * The variation of this measure.
   *
   * @throws IllegalStateException if the measure has no variation
   */
  double getVariation();

  static NewMeasureBuilder newMeasureBuilder() {
    return new NewMeasureBuilder();
  }

  static UpdateMeasureBuilder updatedMeasureBuilder(Measure measure) {
    return new UpdateMeasureBuilder(measure);
  }

  class MeasureImpl implements Measure {
    private final ValueType valueType;
    @CheckForNull
    private final Double value;
    @CheckForNull
    private final String data;
    @CheckForNull
    private final Level dataLevel;
    @CheckForNull
    private final QualityGateStatus qualityGateStatus;
    @CheckForNull
    private final Double variation;

    private MeasureImpl(ValueType valueType, @Nullable Double value, @Nullable String data, @Nullable Level dataLevel,
      @Nullable QualityGateStatus qualityGateStatus, @Nullable Double variation) {
      this.valueType = valueType;
      this.value = DoubleCache.intern(value);
      this.data = data;
      this.dataLevel = dataLevel;
      this.qualityGateStatus = qualityGateStatus;
      this.variation = variation;
    }

    @Override
    public ValueType getValueType() {
      return valueType;
    }

    @Override
    public boolean getBooleanValue() {
      checkValueType(ValueType.BOOLEAN, valueType);
      return value != null && value.intValue() == 1;
    }

    @Override
    public int getIntValue() {
      checkValueType(ValueType.INT, valueType);
      return value.intValue();
    }

    @Override
    public long getLongValue() {
      checkValueType(ValueType.LONG, valueType);
      return value.longValue();
    }

    @Override
    public double getDoubleValue() {
      checkValueType(ValueType.DOUBLE, valueType);
      return value;
    }

    @Override
    public String getStringValue() {
      checkValueType(ValueType.STRING, valueType);
      return data;
    }

    @Override
    public Level getLevelValue() {
      checkValueType(ValueType.LEVEL, valueType);
      return dataLevel;
    }

    @Override
    public String getData() {
      return data;
    }

    @Override
    public boolean hasQualityGateStatus() {
      return this.qualityGateStatus != null;
    }

    @Override
    public QualityGateStatus getQualityGateStatus() {
      checkState(qualityGateStatus != null, "Measure does not have an QualityGate status");
      return this.qualityGateStatus;
    }

    @Override
    public boolean hasVariation() {
      return variation != null;
    }

    @Override
    public double getVariation() {
      checkState(variation != null, "Measure does not have variation");
      return variation;
    }

    private static void checkValueType(ValueType expected, ValueType valueType) {
      if (valueType != expected) {
        throw new IllegalStateException(
          String.format(
            "value can not be converted to %s because current value type is a %s",
            expected.toString().toLowerCase(Locale.US),
            valueType));
      }
    }

    @Override
    public String toString() {
      return com.google.common.base.MoreObjects.toStringHelper(this)
        .add("valueType", valueType)
        .add("value", value)
        .add("data", data)
        .add("dataLevel", dataLevel)
        .add("qualityGateStatus", qualityGateStatus)
        .add("variations", variation)
        .toString();
    }
  }

  class ValueMeasureImpl implements Measure {
    private final ValueType valueType;
    @CheckForNull
    private final Double value;

    private ValueMeasureImpl(ValueType valueType, @Nullable Double value) {
      this.valueType = valueType;
      this.value = DoubleCache.intern(value);
    }

    @Override
    public ValueType getValueType() {
      return valueType;
    }

    @Override
    public boolean getBooleanValue() {
      MeasureImpl.checkValueType(ValueType.BOOLEAN, valueType);
      return value != null && value.intValue() == 1;
    }

    @Override
    public int getIntValue() {
      MeasureImpl.checkValueType(ValueType.INT, valueType);
      return value.intValue();
    }

    @Override
    public long getLongValue() {
      MeasureImpl.checkValueType(ValueType.LONG, valueType);
      return value.longValue();
    }

    @Override
    public double getDoubleValue() {
      MeasureImpl.checkValueType(ValueType.DOUBLE, valueType);
      return value;
    }

    @Override
    public String getStringValue() {
      throw new IllegalStateException();
    }

    @Override
    public Level getLevelValue() {
      throw new IllegalStateException();
    }

    @Override
    public String getData() {
      return null;
    }

    @Override
    public boolean hasQualityGateStatus() {
      return false;
    }

    @Override
    public QualityGateStatus getQualityGateStatus() {
      throw new IllegalStateException("Measure does not have an QualityGate status");
    }

    @Override
    public boolean hasVariation() {
      return false;
    }

    @Override
    public double getVariation() {
      throw new IllegalStateException("Measure does not have variation");

    }

    @Override
    public String toString() {
      return com.google.common.base.MoreObjects.toStringHelper(this)
        .add("valueType", valueType)
        .add("value", value)
        .toString();
    }
  }

  class NoValueVariationMeasureImpl implements Measure {
    @Nullable
    private Double variation;
    private ValueType valueType = ValueType.NO_VALUE;

    private NoValueVariationMeasureImpl(@Nullable Double variation) {
      this.variation = variation;
    }

    @Override
    public ValueType getValueType() {
      return ValueType.NO_VALUE;
    }

    @Override
    public boolean getBooleanValue() {
      throw new IllegalStateException();
    }

    @Override
    public int getIntValue() {
      throw new IllegalStateException();
    }

    @Override
    public long getLongValue() {
      throw new IllegalStateException();
    }

    @Override
    public double getDoubleValue() {
      throw new IllegalStateException();
    }

    @Override
    public String getStringValue() {
      throw new IllegalStateException();
    }

    @Override
    public Level getLevelValue() {
      throw new IllegalStateException();

    }

    @Override
    public String getData() {
      return null;
    }

    @Override
    public boolean hasQualityGateStatus() {
      return false;
    }

    @Override
    public QualityGateStatus getQualityGateStatus() {
      throw new IllegalStateException("Measure does not have an QualityGate status");
    }

    @Override
    public boolean hasVariation() {
      return variation != null;
    }

    @Override
    public double getVariation() {
      checkState(variation != null, "Measure does not have variation");
      return variation;
    }

    @Override
    public String toString() {
      return com.google.common.base.MoreObjects.toStringHelper(this)
        .add("valueType", valueType)
        .add("variations", variation)
        .toString();
    }
  }

  class NewMeasureBuilder {
    private QualityGateStatus qualityGateStatus;
    private Double variation;

    public NewMeasureBuilder setQualityGateStatus(QualityGateStatus qualityGateStatus) {
      this.qualityGateStatus = requireNonNull(qualityGateStatus, "QualityGateStatus can not be set to null");
      return this;
    }

    public NewMeasureBuilder setVariation(double variation) {
      this.variation = variation;
      return this;
    }

    public Measure create(boolean value, @Nullable String data) {
      return createInternal(ValueType.BOOLEAN, value ? 1.0D : 0.0D, data);
    }

    public Measure create(boolean value) {
      return create(value, null);
    }

    public Measure create(int value, @Nullable String data) {
      return createInternal(ValueType.INT, (double) value, data);
    }

    public Measure create(int value) {
      return create(value, null);
    }

    public Measure create(long value, @Nullable String data) {
      return createInternal(ValueType.LONG, (double) value, data);
    }

    public Measure create(long value) {
      return create(value, null);
    }

    public Measure create(double value, int decimalScale, @Nullable String data) {
      checkArgument(!Double.isNaN(value), "NaN is not allowed as a Measure value");
      double scaledValue = scale(value, decimalScale);
      return createInternal(ValueType.DOUBLE, scaledValue, data);
    }

    private Measure createInternal(ValueType type, double value, @Nullable String data) {
      if (data == null && qualityGateStatus == null && variation == null) {
        return new ValueMeasureImpl(type, value);

      }
      return new MeasureImpl(type, value, data, null, qualityGateStatus, variation);
    }

    public Measure create(double value, int decimalScale) {
      return create(value, decimalScale, null);
    }

    public Measure create(String value) {
      return new MeasureImpl(ValueType.STRING, null, requireNonNull(value), null, qualityGateStatus, variation);
    }

    public Measure create(Level level) {
      return new MeasureImpl(ValueType.LEVEL, null, null, requireNonNull(level), qualityGateStatus, variation);
    }

    public Measure createNoValue() {
      if (qualityGateStatus == null) {
        return new NoValueVariationMeasureImpl(variation);
      }
      return new MeasureImpl(ValueType.NO_VALUE, null, null, null, qualityGateStatus, variation);
    }

    private static double scale(double value, int decimalScale) {
      BigDecimal bd = BigDecimal.valueOf(value);
      return bd.setScale(decimalScale, RoundingMode.HALF_UP).doubleValue();
    }
  }

  final class UpdateMeasureBuilder {
    private final Measure source;
    private QualityGateStatus qualityGateStatus;
    private Double variation;

    public UpdateMeasureBuilder(Measure source) {
      this.source = requireNonNull(source, "Can not create a measure from null");
    }

    /**
     * Sets the QualityGateStatus of the updated Measure to create.
     *
     * @throws NullPointerException          if the specified {@link QualityGateStatus} is {@code null}
     * @throws UnsupportedOperationException if the source measure already has a {@link QualityGateStatus}
     */
    public UpdateMeasureBuilder setQualityGateStatus(QualityGateStatus qualityGateStatus) {
      if (source.hasQualityGateStatus()) {
        throw new UnsupportedOperationException("QualityGate status can not be changed if already set on source Measure");
      }
      this.qualityGateStatus = requireNonNull(qualityGateStatus, "QualityGateStatus can not be set to null");
      return this;
    }

    /**
     * Sets the variation of the updated Measure to create.
     *
     * @throws UnsupportedOperationException if the source measure already has a variation
     */
    public UpdateMeasureBuilder setVariation(double variation) {
      if (source.hasVariation()) {
        throw new UnsupportedOperationException("Variation can not be changed if already set on source Measure");
      }
      this.variation = variation;
      return this;
    }

    public Measure create() {
      Double value;
      switch (source.getValueType()) {
        case DOUBLE:
          value = source.getDoubleValue();
          break;
        case INT:
          value = (double) source.getIntValue();
          break;
        case LONG:
          value = (double) source.getLongValue();
          break;
        case BOOLEAN:
          value = source.getBooleanValue() ? 1.0 : 0.0;
          break;
        case NO_VALUE:
        default:
          value = null;
          break;
      }
      Level level = source.getValueType() == ValueType.LEVEL ? source.getLevelValue() : null;
      QualityGateStatus status = source.hasQualityGateStatus() ? source.getQualityGateStatus() : qualityGateStatus;
      Double var;
      if (source.hasVariation()) {
        var = source.getVariation();
      } else {
        var = variation;
      }
      return new MeasureImpl(source.getValueType(), value, source.getData(), level, status, var);
    }
  }
}
