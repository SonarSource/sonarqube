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
import org.sonar.api.issue.IssueFinder;
import org.sonar.api.issue.IssueQuery;
import org.sonar.api.issue.IssueQueryResult;
import org.sonar.core.issue.DefaultIssueFilter;
import org.sonar.core.issue.db.IssueFilterDao;
import org.sonar.core.issue.db.IssueFilterDto;
import org.sonar.server.user.UserSession;

import javax.annotation.CheckForNull;

import java.util.Map;

public class IssueFilterService implements ServerComponent {

  private IssueFilterDao issueFilterDao;
  private final IssueFinder issueFinder;

  public IssueFilterService(IssueFilterDao issueFilterDao, IssueFinder issueFinder) {
    this.issueFilterDao = issueFilterDao;
    this.issueFinder = issueFinder;
  }

  @CheckForNull
  public DefaultIssueFilter createEmptyFilter(Map<String, Object> mapData) {
    return new DefaultIssueFilter(mapData);
  }

  @CheckForNull
  public DefaultIssueFilter findById(Long id, UserSession userSession) {
    // TODO
//    checkAuthorization(userSession, project, UserRole.ADMIN);
    verifyLoggedIn(userSession);
//    access_denied unless filter.shared || filter.owner?(current_user)

    IssueFilterDto issueFilterDto = issueFilterDao.selectById(id);
    if (issueFilterDto == null) {
      return null;
    }
    return issueFilterDto.toIssueFilter();
  }

  public DefaultIssueFilter save(DefaultIssueFilter issueFilter, UserSession userSession) {
    issueFilter.setUser(userSession.login());
    // TODO
//    checkAuthorization(userSession, project, UserRole.ADMIN);
    IssueFilterDto issueFilterDto = IssueFilterDto.toIssueFilter(issueFilter);
    issueFilterDao.insert(issueFilterDto);
    return issueFilterDto.toIssueFilter();
  }

  public DefaultIssueFilter update(DefaultIssueFilter issueFilter, UserSession userSession) {
    // TODO
//    checkAuthorization(userSession, project, UserRole.ADMIN);
    issueFilterDao.update(IssueFilterDto.toIssueFilter(issueFilter));
    return issueFilter;
  }

  public void delete(Long issueFilterId, UserSession userSession) {
    // TODO
    //checkAuthorization(userSession, findActionPlanDto(actionPlanKey).getProjectKey(), UserRole.ADMIN);
    issueFilterDao.delete(issueFilterId);
  }

  public IssueQueryResult execute(IssueQuery issueQuery) {
    return issueFinder.find(issueQuery);
  }

  public IssueQueryResult execute(Long issueFilterId) {
    IssueFilterDto issueFilterDto = issueFilterDao.selectById(issueFilterId);
    if (issueFilterDto == null) {
      // TODO throw 404
      throw new IllegalArgumentException("Issue filter " + issueFilterId + " has not been found.");
    }

    DefaultIssueFilter issueFilter = issueFilterDto.toIssueFilter();
    // convert data to issue query
    issueFilter.data();

//    return issueFinder.find(issueQuery);
    return null;
  }

  private void verifyLoggedIn(UserSession userSession) {
    if (!userSession.isLoggedIn()) {
      // must be logged
      throw new IllegalStateException("User is not logged in");
    }
  }

}
