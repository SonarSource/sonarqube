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
import org.sonar.api.web.UserRole;
import org.sonar.core.issue.DefaultIssueFilter;
import org.sonar.core.issue.db.IssueFilterDao;
import org.sonar.core.issue.db.IssueFilterDto;
import org.sonar.core.issue.db.IssueFilterFavouriteDao;
import org.sonar.core.issue.db.IssueFilterFavouriteDto;
import org.sonar.core.user.AuthorizationDao;
import org.sonar.server.user.UserSession;

import javax.annotation.CheckForNull;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;

public class IssueFilterService implements ServerComponent {

  private final IssueFilterDao issueFilterDao;
  private final IssueFilterFavouriteDao issueFilterFavouriteDao;
  private final IssueFinder issueFinder;
  private final AuthorizationDao authorizationDao;

  public IssueFilterService(IssueFilterDao issueFilterDao, IssueFilterFavouriteDao issueFilterFavouriteDao, IssueFinder issueFinder, AuthorizationDao authorizationDao) {
    this.issueFilterDao = issueFilterDao;
    this.issueFilterFavouriteDao = issueFilterFavouriteDao;
    this.issueFinder = issueFinder;
    this.authorizationDao = authorizationDao;
  }

  public IssueQueryResult execute(IssueQuery issueQuery) {
    return issueFinder.find(issueQuery);
  }

  public IssueQueryResult execute(Long issueFilterId, UserSession userSession) {
    IssueFilterDto issueFilterDto = findIssueFilter(issueFilterId, userSession);

    DefaultIssueFilter issueFilter = issueFilterDto.toIssueFilter();
    IssueQuery issueQuery = PublicRubyIssueService.toQuery(issueFilter.dataAsMap());
    return issueFinder.find(issueQuery);
  }

  @CheckForNull
  public DefaultIssueFilter findById(Long id, UserSession userSession) {
    IssueFilterDto issueFilterDto = findIssueFilter(id, userSession);
    return issueFilterDto.toIssueFilter();
  }

  public List<DefaultIssueFilter> findByUser(UserSession userSession) {
    if (userSession.isLoggedIn() && userSession.login() != null) {
      return toIssueFilters(issueFilterDao.selectByUser(userSession.login()));
    }
    return Collections.emptyList();
  }

  public DefaultIssueFilter save(DefaultIssueFilter issueFilter, UserSession userSession) {
    verifyLoggedIn(userSession);
    issueFilter.setUser(userSession.login());
    verifyNameIsNotAlreadyUsed(issueFilter, userSession);
    return insertIssueFilter(issueFilter, userSession.login());
  }

  public DefaultIssueFilter update(DefaultIssueFilter issueFilter, UserSession userSession) {
    IssueFilterDto issueFilterDto = findIssueFilter(issueFilter.id(), userSession);
    verifyCurrentUserCanModifyFilter(issueFilterDto, userSession);
    verifyNameIsNotAlreadyUsed(issueFilter, userSession);

    issueFilterDao.update(IssueFilterDto.toIssueFilter(issueFilter));
    return issueFilter;
  }

  public DefaultIssueFilter updateData(Long issueFilterId, Map<String, Object> mapData, UserSession userSession) {
    IssueFilterDto issueFilterDto = findIssueFilter(issueFilterId, userSession);
    verifyCurrentUserCanModifyFilter(issueFilterDto, userSession);

    DefaultIssueFilter issueFilter = issueFilterDto.toIssueFilter();
    issueFilter.setData(mapData);
    issueFilterDao.update(IssueFilterDto.toIssueFilter(issueFilter));
    return issueFilter;
  }

  public void delete(Long issueFilterId, UserSession userSession) {
    IssueFilterDto issueFilterDto = findIssueFilter(issueFilterId, userSession);
    verifyCurrentUserCanModifyFilter(issueFilterDto, userSession);

    deleteFavouriteIssueFilters(issueFilterDto);
    issueFilterDao.delete(issueFilterId);
  }

  public DefaultIssueFilter copy(Long issueFilterIdToCopy, DefaultIssueFilter issueFilter, UserSession userSession) {
    IssueFilterDto issueFilterDtoToCopy = findIssueFilter(issueFilterIdToCopy, userSession);
    issueFilter.setUser(userSession.login());
    issueFilter.setData(issueFilterDtoToCopy.getData());
    verifyNameIsNotAlreadyUsed(issueFilter, userSession);

    return insertIssueFilter(issueFilter, userSession.login());
  }

  public List<DefaultIssueFilter> findSharedFilters(UserSession userSession) {
    if (userSession.isLoggedIn() && userSession.login() != null) {
      return toIssueFilters(issueFilterDao.selectSharedForUser(userSession.login()));
    }
    return Collections.emptyList();
  }

  public List<DefaultIssueFilter> findFavoriteFilters(UserSession userSession) {
    if (userSession.isLoggedIn() && userSession.login() != null) {
      return toIssueFilters(issueFilterDao.selectByUserWithOnlyFavoriteFilters(userSession.login()));
    }
    return Collections.emptyList();
  }

  public void toggleFavouriteIssueFilter(Long issueFilterId, UserSession userSession) {
    findIssueFilter(issueFilterId, userSession);
    IssueFilterFavouriteDto issueFilterFavouriteDto = findFavouriteIssueFilter(userSession.login(), issueFilterId);
    if (issueFilterFavouriteDto == null) {
      addFavouriteIssueFilter(issueFilterId, userSession.login());
    } else {
      deleteFavouriteIssueFilter(issueFilterFavouriteDto);
    }
  }

  public IssueFilterDto findIssueFilter(Long id, UserSession userSession) {
    verifyLoggedIn(userSession);
    IssueFilterDto issueFilterDto = issueFilterDao.selectById(id);
    if (issueFilterDto == null) {
      // TODO throw 404
      throw new IllegalArgumentException("Filter not found: " + id);
    }
    verifyCurrentUserCanReadFilter(issueFilterDto, userSession);
    return issueFilterDto;
  }

  private void verifyLoggedIn(UserSession userSession) {
    if (!userSession.isLoggedIn() || userSession.login() == null) {
      throw new IllegalStateException("User is not logged in");
    }
  }

  private void verifyCurrentUserCanReadFilter(IssueFilterDto issueFilterDto, UserSession userSession) {
    if (!issueFilterDto.getUserLogin().equals(userSession.login()) && !issueFilterDto.isShared()) {
      // TODO throw unauthorized
      throw new IllegalStateException("User is not authorized to read this filter");
    }
  }

  private void verifyCurrentUserCanModifyFilter(IssueFilterDto issueFilterDto, UserSession userSession) {
    if (!issueFilterDto.getUserLogin().equals(userSession.login()) && (!issueFilterDto.isShared() || !isAdmin(userSession.login()))) {
      // TODO throw unauthorized
      throw new IllegalStateException("User is not authorized to modify this filter");
    }
  }

  private void verifyNameIsNotAlreadyUsed(DefaultIssueFilter issueFilter, UserSession userSession) {
    if (issueFilterDao.selectByNameAndUser(issueFilter.name(), userSession.login(), issueFilter.id()) != null) {
      throw new IllegalArgumentException("Name already exists");
    }
  }

  private IssueFilterFavouriteDto findFavouriteIssueFilter(String user, Long issueFilterId) {
    return issueFilterFavouriteDao.selectByUserAndIssueFilterId(user, issueFilterId);
  }

  private void addFavouriteIssueFilter(Long issueFilterId, String user) {
    IssueFilterFavouriteDto issueFilterFavouriteDto = new IssueFilterFavouriteDto()
      .setIssueFilterId(issueFilterId)
      .setUserLogin(user);
    issueFilterFavouriteDao.insert(issueFilterFavouriteDto);
  }

  private void deleteFavouriteIssueFilter(IssueFilterFavouriteDto issueFilterFavouriteDto) {
    issueFilterFavouriteDao.delete(issueFilterFavouriteDto.getId());
  }

  private void deleteFavouriteIssueFilters(IssueFilterDto issueFilterDto) {
    issueFilterFavouriteDao.deleteByIssueFilterId(issueFilterDto.getId());
  }

  private DefaultIssueFilter insertIssueFilter(DefaultIssueFilter issueFilter, String user) {
    IssueFilterDto issueFilterDto = IssueFilterDto.toIssueFilter(issueFilter);
    issueFilterDao.insert(issueFilterDto);
    addFavouriteIssueFilter(issueFilterDto.getId(), user);
    return issueFilterDto.toIssueFilter();
  }

  private List<DefaultIssueFilter> toIssueFilters(List<IssueFilterDto> issueFilterDtoList) {
    return newArrayList(Iterables.transform(issueFilterDtoList, new Function<IssueFilterDto, DefaultIssueFilter>() {
      @Override
      public DefaultIssueFilter apply(IssueFilterDto issueFilterDto) {
        return issueFilterDto.toIssueFilter();
      }
    }));
  }

  private boolean isAdmin(String user) {
    return authorizationDao.selectGlobalPermissions(user).contains(UserRole.ADMIN);
  }

}
