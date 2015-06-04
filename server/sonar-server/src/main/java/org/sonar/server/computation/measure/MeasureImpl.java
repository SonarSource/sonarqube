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

import com.google.common.base.Preconditions;
import java.util.Locale;
import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;

public final class MeasureImpl implements Measure {

  private final ValueType valueType;
  @Nullable
  private final Double value;
  @Nullable
  private final String data;
  @Nullable
  private AlertStatus alertStatus;
  @Nullable
  private String alertText;

  protected MeasureImpl(ValueType valueType, @Nullable Double value, @Nullable String data) {
    this.valueType = valueType;
    this.value = value;
    this.data = data;
  }

  public static MeasureImpl create(boolean value, @Nullable String data) {
    return new MeasureImpl(ValueType.BOOLEAN, value ? 1.0d : 0.0d, data);
  }

  public static MeasureImpl create(int value, @Nullable String data) {
    return new MeasureImpl(ValueType.INT, (double) value, data);
  }

  public static MeasureImpl create(long value, @Nullable String data) {
    return new MeasureImpl(ValueType.LONG, (double) value, data);
  }

  public static MeasureImpl create(double value, @Nullable String data) {
    return new MeasureImpl(ValueType.DOUBLE, value, data);
  }

  public static MeasureImpl create(String value) {
    return new MeasureImpl(ValueType.STRING, null, checkNotNull(value));
  }

  public static MeasureImpl createNoValue() {
    return new MeasureImpl(ValueType.NO_VALUE, null, null);
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

  @Override
  public Integer getSeverityIndex() {
    return null;
  }

  public MeasureImpl setAlertStatus(AlertStatus alertStatus) {
    this.alertStatus = checkNotNull(alertStatus, "Can not set a null alert status");
    return this;
  }

  @Override
  public boolean hasAlertStatus() {
    return this.alertStatus != null;
  }

  @Override
  public AlertStatus getAlertStatus() {
    Preconditions.checkState(alertStatus != null, "Measure does not have an alert status");
    return this.alertStatus;
  }

  public MeasureImpl setAlertText(String alertText) {
    this.alertText = Preconditions.checkNotNull(alertText, "Can not set a null alert text");
    return this;
  }

  @Override
  public boolean hasAlertText() {
    return this.alertText != null;
  }

  @Override
  public String getAlertText() {
    Preconditions.checkState(alertText != null, "Measure does not have an alert text");
    return this.alertText;
  }

}
