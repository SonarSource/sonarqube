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

import java.util.Locale;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;

public final class MeasureImpl implements Measure {

  private final ValueType valueType;
  @Nullable
  private final Integer ruleId;
  @Nullable
  private final Integer characteristicId;
  @Nullable
  private final Double value;
  @Nullable
  private final String data;
  @Nullable
  private final Level dataLevel;
  @Nullable
  private QualityGateStatus qualityGateStatus;
  @Nullable
  private MeasureVariations variations;
  @Nullable
  private String description;

  protected MeasureImpl(ValueType valueType, @Nullable Integer ruleId, @Nullable Integer characteristicId, @Nullable Double value, @Nullable String data, @Nullable Level dataLevel) {
    this.valueType = valueType;
    this.ruleId = ruleId;
    this.characteristicId = characteristicId;
    this.value = value;
    this.data = data;
    this.dataLevel = dataLevel;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    public static final String RULE_AND_CHARACTERISTIC_ERROR_MSG = "A measure can not be associated to both a Characteristic and a Rule";
    private Integer ruleId;
    private Integer characteristicId;

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

    public MeasureImpl create(boolean value, @Nullable String data) {
      return new MeasureImpl(ValueType.BOOLEAN, ruleId, characteristicId, value ? 1.0d : 0.0d, data, null);
    }

    public MeasureImpl create(int value, @Nullable String data) {
      return new MeasureImpl(ValueType.INT, ruleId, characteristicId, (double) value, data, null);
    }

    public MeasureImpl create(long value, @Nullable String data) {
      return new MeasureImpl(ValueType.LONG, ruleId, characteristicId, (double) value, data, null);
    }

    public MeasureImpl create(double value, @Nullable String data) {
      return new MeasureImpl(ValueType.DOUBLE, ruleId, characteristicId, value, data, null);
    }

    public MeasureImpl create(String value) {
      return new MeasureImpl(ValueType.STRING, ruleId, characteristicId, null, requireNonNull(value), null);
    }

    public MeasureImpl create(Level level) {
      return new MeasureImpl(ValueType.LEVEL, ruleId, characteristicId, null, null, requireNonNull(level));
    }

    public MeasureImpl createNoValue() {
      return new MeasureImpl(ValueType.NO_VALUE, ruleId, characteristicId, null, null, null);
    }
  }


  @Override
  @CheckForNull
  public Integer getRuleId() {
    return ruleId;
  }

  @Override
  @CheckForNull
  public Integer getCharacteristicId() {
    return characteristicId;
  }

  @Override
  public ValueType getValueType() {
    return valueType;
  }

  @Override
  public boolean getBooleanValue() {
    checkValueType(ValueType.BOOLEAN);
    return value == 1.0d;
  }

  @Override
  public int getIntValue() {
    checkValueType(ValueType.INT);
    return value.intValue();
  }

  @Override
  public long getLongValue() {
    checkValueType(ValueType.LONG);
    return value.longValue();
  }

  @Override
  public double getDoubleValue() {
    checkValueType(ValueType.DOUBLE);
    return value;
  }

  @Override
  public String getStringValue() {
    checkValueType(ValueType.STRING);
    return data;
  }

  @Override
  public Level getLevelValue() {
    checkValueType(ValueType.LEVEL);
    return dataLevel;
  }

  @Override
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

  public MeasureImpl setQualityGateStatus(QualityGateStatus qualityGateStatus) {
    this.qualityGateStatus = requireNonNull(qualityGateStatus, "Can not set a null QualityGate status");
    return this;
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

  public MeasureImpl setVariations(MeasureVariations variations) {
    this.variations = requireNonNull(variations, "Can not set null MeasureVariations");
    return this;
  }

  @Override
  public boolean hasVariations() {
    return variations != null;
  }

  @Override
  public MeasureVariations getVariations() {
    checkState(variations != null, "Measure does not have variations");
    return variations;
  }

  public void setDescription(String description) {
    this.description = requireNonNull(description);
  }

  @Override
  @CheckForNull
  public String getDescription() {
    return description;
  }
}
