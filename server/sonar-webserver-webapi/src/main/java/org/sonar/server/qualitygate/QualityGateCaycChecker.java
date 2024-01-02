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

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.sonar.api.measures.Metric;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.metric.MetricDto;
import org.sonar.db.qualitygate.QualityGateConditionDto;

import static java.util.stream.Collectors.toUnmodifiableMap;
import static org.sonar.api.measures.CoreMetrics.NEW_COVERAGE;
import static org.sonar.api.measures.CoreMetrics.NEW_COVERAGE_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_DUPLICATED_LINES_DENSITY;
import static org.sonar.api.measures.CoreMetrics.NEW_DUPLICATED_LINES_DENSITY_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_MAINTAINABILITY_RATING;
import static org.sonar.api.measures.CoreMetrics.NEW_RELIABILITY_RATING;
import static org.sonar.api.measures.CoreMetrics.NEW_SECURITY_HOTSPOTS_REVIEWED;
import static org.sonar.api.measures.CoreMetrics.NEW_SECURITY_RATING;
import static org.sonar.core.util.stream.MoreCollectors.uniqueIndex;
import static org.sonar.server.qualitygate.QualityGateCaycStatus.COMPLIANT;
import static org.sonar.server.qualitygate.QualityGateCaycStatus.NON_COMPLIANT;
import static org.sonar.server.qualitygate.QualityGateCaycStatus.OVER_COMPLIANT;

public class QualityGateCaycChecker {

  public static final List<Metric<? extends Serializable>> CAYC_METRICS = List.of(
    NEW_MAINTAINABILITY_RATING,
    NEW_RELIABILITY_RATING,
    NEW_SECURITY_HOTSPOTS_REVIEWED,
    NEW_SECURITY_RATING,
    NEW_DUPLICATED_LINES_DENSITY,
    NEW_COVERAGE
  );

  private static final Set<String> EXISTENCY_REQUIREMENTS = Set.of(
    NEW_DUPLICATED_LINES_DENSITY_KEY,
    NEW_COVERAGE_KEY
  );

  private static final Map<String, Double> BEST_VALUE_REQUIREMENTS = CAYC_METRICS.stream()
    .filter(metric -> !EXISTENCY_REQUIREMENTS.contains(metric.getKey()))
    .collect(toUnmodifiableMap(Metric::getKey, Metric::getBestValue));

  private final DbClient dbClient;

  public QualityGateCaycChecker(DbClient dbClient) {
    this.dbClient = dbClient;
  }

  public QualityGateCaycStatus checkCaycCompliant(DbSession dbSession, String qualityGateUuid) {
    var conditionsByMetricId = dbClient.gateConditionDao().selectForQualityGate(dbSession, qualityGateUuid)
      .stream()
      .collect(uniqueIndex(QualityGateConditionDto::getMetricUuid));

    if (conditionsByMetricId.size() < CAYC_METRICS.size()) {
      return NON_COMPLIANT;
    }

    var metrics = dbClient.metricDao().selectByUuids(dbSession, conditionsByMetricId.keySet())
      .stream()
      .filter(MetricDto::isEnabled)
      .toList();

    long count = metrics.stream()
      .filter(metric -> checkMetricCaycCompliant(conditionsByMetricId.get(metric.getUuid()), metric))
      .count();

    if (metrics.size() == count && count == CAYC_METRICS.size()) {
      return COMPLIANT;
    } else if (metrics.size() > count && count == CAYC_METRICS.size()) {
      return OVER_COMPLIANT;
    }

    return NON_COMPLIANT;
  }

  public QualityGateCaycStatus checkCaycCompliantFromProject(DbSession dbSession, String projectUuid) {
    return Optional.ofNullable(dbClient.qualityGateDao().selectByProjectUuid(dbSession, projectUuid))
      .or(() -> Optional.ofNullable(dbClient.qualityGateDao().selectDefault(dbSession)))
      .map(qualityGate -> checkCaycCompliant(dbSession, qualityGate.getUuid()))
      .orElse(NON_COMPLIANT);
  }

  private static boolean checkMetricCaycCompliant(QualityGateConditionDto condition, MetricDto metric) {
    if (EXISTENCY_REQUIREMENTS.contains(metric.getKey())) {
      return true;
    } else if (BEST_VALUE_REQUIREMENTS.containsKey(metric.getKey())) {
      Double errorThreshold = Double.valueOf(condition.getErrorThreshold());
      Double caycRequiredThreshold = BEST_VALUE_REQUIREMENTS.get(metric.getKey());
      return caycRequiredThreshold.compareTo(errorThreshold) == 0;
    } else {
      return false;
    }
  }

}
