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
package org.sonar.batch.scan;

import org.junit.Test;
import org.sonar.batch.scan.DurationLabel;

import java.text.MessageFormat;

import static org.fest.assertions.Assertions.assertThat;

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
  public void testAgoSeconds() {
    DurationLabel durationLabel = new DurationLabel();
    String label = durationLabel.label(now() - System.currentTimeMillis());
    String expected = durationLabel.join(durationLabel.getSeconds(), durationLabel.getSuffixAgo());
    assertThat(label).isEqualTo(expected);
  }

  @Test
  public void testAgoMinute() {
    DurationLabel durationLabel = new DurationLabel();
    String label = durationLabel.label(now() - ago(MINUTE));
    String expected = durationLabel.join(durationLabel.getMinute(), durationLabel.getSuffixAgo());
    assertThat(label).isEqualTo(expected);
  }

  @Test
  public void testAgoMinutes() {
    DurationLabel durationlabel = new DurationLabel();
    int minutes = 2;
    String label = durationlabel.label(now() - ago(minutes * MINUTE));
    String expected = durationlabel.join(
        MessageFormat.format(durationlabel.getMinutes(), minutes), durationlabel.getSuffixAgo());
    assertThat(label).isEqualTo(expected);
  }

  @Test
  public void testAgoHour() {
    DurationLabel durationLabel = new DurationLabel();
    String label = durationLabel.label(now() - ago(HOUR));
    String expected = durationLabel.join(durationLabel.getHour(), durationLabel.getSuffixAgo());
    assertThat(label).isEqualTo(expected);
  }

  @Test
  public void testAgoHours() {
    DurationLabel durationLabel = new DurationLabel();
    long hours = 3;
    String label = durationLabel.label(now() - ago(hours * HOUR));
    String expected = durationLabel.join(MessageFormat.format(durationLabel.getHours(), hours), durationLabel.getSuffixAgo());
    assertThat(label).isEqualTo(expected);
  }

  @Test
  public void testAgoDay() {
    DurationLabel durationLabel = new DurationLabel();
    String label = durationLabel.label(now() - ago(30 * HOUR));
    String expected = durationLabel.join(durationLabel.getDay(), durationLabel.getSuffixAgo());
    assertThat(label).isEqualTo(expected);
  }

  @Test
  public void testAgoDays() {
    DurationLabel durationLabel = new DurationLabel();
    long days = 4;
    String label = durationLabel.label(now() - ago(days * DAY));
    String expected = durationLabel.join(MessageFormat.format(durationLabel.getDays(), days), durationLabel.getSuffixAgo());
    assertThat(label).isEqualTo(expected);
  }

  @Test
  public void testAgoMonth() {
    DurationLabel durationLabel = new DurationLabel();
    String label = durationLabel.label(now() - ago(35 * DAY));
    String expected = durationLabel.join(durationLabel.getMonth(), durationLabel.getSuffixAgo());
    assertThat(label).isEqualTo(expected);
  }

  @Test
  public void testAgoMonths() {
    DurationLabel durationLabel = new DurationLabel();
    long months = 2;
    String label = durationLabel.label(now() - ago(months * MONTH));
    String expected = durationLabel.join(MessageFormat.format(durationLabel.getMonths(), months), durationLabel.getSuffixAgo());
    assertThat(label).isEqualTo(expected);
  }

  @Test
  public void testYearAgo() {
    DurationLabel durationLabel = new DurationLabel();
    String label = durationLabel.label(now() - ago(14 * MONTH));
    String expected = durationLabel.join(durationLabel.getYear(), durationLabel.getSuffixAgo());
    assertThat(label).isEqualTo(expected);
  }

  @Test
  public void testYearsAgo() {
    DurationLabel durationLabel = new DurationLabel();
    long years = 7;
    String label = durationLabel.label(now() - ago(years * YEAR));
    String expected = durationLabel.join(MessageFormat.format(durationLabel.getYears(), years), durationLabel.getSuffixAgo());
    assertThat(label).isEqualTo(expected);
  }

  private long ago(long offset) {
    return System.currentTimeMillis() - offset;
  }

  private long now() {
    return System.currentTimeMillis();
  }

}
