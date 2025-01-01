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
import java.util.Optional;
import org.assertj.core.data.Offset;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sonar.ce.task.projectanalysis.measure.Measure.Level;
import org.sonar.ce.task.projectanalysis.metric.Metric;
import org.sonar.ce.task.projectanalysis.metric.MetricImpl;
import org.sonar.db.measure.ProjectMeasureDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.sonar.ce.task.projectanalysis.measure.ProjectMeasureDtoToMeasure.toMeasure;

@RunWith(DataProviderRunner.class)
public class ProjectMeasureDtoToMeasureTest {
  private static final Metric SOME_INT_METRIC = new MetricImpl("42", "int", "name", Metric.MetricType.INT);
  private static final Metric SOME_LONG_METRIC = new MetricImpl("42", "long", "name", Metric.MetricType.WORK_DUR);
  private static final Metric SOME_DOUBLE_METRIC = new MetricImpl("42", "double", "name", Metric.MetricType.FLOAT);
  private static final Metric SOME_STRING_METRIC = new MetricImpl("42", "string", "name", Metric.MetricType.STRING);
  private static final Metric SOME_BOOLEAN_METRIC = new MetricImpl("42", "boolean", "name", Metric.MetricType.BOOL);
  private static final Metric SOME_LEVEL_METRIC = new MetricImpl("42", "level", "name", Metric.MetricType.LEVEL);

  private static final String SOME_DATA = "some_data man!";
  private static final String SOME_ALERT_TEXT = "some alert text_be_careFul!";
  private static final ProjectMeasureDto EMPTY_MEASURE_DTO = new ProjectMeasureDto();

  @Test
  public void toMeasure_returns_absent_for_null_argument() {
    assertThat(toMeasure(null, SOME_INT_METRIC)).isNotPresent();
  }

  @Test
  public void toMeasure_throws_NPE_if_metric_argument_is_null() {
    assertThatThrownBy(() -> toMeasure(EMPTY_MEASURE_DTO, null))
      .isInstanceOf(NullPointerException.class);
  }

  @Test
  public void toMeasure_throws_NPE_if_both_arguments_are_null() {
    assertThatThrownBy(() -> toMeasure(null, null))
      .isInstanceOf(NullPointerException.class);
  }

  @Test
  public void toMeasure_returns_no_value_if_dto_has_no_data_for_Level_Metric() {
    Optional<Measure> measure = toMeasure(EMPTY_MEASURE_DTO, SOME_LEVEL_METRIC);
    assertThat(measure).isPresent();
    assertThat(measure.get().getValueType()).isEqualTo(ValueType.NO_VALUE);
  }

  @Test
  public void toMeasure_returns_no_value_if_dto_has_invalid_data_for_Level_Metric() {
    Optional<Measure> measure = toMeasure(new ProjectMeasureDto().setData("trololo"), SOME_LEVEL_METRIC);
    assertThat(measure).isPresent();
    assertThat(measure.get().getValueType()).isEqualTo(ValueType.NO_VALUE);
  }

  @Test
  public void toMeasure_returns_no_value_if_dta_has_data_in_wrong_case_for_Level_Metric() {
    Optional<Measure> measure = toMeasure(new ProjectMeasureDto().setData("waRn"), SOME_LEVEL_METRIC);
    assertThat(measure).isPresent();
    assertThat(measure.get().getValueType()).isEqualTo(ValueType.NO_VALUE);
  }

  @Test
  public void toMeasure_returns_no_QualityGateStatus_if_dto_has_no_alertStatus_for_Level_Metric() {
    Optional<Measure> measure = toMeasure(EMPTY_MEASURE_DTO, SOME_STRING_METRIC);
    assertThat(measure).isPresent();
    assertThat(measure.get().hasQualityGateStatus()).isFalse();
  }

  @Test
  public void toMeasure_returns_no_QualityGateStatus_if_alertStatus_has_invalid_data_for_Level_Metric() {
    Optional<Measure> measure = toMeasure(new ProjectMeasureDto().setData("trololo"), SOME_STRING_METRIC);
    assertThat(measure).isPresent();
    assertThat(measure.get().hasQualityGateStatus()).isFalse();
  }

  @Test
  public void toMeasure_returns_no_QualityGateStatus_if_alertStatus_has_data_in_wrong_case_for_Level_Metric() {
    Optional<Measure> measure = toMeasure(new ProjectMeasureDto().setData("waRn"), SOME_STRING_METRIC);
    assertThat(measure).isPresent();
    assertThat(measure.get().hasQualityGateStatus()).isFalse();
  }

  @Test
  public void toMeasure_returns_value_for_LEVEL_Metric() {
    for (Level level : Level.values()) {
      verify_toMeasure_returns_value_for_LEVEL_Metric(level);
    }
  }

  private void verify_toMeasure_returns_value_for_LEVEL_Metric(Level expectedLevel) {
    Optional<Measure> measure = toMeasure(new ProjectMeasureDto().setData(expectedLevel.name()), SOME_LEVEL_METRIC);
    assertThat(measure).isPresent();
    assertThat(measure.get().getValueType()).isEqualTo(ValueType.LEVEL);
    assertThat(measure.get().getLevelValue()).isEqualTo(expectedLevel);
  }

  @Test
  public void toMeasure_for_LEVEL_Metric_can_have_an_qualityGateStatus() {
    ProjectMeasureDto projectMeasureDto = new ProjectMeasureDto().setData(Level.OK.name()).setAlertStatus(Level.ERROR.name()).setAlertText(SOME_ALERT_TEXT);

    Optional<Measure> measure = toMeasure(projectMeasureDto, SOME_LEVEL_METRIC);

    assertThat(measure).isPresent();
    assertThat(measure.get().getValueType()).isEqualTo(ValueType.LEVEL);
    assertThat(measure.get().getLevelValue()).isEqualTo(Level.OK);
    assertThat(measure.get().getQualityGateStatus().getStatus()).isEqualTo(Level.ERROR);
    assertThat(measure.get().getQualityGateStatus().getText()).isEqualTo(SOME_ALERT_TEXT);
  }

  @Test
  public void toMeasure_for_LEVEL_Metric_ignores_data() {
    ProjectMeasureDto projectMeasureDto = new ProjectMeasureDto().setAlertStatus(Level.ERROR.name()).setData(SOME_DATA);

    Optional<Measure> measure = toMeasure(projectMeasureDto, SOME_LEVEL_METRIC);

    assertThat(measure).isPresent();

    assertThatThrownBy(() ->measure.get().getStringValue())
      .isInstanceOf(IllegalStateException.class);
  }

  @Test
  public void toMeasure_returns_no_value_if_dto_has_no_value_for_Int_Metric() {
    Optional<Measure> measure = toMeasure(EMPTY_MEASURE_DTO, SOME_INT_METRIC);

    assertThat(measure).isPresent();
    assertThat(measure.get().getValueType()).isEqualTo(ValueType.NO_VALUE);
  }

  @Test
  public void toMeasure_returns_int_part_of_value_in_dto_for_Int_Metric() {
    Optional<Measure> measure = toMeasure(new ProjectMeasureDto().setValue(1.5d), SOME_INT_METRIC);

    assertThat(measure).isPresent();
    assertThat(measure.get().getValueType()).isEqualTo(ValueType.INT);
    assertThat(measure.get().getIntValue()).isOne();
  }

  @Test
  public void toMeasure_maps_data_and_alert_properties_in_dto_for_Int_Metric() {
    ProjectMeasureDto projectMeasureDto = new ProjectMeasureDto().setValue(10d).setData(SOME_DATA).setAlertStatus(Level.OK.name()).setAlertText(SOME_ALERT_TEXT);

    Optional<Measure> measure = toMeasure(projectMeasureDto, SOME_INT_METRIC);

    assertThat(measure).isPresent();
    assertThat(measure.get().getValueType()).isEqualTo(ValueType.INT);
    assertThat(measure.get().getIntValue()).isEqualTo(10);
    assertThat(measure.get().getData()).isEqualTo(SOME_DATA);
    assertThat(measure.get().getQualityGateStatus().getStatus()).isEqualTo(Level.OK);
    assertThat(measure.get().getQualityGateStatus().getText()).isEqualTo(SOME_ALERT_TEXT);
  }

  @Test
  public void toMeasure_returns_no_value_if_dto_has_no_value_for_Long_Metric() {
    Optional<Measure> measure = toMeasure(EMPTY_MEASURE_DTO, SOME_LONG_METRIC);

    assertThat(measure).isPresent();
    assertThat(measure.get().getValueType()).isEqualTo(ValueType.NO_VALUE);
  }

  @Test
  public void toMeasure_returns_long_part_of_value_in_dto_for_Long_Metric() {
    Optional<Measure> measure = toMeasure(new ProjectMeasureDto().setValue(1.5d), SOME_LONG_METRIC);

    assertThat(measure).isPresent();
    assertThat(measure.get().getValueType()).isEqualTo(ValueType.LONG);
    assertThat(measure.get().getLongValue()).isOne();
  }

  @Test
  public void toMeasure_maps_data_and_alert_properties_in_dto_for_Long_Metric() {
    ProjectMeasureDto projectMeasureDto = new ProjectMeasureDto().setValue(10d).setData(SOME_DATA).setAlertStatus(Level.OK.name()).setAlertText(SOME_ALERT_TEXT);

    Optional<Measure> measure = toMeasure(projectMeasureDto, SOME_LONG_METRIC);

    assertThat(measure).isPresent();
    assertThat(measure.get().getValueType()).isEqualTo(ValueType.LONG);
    assertThat(measure.get().getLongValue()).isEqualTo(10);
    assertThat(measure.get().getData()).isEqualTo(SOME_DATA);
    assertThat(measure.get().getQualityGateStatus().getStatus()).isEqualTo(Level.OK);
    assertThat(measure.get().getQualityGateStatus().getText()).isEqualTo(SOME_ALERT_TEXT);
  }

  @Test
  public void toMeasure_returns_no_value_if_dto_has_no_value_for_Double_Metric() {
    Optional<Measure> measure = toMeasure(EMPTY_MEASURE_DTO, SOME_DOUBLE_METRIC);

    assertThat(measure).isPresent();
    assertThat(measure.get().getValueType()).isEqualTo(ValueType.NO_VALUE);
  }

  @Test
  public void toMeasure_maps_data_and_alert_properties_in_dto_for_Double_Metric() {
    ProjectMeasureDto projectMeasureDto = new ProjectMeasureDto().setValue(10.6395d).setData(SOME_DATA).setAlertStatus(Level.OK.name()).setAlertText(SOME_ALERT_TEXT);

    Optional<Measure> measure = toMeasure(projectMeasureDto, SOME_DOUBLE_METRIC);

    assertThat(measure).isPresent();
    assertThat(measure.get().getValueType()).isEqualTo(ValueType.DOUBLE);
    assertThat(measure.get().getDoubleValue()).isEqualTo(10.6395d);
    assertThat(measure.get().getData()).isEqualTo(SOME_DATA);
    assertThat(measure.get().getQualityGateStatus().getStatus()).isEqualTo(Level.OK);
    assertThat(measure.get().getQualityGateStatus().getText()).isEqualTo(SOME_ALERT_TEXT);
  }

  @Test
  public void toMeasure_returns_no_value_if_dto_has_no_value_for_Boolean_metric() {
    Optional<Measure> measure = toMeasure(EMPTY_MEASURE_DTO, SOME_BOOLEAN_METRIC);

    assertThat(measure).isPresent();
    assertThat(measure.get().getValueType()).isEqualTo(ValueType.NO_VALUE);
  }

  @Test
  public void toMeasure_returns_false_value_if_dto_has_invalid_value_for_Boolean_metric() {
    Optional<Measure> measure = toMeasure(new ProjectMeasureDto().setValue(1.987d), SOME_BOOLEAN_METRIC);

    assertThat(measure).isPresent();
    assertThat(measure.get().getValueType()).isEqualTo(ValueType.BOOLEAN);
    assertThat(measure.get().getBooleanValue()).isFalse();
  }

  @Test
  public void toMeasure_maps_data_and_alert_properties_in_dto_for_Boolean_metric() {
    ProjectMeasureDto projectMeasureDto = new ProjectMeasureDto().setValue(1d).setData(SOME_DATA).setAlertStatus(Level.OK.name()).setAlertText(SOME_ALERT_TEXT);

    Optional<Measure> measure = toMeasure(projectMeasureDto, SOME_BOOLEAN_METRIC);

    assertThat(measure).isPresent();
    assertThat(measure.get().getValueType()).isEqualTo(ValueType.BOOLEAN);
    assertThat(measure.get().getBooleanValue()).isTrue();
    assertThat(measure.get().getData()).isEqualTo(SOME_DATA);
    assertThat(measure.get().getQualityGateStatus().getStatus()).isEqualTo(Level.OK);
    assertThat(measure.get().getQualityGateStatus().getText()).isEqualTo(SOME_ALERT_TEXT);
  }

  @Test
  public void toMeasure_returns_no_value_if_dto_has_no_value_for_String_Metric() {
    Optional<Measure> measure = toMeasure(EMPTY_MEASURE_DTO, SOME_STRING_METRIC);

    assertThat(measure).isPresent();
    assertThat(measure.get().getValueType()).isEqualTo(ValueType.NO_VALUE);
  }

  @Test
  public void toMeasure_maps_alert_properties_in_dto_for_String_Metric() {
    ProjectMeasureDto projectMeasureDto = new ProjectMeasureDto().setData(SOME_DATA).setAlertStatus(Level.OK.name()).setAlertText(SOME_ALERT_TEXT);

    Optional<Measure> measure = toMeasure(projectMeasureDto, SOME_STRING_METRIC);

    assertThat(measure).isPresent();
    assertThat(measure.get().getValueType()).isEqualTo(ValueType.STRING);
    assertThat(measure.get().getStringValue()).isEqualTo(SOME_DATA);
    assertThat(measure.get().getData()).isEqualTo(SOME_DATA);
    assertThat(measure.get().getQualityGateStatus().getStatus()).isEqualTo(Level.OK);
    assertThat(measure.get().getQualityGateStatus().getText()).isEqualTo(SOME_ALERT_TEXT);
  }

  @DataProvider
  public static Object[][] all_types_MeasureDtos() {
    return new Object[][] {
      {new ProjectMeasureDto().setValue(1d), SOME_BOOLEAN_METRIC},
      {new ProjectMeasureDto().setValue(1d), SOME_INT_METRIC},
      {new ProjectMeasureDto().setValue(1d), SOME_LONG_METRIC},
      {new ProjectMeasureDto().setValue(1d), SOME_DOUBLE_METRIC},
      {new ProjectMeasureDto().setData("1"), SOME_STRING_METRIC},
      {new ProjectMeasureDto().setData(Measure.Level.OK.name()), SOME_LEVEL_METRIC}
    };
  }

  @Test
  public void toMeasure_should_not_loose_decimals_of_float_values() {
    MetricImpl metric = new MetricImpl("42", "double", "name", Metric.MetricType.FLOAT, 5, null, false, false);
    ProjectMeasureDto projectMeasureDto = new ProjectMeasureDto()
      .setValue(0.12345);

    Optional<Measure> measure = toMeasure(projectMeasureDto, metric);

    assertThat(measure.get().getDoubleValue()).isEqualTo(0.12345, Offset.offset(0.000001));
  }
}
