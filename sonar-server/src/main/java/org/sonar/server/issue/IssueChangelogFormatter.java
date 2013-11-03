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
package org.sonar.server.issue;

import org.sonar.api.ServerComponent;
import org.sonar.api.issue.internal.FieldDiffs;
import org.sonar.api.technicaldebt.TechnicalDebt;
import org.sonar.core.i18n.I18nManager;
import org.sonar.core.issue.IssueUpdater;
import org.sonar.server.technicaldebt.TechnicalDebtFormatter;

import java.io.Serializable;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;

public class IssueChangelogFormatter implements ServerComponent {

  private final I18nManager i18nManager;
  private final TechnicalDebtFormatter technicalDebtFormatter;

  public IssueChangelogFormatter(I18nManager i18nManager, TechnicalDebtFormatter technicalDebtFormatter) {
    this.i18nManager = i18nManager;
    this.technicalDebtFormatter = technicalDebtFormatter;
  }

  public List<String> format(Locale locale, FieldDiffs diffs) {
    List<String> result = newArrayList();
    for (Map.Entry<String, FieldDiffs.Diff> entry : diffs.diffs().entrySet()) {
      StringBuilder message = new StringBuilder();
      String key = entry.getKey();
      IssueChangelogDiffFormat diffFormat = format(locale, key, entry.getValue());
      if (diffFormat.newValue() != null) {
        message.append(i18nManager.message(locale, "issue.changelog.changed_to", null, i18nManager.message(locale, "issue.changelog.field." + key, null), diffFormat.newValue()));
      } else {
        message.append(i18nManager.message(locale, "issue.changelog.removed", null, i18nManager.message(locale, "issue.changelog.field." + key, null)));
      }
      if (diffFormat.oldValue() != null) {
        message.append(" (");
        message.append(i18nManager.message(locale, "issue.changelog.was", null, diffFormat.oldValue()));
        message.append(")");
      }
      result.add(message.toString());
    }
    return result;
  }

  private IssueChangelogDiffFormat format(Locale locale, String key, FieldDiffs.Diff diff) {
    Serializable newValue = diff.newValue();
    Serializable oldValue = diff.oldValue();

    String newValueString = newValue != null && !newValue.equals("") ? diff.newValue().toString() : null;
    String oldValueString = oldValue != null && !oldValue.equals("") ? diff.oldValue().toString() : null;
    if (IssueUpdater.TECHNICAL_DEBT.equals(key)) {
      if (newValueString != null) {
        newValueString = technicalDebtFormatter.format(locale, TechnicalDebt.fromLong(Long.parseLong(newValueString)));
      }
      if (oldValueString != null) {
        oldValueString = technicalDebtFormatter.format(locale, TechnicalDebt.fromLong(Long.parseLong(oldValueString)));
      }
    }
    return new IssueChangelogDiffFormat(oldValueString, newValueString);
  }

}
