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
package org.sonar.core.i18n;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import static org.fest.assertions.Assertions.assertThat;

@RunWith(MockitoJUnitRunner.class)
public class DurationLabelTest {

  // One second in milliseconds
  private static final long SECOND = 1000;

  // One minute in milliseconds
  private static final long MINUTE = 60 * SECOND;

  // One hour in milliseconds
  private static final long HOUR = 60 * MINUTE;

  // One day in milliseconds
  private static final long DAY = 24 * HOUR;

  // 30 days in milliseconds
  private static final long MONTH = 30 * DAY;

  // 365 days in milliseconds
  private static final long YEAR = 365 * DAY;

  @Test
  public void instant_seconds() {
    long now = System.currentTimeMillis();
    DurationLabel.Result result = DurationLabel.instant(now - System.currentTimeMillis());
    assertThat(result.key()).isEqualTo("duration.seconds");
    assertThat(result.value()).isNull();
  }

  @Test
  public void ago_seconds() {
    long now = System.currentTimeMillis();
    DurationLabel.Result result = DurationLabel.ago(now - System.currentTimeMillis());
    assertThat(result.key()).isEqualTo("duration.seconds.ago");
    assertThat(result.value()).isNull();
  }

  @Test
  public void ago_minute() {
    DurationLabel.Result result = DurationLabel.label(now() - ago(MINUTE), true);
    assertThat(result.key()).isEqualTo("duration.minute.ago");
    assertThat(result.value()).isNull();
  }

  @Test
  public void ago_minutes() {
    long minutes = 2;
    DurationLabel.Result result = DurationLabel.label(now() - ago(minutes * MINUTE), true);
    assertThat(result.key()).isEqualTo("duration.minutes.ago");
    assertThat(result.value()).isEqualTo(minutes);

  }

  @Test
  public void ago_hour() {
    DurationLabel.Result result = DurationLabel.label(now() - ago(HOUR), true);
    assertThat(result.key()).isEqualTo("duration.hour.ago");
    assertThat(result.value()).isNull();
  }

  @Test
  public void ago_hours() {
    long hours = 3;
    DurationLabel.Result result =  DurationLabel.label(now() - ago(hours * HOUR), true);
    assertThat(result.key()).isEqualTo("duration.hours.ago");
    assertThat(result.value()).isEqualTo(hours);
  }

  @Test
  public void ago_day() {
    DurationLabel.Result result = DurationLabel.label(now() - ago(30 * HOUR), true);
    assertThat(result.key()).isEqualTo("duration.day.ago");
    assertThat(result.value()).isNull();
  }

  @Test
  public void ago_days() {
    long days = 4;
    DurationLabel.Result result = DurationLabel.label(now() - ago(days * DAY), true);
    assertThat(result.key()).isEqualTo("duration.days.ago");
    assertThat(result.value()).isEqualTo(days);
  }

  @Test
  public void ago_month() {
    DurationLabel.Result result = DurationLabel.label(now() - ago(35 * DAY), true);
    assertThat(result.key()).isEqualTo("duration.month.ago");
    assertThat(result.value()).isNull();
  }

  @Test
  public void ago_months() {
    long months = 2;
    DurationLabel.Result result = DurationLabel.label(now() - ago(months * MONTH), true);
    assertThat(result.key()).isEqualTo("duration.months.ago");
    assertThat(result.value()).isEqualTo(months);
  }

  @Test
  public void year_ago() {
    DurationLabel.Result result = DurationLabel.label(now() - ago(14 * MONTH), true);
    assertThat(result.key()).isEqualTo("duration.year.ago");
    assertThat(result.value()).isNull();
  }

  @Test
  public void years_ago() {
    long years = 7;
    DurationLabel.Result result = DurationLabel.label(now() - ago(years * YEAR), true);
    assertThat(result.key()).isEqualTo("duration.years.ago");
    assertThat(result.value()).isEqualTo(years);
  }

  private long ago(long offset) {
    return System.currentTimeMillis() - offset;
  }

  private long now() {
    // Add 5 seconds in order to have zero false positive
    return System.currentTimeMillis() + 5000;
  }

}
