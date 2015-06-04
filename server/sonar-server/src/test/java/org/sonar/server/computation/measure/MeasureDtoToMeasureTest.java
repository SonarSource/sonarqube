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
package org.sonar.server.computation.measure;

import com.google.common.base.Optional;
import java.io.Serializable;
import org.junit.Test;
import org.sonar.api.measures.Metric;
import org.sonar.core.measure.db.MeasureDto;
import org.sonar.server.computation.measure.Measure.AlertStatus;

import static org.assertj.guava.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

public class MeasureDtoToMeasureTest {
  private static final Metric<Serializable> SOME_INT_METRIC = new Metric.Builder("key", "name", Metric.ValueType.INT).create();
  private static final Metric<Serializable> SOME_LONG_METRIC = new Metric.Builder("key", "name", Metric.ValueType.WORK_DUR).create();
  private static final Metric<Serializable> SOME_DOUBLE_METRIC = new Metric.Builder("key", "name", Metric.ValueType.FLOAT).create();
  private static final Metric<Serializable> SOME_STRING_METRIC = new Metric.Builder("key", "name", Metric.ValueType.STRING).create();
  private static final Metric<Serializable> SOME_BOOLEAN_METRIC = new Metric.Builder("key", "name", Metric.ValueType.BOOL).create();
  private static final Metric<Serializable> SOME_LEVEL_METRIC = new Metric.Builder("key", "name", Metric.ValueType.LEVEL).create();

  private static final String SOME_DATA = "some_data man!";
  private static final String SOME_ALERT_TEXT = "some alert text_be_careFul!";
  private static final MeasureDto EMPTY_MEASURE_DTO = new MeasureDto();

  private MeasureDtoToMeasure underTest = new MeasureDtoToMeasure();

  @Test
  public void toMeasure_returns_absent_for_null_argument() {
    assertThat(underTest.toMeasure(null, SOME_INT_METRIC)).isAbsent();
  }

  @Test(expected = NullPointerException.class)
  public void toMeasure_throws_NPE_if_metric_argument_is_null() {
    underTest.toMeasure(EMPTY_MEASURE_DTO, null);
  }

  @Test(expected = NullPointerException.class)
  public void toMeasure_throws_NPE_if_both_arguments_are_null() {
    underTest.toMeasure(null, null);
  }

  @Test
  public void toMeasure_returns_no_value_if_dto_has_no_alertStatus_for_Level_Metric() {
    Optional<Measure> measure = underTest.toMeasure(EMPTY_MEASURE_DTO, SOME_LEVEL_METRIC);
    assertThat(measure).isPresent();
    assertThat(measure.get().getValueType()).isEqualTo(Measure.ValueType.NO_VALUE);
    assertThat(measure.get().hasAlertStatus()).isFalse();
  }

  @Test
  public void toMeasure_returns_no_value_if_dto_has_invalid_alertStatus_for_Level_Metric() {
    Optional<Measure> measure = underTest.toMeasure(new MeasureDto().setAlertStatus("trololo"), SOME_LEVEL_METRIC);
    assertThat(measure).isPresent();
    assertThat(measure.get().getValueType()).isEqualTo(Measure.ValueType.NO_VALUE);
    assertThat(measure.get().hasAlertStatus()).isFalse();
  }

  @Test
  public void toMeasure_returns_no_value_if_dta_has_value_in_wrong_case_for_Level_Metric() {
    Optional<Measure> measure = underTest.toMeasure(new MeasureDto().setAlertStatus("waRn"), SOME_LEVEL_METRIC);
    assertThat(measure).isPresent();
    assertThat(measure.get().getValueType()).isEqualTo(Measure.ValueType.NO_VALUE);
    assertThat(measure.get().hasAlertStatus()).isFalse();
  }

  @Test
  public void toMeasure_returns_value_for_Level_Metric() {
    for (AlertStatus alertStatus : AlertStatus.values()) {
      verify_toMeasure_returns_value_for_Level_Metric(alertStatus);
    }
  }

  private void verify_toMeasure_returns_value_for_Level_Metric(AlertStatus expectedAlertStatus) {
    Optional<Measure> measure = underTest.toMeasure(new MeasureDto().setAlertStatus(expectedAlertStatus.name()), SOME_LEVEL_METRIC);
    assertThat(measure).isPresent();
    assertThat(measure.get().getValueType()).isEqualTo(Measure.ValueType.NO_VALUE);
    assertThat(measure.get().getAlertStatus()).isEqualTo(expectedAlertStatus);
  }

  @Test
  public void toMeasure_for_Level_Metric_maps_alertText() {
    MeasureDto measureDto = new MeasureDto().setAlertStatus(AlertStatus.ERROR.name()).setAlertText(SOME_ALERT_TEXT);

    Optional<Measure> measure = underTest.toMeasure(measureDto, SOME_LEVEL_METRIC);

    assertThat(measure).isPresent();
    assertThat(measure.get().getValueType()).isEqualTo(Measure.ValueType.NO_VALUE);
    assertThat(measure.get().getAlertStatus()).isEqualTo(AlertStatus.ERROR);
    assertThat(measure.get().getAlertText()).isEqualTo(SOME_ALERT_TEXT);
  }

  @Test(expected = IllegalStateException.class)
  public void toMeasure_for_Level_Metric_ignores_data() {
    MeasureDto measureDto = new MeasureDto().setAlertStatus(AlertStatus.ERROR.name()).setData(SOME_DATA);

    Optional<Measure> measure = underTest.toMeasure(measureDto, SOME_LEVEL_METRIC);

    assertThat(measure).isPresent();
    measure.get().getStringValue();
  }

  @Test
  public void toMeasure_returns_no_value_if_dto_has_no_value_for_Int_Metric() {
    Optional<Measure> measure = underTest.toMeasure(EMPTY_MEASURE_DTO, SOME_INT_METRIC);

    assertThat(measure.isPresent());
    assertThat(measure.get().getValueType()).isEqualTo(Measure.ValueType.NO_VALUE);
  }

  @Test
  public void toMeasure_returns_int_part_of_value_in_dto_for_Int_Metric() {
    Optional<Measure> measure = underTest.toMeasure(new MeasureDto().setValue(1.5d), SOME_INT_METRIC);

    assertThat(measure.isPresent());
    assertThat(measure.get().getValueType()).isEqualTo(Measure.ValueType.INT);
    assertThat(measure.get().getIntValue()).isEqualTo(1);
  }

  @Test
  public void toMeasure_maps_data_and_alert_properties_in_dto_for_Int_Metric() {
    MeasureDto measureDto = new MeasureDto().setValue(10d).setData(SOME_DATA).setAlertStatus(AlertStatus.OK.name()).setAlertText(SOME_ALERT_TEXT);

    Optional<Measure> measure = underTest.toMeasure(measureDto, SOME_INT_METRIC);

    assertThat(measure.isPresent());
    assertThat(measure.get().getValueType()).isEqualTo(Measure.ValueType.INT);
    assertThat(measure.get().getIntValue()).isEqualTo(10);
    assertThat(measure.get().getData()).isEqualTo(SOME_DATA);
    assertThat(measure.get().getAlertStatus()).isEqualTo(AlertStatus.OK);
    assertThat(measure.get().getAlertText()).isEqualTo(SOME_ALERT_TEXT);
  }

  @Test
  public void toMeasure_returns_no_value_if_dto_has_no_value_for_Long_Metric() {
    Optional<Measure> measure = underTest.toMeasure(EMPTY_MEASURE_DTO, SOME_LONG_METRIC);

    assertThat(measure.isPresent());
    assertThat(measure.get().getValueType()).isEqualTo(Measure.ValueType.NO_VALUE);
  }

  @Test
  public void toMeasure_returns_long_part_of_value_in_dto_for_Long_Metric() {
    Optional<Measure> measure = underTest.toMeasure(new MeasureDto().setValue(1.5d), SOME_LONG_METRIC);

    assertThat(measure.isPresent());
    assertThat(measure.get().getValueType()).isEqualTo(Measure.ValueType.LONG);
    assertThat(measure.get().getLongValue()).isEqualTo(1);
  }

  @Test
  public void toMeasure_maps_data_and_alert_properties_in_dto_for_Long_Metric() {
    MeasureDto measureDto = new MeasureDto().setValue(10d).setData(SOME_DATA).setAlertStatus(AlertStatus.OK.name()).setAlertText(SOME_ALERT_TEXT);

    Optional<Measure> measure = underTest.toMeasure(measureDto, SOME_LONG_METRIC);

    assertThat(measure.isPresent());
    assertThat(measure.get().getValueType()).isEqualTo(Measure.ValueType.LONG);
    assertThat(measure.get().getLongValue()).isEqualTo(10);
    assertThat(measure.get().getData()).isEqualTo(SOME_DATA);
    assertThat(measure.get().getAlertStatus()).isEqualTo(AlertStatus.OK);
    assertThat(measure.get().getAlertText()).isEqualTo(SOME_ALERT_TEXT);
  }

  @Test
  public void toMeasure_returns_no_value_if_dto_has_no_value_for_Double_Metric() {
    Optional<Measure> measure = underTest.toMeasure(EMPTY_MEASURE_DTO, SOME_DOUBLE_METRIC);

    assertThat(measure.isPresent());
    assertThat(measure.get().getValueType()).isEqualTo(Measure.ValueType.NO_VALUE);
  }

  @Test
  public void toMeasure_maps_data_and_alert_properties_in_dto_for_Double_Metric() {
    MeasureDto measureDto = new MeasureDto().setValue(10.6395d).setData(SOME_DATA).setAlertStatus(AlertStatus.OK.name()).setAlertText(SOME_ALERT_TEXT);

    Optional<Measure> measure = underTest.toMeasure(measureDto, SOME_DOUBLE_METRIC);

    assertThat(measure.isPresent());
    assertThat(measure.get().getValueType()).isEqualTo(Measure.ValueType.DOUBLE);
    assertThat(measure.get().getDoubleValue()).isEqualTo(10.6395d);
    assertThat(measure.get().getData()).isEqualTo(SOME_DATA);
    assertThat(measure.get().getAlertStatus()).isEqualTo(AlertStatus.OK);
    assertThat(measure.get().getAlertText()).isEqualTo(SOME_ALERT_TEXT);
  }

  @Test
  public void toMeasure_returns_no_value_if_dto_has_no_value_for_Boolean_metric() {
    Optional<Measure> measure = underTest.toMeasure(EMPTY_MEASURE_DTO, SOME_BOOLEAN_METRIC);

    assertThat(measure.isPresent());
    assertThat(measure.get().getValueType()).isEqualTo(Measure.ValueType.NO_VALUE);
  }

  @Test
  public void toMeasure_returns_false_value_if_dto_has_invalid_value_for_Boolean_metric() {
    Optional<Measure> measure = underTest.toMeasure(new MeasureDto().setValue(1.987d), SOME_BOOLEAN_METRIC);

    assertThat(measure.isPresent());
    assertThat(measure.get().getValueType()).isEqualTo(Measure.ValueType.BOOLEAN);
    assertThat(measure.get().getBooleanValue()).isFalse();
  }

  @Test
  public void toMeasure_maps_data_and_alert_properties_in_dto_for_Boolean_metric() {
    MeasureDto measureDto = new MeasureDto().setValue(1d).setData(SOME_DATA).setAlertStatus(AlertStatus.OK.name()).setAlertText(SOME_ALERT_TEXT);

    Optional<Measure> measure = underTest.toMeasure(measureDto, SOME_BOOLEAN_METRIC);

    assertThat(measure.isPresent());
    assertThat(measure.get().getValueType()).isEqualTo(Measure.ValueType.BOOLEAN);
    assertThat(measure.get().getBooleanValue()).isTrue();
    assertThat(measure.get().getData()).isEqualTo(SOME_DATA);
    assertThat(measure.get().getAlertStatus()).isEqualTo(AlertStatus.OK);
    assertThat(measure.get().getAlertText()).isEqualTo(SOME_ALERT_TEXT);
  }

  @Test
  public void toMeasure_returns_no_value_if_dto_has_no_value_for_String_Metric() {
    Optional<Measure> measure = underTest.toMeasure(EMPTY_MEASURE_DTO, SOME_STRING_METRIC);

    assertThat(measure.isPresent());
    assertThat(measure.get().getValueType()).isEqualTo(Measure.ValueType.NO_VALUE);
  }

  @Test
  public void toMeasure_maps_alert_properties_in_dto_for_String_Metric() {
    MeasureDto measureDto = new MeasureDto().setData(SOME_DATA).setAlertStatus(AlertStatus.OK.name()).setAlertText(SOME_ALERT_TEXT);

    Optional<Measure> measure = underTest.toMeasure(measureDto, SOME_STRING_METRIC);

    assertThat(measure.isPresent());
    assertThat(measure.get().getValueType()).isEqualTo(Measure.ValueType.STRING);
    assertThat(measure.get().getStringValue()).isEqualTo(SOME_DATA);
    assertThat(measure.get().getData()).isEqualTo(SOME_DATA);
    assertThat(measure.get().getAlertStatus()).isEqualTo(AlertStatus.OK);
    assertThat(measure.get().getAlertText()).isEqualTo(SOME_ALERT_TEXT);
  }
}
