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
import org.sonar.api.issue.internal.WorkDayDuration;
import org.sonar.core.i18n.I18nManager;

import java.util.Locale;

public class TechnicalDebtFormatter implements ServerComponent {

  private final I18nManager i18nManager;

  public TechnicalDebtFormatter(I18nManager i18nManager) {
    this.i18nManager = i18nManager;
  }

  public String format(Locale locale, WorkDayDuration technicalDebt) {
    StringBuilder message = new StringBuilder();
    if (technicalDebt.days() > 0) {
      message.append(i18nManager.message(locale, "issue.technical_debt.x_days", null, technicalDebt.days()));
    }
    if (technicalDebt.hours() > 0) {
      if (message.length() > 0) {
        message.append(" ");
      }
      message.append(i18nManager.message(locale, "issue.technical_debt.x_hours", null, technicalDebt.hours()));
    }
    // Do not display minutes if days is not null to not have too much information
    if (technicalDebt.minutes() > 0 && technicalDebt.days() == 0) {
      if (message.length() > 0) {
        message.append(" ");
      }
      message.append(i18nManager.message(locale, "issue.technical_debt.x_minutes", null, technicalDebt.minutes()));
    }
    return message.toString();
  }
}
