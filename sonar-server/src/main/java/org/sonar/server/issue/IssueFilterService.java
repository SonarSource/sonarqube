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
import org.sonar.core.issue.IssueFilterSerializer;
import org.sonar.core.issue.db.IssueFilterDao;
import org.sonar.core.issue.db.IssueFilterDto;
import org.sonar.core.issue.db.IssueFilterFavouriteDao;
import org.sonar.core.issue.db.IssueFilterFavouriteDto;
import org.sonar.core.user.AuthorizationDao;
import org.sonar.server.user.UserSession;

import javax.annotation.CheckForNull;

import java.util.List;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;

/**
 * @since 3.7
 */
public class IssueFilterService implements ServerComponent {

  private final IssueFilterDao issueFilterDao;
  private final IssueFilterFavouriteDao issueFilterFavouriteDao;
  private final IssueFinder issueFinder;
  private final AuthorizationDao authorizationDao;
  private final IssueFilterSerializer issueFilterSerializer;

  public IssueFilterService(IssueFilterDao issueFilterDao, IssueFilterFavouriteDao issueFilterFavouriteDao, IssueFinder issueFinder, AuthorizationDao authorizationDao,
                            IssueFilterSerializer issueFilterSerializer) {
    this.issueFilterDao = issueFilterDao;
    this.issueFilterFavouriteDao = issueFilterFavouriteDao;
    this.issueFinder = issueFinder;
    this.authorizationDao = authorizationDao;
    this.issueFilterSerializer = issueFilterSerializer;
  }

  public IssueFilterResult execute(IssueQuery issueQuery) {
    return createIssueFilterResult(issueFinder.find(issueQuery), issueQuery);
  }

  public DefaultIssueFilter find(Long id, UserSession userSession) {
    return findIssueFilterDto(id, getNotNullLogin(userSession)).toIssueFilter();
  }

  @CheckForNull
  public DefaultIssueFilter findById(Long id) {
    IssueFilterDto issueFilterDto = issueFilterDao.selectById(id);
    if (issueFilterDto != null) {
      return issueFilterDto.toIssueFilter();
    }
    return null;
  }

  public List<DefaultIssueFilter> findByUser(UserSession userSession) {
    return toIssueFilters(issueFilterDao.selectByUser(getNotNullLogin(userSession)));
  }

  public DefaultIssueFilter save(DefaultIssueFilter issueFilter, UserSession userSession) {
    String user = getNotNullLogin(userSession);
    issueFilter.setUser(user);
    validateFilter(issueFilter, user);
    return insertIssueFilter(issueFilter, user);
  }

  public DefaultIssueFilter update(DefaultIssueFilter issueFilter, UserSession userSession) {
    String user = getNotNullLogin(userSession);
    IssueFilterDto issueFilterDto = findIssueFilterDto(issueFilter.id(), user);
    verifyCurrentUserCanModifyFilter(issueFilterDto.toIssueFilter(), user);
    if(issueFilterDto.getUserLogin() != issueFilter.user()) {
      verifyCurrentUserCanChangeFilterOwnership(user);
    }
    validateFilter(issueFilter, user);

    issueFilterDao.update(IssueFilterDto.toIssueFilter(issueFilter));
    return issueFilter;
  }

  public DefaultIssueFilter updateFilterQuery(Long issueFilterId, Map<String, Object> filterQuery, UserSession userSession) {
    String user = getNotNullLogin(userSession);
    IssueFilterDto issueFilterDto = findIssueFilterDto(issueFilterId, user);
    verifyCurrentUserCanModifyFilter(issueFilterDto.toIssueFilter(), user);

    DefaultIssueFilter issueFilter = issueFilterDto.toIssueFilter();
    issueFilter.setData(serializeFilterQuery(filterQuery));
    issueFilterDao.update(IssueFilterDto.toIssueFilter(issueFilter));
    return issueFilter;
  }

  public void delete(Long issueFilterId, UserSession userSession) {
    String user = getNotNullLogin(userSession);
    IssueFilterDto issueFilterDto = findIssueFilterDto(issueFilterId, user);
    verifyCurrentUserCanModifyFilter(issueFilterDto.toIssueFilter(), user);

    deleteFavouriteIssueFilters(issueFilterDto);
    issueFilterDao.delete(issueFilterId);
  }

  public DefaultIssueFilter copy(Long issueFilterIdToCopy, DefaultIssueFilter issueFilter, UserSession userSession) {
    String user = getNotNullLogin(userSession);
    IssueFilterDto issueFilterDtoToCopy = findIssueFilterDto(issueFilterIdToCopy, user);
    issueFilter.setUser(user);
    issueFilter.setData(issueFilterDtoToCopy.getData());
    validateFilter(issueFilter, user);

    return insertIssueFilter(issueFilter, user);
  }

  public List<DefaultIssueFilter> findSharedFilters(UserSession userSession) {
    return toIssueFilters(issueFilterDao.selectSharedWithoutUserFilters(getNotNullLogin(userSession)));
  }

  public List<DefaultIssueFilter> findFavoriteFilters(UserSession userSession) {
    return toIssueFilters(issueFilterDao.selectByUserWithOnlyFavoriteFilters(getNotNullLogin(userSession)));
  }

  public void toggleFavouriteIssueFilter(Long issueFilterId, UserSession userSession) {
    String user = getNotNullLogin(userSession);
    findIssueFilterDto(issueFilterId, user);
    IssueFilterFavouriteDto issueFilterFavouriteDto = findFavouriteIssueFilter(user, issueFilterId);
    if (issueFilterFavouriteDto == null) {
      addFavouriteIssueFilter(issueFilterId, user);
    } else {
      deleteFavouriteIssueFilter(issueFilterFavouriteDto);
    }
  }

  public String serializeFilterQuery(Map<String, Object> filterQuery) {
    return issueFilterSerializer.serialize(filterQuery);
  }

  public Map<String, Object> deserializeIssueFilterQuery(DefaultIssueFilter issueFilter) {
    return issueFilterSerializer.deserialize(issueFilter.data());
  }

  private IssueFilterDto findIssueFilterDto(Long id, String user) {
    IssueFilterDto issueFilterDto = issueFilterDao.selectById(id);
    if (issueFilterDto == null) {
      // TODO throw 404
      throw new IllegalArgumentException("Filter not found: " + id);
    }
    verifyCurrentUserCanReadFilter(issueFilterDto.toIssueFilter(), user);
    return issueFilterDto;
  }

  String getNotNullLogin(UserSession userSession) {
    String user = userSession.login();
    if (!userSession.isLoggedIn() && user != null) {
      throw new IllegalStateException("User is not logged in");
    }
    return user;
  }

  void verifyCurrentUserCanReadFilter(DefaultIssueFilter issueFilter, String user) {
    if (!issueFilter.user().equals(user) && !issueFilter.shared()) {
      // TODO throw unauthorized
      throw new IllegalStateException("User is not authorized to read this filter");
    }
  }

  private void verifyCurrentUserCanModifyFilter(DefaultIssueFilter issueFilter, String user) {
    if (!issueFilter.user().equals(user) && !isAdmin(user)) {
      // TODO throw unauthorized
      throw new IllegalStateException("User is not authorized to modify this filter");
    }
  }

  private void verifyCurrentUserCanChangeFilterOwnership(String user) {
    if(!isAdmin(user)) {
      // TODO throw unauthorized
      throw new IllegalStateException("User is not authorized to change the owner of this filter");
    }
  }

  private void validateFilter(DefaultIssueFilter issueFilter, String user) {
    if (issueFilterDao.selectByNameAndUser(issueFilter.name(), user, issueFilter.id()) != null) {
      throw new IllegalArgumentException("Name already exists");
    }
    if (issueFilter.shared() && issueFilterDao.selectSharedWithoutUserFiltersByName(issueFilter.name(), user, issueFilter.id()) != null) {
      throw new IllegalArgumentException("Other users already share filters with the same name");
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

  private IssueFilterResult createIssueFilterResult(IssueQueryResult issueQueryResult, IssueQuery issueQuery) {
    return new IssueFilterResult(issueQueryResult, issueQuery);
  }

}
