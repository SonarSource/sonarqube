/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
package org.sonar.server.qualitygate.ws;

import com.google.common.io.Resources;
import java.util.Collection;
import java.util.List;
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.Paging;
import org.sonar.api.web.UserRole;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.qualitygate.ProjectQgateAssociationDto;
import org.sonar.db.qualitygate.ProjectQgateAssociationQuery;
import org.sonar.db.qualitygate.QualityGateDto;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.Qualitygates;

import static com.google.common.base.Preconditions.checkArgument;
import static org.sonar.api.server.ws.WebService.Param.SELECTED;
import static org.sonar.api.utils.Paging.forPageIndex;
import static org.sonar.core.util.Uuids.UUID_EXAMPLE_01;
import static org.sonar.db.qualitygate.ProjectQgateAssociationQuery.ANY;
import static org.sonar.server.qualitygate.ws.CreateAction.NAME_MAXIMUM_LENGTH;
import static org.sonar.server.qualitygate.ws.QualityGatesWsParameters.PARAM_GATE_ID;
import static org.sonar.server.qualitygate.ws.QualityGatesWsParameters.PARAM_GATE_NAME;
import static org.sonar.server.qualitygate.ws.QualityGatesWsParameters.PARAM_PAGE;
import static org.sonar.server.qualitygate.ws.QualityGatesWsParameters.PARAM_PAGE_SIZE;
import static org.sonar.server.qualitygate.ws.QualityGatesWsParameters.PARAM_QUERY;
import static org.sonar.server.ws.WsUtils.writeProtobuf;

public class SearchAction implements QualityGatesWsAction {

  private final DbClient dbClient;
  private final UserSession userSession;
  private final QualityGatesWsSupport wsSupport;

  public SearchAction(DbClient dbClient, UserSession userSession, QualityGatesWsSupport wsSupport) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.wsSupport = wsSupport;
  }

  @Override
  public void define(WebService.NewController controller) {
    WebService.NewAction action = controller.createAction("search")
      .setDescription("Search for projects associated (or not) to a quality gate.<br/>" +
        "Only authorized projects for the current user will be returned.")
      .setSince("4.3")
      .setResponseExample(Resources.getResource(this.getClass(), "search-example.json"))
      .setChangelog(
        new Change("8.4", "Parameter 'gateName' added"),
        new Change("8.4", "Parameter 'gateId' is deprecated. Format changes from integer to string. Use 'gateName' instead."),
        new Change("7.9", "New field 'paging' in response"),
        new Change("7.9", "New field 'key' returning the project key in 'results' response"),
        new Change("7.9", "Field 'more' is deprecated in the response"))
      .setHandler(this);

    action.createParam(PARAM_GATE_ID)
      .setDescription("Quality Gate ID. This parameter is deprecated. Use 'gateName' instead.")
      .setRequired(false)
      .setDeprecatedSince("8.4")
      .setExampleValue(UUID_EXAMPLE_01);

    action.createParam(PARAM_GATE_NAME)
      .setDescription("Quality Gate name")
      .setRequired(false)
      .setMaximumLength(NAME_MAXIMUM_LENGTH)
      .setSince("8.4")
      .setExampleValue("SonarSource Way");

    action.createParam(PARAM_QUERY)
      .setDescription("To search for projects containing this string. If this parameter is set, \"selected\" is set to \"all\".")
      .setExampleValue("abc");

    action.addSelectionModeParam();

    action.createParam(PARAM_PAGE)
      .setDescription("Page number")
      .setDefaultValue("1")
      .setExampleValue("2");

    action.createParam(PARAM_PAGE_SIZE)
      .setDescription("Page size")
      .setExampleValue("10");
  }

  @Override
  public void handle(Request request, Response response) {
    try (DbSession dbSession = dbClient.openSession(false)) {

      String gateUuid = request.param(PARAM_GATE_ID);
      String gateName = request.param(PARAM_GATE_NAME);

      checkArgument(gateName != null ^ gateUuid != null, "One of 'gateId' or 'gateName' must be provided, and not both");

      QualityGateDto qualityGate;
      if (gateUuid != null) {
        qualityGate = wsSupport.getByUuid(dbSession, gateUuid);
      } else {
        qualityGate = wsSupport.getByName(dbSession, gateName);
      }

      ProjectQgateAssociationQuery projectQgateAssociationQuery = ProjectQgateAssociationQuery.builder()
        .qualityGate(qualityGate)
        .membership(request.param(PARAM_QUERY) == null ? request.param(SELECTED) : ANY)
        .projectSearch(request.param(PARAM_QUERY))
        .pageIndex(request.paramAsInt(PARAM_PAGE))
        .pageSize(request.paramAsInt(PARAM_PAGE_SIZE))
        .build();
      List<ProjectQgateAssociationDto> projects = dbClient.projectQgateAssociationDao().selectProjects(dbSession, projectQgateAssociationQuery);
      List<ProjectQgateAssociationDto> authorizedProjects = keepAuthorizedProjects(dbSession, projects);
      Paging paging = forPageIndex(projectQgateAssociationQuery.pageIndex())
        .withPageSize(projectQgateAssociationQuery.pageSize())
        .andTotal(authorizedProjects.size());
      List<ProjectQgateAssociationDto> paginatedProjects = getPaginatedProjects(authorizedProjects, paging);

      Qualitygates.SearchResponse.Builder createResponse = Qualitygates.SearchResponse.newBuilder().setMore(paging.hasNextPage());
      createResponse.getPagingBuilder()
        .setPageIndex(paging.pageIndex())
        .setPageSize(paging.pageSize())
        .setTotal(paging.total())
        .build();

      for (ProjectQgateAssociationDto project : paginatedProjects) {
        createResponse.addResultsBuilder()
          .setName(project.getName())
          .setKey(project.getKey())
          .setSelected(project.getGateUuid() != null);
      }

      writeProtobuf(createResponse.build(), request, response);
    }
  }

  private static List<ProjectQgateAssociationDto> getPaginatedProjects(List<ProjectQgateAssociationDto> projects, Paging paging) {
    return projects.stream().skip(paging.offset()).limit(paging.pageSize()).toList();
  }

  private List<ProjectQgateAssociationDto> keepAuthorizedProjects(DbSession dbSession, List<ProjectQgateAssociationDto> projects) {
    List<String> projectUuids = projects.stream().map(ProjectQgateAssociationDto::getUuid).collect(MoreCollectors.toList());
    Collection<String> authorizedProjectIds = dbClient.authorizationDao().keepAuthorizedProjectUuids(dbSession, projectUuids, userSession.getUuid(), UserRole.USER);
    return projects.stream().filter(project -> authorizedProjectIds.contains(project.getUuid())).collect(MoreCollectors.toList());
  }
}
