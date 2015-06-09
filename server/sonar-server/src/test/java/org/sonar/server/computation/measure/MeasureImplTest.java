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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sonar.server.computation.measure.Measure.ValueType;

import static com.google.common.collect.FluentIterable.from;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(DataProviderRunner.class)
public class MeasureImplTest {

  private static final MeasureImpl INT_MEASURE = MeasureImpl.create((int) 1, null);
  private static final MeasureImpl LONG_MEASURE = MeasureImpl.create(1l, null);
  private static final MeasureImpl DOUBLE_MEASURE = MeasureImpl.create(1d, null);
  private static final MeasureImpl STRING_MEASURE = MeasureImpl.create("some_sT ring");
  private static final MeasureImpl TRUE_MEASURE = MeasureImpl.create(true, null);
  private static final MeasureImpl FALSE_MEASURE = MeasureImpl.create(false, null);
  private static final MeasureImpl NO_VALUE_MEASURE = MeasureImpl.createNoValue();

  private static final List<MeasureImpl> MEASURES = ImmutableList.of(
    INT_MEASURE, LONG_MEASURE, DOUBLE_MEASURE, STRING_MEASURE, TRUE_MEASURE, FALSE_MEASURE, NO_VALUE_MEASURE
    );

  @Test(expected = NullPointerException.class)
  public void create_from_String_throws_NPE_if_arg_is_null() {
    MeasureImpl.create((String) null);
  }

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
  public void getIntValue_throws_ISE_for_all_value_types_except_int(Measure measure) {
    measure.getIntValue();
  }

  @Test
  public void getIntValue_returns_value_for_INT_value_type() {
    assertThat(INT_MEASURE.getIntValue()).isEqualTo(1);
  }

  @Test(expected = IllegalStateException.class)
  @UseDataProvider("all_but_LONG_MEASURE")
  public void getLongValue_throws_ISE_for_all_value_types_except_int(Measure measure) {
    measure.getLongValue();
  }

  @Test
  public void getLongValue_returns_value_for_LONG_value_type() {
    assertThat(LONG_MEASURE.getLongValue()).isEqualTo(1);
  }

  @Test(expected = IllegalStateException.class)
  @UseDataProvider("all_but_DOUBLE_MEASURE")
  public void getDoubleValue_throws_ISE_for_all_value_types_except_int(Measure measure) {
    measure.getDoubleValue();
  }

  @Test
  public void getDoubleValue_returns_value_for_DOUBLE_value_type() {
    assertThat(DOUBLE_MEASURE.getDoubleValue()).isEqualTo(1d);
  }

  @Test(expected = IllegalStateException.class)
  @UseDataProvider("all_but_BOOLEAN_MEASURE")
  public void getBooleanValue_throws_ISE_for_all_value_types_except_int(Measure measure) {
    measure.getBooleanValue();
  }

  @Test
  public void getBooleanValue_returns_value_for_BOOLEAN_value_type() {
    assertThat(TRUE_MEASURE.getBooleanValue()).isTrue();
    assertThat(FALSE_MEASURE.getBooleanValue()).isFalse();
  }

  @Test(expected = IllegalStateException.class)
  @UseDataProvider("all_but_STRING_MEASURE")
  public void getStringValue_throws_ISE_for_all_value_types_except_int(Measure measure) {
    measure.getStringValue();
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
    assertThat(measure.hasAlertStatus()).isFalse();
  }

  @Test(expected = IllegalStateException.class)
  @UseDataProvider("all")
  public void getAlertStatus_throws_ISE_for_all_value_types_when_not_set(Measure measure) {
    measure.getAlertStatus();
  }

  @Test
  public void getAlertStatus_returns_argument_from_setAlertStatus() {
    Measure.AlertStatus someStatus = Measure.AlertStatus.OK;

    assertThat(MeasureImpl.create(true, null).setAlertStatus(someStatus).getAlertStatus()).isEqualTo(someStatus);
    assertThat(MeasureImpl.create(false, null).setAlertStatus(someStatus).getAlertStatus()).isEqualTo(someStatus);
    assertThat(MeasureImpl.create((int) 1, null).setAlertStatus(someStatus).getAlertStatus()).isEqualTo(someStatus);
    assertThat(MeasureImpl.create((long) 1, null).setAlertStatus(someStatus).getAlertStatus()).isEqualTo(someStatus);
    assertThat(MeasureImpl.create((double) 1, null).setAlertStatus(someStatus).getAlertStatus()).isEqualTo(someStatus);
  }

  @Test(expected = NullPointerException.class)
  @UseDataProvider("all")
  public void setAlertStatus_throws_NPE_if_arg_is_null(MeasureImpl measure) {
    measure.setAlertStatus(null);
  }

  @Test
  @UseDataProvider("all")
  public void hasAlertText_returns_false_for_all_value_types_when_not_set(Measure measure) {
    assertThat(measure.hasAlertText()).isFalse();
  }

  @Test(expected = IllegalStateException.class)
  @UseDataProvider("all")
  public void getAlertText_throws_ISE_for_all_value_types_when_not_set(Measure measure) {
    measure.getAlertText();
  }

  @Test(expected = NullPointerException.class)
  @UseDataProvider("all")
  public void setAlertText_throws_NPE_if_arg_is_null(MeasureImpl measure) {
    measure.setAlertText(null);
  }

  @Test
  public void getAlertText_returns_argument_from_setAlertText() {
    String someText = "trololo";

    assertThat(MeasureImpl.create(true, null).setAlertText(someText).getAlertText()).isEqualTo(someText);
    assertThat(MeasureImpl.create(false, null).setAlertText(someText).getAlertText()).isEqualTo(someText);
    assertThat(MeasureImpl.create((int) 1, null).setAlertText(someText).getAlertText()).isEqualTo(someText);
    assertThat(MeasureImpl.create((long) 1, null).setAlertText(someText).getAlertText()).isEqualTo(someText);
    assertThat(MeasureImpl.create((double) 1, null).setAlertText(someText).getAlertText()).isEqualTo(someText);
  }

  @Test
  public void getData_returns_argument_from_factory_method() {
    String someData = "lololool";

    assertThat(MeasureImpl.create(true, someData).getData()).isEqualTo(someData);
    assertThat(MeasureImpl.create(false, someData).getData()).isEqualTo(someData);
    assertThat(MeasureImpl.create((int) 1, someData).getData()).isEqualTo(someData);
    assertThat(MeasureImpl.create((long) 1, someData).getData()).isEqualTo(someData);
    assertThat(MeasureImpl.create((double) 1, someData).getData()).isEqualTo(someData);
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
