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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.IssueComment;
import org.sonar.api.server.ServerSide;
import org.sonar.api.web.UserRole;
import org.sonar.core.issue.DefaultIssueComment;
import org.sonar.db.issue.IssueFilterDto;
import org.sonar.server.es.SearchOptions;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.issue.filter.IssueFilterService;
import org.sonar.server.issue.workflow.Transition;
import org.sonar.server.search.QueryContext;
import org.sonar.server.user.UserSession;
import org.sonar.server.util.RubyUtils;
import org.sonar.server.util.Validation;
import org.sonarqube.ws.client.issue.IssueFilterParameters;

/**
 * Used through ruby code <pre>Internal.issues</pre>
 * <p/>
 * All the issue features that are not published to public API.
 *
 * @since 3.6
 */
@ServerSide
public class InternalRubyIssueService {

  private static final String ID_PARAM = "id";
  private static final String NAME_PARAM = "name";
  private static final String DESCRIPTION_PARAM = "description";
  private static final String USER_PARAM = "user";

  private final IssueService issueService;
  private final IssueQueryService issueQueryService;
  private final IssueCommentService commentService;
  private final IssueChangelogService changelogService;
  private final IssueFilterService issueFilterService;
  private final IssueBulkChangeService issueBulkChangeService;
  private final ActionService actionService;
  private final UserSession userSession;

  public InternalRubyIssueService(
    IssueService issueService,
    IssueQueryService issueQueryService,
    IssueCommentService commentService,
    IssueChangelogService changelogService,
    IssueFilterService issueFilterService, IssueBulkChangeService issueBulkChangeService,
    ActionService actionService, UserSession userSession) {
    this.issueService = issueService;
    this.issueQueryService = issueQueryService;
    this.commentService = commentService;
    this.changelogService = changelogService;
    this.issueFilterService = issueFilterService;
    this.issueBulkChangeService = issueBulkChangeService;
    this.actionService = actionService;
    this.userSession = userSession;
  }

  public List<Transition> listTransitions(String issueKey) {
    return issueService.listTransitions(issueKey);
  }

  public List<Transition> listTransitions(Issue issue) {
    return issueService.listTransitions(issue);
  }

  public List<String> listStatus() {
    return issueService.listStatus();
  }

  public List<String> listResolutions() {
    return Issue.RESOLUTIONS;
  }

  public IssueChangelog changelog(String issueKey) {
    return changelogService.changelog(issueKey);
  }

  public IssueChangelog changelog(Issue issue) {
    return changelogService.changelog(issue);
  }

  public List<DefaultIssueComment> findComments(String issueKey) {
    return commentService.findComments(issueKey);
  }

  public List<DefaultIssueComment> findCommentsByIssueKeys(Collection<String> issueKeys) {
    return commentService.findComments(issueKeys);
  }

  public Result<IssueComment> addComment(String issueKey, String text) {
    Result<IssueComment> result = Result.of();
    try {
      result.set(commentService.addComment(issueKey, text));
    } catch (Exception e) {
      result.addError(e.getMessage());
    }
    return result;
  }

  public IssueComment deleteComment(String commentKey) {
    return commentService.deleteComment(commentKey);
  }

  public Result<IssueComment> editComment(String commentKey, String newText) {
    Result<IssueComment> result = Result.of();
    try {
      result.set(commentService.editComment(commentKey, newText));
    } catch (Exception e) {
      result.addError(e.getMessage());
    }
    return result;
  }

  public IssueComment findComment(String commentKey) {
    return commentService.findComment(commentKey);
  }

  public List<String> listActions(String issueKey) {
    return actionService.listAvailableActions(issueKey);
  }

  public IssueQuery emptyIssueQuery() {
    return issueQueryService.createFromMap(Maps.<String, Object>newHashMap());
  }

  @CheckForNull
  public IssueFilterDto findIssueFilterById(Long id) {
    return issueFilterService.findById(id);
  }

  /**
   * Return the issue filter if the user has the right to see it
   * Never return null
   */
  public IssueFilterDto findIssueFilter(Long id) {
    return issueFilterService.find(id, userSession);
  }

  public boolean isUserAuthorized(IssueFilterDto issueFilter) {
    try {
      String user = issueFilterService.getLoggedLogin(userSession);
      issueFilterService.verifyCurrentUserCanReadFilter(issueFilter, user);
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  public boolean canUserShareIssueFilter() {
    return issueFilterService.canShareFilter(userSession);
  }

  public String serializeFilterQuery(Map<String, Object> filterQuery) {
    return issueFilterService.serializeFilterQuery(filterQuery);
  }

  public Map<String, Object> deserializeFilterQuery(IssueFilterDto issueFilter) {
    return issueFilterService.deserializeIssueFilterQuery(issueFilter);
  }

  public Map<String, Object> sanitizeFilterQuery(Map<String, Object> filterQuery) {
    return Maps.filterEntries(filterQuery, MatchIssueFilterParameters.INSTANCE);
  }

  /**
   * Execute issue filter from parameters
   */
  public IssueFilterService.IssueFilterResult execute(Map<String, Object> props) {
    return issueFilterService.execute(issueQueryService.createFromMap(props), toSearchOptions(props));
  }

  /**
   * Execute issue filter from existing filter with optional overridable parameters
   */
  public IssueFilterService.IssueFilterResult execute(Long issueFilterId, Map<String, Object> overrideProps) {
    IssueFilterDto issueFilter = issueFilterService.find(issueFilterId, userSession);
    Map<String, Object> props = issueFilterService.deserializeIssueFilterQuery(issueFilter);
    overrideProps(props, overrideProps);
    return execute(props);
  }

  private static void overrideProps(Map<String, Object> props, Map<String, Object> overrideProps) {
    for (Map.Entry<String, Object> entry : overrideProps.entrySet()) {
      props.put(entry.getKey(), entry.getValue());
    }
  }

  /**
   * List user issue filter
   */
  public List<IssueFilterDto> findIssueFiltersForCurrentUser() {
    return issueFilterService.findByUser(userSession);
  }

  /**
   * Create issue filter
   */
  public IssueFilterDto createIssueFilter(Map<String, String> parameters) {
    IssueFilterDto result = createIssueFilterResultForNew(parameters);
    return issueFilterService.save(result, userSession);
  }

  /**
   * Update issue filter
   */
  public IssueFilterDto updateIssueFilter(Map<String, String> parameters) {
    IssueFilterDto result = createIssueFilterResultForUpdate(parameters);
    return issueFilterService.update(result, userSession);
  }

  /**
   * Update issue filter data
   */
  public IssueFilterDto updateIssueFilterQuery(Long issueFilterId, Map<String, Object> data) {
    return issueFilterService.updateFilterQuery(issueFilterId, data, userSession);
  }

  /**
   * Delete issue filter
   */
  public void deleteIssueFilter(Long issueFilterId) {
    issueFilterService.delete(issueFilterId, userSession);
  }

  /**
   * Copy issue filter
   */
  public IssueFilterDto copyIssueFilter(Long issueFilterIdToCopy, Map<String, String> parameters) {
    IssueFilterDto result = createIssueFilterResultForCopy(parameters);
    return issueFilterService.copy(issueFilterIdToCopy, result, userSession);
  }

  @VisibleForTesting
  IssueFilterDto createIssueFilterResultForNew(Map<String, String> params) {
    return createIssueFilterResult(params, false, false);
  }

  @VisibleForTesting
  IssueFilterDto createIssueFilterResultForUpdate(Map<String, String> params) {
    return createIssueFilterResult(params, true, true);
  }

  @VisibleForTesting
  IssueFilterDto createIssueFilterResultForCopy(Map<String, String> params) {
    return createIssueFilterResult(params, false, false);
  }

  @VisibleForTesting
  IssueFilterDto createIssueFilterResult(Map<String, String> params, boolean checkId, boolean checkUser) {
    String id = params.get(ID_PARAM);
    String name = params.get(NAME_PARAM);
    String description = params.get(DESCRIPTION_PARAM);
    String data = params.get("data");
    String user = params.get(USER_PARAM);
    Boolean sharedParam = RubyUtils.toBoolean(params.get("shared"));
    boolean shared = sharedParam != null ? sharedParam : false;

    if (checkId) {
      Validation.checkMandatoryParameter(id, ID_PARAM);
    }
    if (checkUser) {
      Validation.checkMandatoryParameter(user, USER_PARAM);
    }
    Validation.checkMandatorySizeParameter(name, NAME_PARAM, 100);
    checkOptionalSizeParameter(description, DESCRIPTION_PARAM, 4000);

    IssueFilterDto issueFilterDto = new IssueFilterDto()
      .setName(name)
      .setDescription(description)
      .setShared(shared)
      .setUserLogin(user)
      .setData(data);
    if (!Strings.isNullOrEmpty(id)) {
      issueFilterDto.setId(Long.valueOf(id));
    }
    return issueFilterDto;
  }

  public List<IssueFilterDto> findSharedFiltersForCurrentUser() {
    return issueFilterService.findSharedFiltersWithoutUserFilters(userSession);
  }

  public List<IssueFilterDto> findFavouriteIssueFiltersForCurrentUser() {
    return issueFilterService.findFavoriteFilters(userSession);
  }

  public boolean toggleFavouriteIssueFilter(Long issueFilterId) {
    return issueFilterService.toggleFavouriteIssueFilter(issueFilterId, userSession);
  }

  /**
   * Execute a bulk change
   */
  public IssueBulkChangeResult bulkChange(Map<String, Object> props, String comment, boolean sendNotifications) {
    IssueBulkChangeQuery issueBulkChangeQuery = new IssueBulkChangeQuery(props, comment, sendNotifications);
    return issueBulkChangeService.execute(issueBulkChangeQuery, userSession);
  }

  private static void checkOptionalSizeParameter(String value, String paramName, Integer size) {
    if (!Strings.isNullOrEmpty(value) && value.length() > size) {
      throw new BadRequestException(Validation.IS_TOO_LONG_MESSAGE, paramName, size);
    }
  }

  /**
   * Do not make this method static as it's called by rails
   */
  public int maxPageSize() {
    return QueryContext.MAX_LIMIT;
  }

  @VisibleForTesting
  static SearchOptions toSearchOptions(Map<String, Object> props) {
    SearchOptions options = new SearchOptions();
    Integer pageIndex = RubyUtils.toInteger(props.get(IssueFilterParameters.PAGE_INDEX));
    Integer pageSize = RubyUtils.toInteger(props.get(IssueFilterParameters.PAGE_SIZE));
    if (pageSize != null && pageSize < 0) {
      options.setLimit(SearchOptions.MAX_LIMIT);
    } else {
      options.setPage(pageIndex != null ? pageIndex : 1, pageSize != null ? pageSize : 100);
    }
    return options;
  }

  public Collection<String> listTags() {
    return issueService.listTags(null, 0);
  }

  public Map<String, Long> listTagsForComponent(String componentUuid, int pageSize) {
    IssueQuery query = issueQueryService.createFromMap(
      ImmutableMap.<String, Object>of(
        "componentUuids", componentUuid,
        "resolved", false));
    return issueService.listTagsForComponent(query, pageSize);
  }

  public boolean isUserIssueAdmin(String projectUuid) {
    return userSession.hasComponentUuidPermission(UserRole.ISSUE_ADMIN, projectUuid);
  }

  private enum MatchIssueFilterParameters implements Predicate<Map.Entry<String, Object>> {
    INSTANCE;

    @Override
    public boolean apply(@Nonnull Map.Entry<String, Object> input) {
      return IssueFilterParameters.ALL.contains(input.getKey());
    }
  }
}
