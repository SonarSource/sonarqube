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
package com.sonarsource.decimal_scale_of_measures;

import java.util.Collections;
import java.util.List;
import org.sonar.api.measures.Metric;
import org.sonar.api.measures.Metrics;

public class DecimalScaleMetric implements Metrics {

  public static final String KEY = "decimal_scale";

  private static final Metric METRIC = new Metric.Builder(KEY, "Decimal Scale", Metric.ValueType.FLOAT)
    .setDescription("Numeric metric with overridden decimal scale")
    .setDecimalScale(4)
    .create();

  @Override
  public List getMetrics() {
    return Collections.singletonList(definition());
  }

  public static Metric definition() {
    return METRIC;
  }
}
