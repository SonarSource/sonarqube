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
package org.sonarqube.ws.client.issue;

import org.sonarqube.ws.Issues.SearchWsResponse;
import org.sonarqube.ws.client.BaseService;
import org.sonarqube.ws.client.GetRequest;
import org.sonarqube.ws.client.WsConnector;

import static org.sonarqube.ws.client.issue.IssueFilterParameters.ADDITIONAL_FIELDS;
import static org.sonarqube.ws.client.issue.IssueFilterParameters.ASC;
import static org.sonarqube.ws.client.issue.IssueFilterParameters.ASSIGNED;
import static org.sonarqube.ws.client.issue.IssueFilterParameters.ASSIGNEES;
import static org.sonarqube.ws.client.issue.IssueFilterParameters.AUTHORS;
import static org.sonarqube.ws.client.issue.IssueFilterParameters.COMPONENTS;
import static org.sonarqube.ws.client.issue.IssueFilterParameters.COMPONENT_KEYS;
import static org.sonarqube.ws.client.issue.IssueFilterParameters.COMPONENT_ROOTS;
import static org.sonarqube.ws.client.issue.IssueFilterParameters.COMPONENT_ROOT_UUIDS;
import static org.sonarqube.ws.client.issue.IssueFilterParameters.COMPONENT_UUIDS;
import static org.sonarqube.ws.client.issue.IssueFilterParameters.CREATED_AFTER;
import static org.sonarqube.ws.client.issue.IssueFilterParameters.CREATED_AT;
import static org.sonarqube.ws.client.issue.IssueFilterParameters.CREATED_BEFORE;
import static org.sonarqube.ws.client.issue.IssueFilterParameters.CREATED_IN_LAST;
import static org.sonarqube.ws.client.issue.IssueFilterParameters.DEPRECATED_ACTION_PLANS;
import static org.sonarqube.ws.client.issue.IssueFilterParameters.DIRECTORIES;
import static org.sonarqube.ws.client.issue.IssueFilterParameters.FACET_MODE;
import static org.sonarqube.ws.client.issue.IssueFilterParameters.FILE_UUIDS;
import static org.sonarqube.ws.client.issue.IssueFilterParameters.ISSUES;
import static org.sonarqube.ws.client.issue.IssueFilterParameters.LANGUAGES;
import static org.sonarqube.ws.client.issue.IssueFilterParameters.MODULE_UUIDS;
import static org.sonarqube.ws.client.issue.IssueFilterParameters.ON_COMPONENT_ONLY;
import static org.sonarqube.ws.client.issue.IssueFilterParameters.PROJECTS;
import static org.sonarqube.ws.client.issue.IssueFilterParameters.PROJECT_KEYS;
import static org.sonarqube.ws.client.issue.IssueFilterParameters.PROJECT_UUIDS;
import static org.sonarqube.ws.client.issue.IssueFilterParameters.RESOLUTIONS;
import static org.sonarqube.ws.client.issue.IssueFilterParameters.RESOLVED;
import static org.sonarqube.ws.client.issue.IssueFilterParameters.RULES;
import static org.sonarqube.ws.client.issue.IssueFilterParameters.SEVERITIES;
import static org.sonarqube.ws.client.issue.IssueFilterParameters.SINCE_LEAK_PERIOD;
import static org.sonarqube.ws.client.issue.IssueFilterParameters.STATUSES;
import static org.sonarqube.ws.client.issue.IssueFilterParameters.TAGS;

public class IssuesService extends BaseService {

  public IssuesService(WsConnector wsConnector) {
    super(wsConnector, "api/issues");
  }

  public SearchWsResponse search(SearchWsRequest request) {
    return call(
      new GetRequest(path("search"))
        .setParam(DEPRECATED_ACTION_PLANS, inlineMultipleParamValue(request.getActionPlans()))
        .setParam(ADDITIONAL_FIELDS, inlineMultipleParamValue(request.getAdditionalFields()))
        .setParam(ASC, request.getAsc())
        .setParam(ASSIGNED, request.getAssigned())
        .setParam(ASSIGNEES, inlineMultipleParamValue(request.getAssignees()))
        .setParam(AUTHORS, inlineMultipleParamValue(request.getAuthors()))
        .setParam(COMPONENT_KEYS, inlineMultipleParamValue(request.getComponentKeys()))
        .setParam(COMPONENT_ROOT_UUIDS, inlineMultipleParamValue(request.getComponentRootUuids()))
        .setParam(COMPONENT_ROOTS, inlineMultipleParamValue(request.getComponentRoots()))
        .setParam(COMPONENT_UUIDS, inlineMultipleParamValue(request.getComponentUuids()))
        .setParam(COMPONENTS, inlineMultipleParamValue(request.getComponents()))
        .setParam(CREATED_AFTER, request.getCreatedAfter())
        .setParam(CREATED_AT, request.getCreatedAt())
        .setParam(CREATED_BEFORE, request.getCreatedBefore())
        .setParam(CREATED_IN_LAST, request.getCreatedInLast())
        .setParam(DIRECTORIES, inlineMultipleParamValue(request.getDirectories()))
        .setParam(FACET_MODE, request.getFacetMode())
        .setParam("facets", inlineMultipleParamValue(request.getFacets()))
        .setParam(FILE_UUIDS, inlineMultipleParamValue(request.getFileUuids()))
        .setParam(ISSUES, inlineMultipleParamValue(request.getIssues()))
        .setParam(LANGUAGES, inlineMultipleParamValue(request.getLanguages()))
        .setParam(MODULE_UUIDS, inlineMultipleParamValue(request.getModuleUuids()))
        .setParam(ON_COMPONENT_ONLY, request.getOnComponentOnly())
        .setParam("p", request.getPage())
        .setParam("ps", request.getPageSize())
        .setParam(PROJECT_KEYS, inlineMultipleParamValue(request.getProjectKeys()))
        .setParam(PROJECT_UUIDS, inlineMultipleParamValue(request.getProjectUuids()))
        .setParam(PROJECTS, inlineMultipleParamValue(request.getProjects()))
        .setParam(RESOLUTIONS, inlineMultipleParamValue(request.getResolutions()))
        .setParam(RESOLVED, request.getResolved())
        .setParam(RULES, inlineMultipleParamValue(request.getRules()))
        .setParam("s", request.getSort())
        .setParam(SEVERITIES, inlineMultipleParamValue(request.getSeverities()))
        .setParam(SINCE_LEAK_PERIOD, request.getSinceLeakPeriod())
        .setParam(STATUSES, inlineMultipleParamValue(request.getStatuses()))
        .setParam(TAGS, inlineMultipleParamValue(request.getTags())),
      SearchWsResponse.parser());
  }
}
