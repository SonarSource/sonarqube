/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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

import com.google.common.base.Strings;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.commons.lang3.StringUtils;
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.api.utils.DateUtils;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentQuery;
import org.sonar.db.entity.EntityDto;
import org.sonar.db.permission.GlobalPermission;
import org.sonar.server.component.ComponentCleanerService;
import org.sonar.server.project.DeletedProject;
import org.sonar.server.project.Project;
import org.sonar.server.project.ProjectLifeCycleListeners;
import org.sonar.server.project.Visibility;
import org.sonar.server.user.UserSession;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.Math.min;
import static java.lang.String.format;
import static java.util.stream.Collectors.toSet;
import static org.sonar.db.component.ComponentQualifiers.APP;
import static org.sonar.db.component.ComponentQualifiers.PROJECT;
import static org.sonar.db.component.ComponentQualifiers.VIEW;
import static org.sonar.db.Pagination.forPage;
import static org.sonar.server.project.ws.SearchAction.buildDbQuery;
import static org.sonar.server.ws.KeyExamples.KEY_PROJECT_EXAMPLE_001;
import static org.sonar.server.ws.KeyExamples.KEY_PROJECT_EXAMPLE_002;
import static org.sonarqube.ws.client.project.ProjectsWsParameters.PARAM_ANALYZED_BEFORE;
import static org.sonarqube.ws.client.project.ProjectsWsParameters.PARAM_ON_PROVISIONED_ONLY;
import static org.sonarqube.ws.client.project.ProjectsWsParameters.PARAM_PROJECTS;
import static org.sonarqube.ws.client.project.ProjectsWsParameters.PARAM_QUALIFIERS;
import static org.sonarqube.ws.client.project.ProjectsWsParameters.PARAM_VISIBILITY;

public class BulkDeleteAction implements ProjectsWsAction {

  private static final String ACTION = "bulk_delete";

  private final ComponentCleanerService componentCleanerService;
  private final DbClient dbClient;
  private final UserSession userSession;
  private final ProjectLifeCycleListeners projectLifeCycleListeners;

  public BulkDeleteAction(ComponentCleanerService componentCleanerService, DbClient dbClient, UserSession userSession,
    ProjectLifeCycleListeners projectLifeCycleListeners) {
    this.componentCleanerService = componentCleanerService;
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.projectLifeCycleListeners = projectLifeCycleListeners;
  }

  @Override
  public void define(WebService.NewController context) {
    String parameterRequiredMessage = format("At least one parameter is required among %s, %s and %s",
      PARAM_ANALYZED_BEFORE, PARAM_PROJECTS, Param.TEXT_QUERY);
    WebService.NewAction action = context
      .createAction(ACTION)
      .setPost(true)
      .setDescription("Delete one or several projects.<br />" +
        "Only the 1'000 first items in project filters are taken into account.<br />" +
        "Requires 'Administer System' permission.<br />" +
        parameterRequiredMessage)
      .setSince("5.2")
      .setHandler(this)
      .setChangelog(
        new Change("7.8", parameterRequiredMessage),
        new Change("9.1", "The parameter '" + PARAM_ANALYZED_BEFORE + "' "
          + "takes into account the analysis of all branches and pull requests, not only the main branch."));

    action
      .createParam(PARAM_PROJECTS)
      .setDescription("Comma-separated list of project keys")
      .setExampleValue(String.join(",", KEY_PROJECT_EXAMPLE_001, KEY_PROJECT_EXAMPLE_002));

    action.createParam(Param.TEXT_QUERY)
      .setDescription("Limit to: <ul>" +
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
      .setDescription("Filter the projects for which last analysis of any branch is older than the given date (exclusive).<br> " +
        "Either a date (server timezone) or datetime can be provided.")
      .setSince("6.6")
      .setExampleValue("2017-10-19 or 2017-10-19T13:00:00+0200");

    action.createParam(PARAM_ON_PROVISIONED_ONLY)
      .setDescription("Filter the projects that are provisioned")
      .setBooleanPossibleValues()
      .setDefaultValue("false")
      .setSince("6.6");
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    SearchRequest searchRequest = toSearchWsRequest(request);
    userSession.checkLoggedIn();
    try (DbSession dbSession = dbClient.openSession(false)) {
      userSession.checkPermission(GlobalPermission.ADMINISTER);
      checkAtLeastOneParameterIsPresent(searchRequest);
      checkIfAnalyzedBeforeIsFutureDate(searchRequest);

      ComponentQuery query = buildDbQuery(searchRequest);
      Set<ComponentDto> componentDtos = new HashSet<>(dbClient.componentDao().selectByQuery(dbSession, query, forPage(1).andSize(Integer.MAX_VALUE)));
      List<EntityDto> entities = dbClient.entityDao().selectByKeys(dbSession, componentDtos.stream().map(ComponentDto::getKey).collect(toSet()));
      Set<String> entityUuids = entities.stream().map(EntityDto::getUuid).collect(toSet());
      Map<String, String> mainBranchUuidByEntityUuid = dbClient.branchDao().selectMainBranchesByProjectUuids(dbSession, entityUuids).stream()
        .collect(Collectors.toMap(BranchDto::getProjectUuid, BranchDto::getUuid));

      try {
        entities.forEach(p -> componentCleanerService.deleteEntity(dbSession, p));
      } finally {
        callDeleteListeners(mainBranchUuidByEntityUuid, entities);
      }
    }
    response.noContent();
  }

  private void callDeleteListeners(Map<String, String> mainBranchUuidByEntityUuid , List<EntityDto> entities) {
    Set<DeletedProject> deletedProjects = entities.stream().map(entity -> new DeletedProject(Project.from(entity),
        mainBranchUuidByEntityUuid.get(entity.getUuid()))).collect(toSet());
    projectLifeCycleListeners.onProjectsDeleted(deletedProjects);
  }

  private static void checkAtLeastOneParameterIsPresent(SearchRequest searchRequest) {
    boolean analyzedBeforePresent = !Strings.isNullOrEmpty(searchRequest.getAnalyzedBefore());
    List<String> projects = searchRequest.getProjects();
    boolean projectsPresent = projects != null && !projects.isEmpty();

    boolean queryPresent = !Strings.isNullOrEmpty(searchRequest.getQuery());
    boolean atLeastOneParameterIsPresent = analyzedBeforePresent || projectsPresent || queryPresent;

    checkArgument(atLeastOneParameterIsPresent, format("At least one parameter among %s, %s and %s must be provided",
      PARAM_ANALYZED_BEFORE, PARAM_PROJECTS, Param.TEXT_QUERY));
  }

  private static void checkIfAnalyzedBeforeIsFutureDate(SearchRequest searchRequest) {
    String analyzedBeforeParam = searchRequest.getAnalyzedBefore();

    Optional.ofNullable(analyzedBeforeParam)
      .filter(StringUtils::isNotEmpty)
      .map(DateUtils::parseDateOrDateTime)
      .ifPresent(analyzedBeforeDate -> {
        boolean isFutureDate = new Date().compareTo(analyzedBeforeDate) < 0;
        checkArgument(!isFutureDate, format("Provided value for parameter %s must not be a future date", PARAM_ANALYZED_BEFORE));
      });
  }

  private static SearchRequest toSearchWsRequest(Request request) {
    return SearchRequest.builder()
      .setQualifiers(request.mandatoryParamAsStrings(PARAM_QUALIFIERS))
      .setQuery(request.param(Param.TEXT_QUERY))
      .setVisibility(request.param(PARAM_VISIBILITY))
      .setAnalyzedBefore(request.param(PARAM_ANALYZED_BEFORE))
      .setOnProvisionedOnly(request.mandatoryParamAsBoolean(PARAM_ON_PROVISIONED_ONLY))
      .setProjects(restrictTo1000Values(request.paramAsStrings(PARAM_PROJECTS)))
      .build();
  }

  @CheckForNull
  private static List<String> restrictTo1000Values(@Nullable List<String> values) {
    if (values == null) {
      return null;
    }
    return values.subList(0, min(values.size(), 1_000));
  }
}
