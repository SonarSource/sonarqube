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

package org.sonar.xoo.measures;

import com.google.common.collect.Lists;
import java.util.List;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Metric;
import org.sonar.api.measures.Metrics;

public final class XooMetrics implements Metrics {

  public static final String CONSTANT_FLOAT_MEASURE_KEY = "xoo_constant_float_measure";

  public static final Metric<Float> CONSTANT_FLOAT_MEASURE = new Metric.Builder(CONSTANT_FLOAT_MEASURE_KEY, "Constant float measure", Metric.ValueType.FLOAT)
    .setDescription("Return always the same float measure for every components")
    .setDirection(Metric.DIRECTION_WORST)
    .setDomain(CoreMetrics.DOMAIN_GENERAL)
    .create();

  @Override
  public List<Metric> getMetrics() {
    return Lists.<Metric>newArrayList(CONSTANT_FLOAT_MEASURE);
  }
}
