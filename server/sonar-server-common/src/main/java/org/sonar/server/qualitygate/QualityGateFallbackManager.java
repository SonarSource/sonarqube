/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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

import java.util.Optional;
import org.sonar.server.metric.StandardToMQRMetrics;

/**
 * This class is used to manage the fallback of the quality gate conditions, in case one of the measure has not been computed yet.
 * This is to ensure continuity of the quality gate evaluation in the scenario of introduction of new measures, but the project has not been reanalyzed yet.
 * The live quality gate evaluation will fallback to an equivalent metric until the project is reanalyzed
 */
public class QualityGateFallbackManager {

  public Optional<Condition> getFallbackCondition(Condition condition) {
    Optional<String> equivalentMetric = StandardToMQRMetrics.getEquivalentMetric(condition.getMetricKey());
    if (StandardToMQRMetrics.isMQRMetric(condition.getMetricKey()) && equivalentMetric.isPresent()) {
      return Optional.of(new Condition(equivalentMetric.get(), condition.getOperator(), condition.getErrorThreshold()));
    } else {
      return Optional.empty();
    }
  }
}
