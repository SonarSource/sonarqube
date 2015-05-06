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

package org.sonar.server.measure;

import com.google.common.collect.ImmutableList;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Metric;

import java.util.List;

public class ServerMetrics {

  private ServerMetrics() {
    // only static stuff
  }

  public static final String DEPENDENCY_MATRIX_KEY = "dsm_data";

  public static final Metric<String> DEPENDENCY_MATRIX = new Metric.Builder(DEPENDENCY_MATRIX_KEY, "Dependency Matrix", Metric.ValueType.DATA)
    .setDescription("Dependency Matrix")
    .setDirection(Metric.DIRECTION_NONE)
    .setQualitative(false)
    .setDomain(CoreMetrics.DOMAIN_DESIGN)
    .setDeleteHistoricalData(true)
    .create();

  public static List<Metric> getMetrics() {
    return ImmutableList.<Metric>of(DEPENDENCY_MATRIX);
  }

}
