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
package org.sonar.core.timemachine;

import java.util.Date;
import java.util.Locale;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.i18n.I18n;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.sonar.api.utils.DateUtils.parseDate;
import static org.sonar.core.config.CorePropertyDefinitions.LEAK_PERIOD;
import static org.sonar.core.config.CorePropertyDefinitions.LEAK_PERIOD_MODE_DATE;
import static org.sonar.core.config.CorePropertyDefinitions.LEAK_PERIOD_MODE_DAYS;
import static org.sonar.core.config.CorePropertyDefinitions.LEAK_PERIOD_MODE_PREVIOUS_VERSION;
import static org.sonar.core.config.CorePropertyDefinitions.LEAK_PERIOD_MODE_VERSION;

public class PeriodsTest {

  static String NUMBER_OF_DAYS = "5";
  static String STRING_DATE = "2015-01-01";
  static Date DATE = parseDate(STRING_DATE);
  static String VERSION = "1.1";
  static int PERIOD_INDEX = 1;
  @Rule
  public ExpectedException thrown = ExpectedException.none();
  MapSettings settings = new MapSettings();
  I18n i18n = mock(I18n.class);
  Periods periods = new Periods(settings.asConfig(), i18n);

  @Test
  public void return_over_x_days_label_when_no_date() {
    periods.label(LEAK_PERIOD_MODE_DAYS, NUMBER_OF_DAYS, (String) null);

    verify(i18n).message(any(Locale.class), eq("over_x_days"), isNull(String.class), eq(NUMBER_OF_DAYS));
  }

  @Test
  public void return_over_x_days_abbreviation_when_no_date() {
    periods.abbreviation(LEAK_PERIOD_MODE_DAYS, NUMBER_OF_DAYS, null);

    verify(i18n).message(any(Locale.class), eq("over_x_days.short"), isNull(String.class), eq(NUMBER_OF_DAYS));
  }

  @Test
  public void return_over_x_days_detailed_label_when_date_is_set() {
    periods.label(LEAK_PERIOD_MODE_DAYS, NUMBER_OF_DAYS, STRING_DATE);

    verify(i18n).message(any(Locale.class), eq("over_x_days_detailed"), isNull(String.class), eq(NUMBER_OF_DAYS), eq(STRING_DATE));
  }

  @Test
  public void return_over_x_days_detailed_abbreviation_when_date_is_set() {
    periods.abbreviation(LEAK_PERIOD_MODE_DAYS, NUMBER_OF_DAYS, DATE);

    verify(i18n).message(any(Locale.class), eq("over_x_days_detailed.short"), isNull(String.class), eq(NUMBER_OF_DAYS), anyString());
  }

  @Test
  public void return_over_x_days_label_using_settings() {
    settings.setProperty(LEAK_PERIOD + PERIOD_INDEX, NUMBER_OF_DAYS);

    periods.label(PERIOD_INDEX);

    verify(i18n).message(any(Locale.class), eq("over_x_days"), isNull(String.class), eq(NUMBER_OF_DAYS));
  }

  @Test
  public void return_since_version_label_when_no_date() {
    periods.label(LEAK_PERIOD_MODE_VERSION, VERSION, (String) null);

    verify(i18n).message(any(Locale.class), eq("since_version"), isNull(String.class), eq(VERSION));
  }

  @Test
  public void return_since_version_abbreviation_when_no_date() {
    periods.abbreviation(LEAK_PERIOD_MODE_VERSION, VERSION, null);

    verify(i18n).message(any(Locale.class), eq("since_version.short"), isNull(String.class), eq(VERSION));
  }

  @Test
  public void return_since_version_detailed_label_when_date_is_set() {
    periods.label(LEAK_PERIOD_MODE_VERSION, VERSION, STRING_DATE);

    verify(i18n).message(any(Locale.class), eq("since_version_detailed"), isNull(String.class), eq(VERSION), eq(STRING_DATE));
  }

  @Test
  public void return_since_version_detailed_abbreviation_when_date_is_set() {
    periods.abbreviation(LEAK_PERIOD_MODE_VERSION, VERSION, DATE);

    verify(i18n).message(any(Locale.class), eq("since_version_detailed.short"), isNull(String.class), eq(VERSION), anyString());
  }

  @Test
  public void return_since_version_label_using_settings() {
    settings.setProperty(LEAK_PERIOD + PERIOD_INDEX, VERSION);

    periods.label(PERIOD_INDEX);

    verify(i18n).message(any(Locale.class), eq("since_version"), isNull(String.class), eq(VERSION));
  }

  @Test
  public void return_since_previous_version_label_when_no_param() {
    periods.label(LEAK_PERIOD_MODE_PREVIOUS_VERSION, null, (String) null);

    verify(i18n).message(any(Locale.class), eq("since_previous_version"), isNull(String.class));
  }

  @Test
  public void return_since_previous_version_abbreviation_when_no_param() {
    periods.abbreviation(LEAK_PERIOD_MODE_PREVIOUS_VERSION, null, null);

    verify(i18n).message(any(Locale.class), eq("since_previous_version.short"), isNull(String.class));
  }

  @Test
  public void return_since_previous_version_detailed_label_when_param_is_set_and_no_date() {
    periods.label(LEAK_PERIOD_MODE_PREVIOUS_VERSION, VERSION, (String) null);

    verify(i18n).message(any(Locale.class), eq("since_previous_version_detailed"), isNull(String.class), eq(VERSION));
  }

  @Test
  public void return_since_previous_version_detailed_abbreviation_when_param_is_set_and_no_date() {
    periods.abbreviation(LEAK_PERIOD_MODE_PREVIOUS_VERSION, VERSION, null);

    verify(i18n).message(any(Locale.class), eq("since_previous_version_detailed.short"), isNull(String.class), eq(VERSION));
  }

  @Test
  public void return_since_previous_version_detailed_label_when_param_and_date_are_set() {
    periods.label(LEAK_PERIOD_MODE_PREVIOUS_VERSION, VERSION, STRING_DATE);

    verify(i18n).message(any(Locale.class), eq("since_previous_version_detailed"), isNull(String.class), eq(VERSION), eq(STRING_DATE));
  }

  @Test
  public void return_since_previous_version_with_only_date_label_when_no_param_and_date_is_set() {
    periods.label(LEAK_PERIOD_MODE_PREVIOUS_VERSION, null, STRING_DATE);

    verify(i18n).message(any(Locale.class), eq("since_previous_version_with_only_date"), isNull(String.class), eq(STRING_DATE));
  }

  @Test
  public void return_since_previous_version_detailed_abbreviation_when_param_and_date_are_set() {
    periods.abbreviation(LEAK_PERIOD_MODE_PREVIOUS_VERSION, VERSION, DATE);

    verify(i18n).message(any(Locale.class), eq("since_previous_version_detailed.short"), isNull(String.class), eq(VERSION), anyString());
  }

  @Test
  public void return_since_previous_version_label_using_settings() {
    settings.setProperty(LEAK_PERIOD + PERIOD_INDEX, LEAK_PERIOD_MODE_PREVIOUS_VERSION);

    periods.label(PERIOD_INDEX);

    verify(i18n).message(any(Locale.class), eq("since_previous_version"), isNull(String.class));
  }

  @Test
  public void return_since_x_label() {
    periods.label(LEAK_PERIOD_MODE_DATE, null, STRING_DATE);

    verify(i18n).message(any(Locale.class), eq("since_x"), isNull(String.class), eq(STRING_DATE));
  }

  @Test
  public void return_since_x_label_using_settings() {
    settings.setProperty(LEAK_PERIOD + PERIOD_INDEX, STRING_DATE);

    periods.label(PERIOD_INDEX);

    verify(i18n).message(any(Locale.class), eq("since_x"), isNull(String.class), anyString());
  }

  @Test
  public void return_since_x_abbreviation() {
    periods.abbreviation(LEAK_PERIOD_MODE_DATE, null, DATE);

    verify(i18n).message(any(Locale.class), eq("since_x.short"), isNull(String.class), anyString());
  }

  @Test
  public void throw_IAE_when_mode_is_unknown() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("This mode is not supported : unknown");

    periods.label("unknown", null, (String) null);
  }

  @Test
  public void return_abbreviation_using_settings() {
    settings.setProperty(LEAK_PERIOD + PERIOD_INDEX, NUMBER_OF_DAYS);

    periods.abbreviation(PERIOD_INDEX);

    verify(i18n).message(any(Locale.class), eq("over_x_days.short"), isNull(String.class), eq(NUMBER_OF_DAYS));
  }

  @Test
  public void throw_IAE_when_period_property_is_empty() {
    settings.setProperty(LEAK_PERIOD + PERIOD_INDEX, "");

    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("Period property should not be empty");

    periods.label(PERIOD_INDEX);
  }

  @Test
  public void throw_IAE_when_period_property_is_null() {
    settings.setProperty(LEAK_PERIOD + PERIOD_INDEX, (String) null);

    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("Period property should not be empty");

    periods.label(PERIOD_INDEX);
  }

}
