/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.issue;

import com.google.common.collect.Maps;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import javax.annotation.CheckForNull;
import org.sonar.api.user.User;
import org.sonar.core.issue.FieldDiffs;

/**
 * @since 3.6
 */
public class IssueChangelog {

  private final List<FieldDiffs> changes;
  private final Map<String, User> usersByLogin;

  public IssueChangelog(List<FieldDiffs> changes, Collection<User> users) {
    this.changes = changes;
    replacedTechnicalDebtByEffort(changes);
    this.usersByLogin = Maps.newHashMap();
    for (User user : users) {
      usersByLogin.put(user.login(), user);
    }
  }

  public List<FieldDiffs> changes() {
    return changes;
  }

  private static void replacedTechnicalDebtByEffort(List<FieldDiffs> changes) {
    for (FieldDiffs fieldDiffs : changes) {
      Map<String, FieldDiffs.Diff> diffs = fieldDiffs.diffs();
      for (Map.Entry<String, FieldDiffs.Diff> entry : diffs.entrySet()) {
        // As "technicalDebt" couldn't been updated to "effort" in db, we need to convert it here to correctly display "effort" in WS/UI
        if (entry.getKey().equals(IssueUpdater.TECHNICAL_DEBT)) {
          diffs.put("effort", entry.getValue());
          diffs.remove(entry.getKey());
        }
      }
    }
  }

  @CheckForNull
  public User user(FieldDiffs change) {
    if (change.userLogin() == null) {
      return null;
    }
    return usersByLogin.get(change.userLogin());
  }
}
