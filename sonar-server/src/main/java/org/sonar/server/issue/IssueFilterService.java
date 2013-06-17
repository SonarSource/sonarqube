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

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import org.sonar.api.ServerComponent;
import org.sonar.api.issue.IssueFinder;
import org.sonar.api.issue.IssueQuery;
import org.sonar.api.issue.IssueQueryResult;
import org.sonar.core.issue.DefaultIssueFilter;
import org.sonar.core.issue.db.IssueFilterDao;
import org.sonar.core.issue.db.IssueFilterDto;
import org.sonar.server.user.UserSession;

import javax.annotation.CheckForNull;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;

public class IssueFilterService implements ServerComponent {

  private IssueFilterDao issueFilterDao;
  private final IssueFinder issueFinder;

  public IssueFilterService(IssueFilterDao issueFilterDao, IssueFinder issueFinder) {
    this.issueFilterDao = issueFilterDao;
    this.issueFinder = issueFinder;
  }

  @CheckForNull
  public DefaultIssueFilter findById(Long id, UserSession userSession) {
    verifyLoggedIn(userSession);
    IssueFilterDto issueFilterDto = findIssueFilter(id);
    verifyCurrentUserIsOwnerOfFilter(issueFilterDto, userSession);
    return issueFilterDto.toIssueFilter();
  }

  public List<DefaultIssueFilter> findByUser(UserSession userSession) {
    if (userSession.isLoggedIn()) {
      List<IssueFilterDto> issueFilterDtoList = issueFilterDao.selectByUser(userSession.login());
      return newArrayList(Iterables.transform(issueFilterDtoList, new Function<IssueFilterDto, DefaultIssueFilter>() {
        @Override
        public DefaultIssueFilter apply(IssueFilterDto issueFilterDto) {
          return issueFilterDto.toIssueFilter();
        }
      }));
    }
    return Collections.emptyList();
  }

  public DefaultIssueFilter save(DefaultIssueFilter issueFilter, UserSession userSession) {
    verifyLoggedIn(userSession);
    issueFilter.setUser(userSession.login());
    VerifyNameIsNotAlreadyUsed(issueFilter, userSession);

    IssueFilterDto issueFilterDto = IssueFilterDto.toIssueFilter(issueFilter);
    issueFilterDao.insert(issueFilterDto);
    return issueFilterDto.toIssueFilter();
  }

  public DefaultIssueFilter update(DefaultIssueFilter issueFilter, UserSession userSession) {
    verifyLoggedIn(userSession);
    IssueFilterDto issueFilterDto = findIssueFilter(issueFilter.id());
    verifyCurrentUserIsOwnerOfFilter(issueFilterDto, userSession);
    VerifyNameIsNotAlreadyUsed(issueFilter, userSession);

    issueFilterDao.update(IssueFilterDto.toIssueFilter(issueFilter));
    return issueFilter;
  }

  public DefaultIssueFilter updateData(Long issueFilterId, Map<String, Object> mapData, UserSession userSession) {
    verifyLoggedIn(userSession);
    IssueFilterDto issueFilterDto = findIssueFilter(issueFilterId);
    verifyCurrentUserIsOwnerOfFilter(issueFilterDto, userSession);
    DefaultIssueFilter issueFilter = issueFilterDto.toIssueFilter();
    issueFilter.setData(mapData);
    issueFilterDao.update(IssueFilterDto.toIssueFilter(issueFilter));
    return issueFilter;
  }

  public void delete(Long issueFilterId, UserSession userSession) {
    verifyLoggedIn(userSession);
    IssueFilterDto issueFilterDto = findIssueFilter(issueFilterId);
    verifyCurrentUserIsOwnerOfFilter(issueFilterDto, userSession);
    issueFilterDao.delete(issueFilterId);
  }

  public IssueQueryResult execute(IssueQuery issueQuery) {
    return issueFinder.find(issueQuery);
  }

  public IssueQueryResult execute(Long issueFilterId, UserSession userSession) {
    IssueFilterDto issueFilterDto = findIssueFilter(issueFilterId);
    verifyCurrentUserIsOwnerOfFilter(issueFilterDto, userSession);

    DefaultIssueFilter issueFilter = issueFilterDto.toIssueFilter();
    IssueQuery issueQuery = PublicRubyIssueService.toQuery(issueFilter.dataAsMap());
    return issueFinder.find(issueQuery);
  }

  public IssueFilterDto findIssueFilter(Long id){
    IssueFilterDto issueFilterDto = issueFilterDao.selectById(id);
    if (issueFilterDto == null) {
      // TODO throw 404
      throw new IllegalArgumentException("Filter not found: " + id);
    }
    return issueFilterDto;
  }

  private void verifyLoggedIn(UserSession userSession) {
    if (!userSession.isLoggedIn()) {
      throw new IllegalStateException("User is not logged in");
    }
  }

  private void verifyCurrentUserIsOwnerOfFilter(IssueFilterDto issueFilterDto, UserSession userSession){
    if (!issueFilterDto.getUserLogin().equals(userSession.login())) {
      // TODO throw unauthorized
      throw new IllegalStateException("User is not authorized to get this filter");
    }
  }

  private void VerifyNameIsNotAlreadyUsed(DefaultIssueFilter issueFilter, UserSession userSession){
    if (issueFilterDao.selectByNameAndUser(issueFilter.name(), userSession.login(), issueFilter.id()) != null) {
      throw new IllegalArgumentException("Name already exists");
    }
  }

}
