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

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;
import org.sonar.db.metric.MetricDto;
import org.sonar.server.metric.StandardToMQRMetrics;

public class QualityGateModeChecker {

  public QualityModeResult getUsageOfModeMetrics(Collection<MetricDto> metrics) {
    Set<String> metricKeys = metrics.stream().map(MetricDto::getKey).collect(Collectors.toSet());

    boolean hasStandardConditions = metricKeys.stream().anyMatch(StandardToMQRMetrics::isStandardMetric);
    boolean hasMQRConditions = metricKeys.stream().anyMatch(StandardToMQRMetrics::isMQRMetric);
    return new QualityModeResult(hasMQRConditions, hasStandardConditions);
  }

  public record QualityModeResult(boolean hasMQRConditions, boolean hasStandardConditions) {
  }

}
