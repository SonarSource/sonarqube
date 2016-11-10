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
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.IssueComment;
import org.sonar.api.server.ServerSide;
import org.sonar.api.web.UserRole;
import org.sonar.core.issue.DefaultIssueComment;
import org.sonar.server.es.SearchOptions;
import org.sonar.server.issue.index.IssueDoc;
import org.sonar.server.issue.index.IssueIndex;
import org.sonar.server.issue.workflow.Transition;
import org.sonar.server.search.QueryContext;
import org.sonar.server.user.UserSession;
import org.sonar.server.util.RubyUtils;
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

  private final IssueIndex issueIndex;
  private final IssueService issueService;
  private final IssueQueryService issueQueryService;
  private final IssueCommentService commentService;
  private final IssueChangelogService changelogService;
  private final IssueBulkChangeService issueBulkChangeService;
  private final ActionService actionService;
  private final UserSession userSession;

  public InternalRubyIssueService(
    IssueIndex issueIndex, IssueService issueService,
    IssueQueryService issueQueryService,
    IssueCommentService commentService,
    IssueChangelogService changelogService,
    IssueBulkChangeService issueBulkChangeService,
    ActionService actionService, UserSession userSession) {
    this.issueIndex = issueIndex;
    this.issueService = issueService;
    this.issueQueryService = issueQueryService;
    this.commentService = commentService;
    this.changelogService = changelogService;
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

  /**
   * Execute issue filter from parameters
   */
  public List<IssueDoc> execute(Map<String, Object> props) {
    return issueIndex.search(issueQueryService.createFromMap(props), toSearchOptions(props)).getDocs();
  }

  /**
   * Execute a bulk change
   */
  public IssueBulkChangeResult bulkChange(Map<String, Object> props, String comment, boolean sendNotifications) {
    IssueBulkChangeQuery issueBulkChangeQuery = new IssueBulkChangeQuery(props, comment, sendNotifications);
    return issueBulkChangeService.execute(issueBulkChangeQuery, userSession);
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

}
