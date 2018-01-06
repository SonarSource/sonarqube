/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.server.projectanalysis.ws;

import com.google.common.collect.ImmutableSet;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.resources.Scopes;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.api.web.UserRole;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.component.SnapshotQuery;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.user.UserSession;
import org.sonar.server.ws.KeyExamples;
import org.sonarqube.ws.ProjectAnalyses;

import static com.google.common.base.Preconditions.checkArgument;
import static org.sonar.api.utils.DateUtils.parseEndingDateOrDateTime;
import static org.sonar.api.utils.DateUtils.parseStartingDateOrDateTime;
import static org.sonar.core.util.Protobuf.setNullable;
import static org.sonar.db.component.SnapshotQuery.SORT_FIELD.BY_DATE;
import static org.sonar.db.component.SnapshotQuery.SORT_ORDER.DESC;
import static org.sonar.server.ws.KeyExamples.KEY_BRANCH_EXAMPLE_001;
import static org.sonar.server.ws.WsUtils.writeProtobuf;
import static org.sonar.server.projectanalysis.ws.EventCategory.OTHER;
import static org.sonar.server.projectanalysis.ws.ProjectAnalysesWsParameters.PARAM_BRANCH;
import static org.sonar.server.projectanalysis.ws.ProjectAnalysesWsParameters.PARAM_CATEGORY;
import static org.sonar.server.projectanalysis.ws.ProjectAnalysesWsParameters.PARAM_FROM;
import static org.sonar.server.projectanalysis.ws.ProjectAnalysesWsParameters.PARAM_PROJECT;
import static org.sonar.server.projectanalysis.ws.ProjectAnalysesWsParameters.PARAM_TO;
import static org.sonar.server.projectanalysis.ws.SearchRequest.DEFAULT_PAGE_SIZE;

public class SearchAction implements ProjectAnalysesWsAction {
  private static final Set<String> ALLOWED_QUALIFIERS = ImmutableSet.of(Qualifiers.PROJECT, Qualifiers.APP, Qualifiers.VIEW);

  private final DbClient dbClient;
  private final ComponentFinder componentFinder;
  private final UserSession userSession;

  public SearchAction(DbClient dbClient, ComponentFinder componentFinder, UserSession userSession) {
    this.dbClient = dbClient;
    this.componentFinder = componentFinder;
    this.userSession = userSession;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction("search")
      .setDescription("Search a project analyses and attached events.<br>" +
        "Requires the following permission: 'Browse' on the specified project")
      .setSince("6.3")
      .setResponseExample(getClass().getResource("search-example.json"))
      .setHandler(this);

    action.addPagingParams(DEFAULT_PAGE_SIZE, 500);

    action.createParam(PARAM_PROJECT)
      .setDescription("Project key")
      .setRequired(true)
      .setExampleValue(KeyExamples.KEY_PROJECT_EXAMPLE_001);

    action.createParam(PARAM_BRANCH)
      .setDescription("Branch key")
      .setSince("6.6")
      .setInternal(true)
      .setExampleValue(KEY_BRANCH_EXAMPLE_001);

    action.createParam(PARAM_CATEGORY)
      .setDescription("Event category. Filter analyses that have at least one event of the category specified.")
      .setPossibleValues(EnumSet.allOf(EventCategory.class))
      .setExampleValue(OTHER.name());

    action.createParam(PARAM_FROM)
      .setDescription("Filter analyses created after the given date (inclusive). <br>" +
        "Either a date (server timezone) or datetime can be provided")
      .setExampleValue("2013-05-01")
      .setSince("6.5");

    action.createParam(PARAM_TO)
      .setDescription("Filter analyses created before the given date (inclusive). <br>" +
        "Either a date (server timezone) or datetime can be provided")
      .setExampleValue("2017-10-19 or 2017-10-19T13:00:00+0200")
      .setSince("6.5");

  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    SearchData searchData = load(toWsRequest(request));
    ProjectAnalyses.SearchResponse searchResponse = new SearchResponseBuilder(searchData).build();
    writeProtobuf(searchResponse, request, response);
  }

  private static SearchRequest toWsRequest(Request request) {
    String category = request.param(PARAM_CATEGORY);
    return SearchRequest.builder()
      .setProject(request.mandatoryParam(PARAM_PROJECT))
      .setBranch(request.param(PARAM_BRANCH))
      .setCategory(category == null ? null : EventCategory.valueOf(category))
      .setPage(request.mandatoryParamAsInt(Param.PAGE))
      .setPageSize(request.mandatoryParamAsInt(Param.PAGE_SIZE))
      .setFrom(request.param(PARAM_FROM))
      .setTo(request.param(PARAM_TO))
      .build();
  }

  private SearchData load(SearchRequest request) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      SearchData.Builder searchResults = SearchData.builder(dbSession, request);
      addProject(searchResults);
      checkPermission(searchResults.getProject());
      addAnalyses(searchResults);
      addEvents(searchResults);
      return searchResults.build();
    }
  }

  private void addAnalyses(SearchData.Builder data) {
    SnapshotQuery dbQuery = new SnapshotQuery()
      .setComponentUuid(data.getProject().uuid())
      .setStatus(SnapshotDto.STATUS_PROCESSED)
      .setSort(BY_DATE, DESC);
    setNullable(data.getRequest().getFrom(), from -> dbQuery.setCreatedAfter(parseStartingDateOrDateTime(from).getTime()));
    setNullable(data.getRequest().getTo(), to -> dbQuery.setCreatedBefore(parseEndingDateOrDateTime(to).getTime() + 1_000L));
    data.setAnalyses(dbClient.snapshotDao().selectAnalysesByQuery(data.getDbSession(), dbQuery));
  }

  private void addEvents(SearchData.Builder data) {
    List<String> analyses = data.getAnalyses().stream().map(SnapshotDto::getUuid).collect(MoreCollectors.toList());
    data.setEvents(dbClient.eventDao().selectByAnalysisUuids(data.getDbSession(), analyses));
  }

  private void checkPermission(ComponentDto project) {
    userSession.checkComponentPermission(UserRole.USER, project);
  }

  private void addProject(SearchData.Builder data) {
    ComponentDto project = loadComponent(data.getDbSession(), data.getRequest());
    checkArgument(Scopes.PROJECT.equals(project.scope()) && ALLOWED_QUALIFIERS.contains(project.qualifier()), "A project, portfolio or application is required");
    data.setProject(project);
  }

  private ComponentDto loadComponent(DbSession dbSession, SearchRequest request) {
    String project = request.getProject();
    String branch = request.getBranch();
    if (branch != null) {
      return componentFinder.getByKeyAndBranch(dbSession, project, branch);
    }
    return componentFinder.getByKey(dbSession, project);
  }

}
