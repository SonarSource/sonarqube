/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.server.project.ws;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.api.utils.Paging;
import org.sonar.db.DatabaseUtils;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentQuery;
import org.sonar.db.component.ProjectLastAnalysisDateDto;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.permission.GlobalPermission;
import org.sonar.server.management.ManagedProjectService;
import org.sonar.server.project.Visibility;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.Projects.SearchWsResponse;

import static java.util.Optional.ofNullable;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static org.sonar.db.component.ComponentQualifiers.APP;
import static org.sonar.db.component.ComponentQualifiers.PROJECT;
import static org.sonar.db.component.ComponentQualifiers.VIEW;
import static org.sonar.api.utils.DateUtils.formatDateTime;
import static org.sonar.api.utils.DateUtils.parseDateOrDateTime;
import static org.sonar.db.Pagination.forPage;
import static org.sonar.server.project.Visibility.PRIVATE;
import static org.sonar.server.project.Visibility.PUBLIC;
import static org.sonar.server.ws.KeyExamples.KEY_PROJECT_EXAMPLE_001;
import static org.sonar.server.ws.KeyExamples.KEY_PROJECT_EXAMPLE_002;
import static org.sonar.server.ws.WsUtils.writeProtobuf;
import static org.sonarqube.ws.Projects.SearchWsResponse.Component;
import static org.sonarqube.ws.Projects.SearchWsResponse.newBuilder;
import static org.sonarqube.ws.client.project.ProjectsWsParameters.ACTION_SEARCH;
import static org.sonarqube.ws.client.project.ProjectsWsParameters.MAX_PAGE_SIZE;
import static org.sonarqube.ws.client.project.ProjectsWsParameters.PARAM_ANALYZED_BEFORE;
import static org.sonarqube.ws.client.project.ProjectsWsParameters.PARAM_ON_PROVISIONED_ONLY;
import static org.sonarqube.ws.client.project.ProjectsWsParameters.PARAM_PROJECTS;
import static org.sonarqube.ws.client.project.ProjectsWsParameters.PARAM_QUALIFIERS;
import static org.sonarqube.ws.client.project.ProjectsWsParameters.PARAM_VISIBILITY;

public class SearchAction implements ProjectsWsAction {

  private final DbClient dbClient;
  private final UserSession userSession;
  private final ManagedProjectService managedProjectService;

  public SearchAction(DbClient dbClient, UserSession userSession, ManagedProjectService managedProjectService) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.managedProjectService = managedProjectService;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction(ACTION_SEARCH)
      .setSince("6.3")
      .setDescription("""
        Search for projects or views to administrate them.
        <ul>
          <li>The response field 'lastAnalysisDate' takes into account the analysis of all branches and pull requests, not only the main branch.</li>
          <li>The response field 'revision' takes into account the analysis of the main branch only.</li>
        </ul>
        Requires 'Administer System' permission""")
      .addPagingParams(100, MAX_PAGE_SIZE)
      .setResponseExample(getClass().getResource("search-example.json"))
      .setChangelog(new Change("10.2", "Response includes 'managed' field."))
      .setChangelog(new Change("9.1", "The parameter '" + PARAM_ANALYZED_BEFORE + "' and the field 'lastAnalysisDate' of the returned projects "
        + "take into account the analysis of all branches and pull requests, not only the main branch."))
      .setHandler(this);

    action.createParam(Param.TEXT_QUERY)
      .setDescription("Limit search to: <ul>" +
        "<li>component names that contain the supplied string</li>" +
        "<li>component keys that contain the supplied string</li>" +
        "</ul>")
      .setExampleValue("sonar");

    action.createParam(PARAM_QUALIFIERS)
      .setDescription("Comma-separated list of component qualifiers. Filter the results with the specified qualifiers")
      .setPossibleValues(PROJECT, VIEW, APP)
      .setDefaultValue(PROJECT);

    action.createParam(PARAM_VISIBILITY)
      .setDescription("Filter the projects that should be visible to everyone (%s), or only specific user/groups (%s).<br/>" +
        "If no visibility is specified, the default project visibility will be used.",
        Visibility.PUBLIC.getLabel(), Visibility.PRIVATE.getLabel())
      .setRequired(false)
      .setInternal(true)
      .setSince("6.4")
      .setPossibleValues(Visibility.getLabels());

    action.createParam(PARAM_ANALYZED_BEFORE)
      .setDescription("Filter the projects for which the last analysis of all branches are older than the given date (exclusive).<br> " +
        "Either a date (server timezone) or datetime can be provided.")
      .setSince("6.6")
      .setExampleValue("2017-10-19 or 2017-10-19T13:00:00+0200");

    action.createParam(PARAM_ON_PROVISIONED_ONLY)
      .setDescription("Filter the projects that are provisioned")
      .setBooleanPossibleValues()
      .setDefaultValue("false")
      .setSince("6.6");

    action
      .createParam(PARAM_PROJECTS)
      .setDescription("Comma-separated list of project keys")
      .setSince("6.6")
      // Limitation of ComponentDao#selectByQuery(), max 1000 values are accepted.
      // Restricting size of HTTP parameter allows to not fail with SQL error
      .setMaxValuesAllowed(DatabaseUtils.PARTITION_SIZE_FOR_ORACLE)
      .setExampleValue(String.join(",", KEY_PROJECT_EXAMPLE_001, KEY_PROJECT_EXAMPLE_002));

  }

  @Override
  public void handle(Request wsRequest, Response wsResponse) throws Exception {
    SearchWsResponse searchWsResponse = doHandle(toSearchWsRequest(wsRequest));
    writeProtobuf(searchWsResponse, wsRequest, wsResponse);
  }

  private static SearchRequest toSearchWsRequest(Request request) {
    return SearchRequest.builder()
      .setQualifiers(request.mandatoryParamAsStrings(PARAM_QUALIFIERS))
      .setQuery(request.param(Param.TEXT_QUERY))
      .setPage(request.mandatoryParamAsInt(Param.PAGE))
      .setPageSize(request.mandatoryParamAsInt(Param.PAGE_SIZE))
      .setVisibility(request.param(PARAM_VISIBILITY))
      .setAnalyzedBefore(request.param(PARAM_ANALYZED_BEFORE))
      .setOnProvisionedOnly(request.mandatoryParamAsBoolean(PARAM_ON_PROVISIONED_ONLY))
      .setProjects(request.paramAsStrings(PARAM_PROJECTS))
      .build();
  }

  private SearchWsResponse doHandle(SearchRequest request) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      userSession.checkPermission(GlobalPermission.ADMINISTER);

      ComponentQuery query = buildDbQuery(request);
      Paging paging = buildPaging(dbSession, request, query);
      List<ComponentDto> components = dbClient.componentDao().selectByQuery(dbSession, query, forPage(paging.pageIndex()).andSize(paging.pageSize()));
      Set<String> componentUuids = components.stream().map(ComponentDto::uuid).collect(Collectors.toSet());
      List<BranchDto> branchDtos = dbClient.branchDao().selectByUuids(dbSession, componentUuids);
      Map<String, String> componentUuidToProjectUuid = branchDtos.stream().collect(Collectors.toMap(BranchDto::getUuid,BranchDto::getProjectUuid));
      Map<String, Boolean> projectUuidToManaged = managedProjectService.getProjectUuidToManaged(dbSession, new HashSet<>(componentUuidToProjectUuid.values()));
      Map<String, Boolean> componentUuidToManaged = toComponentUuidToManaged(componentUuidToProjectUuid, projectUuidToManaged);
      Map<String, Long> lastAnalysisDateByComponentUuid = dbClient.snapshotDao().selectLastAnalysisDateByProjectUuids(dbSession, componentUuidToProjectUuid.values()).stream()
        .collect(Collectors.toMap(ProjectLastAnalysisDateDto::getProjectUuid, ProjectLastAnalysisDateDto::getDate));
      Map<String, SnapshotDto> snapshotsByComponentUuid = dbClient.snapshotDao()
        .selectLastAnalysesByRootComponentUuids(dbSession, componentUuids).stream()
        .collect(Collectors.toMap(SnapshotDto::getRootComponentUuid, identity()));

      return buildResponse(components, snapshotsByComponentUuid, lastAnalysisDateByComponentUuid, componentUuidToProjectUuid, componentUuidToManaged, paging);
    }
  }

  private Map<String, Boolean> toComponentUuidToManaged(Map<String, String> componentUuidToProjectUuid, Map<String, Boolean> projectUuidToManaged) {
    return componentUuidToProjectUuid.keySet().stream()
      .collect(toMap(identity(), componentUuid -> isComponentManaged(
        componentUuidToProjectUuid.get(componentUuid),
        projectUuidToManaged))
      );
  }

  private boolean isComponentManaged(String projectUuid, Map<String, Boolean> projectUuidToManaged) {
    return ofNullable(projectUuidToManaged.get(projectUuid)).orElse(false);
  }

  static ComponentQuery buildDbQuery(SearchRequest request) {
    List<String> qualifiers = request.getQualifiers();
    ComponentQuery.Builder query = ComponentQuery.builder()
      .setQualifiers(qualifiers.toArray(new String[0]));

    ofNullable(request.getQuery()).ifPresent(q -> {
      query.setNameOrKeyQuery(q);
      query.setPartialMatchOnKey(true);
    });
    ofNullable(request.getVisibility()).ifPresent(v -> query.setPrivate(Visibility.isPrivate(v)));
    ofNullable(request.getAnalyzedBefore()).ifPresent(d -> query.setAllBranchesAnalyzedBefore(parseDateOrDateTime(d).getTime()));
    query.setOnProvisionedOnly(request.isOnProvisionedOnly());
    ofNullable(request.getProjects()).ifPresent(keys -> query.setComponentKeys(new HashSet<>(keys)));

    return query.build();
  }

  private Paging buildPaging(DbSession dbSession, SearchRequest request, ComponentQuery query) {
    int total = dbClient.componentDao().countByQuery(dbSession, query);
    return Paging.forPageIndex(request.getPage())
      .withPageSize(request.getPageSize())
      .andTotal(total);
  }

  private static SearchWsResponse buildResponse(List<ComponentDto> components, Map<String, SnapshotDto> snapshotsByComponentUuid,
    Map<String, Long> lastAnalysisDateByComponentUuid, Map<String, String> projectUuidByComponentUuid, Map<String, Boolean> componentUuidToManaged, Paging paging) {
    SearchWsResponse.Builder responseBuilder = newBuilder();
    responseBuilder.getPagingBuilder()
      .setPageIndex(paging.pageIndex())
      .setPageSize(paging.pageSize())
      .setTotal(paging.total())
      .build();

    components.stream()
      .map(dto -> {
        var projectUuid = projectUuidByComponentUuid.get(dto.uuid());
        return dtoToProject(
          dto,
          snapshotsByComponentUuid.get(dto.uuid()),
          lastAnalysisDateByComponentUuid.get(projectUuid),
          PROJECT.equals(dto.qualifier()) ? componentUuidToManaged.get(dto.uuid()) : null,
          projectUuid
        );
      })
      .forEach(responseBuilder::addComponents);
    return responseBuilder.build();
  }

  private static Component dtoToProject(
    ComponentDto dto,
    @Nullable SnapshotDto snapshot,
    @Nullable Long lastAnalysisDate,
    @Nullable Boolean isManaged,
    @Nullable String projectUuid) {
    var builder = Component.newBuilder()
      .setKey(dto.getKey())
      .setName(dto.name())
      .setQualifier(dto.qualifier())
      .setVisibility(dto.isPrivate() ? PRIVATE.getLabel() : PUBLIC.getLabel());

    ofNullable(projectUuid).ifPresent(builder::setProjectUuid);
    ofNullable(snapshot).map(SnapshotDto::getRevision).ifPresent(builder::setRevision);
    ofNullable(lastAnalysisDate).ifPresent(d -> builder.setLastAnalysisDate(formatDateTime(d)));
    ofNullable(isManaged).ifPresent(builder::setManaged);

    return builder.build();
  }

}
