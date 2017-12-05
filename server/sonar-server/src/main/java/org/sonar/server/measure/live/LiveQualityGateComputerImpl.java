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
package org.sonar.server.measure.live;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.Set;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Metric;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.BranchType;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.measure.LiveMeasureDto;
import org.sonar.db.metric.MetricDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.qualitygate.QualityGateConditionDto;
import org.sonar.db.qualitygate.QualityGateDto;
import org.sonar.server.qualitygate.Condition;
import org.sonar.server.qualitygate.EvaluatedQualityGate;
import org.sonar.server.qualitygate.QualityGate;
import org.sonar.server.qualitygate.QualityGateConverter;
import org.sonar.server.qualitygate.QualityGateEvaluator;
import org.sonar.server.qualitygate.QualityGateFinder;
import org.sonar.server.qualitygate.ShortLivingBranchQualityGate;

import static org.sonar.core.util.stream.MoreCollectors.toHashSet;
import static org.sonar.core.util.stream.MoreCollectors.uniqueIndex;

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
  public QualityGate loadQualityGate(DbSession dbSession, OrganizationDto organization, ComponentDto project, BranchDto branch) {
    if (branch.getBranchType() == BranchType.SHORT) {
      return ShortLivingBranchQualityGate.GATE;
    }

    ComponentDto mainProject = project.getMainBranchProjectUuid() == null ? project : dbClient.componentDao().selectOrFailByKey(dbSession, project.getKey());
    QualityGateDto gateDto = qGateFinder.getQualityGate(dbSession, organization, mainProject).getQualityGate();
    Collection<QualityGateConditionDto> conditionDtos = dbClient.gateConditionDao().selectForQualityGate(dbSession, gateDto.getId());
    Set<Integer> metricIds = conditionDtos.stream().map(c -> (int) c.getMetricId())
      .collect(toHashSet(conditionDtos.size()));
    Map<Integer, MetricDto> metricsById = dbClient.metricDao().selectByIds(dbSession, metricIds).stream()
      .collect(uniqueIndex(MetricDto::getId));

    Set<Condition> conditions = conditionDtos.stream().map(conditionDto -> {
      String metricKey = metricsById.get((int) conditionDto.getMetricId()).getKey();
      Condition.Operator operator = Condition.Operator.fromDbValue(conditionDto.getOperator());
      boolean onLeak = Objects.equals(conditionDto.getPeriod(), 1);
      return new Condition(metricKey, operator, conditionDto.getErrorThreshold(), conditionDto.getWarningThreshold(), onLeak);
    }).collect(toHashSet(conditionDtos.size()));

    return new QualityGate(String.valueOf(gateDto.getId()), gateDto.getName(), conditions);
  }

  @Override
  public EvaluatedQualityGate refreshGateStatus(ComponentDto project, QualityGate gate, MeasureMatrix measureMatrix) {
    QualityGateEvaluator.Measures measures = metricKey -> {
      Optional<LiveMeasureDto> liveMeasureDto = measureMatrix.getMeasure(project, metricKey);
      if (!liveMeasureDto.isPresent()) {
        return Optional.empty();
      }
      MetricDto metric = measureMatrix.getMetric(liveMeasureDto.get().getMetricId());
      return Optional.of(new LiveMeasure(liveMeasureDto.get(), metric));
    };

    EvaluatedQualityGate evaluatedGate = evaluator.evaluate(gate, measures);

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
    private final LiveMeasureDto dto;
    private final MetricDto metric;

    LiveMeasure(LiveMeasureDto dto, MetricDto metric) {
      this.dto = dto;
      this.metric = metric;
    }

    @Override
    public Metric.ValueType getType() {
      return Metric.ValueType.valueOf(metric.getValueType());
    }

    @Override
    public OptionalDouble getValue() {
      if (dto.getValue() == null) {
        return OptionalDouble.empty();
      }
      return OptionalDouble.of(dto.getValue());
    }

    @Override
    public Optional<String> getStringValue() {
      return Optional.ofNullable(dto.getTextValue());
    }

    @Override
    public OptionalDouble getLeakValue() {
      if (dto.getVariation() == null) {
        return OptionalDouble.empty();
      }
      return OptionalDouble.of(dto.getVariation());
    }
  }
}
