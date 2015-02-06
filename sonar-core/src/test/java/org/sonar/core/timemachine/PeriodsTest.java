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

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.CoreProperties;
import org.sonar.api.config.Settings;
import org.sonar.api.database.model.Snapshot;
import org.sonar.api.i18n.I18n;
import org.sonar.api.utils.System2;

import java.util.Locale;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.*;

public class PeriodsTest {

  private Periods periods;

  private Snapshot snapshot;

  private Settings settings;
  private I18n i18n;

  private int periodIndex;
  private String param;

  @Before
  public void before() {
    periodIndex = 1;
    param = "10";

    snapshot = mock(Snapshot.class);
    when(snapshot.getPeriodModeParameter(periodIndex)).thenReturn(param);

    settings = new Settings();
    i18n = mock(I18n.class);
    periods = new Periods(settings, i18n);
  }

  @Test
  public void label_of_duration_in_days() {
    when(snapshot.getPeriodMode(periodIndex)).thenReturn(CoreProperties.TIMEMACHINE_MODE_DAYS);
    when(snapshot.getPeriodDateMs(periodIndex)).thenReturn(System2.INSTANCE.now());
    when(snapshot.getPeriodModeParameter(periodIndex)).thenReturn(param);

    periods.label(snapshot, periodIndex);
    verify(i18n).message(any(Locale.class), eq("over_x_days_detailed"), isNull(String.class), eq(param), anyString());
  }

  @Test
  public void abbreviation_of_duration_in_days() {
    when(snapshot.getPeriodMode(periodIndex)).thenReturn(CoreProperties.TIMEMACHINE_MODE_DAYS);
    when(snapshot.getPeriodDateMs(periodIndex)).thenReturn(System.currentTimeMillis());
    when(snapshot.getPeriodModeParameter(periodIndex)).thenReturn(param);

    periods.abbreviation(snapshot, periodIndex);
    verify(i18n).message(any(Locale.class), eq("over_x_days_detailed.short"), isNull(String.class), eq(param), anyString());
  }

  @Test
  public void label_of_snapshot_version() {
    when(snapshot.getPeriodMode(periodIndex)).thenReturn(CoreProperties.TIMEMACHINE_MODE_VERSION);
    when(snapshot.getPeriodDateMs(periodIndex)).thenReturn(System.currentTimeMillis());
    when(snapshot.getPeriodModeParameter(periodIndex)).thenReturn(param);

    periods.label(snapshot, periodIndex);
    verify(i18n).message(any(Locale.class), eq("since_version_detailed"), isNull(String.class), eq(param), anyString());
  }

  @Test
  public void abbreviation_of_snapshot_version() {
    when(snapshot.getPeriodMode(periodIndex)).thenReturn(CoreProperties.TIMEMACHINE_MODE_VERSION);
    when(snapshot.getPeriodDateMs(periodIndex)).thenReturn(System.currentTimeMillis());
    when(snapshot.getPeriodModeParameter(periodIndex)).thenReturn(param);

    periods.abbreviation(snapshot, periodIndex);
    verify(i18n).message(any(Locale.class), eq("since_version_detailed.short"), isNull(String.class), eq(param), anyString());
  }

  @Test
  public void label_of_previous_analysis_with_date() {
    when(snapshot.getPeriodMode(periodIndex)).thenReturn(CoreProperties.TIMEMACHINE_MODE_PREVIOUS_ANALYSIS);
    when(snapshot.getPeriodDateMs(periodIndex)).thenReturn(System.currentTimeMillis());

    periods.label(snapshot, periodIndex);
    verify(i18n).message(any(Locale.class), eq("since_previous_analysis_detailed"), isNull(String.class), anyString());
  }

  @Test
  public void label_of_previous_analysis_without_date() {
    when(snapshot.getPeriodMode(periodIndex)).thenReturn(CoreProperties.TIMEMACHINE_MODE_PREVIOUS_ANALYSIS);
    when(snapshot.getPeriodDateMs(periodIndex)).thenReturn(null);
    when(snapshot.getPeriodModeParameter(periodIndex)).thenReturn(param);

    periods.label(snapshot, periodIndex);
    verify(i18n).message(any(Locale.class), eq("since_previous_analysis"), isNull(String.class));
  }

  @Test
  public void abbreviation_of_previous_analysis_with_date() {
    when(snapshot.getPeriodMode(periodIndex)).thenReturn(CoreProperties.TIMEMACHINE_MODE_PREVIOUS_ANALYSIS);
    when(snapshot.getPeriodDateMs(periodIndex)).thenReturn(System.currentTimeMillis());

    periods.abbreviation(snapshot, periodIndex);
    verify(i18n).message(any(Locale.class), eq("since_previous_analysis_detailed.short"), isNull(String.class), anyString());
  }

  @Test
  public void abbreviation_of_previous_analysis_without_date() {
    when(snapshot.getPeriodMode(periodIndex)).thenReturn(CoreProperties.TIMEMACHINE_MODE_PREVIOUS_ANALYSIS);
    when(snapshot.getPeriodDateMs(periodIndex)).thenReturn(null);
    when(snapshot.getPeriodModeParameter(periodIndex)).thenReturn(param);

    periods.abbreviation(snapshot, periodIndex);
    verify(i18n).message(any(Locale.class), eq("since_previous_analysis.short"), isNull(String.class));
  }

  @Test
  public void shouldReturnSnapshotLabelInModePreviousVersionWithParamNotNull() {
    when(snapshot.getPeriodMode(periodIndex)).thenReturn(CoreProperties.TIMEMACHINE_MODE_PREVIOUS_VERSION);
    when(snapshot.getPeriodModeParameter(periodIndex)).thenReturn(param);

    periods.label(snapshot, periodIndex);
    verify(i18n).message(any(Locale.class), eq("since_previous_version_detailed"), isNull(String.class), eq(param));
  }

  @Test
  public void label_of_previous_version_with_param_and_date() {
    when(snapshot.getPeriodMode(periodIndex)).thenReturn(CoreProperties.TIMEMACHINE_MODE_PREVIOUS_VERSION);
    when(snapshot.getPeriodModeParameter(periodIndex)).thenReturn(param);
    when(snapshot.getPeriodDateMs(periodIndex)).thenReturn(System.currentTimeMillis());

    periods.label(snapshot, periodIndex);
    verify(i18n).message(any(Locale.class), eq("since_previous_version_detailed"), isNull(String.class), eq(param), anyString());
  }

  @Test
  public void shouldReturnSnapshotLabelInModePreviousVersionWithParamNull() {
    when(snapshot.getPeriodMode(periodIndex)).thenReturn(CoreProperties.TIMEMACHINE_MODE_PREVIOUS_VERSION);
    when(snapshot.getPeriodModeParameter(periodIndex)).thenReturn(null);

    periods.label(snapshot, periodIndex);
    verify(i18n).message(any(Locale.class), eq("since_previous_version"), isNull(String.class));
  }

  @Test
  public void shouldReturnSnapshotLabelInModeDate() {
    when(snapshot.getPeriodMode(periodIndex)).thenReturn(CoreProperties.TIMEMACHINE_MODE_DATE);
    when(snapshot.getPeriodDateMs(periodIndex)).thenReturn(System.currentTimeMillis());

    periods.label(snapshot, periodIndex);

    verify(i18n).message(any(Locale.class), eq("since_x"), isNull(String.class), anyString());
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldNotSupportUnknownModeForSnapshotLabel() {
    when(snapshot.getPeriodMode(periodIndex)).thenReturn("Unknown mode");

    periods.label(snapshot, periodIndex);
  }

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
