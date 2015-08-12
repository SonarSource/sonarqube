/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.computation.step;

import com.google.common.base.Optional;
import java.util.Collections;
import javax.annotation.Nullable;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.server.computation.batch.TreeRootHolderRule;
import org.sonar.server.computation.component.Component;
import org.sonar.server.computation.component.ReportComponent;
import org.sonar.server.computation.measure.Measure;
import org.sonar.server.computation.measure.MeasureRepository;
import org.sonar.server.computation.measure.qualitygatedetails.EvaluatedCondition;
import org.sonar.server.computation.measure.qualitygatedetails.QualityGateDetailsData;
import org.sonar.server.computation.metric.Metric;
import org.sonar.server.computation.metric.MetricImpl;
import org.sonar.server.computation.metric.MetricRepository;
import org.sonar.server.computation.qualitygate.Condition;
import org.sonar.server.computation.qualitygate.EvaluationResult;
import org.sonar.server.computation.qualitygate.EvaluationResultTextConverter;
import org.sonar.server.computation.qualitygate.QualityGate;
import org.sonar.server.computation.qualitygate.QualityGateHolderRule;

import static com.google.common.collect.ImmutableList.of;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.sonar.server.computation.measure.Measure.Level.ERROR;
import static org.sonar.server.computation.measure.Measure.Level.OK;
import static org.sonar.server.computation.measure.Measure.Level.WARN;
import static org.sonar.server.computation.measure.MeasureAssert.assertThat;

public class QualityGateMeasuresStepTest {
  private static final MetricImpl INT_METRIC_1 = createIntMetric(1);
  private static final MetricImpl INT_METRIC_2 = createIntMetric(2);

  private static final ReportComponent PROJECT_COMPONENT = ReportComponent.builder(Component.Type.PROJECT, 1).build();

  @Rule
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule();
  @Rule
  public QualityGateHolderRule qualityGateHolder = new QualityGateHolderRule();

  private static final Metric ALERT_STATUS_METRIC = mock(Metric.class);
  private static final Metric QUALITY_GATE_DETAILS_METRIC = mock(Metric.class);

  private ArgumentCaptor<Measure> alertStatusMeasureCaptor = ArgumentCaptor.forClass(Measure.class);
  private ArgumentCaptor<Measure> qgDetailsMeasureCaptor = ArgumentCaptor.forClass(Measure.class);

  private MeasureRepository measureRepository = mock(MeasureRepository.class);
  private MetricRepository metricRepository = mock(MetricRepository.class);
  private EvaluationResultTextConverter resultTextConverter = mock(EvaluationResultTextConverter.class);
  private QualityGateMeasuresStep underTest = new QualityGateMeasuresStep(treeRootHolder, qualityGateHolder, measureRepository, metricRepository, resultTextConverter);

  @Before
  public void setUp() {
    treeRootHolder.setRoot(PROJECT_COMPONENT);

    when(metricRepository.getByKey(CoreMetrics.ALERT_STATUS_KEY)).thenReturn(ALERT_STATUS_METRIC);
    when(metricRepository.getByKey(CoreMetrics.QUALITY_GATE_DETAILS_KEY)).thenReturn(QUALITY_GATE_DETAILS_METRIC);

    // mock response of asText to any argument to return the result of dumbResultTextAnswer method
    when(resultTextConverter.asText(any(Condition.class), any(EvaluationResult.class))).thenAnswer(new Answer<String>() {
      @Override
      public String answer(InvocationOnMock invocation) throws Throwable {
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
    ReportComponent notAProjectComponent = ReportComponent.builder(Component.Type.MODULE, 1).build();

    treeRootHolder.setRoot(notAProjectComponent);

    underTest.execute();

    verifyNoMoreInteractions(measureRepository);
  }

  @Test
  public void no_measure_if_there_is_no_qualitygate() {
    qualityGateHolder.setQualityGate(null);

    underTest.execute();

    verifyNoMoreInteractions(measureRepository);
  }

  @Test
  public void new_measures_are_created_even_if_there_is_no_rawMeasure_for_metric_of_condition() {
    Condition equals2Condition = createEqualsCondition(INT_METRIC_1, "2", null);
    qualityGateHolder.setQualityGate(new QualityGate("name", of(equals2Condition)));
    when(measureRepository.getRawMeasure(PROJECT_COMPONENT, INT_METRIC_1)).thenReturn(Optional.<Measure>absent());

    underTest.execute();

    verify(measureRepository).getRawMeasure(PROJECT_COMPONENT, INT_METRIC_1);
    verify(measureRepository).add(same(PROJECT_COMPONENT), same(ALERT_STATUS_METRIC), alertStatusMeasureCaptor.capture());
    verify(measureRepository).add(same(PROJECT_COMPONENT), same(QUALITY_GATE_DETAILS_METRIC), qgDetailsMeasureCaptor.capture());
    verifyNoMoreInteractions(measureRepository);

    assertThat(alertStatusMeasureCaptor.getValue())
      .hasQualityGateLevel(OK)
      .hasQualityGateText("");
    assertThat(qgDetailsMeasureCaptor.getValue())
      .hasValue(new QualityGateDetailsData(OK, Collections.<EvaluatedCondition>emptyList()).toJson());
  }

  @Test
  public void rawMeasure_is_updated_if_present_and_new_measures_are_created_if_project_has_measure_for_metric_of_condition() {
    int rawValue = 1;
    Condition equals2Condition = createEqualsCondition(INT_METRIC_1, "2", null);
    Measure rawMeasure = Measure.newMeasureBuilder().create(rawValue, null);

    qualityGateHolder.setQualityGate(new QualityGate("name", of(equals2Condition)));
    when(measureRepository.getRawMeasure(PROJECT_COMPONENT, INT_METRIC_1)).thenReturn(Optional.of(rawMeasure));

    underTest.execute();

    ArgumentCaptor<Measure> equals2ConditionMeasureCaptor = ArgumentCaptor.forClass(Measure.class);

    verify(measureRepository).getRawMeasure(PROJECT_COMPONENT, INT_METRIC_1);
    verify(measureRepository).update(same(PROJECT_COMPONENT), same(INT_METRIC_1), equals2ConditionMeasureCaptor.capture());
    verify(measureRepository).add(same(PROJECT_COMPONENT), same(ALERT_STATUS_METRIC), alertStatusMeasureCaptor.capture());
    verify(measureRepository).add(same(PROJECT_COMPONENT), same(QUALITY_GATE_DETAILS_METRIC), qgDetailsMeasureCaptor.capture());
    verifyNoMoreInteractions(measureRepository);

    assertThat(equals2ConditionMeasureCaptor.getValue())
      .hasQualityGateLevel(OK)
      .hasQualityGateText(dumbResultTextAnswer(equals2Condition, OK, rawValue));
    assertThat(alertStatusMeasureCaptor.getValue())
      .hasQualityGateLevel(OK)
      .hasQualityGateText(dumbResultTextAnswer(equals2Condition, OK, rawValue));
    assertThat(qgDetailsMeasureCaptor.getValue())
      .hasValue(new QualityGateDetailsData(OK, of(new EvaluatedCondition(equals2Condition, OK, rawValue))).toJson());
  }

  @Test
  public void new_measures_have_ERROR_level_if_at_least_one_updated_measure_has_ERROR_level() {
    int rawValue = 1;
    Condition equals1ErrorCondition = createEqualsCondition(INT_METRIC_1, "1", null);
    Condition equals1WarningCondition = createEqualsCondition(INT_METRIC_2, null, "1");
    Measure rawMeasure = Measure.newMeasureBuilder().create(rawValue, null);

    qualityGateHolder.setQualityGate(new QualityGate("name", of(equals1ErrorCondition, equals1WarningCondition)));
    when(measureRepository.getRawMeasure(PROJECT_COMPONENT, INT_METRIC_1)).thenReturn(Optional.of(rawMeasure));
    when(measureRepository.getRawMeasure(PROJECT_COMPONENT, INT_METRIC_2)).thenReturn(Optional.of(rawMeasure));

    underTest.execute();

    ArgumentCaptor<Measure> equals1ErrorConditionMeasureCaptor = ArgumentCaptor.forClass(Measure.class);
    ArgumentCaptor<Measure> equals1WarningConditionMeasureCaptor = ArgumentCaptor.forClass(Measure.class);

    verify(measureRepository).getRawMeasure(PROJECT_COMPONENT, INT_METRIC_1);
    verify(measureRepository).getRawMeasure(PROJECT_COMPONENT, INT_METRIC_2);
    verify(measureRepository).update(same(PROJECT_COMPONENT), same(INT_METRIC_1), equals1ErrorConditionMeasureCaptor.capture());
    verify(measureRepository).update(same(PROJECT_COMPONENT), same(INT_METRIC_2), equals1WarningConditionMeasureCaptor.capture());
    verify(measureRepository).add(same(PROJECT_COMPONENT), same(ALERT_STATUS_METRIC), alertStatusMeasureCaptor.capture());
    verify(measureRepository).add(same(PROJECT_COMPONENT), same(QUALITY_GATE_DETAILS_METRIC), qgDetailsMeasureCaptor.capture());
    verifyNoMoreInteractions(measureRepository);

    assertThat(equals1ErrorConditionMeasureCaptor.getValue())
        .hasQualityGateLevel(ERROR)
        .hasQualityGateText(dumbResultTextAnswer(equals1ErrorCondition, ERROR, rawValue));
    assertThat(equals1WarningConditionMeasureCaptor.getValue())
        .hasQualityGateLevel(WARN)
        .hasQualityGateText(dumbResultTextAnswer(equals1WarningCondition, WARN, rawValue));
    assertThat(alertStatusMeasureCaptor.getValue())
        .hasQualityGateLevel(ERROR)
        .hasQualityGateText(dumbResultTextAnswer(equals1ErrorCondition, ERROR, rawValue) + ", "
            + dumbResultTextAnswer(equals1WarningCondition, WARN, rawValue));
    assertThat(qgDetailsMeasureCaptor.getValue())
        .hasValue(new QualityGateDetailsData(ERROR, of(
            new EvaluatedCondition(equals1ErrorCondition, ERROR, rawValue),
            new EvaluatedCondition(equals1WarningCondition, WARN, rawValue)
        )).toJson());
  }

  @Test
  public void new_measures_have_WARNING_level_if_no_updated_measure_has_ERROR_level() {
    int rawValue = 1;
    Condition equals2Condition = createEqualsCondition(INT_METRIC_1, "2", null);
    Condition equals1WarningCondition = createEqualsCondition(INT_METRIC_2, null, "1");
    Measure rawMeasure = Measure.newMeasureBuilder().create(rawValue, null);

    qualityGateHolder.setQualityGate(new QualityGate("name", of(equals2Condition, equals1WarningCondition)));
    when(measureRepository.getRawMeasure(PROJECT_COMPONENT, INT_METRIC_1)).thenReturn(Optional.of(rawMeasure));
    when(measureRepository.getRawMeasure(PROJECT_COMPONENT, INT_METRIC_2)).thenReturn(Optional.of(rawMeasure));

    underTest.execute();

    ArgumentCaptor<Measure> equals2ConditionMeasureCaptor = ArgumentCaptor.forClass(Measure.class);
    ArgumentCaptor<Measure> equals1WarningConditionMeasureCaptor = ArgumentCaptor.forClass(Measure.class);

    verify(measureRepository).getRawMeasure(PROJECT_COMPONENT, INT_METRIC_1);
    verify(measureRepository).getRawMeasure(PROJECT_COMPONENT, INT_METRIC_2);
    verify(measureRepository).update(same(PROJECT_COMPONENT), same(INT_METRIC_1), equals2ConditionMeasureCaptor.capture());
    verify(measureRepository).update(same(PROJECT_COMPONENT), same(INT_METRIC_2), equals1WarningConditionMeasureCaptor.capture());
    verify(measureRepository).add(same(PROJECT_COMPONENT), same(ALERT_STATUS_METRIC), alertStatusMeasureCaptor.capture());
    verify(measureRepository).add(same(PROJECT_COMPONENT), same(QUALITY_GATE_DETAILS_METRIC), qgDetailsMeasureCaptor.capture());
    verifyNoMoreInteractions(measureRepository);

    assertThat(equals2ConditionMeasureCaptor.getValue())
        .hasQualityGateLevel(OK)
        .hasQualityGateText(dumbResultTextAnswer(equals2Condition, OK, rawValue));
    assertThat(equals1WarningConditionMeasureCaptor.getValue())
        .hasQualityGateLevel(WARN)
        .hasQualityGateText(dumbResultTextAnswer(equals1WarningCondition, WARN, rawValue));
    assertThat(alertStatusMeasureCaptor.getValue())
        .hasQualityGateLevel(WARN)
        .hasQualityGateText(dumbResultTextAnswer(equals2Condition, OK, rawValue) + ", "
            + dumbResultTextAnswer(equals1WarningCondition, WARN, rawValue));
    assertThat(qgDetailsMeasureCaptor.getValue())
        .hasValue(new QualityGateDetailsData(WARN, of(
            new EvaluatedCondition(equals2Condition, OK, rawValue),
            new EvaluatedCondition(equals1WarningCondition, WARN, rawValue)
        )).toJson());
  }

  private static Condition createEqualsCondition(Metric metric, @Nullable String errorThreshold, @Nullable String warningThreshold) {
    return new Condition(metric, Condition.Operator.EQUALS.getDbValue(), errorThreshold, warningThreshold, null);
  }

  private static MetricImpl createIntMetric(int index) {
    return new MetricImpl(index, "metricKey" + index, "metricName" + index, Metric.MetricType.INT);
  }

}
