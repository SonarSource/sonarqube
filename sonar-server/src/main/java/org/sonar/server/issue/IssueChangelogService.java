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
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.IssueQuery;
import org.sonar.api.issue.IssueQueryResult;
import org.sonar.api.issue.internal.FieldDiffs;
import org.sonar.api.user.User;
import org.sonar.api.user.UserFinder;
import org.sonar.api.web.UserRole;
import org.sonar.core.issue.db.IssueChangeDao;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.user.UserSession;

import java.util.Collection;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

/**
 * @since 3.6
 */
public class IssueChangelogService implements ServerComponent {

  private final IssueChangeDao changeDao;
  private final UserFinder userFinder;
  private final DefaultIssueFinder finder;
  private final IssueChangelogFormatter formatter;

  public IssueChangelogService(IssueChangeDao changeDao, UserFinder userFinder, DefaultIssueFinder finder, IssueChangelogFormatter formatter) {
    this.changeDao = changeDao;
    this.userFinder = userFinder;
    this.finder = finder;
    this.formatter = formatter;
  }

  public IssueChangelog changelog(String issueKey) {
    Issue issue = loadIssue(issueKey).first();
    return changelog(issue);
  }

  public IssueChangelog changelog(Issue issue) {
    List<FieldDiffs> changes = changeDao.selectChangelogByIssue(issue.key());

    // Load users
    List<String> logins = newArrayList();
    for (FieldDiffs change : changes) {
      if (change.userLogin() != null) {
        logins.add(change.userLogin());
      }
    }
    Collection<User> users = userFinder.findByLogins(logins);
    return new IssueChangelog(changes, users);
  }

  public IssueQueryResult loadIssue(String issueKey) {
    IssueQueryResult result = finder.find(IssueQuery.builder().issueKeys(newArrayList(issueKey)).requiredRole(UserRole.USER).build());
    if (result.issues().size() != 1) {
      throw new NotFoundException("Issue not found: " + issueKey);
    }
    return result;
  }

  public List<String> formatDiffs(FieldDiffs diffs) {
    return formatter.format(UserSession.get().locale(), diffs);
  }
}
