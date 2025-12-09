/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.server.measure.live;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.sonar.api.config.Configuration;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Metric;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.BranchType;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.metric.MetricDto;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.qualitygate.QualityGateConditionDto;
import org.sonar.server.qualitygate.Condition;
import org.sonar.server.qualitygate.EvaluatedQualityGate;
import org.sonar.server.qualitygate.QualityGate;
import org.sonar.server.qualitygate.QualityGateConverter;
import org.sonar.server.qualitygate.QualityGateEvaluator;
import org.sonar.server.qualitygate.QualityGateFinder;
import org.sonar.server.qualitygate.QualityGateFinder.QualityGateData;

public class LiveQualityGateComputerImpl implements LiveQualityGateComputer {

  private final DbClient dbClient;
  private final QualityGateFinder qGateFinder;
  private final QualityGateEvaluator evaluator;

  public LiveQualityGateComputerImpl(DbClient dbClient, QualityGateFinder qGateFinder, QualityGateEvaluator evaluator) {
    this.dbClient = dbClient;
    this.qGateFinder = qGateFinder;
    this.evaluator = evaluator;
  }

  @Override
  public QualityGate loadQualityGate(DbSession dbSession, ProjectDto project, BranchDto branch) {
    QualityGateData qg = qGateFinder.getEffectiveQualityGate(dbSession, project);
    Collection<QualityGateConditionDto> conditionDtos = dbClient.gateConditionDao().selectForQualityGate(dbSession, qg.getUuid());
    Set<String> metricUuids = conditionDtos.stream().map(QualityGateConditionDto::getMetricUuid).collect(Collectors.toSet());
    Map<String, MetricDto> metricsByUuid = dbClient.metricDao().selectByUuids(dbSession, metricUuids).stream().collect(Collectors.toMap(MetricDto::getUuid, Function.identity()));

    Stream<Condition> conditions = conditionDtos.stream().map(conditionDto -> {
      String metricKey = metricsByUuid.get(conditionDto.getMetricUuid()).getKey();
      Condition.Operator operator = Condition.Operator.fromDbValue(conditionDto.getOperator());
      return new Condition(metricKey, operator, conditionDto.getErrorThreshold());
    });

    if (branch.getBranchType() == BranchType.PULL_REQUEST) {
      conditions = conditions.filter(Condition::isOnLeakPeriod);
    }

    return new QualityGate(String.valueOf(qg.getUuid()), qg.getName(), conditions.collect(Collectors.toSet()));
  }

  @Override
  public EvaluatedQualityGate refreshGateStatus(ComponentDto project, QualityGate gate, MeasureMatrix measureMatrix, Configuration configuration) {
    QualityGateEvaluator.Measures measures = metricKey -> {
      Optional<MeasureMatrix.Measure> measure = measureMatrix.getMeasure(project, metricKey);
      if (measure.isEmpty()) {
        return Optional.empty();
      }
      MetricDto metric = measureMatrix.getMetric(measure.get().getMetricKey());
      return Optional.of(new LiveMeasure(measure.get(), metric));
    };

    EvaluatedQualityGate evaluatedGate = evaluator.evaluate(gate, measures, configuration);

    measureMatrix.setValue(project, CoreMetrics.ALERT_STATUS_KEY, evaluatedGate.getStatus().name());
    measureMatrix.setValue(project, CoreMetrics.QUALITY_GATE_DETAILS_KEY, QualityGateConverter.toJson(evaluatedGate));

    return evaluatedGate;
  }

  @Override
  public Set<String> getMetricsRelatedTo(QualityGate gate) {
    Set<String> metricKeys = new HashSet<>();
    metricKeys.add(CoreMetrics.ALERT_STATUS_KEY);
    metricKeys.add(CoreMetrics.QUALITY_GATE_DETAILS_KEY);
    metricKeys.addAll(evaluator.getMetricKeys(gate));
    return metricKeys;
  }

  private static class LiveMeasure implements QualityGateEvaluator.Measure {
    private final MeasureMatrix.Measure measure;
    private final MetricDto metric;

    LiveMeasure(MeasureMatrix.Measure measure, MetricDto metric) {
      this.measure = measure;
      this.metric = metric;
    }

    @Override
    public Metric.ValueType getType() {
      return Metric.ValueType.valueOf(metric.getValueType());
    }

    @Override
    public OptionalDouble getValue() {
      Double doubleValue = measure.doubleValue();
      if (doubleValue == null) {
        return OptionalDouble.empty();
      }
      return OptionalDouble.of(doubleValue);
    }

    @Override
    public Optional<String> getStringValue() {
      return Optional.ofNullable(measure.stringValue());
    }
  }
}
