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
package org.sonar.ce.task.projectanalysis.measure;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sonar.ce.task.projectanalysis.analysis.MutableAnalysisMetadataHolderRule;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.MutableTreeRootHolderRule;
import org.sonar.ce.task.projectanalysis.component.ReportComponent;
import org.sonar.ce.task.projectanalysis.metric.Metric;
import org.sonar.ce.task.projectanalysis.metric.MetricImpl;
import org.sonar.db.measure.LiveMeasureDto;
import org.sonar.db.measure.MeasureDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.sonar.ce.task.projectanalysis.measure.MeasureToMeasureDto.getMeasureValue;

@RunWith(DataProviderRunner.class)
public class MeasureToMeasureDtoTest {
  private static final MetricImpl SOME_METRIC = new MetricImpl("42", "metric_key", "metric_name", Metric.MetricType.STRING);
  private static final String SOME_DATA = "some_data";
  private static final String SOME_STRING = "some_string";
  private static final MetricImpl SOME_BOOLEAN_METRIC = new MetricImpl("1", "1", "1", Metric.MetricType.BOOL);
  private static final MetricImpl SOME_INT_METRIC = new MetricImpl("2", "2", "2", Metric.MetricType.INT);
  private static final MetricImpl SOME_LONG_METRIC = new MetricImpl("3", "3", "3", Metric.MetricType.DISTRIB);
  private static final MetricImpl SOME_DOUBLE_METRIC = new MetricImpl("4", "4", "4", Metric.MetricType.FLOAT);
  private static final MetricImpl SOME_STRING_METRIC = new MetricImpl("5", "5", "5", Metric.MetricType.STRING);
  private static final MetricImpl SOME_LEVEL_METRIC = new MetricImpl("6", "6", "6", Metric.MetricType.LEVEL);
  private static final String ANALYSIS_UUID = "a1";
  private static final Component SOME_COMPONENT = ReportComponent.builder(Component.Type.PROJECT, 1).setUuid("uuid_1").build();

  @Rule
  public MutableAnalysisMetadataHolderRule analysisMetadataHolder = new MutableAnalysisMetadataHolderRule();

  @Rule
  public MutableTreeRootHolderRule treeRootHolder = new MutableTreeRootHolderRule();

  private MeasureToMeasureDto underTest = new MeasureToMeasureDto(analysisMetadataHolder, treeRootHolder);

  @Before
  public void setUp() {
    analysisMetadataHolder.setUuid(ANALYSIS_UUID);
  }

  @Test
  public void toMeasureDto_throws_NPE_if_Measure_arg_is_null() {
    assertThatThrownBy(() -> underTest.toMeasureDto(null, SOME_METRIC, SOME_COMPONENT))
      .isInstanceOf(NullPointerException.class);
  }

  @Test
  public void toMeasureDto_throws_NPE_if_Metric_arg_is_null() {
    assertThatThrownBy(() -> underTest.toMeasureDto(Measure.newMeasureBuilder().createNoValue(), null, SOME_COMPONENT))
      .isInstanceOf(NullPointerException.class);
  }

  @DataProvider
  public static Object[][] all_types_Measures() {
    return new Object[][] {
      {Measure.newMeasureBuilder().create(true, SOME_DATA), SOME_BOOLEAN_METRIC},
      {Measure.newMeasureBuilder().create(1, SOME_DATA), SOME_INT_METRIC},
      {Measure.newMeasureBuilder().create((long) 1, SOME_DATA), SOME_LONG_METRIC},
      {Measure.newMeasureBuilder().create(2, 1, SOME_DATA), SOME_DOUBLE_METRIC},
      {Measure.newMeasureBuilder().create(SOME_STRING), SOME_STRING_METRIC},
      {Measure.newMeasureBuilder().create(Measure.Level.OK), SOME_LEVEL_METRIC}
    };
  }

  @Test
  @UseDataProvider("all_types_Measures")
  public void toMeasureDto_returns_Dto_without_alertStatus_nor_alertText_if_Measure_has_no_QualityGateStatus(Measure measure, Metric metric) {
    MeasureDto measureDto = underTest.toMeasureDto(measure, metric, SOME_COMPONENT);

    assertThat(measureDto.getAlertStatus()).isNull();
    assertThat(measureDto.getAlertText()).isNull();
  }

  @Test
  public void toMeasureDto_returns_Dto_with_alertStatus_and_alertText_if_Measure_has_QualityGateStatus() {
    String alertText = "some error";
    MeasureDto measureDto = underTest.toMeasureDto(Measure.newMeasureBuilder().setQualityGateStatus(new QualityGateStatus(Measure.Level.ERROR, alertText)).create(SOME_STRING),
      SOME_STRING_METRIC, SOME_COMPONENT);

    assertThat(measureDto.getAlertStatus()).isEqualTo(Measure.Level.ERROR.name());
    assertThat(measureDto.getAlertText()).isEqualTo(alertText);
  }

  @Test
  @UseDataProvider("all_types_Measures")
  public void toMeasureDto_set_componentId_and_snapshotId_from_method_arguments(Measure measure, Metric metric) {
    MeasureDto measureDto = underTest.toMeasureDto(measure, metric, SOME_COMPONENT);

    assertThat(measureDto.getComponentUuid()).isEqualTo(SOME_COMPONENT.getUuid());
  }

  @Test
  public void toMeasureDto_maps_value_to_1_or_0_and_data_from_data_field_for_BOOLEAN_metric() {
    MeasureDto trueMeasureDto = underTest.toMeasureDto(Measure.newMeasureBuilder().create(true, SOME_DATA), SOME_BOOLEAN_METRIC, SOME_COMPONENT);

    assertThat(trueMeasureDto.getValue()).isEqualTo(1d);
    assertThat(trueMeasureDto.getData()).isEqualTo(SOME_DATA);

    MeasureDto falseMeasureDto = underTest.toMeasureDto(Measure.newMeasureBuilder().create(false, SOME_DATA), SOME_BOOLEAN_METRIC, SOME_COMPONENT);

    assertThat(falseMeasureDto.getValue()).isEqualTo(0d);
    assertThat(falseMeasureDto.getData()).isEqualTo(SOME_DATA);
  }

  @Test
  public void toMeasureDto_maps_value_and_data_from_data_field_for_INT_metric() {
    MeasureDto trueMeasureDto = underTest.toMeasureDto(Measure.newMeasureBuilder().create(123, SOME_DATA), SOME_INT_METRIC, SOME_COMPONENT);

    assertThat(trueMeasureDto.getValue()).isEqualTo(123);
    assertThat(trueMeasureDto.getData()).isEqualTo(SOME_DATA);
  }

  @Test
  public void toMeasureDto_maps_value_and_data_from_data_field_for_LONG_metric() {
    MeasureDto trueMeasureDto = underTest.toMeasureDto(Measure.newMeasureBuilder().create((long) 456, SOME_DATA), SOME_LONG_METRIC, SOME_COMPONENT);

    assertThat(trueMeasureDto.getValue()).isEqualTo(456);
    assertThat(trueMeasureDto.getData()).isEqualTo(SOME_DATA);
  }

  @Test
  public void toMeasureDto_maps_value_and_data_from_data_field_for_DOUBLE_metric() {
    MeasureDto trueMeasureDto = underTest.toMeasureDto(Measure.newMeasureBuilder().create(789, 1, SOME_DATA), SOME_DOUBLE_METRIC, SOME_COMPONENT);

    assertThat(trueMeasureDto.getValue()).isEqualTo(789);
    assertThat(trueMeasureDto.getData()).isEqualTo(SOME_DATA);
  }

  @Test
  public void toMeasureDto_maps_to_only_data_for_STRING_metric() {
    MeasureDto trueMeasureDto = underTest.toMeasureDto(Measure.newMeasureBuilder().create(SOME_STRING), SOME_STRING_METRIC, SOME_COMPONENT);

    assertThat(trueMeasureDto.getValue()).isNull();
    assertThat(trueMeasureDto.getData()).isEqualTo(SOME_STRING);
  }

  @Test
  public void toMeasureDto_maps_name_of_Level_to_data_and_has_no_value_for_LEVEL_metric() {
    MeasureDto trueMeasureDto = underTest.toMeasureDto(Measure.newMeasureBuilder().create(Measure.Level.OK), SOME_LEVEL_METRIC, SOME_COMPONENT);

    assertThat(trueMeasureDto.getValue()).isNull();
    assertThat(trueMeasureDto.getData()).isEqualTo(Measure.Level.OK.name());
  }

  @Test
  public void toLiveMeasureDto() {
    treeRootHolder.setRoot(SOME_COMPONENT);

    LiveMeasureDto liveMeasureDto = underTest.toLiveMeasureDto(
      Measure.newMeasureBuilder().create(Measure.Level.OK),
      SOME_LEVEL_METRIC,
      SOME_COMPONENT);

    assertThat(liveMeasureDto.getTextValue()).isEqualTo(Measure.Level.OK.name());
  }

  @Test
  public void getMeasureValue_returns_null_if_measure_is_empty() {
    assertThat(getMeasureValue(Measure.newMeasureBuilder().createNoValue())).isNull();
  }

  @Test
  public void getMeasureValue_maps_value_to_1_or_0_and_data_from_data_field_for_BOOLEAN_metric() {
    assertThat(getMeasureValue(Measure.newMeasureBuilder().create(true, SOME_DATA))).isEqualTo(1d);
    assertThat(getMeasureValue(Measure.newMeasureBuilder().create(false, SOME_DATA))).isEqualTo(0d);
  }

  @Test
  public void getMeasureValue_maps_value_and_data_from_data_field_for_INT_metric() {
    assertThat(getMeasureValue(Measure.newMeasureBuilder().create(123, SOME_DATA))).isEqualTo(123.0);
  }

  @Test
  public void getMeasureValue_maps_value_and_data_from_data_field_for_LONG_metric() {
    assertThat(getMeasureValue(Measure.newMeasureBuilder().create((long) 456, SOME_DATA))).isEqualTo(456.0);
  }

  @Test
  public void getMeasureValue_maps_value_and_data_from_data_field_for_DOUBLE_metric() {
    assertThat(getMeasureValue(Measure.newMeasureBuilder().create(789, 1, SOME_DATA))).isEqualTo(789.0);
  }

  @Test
  public void getMeasureValue_maps_to_only_data_for_STRING_metric() {
    assertThat(getMeasureValue(
      Measure.newMeasureBuilder().create(SOME_STRING))).isEqualTo(SOME_STRING);
  }

  @Test
  public void getMeasureValue_maps_name_of_Level_to_data_and_has_no_value_for_LEVEL_metric() {
    assertThat(getMeasureValue(
      Measure.newMeasureBuilder().create(Measure.Level.OK))).isEqualTo(Measure.Level.OK.name());
  }
}
