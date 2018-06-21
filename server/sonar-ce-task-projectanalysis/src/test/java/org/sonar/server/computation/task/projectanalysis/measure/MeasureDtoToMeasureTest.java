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
package org.sonar.server.computation.task.projectanalysis.measure;

import com.google.common.base.Optional;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.assertj.core.data.Offset;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.sonar.db.measure.MeasureDto;
import org.sonar.server.computation.task.projectanalysis.measure.Measure.Level;
import org.sonar.server.computation.task.projectanalysis.metric.Metric;
import org.sonar.server.computation.task.projectanalysis.metric.MetricImpl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.guava.api.Assertions.assertThat;

@RunWith(DataProviderRunner.class)
public class MeasureDtoToMeasureTest {
  private static final Metric SOME_INT_METRIC = new MetricImpl(42, "int", "name", Metric.MetricType.INT);
  private static final Metric SOME_LONG_METRIC = new MetricImpl(42, "long", "name", Metric.MetricType.WORK_DUR);
  private static final Metric SOME_DOUBLE_METRIC = new MetricImpl(42, "double", "name", Metric.MetricType.FLOAT);
  private static final Metric SOME_STRING_METRIC = new MetricImpl(42, "string", "name", Metric.MetricType.STRING);
  private static final Metric SOME_BOOLEAN_METRIC = new MetricImpl(42, "boolean", "name", Metric.MetricType.BOOL);
  private static final Metric SOME_LEVEL_METRIC = new MetricImpl(42, "level", "name", Metric.MetricType.LEVEL);

  private static final String SOME_DATA = "some_data man!";
  private static final String SOME_ALERT_TEXT = "some alert text_be_careFul!";
  private static final MeasureDto EMPTY_MEASURE_DTO = new MeasureDto();

  @Rule
  public final ExpectedException expectedException = ExpectedException.none();

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
  public void toMeasure_returns_no_value_if_dto_has_no_data_for_Level_Metric() {
    Optional<Measure> measure = underTest.toMeasure(EMPTY_MEASURE_DTO, SOME_LEVEL_METRIC);
    assertThat(measure).isPresent();
    assertThat(measure.get().getValueType()).isEqualTo(Measure.ValueType.NO_VALUE);
  }

  @Test
  public void toMeasure_returns_no_value_if_dto_has_invalid_data_for_Level_Metric() {
    Optional<Measure> measure = underTest.toMeasure(new MeasureDto().setData("trololo"), SOME_LEVEL_METRIC);
    assertThat(measure).isPresent();
    assertThat(measure.get().getValueType()).isEqualTo(Measure.ValueType.NO_VALUE);
  }

  @Test
  public void toMeasure_returns_no_value_if_dta_has_data_in_wrong_case_for_Level_Metric() {
    Optional<Measure> measure = underTest.toMeasure(new MeasureDto().setData("waRn"), SOME_LEVEL_METRIC);
    assertThat(measure).isPresent();
    assertThat(measure.get().getValueType()).isEqualTo(Measure.ValueType.NO_VALUE);
  }

  @Test
  public void toMeasure_returns_no_QualityGateStatus_if_dto_has_no_alertStatus_for_Level_Metric() {
    Optional<Measure> measure = underTest.toMeasure(EMPTY_MEASURE_DTO, SOME_STRING_METRIC);
    assertThat(measure).isPresent();
    assertThat(measure.get().hasQualityGateStatus()).isFalse();
  }

  @Test
  public void toMeasure_returns_no_QualityGateStatus_if_alertStatus_has_invalid_data_for_Level_Metric() {
    Optional<Measure> measure = underTest.toMeasure(new MeasureDto().setData("trololo"), SOME_STRING_METRIC);
    assertThat(measure).isPresent();
    assertThat(measure.get().hasQualityGateStatus()).isFalse();
  }

  @Test
  public void toMeasure_returns_no_QualityGateStatus_if_alertStatus_has_data_in_wrong_case_for_Level_Metric() {
    Optional<Measure> measure = underTest.toMeasure(new MeasureDto().setData("waRn"), SOME_STRING_METRIC);
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
    Optional<Measure> measure = underTest.toMeasure(new MeasureDto().setData(expectedLevel.name()), SOME_LEVEL_METRIC);
    assertThat(measure).isPresent();
    assertThat(measure.get().getValueType()).isEqualTo(Measure.ValueType.LEVEL);
    assertThat(measure.get().getLevelValue()).isEqualTo(expectedLevel);
  }

  @Test
  public void toMeasure_for_LEVEL_Metric_can_have_an_qualityGateStatus() {
    MeasureDto measureDto = new MeasureDto().setData(Level.OK.name()).setAlertStatus(Level.ERROR.name()).setAlertText(SOME_ALERT_TEXT);

    Optional<Measure> measure = underTest.toMeasure(measureDto, SOME_LEVEL_METRIC);

    assertThat(measure).isPresent();
    assertThat(measure.get().getValueType()).isEqualTo(Measure.ValueType.LEVEL);
    assertThat(measure.get().getLevelValue()).isEqualTo(Level.OK);
    assertThat(measure.get().getQualityGateStatus().getStatus()).isEqualTo(Level.ERROR);
    assertThat(measure.get().getQualityGateStatus().getText()).isEqualTo(SOME_ALERT_TEXT);
  }

  @Test(expected = IllegalStateException.class)
  public void toMeasure_for_LEVEL_Metric_ignores_data() {
    MeasureDto measureDto = new MeasureDto().setAlertStatus(Level.ERROR.name()).setData(SOME_DATA);

    Optional<Measure> measure = underTest.toMeasure(measureDto, SOME_LEVEL_METRIC);

    assertThat(measure).isPresent();
    measure.get().getStringValue();
  }

  @Test
  public void toMeasure_returns_no_value_if_dto_has_no_value_for_Int_Metric() {
    Optional<Measure> measure = underTest.toMeasure(EMPTY_MEASURE_DTO, SOME_INT_METRIC);

    assertThat(measure.isPresent()).isTrue();
    assertThat(measure.get().getValueType()).isEqualTo(Measure.ValueType.NO_VALUE);
  }

  @Test
  public void toMeasure_returns_int_part_of_value_in_dto_for_Int_Metric() {
    Optional<Measure> measure = underTest.toMeasure(new MeasureDto().setValue(1.5d), SOME_INT_METRIC);

    assertThat(measure.isPresent()).isTrue();
    assertThat(measure.get().getValueType()).isEqualTo(Measure.ValueType.INT);
    assertThat(measure.get().getIntValue()).isEqualTo(1);
  }

  @Test
  public void toMeasure_maps_data_and_alert_properties_in_dto_for_Int_Metric() {
    MeasureDto measureDto = new MeasureDto().setValue(10d).setData(SOME_DATA).setAlertStatus(Level.OK.name()).setAlertText(SOME_ALERT_TEXT);

    Optional<Measure> measure = underTest.toMeasure(measureDto, SOME_INT_METRIC);

    assertThat(measure.isPresent()).isTrue();
    assertThat(measure.get().getValueType()).isEqualTo(Measure.ValueType.INT);
    assertThat(measure.get().getIntValue()).isEqualTo(10);
    assertThat(measure.get().getData()).isEqualTo(SOME_DATA);
    assertThat(measure.get().getQualityGateStatus().getStatus()).isEqualTo(Level.OK);
    assertThat(measure.get().getQualityGateStatus().getText()).isEqualTo(SOME_ALERT_TEXT);
  }

  @Test
  public void toMeasure_returns_no_value_if_dto_has_no_value_for_Long_Metric() {
    Optional<Measure> measure = underTest.toMeasure(EMPTY_MEASURE_DTO, SOME_LONG_METRIC);

    assertThat(measure.isPresent()).isTrue();
    assertThat(measure.get().getValueType()).isEqualTo(Measure.ValueType.NO_VALUE);
  }

  @Test
  public void toMeasure_returns_long_part_of_value_in_dto_for_Long_Metric() {
    Optional<Measure> measure = underTest.toMeasure(new MeasureDto().setValue(1.5d), SOME_LONG_METRIC);

    assertThat(measure.isPresent()).isTrue();
    assertThat(measure.get().getValueType()).isEqualTo(Measure.ValueType.LONG);
    assertThat(measure.get().getLongValue()).isEqualTo(1);
  }

  @Test
  public void toMeasure_maps_data_and_alert_properties_in_dto_for_Long_Metric() {
    MeasureDto measureDto = new MeasureDto().setValue(10d).setData(SOME_DATA).setAlertStatus(Level.OK.name()).setAlertText(SOME_ALERT_TEXT);

    Optional<Measure> measure = underTest.toMeasure(measureDto, SOME_LONG_METRIC);

    assertThat(measure.isPresent()).isTrue();
    assertThat(measure.get().getValueType()).isEqualTo(Measure.ValueType.LONG);
    assertThat(measure.get().getLongValue()).isEqualTo(10);
    assertThat(measure.get().getData()).isEqualTo(SOME_DATA);
    assertThat(measure.get().getQualityGateStatus().getStatus()).isEqualTo(Level.OK);
    assertThat(measure.get().getQualityGateStatus().getText()).isEqualTo(SOME_ALERT_TEXT);
  }

  @Test
  public void toMeasure_returns_no_value_if_dto_has_no_value_for_Double_Metric() {
    Optional<Measure> measure = underTest.toMeasure(EMPTY_MEASURE_DTO, SOME_DOUBLE_METRIC);

    assertThat(measure.isPresent()).isTrue();
    assertThat(measure.get().getValueType()).isEqualTo(Measure.ValueType.NO_VALUE);
  }

  @Test
  public void toMeasure_maps_data_and_alert_properties_in_dto_for_Double_Metric() {
    MeasureDto measureDto = new MeasureDto().setValue(10.6395d).setData(SOME_DATA).setAlertStatus(Level.OK.name()).setAlertText(SOME_ALERT_TEXT);

    Optional<Measure> measure = underTest.toMeasure(measureDto, SOME_DOUBLE_METRIC);

    assertThat(measure.isPresent()).isTrue();
    assertThat(measure.get().getValueType()).isEqualTo(Measure.ValueType.DOUBLE);
    assertThat(measure.get().getDoubleValue()).isEqualTo(10.6395d);
    assertThat(measure.get().getData()).isEqualTo(SOME_DATA);
    assertThat(measure.get().getQualityGateStatus().getStatus()).isEqualTo(Level.OK);
    assertThat(measure.get().getQualityGateStatus().getText()).isEqualTo(SOME_ALERT_TEXT);
  }

  @Test
  public void toMeasure_returns_no_value_if_dto_has_no_value_for_Boolean_metric() {
    Optional<Measure> measure = underTest.toMeasure(EMPTY_MEASURE_DTO, SOME_BOOLEAN_METRIC);

    assertThat(measure.isPresent()).isTrue();
    assertThat(measure.get().getValueType()).isEqualTo(Measure.ValueType.NO_VALUE);
  }

  @Test
  public void toMeasure_returns_false_value_if_dto_has_invalid_value_for_Boolean_metric() {
    Optional<Measure> measure = underTest.toMeasure(new MeasureDto().setValue(1.987d), SOME_BOOLEAN_METRIC);

    assertThat(measure.isPresent()).isTrue();
    assertThat(measure.get().getValueType()).isEqualTo(Measure.ValueType.BOOLEAN);
    assertThat(measure.get().getBooleanValue()).isFalse();
  }

  @Test
  public void toMeasure_maps_data_and_alert_properties_in_dto_for_Boolean_metric() {
    MeasureDto measureDto = new MeasureDto().setValue(1d).setData(SOME_DATA).setAlertStatus(Level.OK.name()).setAlertText(SOME_ALERT_TEXT);

    Optional<Measure> measure = underTest.toMeasure(measureDto, SOME_BOOLEAN_METRIC);

    assertThat(measure.isPresent()).isTrue();
    assertThat(measure.get().getValueType()).isEqualTo(Measure.ValueType.BOOLEAN);
    assertThat(measure.get().getBooleanValue()).isTrue();
    assertThat(measure.get().getData()).isEqualTo(SOME_DATA);
    assertThat(measure.get().getQualityGateStatus().getStatus()).isEqualTo(Level.OK);
    assertThat(measure.get().getQualityGateStatus().getText()).isEqualTo(SOME_ALERT_TEXT);
  }

  @Test
  public void toMeasure_returns_no_value_if_dto_has_no_value_for_String_Metric() {
    Optional<Measure> measure = underTest.toMeasure(EMPTY_MEASURE_DTO, SOME_STRING_METRIC);

    assertThat(measure.isPresent()).isTrue();
    assertThat(measure.get().getValueType()).isEqualTo(Measure.ValueType.NO_VALUE);
  }

  @Test
  public void toMeasure_maps_alert_properties_in_dto_for_String_Metric() {
    MeasureDto measureDto = new MeasureDto().setData(SOME_DATA).setAlertStatus(Level.OK.name()).setAlertText(SOME_ALERT_TEXT);

    Optional<Measure> measure = underTest.toMeasure(measureDto, SOME_STRING_METRIC);

    assertThat(measure.isPresent()).isTrue();
    assertThat(measure.get().getValueType()).isEqualTo(Measure.ValueType.STRING);
    assertThat(measure.get().getStringValue()).isEqualTo(SOME_DATA);
    assertThat(measure.get().getData()).isEqualTo(SOME_DATA);
    assertThat(measure.get().getQualityGateStatus().getStatus()).isEqualTo(Level.OK);
    assertThat(measure.get().getQualityGateStatus().getText()).isEqualTo(SOME_ALERT_TEXT);
  }

  @DataProvider
  public static Object[][] all_types_MeasureDtos() {
    return new Object[][] {
      {new MeasureDto().setValue(1d), SOME_BOOLEAN_METRIC},
      {new MeasureDto().setValue(1d), SOME_INT_METRIC},
      {new MeasureDto().setValue(1d), SOME_LONG_METRIC},
      {new MeasureDto().setValue(1d), SOME_DOUBLE_METRIC},
      {new MeasureDto().setData("1"), SOME_STRING_METRIC},
      {new MeasureDto().setData(Measure.Level.OK.name()), SOME_LEVEL_METRIC}
    };
  }

  @Test
  @UseDataProvider("all_types_MeasureDtos")
  public void toMeasure_creates_no_MeasureVariation_if_dto_has_none_whichever_the_ValueType(MeasureDto measureDto, Metric metric) {
    assertThat(underTest.toMeasure(measureDto, metric).get().hasVariation()).isFalse();
  }

  @Test
  @UseDataProvider("all_types_MeasureDtos")
  public void toMeasure_creates_MeasureVariation_and_maps_the_right_one(MeasureDto builder, Metric metric) {
    assertThat(underTest.toMeasure(builder.setVariation(1d), metric).get().getVariation()).isEqualTo(1);
  }

  @Test
  public void toMeasure_creates_MeasureVariation_and_maps_the_right_one() {
    MeasureDto measureDto = new MeasureDto()
      .setData("1")
      .setVariation(2d);

    Optional<Measure> measure = underTest.toMeasure(measureDto, SOME_STRING_METRIC);

    assertThat(measure.get().getVariation()).isEqualTo(2);
  }

  @Test
  public void toMeasure_should_not_loose_decimals_of_float_values() {
    MetricImpl metric = new MetricImpl(42, "double", "name", Metric.MetricType.FLOAT, 5, null, false);
    MeasureDto measureDto = new MeasureDto()
      .setValue(0.12345);

    Optional<Measure> measure = underTest.toMeasure(measureDto, metric);

    assertThat(measure.get().getDoubleValue()).isEqualTo(0.12345, Offset.offset(0.000001));
  }
}
