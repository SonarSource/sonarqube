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

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.sonar.server.computation.measure.Measure.ValueType;

import static com.google.common.collect.FluentIterable.from;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.server.computation.measure.Measure.newMeasureBuilder;

@RunWith(DataProviderRunner.class)
public class MeasureTest {

  private static final Measure INT_MEASURE = newMeasureBuilder().create((int) 1);
  private static final Measure LONG_MEASURE = newMeasureBuilder().create(1l);
  private static final Measure DOUBLE_MEASURE = newMeasureBuilder().create(1d);
  private static final Measure STRING_MEASURE = newMeasureBuilder().create("some_sT ring");
  private static final Measure TRUE_MEASURE = newMeasureBuilder().create(true);
  private static final Measure FALSE_MEASURE = newMeasureBuilder().create(false);
  private static final Measure LEVEL_MEASURE = newMeasureBuilder().create(Measure.Level.OK);
  private static final Measure NO_VALUE_MEASURE = newMeasureBuilder().createNoValue();

  private static final List<Measure> MEASURES = ImmutableList.of(
    INT_MEASURE, LONG_MEASURE, DOUBLE_MEASURE, STRING_MEASURE, TRUE_MEASURE, FALSE_MEASURE, NO_VALUE_MEASURE, LEVEL_MEASURE
    );
  private static final int SOME_RULE_ID = 95236;
  private static final int SOME_CHARACTERISTIC_ID = 42;

  @Rule
  public final ExpectedException expectedException = ExpectedException.none();

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
    return from(MEASURES).transform(WrapInArray.INSTANCE).toArray(Measure[].class);
  }

  private static Measure[][] getMeasuresExcept(final ValueType valueType) {
    return from(MEASURES)
      .filter(new Predicate<Measure>() {
        @Override
        public boolean apply(@Nonnull Measure input) {
          return input.getValueType() != valueType;
        }
      }).transform(WrapInArray.INSTANCE)
      .toArray(Measure[].class);
  }

  @Test
  public void forRule_throw_UOE_if_characteristicId_is_already_set() {
    expectedException.expect(UnsupportedOperationException.class);
    expectedException.expectMessage("A measure can not be associated to both a Characteristic and a Rule");

    newMeasureBuilder().forCharacteristic(SOME_CHARACTERISTIC_ID).forRule(SOME_RULE_ID);
  }

  @Test
  public void forCharacteristic_throw_UOE_if_ruleKey_is_already_set() {
    expectedException.expect(UnsupportedOperationException.class);
    expectedException.expectMessage("A measure can not be associated to both a Characteristic and a Rule");

    newMeasureBuilder().forRule(SOME_RULE_ID).forCharacteristic(SOME_CHARACTERISTIC_ID);
  }

  @Test
  public void getRuleId_returns_null_when_ruleKey_has_not_been_set() {
    assertThat(newMeasureBuilder().createNoValue().getRuleId()).isNull();
    assertThat(newMeasureBuilder().forCharacteristic(SOME_CHARACTERISTIC_ID).createNoValue().getRuleId()).isNull();
  }

  @Test
  public void getRuleId_returns_key_set_in_builder() {
    assertThat(newMeasureBuilder().forRule(SOME_RULE_ID).createNoValue().getRuleId()).isEqualTo(SOME_RULE_ID);
  }

  @Test
  public void getCharacteristicId_returns_null_when_ruleKey_has_not_been_set() {
    assertThat(newMeasureBuilder().createNoValue().getCharacteristicId()).isNull();
    assertThat(newMeasureBuilder().forRule(SOME_RULE_ID).createNoValue().getCharacteristicId()).isNull();
  }

  @Test
  public void getCharacteristicId_returns_id_set_in_builder() {
    assertThat(newMeasureBuilder().forCharacteristic(SOME_CHARACTERISTIC_ID).createNoValue().getCharacteristicId()).isEqualTo(SOME_CHARACTERISTIC_ID);
  }

  @Test(expected = NullPointerException.class)
  public void create_from_String_throws_NPE_if_arg_is_null() {
    newMeasureBuilder().create((String) null);
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

  @Test(expected = IllegalStateException.class)
  @UseDataProvider("all_but_INT_MEASURE")
  public void getIntValue_throws_ISE_for_all_value_types_except_INT(Measure measure) {
    measure.getIntValue();
  }

  @Test
  public void getIntValue_returns_value_for_INT_value_type() {
    assertThat(INT_MEASURE.getIntValue()).isEqualTo(1);
  }

  @Test(expected = IllegalStateException.class)
  @UseDataProvider("all_but_LONG_MEASURE")
  public void getLongValue_throws_ISE_for_all_value_types_except_LONG(Measure measure) {
    measure.getLongValue();
  }

  @Test
  public void getLongValue_returns_value_for_LONG_value_type() {
    assertThat(LONG_MEASURE.getLongValue()).isEqualTo(1);
  }

  @Test(expected = IllegalStateException.class)
  @UseDataProvider("all_but_DOUBLE_MEASURE")
  public void getDoubleValue_throws_ISE_for_all_value_types_except_DOUBLE(Measure measure) {
    measure.getDoubleValue();
  }

  @Test
  public void getDoubleValue_returns_value_for_DOUBLE_value_type() {
    assertThat(DOUBLE_MEASURE.getDoubleValue()).isEqualTo(1d);
  }

  @Test(expected = IllegalStateException.class)
  @UseDataProvider("all_but_BOOLEAN_MEASURE")
  public void getBooleanValue_throws_ISE_for_all_value_types_except_BOOLEAN(Measure measure) {
    measure.getBooleanValue();
  }

  @Test
  public void getBooleanValue_returns_value_for_BOOLEAN_value_type() {
    assertThat(TRUE_MEASURE.getBooleanValue()).isTrue();
    assertThat(FALSE_MEASURE.getBooleanValue()).isFalse();
  }

  @Test(expected = IllegalStateException.class)
  @UseDataProvider("all_but_STRING_MEASURE")
  public void getStringValue_throws_ISE_for_all_value_types_except_STRING(Measure measure) {
    measure.getStringValue();
  }

  @Test(expected = IllegalStateException.class)
  @UseDataProvider("all_but_LEVEL_MEASURE")
  public void getLevelValue_throws_ISE_for_all_value_types_except_LEVEL(Measure measure) {
    measure.getLevelValue();
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

  @Test(expected = IllegalStateException.class)
  @UseDataProvider("all")
  public void getAlertStatus_throws_ISE_for_all_value_types_when_not_set(Measure measure) {
    measure.getQualityGateStatus();
  }

  @Test
  public void getAlertStatus_returns_argument_from_setQualityGateStatus() {
    QualityGateStatus someStatus = new QualityGateStatus(Measure.Level.OK);

    assertThat(newMeasureBuilder().setQualityGateStatus(someStatus).create(true, null).getQualityGateStatus()).isEqualTo(someStatus);
    assertThat(newMeasureBuilder().setQualityGateStatus(someStatus).create(false, null).getQualityGateStatus()).isEqualTo(someStatus);
    assertThat(newMeasureBuilder().setQualityGateStatus(someStatus).create((int) 1, null).getQualityGateStatus()).isEqualTo(someStatus);
    assertThat(newMeasureBuilder().setQualityGateStatus(someStatus).create((long) 1, null).getQualityGateStatus()).isEqualTo(someStatus);
    assertThat(newMeasureBuilder().setQualityGateStatus(someStatus).create((double) 1, null).getQualityGateStatus()).isEqualTo(someStatus);
    assertThat(newMeasureBuilder().setQualityGateStatus(someStatus).create("str").getQualityGateStatus()).isEqualTo(someStatus);
    assertThat(newMeasureBuilder().setQualityGateStatus(someStatus).create(Measure.Level.OK).getQualityGateStatus()).isEqualTo(someStatus);
  }

  @Test(expected = NullPointerException.class)
  public void newMeasureBuilder_setQualityGateStatus_throws_NPE_if_arg_is_null() {
    newMeasureBuilder().setQualityGateStatus(null);
  }

  @Test(expected = NullPointerException.class)
  public void newMeasureBuilder_setVariations_throws_NPE_if_arg_is_null() {
    newMeasureBuilder().setVariations(null);
  }

  @Test(expected = NullPointerException.class)
  public void newMeasureBuilder_setDescription_throws_NPE_if_arg_is_null() {
    newMeasureBuilder().setDescription(null);
  }

  @Test(expected = NullPointerException.class)
  public void updateMeasureBuilder_setQualityGateStatus_throws_NPE_if_arg_is_null() {
    Measure.updatedMeasureBuilder(newMeasureBuilder().createNoValue()).setQualityGateStatus(null);
  }

  @Test(expected = NullPointerException.class)
  public void updateMeasureBuilder_setVariations_throws_NPE_if_arg_is_null() {
    Measure.updatedMeasureBuilder(newMeasureBuilder().createNoValue()).setVariations(null);
  }

  @Test(expected = UnsupportedOperationException.class)
  public void updateMeasureBuilder_setQualityGateStatus_throws_USO_if_measure_already_has_a_QualityGateStatus() {
    QualityGateStatus qualityGateStatus = new QualityGateStatus(Measure.Level.ERROR);

    Measure.updatedMeasureBuilder(newMeasureBuilder().setQualityGateStatus(qualityGateStatus).createNoValue()).setQualityGateStatus(qualityGateStatus);
  }

  @Test(expected = UnsupportedOperationException.class)
  public void updateMeasureBuilder_setVariations_throws_USO_if_measure_already_has_Variations() {
    MeasureVariations variations = new MeasureVariations(1d);

    Measure.updatedMeasureBuilder(newMeasureBuilder().setVariations(variations).createNoValue()).setVariations(variations);
  }

  @Test
  @UseDataProvider("all")
  public void updateMeasureBuilder_creates_Measure_with_same_immutable_properties(Measure measure) {
    Measure newMeasure = Measure.updatedMeasureBuilder(measure).create();

    assertThat(newMeasure.getValueType()).isEqualTo(measure.getValueType());
    assertThat(newMeasure.getRuleId()).isEqualTo(measure.getRuleId());
    assertThat(newMeasure.getCharacteristicId()).isEqualTo(measure.getCharacteristicId());
    assertThat(newMeasure.getDescription()).isEqualTo(measure.getDescription());
    assertThat(newMeasure.hasQualityGateStatus()).isEqualTo(measure.hasQualityGateStatus());
    assertThat(newMeasure.hasVariations()).isEqualTo(measure.hasVariations());
  }

  @Test
  public void getData_returns_argument_from_factory_method() {
    String someData = "lololool";

    assertThat(newMeasureBuilder().create(true, someData).getData()).isEqualTo(someData);
    assertThat(newMeasureBuilder().create(false, someData).getData()).isEqualTo(someData);
    assertThat(newMeasureBuilder().create((int) 1, someData).getData()).isEqualTo(someData);
    assertThat(newMeasureBuilder().create((long) 1, someData).getData()).isEqualTo(someData);
    assertThat(newMeasureBuilder().create((double) 1, someData).getData()).isEqualTo(someData);
  }

  @Test
  public void measure_of_value_type_LEVEL_has_no_data() {
    assertThat(LEVEL_MEASURE.getData()).isNull();
  }

  @Test
  public void double_values_are_scaled_to_1_digit_and_round() {
    assertThat(newMeasureBuilder().create(30.27777d).getDoubleValue()).isEqualTo(30.3d);
    assertThat(newMeasureBuilder().create(30d).getDoubleValue()).isEqualTo(30d);
    assertThat(newMeasureBuilder().create(30.01d).getDoubleValue()).isEqualTo(30d);
    assertThat(newMeasureBuilder().create(30.1d).getDoubleValue()).isEqualTo(30.1d);
  }

  @Test
  public void create_with_double_value_throws_IAE_if_value_is_NaN() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Nan is not allowed as a Measure value");

    newMeasureBuilder().create(Double.NaN);
  }

  @Test
  public void create_with_double_value_data_throws_IAE_if_value_is_NaN() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Nan is not allowed as a Measure value");

    newMeasureBuilder().create(Double.NaN, "some data");
  }

  private enum WrapInArray implements Function<Measure, Measure[]> {
    INSTANCE;

    @Nullable
    @Override
    public Measure[] apply(@Nonnull Measure input) {
      return new Measure[] {input};
    }
  }
}
