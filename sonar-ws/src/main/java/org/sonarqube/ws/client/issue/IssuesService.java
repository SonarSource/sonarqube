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

package org.sonarqube.ws.client.issue;

import com.google.common.base.Joiner;
import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonarqube.ws.Issues.SearchWsResponse;
import org.sonarqube.ws.client.BaseService;
import org.sonarqube.ws.client.GetRequest;
import org.sonarqube.ws.client.WsConnector;

import static org.sonarqube.ws.client.issue.IssueFilterParameters.ACTION_PLANS;
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
import static org.sonarqube.ws.client.issue.IssueFilterParameters.DIRECTORIES;
import static org.sonarqube.ws.client.issue.IssueFilterParameters.FACET_MODE;
import static org.sonarqube.ws.client.issue.IssueFilterParameters.FILE_UUIDS;
import static org.sonarqube.ws.client.issue.IssueFilterParameters.ISSUES;
import static org.sonarqube.ws.client.issue.IssueFilterParameters.LANGUAGES;
import static org.sonarqube.ws.client.issue.IssueFilterParameters.MODULE_UUIDS;
import static org.sonarqube.ws.client.issue.IssueFilterParameters.ON_COMPONENT_ONLY;
import static org.sonarqube.ws.client.issue.IssueFilterParameters.PLANNED;
import static org.sonarqube.ws.client.issue.IssueFilterParameters.PROJECTS;
import static org.sonarqube.ws.client.issue.IssueFilterParameters.PROJECT_KEYS;
import static org.sonarqube.ws.client.issue.IssueFilterParameters.PROJECT_UUIDS;
import static org.sonarqube.ws.client.issue.IssueFilterParameters.REPORTERS;
import static org.sonarqube.ws.client.issue.IssueFilterParameters.RESOLUTIONS;
import static org.sonarqube.ws.client.issue.IssueFilterParameters.RESOLVED;
import static org.sonarqube.ws.client.issue.IssueFilterParameters.RULES;
import static org.sonarqube.ws.client.issue.IssueFilterParameters.SEVERITIES;
import static org.sonarqube.ws.client.issue.IssueFilterParameters.STATUSES;
import static org.sonarqube.ws.client.issue.IssueFilterParameters.TAGS;

public class IssuesService extends BaseService {
  private static final Joiner LIST_TO_PARAMS_STRING = Joiner.on(",").skipNulls();

  public IssuesService(WsConnector wsConnector) {
    super(wsConnector, "api/issues");
  }

  public SearchWsResponse search(SearchWsRequest request) {
    return call(
      new GetRequest(path("search"))
        .setParam(ACTION_PLANS, listToParamList(request.getActionPlans()))
        .setParam(ADDITIONAL_FIELDS, listToParamList(request.getAdditionalFields()))
        .setParam(ASC, request.getAsc())
        .setParam(ASSIGNED, request.getAssigned())
        .setParam(ASSIGNEES, listToParamList(request.getAssignees()))
        .setParam(AUTHORS, listToParamList(request.getAuthors()))
        .setParam(COMPONENT_KEYS, listToParamList(request.getComponentKeys()))
        .setParam(COMPONENT_ROOT_UUIDS, listToParamList(request.getComponentRootUuids()))
        .setParam(COMPONENT_ROOTS, listToParamList(request.getComponentRoots()))
        .setParam(COMPONENT_UUIDS, listToParamList(request.getComponentUuids()))
        .setParam(COMPONENTS, listToParamList(request.getComponents()))
        .setParam(CREATED_AFTER, request.getCreatedAfter())
        .setParam(CREATED_AT, request.getCreatedAt())
        .setParam(CREATED_BEFORE, request.getCreatedBefore())
        .setParam(CREATED_IN_LAST, request.getCreatedInLast())
        .setParam(DIRECTORIES, listToParamList(request.getDirectories()))
        .setParam(FACET_MODE, request.getFacetMode())
        .setParam("facets", listToParamList(request.getFacets()))
        .setParam(FILE_UUIDS, listToParamList(request.getFileUuids()))
        .setParam(ISSUES, listToParamList(request.getIssues()))
        .setParam(LANGUAGES, listToParamList(request.getLanguages()))
        .setParam(MODULE_UUIDS, listToParamList(request.getModuleUuids()))
        .setParam(ON_COMPONENT_ONLY, request.getOnComponentOnly())
        .setParam("p", request.getPage())
        .setParam("ps", request.getPageSize())
        .setParam(PLANNED, request.getPlanned())
        .setParam(PROJECT_KEYS, listToParamList(request.getProjectKeys()))
        .setParam(PROJECT_UUIDS, listToParamList(request.getProjectUuids()))
        .setParam(PROJECTS, listToParamList(request.getProjects()))
        .setParam(REPORTERS, listToParamList(request.getReporters()))
        .setParam(RESOLUTIONS, listToParamList(request.getResolutions()))
        .setParam(RESOLVED, request.getResolved())
        .setParam(RULES, listToParamList(request.getRules()))
        .setParam("s", request.getSort())
        .setParam(SEVERITIES, listToParamList(request.getSeverities()))
        .setParam(STATUSES, listToParamList(request.getStatuses()))
        .setParam(TAGS, listToParamList(request.getTags())),
      SearchWsResponse.parser());
  }

  @CheckForNull
  private static String listToParamList(@Nullable List<String> strings) {
    return strings == null
      ? null
      : LIST_TO_PARAMS_STRING.join(strings);
  }
}
