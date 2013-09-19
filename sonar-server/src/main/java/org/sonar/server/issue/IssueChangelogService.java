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

import com.google.common.collect.Lists;
import org.sonar.api.ServerComponent;
import org.sonar.api.issue.internal.FieldDiffs;
import org.sonar.api.user.User;
import org.sonar.api.user.UserFinder;
import org.sonar.core.issue.db.IssueChangeDao;
import org.sonar.server.user.UserSession;

import java.util.Collection;
import java.util.List;

/**
 * @since 3.6
 */
public class IssueChangelogService implements ServerComponent {

  private final IssueChangeDao changeDao;
  private final UserFinder userFinder;

  public IssueChangelogService(IssueChangeDao changeDao, UserFinder userFinder) {
    this.changeDao = changeDao;
    this.userFinder = userFinder;
  }

  public IssueChangelog changelog(String issueKey, UserSession userSession) {
    // TODO verify security
    List<FieldDiffs> changes = changeDao.selectChangelogByIssue(issueKey);

    // Load users
    List<String> logins = Lists.newArrayList();
    for (FieldDiffs change : changes) {
      if (change.userLogin() != null) {
        logins.add(change.userLogin());
      }
    }
    Collection<User> users = userFinder.findByLogins(logins);

    return new IssueChangelog(changes, users);
  }
}
