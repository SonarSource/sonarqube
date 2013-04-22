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

import org.sonar.api.issue.Issue;
import org.sonar.api.issue.IssueChange;
import org.sonar.api.web.UserRole;
import org.sonar.core.issue.ApplyIssueChange;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.IssueDao;
import org.sonar.core.issue.IssueDto;
import org.sonar.core.user.AuthorizationDao;

import javax.annotation.Nullable;
import java.util.Arrays;

/**
 * @since 3.6
 */
public class ServerIssueChanges {

  private final IssueDao issueDao;
  private final AuthorizationDao authorizationDao;

  public ServerIssueChanges(IssueDao issueDao, AuthorizationDao authorizationDao) {
    this.issueDao = issueDao;
    this.authorizationDao = authorizationDao;
  }

  public Issue change(String issueKey, IssueChange change, @Nullable Integer userId) {
    if (userId == null) {
      // must be logged
      throw new IllegalStateException("User is not logged in");
    }
    IssueDto dto = issueDao.selectByKey(issueKey);
    if (dto == null) {
      throw new IllegalStateException("Unknown issue: " + issueKey);
    }
    String requiredRole = UserRole.USER;
    if (!authorizationDao.isAuthorizedComponentId(dto.getResourceId(), userId, requiredRole)) {
      throw new IllegalStateException("User does not have the role " + requiredRole + " required to change the issue: " + issueKey);
    }
    DefaultIssue issue = dto.toDefaultIssue();
    if (change.hasChanges()) {
      ApplyIssueChange.apply(issue, change);
      issueDao.update(Arrays.asList(IssueDto.toDto(issue, dto.getResourceId(), dto.getRuleId())));
    }
    return issue;
  }
}
