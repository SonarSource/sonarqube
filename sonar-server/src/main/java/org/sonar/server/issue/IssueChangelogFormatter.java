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
import org.sonar.core.i18n.I18nManager;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;

public class IssueChangelogFormatter implements ServerComponent {

  private final I18nManager i18nManager;

  public IssueChangelogFormatter(I18nManager i18nManager) {
    this.i18nManager = i18nManager;
  }

  public List<String> format(Locale locale, FieldDiffs diffs) {
    List<String> result = newArrayList();
    for (Map.Entry<String, FieldDiffs.Diff> entry : diffs.diffs().entrySet()) {
      StringBuilder message = new StringBuilder();
      String key = entry.getKey();
      FieldDiffs.Diff diff = entry.getValue();
      if (diff.newValue() != null && !diff.newValue().equals("")) {
        message.append(i18nManager.message(locale, "issue.changelog.changed_to", null, i18nManager.message(locale, "issue.changelog.field." + key, null), diff.newValue()));
      } else {
        message.append(i18nManager.message(locale, "issue.changelog.removed", null, i18nManager.message(locale, "issue.changelog.field." + key, null)));
      }
      if (diff.oldValue() != null && !diff.oldValue().equals("")) {
        message.append(" (");
        message.append(i18nManager.message(locale, "issue.changelog.was", null, diff.oldValue()));
        message.append(")");
      }
      result.add(message.toString());
    }
    return result;
  }

}
