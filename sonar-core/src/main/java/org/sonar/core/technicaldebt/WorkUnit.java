/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
package org.sonar.core.technicaldebt;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;

import javax.annotation.Nullable;

public final class WorkUnit {

  public static final String DAYS = "d";
  public static final String MINUTES = "mn";
  public static final String HOURS = "h";
  public static final String DEFAULT_UNIT = DAYS;
  private static final String[] UNITS = {DAYS, MINUTES, HOURS};

  public static final double DEFAULT_VALUE = 1.0;

  private double value;
  private String unit;

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
    unit = StringUtils.defaultIfEmpty(unit, DEFAULT_UNIT);
    if (!ArrayUtils.contains(UNITS, unit)) {
      throw new IllegalArgumentException("Remediation factor unit can not be: " + unit + ". Possible values are " + ArrayUtils.toString(UNITS));
    }
    double d = value != null ? value : DEFAULT_VALUE;
    if (d < 0.0) {
      throw new IllegalArgumentException("Remediation factor can not be negative: " + d);
    }
    return new WorkUnit(d, unit);
  }

  public static WorkUnit createInDays(Double value) {
    return create(value, DAYS);
  }
}
