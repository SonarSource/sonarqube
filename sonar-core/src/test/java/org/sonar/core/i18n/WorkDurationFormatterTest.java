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

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.CoreProperties;
import org.sonar.api.config.Settings;
import org.sonar.api.utils.WorkDurationFactory;

import static com.google.common.collect.Lists.newArrayList;
import static org.fest.assertions.Assertions.assertThat;

public class WorkDurationFormatterTest {

  static final int HOURS_IN_DAY = 8;

  static final long ONE_MINUTE = 1L;
  static final long ONE_HOUR = ONE_MINUTE * 60;
  static final long ONE_DAY = HOURS_IN_DAY * ONE_HOUR;

  Settings settings;

  WorkDurationFormatter formatter;

  @Before
  public void setUp() throws Exception {
    settings = new Settings();
    settings.setProperty(CoreProperties.HOURS_IN_DAY, Integer.toString(HOURS_IN_DAY));
    formatter = new WorkDurationFormatter(new WorkDurationFactory(settings));
  }

  @Test
  public void format() {
    assertThat(formatter.format(5 * ONE_DAY)).isEqualTo(newArrayList(new WorkDurationFormatter.Result("work_duration.x_days", 5)));
    assertThat(formatter.format(2 * ONE_HOUR)).isEqualTo(newArrayList(new WorkDurationFormatter.Result("work_duration.x_hours", 2)));
    assertThat(formatter.format(ONE_MINUTE)).isEqualTo(newArrayList(new WorkDurationFormatter.Result("work_duration.x_minutes", 1)));

    assertThat(formatter.format(5 * ONE_DAY + 2 * ONE_HOUR)).isEqualTo(newArrayList(
      new WorkDurationFormatter.Result("work_duration.x_days", 5),
      new WorkDurationFormatter.Result(" ", null),
      new WorkDurationFormatter.Result("work_duration.x_hours", 2)
    ));

    assertThat(formatter.format(2 * ONE_HOUR + ONE_MINUTE)).isEqualTo(newArrayList(
      new WorkDurationFormatter.Result("work_duration.x_hours", 2),
      new WorkDurationFormatter.Result(" ", null),
      new WorkDurationFormatter.Result("work_duration.x_minutes", 1)
    ));

    assertThat(formatter.format(5 * ONE_DAY + 2 * ONE_HOUR + ONE_MINUTE)).isEqualTo(newArrayList(
      new WorkDurationFormatter.Result("work_duration.x_days", 5),
      new WorkDurationFormatter.Result(" ", null),
      new WorkDurationFormatter.Result("work_duration.x_hours", 2)
    ));
  }

  @Test
  public void not_display_following_element_when_bigger_than_ten() {
    int hoursInDay = 15;
    settings.setProperty(CoreProperties.HOURS_IN_DAY, Integer.toString(hoursInDay));

    assertThat(formatter.format(15 * hoursInDay * ONE_HOUR + 2 * ONE_HOUR + ONE_MINUTE)).isEqualTo(newArrayList(new WorkDurationFormatter.Result("work_duration.x_days", 15)));

    assertThat(formatter.format(12 * ONE_HOUR + ONE_MINUTE)).isEqualTo(newArrayList(new WorkDurationFormatter.Result("work_duration.x_hours", 12)));
  }

  @Test
  public void display_zero_without_unit() {
    assertThat(formatter.format(0)).isEqualTo(newArrayList(new WorkDurationFormatter.Result("0", null)));
  }

  @Test
  public void display_negative_duration() {
    assertThat(formatter.format(-5 * ONE_DAY)).isEqualTo(newArrayList(new WorkDurationFormatter.Result("work_duration.x_days", -5)));
    assertThat(formatter.format(-2 * ONE_HOUR)).isEqualTo(newArrayList(new WorkDurationFormatter.Result("work_duration.x_hours", -2)));
    assertThat(formatter.format(-1 * ONE_MINUTE)).isEqualTo(newArrayList(new WorkDurationFormatter.Result("work_duration.x_minutes", -1)));
  }

}
