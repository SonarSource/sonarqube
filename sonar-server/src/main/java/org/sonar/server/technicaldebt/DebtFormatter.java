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

import org.sonar.api.ServerComponent;
import org.sonar.api.utils.WorkDuration;
import org.sonar.api.utils.WorkDurationFactory;
import org.sonar.core.i18n.DefaultI18n;

import java.util.Locale;

public class DebtFormatter implements ServerComponent {

  private final DefaultI18n defaultI18n;
  private final WorkDurationFactory workDurationFactory;

  public DebtFormatter(DefaultI18n defaultI18n, WorkDurationFactory workDurationFactory) {
    this.defaultI18n = defaultI18n;
    this.workDurationFactory = workDurationFactory;
  }

  public String format(Locale locale, long debt) {
    return formatWorkDuration(locale, workDurationFactory.createFromSeconds(debt));
  }

  private String formatWorkDuration(Locale locale, WorkDuration debt) {
    StringBuilder message = new StringBuilder();
    if (debt.days() > 0) {
      message.append(defaultI18n.message(locale, "issue.technical_debt.x_days", null, debt.days()));
    }
    if (debt.hours() > 0) {
      if (message.length() > 0) {
        message.append(" ");
      }
      message.append(defaultI18n.message(locale, "issue.technical_debt.x_hours", null, debt.hours()));
    }
    // Do not display minutes if days is not null to not have too much information
    if (debt.minutes() > 0 && debt.days() == 0) {
      if (message.length() > 0) {
        message.append(" ");
      }
      message.append(defaultI18n.message(locale, "issue.technical_debt.x_minutes", null, debt.minutes()));
    }
    return message.toString();
  }
}
