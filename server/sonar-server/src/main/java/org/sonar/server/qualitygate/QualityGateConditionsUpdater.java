/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Metric.ValueType;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.metric.MetricDto;
import org.sonar.db.qualitygate.QualityGateConditionDto;
import org.sonar.db.qualitygate.QualityGateDto;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.measure.Rating;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.lang.Double.parseDouble;
import static java.lang.Integer.parseInt;
import static java.lang.Long.parseLong;
import static java.lang.String.format;
import static java.lang.String.valueOf;
import static java.util.Arrays.stream;
import static java.util.Objects.requireNonNull;
import static org.sonar.api.measures.Metric.DIRECTION_BETTER;
import static org.sonar.api.measures.Metric.DIRECTION_NONE;
import static org.sonar.api.measures.Metric.DIRECTION_WORST;
import static org.sonar.api.measures.Metric.ValueType.RATING;
import static org.sonar.server.measure.Rating.E;
import static org.sonar.server.qualitygate.Condition.Operator.GREATER_THAN;
import static org.sonar.server.qualitygate.Condition.Operator.LESS_THAN;
import static org.sonar.server.qualitygate.ValidRatingMetrics.isCoreRatingMetric;
import static org.sonar.server.ws.WsUtils.checkRequest;

public class QualityGateConditionsUpdater {

  private static final Map<Integer, ImmutableSet<Condition.Operator>> VALID_OPERATORS_BY_DIRECTION = ImmutableMap.<Integer, ImmutableSet<Condition.Operator>>builder()
    .put(DIRECTION_NONE, ImmutableSet.of(GREATER_THAN, LESS_THAN))
    .put(DIRECTION_BETTER, ImmutableSet.of(LESS_THAN))
    .put(DIRECTION_WORST, ImmutableSet.of(GREATER_THAN))
    .build();

  private static final EnumSet<ValueType> VALID_METRIC_TYPES = EnumSet.of(
    ValueType.INT,
    ValueType.FLOAT,
    ValueType.PERCENT,
    ValueType.MILLISEC,
    ValueType.LEVEL,
    ValueType.RATING,
    ValueType.WORK_DUR
  );

  private static final List<String> RATING_VALID_INT_VALUES = stream(Rating.values()).map(r -> Integer.toString(r.getIndex())).collect(Collectors.toList());

  private final DbClient dbClient;

  public QualityGateConditionsUpdater(DbClient dbClient) {
    this.dbClient = dbClient;
  }

  public QualityGateConditionDto createCondition(DbSession dbSession, QualityGateDto qualityGate, String metricKey, String operator,
    String errorThreshold) {
    MetricDto metric = getNonNullMetric(dbSession, metricKey);
    validateCondition(metric, operator, errorThreshold);
    checkConditionDoesNotExistOnSameMetric(getConditions(dbSession, qualityGate.getId()), metric);

    QualityGateConditionDto newCondition = new QualityGateConditionDto().setQualityGateId(qualityGate.getId())
      .setMetricId(metric.getId()).setMetricKey(metric.getKey())
      .setOperator(operator)
      .setErrorThreshold(errorThreshold);
    dbClient.gateConditionDao().insert(newCondition, dbSession);
    return newCondition;
  }

  public QualityGateConditionDto updateCondition(DbSession dbSession, QualityGateConditionDto condition, String metricKey, String operator,
    String errorThreshold) {
    MetricDto metric = getNonNullMetric(dbSession, metricKey);
    validateCondition(metric, operator, errorThreshold);

    condition
      .setMetricId(metric.getId())
      .setMetricKey(metric.getKey())
      .setOperator(operator)
      .setErrorThreshold(errorThreshold);
    dbClient.gateConditionDao().update(condition, dbSession);
    return condition;
  }

  private MetricDto getNonNullMetric(DbSession dbSession, String metricKey) {
    MetricDto metric = dbClient.metricDao().selectByKey(dbSession, metricKey);
    if (metric == null) {
      throw new NotFoundException(format("There is no metric with key=%s", metricKey));
    }
    return metric;
  }

  private Collection<QualityGateConditionDto> getConditions(DbSession dbSession, long qGateId) {
    return dbClient.gateConditionDao().selectForQualityGate(dbSession, qGateId);
  }

  private static void validateCondition(MetricDto metric, String operator, String errorThreshold) {
    List<String> errors = new ArrayList<>();
    validateMetric(metric, errors);
    checkOperator(metric, operator, errors);
    checkErrorThreshold(metric, errorThreshold, errors);
    checkRatingMetric(metric, errorThreshold, errors);
    checkRequest(errors.isEmpty(), errors);
  }

  private static void validateMetric(MetricDto metric, List<String> errors) {
    check(isAlertable(metric), errors, "Metric '%s' cannot be used to define a condition.", metric.getKey());
  }

  private static boolean isAlertable(MetricDto metric) {
    return !metric.isHidden()
      && VALID_METRIC_TYPES.contains(ValueType.valueOf(metric.getValueType()))
      && !CoreMetrics.ALERT_STATUS_KEY.equals(metric.getKey());
  }

  private static void checkOperator(MetricDto metric, String operator, List<String> errors) {
    check(
      Condition.Operator.isValid(operator) && isAllowedOperator(operator, metric),
      errors,
      "Operator %s is not allowed for this metric.", operator
    );
  }

  private static void checkErrorThreshold(MetricDto metric, String errorThreshold, List<String> errors) {
    requireNonNull(errorThreshold, "errorThreshold can not be null");
    validateErrorThresholdValue(metric, errorThreshold, errors);
  }

  private static void checkConditionDoesNotExistOnSameMetric(Collection<QualityGateConditionDto> conditions, MetricDto metric) {
    if (conditions.isEmpty()) {
      return;
    }

    boolean conditionExists = conditions.stream().anyMatch(c -> c.getMetricId() == metric.getId());
    checkRequest(!conditionExists, format("Condition on metric '%s' already exists.", metric.getShortName()));
  }

  private static boolean isAllowedOperator(String operator, MetricDto metric) {
    if (VALID_OPERATORS_BY_DIRECTION.containsKey(metric.getDirection())) {
      return VALID_OPERATORS_BY_DIRECTION.get(metric.getDirection()).contains(Condition.Operator.fromDbValue(operator));
    }

    return false;
  }

  private static void validateErrorThresholdValue(MetricDto metric, String errorThreshold, List<String> errors) {
    try {
      ValueType valueType = ValueType.valueOf(metric.getValueType());
      switch (valueType) {
        case BOOL:
        case INT:
        case RATING:
          parseInt(errorThreshold);
          return;
        case MILLISEC:
        case WORK_DUR:
          parseLong(errorThreshold);
          return;
        case FLOAT:
        case PERCENT:
          parseDouble(errorThreshold);
          return;
        case STRING:
        case LEVEL:
          return;
        default:
          throw new IllegalArgumentException(format("Unsupported value type %s. Cannot convert condition value", valueType));
      }
    } catch (Exception e) {
      errors.add(format("Invalid value '%s' for metric '%s'", errorThreshold, metric.getShortName()));
    }
  }

  private static void checkRatingMetric(MetricDto metric, String errorThreshold, List<String> errors) {
    if (!metric.getValueType().equals(RATING.name())) {
      return;
    }
    if (!isCoreRatingMetric(metric.getKey())) {
      errors.add(format("The metric '%s' cannot be used", metric.getShortName()));
    }
    if (!isValidRating(errorThreshold)) {
      addInvalidRatingError(errorThreshold, errors);
      return;
    }
    checkRatingGreaterThanOperator(errorThreshold, errors);
  }

  private static void addInvalidRatingError(@Nullable String value, List<String> errors) {
    errors.add(format("'%s' is not a valid rating", value));
  }

  private static void checkRatingGreaterThanOperator(@Nullable String value, List<String> errors) {
    check(isNullOrEmpty(value) || !Objects.equals(toRating(value), E), errors, "There's no worse rating than E (%s)", value);
  }

  private static Rating toRating(String value) {
    return Rating.valueOf(parseInt(value));
  }

  private static boolean isValidRating(@Nullable String value) {
    return isNullOrEmpty(value) || RATING_VALID_INT_VALUES.contains(value);
  }

  @SuppressWarnings("unchecked")
  private static boolean check(boolean expression, List<String> errors, String message, String... args) {
    if (!expression) {
      errors.add(format(message, (Object[]) args));
    }
    return expression;
  }
}
