/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.computation.measure;

import com.google.common.base.Optional;
import java.util.Locale;
import java.util.Objects;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

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
  private final Integer ruleId;
  @CheckForNull
  private final Integer characteristicId;
  @CheckForNull
  private final Double value;
  @CheckForNull
  private final String data;
  @CheckForNull
  private final Level dataLevel;
  @CheckForNull
  private final String description;
  @CheckForNull
  private QualityGateStatus qualityGateStatus;
  @CheckForNull
  private MeasureVariations variations;

  private Measure(ValueType valueType, @Nullable Integer ruleId, @Nullable Integer characteristicId,
    @Nullable Double value, @Nullable String data, @Nullable Level dataLevel,
    @Nullable String description) {
    this.valueType = valueType;
    this.ruleId = ruleId;
    this.characteristicId = characteristicId;
    this.value = value;
    this.data = data;
    this.dataLevel = dataLevel;
    this.description = description;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    public static final String RULE_AND_CHARACTERISTIC_ERROR_MSG = "A measure can not be associated to both a Characteristic and a Rule";
    private Integer ruleId;
    private Integer characteristicId;
    private String description;

    /**
     * Sets the rule this measure is associated to.
     *
     * @throws UnsupportedOperationException if the characteristicId as already been set
     *
     * @see #forCharacteristic(int)
     */
    public Builder forRule(int ruleId) {
      if (characteristicId != null) {
        throw new UnsupportedOperationException(RULE_AND_CHARACTERISTIC_ERROR_MSG);
      }
      this.ruleId = ruleId;
      return this;
    }

    /**
     * Sets the characteristic this measure is associated to.
     *
     * @throws UnsupportedOperationException if the ruleId as already been set
     *
     * @see #forCharacteristic(int)
     */
    public Builder forCharacteristic(int characteristicId) {
      if (ruleId != null) {
        throw new UnsupportedOperationException(RULE_AND_CHARACTERISTIC_ERROR_MSG);
      }
      this.characteristicId = characteristicId;
      return this;
    }

    /**
     * Sets the description of the measure
     *
     * @throws NullPointerException if the specified argument is {@code null}
     */
    public Builder withDescription(String description) {
      this.description = requireNonNull(description, "description can not be set to null");
      return this;
    }

    public Measure create(boolean value, @Nullable String data) {
      return new Measure(ValueType.BOOLEAN, ruleId, characteristicId, value ? 1.0d : 0.0d, data, null, description);
    }

    public Measure create(int value, @Nullable String data) {
      return new Measure(ValueType.INT, ruleId, characteristicId, (double) value, data, null, description);
    }

    public Measure create(long value, @Nullable String data) {
      return new Measure(ValueType.LONG, ruleId, characteristicId, (double) value, data, null, description);
    }

    public Measure create(double value, @Nullable String data) {
      return new Measure(ValueType.DOUBLE, ruleId, characteristicId, value, data, null, description);
    }

    public Measure create(String value) {
      return new Measure(ValueType.STRING, ruleId, characteristicId, null, requireNonNull(value), null, description);
    }

    public Measure create(Level level) {
      return new Measure(ValueType.LEVEL, ruleId, characteristicId, null, null, requireNonNull(level), description);
    }

    public Measure createNoValue() {
      return new Measure(ValueType.NO_VALUE, ruleId, characteristicId, null, null, null, description);
    }
  }

  @CheckForNull
  public Integer getRuleId() {
    return ruleId;
  }

  @CheckForNull
  public Integer getCharacteristicId() {
    return characteristicId;
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
          valueType
          ));
    }
  }

  public Measure setQualityGateStatus(QualityGateStatus qualityGateStatus) {
    this.qualityGateStatus = requireNonNull(qualityGateStatus, "Can not set a null QualityGate status");
    return this;
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
   * Sets the variations of this Measure.
   *
   * @throws NullPointerException if the specified argument is {@code null}
   * @throws IllegalStateException if the variations have already been set
   */
  public Measure setVariations(MeasureVariations variations) {
    this.variations = requireNonNull(variations, "Can not set null MeasureVariations");
    return this;
  }

  /**
   * Any Measure, which ever is its value type, can have a Variations.
   */
  public boolean hasVariations() {
    return variations != null;
  }

  /**
   * The variations of this measure.
   *
   * @throws IllegalStateException if the measure has no MeasureVariations
   */
  public MeasureVariations getVariations() {
    checkState(variations != null, "Measure does not have variations");
    return variations;
  }

  /**
   * The optional description of the measure. Relevant for manual measures.
   */
  @CheckForNull
  public String getDescription() {
    return description;
  }

  /**
   * a Metric is equal to another Metric if it has the same ruleId/characteristicId paar (both being potentially
   * {@code null} but only one of them can be non {@code null}.
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
    return Objects.equals(ruleId, measure.ruleId) &&
      Objects.equals(characteristicId, measure.characteristicId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(ruleId, characteristicId);
  }

  @Override
  public String toString() {
    return com.google.common.base.Objects.toStringHelper(this)
      .add("valueType", valueType)
      .add("ruleId", ruleId)
      .add("characteristicId", characteristicId)
      .add("value", value)
      .add("data", data)
      .add("dataLevel", dataLevel)
      .add("qualityGateStatus", qualityGateStatus)
      .add("variations", variations)
      .add("description", description)
      .toString();
  }
}
