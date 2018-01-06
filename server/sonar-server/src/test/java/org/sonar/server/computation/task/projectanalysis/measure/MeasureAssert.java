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
import java.util.Objects;
import javax.annotation.Nullable;
import org.assertj.core.api.AbstractAssert;
import org.assertj.core.data.Offset;

import static java.lang.Math.abs;

public class MeasureAssert extends AbstractAssert<MeasureAssert, Measure> {

  protected MeasureAssert(@Nullable Measure actual) {
    super(actual, MeasureAssert.class);
  }

  public static MeasureAssert assertThat(Measure actual) {
    return new MeasureAssert(actual);
  }

  public static MeasureAssert assertThat(@Nullable Optional<Measure> actual) {
    return new MeasureAssert(actual == null ? null : actual.orNull());
  }

  public MeasureAssert hasValueType(Measure.ValueType expected) {
    isNotNull();

    if (actual.getValueType() != expected) {
      failWithMessage("Expected ValueType of Measure to be <%s> but was <%s>", expected, actual.getValueType());
    }

    return this;
  }

  public MeasureAssert hasValue(int expected) {
    isNotNull();

    if (actual.getValueType() != Measure.ValueType.INT) {
      failWithMessage(
        "Expected Measure to have an int value and therefore its ValueType to be <%s> but was <%s>",
        Measure.ValueType.INT, actual.getValueType());
    }

    if (actual.getIntValue() != expected) {
      failWithMessage("Expected value of Measure to be <%s> but was <%s>", expected, actual.getIntValue());
    }

    return this;
  }

  public MeasureAssert hasValue(long expected) {
    isNotNull();

    if (actual.getValueType() != Measure.ValueType.LONG) {
      failWithMessage(
        "Expected Measure to have a long value and therefore its ValueType to be <%s> but was <%s>",
        Measure.ValueType.LONG, actual.getValueType());
    }

    if (actual.getLongValue() != expected) {
      failWithMessage("Expected value of Measure to be <%s> but was <%s>", expected, actual.getLongValue());
    }

    return this;
  }

  public MeasureAssert hasValue(double expected) {
    isNotNull();

    if (actual.getValueType() != Measure.ValueType.DOUBLE) {
      failWithMessage(
        "Expected Measure to have a double value and therefore its ValueType to be <%s> but was <%s>",
        Measure.ValueType.DOUBLE, actual.getValueType());
    }

    if (actual.getDoubleValue() != expected) {
      failWithMessage("Expected value of Measure to be <%s> but was <%s>", expected, actual.getDoubleValue());
    }

    return this;
  }

  public MeasureAssert hasValue(boolean expected) {
    isNotNull();

    if (actual.getValueType() != Measure.ValueType.BOOLEAN) {
      failWithMessage(
        "Expected Measure to have a boolean value and therefore its ValueType to be <%s> but was <%s>",
        Measure.ValueType.DOUBLE, actual.getValueType());
    }

    if (actual.getBooleanValue() != expected) {
      failWithMessage("Expected value of Measure to be <%s> but was <%s>", expected, actual.getBooleanValue());
    }

    return this;
  }

  public MeasureAssert hasValue(String expected) {
    isNotNull();

    if (actual.getValueType() != Measure.ValueType.STRING) {
      failWithMessage(
        "Expected Measure to have a String value and therefore its ValueType to be <%s> but was <%s>",
        Measure.ValueType.DOUBLE, actual.getValueType());
    }

    if (!Objects.equals(actual.getStringValue(), expected)) {
      failWithMessage("Expected value of Measure to be <%s> but was <%s>", expected, actual.getStringValue());
    }

    return this;
  }

  public MeasureAssert hasValue(Measure.Level expected) {
    isNotNull();

    if (actual.getValueType() != Measure.ValueType.LEVEL) {
      failWithMessage(
        "Expected Measure to have a Level value and therefore its ValueType to be <%s> but was <%s>",
        Measure.ValueType.DOUBLE, actual.getValueType());
    }

    if (actual.getLevelValue() != expected) {
      failWithMessage("Expected value of Measure to be <%s> but was <%s>", expected, actual.getLevelValue());
    }

    return this;
  }

  public MeasureAssert hasNoValue() {
    isNotNull();

    if (actual.getValueType() != Measure.ValueType.NO_VALUE) {
      failWithMessage(
        "Expected Measure to have no value and therefore its ValueType to be <%s> but was <%s>",
        Measure.ValueType.DOUBLE, actual.getValueType());
    }

    return this;
  }

  public MeasureAssert hasData(String expected) {
    isNotNull();

    if (!Objects.equals(actual.getData(), expected)) {
      failWithMessage("Expected data of Measure to be <%s> but was <%s>", expected, actual.getData());
    }

    return this;
  }

  public MeasureAssert hasNoData() {
    isNotNull();

    if (actual.getData() == null) {
      failWithMessage("Expected Measure to have no data but was <%s>", actual.getData());
    }

    return this;
  }

  public MeasureAssert hasQualityGateLevel(Measure.Level expected) {
    isNotNull();
    hasQualityGateStatus();

    if (actual.getQualityGateStatus().getStatus() != expected) {
      failWithMessage("Expected Level of QualityGateStatus of Measure to be <%s> but was <%s>", expected, actual.getQualityGateStatus().getStatus());
    }

    return this;
  }

  public MeasureAssert hasQualityGateText(String expected) {
    isNotNull();
    hasQualityGateStatus();

    if (!Objects.equals(actual.getQualityGateStatus().getText(), expected)) {
      failWithMessage("Expected text of QualityGateStatus of Measure to be \n<%s>\n but was \n<%s>", expected, actual.getQualityGateStatus().getText());
    }

    return this;
  }

  private void hasQualityGateStatus() {
    if (!actual.hasQualityGateStatus()) {
      failWithMessage("Expected Measure to have a QualityGateStatus but it did not");
    }
  }

  public MeasureAssert hasVariation(double expected) {
    isNotNull();
    hasVariation();

    if (!actual.hasVariation()) {
      failWithMessage("Expected Measure to have a variation but it did not");
    }

    double variation = actual.getVariation();
    if (variation != expected) {
      failWithMessage("Expected variation of Measure to be <%s> but was <%s>", expected, variation);
    }

    return this;
  }

  public MeasureAssert hasVariation(double expected, Offset<Double> offset) {
    isNotNull();
    hasVariation();

    if (!actual.hasVariation()) {
      failWithMessage("Expected Measure to have a variation but it did not");
    }

    double variation = actual.getVariation();
    if (abs(expected - variation) > offset.value) {
      failWithMessage(
        "Expected variation of Measure to be close to <%s> by less than <%s> but was <%s>",
        expected, offset.value, variation);
    }

    return this;
  }

  private void hasVariation() {
    if (!actual.hasVariation()) {
      failWithMessage("Expected Measure to have a variation but it did not");
    }
  }

  public void isAbsent() {
    if (actual != null) {
      failWithMessage("Expected measure to be absent");
    }
  }
}
