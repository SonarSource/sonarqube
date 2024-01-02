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

import com.google.common.collect.ImmutableList;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sonar.ce.task.projectanalysis.measure.Measure.ValueType;
import org.sonar.ce.task.projectanalysis.util.WrapInSingleElementArray;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.sonar.ce.task.projectanalysis.measure.Measure.newMeasureBuilder;

@RunWith(DataProviderRunner.class)
public class MeasureTest {

  private static final Measure INT_MEASURE = newMeasureBuilder().create(1);
  private static final Measure LONG_MEASURE = newMeasureBuilder().create(1L);
  private static final Measure DOUBLE_MEASURE = newMeasureBuilder().create(1d, 1);
  private static final Measure STRING_MEASURE = newMeasureBuilder().create("some_sT ring");
  private static final Measure TRUE_MEASURE = newMeasureBuilder().create(true);
  private static final Measure FALSE_MEASURE = newMeasureBuilder().create(false);
  private static final Measure LEVEL_MEASURE = newMeasureBuilder().create(Measure.Level.OK);
  private static final Measure NO_VALUE_MEASURE = newMeasureBuilder().createNoValue();

  private static final List<Measure> MEASURES = ImmutableList.of(
    INT_MEASURE, LONG_MEASURE, DOUBLE_MEASURE, STRING_MEASURE, TRUE_MEASURE, FALSE_MEASURE, NO_VALUE_MEASURE, LEVEL_MEASURE);


  @DataProvider
  public static Object[][] all_but_INT_MEASURE() {
    return getMeasuresExcept(ValueType.INT);
  }

  @DataProvider
  public static Object[][] all_but_LONG_MEASURE() {
    return getMeasuresExcept(ValueType.LONG);
  }

  @DataProvider
  public static Object[][] all_but_DOUBLE_MEASURE() {
    return getMeasuresExcept(ValueType.DOUBLE);
  }

  @DataProvider
  public static Object[][] all_but_BOOLEAN_MEASURE() {
    return getMeasuresExcept(ValueType.BOOLEAN);
  }

  @DataProvider
  public static Object[][] all_but_STRING_MEASURE() {
    return getMeasuresExcept(ValueType.STRING);
  }

  @DataProvider
  public static Object[][] all_but_LEVEL_MEASURE() {
    return getMeasuresExcept(ValueType.LEVEL);
  }

  @DataProvider
  public static Object[][] all() {
    return MEASURES.stream().map(WrapInSingleElementArray.INSTANCE).toArray(Object[][]::new);
  }

  private static Object[][] getMeasuresExcept(final ValueType valueType) {
    return MEASURES.stream()
      .filter(input -> input.getValueType() != valueType)
      .map(WrapInSingleElementArray.INSTANCE)
      .toArray(Object[][]::new);
  }

  @Test
  public void create_from_String_throws_NPE_if_arg_is_null() {
    assertThatThrownBy(() -> newMeasureBuilder().create((String) null))
      .isInstanceOf(NullPointerException.class);
  }

  @Test
  public void create_from_int_has_INT_value_type() {
    assertThat(INT_MEASURE.getValueType()).isEqualTo(ValueType.INT);
  }

  @Test
  public void create_from_long_has_LONG_value_type() {
    assertThat(LONG_MEASURE.getValueType()).isEqualTo(ValueType.LONG);
  }

  @Test
  public void create_from_double_has_DOUBLE_value_type() {
    assertThat(DOUBLE_MEASURE.getValueType()).isEqualTo(ValueType.DOUBLE);
  }

  @Test
  public void create_from_boolean_has_BOOLEAN_value_type() {
    assertThat(TRUE_MEASURE.getValueType()).isEqualTo(ValueType.BOOLEAN);
    assertThat(FALSE_MEASURE.getValueType()).isEqualTo(ValueType.BOOLEAN);
  }

  @Test
  public void create_from_String_has_STRING_value_type() {
    assertThat(STRING_MEASURE.getValueType()).isEqualTo(ValueType.STRING);
  }

  @Test
  @UseDataProvider("all_but_INT_MEASURE")
  public void getIntValue_throws_ISE_for_all_value_types_except_INT(Measure measure) {
    assertThatThrownBy(measure::getIntValue)
      .isInstanceOf(IllegalStateException.class);
  }

  @Test
  public void getIntValue_returns_value_for_INT_value_type() {
    assertThat(INT_MEASURE.getIntValue()).isOne();
  }

  @Test
  @UseDataProvider("all_but_LONG_MEASURE")
  public void getLongValue_throws_ISE_for_all_value_types_except_LONG(Measure measure) {
    assertThatThrownBy(measure::getLongValue)
      .isInstanceOf(IllegalStateException.class);
  }

  @Test
  public void getLongValue_returns_value_for_LONG_value_type() {
    assertThat(LONG_MEASURE.getLongValue()).isOne();
  }

  @Test
  @UseDataProvider("all_but_DOUBLE_MEASURE")
  public void getDoubleValue_throws_ISE_for_all_value_types_except_DOUBLE(Measure measure) {
    assertThatThrownBy(measure::getDoubleValue)
      .isInstanceOf(IllegalStateException.class);
  }

  @Test
  public void getDoubleValue_returns_value_for_DOUBLE_value_type() {
    assertThat(DOUBLE_MEASURE.getDoubleValue()).isEqualTo(1d);
  }

  @Test
  @UseDataProvider("all_but_BOOLEAN_MEASURE")
  public void getBooleanValue_throws_ISE_for_all_value_types_except_BOOLEAN(Measure measure) {
    assertThatThrownBy(measure::getBooleanValue)
      .isInstanceOf(IllegalStateException.class);
  }

  @Test
  public void getBooleanValue_returns_value_for_BOOLEAN_value_type() {
    assertThat(TRUE_MEASURE.getBooleanValue()).isTrue();
    assertThat(FALSE_MEASURE.getBooleanValue()).isFalse();
  }

  @Test
  @UseDataProvider("all_but_STRING_MEASURE")
  public void getStringValue_throws_ISE_for_all_value_types_except_STRING(Measure measure) {
    assertThatThrownBy(measure::getStringValue)
      .isInstanceOf(IllegalStateException.class);
  }

  @Test
  @UseDataProvider("all_but_LEVEL_MEASURE")
  public void getLevelValue_throws_ISE_for_all_value_types_except_LEVEL(Measure measure) {
    assertThatThrownBy(measure::getLevelValue)
      .isInstanceOf(IllegalStateException.class);
  }

  @Test
  public void getData_returns_null_for_NO_VALUE_value_type() {
    assertThat(NO_VALUE_MEASURE.getData()).isNull();
  }

  @Test
  @UseDataProvider("all_but_STRING_MEASURE")
  public void getData_returns_null_for_all_value_types_but_STRING_when_not_set(Measure measure) {
    assertThat(measure.getData()).isNull();
  }

  @Test
  public void getData_returns_value_for_STRING_value_type() {
    assertThat(STRING_MEASURE.getData()).isEqualTo(STRING_MEASURE.getStringValue());
  }

  @Test
  @UseDataProvider("all")
  public void hasAlertStatus_returns_false_for_all_value_types_when_not_set(Measure measure) {
    assertThat(measure.hasQualityGateStatus()).isFalse();
  }

  @Test
  @UseDataProvider("all")
  public void getAlertStatus_throws_ISE_for_all_value_types_when_not_set(Measure measure) {
    assertThatThrownBy(measure::getQualityGateStatus)
      .isInstanceOf(IllegalStateException.class);
  }

  @Test
  public void getAlertStatus_returns_argument_from_setQualityGateStatus() {
    QualityGateStatus someStatus = new QualityGateStatus(Measure.Level.OK);

    assertThat(newMeasureBuilder().setQualityGateStatus(someStatus).create(true, null).getQualityGateStatus()).isEqualTo(someStatus);
    assertThat(newMeasureBuilder().setQualityGateStatus(someStatus).create(false, null).getQualityGateStatus()).isEqualTo(someStatus);
    assertThat(newMeasureBuilder().setQualityGateStatus(someStatus).create(1, null).getQualityGateStatus()).isEqualTo(someStatus);
    assertThat(newMeasureBuilder().setQualityGateStatus(someStatus).create((long) 1, null).getQualityGateStatus()).isEqualTo(someStatus);
    assertThat(newMeasureBuilder().setQualityGateStatus(someStatus).create(1, 1, null).getQualityGateStatus()).isEqualTo(someStatus);
    assertThat(newMeasureBuilder().setQualityGateStatus(someStatus).create("str").getQualityGateStatus()).isEqualTo(someStatus);
    assertThat(newMeasureBuilder().setQualityGateStatus(someStatus).create(Measure.Level.OK).getQualityGateStatus()).isEqualTo(someStatus);
  }

  @Test
  public void newMeasureBuilder_setQualityGateStatus_throws_NPE_if_arg_is_null() {
    assertThatThrownBy(() -> newMeasureBuilder().setQualityGateStatus(null))
      .isInstanceOf(NullPointerException.class);
  }

  @Test
  public void updateMeasureBuilder_setQualityGateStatus_throws_NPE_if_arg_is_null() {
    assertThatThrownBy(() -> {
      Measure.updatedMeasureBuilder(newMeasureBuilder().createNoValue()).setQualityGateStatus(null);
    })
      .isInstanceOf(NullPointerException.class);
  }

  @Test
  public void updateMeasureBuilder_setQualityGateStatus_throws_USO_if_measure_already_has_a_QualityGateStatus() {
    assertThatThrownBy(() -> {
      QualityGateStatus qualityGateStatus = new QualityGateStatus(Measure.Level.ERROR);
      Measure.updatedMeasureBuilder(newMeasureBuilder().setQualityGateStatus(qualityGateStatus).createNoValue()).setQualityGateStatus(qualityGateStatus);
    })
      .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  @UseDataProvider("all")
  public void updateMeasureBuilder_creates_Measure_with_same_immutable_properties(Measure measure) {
    Measure newMeasure = Measure.updatedMeasureBuilder(measure).create();

    assertThat(newMeasure.getValueType()).isEqualTo(measure.getValueType());
    assertThat(newMeasure.hasQualityGateStatus()).isEqualTo(measure.hasQualityGateStatus());
  }

  @Test
  public void getData_returns_argument_from_factory_method() {
    String someData = "lololool";

    assertThat(newMeasureBuilder().create(true, someData).getData()).isEqualTo(someData);
    assertThat(newMeasureBuilder().create(false, someData).getData()).isEqualTo(someData);
    assertThat(newMeasureBuilder().create(1, someData).getData()).isEqualTo(someData);
    assertThat(newMeasureBuilder().create((long) 1, someData).getData()).isEqualTo(someData);
    assertThat(newMeasureBuilder().create(1, 1, someData).getData()).isEqualTo(someData);
  }

  @Test
  public void measure_of_value_type_LEVEL_has_no_data() {
    assertThat(LEVEL_MEASURE.getData()).isNull();
  }

  @Test
  public void double_values_are_scaled_to_1_digit_and_round() {
    assertThat(newMeasureBuilder().create(30.27777d, 1).getDoubleValue()).isEqualTo(30.3d);
    assertThat(newMeasureBuilder().create(30d, 1).getDoubleValue()).isEqualTo(30d);
    assertThat(newMeasureBuilder().create(30.01d, 1).getDoubleValue()).isEqualTo(30d);
    assertThat(newMeasureBuilder().create(30.1d, 1).getDoubleValue()).isEqualTo(30.1d);
  }

  @Test
  public void create_with_double_value_throws_IAE_if_value_is_NaN() {
    assertThatThrownBy(() -> newMeasureBuilder().create(Double.NaN, 1))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("NaN is not allowed as a Measure value");
  }

  @Test
  public void create_with_double_value_data_throws_IAE_if_value_is_NaN() {
    assertThatThrownBy(() -> newMeasureBuilder().create(Double.NaN, 1, "some data"))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("NaN is not allowed as a Measure value");
  }

  @Test
  public void valueMeasureImplEquals_instanceNotEqualToNull() {
    Measure.ValueMeasureImpl valueMeasureImpl = (Measure.ValueMeasureImpl) new Measure.NewMeasureBuilder().create(0, null);

    boolean equal = valueMeasureImpl.equals(null);

    assertThat(equal).isFalse();
  }

  @Test
  public void valueMeasureImplEquals_sameInstance_returnTrue() {
    Measure.ValueMeasureImpl valueMeasureImpl = (Measure.ValueMeasureImpl) new Measure.NewMeasureBuilder().create(0, null);

    boolean equal = valueMeasureImpl.equals(valueMeasureImpl);

    assertThat(equal).isTrue();
  }

}
