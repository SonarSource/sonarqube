/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
import java.util.Objects;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.ObjectUtils;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Metric;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.metric.MetricDto;
import org.sonar.db.qualitygate.QualityGateConditionDto;
import org.sonar.db.qualitygate.QualityGateDto;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.Errors;
import org.sonar.server.exceptions.NotFoundException;

import static java.lang.String.format;
import static org.sonar.db.qualitygate.QualityGateConditionDto.isOperatorAllowed;

public class QualityGateConditionsUpdater {

  private final DbClient dbClient;

  public QualityGateConditionsUpdater(DbClient dbClient) {
    this.dbClient = dbClient;
  }

  public QualityGateConditionDto createCondition(DbSession dbSession, long qGateId, String metricKey, String operator,
    @Nullable String warningThreshold, @Nullable String errorThreshold, @Nullable Integer period) {
    getNonNullQgate(dbSession, qGateId);
    MetricDto metric = getNonNullMetric(dbSession, metricKey);
    validateCondition(metric, operator, warningThreshold, errorThreshold, period);
    checkConditionDoesNotAlreadyExistOnSameMetricAndPeriod(getConditions(dbSession, qGateId, null), metric, period);

    QualityGateConditionDto newCondition = new QualityGateConditionDto().setQualityGateId(qGateId)
      .setMetricId(metric.getId()).setMetricKey(metric.getKey())
      .setOperator(operator)
      .setWarningThreshold(warningThreshold)
      .setErrorThreshold(errorThreshold)
      .setPeriod(period);
    dbClient.gateConditionDao().insert(newCondition, dbSession);
    return newCondition;
  }

  private QualityGateDto getNonNullQgate(DbSession dbSession, long id) {
    QualityGateDto qGate = dbClient.qualityGateDao().selectById(dbSession, id);
    if (qGate == null) {
      throw new NotFoundException(format("There is no quality gate with id=%s", id));
    }
    return qGate;
  }

  private MetricDto getNonNullMetric(DbSession dbSession, String metricKey) {
    MetricDto metric = dbClient.metricDao().selectByKey(dbSession, metricKey);
    if (metric == null) {
      throw new NotFoundException(format("There is no metric with key=%s", metricKey));
    }
    return metric;
  }

  private Collection<QualityGateConditionDto> getConditions(DbSession dbSession, long qGateId, @Nullable Long conditionId) {
    Collection<QualityGateConditionDto> conditions = dbClient.gateConditionDao().selectForQualityGate(qGateId, dbSession);
    if (conditionId == null) {
      return conditions;
    }
    return dbClient.gateConditionDao().selectForQualityGate(qGateId, dbSession).stream()
      .filter(condition -> condition.getId() != conditionId)
      .collect(Collectors.toList());
  }

  private static void validateCondition(MetricDto metric, String operator, @Nullable String warningThreshold, @Nullable String errorThreshold, @Nullable Integer period) {
    Errors errors = new Errors();
    validateMetric(metric, errors);
    checkOperator(metric, operator, errors);
    checkThresholds(warningThreshold, errorThreshold, errors);
    checkPeriod(metric, period, errors);
    if (!errors.isEmpty()) {
      throw new BadRequestException(errors);
    }
  }

  private static void validateMetric(MetricDto metric, Errors errors) {
    errors.check(isAlertable(metric), format("Metric '%s' cannot be used to define a condition.", metric.getKey()));
  }

  private static boolean isAlertable(MetricDto metric) {
    return isAvailableForInit(metric) && BooleanUtils.isFalse(metric.isHidden());
  }

  private static boolean isAvailableForInit(MetricDto metric) {
    return !metric.isDataType() && !CoreMetrics.ALERT_STATUS_KEY.equals(metric.getKey()) && !Objects.equals(Metric.ValueType.RATING.name(), metric.getValueType());
  }

  private static void checkOperator(MetricDto metric, String operator, Errors errors) {
    Metric.ValueType valueType = Metric.ValueType.valueOf(metric.getValueType());
    errors.check(isOperatorAllowed(operator, valueType), format("Operator %s is not allowed for metric type %s.", operator, metric.getValueType()));
  }

  private static void checkThresholds(@Nullable String warningThreshold, @Nullable String errorThreshold, Errors errors) {
    errors.check(warningThreshold != null || errorThreshold != null, "At least one threshold (warning, error) must be set.");
  }

  private static void checkPeriod(MetricDto metric, @Nullable Integer period, Errors errors) {
    if (period == null) {
      errors.check(!metric.getKey().startsWith("new_"), "A period must be selected for differential metrics.");
    } else {
      errors.check(period == 1, "The only valid quality gate period is 1, the leak period.");
    }
  }

  private static void checkConditionDoesNotAlreadyExistOnSameMetricAndPeriod(Collection<QualityGateConditionDto> conditions, MetricDto metric, @Nullable final Integer period) {
    if (conditions.isEmpty()) {
      return;
    }

    boolean conditionExists = conditions.stream().anyMatch(c -> c.getMetricId() == metric.getId() && ObjectUtils.equals(c.getPeriod(), period));
    if (conditionExists) {
      String errorMessage = period == null
        ? format("Condition on metric '%s' already exists.", metric.getShortName())
        : format("Condition on metric '%s' over leak period already exists.", metric.getShortName());
      throw new BadRequestException(errorMessage);
    }
  }

}
