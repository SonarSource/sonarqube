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
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.sonar.api.measures.Metric;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.metric.MetricDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.qualitygate.QualityGateConditionDto;

import static java.util.stream.Collectors.toMap;
import static org.sonar.api.measures.CoreMetrics.NEW_COVERAGE;
import static org.sonar.api.measures.CoreMetrics.NEW_COVERAGE_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_DUPLICATED_LINES_DENSITY;
import static org.sonar.api.measures.CoreMetrics.NEW_DUPLICATED_LINES_DENSITY_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_MAINTAINABILITY_RATING;
import static org.sonar.api.measures.CoreMetrics.NEW_RELIABILITY_RATING;
import static org.sonar.api.measures.CoreMetrics.NEW_SECURITY_HOTSPOTS_REVIEWED;
import static org.sonar.api.measures.CoreMetrics.NEW_SECURITY_RATING;
import static org.sonar.api.measures.CoreMetrics.NEW_VIOLATIONS;
import static org.sonar.server.qualitygate.QualityGateCaycStatus.COMPLIANT;
import static org.sonar.server.qualitygate.QualityGateCaycStatus.NON_COMPLIANT;
import static org.sonar.server.qualitygate.QualityGateCaycStatus.OVER_COMPLIANT;

public class QualityGateCaycChecker {

  static final Map<String, Double> BEST_VALUE_REQUIREMENTS = Stream.of(
    NEW_VIOLATIONS,
    NEW_SECURITY_HOTSPOTS_REVIEWED).collect(toMap(Metric::getKey, Metric::getBestValue));

  static final Set<String> EXISTENCY_REQUIREMENTS = Set.of(
    NEW_DUPLICATED_LINES_DENSITY_KEY,
    NEW_COVERAGE_KEY);
  public static final Set<Metric<? extends Serializable>> CAYC_METRICS = Set.of(
    NEW_VIOLATIONS,
    NEW_SECURITY_HOTSPOTS_REVIEWED,
    NEW_DUPLICATED_LINES_DENSITY,
    NEW_COVERAGE);

  // To be removed after transition
  static final Map<String, Double> LEGACY_BEST_VALUE_REQUIREMENTS = Stream.of(
    NEW_MAINTAINABILITY_RATING,
    NEW_RELIABILITY_RATING,
    NEW_SECURITY_HOTSPOTS_REVIEWED,
    NEW_SECURITY_RATING).collect(toMap(Metric::getKey, Metric::getBestValue));
  static final Set<Metric<? extends Serializable>> LEGACY_CAYC_METRICS = Set.of(
    NEW_MAINTAINABILITY_RATING,
    NEW_RELIABILITY_RATING,
    NEW_SECURITY_HOTSPOTS_REVIEWED,
    NEW_SECURITY_RATING,
    NEW_DUPLICATED_LINES_DENSITY,
    NEW_COVERAGE);

  private final DbClient dbClient;

  public QualityGateCaycChecker(DbClient dbClient) {
    this.dbClient = dbClient;
  }

  public QualityGateCaycStatus checkCaycCompliant(DbSession dbSession, String qualityGateUuid) {
    Collection<QualityGateConditionDto> conditions = dbClient.gateConditionDao().selectForQualityGate(dbSession, qualityGateUuid);
    var metrics = dbClient.metricDao().selectByUuids(dbSession, conditions.stream().map(QualityGateConditionDto::getMetricUuid).collect(Collectors.toSet()))
      .stream()
      .filter(MetricDto::isEnabled)
      .toList();

    return checkCaycCompliant(conditions, metrics);
  }

  public QualityGateCaycStatus checkCaycCompliant(Collection<QualityGateConditionDto> conditions, List<MetricDto> metrics) {
    var conditionsByMetricId = conditions
      .stream()
      .collect(Collectors.toMap(QualityGateConditionDto::getMetricUuid, Function.identity()));

    if (conditionsByMetricId.size() < CAYC_METRICS.size()) {
      return NON_COMPLIANT;
    }

    var caycStatus = checkCaycConditions(metrics, conditionsByMetricId, false);
    if (caycStatus == NON_COMPLIANT) {
      caycStatus = checkCaycConditions(metrics, conditionsByMetricId, true);
    }
    return caycStatus;
  }

  private static QualityGateCaycStatus checkCaycConditions(List<MetricDto> metrics, Map<String, QualityGateConditionDto> conditionsByMetricId, boolean legacy) {
    var caycMetrics = legacy ? LEGACY_CAYC_METRICS : CAYC_METRICS;

    long count = metrics.stream()
      .filter(metric -> checkMetricCaycCompliant(conditionsByMetricId.get(metric.getUuid()), metric, legacy))
      .count();
    if (metrics.size() == count && count == caycMetrics.size()) {
      return COMPLIANT;
    } else if (metrics.size() > count && count == caycMetrics.size()) {
      return OVER_COMPLIANT;
    }
    return NON_COMPLIANT;
  }

  public QualityGateCaycStatus checkCaycCompliantFromProject(DbSession dbSession, OrganizationDto organization, String projectUuid) {
    return Optional.ofNullable(dbClient.qualityGateDao().selectByProjectUuid(dbSession, projectUuid))
      .or(() -> Optional.ofNullable(dbClient.qualityGateDao().selectDefault(dbSession, organization)))
      .map(qualityGate -> checkCaycCompliant(dbSession, qualityGate.getUuid()))
      .orElse(NON_COMPLIANT);
  }

  public boolean isCaycCondition(MetricDto metric) {
    return Stream.concat(CAYC_METRICS.stream(), LEGACY_CAYC_METRICS.stream())
      .map(Metric::getKey).anyMatch(metric.getKey()::equals);
  }

  private static boolean checkMetricCaycCompliant(QualityGateConditionDto condition, MetricDto metric, boolean legacy) {
    var bestValueRequirements = legacy ? LEGACY_BEST_VALUE_REQUIREMENTS : BEST_VALUE_REQUIREMENTS;
    if (EXISTENCY_REQUIREMENTS.contains(metric.getKey())) {
      return true;
    } else if (bestValueRequirements.containsKey(metric.getKey())) {
      Double errorThreshold = Double.valueOf(condition.getErrorThreshold());
      Double caycRequiredThreshold = bestValueRequirements.get(metric.getKey());
      return caycRequiredThreshold.compareTo(errorThreshold) == 0;
    } else {
      return false;
    }
  }

}
