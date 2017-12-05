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

import com.google.common.collect.ImmutableSet;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Metric;
import org.sonar.db.DbTester;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.BranchType;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.measure.LiveMeasureDto;
import org.sonar.db.metric.MetricDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.qualitygate.QGateWithOrgDto;
import org.sonar.db.qualitygate.QualityGateConditionDto;
import org.sonar.server.qualitygate.Condition;
import org.sonar.server.qualitygate.EvaluatedCondition;
import org.sonar.server.qualitygate.EvaluatedQualityGate;
import org.sonar.server.qualitygate.QualityGate;
import org.sonar.server.qualitygate.QualityGateEvaluator;
import org.sonar.server.qualitygate.QualityGateFinder;
import org.sonar.server.qualitygate.ShortLivingBranchQualityGate;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.sonar.db.component.ComponentTesting.newBranchDto;
import static org.sonar.db.metric.MetricTesting.newMetricDto;
import static org.sonar.db.organization.OrganizationTesting.newOrganizationDto;

public class LiveQualityGateComputerImplTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Rule
  public DbTester db = DbTester.create();

  private TestQualityGateEvaluator qualityGateEvaluator = new TestQualityGateEvaluator();
  private LiveQualityGateComputerImpl underTest = new LiveQualityGateComputerImpl(db.getDbClient(), new QualityGateFinder(db.getDbClient()), qualityGateEvaluator);

  @Test
  public void loadQualityGate_returns_hardcoded_gate_for_short_living_branches() {
    OrganizationDto organization = db.organizations().insert();
    ComponentDto project = db.components().insertPublicProject(organization);
    BranchDto branch = newBranchDto(project).setBranchType(BranchType.SHORT);
    db.components().insertProjectBranch(project, branch);

    QualityGate result = underTest.loadQualityGate(db.getSession(), organization, project, branch);

    assertThat(result).isSameAs(ShortLivingBranchQualityGate.GATE);
  }

  @Test
  public void loadQualityGate_on_long_branch_returns_organization_default_gate() {
    OrganizationDto organization = db.organizations().insert();
    ComponentDto project = db.components().insertPublicProject(organization);
    BranchDto branch = newBranchDto(project).setBranchType(BranchType.LONG);
    db.components().insertProjectBranch(project, branch);

    MetricDto metric = db.measures().insertMetric();
    QGateWithOrgDto gate = db.qualityGates().insertQualityGate(organization);
    db.qualityGates().setDefaultQualityGate(organization, gate);
    QualityGateConditionDto leakCondition = db.qualityGates().addCondition(gate, metric, c -> c.setPeriod(1));
    QualityGateConditionDto absoluteCondition = db.qualityGates().addCondition(gate, metric, c -> c.setPeriod(null));

    QualityGate result = underTest.loadQualityGate(db.getSession(), organization, project, branch);

    assertThat(result.getId()).isEqualTo("" + gate.getId());
    assertThat(result.getConditions())
      .extracting(Condition::getMetricKey, Condition::getOperator, c -> c.getErrorThreshold().get(), c -> c.getWarningThreshold().get(), Condition::isOnLeakPeriod)
      .containsExactlyInAnyOrder(
        tuple(metric.getKey(), Condition.Operator.fromDbValue(leakCondition.getOperator()), leakCondition.getErrorThreshold(), leakCondition.getWarningThreshold(), true),
        tuple(metric.getKey(), Condition.Operator.fromDbValue(absoluteCondition.getOperator()), absoluteCondition.getErrorThreshold(), absoluteCondition.getWarningThreshold(),
          false));
  }

  @Test
  public void getMetricsRelatedTo() {
    Condition condition = new Condition("metric1", Condition.Operator.EQUALS, "10", null, false);
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
    ComponentDto project = ComponentTesting.newPublicProjectDto(newOrganizationDto());
    MetricDto conditionMetric = newMetricDto();
    MetricDto statusMetric = newMetricDto().setKey(CoreMetrics.ALERT_STATUS_KEY);
    MetricDto detailsMetric = newMetricDto().setKey(CoreMetrics.QUALITY_GATE_DETAILS_KEY);
    Condition condition = new Condition(conditionMetric.getKey(), Condition.Operator.GREATER_THAN, "10", null, false);
    QualityGate gate = new QualityGate("1", "foo", ImmutableSet.of(condition));
    MeasureMatrix matrix = new MeasureMatrix(singleton(project), asList(conditionMetric, statusMetric, detailsMetric), emptyList());

    EvaluatedQualityGate result = underTest.refreshGateStatus(project, gate, matrix);

    QualityGateEvaluator.Measures measures = qualityGateEvaluator.getCalledMeasures();
    assertThat(measures.get(conditionMetric.getKey())).isEmpty();

    assertThat(result.getStatus()).isEqualTo(Metric.Level.OK);
    assertThat(result.getEvaluatedConditions())
      .extracting(EvaluatedCondition::getStatus)
      .containsExactly(EvaluatedCondition.EvaluationStatus.OK);
    assertThat(matrix.getMeasure(project, CoreMetrics.ALERT_STATUS_KEY).get().getDataAsString()).isEqualTo(Metric.Level.OK.name());
    assertThat(matrix.getMeasure(project, CoreMetrics.QUALITY_GATE_DETAILS_KEY).get().getDataAsString())
      .isNotEmpty()
      // json format
      .startsWith("{").endsWith("}");
  }

  @Test
  public void refreshGateStatus_provides_measures_to_evaluator() {
    ComponentDto project = ComponentTesting.newPublicProjectDto(newOrganizationDto());
    MetricDto numericMetric = newMetricDto().setValueType(Metric.ValueType.FLOAT.name());
    MetricDto stringMetric = newMetricDto().setValueType(Metric.ValueType.STRING.name());
    MetricDto statusMetric = newMetricDto().setKey(CoreMetrics.ALERT_STATUS_KEY);
    MetricDto detailsMetric = newMetricDto().setKey(CoreMetrics.QUALITY_GATE_DETAILS_KEY);
    QualityGate gate = new QualityGate("1", "foo", Collections.emptySet());
    LiveMeasureDto numericMeasure = new LiveMeasureDto().setMetricId(numericMetric.getId()).setValue(1.23).setVariation(4.56).setComponentUuid(project.uuid());
    LiveMeasureDto stringMeasure = new LiveMeasureDto().setMetricId(stringMetric.getId()).setData("bar").setComponentUuid(project.uuid());
    MeasureMatrix matrix = new MeasureMatrix(singleton(project), asList(statusMetric, detailsMetric, numericMetric, stringMetric), asList(numericMeasure, stringMeasure));

    underTest.refreshGateStatus(project, gate, matrix);

    QualityGateEvaluator.Measures measures = qualityGateEvaluator.getCalledMeasures();

    QualityGateEvaluator.Measure loadedStringMeasure = measures.get(stringMetric.getKey()).get();
    assertThat(loadedStringMeasure.getStringValue()).hasValue("bar");
    assertThat(loadedStringMeasure.getValue()).isEmpty();
    assertThat(loadedStringMeasure.getLeakValue()).isEmpty();
    assertThat(loadedStringMeasure.getType()).isEqualTo(Metric.ValueType.STRING);

    QualityGateEvaluator.Measure loadedNumericMeasure = measures.get(numericMetric.getKey()).get();
    assertThat(loadedNumericMeasure.getStringValue()).isEmpty();
    assertThat(loadedNumericMeasure.getValue()).hasValue(1.23);
    assertThat(loadedNumericMeasure.getLeakValue()).hasValue(4.56);
    assertThat(loadedNumericMeasure.getType()).isEqualTo(Metric.ValueType.FLOAT);
  }

  private static class TestQualityGateEvaluator implements QualityGateEvaluator {
    private Measures measures;

    @Override
    public EvaluatedQualityGate evaluate(QualityGate gate, Measures measures) {
      checkState(this.measures == null);
      this.measures = measures;
      EvaluatedQualityGate.Builder builder = EvaluatedQualityGate.newBuilder().setQualityGate(gate).setStatus(Metric.Level.OK);
      for (Condition condition : gate.getConditions()) {
        builder.addCondition(condition, EvaluatedCondition.EvaluationStatus.OK, "bar");
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
