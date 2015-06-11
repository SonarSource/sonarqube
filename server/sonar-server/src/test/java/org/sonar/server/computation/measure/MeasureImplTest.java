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
import static org.sonar.server.computation.measure.MeasureImpl.builder;

@RunWith(DataProviderRunner.class)
public class MeasureImplTest {

  private static final MeasureImpl INT_MEASURE = builder().create((int) 1, null);
  private static final MeasureImpl LONG_MEASURE = builder().create(1l, null);
  private static final MeasureImpl DOUBLE_MEASURE = builder().create(1d, null);
  private static final MeasureImpl STRING_MEASURE = builder().create("some_sT ring");
  private static final MeasureImpl TRUE_MEASURE = builder().create(true, null);
  private static final MeasureImpl FALSE_MEASURE = builder().create(false, null);
  private static final MeasureImpl LEVEL_MEASURE = builder().create(Measure.Level.OK);
  private static final MeasureImpl NO_VALUE_MEASURE = builder().createNoValue();

  private static final List<MeasureImpl> MEASURES = ImmutableList.of(
    INT_MEASURE, LONG_MEASURE, DOUBLE_MEASURE, STRING_MEASURE, TRUE_MEASURE, FALSE_MEASURE, NO_VALUE_MEASURE, LEVEL_MEASURE
    );
  private static final int SOME_RULE_ID = 95236;
  private static final int SOME_CHARACTERISTIC_ID = 42;

  @Rule
  public final ExpectedException executionException = ExpectedException.none();

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
    return from(MEASURES).transform(WrapInArray.INSTANCE).toArray(MeasureImpl[].class);
  }

  private static MeasureImpl[][] getMeasuresExcept(final ValueType valueType) {
    return from(MEASURES)
      .filter(new Predicate<MeasureImpl>() {
        @Override
        public boolean apply(@Nonnull MeasureImpl input) {
          return input.getValueType() != valueType;
        }
      }).transform(WrapInArray.INSTANCE)
      .toArray(MeasureImpl[].class);
  }

  @Test
  public void forRule_throw_UOE_if_characteristicId_is_already_set() {
    executionException.expect(UnsupportedOperationException.class);
    executionException.expectMessage("A measure can not be associated to both a Characteristic and a Rule");

    builder().forCharacteristic(SOME_CHARACTERISTIC_ID).forRule(SOME_RULE_ID);
  }

  @Test
  public void forCharacteristic_throw_UOE_if_ruleKey_is_already_set() {
    executionException.expect(UnsupportedOperationException.class);
    executionException.expectMessage("A measure can not be associated to both a Characteristic and a Rule");

    builder().forRule(SOME_RULE_ID).forCharacteristic(SOME_CHARACTERISTIC_ID);
  }

  @Test
  public void getRuleId_returns_null_when_ruleKey_has_not_been_set() {
    assertThat(builder().createNoValue().getRuleId()).isNull();
    assertThat(builder().forCharacteristic(SOME_CHARACTERISTIC_ID).createNoValue().getRuleId()).isNull();
  }

  @Test
  public void getRuleId_returns_key_set_in_builder() {
    assertThat(builder().forRule(SOME_RULE_ID).createNoValue().getRuleId()).isEqualTo(SOME_RULE_ID);
  }

  @Test
  public void getCharacteristicId_returns_null_when_ruleKey_has_not_been_set() {
    assertThat(builder().createNoValue().getCharacteristicId()).isNull();
    assertThat(builder().forRule(SOME_RULE_ID).createNoValue().getCharacteristicId()).isNull();
  }

  @Test
  public void getCharacteristicId_returns_id_set_in_builder() {
    assertThat(builder().forCharacteristic(SOME_CHARACTERISTIC_ID).createNoValue().getCharacteristicId()).isEqualTo(SOME_CHARACTERISTIC_ID);
  }

  @Test(expected = NullPointerException.class)
  public void create_from_String_throws_NPE_if_arg_is_null() {
    builder().create((String) null);
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
  public void getAlertStatus_returns_argument_from_setAlertStatus() {
    QualityGateStatus someStatus = new QualityGateStatus(Measure.Level.OK);

    assertThat(builder().create(true, null).setQualityGateStatus(someStatus).getQualityGateStatus()).isEqualTo(someStatus);
    assertThat(builder().create(false, null).setQualityGateStatus(someStatus).getQualityGateStatus()).isEqualTo(someStatus);
    assertThat(builder().create((int) 1, null).setQualityGateStatus(someStatus).getQualityGateStatus()).isEqualTo(someStatus);
    assertThat(builder().create((long) 1, null).setQualityGateStatus(someStatus).getQualityGateStatus()).isEqualTo(someStatus);
    assertThat(builder().create((double) 1, null).setQualityGateStatus(someStatus).getQualityGateStatus()).isEqualTo(someStatus);
    assertThat(builder().create("str").setQualityGateStatus(someStatus).getQualityGateStatus()).isEqualTo(someStatus);
    assertThat(builder().create(Measure.Level.OK).setQualityGateStatus(someStatus).getQualityGateStatus()).isEqualTo(someStatus);
  }

  @Test(expected = NullPointerException.class)
  @UseDataProvider("all")
  public void setAlertStatus_throws_NPE_if_arg_is_null(MeasureImpl measure) {
    measure.setQualityGateStatus(null);
  }

  @Test
  public void getData_returns_argument_from_factory_method() {
    String someData = "lololool";

    assertThat(builder().create(true, someData).getData()).isEqualTo(someData);
    assertThat(builder().create(false, someData).getData()).isEqualTo(someData);
    assertThat(builder().create((int) 1, someData).getData()).isEqualTo(someData);
    assertThat(builder().create((long) 1, someData).getData()).isEqualTo(someData);
    assertThat(builder().create((double) 1, someData).getData()).isEqualTo(someData);
  }

  @Test
  public void measure_of_value_type_LEVEL_has_no_data() {
    assertThat(LEVEL_MEASURE.getData()).isNull();
  }

  private enum WrapInArray implements Function<MeasureImpl, MeasureImpl[]> {
    INSTANCE;

    @Nullable
    @Override
    public MeasureImpl[] apply(@Nonnull MeasureImpl input) {
      return new MeasureImpl[] {input};
    }
  }
}
