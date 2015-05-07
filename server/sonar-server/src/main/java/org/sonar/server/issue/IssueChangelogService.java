/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
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

import org.sonar.api.ServerSide;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.internal.FieldDiffs;
import org.sonar.api.user.User;
import org.sonar.api.user.UserFinder;
import org.sonar.core.issue.db.IssueChangeDao;
import org.sonar.server.user.UserSession;

import java.util.Collection;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

/**
 * @since 3.6
 */
@ServerSide
public class IssueChangelogService {

  private final IssueChangeDao changeDao;
  private final UserFinder userFinder;
  private final IssueService issueService;
  private final IssueChangelogFormatter formatter;

  public IssueChangelogService(IssueChangeDao changeDao, UserFinder userFinder, IssueService issueService, IssueChangelogFormatter formatter) {
    this.changeDao = changeDao;
    this.userFinder = userFinder;
    this.issueService = issueService;
    this.formatter = formatter;
  }

  public IssueChangelog changelog(String issueKey) {
    Issue issue = issueService.getByKey(issueKey);
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

  public List<String> formatDiffs(FieldDiffs diffs) {
    return formatter.format(UserSession.get().locale(), diffs);
  }
}
