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

import org.sonar.api.ServerComponent;
import org.sonar.api.utils.WorkDuration;
import org.sonar.api.utils.WorkDurationFactory;
import org.sonar.core.i18n.DefaultI18n;
import org.sonar.server.user.UserSession;

import java.util.Locale;

public class WorkDurationFormatter implements ServerComponent {

  private final DefaultI18n defaultI18n;
  private final WorkDurationFactory workDurationFactory;

  public WorkDurationFormatter(DefaultI18n defaultI18n, WorkDurationFactory workDurationFactory) {
    this.defaultI18n = defaultI18n;
    this.workDurationFactory = workDurationFactory;
  }

  public String format(long durationInSeconds) {
    return formatWorkDuration(UserSession.get().locale(), workDurationFactory.createFromSeconds(durationInSeconds), false);
  }

  public String abbreviation(long durationInSeconds) {
    return formatWorkDuration(UserSession.get().locale(), workDurationFactory.createFromSeconds(durationInSeconds), true);
  }

  private String formatWorkDuration(Locale locale, WorkDuration workDuration, boolean shortLabel) {
    StringBuilder message = new StringBuilder();
    if (workDuration.days() > 0) {
      message.append(message(locale, "work_duration.x_days", shortLabel, workDuration.days()));
    }
    if (workDuration.hours() > 0) {
      if (message.length() > 0) {
        message.append(" ");
      }
      message.append(message(locale, "work_duration.x_hours", shortLabel, workDuration.hours()));
    }
    if (workDuration.minutes() > 0) {
      if (message.length() > 0) {
        message.append(" ");
      }
      message.append(message(locale, "work_duration.x_minutes", shortLabel, workDuration.minutes()));
    }
    return message.toString();
  }

  private String message(Locale locale, String key, boolean shortLabel, Object... parameters) {
    String msgKey = key;
    if (shortLabel) {
      msgKey += ".short";
    }
    return defaultI18n.message(locale, msgKey, null, parameters);
  }
}
