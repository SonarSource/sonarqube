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
package org.sonar.server.measure.live;

import com.google.common.collect.ImmutableSet;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.config.Configuration;
import org.sonar.api.config.internal.ConfigurationBridge;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Metric;
import org.sonar.db.DbTester;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.BranchType;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.measure.MeasureDto;
import org.sonar.db.metric.MetricDto;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.qualitygate.QualityGateConditionDto;
import org.sonar.db.qualitygate.QualityGateDto;
import org.sonar.server.qualitygate.Condition;
import org.sonar.server.qualitygate.EvaluatedCondition;
import org.sonar.server.qualitygate.EvaluatedQualityGate;
import org.sonar.server.qualitygate.QualityGate;
import org.sonar.server.qualitygate.QualityGateEvaluator;
import org.sonar.server.qualitygate.QualityGateFinder;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.sonar.db.measure.MeasureTesting.newMeasure;
import static org.sonar.db.metric.MetricTesting.newMetricDto;

public class LiveQualityGateComputerImplIT {
  @Rule
  public DbTester db = DbTester.create();

  private final MapSettings settings = new MapSettings();
  private final Configuration configuration = new ConfigurationBridge(settings);
  private final TestQualityGateEvaluator qualityGateEvaluator = new TestQualityGateEvaluator();
  private final LiveQualityGateComputerImpl underTest = new LiveQualityGateComputerImpl(db.getDbClient(), new QualityGateFinder(db.getDbClient()), qualityGateEvaluator);

  @Test
  public void loadQualityGate_returns_hardcoded_gate_for_pull_requests() {
    ProjectDto project = db.components().insertPublicProject().getProjectDto();
    BranchDto branch = db.components().insertProjectBranch(project, b -> b.setBranchType(BranchType.PULL_REQUEST));
    MetricDto metric1 = db.measures().insertMetric(m -> m.setKey("new_metric"));
    MetricDto metric2 = db.measures().insertMetric(m -> m.setKey("metric"));

    QualityGateDto gate = db.qualityGates().insertQualityGate();
    db.qualityGates().setDefaultQualityGate(gate);

    db.qualityGates().addCondition(gate, metric1);
    db.qualityGates().addCondition(gate, metric2);

    QualityGate result = underTest.loadQualityGate(db.getSession(), project, branch);
    assertThat(result.getConditions()).extracting(Condition::getMetricKey).containsExactly("new_metric");
  }

  @Test
  public void loadQualityGate_on_branch_returns_default_gate() {
    ProjectDto project = db.components().insertPublicProject().getProjectDto();
    BranchDto branch = db.components().insertProjectBranch(project).setBranchType(BranchType.BRANCH);

    MetricDto metric = db.measures().insertMetric();
    QualityGateDto gate = db.qualityGates().insertQualityGate();
    db.qualityGates().setDefaultQualityGate(gate);
    QualityGateConditionDto condition = db.qualityGates().addCondition(gate, metric);

    QualityGate result = underTest.loadQualityGate(db.getSession(), project, branch);

    assertThat(result.getId()).isEqualTo("" + gate.getUuid());
    assertThat(result.getConditions())
      .extracting(Condition::getMetricKey, Condition::getOperator, Condition::getErrorThreshold)
      .containsExactlyInAnyOrder(
        tuple(metric.getKey(), Condition.Operator.fromDbValue(condition.getOperator()), condition.getErrorThreshold()));
  }

  @Test
  public void getMetricsRelatedTo() {
    Condition condition = new Condition("metric1", Condition.Operator.GREATER_THAN, "10");
    QualityGate gate = new QualityGate("1", "foo", ImmutableSet.of(condition));

    Set<String> result = underTest.getMetricsRelatedTo(gate);

    assertThat(result).containsExactlyInAnyOrder(
      // the metrics needed to compute the status of gate
      condition.getMetricKey(),
      // generated metrics
      CoreMetrics.ALERT_STATUS_KEY, CoreMetrics.QUALITY_GATE_DETAILS_KEY);
  }

  @Test
  public void refreshGateStatus_generates_gate_related_measures() {
    ComponentDto project = ComponentTesting.newPublicProjectDto();
    MetricDto conditionMetric = newMetricDto();
    MetricDto statusMetric = newMetricDto().setKey(CoreMetrics.ALERT_STATUS_KEY).setValueType(Metric.ValueType.STRING.name());
    MetricDto detailsMetric = newMetricDto().setKey(CoreMetrics.QUALITY_GATE_DETAILS_KEY).setValueType(Metric.ValueType.STRING.name());
    Condition condition = new Condition(conditionMetric.getKey(), Condition.Operator.GREATER_THAN, "10");
    QualityGate gate = new QualityGate("1", "foo", ImmutableSet.of(condition));
    MeasureMatrix matrix = new MeasureMatrix(singleton(project), asList(conditionMetric, statusMetric, detailsMetric), emptyList());

    EvaluatedQualityGate result = underTest.refreshGateStatus(project, gate, matrix, configuration);

    QualityGateEvaluator.Measures measures = qualityGateEvaluator.getCalledMeasures();
    assertThat(measures.get(conditionMetric.getKey())).isEmpty();

    assertThat(result.getStatus()).isEqualTo(Metric.Level.OK);
    assertThat(result.getEvaluatedConditions())
      .extracting(EvaluatedCondition::getStatus)
      .containsExactly(EvaluatedCondition.EvaluationStatus.OK);
    assertThat(matrix.getMeasure(project, CoreMetrics.ALERT_STATUS_KEY).get().stringValue()).isEqualTo(Metric.Level.OK.name());
    assertThat(matrix.getMeasure(project, CoreMetrics.QUALITY_GATE_DETAILS_KEY).get().stringValue())
      .isNotEmpty()
      // json format
      .startsWith("{").endsWith("}");
  }

  @Test
  public void refreshGateStatus_provides_measures_to_evaluator() {
    ComponentDto project = ComponentTesting.newPublicProjectDto();
    MetricDto numericMetric = newMetricDto().setValueType(Metric.ValueType.FLOAT.name());
    MetricDto numericNewMetric = newMetricDto().setValueType(Metric.ValueType.FLOAT.name()).setKey("new_metric");
    MetricDto stringMetric = newMetricDto().setValueType(Metric.ValueType.STRING.name());
    MetricDto statusMetric = newMetricDto().setKey(CoreMetrics.ALERT_STATUS_KEY);
    MetricDto detailsMetric = newMetricDto().setKey(CoreMetrics.QUALITY_GATE_DETAILS_KEY);
    QualityGate gate = new QualityGate("1", "foo", Collections.emptySet());
    MeasureDto numericMeasure = newMeasure(project, numericMetric, 1.23);
    MeasureDto numericNewMeasure = newMeasure(project, numericNewMetric, 8.9);
    MeasureDto stringMeasure = newMeasure(project, stringMetric, "bar");
    MeasureMatrix matrix = new MeasureMatrix(singleton(project), asList(statusMetric, detailsMetric, numericMetric, numericNewMetric, stringMetric),
      asList(numericMeasure, numericNewMeasure, stringMeasure));

    underTest.refreshGateStatus(project, gate, matrix, configuration);

    QualityGateEvaluator.Measures measures = qualityGateEvaluator.getCalledMeasures();

    QualityGateEvaluator.Measure loadedStringMeasure = measures.get(stringMetric.getKey()).get();
    assertThat(loadedStringMeasure.getStringValue()).hasValue("bar");
    assertThat(loadedStringMeasure.getValue()).isEmpty();
    assertThat(loadedStringMeasure.getType()).isEqualTo(Metric.ValueType.STRING);

    QualityGateEvaluator.Measure loadedNumericMeasure = measures.get(numericMetric.getKey()).get();
    assertThat(loadedNumericMeasure.getStringValue()).isEmpty();
    assertThat(loadedNumericMeasure.getValue()).hasValue(1.23);
    assertThat(loadedNumericMeasure.getType()).isEqualTo(Metric.ValueType.FLOAT);

    QualityGateEvaluator.Measure loadedNumericNewMeasure = measures.get(numericNewMetric.getKey()).get();
    assertThat(loadedNumericNewMeasure.getStringValue()).isEmpty();
    assertThat(loadedNumericNewMeasure.getValue()).hasValue(8.9);
    assertThat(loadedNumericNewMeasure.getType()).isEqualTo(Metric.ValueType.FLOAT);
  }

  private static class TestQualityGateEvaluator implements QualityGateEvaluator {
    private Measures measures;

    @Override
    public EvaluatedQualityGate evaluate(QualityGate gate, Measures measures, Configuration configuration) {
      checkState(this.measures == null);
      this.measures = measures;
      EvaluatedQualityGate.Builder builder = EvaluatedQualityGate.newBuilder().setQualityGate(gate).setStatus(Metric.Level.OK);
      for (Condition condition : gate.getConditions()) {
        builder.addEvaluatedCondition(condition, EvaluatedCondition.EvaluationStatus.OK, "bar");
      }
      return builder.build();
    }

    private Measures getCalledMeasures() {
      return measures;
    }

    @Override
    public Set<String> getMetricKeys(QualityGate gate) {
      return gate.getConditions().stream().map(Condition::getMetricKey).collect(Collectors.toSet());
    }
  }
}
