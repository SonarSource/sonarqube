/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.core.timemachine;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sonar.api.CoreProperties;
import org.sonar.api.config.Settings;
import org.sonar.api.database.model.Snapshot;
import org.sonar.api.i18n.I18n;

import java.util.Date;
import java.util.Locale;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
    when(snapshot.getPeriodDate(periodIndex)).thenReturn(new Date());
    when(snapshot.getPeriodModeParameter(periodIndex)).thenReturn(param);

    periods.label(snapshot, periodIndex);
    verify(i18n).message(Mockito.any(Locale.class), Mockito.eq("over_x_days"), Mockito.isNull(String.class), Mockito.eq(param));
  }

  @Test
  public void abbreviation_of_duration_in_days() {
    when(snapshot.getPeriodMode(periodIndex)).thenReturn(CoreProperties.TIMEMACHINE_MODE_DAYS);
    when(snapshot.getPeriodDate(periodIndex)).thenReturn(new Date());
    when(snapshot.getPeriodModeParameter(periodIndex)).thenReturn(param);

    periods.abbreviation(snapshot, periodIndex);
    verify(i18n).message(Mockito.any(Locale.class), Mockito.eq("over_x_days.short"), Mockito.isNull(String.class), Mockito.eq(param));
  }

  @Test
  public void label_of_snapshot_version() {
    when(snapshot.getPeriodMode(periodIndex)).thenReturn(CoreProperties.TIMEMACHINE_MODE_VERSION);
    when(snapshot.getPeriodDate(periodIndex)).thenReturn(new Date());
    when(snapshot.getPeriodModeParameter(periodIndex)).thenReturn(param);

    periods.label(snapshot, periodIndex);
    verify(i18n).message(Mockito.any(Locale.class), Mockito.eq("since_version_detailed"), Mockito.isNull(String.class), Mockito.eq(param), Mockito.anyString());
  }

  @Test
  public void abbreviation_of_snapshot_version() {
    when(snapshot.getPeriodMode(periodIndex)).thenReturn(CoreProperties.TIMEMACHINE_MODE_VERSION);
    when(snapshot.getPeriodDate(periodIndex)).thenReturn(new Date());
    when(snapshot.getPeriodModeParameter(periodIndex)).thenReturn(param);

    periods.abbreviation(snapshot, periodIndex);
    verify(i18n).message(Mockito.any(Locale.class), Mockito.eq("since_version_detailed.short"), Mockito.isNull(String.class), Mockito.eq(param), Mockito.anyString());
  }

  @Test
  public void label_of_previous_analysis_with_date() {
    when(snapshot.getPeriodMode(periodIndex)).thenReturn(CoreProperties.TIMEMACHINE_MODE_PREVIOUS_ANALYSIS);
    when(snapshot.getPeriodDate(periodIndex)).thenReturn(new Date());

    periods.label(snapshot, periodIndex);
    verify(i18n).message(Mockito.any(Locale.class), Mockito.eq("since_previous_analysis_detailed"), Mockito.isNull(String.class), Mockito.anyString());
  }

  @Test
  public void label_of_previous_analysis_without_date() {
    when(snapshot.getPeriodMode(periodIndex)).thenReturn(CoreProperties.TIMEMACHINE_MODE_PREVIOUS_ANALYSIS);
    when(snapshot.getPeriodDate(periodIndex)).thenReturn(null);
    when(snapshot.getPeriodModeParameter(periodIndex)).thenReturn(param);

    periods.label(snapshot, periodIndex);
    verify(i18n).message(Mockito.any(Locale.class), Mockito.eq("since_previous_analysis"), Mockito.isNull(String.class));
  }

  @Test
  public void abbreviation_of_previous_analysis_with_date() {
    when(snapshot.getPeriodMode(periodIndex)).thenReturn(CoreProperties.TIMEMACHINE_MODE_PREVIOUS_ANALYSIS);
    when(snapshot.getPeriodDate(periodIndex)).thenReturn(new Date());

    periods.abbreviation(snapshot, periodIndex);
    verify(i18n).message(Mockito.any(Locale.class), Mockito.eq("since_previous_analysis_detailed.short"), Mockito.isNull(String.class), Mockito.anyString());
  }

  @Test
  public void abbreviation_of_previous_analysis_without_date() {
    when(snapshot.getPeriodMode(periodIndex)).thenReturn(CoreProperties.TIMEMACHINE_MODE_PREVIOUS_ANALYSIS);
    when(snapshot.getPeriodDate(periodIndex)).thenReturn(null);
    when(snapshot.getPeriodModeParameter(periodIndex)).thenReturn(param);

    periods.abbreviation(snapshot, periodIndex);
    verify(i18n).message(Mockito.any(Locale.class), Mockito.eq("since_previous_analysis.short"), Mockito.isNull(String.class));
  }

  @Test
  public void shouldReturnSnapshotLabelInModePreviousVersionWithParamNotNull() {
    when(snapshot.getPeriodMode(periodIndex)).thenReturn(CoreProperties.TIMEMACHINE_MODE_PREVIOUS_VERSION);
    when(snapshot.getPeriodModeParameter(periodIndex)).thenReturn(param);

    periods.label(snapshot, periodIndex);
    verify(i18n).message(Mockito.any(Locale.class), Mockito.eq("since_previous_version_detailed"), Mockito.isNull(String.class), Mockito.eq(param));
  }

  @Test
  public void shouldReturnSnapshotLabelInModePreviousVersionWithParamNull() {
    when(snapshot.getPeriodMode(periodIndex)).thenReturn(CoreProperties.TIMEMACHINE_MODE_PREVIOUS_VERSION);
    when(snapshot.getPeriodModeParameter(periodIndex)).thenReturn(null);

    periods.label(snapshot, periodIndex);
    verify(i18n).message(Mockito.any(Locale.class), Mockito.eq("since_previous_version"), Mockito.isNull(String.class));
  }

  @Test
  public void shouldReturnSnapshotLabelInModeDate() {
    when(snapshot.getPeriodMode(periodIndex)).thenReturn(CoreProperties.TIMEMACHINE_MODE_DATE);
    when(snapshot.getPeriodDate(periodIndex)).thenReturn(new Date());

    periods.label(snapshot, periodIndex);

    verify(i18n).message(Mockito.any(Locale.class), Mockito.eq("since_x"), Mockito.isNull(String.class), Mockito.anyString());
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
    verify(i18n).message(Mockito.any(Locale.class), Mockito.eq("over_x_days"), Mockito.isNull(String.class), Mockito.eq(days));
  }

  @Test
  public void shouldReturnLabelInModeVersion() {
    int periodIndex = 1;
    String version = "3.5";
    settings.setProperty(CoreProperties.TIMEMACHINE_PERIOD_PREFIX + periodIndex, version);

    periods.label(periodIndex);
    verify(i18n).message(Mockito.any(Locale.class), Mockito.eq("since_version"), Mockito.isNull(String.class), Mockito.eq(version));
  }

  @Test
  public void shouldReturnLabelInModePreviousAnalysis() {
    int periodIndex = 1;
    settings.setProperty(CoreProperties.TIMEMACHINE_PERIOD_PREFIX + periodIndex, CoreProperties.TIMEMACHINE_MODE_PREVIOUS_ANALYSIS);

    periods.label(periodIndex);
    verify(i18n).message(Mockito.any(Locale.class), Mockito.eq("since_previous_analysis"), Mockito.isNull(String.class));
  }

  @Test
  public void label_of_previous_version() {
    int periodIndex = 1;
    settings.setProperty(CoreProperties.TIMEMACHINE_PERIOD_PREFIX + periodIndex, CoreProperties.TIMEMACHINE_MODE_PREVIOUS_VERSION);

    periods.label(periodIndex);
    verify(i18n).message(Mockito.any(Locale.class), Mockito.eq("since_previous_version"), Mockito.isNull(String.class));
  }

  @Test
  public void abbreviation_of_previous_version() {
    int periodIndex = 1;
    settings.setProperty(CoreProperties.TIMEMACHINE_PERIOD_PREFIX + periodIndex, CoreProperties.TIMEMACHINE_MODE_PREVIOUS_VERSION);

    periods.abbreviation(periodIndex);
    verify(i18n).message(Mockito.any(Locale.class), Mockito.eq("since_previous_version.short"), Mockito.isNull(String.class));
  }

  @Test
  public void label_of_date() {
    int periodIndex = 1;
    settings.setProperty(CoreProperties.TIMEMACHINE_PERIOD_PREFIX + periodIndex, "2012-12-12");

    periods.label(periodIndex);

    verify(i18n).message(Mockito.any(Locale.class), Mockito.eq("since_x"), Mockito.isNull(String.class), Mockito.anyString());
  }

  @Test
  public void abbreviation_of_date() {
    int periodIndex = 1;
    settings.setProperty(CoreProperties.TIMEMACHINE_PERIOD_PREFIX + periodIndex, "2012-12-12");

    periods.abbreviation(periodIndex);

    verify(i18n).message(Mockito.any(Locale.class), Mockito.eq("since_x.short"), Mockito.isNull(String.class), Mockito.anyString());
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldNotSupportUnknownModeForLabel() {
    int periodIndex = 1;
    settings.setProperty(CoreProperties.TIMEMACHINE_PERIOD_PREFIX + periodIndex, "");

    periods.label(periodIndex);
  }


}
