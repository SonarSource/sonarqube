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

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import org.sonar.api.ServerComponent;
import org.sonar.api.utils.Paging;
import org.sonar.core.issue.DefaultIssueFilter;
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

import java.util.List;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;

public class IssueFilterService implements ServerComponent {

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

  public DefaultIssueFilter find(Long id, UserSession userSession) {
    return findIssueFilterDto(id, getLoggedLogin(userSession)).toIssueFilter();
  }

  @CheckForNull
  public DefaultIssueFilter findById(Long id) {
    IssueFilterDto issueFilterDto = filterDao.selectById(id);
    if (issueFilterDto != null) {
      return issueFilterDto.toIssueFilter();
    }
    return null;
  }

  public List<DefaultIssueFilter> findByUser(UserSession userSession) {
    return toIssueFilters(selectUserIssueFilters(getLoggedLogin(userSession)));
  }

  public DefaultIssueFilter save(DefaultIssueFilter issueFilter, UserSession userSession) {
    String user = getLoggedLogin(userSession);
    issueFilter.setUser(user);
    validateFilter(issueFilter);
    return insertIssueFilter(issueFilter, user);
  }

  public DefaultIssueFilter update(DefaultIssueFilter issueFilter, UserSession userSession) {
    String login = getLoggedLogin(userSession);
    IssueFilterDto existingFilterDto = findIssueFilterDto(issueFilter.id(), login);
    verifyCurrentUserCanModifyFilter(existingFilterDto.toIssueFilter(), login);
    verifyCurrentUserCanChangeFilterSharingFilter(issueFilter, existingFilterDto, login);
    if (!existingFilterDto.getUserLogin().equals(issueFilter.user())) {
      verifyCurrentUserCanChangeFilterOwnership(login);
    }
    validateFilter(issueFilter);
    deleteOtherFavoriteFiltersIfFilterBecomeUnshared(existingFilterDto, issueFilter);
    filterDao.update(IssueFilterDto.toIssueFilter(issueFilter));
    return issueFilter;
  }

  private void deleteOtherFavoriteFiltersIfFilterBecomeUnshared(IssueFilterDto existingFilterDto, DefaultIssueFilter issueFilter) {
    if (existingFilterDto.isShared() && !issueFilter.shared()) {
      for (IssueFilterFavouriteDto favouriteDto : selectFavouriteFilters(existingFilterDto.getId())) {
        if (!favouriteDto.getUserLogin().equals(issueFilter.user())) {
          deleteFavouriteIssueFilter(favouriteDto);
        }
      }
    }
  }

  public DefaultIssueFilter updateFilterQuery(Long issueFilterId, Map<String, Object> filterQuery, UserSession userSession) {
    String login = getLoggedLogin(userSession);
    IssueFilterDto issueFilterDto = findIssueFilterDto(issueFilterId, login);
    verifyCurrentUserCanModifyFilter(issueFilterDto.toIssueFilter(), login);

    DefaultIssueFilter issueFilter = issueFilterDto.toIssueFilter();
    issueFilter.setData(serializeFilterQuery(filterQuery));
    filterDao.update(IssueFilterDto.toIssueFilter(issueFilter));
    return issueFilter;
  }

  public void delete(Long issueFilterId, UserSession userSession) {
    String login = getLoggedLogin(userSession);
    IssueFilterDto issueFilterDto = findIssueFilterDto(issueFilterId, login);
    verifyCurrentUserCanModifyFilter(issueFilterDto.toIssueFilter(), login);

    deleteFavouriteIssueFilters(issueFilterDto);
    filterDao.delete(issueFilterId);
  }

  public DefaultIssueFilter copy(Long issueFilterIdToCopy, DefaultIssueFilter issueFilter, UserSession userSession) {
    String login = getLoggedLogin(userSession);
    IssueFilterDto issueFilterDtoToCopy = findIssueFilterDto(issueFilterIdToCopy, login);
    // Copy of filter should not be shared
    issueFilterDtoToCopy.setShared(false);
    issueFilter.setUser(login);
    issueFilter.setData(issueFilterDtoToCopy.getData());
    validateFilter(issueFilter);
    return insertIssueFilter(issueFilter, login);
  }

  public List<DefaultIssueFilter> findSharedFiltersWithoutUserFilters(UserSession userSession) {
    final String login = getLoggedLogin(userSession);
    return toIssueFilters(newArrayList(Iterables.filter(selectSharedFilters(), new Predicate<IssueFilterDto>() {
      @Override
      public boolean apply(IssueFilterDto input) {
        return !input.getUserLogin().equals(login);
      }
    })));
  }

  public List<DefaultIssueFilter> findFavoriteFilters(UserSession userSession) {
    return toIssueFilters(filterDao.selectFavoriteFiltersByUser(getLoggedLogin(userSession)));
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

  public Map<String, Object> deserializeIssueFilterQuery(DefaultIssueFilter issueFilter) {
    return serializer.deserialize(issueFilter.data());
  }

  private IssueFilterDto findIssueFilterDto(Long id, String login) {
    IssueFilterDto issueFilterDto = filterDao.selectById(id);
    if (issueFilterDto == null) {
      throw new NotFoundException("Filter not found: " + id);
    }
    verifyCurrentUserCanReadFilter(issueFilterDto.toIssueFilter(), login);
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

  public void verifyCurrentUserCanReadFilter(DefaultIssueFilter issueFilter, String login) {
    if (!issueFilter.user().equals(login) && !issueFilter.shared()) {
      throw new ForbiddenException("User is not authorized to read this filter");
    }
  }

  private void verifyCurrentUserCanModifyFilter(DefaultIssueFilter issueFilter, String user) {
    if (!issueFilter.user().equals(user) && !isAdmin(user)) {
      throw new ForbiddenException("User is not authorized to modify this filter");
    }
  }

  private void verifyCurrentUserCanChangeFilterSharingFilter(DefaultIssueFilter issueFilter, IssueFilterDto existingFilterDto, String login) {
    if (existingFilterDto.isShared() != issueFilter.shared() && !existingFilterDto.getUserLogin().equals(login)) {
      throw new ForbiddenException("Only owner of a filter can change sharing");
    }
  }

  private void verifyCurrentUserCanChangeFilterOwnership(String user) {
    if (!isAdmin(user)) {
      throw new ForbiddenException("User is not authorized to change the owner of this filter");
    }
  }

  private void verifyCurrentUserCanShareFilter(DefaultIssueFilter issueFilter, String user) {
    if (issueFilter.shared() && !hasUserSharingPermission(user)) {
      throw new ForbiddenException("User cannot own this filter because of insufficient rights");
    }
  }

  private void validateFilter(final DefaultIssueFilter issueFilter) {
    List<IssueFilterDto> userFilters = selectUserIssueFilters(issueFilter.user());
    IssueFilterDto userFilterSameName = findFilterWithSameName(userFilters, issueFilter.name());
    if (userFilterSameName != null && !userFilterSameName.getId().equals(issueFilter.id())) {
      throw new BadRequestException("Name already exists");
    }
    if (issueFilter.shared()) {
      List<IssueFilterDto> sharedFilters = selectSharedFilters();
      IssueFilterDto sharedFilterWithSameName = findFilterWithSameName(sharedFilters, issueFilter.name());
      if (sharedFilterWithSameName != null && !sharedFilterWithSameName.getId().equals(issueFilter.id())) {
        throw new BadRequestException("Other users already share filters with the same name");
      }
      verifyCurrentUserCanShareFilter(issueFilter, issueFilter.user());
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

  private DefaultIssueFilter insertIssueFilter(DefaultIssueFilter issueFilter, String user) {
    IssueFilterDto issueFilterDto = IssueFilterDto.toIssueFilter(issueFilter);
    filterDao.insert(issueFilterDto);
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
    return authorizationDao.selectGlobalPermissions(user).contains(GlobalPermissions.SYSTEM_ADMIN);
  }

  private IssueFilterResult createIssueFilterResult(SearchResult<IssueDoc> issues, SearchOptions options) {
    return new IssueFilterResult(issues.getDocs(), Paging.create(options.getLimit(), options.getPage(), (int) issues.getTotal()));
  }

  private boolean hasUserSharingPermission(String user) {
    return authorizationDao.selectGlobalPermissions(user).contains(GlobalPermissions.DASHBOARD_SHARING);
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
