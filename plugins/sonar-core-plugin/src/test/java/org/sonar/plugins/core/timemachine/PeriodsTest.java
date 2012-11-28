/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.plugins.core.timemachine;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sonar.api.CoreProperties;
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
  private I18n i18n;

  private int periodIndex;
  private String param;

  @Before
  public void before() {
    periodIndex = 1;
    param = "10";

    snapshot = mock(Snapshot.class);
    i18n = mock(I18n.class);

    when(snapshot.getPeriodModeParameter(periodIndex)).thenReturn(param);

    periods = new Periods(snapshot, i18n);
  }

  @Test
  public void shouldReturnLabelInModeDays() {
    when(snapshot.getPeriodMode(periodIndex)).thenReturn(CoreProperties.TIMEMACHINE_MODE_DAYS);
    when(snapshot.getPeriodDate(periodIndex)).thenReturn(new Date());
    when(snapshot.getPeriodModeParameter(periodIndex)).thenReturn(param);

    periods.getLabel(periodIndex);
    verify(i18n).message(Mockito.any(Locale.class), Mockito.eq("over_x_days"), Mockito.isNull(String.class), Mockito.eq(param));
  }

  @Test
  public void shouldReturnLabelInModeVersion() {
    when(snapshot.getPeriodMode(periodIndex)).thenReturn(CoreProperties.TIMEMACHINE_MODE_VERSION);
    when(snapshot.getPeriodDate(periodIndex)).thenReturn(new Date());
    when(snapshot.getPeriodModeParameter(periodIndex)).thenReturn(param);

    periods.getLabel(periodIndex);

    verify(i18n).message(Mockito.any(Locale.class), Mockito.eq("since_version_detailed"), Mockito.isNull(String.class), Mockito.eq(param), Mockito.anyString());
  }

  @Test
     public void shouldReturnLabelInModePreviousAnalysis() {
    when(snapshot.getPeriodMode(periodIndex)).thenReturn(CoreProperties.TIMEMACHINE_MODE_VERSION);
    when(snapshot.getPeriodDate(periodIndex)).thenReturn(new Date());
    when(snapshot.getPeriodModeParameter(periodIndex)).thenReturn(param);

    periods.getLabel(periodIndex);

    verify(i18n).message(Mockito.any(Locale.class), Mockito.eq("since_version_detailed"), Mockito.isNull(String.class), Mockito.eq(param), Mockito.anyString());

    when(snapshot.getPeriodMode(periodIndex)).thenReturn(CoreProperties.TIMEMACHINE_MODE_VERSION);
    when(snapshot.getPeriodDate(periodIndex)).thenReturn(null);
    when(snapshot.getPeriodModeParameter(periodIndex)).thenReturn(param);

    periods.getLabel(periodIndex);

    verify(i18n).message(Mockito.any(Locale.class), Mockito.eq("since_version"), Mockito.isNull(String.class), Mockito.eq(param));
  }

  @Test
  public void shouldReturnLabelInModePreviousVersion() {
    when(snapshot.getPeriodMode(periodIndex)).thenReturn(CoreProperties.TIMEMACHINE_MODE_PREVIOUS_VERSION);
    when(snapshot.getPeriodModeParameter(periodIndex)).thenReturn(param);

    periods.getLabel(periodIndex);

    verify(i18n).message(Mockito.any(Locale.class), Mockito.eq("since_previous_version_detailed"), Mockito.isNull(String.class), Mockito.eq(param));

    when(snapshot.getPeriodMode(periodIndex)).thenReturn(CoreProperties.TIMEMACHINE_MODE_PREVIOUS_VERSION);
    when(snapshot.getPeriodModeParameter(periodIndex)).thenReturn(null);

    periods.getLabel(periodIndex);

    verify(i18n).message(Mockito.any(Locale.class), Mockito.eq("since_previous_version"), Mockito.isNull(String.class));
  }

  @Test
  public void shouldReturnLabelInModeDate() {
    when(snapshot.getPeriodMode(periodIndex)).thenReturn(CoreProperties.TIMEMACHINE_MODE_DATE);
    when(snapshot.getPeriodDate(periodIndex)).thenReturn(new Date());

    periods.getLabel(periodIndex);

    verify(i18n).message(Mockito.any(Locale.class), Mockito.eq("since_x"), Mockito.isNull(String.class), Mockito.anyString());
  }

  @Test(expected = IllegalStateException.class)
  public void shouldNotSupportUnknownMode() {
    when(snapshot.getPeriodMode(periodIndex)).thenReturn("Unknown mode");

    periods.getLabel(periodIndex);
  }
}
