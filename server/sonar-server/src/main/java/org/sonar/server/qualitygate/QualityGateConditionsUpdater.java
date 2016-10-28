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
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.ObjectUtils;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Metric.ValueType;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.metric.MetricDto;
import org.sonar.db.qualitygate.QualityGateConditionDto;
import org.sonar.db.qualitygate.QualityGateDto;
import org.sonar.server.computation.task.projectanalysis.qualitymodel.RatingGrid.Rating;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.Errors;
import org.sonar.server.exceptions.Message;
import org.sonar.server.exceptions.NotFoundException;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.lang.Integer.parseInt;
import static java.lang.String.format;
import static java.util.Arrays.stream;
import static org.sonar.api.measures.Metric.ValueType.RATING;
import static org.sonar.api.measures.Metric.ValueType.valueOf;
import static org.sonar.db.qualitygate.QualityGateConditionDto.isOperatorAllowed;
import static org.sonar.server.computation.task.projectanalysis.qualitymodel.RatingGrid.Rating.E;
import static org.sonar.server.qualitygate.ValidRatingMetrics.isCoreRatingMetric;

public class QualityGateConditionsUpdater {

  private static final List<String> RATING_VALID_INT_VALUES = stream(Rating.values()).map(r -> Integer.toString(r.getIndex())).collect(Collectors.toList());

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

  public QualityGateConditionDto updateCondition(DbSession dbSession, long condId, String metricKey, String operator,
    @Nullable String warningThreshold, @Nullable String errorThreshold, @Nullable Integer period) {
    QualityGateConditionDto condition = getNonNullCondition(dbSession, condId);
    MetricDto metric = getNonNullMetric(dbSession, metricKey);
    validateCondition(metric, operator, warningThreshold, errorThreshold, period);
    checkConditionDoesNotAlreadyExistOnSameMetricAndPeriod(getConditions(dbSession, condition.getQualityGateId(), condition.getId()), metric, period);

    condition
      .setMetricId(metric.getId())
      .setMetricKey(metric.getKey())
      .setOperator(operator)
      .setWarningThreshold(warningThreshold)
      .setErrorThreshold(errorThreshold)
      .setPeriod(period);
    dbClient.gateConditionDao().update(condition, dbSession);
    return condition;
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

  private QualityGateConditionDto getNonNullCondition(DbSession dbSession, long id) {
    QualityGateConditionDto condition = dbClient.gateConditionDao().selectById(id, dbSession);
    if (condition == null) {
      throw new NotFoundException("There is no condition with id=" + id);
    }
    return condition;
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
    checkRatingMetric(metric, warningThreshold, errorThreshold, period, errors);
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
    return !metric.isDataType() && !CoreMetrics.ALERT_STATUS_KEY.equals(metric.getKey());
  }

  private static void checkOperator(MetricDto metric, String operator, Errors errors) {
    ValueType valueType = valueOf(metric.getValueType());
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

  private static void checkRatingMetric(MetricDto metric, @Nullable String warningThreshold, @Nullable String errorThreshold, @Nullable Integer period, Errors errors) {
    if (!metric.getValueType().equals(RATING.name())) {
      return;
    }
    if (!isCoreRatingMetric(metric.getKey())) {
      errors.add(Message.of(format("The metric '%s' cannot be used", metric.getShortName())));
    }
    if (period != null && !metric.getKey().startsWith("new_")) {
      errors.add(Message.of(format("The metric '%s' cannot be used on the leak period", metric.getShortName())));
    }
    if (!isValidRating(warningThreshold)) {
      addInvalidRatingError(warningThreshold, errors);
      return;
    }
    if (!isValidRating(errorThreshold)) {
      addInvalidRatingError(errorThreshold, errors);
      return;
    }
    checkRatingGreaterThanOperator(warningThreshold, errors);
    checkRatingGreaterThanOperator(errorThreshold, errors);
  }

  private static void addInvalidRatingError(@Nullable String value, Errors errors) {
    errors.add(Message.of(format("'%s' is not a valid rating", value)));
  }

  private static void checkRatingGreaterThanOperator(@Nullable String value, Errors errors) {
    errors.check(isNullOrEmpty(value) || !Objects.equals(toRating(value), E), format("There's no worse rating than E (%s)", value));
  }

  private static Rating toRating(String value) {
    return Rating.valueOf(parseInt(value));
  }

  private static boolean isValidRating(@Nullable String value) {
    return isNullOrEmpty(value) || RATING_VALID_INT_VALUES.contains(value);
  }
}
