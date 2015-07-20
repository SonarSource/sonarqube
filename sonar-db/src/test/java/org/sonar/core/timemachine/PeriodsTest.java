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
package org.sonar.core.timemachine;

import java.util.Locale;
import org.junit.Test;
import org.sonar.api.CoreProperties;
import org.sonar.api.config.Settings;
import org.sonar.api.i18n.I18n;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class PeriodsTest {

  Settings settings = new Settings();
  I18n i18n = mock(I18n.class);
  Periods periods = new Periods(settings, i18n);

  @Test
  public void shouldReturnLabelInModeDays() {
    int periodIndex = 1;
    String days = "5";
    settings.setProperty(CoreProperties.TIMEMACHINE_PERIOD_PREFIX + periodIndex, days);

    periods.label(periodIndex);
    verify(i18n).message(any(Locale.class), eq("over_x_days"), isNull(String.class), eq(days));
  }

  @Test
  public void shouldReturnLabelInModeVersion() {
    int periodIndex = 1;
    String version = "3.5";
    settings.setProperty(CoreProperties.TIMEMACHINE_PERIOD_PREFIX + periodIndex, version);

    periods.label(periodIndex);
    verify(i18n).message(any(Locale.class), eq("since_version"), isNull(String.class), eq(version));
  }

  @Test
  public void shouldReturnLabelInModePreviousAnalysis() {
    int periodIndex = 1;
    settings.setProperty(CoreProperties.TIMEMACHINE_PERIOD_PREFIX + periodIndex, CoreProperties.TIMEMACHINE_MODE_PREVIOUS_ANALYSIS);

    periods.label(periodIndex);
    verify(i18n).message(any(Locale.class), eq("since_previous_analysis"), isNull(String.class));
  }

  @Test
  public void label_of_previous_version() {
    int periodIndex = 1;
    settings.setProperty(CoreProperties.TIMEMACHINE_PERIOD_PREFIX + periodIndex, CoreProperties.TIMEMACHINE_MODE_PREVIOUS_VERSION);

    periods.label(periodIndex);
    verify(i18n).message(any(Locale.class), eq("since_previous_version"), isNull(String.class));
  }

  @Test
  public void abbreviation_of_previous_version() {
    int periodIndex = 1;
    settings.setProperty(CoreProperties.TIMEMACHINE_PERIOD_PREFIX + periodIndex, CoreProperties.TIMEMACHINE_MODE_PREVIOUS_VERSION);

    periods.abbreviation(periodIndex);
    verify(i18n).message(any(Locale.class), eq("since_previous_version.short"), isNull(String.class));
  }

  @Test
  public void label_of_date() {
    int periodIndex = 1;
    settings.setProperty(CoreProperties.TIMEMACHINE_PERIOD_PREFIX + periodIndex, "2012-12-12");

    periods.label(periodIndex);

    verify(i18n).message(any(Locale.class), eq("since_x"), isNull(String.class), anyString());
  }

  @Test
  public void abbreviation_of_date() {
    int periodIndex = 1;
    settings.setProperty(CoreProperties.TIMEMACHINE_PERIOD_PREFIX + periodIndex, "2012-12-12");

    periods.abbreviation(periodIndex);

    verify(i18n).message(any(Locale.class), eq("since_x.short"), isNull(String.class), anyString());
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldNotSupportUnknownModeForLabel() {
    int periodIndex = 1;
    settings.setProperty(CoreProperties.TIMEMACHINE_PERIOD_PREFIX + periodIndex, "");

    periods.label(periodIndex);
  }

}
