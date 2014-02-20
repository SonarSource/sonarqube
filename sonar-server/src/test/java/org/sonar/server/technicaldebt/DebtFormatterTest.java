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

package org.sonar.server.technicaldebt;

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.CoreProperties;
import org.sonar.api.config.Settings;
import org.sonar.api.utils.WorkDurationFactory;
import org.sonar.core.i18n.DefaultI18n;

import java.util.Locale;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DebtFormatterTest {

  private static final Locale DEFAULT_LOCALE = Locale.getDefault();
  private static final int HOURS_IN_DAY = 8;

  private static final long ONE_MINUTE = 60;
  private static final long ONE_HOUR = 60 * ONE_MINUTE;
  private static final long ONE_DAY = HOURS_IN_DAY * ONE_HOUR;

  DefaultI18n i18n = mock(DefaultI18n.class);
  DebtFormatter formatter;

  @Before
  public void setUp() throws Exception {
    Settings settings = new Settings();
    settings.setProperty(CoreProperties.HOURS_IN_DAY, Integer.toString(HOURS_IN_DAY));
    formatter = new DebtFormatter(i18n, new WorkDurationFactory(settings));

    when(i18n.message(DEFAULT_LOCALE, "issue.technical_debt.x_days", null, 5)).thenReturn("5 days");
    when(i18n.message(DEFAULT_LOCALE, "issue.technical_debt.x_hours", null, 2)).thenReturn("2 hours");
    when(i18n.message(DEFAULT_LOCALE, "issue.technical_debt.x_minutes", null, 1)).thenReturn("1 minutes");
  }

  @Test
  public void format_from_seconds() {
    assertThat(formatter.format(DEFAULT_LOCALE, 5 * ONE_DAY)).isEqualTo("5 days");
    assertThat(formatter.format(DEFAULT_LOCALE, 2 * ONE_HOUR)).isEqualTo("2 hours");
    assertThat(formatter.format(DEFAULT_LOCALE, ONE_MINUTE)).isEqualTo("1 minutes");

    assertThat(formatter.format(DEFAULT_LOCALE, 5 * ONE_DAY + 2 * ONE_HOUR)).isEqualTo("5 days 2 hours");
    assertThat(formatter.format(DEFAULT_LOCALE, 2 * ONE_HOUR + ONE_MINUTE)).isEqualTo("2 hours 1 minutes");
  }

  @Test
  public void format_from_seconds_not_display_minutes_if_hours_exists() {
    // 5 days 2 hours 1 minute -> 1 minute is not displayed
    assertThat(formatter.format(DEFAULT_LOCALE, 5 * ONE_DAY + 2 * ONE_HOUR + ONE_MINUTE)).isEqualTo("5 days 2 hours");
  }

}
