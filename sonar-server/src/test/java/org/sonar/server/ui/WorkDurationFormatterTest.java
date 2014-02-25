/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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

package org.sonar.server.ui;

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.CoreProperties;
import org.sonar.api.config.Settings;
import org.sonar.api.utils.WorkDurationFactory;
import org.sonar.core.i18n.DefaultI18n;

import java.util.Locale;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WorkDurationFormatterTest {

  static final int HOURS_IN_DAY = 8;

  static final long ONE_MINUTE = 1L;
  static final long ONE_HOUR = ONE_MINUTE * 60;
  static final long ONE_DAY = HOURS_IN_DAY * ONE_HOUR;

  DefaultI18n i18n = mock(DefaultI18n.class);
  WorkDurationFormatter formatter;

  @Before
  public void setUp() throws Exception {
    Settings settings = new Settings();
    settings.setProperty(CoreProperties.HOURS_IN_DAY, Integer.toString(HOURS_IN_DAY));
    formatter = new WorkDurationFormatter(i18n, new WorkDurationFactory(settings));

    when(i18n.message(any(Locale.class), eq("work_duration.x_days"), eq((String) null), eq(5))).thenReturn("5 days");
    when(i18n.message(any(Locale.class), eq("work_duration.x_days.short"), eq((String) null), eq(5))).thenReturn("5d");
    when(i18n.message(any(Locale.class), eq("work_duration.x_hours"), eq((String) null), eq(2))).thenReturn("2 hours");
    when(i18n.message(any(Locale.class), eq("work_duration.x_hours.short"), eq((String) null), eq(2))).thenReturn("2h");
    when(i18n.message(any(Locale.class), eq("work_duration.x_minutes"), eq((String) null), eq(1))).thenReturn("1 minutes");
    when(i18n.message(any(Locale.class), eq("work_duration.x_minutes.short"), eq((String) null), eq(1))).thenReturn("1min");
  }

  @Test
  public void long_format() {
    assertThat(formatter.format(5 * ONE_DAY, WorkDurationFormatter.Format.LONG)).isEqualTo("5 days");
    assertThat(formatter.format(2 * ONE_HOUR, WorkDurationFormatter.Format.LONG)).isEqualTo("2 hours");
    assertThat(formatter.format(ONE_MINUTE, WorkDurationFormatter.Format.LONG)).isEqualTo("1 minutes");

    assertThat(formatter.format(5 * ONE_DAY + 2 * ONE_HOUR, WorkDurationFormatter.Format.LONG)).isEqualTo("5 days 2 hours");
    assertThat(formatter.format(2 * ONE_HOUR + ONE_MINUTE, WorkDurationFormatter.Format.LONG)).isEqualTo("2 hours 1 minutes");

    assertThat(formatter.format(5 * ONE_DAY + 2 * ONE_HOUR + ONE_MINUTE, WorkDurationFormatter.Format.LONG)).isEqualTo("5 days 2 hours 1 minutes");
  }

  @Test
  public void short_format() {
    assertThat(formatter.format(5 * ONE_DAY, WorkDurationFormatter.Format.SHORT)).isEqualTo("5d");
    assertThat(formatter.format(2 * ONE_HOUR, WorkDurationFormatter.Format.SHORT)).isEqualTo("2h");
    assertThat(formatter.format(ONE_MINUTE, WorkDurationFormatter.Format.SHORT)).isEqualTo("1min");

    assertThat(formatter.format(5 * ONE_DAY + 2 * ONE_HOUR, WorkDurationFormatter.Format.SHORT)).isEqualTo("5d 2h");
    assertThat(formatter.format(2 * ONE_HOUR + ONE_MINUTE, WorkDurationFormatter.Format.SHORT)).isEqualTo("2h 1min");

    assertThat(formatter.format(5 * ONE_DAY + 2 * ONE_HOUR + ONE_MINUTE, WorkDurationFormatter.Format.SHORT)).isEqualTo("5d 2h 1min");
  }

  @Test
  public void format_with_string_parameter() {
    assertThat(formatter.format(5 * ONE_DAY, "LONG")).isEqualTo("5 days");
    assertThat(formatter.format(5 * ONE_DAY, "SHORT")).isEqualTo("5d");
  }

  @Test
  public void display_zero_without_unit() {
    assertThat(formatter.format(0, WorkDurationFormatter.Format.SHORT)).isEqualTo("0");
  }

  @Test
  public void display_negative_duration() {
    assertThat(formatter.format(-5 * ONE_DAY, WorkDurationFormatter.Format.SHORT)).isEqualTo("-5d");
    assertThat(formatter.format(-2 * ONE_HOUR, WorkDurationFormatter.Format.SHORT)).isEqualTo("-2h");
    assertThat(formatter.format(-1 * ONE_MINUTE, WorkDurationFormatter.Format.SHORT)).isEqualTo("-1min");
  }

}
