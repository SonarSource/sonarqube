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
package org.sonar.server.qualitygate;

import java.util.stream.Stream;

import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Metric;
import org.sonar.core.metric.SoftwareQualitiesMetrics;

public class BaseAllowedQualityGateRatingMetricKeysSource extends AllowedQualityGateRatingMetricKeysSource {
  protected Stream<Metric<Integer>> allMetrics() {
    return Stream.concat(
      CoreMetrics.getMetrics().stream()
        // All Rating types are integers, but they come out of getMetrics as just Metric.
        // Convert them to Metric<Integer> here and below.
        .filter(m -> m.getType().equals(Metric.ValueType.RATING))
        .map(m -> (Metric<Integer>) m),
      new SoftwareQualitiesMetrics().getMetrics().stream()
        .filter(m -> m.getType().equals(Metric.ValueType.RATING))
        .map(m -> (Metric<Integer>) m));
  }
}
