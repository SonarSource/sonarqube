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

package org.sonar.api.utils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.api.CoreProperties;
import org.sonar.api.config.Settings;
import org.sonar.api.i18n.I18n;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DurationsTest {

  static final int HOURS_IN_DAY = 8;

  static final long ONE_MINUTE = 1L;
  static final long ONE_HOUR = ONE_MINUTE * 60;
  static final long ONE_DAY = HOURS_IN_DAY * ONE_HOUR;

  @Mock
  I18n i18n;

  Locale locale = Locale.ENGLISH;

  Settings settings;

  Durations durations;

  @Before
  public void setUp() throws Exception {
    settings = new Settings();
    settings.setProperty(CoreProperties.HOURS_IN_DAY, HOURS_IN_DAY);
    durations = new Durations(settings, i18n);
  }

  @Test
  public void create_from_minutes() throws Exception {
    assertThat(durations.create(10L).toMinutes()).isEqualTo(10L);
  }

  @Test
  public void decode() throws Exception {
    // 1 working day -> 8 hours
    assertThat(durations.decode("1d").toMinutes()).isEqualTo(8L * ONE_HOUR);
    // 8 hours
    assertThat(durations.decode("8h").toMinutes()).isEqualTo(8L * ONE_HOUR);
  }

  @Test
  public void format() {
    when(i18n.message(eq(locale), eq("work_duration.x_days"), eq((String) null), eq(5))).thenReturn("5d");
    when(i18n.message(eq(locale), eq("work_duration.x_hours"), eq((String) null), eq(2))).thenReturn("2h");
    when(i18n.message(eq(locale), eq("work_duration.x_minutes"), eq((String) null), eq(1))).thenReturn("1min");

    assertThat(durations.format(locale, Duration.create(5 * ONE_DAY), Durations.DurationFormat.SHORT)).isEqualTo("5d");
    assertThat(durations.format(locale, Duration.create(2 * ONE_HOUR), Durations.DurationFormat.SHORT)).isEqualTo("2h");
    assertThat(durations.format(locale, Duration.create(ONE_MINUTE), Durations.DurationFormat.SHORT)).isEqualTo("1min");

    assertThat(durations.format(locale, Duration.create(5 * ONE_DAY + 2 * ONE_HOUR), Durations.DurationFormat.SHORT)).isEqualTo("5d 2h");
    assertThat(durations.format(locale, Duration.create(2 * ONE_HOUR + ONE_MINUTE), Durations.DurationFormat.SHORT)).isEqualTo("2h 1min");
    assertThat(durations.format(locale, Duration.create(5 * ONE_DAY + 2 * ONE_HOUR + ONE_MINUTE), Durations.DurationFormat.SHORT)).isEqualTo("5d 2h");
  }

  @Test
  public void not_display_following_element_when_bigger_than_ten() {
    int hoursInDay = 15;
    settings.setProperty(CoreProperties.HOURS_IN_DAY, Integer.toString(hoursInDay));

    when(i18n.message(eq(locale), eq("work_duration.x_days"), eq((String) null), eq(15))).thenReturn("15d");
    when(i18n.message(eq(locale), eq("work_duration.x_hours"), eq((String) null), eq(12))).thenReturn("12h");

    assertThat(durations.format(locale, Duration.create(15 * hoursInDay * ONE_HOUR + 2 * ONE_HOUR + ONE_MINUTE), Durations.DurationFormat.SHORT)).isEqualTo("15d");
    assertThat(durations.format(locale, Duration.create(12 * ONE_HOUR + ONE_MINUTE), Durations.DurationFormat.SHORT)).isEqualTo("12h");
  }

  @Test
  public void display_zero_without_unit() {
    assertThat(durations.format(locale, Duration.create(0), Durations.DurationFormat.SHORT)).isEqualTo("0");
  }

  @Test
  public void display_negative_duration() {
    when(i18n.message(eq(locale), eq("work_duration.x_days"), eq((String) null), eq(-5))).thenReturn("-5d");
    when(i18n.message(eq(locale), eq("work_duration.x_hours"), eq((String) null), eq(-2))).thenReturn("-2h");
    when(i18n.message(eq(locale), eq("work_duration.x_minutes"), eq((String) null), eq(-1))).thenReturn("-1min");

    assertThat(durations.format(locale, Duration.create(-5 * ONE_DAY), Durations.DurationFormat.SHORT)).isEqualTo("-5d");
    assertThat(durations.format(locale, Duration.create(-2 * ONE_HOUR), Durations.DurationFormat.SHORT)).isEqualTo("-2h");
    assertThat(durations.format(locale, Duration.create(-1 * ONE_MINUTE), Durations.DurationFormat.SHORT)).isEqualTo("-1min");
  }

}
