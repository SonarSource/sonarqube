/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.server.computation.task.projectanalysis.measure;

import com.google.common.base.Optional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.Objects;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.server.computation.task.projectanalysis.component.Developer;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;

public final class Measure {

  public enum ValueType {
    NO_VALUE, BOOLEAN, INT, LONG, DOUBLE, STRING, LEVEL
  }

  public enum Level {
    OK("Green"),
    WARN("Orange"),
    ERROR("Red");

    private final String colorName;

    Level(String colorName) {
      this.colorName = colorName;
    }

    public String getColorName() {
      return colorName;
    }

    public static Optional<Level> toLevel(@Nullable String level) {
      if (level == null) {
        return Optional.absent();
      }

      try {
        return Optional.of(Level.valueOf(level));
      } catch (IllegalArgumentException e) {
        return Optional.absent();
      }
    }
  }

  private final ValueType valueType;
  @CheckForNull
  private final Developer developer;
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

  private Measure(ValueType valueType, @Nullable Developer developer,
    @Nullable Double value, @Nullable String data, @Nullable Level dataLevel,
    @Nullable QualityGateStatus qualityGateStatus, @Nullable Double variation) {
    this.valueType = valueType;
    this.developer = developer;
    this.value = value;
    this.data = data;
    this.dataLevel = dataLevel;
    this.qualityGateStatus = qualityGateStatus;
    this.variation = variation;
  }

  public static NewMeasureBuilder newMeasureBuilder() {
    return new NewMeasureBuilder();
  }

  public static UpdateMeasureBuilder updatedMeasureBuilder(Measure measure) {
    return new UpdateMeasureBuilder(measure);
  }

  public static final class NewMeasureBuilder {
    private Developer developer;
    private QualityGateStatus qualityGateStatus;
    private Double variation;

    /**
     * Sets the developer this measure is associated to.
     *
     */
    public NewMeasureBuilder forDeveloper(Developer developer) {
      this.developer = developer;
      return this;
    }

    public NewMeasureBuilder setQualityGateStatus(QualityGateStatus qualityGateStatus) {
      this.qualityGateStatus = requireNonNull(qualityGateStatus, "QualityGateStatus can not be set to null");
      return this;
    }

    public NewMeasureBuilder setVariation(double variation) {
      this.variation = variation;
      return this;
    }

    public Measure create(boolean value, @Nullable String data) {
      return new Measure(ValueType.BOOLEAN, developer, value ? 1.0d : 0.0d, data, null, qualityGateStatus, variation);
    }

    public Measure create(boolean value) {
      return create(value, null);
    }

    public Measure create(int value, @Nullable String data) {
      return new Measure(ValueType.INT, developer, (double) value, data, null, qualityGateStatus, variation);
    }

    public Measure create(int value) {
      return create(value, null);
    }

    public Measure create(long value, @Nullable String data) {
      return new Measure(ValueType.LONG, developer, (double) value, data, null, qualityGateStatus, variation);
    }

    public Measure create(long value) {
      return create(value, null);
    }

    public Measure create(double value, int decimalScale, @Nullable String data) {
      checkArgument(!Double.isNaN(value), "NaN is not allowed as a Measure value");
      double scaledValue = scale(value, decimalScale);
      return new Measure(ValueType.DOUBLE, developer, scaledValue, data, null, qualityGateStatus, variation);
    }

    public Measure create(double value, int decimalScale) {
      return create(value, decimalScale, null);
    }

    public Measure create(String value) {
      return new Measure(ValueType.STRING, developer, null, requireNonNull(value), null, qualityGateStatus, variation);
    }

    public Measure create(Level level) {
      return new Measure(ValueType.LEVEL, developer, null, null, requireNonNull(level), qualityGateStatus, variation);
    }

    public Measure createNoValue() {
      return new Measure(ValueType.NO_VALUE, developer, null, null, null, qualityGateStatus, variation);
    }

    private static double scale(double value, int decimalScale) {
      BigDecimal bd = BigDecimal.valueOf(value);
      return bd.setScale(decimalScale, RoundingMode.HALF_UP).doubleValue();
    }
  }

  public static final class UpdateMeasureBuilder {
    private final Measure source;
    private QualityGateStatus qualityGateStatus;
    private Double variation;

    public UpdateMeasureBuilder(Measure source) {
      this.source = requireNonNull(source, "Can not create a measure from null");
    }

    /**
     * Sets the QualityGateStatus of the updated Measure to create.
     *
     * @throws NullPointerException if the specified {@link QualityGateStatus} is {@code null}
     * @throws UnsupportedOperationException if the source measure already has a {@link QualityGateStatus}
     */
    public UpdateMeasureBuilder setQualityGateStatus(QualityGateStatus qualityGateStatus) {
      if (source.qualityGateStatus != null) {
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
      if (source.variation != null) {
        throw new UnsupportedOperationException("Variation can not be changed if already set on source Measure");
      }
      this.variation = variation;
      return this;
    }

    public Measure create() {
      return new Measure(source.valueType, source.developer,
        source.value, source.data, source.dataLevel,
        source.qualityGateStatus == null ? qualityGateStatus : source.qualityGateStatus,
        source.variation == null ? variation : source.variation);
    }
  }

  @CheckForNull
  public Developer getDeveloper() {
    return developer;
  }

  /**
   * The type of value stored in the measure.
   */
  public ValueType getValueType() {
    return valueType;
  }

  /**
   * The value of this measure as a boolean if the type is {@link Measure.ValueType#BOOLEAN}.
   *
   * @throws IllegalStateException if the value type of the measure is not {@link Measure.ValueType#BOOLEAN}
   */
  public boolean getBooleanValue() {
    checkValueType(ValueType.BOOLEAN);
    return value == 1.0d;
  }

  /**
   * The value of this measure as a int if the type is {@link Measure.ValueType#INT}.
   *
   * @throws IllegalStateException if the value type of the measure is not {@link Measure.ValueType#INT}
   */
  public int getIntValue() {
    checkValueType(ValueType.INT);
    return value.intValue();
  }

  /**
   * The value of this measure as a long if the type is {@link Measure.ValueType#LONG}.
   *
   * @throws IllegalStateException if the value type of the measure is not {@link Measure.ValueType#LONG}
   */
  public long getLongValue() {
    checkValueType(ValueType.LONG);
    return value.longValue();
  }

  /**
   * The value of this measure as a double if the type is {@link Measure.ValueType#DOUBLE}.
   *
   * @throws IllegalStateException if the value type of the measure is not {@link Measure.ValueType#DOUBLE}
   */
  public double getDoubleValue() {
    checkValueType(ValueType.DOUBLE);
    return value;
  }

  /**
   * The value of this measure as a String if the type is {@link Measure.ValueType#STRING}.
   *
   * @throws IllegalStateException if the value type of the measure is not {@link Measure.ValueType#STRING}
   */
  public String getStringValue() {
    checkValueType(ValueType.STRING);
    return data;
  }

  /**
   * The value of this measure as a Level if the type is {@link Measure.ValueType#LEVEL}.
   *
   * @throws IllegalStateException if the value type of the measure is not {@link Measure.ValueType#LEVEL}
   */
  public Level getLevelValue() {
    checkValueType(ValueType.LEVEL);
    return dataLevel;
  }

  /**
   * The data of this measure if it exists.
   * <p>
   * If the measure type is {@link Measure.ValueType#STRING}, the value returned by this function is the same as {@link #getStringValue()}.
   * </p>
   */
  public String getData() {
    return data;
  }

  private void checkValueType(ValueType expected) {
    if (valueType != expected) {
      throw new IllegalStateException(
        String.format(
          "value can not be converted to %s because current value type is a %s",
          expected.toString().toLowerCase(Locale.US),
          valueType));
    }
  }

  /**
   * Any Measure, which ever is its value type, can have a QualityGate status.
   */
  public boolean hasQualityGateStatus() {
    return this.qualityGateStatus != null;
  }

  /**
   * The QualityGate status for this measure.
   * <strong>Don't call this method unless you've checked the result of {@link #hasQualityGateStatus()} first</strong>
   *
   * @throws IllegalStateException if the measure has no QualityGate status
   */
  public QualityGateStatus getQualityGateStatus() {
    checkState(qualityGateStatus != null, "Measure does not have an QualityGate status");
    return this.qualityGateStatus;
  }

  /**
   * Any Measure, which ever is its value type, can have a variation.
   */
  public boolean hasVariation() {
    return variation != null;
  }

  /**
   * The variation of this measure.
   *
   * @throws IllegalStateException if the measure has no variation
   */
  public double getVariation() {
    checkState(variation != null, "Measure does not have variation");
    return variation;
  }

  /**
   * a Metric is equal to another Metric if it has the same ruleId/characteristicId paar (both being potentially
   * {@code null} but only one of them can be non {@code null}).
   */
  @Override
  public boolean equals(@Nullable Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Measure measure = (Measure) o;
    return Objects.equals(developer, measure.developer);
  }

  @Override
  public int hashCode() {
    return Objects.hash(developer);
  }

  @Override
  public String toString() {
    return com.google.common.base.MoreObjects.toStringHelper(this)
      .add("valueType", valueType)
      .add("developer", developer)
      .add("value", value)
      .add("data", data)
      .add("dataLevel", dataLevel)
      .add("qualityGateStatus", qualityGateStatus)
      .add("variations", variation)
      .toString();
  }

}
