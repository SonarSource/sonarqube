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
package org.sonar.api.utils;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

import javax.annotation.Nullable;

import java.io.Serializable;

/**
 * @deprecated since 4.2. Use WorkDuration instead
 */
@Deprecated
public final class WorkUnit implements Serializable {

  public static final String DAYS = "d";
  public static final String MINUTES = "mn";
  public static final String HOURS = "h";
  public static final String DEFAULT_UNIT = DAYS;
  private static final String[] UNITS = {DAYS, MINUTES, HOURS};

  public static final double DEFAULT_VALUE = 0.0;

  private double value = 0d;
  private String unit = DEFAULT_UNIT;

  WorkUnit(double value, String unit) {
    this.value = value;
    this.unit = unit;
  }

  public double getValue() {
    return value;
  }

  public String getUnit() {
    return unit;
  }

  public static WorkUnit create(@Nullable Double value, @Nullable String unit) {
    String defaultIfEmptyUnit = StringUtils.defaultIfEmpty(unit, DEFAULT_UNIT);
    if (!ArrayUtils.contains(UNITS, defaultIfEmptyUnit)) {
      throw new IllegalArgumentException("Unit can not be: " + defaultIfEmptyUnit + ". Possible values are " + ArrayUtils.toString(UNITS));
    }
    double d = value != null ? value : DEFAULT_VALUE;
    if (d < 0.0) {
      throw new IllegalArgumentException("Value can not be negative: " + d);
    }
    return new WorkUnit(d, defaultIfEmptyUnit);
  }

  public static WorkUnit create() {
    return create(0d, DEFAULT_UNIT);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    WorkUnit workUnit = (WorkUnit) o;

    if (Double.compare(workUnit.value, value) != 0) {
      return false;
    }
    return unit.equals(workUnit.unit);

  }

  @Override
  public int hashCode() {
    int result;
    long temp;
    temp = Double.doubleToLongBits(value);
    result = (int) (temp ^ (temp >>> 32));
    result = 31 * result + unit.hashCode();
    return result;
  }

  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
  }
}
