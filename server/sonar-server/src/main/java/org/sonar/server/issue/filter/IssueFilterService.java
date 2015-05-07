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

package org.sonar.server.issue.filter;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import org.sonar.api.ServerSide;
import org.sonar.api.utils.Paging;
import org.sonar.core.issue.IssueFilterSerializer;
import org.sonar.core.issue.db.IssueFilterDao;
import org.sonar.core.issue.db.IssueFilterDto;
import org.sonar.core.issue.db.IssueFilterFavouriteDao;
import org.sonar.core.issue.db.IssueFilterFavouriteDto;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.core.user.AuthorizationDao;
import org.sonar.server.es.SearchOptions;
import org.sonar.server.es.SearchResult;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.issue.IssueQuery;
import org.sonar.server.issue.index.IssueDoc;
import org.sonar.server.issue.index.IssueIndex;
import org.sonar.server.user.UserSession;

import javax.annotation.CheckForNull;

import java.util.Date;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;

@ServerSide
public class IssueFilterService {

  private final IssueFilterDao filterDao;
  private final IssueFilterFavouriteDao favouriteDao;
  private final IssueIndex issueIndex;
  private final AuthorizationDao authorizationDao;
  private final IssueFilterSerializer serializer;

  public IssueFilterService(IssueFilterDao filterDao, IssueFilterFavouriteDao favouriteDao,
    IssueIndex issueIndex, AuthorizationDao authorizationDao,
    IssueFilterSerializer serializer) {
    this.filterDao = filterDao;
    this.favouriteDao = favouriteDao;
    this.issueIndex = issueIndex;
    this.authorizationDao = authorizationDao;
    this.serializer = serializer;
  }

  public IssueFilterResult execute(IssueQuery issueQuery, SearchOptions options) {
    return createIssueFilterResult(issueIndex.search(issueQuery, options), options);
  }

  public IssueFilterDto find(Long id, UserSession userSession) {
    return findIssueFilterDto(id, getLoggedLogin(userSession));
  }

  @CheckForNull
  public IssueFilterDto findById(Long id) {
    IssueFilterDto issueFilterDto = filterDao.selectById(id);
    if (issueFilterDto != null) {
      return issueFilterDto;
    }
    return null;
  }

  public List<IssueFilterDto> findByUser(UserSession userSession) {
    return selectUserIssueFilters(getLoggedLogin(userSession));
  }

  public IssueFilterDto save(IssueFilterDto issueFilter, UserSession userSession) {
    String user = getLoggedLogin(userSession);
    issueFilter.setUserLogin(user);
    validateFilter(issueFilter);
    return insertIssueFilter(issueFilter, user);
  }

  IssueFilterDto save(IssueFilterDto issueFilter) {
    return insertIssueFilter(issueFilter);
  }

  public IssueFilterDto update(IssueFilterDto issueFilter, UserSession userSession) {
    String login = getLoggedLogin(userSession);
    IssueFilterDto existingFilterDto = findIssueFilterDto(issueFilter.getId(), login);
    verifyCurrentUserCanModifyFilter(existingFilterDto, login);
    verifyCurrentUserCanChangeFilterSharingFilter(issueFilter, existingFilterDto, login);
    if (!isFilterOwnedByUser(existingFilterDto, issueFilter.getUserLogin())) {
      verifyCurrentUserCanChangeFilterOwnership(login);
    }
    validateFilter(issueFilter);
    deleteOtherFavoriteFiltersIfFilterBecomeUnshared(existingFilterDto, issueFilter);
    issueFilter.setUpdatedAt(new Date());
    filterDao.update(issueFilter);
    return issueFilter;
  }

  private void deleteOtherFavoriteFiltersIfFilterBecomeUnshared(IssueFilterDto existingFilterDto, IssueFilterDto issueFilter) {
    if (existingFilterDto.isShared() && !issueFilter.isShared()) {
      for (IssueFilterFavouriteDto favouriteDto : selectFavouriteFilters(existingFilterDto.getId())) {
        if (!favouriteDto.getUserLogin().equals(issueFilter.getUserLogin())) {
          deleteFavouriteIssueFilter(favouriteDto);
        }
      }
    }
  }

  public IssueFilterDto updateFilterQuery(Long issueFilterId, Map<String, Object> filterQuery, UserSession userSession) {
    String login = getLoggedLogin(userSession);
    IssueFilterDto issueFilterDto = findIssueFilterDto(issueFilterId, login);
    verifyCurrentUserCanModifyFilter(issueFilterDto, login);

    issueFilterDto.setData(serializeFilterQuery(filterQuery));
    issueFilterDto.setUpdatedAt(new Date());
    filterDao.update(issueFilterDto);
    return issueFilterDto;
  }

  public void delete(Long issueFilterId, UserSession userSession) {
    String login = getLoggedLogin(userSession);
    IssueFilterDto issueFilterDto = findIssueFilterDto(issueFilterId, login);
    verifyCurrentUserCanModifyFilter(issueFilterDto, login);

    deleteFavouriteIssueFilters(issueFilterDto);
    filterDao.delete(issueFilterId);
  }

  public IssueFilterDto copy(Long issueFilterIdToCopy, IssueFilterDto issueFilter, UserSession userSession) {
    String login = getLoggedLogin(userSession);
    IssueFilterDto issueFilterDtoToCopy = findIssueFilterDto(issueFilterIdToCopy, login);
    // Copy of filter should not be shared
    issueFilter.setShared(false);
    issueFilter.setUserLogin(login);
    issueFilter.setData(issueFilterDtoToCopy.getData());
    validateFilter(issueFilter);
    return insertIssueFilter(issueFilter, login);
  }

  public List<IssueFilterDto> findSharedFiltersWithoutUserFilters(UserSession userSession) {
    final String login = getLoggedLogin(userSession);
    return newArrayList(Iterables.filter(selectSharedFilters(), new Predicate<IssueFilterDto>() {
      @Override
      public boolean apply(IssueFilterDto input) {
        return !isFilterOwnedByUser(input, login);
      }
    }));
  }

  public List<IssueFilterDto> findFavoriteFilters(UserSession userSession) {
    return filterDao.selectFavoriteFiltersByUser(getLoggedLogin(userSession));
  }

  /**
   * Return true if favorite is added, false if favorite is removed
   */
  public boolean toggleFavouriteIssueFilter(Long filterId, UserSession userSession) {
    String user = getLoggedLogin(userSession);
    findIssueFilterDto(filterId, user);
    IssueFilterFavouriteDto issueFilterFavouriteDto = selectFavouriteFilterForUser(filterId, user);
    if (issueFilterFavouriteDto == null) {
      addFavouriteIssueFilter(filterId, user);
      return true;
    } else {
      deleteFavouriteIssueFilter(issueFilterFavouriteDto);
      return false;
    }
  }

  public String serializeFilterQuery(Map<String, Object> filterQuery) {
    Map<String, Object> filterQueryFiltered = Maps.filterEntries(filterQuery, new Predicate<Map.Entry<String, Object>>() {
      @Override
      public boolean apply(Map.Entry<String, Object> input) {
        return IssueFilterParameters.ALL_WITHOUT_PAGINATION.contains(input.getKey());
      }
    });
    return serializer.serialize(filterQueryFiltered);
  }

  public Map<String, Object> deserializeIssueFilterQuery(IssueFilterDto issueFilter) {
    return serializer.deserialize(issueFilter.getData());
  }

  private IssueFilterDto findIssueFilterDto(Long id, String login) {
    IssueFilterDto issueFilterDto = filterDao.selectById(id);
    if (issueFilterDto == null) {
      throw new NotFoundException("Filter not found: " + id);
    }
    verifyCurrentUserCanReadFilter(issueFilterDto, login);
    return issueFilterDto;
  }

  public boolean canShareFilter(UserSession userSession) {
    if (userSession.isLoggedIn()) {
      String user = userSession.login();
      return hasUserSharingPermission(user);
    }
    return false;
  }

  public String getLoggedLogin(UserSession userSession) {
    String user = userSession.login();
    if (!userSession.isLoggedIn()) {
      throw new UnauthorizedException("User is not logged in");
    }
    return user;
  }

  public void verifyCurrentUserCanReadFilter(IssueFilterDto issueFilter, String login) {
    if (issueFilter.getUserLogin() != null && !issueFilter.getUserLogin().equals(login) && !issueFilter.isShared()) {
      throw new ForbiddenException("User is not authorized to read this filter");
    }
  }

  private void verifyCurrentUserCanModifyFilter(IssueFilterDto issueFilter, String user) {
    if (issueFilter.getUserLogin() != null && !issueFilter.getUserLogin().equals(user) && !isAdmin(user)) {
      throw new ForbiddenException("User is not authorized to modify this filter");
    }
  }

  private void verifyCurrentUserCanChangeFilterSharingFilter(IssueFilterDto issueFilter, IssueFilterDto existingFilterDto, String login) {
    if (existingFilterDto.isShared() != issueFilter.isShared() && !isFilterOwnedByUser(existingFilterDto, login)) {
      throw new ForbiddenException("Only owner of a filter can change sharing");
    }
  }

  private void verifyCurrentUserCanChangeFilterOwnership(String user) {
    if (!isAdmin(user)) {
      throw new ForbiddenException("User is not authorized to change the owner of this filter");
    }
  }

  private void verifyCurrentUserCanShareFilter(IssueFilterDto issueFilter, String user) {
    if (issueFilter.isShared() && !hasUserSharingPermission(user)) {
      throw new ForbiddenException("User cannot own this filter because of insufficient rights");
    }
  }

  private void validateFilter(final IssueFilterDto issueFilter) {
    List<IssueFilterDto> userFilters = selectUserIssueFilters(issueFilter.getUserLogin());
    IssueFilterDto userFilterSameName = findFilterWithSameName(userFilters, issueFilter.getName());
    if (userFilterSameName != null && !userFilterSameName.getId().equals(issueFilter.getId())) {
      throw new BadRequestException("Name already exists");
    }
    if (issueFilter.isShared()) {
      List<IssueFilterDto> sharedFilters = selectSharedFilters();
      IssueFilterDto sharedFilterWithSameName = findFilterWithSameName(sharedFilters, issueFilter.getName());
      if (sharedFilterWithSameName != null && !sharedFilterWithSameName.getId().equals(issueFilter.getId())) {
        throw new BadRequestException("Other users already share filters with the same name");
      }
      verifyCurrentUserCanShareFilter(issueFilter, issueFilter.getUserLogin());
    }
  }

  @CheckForNull
  private IssueFilterFavouriteDto selectFavouriteFilterForUser(Long filterId, final String user) {
    return Iterables.find(selectFavouriteFilters(filterId), new Predicate<IssueFilterFavouriteDto>() {
      @Override
      public boolean apply(IssueFilterFavouriteDto input) {
        return input.getUserLogin().equals(user);
      }
    }, null);
  }

  private List<IssueFilterFavouriteDto> selectFavouriteFilters(Long filterId) {
    return favouriteDao.selectByFilterId(filterId);
  }

  private List<IssueFilterDto> selectUserIssueFilters(String user) {
    return filterDao.selectByUser(user);
  }

  private List<IssueFilterDto> selectSharedFilters() {
    return filterDao.selectSharedFilters();
  }

  @CheckForNull
  private IssueFilterDto findFilterWithSameName(List<IssueFilterDto> dtos, final String name) {
    return Iterables.find(dtos, new Predicate<IssueFilterDto>() {
      @Override
      public boolean apply(IssueFilterDto input) {
        return input.getName().equals(name);
      }
    }, null);
  }

  private void addFavouriteIssueFilter(Long issueFilterId, String user) {
    IssueFilterFavouriteDto issueFilterFavouriteDto = new IssueFilterFavouriteDto()
      .setIssueFilterId(issueFilterId)
      .setUserLogin(user);
    favouriteDao.insert(issueFilterFavouriteDto);
  }

  private void deleteFavouriteIssueFilter(IssueFilterFavouriteDto issueFilterFavouriteDto) {
    favouriteDao.delete(issueFilterFavouriteDto.getId());
  }

  private void deleteFavouriteIssueFilters(IssueFilterDto issueFilterDto) {
    favouriteDao.deleteByFilterId(issueFilterDto.getId());
  }

  private IssueFilterDto insertIssueFilter(IssueFilterDto issueFilter, String user) {
    filterDao.insert(issueFilter);
    addFavouriteIssueFilter(issueFilter.getId(), user);
    return issueFilter;
  }

  private IssueFilterDto insertIssueFilter(IssueFilterDto issueFilter) {
    filterDao.insert(issueFilter);
    return issueFilter;
  }

  private boolean isAdmin(String user) {
    return authorizationDao.selectGlobalPermissions(user).contains(GlobalPermissions.SYSTEM_ADMIN);
  }

  private IssueFilterResult createIssueFilterResult(SearchResult<IssueDoc> issues, SearchOptions options) {
    return new IssueFilterResult(issues.getDocs(), Paging.create(options.getLimit(), options.getPage(), (int) issues.getTotal()));
  }

  private boolean hasUserSharingPermission(String user) {
    return authorizationDao.selectGlobalPermissions(user).contains(GlobalPermissions.DASHBOARD_SHARING);
  }

  private boolean isFilterOwnedByUser(IssueFilterDto filter, String login) {
    String ownerLogin = filter.getUserLogin();
    return ownerLogin != null && ownerLogin.equals(login);
  }

  public static class IssueFilterResult {

    private final List<IssueDoc> issues;
    private final Paging paging;

    public IssueFilterResult(List<IssueDoc> issues, Paging paging) {
      this.issues = issues;
      this.paging = paging;
    }

    public List<IssueDoc> issues() {
      return issues;
    }

    public Paging paging() {
      return paging;
    }
  }

}
