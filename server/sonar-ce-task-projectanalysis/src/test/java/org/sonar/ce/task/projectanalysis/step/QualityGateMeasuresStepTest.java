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
package org.sonar.ce.task.projectanalysis.step;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nullable;
import org.assertj.core.api.AbstractAssert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.ReportComponent;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolderRule;
import org.sonar.ce.task.projectanalysis.measure.Measure;
import org.sonar.ce.task.projectanalysis.measure.MeasureRepositoryRule;
import org.sonar.ce.task.projectanalysis.measure.qualitygatedetails.EvaluatedCondition;
import org.sonar.ce.task.projectanalysis.measure.qualitygatedetails.QualityGateDetailsData;
import org.sonar.ce.task.projectanalysis.metric.Metric;
import org.sonar.ce.task.projectanalysis.metric.MetricImpl;
import org.sonar.ce.task.projectanalysis.metric.MetricRepositoryRule;
import org.sonar.ce.task.projectanalysis.qualitygate.Condition;
import org.sonar.ce.task.projectanalysis.qualitygate.ConditionStatus;
import org.sonar.ce.task.projectanalysis.qualitygate.EvaluationResult;
import org.sonar.ce.task.projectanalysis.qualitygate.EvaluationResultTextConverter;
import org.sonar.ce.task.projectanalysis.qualitygate.MutableQualityGateStatusHolderRule;
import org.sonar.ce.task.projectanalysis.qualitygate.QualityGate;
import org.sonar.ce.task.projectanalysis.qualitygate.QualityGateHolderRule;
import org.sonar.ce.task.projectanalysis.qualitygate.QualityGateStatus;
import org.sonar.ce.task.projectanalysis.qualitygate.QualityGateStatusHolder;
import org.sonar.ce.task.step.TestComputationStepContext;

import static com.google.common.collect.ImmutableList.of;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.api.measures.CoreMetrics.ALERT_STATUS_KEY;
import static org.sonar.ce.task.projectanalysis.measure.Measure.Level.ERROR;
import static org.sonar.ce.task.projectanalysis.measure.Measure.Level.OK;
import static org.sonar.ce.task.projectanalysis.measure.Measure.newMeasureBuilder;
import static org.sonar.ce.task.projectanalysis.measure.MeasureAssert.assertThat;

public class QualityGateMeasuresStepTest {
  private static final MetricImpl INT_METRIC_1 = createIntMetric(1);
  private static final String INT_METRIC_1_KEY = INT_METRIC_1.getKey();
  private static final MetricImpl INT_METRIC_2 = createIntMetric(2);
  private static final String INT_METRIC_2_KEY = INT_METRIC_2.getKey();

  private static final int PROJECT_REF = 1;
  private static final ReportComponent PROJECT_COMPONENT = ReportComponent.builder(Component.Type.PROJECT, PROJECT_REF).build();
  private static final long SOME_QG_ID = 7521551;
  private static final String SOME_QG_NAME = "name";

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule();
  @Rule
  public QualityGateHolderRule qualityGateHolder = new QualityGateHolderRule();
  @Rule
  public MutableQualityGateStatusHolderRule qualityGateStatusHolder = new MutableQualityGateStatusHolderRule();
  @Rule
  public MetricRepositoryRule metricRepository = new MetricRepositoryRule();
  @Rule
  public MeasureRepositoryRule measureRepository = MeasureRepositoryRule.create(treeRootHolder, metricRepository);

  private EvaluationResultTextConverter resultTextConverter = mock(EvaluationResultTextConverter.class);
  private QualityGateMeasuresStep underTest = new QualityGateMeasuresStep(treeRootHolder, qualityGateHolder, qualityGateStatusHolder, measureRepository, metricRepository,
    resultTextConverter, new SmallChangesetQualityGateSpecialCase(measureRepository, metricRepository));

  @Before
  public void setUp() {
    metricRepository
      .add(CoreMetrics.ALERT_STATUS)
      .add(CoreMetrics.QUALITY_GATE_DETAILS)
      .add(INT_METRIC_1)
      .add(INT_METRIC_2);
    treeRootHolder.setRoot(PROJECT_COMPONENT);

    // mock response of asText to any argument to return the result of dumbResultTextAnswer method
    when(resultTextConverter.asText(any(Condition.class), any(EvaluationResult.class))).thenAnswer(new Answer<String>() {
      @Override
      public String answer(InvocationOnMock invocation) {
        Condition condition = (Condition) invocation.getArguments()[0];
        EvaluationResult evaluationResult = (EvaluationResult) invocation.getArguments()[1];
        return dumbResultTextAnswer(condition, evaluationResult.getLevel(), evaluationResult.getValue());
      }
    });
  }

  private static String dumbResultTextAnswer(Condition condition, Measure.Level level, Object value) {
    return condition.toString() + level + value;
  }

  @Test
  public void no_measure_if_tree_has_no_project() {
    ReportComponent notAProjectComponent = ReportComponent.builder(Component.Type.DIRECTORY, 1).build();

    treeRootHolder.setRoot(notAProjectComponent);

    underTest.execute(new TestComputationStepContext());

    measureRepository.getAddedRawMeasures(1).isEmpty();
  }

  @Test
  public void no_measure_if_there_is_no_qualitygate() {
    qualityGateHolder.setQualityGate(null);

    underTest.execute(new TestComputationStepContext());

    measureRepository.getAddedRawMeasures(PROJECT_COMPONENT).isEmpty();
  }

  @Test
  public void mutableQualityGateStatusHolder_is_not_populated_if_there_is_no_qualitygate() {
    qualityGateHolder.setQualityGate(null);

    underTest.execute(new TestComputationStepContext());

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Quality gate status has not been set yet");

    qualityGateStatusHolder.getStatus();
  }

  @Test
  public void new_measures_are_created_even_if_there_is_no_rawMeasure_for_metric_of_condition() {
    Condition equals2Condition = createLessThanCondition(INT_METRIC_1, "2");
    qualityGateHolder.setQualityGate(new QualityGate(SOME_QG_ID, SOME_QG_NAME, of(equals2Condition)));

    underTest.execute(new TestComputationStepContext());

    Optional<Measure> addedRawMeasure = measureRepository.getAddedRawMeasure(PROJECT_COMPONENT, INT_METRIC_1_KEY);

    assertThat(addedRawMeasure).isAbsent();
    assertThat(getAlertStatusMeasure())
      .hasQualityGateLevel(OK)
      .hasQualityGateText("");
    assertThat(getQGDetailsMeasure())
      .hasValue(new QualityGateDetailsData(OK, Collections.emptyList(), false).toJson());

    QualityGateStatusHolderAssertions.assertThat(qualityGateStatusHolder)
      .hasStatus(QualityGateStatus.OK)
      .hasConditionCount(1)
      .hasCondition(equals2Condition, ConditionStatus.EvaluationStatus.NO_VALUE, null);
  }

  @Test
  public void rawMeasure_is_updated_if_present_and_new_measures_are_created_if_project_has_measure_for_metric_of_condition() {
    int rawValue = 3;
    Condition equals2Condition = createLessThanCondition(INT_METRIC_1, "2");
    Measure rawMeasure = newMeasureBuilder().create(rawValue, null);

    qualityGateHolder.setQualityGate(new QualityGate(SOME_QG_ID, SOME_QG_NAME, of(equals2Condition)));
    measureRepository.addRawMeasure(PROJECT_REF, INT_METRIC_1_KEY, rawMeasure);

    underTest.execute(new TestComputationStepContext());

    Optional<Measure> addedRawMeasure = measureRepository.getAddedRawMeasure(PROJECT_COMPONENT, INT_METRIC_1_KEY);

    assertThat(addedRawMeasure)
      .hasQualityGateLevel(OK)
      .hasQualityGateText(dumbResultTextAnswer(equals2Condition, OK, rawValue));
    assertThat(getAlertStatusMeasure())
      .hasQualityGateLevel(OK)
      .hasQualityGateText(dumbResultTextAnswer(equals2Condition, OK, rawValue));
    assertThat(getQGDetailsMeasure().get())
      .hasValue(new QualityGateDetailsData(OK, of(new EvaluatedCondition(equals2Condition, OK, rawValue)), false).toJson());

    QualityGateStatusHolderAssertions.assertThat(qualityGateStatusHolder)
      .hasStatus(QualityGateStatus.OK)
      .hasConditionCount(1)
      .hasCondition(equals2Condition, ConditionStatus.EvaluationStatus.OK, String.valueOf(rawValue));
  }

  @Test
  public void new_measures_have_ERROR_level_if_at_least_one_updated_measure_has_ERROR_level() {
    int rawValue = 3;
    Condition equalsOneErrorCondition = createLessThanCondition(INT_METRIC_1, "4");
    Condition equalsOneOkCondition = createLessThanCondition(INT_METRIC_2, "2");
    Measure rawMeasure = newMeasureBuilder().create(rawValue, null);

    qualityGateHolder.setQualityGate(new QualityGate(SOME_QG_ID, SOME_QG_NAME, of(equalsOneErrorCondition, equalsOneOkCondition)));
    measureRepository.addRawMeasure(PROJECT_REF, INT_METRIC_1_KEY, rawMeasure);
    measureRepository.addRawMeasure(PROJECT_REF, INT_METRIC_2_KEY, rawMeasure);

    underTest.execute(new TestComputationStepContext());

    Optional<Measure> rawMeasure1 = measureRepository.getAddedRawMeasure(PROJECT_REF, INT_METRIC_1_KEY);
    Optional<Measure> rawMeasure2 = measureRepository.getAddedRawMeasure(PROJECT_REF, INT_METRIC_2_KEY);

    assertThat(rawMeasure1.get())
      .hasQualityGateLevel(ERROR)
      .hasQualityGateText(dumbResultTextAnswer(equalsOneErrorCondition, ERROR, rawValue));
    assertThat(rawMeasure2.get())
      .hasQualityGateLevel(OK)
      .hasQualityGateText(dumbResultTextAnswer(equalsOneOkCondition, OK, rawValue));
    assertThat(getAlertStatusMeasure())
      .hasQualityGateLevel(ERROR)
      .hasQualityGateText(dumbResultTextAnswer(equalsOneErrorCondition, ERROR, rawValue) + ", "
        + dumbResultTextAnswer(equalsOneOkCondition, OK, rawValue));
    assertThat(getQGDetailsMeasure())
      .hasValue(new QualityGateDetailsData(ERROR, of(
        new EvaluatedCondition(equalsOneErrorCondition, ERROR, rawValue),
        new EvaluatedCondition(equalsOneOkCondition, OK, rawValue)), false).toJson());

    QualityGateStatusHolderAssertions.assertThat(qualityGateStatusHolder)
      .hasStatus(QualityGateStatus.ERROR)
      .hasConditionCount(2)
      .hasCondition(equalsOneErrorCondition, ConditionStatus.EvaluationStatus.ERROR, String.valueOf(rawValue))
      .hasCondition(equalsOneOkCondition, ConditionStatus.EvaluationStatus.OK, String.valueOf(rawValue));
  }

  @Test
  public void new_measure_has_ERROR_level_of_all_conditions_for_a_specific_metric_if_its_the_worst() {
    int rawValue = 3;
    Condition fixedCondition = createLessThanCondition(INT_METRIC_1, "4");
    Condition periodCondition = createLessThanCondition(INT_METRIC_1, "2");

    qualityGateHolder.setQualityGate(new QualityGate(SOME_QG_ID, SOME_QG_NAME, of(fixedCondition, periodCondition)));
    Measure measure = newMeasureBuilder().create(rawValue, null);
    measureRepository.addRawMeasure(PROJECT_REF, INT_METRIC_1_KEY, measure);

    underTest.execute(new TestComputationStepContext());

    Optional<Measure> rawMeasure1 = measureRepository.getAddedRawMeasure(PROJECT_REF, INT_METRIC_1_KEY);
    assertThat(rawMeasure1.get())
      .hasQualityGateLevel(ERROR)
      .hasQualityGateText(dumbResultTextAnswer(fixedCondition, ERROR, rawValue));
  }

  @Test
  public void new_measure_has_condition_on_leak_period_when_all_conditions_on_specific_metric_has_same_QG_level() {
    int rawValue = 0;
    Condition fixedCondition = createLessThanCondition(INT_METRIC_1, "1");
    Condition periodCondition = createLessThanCondition(INT_METRIC_1, "1");

    qualityGateHolder.setQualityGate(new QualityGate(SOME_QG_ID, SOME_QG_NAME, of(fixedCondition, periodCondition)));
    Measure measure = newMeasureBuilder()
      .setVariation(rawValue)
      .create(rawValue, null);
    measureRepository.addRawMeasure(PROJECT_REF, INT_METRIC_1_KEY, measure);

    underTest.execute(new TestComputationStepContext());

    Optional<Measure> rawMeasure1 = measureRepository.getAddedRawMeasure(PROJECT_REF, INT_METRIC_1_KEY);
    assertThat(rawMeasure1.get())
      .hasQualityGateLevel(ERROR)
      .hasQualityGateText(dumbResultTextAnswer(periodCondition, ERROR, rawValue));
  }

  private Measure getAlertStatusMeasure() {
    return measureRepository.getAddedRawMeasure(PROJECT_REF, ALERT_STATUS_KEY).get();
  }

  private Optional<Measure> getQGDetailsMeasure() {
    return measureRepository.getAddedRawMeasure(PROJECT_REF, CoreMetrics.QUALITY_GATE_DETAILS_KEY);
  }

  private static Condition createLessThanCondition(Metric metric, String errorThreshold) {
    return new Condition(metric, Condition.Operator.LESS_THAN.getDbValue(), errorThreshold);
  }

  private static MetricImpl createIntMetric(int index) {
    return new MetricImpl(index, "metricKey" + index, "metricName" + index, Metric.MetricType.INT);
  }

  private static class QualityGateStatusHolderAssertions extends AbstractAssert<QualityGateStatusHolderAssertions, QualityGateStatusHolder> {

    private QualityGateStatusHolderAssertions(QualityGateStatusHolder actual) {
      super(actual, QualityGateStatusHolderAssertions.class);
    }

    public static QualityGateStatusHolderAssertions assertThat(QualityGateStatusHolder holder) {
      return new QualityGateStatusHolderAssertions(holder);
    }

    public QualityGateStatusHolderAssertions hasStatus(QualityGateStatus status) {
      if (actual.getStatus() != status) {
        failWithMessage(
          "Expected QualityGateStatusHolder to have global status <%s> but was <%s>",
          status, actual.getStatus());
      }

      return this;
    }

    public QualityGateStatusHolderAssertions hasConditionCount(int count) {
      int conditionCount = actual.getStatusPerConditions().size();
      if (conditionCount != count) {
        failWithMessage(
          "Expected QualityGateStatusHolder to have <%s> conditions but it has <%s>",
          count, conditionCount);
      }

      return this;
    }

    public QualityGateStatusHolderAssertions hasCondition(Condition condition, ConditionStatus.EvaluationStatus evaluationStatus, @Nullable String expectedValue) {
      for (Map.Entry<Condition, ConditionStatus> entry : actual.getStatusPerConditions().entrySet()) {
        if (entry.getKey() == condition) {
          ConditionStatus.EvaluationStatus actualStatus = entry.getValue().getStatus();
          if (actualStatus != evaluationStatus) {
            failWithMessage(
              "Expected Status of condition <%s> in QualityGateStatusHolder to be <%s> but it was <%s>",
              condition, evaluationStatus, actualStatus);
          }
          String actualValue = entry.getValue().getValue();
          if (!Objects.equals(expectedValue, actualValue)) {
            failWithMessage(
              "Expected Value of condition <%s> in QualityGateStatusHolder to be <%s> but it was <%s>",
              condition, expectedValue, actualValue);
          }
          return this;
        }
      }

      failWithMessage(
        "Expected QualityGateStatusHolder to have an entry for <%s> but none was found",
        condition);

      return this;
    }
  }

}
