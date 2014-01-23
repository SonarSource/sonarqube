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
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.api.issue.internal.WorkDayDuration;
import org.sonar.core.i18n.DefaultI18n;

import java.util.Locale;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TechnicalDebtFormatterTest {

  private static final Locale DEFAULT_LOCALE = Locale.getDefault();

  @Mock
  private DefaultI18n defaultI18n;

  private TechnicalDebtFormatter formatter;

  @Before
  public void before() {
    formatter = new TechnicalDebtFormatter(defaultI18n);
  }

  @Test
  public void format() {
    when(defaultI18n.message(DEFAULT_LOCALE, "issue.technical_debt.x_days", null, 5)).thenReturn("5 days");
    when(defaultI18n.message(DEFAULT_LOCALE, "issue.technical_debt.x_hours", null, 2)).thenReturn("2 hours");
    when(defaultI18n.message(DEFAULT_LOCALE, "issue.technical_debt.x_minutes", null, 1)).thenReturn("1 minutes");

    assertThat(formatter.format(DEFAULT_LOCALE, WorkDayDuration.of(0, 0, 5))).isEqualTo("5 days");
    assertThat(formatter.format(DEFAULT_LOCALE, WorkDayDuration.of(0, 2, 0))).isEqualTo("2 hours");
    assertThat(formatter.format(DEFAULT_LOCALE, WorkDayDuration.of(1, 0, 0))).isEqualTo("1 minutes");

    assertThat(formatter.format(DEFAULT_LOCALE, WorkDayDuration.of(0, 2, 5))).isEqualTo("5 days 2 hours");
    assertThat(formatter.format(DEFAULT_LOCALE, WorkDayDuration.of(1, 2, 0))).isEqualTo("2 hours 1 minutes");
    assertThat(formatter.format(DEFAULT_LOCALE, WorkDayDuration.of(1, 2, 5))).isEqualTo("5 days 2 hours");
  }


}
