/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
 * mailto:info AT sonarsource DOT com
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
package org.sonarqube.ws.client.issue;

import java.util.Arrays;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.sonarqube.ws.Issues;
import org.sonarqube.ws.Issues.ChangelogWsResponse;
import org.sonarqube.ws.Issues.SearchWsResponse;
import org.sonarqube.ws.client.BaseService;
import org.sonarqube.ws.client.GetRequest;
import org.sonarqube.ws.client.PostRequest;
import org.sonarqube.ws.client.WsConnector;
import org.sonarqube.ws.client.WsResponse;

import static org.sonar.api.server.ws.WebService.Param.FACETS;
import static org.sonar.api.server.ws.WebService.Param.PAGE;
import static org.sonar.api.server.ws.WebService.Param.PAGE_SIZE;
import static org.sonar.api.server.ws.WebService.Param.SORT;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.ACTION_ADD_COMMENT;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.ACTION_ASSIGN;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.ACTION_BULK_CHANGE;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.ACTION_CHANGELOG;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.ACTION_DELETE_COMMENT;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.ACTION_DO_TRANSITION;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.ACTION_EDIT_COMMENT;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.ACTION_SEARCH;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.ACTION_SET_SEVERITY;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.ACTION_SET_TAGS;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.ACTION_SET_TYPE;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.ACTION_TAGS;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.CONTROLLER_ISSUES;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.DEPRECATED_PARAM_ACTION_PLANS;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.FACET_MODE;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_ADDITIONAL_FIELDS;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_ADD_TAGS;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_ASC;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_ASSIGN;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_ASSIGNED;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_ASSIGNEE;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_ASSIGNEES;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_AUTHORS;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_COMMENT;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_COMPONENTS;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_COMPONENT_KEYS;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_COMPONENT_ROOTS;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_COMPONENT_ROOT_UUIDS;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_COMPONENT_UUIDS;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_CREATED_AFTER;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_CREATED_AT;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_CREATED_BEFORE;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_CREATED_IN_LAST;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_DIRECTORIES;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_DO_TRANSITION;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_FILE_UUIDS;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_ISSUE;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_ISSUES;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_LANGUAGES;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_MODULE_UUIDS;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_ON_COMPONENT_ONLY;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_ORGANIZATION;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_PROJECTS;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_PROJECT_KEYS;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_PROJECT_UUIDS;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_REMOVE_TAGS;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_RESOLUTIONS;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_RESOLVED;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_RULES;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_SEND_NOTIFICATIONS;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_SET_SEVERITY;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_SET_TYPE;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_SEVERITIES;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_SEVERITY;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_SINCE_LEAK_PERIOD;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_STATUSES;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_TAGS;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_TEXT;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_TRANSITION;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_TYPE;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_TYPES;

public class IssuesService extends BaseService {

  public IssuesService(WsConnector wsConnector) {
    super(wsConnector, CONTROLLER_ISSUES);
  }

  public Issues.Operation addComment(AddCommentRequest request) {
    return call(new PostRequest(path(ACTION_ADD_COMMENT))
      .setParam(PARAM_ISSUE, request.getIssue())
      .setParam(PARAM_TEXT, request.getText()),
      Issues.Operation.parser());
  }

  public Issues.Operation assign(AssignRequest request) {
    return call(new PostRequest(path(ACTION_ASSIGN))
      .setParam(PARAM_ISSUE, request.getIssue())
      .setParam(PARAM_ASSIGNEE, request.getAssignee()),
      Issues.Operation.parser());
  }

  public Issues.BulkChangeWsResponse bulkChange(BulkChangeRequest request) {
    return call(new PostRequest(path(ACTION_BULK_CHANGE))
      .setParam(PARAM_ISSUES, inlineMultipleParamValue(request.getIssues()))
      .setParam(PARAM_ASSIGN, request.getAssign())
      .setParam(PARAM_SET_SEVERITY, request.getSetSeverity())
      .setParam(PARAM_SET_TYPE, request.getSetType())
      .setParam(PARAM_DO_TRANSITION, request.getDoTransition())
      .setParam(PARAM_ADD_TAGS, inlineMultipleParamValue(request.getAddTags()))
      .setParam(PARAM_REMOVE_TAGS, inlineMultipleParamValue(request.getRemoveTags()))
      .setParam(PARAM_COMMENT, request.getComment())
      .setParam(PARAM_SEND_NOTIFICATIONS, request.getSendNotifications()),
      Issues.BulkChangeWsResponse.parser());
  }

  public ChangelogWsResponse changelog(String issueKey) {
    return call(new GetRequest(path(ACTION_CHANGELOG))
      .setParam(PARAM_ISSUE, issueKey),
      ChangelogWsResponse.parser());
  }

  public Issues.Operation doTransition(DoTransitionRequest request) {
    return call(new PostRequest(path(ACTION_DO_TRANSITION))
      .setParam(PARAM_ISSUE, request.getIssue())
      .setParam(PARAM_TRANSITION, request.getTransition()),
      Issues.Operation.parser());
  }

  public Issues.Operation deleteComment(String commentKey) {
    return call(new PostRequest(path(ACTION_DELETE_COMMENT))
      .setParam(PARAM_COMMENT, commentKey),
      Issues.Operation.parser());
  }

  public Issues.Operation editComment(EditCommentRequest request) {
    return call(new PostRequest(path(ACTION_EDIT_COMMENT))
      .setParam(PARAM_COMMENT, request.getComment())
      .setParam(PARAM_TEXT, request.getText()),
      Issues.Operation.parser());
  }

  public SearchWsResponse search(SearchWsRequest request) {
    return call(
      new GetRequest(path(ACTION_SEARCH))
        .setParam(DEPRECATED_PARAM_ACTION_PLANS, inlineMultipleParamValue(request.getActionPlans()))
        .setParam(PARAM_ADDITIONAL_FIELDS, inlineMultipleParamValue(request.getAdditionalFields()))
        .setParam(PARAM_ASC, request.getAsc())
        .setParam(PARAM_ASSIGNED, request.getAssigned())
        .setParam(PARAM_ASSIGNEES, inlineMultipleParamValue(request.getAssignees()))
        .setParam(PARAM_AUTHORS, inlineMultipleParamValue(request.getAuthors()))
        .setParam(PARAM_COMPONENT_KEYS, inlineMultipleParamValue(request.getComponentKeys()))
        .setParam(PARAM_COMPONENT_ROOT_UUIDS, inlineMultipleParamValue(request.getComponentRootUuids()))
        .setParam(PARAM_COMPONENT_ROOTS, inlineMultipleParamValue(request.getComponentRoots()))
        .setParam(PARAM_COMPONENT_UUIDS, inlineMultipleParamValue(request.getComponentUuids()))
        .setParam(PARAM_COMPONENTS, inlineMultipleParamValue(request.getComponents()))
        .setParam(PARAM_CREATED_AFTER, request.getCreatedAfter())
        .setParam(PARAM_CREATED_AT, request.getCreatedAt())
        .setParam(PARAM_CREATED_BEFORE, request.getCreatedBefore())
        .setParam(PARAM_CREATED_IN_LAST, request.getCreatedInLast())
        .setParam(PARAM_DIRECTORIES, inlineMultipleParamValue(request.getDirectories()))
        .setParam(FACET_MODE, request.getFacetMode())
        .setParam(FACETS, inlineMultipleParamValue(request.getFacets()))
        .setParam(PARAM_FILE_UUIDS, inlineMultipleParamValue(request.getFileUuids()))
        .setParam(PARAM_ISSUES, inlineMultipleParamValue(request.getIssues()))
        .setParam(PARAM_LANGUAGES, inlineMultipleParamValue(request.getLanguages()))
        .setParam(PARAM_MODULE_UUIDS, inlineMultipleParamValue(request.getModuleUuids()))
        .setParam(PARAM_ON_COMPONENT_ONLY, request.getOnComponentOnly())
        .setParam(PAGE, request.getPage())
        .setParam(PAGE_SIZE, request.getPageSize())
        .setParam(PARAM_PROJECT_KEYS, inlineMultipleParamValue(request.getProjectKeys()))
        .setParam(PARAM_PROJECT_UUIDS, inlineMultipleParamValue(request.getProjectUuids()))
        .setParam(PARAM_PROJECTS, inlineMultipleParamValue(request.getProjects()))
        .setParam(PARAM_RESOLUTIONS, inlineMultipleParamValue(request.getResolutions()))
        .setParam(PARAM_RESOLVED, request.getResolved())
        .setParam(PARAM_RULES, inlineMultipleParamValue(request.getRules()))
        .setParam(SORT, request.getSort())
        .setParam(PARAM_SEVERITIES, inlineMultipleParamValue(request.getSeverities()))
        .setParam(PARAM_SINCE_LEAK_PERIOD, request.getSinceLeakPeriod())
        .setParam(PARAM_STATUSES, inlineMultipleParamValue(request.getStatuses()))
        .setParam(PARAM_TAGS, inlineMultipleParamValue(request.getTags()))
        .setParam(PARAM_TYPES, inlineMultipleParamValue(request.getTypes())),
      SearchWsResponse.parser());
  }

  public Issues.Operation setSeverity(SetSeverityRequest request) {
    return call(new PostRequest(path(ACTION_SET_SEVERITY))
      .setParam(PARAM_ISSUE, request.getIssue())
      .setParam(PARAM_SEVERITY, request.getSeverity()),
      Issues.Operation.parser());
  }

  public Issues.Operation setType(SetTypeRequest request) {
    return call(new PostRequest(path(ACTION_SET_TYPE))
      .setParam(PARAM_ISSUE, request.getIssue())
      .setParam(PARAM_TYPE, request.getType()),
      Issues.Operation.parser());
  }


  public Issues.Operation setTags(String issue, String... tags) {
    return call(new PostRequest(path(ACTION_SET_TAGS))
        .setParam(PARAM_ISSUE, issue)
        .setParam(PARAM_TAGS, Arrays.stream(tags).collect(Collectors.joining(","))),
      Issues.Operation.parser());
  }

  public WsResponse getTags(@Nullable String organization) {
    return call(new PostRequest(path(ACTION_TAGS))
      .setParam(PARAM_ORGANIZATION, organization)
    );
  }
}
