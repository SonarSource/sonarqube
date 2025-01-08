/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.server.metric;

import java.util.List;
import org.sonar.api.measures.Metric;
import org.sonar.api.measures.Metrics;

import static java.util.Arrays.asList;
import static org.sonar.api.measures.CoreMetrics.DOMAIN_SIZE;

public class UnanalyzedLanguageMetrics implements Metrics {

  public static final String UNANALYZED_C_KEY = "unanalyzed_c";

  public static final Metric<Integer> UNANALYZED_C = new Metric.Builder(UNANALYZED_C_KEY, "Number of unanalyzed c files", Metric.ValueType.INT)
    .setDirection(Metric.DIRECTION_WORST)
    .setQualitative(false)
    .setDomain(DOMAIN_SIZE)
    .setHidden(true)
    .create();

  public static final String UNANALYZED_CPP_KEY = "unanalyzed_cpp";

  public static final Metric<Integer> UNANALYZED_CPP = new Metric.Builder(UNANALYZED_CPP_KEY, "Number of unanalyzed c++ files", Metric.ValueType.INT)
    .setDirection(Metric.DIRECTION_WORST)
    .setQualitative(false)
    .setDomain(DOMAIN_SIZE)
    .setHidden(true)
    .create();

  @Override
  public List<Metric> getMetrics() {
    return asList(UNANALYZED_C, UNANALYZED_CPP);
  }
}
