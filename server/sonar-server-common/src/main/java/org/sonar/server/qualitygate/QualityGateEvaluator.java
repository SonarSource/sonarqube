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
package org.sonar.server.qualitygate;

import java.util.Optional;
import java.util.OptionalDouble;
import java.util.Set;
import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.api.measures.Metric;
import org.sonar.api.server.ServerSide;

@ComputeEngineSide
@ServerSide
public interface QualityGateEvaluator {

  /**
   * @param measures must provide the measures related to the metrics
   *                 defined by {@link #getMetricKeys(QualityGate)}
   */
  EvaluatedQualityGate evaluate(QualityGate gate, Measures measures);

  /**
   * Keys of the metrics involved in the computation of gate status.
   * It may include metrics that are not part of conditions,
   * for instance "new_lines" for the circuit-breaker on
   * small changesets.
   */
  Set<String> getMetricKeys(QualityGate gate);

  interface Measures {
    Optional<Measure> get(String metricKey);
  }

  interface Measure {
    Metric.ValueType getType();

    OptionalDouble getValue();

    Optional<String> getStringValue();

    OptionalDouble getLeakValue();
  }
}
