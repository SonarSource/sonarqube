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

import java.util.Collection;
import java.util.List;
import org.sonar.api.issue.Issue;
import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.api.server.ServerSide;
import org.sonar.api.user.User;
import org.sonar.api.user.UserFinder;
import org.sonar.core.issue.FieldDiffs;
import org.sonar.db.issue.IssueChangeDao;

import static com.google.common.collect.Lists.newArrayList;

/**
 * @since 3.6
 */
@ServerSide
@ComputeEngineSide
public class IssueChangelogService {

  private final IssueChangeDao changeDao;
  private final UserFinder userFinder;
  private final IssueService issueService;

  public IssueChangelogService(IssueChangeDao changeDao, UserFinder userFinder, IssueService issueService) {
    this.changeDao = changeDao;
    this.userFinder = userFinder;
    this.issueService = issueService;
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
}
